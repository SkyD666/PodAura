#import "PodAuraNotifications.h"

#import <Foundation/Foundation.h>
#import <UserNotifications/UserNotifications.h>
#import <dispatch/dispatch.h>

static NSString *const PodAuraActivationURIKey = @"activationUri";
typedef void (^PodAuraAuthorizationCompletion)(BOOL authorized);

@interface PodAuraNotificationDelegate : NSObject <UNUserNotificationCenterDelegate>
@property(nonatomic, strong) UNUserNotificationCenter *center;
@property(nonatomic, assign) PodAuraNotificationActivationCallback callback;
@property(nonatomic, strong) dispatch_queue_t authorizationQueue;
// Accessed only on authorizationQueue; coalesces concurrent permission checks.
@property(nonatomic, strong) NSMutableArray<PodAuraAuthorizationCompletion> *authorizationWaiters;
- (void)authorize:(PodAuraAuthorizationCompletion)completion;
- (void)finishAuthorization:(BOOL)authorized;
@end

// UNUserNotificationCenter.delegate is weak. Retain ours until process exit.
// Access to this variable and initialization is protected by the class lock.
static PodAuraNotificationDelegate *PodAuraNotificationsDelegate;

static void PodAuraLogNotificationException(NSString *operation, NSException *exception) {
    NSLog(@"[PodAura Notifications] %@ failed: %@: %@", operation,
          exception.name, exception.reason);
}

@implementation PodAuraNotificationDelegate

- (instancetype)init {
    self = [super init];
    if (self != nil) {
        _authorizationQueue = dispatch_queue_create("com.skyd.podaura.notifications.authorization",
                                                     DISPATCH_QUEUE_SERIAL);
        _authorizationWaiters = [NSMutableArray array];
    }
    return self;
}

- (void)finishAuthorization:(BOOL)authorized {
    // Always called on authorizationQueue. Clear before invoking client blocks.
    NSArray<PodAuraAuthorizationCompletion> *waiters = [self.authorizationWaiters copy];
    [self.authorizationWaiters removeAllObjects];
    for (PodAuraAuthorizationCompletion completion in waiters) {
        completion(authorized);
    }
}

- (void)authorize:(PodAuraAuthorizationCompletion)completion {
    dispatch_async(self.authorizationQueue, ^{
        @autoreleasepool {
            [self.authorizationWaiters addObject:[completion copy]];
            if (self.authorizationWaiters.count > 1) return;
            @try {
                [self.center getNotificationSettingsWithCompletionHandler:^(UNNotificationSettings *settings) {
                    dispatch_async(self.authorizationQueue, ^{
                        @autoreleasepool {
                            if (settings.authorizationStatus == UNAuthorizationStatusNotDetermined) {
                                @try {
                                    [self.center requestAuthorizationWithOptions:UNAuthorizationOptionAlert |
                                                                               UNAuthorizationOptionSound
                                                              completionHandler:^(BOOL granted, NSError *error) {
                                        if (error != nil) {
                                            NSLog(@"[PodAura Notifications] Authorization failed: %@", error);
                                        } else if (!granted) {
                                            NSLog(@"[PodAura Notifications] Authorization denied.");
                                        }
                                        dispatch_async(self.authorizationQueue, ^{
                                            @autoreleasepool {
                                                [self finishAuthorization:granted && error == nil];
                                            }
                                        });
                                    }];
                                } @catch (NSException *exception) {
                                    PodAuraLogNotificationException(@"Authorization", exception);
                                    [self finishAuthorization:NO];
                                }
                            } else {
                                BOOL authorized = settings.authorizationStatus == UNAuthorizationStatusAuthorized ||
                                                  settings.authorizationStatus == UNAuthorizationStatusProvisional;
                                if (!authorized) {
                                    NSLog(@"[PodAura Notifications] Authorization denied or unavailable; status: %ld",
                                          (long)settings.authorizationStatus);
                                }
                                [self finishAuthorization:authorized];
                            }
                        }
                    });
                }];
            } @catch (NSException *exception) {
                PodAuraLogNotificationException(@"Reading notification settings", exception);
                [self finishAuthorization:NO];
            }
        }
    });
}

- (void)userNotificationCenter:(UNUserNotificationCenter *)center
      willPresentNotification:(UNNotification *)notification
        withCompletionHandler:(void (^)(UNNotificationPresentationOptions options))completionHandler {
    (void)center;
    (void)notification;
    completionHandler(UNNotificationPresentationOptionBanner | UNNotificationPresentationOptionList |
                      UNNotificationPresentationOptionSound);
}

- (void)userNotificationCenter:(UNUserNotificationCenter *)center
didReceiveNotificationResponse:(UNNotificationResponse *)response
        withCompletionHandler:(void (^)(void))completionHandler {
    (void)center;
    @autoreleasepool {
        @try {
            // A dismissal must not navigate. There are no custom actions.
            if (![response.actionIdentifier isEqualToString:UNNotificationDefaultActionIdentifier]) return;
            id uri = response.notification.request.content.userInfo[PodAuraActivationURIKey];
            if (![uri isKindOfClass:[NSString class]] || [uri length] == 0) return;
            PodAuraNotificationActivationCallback callback;
            @synchronized (self) {
                callback = self.callback;
            }
            if (callback != NULL) callback([uri UTF8String]);
        } @catch (NSException *exception) {
            PodAuraLogNotificationException(@"Handling activation", exception);
        } @finally {
            completionHandler();
        }
    }
}

