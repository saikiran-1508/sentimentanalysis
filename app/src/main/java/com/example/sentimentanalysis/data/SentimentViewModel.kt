package com.example.sentimentanalysis.data

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

// --- DATA CLASSES ---
data class UserProfile(
    val name: String = "User",
    val email: String = "user@example.com",
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

// --- UI STATES ---
sealed class SentimentState {
    object Idle : SentimentState()
    object Loading : SentimentState()
    data class Success(
        val sentiment: String,
        val input: String,
        val profile: EmotionProfile
    ) : SentimentState()
    data class Error(val message: String) : SentimentState()
}

class SentimentViewModel : ViewModel() {

    // --- STATES ---
    private val _uiState = MutableStateFlow<SentimentState>(SentimentState.Idle)
    val uiState: StateFlow<SentimentState> = _uiState.asStateFlow()

    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private val _sentimentHistory = MutableStateFlow<List<SentimentDataPoint>>(emptyList())
    val sentimentHistory: StateFlow<List<SentimentDataPoint>> = _sentimentHistory.asStateFlow()

    // --- FIREBASE & NETWORK ---
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    // --- AUTH LISTENER ---
    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val user = firebaseAuth.currentUser
        if (user != null) {
            val currentName = if (user.displayName.isNullOrBlank()) "User" else user.displayName!!
            val currentEmail = user.email ?: "No Email"
            val currentPhoto = user.photoUrl?.toString()

            _userProfile.value = UserProfile(
                name = currentName,
                email = currentEmail,
                imageUriString = currentPhoto
            )
            fetchHistory(user.uid)
        } else {
            _userProfile.value = UserProfile()
            _sentimentHistory.value = emptyList()
        }
    }

    init {
        auth.addAuthStateListener(authStateListener)
    }

    override fun onCleared() {
        super.onCleared()
        auth.removeAuthStateListener(authStateListener)
    }

    // --- ANALYZE SENTIMENT ---
    fun analyzeSentiment(inputText: String) {
        if (inputText.isBlank()) {
            _uiState.value = SentimentState.Idle // Reset if empty
            return
        }

        _uiState.value = SentimentState.Loading

        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) { callGroqApi(inputText) }

                val sentiment = extractValue(result, "SENTIMENT: ") ?: "Neutral"
                val profile = EmotionProfile(
                    happiness = extractScore(result, "Happiness="),
                    sadness = extractScore(result, "Sadness="),
                    anger = extractScore(result, "Anger="),
                    fear = extractScore(result, "Fear="),
                    surprise = extractScore(result, "Surprise="),
                    disgust = extractScore(result, "Disgust=")
                )

                _uiState.value = SentimentState.Success(sentiment, inputText, profile)
                saveToFirebase(inputText, sentiment, profile)

            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = SentimentState.Error("Analysis Failed: ${e.message}")
            }
        }
    }

    // --- SAVE TO FIREBASE ---
    private fun saveToFirebase(text: String, sentiment: String, profile: EmotionProfile) {
        val user = auth.currentUser ?: return
        val now = Date()
        val formatterDate = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val formatterTime = SimpleDateFormat("hh:mm a", Locale.getDefault())

        val dataPoint = SentimentDataPoint(
            id = System.currentTimeMillis().toString(),
            text = text,
            sentiment = sentiment,
            profile = profile,
            date = formatterDate.format(now),
            timestamp = formatterTime.format(now)
        )

        viewModelScope.launch(Dispatchers.IO) {
            try {
                firestore.collection("users").document(user.uid).collection("history")
                    .add(dataPoint).await()
                fetchHistory(user.uid)
            } catch (e: Exception) {
                Log.e("Firebase", "Error saving", e)
            }
        }
    }

    private fun fetchHistory(uid: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val snapshot = firestore.collection("users").document(uid).collection("history")
                    .orderBy("id", Query.Direction.DESCENDING).get().await()

                val list = snapshot.documents.mapNotNull { doc ->
                    val profileMap = doc.get("profile") as? Map<String, Long> ?: emptyMap()
                    val profile = EmotionProfile(
                        happiness = profileMap["happiness"]?.toInt() ?: 0,
                        sadness = profileMap["sadness"]?.toInt() ?: 0,
                        anger = profileMap["anger"]?.toInt() ?: 0,
                        fear = profileMap["fear"]?.toInt() ?: 0,
                        surprise = profileMap["surprise"]?.toInt() ?: 0,
                        disgust = profileMap["disgust"]?.toInt() ?: 0
                    )
                    SentimentDataPoint(
                        id = doc.getString("id") ?: "",
                        text = doc.getString("text") ?: "",
                        sentiment = doc.getString("sentiment") ?: "",
                        profile = profile,
                        date = doc.getString("date") ?: "",
                        timestamp = doc.getString("timestamp") ?: ""
                    )
                }
                _sentimentHistory.value = list
            } catch (e: Exception) {
                Log.e("Firebase", "Error fetching", e)
            }
        }
    }

    // --- GROQ API (Fixed Model Name) ---
    private fun callGroqApi(inputText: String): String {
        val apiKey = Secrets.GROQ_API_KEY
        if (apiKey.isBlank()) throw Exception("Missing API Key")

        val url = "https://api.groq.com/openai/v1/chat/completions"
        val prompt = """
            Analyze sentiment: "$inputText".
            Return ONLY a string in this exact format:
            SENTIMENT: [One word: Positive, Negative, or Neutral]
            SCORES: Happiness=[0-100], Sadness=[0-100], Anger=[0-100], Fear=[0-100], Surprise=[0-100], Disgust=[0-100]
        """.trimIndent()

        val jsonBody = JSONObject().apply {
            // *** THIS IS THE CRITICAL FIX: ***
            put("model", "llama-3.3-70b-versatile")
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            })
        }

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body?.string()
                throw Exception("Groq Error ${response.code}: $errorBody")
            }
            val json = JSONObject(response.body?.string() ?: "")
            return json.getJSONArray("choices").getJSONObject(0)
                .getJSONObject("message").getString("content")
        }
    }

    // --- PARSING & HELPERS ---
    private fun extractValue(text: String, prefix: String): String? {
        val startIndex = text.indexOf(prefix)
        if (startIndex == -1) return null
        val endLine = text.indexOf("\n", startIndex)
        val endIndex = if (endLine == -1) text.length else endLine
        return text.substring(startIndex + prefix.length, endIndex).trim()
    }

    private fun extractScore(text: String, label: String): Int {
        val start = text.indexOf(label)
        if (start == -1) return 0
        val endComma = text.indexOf(",", start)
        val endLine = text.indexOf("\n", start)
        var end = if (endComma != -1 && (endLine == -1 || endComma < endLine)) endComma else endLine
        if (end == -1) end = text.length
        return try {
            text.substring(start + label.length, end).replace("]", "").trim().toInt()
        } catch (e: Exception) { 0 }
    }

    // Profile Updates
    fun updateProfile(name: String, email: String) {
        val user = auth.currentUser ?: return
        _userProfile.value = _userProfile.value.copy(name = name)
        user.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(name).build())
    }
    fun setProfileImage(uri: Uri) {
        val user = auth.currentUser ?: return
        _userProfile.value = _userProfile.value.copy(imageUriString = uri.toString())
        user.updateProfile(UserProfileChangeRequest.Builder().setPhotoUri(uri).build())
    }
    fun setProfileEmoji(emoji: String) { _userProfile.value = _userProfile.value.copy(avatarEmoji = emoji) }
    fun generateAiAvatar() { _userProfile.value = _userProfile.value.copy(isGeneratedAvatar = true) }
}