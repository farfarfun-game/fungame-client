# fungame-client

FunGame 的多端封装壳:把 Web 版本装进 iOS、Android 与桌面(macOS / Linux)应用。

壳本身不含任何业务逻辑,也不打包前端资源。每个壳只做一件事:**加载一个可配置的服务地址**,并加上最基本的安全限制(导航限定在该地址的同源范围内,外链交给系统浏览器)。

| 目录 | 平台 | 技术 | 说明 |
| --- | --- | --- | --- |
| [`desktop/`](desktop/README.md) | macOS / Linux | Electron | 服务地址写在 `<userData>/config.json` |
| [`android/`](android/README.md) | Android | Kotlin,单 Activity | 构建时用 `-PfungameServer=...` 指定 |
| [`ios/`](ios/) | iOS | Swift,WKWebView | `ios/Config.xcconfig` 的 `FUNGAME_SERVER_ADDRESS` |

服务地址的格式统一为 `IP或主机名[:端口]`,可带 `http://` 或 `https://`,缺省按 `http`。各端的默认值都是 `https://fungame.farfarfun.com`,需要连别的服务时按上表改配置即可。

> 建议使用 `https` 域名。`http://IP` 不是浏览器的 secure context,Service Worker 与 Cache API 不可用,Web 侧会自动退化为不带离线能力的模式;iOS 还需要放开 ATS(见下)。

## iOS

极简 WebView 壳,唯一配置项是服务器地址,WKWebView 直接加载它。

- 配置:`ios/Config.xcconfig` 的 `FUNGAME_SERVER_ADDRESS`。个人覆盖写 `ios/Local.xcconfig`(已 `.gitignore`),或命令行 `xcodebuild ... FUNGAME_SERVER_ADDRESS=https://host`。
- ATS:地址可能是 `http://IP`,Info.plist 设了 `NSAllowsArbitraryLoadsInWebContent`(只放开 WebView 内容,原生请求仍受 ATS)。导航只允许与配置地址同源,其它 http(s) 用 SFSafariViewController 打开,`mailto` / `tel` 交给系统,其余拒绝。WebContent 进程崩溃自动重载,加载失败显示带「重试」的兜底页。App Store 审核对 ATS 放开可能要求说明,正式发布建议用 https 域名。
- 构建:`brew install xcodegen && cd ios && xcodegen generate`,然后 `open FungameShell.xcodeproj`;命令行模拟器构建:`xcodebuild -project FungameShell.xcodeproj -scheme FungameShell -sdk iphonesimulator -destination 'generic/platform=iOS Simulator' build`。
- 单测(地址解析、导航判定):`cd ios && swift test`。
- 签名:默认 `CODE_SIGNING_ALLOWED=NO`,无证书即可出模拟器包与未签名归档;`DEVELOPMENT_TEAM`、`CODE_SIGN_IDENTITY`、`PROVISIONING_PROFILE_SPECIFIER` 均为可注入变量,见 [`ios/TODO-signing.md`](ios/TODO-signing.md)。

## 签名与密钥

仓库里不包含任何密钥、证书或描述文件。本地开发使用自签材料(放在仓库之外),正式发布所需的签名材料通过 CI 的 Secrets 注入,各端说明见各自目录下的 `TODO-signing.md`。
