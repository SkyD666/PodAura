#ifndef PODAURA_WINDOWS_NOTIFICATIONS_H
#define PODAURA_WINDOWS_NOTIFICATIONS_H

#if defined(_WIN32)
#define PODAURA_NOTIFICATIONS_API __declspec(dllexport)
#else
#define PODAURA_NOTIFICATIONS_API
#endif

#ifdef __cplusplus
extern "C" {
#define PODAURA_NOTIFICATIONS_NOEXCEPT noexcept
#else
#define PODAURA_NOTIFICATIONS_NOEXCEPT
#endif

// All strings are NUL-terminated UTF-8. Returns 1 on success, 0 on failure.
// Failure details use podaura_windows_media_player_last_error(), on the same
// thread, immediately after a failed call (before another shim call). Copy the
// returned UTF-8 string before its thread-local backing storage changes.
// No C++ exceptions escape these functions.
//
// For MSI/unpackaged installs, appExecutable must be an absolute path to the
// installed PodAura.exe (not java.exe/javaw.exe). A null/empty value uses the
// current process executable. MSIX uses its manifest identity and ignores it.
// Call init before send; calls are serialized and may originate on JVM threads.
// Shortcut registration only happens during init; later media initialization
// preserves its toast CLSID through the shared identity writer.
PODAURA_NOTIFICATIONS_API int podaura_notifications_init(
        const char *appExecutable
) PODAURA_NOTIFICATIONS_NOEXCEPT;

// id: stable, nonempty notification tag, at most 64 UTF-16 code units (Windows
// 10 1703+). Reusing it replaces that notification in the "PodAura" group.
// title/body: non-null, valid XML text; either may be empty, but not both.
// activationUri: exactly podaura://notification/<UUID>, with a 36-character
// hyphenated UUID. The caller persists its article mapping BEFORE calling send.
//
// The caller/installer must register the podaura protocol (registry for MSI,
// manifest for MSIX) and handle its URI on startup. There is no in-process click
// callback: Windows launches the URI even after this JVM has exited.
// MSIX targets its own package family to bypass competing protocol handlers.
// MSI has no package family and uses the user's default protocol association.
// Success means Show accepted the toast, not that a banner was visible; Windows
// Focus Assist and subsequent asynchronous delivery failures can suppress it.
PODAURA_NOTIFICATIONS_API int podaura_notifications_send(
        const char *id,
        const char *title,
        const char *body,
        const char *activationUri
) PODAURA_NOTIFICATIONS_NOEXCEPT;

#ifdef __cplusplus
}
#endif

#undef PODAURA_NOTIFICATIONS_NOEXCEPT

#endif
