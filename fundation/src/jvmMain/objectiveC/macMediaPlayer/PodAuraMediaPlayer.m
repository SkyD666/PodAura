#import "PodAuraMediaPlayer.h"

#import <AppKit/AppKit.h>
#import <MediaPlayer/MediaPlayer.h>
#import <math.h>

@interface PodAuraMediaSession : NSObject

@property(nonatomic, assign) PodAuraMediaCommandCallback callback;
@property(nonatomic, assign, getter=isActive) BOOL active;
@property(nonatomic, copy, nullable) NSString *currentArtworkID;
@property(nonatomic, strong, nullable) MPMediaItemArtwork *currentArtwork;

- (nullable instancetype)initWithCallback:(PodAuraMediaCommandCallback)callback;

- (BOOL)updateWithInfo:(const PodAuraMediaNowPlayingInfo *)info
          availability:(const PodAuraMediaCommandAvailability *)availability;

- (void)clear;

- (void)invalidate;

@end

static NSString *PodAuraString(const char *value) {
    if (value == NULL) return nil;
    NSString *string = [NSString stringWithUTF8String:value];
    return string.length > 0 ? string : nil;
}

static void PodAuraPutString(

NSMutableDictionary<NSString *, id> *dictionary,
NSString *key,
const char *value
) {
NSString *string = PodAuraString(value);
if (string != nil) dictionary[key] =
string;
}

@implementation PodAuraMediaSession

- (nullable instancetype)initWithCallback:(PodAuraMediaCommandCallback)callback {
    if (callback == NULL) return nil;

    self = [super init];
    if (self != nil) {
        _callback = callback;
        _active = YES;
        [self attachCommandTargets];
        [self disableAllCommands];
    }
    return self;
}

- (void)attachCommandTargets {
    MPRemoteCommandCenter *center = [MPRemoteCommandCenter sharedCommandCenter];
    [center.playCommand addTarget:self action:@selector(handlePlay:)];
    [center.pauseCommand addTarget:self action:@selector(handlePause:)];
    [center.togglePlayPauseCommand addTarget:self action:@selector(handleTogglePlayPause:)];
    [center.previousTrackCommand addTarget:self action:@selector(handlePrevious:)];
    [center.nextTrackCommand addTarget:self action:@selector(handleNext:)];
    [center.changePlaybackPositionCommand addTarget:self action:@selector(handlePositionChange:)];
}

- (void)detachCommandTargets {
    MPRemoteCommandCenter *center = [MPRemoteCommandCenter sharedCommandCenter];
    [center.playCommand removeTarget:self action:@selector(handlePlay:)];
    [center.pauseCommand removeTarget:self action:@selector(handlePause:)];
    [center.togglePlayPauseCommand removeTarget:self action:@selector(handleTogglePlayPause:)];
    [center.previousTrackCommand removeTarget:self action:@selector(handlePrevious:)];
    [center.nextTrackCommand removeTarget:self action:@selector(handleNext:)];
    [center.changePlaybackPositionCommand removeTarget:self action:@selector(handlePositionChange:)];
}

- (MPRemoteCommandHandlerStatus)dispatchCommand:(PodAuraMediaCommand)command
                                       position:(double)position {
    PodAuraMediaCommandCallback callback;
    @synchronized (self) {
        if (!self.isActive || self.callback == NULL) {
            return MPRemoteCommandHandlerStatusNoActionableNowPlayingItem;
        }
        callback = self.callback;
    }

    int32_t result = callback((int32_t) command, position);
    if (result > 0) return MPRemoteCommandHandlerStatusSuccess;
    if (result == 0) return MPRemoteCommandHandlerStatusNoActionableNowPlayingItem;
    return MPRemoteCommandHandlerStatusCommandFailed;
}

- (MPRemoteCommandHandlerStatus)handlePlay:(MPRemoteCommandEvent *)event {
    (void) event;
    return [self dispatchCommand:PODAURA_MEDIA_COMMAND_PLAY position:0.0];
}

- (MPRemoteCommandHandlerStatus)handlePause:(MPRemoteCommandEvent *)event {
    (void) event;
    return [self dispatchCommand:PODAURA_MEDIA_COMMAND_PAUSE position:0.0];
}

- (MPRemoteCommandHandlerStatus)handleTogglePlayPause:(MPRemoteCommandEvent *)event {
    (void) event;
    return [self dispatchCommand:PODAURA_MEDIA_COMMAND_TOGGLE_PLAY_PAUSE position:0.0];
}

- (MPRemoteCommandHandlerStatus)handlePrevious:(MPRemoteCommandEvent *)event {
    (void) event;
    return [self dispatchCommand:PODAURA_MEDIA_COMMAND_PREVIOUS position:0.0];
}

