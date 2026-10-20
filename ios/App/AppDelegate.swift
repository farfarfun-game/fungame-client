import UIKit

@main
final class AppDelegate: UIResponder, UIApplicationDelegate {
    var window: UIWindow?

    func application(_ application: UIApplication,
                     didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        let window = UIWindow(frame: UIScreen.main.bounds)
        window.rootViewController = WebViewController(rawAddress: Bundle.main.object(forInfoDictionaryKey: "FungameServerAddress") as? String ?? "")
        window.makeKeyAndVisible()
        self.window = window
        return true
    }
}
