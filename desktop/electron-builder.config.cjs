// 签名策略:读不到签名身份时自动跳过(不报错),CI 无密钥也能出未签名包。
// - CI/正式:设置 CSC_LINK + CSC_KEY_PASSWORD(或 CSC_NAME),macOS 公证另设 APPLE_ID 等(见 README)。
// - 本地:无身份 => 不走 electron-builder 签名,afterPack 对 .app 做 ad-hoc 签名(codesign -s -),不公证。
const { execFileSync } = require("node:child_process");
const path = require("node:path");

const hasIdentity = Boolean(process.env.CSC_LINK || process.env.CSC_NAME);

module.exports = {
  appId: "com.farfarfun.fungame",
  productName: "FunGame",
  files: ["src/**", "package.json"],
  mac: {
    category: "public.app-category.games",
    target: ["dmg"],
    ...(hasIdentity ? {} : { identity: null }),
  },
  linux: { target: ["deb"], category: "Game", maintainer: "farfarfun <noreply@farfarfun.com>" },
  afterPack: async (context) => {
    if (hasIdentity || context.electronPlatformName !== "darwin") return;
    const app = path.join(context.appOutDir, `${context.packager.appInfo.productFilename}.app`);
    try {
      execFileSync("codesign", ["--force", "--deep", "-s", "-", app], { stdio: "inherit" });
    } catch (e) {
      console.warn(`ad-hoc 签名跳过: ${e.message}`);
    }
  },
};
