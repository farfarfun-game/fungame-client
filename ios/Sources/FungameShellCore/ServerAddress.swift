import Foundation

/// 壳唯一的配置项:服务器地址(IP 或主机名[:端口],http 或 https)。
public struct ServerAddress: Equatable {
    public enum ParseError: Error, Equatable {
        case empty
        case invalid
        case unsupportedScheme
        case hasPathOrQuery
    }

    public let scheme: String
    public let host: String
    public let port: Int?

    /// WebView 加载的起始 URL。
    public var url: URL {
        var c = URLComponents()
        c.scheme = scheme
        c.host = host
        c.port = port
        c.path = "/"
        return c.url!
    }

    /// 接受 `1.2.3.4`、`1.2.3.4:8080`、`example.com`、`http://1.2.3.4:8080`、`https://example.com/`。
    /// 未写协议时默认 http(典型场景是内网 IP);不允许带路径、查询或账号。
    public static func parse(_ raw: String) -> Result<ServerAddress, ParseError> {
        let text = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        if text.isEmpty { return .failure(.empty) }
        let withScheme = text.contains("://") ? text : "http://" + text
        guard let c = URLComponents(string: withScheme),
              let scheme = c.scheme?.lowercased(),
              let host = c.host, !host.isEmpty else { return .failure(.invalid) }
        guard scheme == "http" || scheme == "https" else { return .failure(.unsupportedScheme) }
        guard c.user == nil, c.password == nil, c.fragment == nil, c.query == nil,
              c.path.isEmpty || c.path == "/" else { return .failure(.hasPathOrQuery) }
        if let p = c.port, !(1...65535).contains(p) { return .failure(.invalid) }
        return .success(ServerAddress(scheme: scheme, host: host.lowercased(), port: c.port))
    }

    /// 缺省端口归一化后比较,`http://a` 与 `http://a:80` 视为同源。
    var effectivePort: Int { port ?? (scheme == "https" ? 443 : 80) }
}
