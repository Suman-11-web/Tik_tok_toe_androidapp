package com.example.ui.game

import android.content.Context
import android.widget.Toast
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.MatchRecord
import java.text.SimpleDateFormat
import java.util.*

// Neon Branding Colors (TikTok Toy Sleek Cyberpunk theme)
val SlateDark = Color(0xFF14151B)
val SlateCard = Color(0xFF1F2026)
val NeonCyan = Color(0xFF00F2FE)
val NeonMagenta = Color(0xFFFE0979)
val NeonYellow = Color(0xFFFFE600)
val PureWhite = Color(0xFFFFFFFF)
val MutedSlate = Color(0xFF8D8E99)

val CyanMagentaBrush = Brush.linearGradient(
    colors = listOf(NeonCyan, NeonMagenta)
)

val DarkMeshBrush = Brush.verticalGradient(
    colors = listOf(Color(0xFF0C0D11), SlateDark)
)

@Composable
fun TikTokToeApp(viewModel: GameViewModel) {
    val appState by viewModel.appState.collectAsState()
    val stats by viewModel.profileStats.collectAsState()
    var showSplash by remember { mutableStateOf(true) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = SlateDark
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Smooth background ambient animations
            PulseAmbientBackground()

            if (showSplash) {
                SumanEpicSplash(onFinished = { showSplash = false })
            } else if (stats != null && !stats!!.hasCompletedOnboarding) {
                CyberOnboardingScreen(viewModel = viewModel)
            } else {
                when (appState) {
                    AppState.MENU -> MenuScreen(viewModel)
                    AppState.LOBBY_SETUP -> LobbySetupScreen(viewModel)
                    AppState.LOBBY_WAIT -> LobbyWaitScreen(viewModel)
                    AppState.GAME_PLAY -> GamePlayScreen(viewModel)
                    AppState.HISTORY -> HistoryScreen(viewModel)
                    AppState.PROFILE -> ProfileScreen(viewModel)
                }
            }
        }
    }
}

@Composable
fun PulseAmbientBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "BgPulse")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.03f,
        targetValue = 0.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BgLightPulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkMeshBrush)
    ) {
        // Glowing radial light in top-right
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = NeonCyan,
                radius = size.width * 0.45f,
                center = Offset(size.width * 0.9f, size.height * 0.1f),
                alpha = alphaAnim
            )
            // Glowing radial light in bottom-left
            drawCircle(
                color = NeonMagenta,
                radius = size.width * 0.5f,
                center = Offset(size.width * 0.1f, size.height * 0.9f),
                alpha = alphaAnim * 0.8f
            )
        }
    }
}

