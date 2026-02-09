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
import androidx.compose.ui.graphics.Brush
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
import com.example.sentimentanalysis.data.SentimentViewModel
import com.example.sentimentanalysis.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: SentimentViewModel,
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    val userProfile by viewModel.userProfile.collectAsState()
    val history by viewModel.sentimentHistory.collectAsState()
    var isEditingName by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf(userProfile.name) }

    var showAvatarSheet by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // --- FIX: USE OPEN_DOCUMENT FOR PERMANENT PERMISSION ---
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            // 1. Grant Permanent Read Permission so it loads after restart
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(it, flags)

            // 2. Save to ViewModel (and Database)
            viewModel.setProfileImage(it)
            showAvatarSheet = false
        }
    }

    val defaultCharacters = listOf("🤖", "👽", "🦊", "🦁", "🐵", "🦄", "💀", "🎃", "🦸", "🥷", "🧙", "👼")

    Scaffold(
        containerColor = DarkBg,
        topBar = {
            TopAppBar(
                title = { Text("Profile & History", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back", tint = Color.White) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg)
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

            // 1. EDITABLE PROFILE IMAGE
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clickable { showAvatarSheet = true },
                contentAlignment = Alignment.Center
            ) {
                // The Image Circle
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(CardBg)
                        .border(2.dp, Brush.linearGradient(listOf(NeonPurple, NeonCyan)), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (userProfile.imageUriString != null) {
                        Image(
                            painter = rememberAsyncImagePainter(Uri.parse(userProfile.imageUriString)),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else if (userProfile.isGeneratedAvatar) {
                        Icon(Icons.Default.SmartToy, null, tint = NeonCyan, modifier = Modifier.size(60.dp))
                    } else if (userProfile.avatarEmoji != null) {
                        Text(userProfile.avatarEmoji!!, fontSize = 50.sp)
                    } else {
                        Icon(Icons.Default.Person, null, tint = Color.Gray, modifier = Modifier.size(60.dp))
                    }
                }

                // The "+" Plus Badge
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 8.dp, end = 8.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(NeonPurple)
                        .border(2.dp, DarkBg, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. EDITABLE NAME
            if (isEditingName) {
                OutlinedTextField(
                    value = editName,
                    onValueChange = { editName = it },
                    label = { Text("Your Name") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = Color.Gray
                    ),
                    trailingIcon = {
                        IconButton(onClick = {
                            viewModel.updateProfile(editName, userProfile.email)
                            isEditingName = false
                        }) {
                            Icon(Icons.Default.Check, "Save", tint = NeonCyan)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(0.8f)
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        userProfile.name,
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { isEditingName = true }) {
                        Icon(Icons.Default.Edit, "Edit", tint = Color.Gray, modifier = Modifier.size(18.dp))
                    }
                }
                Text(userProfile.email, color = Color.Gray)
            }

            Spacer(modifier = Modifier.height(30.dp))

            // 3. HISTORY LIST
            Text("Analysis History", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(10.dp))

            if (history.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    Text("No history yet.", color = Color.Gray)
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(history) { item ->
                        HistoryExpandableCard(item)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onLogout,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCF6679)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Log Out", color = Color.Black)
            }
        }

        // --- BOTTOM SHEET ---
        if (showAvatarSheet) {
            ModalBottomSheet(
                onDismissRequest = { showAvatarSheet = false },
                containerColor = CardBg
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Choose an Avatar", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(20.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        // Note: launch arguments for OpenDocument are different
                        AvatarOptionButton(Icons.Default.Image, "Upload Photo", NeonBlue) { imagePicker.launch(arrayOf("image/*")) }
                        AvatarOptionButton(Icons.Default.AutoAwesome, "Generate AI", NeonPurple) { viewModel.generateAiAvatar(); showAvatarSheet = false }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = Color.DarkGray)
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Or pick a character:", color = Color.Gray, modifier = Modifier.padding(bottom = 12.dp))

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
                                    .background(DarkBg)
                                    .clickable {
                                        viewModel.setProfileEmoji(char)
                                        showAvatarSheet = false
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(char, fontSize = 28.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }
}

// --- HELPER COMPONENTS ---

@Composable
fun AvatarOptionButton(icon: ImageVector, text: String, color: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(color.copy(alpha=0.15f))
                .border(1.dp, color.copy(alpha=0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(30.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text, color = Color.White, fontSize = 12.sp)
    }
}

@Composable
fun HistoryExpandableCard(item: SentimentDataPoint) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row {
                        Text(item.date, color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(item.timestamp, color = Color.Gray, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.text,
                        color = Color.White,
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
                    Text("Detailed Breakdown", color = Color.Gray, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    MiniEmotionBars(item.profile)
                }
            }
        }
    }
}

@Composable
fun MiniEmotionBars(profile: EmotionProfile) {
    Column {
        MiniBarRow("Happiness", profile.happiness, NeonGreen)
        MiniBarRow("Sadness", profile.sadness, NeonBlue)
        MiniBarRow("Anger", profile.anger, NeonRed)
    }
}

@Composable
fun MiniBarRow(label: String, pct: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Text(label, color = Color.Gray, fontSize = 10.sp, modifier = Modifier.width(60.dp))
        LinearProgressIndicator(
            progress = { pct / 100f },
            modifier = Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp)),
            color = color,
            trackColor = Color.DarkGray
        )
        Text("$pct%", color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(start=8.dp))
    }
}