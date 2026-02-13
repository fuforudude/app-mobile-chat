package fr.supdevinci.b3dev.applimenu.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import fr.supdevinci.b3dev.applimenu.domain.model.Message
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun MessageBubble(message: Message) {
    val alignment = if (message.isFromMe) Alignment.End else Alignment.Start
    val color = if (message.isFromMe) MaterialTheme.colorScheme.primary else Color.LightGray

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        horizontalAlignment = alignment
    ) {
        if (!message.isFromMe) {
            Text(text = message.senderId, style = MaterialTheme.typography.labelSmall)
        }
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = color,
            tonalElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text(text = message.text, color = if (message.isFromMe) Color.White else Color.Black)
                Text(
                    text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(message.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}