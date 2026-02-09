package com.example.sentimentanalysis.data

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

// --- DATA MODELS ---

data class EmotionProfile(
    val happiness: Int = 0,
    val sadness: Int = 0,
    val anger: Int = 0,
    val fear: Int = 0,
    val surprise: Int = 0,
    val disgust: Int = 0
) {
    constructor() : this(0,0,0,0,0,0)
}

data class SentimentDataPoint(
    val id: String = UUID.randomUUID().toString(),
    val label: String = "",
    val text: String = "",
    val timestamp: String = "",
    val date: String = "",
    val timestampLong: Long = 0L,
    val profile: EmotionProfile = EmotionProfile()
) {
    constructor() : this(UUID.randomUUID().toString(), "", "", "", "", 0L, EmotionProfile())
}

// NOTE: imageUriString is what Firestore saves.
data class UserProfile(
    val name: String = "User",
    val email: String = "user@example.com",
    val imageUriString: String? = null,
    val isGeneratedAvatar: Boolean = false,
    val avatarEmoji: String? = null
) {
    constructor() : this("User", "user@example.com", null, false, null)
}

class SentimentViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<SentimentState>(SentimentState.Idle)
    val uiState = _uiState.asStateFlow()

    private val _sentimentHistory = MutableStateFlow<List<SentimentDataPoint>>(emptyList())
    val sentimentHistory = _sentimentHistory.asStateFlow()

    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile = _userProfile.asStateFlow()

    private val repository = SentimentRepository()
    private var feedbackContext = ""

    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = "YOUR_GEMINI_API_KEY_HERE"
    )

    init {
        // Load data on start (Restores permanent name/image)
        loadUserData()
    }

    private fun loadUserData() {
        viewModelScope.launch {
            val onlineProfile = repository.getUserProfile()
            if (onlineProfile != null) {
                _userProfile.value = onlineProfile
            }
            val onlineHistory = repository.getHistory()
            _sentimentHistory.value = onlineHistory
        }
    }

    // --- SAVING LOGIC (Connects to Firebase) ---

    fun updateProfile(name: String, email: String) {
        val newProfile = _userProfile.value.copy(name = name, email = email)
        _userProfile.value = newProfile
        saveProfileToBackend(newProfile)
    }

    fun setProfileImage(uri: Uri?) {
        val newProfile = _userProfile.value.copy(
            imageUriString = uri?.toString(),
            isGeneratedAvatar = false,
            avatarEmoji = null
        )
        _userProfile.value = newProfile
        saveProfileToBackend(newProfile)
    }

    fun generateAiAvatar() {
        val newProfile = _userProfile.value.copy(
            imageUriString = null,
            isGeneratedAvatar = true,
            avatarEmoji = null
        )
        _userProfile.value = newProfile
        saveProfileToBackend(newProfile)
    }

    fun setProfileEmoji(emoji: String) {
        val newProfile = _userProfile.value.copy(
            imageUriString = null,
            isGeneratedAvatar = false,
            avatarEmoji = emoji
        )
        _userProfile.value = newProfile
        saveProfileToBackend(newProfile)
    }

    fun usePresetAvatar() {
        val newProfile = _userProfile.value.copy(imageUriString = null, isGeneratedAvatar = false, avatarEmoji = null)
        _userProfile.value = newProfile
        saveProfileToBackend(newProfile)
    }

    private fun saveProfileToBackend(profile: UserProfile) {
        viewModelScope.launch {
            repository.saveUserProfile(profile)
        }
    }

    // --- ANALYSIS LOGIC ---

    fun teachAI(originalText: String, correctEmotion: String) {
        feedbackContext += "\n- When user says '$originalText', emotion is '$correctEmotion'."
    }

    fun analyzeSentiment(text: String) {
        if (text.isBlank()) return
        _uiState.value = SentimentState.Loading

        viewModelScope.launch {
            try {
                val prompt = """
                    Analyze tone: "$text". 
                    Corrections: $feedbackContext
                    Breakdown: Happiness, Sadness, Anger, Fear, Surprise, Disgust. 0-100%.
                    Respond: DOMINANT|HAP|SAD|ANG|FEAR|SUR|DIS
                """.trimIndent()

                val response = generativeModel.generateContent(prompt)
                val rawText = response.text?.trim() ?: "Neutral|0|0|0|0|0|0"

                val parts = rawText.split("|")
                val dominantLabel = parts.getOrElse(0) { "Neutral" }
                val hap = parts.getOrElse(1) { "0" }.toIntOrNull() ?: 0
                val sad = parts.getOrElse(2) { "0" }.toIntOrNull() ?: 0
                val ang = parts.getOrElse(3) { "0" }.toIntOrNull() ?: 0
                val fea = parts.getOrElse(4) { "0" }.toIntOrNull() ?: 0
                val sur = parts.getOrElse(5) { "0" }.toIntOrNull() ?: 0
                val dis = parts.getOrElse(6) { "0" }.toIntOrNull() ?: 0

                val profile = EmotionProfile(hap, sad, ang, fea, sur, dis)
                val now = Date()

                val newPoint = SentimentDataPoint(
                    label = dominantLabel,
                    text = text,
                    timestamp = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now),
                    date = SimpleDateFormat("MMM dd", Locale.getDefault()).format(now),
                    timestampLong = now.time,
                    profile = profile
                )

                var currentList = _sentimentHistory.value.toMutableList()
                currentList.add(0, newPoint)
                if (currentList.size > 10) currentList = currentList.take(10).toMutableList()
                _sentimentHistory.value = currentList

                // Save record to DB
                repository.addRecord(newPoint)

                _uiState.value = SentimentState.Success(dominantLabel, text, profile)

            } catch (e: Exception) {
                _uiState.value = SentimentState.Error("AI Error: ${e.message}")
            }
        }
    }
}

sealed class SentimentState {
    object Idle : SentimentState()
    object Loading : SentimentState()
    data class Success(val sentiment: String, val input: String, val profile: EmotionProfile) : SentimentState()
    data class Error(val message: String) : SentimentState()
}