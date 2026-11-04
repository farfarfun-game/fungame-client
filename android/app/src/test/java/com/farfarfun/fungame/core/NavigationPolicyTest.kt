package com.farfarfun.fungame.core

import org.junit.Assert.assertEquals
import org.junit.Test

class NavigationPolicyTest {
    private fun policy(addr: String) = NavigationPolicy(ServerAddress.parse(addr)!!)

    @Test fun sameOriginAllowed() {
        val p = policy("http://10.0.0.5:8080")
        assertEquals(NavDecision.ALLOW, p.decide("http://10.0.0.5:8080/games/1?x=1#y"))
    }

    @Test fun portAndSchemeMustMatch() {
        val p = policy("http://10.0.0.5:8080")
        assertEquals(NavDecision.EXTERNAL, p.decide("http://10.0.0.5:9090/"))
        assertEquals(NavDecision.EXTERNAL, p.decide("https://10.0.0.5:8080/"))
        assertEquals(NavDecision.EXTERNAL, p.decide("http://10.0.0.6:8080/"))
    }

    @Test fun defaultPortsEquivalent() {
        assertEquals(NavDecision.ALLOW, policy("https://example.com").decide("https://example.com:443/x"))
        assertEquals(NavDecision.ALLOW, policy("example.com").decide("http://EXAMPLE.com/x"))
    }

    @Test fun httpsDowngradeIsNotSameOrigin() {
        assertEquals(NavDecision.EXTERNAL, policy("https://example.com").decide("http://example.com/"))
    }

    @Test fun externalLinksGoToSystem() {
        val p = policy("https://example.com")
        assertEquals(NavDecision.EXTERNAL, p.decide("https://other.org/"))
        assertEquals(NavDecision.EXTERNAL, p.decide("mailto:a@b.c"))
        assertEquals(NavDecision.EXTERNAL, p.decide("tel:123"))
    }

    @Test fun dangerousSchemesBlocked() {
        val p = policy("https://example.com")
        for (u in listOf("file:///data/data/x", "content://x/y", "intent://x#Intent;end",
            "javascript:alert(1)", "data:text/html,hi", "about:blank", "not a url", "")) {
            assertEquals(u, NavDecision.BLOCK, p.decide(u))
        }
    }

    @Test fun hostSpoofingBlocked() {
        val p = policy("https://example.com")
        assertEquals(NavDecision.BLOCK, p.decide("https://example.com@evil.com/"))
        assertEquals(NavDecision.EXTERNAL, p.decide("https://example.com.evil.com/"))
    }

    @Test fun casdoorLoginAndCallbackStayInWebView() {
        val p = policy("https://fungame.farfarfun.com")
        for (url in listOf(
            "https://id.farfarfun.com/login/oauth/authorize?client_id=fungame&state=test",
            "https://ID.FARFARFUN.COM:443/login",
            "https://fungame.farfarfun.com/api/auth/oidc/callback?code=test&state=test",
            "https://fungame.farfarfun.com/home",
        )) {
            assertEquals(url, NavDecision.ALLOW, p.decide(url))
        }
    }

    @Test fun casdoorAllowlistRequiresExactHttpsOrigin() {
        val p = policy("https://fungame.farfarfun.com")
        for (url in listOf(
            "http://id.farfarfun.com/login",
            "https://id.farfarfun.com:8443/login",
            "https://id.farfarfun.com.evil.test/login",
            "https://other.farfarfun.com/login",
        )) {
            assertEquals(url, NavDecision.EXTERNAL, p.decide(url))
        }
        assertEquals(NavDecision.BLOCK, p.decide("https://id.farfarfun.com@evil.test/login"))
        assertEquals(NavDecision.BLOCK, p.decide("https://evil.test@id.farfarfun.com/login"))
    }
}
