package com.nordgas.ngsvisitor;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class ApiUrlTest {
    @Test public void appendsLocalIntegrationPath() {
        assertEquals("https://192.168.1.1/proxy/network/integration",
                ApiUrl.normalizeRoot("https://192.168.1.1/"));
    }

    @Test public void keepsFullConnectorUrl() {
        String url = "https://api.ui.com/v1/connector/consoles/abc/proxy/network/integration";
        assertEquals(url, ApiUrl.normalizeRoot(url));
    }
}

