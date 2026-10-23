package com.nordgas.ngsvisitor;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class UniFiApiClientTest {
    @Test public void formatsTenDigitVoucherCode() {
        assertEquals("01234-56789", UniFiApiClient.formatCode("0123456789"));
    }

    @Test public void preservesAlreadyFormattedVoucherCode() {
        assertEquals("01234-56789", UniFiApiClient.formatCode("01234-56789"));
    }
}
