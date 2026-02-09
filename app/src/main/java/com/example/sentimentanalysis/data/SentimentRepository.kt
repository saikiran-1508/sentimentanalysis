package com.example.sentimentanalysis.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class SentimentRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Get current User ID to keep data private
    private val userId: String
        get() = auth.currentUser?.uid ?: "guest"

    // --- 1. PROFILE MANAGEMENT (Makes Name Permanent) ---

    // Save Profile (Name, Email, Avatar info)
    suspend fun saveUserProfile(profile: UserProfile) {
        try {
            // Overwrites the document at "users/{userId}" with the new data
            db.collection("users").document(userId).set(profile).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Load Profile
    suspend fun getUserProfile(): UserProfile? {
        return try {
            val doc = db.collection("users").document(userId).get().await()
            doc.toObject(UserProfile::class.java)
        } catch (e: Exception) {
            null
        }
    }

    // --- 2. HISTORY MANAGEMENT ---

    private val historyCollection
        get() = db.collection("users").document(userId).collection("history")

    suspend fun addRecord(record: SentimentDataPoint) {
        try {
            // Save new record
            historyCollection.document(record.id).set(record).await()

            // Enforce Limit of 10
            val snapshot = historyCollection
                .orderBy("timestampLong", Query.Direction.DESCENDING)
                .get().await()

            if (snapshot.size() > 10) {
                // Delete anything older than the 10th item
                val documents = snapshot.documents
                for (i in 10 until documents.size) {
                    documents[i].reference.delete()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getHistory(): List<SentimentDataPoint> {
        return try {
            val snapshot = historyCollection
                .orderBy("timestampLong", Query.Direction.DESCENDING)
                .get().await()
            snapshot.toObjects(SentimentDataPoint::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }
}