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
}
