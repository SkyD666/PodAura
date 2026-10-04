/* SPDX-License-Identifier: LGPL-2.1-or-later */
#import <AVFoundation/AVFoundation.h>
#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>
#include "config.h"
#include "options/options.h"
#include "video/out/vulkan/context.h"
#include "video/out/vulkan/utils.h"
#include <vulkan/vulkan_metal.h>
#include "context_podaura.h"
#include "include/mpv/podaura.h"

@interface PodAuraVideoOutput : NSObject {
@public
    AVSampleBufferDisplayLayer *layer;
    NSRecursiveLock *lock;
    int width, height;
    bool active;
    bool failed;
    uint64_t frames;
}
@end
@implementation PodAuraVideoOutput
- (void)dealloc {
    [layer release];
    [lock release];
    [super dealloc];
}
@end

void *podaura_output_create(void *display, int width, int height)
{
    PodAuraVideoOutput *output = [PodAuraVideoOutput new];
    output->layer = [(AVSampleBufferDisplayLayer *)display retain];
    output->lock = [NSRecursiveLock new];
    output->width = MAX(2, width);
    output->height = MAX(2, height);
    output->active = true;
    return output;
}

void podaura_output_resize(void *ptr, int width, int height)
{
    PodAuraVideoOutput *output = ptr;
    [output->lock lock];
    output->width = MAX(2, width);
    output->height = MAX(2, height);
    [output->lock unlock];
}

void podaura_output_set_active(void *ptr, bool active)
{
    PodAuraVideoOutput *output = ptr;
    [output->lock lock];
    output->active = active;
    [output->lock unlock];
}

void podaura_output_destroy(void *ptr) { [(PodAuraVideoOutput *)ptr release]; }

uint64_t podaura_output_frame_count(void *ptr)
{
    PodAuraVideoOutput *o = ptr;
    [o->lock lock];
    uint64_t frames = o->frames;
    [o->lock unlock];
    return frames;
}

bool podaura_output_failed(void *ptr)
{
    PodAuraVideoOutput *o = ptr;
    [o->lock lock];
    bool failed = o->failed;
    [o->lock unlock];
    return failed;
}

int podaura_output_attach(mpv_handle *mpv, void *output)
{
    int64_t wid = (intptr_t)output;
    return mpv_set_property(mpv, "wid", MPV_FORMAT_INT64, &wid);
}

void podaura_output_show_artwork(void *display, void *artwork)
{
    AVSampleBufferDisplayLayer *layer = display;
    UIImage *image = artwork;
    CGFloat scale = image.size.width > 0 && image.size.height > 0
        ? MIN(640 / image.size.width, 640 / image.size.height) : 1;
    int width = image ? MAX(2, image.size.width * scale) : 640;
    int height = image ? MAX(2, image.size.height * scale) : 640;
    CVPixelBufferRef pixel = NULL;
    NSDictionary *attrs = @{(id)kCVPixelBufferIOSurfacePropertiesKey: @{}};
    if (CVPixelBufferCreate(NULL, width, height, kCVPixelFormatType_32BGRA,
                           (CFDictionaryRef)attrs, &pixel) != kCVReturnSuccess) return;
    CVPixelBufferLockBaseAddress(pixel, 0);
    CGColorSpaceRef color = CGColorSpaceCreateDeviceRGB();
    CGContextRef canvas = CGBitmapContextCreate(CVPixelBufferGetBaseAddress(pixel),
        width, height, 8, CVPixelBufferGetBytesPerRow(pixel), color,
        kCGBitmapByteOrder32Little | kCGImageAlphaPremultipliedFirst);
    if (!canvas) {
        CGColorSpaceRelease(color);
        CVPixelBufferUnlockBaseAddress(pixel, 0);
        CVPixelBufferRelease(pixel);
        return;
    }
    CGContextSetRGBFillColor(canvas, 0, 0, 0, 1);
    CGContextFillRect(canvas, CGRectMake(0, 0, width, height));
    if (image.CGImage) {
        CGContextTranslateCTM(canvas, 0, height);
        CGContextScaleCTM(canvas, 1, -1);
        UIGraphicsPushContext(canvas);
        [image drawInRect:CGRectMake(0, 0, width, height)];
        UIGraphicsPopContext();
    }
    CGContextRelease(canvas);
    CGColorSpaceRelease(color);
    CVPixelBufferUnlockBaseAddress(pixel, 0);
    CMVideoFormatDescriptionRef format = NULL;
    CMSampleBufferRef sample = NULL;
    CMSampleTimingInfo timing = {.duration = kCMTimeInvalid,
        .presentationTimeStamp = layer.controlTimebase ? CMTimebaseGetTime(layer.controlTimebase) : kCMTimeZero,
        .decodeTimeStamp = kCMTimeInvalid};
    if (CMVideoFormatDescriptionCreateForImageBuffer(NULL, pixel, &format) == noErr &&
        CMSampleBufferCreateReadyWithImageBuffer(NULL, pixel, format, &timing, &sample) == noErr) {
        CFArrayRef attachments = CMSampleBufferGetSampleAttachmentsArray(sample, true);
        CFDictionarySetValue((CFMutableDictionaryRef)CFArrayGetValueAtIndex(attachments, 0),
                             kCMSampleAttachmentKey_DisplayImmediately, kCFBooleanTrue);
        [layer flush];
        [layer enqueueSampleBuffer:sample];
    }
    if (sample) CFRelease(sample);
    if (format) CFRelease(format);
    CVPixelBufferRelease(pixel);
}

