import Foundation

public enum NavigationDecision: Equatable {
    /// 在 WebView 内加载。
    case allow
    /// 交给系统(SFSafariViewController / 系统打开)。
    case openExternal
    /// 直接拒绝。
    case block
}

/// 导航限制在配置的主机(协议 + 主机 + 端口同源);其余 http(s)/mailto/tel 走外部,其余协议拒绝。
public struct NavigationPolicy {
    public let server: ServerAddress

    public init(server: ServerAddress) { self.server = server }

    public func decide(url: URL?, isMainFrame: Bool = true) -> NavigationDecision {
        guard let url, let scheme = url.scheme?.lowercased() else { return .block }
        if url.absoluteString == "about:blank" { return .allow }
        switch scheme {
        case "http", "https":
            if isSameOrigin(url, scheme: scheme) { return .allow }
            // 子 frame 的跨站请求不应弹出系统浏览器,直接拒绝
            return isMainFrame ? .openExternal : .block
        case "mailto", "tel":
            return isMainFrame ? .openExternal : .block
        default:
            return .block
        }
    }

    private func isSameOrigin(_ url: URL, scheme: String) -> Bool {
        guard let host = url.host?.lowercased(), host == server.host, scheme == server.scheme else { return false }
        let port = url.port ?? (scheme == "https" ? 443 : 80)
        return port == server.effectivePort
    }
}
