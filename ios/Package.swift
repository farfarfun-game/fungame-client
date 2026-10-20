// swift-tools-version:5.9
import PackageDescription

// 纯逻辑(地址解析、导航判定)放在无 UIKit 依赖的 target,macOS 上 `swift test` 即可运行。
let package = Package(
    name: "FungameShell",
    platforms: [.iOS(.v15), .macOS(.v12)],
    products: [.library(name: "FungameShellCore", targets: ["FungameShellCore"])],
    targets: [
        .target(name: "FungameShellCore"),
        .testTarget(name: "FungameShellCoreTests", dependencies: ["FungameShellCore"]),
    ]
)