struct slot { IOSurfaceRef surface; pl_tex texture; };
struct priv {
    struct mpvk_ctx vk;
    PodAuraVideoOutput *output;
    CVPixelBufferPoolRef pool;
    CVPixelBufferRef pixel;
    struct slot slots[3];
    int width, height;
};

static void clear_pool(struct priv *p)
{
    if (p->vk.gpu) pl_gpu_finish(p->vk.gpu);
    for (int i = 0; i < 3; i++) {
        if (p->slots[i].texture) pl_tex_destroy(p->vk.gpu, &p->slots[i].texture);
        if (p->slots[i].surface) CFRelease(p->slots[i].surface);
        p->slots[i] = (struct slot){0};
    }
    if (p->pool) CVPixelBufferPoolRelease(p->pool);
    p->pool = NULL;
}

static bool resize_pool(struct priv *p)
{
    @autoreleasepool {
    PodAuraVideoOutput *o = p->output;
    if (p->pool && p->width == o->width && p->height == o->height) return true;
    clear_pool(p);
    p->width = o->width;
    p->height = o->height;
    NSDictionary *attrs = @{
        (id)kCVPixelBufferWidthKey: @(p->width),
        (id)kCVPixelBufferHeightKey: @(p->height),
        (id)kCVPixelBufferPixelFormatTypeKey: @(kCVPixelFormatType_32BGRA),
        (id)kCVPixelBufferMetalCompatibilityKey: @YES,
        (id)kCVPixelBufferIOSurfacePropertiesKey: @{},
    };
    // The texture cache shares these three surfaces for the pool's lifetime.
    // CoreVideo otherwise ages idle buffers out after one second.
    NSDictionary *poolAttrs = @{(id)kCVPixelBufferPoolMaximumBufferAgeKey: @0};
    return CVPixelBufferPoolCreate(NULL, (CFDictionaryRef)poolAttrs,
                                   (CFDictionaryRef)attrs, &p->pool)
        == kCVReturnSuccess;
    }
}

bool podaura_begin_frame(struct ra_ctx *ctx)
{
    struct priv *p = ctx->priv;
    [p->output->lock lock];
    if (p->output->active && !p->output->failed) {
        if (p->output->layer.status == AVQueuedSampleBufferRenderingStatusFailed)
            [p->output->layer flush];
        if (p->output->layer.readyForMoreMediaData) return true;
    }
    [p->output->lock unlock];
    return false;
}

void podaura_discard_frame(struct ra_ctx *ctx)
{
    struct priv *p = ctx->priv;
    pl_gpu_finish(p->vk.gpu);
    if (pl_gpu_is_failed(p->vk.gpu)) p->output->failed = true;
    if (p->pixel) CVPixelBufferRelease(p->pixel);
    p->pixel = NULL;
    [p->output->lock unlock];
}

bool podaura_start_frame(struct ra_ctx *ctx, struct pl_swapchain_frame *frame)
{
    @autoreleasepool {
    struct priv *p = ctx->priv;
    PodAuraVideoOutput *o = p->output;
    if (!resize_pool(p)) { o->failed = true; goto skip; }
    NSDictionary *limit = @{(id)kCVPixelBufferPoolAllocationThresholdKey: @3};
    if (CVPixelBufferPoolCreatePixelBufferWithAuxAttributes(NULL, p->pool,
            (CFDictionaryRef)limit, &p->pixel) != kCVReturnSuccess) goto skip;
    IOSurfaceRef surface = CVPixelBufferGetIOSurface(p->pixel);
    struct slot *slot = NULL;
    for (int i = 0; i < 3; i++) {
        if (p->slots[i].surface == surface) { slot = &p->slots[i]; break; }
        if (!p->slots[i].surface) slot = &p->slots[i];
    }
    if (!slot) goto skip;
    if (!slot->texture) {
        slot->surface = (IOSurfaceRef)CFRetain(surface);
        slot->texture = pl_tex_create(p->vk.gpu, pl_tex_params(
            .w = p->width, .h = p->height,
            .format = pl_find_named_fmt(p->vk.gpu, "bgra8"),
            .renderable = true, .storable = true, .blit_dst = true,
            .import_handle = PL_HANDLE_IOSURFACE,
            .shared_mem.handle.handle = (void *)surface,
        ));
        if (!slot->texture) {
            o->failed = true;
            MP_ERR(ctx, "Unable to import the PiP IOSurface into Vulkan/Metal\n");
            CFRelease(slot->surface);
            slot->surface = NULL;
            goto skip;
        }
    }
    *frame = (struct pl_swapchain_frame){
        .fbo = slot->texture,
        .color_repr = pl_color_repr_rgb,
        .color_space = pl_color_space_srgb,
    };
    // begin_frame keeps the lock through all gpu-next work and submission.
    return true;
skip:
    if (p->pixel) CVPixelBufferRelease(p->pixel);
    p->pixel = NULL;
    return false;
    }
}

