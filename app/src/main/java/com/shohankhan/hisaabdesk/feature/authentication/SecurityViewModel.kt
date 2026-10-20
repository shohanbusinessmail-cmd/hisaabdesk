package com.shohankhan.hisaabdesk.feature.authentication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shohankhan.hisaabdesk.AppContainer
import com.shohankhan.hisaabdesk.core.security.PinManager
import com.shohankhan.hisaabdesk.data.repository.SettingsRepository
import com.shohankhan.hisaabdesk.data.repository.UserSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SecurityUiState(
    val pinSet: Boolean = false,
    val locked: Boolean = true,
    val error: String? = null,
    val message: String? = null,
    val lockSecondsRemaining: Long = 0,
    val attemptsLeft: Int = 6
)

class SecurityViewModel(
    private val container: AppContainer
) : ViewModel() {

    private val pinManager: PinManager = container.pinManager

    private val _ui = MutableStateFlow(SecurityUiState())
    val ui: StateFlow<SecurityUiState> = _ui.asStateFlow()

    val settings: StateFlow<UserSettings> = container.settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserSettings())

    init {
        refresh()
    }

    fun refresh() {
        val state = pinManager.state()
        _ui.value = _ui.value.copy(
            pinSet = state.isSet,
            locked = state.isSet,
            lockSecondsRemaining = pinManager.remainingLockSeconds(),
            attemptsLeft = (6 - state.failedAttempts).coerceAtLeast(0)
        )
    }

    fun createPin(pin: String, confirm: String, onDone: () -> Unit) {
        viewModelScope.launch {
            if (pin != confirm) {
                _ui.value = _ui.value.copy(error = "পিন দুটি মিলছে না")
                return@launch
            }
            if (pin.length !in 4..6 || !pin.all { it in '0'..'9' }) {
                _ui.value = _ui.value.copy(error = "পিন ৪ থেকে ৬ সংখ্যার হতে হবে")
                return@launch
            }
            pinManager.setPin(pin)
            _ui.value = _ui.value.copy(error = null, pinSet = true, locked = false, message = "পিন তৈরি হয়েছে")
            onDone()
        }
    }

    fun verify(pin: String, onUnlocked: () -> Unit) {
        viewModelScope.launch {
            if (pinManager.isLockedOut()) {
                _ui.value = _ui.value.copy(
                    error = "অনেকবার ভুল হয়েছে। ${pinManager.remainingLockSeconds()} সেকেন্ড পর আবার চেষ্টা করুন।"
                )
                return@launch
            }
            if (pinManager.verify(pin)) {
                _ui.value = _ui.value.copy(error = null, locked = false)
                onUnlocked()
            } else {
                val state = pinManager.state()
                _ui.value = _ui.value.copy(
                    error = if (pinManager.isLockedOut()) {
                        "অনেকবার ভুল হয়েছে। ${pinManager.remainingLockSeconds()} সেকেন্ড পর আবার চেষ্টা করুন।"
                    } else {
                        "পিন সঠিক নয়"
                    },
                    attemptsLeft = (6 - state.failedAttempts).coerceAtLeast(0),
                    lockSecondsRemaining = pinManager.remainingLockSeconds()
                )
            }
        }
    }

    fun changePin(current: String, new: String, confirm: String, onDone: () -> Unit) {
        viewModelScope.launch {
            if (new != confirm) {
                _ui.value = _ui.value.copy(error = "পিন দুটি মিলছে না", message = null)
                return@launch
            }
            if (new.length !in 4..6 || !new.all { it in '0'..'9' }) {
                _ui.value = _ui.value.copy(error = "পিন ৪ থেকে ৬ সংখ্যার হতে হবে", message = null)
                return@launch
            }
            if (pinManager.changePin(current, new)) {
                _ui.value = _ui.value.copy(error = null, message = "পিন পরিবর্তন হয়েছে")
                onDone()
            } else {
                _ui.value = _ui.value.copy(error = "বর্তমান পিন সঠিক নয়", message = null)
            }
        }
    }

    fun lockNow() {
        _ui.value = _ui.value.copy(locked = true, error = null)
    }

    fun clearTransient() {
        _ui.value = _ui.value.copy(error = null, message = null)
    }

    fun setLockTimeout(minutes: Int) {
        viewModelScope.launch { container.settingsRepository.setLockTimeoutMinutes(minutes) }
    }

    fun setReminders(enabled: Boolean) {
        viewModelScope.launch { container.settingsRepository.setRemindersEnabled(enabled) }
    }
}
