package com.example.cs388finalproject.data
import android.net.Uri
/*
import com.google.firebase.firestore.FirebaseFirestore

class PostRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {
    suspend fun uploadPhotoAndCreatePost(localUri: Uri, post: Post): String = suspendCancellableCoroutine { cont ->
        val file = storage.reference.child("users/${post.uid}/photos/${UUID.randomUUID()}.jpg")
        file.putFile(localUri).continueWithTask { file.downloadUrl }
            .onSuccessTask { uri ->
                val toSave = post.copy(imagePath = file.path) // or uri.toString()
                db.collection("posts").add(toSave)
            }
            .addOnSuccessListener { cont.resume(it.id) {} }
            .addOnFailureListener { cont.resumeWithException(it) }
    }

    fun feed(limit: Long = 50) =
        db.collection("posts").orderBy("createdAt", Query.Direction.DESCENDING).limit(limit)
}

 */