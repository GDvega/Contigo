package com.cuidavoz.mobile.data.firebase

import com.google.firebase.firestore.DocumentSnapshot

data class FirestorePage(
    val documents: List<DocumentSnapshot>,
    val nextCursor: DocumentSnapshot?,
)
