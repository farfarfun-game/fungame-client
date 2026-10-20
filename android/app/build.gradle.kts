import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// ---- 服务地址(唯一配置项) ----
val serverAddress = (project.findProperty("fungameServer") as String).trim()
// 与 core/ServerAddress.kt 的规则保持一致(那边有单测);这里只为在构建期失败得更早、并生成明文白名单
val addressMatch = Regex("^(?:(https?)://)?(\\[[0-9a-fA-F:]+]|[A-Za-z0-9._-]+)(?::(\\d{1,5}))?(?:/.*)?$")
    .matchEntire(serverAddress)
    ?: throw GradleException("fungameServer 不合法: '$serverAddress'(示例 192.168.1.10:8080 或 https://example.com)")
val serverScheme = addressMatch.groupValues[1].ifEmpty { "http" }
val serverHost = addressMatch.groupValues[2].removePrefix("[").removeSuffix("]")

// network security config:默认禁止明文;仅当配置地址是 http 时,只对该主机放行明文
val genNetworkSecurityConfig = tasks.register("genNetworkSecurityConfig") {
    val out = layout.projectDirectory.file("src/main/res/xml/network_security_config.xml")
    inputs.property("scheme", serverScheme)
    inputs.property("host", serverHost)
    outputs.file(out)
    doLast {
        val domain = if (serverScheme == "http") {
            "\n    <domain-config cleartextTrafficPermitted=\"true\">\n" +
                "        <domain includeSubdomains=\"false\">$serverHost</domain>\n    </domain-config>"
        } else ""
        out.asFile.parentFile.mkdirs()
        out.asFile.writeText(
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<!-- 由 genNetworkSecurityConfig 生成,勿手改、勿提交 -->\n" +
                "<network-security-config>\n    <base-config cleartextTrafficPermitted=\"false\" />$domain\n</network-security-config>\n"
        )
    }
}
tasks.matching { it.name == "preBuild" }.configureEach { dependsOn(genNetworkSecurityConfig) }

// ---- 签名:环境变量 > keystore.properties > 回落 debug 签名(带警告) ----
// 查找顺序:-PfungameKeystoreProps / 环境变量 FUNGAME_KEYSTORE_PROPS / ~/.fungame-signing/android/keystore.properties
// CI 中用环境变量(FUNGAME_KEYSTORE_FILE / _PASSWORD / FUNGAME_KEY_ALIAS / FUNGAME_KEY_PASSWORD),来自 Secrets
val signingProps = Properties().also { props ->
    val path = (project.findProperty("fungameKeystoreProps") as String?)
        ?: System.getenv("FUNGAME_KEYSTORE_PROPS")
        ?: "${System.getProperty("user.home")}/.fungame-signing/android/keystore.properties"
    val f = file(path)
    if (f.isFile) f.inputStream().use { props.load(it) }
}
fun signingValue(env: String, key: String): String? =
    System.getenv(env)?.takeIf { it.isNotEmpty() } ?: signingProps.getProperty(key)?.takeIf { it.isNotEmpty() }
val ksFile = signingValue("FUNGAME_KEYSTORE_FILE", "storeFile")
val ksPassword = signingValue("FUNGAME_KEYSTORE_PASSWORD", "storePassword")
val ksAlias = signingValue("FUNGAME_KEY_ALIAS", "keyAlias")
val ksKeyPassword = signingValue("FUNGAME_KEY_PASSWORD", "keyPassword") ?: ksPassword
val hasReleaseKey = ksFile != null && ksPassword != null && ksAlias != null && file(ksFile).isFile

android {
    namespace = "com.farfarfun.fungame"
    compileSdk = 35
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "com.farfarfun.fungame" // 占位包名,待用户确认
        minSdk = 24
        targetSdk = 35
        // 发版时由 CI 通过 -PappVersion=1.2.3 -PappVersionCode=10203 注入
        versionCode = (findProperty("appVersionCode") as String?)?.toInt() ?: 1
        versionName = (findProperty("appVersion") as String?) ?: "0.1.0"
        buildConfigField("String", "SERVER_ADDRESS", "\"$serverAddress\"")
    }

    buildFeatures {
        buildConfig = true
    }

    signingConfigs {
        if (hasReleaseKey) {
            create("release") {
                storeFile = file(ksFile!!)
                storePassword = ksPassword
                keyAlias = ksAlias
                keyPassword = ksKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (hasReleaseKey) {
                signingConfig = signingConfigs.getByName("release")
            } else {
                logger.warn("WARNING: 未找到 release keystore,release 包回落到 debug 签名(仅限开发/CI 验证,不可发布)")
                signingConfig = signingConfigs.getByName("debug")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.0")

    testImplementation("junit:junit:4.13.2")
}
