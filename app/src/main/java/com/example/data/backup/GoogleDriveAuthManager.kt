package com.example.data.backup

import android.accounts.Account
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GoogleDriveAuthManager(private val context: Context) {

    companion object {
        const val DRIVE_FILE_SCOPE_URL = "https://www.googleapis.com/auth/drive.file"
        val DRIVE_FILE_SCOPE = Scope(DRIVE_FILE_SCOPE_URL)
        private const val OAUTH_SCOPE_STRING = "oauth2:$DRIVE_FILE_SCOPE_URL"
    }

    fun getGoogleSignInOptions(): GoogleSignInOptions {
        return GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(DRIVE_FILE_SCOPE)
            .build()
    }

    fun getGoogleSignInClient(): GoogleSignInClient {
        return GoogleSignIn.getClient(context, getGoogleSignInOptions())
    }

    fun getSignInIntent(): Intent {
        return getGoogleSignInClient().signInIntent
    }

    fun getLastSignedInAccount(): GoogleSignInAccount? {
        return GoogleSignIn.getLastSignedInAccount(context)
    }

    fun hasDrivePermission(account: GoogleSignInAccount? = null): Boolean {
        val target = account ?: getLastSignedInAccount() ?: return false
        return GoogleSignIn.hasPermissions(target, DRIVE_FILE_SCOPE)
    }

    /**
     * Obtains a real OAuth2 Bearer Access Token for Google Drive API calls.
     * Must be executed off the main thread.
     */
    suspend fun getAccessToken(accountEmail: String? = null): Result<String> = withContext(Dispatchers.IO) {
        try {
            val signedInAccount = getLastSignedInAccount()
            val accountEmailStr = signedInAccount?.email ?: accountEmail
            val account: Account = when {
                signedInAccount?.account != null -> signedInAccount.account!!
                !accountEmailStr.isNullOrBlank() -> Account(accountEmailStr, "com.google")
                else -> return@withContext Result.failure(
                    IllegalStateException("Google Drive authorization required. Please connect Google Drive first.")
                )
            }

            val token = GoogleAuthUtil.getToken(context, account, OAUTH_SCOPE_STRING)
            if (token.isNullOrBlank()) {
                Result.failure(IllegalStateException("Failed to obtain Google Drive authorization token."))
            } else {
                Result.success(token)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun invalidateToken(token: String) = withContext(Dispatchers.IO) {
        try {
            GoogleAuthUtil.invalidateToken(context, token)
        } catch (e: Exception) {
            // Ignore token invalidation errors
        }
    }

    suspend fun signOut(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val client = getGoogleSignInClient()
            Tasks.await(client.signOut())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.success(Unit)
        }
    }
}
