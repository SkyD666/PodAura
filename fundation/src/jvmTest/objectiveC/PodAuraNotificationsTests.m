// Standalone native lifecycle tests; run ./run-notification-tests.sh on macOS.
// Include the implementation to inspect/reset process-lifetime state without
// exporting test hooks in the shipping ABI. Do not compile it a second time.
// The system center accessor is replaced for the entire test run. No real
// permission requests, notifications, application launch, or AppKit are used.
#import "PodAuraNotifications.m"
#import <objc/runtime.h>
#include <assert.h>
#include <stdio.h>

@interface TestSettings : NSObject
@property(nonatomic) UNAuthorizationStatus authorizationStatus;
@end
@implementation TestSettings
@end

@interface TestNotification : NSObject
@property(nonatomic, strong) UNNotificationRequest *request;
@end
@implementation TestNotification
@end

@interface TestResponse : NSObject
@property(nonatomic, copy) NSString *actionIdentifier;
@property(nonatomic, strong) TestNotification *notification;
@end
@implementation TestResponse
@end

@interface TestCenter : NSObject
@property(nonatomic, weak) id<UNUserNotificationCenterDelegate> delegate;
@property(nonatomic, strong) TestSettings *settings;
@property(nonatomic, strong) NSMutableArray<UNNotificationRequest *> *sent;
@property(nonatomic, copy) void (^pendingSettings)(UNNotificationSettings *);
@property(nonatomic, copy) void (^onDelegateInstalled)(id<UNUserNotificationCenterDelegate>);
@property(nonatomic) NSUInteger settingsCalls;
@property(nonatomic) NSUInteger authorizationCalls;
@property(nonatomic) NSUInteger submissionCalls;
@property(nonatomic) BOOL holdSettings;
@property(nonatomic) BOOL granted;
@property(nonatomic) BOOL throwSettings;
@property(nonatomic) BOOL throwAuthorization;
@property(nonatomic) BOOL throwSubmission;
@property(nonatomic, strong) NSError *authorizationError;
@property(nonatomic, strong) NSError *submissionError;
@end

@implementation TestCenter
@synthesize delegate = _delegate;
- (void)setDelegate:(id<UNUserNotificationCenterDelegate>)delegate {
    _delegate = delegate;
    if (self.onDelegateInstalled != nil) self.onDelegateInstalled(delegate);
}
- (void)getNotificationSettingsWithCompletionHandler:(void (^)(UNNotificationSettings *))completion {
    self.settingsCalls++;
    if (self.throwSettings) [NSException raise:@"TestException" format:@"settings failed"];
    if (self.holdSettings) self.pendingSettings = completion;
    else completion((id)self.settings);
}
- (void)requestAuthorizationWithOptions:(UNAuthorizationOptions)options
                     completionHandler:(void (^)(BOOL, NSError *))completion {
    assert(options == (UNAuthorizationOptionAlert | UNAuthorizationOptionSound));
    self.authorizationCalls++;
    if (self.throwAuthorization) [NSException raise:@"TestException" format:@"authorization failed"];
    self.settings.authorizationStatus = self.granted ? UNAuthorizationStatusAuthorized : UNAuthorizationStatusDenied;
    completion(self.granted, self.authorizationError);
}
- (void)addNotificationRequest:(UNNotificationRequest *)request
         withCompletionHandler:(void (^)(NSError *))completion {
    self.submissionCalls++;
    if (self.throwSubmission) [NSException raise:@"TestException" format:@"submission failed"];
    if (self.submissionError == nil) [self.sent addObject:request];
    completion(self.submissionError);
}
@end

@interface TestBundle : NSObject
@property(nonatomic, copy) NSString *bundleIdentifier;
@property(nonatomic, strong) NSURL *bundleURL;
@property(nonatomic, copy) NSString *packageType;
@end
@implementation TestBundle
- (id)objectForInfoDictionaryKey:(NSString *)key {
    return [key isEqualToString:@"CFBundlePackageType"] ? self.packageType : nil;
}
@end

static TestCenter *testCenter;
static TestBundle *testBundle;
static NSUInteger centerAccesses;
static BOOL throwCenter;
static NSMutableArray<NSString *> *receivedURIs;

