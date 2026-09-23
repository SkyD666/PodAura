#include "WindowsNotifications.h"
#include "WindowsMediaSession.h"
#include "WindowsAppIdentity.h"

#include <appmodel.h>
#include <roapi.h>

#include <winrt/Windows.Data.Xml.Dom.h>
#include <winrt/Windows.Foundation.h>
#include <winrt/Windows.UI.Notifications.h>

#include <exception>
#include <filesystem>
#include <string_view>

namespace {

using namespace winrt::Windows::Data::Xml::Dom;
using namespace winrt::Windows::UI::Notifications;

constexpr wchar_t kAppUserModelId[] = L"com.skyd.podaura";
constexpr wchar_t kApplicationName[] = L"PodAura";

std::mutex notification_mutex;
bool initialized = false;
bool packaged = false;
std::wstring package_family_name;

class ScopedRuntime final {
public:
    ScopedRuntime() {
        const HRESULT result = RoInitialize(RO_INIT_MULTITHREADED);
        initialized_ = SUCCEEDED(result);
        // A JVM/UI thread may already be STA. Use its existing apartment.
        if (FAILED(result) && result != RPC_E_CHANGED_MODE) {
            winrt::check_hresult(result);
        }
    }

    ~ScopedRuntime() {
        if (initialized_) RoUninitialize();
    }

    ScopedRuntime(const ScopedRuntime &) = delete;
    ScopedRuntime &operator=(const ScopedRuntime &) = delete;

private:
    bool initialized_ = false;
};

// Even formatting diagnostics can allocate/throw. Guard error reporting too so
// an allocation failure never escapes the C ABI (or terminates via noexcept).
void capture_notification_error() noexcept {
    try {
        try {
            throw;
        } catch (const winrt::hresult_error &error) {
            podaura::windows_media::capture_error(error);
        } catch (const std::exception &error) {
            podaura::windows_media::set_last_error(error.what());
        } catch (...) {
            podaura::windows_media::set_last_error("Unknown Windows notification error");
        }
    } catch (...) {
        OutputDebugStringW(L"PodAura: failed to record a Windows notification error\n");
    }
}

std::wstring from_utf8(const char *value) {
    if (value == nullptr) throw winrt::hresult_invalid_argument(L"Null notification string");
    const int length = MultiByteToWideChar(CP_UTF8, MB_ERR_INVALID_CHARS, value, -1, nullptr, 0);
    if (length == 0) winrt::throw_last_error();
    std::wstring result(static_cast<size_t>(length), L'\0');
    if (MultiByteToWideChar(CP_UTF8, MB_ERR_INVALID_CHARS, value, -1, result.data(), length) == 0) {
        winrt::throw_last_error();
    }
    result.pop_back();
    return result;
}

std::wstring current_package_family_name() {
    UINT32 length = 0;
    const LONG result = GetCurrentPackageFamilyName(&length, nullptr);
    if (result == APPMODEL_ERROR_NO_PACKAGE) return {};
    if (result != ERROR_INSUFFICIENT_BUFFER) {
        winrt::check_hresult(HRESULT_FROM_WIN32(result));
        throw winrt::hresult_error(E_UNEXPECTED, L"Could not size the package family name");
    }
    std::wstring family_name(length, L'\0');
    winrt::check_hresult(HRESULT_FROM_WIN32(GetCurrentPackageFamilyName(&length, family_name.data())));
    if (length <= 1 || length > family_name.size()) {
        throw winrt::hresult_error(E_UNEXPECTED, L"Invalid package family name");
    }
    family_name.resize(length - 1); // API length includes the terminating NUL.
    return family_name;
}

std::filesystem::path executable_path(const char *value) {
    std::filesystem::path path;
    if (value != nullptr && *value != '\0') {
        path = from_utf8(value);
    } else {
        std::wstring buffer(MAX_PATH, L'\0');
        for (;;) {
            const DWORD length = GetModuleFileNameW(nullptr, buffer.data(), static_cast<DWORD>(buffer.size()));
            if (length == 0) winrt::throw_last_error();
            if (length < buffer.size()) {
                buffer.resize(length);
                path = buffer;
                break;
            }
            buffer.resize(buffer.size() * 2);
        }
    }
    if (!path.is_absolute() || !std::filesystem::is_regular_file(path) ||
        _wcsicmp(path.filename().c_str(), L"PodAura.exe") != 0) {
        throw winrt::hresult_invalid_argument(L"Notifications require the installed PodAura.exe absolute path");
    }
    return path;
}

ToastNotifier create_notifier(bool with_package_identity) {
    // MSIX obtains its actual package AUMID and manifest display name/logo.
    // The unpackaged AUMID must never replace that package identity.
    return with_package_identity
            ? ToastNotificationManager::CreateToastNotifier()
            : ToastNotificationManager::CreateToastNotifier(kAppUserModelId);
}

void validate_activation_uri(std::wstring_view uri) {
    constexpr std::wstring_view prefix = L"podaura://notification/";
    if (!uri.starts_with(prefix) || uri.size() != prefix.size() + 36) {
        throw winrt::hresult_invalid_argument(L"Expected podaura://notification/<UUID>");
    }
    const auto uuid = uri.substr(prefix.size());
    for (size_t i = 0; i < uuid.size(); ++i) {
        const wchar_t c = uuid[i];
        const bool separator = i == 8 || i == 13 || i == 18 || i == 23;
        const bool hex = (c >= L'0' && c <= L'9') ||
                         (c >= L'a' && c <= L'f') || (c >= L'A' && c <= L'F');
        if (separator ? c != L'-' : !hex) {
            throw winrt::hresult_invalid_argument(L"Invalid notification activation UUID");
        }
    }
}

void append_text(const XmlDocument &document, const XmlElement &binding, const std::wstring &text) {
    auto element = document.CreateElement(L"text");
    element.AppendChild(document.CreateTextNode(text));
    binding.AppendChild(element);
}

} // namespace

