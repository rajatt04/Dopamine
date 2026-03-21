package com.google.android.piyush.dopamine.authentication.repository

import android.content.Context
import android.content.Intent
import android.content.IntentSender
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.piyush.dopamine.R
import com.google.android.piyush.dopamine.authentication.SignInResult
import com.google.android.piyush.dopamine.authentication.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

class UserAuthRepositoryImpl(
    private val context: Context
) {

    private val oneTapClient: SignInClient = Identity.getSignInClient(context)
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val auth = Firebase.auth

    private val googleSignInClient: GoogleSignInClient by lazy {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.web_client_id))
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    private fun buildSignInRequest(): BeginSignInRequest {
        return BeginSignInRequest.Builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(context.getString(R.string.web_client_id))
                    .build()
            )
            .setAutoSelectEnabled(false)
            .build()
    }

    suspend fun googleSignIn(): IntentSender? {
        val result = try {
            oneTapClient.beginSignIn(
                buildSignInRequest()
            ).await()
        } catch(e: Exception) {
            e.printStackTrace()
            if(e is CancellationException) throw e
            null
        }
        return result?.pendingIntent?.intentSender
    }

    fun getLegacySignInIntent(): Intent {
        return googleSignInClient.signInIntent
    }

    suspend fun signInWithIntent(intent: Intent): SignInResult {
        return try {
            val credential = try {
                oneTapClient.getSignInCredentialFromIntent(intent)
            } catch (e: Exception) {
                null
            }

            val googleIdToken = credential?.googleIdToken
            if (googleIdToken != null) {
                val googleCredentials = GoogleAuthProvider.getCredential(googleIdToken, null)
                val user = auth.signInWithCredential(googleCredentials).await().user
                SignInResult(
                    userData = user?.run {
                        User(
                            userId = uid,
                            userName = displayName ?: "",
                            userEmail = email ?: "",
                            userImage = photoUrl?.toString()
                        )
                    },
                    errorMessage = null
                )
            } else {
                val task = GoogleSignIn.getSignedInAccountFromIntent(intent)
                val account = task.await()
                val idToken = account.idToken
                if (idToken != null) {
                    val googleCredentials = GoogleAuthProvider.getCredential(idToken, null)
                    val user = auth.signInWithCredential(googleCredentials).await().user
                    SignInResult(
                        userData = user?.run {
                            User(
                                userId = uid,
                                userName = displayName ?: "",
                                userEmail = email ?: "",
                                userImage = photoUrl?.toString()
                            )
                        },
                        errorMessage = null
                    )
                } else {
                    SignInResult(userData = null, errorMessage = "No ID token found")
                }
            }
        } catch(e: Exception) {
            e.printStackTrace()
            if(e is CancellationException) throw e
            SignInResult(
                userData = null,
                errorMessage = e.message
            )
        }
    }

    fun currentUser() : FirebaseUser? {
        return firebaseAuth.currentUser
    }
}