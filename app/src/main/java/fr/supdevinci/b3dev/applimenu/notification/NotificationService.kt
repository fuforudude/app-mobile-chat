package fr.supdevinci.b3dev.applimenu.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import fr.supdevinci.b3dev.applimenu.MainActivity

/**
 * Service de gestion des notifications pour les messages
 */
class NotificationService(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "chat_messages"
        const val CHANNEL_NAME = "Messages"
        const val CHANNEL_DESCRIPTION = "Notifications pour les nouveaux messages"

        private var notificationId = 0

        fun getNextNotificationId(): Int {
            return notificationId++
        }
    }

    init {
        createNotificationChannel()
    }

    /**
     * Créer le canal de notification (requis pour Android 8.0+)
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                enableLights(true)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Afficher une notification pour un nouveau message
     */
    fun showMessageNotification(
        senderName: String,
        messageContent: String,
        conversationId: Int? = null
    ) {
        // Intent pour ouvrir l'app quand on clique sur la notification
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            conversationId?.let { putExtra("conversationId", it) }
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Construire la notification
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_email) // Icône par défaut, à remplacer
            .setContentTitle(senderName)
            .setContentText(messageContent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(messageContent))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        // Afficher la notification
        try {
            NotificationManagerCompat.from(context).notify(getNextNotificationId(), notification)
        } catch (e: SecurityException) {
            // Permission non accordée
            android.util.Log.e("NotificationService", "Permission de notification non accordée", e)
        }
    }

    /**
     * Vérifier si les notifications sont activées
     */
    fun areNotificationsEnabled(): Boolean {
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
}

