package fr.supdevinci.b3dev.applimenu.framework

import android.Manifest
import android.content.Context
import androidx.annotation.RequiresPermission
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import fr.supdevinci.b3dev.applimenu.framework.Notif.openAppPendingIntent

class ServerPollWorker (
    appContext: Context,
    params: WorkerParameters
): CoroutineWorker(appContext, params){

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override suspend fun doWork(): Result {
        val hasUpdate = true

        if (hasUpdate) {
            Notif.ensureChannel(applicationContext)
            val pi = openAppPendingIntent(applicationContext)

            Notif.show(
                applicationContext,
                title ="Mise à jour",
                text = "Une mise à jour est disponible",
                pendingIntent = pi
            )
        }
        return Result.success()
    }
}