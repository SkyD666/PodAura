#include "WindowsMediaSession.h"

#include <appmodel.h>
#include <propkey.h>
#include <shlobj.h>

#include <exception>
#include <filesystem>
#include <system_error>

namespace podaura::windows_media {
namespace {

constexpr wchar_t kAppUserModelId[] = L"com.skyd.podaura";
constexpr wchar_t kApplicationName[] = L"PodAura";

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
        PWSTR programs_path_value = nullptr;
        winrt::check_hresult(SHGetKnownFolderPath(
                FOLDERID_Programs,
                KF_FLAG_CREATE,
                nullptr,
                &programs_path_value
        ));
        const std::filesystem::path programs_path(programs_path_value);
        CoTaskMemFree(programs_path_value);

        const auto application_directory = programs_path / kApplicationName;
        std::error_code directory_error;
        std::filesystem::create_directories(application_directory, directory_error);
        if (directory_error) {
            set_last_error("Could not create the PodAura Start Menu directory: " +
                           directory_error.message());
            return false;
        }

        winrt::com_ptr<IShellLinkW> shortcut;
        winrt::check_hresult(CoCreateInstance(
                CLSID_ShellLink,
                nullptr,
                CLSCTX_INPROC_SERVER,
                __uuidof(IShellLinkW),
                shortcut.put_void()
        ));
        winrt::check_hresult(shortcut->SetPath(executable.c_str()));
        winrt::check_hresult(shortcut->SetWorkingDirectory(executable.parent_path().c_str()));
        winrt::check_hresult(shortcut->SetDescription(kApplicationName));
        winrt::check_hresult(shortcut->SetIconLocation(executable.c_str(), 0));

        auto properties = shortcut.as<IPropertyStore>();
        PROPVARIANT app_id{};
        app_id.vt = VT_LPWSTR;
        app_id.pwszVal = const_cast<PWSTR>(kAppUserModelId);
        winrt::check_hresult(properties->SetValue(PKEY_AppUserModel_ID, app_id));
        winrt::check_hresult(properties->Commit());

        const auto shortcut_path = application_directory / L"PodAura.lnk";
        auto persist_file = shortcut.as<IPersistFile>();
        winrt::check_hresult(persist_file->Save(shortcut_path.c_str(), TRUE));
        SHChangeNotify(SHCNE_ASSOCCHANGED, SHCNF_IDLIST, nullptr, nullptr);
        return true;
    } catch (const winrt::hresult_error &error) {
        capture_error(error);
        return false;
    } catch (const std::exception &error) {
        set_last_error(error.what());
        return false;
    } catch (...) {
        capture_unknown_error();
        return false;
    }
}

} // namespace podaura::windows_media
