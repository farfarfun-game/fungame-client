import test from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { parseServerAddress, loadServerConfig, DEFAULT_SERVER, CONFIG_FILE } from "../src/config.js";

test("IP/主机名[:端口],缺省协议按 http", () => {
  assert.deepEqual(parseServerAddress("192.168.1.10:8808"), { url: "http://192.168.1.10:8808/", origin: "http://192.168.1.10:8808" });
  assert.equal(parseServerAddress("game.example.com").origin, "http://game.example.com");
  assert.equal(parseServerAddress(" https://game.example.com:8443/app ").origin, "https://game.example.com:8443");
  assert.equal(parseServerAddress("[::1]:8808").origin, "http://[::1]:8808");
});

test("非法地址被拒绝", () => {
  for (const bad of ["", "   ", null, undefined, 5, "file:///etc/passwd", "javascript:alert(1)", "ftp://a.com", "http://u:p@a.com", "http://", "http://a b"]) {
    assert.throws(() => parseServerAddress(bad), undefined, String(bad));
  }
});

test("配置文件:缺失时写入默认值,存在时读取,损坏时报错", () => {
  const dir = fs.mkdtempSync(path.join(os.tmpdir(), "fgd-"));
  const first = loadServerConfig(dir);
  assert.equal(first.url, new URL(DEFAULT_SERVER).href);
  assert.ok(fs.existsSync(path.join(dir, CONFIG_FILE)));
  fs.writeFileSync(path.join(dir, CONFIG_FILE), JSON.stringify({ server: "10.0.0.5:9000" }));
  assert.equal(loadServerConfig(dir).origin, "http://10.0.0.5:9000");
  fs.writeFileSync(path.join(dir, CONFIG_FILE), "{oops");
  assert.throws(() => loadServerConfig(dir), /JSON/);
  fs.writeFileSync(path.join(dir, CONFIG_FILE), "{}");
  assert.throws(() => loadServerConfig(dir));
});
