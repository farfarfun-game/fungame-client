import UIKit
import WebKit
import SafariServices
import FungameShellCore

/// WebView 直接加载配置的服务器地址;不注入 bridge、不做预热。
final class WebViewController: UIViewController, WKNavigationDelegate, WKUIDelegate {
    private let policy: NavigationPolicy?
    private let configError: String?
    private var webView: WKWebView!
    private let fallback = UIStackView()
    private let fallbackLabel = UILabel()

    init(rawAddress: String) {
        switch ServerAddress.parse(rawAddress) {
        case .success(let s): policy = NavigationPolicy(server: s); configError = nil
        case .failure: policy = nil; configError = "服务器地址配置无效:\"\(rawAddress)\"\n请检查 FUNGAME_SERVER_ADDRESS。"
        }
        super.init(nibName: nil, bundle: nil)
    }
    required init?(coder: NSCoder) { fatalError("init(coder:) is not supported") }

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .systemBackground

        let config = WKWebViewConfiguration()
        config.allowsInlineMediaPlayback = true
        config.mediaTypesRequiringUserActionForPlayback = []
        webView = WKWebView(frame: view.bounds, configuration: config)
        webView.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        webView.navigationDelegate = self
        webView.uiDelegate = self
        webView.allowsBackForwardNavigationGestures = true
        view.addSubview(webView)

        fallbackLabel.numberOfLines = 0
        fallbackLabel.textAlignment = .center
        let retry = UIButton(type: .system)
        retry.setTitle("重试", for: .normal)
        retry.addTarget(self, action: #selector(load), for: .touchUpInside)
        fallback.axis = .vertical
        fallback.spacing = 16
        fallback.alignment = .center
        fallback.addArrangedSubview(fallbackLabel)
        fallback.addArrangedSubview(retry)
        fallback.isHidden = true
        fallback.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(fallback)
        NSLayoutConstraint.activate([
            fallback.centerYAnchor.constraint(equalTo: view.centerYAnchor),
            fallback.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 24),
            fallback.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -24),
        ])
        load()
    }

    @objc private func load() {
        guard let policy else { showFallback(configError ?? "配置无效", canRetry: false); return }
        fallback.isHidden = true
        webView.isHidden = false
        webView.load(URLRequest(url: policy.server.url))
    }

    private func showFallback(_ message: String, canRetry: Bool = true) {
        fallbackLabel.text = message
        fallback.arrangedSubviews.last?.isHidden = !canRetry
        fallback.isHidden = false
        webView.isHidden = true
    }

    // MARK: 导航限制

    func webView(_ webView: WKWebView, decidePolicyFor action: WKNavigationAction,
                 decisionHandler: @escaping (WKNavigationActionPolicy) -> Void) {
        let isMain = action.targetFrame?.isMainFrame ?? true
        switch policy?.decide(url: action.request.url, isMainFrame: isMain) ?? .block {
        case .allow: decisionHandler(.allow)
        case .block: decisionHandler(.cancel)
        case .openExternal:
            decisionHandler(.cancel)
            if let url = action.request.url { openExternally(url) }
        }
    }

    /// target=_blank / window.open:同源则在当前 WebView 加载,否则走外部。
    func webView(_ webView: WKWebView, createWebViewWith configuration: WKWebViewConfiguration,
                 for action: WKNavigationAction, windowFeatures: WKWindowFeatures) -> WKWebView? {
        guard let url = action.request.url else { return nil }
        switch policy?.decide(url: url, isMainFrame: true) ?? .block {
        case .allow: webView.load(action.request)
        case .openExternal: openExternally(url)
        case .block: break
        }
        return nil
    }

    private func openExternally(_ url: URL) {
        if url.scheme == "http" || url.scheme == "https" {
            present(SFSafariViewController(url: url), animated: true)
        } else {
            UIApplication.shared.open(url)
        }
    }

    // MARK: 失败与崩溃兜底

    func webView(_ webView: WKWebView, didFailProvisionalNavigation navigation: WKNavigation!, withError error: Error) {
        handleLoadError(error)
    }
    func webView(_ webView: WKWebView, didFail navigation: WKNavigation!, withError error: Error) {
        handleLoadError(error)
    }
    private func handleLoadError(_ error: Error) {
        let code = (error as NSError).code
        if code == NSURLErrorCancelled { return }  // 被导航策略取消,不是故障
        showFallback("无法连接到服务器\n\(error.localizedDescription)")
    }

    func webViewWebContentProcessDidTerminate(_ webView: WKWebView) {
        load()  // WebContent 进程被系统回收/崩溃后重载
    }
}
