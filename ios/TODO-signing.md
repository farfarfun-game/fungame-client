# 待办:签名

- [ ] 正式发布前,由团队统一申请 Apple Developer 账号、证书与描述文件并集中保管,再配入 GitHub Secrets。

当前工程默认 `CODE_SIGNING_ALLOWED=NO`,模拟器构建和未签名归档不需要任何证书。签名相关变量(`DEVELOPMENT_TEAM`、`CODE_SIGN_IDENTITY`、`PROVISIONING_PROFILE_SPECIFIER`)均在 `Config.xcconfig` 中留空,可由 `Local.xcconfig`(已 .gitignore)、环境变量或 xcodebuild 命令行覆盖。