int podaura_notifications_init(const char *appExecutable) noexcept {
    try {
        podaura::windows_media::clear_last_error();
        const std::lock_guard<std::mutex> lock(notification_mutex);
        initialized = false;
        ScopedRuntime runtime;
        auto family_name = current_package_family_name();
        const bool with_package_identity = !family_name.empty();
        if (!with_package_identity) {
            podaura::windows_media::update_unpacked_start_menu_shortcut(
                    executable_path(appExecutable), true
            );
        }
        // Create/dispose on this thread while its WinRT apartment is alive.
        // No apartment-bound WinRT objects are cached between JVM calls.
        const auto notifier = create_notifier(with_package_identity);
        (void)notifier.Setting();
        package_family_name = std::move(family_name);
        packaged = with_package_identity;
        initialized = true;
        return 1;
    } catch (...) {
        capture_notification_error();
        return 0;
    }
}

int podaura_notifications_send(
        const char *id, const char *title, const char *body, const char *activationUri
) noexcept {
    try {
        podaura::windows_media::clear_last_error();
        const std::lock_guard<std::mutex> lock(notification_mutex);
        if (!initialized) {
            throw winrt::hresult_error(E_UNEXPECTED, L"Call podaura_notifications_init before send");
        }
        const auto tag = from_utf8(id);
        const auto title_text = from_utf8(title);
        const auto body_text = from_utf8(body);
        const auto uri = from_utf8(activationUri);
        if (tag.empty() || tag.size() > 64 || (title_text.empty() && body_text.empty())) {
            throw winrt::hresult_invalid_argument(L"Invalid notification tag or empty content");
        }
        validate_activation_uri(uri);

        ScopedRuntime runtime;
        const auto notifier = create_notifier(packaged);
        if (notifier.Setting() != NotificationSetting::Enabled) {
            throw winrt::hresult_error(E_ACCESSDENIED, L"Windows notification settings block PodAura toasts");
        }

        XmlDocument document;
        auto toast_element = document.CreateElement(L"toast");
        toast_element.SetAttribute(L"activationType", L"protocol");
        toast_element.SetAttribute(L"launch", uri);
        if (packaged) {
            // Keep this package's clicks in this package even when the MSI or
            // another app is the default podaura: handler. Unpackaged installs
            // have no PFN and necessarily use normal protocol association.
            // Toolkit's XML serializer spells this with the "protocol" prefix:
            // https://github.com/CommunityToolkit/WindowsCommunityToolkit/blob/main/Microsoft.Toolkit.Uwp.Notifications/Toasts/Elements/Element_Toast.cs
            toast_element.SetAttribute(L"protocolActivationTargetApplicationPfn", package_family_name);
        }
        auto visual = document.CreateElement(L"visual");
        auto binding = document.CreateElement(L"binding");
        binding.SetAttribute(L"template", L"ToastGeneric");
        // DOM text nodes/attributes escape XML metacharacters; never interpolate
        // caller-supplied strings into an XML template.
        append_text(document, binding, title_text);
        append_text(document, binding, body_text);
        visual.AppendChild(binding);
        toast_element.AppendChild(visual);
        document.AppendChild(toast_element);

        ToastNotification toast(document);
        // Tag grew from 16 to 64 characters in Windows 10 1703 (15063).
        // A full UUID fits; PodAura's MSIX minimum is 17763. Earlier Windows 10
        // builds cannot accept UUID tags and will return failure through the ABI.
        // https://learn.microsoft.com/uwp/api/windows.ui.notifications.toastnotification.tag
        toast.Tag(tag);
        toast.Group(kApplicationName);
        // No Activated handler: the OS owns protocol dispatch and cold start.
        notifier.Show(toast);
        return 1;
    } catch (...) {
        capture_notification_error();
        return 0;
    }
}
