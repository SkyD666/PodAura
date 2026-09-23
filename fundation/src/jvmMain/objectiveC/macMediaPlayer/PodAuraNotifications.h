#ifndef PODAURA_NOTIFICATIONS_H
#define PODAURA_NOTIFICATIONS_H

#ifdef __cplusplus
extern "C" {
#endif

/* Called on a system callback thread. Copy the UTF-8 URI into the Kotlin
 * channel before returning: the pointer is valid only during this call.
 * Keep the callback/JNA object alive for the lifetime of the process; it must
 * return promptly and must not let exceptions cross the native boundary. */
typedef void (*PodAuraNotificationActivationCallback)(const char *activationUri);

/* Load the library and call this BEFORE AWT/Compose starts NSApplication.
 * Installs the delegate synchronously without starting AppKit or requesting
 * permission. Returns 1 on success, 0 for an unsupported bundle or failure.
 * Requires macOS 11+, the packaged com.skyd.podaura .app, and a non-NULL callback.
 * Repeated calls may update the callback; old callbacks must remain alive for
 * any invocation already in progress. No shutdown/unregister is provided.
 *
 * The system retains activationUri in each notification's userInfo. A cold
 * launch can deliver it only if this delegate is installed before launch
 * finishes. This bridge cannot recover a launch response consumed before init;
 * validate launcher timing with a retained notification after quitting the app.
 * See https://developer.apple.com/documentation/usernotifications/unusernotificationcenterdelegate
 */
__attribute__((visibility("default")))
int podaura_notifications_init(PodAuraNotificationActivationCallback callback);

/* Asynchronous; requests authorization only while status is notDetermined.
 * Must follow a successful init. Denial and errors are logged. */
__attribute__((visibility("default")))
void podaura_notifications_request_permission(void);

/* Copies all strings before returning. id must be nonempty UTF-8; title/body
 * may be NULL (empty), activationUri may be NULL/empty (no activation callback).
 * Returns 1 when accepted for asynchronous authorization and submission, NOT
 * proof of delivery. Returns 0 for invalid arguments or unavailable bridge.
 * Authorization denial and asynchronous submission failures are logged.
 * First send also requests permission if needed (including migrated installs).
 */
__attribute__((visibility("default")))
int podaura_notifications_send(const char *id, const char *title,
                               const char *body, const char *activationUri);

#ifdef __cplusplus
}
#endif

#endif
