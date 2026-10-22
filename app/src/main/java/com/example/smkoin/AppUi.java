package com.example.smkoin;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Small set of shared native UI helpers used by the Java/XML screens. */
final class AppUi {
    static final int INK = Color.rgb(23, 25, 20);
    static final int MUTED = Color.rgb(115, 118, 111);
    static final int LIME = Color.rgb(183, 239, 40);
    static final int SOFT = Color.rgb(245, 246, 242);
    static final int WHITE = Color.WHITE;

    private AppUi() { }

    static int dp(Context context, int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    static LinearLayout column(Context context) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    static LinearLayout row(Context context) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(Gravity.CENTER_VERTICAL);
        return layout;
    }

    static TextView text(Context context, String value, int size, boolean bold, int color) {
        TextView text = new TextView(context);
        text.setText(value);
        text.setTextSize(size);
        text.setTextColor(color);
        if (bold) text.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return text;
    }

    static Button button(Context context, String label, boolean primary) {
        Button button = new Button(context);
        button.setText(label);
        button.setTextColor(INK);
        button.setAllCaps(false);
        button.setTextSize(15);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setBackground(background(primary ? LIME : WHITE, dp(context, 18), 0));
        button.setMinHeight(dp(context, 48));
        return button;
    }

    static GradientDrawable background(int color, int radiusDp, int strokeColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radiusDp);
        if (strokeColor != 0) drawable.setStroke(1, strokeColor);
        return drawable;
    }

    static LinearLayout card(Context context) {
        LinearLayout card = column(context);
        card.setPadding(dp(context, 16), dp(context, 16), dp(context, 16), dp(context, 16));
        card.setBackground(background(WHITE, dp(context, 20), 0));
        card.setElevation(dp(context, 2));
        return card;
    }

    static void space(Context context, LinearLayout parent, int heightDp) {
        View space = new View(context);
        parent.addView(space, new LinearLayout.LayoutParams(1, dp(context, heightDp)));
    }
}
