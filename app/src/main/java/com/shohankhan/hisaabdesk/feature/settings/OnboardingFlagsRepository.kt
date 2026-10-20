package com.shohankhan.hisaabdesk.feature.settings

import android.content.Context
import android.content.SharedPreferences

/** Tiny flag store for first-run completion (kept separate from DataStore settings). */
class OnboardingFlags(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("hisaabdesk_flags", Context.MODE_PRIVATE)

    var onboardingComplete: Boolean
        get() = prefs.getBoolean("onboarding_complete", false)
        set(value) = prefs.edit().putBoolean("onboarding_complete", value).apply()
}