bool podaura_submit_frame(struct ra_ctx *ctx)
{
    struct priv *p = ctx->priv;
    // CoreVideo shares the IOSurface, with no CPU readback. Wait before handing
    // it to the display server; the pool prevents reuse while it is displayed.
    pl_gpu_finish(p->vk.gpu);
    bool ok = false;
    if (pl_gpu_is_failed(p->vk.gpu)) {
        p->output->failed = true;
        goto done;
    }
    @autoreleasepool {
        CMVideoFormatDescriptionRef format = NULL;
        CMSampleBufferRef sample = NULL;
        CMSampleTimingInfo timing = {
            .duration = kCMTimeInvalid,
            .presentationTimeStamp = p->output->layer.controlTimebase
                ? CMTimebaseGetTime(p->output->layer.controlTimebase)
                : CMClockGetTime(CMClockGetHostTimeClock()),
            .decodeTimeStamp = kCMTimeInvalid,
        };
        if (CMVideoFormatDescriptionCreateForImageBuffer(NULL, p->pixel, &format) == noErr &&
            CMSampleBufferCreateReadyWithImageBuffer(NULL, p->pixel, format, &timing, &sample) == noErr) {
            CFArrayRef attachments = CMSampleBufferGetSampleAttachmentsArray(sample, true);
            CFDictionarySetValue((CFMutableDictionaryRef)CFArrayGetValueAtIndex(attachments, 0),
                                 kCMSampleAttachmentKey_DisplayImmediately, kCFBooleanTrue);
            if (p->output->layer.status == AVQueuedSampleBufferRenderingStatusFailed)
                [p->output->layer flush];
            [p->output->layer enqueueSampleBuffer:sample];
            p->output->frames++;
            ok = true;
        }
        if (sample) CFRelease(sample);
        if (format) CFRelease(format);
    }
done:
    if (!ok) p->output->failed = true;
    CVPixelBufferRelease(p->pixel);
    p->pixel = NULL;
    [p->output->lock unlock];
    return ok;
}

static void uninit(struct ra_ctx *ctx)
{
    struct priv *p = ctx->priv;
    if (!p) return;
    if (p->pixel) {
        pl_gpu_finish(p->vk.gpu);
        CVPixelBufferRelease(p->pixel);
        p->pixel = NULL;
        [p->output->lock unlock];
    }
    clear_pool(p);
    ra_vk_ctx_uninit(ctx);
    mpvk_uninit(&p->vk);
    [p->output release];
    p->output = nil;
}

static bool reconfig(struct ra_ctx *ctx)
{
    struct priv *p = ctx->priv;
    [p->output->lock lock];
    ctx->vo->dwidth = p->output->width;
    ctx->vo->dheight = p->output->height;
    [p->output->lock unlock];
    return true;
}

static int control(struct ra_ctx *ctx, int *events, int request, void *arg)
{
    if (request == VOCTRL_CHECK_EVENTS) {
        struct priv *p = ctx->priv;
        [p->output->lock lock];
        if (ctx->vo->dwidth != p->output->width || ctx->vo->dheight != p->output->height) {
            *events |= VO_EVENT_RESIZE;
            reconfig(ctx);
        }
        [p->output->lock unlock];
        return VO_TRUE;
    }
    return VO_NOTIMPL;
}

static bool init(struct ra_ctx *ctx)
{
    struct priv *p = ctx->priv = talloc_zero(ctx, struct priv);
    if (ctx->vo->opts->WinID <= 0) return false;
    p->output = [(PodAuraVideoOutput *)(intptr_t)ctx->vo->opts->WinID retain];
    if (!mpvk_init(&p->vk, ctx, VK_EXT_METAL_SURFACE_EXTENSION_NAME) ||
        !ra_vk_ctx_init(ctx, &p->vk, (struct ra_ctx_params){0}, VK_PRESENT_MODE_FIFO_KHR)) {
        [p->output->lock lock];
        p->output->failed = true;
        [p->output->lock unlock];
        uninit(ctx);
        return false;
    }
    if (!(p->vk.gpu->import_caps.tex & PL_HANDLE_IOSURFACE)) {
        [p->output->lock lock];
        p->output->failed = true;
        [p->output->lock unlock];
        MP_ERR(ctx, "The GPU does not support IOSurface import\n");
        uninit(ctx);
        return false;
    }
    return reconfig(ctx);
}

const struct ra_ctx_fns ra_ctx_podaura = {
    .type = "vulkan", .name = "podaura", .description = "PodAura IOSurface/Metal",
    .init = init, .uninit = uninit, .reconfig = reconfig, .control = control,
};
