#ifndef PODAURA_WINDOWS_MEDIA_SESSION_H
#define PODAURA_WINDOWS_MEDIA_SESSION_H

#define NOMINMAX
#define WIN32_LEAN_AND_MEAN

#include "PodAuraWindowsMediaPlayer.h"

#include <windows.h>
#include <shobjidl_core.h>

#include <winrt/Windows.Media.h>
#include <winrt/Windows.Storage.Streams.h>
#include <winrt/base.h>

#include <atomic>
#include <cstdint>
#include <deque>
#include <memory>
#include <mutex>
#include <string>
#include <unordered_map>
#include <utility>
#include <vector>

namespace podaura::windows_media {

void clear_last_error();
void set_last_error(std::string message);
const char *last_error();
void capture_error(const winrt::hresult_error &error);
void capture_unknown_error();
bool ensure_unpacked_start_menu_shortcut() noexcept;

struct Tooltips {
    std::wstring previous;
    std::wstring play;
    std::wstring pause;
    std::wstring next;

    static Tooltips from_native(const PodAuraTaskbarTooltips *tooltips);
};

struct Availability {
    bool can_play = false;
    bool can_pause = false;
    bool can_toggle = false;
    bool can_previous = false;
    bool can_next = false;
    bool can_seek = false;

    static Availability from_native(const PodAuraMediaCommandAvailability &value);
};

class Session;

struct WindowRegistration {
    HWND window = nullptr;
    bool main_window = false;
    bool toolbar_registered = false;
    Tooltips tooltips;
    std::weak_ptr<Session> session;
    std::atomic<bool> active{true};
    std::shared_ptr<WindowRegistration> *subclass_owner = nullptr;
    bool refresh_requested = false;
    int refresh_attempts = 0;
};

bool change_window_subclass(
        const std::shared_ptr<WindowRegistration> &registration,
        bool install
);

class Session final : public std::enable_shared_from_this<Session> {
public:
    explicit Session(PodAuraMediaCommandCallback callback);
    ~Session();

    bool attach_window(HWND window, bool main_window, const Tooltips &tooltips);
    bool update_window(HWND window, const Tooltips &tooltips);
    bool detach_window(HWND window);
    bool update(
            const PodAuraMediaNowPlayingInfo &info,
            const PodAuraMediaCommandAvailability &availability
    );
    bool clear();
    void close();
    int32_t dispatch_pending();

    static LRESULT CALLBACK subclass_proc(
            HWND window,
            UINT message,
            WPARAM w_param,
            LPARAM l_param,
            UINT_PTR subclass_id,
            DWORD_PTR reference
    );

private:
    struct Info {
        std::wstring title;
        std::wstring artist;
        std::wstring album;
        bool has_duration = false;
        double duration_seconds = 0.0;
        bool has_elapsed_time = false;
        double elapsed_seconds = 0.0;
        double playback_rate = 0.0;
        double default_playback_rate = 1.0;
        bool has_queue_index = false;
        int64_t queue_index = 0;
        bool has_queue_count = false;
        int64_t queue_count = 0;
        int32_t media_type = PODAURA_MEDIA_TYPE_AUDIO;
        int32_t playback_state = PODAURA_PLAYBACK_STATE_STOPPED;
        std::string artwork_id;
        std::vector<uint8_t> artwork_bytes;
    };

    static Info copy_info(const PodAuraMediaNowPlayingInfo &source);

    bool command_available(int32_t command) const;
    void queue_command(int32_t command, double position_seconds);

    void initialize_smtc(HWND window);
    void release_smtc_locked() noexcept;
    void on_smtc_button(winrt::Windows::Media::SystemMediaTransportControlsButton button);
    winrt::Windows::Storage::Streams::RandomAccessStreamReference
    create_artwork_reference() const;
    void apply_smtc_locked();
    void clear_smtc_locked();

    void ensure_taskbar();
    void update_taskbar(WindowRegistration &registration);
    void refresh_taskbars_locked();
    void hide_taskbar(WindowRegistration &registration);
    void on_window_message(WindowRegistration &registration, UINT message, WPARAM w_param);

    std::recursive_mutex mutex_;
    std::atomic<bool> active_{true};
    PodAuraMediaCommandCallback callback_;
    std::deque<std::pair<int32_t, double>> pending_commands_;
    bool ro_initialized_ = false;
    bool has_media_ = false;
    HWND main_window_ = nullptr;
    Info info_;
    Availability availability_;
    std::unordered_map<HWND, std::shared_ptr<WindowRegistration>> windows_;
    winrt::com_ptr<ITaskbarList3> taskbar_;
    winrt::Windows::Media::SystemMediaTransportControls smtc_{nullptr};
    winrt::event_token button_token_{};
    winrt::event_token position_token_{};
    bool smtc_events_attached_ = false;
    std::string artwork_id_;
    winrt::Windows::Storage::Streams::RandomAccessStreamReference artwork_reference_{nullptr};
};

} // namespace podaura::windows_media

#endif