@Composable
fun MenuScreen(viewModel: GameViewModel) {
    val stats by viewModel.profileStats.collectAsState()
    val activeTheme = stats?.preferredTheme ?: "CYAN"
    val themeColors = getThemeColors(activeTheme)
    val appPrimary = themeColors.first
    val appSecondary = themeColors.second

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Profile Brief pill
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(SlateCard)
                .clickable { viewModel.navigateTo(AppState.PROFILE) }
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile",
                    tint = appPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stats?.username ?: "Runner",
                    color = PureWhite,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 140.dp)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Wins",
                    tint = appSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "LVL ${stats?.level ?: 1}",
                    color = appSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        // Title Header with Glowing Layered Shadow
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                // Cyber Offset Layer
                Text(
                    text = "NEON GRID",
                    fontSize = 46.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp,
                    color = appSecondary,
                    modifier = Modifier.offset(x = 2.dp, y = 2.dp)
                )
                Text(
                    text = "NEON GRID",
                    fontSize = 46.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp,
                    color = appPrimary,
                    style = TextStyle(
                        shadow = Shadow(
                            color = appPrimary,
                            blurRadius = 15f
                        )
                    )
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "⚡ ULTIMATE NEON CHRONICLES ⚡",
                fontSize = 11.sp,
                color = MutedSlate,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
        }

        // GRID SIZE SYSTEM SELECTOR
        val activeSize by viewModel.gridSize.collectAsState()
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
        ) {
            Text(
                text = "⚡ ARENA GRID SIZE SELECTOR",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = appPrimary,
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf(3, 4, 6).forEach { size ->
                    val active = activeSize == size
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("grid_selector_$size")
                            .clickable { viewModel.updateGridSize(size) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (active) appPrimary.copy(alpha = 0.2f) else SlateCard
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (active) appPrimary else MutedSlate.copy(alpha = 0.15f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${size}x${size}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = if (active) appPrimary else PureWhite,
                                style = TextStyle(
                                    shadow = Shadow(
                                        color = if (active) appPrimary else Color.Transparent,
                                        blurRadius = if (active) 8f else 0f
                                    )
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = when (size) {
                                    3 -> "3 in Row"
                                    4 -> "4 in Row"
                                    else -> "Tactical 4"
                                },
                                fontSize = 10.sp,
                                color = if (active) PureWhite else MutedSlate,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Action Menu Buttons
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MenuButton(
                text = "Online Multiplayer Match",
                subtext = "Sync & play logs via custom room codes",
                icon = Icons.Default.Share,
                glowColor = appPrimary,
                testTag = "btn_online_mult",
                onClick = { viewModel.startOnlineMatchSetup() }
            )

            MenuButton(
                text = "Play vs Smart Bot",
                subtext = "Test your skills against smart heuristic AI",
                icon = Icons.Default.PlayArrow,
                glowColor = appSecondary,
                testTag = "btn_play_ai",
                onClick = { viewModel.selectOfflineMode(GameMode.VS_AI) }
            )

            MenuButton(
                text = "Pass & Play Locally",
                subtext = "Classic dual play offline with friend",
                icon = Icons.Default.Refresh,
                glowColor = MutedSlate,
                testTag = "btn_play_local",
                onClick = { viewModel.selectOfflineMode(GameMode.LOCAL) }
            )
        }

        // Footer buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            IconButtonWithText(
                text = "Match History",
                icon = Icons.Default.List,
                modifier = Modifier.weight(1f),
                onClick = { viewModel.navigateTo(AppState.HISTORY) }
            )
            IconButtonWithText(
                text = "Profile Stats",
                icon = Icons.Default.Person,
                modifier = Modifier.weight(1f),
                onClick = { viewModel.navigateTo(AppState.PROFILE) }
            )
        }
    }
}

@Composable
fun MenuButton(
    text: String,
    subtext: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    glowColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
            .clickable(onClick = onClick)
            .border(1.dp, glowColor.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(glowColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = glowColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = text,
                    color = PureWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtext,
                    color = MutedSlate,
                    fontSize = 12.sp
                )
            }
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = MutedSlate,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun IconButtonWithText(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        colors = ButtonDefaults.buttonColors(containerColor = SlateCard),
        border = BorderStroke(1.dp, MutedSlate.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = PureWhite, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = text, color = PureWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun LobbySetupScreen(viewModel: GameViewModel) {
    val networkStatus by viewModel.networkStatus.collectAsState()
    val stats by viewModel.profileStats.collectAsState()
    val networkError by viewModel.networkError.collectAsState()

    var nicknameInput by remember { mutableStateOf(stats.username) }
    var codeToJoin by remember { mutableStateOf("") }
    
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Back Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.navigateTo(AppState.MENU) }) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = PureWhite)
            }
            Text(
                text = "MULTIPLAYER MATCH SETUP",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = PureWhite,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        // Body setup contents
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile setup Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SlateCard)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Your Display Nickname",
                        color = NeonCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = nicknameInput,
                        onValueChange = {
                            nicknameInput = it.take(15)
                            viewModel.updateUsername(nicknameInput)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = PureWhite,
                            unfocusedTextColor = PureWhite,
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = MutedSlate.copy(alpha = 0.5f),
                            focusedContainerColor = SlateDark,
                            unfocusedContainerColor = SlateDark
                        ),
                        singleLine = true,
                        placeholder = { Text("Enter static username...", color = MutedSlate) },
                        modifier = Modifier.fillMaxWidth().testTag("nickname_tf")
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (networkStatus == NetStatus.FAILED || networkStatus == NetStatus.DISCONNECTED) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(NeonMagenta.copy(alpha = 0.15f))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Live Server Disconnected", color = PureWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(networkError ?: "Verify your internet connection & retry", color = MutedSlate, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Button(
                        onClick = { viewModel.reconnectNetwork() },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(32.dp).testTag("reconnect_button")
                    ) {
                        Text("Retry", color = SlateDark, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Selection options
            if (networkStatus == NetStatus.CONNECTING) {
                CircularProgressIndicator(color = NeonCyan)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Connecting to live sync servers...", color = MutedSlate, fontSize = 13.sp)
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Host Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("host_card")
                            .clickable { viewModel.hostOnlineRoom() },
                        colors = CardDefaults.cardColors(containerColor = SlateCard),
                        border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(NeonCyan.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = NeonCyan)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Create Game", color = PureWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Get code & share", color = MutedSlate, fontSize = 11.sp, textAlign = TextAlign.Center)
                        }
                    }

                    // Join Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("join_card")
                            .clickable {
                                if (codeToJoin.length == 6) {
                                    viewModel.joinOnlineRoom(codeToJoin)
                                } else {
                                    Toast.makeText(context, "Please enter 6-digit code first!", Toast.LENGTH_SHORT).show()
                                }
                            },
                        colors = CardDefaults.cardColors(containerColor = SlateCard),
                        border = BorderStroke(1.dp, NeonMagenta.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(NeonMagenta.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = NeonMagenta)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Join Code", color = PureWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Enter shared code", color = MutedSlate, fontSize = 11.sp, textAlign = TextAlign.Center)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Enter code input matching joining mode
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateCard)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Or Enter Active 6-digit Lobby Code to Join:",
                            color = NeonMagenta,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = codeToJoin,
                            onValueChange = { codeToJoin = it.filter { c -> c.isDigit() }.take(6) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = PureWhite,
                                unfocusedTextColor = PureWhite,
                                focusedBorderColor = NeonMagenta,
                                unfocusedBorderColor = MutedSlate.copy(alpha = 0.5f),
                                focusedContainerColor = SlateDark,
                                unfocusedContainerColor = SlateDark
                            ),
                            singleLine = true,
                            placeholder = { Text("e.g. 159357", color = MutedSlate) },
                            modifier = Modifier.fillMaxWidth().testTag("code_tf")
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                if (codeToJoin.length == 6) {
                                    viewModel.joinOnlineRoom(codeToJoin)
                                } else {
                                    Toast.makeText(context, "Lobby Code must be exactly 6 digits!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonMagenta),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("btn_join_action")
                        ) {
                            Text("Join Room Live", color = PureWhite, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            networkError?.let {
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "❌ $it", color = NeonMagenta, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            }
        }

        // Footer connection indicators
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (networkStatus == NetStatus.CONNECTED) NeonCyan else NeonMagenta)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Live Sync Status: ${networkStatus.name}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MutedSlate
            )
        }
    }
}

