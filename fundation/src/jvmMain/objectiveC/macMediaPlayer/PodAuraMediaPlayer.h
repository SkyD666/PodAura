#ifndef PODAURA_MEDIA_PLAYER_H
#define PODAURA_MEDIA_PLAYER_H

#include <stdint.h>

#if defined(__cplusplus)
extern "C" {
#endif

#define PODAURA_MEDIA_PLAYER_API_VERSION 1

typedef enum {
    PODAURA_MEDIA_COMMAND_PLAY = 1,
    PODAURA_MEDIA_COMMAND_PAUSE = 2,
    PODAURA_MEDIA_COMMAND_TOGGLE_PLAY_PAUSE = 3,
    PODAURA_MEDIA_COMMAND_PREVIOUS = 4,
    PODAURA_MEDIA_COMMAND_NEXT = 5,
    PODAURA_MEDIA_COMMAND_CHANGE_PLAYBACK_POSITION = 6,
} PodAuraMediaCommand;

typedef int32_t (*PodAuraMediaCommandCallback)(
        int32_t command,
        double position_seconds
);

typedef struct {
    const char *title;
    const char *artist;
    const char *album;

    int32_t has_duration;
    double duration_seconds;
    int32_t has_elapsed_time;
    double elapsed_seconds;
    double playback_rate;
    double default_playback_rate;

    int32_t has_queue_index;
    int64_t queue_index;
    int32_t has_queue_count;
    int64_t queue_count;

    int32_t media_type;
    int32_t playback_state;

    const char *artwork_id;
    const uint8_t *artwork_bytes;
    int64_t artwork_length;
    int32_t artwork_width;
    int32_t artwork_height;
} PodAuraMediaNowPlayingInfo;

typedef struct {
    int32_t can_play;
    int32_t can_pause;
    int32_t can_toggle_play_pause;
    int32_t can_go_previous;
    int32_t can_go_next;
    int32_t can_change_playback_position;
} PodAuraMediaCommandAvailability;

__attribute__((visibility("default")))
int32_t podaura_media_player_api_version(void);

__attribute__((visibility("default")))
void *podaura_media_session_create(PodAuraMediaCommandCallback callback);

__attribute__((visibility("default")))
int32_t podaura_media_session_update(
        void *session,
        const PodAuraMediaNowPlayingInfo *info,
        const PodAuraMediaCommandAvailability *availability
);

__attribute__((visibility("default")))
int32_t podaura_media_session_clear(void *session);

__attribute__((visibility("default")))
int32_t podaura_media_session_destroy(void *session);

__attribute__((visibility("default")))
int32_t podaura_media_player_request_published_artwork(
        double width,
        double height
);

#if defined(__cplusplus)
}
#endif

#endif
