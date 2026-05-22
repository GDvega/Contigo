package com.cuidavoz.mobile.reminders

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ReminderLaunchState {
    private val _prompt = MutableStateFlow<ReminderPrompt?>(null)
    val prompt: StateFlow<ReminderPrompt?> = _prompt.asStateFlow()

    fun showPrompt(prompt: ReminderPrompt) {
        _prompt.value = prompt
    }

    fun clearPrompt() {
        _prompt.value = null
    }
}
