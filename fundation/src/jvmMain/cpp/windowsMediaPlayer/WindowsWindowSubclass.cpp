#include "WindowsMediaSession.h"

#include <commctrl.h>

#include <memory>
#include <new>

namespace podaura::windows_media {
namespace {

constexpr UINT_PTR kWindowSubclassId = 0x504F44415552414ULL;

struct SubclassChange {
    std::shared_ptr<WindowRegistration> registration;
    bool install;
    bool succeeded = false;
};

UINT subclass_control_message() {
    static const UINT message = RegisterWindowMessageW(L"PodAuraMediaSubclassControl");
    return message;
}

void apply_subclass_change(SubclassChange &change) {
    auto &registration = *change.registration;
    if (change.install) {
        if (registration.subclass_owner != nullptr) {
            change.succeeded = true;
            return;
        }

        auto *owner = new(std::nothrow)
                std::shared_ptr<WindowRegistration>(change.registration);
        if (owner == nullptr) return;
        const BOOL installed = SetWindowSubclass(
                registration.window,
                &Session::subclass_proc,
                kWindowSubclassId,
                reinterpret_cast<DWORD_PTR>(owner)
        );
        if (installed != FALSE) {
            registration.subclass_owner = owner;
            change.succeeded = true;
        } else {
            delete owner;
        }
        return;
    }

    auto *owner = registration.subclass_owner;
    if (owner == nullptr) {
        change.succeeded = true;
        return;
    }
    if (RemoveWindowSubclass(
            registration.window,
            &Session::subclass_proc,
            kWindowSubclassId
    ) != FALSE) {
        registration.subclass_owner = nullptr;
        delete owner;
        change.succeeded = true;
    }
}

LRESULT CALLBACK subclass_control_hook(int code, WPARAM w_param, LPARAM l_param) {
    if (code >= 0) {
        const auto *message = reinterpret_cast<CWPSTRUCT *>(l_param);
        if (message != nullptr && message->message == subclass_control_message()) {
            auto *change = reinterpret_cast<SubclassChange *>(message->wParam);
            if (change != nullptr && change->registration &&
                change->registration->window == message->hwnd) {
                apply_subclass_change(*change);
            }
        }
    }
    return CallNextHookEx(nullptr, code, w_param, l_param);
}

} // namespace

LRESULT CALLBACK Session::subclass_proc(
        HWND window,
        UINT message,
        WPARAM w_param,
        LPARAM l_param,
        UINT_PTR,
        DWORD_PTR reference
) {
    auto *owner = reinterpret_cast<std::shared_ptr<WindowRegistration> *>(reference);
    const auto registration = owner == nullptr
                              ? std::shared_ptr<WindowRegistration>()
                              : *owner;
    const auto session = registration ? registration->session.lock() : nullptr;
    if (session) {
        try {
            session->on_window_message(*registration, message, w_param);
        } catch (...) {
            // Never unwind through a native Windows window procedure.
        }
    }

    if (message == WM_NCDESTROY && registration) {
        RemoveWindowSubclass(window, &Session::subclass_proc, kWindowSubclassId);
        if (registration->subclass_owner == owner) {
            registration->subclass_owner = nullptr;
            delete owner;
        }
        if (session) {
            try {
                session->detach_window(window);
            } catch (...) {
            }
        }
    }

    if (registration && message == WM_COMMAND && HIWORD(w_param) == THBN_CLICKED) {
        // AWT cannot safely interpret taskbar thumbnail command notifications.
        return 0;
    }
    return DefSubclassProc(window, message, w_param, l_param);
}

bool change_window_subclass(
        const std::shared_ptr<WindowRegistration> &registration,
        bool install
) {
    if (!registration) return false;
    const DWORD window_thread = GetWindowThreadProcessId(registration->window, nullptr);
    if (window_thread == 0) return false;

    SubclassChange change{registration, install};
    if (window_thread == GetCurrentThreadId()) {
        apply_subclass_change(change);
        return change.succeeded;
    }

    HHOOK hook = SetWindowsHookExW(
            WH_CALLWNDPROC,
            &subclass_control_hook,
            nullptr,
            window_thread
    );
    if (hook == nullptr) return false;
    SendMessageW(
            registration->window,
            subclass_control_message(),
            reinterpret_cast<WPARAM>(&change),
            0
    );
    UnhookWindowsHookEx(hook);
    return change.succeeded;
}

} // namespace podaura::windows_media
