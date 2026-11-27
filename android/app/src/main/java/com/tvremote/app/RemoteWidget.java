package com.tvremote.app;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.widget.RemoteViews;

/**
 * ويدجت الريموت على الشاشة الرئيسية — الصغير (4×1). {@link Big} هو الكبير (4×3).
 *
 * لو التطبيق شغال في الخلفية: الزراير broadcast بتروح لـ MainActivity على طول.
 * لو التطبيق مقفول خالص: الزراير بتفتح MainActivity "في الخلفية" (بيرجع ورا على طول)
 * ويوصل ويبعت الزرار أول ما الاتصال يجهز.
 */
public class RemoteWidget extends AppWidgetProvider {

    static final String ACTION_KEY = "com.tvremote.app.WIDGET_KEY";
    static final String ACTION_OPEN_KEY = "com.tvremote.app.WIDGET";
    /** كود "بدّل الجهاز" (مش زرار تليفزيون). */
    static final int NEXT_DEVICE = -1;

    /** الكبير: نفس المنطق بتصميم تاني. */
    public static class Big extends RemoteWidget {
        @Override
        int layout() {
            return R.layout.widget_big;
        }
    }

    int layout() {
        return R.layout.widget_small;
    }

    // ------------------------------------------------------------ Provider

    @Override
    public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
        updateAll(c);
    }

    @Override
    public void onReceive(Context c, Intent intent) {
        super.onReceive(c, intent);
        if (!ACTION_KEY.equals(intent.getAction())) return;
        int code = intent.getIntExtra("code", 0);
        MainActivity a = MainActivity.live;
        if (a != null) {
            a.widgetKey(code);
            return;
        }
        // التطبيق اتقفل من غير ما يلحق يحدّث الويدجت: نفتحه في الخلفية، والزراير الجاية تفتحه على طول
        updateAll(c);
        try {
            c.startActivity(openIntent(c, code));
        } catch (Exception ignored) { }
    }

    // --------------------------------------------------------------- Static

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("remote", Context.MODE_PRIVATE);
    }

    /** ألوان الثيم الحالي في التطبيق (بتتبعت من الواجهة). */
    static void setTheme(Context c, String bg, String panel, String accent, boolean light) {
        prefs(c).edit().putString("wBg", bg).putString("wPanel", panel).putString("wAccent", accent)
                .putBoolean("wLight", light).apply();
        updateAll(c);
    }

    /** بيحدّث كل الويدجتات (الاسم والحالة والزراير). */
    static void updateAll(Context c) {
        try {
            Context app = c.getApplicationContext();
            AppWidgetManager m = AppWidgetManager.getInstance(app);
            if (m == null) return;
            int[] small = m.getAppWidgetIds(new ComponentName(app, RemoteWidget.class));
            int[] big = m.getAppWidgetIds(new ComponentName(app, Big.class));
            if (small.length == 0 && big.length == 0) return;

            // الحالة: من التطبيق لو شغال، وإلا آخر حاجة اتحفظت
            SharedPreferences p = prefs(app);
            MainActivity a = MainActivity.live;
            String name, state;
            if (a != null) {
                name = MainActivity.nName == null ? "" : MainActivity.nName;
                state = MainActivity.nState == null ? "" : MainActivity.nState;
                p.edit().putString("wName", name).apply();
            } else {
                name = p.getString("wName", "");
                state = "offline";
            }
            boolean multi = a != null ? a.deviceCount() > 1 : p.getBoolean("wMulti", false);
            if (a != null) p.edit().putBoolean("wMulti", multi).apply();
            if (name.isEmpty()) name = L.t("ريموت TV");

            for (int id : small) m.updateAppWidget(id, build(app, R.layout.widget_small, name, state, multi, a != null));
            for (int id : big) m.updateAppWidget(id, build(app, R.layout.widget_big, name, state, multi, a != null));
        } catch (Exception ignored) { }
    }

    private static int color(String s, int def) {
        try {
            return s == null || s.isEmpty() ? def : Color.parseColor(s.trim());
        } catch (Exception e) {
            return def;
        }
    }

    private static RemoteViews build(Context c, int layout, String name, String state, boolean multi, boolean appAlive) {
        SharedPreferences p = prefs(c);
        boolean light = p.getBoolean("wLight", false);
        int bg = color(p.getString("wBg", ""), 0xFF0B0D12);
        int panel = color(p.getString("wPanel", ""), 0xFF1A1F2A);
        int accent = color(p.getString("wAccent", ""), 0xFF2DE0C9);
        int text = light ? 0xFF1E2533 : 0xFFF2F5FA;

        RemoteViews v = new RemoteViews(c.getPackageName(), layout);
        v.setInt(R.id.w_bg, "setColorFilter", bg);
        v.setInt(R.id.w_bg, "setImageAlpha", 238);
        v.setTextViewText(R.id.w_name, name);
        v.setTextColor(R.id.w_name, text);
        int dot = "connected".equals(state) ? 0xFF2FD27F : "connecting".equals(state) ? 0xFFF5B942 : 0xFF8A93A6;
        v.setInt(R.id.w_dot, "setColorFilter", dot);

        // فتح التطبيق من الاسم
        Intent open = new Intent(c, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        v.setOnClickPendingIntent(R.id.w_head, PendingIntent.getActivity(c, 900, open,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT));

        int[][] keys = layout == R.layout.widget_big ? BIG_KEYS : SMALL_KEYS;
        for (int[] k : keys) {
            int id = k[0], code = k[1];
            v.setOnClickPendingIntent(id, keyIntent(c, code, appAlive));
            if (id == R.id.w_ok) continue; // TextView: لونه تحت
            if (id == R.id.w_power) v.setInt(id, "setColorFilter", 0xFFE5384A);
            else if (id == R.id.w_play) v.setInt(id, "setColorFilter", accent);
            else v.setInt(id, "setColorFilter", text);
        }
        if (layout == R.layout.widget_big) {
            v.setInt(R.id.w_pad, "setColorFilter", panel);
            v.setInt(R.id.w_okbg, "setColorFilter", accent);
            v.setTextColor(R.id.w_ok, light ? 0xFFFFFFFF : 0xFF0B0D12);
            v.setViewVisibility(R.id.w_swap, multi ? android.view.View.VISIBLE : android.view.View.GONE);
            if (multi) {
                v.setOnClickPendingIntent(R.id.w_swap, keyIntent(c, NEXT_DEVICE, appAlive));
                v.setInt(R.id.w_swap, "setColorFilter", accent);
            }
        }
        return v;
    }

    /** {زرار في الـ layout، KEYCODE}. */
    private static final int[][] SMALL_KEYS = {
            {R.id.w_power, 26}, {R.id.w_vol_down, 25}, {R.id.w_vol_up, 24}, {R.id.w_play, 85}, {R.id.w_home, 3},
    };
    private static final int[][] BIG_KEYS = {
            {R.id.w_power, 26}, {R.id.w_up, 19}, {R.id.w_down, 20}, {R.id.w_left, 21}, {R.id.w_right, 22},
            {R.id.w_ok, 23}, {R.id.w_back, 4}, {R.id.w_home, 3}, {R.id.w_vol_down, 25}, {R.id.w_vol_up, 24},
    };

    private static Intent openIntent(Context c, int code) {
        return new Intent(c, MainActivity.class).setAction(ACTION_OPEN_KEY).putExtra("code", code)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
    }

    private static PendingIntent keyIntent(Context c, int code, boolean appAlive) {
        int rc = 1000 + code;
        if (appAlive) {
            Intent i = new Intent(c, RemoteWidget.class).setAction(ACTION_KEY).putExtra("code", code);
            return PendingIntent.getBroadcast(c, rc, i, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        }
        // التطبيق مقفول: الدوسة بتفتحه في الخلفية (الـ broadcast مش بيقدر يفتح activity في أندرويد الجديد)
        return PendingIntent.getActivity(c, rc, openIntent(c, code),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