@Composable
fun LobbyWaitScreen(viewModel: GameViewModel) {
    val roomCode by viewModel.roomCode.collectAsState()
    val opponentName by viewModel.opponentName.collectAsState()
    val networkStatus by viewModel.networkStatus.collectAsState()
    
    val clipManager = LocalClipboardManager.current
    val ctx = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Upper back bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.navigateTo(AppState.LOBBY_SETUP) }) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = PureWhite)
            }
            Text(
                text = "WAITING LOBBY",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = PureWhite,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        // Center card presenting code
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "Room Created!",
                color = NeonCyan,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Share this 6-digit code with your friend to connect instantly",
                color = MutedSlate,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Large Lobby Code box
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = roomCode ?: "------",
                        fontSize = 54.sp,
                        fontWeight = FontWeight.Black,
                        color = PureWhite,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 4.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            roomCode?.let {
                                clipManager.setText(AnnotatedString(it))
                                Toast.makeText(ctx, "Lobby code copied to clipboard!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SlateDark),
                        border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Share, contentDescription = "Copy", tint = NeonCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Copy Game Code", color = PureWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Status Loader
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(80.dp)) {
                CircularProgressIndicator(
                    color = NeonMagenta,
                    strokeWidth = 3.dp,
                    modifier = Modifier.fillMaxSize()
                )
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = null,
                    tint = NeonMagenta,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Status: $opponentName",
                color = PureWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            if (networkStatus != NetStatus.CONNECTED) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(NeonMagenta.copy(alpha = 0.2f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = NeonMagenta, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Connection Offline... Retrying...", color = PureWhite, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Tip text
        Text(
            text = "Keep this screen open. The game will automatically transition once your opponent enters the match code.",
            color = MutedSlate,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
    }
}

@Composable
fun GamePlayScreen(viewModel: GameViewModel) {
    val board by viewModel.board.collectAsState()
    val currentTurn by viewModel.currentTurn.collectAsState()
    val mySymbol by viewModel.mySymbol.collectAsState()
    val opponentSymbol by viewModel.opponentSymbol.collectAsState()
    val opponentName by viewModel.opponentName.collectAsState()
    val gameResult by viewModel.gameResult.collectAsState()
    val winningLine by viewModel.winningLine.collectAsState()
    val gameMode by viewModel.gameMode.collectAsState()
    val roomCode by viewModel.roomCode.collectAsState()
    val stats by viewModel.profileStats.collectAsState()
    val networkStatus by viewModel.networkStatus.collectAsState()

    val rematchMe by viewModel.rematchProposedByMe.collectAsState()
    val rematchOpp by viewModel.rematchProposedByOpponent.collectAsState()

    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: My Profile vs Opponent Dashboard
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // My Player Info Bubble
                Card(
                    modifier = Modifier.width(130.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateCard),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = stats.username,
                            color = PureWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(NeonCyan),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (gameMode == GameMode.ONLINE) mySymbol else "X",
                                    color = SlateDark,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Player 1", color = MutedSlate, fontSize = 11.sp)
                        }
                    }
                }

                // VS circle
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(SlateCard),
                    contentAlignment = Alignment.Center
                ) {
                    Text("VS", color = NeonMagenta, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                // Opponent Info Bubble
                Card(
                    modifier = Modifier.width(130.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateCard),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = when (gameMode) {
                                GameMode.ONLINE -> opponentName
                                GameMode.VS_AI -> "Gemini AI"
                                GameMode.LOCAL -> "Local Friend"
                            },
                            color = PureWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Player 2", color = MutedSlate, fontSize = 11.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(NeonMagenta),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (gameMode == GameMode.ONLINE) opponentSymbol else "O",
                                    color = SlateDark,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            // Room / Mode metadata bar
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(SlateCard)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (gameMode == GameMode.ONLINE) "Online Lobby Code: $roomCode" else "Mode: ${gameMode.name}",
                    color = PureWhite,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Turn indicator Card
            TurnIndicator(currentTurn = currentTurn, mySymbol = mySymbol, isOnline = gameMode == GameMode.ONLINE)
            
            if (gameMode == GameMode.ONLINE && networkStatus != NetStatus.CONNECTED) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(NeonMagenta.copy(alpha = 0.2f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = NeonMagenta, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Connection offline. Retrying...", color = PureWhite, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // Tic Tac Toe Game Grid Board
            val gridSize by viewModel.gridSize.collectAsState()
            val activeTheme = stats?.preferredTheme ?: "CYAN"
            val themeColors = getThemeColors(activeTheme)
            val appPrimary = themeColors.first
            val appSecondary = themeColors.second

            Box(
                modifier = Modifier
                    .sizeIn(maxWidth = 340.dp, maxHeight = 340.dp)
                    .aspectRatio(1f)
                    .background(SlateCard, RoundedCornerShape(20.dp))
                    .border(BorderStroke(1.dp, MutedSlate.copy(alpha = 0.2f)), RoundedCornerShape(20.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                // Game cells
                Column(modifier = Modifier.fillMaxSize()) {
                    for (row in 0 until gridSize) {
                        Row(modifier = Modifier.weight(1f)) {
                            for (col in 0 until gridSize) {
                                val cellIdx = row * gridSize + col
                                val mark = board.getOrNull(cellIdx)

                                CellView(
                                    symbol = mark,
                                    index = cellIdx,
                                    gridSize = gridSize,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    onClick = { viewModel.makeMove(cellIdx) }
                                )

                                if (col < gridSize - 1) {
                                    Spacer(
                                        modifier = Modifier
                                            .width(2.dp)
                                            .fillMaxHeight()
                                            .background(MutedSlate.copy(alpha = 0.15f))
                                    )
                                }
                            }
                        }
                        if (row < gridSize - 1) {
                            Spacer(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.dp)
                                    .background(MutedSlate.copy(alpha = 0.15f))
                            )
                        }
                    }
                }

                // Award line generator
                winningLine?.let { line ->
                    WinningStrokeLine(winningCombo = line, gridSize = gridSize)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick emoji feedback panel (circular spams)
            EmojiTray(onSelectEmoji = { sym -> viewModel.sendLocalEmoji(sym) })

            Spacer(modifier = Modifier.weight(1f))

            // Exit/Reset action keys
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = { viewModel.navigateTo(AppState.MENU) },
                    colors = ButtonDefaults.buttonColors(containerColor = SlateCard),
                    border = BorderStroke(1.dp, MutedSlate.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Exit Deck", color = PureWhite, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { viewModel.startNewGame() },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonMagenta),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Reset Board", color = PureWhite, fontWeight = FontWeight.Bold)
                }
            }
        }

        // In-game modal overlay when the game is finalized
        gameResult?.let { res ->
            ResultModalOverlay(
                result = res,
                mySymbol = mySymbol,
                isOnline = gameMode == GameMode.ONLINE,
                rematchMe = rematchMe,
                rematchOpp = rematchOpp,
                onRematchClick = { viewModel.proposeRematch() },
                onExitClick = { viewModel.navigateTo(AppState.MENU) }
            )
        }

        // Floating falling emojis stream
        EmojiFloatingCanvas(emojis = viewModel.activeEmojis)
    }
}

@Composable
fun TurnIndicator(currentTurn: String, mySymbol: String, isOnline: Boolean) {
    val activeGlow = if (currentTurn == "X") NeonCyan else NeonMagenta
    val infiniteTransition = rememberInfiniteTransition(label = "TurnGlow")
    val scaleAnim by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowScale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scaleAnim)
            .padding(horizontal = 24.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        border = BorderStroke(2.dp, activeGlow.copy(alpha = 0.8f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(activeGlow)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = if (isOnline) {
                    if (currentTurn == mySymbol) "⭐ YOUR TURN" else "OPPONENT'S TURN"
                } else {
                    "PLAYER $currentTurn's TURN"
                },
                color = PureWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
fun CellView(symbol: String?, index: Int, gridSize: Int, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val scale = remember { Animatable(0f) }

    LaunchedEffect(symbol) {
        if (symbol != null) {
            scale.snapTo(0f)
            scale.animateTo(
                targetValue = 1.2f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
            )
            scale.animateTo(1f, animationSpec = tween(100))
        }
    }

    val textSize = when (gridSize) {
        3 -> 44.sp
        4 -> 32.sp
        else -> 20.sp
    }

    Box(
        modifier = modifier
            .testTag("cell_$index")
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (symbol != null) {
            Text(
                text = symbol,
                fontSize = textSize,
                color = if (symbol == "X") NeonCyan else NeonMagenta,
                fontWeight = FontWeight.Black,
                modifier = Modifier.scale(scale.value),
                style = TextStyle(
                    shadow = Shadow(
                        color = if (symbol == "X") NeonCyan else NeonMagenta,
                        blurRadius = 15f
                    )
                )
            )
        }
    }
}

@Composable
fun WinningStrokeLine(winningCombo: List<Int>, gridSize: Int) {
    val drawPercent = remember { Animatable(0f) }

    LaunchedEffect(winningCombo) {
        drawPercent.snapTo(0f)
        drawPercent.animateTo(1f, animationSpec = tween(500, easing = EaseOutCubic))
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val cellCount = gridSize.toFloat()
        val itemW = size.width / cellCount
        val itemH = size.height / cellCount

        // Resolve coordinates of start and endpoints
        val firstIndex = winningCombo.first()
        val lastIndex = winningCombo.last()

        val startX = (firstIndex % gridSize) * itemW + (itemW / 2)
        val startY = (firstIndex / gridSize) * itemH + (itemH / 2)

        val endX = (lastIndex % gridSize) * itemW + (itemW / 2)
        val endY = (lastIndex / gridSize) * itemH + (itemH / 2)

        // Interpolated points
        val activeX = startX + (endX - startX) * drawPercent.value
        val activeY = startY + (endY - startY) * drawPercent.value

        drawLine(
            color = Color.White,
            start = Offset(startX, startY),
            end = Offset(activeX, activeY),
            strokeWidth = 14f,
            cap = StrokeCap.Round
        )

        // Overlay brand glowing stroke
        drawLine(
            color = NeonYellow,
            start = Offset(startX, startY),
            end = Offset(activeX, activeY),
            strokeWidth = 8f,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun EmojiTray(onSelectEmoji: (String) -> Unit) {
    val list = listOf("😂", "🔥", "😭", "😮", "💖")
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(SlateCard)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        list.forEachIndexed { idx, symbol ->
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(SlateDark)
                    .testTag("emoji_btn_$idx")
                    .clickable { onSelectEmoji(symbol) },
                contentAlignment = Alignment.Center
            ) {
                Text(text = symbol, fontSize = 22.sp)
            }
        }
    }
}

@Composable
fun EmojiFloatingCanvas(emojis: List<FloatingEmoji>) {
    Box(modifier = Modifier.fillMaxSize()) {
        emojis.forEach { item ->
            var activeYOffset by remember { mutableStateOf(1f) }
            var activeAlpha by remember { mutableStateOf(1f) }

            LaunchedEffect(item) {
                animate(
                    initialValue = 1f,
                    targetValue = 0.1f,
                    animationSpec = tween(item.duration, easing = LinearEasing)
                ) { value, _ ->
                    activeYOffset = value
                    activeAlpha = value
                }
            }

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val xPos = maxWidth * item.xOffset
                val yPos = maxHeight * activeYOffset

                Text(
                    text = item.emoji,
                    fontSize = 32.sp,
                    modifier = Modifier
                        .offset(x = xPos, y = yPos)
                        .scale(1.2f - activeYOffset)
                        .alpha(activeAlpha)
                        .background(Color.Transparent)
                )
            }
        }
    }
}

@Composable
fun ResultModalOverlay(
    result: String,
    mySymbol: String,
    isOnline: Boolean,
    rematchMe: Boolean,
    rematchOpp: Boolean,
    onRematchClick: () -> Unit,
    onExitClick: () -> Unit
) {
    val isWin = if (isOnline) {
        val winningSymbol = result.substringAfter("WIN_")
        winningSymbol == mySymbol
    } else {
        result != "DRAW"
    }

    val heading = if (result == "DRAW") {
        "🤝 DRAW MATCH"
    } else if (isWin) {
        "🏆 VICTORY!"
    } else {
        "💀 DEFEAT..."
    }

    val glowColor = if (result == "DRAW") NeonYellow else if (isWin) NeonCyan else NeonMagenta

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.82f))
            .clickable(enabled = false) {}, // Prevent back clicks
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .width(320.dp)
                .padding(20.dp)
                .border(2.dp, glowColor, RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = SlateCard),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = heading,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black,
                    color = glowColor,
                    style = TextStyle(
                        shadow = Shadow(color = glowColor, blurRadius = 15f)
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = if (result == "DRAW") "A perfectly synced standoff!" 
                           else if (isWin) "Outstanding strategy! Stats updated." 
                           else "Better luck next round! Keep matching.",
                    color = MutedSlate,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Rematch Status Text
                if (isOnline) {
                    val statusText = when {
                        rematchMe && rematchOpp -> "Initializing Rematch..."
                        rematchMe -> "Rematch invitation sent..."
                        rematchOpp -> "Opponent requests a rematch!"
                        else -> "Challenge your opponent to a rematch"
                    }
                    Text(
                        text = statusText,
                        color = PureWhite,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp),
                        textAlign = TextAlign.Center
                    )
                }

                // Action keys
                Button(
                    onClick = onRematchClick,
                    colors = ButtonDefaults.buttonColors(containerColor = glowColor),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("btn_rematch")
                ) {
                    Text(
                        text = if (isOnline && rematchOpp) "Accept Rematch" else "Rematch Challenge",
                        color = if (glowColor == NeonYellow) SlateDark else PureWhite,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onExitClick,
                    colors = ButtonDefaults.buttonColors(containerColor = SlateDark),
                    border = BorderStroke(1.dp, MutedSlate.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("btn_exit_modal")
                ) {
                    Text("Exit to Menu", color = PureWhite, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun HistoryScreen(viewModel: GameViewModel) {
    val records by viewModel.matchHistory.collectAsState()
    val ctx = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
    ) {
        // History Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.navigateTo(AppState.MENU) }) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = PureWhite)
                }
                Text(
                    text = "MATCH LOGS",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = PureWhite,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            if (records.isNotEmpty()) {
                IconButton(onClick = {
                    viewModel.clearStats()
                    Toast.makeText(ctx, "Stats logs cleared!", Toast.LENGTH_SHORT).show()
                }) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Clear logs", tint = NeonMagenta)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (records.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(SlateCard),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MutedSlate)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text("No match records found yet", color = MutedSlate, fontSize = 14.sp)
                Text("Play dynamic matches to capture stats!", color = MutedSlate, fontSize = 11.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(records) { item ->
                    HistoryItemCard(record = item)
                }
            }
        }
    }
}

@Composable
fun HistoryItemCard(record: MatchRecord) {
    val dateStr = remember(record.timestamp) {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(record.timestamp))
    }

    val outcomeGlow = when (record.outcome) {
        "WIN" -> NeonCyan
        "LOSS" -> NeonMagenta
        else -> NeonYellow
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(outcomeGlow)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "vs ${record.opponentName}",
                        color = PureWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${record.mode} • code: ${record.roomCode ?: "local"}",
                    color = MutedSlate,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = dateStr,
                    color = MutedSlate,
                    fontSize = 9.sp
                )
            }

            // Outcome badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(outcomeGlow.copy(alpha = 0.15f))
                    .border(BorderStroke(1.dp, outcomeGlow.copy(alpha = 0.5f)), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = record.outcome,
                    color = outcomeGlow,
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun ProfileScreen(viewModel: GameViewModel) {
    val stats by viewModel.profileStats.collectAsState()
    val ctx = LocalContext.current

    val activeTheme = stats?.preferredTheme ?: "CYAN"
    val themeColors = getThemeColors(activeTheme)
    val appPrimary = themeColors.first
    val appSecondary = themeColors.second

    var editName by remember { mutableStateOf("") }
    LaunchedEffect(stats) {
        if (stats != null && editName.isEmpty()) {
            editName = stats!!.username
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // App bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.navigateTo(AppState.MENU) }) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = PureWhite)
            }
            Text(
                text = "NEURAL DECK DASHBOARD",
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = PureWhite,
                modifier = Modifier.padding(start = 8.dp),
                style = TextStyle(
                    shadow = Shadow(color = appPrimary, blurRadius = 10f)
                )
            )
        }

        // Body profile cards
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile Card (nickname setter)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, appPrimary.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Change Cybernetic Alias", color = appPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editName,
                        onValueChange = {
                            editName = it.take(12)
                            viewModel.updateUsername(editName)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = PureWhite,
                            unfocusedTextColor = PureWhite,
                            focusedBorderColor = appPrimary,
                            unfocusedBorderColor = MutedSlate.copy(alpha = 0.5f),
                            focusedContainerColor = SlateDark,
                            unfocusedContainerColor = SlateDark
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // XP LEVEL PROGRESSION CARD
            val xpCount = stats?.xp ?: 0
            val lvl = stats?.level ?: 1
            val relativeXp = xpCount % 300
            val progressFraction = relativeXp.toFloat() / 300f

            val userRank = when {
                lvl < 2 -> "GRID RECRUIT"
                lvl < 3 -> "NEURON TRACER"
                lvl < 4 -> "SYNAPSE STRATEGIST"
                lvl < 5 -> "GRID CHIEF RUNNER"
                else -> "CYBERNETIC DECKMASTER"
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, appPrimary.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LEVEL $lvl PROFILE",
                            color = appSecondary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = userRank,
                            color = appPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(appPrimary.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "XP Progress: $relativeXp / 300 to NEXT LEVEL",
                        color = MutedSlate,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    // Progress Indicator Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape)
                            .background(SlateDark)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progressFraction)
                                .fillMaxHeight()
                                .clip(CircleShape)
                                .background(appPrimary)
                        )
                    }
                }
            }

            // SELECT FACTION THEME CARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, appPrimary.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Customize System Accent Light",
                        color = appPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf("CYAN", "MAGENTA", "GREEN", "AMBER", "PURPLE").forEach { thName ->
                            val active = activeTheme == thName
                            val colors = getThemeColors(thName)
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(colors.first)
                                    .border(
                                        BorderStroke(
                                            if (active) 2.5.dp else 0.dp,
                                            if (active) PureWhite else Color.Transparent
                                        ),
                                        CircleShape
                                    )
                                    .clickable { viewModel.updateTheme(thName) }
                                    .testTag("theme_btn_profile_$thName")
                            )
                        }
                    }
                }
            }

            // Game Statistics board
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Lifetime Arena Registry",
                        color = appSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ProfileStatBadge(title = "Wins", value = "${stats?.wins ?: 0}", color = appPrimary, modifier = Modifier.weight(1f))
                        ProfileStatBadge(title = "Losses", value = "${stats?.losses ?: 0}", color = appSecondary, modifier = Modifier.weight(1f))
                        ProfileStatBadge(title = "Draws", value = "${stats?.draws ?: 0}", color = NeonYellow, modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Active Winning Streak:", color = PureWhite, fontSize = 13.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = NeonYellow, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("${stats?.currentStreak ?: 0} Games", color = NeonYellow, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Peak Historic Streak:", color = PureWhite, fontSize = 13.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ThumbUp, contentDescription = null, tint = appPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("${stats?.maxStreak ?: 0} Games", color = appPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Clear all button
        Button(
            onClick = {
                viewModel.clearStats()
                Toast.makeText(ctx, "Lifetime Registry completely wiped!", Toast.LENGTH_SHORT).show()
            },
            colors = ButtonDefaults.buttonColors(containerColor = SlateCard),
            border = BorderStroke(1.dp, appSecondary.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text("Clear Lifetime Statistics", color = appSecondary, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ProfileStatBadge(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = SlateDark),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title, color = MutedSlate, fontSize = 10.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, color = color, fontSize = 24.sp, fontWeight = FontWeight.Black)
        }
    }
}

// ==========================================
// SUMAN EPIC LEVEL ENTRY ANIMATION ENGINE
// ==========================================
@Composable
fun SumanEpicSplash(onFinished: () -> Unit) {
    var loadingPercent by remember { mutableStateOf(0) }
    var isWarping by remember { mutableStateOf(false) }

    val warpScale = remember { Animatable(1f) }
    val warpAlpha = remember { Animatable(1f) }

    // Intro text entrance animations
    val textYOffset = remember { Animatable(60f) }
    val textAlpha = remember { Animatable(0f) }
    
    // Core scale pulse
    val coreScale = remember { Animatable(0.2f) }

    // Automatic 4-second loading progress & cinematic soundscapes
    LaunchedEffect(Unit) {
        // Smoothly animate title elements into view at start
        launch {
            textYOffset.animateTo(0f, animationSpec = tween(1200, easing = EaseOutBack))
        }
        launch {
            textAlpha.animateTo(1f, animationSpec = tween(1000, easing = EaseOutQuad))
        }
        launch {
            coreScale.animateTo(1.0f, animationSpec = tween(1400, easing = EaseOutElastic))
        }

        val milestones = setOf(20, 40, 60, 80, 100)
        for (p in 1..100) {
            delay(40) // 100 * 40ms = 4 seconds total loading time
            loadingPercent = p
            if (p in milestones) {
                val freq = when (p) {
                    20 -> 261.63 // Low drone middle C
                    40 -> 329.63 // E4
                    60 -> 392.00 // G4
                    80 -> 523.25 // C5 chord progression
                    else -> 659.25 // E5 climax harmonic
                }
                com.example.audio.SoundSynth.playTone(freq, 100, "sine")
            }
        }
        
        // Brief moment of full compliance
        delay(400)
        isWarping = true
        
        // Deep synthetic propulsion sounds
        launch {
            for (f in 523..1200 step 35) {
                com.example.audio.SoundSynth.playTone(f.toDouble(), 35, "triangle")
                delay(15)
            }
            com.example.audio.SoundSynth.playTone(1320.00, 250, "sine")
        }

        // Warp cosmic leap forward out of the screen
        launch {
            warpScale.animateTo(3.5f, animationSpec = tween(700, easing = EaseInBack))
        }
        warpAlpha.animateTo(0f, animationSpec = tween(700, easing = EaseOutQuad))

        delay(700)
        onFinished()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "SplashAnims")

    // Double gear rotation angles
    val spinAngleLeft by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SpinLeft"
    )
    val spinAngleRight by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SpinRight"
    )

    // Breathing glow intensity
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowPulse"
    )

    // Fluid particle streams
    val starVerticalOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "StarsMove"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .scale(warpScale.value)
            .alpha(warpAlpha.value)
            .background(Color(0xFF030305)),
        contentAlignment = Alignment.Center
    ) {
        // High-density stellar viewport canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val count = 50
            for (i in 0 until count) {
                val xPos = (Math.sin(i.toDouble() * 311.12) * 0.5 + 0.5) * size.width
                val initialY = (Math.cos(i.toDouble() * 149.23) * 0.5 + 0.5) * size.height
                val yPos = ((initialY - starVerticalOffset) % size.height + size.height) % size.height

                val rawAlpha = (Math.sin(i.toDouble() + starVerticalOffset.toDouble() * 0.008) * 0.4 + 0.6)
                val dotColor = if (i % 3 == 0) NeonCyan else if (i % 3 == 1) NeonMagenta else NeonYellow
                drawCircle(
                    color = dotColor,
                    radius = if (i % 4 == 0) 2.5.dp.toPx() else 1.2.dp.toPx(),
                    center = Offset(xPos.toFloat(), yPos.toFloat()),
                    alpha = (rawAlpha * 0.35f * (loadingPercent / 100f + 0.2f)).toFloat()
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header presentation
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(top = 40.dp)
                    .offset(y = textYOffset.value.dp)
                    .alpha(textAlpha.value)
            ) {
                Text(
                    text = "SUMAN EPIC LABS PRESENTS",
                    fontSize = 11.sp,
                    color = MutedSlate,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 6.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .height(2.5.dp)
                        .width(64.dp)
                        .background(Brush.horizontalGradient(listOf(NeonCyan, Color.Transparent, NeonMagenta)))
                )
            }

            // Central Spectacular Visual Element (MADE BY SUMAN)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .scale(coreScale.value)
                    .alpha(textAlpha.value)
            ) {
                // Outer ring structure
                Box(
                    modifier = Modifier.size(240.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        // Cyan outer dash ring
                        drawCircle(
                            color = NeonCyan,
                            radius = size.width * 0.44f,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = 2.dp.toPx(),
                                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                                    floatArrayOf(50f, 70f),
                                    spinAngleLeft * 3f
                                )
                            ),
                            alpha = glowPulse * 0.7f
                        )

                        // Magenta middle dash ring
                        drawCircle(
                            color = NeonMagenta,
                            radius = size.width * 0.38f,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = 1.5.dp.toPx(),
                                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                                    floatArrayOf(80f, 40f),
                                    spinAngleRight * 2f
                                )
                            ),
                            alpha = glowPulse * 0.6f
                        )

                        // Ambient back glow
                        drawCircle(
                            color = NeonCyan,
                            radius = size.width * 0.32f,
                            alpha = 0.05f * glowPulse
                        )
                    }

                    // Floating core panel
                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(Color(0xFF0F111B), Color(0xFF040508))
                                )
                            )
                            .border(
                                BorderStroke(
                                    2.dp,
                                    Brush.linearGradient(listOf(NeonCyan, NeonMagenta, NeonYellow))
                                ), CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$loadingPercent%",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                color = PureWhite,
                                fontFamily = FontFamily.Monospace,
                                style = TextStyle(
                                    shadow = Shadow(
                                        color = NeonCyan,
                                        blurRadius = 15f
                                    )
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (loadingPercent < 100) "COMPILING" else "READY",
                                fontSize = 9.sp,
                                color = if (loadingPercent < 100) NeonYellow else NeonCyan,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 2.sp,
                                style = TextStyle(
                                    shadow = Shadow(
                                        color = if (loadingPercent < 100) NeonYellow else NeonCyan,
                                        blurRadius = 8f
                                    )
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))

                // The Prestigious "MADE BY SUMAN" signature layout
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.offset(y = (textYOffset.value * 0.5f).dp)
                ) {
                    Text(
                        text = "MADE BY",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MutedSlate,
                        letterSpacing = 4.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Box(contentAlignment = Alignment.Center) {
                        // Back Stereoscopic Neon Magenta Layer
                        Text(
                            text = "SUMAN",
                            fontSize = 58.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 6.sp,
                            color = NeonMagenta,
                            modifier = Modifier.offset(x = 3.dp, y = 3.dp)
                        )
                        // Back Stereoscopic Neon Cyan Blur Layer
                        Text(
                            text = "SUMAN",
                            fontSize = 58.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 6.sp,
                            color = NeonCyan.copy(alpha = 0.5f),
                            modifier = Modifier.offset(x = (-2).dp, y = (-2).dp),
                            style = TextStyle(
                                shadow = Shadow(
                                    color = NeonCyan,
                                    blurRadius = 25f
                                )
                            )
                        )
                        // Foreground Crisp White Layer
                        Text(
                            text = "SUMAN",
                            fontSize = 58.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 6.sp,
                            color = PureWhite
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "ULTIMATE SYNCHRONIZED GAME ENGINE",
                        fontSize = 9.sp,
                        color = MutedSlate,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                }
            }

            // Bottom loading status layout
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 50.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth(0.85f)
                ) {
                    // Modern styled tech progress bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFF14151B))
                            .border(BorderStroke(0.5.dp, Color(0xFF262833)), RoundedCornerShape(3.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(loadingPercent / 100f)
                                .fillMaxHeight()
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(NeonCyan, NeonMagenta, NeonYellow)
                                    )
                                )
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = if (loadingPercent < 100) {
                            "CONNECTING ULTRA PLATFORM PROTOCOLS..."
                        } else {
                            "SECURE HANDSHAKE COMPLETED"
                        },
                        fontSize = 9.sp,
                        color = if (loadingPercent < 100) MutedSlate else NeonCyan,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        style = TextStyle(
                            shadow = Shadow(
                                color = if (loadingPercent < 100) Color.Transparent else NeonCyan,
                                blurRadius = 10f
                            )
                        )
                    )
                }
            }
        }
    }
}

