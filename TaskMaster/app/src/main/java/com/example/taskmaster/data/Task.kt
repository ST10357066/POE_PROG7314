package com.example.taskmaster.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

@Entity(tableName = "tasks")
data class Task(
    // The @DocumentId annotation tells Firestore to automatically
    // populate this field with the document's ID.
    @PrimaryKey @DocumentId var id: String = "",

    val userId: String = "",
    val title: String = "",
    val description: String? = null, // Nullable types are already optional
    @get:PropertyName("isDone")
    val isDone: Boolean = false,
    val dueDate: String? = null,
    val createdAt: String = "",
    val isSynced: Boolean = true
)