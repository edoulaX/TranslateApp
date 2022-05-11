package com.github.sdpsharelook.storage

import com.github.sdpsharelook.Word
import com.github.sdpsharelook.authorization.AuthProvider
import com.google.firebase.database.*
import com.google.firebase.database.ktx.getValue
import com.google.gson.Gson
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class RTDBWordListRepository @Inject constructor(
    private val firebaseDatabase: FirebaseDatabase,
    private val auth: AuthProvider
) : IRepository<List<@JvmSuppressWildcards Word>> {

    private val reference: DatabaseReference by lazy { firebaseDatabase.getReference("wordlists") }


    /**
     * Gets an asynchronous data stream any updated [List] of [String]s
     *
     * @param name format: "path/name"
     * @return [Flow] of changes in the database at [name]
     */
    override fun flow(name: String): Flow<Result<List<Word>?>> =
        callbackFlow {
            val fireListener = object : ChildEventListener {
                val wordList = mutableListOf<Word>()
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    val word = Gson().fromJson(snapshot.value.toString(), Word::class.java)
                    wordList.add(wordList.size, word)
                    trySendBlocking(Result.success(wordList))
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                    val list = listOfNotNull(snapshot.getValue<Word>())
                    trySendBlocking(Result.success(list))
                }

                override fun onChildRemoved(snapshot: DataSnapshot) {
                    val list: List<Word> = listOfNotNull(snapshot.getValue<Word>())
                    trySendBlocking(Result.success(list))
                }

                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}


                override fun onCancelled(error: DatabaseError) {
                    val result = Result.failure<List<Word>>(error.toException())
                    trySendBlocking(result)
                }

            }
            databaseReference(name).addChildEventListener(fireListener)
            awaitClose {
                databaseReference(name).removeEventListener(fireListener)
            }
        }

    private fun getUserReference(): DatabaseReference {
        val user = auth.currentUser
        //TODO: handle when user not logged
        if (user != null) {
            return firebaseDatabase.getReference("users/" + user.uid + "/words")
        }
        return firebaseDatabase.getReference("users/guest/words")
    }

    /**
     * Create permanent repository entry
     *
     * @param name identifier of entity
     * @param entity Entity
     */
    override suspend fun insert(name: String, entity: List<Word>) {
        entity.forEach {
            getUserReference().child(it.uid).setValue(Gson().toJson(it).toString())
                .addOnSuccessListener {
                }
        }
    }

    /**
     * Don't use
     */
    override suspend fun read(name: String): List<Word> {
        throw UnsupportedOperationException("Use flow function for lists")
    }


    /**
     * Update data entry at [name].
     *
     * Note: will not create entry, for that use [insert]
     *
     * @param name Caution: wrong [name] can overwrite data.
     * @param entity Entity
     */
    override suspend fun update(name: String, entity: List<Word>) {
        val databaseReference =
            databaseReference(name)
        databaseReference.setValue(entity).await()
    }

    /**
     * Delete repository entry at [name].
     *
     * @param name identifier of entity
     */
    override suspend fun delete(name: String) {
        databaseReference(name).removeValue().await()
    }

    private fun databaseReference(name: String): DatabaseReference {
        return if (name == "test") getUserReference() else reference.child(name)
    }

}