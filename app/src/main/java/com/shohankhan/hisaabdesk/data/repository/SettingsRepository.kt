package com.shohankhan.hisaabdesk.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.shohankhan.hisaabdesk.core.formatting.Money
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "hisaabdesk_settings")

data class UserSettings(
    val digitStyle: Money.DigitStyle = Money.DigitStyle.BENGALI,
    val grouping: Money.Grouping = Money.Grouping.SOUTH_ASIAN,
    val isoDates: Boolean = false,
    /** -1 = never, 0 = immediately, >0 minutes. */
    val lockTimeoutMinutes: Int = 0,
    val remindersEnabled: Boolean = true
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val DIGIT_STYLE = stringPreferencesKey("digit_style")
        val GROUPING = stringPreferencesKey("grouping")
        val ISO_DATES = booleanPreferencesKey("iso_dates")
        val LOCK_TIMEOUT = intPreferencesKey("lock_timeout")
        val REMINDERS = booleanPreferencesKey("reminders_enabled")
    }

    val settings: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        UserSettings(
            digitStyle = runCatching { Money.DigitStyle.valueOf(prefs[Keys.DIGIT_STYLE] ?: "BENGALI") }
                .getOrDefault(Money.DigitStyle.BENGALI),
            grouping = runCatching { Money.Grouping.valueOf(prefs[Keys.GROUPING] ?: "SOUTH_ASIAN") }
                .getOrDefault(Money.Grouping.SOUTH_ASIAN),
            isoDates = prefs[Keys.ISO_DATES] ?: false,
            lockTimeoutMinutes = prefs[Keys.LOCK_TIMEOUT] ?: 0,
            remindersEnabled = prefs[Keys.REMINDERS] ?: true
        )
    }

    suspend fun setDigitStyle(style: Money.DigitStyle) {
        context.dataStore.edit { it[Keys.DIGIT_STYLE] = style.name }
    }

    suspend fun setGrouping(grouping: Money.Grouping) {
        context.dataStore.edit { it[Keys.GROUPING] = grouping.name }
    }

    suspend fun setIsoDates(iso: Boolean) {
        context.dataStore.edit { it[Keys.ISO_DATES] = iso }
    }

    suspend fun setLockTimeoutMinutes(minutes: Int) {
        context.dataStore.edit { it[Keys.LOCK_TIMEOUT] = minutes }
    }

    suspend fun setRemindersEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.REMINDERS] = enabled }
    }
}
