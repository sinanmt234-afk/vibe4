package com.aurelia.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aurelia.music.data.party.PartySyncClient
import com.aurelia.music.player.PlayerController
import com.aurelia.music.ui.theme.ChampagneGold
import com.aurelia.music.ui.theme.GlassSurface
import com.aurelia.music.ui.theme.GoldBorder
import com.aurelia.music.ui.theme.IvoryText
import com.aurelia.music.ui.theme.MutedSilver
import com.aurelia.music.ui.theme.SubtleBorder

@Composable
fun PartyRoomScreen(
    playerController: PlayerController,
    modifier: Modifier = Modifier
) {
    val partyClient = playerController.partySyncClient
    val connectionState by partyClient.connectionState.collectAsState()
    val roomState by partyClient.roomState.collectAsState()
    val isHost by partyClient.isHost.collectAsState()
    val clipboard = LocalClipboardManager.current

    var hostNameInput by remember { mutableStateOf("Alex") }
    var joinCodeInput by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text("FRIENDS PARTY", style = MaterialTheme.typography.labelSmall)
        Text("Listen Together", style = MaterialTheme.typography.displaySmall)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            "Create a private room and share your 6-character code. Friends anywhere in the world hear the exact same beat in sync.",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(22.dp))

        if (roomState == null) {
            // Create Room Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(GlassSurface)
                    .border(1.dp, GoldBorder, RoundedCornerShape(22.dp))
                    .padding(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Groups, contentDescription = null, tint = ChampagneGold)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Host a Party Room", style = MaterialTheme.typography.titleLarge)
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = hostNameInput,
                    onValueChange = { hostNameInput = it },
                    label = { Text("Your Display Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ChampagneGold,
                        unfocusedBorderColor = SubtleBorder,
                        focusedTextColor = IvoryText,
                        unfocusedTextColor = IvoryText
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = {
                        partyClient.createRoom(hostNameInput)
                        playerController.broadcastPartyIfHost()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ChampagneGold, contentColor = Color(0xFF141106)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("Create Room & Broadcast", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Join Room Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(GlassSurface)
                    .border(1.dp, SubtleBorder, RoundedCornerShape(22.dp))
                    .padding(18.dp)
            ) {
                Text("Join a Friend's Room", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = joinCodeInput,
                    onValueChange = { joinCodeInput = it.uppercase().take(6) },
                    label = { Text("Enter 6-Character Room Code") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ChampagneGold,
                        unfocusedBorderColor = SubtleBorder,
                        focusedTextColor = IvoryText,
                        unfocusedTextColor = IvoryText
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedButton(
                    onClick = { partyClient.joinRoom(joinCodeInput) },
                    enabled = joinCodeInput.length >= 4,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("Join Room", color = ChampagneGold, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            val room = roomState!!
            // Active Room Dashboard
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(GlassSurface)
                    .border(1.dp, GoldBorder, RoundedCornerShape(22.dp))
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isHost) "HOSTING ROOM" else "LISTENING IN ROOM",
                            style = MaterialTheme.typography.labelSmall
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = room.roomCode,
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Bold,
                                color = ChampagneGold,
                                letterSpacing = 3.sp
                            )
                            IconButton(onClick = { clipboard.setText(AnnotatedString(room.roomCode)) }) {
                                Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy Code", tint = IvoryText)
                            }
                        }
                    }

                    IconButton(onClick = { partyClient.leaveRoom() }) {
                        Icon(Icons.Rounded.Logout, contentDescription = "Leave Room", tint = Color(0xFFFF6B6B))
                    }
                }

                Text(
                    text = "Status: ${connectionState.name} • Host: ${room.hostName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (connectionState == PartySyncClient.PartyConnectionState.CONNECTED) Color(0xFF7CE8A5) else MutedSilver
                )

                Spacer(modifier = Modifier.height(18.dp))

                room.track?.let { t ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = t.thumbnailUrl,
                            contentDescription = t.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(t.title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                            Text(t.artist, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                        }
                    }
                } ?: Text(
                    text = if (isHost) "Pick any song from Discover to start streaming to the room." else "Waiting for host to play a song...",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("LIVE REACTIONS", style = MaterialTheme.typography.labelSmall)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("🔥", "✨", "💜", "🎧", "🥂").forEach { emoji ->
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White.copy(alpha = 0.06f))
                                .clickable { partyClient.sendReaction(emoji) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(emoji, fontSize = 22.sp)
                        }
                    }
                }
                if (room.latestReaction.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Latest room vibe: ${room.latestReaction}", color = ChampagneGold)
                }
            }
        }
        Spacer(modifier = Modifier.height(110.dp))
    }
}
