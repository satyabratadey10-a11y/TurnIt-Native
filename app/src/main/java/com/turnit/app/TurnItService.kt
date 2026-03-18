package com.turnit.app
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import io.ktor.server.application.call
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.request.receiveText
import io.ktor.server.response.respondText
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
class TurnItService : Service() {
    companion object {
        private const val CH  = "turnit_svc"
        private const val NID = 1001
        const val PORT = 7234
        const val BASE = "http://127.0.0.1:$PORT"
    }
    inner class LocalBinder : Binder() {
        fun get() = this@TurnItService
    }
    private val binder = LocalBinder()
    override fun onBind(i: Intent): IBinder = binder
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val server by lazy {
        embeddedServer(Netty, port = PORT) {
            routing {
                post("/health") { call.respondText("TurnIt OK") }
                post("/chat")   { call.respondText(call.receiveText()) }
            }
        }
    }
    override fun onCreate() {
        super.onCreate(); makeChannel()
        startForeground(NID, buildNotif())
        scope.launch { server.start(wait = false) }
    }
    override fun onDestroy() {
        server.stop(500, 1000); scope.cancel(); super.onDestroy()
    }
    private fun makeChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(NotificationChannel(
                    CH, "TurnIt Gateway", NotificationManager.IMPORTANCE_LOW))
        }
    }
    private fun buildNotif(): Notification {
        val b = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            Notification.Builder(this, CH)
        else @Suppress("DEPRECATION") Notification.Builder(this)
        return b.setContentTitle("TurnIt")
            .setContentText("Gateway active")
            .setSmallIcon(android.R.drawable.ic_menu_send).build()
    }
}
