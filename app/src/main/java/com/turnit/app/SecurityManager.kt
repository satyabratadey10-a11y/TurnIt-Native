package com.turnit.app
import android.content.Context
import android.content.SharedPreferences
import android.os.Environment
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
data class HandshakeData(
    val username: String, val userId: String,
    val appId: String, val tierStatus: String, val versionId: String
)
sealed class HandshakeResult {
    data class Valid(val data: HandshakeData) : HandshakeResult()
    data class Invalid(val reason: String)   : HandshakeResult()
    object Missing                           : HandshakeResult()
}
class SecurityManager(private val ctx: Context) {
    companion object {
        private const val PREFS   = "turnit_prefs"
        private const val K_UID   = "uid"
        private const val K_USER  = "uname"
        private const val K_TIER  = "tier"
        private const val DIR     = "Turn-Ai"
        private const val HFILE   = "Turn.json"
        private const val LOG_EXT = ".me"
    }
    private val prefs: SharedPreferences =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val root: File
        get() = File(Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DOCUMENTS), DIR)
    private val hFile: File get() = File(root, HFILE)
    suspend fun ensureDir() = withContext(Dispatchers.IO) {
        if (!root.exists()) root.mkdirs()
    }
    suspend fun writeHandshake(d: HandshakeData) = withContext(Dispatchers.IO) {
        ensureDir()
        val json = JSONObject().apply {
            put("username", d.username); put("userId", d.userId)
            put("appId", d.appId); put("tierStatus", d.tierStatus)
            put("versionId", d.versionId)
        }
        hFile.writeText(Base64.encodeToString(
            json.toString().toByteArray(Charsets.UTF_8), Base64.NO_WRAP))
    }
    suspend fun readHandshake(): HandshakeResult = withContext(Dispatchers.IO) {
        if (!hFile.exists()) return@withContext HandshakeResult.Missing
        runCatching {
            val j = JSONObject(String(
                Base64.decode(hFile.readText().trim(), Base64.NO_WRAP),
                Charsets.UTF_8))
            HandshakeResult.Valid(HandshakeData(
                j.getString("username"), j.getString("userId"),
                j.getString("appId"), j.getString("tierStatus"),
                j.getString("versionId")))
        }.getOrElse { HandshakeResult.Invalid(it.message ?: "decode error") }
    }
    fun encodeForTransport(d: HandshakeData): String {
        val j = JSONObject().apply {
            put("username", d.username); put("userId", d.userId)
            put("appId", d.appId); put("tierStatus", d.tierStatus)
            put("versionId", d.versionId)
        }
        return Base64.encodeToString(
            j.toString().toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
    }
    fun verify(local: HandshakeData, uid: String,
               appId: String, tier: String) =
        local.userId == uid && local.appId == appId && local.tierStatus == tier
    suspend fun appendLog(convId: String, role: String, msg: String) =
        withContext(Dispatchers.IO) {
            val folder = File(root, convId).also { if (!it.exists()) it.mkdirs() }
            val ts = SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
                Locale.US).format(Date())
            File(folder, "log$LOG_EXT").appendText("[$ts] $role: $msg\n")
        }
    suspend fun listConversations(): List<String> = withContext(Dispatchers.IO) {
        runCatching {
            root.listFiles()?.filter { it.isDirectory }?.map { it.name }
                ?: emptyList()
        }.getOrElse { emptyList() }
    }
    fun cacheSession(uid: String, uname: String, tier: String) =
        prefs.edit().putString(K_UID, uid)
            .putString(K_USER, uname).putString(K_TIER, tier).apply()
    fun getCachedUid()   = prefs.getString(K_UID,  null)
    fun getCachedUname() = prefs.getString(K_USER, null)
    fun clearSession()   = prefs.edit().clear().apply()
}
