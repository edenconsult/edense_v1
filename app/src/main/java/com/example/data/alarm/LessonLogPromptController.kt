package com.example.data.alarm

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class LessonLogPrompt(
    val blockId: String,
    val subjectId: String,
    val className: String,
    val sessionDate: String
)

object LessonLogPromptController {
    private val _pendingPrompt = MutableStateFlow<LessonLogPrompt?>(null)
    val pendingPrompt = _pendingPrompt.asStateFlow()

    fun request(prompt: LessonLogPrompt) {
        _pendingPrompt.value = prompt
    }

    fun clear() {
        _pendingPrompt.value = null
    }
}