- (MPRemoteCommandHandlerStatus)handleNext:(MPRemoteCommandEvent *)event {
    (void) event;
    return [self dispatchCommand:PODAURA_MEDIA_COMMAND_NEXT position:0.0];
}

- (MPRemoteCommandHandlerStatus)handlePositionChange:(MPRemoteCommandEvent *)event {
    if (![event isKindOfClass:[MPChangePlaybackPositionCommandEvent class]]) {
        return MPRemoteCommandHandlerStatusCommandFailed;
    }
    double position = ((MPChangePlaybackPositionCommandEvent *) event).positionTime;
    if (!isfinite(position)) return MPRemoteCommandHandlerStatusCommandFailed;
    return [self dispatchCommand:PODAURA_MEDIA_COMMAND_CHANGE_PLAYBACK_POSITION
                        position:position];
}

- (nullable MPMediaItemArtwork

*)artworkForInfo:(const PodAuraMediaNowPlayingInfo *)info {
    NSString *artworkID = PodAuraString(info->artwork_id);
    BOOL hasValidArtwork = artworkID != nil &&
                           info->artwork_bytes != NULL &&
                           info->artwork_length > 0 &&
                           info->artwork_width > 0 &&
                           info->artwork_height > 0;
    if (!hasValidArtwork) {
        self.currentArtworkID = nil;
        self.currentArtwork = nil;
        return nil;
    }
    if ([self.currentArtworkID isEqualToString:artworkID] && self.currentArtwork != nil) {
        return self.currentArtwork;
    }

    self.currentArtworkID = nil;
    self.currentArtwork = nil;
    NSData *data = [NSData dataWithBytes:info->artwork_bytes
                                  length:(NSUInteger) info->artwork_length];
    NSImage *sourceImage = [[NSImage alloc] initWithData:data];
    if (sourceImage == nil) return nil;

    NSSize boundsSize = NSMakeSize(info->artwork_width, info->artwork_height);
    MPMediaItemArtwork *artwork = [[MPMediaItemArtwork alloc]
            initWithBoundsSize:boundsSize
                requestHandler:^NSImage *(CGSize requestedSize) {
                    CGFloat width = isfinite(requestedSize.width) && requestedSize.width > 0.0
                                    ? MIN(requestedSize.width, boundsSize.width)
                                    : boundsSize.width;
                    CGFloat height = isfinite(requestedSize.height) && requestedSize.height > 0.0
                                     ? MIN(requestedSize.height, boundsSize.height)
                                     : boundsSize.height;
                    NSImage *result = [sourceImage copy];
                    result.size = NSMakeSize(width, height);
                    return result;
                }];
    self.currentArtworkID = artworkID;
    self.currentArtwork = artwork;
    return artwork;
}

- (BOOL)updateWithInfo:(const PodAuraMediaNowPlayingInfo *)info
          availability:(const PodAuraMediaCommandAvailability *)availability {
    if (info == NULL || availability == NULL) return NO;

    @synchronized (self) {
        if (!self.isActive) return NO;

        NSMutableDictionary < NSString * , id > *nowPlayingInfo = [NSMutableDictionary dictionary];
        PodAuraPutString(nowPlayingInfo, MPMediaItemPropertyTitle, info->title);
        PodAuraPutString(nowPlayingInfo, MPMediaItemPropertyArtist, info->artist);
        PodAuraPutString(nowPlayingInfo, MPMediaItemPropertyAlbumTitle, info->album);
        PodAuraPutString(nowPlayingInfo, MPMediaItemPropertyPodcastTitle, info->album);

        if (info->has_duration && isfinite(info->duration_seconds)) {
            nowPlayingInfo[MPMediaItemPropertyPlaybackDuration] = @(info->duration_seconds);
        }
        if (info->has_elapsed_time && isfinite(info->elapsed_seconds)) {
            nowPlayingInfo[MPNowPlayingInfoPropertyElapsedPlaybackTime] = @(info->elapsed_seconds);
        }
        if (isfinite(info->playback_rate)) {
            nowPlayingInfo[MPNowPlayingInfoPropertyPlaybackRate] = @(info->playback_rate);
        }
        if (isfinite(info->default_playback_rate)) {
            nowPlayingInfo[MPNowPlayingInfoPropertyDefaultPlaybackRate] =
                    @(info->default_playback_rate);
        }
        if (info->has_queue_index) {
            nowPlayingInfo[MPNowPlayingInfoPropertyPlaybackQueueIndex] = @(info->queue_index);
        }
        if (info->has_queue_count) {
            nowPlayingInfo[MPNowPlayingInfoPropertyPlaybackQueueCount] = @(info->queue_count);
        }
        nowPlayingInfo[MPNowPlayingInfoPropertyMediaType] = @(info->media_type);

        MPMediaItemArtwork *artwork = [self artworkForInfo:info];
        if (artwork != nil) nowPlayingInfo[MPMediaItemPropertyArtwork] = artwork;

        MPNowPlayingInfoCenter *infoCenter = [MPNowPlayingInfoCenter defaultCenter];
        infoCenter.nowPlayingInfo = nowPlayingInfo;
        infoCenter.playbackState = (MPNowPlayingPlaybackState) info->playback_state;
        [self applyCommandAvailability:availability];
        return YES;
    }
}

