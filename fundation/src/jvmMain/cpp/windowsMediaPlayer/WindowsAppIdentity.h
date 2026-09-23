#ifndef PODAURA_WINDOWS_APP_IDENTITY_H
#define PODAURA_WINDOWS_APP_IDENTITY_H

#include <filesystem>

namespace podaura::windows_media {

// Internal shared shortcut writer. The caller initializes COM/WinRT and catches
// exceptions. Serializes load/update/save across media and notification init;
// preserves existing properties, including the toast CLSID when enable_toasts
// is false. Only call for an unpackaged process with a validated executable.
void update_unpacked_start_menu_shortcut(
        const std::filesystem::path &executable,
        bool enable_toasts
);

} // namespace podaura::windows_media

#endif
