import fs from "node:fs";
import path from "node:path";

// 壳只需要一个配置:服务地址。写在 <userData>/config.json:  { "server": "192.168.1.10:8808" }
export const DEFAULT_SERVER = "https://fungame.farfarfun.com";
export const CONFIG_FILE = "config.json";

// 解析"IP/主机名[:端口]",可带 http:// 或 https://;缺省协议按 http。
// 非法输入抛错;返回 { url(加载地址), origin(导航白名单) }。
export function parseServerAddress(input) {
  if (typeof input !== "string" || input.trim() === "") throw new Error("服务地址为空");
  const raw = input.trim();
  const withScheme = /^[a-zA-Z][a-zA-Z0-9+.-]*:\/\//.test(raw) ? raw : `http://${raw}`;
  let u;
  try {
    u = new URL(withScheme);
  } catch {
    throw new Error(`服务地址无法解析: ${raw}`);
  }
  if (u.protocol !== "http:" && u.protocol !== "https:") throw new Error(`服务地址只支持 http/https: ${raw}`);
  if (u.username || u.password) throw new Error("服务地址不能包含用户名或密码");
  if (!u.hostname) throw new Error(`服务地址缺少主机: ${raw}`);
  return { url: u.href, origin: u.origin };
}

// 读取配置;文件不存在时写入默认值,方便用户直接编辑。
export function loadServerConfig(userDataDir) {
  const file = path.join(userDataDir, CONFIG_FILE);
  if (!fs.existsSync(file)) {
    fs.mkdirSync(userDataDir, { recursive: true });
    fs.writeFileSync(file, JSON.stringify({ server: DEFAULT_SERVER }, null, 2) + "\n");
  }
  let parsed;
  try {
    parsed = JSON.parse(fs.readFileSync(file, "utf8"));
  } catch {
    throw new Error(`配置文件不是合法 JSON: ${file}`);
  }
  return { file, ...parseServerAddress(parsed?.server) };
}
