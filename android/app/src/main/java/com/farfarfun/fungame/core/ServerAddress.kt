package com.farfarfun.fungame.core

import java.net.URI

/**
 * 壳唯一的配置项:服务地址。接受 `192.168.1.10:8080`、`example.com`、
 * `http://10.0.0.5`、`https://example.com/app/`;缺省协议按 http 处理。
 */
data class ServerAddress(val scheme: String, val host: String, val port: Int, val startUrl: String) {
    /** 把端口补成协议默认值,用于来源比较。 */
    val effectivePort: Int get() = if (port != -1) port else defaultPort(scheme)

    companion object {
        private fun defaultPort(scheme: String) = if (scheme == "https") 443 else 80

        /** 非法返回 null。拒绝:空串、非 http(s)、无主机、带用户信息、端口越界。 */
        fun parse(input: String): ServerAddress? {
            val text = input.trim()
            if (text.isEmpty() || text.any { it.isWhitespace() }) return null
            val withScheme = if ("://" in text) text else "http://$text"
            val uri = try { URI(withScheme) } catch (_: Exception) { return null }
            val scheme = uri.scheme?.lowercase() ?: return null
            if (scheme != "http" && scheme != "https") return null
            val host = uri.host?.lowercase() ?: return null
            if (uri.userInfo != null) return null
            if (uri.port != -1 && uri.port !in 1..65535) return null
            val path = uri.rawPath.orEmpty().ifEmpty { "/" }
            val authority = if (uri.port != -1) "$host:${uri.port}" else host
            // URI.host 对 IPv6 字面量已带方括号
            return ServerAddress(scheme, host, uri.port, "$scheme://$authority$path")
        }
    }
}