// Global cyber theme resolver mapping faction names to glow colors
@Composable
fun getThemeColors(themeName: String?): Pair<Color, Color> {
    return when (themeName?.uppercase() ?: "CYAN") {
        "CYAN" -> Pair(Color(0xFF00F2FE), Color(0xFFFE0979)) // Neon Cyan, Neon Magenta
        "MAGENTA" -> Pair(Color(0xFFFE0979), Color(0xFFFFE600)) // Neon Magenta, Neon Yellow
        "GREEN" -> Pair(Color(0xFF39FF14), Color(0xFF00F2FE)) // Laser Lime, Neon Cyan
        "AMBER" -> Pair(Color(0xFFFF9F0A), Color(0xFFFFD60A)) // Solar Energy, Intense Yellow
        "PURPLE" -> Pair(Color(0xFFBF5AF2), Color(0xFFFF2D55)) // Vapor Glow, Hot Pink
        else -> Pair(Color(0xFF00F2FE), Color(0xFFFE0979))
    }
}

@Composable
fun CyberOnboardingScreen(viewModel: GameViewModel) {
    var usernameInput by remember { mutableStateOf("") }
    var selectedTheme by remember { mutableStateOf("CYAN") }
    val themesList = listOf("CYAN", "MAGENTA", "GREEN", "AMBER", "PURPLE")
    
    val currentThemeColors = getThemeColors(selectedTheme)
    val appPrimary = currentThemeColors.first

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 24.dp)
        ) {
            Text(
                text = "NEURAL HARDWARE SYNC",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = appPrimary,
                letterSpacing = 4.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "NEON GRID",
                fontSize = 38.sp,
                fontWeight = FontWeight.Black,
                color = PureWhite,
                fontFamily = FontFamily.Monospace,
                style = TextStyle(
                    shadow = Shadow(
                        color = appPrimary,
                        blurRadius = 15f
                    )
                )
            )
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .padding(vertical = 16.dp),
            colors = CardDefaults.cardColors(containerColor = SlateCard),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, appPrimary.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "ENTER CYBERNETIC ALIAS",
                    color = appPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = usernameInput,
                    onValueChange = { usernameInput = it.take(12) },
                    placeholder = { Text("e.g. CyberRunner", color = MutedSlate.copy(alpha = 0.5f)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = PureWhite,
                        unfocusedTextColor = PureWhite,
                        focusedBorderColor = appPrimary,
                        unfocusedBorderColor = MutedSlate.copy(alpha = 0.4f),
                        focusedContainerColor = SlateDark,
                        unfocusedContainerColor = SlateDark
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("username_onboard_input")
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "SELECT FACTION LIGHTING",
                    color = appPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    themesList.forEach { th ->
                        val colors = getThemeColors(th)
                        val active = selectedTheme == th
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(colors.first)
                                .border(
                                    BorderStroke(
                                        if (active) 3.dp else 0.dp,
                                        if (active) PureWhite else Color.Transparent
                                    ),
                                    CircleShape
                                )
                                .clickable { selectedTheme = th }
                                .testTag("theme_btn_$th"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (active) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(colors.second)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = when (selectedTheme) {
                        "CYAN" -> "CYBR NEON (Cyan & Magenta Glow)"
                        "MAGENTA" -> "LASER SHIELD (Magenta & Yellow Glow)"
                        "GREEN" -> "ATOMIC MATRIX (Lime & Cyan Glow)"
                        "AMBER" -> "SOLAR CORES (Orange & Amber Glow)"
                        else -> "VOID SLINGERS (Purple & Pink Glow)"
                    },
                    color = MutedSlate,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        val isValid = usernameInput.trim().length >= 3
        Button(
            onClick = {
                if (isValid) {
                    viewModel.completeOnboarding(usernameInput, selectedTheme)
                }
            },
            enabled = isValid,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isValid) appPrimary else MutedSlate.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("submit_onboard_btn")
        ) {
            Text(
                text = "INITIALIZE COGNITIVE CONNECTION",
                color = if (isValid) PureWhite else MutedSlate,
                fontWeight = FontWeight.Black,
                fontSize = 12.sp,
                letterSpacing = 1.sp
            )
        }
    }
}
