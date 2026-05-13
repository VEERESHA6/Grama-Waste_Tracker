package com.example.gwt.repository

import android.net.Uri
import com.example.gwt.model.BlackspotReport
import com.example.gwt.model.TractorLocation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class TractorRepository {
    private val database = FirebaseDatabase.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private val tractorRef = database.getReference("tractors")
    private val blackspotRef = database.getReference("blackspots")

    fun getTractorLocation(tractorId: String): Flow<TractorLocation?> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val location = snapshot.getValue(TractorLocation::class.java)
                trySend(location)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        tractorRef.child(tractorId).addValueEventListener(listener)
        awaitClose { tractorRef.child(tractorId).removeEventListener(listener) }
    }

    fun getAllBlackspots(): Flow<List<BlackspotReport>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val reports = snapshot.children.mapNotNull { it.getValue(BlackspotReport::class.java) }
                trySend(reports)
            }
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        blackspotRef.addValueEventListener(listener)
        awaitClose { blackspotRef.removeEventListener(listener) }
    }

    suspend fun updateBlackspotStatus(reportId: String, status: String, comment: String): Boolean {
        return try {
            val updates = mapOf(
                "status" to status,
                "adminComment" to comment
            )
            blackspotRef.child(reportId).updateChildren(updates).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun updateTractorLocation(location: TractorLocation) {
        tractorRef.child(location.tractorId).setValue(location)
    }

    suspend fun uploadBlackspotReport(uri: Uri, lat: Double, lng: Double, description: String): Boolean {
        return try {
            if (auth.currentUser == null) {
                auth.signInAnonymously().await()
            }

            val reportId = blackspotRef.push().key ?: return false
            val storageRef = storage.reference.child("blackspots/$reportId.jpg")
            
            storageRef.putFile(uri).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()
            
            val report = BlackspotReport(
                id = reportId,
                latitude = lat,
                longitude = lng,
                imageUrl = downloadUrl,
                description = description,
                reporterId = auth.currentUser?.uid ?: "Anonymous"
            )
            
            blackspotRef.child(reportId).setValue(report).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
