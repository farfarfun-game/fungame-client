import { app, BrowserWindow, shell, dialog } from "electron";
import { loadServerConfig } from "./config.js";
import { installNavigationGuards } from "./navigation.js";

function errorPage(url, file, message) {
  const esc = (s) => String(s).replace(/[&<>"]/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" })[c]);
  return "data:text/html;charset=utf-8," + encodeURIComponent(
    `<meta charset="utf-8"><body style="font:15px sans-serif;padding:48px"><h2>无法连接服务</h2>` +
    `<p>${esc(url)}</p><p>${esc(message)}</p><p>服务地址可在配置文件中修改:<code>${esc(file)}</code>(改完重启)</p>` +
    `<p><a href="${esc(url)}">重试</a></p>`,
  );
}

if (!app.requestSingleInstanceLock()) {
  app.quit();
} else {
  let win = null;
  app.on("second-instance", () => {
    if (win) {
      if (win.isMinimized()) win.restore();
      win.focus();
    }
  });

  app.whenReady().then(() => {
    let cfg;
    try {
      cfg = loadServerConfig(app.getPath("userData"));
    } catch (err) {
      dialog.showErrorBox("FunGame 配置错误", String(err.message));
      app.exit(1);
      return;
    }
    win = new BrowserWindow({
      width: 1280,
      height: 800,
      webPreferences: { contextIsolation: true, nodeIntegration: false, sandbox: true, webSecurity: true },
    });
    installNavigationGuards(win.webContents, { origin: cfg.origin, openExternal: (u) => shell.openExternal(u) });
    win.webContents.on("did-fail-load", (_e, code, desc, failedUrl, isMainFrame) => {
      if (isMainFrame && code !== -3 && !failedUrl.startsWith("data:")) win.loadURL(errorPage(cfg.url, cfg.file, desc));
    });
    win.loadURL(cfg.url);
  });

  app.on("window-all-closed", () => app.quit());
}
