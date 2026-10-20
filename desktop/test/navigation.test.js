import test from "node:test";
import assert from "node:assert/strict";
import { EventEmitter } from "node:events";
import { isAllowedNavigation, isSafeExternalUrl, installNavigationGuards } from "../src/navigation.js";

const ORIGIN = "http://192.168.1.10:8808";

test("仅允许配置的 origin", () => {
  assert.ok(isAllowedNavigation(`${ORIGIN}/games/1`, ORIGIN));
  assert.ok(!isAllowedNavigation("http://192.168.1.10:8809/", ORIGIN));
  assert.ok(!isAllowedNavigation("https://192.168.1.10:8808/", ORIGIN));
  assert.ok(!isAllowedNavigation("https://evil.example/", ORIGIN));
  assert.ok(!isAllowedNavigation("file:///etc/passwd", ORIGIN));
  assert.ok(!isAllowedNavigation("not a url", ORIGIN));
});

test("外链只放行 http/https/mailto", () => {
  assert.ok(isSafeExternalUrl("https://a.com"));
  assert.ok(isSafeExternalUrl("mailto:a@b.c"));
  assert.ok(!isSafeExternalUrl("file:///x"));
  assert.ok(!isSafeExternalUrl("javascript:alert(1)"));
  assert.ok(!isSafeExternalUrl("app://x"));
});

function fakeContents() {
  const wc = new EventEmitter();
  wc.setWindowOpenHandler = (fn) => { wc.openHandler = fn; };
  return wc;
}

test("will-navigate/will-redirect 跨 origin 被拦截,安全外链交给系统浏览器", () => {
  const wc = fakeContents();
  const opened = [];
  installNavigationGuards(wc, { origin: ORIGIN, openExternal: (u) => opened.push(u) });
  let prevented = 0;
  const ev = { preventDefault: () => { prevented += 1; } };
  wc.emit("will-navigate", ev, `${ORIGIN}/ok`);
  assert.equal(prevented, 0);
  wc.emit("will-navigate", ev, "https://a.com/");
  wc.emit("will-redirect", ev, "file:///etc/passwd");
  assert.equal(prevented, 2);
  assert.deepEqual(opened, ["https://a.com/"]);
});

test("window.open 一律 deny,外链走系统浏览器", () => {
  const wc = fakeContents();
  const opened = [];
  installNavigationGuards(wc, { origin: ORIGIN, openExternal: (u) => opened.push(u) });
  assert.deepEqual(wc.openHandler({ url: "https://a.com" }), { action: "deny" });
  assert.deepEqual(wc.openHandler({ url: `${ORIGIN}/x` }), { action: "deny" });
  assert.deepEqual(wc.openHandler({ url: "javascript:1" }), { action: "deny" });
  assert.deepEqual(opened, ["https://a.com"]);
});
