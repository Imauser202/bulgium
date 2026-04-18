package com.example.bulgium;

import android.content.Context;
import android.content.SharedPreferences;

public class CurrencyPrefs {
    private static final String PREFS_NAME = "bulgium_settings";
    private static final String KEY_CURRENCY_SYMBOL = "currency_symbol";

    public static String getCurrencySymbol(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_CURRENCY_SYMBOL, "₱");
    }

    public static void setCurrencySymbol(Context context, String symbol) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_CURRENCY_SYMBOL, symbol).apply();
    }
}