static id FakeCurrentCenter(id object, SEL selector) {
    (void)object; (void)selector;
    centerAccesses++;
    if (throwCenter) [NSException raise:@"TestException" format:@"unregistered bundle"];
    return testCenter;
}
static id FakeMainBundle(id object, SEL selector) {
    (void)object; (void)selector;
    return testBundle;
}
static void Activated(const char *uri) {
    [receivedURIs addObject:[NSString stringWithUTF8String:uri]];
}
static void AlternateActivated(const char *uri) {
    [receivedURIs addObject:[@"alternate:" stringByAppendingString:[NSString stringWithUTF8String:uri]]];
}
static void ThrowingActivation(const char *uri) {
    (void)uri;
    [NSException raise:@"TestException" format:@"callback failed"];
}

static void Reset(void) {
    PodAuraNotificationsDelegate = nil;
    testCenter = [TestCenter new];
    testCenter.settings = [TestSettings new];
    testCenter.settings.authorizationStatus = UNAuthorizationStatusNotDetermined;
    testCenter.sent = [NSMutableArray array];
    testCenter.granted = YES;
    testBundle = [TestBundle new];
    testBundle.bundleIdentifier = @"com.skyd.podaura";
    testBundle.bundleURL = [NSURL fileURLWithPath:@"/test/PodAura.app"];
    testBundle.packageType = @"APPL";
    centerAccesses = 0;
    throwCenter = NO;
    receivedURIs = [NSMutableArray array];
}

static int Initialize(PodAuraNotificationActivationCallback callback) {
    Method method = class_getClassMethod([NSBundle class], @selector(mainBundle));
    IMP original = method_setImplementation(method, (IMP)FakeMainBundle);
    @try {
        return podaura_notifications_init(callback);
    } @finally {
        method_setImplementation(method, original);
    }
}

static void Drain(void) {
    // Fake API completions run inline. The bridge adds at most three queue
    // hops (check settings, request authorization, finish). Bound all waits so
    // a broken queue/lock fails instead of hanging the test runner.
    for (int i = 0; i < 4; i++) {
        dispatch_semaphore_t barrier = dispatch_semaphore_create(0);
        dispatch_async(PodAuraNotificationsDelegate.authorizationQueue, ^{ dispatch_semaphore_signal(barrier); });
        assert(dispatch_semaphore_wait(barrier, dispatch_time(DISPATCH_TIME_NOW, 5 * NSEC_PER_SEC)) == 0);
    }
}

static void Respond(id<UNUserNotificationCenterDelegate> delegate, UNNotificationRequest *request,
                    NSString *action) {
    TestResponse *response = [TestResponse new];
    response.notification = [TestNotification new];
    response.notification.request = request;
    response.actionIdentifier = action;
    __block NSUInteger completions = 0;
    [delegate userNotificationCenter:(id)testCenter didReceiveNotificationResponse:(id)response
              withCompletionHandler:^{ completions++; }];
    assert(completions == 1);
}

static UNNotificationRequest *RetainedRequest(id uri) {
    UNMutableNotificationContent *content = [UNMutableNotificationContent new];
    if (uri != nil) content.userInfo = @{@"activationUri": uri};
    return [UNNotificationRequest requestWithIdentifier:@"from-an-earlier-process" content:content trigger:nil];
}

static void TestUnsupportedBundles(void) {
    Reset();
    assert(podaura_notifications_init(Activated) == 0); // Actual plain executable.
    assert(Initialize(NULL) == 0);
    testBundle.bundleIdentifier = @"org.example.other";
    assert(Initialize(Activated) == 0);
    testBundle.bundleIdentifier = @"com.skyd.podaura";
    testBundle.bundleURL = [NSURL fileURLWithPath:@"/test/java"];
    assert(Initialize(Activated) == 0);
    testBundle.bundleURL = [NSURL fileURLWithPath:@"/test/PodAura.app"];
    testBundle.packageType = @"BNDL";
    assert(Initialize(Activated) == 0);
    assert(centerAccesses == 0);
    podaura_notifications_request_permission();
    assert(podaura_notifications_send("uninitialized", NULL, NULL, NULL) == 0);
    testBundle.packageType = @"APPL";
    throwCenter = YES;
    assert(Initialize(Activated) == 0);
    assert(PodAuraNotificationsDelegate == nil);
    throwCenter = NO;
    assert(Initialize(Activated) == 1); // Failed initialization remains retryable.
    puts("PASS unsupported bundles, exception containment, initialization retry");
}

