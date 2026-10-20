# Android 壳

最小 WebView 壳:单 Activity,全屏加载**一个可配置的服务地址**。无缓存层、无离线包、无 JSBridge(Web 侧检测不到 bridge 时自动走自带的 Service Worker 缓存)。

## 配置服务地址

唯一配置项 `fungameServer`:IP / 主机名,可带端口与协议,缺省按 `http` 处理。

```bash
./gradlew assembleDebug -PfungameServer=192.168.1.10:8080
./gradlew assembleDebug -PfungameServer=https://game.example.com
```

也可直接改 `gradle.properties`。默认值是 `https://fungame.farfarfun.com`。地址在构建期写入 `BuildConfig.SERVER_ADDRESS`(未做运行时修改入口,改地址 = 重新构建)。构建期会校验格式,不合法直接失败。

### 明文 HTTP

地址是 `http://` 时,构建任务 `genNetworkSecurityConfig` 会生成 `res/xml/network_security_config.xml`(已 gitignore),**只对配置的那个主机放行明文**,其余一律禁止。地址是 `https://` 时不放行任何明文。注意 `http://IP` 没有 secure context(无 Service Worker、部分浏览器 API 不可用),生产环境应使用 https 域名。

## 安全基线

- 关闭 `allowFileAccess` / `allowContentAccess` / `allowFileAccessFromFileURLs` / `allowUniversalAccessFromFileURLs`;混合内容禁止。
- 导航只允许与配置地址同协议、同主机、同端口;其它 http(s)、`mailto:`、`tel:` 交给系统处理;`file:` / `content:` / `intent:` / `javascript:` 等一律拦截(`NavigationPolicy`)。

## 构建与测试

需要 JDK 17+ 与 Android SDK(`local.properties` 中 `sdk.dir`,或 `ANDROID_HOME`)。

```bash
./gradlew test             # 地址解析/校验、导航判定(纯 JVM)
./gradlew assembleDebug    # app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease
```

## 签名

release 签名按以下顺序取值:环境变量 `FUNGAME_KEYSTORE_FILE` / `FUNGAME_KEYSTORE_PASSWORD` / `FUNGAME_KEY_ALIAS` / `FUNGAME_KEY_PASSWORD`,其次 `keystore.properties`(字段 `storeFile` `storePassword` `keyAlias` `keyPassword`)。`keystore.properties` 路径依次为 `-PfungameKeystoreProps=...`、环境变量 `FUNGAME_KEYSTORE_PROPS`、默认 `~/.fungame-signing/android/keystore.properties`。都读不到时 release 回落到 debug 签名并打印警告,所以 CI 没有密钥也能出包(该包不可发布)。

本地生成开发用 keystore(放在仓库之外,口令随机):

```bash
D=~/.fungame-signing/android; mkdir -p $D && chmod 700 $D
PW=$(LC_ALL=C tr -dc 'A-Za-z0-9' </dev/urandom | head -c 24)
keytool -genkeypair -keystore $D/fungame-release.jks -storetype PKCS12 -alias fungame \
  -keyalg RSA -keysize 2048 -validity 10000 -storepass "$PW" -keypass "$PW" \
  -dname "CN=FunGame Dev, O=FarFarFun, C=CN"
printf 'storeFile=%s\nstorePassword=%s\nkeyAlias=fungame\nkeyPassword=%s\n' "$D/fungame-release.jks" "$PW" "$PW" > $D/keystore.properties
chmod 600 $D/keystore.properties $D/fungame-release.jks
```

CI 中改用 Secrets:把 keystore 做 base64 存入 Secret,job 里解码到临时文件,再以 `FUNGAME_KEYSTORE_FILE`(解码后路径)、`FUNGAME_KEYSTORE_PASSWORD`、`FUNGAME_KEY_ALIAS`、`FUNGAME_KEY_PASSWORD` 环境变量传入。`*.jks` / `*.keystore` / `keystore.properties` 已被 `.gitignore` 排除,绝不入库。见 `TODO-signing.md`。

## 待用户决定

包名(目前占位 `com.farfarfun.fungame`)、应用名与图标、正式服务地址、正式发布 keystore。