- (void)applyCommandAvailability:(const PodAuraMediaCommandAvailability *)availability {
    MPRemoteCommandCenter *center = [MPRemoteCommandCenter sharedCommandCenter];
    center.playCommand.enabled = availability->can_play != 0;
    center.pauseCommand.enabled = availability->can_pause != 0;
    center.togglePlayPauseCommand.enabled = availability->can_toggle_play_pause != 0;
    center.previousTrackCommand.enabled = availability->can_go_previous != 0;
    center.nextTrackCommand.enabled = availability->can_go_next != 0;
    center.changePlaybackPositionCommand.enabled =
            availability->can_change_playback_position != 0;
}

- (void)disableAllCommands {
    MPRemoteCommandCenter *center = [MPRemoteCommandCenter sharedCommandCenter];
    center.playCommand.enabled = NO;
    center.pauseCommand.enabled = NO;
    center.togglePlayPauseCommand.enabled = NO;
    center.previousTrackCommand.enabled = NO;
    center.nextTrackCommand.enabled = NO;
    center.changePlaybackPositionCommand.enabled = NO;
}

- (void)clearLocked {
    [self disableAllCommands];
    MPNowPlayingInfoCenter *infoCenter = [MPNowPlayingInfoCenter defaultCenter];
    infoCenter.playbackState = MPNowPlayingPlaybackStateStopped;
    infoCenter.nowPlayingInfo = nil;
    self.currentArtworkID = nil;
    self.currentArtwork = nil;
}

- (void)clear {
    @synchronized (self) {
        if (self.isActive) [self clearLocked];
    }
}

- (void)invalidate {
    @synchronized (self) {
        if (!self.isActive) return;
        [self clearLocked];
        [self detachCommandTargets];
        self.active = NO;
        self.callback = NULL;
    }
}

@end

int32_t podaura_media_player_api_version(void) {
    return PODAURA_MEDIA_PLAYER_API_VERSION;
}

void *podaura_media_session_create(PodAuraMediaCommandCallback callback) {
    @autoreleasepool {
        @try {
            PodAuraMediaSession *session =
                    [[PodAuraMediaSession alloc] initWithCallback:callback];
            return session == nil ? NULL : (__bridge_retained void *) session;
        } @catch (NSException *exception) {
            NSLog(@"PodAura media session creation failed: %@", exception.reason);
            return NULL;
        }
    }
}

int32_t podaura_media_session_update(
        void *session,
        const PodAuraMediaNowPlayingInfo *info,
        const PodAuraMediaCommandAvailability *availability
) {
    if (session == NULL) return 0;
    @autoreleasepool {
        @try {
            return [(__bridge PodAuraMediaSession *) session updateWithInfo:info
                                                               availability:availability]
                   ? 1
                   : 0;
        } @catch (NSException *exception) {
            NSLog(@"PodAura media session update failed: %@", exception.reason);
            return 0;
        }
    }
}

int32_t podaura_media_session_clear(void *session) {
    if (session == NULL) return 0;
    @autoreleasepool {
        @try {
            [(__bridge PodAuraMediaSession *) session clear];
            return 1;
        } @catch (NSException *exception) {
            NSLog(@"PodAura media session clear failed: %@", exception.reason);
            return 0;
        }
    }
}

int32_t podaura_media_session_destroy(void *session) {
    if (session == NULL) return 0;
    @autoreleasepool {
        PodAuraMediaSession *ownedSession = (__bridge_transfer PodAuraMediaSession *) session;
        @try {
            [ownedSession invalidate];
            return 1;
        } @catch (NSException *exception) {
            NSLog(@"PodAura media session destruction failed: %@", exception.reason);
            return 0;
        }
    }
}

int32_t podaura_media_player_request_published_artwork(double width, double height) {
    @autoreleasepool {
        @try {
            NSDictionary < NSString * , id > *nowPlayingInfo =
                                                [MPNowPlayingInfoCenter defaultCenter].nowPlayingInfo;
            MPMediaItemArtwork *artwork = nowPlayingInfo[MPMediaItemPropertyArtwork];
            if (artwork == nil) return 0;
            NSImage *image = [artwork imageWithSize:CGSizeMake(width, height)];
            return image == nil ? 0 : 1;
        } @catch (NSException *exception) {
            NSLog(@"PodAura published artwork request failed: %@", exception.reason);
            return 0;
        }
    }
}
