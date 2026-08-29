#ifndef PODAURA_WINDOWS_MEDIA_PLAYER_H
#define PODAURA_WINDOWS_MEDIA_PLAYER_H

#include <stdint.h>

#if defined(__cplusplus)
extern "C" {
#endif

#define PODAURA_WINDOWS_MEDIA_PLAYER_API_VERSION 2

#if defined(_WIN32)
#define PODAURA_WINDOWS_MEDIA_API __declspec(dllexport)
#else
#define PODAURA_WINDOWS_MEDIA_API
#endif

typedef enum {
    PODAURA_MEDIA_COMMAND_PLAY = 1,
    PODAURA_MEDIA_COMMAND_PAUSE = 2,
    PODAURA_MEDIA_COMMAND_TOGGLE_PLAY_PAUSE = 3,
    PODAURA_MEDIA_COMMAND_PREVIOUS = 4,
    PODAURA_MEDIA_COMMAND_NEXT = 5,
    PODAURA_MEDIA_COMMAND_CHANGE_PLAYBACK_POSITION = 6,
} PodAuraMediaCommand;

typedef enum {
    PODAURA_MEDIA_TYPE_AUDIO = 1,
    PODAURA_MEDIA_TYPE_VIDEO = 2,
} PodAuraMediaType;

typedef enum {
    PODAURA_PLAYBACK_STATE_PLAYING = 1,
    PODAURA_PLAYBACK_STATE_PAUSED = 2,
    PODAURA_PLAYBACK_STATE_STOPPED = 3,
} PodAuraPlaybackState;

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

typedef struct {
    const wchar_t *previous;
    const wchar_t *play;
    const wchar_t *pause;
    const wchar_t *next;
} PodAuraTaskbarTooltips;

PODAURA_WINDOWS_MEDIA_API
int32_t podaura_windows_media_player_api_version(void);

PODAURA_WINDOWS_MEDIA_API
const char *podaura_windows_media_player_last_error(void);

PODAURA_WINDOWS_MEDIA_API
int32_t podaura_windows_media_player_ensure_app_identity(void);

PODAURA_WINDOWS_MEDIA_API
void *podaura_windows_media_session_create(PodAuraMediaCommandCallback callback);

PODAURA_WINDOWS_MEDIA_API
int32_t podaura_windows_media_session_attach_window(
        void *session,
        void *window_handle,
        int32_t is_main_window,
        const PodAuraTaskbarTooltips *tooltips
);

PODAURA_WINDOWS_MEDIA_API
int32_t podaura_windows_media_session_update_window(
        void *session,
        void *window_handle,
        const PodAuraTaskbarTooltips *tooltips
);

PODAURA_WINDOWS_MEDIA_API
int32_t podaura_windows_media_session_detach_window(
        void *session,
        void *window_handle
);

PODAURA_WINDOWS_MEDIA_API
int32_t podaura_windows_media_session_update(
        void *session,
        const PodAuraMediaNowPlayingInfo *info,
        const PodAuraMediaCommandAvailability *availability
);

PODAURA_WINDOWS_MEDIA_API
int32_t podaura_windows_media_session_dispatch_pending(void *session);

PODAURA_WINDOWS_MEDIA_API
int32_t podaura_windows_media_session_clear(void *session);

PODAURA_WINDOWS_MEDIA_API
int32_t podaura_windows_media_session_destroy(void *session);

#if defined(__cplusplus)
}
#endif

#endif
