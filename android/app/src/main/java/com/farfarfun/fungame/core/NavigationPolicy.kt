package com.farfarfun.fungame.core

import java.net.URI

enum class NavDecision { ALLOW, EXTERNAL, BLOCK }

/**
 * 导航限制:只有与配置地址同协议、同主机、同端口的 URL 留在 WebView 内;
 * 其它 http(s)、mailto、tel 交给系统处理;file:/content:/intent:/javascript: 等一律拦截。
 */
class NavigationPolicy(private val server: ServerAddress) {
    fun decide(url: String): NavDecision {
        val uri = try { URI(url.trim()) } catch (_: Exception) { return NavDecision.BLOCK }
        val scheme = uri.scheme?.lowercase() ?: return NavDecision.BLOCK
        if (scheme == "mailto" || scheme == "tel") return NavDecision.EXTERNAL
        if (scheme != "http" && scheme != "https") return NavDecision.BLOCK
        val host = uri.host?.lowercase() ?: return NavDecision.BLOCK
        // https://good.com@evil.com 之类的主机欺骗
        if (uri.userInfo != null) return NavDecision.BLOCK
        val port = if (uri.port != -1) uri.port else if (scheme == "https") 443 else 80
        val same = scheme == server.scheme && host == server.host && port == server.effectivePort
        return if (same) NavDecision.ALLOW else NavDecision.EXTERNAL
    }
}