static void TestEarlyDelegateLifetime(void) {
    Reset();
    testCenter.onDelegateInstalled = ^(id<UNUserNotificationCenterDelegate> delegate) {
        Respond(delegate, RetainedRequest(@"podaura://retained-before-ui"), UNNotificationDefaultActionIdentifier);
    };
    @autoreleasepool {
        assert(Initialize(Activated) == 1);
    }
    assert([receivedURIs isEqualToArray:@[@"podaura://retained-before-ui"]]);
    assert(testCenter.delegate != nil); // Fake center's delegate is weak.
    assert(testCenter.delegate == PodAuraNotificationsDelegate);
    assert(testCenter.authorizationCalls == 0 && testCenter.settingsCalls == 0);
    testCenter.onDelegateInstalled = nil;
    id originalDelegate = testCenter.delegate;
    assert(Initialize(AlternateActivated) == 1);
    assert(centerAccesses == 1 && testCenter.delegate == originalDelegate);
    Respond(testCenter.delegate, RetainedRequest(@"podaura://new-callback"), UNNotificationDefaultActionIdentifier);
    assert([receivedURIs.lastObject isEqualToString:@"alternate:podaura://new-callback"]);
    puts("PASS synchronous early response, strong delegate lifetime, callback replacement");
}

static void TestFirstSendAuthorization(void) {
    Reset();
    assert(Initialize(Activated) == 1);
    testCenter.holdSettings = YES;
    char uri[] = "podaura://episode/123";
    char title[] = "Title";
    assert(podaura_notifications_send("one", title, "Body", uri) == 1);
    uri[0] = 'X'; title[0] = 'X';
    assert(podaura_notifications_send("two", NULL, NULL, NULL) == 1);
    podaura_notifications_request_permission();
    Drain();
    assert(testCenter.settingsCalls == 1 && testCenter.authorizationCalls == 0);
    assert(PodAuraNotificationsDelegate.authorizationWaiters.count == 3);
    assert(testCenter.sent.count == 0);
    testCenter.holdSettings = NO;
    testCenter.pendingSettings((id)testCenter.settings);
    testCenter.pendingSettings = nil;
    Drain();
    assert(testCenter.authorizationCalls == 1 && testCenter.sent.count == 2);
    UNNotificationRequest *request = testCenter.sent[0];
    assert([request.content.userInfo[@"activationUri"] isEqualToString:@"podaura://episode/123"]);
    assert([request.content.title isEqualToString:@"Title"]);
    assert([request.content.body isEqualToString:@"Body"] && request.trigger == nil);
    assert(testCenter.sent[1].content.userInfo.count == 0);
    assert(PodAuraNotificationsDelegate.authorizationWaiters.count == 0);
    assert(podaura_notifications_send("authorized", "title", "body", "uri") == 1);
    Drain();
    assert(testCenter.sent.count == 3 && testCenter.authorizationCalls == 1);
    testCenter.settings.authorizationStatus = UNAuthorizationStatusProvisional;
    assert(podaura_notifications_send("provisional", "title", "body", "uri") == 1);
    Drain();
    assert(testCenter.sent.count == 4 && testCenter.authorizationCalls == 1);
    puts("PASS first sends share authorization, copy input, persist URI, accept authorized/provisional");
}

static void TestDenialDoesNotPromptAgain(void) {
    Reset();
    assert(Initialize(Activated) == 1);
    testCenter.granted = NO;
    assert(podaura_notifications_send("declined", "title", "body", "uri") == 1);
    Drain();
    assert(testCenter.authorizationCalls == 1 && testCenter.sent.count == 0);
    for (int i = 0; i < 3; i++) {
        podaura_notifications_request_permission();
        assert(podaura_notifications_send("denied", "title", "body", "uri") == 1);
        Drain();
    }
    assert(testCenter.authorizationCalls == 1 && testCenter.submissionCalls == 0);
    assert(PodAuraNotificationsDelegate.authorizationWaiters.count == 0);
    puts("PASS denial suppresses sends and never requests authorization again");
}

