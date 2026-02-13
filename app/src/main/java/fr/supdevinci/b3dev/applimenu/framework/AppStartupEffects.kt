package fr.supdevinci.b3dev.applimenu.framework

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext

@Composable
fun AppStartupEffects(){
    val context = LocalContext.current

    val notifpermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        BackgroundSync.schedul(context)
    }

    LaunchedEffect(Unit){
        Notif.ensureChannel(context)

        if (Build.VERSION.SDK_INT >= 33){
            notifpermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            BackgroundSync.schedul(context)
        }
    }
}