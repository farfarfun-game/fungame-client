# FunGame / desktop

Electron 桌面壳(macOS / Linux)。壳本身只做一件事:**打开一个可配置的服务地址**。不含前端代码、不依赖 web 包或内部 npm 源,构建不需要任何内部凭据。

## 配置服务地址

编辑 `<userData>/config.json`(首次启动自动生成,默认 `https://fungame.farfarfun.com`):

```json
{ "server": "192.168.1.10:8808" }
```

- 格式为 `IP或主机名[:端口]`,可带 `http://` / `https://`,缺省按 `http`;改完重启生效。
- 路径:macOS `~/Library/Application Support/FunGame/config.json`,Linux `~/.config/FunGame/config.json`(开发模式下目录名取 package 名)。
- 连不上时窗口显示错误页,并给出配置文件路径和"重试"链接。

## 开发与测试

```bash
npm install        # 若 electron 二进制未下载:node node_modules/electron/install.js
npm test           # 地址解析/校验、导航限制
npm start
```

## 安全基线

`contextIsolation: true`、`nodeIntegration: false`、`sandbox: true`;主窗口只允许导航到配置的 origin;`window.open` 一律拒绝,`http/https/mailto` 外链交给系统浏览器;单实例。

## 打包与签名

```bash
npm run dist       # electron-builder,配置见 electron-builder.config.cjs
```

- **读不到签名身份时自动跳过签名,不报错**,CI 无密钥也能出未签名包。
- 本地(无 `CSC_LINK`/`CSC_NAME`):macOS 产物由 afterPack 做 ad-hoc 签名(`codesign -s -`),不公证;Linux 无需签名。
- CI 正式签名:在 GitHub Secrets 配 `CSC_LINK`(p12 的 base64)、`CSC_KEY_PASSWORD`;公证另配 `APPLE_ID`、`APPLE_APP_SPECIFIC_PASSWORD`、`APPLE_TEAM_ID`。
- 证书/密钥(`*.p12 *.pem *.key` 等)已被 `.gitignore` 排除,绝不入库。
- 待办见 `TODO-signing.md`。
