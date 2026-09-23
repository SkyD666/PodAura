#include "WindowsMediaSession.h"
#include "WindowsAppIdentity.h"

#include <appmodel.h>
#include <propkey.h>
#include <shlobj.h>

#include <exception>
#include <filesystem>

namespace podaura::windows_media {
namespace {

constexpr wchar_t kAppUserModelId[] = L"com.skyd.podaura";
constexpr wchar_t kApplicationName[] = L"PodAura";

// Deliberately unregistered: protocol-only toasts remain in Notification Center
// after exit without a COM activator. Never use this CLSID for foreground toasts.
// https://learn.microsoft.com/en-au/answers/questions/1016/push-notification-in-uwp-after-covert
constexpr CLSID kToastActivator = {
        0x8296a918, 0x35cb, 0x4a8e, {0xb3, 0xf3, 0x4d, 0xca, 0x64, 0x23, 0xa3, 0x98}
};
std::mutex shortcut_mutex;

class ScopedComInitialization {
public:
    ScopedComInitialization() {
        const HRESULT result = CoInitializeEx(nullptr, COINIT_MULTITHREADED);
        initialized_ = SUCCEEDED(result);
        if (FAILED(result) && result != RPC_E_CHANGED_MODE) {
            winrt::check_hresult(result);
        }
    }

    ~ScopedComInitialization() {
        if (initialized_) CoUninitialize();
    }

private:
    bool initialized_ = false;
};

bool has_package_identity() {
    UINT32 package_name_length = 0;
    return GetCurrentPackageFullName(&package_name_length, nullptr) !=
           APPMODEL_ERROR_NO_PACKAGE;
}

std::filesystem::path current_executable_path() {
    std::wstring path(MAX_PATH, L'\0');
    while (true) {
        const DWORD length = GetModuleFileNameW(
                nullptr,
                path.data(),
                static_cast<DWORD>(path.size())
        );
        if (length == 0) return {};
        if (length < path.size() - 1) {
            path.resize(length);
            return std::filesystem::path(path);
        }
        path.resize(path.size() * 2);
    }
}

} // namespace

void update_unpacked_start_menu_shortcut(
        const std::filesystem::path &executable,
        bool enable_toasts
) {
    // https://learn.microsoft.com/windows/win32/shell/enable-desktop-toast-with-appusermodelid
    const std::lock_guard<std::mutex> lock(shortcut_mutex);
    PWSTR raw_programs = nullptr;
    winrt::check_hresult(SHGetKnownFolderPath(FOLDERID_Programs, KF_FLAG_CREATE, nullptr, &raw_programs));
    // Adopt before constructing a filesystem path, which can throw.
    const std::unique_ptr<wchar_t, decltype(&CoTaskMemFree)> programs(raw_programs, &CoTaskMemFree);
    const auto directory = std::filesystem::path(programs.get()) / kApplicationName;
    std::filesystem::create_directories(directory);
    const auto shortcut_path = directory / L"PodAura.lnk";

    winrt::com_ptr<IShellLinkW> shortcut;
    winrt::check_hresult(CoCreateInstance(
            CLSID_ShellLink, nullptr, CLSCTX_INPROC_SERVER,
            __uuidof(IShellLinkW), shortcut.put_void()
    ));
    auto file = shortcut.as<IPersistFile>();
    // Both initialization paths load the existing property store. In particular,
    // media initialization must not erase ToastActivatorCLSID, regardless of
    // which initializer runs first. The mutex also protects concurrent callers.
    if (std::filesystem::exists(shortcut_path)) {
        winrt::check_hresult(file->Load(shortcut_path.c_str(), STGM_READWRITE));
    }
    winrt::check_hresult(shortcut->SetPath(executable.c_str()));
    winrt::check_hresult(shortcut->SetWorkingDirectory(executable.parent_path().c_str()));
    winrt::check_hresult(shortcut->SetDescription(kApplicationName));
    winrt::check_hresult(shortcut->SetIconLocation(executable.c_str(), 0));

    auto properties = shortcut.as<IPropertyStore>();
    PROPVARIANT app_id{};
    app_id.vt = VT_LPWSTR;
    app_id.pwszVal = const_cast<PWSTR>(kAppUserModelId);
    winrt::check_hresult(properties->SetValue(PKEY_AppUserModel_ID, app_id));
    if (enable_toasts) {
        CLSID activator = kToastActivator;
        PROPVARIANT toast_activator{};
        toast_activator.vt = VT_CLSID;
        toast_activator.puuid = &activator;
        winrt::check_hresult(properties->SetValue(PKEY_AppUserModel_ToastActivatorCLSID, toast_activator));
    }
    winrt::check_hresult(properties->Commit());
    winrt::check_hresult(file->Save(shortcut_path.c_str(), TRUE));
    SHChangeNotify(SHCNE_ASSOCCHANGED, SHCNF_IDLIST, nullptr, nullptr);
}

bool ensure_unpacked_start_menu_shortcut() noexcept {
    try {
        if (has_package_identity()) return true;

        const auto executable = current_executable_path();
        if (executable.empty()) {
            set_last_error("Could not resolve the current PodAura executable path");
            return false;
        }
        const auto executable_name = executable.filename().wstring();
        if (_wcsicmp(executable_name.c_str(), L"java.exe") == 0 ||
            _wcsicmp(executable_name.c_str(), L"javaw.exe") == 0) {
            return true;
        }

        ScopedComInitialization com_initialization;
        update_unpacked_start_menu_shortcut(executable, false);
        return true;
    } catch (...) {
        // Error formatting may itself allocate. Preserve the noexcept ABI even
        // if recording the original failure runs out of memory.
        try {
            try {
                throw;
            } catch (const winrt::hresult_error &error) {
                capture_error(error);
            } catch (const std::exception &error) {
                set_last_error(error.what());
            } catch (...) {
                capture_unknown_error();
            }
        } catch (...) {
            OutputDebugStringW(L"PodAura: failed to record a Windows identity error\n");
        }
        return false;
    }
}

} // namespace podaura::windows_media