static void TestFailuresReleasePendingWork(void) {
    Reset();
    assert(Initialize(Activated) == 1);
    testCenter.throwSettings = YES;
    podaura_notifications_request_permission(); Drain();
    assert(PodAuraNotificationsDelegate.authorizationWaiters.count == 0);
    testCenter.throwSettings = NO;
    testCenter.throwAuthorization = YES;
    assert(podaura_notifications_send("auth-throws", NULL, NULL, NULL) == 1); Drain();
    assert(PodAuraNotificationsDelegate.authorizationWaiters.count == 0);
    testCenter.throwAuthorization = NO;
    testCenter.authorizationError = [NSError errorWithDomain:@"Test" code:1 userInfo:nil];
    assert(podaura_notifications_send("auth-error", NULL, NULL, NULL) == 1); Drain();
    assert(testCenter.submissionCalls == 0 && PodAuraNotificationsDelegate.authorizationWaiters.count == 0);
    testCenter.authorizationError = nil;
    testCenter.throwSubmission = YES;
    assert(podaura_notifications_send("send-throws", NULL, NULL, NULL) == 1); Drain();
    testCenter.throwSubmission = NO;
    testCenter.submissionError = [NSError errorWithDomain:@"Test" code:2 userInfo:nil];
    assert(podaura_notifications_send("send-error", NULL, NULL, NULL) == 1); Drain();
    assert(testCenter.sent.count == 0 && PodAuraNotificationsDelegate.authorizationWaiters.count == 0);
    testCenter.submissionError = nil;
    assert(podaura_notifications_send("recovery", NULL, NULL, NULL) == 1); Drain();
    assert(testCenter.sent.count == 1);
    puts("PASS settings/authorization/submission failures clear pending work and allow recovery");
}

static void TestActivationAndForeground(void) {
    Reset();
    assert(Initialize(Activated) == 1);
    // This request was never sent in this process: dispatch must use its own
    // persisted URI rather than an id -> URI map or the most recent send.
    Respond(testCenter.delegate, RetainedRequest(@"podaura://own-uri/中文"), UNNotificationDefaultActionIdentifier);
    assert([receivedURIs isEqualToArray:@[@"podaura://own-uri/中文"]]);
    Respond(testCenter.delegate, RetainedRequest(@"podaura://dismissed"), UNNotificationDismissActionIdentifier);
    Respond(testCenter.delegate, RetainedRequest(@"podaura://custom"), @"unknown-action");
    Respond(testCenter.delegate, RetainedRequest(nil), UNNotificationDefaultActionIdentifier);
    Respond(testCenter.delegate, RetainedRequest(@""), UNNotificationDefaultActionIdentifier);
    Respond(testCenter.delegate, RetainedRequest(@42), UNNotificationDefaultActionIdentifier);
    assert(receivedURIs.count == 1);
    assert(Initialize(ThrowingActivation) == 1);
    Respond(testCenter.delegate, RetainedRequest(@"podaura://exception"), UNNotificationDefaultActionIdentifier);
    __block NSUInteger completions = 0;
    __block UNNotificationPresentationOptions options = 0;
    TestNotification *notification = [TestNotification new];
    notification.request = RetainedRequest(nil);
    [testCenter.delegate userNotificationCenter:(id)testCenter willPresentNotification:(id)notification
                         withCompletionHandler:^(UNNotificationPresentationOptions value) {
        completions++; options = value;
    }];
    assert(completions == 1);
    assert(options == (UNNotificationPresentationOptionBanner | UNNotificationPresentationOptionList |
                       UNNotificationPresentationOptionSound));
    assert(testCenter.authorizationCalls == 0 && testCenter.submissionCalls == 0);
    puts("PASS response-owned UTF-8 URI, ignored actions/payloads, completion on exception, foreground options");
}

static void TestInvalidArguments(void) {
    Reset();
    assert(Initialize(Activated) == 1);
    const char invalid[] = {(char)0xff, 0};
    assert(podaura_notifications_send(NULL, NULL, NULL, NULL) == 0);
    assert(podaura_notifications_send("", NULL, NULL, NULL) == 0);
    assert(podaura_notifications_send(invalid, NULL, NULL, NULL) == 0);
    assert(podaura_notifications_send("id", invalid, NULL, NULL) == 0);
    assert(podaura_notifications_send("id", NULL, invalid, NULL) == 0);
    assert(podaura_notifications_send("id", NULL, NULL, invalid) == 0);
    Drain();
    assert(testCenter.settingsCalls == 0 && testCenter.submissionCalls == 0);
    puts("PASS invalid identifiers/UTF-8 rejected before authorization");
}

int main(void) {
    @autoreleasepool {
        Method method = class_getClassMethod([UNUserNotificationCenter class], @selector(currentNotificationCenter));
        IMP original = method_setImplementation(method, (IMP)FakeCurrentCenter);
        @try {
            TestUnsupportedBundles();
            TestEarlyDelegateLifetime();
            TestFirstSendAuthorization();
            TestDenialDoesNotPromptAgain();
            TestFailuresReleasePendingWork();
            TestActivationAndForeground();
            TestInvalidArguments();
            puts("All 7 native notification tests passed (fake center; no system permissions or notifications).");
        } @finally {
            PodAuraNotificationsDelegate = nil;
            method_setImplementation(method, original);
        }
    }
    return 0;
}
