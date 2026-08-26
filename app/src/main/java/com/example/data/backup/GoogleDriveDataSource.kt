package com.example.data.backup

import com.example.domain.model.DriveBackupInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class GoogleDriveDataSource {

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    companion object {
        const val BACKUP_FILE_NAME = "TripTimer_Backup.json"
        private const val DRIVE_API_FILES_URL = "https://www.googleapis.com/drive/v3/files"
        private const val DRIVE_UPLOAD_URL = "https://www.googleapis.com/upload/drive/v3/files"
    }

    /**
     * Searches for the existing TripTimer_Backup.json in the user's Google Drive.
     * Requires a valid Google OAuth access token.
     */
    suspend fun findBackupFile(accessToken: String): Result<DriveBackupInfo?> = withContext(Dispatchers.IO) {
        if (accessToken.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("Google Drive authorization required. Please connect Google Drive first.")
            )
        }

        try {
            val query = "name = '$BACKUP_FILE_NAME' and trashed = false"
            val url = "$DRIVE_API_FILES_URL?q=${java.net.URLEncoder.encode(query, "UTF-8")}&fields=files(id,name,modifiedTime,size,description)&spaces=drive"

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $accessToken")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val code = response.code
                    val errorBody = response.body?.string() ?: ""
                    return@withContext Result.failure(
                        IOException("Google Drive check failed ($code): ${if (code == 401) "Authorization expired. Please reconnect Google Drive." else errorBody}")
                    )
                }

                val bodyStr = response.body?.string() ?: "{}"
                val json = JSONObject(bodyStr)
                val files = json.optJSONArray("files")

                if (files == null || files.length() == 0) {
                    return@withContext Result.success(null)
                }

                val fileObj = files.getJSONObject(0)
                val fileId = fileObj.getString("id")
                val name = fileObj.optString("name", BACKUP_FILE_NAME)
                val modifiedTimeStr = fileObj.optString("modifiedTime", "")
                val sizeBytes = fileObj.optLong("size", 0L)

                val modifiedTime = parseIsoDate(modifiedTimeStr)

                // Fetch brief header or download to extract counts
                val contentRes = downloadBackup(accessToken, fileId)
                if (contentRes.isSuccess) {
                    val info = parseDriveBackupInfoFromContent(fileId, name, modifiedTime, sizeBytes, contentRes.getOrThrow())
                    Result.success(info)
                } else {
                    Result.success(
                        DriveBackupInfo(
                            fileId = fileId,
                            name = name,
                            modifiedTime = modifiedTime,
                            sizeBytes = sizeBytes,
                            backupVersion = 1
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Result.failure(IOException("Google Drive communication error: ${e.localizedMessage ?: "Network error"}", e))
        }
    }

    /**
     * Uploads the backup JSON payload to Google Drive, updating the existing file or creating a new one.
     * Requires a valid Google OAuth access token.
     */
    suspend fun uploadBackup(accessToken: String, backupJson: String): Result<DriveBackupInfo> = withContext(Dispatchers.IO) {
        if (accessToken.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("Google Drive authorization required. Please connect Google Drive first.")
            )
        }

        try {
            // First check if file already exists in user's Drive
            val existing = findBackupFile(accessToken).getOrNull()

            if (existing != null && existing.fileId.isNotBlank()) {
                // Update existing file via PATCH media
                val updateUrl = "$DRIVE_UPLOAD_URL/${existing.fileId}?uploadType=media"
                val mediaType = "application/json; charset=utf-8".toMediaType()
                val requestBody = backupJson.toRequestBody(mediaType)

                val request = Request.Builder()
                    .url(updateUrl)
                    .addHeader("Authorization", "Bearer $accessToken")
                    .patch(requestBody)
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        val code = response.code
                        val err = response.body?.string() ?: ""
                        return@withContext Result.failure(IOException("Failed to update Google Drive backup ($code): $err"))
                    }
                    val now = System.currentTimeMillis()
                    val info = parseDriveBackupInfoFromContent(existing.fileId, BACKUP_FILE_NAME, now, backupJson.toByteArray().size.toLong(), backupJson)
                    return@withContext Result.success(info)
                }
            } else {
                // Create new file via Multipart POST
                val metadataJson = JSONObject().apply {
                    put("name", BACKUP_FILE_NAME)
                    put("mimeType", "application/json")
                    put("description", "Trip Timer Cloud Backup")
                }.toString()

                val multipartBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addPart(
                        metadataJson.toRequestBody("application/json; charset=UTF-8".toMediaType())
                    )
                    .addPart(
                        backupJson.toRequestBody("application/json; charset=UTF-8".toMediaType())
                    )
                    .build()

                val createUrl = "$DRIVE_UPLOAD_URL?uploadType=multipart"
                val request = Request.Builder()
                    .url(createUrl)
                    .addHeader("Authorization", "Bearer $accessToken")
                    .post(multipartBody)
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        val code = response.code
                        val errorBody = response.body?.string() ?: ""
                        return@withContext Result.failure(IOException("Failed to create Google Drive backup ($code): $errorBody"))
                    }

                    val respBody = response.body?.string() ?: "{}"
                    val respObj = JSONObject(respBody)
                    val newFileId = respObj.optString("id", "")
                    val now = System.currentTimeMillis()
                    val info = parseDriveBackupInfoFromContent(newFileId, BACKUP_FILE_NAME, now, backupJson.toByteArray().size.toLong(), backupJson)
                    return@withContext Result.success(info)
                }
            }
        } catch (e: Exception) {
            Result.failure(IOException("Google Drive upload error: ${e.localizedMessage ?: "Network error"}", e))
        }
    }

    /**
     * Downloads the raw JSON content of the backup file from Google Drive.
     * Requires a valid Google OAuth access token.
     */
    suspend fun downloadBackup(accessToken: String, fileId: String): Result<String> = withContext(Dispatchers.IO) {
        if (accessToken.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("Google Drive authorization required. Please connect Google Drive first.")
            )
        }

        try {
            val downloadUrl = "$DRIVE_API_FILES_URL/$fileId?alt=media"
            val request = Request.Builder()
                .url(downloadUrl)
                .addHeader("Authorization", "Bearer $accessToken")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val code = response.code
                    val errorBody = response.body?.string() ?: ""
                    return@withContext Result.failure(IOException("Failed to download Google Drive backup ($code): $errorBody"))
                }
                val content = response.body?.string() ?: ""
                Result.success(content)
            }
        } catch (e: Exception) {
            Result.failure(IOException("Google Drive download error: ${e.localizedMessage ?: "Network error"}", e))
        }
    }

    /**
     * Deletes the application-managed backup file from Google Drive.
     * Requires a valid Google OAuth access token.
     */
    suspend fun deleteBackupFile(accessToken: String, fileId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        if (accessToken.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("Google Drive authorization required. Please connect Google Drive first.")
            )
        }

        try {
            val deleteUrl = "$DRIVE_API_FILES_URL/$fileId"
            val request = Request.Builder()
                .url(deleteUrl)
                .addHeader("Authorization", "Bearer $accessToken")
                .delete()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful && response.code != 404) {
                    val code = response.code
                    val errorBody = response.body?.string() ?: ""
                    return@withContext Result.failure(IOException("Failed to delete Google Drive backup ($code): $errorBody"))
                }
                Result.success(true)
            }
        } catch (e: Exception) {
            Result.failure(IOException("Google Drive delete error: ${e.localizedMessage ?: "Network error"}", e))
        }
    }

    private fun parseDriveBackupInfoFromContent(
        fileId: String,
        name: String,
        modifiedTime: Long,
        sizeBytes: Long,
        content: String
    ): DriveBackupInfo {
        return runCatching {
            val obj = JSONObject(content)
            val tripsCount = obj.optJSONArray("trips")?.length() ?: 0
            val routesCount = obj.optJSONArray("routes")?.length() ?: 0
            val diagCount = obj.optJSONArray("gpsDiagnostics")?.length() ?: 0
            val createdAt = obj.optLong("createdAt", modifiedTime)
            val version = obj.optInt("backupVersion", 1)

            DriveBackupInfo(
                fileId = fileId,
                name = name,
                modifiedTime = modifiedTime,
                sizeBytes = if (sizeBytes > 0) sizeBytes else content.toByteArray().size.toLong(),
                backupVersion = version,
                tripCount = tripsCount,
                routeCount = routesCount,
                gpsDiagnosticCount = diagCount,
                createdAt = createdAt
            )
        }.getOrDefault(
            DriveBackupInfo(
                fileId = fileId,
                name = name,
                modifiedTime = modifiedTime,
                sizeBytes = sizeBytes,
                backupVersion = 1
            )
        )
    }

    private fun parseIsoDate(isoStr: String): Long {
        if (isoStr.isBlank()) return System.currentTimeMillis()
        return runCatching {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            format.parse(isoStr)?.time ?: SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).parse(isoStr)?.time ?: System.currentTimeMillis()
        }.getOrDefault(System.currentTimeMillis())
    }
}
