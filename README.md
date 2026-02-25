Sentiment Analysis AI App
A sophisticated Android application that records audio, converts it to text, and uses Google Gemini 2.5 Flash to perform deep emotional sentiment analysis. The app tracks six core emotions and provides a visual breakdown via interactive charts.

🚀 Features
Voice-to-Sentiment: Record audio directly in the app to analyze your tone and mood.

Gemini AI Integration: Utilizes state-of-the-art LLM to extract:

Full Transcript

Overall Sentiment (Positive, Negative, Neutral)

Emotion Scores: Happiness, Sadness, Anger, Fear, Surprise, and Disgust.

Visual Analytics: Interactive Pie Charts and Side Bars for a quick glance at emotional data.

AI Avatar Generator: Describe your look, and the AI picks a unique emoji avatar for your profile.

Firebase Backend: * Authentication: Secure Google/Email login.

Firestore: Real-time synchronization of analysis history and user profiles.

Modern UI: Built entirely with Jetpack Compose featuring a sleek "Black & White" dark-mode aesthetic.

🛠️ Technical Stack
Language: Kotlin

UI Framework: Jetpack Compose

AI Model: Google Gemini 1.5 Flash

Database/Auth: Firebase (Firestore & Auth)

Image Loading: Coil

Architecture: MVVM (Model-View-ViewModel)

📸 App Architecture
The app follows the MVVM pattern to ensure a clean separation of concerns:

View (Compose UI): Displays the data and handles user interactions.

ViewModel: Manages the state and communicates with the AI and Firebase.

Model (Data Layer): Handles API calls to Gemini and Firestore data operations.

⚙️ Setup & Installation
1. Prerequisites
Android Studio Ladybug or newer.

A Google AI Studio API Key (for Gemini).

A Firebase Project.

2. Firebase Configuration
Create a project in the Firebase Console.

Add an Android App with your package name (com.example.sentimentanalysis).

Download the google-services.json and place it in the app/ directory.

Enable Email/Password Authentication and Cloud Firestore.

3. API Key Security
Create a Secrets.kt file in your data package:

Kotlin
object Secrets {
    const val GEMINI_API_KEY = "YOUR_API_KEY_HERE"
}
4. Running the App
Clone the repository.

Sync Gradle files.

Build and Run on a physical device or emulator (API Level 24+).

Ensure microphone permissions are granted when prompted.

📈 How It Works
Record: The user holds the microphone button to record their voice.

Processing: The audio file is sent to the Gemini 1.5 Flash model.

Analysis: Gemini returns a structured text response containing the transcript and percentages for 6 emotions.

Visualize: The SentimentViewModel parses the data, updates the DashboardScreen Pie Chart, and saves the entry to Firestore history.
