package com.mobile.bible.kjv.ui.vm

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

data class ReadUiState(
    val progressToday: Int = 0,
    val trackTitle: String = "PERSONALIZED DEVOTIONAL.3 MIN",
    val currentTime: String = "00:00",
    val totalTime: String = "02:32",
    val progress: Int = 0,
    val quoteText: String = "Dear Heavenly Father, we thank You that Your grace remains even in judgment. Lord, there are still many \"serpents\" entangling our lives—pride, lies, fear, addiction. Please let us hear Your firm yet gentle voice in Genesis 3:15: \"I have prepared a Victor for you.\" May we no longer fight the serpent in our own strength today, but look to the Savior who has already crushed the serpent's head. In Jesus' name, Amen.",
    val stepId: Int? = null,
    val verseReference: String = "",
    val taskOrder: Int = 0
)

class ReadViewModel(application: android.app.Application) : androidx.lifecycle.AndroidViewModel(application) {
    private val repository = com.mobile.bible.kjv.data.repository.KjvRepository(application)
    private val _uiState = MutableStateFlow(ReadUiState())
    val uiState: StateFlow<ReadUiState> = _uiState

    private var dailySteps: List<com.mobile.bible.kjv.data.model.DevotionalStep> = emptyList()
    private var currentStepIndex: Int = -1

    init {
        viewModelScope.launch {
            repository.getDailyProgressFlow().collect { progress ->
                _uiState.value = _uiState.value.copy(progressToday = progress)
            }
        }
        loadTodaysPlan()
    }

    fun setStepId(stepId: Int?, quoteText: String?, trackTitle: String?, verseReference: String?) {
        val fallbackOrder = ((stepId ?: 1) - 1).coerceAtLeast(0)
        _uiState.value = _uiState.value.copy(
            stepId = stepId,
            quoteText = quoteText ?: _uiState.value.quoteText,
            trackTitle = trackTitle ?: _uiState.value.trackTitle,
            verseReference = verseReference ?: _uiState.value.verseReference,
            taskOrder = fallbackOrder
        )

        if (dailySteps.isNotEmpty() && stepId != null) {
            val index = dailySteps.indexOfFirst { it.stepId == stepId }
            if (index != -1) {
                currentStepIndex = index
                _uiState.value = _uiState.value.copy(taskOrder = currentStepIndex)
            }
        }

        if (stepId != null) {
            markStepAsComplete(stepId)
        }
    }

    fun markStepAsComplete(stepId: Int) {
        viewModelScope.launch {
            repository.markDailyStepComplete(stepId)
        }
    }

    fun nextTask(): Job {
        val currentStepId = _uiState.value.stepId

        return viewModelScope.launch {
            // Mark current step as complete
            if (currentStepId != null) {
                repository.markDailyStepComplete(currentStepId)
            }

            // Navigate to next step if plan data is loaded
            if (dailySteps.isNotEmpty()) {
                val nextIndex = (currentStepIndex + 1) % dailySteps.size
                updateToStep(nextIndex)

                // Mark the newly entered step as complete as well
                val newStepId = _uiState.value.stepId
                if (newStepId != null) {
                    repository.markDailyStepComplete(newStepId)
                }
            }

            // Explicitly refresh progress to ensure UI reflects the latest state
            val completedCount = repository.getCompletedStepIds().size
            val total = repository.getDailyTotalSteps()
            val progress = if (total > 0) (completedCount.toFloat() / total * 100).toInt() else 0
            _uiState.value = _uiState.value.copy(progressToday = progress)
        }
    }

    private fun updateToStep(index: Int) {
        if (index !in dailySteps.indices) return

        currentStepIndex = index
        val step = dailySteps[index]

        val quoteText = when (step.stepId) {
            1 -> step.content.verseText ?: ""
            else -> step.content.text ?: ""
        }

        val title = "${step.entryTitle} (${step.entryDuration})"
        val verseRef = step.content.verseReference ?: ""

        _uiState.value = _uiState.value.copy(
            stepId = step.stepId,
            quoteText = quoteText,
            trackTitle = title,
            verseReference = verseRef,
            taskOrder = currentStepIndex
        )
    }

    private fun loadTodaysPlan() {
        viewModelScope.launch {
            try {
                val items = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    val daysSinceInstall = calculateDaysSinceInstall()
                    val allDays = parseDevotionalPlan()

                    val planIndex = if (allDays.isNotEmpty()) {
                        (daysSinceInstall % allDays.size).toInt()
                    } else {
                        0
                    }

                    val dayData = allDays.find { it.index == planIndex } ?: allDays.firstOrNull()
                    dayData?.steps ?: emptyList()
                }
                dailySteps = items

                val currentId = _uiState.value.stepId
                if (currentId != null && currentStepIndex == -1) {
                    val index = dailySteps.indexOfFirst { it.stepId == currentId }
                    if (index != -1) {
                        currentStepIndex = index
                        _uiState.value = _uiState.value.copy(taskOrder = currentStepIndex)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun getTodayStartMillis(): Long {
        return java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun calculateDaysSinceInstall(): Long {
        val startDate = if (com.mobile.bible.kjv.prefs.DailyVerseSettings.startDate == 0L) {
            getTodayStartMillis()
        } else {
            com.mobile.bible.kjv.prefs.DailyVerseSettings.startDate
        }

        val today = getTodayStartMillis()
        val diff = today - startDate
        return if (diff < 0) 0 else java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diff)
    }

    private fun parseDevotionalPlan(): List<com.mobile.bible.kjv.data.model.DevotionalDay> {
        val jsonString = getApplication<android.app.Application>().assets.open("daily_devotional_plan.json").bufferedReader().use { it.readText() }
        val type = object : com.google.gson.reflect.TypeToken<List<com.mobile.bible.kjv.data.model.DevotionalDay>>() {}.type
        return com.google.gson.Gson().fromJson(jsonString, type)
    }
}
