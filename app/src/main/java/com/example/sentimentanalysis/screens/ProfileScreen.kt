package com.example.sentimentanalysis.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.sentimentanalysis.data.EmotionProfile
import com.example.sentimentanalysis.data.SentimentDataPoint
import com.example.sentimentanalysis.data.SentimentState
import com.example.sentimentanalysis.data.SentimentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: SentimentViewModel,
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    // --- STATE OBSERVATION ---
    val userProfile by viewModel.userProfile.collectAsState()

    val history by viewModel.sentimentHistory.collectAsState()

    val uiState by viewModel.uiState.collectAsState()

    var isEditingName by remember { mutableStateOf(false) }

    var editName by remember { mutableStateOf(userProfile.name) }

    var showAvatarSheet by remember { mutableStateOf(false) }

    // Prompt state for AI generation text bar
    var aiPrompt by remember { mutableStateOf("") }

    val context = LocalContext.current

    // --- IMAGE PICKER LOGIC ---
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION

                context.contentResolver.takePersistableUriPermission(it, takeFlags)

                viewModel.setProfileImage(it)

                showAvatarSheet = false
            } catch (e: Exception) {
                viewModel.setProfileImage(it)

                showAvatarSheet = false
            }
        }
    }

    val defaultCharacters = listOf(
        "🤖", "👽", "🦊", "🦁", "🐵", "🦄", "💀", "🎃", "🦸", "🥷", "🧙", "👼"
    )

    // --- UI CONTENT ---
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Profile & History",
                            color = Color.White
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Black
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // 1. PROFILE IMAGE (With Generating Design logic)
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clickable {
                            if (uiState !is SentimentState.Loading) {
                                showAvatarSheet = true
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .background(Color.DarkGray)
                            .border(
                                width = 2.dp,
                                color = Color.White,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // GENERATING DESIGN: Show spinner inside the circle
                        if (uiState is SentimentState.Loading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(40.dp),
                                strokeWidth = 3.dp
                            )
                        } else {
                            if (userProfile.imageUriString != null) {
                                Image(
                                    painter = rememberAsyncImagePainter(userProfile.imageUriString),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else if (userProfile.avatarEmoji != null) {
                                Text(
                                    text = userProfile.avatarEmoji!!,
                                    fontSize = 50.sp
                                )
                            } else if (userProfile.isGeneratedAvatar) {
                                Icon(
                                    imageVector = Icons.Default.SmartToy,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(60.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color.LightGray,
                                    modifier = Modifier.size(60.dp)
                                )
                            }
                        }
                    }

                    if (uiState !is SentimentState.Loading) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(bottom = 8.dp, end = 8.dp)
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(
                                    width = 2.dp,
                                    color = Color.Black,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2. NAME & LOGIN EMAIL
                if (isEditingName) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Your Name", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color.Gray,
                            cursorColor = Color.White
                        ),
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    viewModel.updateProfile(editName, userProfile.email)
                                    isEditingName = false
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Save",
                                    tint = Color.White
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(0.8f)
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = userProfile.name,
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )

                        IconButton(onClick = { isEditingName = true }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // FIXED: Now displays actual user login email
                    Text(
                        text = userProfile.email,
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(30.dp))

                // 3. HISTORY LIST
                Text(
                    text = "Analysis History",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (history.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No history yet.",
                            color = Color.Gray
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f)
                    ) {
                        items(history) { item ->
                            HistoryExpandableCardBW(item)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // LOGOUT BUTTON
                OutlinedButton(
                    onClick = onLogout,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Log Out",
                        color = Color.White
                    )
                }
            }

            // --- 4. AVATAR SHEET (With AI Logic) ---
            if (showAvatarSheet) {
                ModalBottomSheet(
                    onDismissRequest = {
                        if (uiState !is SentimentState.Loading) {
                            showAvatarSheet = false
                        }
                    },
                    containerColor = Color.Black,
                    contentColor = Color.White
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = "AI Avatar Generator",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Text input bar for AI Generation
                        OutlinedTextField(
                            value = aiPrompt,
                            onValueChange = { aiPrompt = it },
                            label = {
                                Text(
                                    text = "Describe your look (e.g. 'Space Fox')",
                                    color = Color.Gray
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color.White,
                                unfocusedBorderColor = Color.DarkGray
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // GENERATING DESIGN: Button shows spinner when loading
                        Button(
                            onClick = {
                                if (aiPrompt.isNotBlank()) {
                                    viewModel.generateAiAvatar(aiPrompt)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.DarkGray
                            ),
                            enabled = uiState !is SentimentState.Loading
                        ) {
                            if (uiState is SentimentState.Loading) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )

                                Spacer(modifier = Modifier.width(10.dp))

                                Text(text = "Generating...")
                            } else {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color.White
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = "Generate & Upload",
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        HorizontalDivider(color = Color.Gray)

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            AvatarOptionButtonBW(
                                icon = Icons.Default.Image,
                                text = "Upload Photo"
                            ) {
                                imagePicker.launch(arrayOf("image/*"))
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "Or pick a character:",
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(defaultCharacters) { char ->
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape)
                                        .background(Color.DarkGray)
                                        .clickable {
                                            viewModel.setProfileEmoji(char)
                                            showAvatarSheet = false
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = char,
                                        fontSize = 28.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(40.dp))
                    }
                }
            }
        }
    }
}

// --- HELPER COMPONENTS ---

@Composable
fun AvatarOptionButtonBW(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(Color.DarkGray)
                .border(
                    width = 1.dp,
                    color = Color.White,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(30.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = text,
            color = Color.White,
            fontSize = 12.sp
        )
    }
}

@Composable
fun HistoryExpandableCardBW(item: SentimentDataPoint) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1A1A)
        ),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = Color.DarkGray
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row {
                        Text(
                            text = item.date,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = item.timestamp,
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = item.text,
                        color = Color.LightGray,
                        maxLines = if (expanded) 10 else 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Expand",
                    tint = Color.Gray
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {

                    HorizontalDivider(color = Color.DarkGray)

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Detailed Breakdown",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // FIXED: Now shows all 6 emotions
                    MiniEmotionBarsBW(item.profile)
                }
            }
        }
    }
}

@Composable
fun MiniEmotionBarsBW(profile: EmotionProfile) {
    Column {
        MiniBarRowBW("Happiness", profile.happiness)

        MiniBarRowBW("Sadness", profile.sadness)

        MiniBarRowBW("Anger", profile.anger)

        MiniBarRowBW("Fear", profile.fear)

        MiniBarRowBW("Surprise", profile.surprise)

        MiniBarRowBW("Disgust", profile.disgust)
    }
}

@Composable
fun MiniBarRowBW(label: String, pct: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Text(
            text = label,
            color = Color.Gray,
            fontSize = 10.sp,
            modifier = Modifier.width(60.dp)
        )

        LinearProgressIndicator(
            progress = { pct / 100f },
            modifier = Modifier
                .weight(1f)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = Color.White,
            trackColor = Color.DarkGray
        )

        Text(
            text = "$pct%",
            color = Color.White,
            fontSize = 10.sp,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}