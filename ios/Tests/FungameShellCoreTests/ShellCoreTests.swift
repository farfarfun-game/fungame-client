import XCTest
@testable import FungameShellCore

final class ServerAddressTests: XCTestCase {
    private func ok(_ s: String) -> ServerAddress? { try? ServerAddress.parse(s).get() }

    func testBareIPDefaultsToHttp() {
        let a = ok("192.168.1.10")
        XCTAssertEqual(a, ServerAddress(scheme: "http", host: "192.168.1.10", port: nil))
        XCTAssertEqual(a?.url.absoluteString, "http://192.168.1.10/")
    }
    func testIPWithPort() { XCTAssertEqual(ok(" 10.0.0.1:8080 ")?.url.absoluteString, "http://10.0.0.1:8080/") }
    func testHttpsHostLowercased() { XCTAssertEqual(ok("HTTPS://Example.COM/")?.url.absoluteString, "https://example.com/") }
    func testErrors() {
        XCTAssertEqual(ServerAddress.parse("  "), .failure(.empty))
        XCTAssertEqual(ServerAddress.parse("ftp://a.com"), .failure(.unsupportedScheme))
        XCTAssertEqual(ServerAddress.parse("http://a.com/x"), .failure(.hasPathOrQuery))
        XCTAssertEqual(ServerAddress.parse("http://a.com?q=1"), .failure(.hasPathOrQuery))
        XCTAssertEqual(ServerAddress.parse("http://u:p@a.com"), .failure(.hasPathOrQuery))
        XCTAssertEqual(ServerAddress.parse("http://a.com:99999"), .failure(.invalid))
        XCTAssertEqual(ServerAddress.parse("http://"), .failure(.invalid))
    }
}

final class NavigationPolicyTests: XCTestCase {
    private func policy(_ s: String) -> NavigationPolicy { NavigationPolicy(server: try! ServerAddress.parse(s).get()) }
    private func d(_ p: NavigationPolicy, _ u: String, main: Bool = true) -> NavigationDecision {
        p.decide(url: URL(string: u), isMainFrame: main)
    }

    func testSameOriginAllowed() {
        let p = policy("http://10.0.0.1:8080")
        XCTAssertEqual(d(p, "http://10.0.0.1:8080/games/1?x=2"), .allow)
        XCTAssertEqual(d(p, "about:blank"), .allow)
    }
    func testDifferentPortSchemeHostNotSameOrigin() {
        let p = policy("http://10.0.0.1:8080")
        XCTAssertEqual(d(p, "http://10.0.0.1:9090/"), .openExternal)
        XCTAssertEqual(d(p, "https://10.0.0.1:8080/"), .openExternal)
        XCTAssertEqual(d(p, "http://evil.com/"), .openExternal)
        XCTAssertEqual(d(p, "http://10.0.0.1.evil.com:8080/"), .openExternal)
    }
    func testDefaultPortNormalised() {
        XCTAssertEqual(d(policy("http://a.com"), "http://a.com:80/x"), .allow)
        XCTAssertEqual(d(policy("https://a.com:443"), "https://a.com/x"), .allow)
    }
    func testSubframeCrossOriginBlocked() {
        XCTAssertEqual(d(policy("http://a.com"), "http://b.com/", main: false), .block)
    }
    func testOtherSchemes() {
        let p = policy("http://a.com")
        XCTAssertEqual(d(p, "tel:123"), .openExternal)
        XCTAssertEqual(d(p, "javascript:alert(1)"), .block)
        XCTAssertEqual(d(p, "file:///etc/passwd"), .block)
        XCTAssertEqual(p.decide(url: nil), .block)
    }
}
