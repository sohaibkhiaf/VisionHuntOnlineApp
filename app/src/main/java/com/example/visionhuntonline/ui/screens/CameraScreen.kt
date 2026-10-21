package com.example.visionhuntonline.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.visionhuntonline.data.CocoTasks
import com.example.visionhuntonline.ui.components.CameraPreview
import com.example.visionhuntonline.ui.components.DetectionOverlay
import com.example.visionhuntonline.ui.viewmodel.GameViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    viewModel: GameViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToCompletion: () -> Unit
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    val gameState by viewModel.gameState.collectAsState()
    val rawDetections by viewModel.rawDetections.collectAsState()
    val gameDetections by viewModel.gameDetections.collectAsState()
    val inferenceTime by viewModel.inferenceTime.collectAsState()
    val inferenceError by viewModel.inferenceError.collectAsState()
    val isModelLoaded by viewModel.isModelLoaded.collectAsState()
    val progress by viewModel.gameProgress.collectAsState()

    LaunchedEffect(progress.isCompleted) {
        if (progress.isCompleted) {
            onNavigateToCompletion()
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "الكاميرا والاستدلال المباشر",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "العودة"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFFF8FAFC)
                    )
                )
            },
            containerColor = Color(0xFFF8FAFC)
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = "VISION HUNT",
                        color = Color(0xFF16A34A),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "اكتشف الأشياء",
                        color = Color(0xFF0F172A),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    Text(
                        text = "وجّه الكاميرا نحو الأشياء من حولك لاكتشافها.",
                        color = Color(0xFF64748B),
                        fontSize = 15.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                // Camera Preview Container
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(380.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color(0xFF0F172A)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (hasCameraPermission) {
                            CameraPreview(
                                onDetectionsReceived = { results, time ->
                                    viewModel.onDetectionsReceived(results, time)
                                },
                                onError = { error ->
                                    viewModel.onDetectorError(error)
                                }
                            )

                            DetectionOverlay(
                                detections = rawDetections,
                                modifier = Modifier.fillMaxSize()
                            )

                            // Status Badge Top Right
                            Row(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(14.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(0xCC0F172A))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (isModelLoaded) Color(0xFF86EFAC) else Color(0xFFEF4444))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isModelLoaded) "الكشف نشط (${inferenceTime}ms)" else "فشل النموذج",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Text(
                                    text = "نحتاج إلى إذن الكاميرا",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { launcher.launch(Manifest.permission.CAMERA) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                                ) {
                                    Text("السماح بالكاميرا")
                                }
                            }
                        }
                    }
                }

                // Info & Diagnostics Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "حالة نظام الكشف",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )

                            Text(
                                text = if (inferenceError != null)
                                    "خطأ الاستدلال: $inferenceError"
                                else
                                    "تم تحميل EfficientDet-Lite3 بنجاح.",
                                fontSize = 13.sp,
                                color = if (inferenceError != null) Color(0xFFB91C1C) else Color(0xFF475569),
                                modifier = Modifier.padding(top = 6.dp)
                            )

                            Text(
                                text = "الأجسام المكتشفة: ${rawDetections.size}",
                                fontSize = 13.sp,
                                color = Color(0xFF475569),
                                modifier = Modifier.padding(top = 4.dp)
                            )

                            Text(
                                text = "الأجسام المطابقة لمهام اللعبة: ${gameDetections.size}",
                                fontSize = 13.sp,
                                color = Color(0xFF475569),
                                modifier = Modifier.padding(top = 4.dp)
                            )

                            if (rawDetections.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "المكتشف الآن:",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF15803D)
                                )
                                rawDetections.take(5).forEach { detection ->
                                    Text(
                                        text = "• ${detection.label} — ${(detection.score * 100).toInt()}%",
                                        fontSize = 12.sp,
                                        color = Color(0xFF166534)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "حالة المهام المطلوبة:",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )

                            CocoTasks.GAME_TASKS.take(10).forEach { task ->
                                val completed = task.id in gameState.completedTaskIds
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = task.displayNameAr,
                                        fontSize = 13.sp,
                                        color = Color(0xFF475569)
                                    )
                                    Text(
                                        text = if (completed) "✓ مكتمل (+${task.points})" else "لم يُكتشف بعد",
                                        fontSize = 12.sp,
                                        fontWeight = if (completed) FontWeight.Bold else FontWeight.Normal,
                                        color = if (completed) Color(0xFF15803D) else Color(0xFF64748B)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
