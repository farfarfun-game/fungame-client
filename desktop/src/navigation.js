const EXTERNAL_PROTOCOLS = new Set(["https:", "http:", "mailto:"]);

function parse(url) {
  try {
    return new URL(url);
  } catch {
    return null;
  }
}

// 只允许导航到本 origin。
export function isAllowedNavigation(url, origin) {
  const u = parse(url);
  return u !== null && u.origin === origin;
}

// 外链只放行 http/https/mailto 交给系统浏览器;file:、javascript:、自定义 scheme 一律拒绝。
export function isSafeExternalUrl(url) {
  const u = parse(url);
  return u !== null && EXTERNAL_PROTOCOLS.has(u.protocol);
}

// 对 webContents 安装导航限制。openExternal 由调用方注入(shell.openExternal)。
export function installNavigationGuards(webContents, { origin, openExternal }) {
  const handleNav = (event, url) => {
    if (isAllowedNavigation(url, origin)) return;
    event.preventDefault();
    if (isSafeExternalUrl(url)) openExternal(url);
  };
  webContents.on("will-navigate", handleNav);
  webContents.on("will-redirect", handleNav);
  webContents.setWindowOpenHandler(({ url }) => {
    if (isSafeExternalUrl(url) && !isAllowedNavigation(url, origin)) openExternal(url);
    return { action: "deny" };
  });
  webContents.on("will-attach-webview", (event) => event.preventDefault());
}
