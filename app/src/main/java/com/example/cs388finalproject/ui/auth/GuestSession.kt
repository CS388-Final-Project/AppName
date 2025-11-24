package com.example.cs388finalproject.ui.auth

import android.content.Context

object GuestSession {
    private const val PREF = "app_session"
    private const val KEY_GUEST = "is_guest"
    private const val KEY_FIRST_LAUNCH = "is_first_launch"

    fun setGuest(context: Context, value: Boolean) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_GUEST, value)
            .apply()
    }

    fun isGuest(context: Context): Boolean {
        return context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getBoolean(KEY_GUEST, false)
    }

    fun setFirstLaunchDone(context: Context) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_FIRST_LAUNCH, false)
            .apply()
    }

    fun isFirstLaunch(context: Context): Boolean {
        return context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getBoolean(KEY_FIRST_LAUNCH, true)
    }

    fun clearAll(context: Context) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }
}
