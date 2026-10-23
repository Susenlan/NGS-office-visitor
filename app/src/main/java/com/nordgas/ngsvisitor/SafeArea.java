package com.nordgas.ngsvisitor;

import android.app.Activity;
import android.graphics.Insets;
import android.os.Build;
import android.view.DisplayCutout;
import android.view.View;
import android.view.WindowInsets;

/** Keeps controls clear of status bars, navigation bars, notches and punch-hole cameras. */
@SuppressWarnings("deprecation")
public final class SafeArea {
    private SafeArea() {}

    public static void apply(Activity activity, View header, View body) {
        if (Build.VERSION.SDK_INT >= 30) {
            activity.getWindow().setDecorFitsSystemWindows(false);
        } else {
            activity.getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                            | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        }

        int headerLeft = header.getPaddingLeft();
        int headerTop = header.getPaddingTop();
        int headerRight = header.getPaddingRight();
        int headerBottom = header.getPaddingBottom();
        int bodyLeft = body.getPaddingLeft();
        int bodyTop = body.getPaddingTop();
        int bodyRight = body.getPaddingRight();
        int bodyBottom = body.getPaddingBottom();

        header.setOnApplyWindowInsetsListener((view, windowInsets) -> {
            SafeInsets safe = insets(windowInsets);
            header.setPadding(headerLeft + safe.left, headerTop + safe.top,
                    headerRight + safe.right, headerBottom);
            body.setPadding(bodyLeft + safe.left, bodyTop,
                    bodyRight + safe.right, bodyBottom + safe.bottom);
            return windowInsets;
        });
        header.requestApplyInsets();
    }

    private static SafeInsets insets(WindowInsets windowInsets) {
        if (Build.VERSION.SDK_INT >= 30) {
            Insets value = windowInsets.getInsets(
                    WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            return new SafeInsets(value.left, value.top, value.right, value.bottom);
        }

        int left = windowInsets.getSystemWindowInsetLeft();
        int top = windowInsets.getSystemWindowInsetTop();
        int right = windowInsets.getSystemWindowInsetRight();
        int bottom = windowInsets.getSystemWindowInsetBottom();
        if (Build.VERSION.SDK_INT >= 28) {
            DisplayCutout cutout = windowInsets.getDisplayCutout();
            if (cutout != null) {
                left = Math.max(left, cutout.getSafeInsetLeft());
                top = Math.max(top, cutout.getSafeInsetTop());
                right = Math.max(right, cutout.getSafeInsetRight());
                bottom = Math.max(bottom, cutout.getSafeInsetBottom());
            }
        }
        return new SafeInsets(left, top, right, bottom);
    }

    private static final class SafeInsets {
        final int left;
        final int top;
        final int right;
        final int bottom;

        SafeInsets(int left, int top, int right, int bottom) {
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
        }
    }
}
