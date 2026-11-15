import SwiftUI
import ComposeApp
import FirebaseCore
import FirebaseMessaging
import UserNotifications

/// Firebase setup, APNs → FCM token, and notification presentation and taps.
/// The FCM token and tapped links go to Kotlin through `PushBridge` (composeApp/src/iosMain).
class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate, MessagingDelegate {
    func application(_ application: UIApplication,
                     didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil) -> Bool {
        if FirebaseApp.app() == nil {
            FirebaseApp.configure()
        }
        UNUserNotificationCenter.current().delegate = self
        Messaging.messaging().delegate = self
        PushBridge.shared.setDeleteTokenHandler {
            Messaging.messaging().deleteToken { error in
                if let error { print("FCM token delete failed: \(error)") }
            }
        }
        // Doesn't prompt; the alert permission is asked in onboarding and Settings.
        application.registerForRemoteNotifications()
        return true
    }

    func application(_ application: UIApplication,
                     didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        Messaging.messaging().apnsToken = deviceToken
    }

    func application(_ application: UIApplication,
                     didFailToRegisterForRemoteNotificationsWithError error: Error) {
        // Simulator without push support, or a build without the aps-environment entitlement.
        print("APNs registration failed: \(error)")
    }

    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        guard let fcmToken, !fcmToken.isEmpty else { return }
        PushBridge.shared.onToken(token: fcmToken)
    }

    // Without this, reminders and pushes that arrive while FitAI is open are silently dropped.
    func userNotificationCenter(_ center: UNUserNotificationCenter,
                                willPresent notification: UNNotification,
                                withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void) {
        completionHandler([.banner, .list, .sound])
    }

    // Tap on a local reminder or a server push: `link` in userInfo picks the screen.
    func userNotificationCenter(_ center: UNUserNotificationCenter,
                                didReceive response: UNNotificationResponse,
                                withCompletionHandler completionHandler: @escaping () -> Void) {
        let link = response.notification.request.content.userInfo["link"] as? String
        PushBridge.shared.onNotificationOpened(link: link)
        completionHandler()
    }
}

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
