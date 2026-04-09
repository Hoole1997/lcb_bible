package com.mobile.bible.kjv.ui.vm

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

data class PlayerUiState(
    val progressToday: Int = 0,
    val trackTitle: String = "YOUR VERSE ·1 MIN",
    val currentTime: String = "00:00",
    val totalTime: String = "02:32",
    val progress: Int = 0,
    val quoteText: String = "Dear Heavenly Father, we thank You that Your grace remains even in judgment. Lord, there are still many \"serpents\" entangling our lives—pride, lies, fear, addiction. Please let us hear Your firm yet gentle voice in Genesis 3:15: \"I have prepared a Victor for you.\" May we no longer fight the serpent in our own strength today, but look to the Savior who has already crushed the serpent's head. In Jesus' name, Amen.",
    val stepId: Int? = null,
    val verseReference: String = ""
)

class PlayerViewModel(application: android.app.Application) : androidx.lifecycle.AndroidViewModel(application) {
    private val repository = com.mobile.bible.kjv.data.repository.KjvRepository(application)
    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState

    // Daily Plan Data
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
        // Initial setup from Intent
        // Only if we haven't already set up the state or if it's a fresh start
        if (_uiState.value.stepId == null || _uiState.value.stepId != stepId) {
             _uiState.value = _uiState.value.copy(
                stepId = stepId,
                quoteText = quoteText ?: _uiState.value.quoteText,
                trackTitle = trackTitle ?: _uiState.value.trackTitle,
                verseReference = verseReference ?: _uiState.value.verseReference
            )
            
            // Sync current index with loaded steps
            if (dailySteps.isNotEmpty() && stepId != null) {
                val index = dailySteps.indexOfFirst { it.stepId == stepId }
                if (index != -1) {
                    currentStepIndex = index
                }
            }
        }
    }

    fun markStepAsComplete(stepId: Int) {
        viewModelScope.launch {
            repository.markDailyStepComplete(stepId)
        }
    }

    fun nextTask() {
        if (dailySteps.isEmpty()) return
        
        val nextIndex = (currentStepIndex + 1) % dailySteps.size
        updateToStep(nextIndex)
    }

    fun previousTask() {
        if (dailySteps.isEmpty()) return

        val prevIndex = if (currentStepIndex - 1 < 0) dailySteps.size - 1 else currentStepIndex - 1
        updateToStep(prevIndex)
    }

    private fun updateToStep(index: Int) {
        if (index !in dailySteps.indices) return
        
        currentStepIndex = index
        val step = dailySteps[index]
        
        // Map step data to UI state
        // Logic reused from TodayViewModel mapping
        val quoteText = when(step.stepId) {
            1 -> step.content.verseText ?: ""
            else -> step.content.text ?: ""
        }
        
        val subtitle = when(step.stepId) {
            1 -> step.content.verseReference ?: ""
            else -> step.content.title ?: ""
        }
        
        val title = "${step.entryTitle} (${step.entryDuration})"
        val verseRef = step.content.verseReference ?: ""

        _uiState.value = _uiState.value.copy(
            stepId = step.stepId,
            quoteText = quoteText,
            trackTitle = title,
            verseReference = verseRef,
            // Reset progress for new track if needed, or keep it if we want to retain "today's progress"
            // For the player UI, we likely want to reset current playback time/progress
            currentTime = "00:00",
            progress = 0
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
                
                // If we already have a stepId set, find its index now
                val currentId = _uiState.value.stepId
                if (currentId != null && currentStepIndex == -1) {
                    val index = dailySteps.indexOfFirst { it.stepId == currentId }
                    if (index != -1) {
                        currentStepIndex = index
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    // Helper functions duplicated from TodayViewModel to avoid tight coupling or refactoring risks
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
             getTodayStartMillis() // Fallback if not set, though TodayViewModel should have set it
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