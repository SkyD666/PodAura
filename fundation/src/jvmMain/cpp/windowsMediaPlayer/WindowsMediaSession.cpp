#include "WindowsMediaSession.h"

#include <roapi.h>

#include <algorithm>
#include <cmath>

namespace podaura::windows_media {
namespace {

std::wstring from_utf8(const char *value) {
    if (value == nullptr || *value == '\0') return {};
    const int length = MultiByteToWideChar(
            CP_UTF8,
            MB_ERR_INVALID_CHARS,
            value,
            -1,
            nullptr,
            0
    );
    if (length <= 1) return {};
    std::wstring result(static_cast<size_t>(length), L'\0');
    MultiByteToWideChar(
            CP_UTF8,
            MB_ERR_INVALID_CHARS,
            value,
            -1,
            result.data(),
            length
    );
    result.resize(static_cast<size_t>(length - 1));
    return result;
}

std::wstring safe_wide(const wchar_t *value) {
    return value == nullptr ? std::wstring() : std::wstring(value);
}

} // namespace

Tooltips Tooltips::from_native(const PodAuraTaskbarTooltips *tooltips) {
    if (tooltips == nullptr) return {};
    return {
            safe_wide(tooltips->previous),
            safe_wide(tooltips->play),
            safe_wide(tooltips->pause),
            safe_wide(tooltips->next),
    };
}

Availability Availability::from_native(const PodAuraMediaCommandAvailability &value) {
    return {
            value.can_play != 0,
            value.can_pause != 0,
            value.can_toggle_play_pause != 0,
            value.can_go_previous != 0,
            value.can_go_next != 0,
            value.can_change_playback_position != 0,
    };
}

Session::Session(PodAuraMediaCommandCallback callback) : callback_(callback) {
    const HRESULT result = RoInitialize(RO_INIT_MULTITHREADED);
    ro_initialized_ = SUCCEEDED(result);
    if (FAILED(result) && result != RPC_E_CHANGED_MODE) winrt::check_hresult(result);
}

Session::~Session() {
    close();
    if (ro_initialized_) RoUninitialize();
}

bool Session::attach_window(HWND window, bool main_window, const Tooltips &tooltips) {
    if (window == nullptr || !IsWindow(window)) {
        set_last_error("The supplied HWND is not a valid top-level window");
        return false;
    }
    std::unique_lock lock(mutex_);
    if (!active_) return false;

    auto existing = windows_.find(window);
    if (existing != windows_.end()) {
        existing->second->tooltips = tooltips;
        update_taskbar(*existing->second);
        return true;
    }
    if (main_window && main_window_ != nullptr) {
        set_last_error("A main window is already attached to the Windows media session");
        return false;
    }

    auto registration = std::make_shared<WindowRegistration>();
    registration->window = window;
    registration->main_window = main_window;
    registration->tooltips = tooltips;
    registration->session = weak_from_this();
    if (!change_window_subclass(registration, true)) {
        const DWORD window_thread = GetWindowThreadProcessId(window, nullptr);
        set_last_error(
                "SetWindowSubclass failed with Win32 error " +
                std::to_string(GetLastError()) + ", currentThread=" +
                std::to_string(GetCurrentThreadId()) + ", windowThread=" +
                std::to_string(window_thread)
        );
        return false;
    }

    try {
        windows_.emplace(window, registration);
        if (main_window) {
            initialize_smtc(window);
            main_window_ = window;
        }
        ensure_taskbar();
        registration->refresh_requested = true;
        update_taskbar(*registration);
        if (has_media_) apply_smtc_locked();
        return true;
    } catch (...) {
        registration->active.store(false);
        try {
            hide_taskbar(*registration);
        } catch (...) {
        }
        if (main_window) {
            try {
                clear_smtc_locked();
            } catch (...) {
            }
            release_smtc_locked();
            if (main_window_ == window) main_window_ = nullptr;
        }
        windows_.erase(window);
        lock.unlock();
        change_window_subclass(registration, false);
        throw;
    }
}

bool Session::update_window(HWND window, const Tooltips &tooltips) {
    std::scoped_lock lock(mutex_);
    auto found = windows_.find(window);
    if (!active_ || found == windows_.end()) return false;
    found->second->tooltips = tooltips;
    update_taskbar(*found->second);
    return true;
}

bool Session::detach_window(HWND window) {
    std::shared_ptr<WindowRegistration> registration;
    {
        std::scoped_lock lock(mutex_);
        auto found = windows_.find(window);
        if (found == windows_.end()) return true;
        registration = found->second;
        registration->active.store(false);
        hide_taskbar(*registration);
        if (main_window_ == window) {
            clear_smtc_locked();
            release_smtc_locked();
            main_window_ = nullptr;
        }
        windows_.erase(found);
    }
    change_window_subclass(registration, false);
    return true;
}

bool Session::update(
        const PodAuraMediaNowPlayingInfo &info,
        const PodAuraMediaCommandAvailability &availability
) {
    std::scoped_lock lock(mutex_);
    if (!active_) return false;
    has_media_ = true;
    info_ = copy_info(info);
    availability_ = Availability::from_native(availability);
    for (auto &[_, window]: windows_) update_taskbar(*window);
    apply_smtc_locked();
    return true;
}

bool Session::clear() {
    std::scoped_lock lock(mutex_);
    if (!active_) return false;
    has_media_ = false;
    info_ = {};
    availability_ = {};
    artwork_id_.clear();
    artwork_reference_ = nullptr;
    for (auto &[_, window]: windows_) update_taskbar(*window);
    clear_smtc_locked();
    return true;
}

void Session::close() {
    std::vector<std::shared_ptr<WindowRegistration>> registrations;
    {
        std::scoped_lock lock(mutex_);
        if (!active_.exchange(false)) return;
        has_media_ = false;
        registrations.reserve(windows_.size());
        for (auto &[_, window]: windows_) {
            window->active.store(false);
            hide_taskbar(*window);
            registrations.push_back(window);
        }
        windows_.clear();
        clear_smtc_locked();
        release_smtc_locked();
        taskbar_ = nullptr;
        pending_commands_.clear();
        callback_ = nullptr;
    }
    for (const auto &registration: registrations) {
        change_window_subclass(registration, false);
    }
}

Session::Info Session::copy_info(const PodAuraMediaNowPlayingInfo &source) {
    Info result;
    result.title = from_utf8(source.title);
    result.artist = from_utf8(source.artist);
    result.album = from_utf8(source.album);
    result.has_duration = source.has_duration != 0 &&
                          std::isfinite(source.duration_seconds) &&
                          source.duration_seconds > 0.0;
    result.duration_seconds = result.has_duration ? source.duration_seconds : 0.0;
    result.has_elapsed_time = source.has_elapsed_time != 0 &&
                              std::isfinite(source.elapsed_seconds);
    result.elapsed_seconds = result.has_elapsed_time ? source.elapsed_seconds : 0.0;
    result.playback_rate = std::isfinite(source.playback_rate) ? source.playback_rate : 0.0;
    result.default_playback_rate = std::isfinite(source.default_playback_rate)
                                   ? source.default_playback_rate
                                   : 1.0;
    result.has_queue_index = source.has_queue_index != 0;
    result.queue_index = source.queue_index;
    result.has_queue_count = source.has_queue_count != 0;
    result.queue_count = source.queue_count;
    result.media_type = source.media_type;
    result.playback_state = source.playback_state;
    result.artwork_id = source.artwork_id == nullptr ? std::string() : source.artwork_id;
    if (source.artwork_bytes != nullptr && source.artwork_length > 0 &&
        source.artwork_length <= static_cast<int64_t>(64 * 1024 * 1024)) {
        result.artwork_bytes.assign(
                source.artwork_bytes,
                source.artwork_bytes + source.artwork_length
        );
    }
    return result;
}

bool Session::command_available(int32_t command) const {
    if (!has_media_) return false;
    switch (command) {
        case PODAURA_MEDIA_COMMAND_PLAY:
            return availability_.can_play;
        case PODAURA_MEDIA_COMMAND_PAUSE:
            return availability_.can_pause;
        case PODAURA_MEDIA_COMMAND_TOGGLE_PLAY_PAUSE:
            return availability_.can_toggle;
        case PODAURA_MEDIA_COMMAND_PREVIOUS:
            return availability_.can_previous;
        case PODAURA_MEDIA_COMMAND_NEXT:
            return availability_.can_next;
        case PODAURA_MEDIA_COMMAND_CHANGE_PLAYBACK_POSITION:
            return availability_.can_seek;
        default:
            return false;
    }
}

void Session::queue_command(int32_t command, double position_seconds) {
    std::scoped_lock lock(mutex_);
    if (!active_ || callback_ == nullptr || !command_available(command)) return;
    if (pending_commands_.size() >= 64) pending_commands_.pop_front();
    pending_commands_.emplace_back(command, position_seconds);
}

int32_t Session::dispatch_pending() {
    int32_t dispatched = 0;
    while (true) {
        PodAuraMediaCommandCallback callback;
        std::pair<int32_t, double> command;
        {
            std::scoped_lock lock(mutex_);
            refresh_taskbars_locked();
            if (!active_ || callback_ == nullptr || pending_commands_.empty()) {
                return dispatched;
            }
            callback = callback_;
            command = pending_commands_.front();
            pending_commands_.pop_front();
        }
        callback(command.first, command.second);
        ++dispatched;
    }
}

} // namespace podaura::windows_media
