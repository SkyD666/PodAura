#include "PodAuraWindowsMediaPlayer.h"
#include "WindowsMediaSession.h"

#include <winrt/base.h>

#include <cstdio>
#include <memory>
#include <string>
#include <utility>

namespace podaura {
    namespace windows_media {
        namespace {

            thread_local std::string
            last_error_message;

        } // namespace

        void clear_last_error() {
            last_error_message.clear();
        }

        void set_last_error(std::string message) {
            last_error_message = std::move(message);
        }

        const char *last_error() {
            return last_error_message.c_str();
        }

        void capture_error(const winrt::hresult_error &error) {
            const auto message = winrt::to_string(error.message());
            char code[16]{};
            snprintf(code, sizeof(code), "0x%08lX", static_cast<unsigned long>(error.code().value));
            last_error_message = std::string(code) + ": " + message;
        }

        void capture_unknown_error() {
            last_error_message = "Unknown native Windows media integration error";
        }

    }
} // namespace podaura::windows_media

namespace {

    using podaura::windows_media::Session;

    struct SessionHandle {
        std::shared_ptr <Session> session;
    };

    SessionHandle *as_handle(void *handle) {
        return static_cast<SessionHandle *>(handle);
    }

} // namespace

int32_t podaura_windows_media_player_api_version(void) {
    return PODAURA_WINDOWS_MEDIA_PLAYER_API_VERSION;
}

const char *podaura_windows_media_player_last_error(void) {
    return podaura::windows_media::last_error();
}

int32_t podaura_windows_media_player_ensure_app_identity(void) {
    podaura::windows_media::clear_last_error();
    return podaura::windows_media::ensure_unpacked_start_menu_shortcut() ? 1 : 0;
}

void *podaura_windows_media_session_create(PodAuraMediaCommandCallback callback) {
    if (callback == nullptr) return nullptr;
    try {
        return new SessionHandle{std::make_shared<Session>(callback)};
    } catch (...) {
        return nullptr;
    }
}

int32_t podaura_windows_media_session_attach_window(
        void *session,
        void *window_handle,
        int32_t is_main_window,
        const PodAuraTaskbarTooltips *tooltips
) {
    podaura::windows_media::clear_last_error();
    try {
        auto *handle = as_handle(session);
        return handle != nullptr && handle->session &&
               handle->session->attach_window(
                       static_cast<HWND>(window_handle),
                       is_main_window != 0,
                       podaura::windows_media::Tooltips::from_native(tooltips)
               ) ? 1 : 0;
    } catch (const winrt::hresult_error &error) {
        podaura::windows_media::capture_error(error);
        return 0;
    } catch (...) {
        podaura::windows_media::capture_unknown_error();
        return 0;
    }
}

int32_t podaura_windows_media_session_update_window(
        void *session,
        void *window_handle,
        const PodAuraTaskbarTooltips *tooltips
) {
    try {
        auto *handle = as_handle(session);
        return handle != nullptr && handle->session &&
               handle->session->update_window(
                       static_cast<HWND>(window_handle),
                       podaura::windows_media::Tooltips::from_native(tooltips)
               ) ? 1 : 0;
    } catch (...) {
        return 0;
    }
}

int32_t podaura_windows_media_session_detach_window(void *session, void *window_handle) {
    try {
        auto *handle = as_handle(session);
        return handle != nullptr && handle->session &&
               handle->session->detach_window(static_cast<HWND>(window_handle)) ? 1 : 0;
    } catch (...) {
        return 0;
    }
}

int32_t podaura_windows_media_session_update(
        void *session,
        const PodAuraMediaNowPlayingInfo *info,
        const PodAuraMediaCommandAvailability *availability
) {
    if (info == nullptr || availability == nullptr) return 0;
    try {
        auto *handle = as_handle(session);
        return handle != nullptr && handle->session &&
               handle->session->update(*info, *availability) ? 1 : 0;
    } catch (...) {
        return 0;
    }
}

int32_t podaura_windows_media_session_dispatch_pending(void *session) {
    try {
        auto *handle = as_handle(session);
        return handle != nullptr && handle->session
               ? handle->session->dispatch_pending()
               : 0;
    } catch (...) {
        return 0;
    }
}

int32_t podaura_windows_media_session_clear(void *session) {
    try {
        auto *handle = as_handle(session);
        return handle != nullptr && handle->session && handle->session->clear() ? 1 : 0;
    } catch (...) {
        return 0;
    }
}

int32_t podaura_windows_media_session_destroy(void *session) {
    auto *handle = as_handle(session);
    if (handle == nullptr) return 0;
    try {
        if (handle->session) handle->session->close();
        delete handle;
        return 1;
    } catch (...) {
        delete handle;
        return 0;
    }
}
