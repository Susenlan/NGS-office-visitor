package com.nordgas.ngsvisitor;

import java.time.Instant;

public final class Voucher {
    public enum State { UNUSED, ACTIVE, UNAVAILABLE }

    public final String id;
    public final String code;
    public final String name;
    public final int guestLimit;
    public final int guestCount;
    public final Instant activatedAt;
    public final Instant expiresAt;
    public final boolean expired;

    public Voucher(String id, String code, String name, int guestLimit, int guestCount,
                   Instant activatedAt, Instant expiresAt, boolean expired) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.guestLimit = guestLimit;
        this.guestCount = guestCount;
        this.activatedAt = activatedAt;
        this.expiresAt = expiresAt;
        this.expired = expired;
    }

    public int remainingSlots() {
        return Math.max(0, guestLimit - guestCount);
    }

    public boolean isAvailable(Instant now) {
        return !expired && remainingSlots() > 0 && (expiresAt == null || now.isBefore(expiresAt));
    }

    public State state(Instant now) {
        if (!isAvailable(now)) return State.UNAVAILABLE;
        return guestCount == 0 ? State.UNUSED : State.ACTIVE;
    }

}
