package com.example.sentimentanalysis.data

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// --- DATA CLASSES ---
data class UserProfile(
    val name: String = "User",
    val email: String = "",
    val imageUriString: String? = null,
    val avatarEmoji: String? = null,
    val isGeneratedAvatar: Boolean = false
)

data class EmotionProfile(
    val happiness: Int = 0,
    val sadness: Int = 0,
    val anger: Int = 0,
    val fear: Int = 0,
    val surprise: Int = 0,
    val disgust: Int = 0
)

data class SentimentDataPoint(
    val id: String = "",
    val text: String = "",
    val sentiment: String = "",
    val profile: EmotionProfile = EmotionProfile(),
    val timestamp: String = "",
    val date: String = ""
)

sealed class SentimentState {
    object Idle : SentimentState()
    object Recording : SentimentState()
    object Loading : SentimentState()
    data class Success(val sentiment: String, val input: String, val profile: EmotionProfile) : SentimentState()
    data class Error(val message: String) : SentimentState()
}

class SentimentViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<SentimentState>(SentimentState.Idle)
    val uiState: StateFlow<SentimentState> = _uiState.asStateFlow()

    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private val _sentimentHistory = MutableStateFlow<List<SentimentDataPoint>>(emptyList())
    val sentimentHistory: StateFlow<List<SentimentDataPoint>> = _sentimentHistory.asStateFlow()

    private val _lastAudioPath = MutableStateFlow<String?>(null)
    val lastAudioPath: StateFlow<String?> = _lastAudioPath.asStateFlow()

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private var audioRecorder: AudioRecorder? = null

    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = Secrets.GEMINI_API_KEY
    )

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                _userProfile.value = _userProfile.value.copy(
                    name = user.displayName ?: "User",
                    email = user.email ?: ""
                )
                viewModelScope.launch {
                    fetchUserProfile(user.uid)
                    fetchHistory(user.uid)
                }
            }
        }
    }

    private suspend fun fetchUserProfile(uid: String) {
        try {
            val doc = firestore.collection("users").document(uid).get().await()
            doc.toObject(UserProfile::class.java)?.let { _userProfile.value = it }
        } catch (e: Exception) {
            Log.e("SentimentViewModel", "Fetch Error: ${e.message}")
        }
    }

    fun resetState() { _uiState.value = SentimentState.Idle }

    fun startRecording(context: Context) {
        try {
            if (audioRecorder == null) audioRecorder = AudioRecorder(context)
            audioRecorder?.startRecording("audio_${System.currentTimeMillis()}.mp4")
            _uiState.value = SentimentState.Recording
        } catch (e: Exception) {
            _uiState.value = SentimentState.Error("Rec Error: ${e.message}")
        }
    }

    fun stopAndAnalyze() {
        _uiState.value = SentimentState.Loading
        val file = audioRecorder?.stopRecording()
        if (file != null && file.exists()) {
            _lastAudioPath.value = file.absolutePath
            analyzeAudio(file)
        } else {
            _uiState.value = SentimentState.Error("Recording reference missing")
        }
    }

    private fun analyzeAudio(file: File) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = generativeModel.generateContent(content {
                    blob("audio/mp4", file.readBytes())
                    text("Analyze audio tone. Return: TRANSCRIPT, SENTIMENT, and SCORES for Happiness, Sadness, Anger, Fear, Surprise, Disgust. Use format Happiness=20.")
                })
                val out = response.text ?: ""

                val profile = EmotionProfile(
                    happiness = extractScore(out, "Happiness"),
                    sadness = extractScore(out, "Sadness"),
                    anger = extractScore(out, "Anger"),
                    fear = extractScore(out, "Fear"),
                    surprise = extractScore(out, "Surprise"),
                    disgust = extractScore(out, "Disgust")
                )

                _uiState.value = SentimentState.Success(
                    extractValue(out, "SENTIMENT: ") ?: "Neutral",
                    extractValue(out, "TRANSCRIPT: ") ?: "Audio Input",
                    profile
                )
                saveToFirebase(
                    extractValue(out, "TRANSCRIPT: ") ?: "Audio",
                    extractValue(out, "SENTIMENT: ") ?: "Neutral",
                    profile
                )
            } catch (e: Exception) {
                _uiState.value = SentimentState.Error("AI Error: ${e.message}")
            }
        }
    }

    private fun extractValue(text: String, prefix: String) = text.indexOf(prefix).takeIf { it != -1 }?.let {
        text.substring(it + prefix.length, text.indexOf("\n", it).takeIf { i -> i != -1 } ?: text.length).trim()
    }

    private fun extractScore(text: String, label: String): Int {
        val pattern = Regex("$label[\\s=:]*(\\d+)", RegexOption.IGNORE_CASE)
        return pattern.find(text)?.groupValues?.get(1)?.toInt() ?: 0
    }

    private fun saveToFirebase(text: String, sentiment: String, profile: EmotionProfile) {
        val user = auth.currentUser ?: return
        val now = Date()
        val data = SentimentDataPoint(
            id = System.currentTimeMillis().toString(),
            text = text,
            sentiment = sentiment,
            profile = profile,
            date = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(now),
            timestamp = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(now)
        )

        viewModelScope.launch {
            try {
                firestore.collection("users")
                    .document(user.uid)
                    .collection("history")
                    .document(data.id)
                    .set(data)
                    .await()

                fetchHistory(user.uid)
            } catch (e: Exception) {
                Log.e("FirestoreError", "Save failed: ${e.message}")
            }
        }
    }

    private fun fetchHistory(uid: String) {
        viewModelScope.launch {
            try {
                val snap = firestore.collection("users")
                    .document(uid)
                    .collection("history")
                    .orderBy("id", Query.Direction.DESCENDING)
                    .limit(10)
                    .get()
                    .await()

                _sentimentHistory.value = snap.documents.mapNotNull { it.toObject(SentimentDataPoint::class.java) }
            } catch (e: Exception) {
                Log.e("FirestoreError", "Fetch failed: ${e.message}")
            }
        }
    }

    fun updateProfile(name: String, email: String) {
        val user = auth.currentUser ?: return
        viewModelScope.launch {
            try {
                user.updateProfile(userProfileChangeRequest { displayName = name }).await()
                val updated = _userProfile.value.copy(name = name, email = email)
                _userProfile.value = updated
                firestore.collection("users").document(user.uid).set(updated, SetOptions.merge()).await()
            } catch (e: Exception) { Log.e("VM", "Update Error: ${e.message}") }
        }
    }

    fun setProfileImage(uri: Uri) {
        val user = auth.currentUser ?: return
        viewModelScope.launch {
            val updated = _userProfile.value.copy(imageUriString = uri.toString(), avatarEmoji = null, isGeneratedAvatar = false)
            _userProfile.value = updated
            firestore.collection("users").document(user.uid).set(updated, SetOptions.merge()).await()
        }
    }

    fun setProfileEmoji(emoji: String) {
        val user = auth.currentUser ?: return
        viewModelScope.launch {
            val updated = _userProfile.value.copy(avatarEmoji = emoji, imageUriString = null, isGeneratedAvatar = false)
            _userProfile.value = updated
            firestore.collection("users").document(user.uid).set(updated, SetOptions.merge()).await()
        }
    }

    fun generateAiAvatar(prompt: String) {
        val user = auth.currentUser ?: return
        viewModelScope.launch {
            try {
                _uiState.value = SentimentState.Loading
                val response = generativeModel.generateContent("Pick ONE emoji character for: $prompt. Return ONLY emoji.")
                val emoji = response.text?.trim() ?: "🤖"
                val updated = _userProfile.value.copy(avatarEmoji = emoji, imageUriString = null, isGeneratedAvatar = true)
                _userProfile.value = updated
                firestore.collection("users").document(user.uid).set(updated, SetOptions.merge()).await()
                _uiState.value = SentimentState.Idle
            } catch (e: Exception) {
                _uiState.value = SentimentState.Error("AI Fail")
            }
        }
    }
}