package com.nordgas.ngsvisitor;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class VoucherAdapter extends BaseAdapter {
    private final Context context;
    private final List<Voucher> items = new ArrayList<>();
    private final ReservationStore reservations;
    private final Runnable onReservationChanged;

    public VoucherAdapter(Context context, Runnable onReservationChanged) {
        this.context = context;
        this.reservations = new ReservationStore(context);
        this.onReservationChanged = onReservationChanged;
    }

    public void replace(List<Voucher> vouchers) {
        items.clear();
        items.addAll(vouchers);
        notifyDataSetChanged();
    }

    @Override public int getCount() { return items.size(); }
    @Override public Voucher getItem(int position) { return items.get(position); }
    @Override public long getItemId(int position) { return position; }

    @Override public View getView(int position, View convertView, ViewGroup parent) {
        Voucher voucher = getItem(position);
        Instant now = Instant.now();
        boolean reserved = reservations.isReserved(voucher.code, now) && voucher.guestCount == 0;

        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(14), dp(12), dp(12));
        card.setBackgroundResource(R.drawable.brand_card);

        LinearLayout top = new LinearLayout(context);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView code = text(voucher.code, 22, context.getColor(R.color.navy));
        code.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        top.addView(code, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        TextView badge = text(stateText(voucher, now, reserved), 14,
                stateColor(voucher, now, reserved));
        badge.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        top.addView(badge);
        card.addView(top);

        TextView detail = text(expiryText(voucher, now) + "  ·  " +
                context.getString(R.string.remaining_slots,
                        voucher.remainingSlots(), voucher.guestLimit),
                14, context.getColor(R.color.text_secondary));
        detail.setPadding(0, dp(6), 0, dp(8));
        card.addView(detail);

        LinearLayout actions = new LinearLayout(context);
        Button copy = smallButton(context.getString(reserved
                ? R.string.cancel_reservation : R.string.issue_and_copy), !reserved);
        copy.setEnabled(voucher.isAvailable(now));
        copy.setAlpha(copy.isEnabled() ? 1f : 0.45f);
        copy.setOnClickListener(v -> {
            if (reserved) {
                reservations.clear(voucher.code);
                onReservationChanged.run();
            } else {
                reservations.reserve(voucher.code);
                copyCode(voucher.code);
                onReservationChanged.run();
            }
        });
        actions.addView(copy, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        Button qr = smallButton(context.getString(R.string.qr_code), false);
        qr.setEnabled(voucher.isAvailable(now) && !reserved);
        qr.setAlpha(qr.isEnabled() ? 1f : 0.45f);
        qr.setOnClickListener(v -> {
            reservations.reserve(voucher.code);
            showQr(voucher.code);
            onReservationChanged.run();
        });
        LinearLayout.LayoutParams qrParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        qrParams.leftMargin = dp(8);
        actions.addView(qr, qrParams);
        card.addView(actions);

        LinearLayout wrapper = new LinearLayout(context);
        wrapper.setPadding(dp(12), dp(6), dp(12), dp(6));
        wrapper.addView(card, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return wrapper;
    }

    private String stateText(Voucher voucher, Instant now, boolean reserved) {
        if (reserved) return context.getString(R.string.state_waiting);
        return switch (voucher.state(now)) {
            case UNUSED -> context.getString(R.string.state_unused);
            case ACTIVE -> context.getString(R.string.state_active);
            case UNAVAILABLE -> context.getString(R.string.state_unavailable);
        };
    }

    private String expiryText(Voucher voucher, Instant now) {
        if (voucher.expiresAt == null) return context.getString(R.string.first_use_8h);
        Duration duration = Duration.between(now, voucher.expiresAt);
        if (duration.isNegative() || duration.isZero()) {
            return context.getString(R.string.expired);
        }
        long totalMinutes = duration.toMinutes();
        return context.getString(R.string.remaining_time,
                totalMinutes / 60, totalMinutes % 60);
    }

    private int stateColor(Voucher voucher, Instant now, boolean reserved) {
        if (reserved) return context.getColor(R.color.blue);
        return switch (voucher.state(now)) {
            case UNUSED -> context.getColor(R.color.green);
            case ACTIVE -> context.getColor(R.color.amber);
            case UNAVAILABLE -> context.getColor(R.color.red);
        };
    }

    private void copyCode(String code) {
        ClipboardSecurity.copyVoucher(context, code);
        Toast.makeText(context, R.string.voucher_copied, Toast.LENGTH_SHORT).show();
    }

    private void showQr(String code) {
        try {
            ImageView image = new ImageView(context);
            image.setPadding(dp(24), dp(24), dp(24), dp(12));
            image.setImageBitmap(QrCode.create(code.replace("-", ""), 720));
            new AlertDialog.Builder(context)
                    .setTitle(code)
                    .setView(image)
                    .setMessage(R.string.qr_message)
                    .setPositiveButton(R.string.close, null)
                    .show();
        } catch (Exception e) {
            Toast.makeText(context, R.string.qr_error, Toast.LENGTH_SHORT).show();
        }
    }

    private Button smallButton(String label, boolean primary) {
        Button button = new Button(context);
        button.setText(label);
        button.setTextColor(context.getColor(primary ? R.color.white : R.color.navy));
        button.setBackgroundResource(primary
                ? R.drawable.brand_button_primary : R.drawable.brand_button);
        button.setAllCaps(false);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        return button;
    }

    private TextView text(String value, float sp, int color) {
        TextView view = new TextView(context);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        return view;
    }

    private int dp(int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
