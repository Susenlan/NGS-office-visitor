package com.nordgas.ngsvisitor;

import org.junit.Test;
import java.time.Instant;
import static org.junit.Assert.*;

public class VoucherTest {
    private final Instant now = Instant.parse("2026-09-16T02:00:00Z");

    @Test public void unusedVoucherHasFiveSlots() {
        Voucher voucher = new Voucher("1", "12345-67890", "", 5, 0, null, null, false);
        assertEquals(Voucher.State.UNUSED, voucher.state(now));
        assertEquals(5, voucher.remainingSlots());
        assertTrue(voucher.isAvailable(now));
    }

    @Test public void activeVoucherRemainsUsableUntilLimit() {
        Voucher voucher = new Voucher("1", "12345-67890", "", 5, 4, now,
                now.plusSeconds(3600), false);
        assertEquals(Voucher.State.ACTIVE, voucher.state(now));
        assertEquals(1, voucher.remainingSlots());
    }

    @Test public void fifthGuestExhaustsVoucher() {
        Voucher voucher = new Voucher("1", "12345-67890", "", 5, 5, now,
                now.plusSeconds(3600), false);
        assertEquals(Voucher.State.UNAVAILABLE, voucher.state(now));
    }

    @Test public void expiredVoucherIsUnavailableEvenWithSlots() {
        Voucher voucher = new Voucher("1", "12345-67890", "", 5, 2,
                now.minusSeconds(3600), now.minusSeconds(1), true);
        assertFalse(voucher.isAvailable(now));
    }

    @Test public void pastExpiryWinsEvenBeforeApiFlagRefreshes() {
        Voucher voucher = new Voucher("1", "12345-67890", "", 5, 2,
                now.minusSeconds(3600), now.minusSeconds(1), false);
        assertEquals(Voucher.State.UNAVAILABLE, voucher.state(now));
    }
}
