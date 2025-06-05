package com.clevertap.demo.ui.inbox

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val RedCt = Color(0xFFD32F2F)
val btnClr = Color(0xFF757de8)

@Composable
fun InboxScreen(
    viewModel: CustomInboxViewModelContract,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        if (!viewModel.isInboxInitialized) {
            Text("Initializing inbox...")
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(Modifier.fillMaxWidth())
        } else {
            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Messages:",
                    style = MaterialTheme.typography.titleMedium
                )
                Text("Total: ${viewModel.totalMessagesCount}")
                Text("Unread: ${viewModel.unreadMessagesCount}")
            }
            Spacer(Modifier.height(16.dp))
            LazyColumn(Modifier.fillMaxWidth()) {
                items(count = viewModel.inboxMessages.size) { index ->
                    val message = viewModel.inboxMessages[index]
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            val firstContent = message.inboxMessageContents.firstOrNull()
                            Row {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "Title: ${firstContent?.title ?: "No title"}",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        "Message: ${firstContent?.message ?: "No message"}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        "Read: ${message.isRead}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        "Actions:",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                TextButton(onClick = { viewModel.click(message) }) {
                                    Text("Click", color = btnClr)
                                }
                                TextButton(onClick = { viewModel.view(message) }) {
                                    Text("View", color = btnClr)
                                }
                                TextButton(onClick = { viewModel.markRead(message) }) {
                                    Text("Mark Read", color = btnClr)
                                }
                                TextButton(onClick = { viewModel.delete(message) }) {
                                    Text("Delete", color = RedCt)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
