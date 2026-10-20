package com.farfarfun.fungame.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ServerAddressTest {
    private fun ok(s: String) = ServerAddress.parse(s).also { assertNotNull(s, it) }!!

    @Test fun bareIpDefaultsToHttp() {
        val a = ok("192.168.1.10")
        assertEquals("http", a.scheme)
        assertEquals("http://192.168.1.10/", a.startUrl)
        assertEquals(80, a.effectivePort)
    }

    @Test fun ipWithPort() {
        val a = ok(" 10.0.0.5:8080 ")
        assertEquals("10.0.0.5", a.host)
        assertEquals(8080, a.port)
        assertEquals("http://10.0.0.5:8080/", a.startUrl)
    }

    @Test fun httpsHostAndPath() {
        val a = ok("https://Example.com/app/")
        assertEquals("https", a.scheme)
        assertEquals("example.com", a.host)
        assertEquals(443, a.effectivePort)
        assertEquals("https://example.com/app/", a.startUrl)
    }

    @Test fun hostnameWithoutScheme() {
        assertEquals("http://game.local:3000/", ok("game.local:3000").startUrl)
    }

    @Test fun ipv6Literal() {
        val a = ok("http://[::1]:8080")
        assertEquals("http://[::1]:8080/", a.startUrl)
    }

    @Test fun rejectsInvalid() {
        for (s in listOf("", "   ", "ftp://x.com", "file:///etc/passwd", "javascript:alert(1)",
            "http://user:pw@x.com", "http://", "a b.com", "x.com:99999", "http://:80")) {
            assertNull(s, ServerAddress.parse(s))
        }
    }
}