@end

static PodAuraNotificationDelegate *PodAuraGetNotificationDelegate(void) {
    @synchronized ([PodAuraNotificationDelegate class]) {
        if (PodAuraNotificationsDelegate == nil) {
            NSLog(@"[PodAura Notifications] Bridge unavailable; initialize in the packaged app before AWT starts.");
        }
        return PodAuraNotificationsDelegate;
    }
}

int podaura_notifications_init(PodAuraNotificationActivationCallback callback) {
    @autoreleasepool {
        if (callback == NULL) {
            NSLog(@"[PodAura Notifications] Initialization requires an activation callback.");
            return 0;
        }
        @synchronized ([PodAuraNotificationDelegate class]) {
            @try {
                NSBundle *bundle = NSBundle.mainBundle;
                // Do NOT ask UserNotifications for its center in a Gradle/java
                // process: its unregistered main bundle can raise an exception.
                if (![bundle.bundleIdentifier isEqualToString:@"com.skyd.podaura"] ||
                    ![bundle.bundleURL.pathExtension.lowercaseString isEqualToString:@"app"] ||
                    ![[bundle objectForInfoDictionaryKey:@"CFBundlePackageType"] isEqual:@"APPL"]) {
                    NSLog(@"[PodAura Notifications] Unsupported main bundle (%@, %@); requires a .app with bundle identifier com.skyd.podaura.",
                          bundle.bundleIdentifier, bundle.bundleURL.path);
                    return 0;
                }
                if (@available(macOS 11.0, *)) {
                    PodAuraNotificationDelegate *delegate = PodAuraNotificationsDelegate;
                    if (delegate == nil) {
                        delegate = [[PodAuraNotificationDelegate alloc] init];
                        delegate.center = [UNUserNotificationCenter currentNotificationCenter];
                        if (delegate.center == nil) {
                            NSLog(@"[PodAura Notifications] Notification center unavailable.");
                            return 0;
                        }
                    }
                    @synchronized (delegate) {
                        delegate.callback = callback;
                    }
                    // Synchronous on the calling thread. Dispatching to the main
                    // queue here could miss launch or deadlock JVM/AWT startup.
                    // Never create NSApplication or replace its delegate.
                    delegate.center.delegate = delegate;
                    PodAuraNotificationsDelegate = delegate;
                    return 1;
                }
                NSLog(@"[PodAura Notifications] macOS 11.0 or later is required.");
            } @catch (NSException *exception) {
                // Also protects malformed/unregistered .app bundles.
                PodAuraLogNotificationException(@"Initialization", exception);
            }
            return 0;
        }
    }
}

void podaura_notifications_request_permission(void) {
    @autoreleasepool {
        @try {
            [PodAuraGetNotificationDelegate() authorize:^(BOOL authorized) { (void)authorized; }];
        } @catch (NSException *exception) {
            PodAuraLogNotificationException(@"Requesting permission", exception);
        }
    }
}

int podaura_notifications_send(const char *id, const char *title,
                               const char *body, const char *activationUri) {
    @autoreleasepool {
        @try {
            PodAuraNotificationDelegate *delegate = PodAuraGetNotificationDelegate();
            if (delegate == nil) return 0;
            NSString *identifier = id == NULL ? nil : [NSString stringWithUTF8String:id];
            NSString *titleString = title == NULL ? @"" : [NSString stringWithUTF8String:title];
            NSString *bodyString = body == NULL ? @"" : [NSString stringWithUTF8String:body];
            NSString *uri = activationUri == NULL ? @"" : [NSString stringWithUTF8String:activationUri];
            if (identifier.length == 0 || titleString == nil || bodyString == nil || uri == nil) {
                NSLog(@"[PodAura Notifications] Invalid UTF-8 or empty notification identifier.");
                return 0;
            }
            UNMutableNotificationContent *content = [[UNMutableNotificationContent alloc] init];
            content.title = titleString;
            content.body = bodyString;
            content.sound = UNNotificationSound.defaultSound;
            // Persist navigation data with the system notification, not in a
            // process-local id map: it must survive termination and relaunch.
            if (uri.length > 0) content.userInfo = @{PodAuraActivationURIKey: uri};
            UNNotificationRequest *request = [UNNotificationRequest requestWithIdentifier:identifier
                                                                                 content:content trigger:nil];
            [delegate authorize:^(BOOL authorized) {
                if (!authorized) return;
                @try {
                    [delegate.center addNotificationRequest:request withCompletionHandler:^(NSError *error) {
                        if (error != nil) {
                            NSLog(@"[PodAura Notifications] Sending notification %@ failed: %@", identifier, error);
                        }
                    }];
                } @catch (NSException *exception) {
                    PodAuraLogNotificationException(@"Sending notification", exception);
                }
            }];
            return 1;
        } @catch (NSException *exception) {
            PodAuraLogNotificationException(@"Preparing notification", exception);
            return 0;
        }
    }
}
