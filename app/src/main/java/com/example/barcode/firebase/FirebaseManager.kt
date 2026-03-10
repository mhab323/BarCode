package com.example.barcode.firebase

import com.example.barcode.data.Event
import com.example.barcode.data.Order
import com.example.barcode.model.Cocktail
import com.example.barcode.model.User
import com.example.barcode.utils.UserManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object FirebaseManager {

    val auth: FirebaseAuth
        get() = FirebaseAuth.getInstance()

    val db: FirebaseFirestore
        get() = FirebaseFirestore.getInstance()

    var currentUserRole: String? = null

    fun saveEvent(eventData: HashMap<String, Any>, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val newEventRef = db.collection("events").document()

        eventData["eventId"] = newEventRef.id

        newEventRef.set(eventData)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e) }

    }
    fun saveCocktail(cocktail: Cocktail, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val newDocRef = db.collection("cocktails").document()
        cocktail.cocktailId = newDocRef.id

        newDocRef.set(cocktail)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }
    fun deleteCocktail(cocktailId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        db.collection("cocktails").document(cocktailId)
            .delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }
    fun updateCocktail(cocktail: Cocktail, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        db.collection("cocktails").document(cocktail.cocktailId)
            .set(cocktail)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }
    fun listenToBartenderCocktails(bartenderId: String, onSuccess: (List<Cocktail>) -> Unit, onFailure: (Exception) -> Unit): com.google.firebase.firestore.ListenerRegistration {
        return db.collection("cocktails")
            .whereEqualTo("bartenderId", bartenderId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onFailure(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val cocktailList = mutableListOf<Cocktail>()
                    for (document in snapshot.documents) {
                        val cocktail = document.toObject(Cocktail::class.java)
                        if (cocktail != null) {
                            cocktail.cocktailId = document.id
                            cocktailList.add(cocktail)
                        }
                    }
                    onSuccess(cocktailList)
                }
            }
    }
    fun listenToHostEvents(hostId: String, onSuccess: (List<Event>) -> Unit, onFailure: (Exception) -> Unit) {
        db.collection("events")
            .whereEqualTo("hostId", hostId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onFailure(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val eventsList = mutableListOf<Event>()
                    for (document in snapshot.documents) {
                        val event = document.toObject(Event::class.java)
                        if (event != null) {
                            event.eventId = document.id
                            eventsList.add(event)
                        }
                    }
                    onSuccess(eventsList)
                }
            }
    }
    fun getBartenderByShareCode(code: String, onSuccess: (User) -> Unit, onFailure: (Exception) -> Unit) {
        db.collection("users")
            .whereEqualTo("shareCode", code)
            .whereEqualTo("role", "admin")
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    onFailure(Exception("Invalid Bartender Code!"))
                    return@addOnSuccessListener
                }

                val bartender = documents.documents[0].toObject(User::class.java)
                if (bartender != null) {
                    onSuccess(bartender)
                } else {
                    onFailure(Exception("Error loading profile"))
                }
            }
            .addOnFailureListener { onFailure(it) }
    }

    fun listenToBartenderEvents(bartenderId: String, onSuccess: (List<Event>) -> Unit, onFailure: (Exception) -> Unit): com.google.firebase.firestore.ListenerRegistration {
        return db.collection("events")
            .whereArrayContains("bartenderIds", bartenderId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onFailure(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val eventsList = mutableListOf<Event>()
                    for (document in snapshot.documents) {
                        val event = document.toObject(Event::class.java)
                        if (event != null) {
                            event.eventId = document.id
                            eventsList.add(event)
                        }
                    }
                    onSuccess(eventsList)
                }
            }
    }
    fun uploadCocktailImage(imageUri: android.net.Uri, onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit) {
        val filename = java.util.UUID.randomUUID().toString() + ".jpg"
        val storageRef = com.google.firebase.storage.FirebaseStorage.getInstance().reference.child("cocktail_images/$filename")

        storageRef.putFile(imageUri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    onSuccess(downloadUri.toString())
                }.addOnFailureListener { e -> onFailure(e) }
            }
            .addOnFailureListener { e -> onFailure(e) }
    }

    fun fetchAndCacheCurrentUser(onSuccess: (User) -> Unit, onFailure: (Exception) -> Unit) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onFailure(Exception("No user logged in"))
            return
        }
        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                val user = document.toObject(User::class.java)
                if (user != null) {
                    UserManager.currentUser = user
                    onSuccess(user)
                } else {
                    onFailure(Exception("User data is empty"))
                }
            }
            .addOnFailureListener { e -> onFailure(e) }
    }
    fun generateAndSaveShareCode(onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onFailure(Exception("No user logged in"))
            return
        }
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        val newCode = (1..6).map { chars.random() }.joinToString("")

        db.collection("users").document(uid)
            .update("shareCode", newCode)
            .addOnSuccessListener {
                UserManager.currentUser?.shareCode = newCode
                onSuccess(newCode)
            }
            .addOnFailureListener { e -> onFailure(e) }
    }

    //######################################################################################
    //Real time database
    //######################################################################################

    fun placeLiveOrder(order: Order, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val rtdb = com.google.firebase.database.FirebaseDatabase.getInstance("https://barcode-app-71522-default-rtdb.europe-west1.firebasedatabase.app/").reference

        val newOrderRef = rtdb.child("orders").child(order.eventId).push()
        order.orderId = newOrderRef.key ?: ""

        newOrderRef.setValue(order)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e) }
    }

}