/* SPDX-License-Identifier: LGPL-2.1-or-later */
#pragma once
#include <stdbool.h>
#include <stdint.h>
#include "client.h"

#ifdef __OBJC__
#import <Foundation/Foundation.h>
// K/N cannot override NSObject category methods without a protocol declaration.
@protocol PodAuraPictureInPictureObserver <NSObject>
- (void)observeValueForKeyPath:(NSString *)keyPath ofObject:(id)object
                       change:(NSDictionary *)change context:(void *)context;
@end
#endif

// The layer is an AVSampleBufferDisplayLayer. The output retains it until destroy.
MPV_EXPORT void *podaura_output_create(void *layer, int width, int height);
MPV_EXPORT void podaura_output_resize(void *output, int width, int height);
// Disabling waits for outstanding GPU work before returning.
MPV_EXPORT void podaura_output_set_active(void *output, bool active);
MPV_EXPORT void podaura_output_destroy(void *output);
MPV_EXPORT int podaura_output_attach(mpv_handle *mpv, void *output);
// Pure audio emits a still only when the artwork changes (UIImage, or NULL).
MPV_EXPORT void podaura_output_show_artwork(void *layer, void *image);
MPV_EXPORT uint64_t podaura_output_frame_count(void *output);
MPV_EXPORT bool podaura_output_failed(void *output);
