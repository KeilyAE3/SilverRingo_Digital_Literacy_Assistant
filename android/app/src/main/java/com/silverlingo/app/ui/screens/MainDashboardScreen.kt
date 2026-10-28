package com.silverlingo.app.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.silverlingo.app.data.UserProfile
import com.silverlingo.app.data.UserProfileManager
import com.silverlingo.app.service.FloatingCompanionService
import com.silverlingo.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboardScreen() {
    val context = LocalContext.current
    val profileManager = remember { UserProfileManager(context) }
    var userProfile by remember { mutableStateOf(profileManager.getProfile()) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var pendingAppPrompt by remember { mutableStateOf<AppLaunchPrompt?>(null) }
    var permissionRequiredPrompt by remember { mutableStateOf<AppLaunchPrompt?>(null) }

    var hasOverlayPermission by remember {
        mutableStateOf(
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else true
        )
    }

    var hasAccessibilityPermission by remember {
        mutableStateOf(
            checkAccessibilityPermissionState(context)
        )
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                hasOverlayPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    Settings.canDrawOverlays(context)
                } else true
                hasAccessibilityPermission = checkAccessibilityPermissionState(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "SilverLingo",
                            color = AmberAccent,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 24.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "실버링고",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showProfileDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "내 정보",
                            tint = AmberAccent
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NavyPrimary
                )
            )
        },
        containerColor = BackgroundLight
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // 1. 환영 인사 및 내 정보 배너
            item {
                UserProfileCard(
                    userProfile = userProfile,
                    onClick = { showProfileDialog = true }
                )
            }

            // 2. 다른 앱 위에 그리기 권한 및 도우미 실행 상태 배너
            item {
                CompanionStatusCard(
                    hasOverlayPermission = hasOverlayPermission,
                    hasAccessibilityPermission = hasAccessibilityPermission,
                    onRequestOverlayPermission = {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        }
                    },
                    onRequestAccessibilityPermission = {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        context.startActivity(intent)
                    },
                    onStartFloatingService = {
                        val intent = Intent(context, FloatingCompanionService::class.java)
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            context.startForegroundService(intent)
                        } else {
                            context.startService(intent)
                        }
                    }
                )
            }

            item {
                Text(
                    text = "오늘 병원 접수, 같이 해요",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    text = "아래 버튼을 누르시면 똑닥 화면 위에 도우미가 나타나 누를 곳을 순서대로 짚어드립니다.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // 3. 단일 서비스 진입: 병원 안심 당일 접수 (똑닥)
            item {
                ServiceActionCard(
                    icon = "🏥",
                    title = "병원 안심 당일 접수",
                    category = "똑닥 (DDocDoc) 병원 접수",
                    badgeText = "헛걸음 방지",
                    onClick = {
                        handleServiceClick(
                            context = context,
                            prompt = AppLaunchPrompt(
                                serviceTitle = "병원 안심 당일 접수",
                                appName = "똑닥",
                                packageName = "com.bbros.sayup",
                                webFallbackUrl = "https://www.ddocdoc.com/",
                                serviceKey = "ddocdoc"
                            ),
                            onShowPrompt = { pendingAppPrompt = it }
                        )
                    }
                )
            }

            // 4. 접수 순서 미리보기 (도우미가 짚어주는 순서와 동일)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "이렇게 진행돼요",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                        listOf(
                            "1. 맨 위 검색창 누르기",
                            "2. 아픈 곳·진료과·병원 이름 적고 검색",
                            "3. 원하시는 병원 고르기",
                            "4. 다른 분들의 후기 확인하기",
                            "5. [바로 접수] 누르기",
                            "6. 진료받으실 분 [본인] 선택",
                            "7. [접수 신청] 누르면 끝!"
                        ).forEach { step ->
                            Text(text = step, fontSize = 18.sp, color = TextPrimary)
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    if (showProfileDialog) {
        UserProfileDialog(
            currentProfile = userProfile,
            onDismiss = { showProfileDialog = false },
            onSave = { updated ->
                profileManager.saveProfile(updated)
                userProfile = updated
                showProfileDialog = false
            }
        )
    }

    // 공식 앱 실행/설치 또는 웹 동행 안내 다이얼로그
    pendingAppPrompt?.let { prompt ->
        AppInstallOrWebDialog(
            prompt = prompt,
            onDismiss = { pendingAppPrompt = null },
            onLaunchOrInstallApp = {
                val currentPrompt = prompt
                pendingAppPrompt = null
                if (currentPrompt.isAppInstalled) {
                    val overlayOk = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        Settings.canDrawOverlays(context)
                    } else true
                    val accessOk = checkAccessibilityPermissionState(context)

                    if (!overlayOk || !accessOk) {
                        permissionRequiredPrompt = currentPrompt
                    } else {
                        // 공식 앱 즉시 실행
                        try {
                            startFloatingService(context)
                            val launchIntent = context.packageManager.getLaunchIntentForPackage(currentPrompt.packageName)?.apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            if (launchIntent != null) {
                                context.startActivity(launchIntent)
                            }
                        } catch (_: Exception) {}
                    }
                } else {
                    // 플레이스토어 설치 페이지 연결
                    try {
                        val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${prompt.packageName}")).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(marketIntent)
                    } catch (_: Exception) {
                        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${prompt.packageName}")).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(webIntent)
                    }
                }
            },
            onContinueWeb = {
                pendingAppPrompt = null
                launchServiceWithOverlay(
                    context = context,
                    url = prompt.webFallbackUrl,
                    serviceKey = prompt.serviceKey,
                    serviceTitle = prompt.serviceTitle
                )
            }
        )
    }

    // 도우미 권한 미부여 시 친절 안내 모달 (태블릿 / Android 13+ 제한된 설정 완벽 대응)
    permissionRequiredPrompt?.let { prompt ->
        PermissionRequiredDialog(
            prompt = prompt,
            hasOverlayPermission = hasOverlayPermission,
            hasAccessibilityPermission = hasAccessibilityPermission,
            onDismiss = { permissionRequiredPrompt = null },
            onRequestOverlayPermission = {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                    context.startActivity(intent)
                }
            },
            onRequestAccessibilityPermission = {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                context.startActivity(intent)
            },
            onLaunchAppAnyway = {
                permissionRequiredPrompt = null
                try {
                    startFloatingService(context)
                    val launchIntent = context.packageManager.getLaunchIntentForPackage(prompt.packageName)?.apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    if (launchIntent != null) {
                        context.startActivity(launchIntent)
                    }
                } catch (_: Exception) {}
            },
            onContinueWeb = {
                permissionRequiredPrompt = null
                launchServiceWithOverlay(
                    context = context,
                    url = prompt.webFallbackUrl,
                    serviceKey = prompt.serviceKey,
                    serviceTitle = prompt.serviceTitle
                )
            }
        )
    }
}

data class AppLaunchPrompt(
    val serviceTitle: String,
    val appName: String,
    val packageName: String,
    val webFallbackUrl: String,
    val serviceKey: String,
    val isAppInstalled: Boolean = false
)

private fun checkAccessibilityPermissionState(context: Context): Boolean {
    return try {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: ""
        enabledServices.contains(context.packageName)
    } catch (_: Exception) {
        false
    }
}

private fun isPackageInstalled(context: Context, packageName: String): Boolean {
    return try {
        context.packageManager.getPackageInfo(packageName, 0)
        true
    } catch (_: Exception) {
        false
    }
}

private fun startFloatingService(context: Context) {
    try {
        val serviceIntent = Intent(context, FloatingCompanionService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    } catch (_: Exception) {}
}

private fun handleServiceClick(
    context: Context,
    prompt: AppLaunchPrompt,
    onShowPrompt: (AppLaunchPrompt) -> Unit
) {
    val installed = isPackageInstalled(context, prompt.packageName)
    onShowPrompt(prompt.copy(isAppInstalled = installed))
}

// 실제 사이트를 열면서 인앱 동행 브라우저 및 플로팅 오버레이 서비스를 가동
private fun launchServiceWithOverlay(
    context: Context,
    url: String,
    serviceKey: String,
    serviceTitle: String
) {
    // 1. 플로팅 오버레이 서비스 켜기 (백그라운드 위젯)
    startFloatingService(context)

    // 2. 실제 사이트 안심 동행 인앱 브라우저 실행
    val browserIntent = Intent(context, CompanionBrowserActivity::class.java).apply {
        putExtra("EXTRA_TARGET_URL", url)
        putExtra("EXTRA_SERVICE_KEY", serviceKey)
        putExtra("EXTRA_SERVICE_TITLE", serviceTitle)
    }
    context.startActivity(browserIntent)
}

@Composable
fun AppInstallOrWebDialog(
    prompt: AppLaunchPrompt,
    onDismiss: () -> Unit,
    onLaunchOrInstallApp: () -> Unit,
    onContinueWeb: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "📱", fontSize = 26.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (prompt.isAppInstalled) "[${prompt.appName}] 실행 방법 선택" else "[${prompt.appName}] 앱 안내",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = NavyPrimary
                )
            }
        },
        text = {
            Column {
                if (prompt.isAppInstalled) {
                    Text(
                        text = "스마트폰에 [${prompt.appName}] 공식 앱이 설치되어 있습니다.",
                        fontSize = 15.sp,
                        lineHeight = 21.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "[공식 앱으로 열기]를 누르시면 똑닥 화면 위에서 누를 곳을 순서대로 짚어드립니다.",
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = Color(0xFF475569)
                    )
                } else {
                    Text(
                        text = "${prompt.serviceTitle}은(는) 똑닥 공식 앱 또는 인터넷 화면으로 이용하실 수 있습니다.",
                        fontSize = 15.sp,
                        lineHeight = 21.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "공식 앱을 설치하시면 실버링고가 그 앱 화면 위에서 버튼 위치를 계속 짚어드립니다.\n어떻게 진행하시겠습니까?",
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = Color(0xFF475569)
                    )
                }
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onLaunchOrInstallApp,
                    colors = ButtonDefaults.buttonColors(containerColor = BlueAction),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text(
                        text = if (prompt.isAppInstalled) "📱 ${prompt.appName} 공식 앱으로 열기" else "📱 ${prompt.appName} 앱 다운로드하기",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                OutlinedButton(
                    onClick = onContinueWeb,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Text(
                        text = if (prompt.isAppInstalled) "🌐 인터넷 화면으로 열기 (웹 동행)" else "🌐 인터넷 화면으로 계속하기",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = NavyPrimary
                    )
                }
            }
        },
        dismissButton = null,
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White
    )
}

@Composable
fun PermissionRequiredDialog(
    prompt: AppLaunchPrompt,
    hasOverlayPermission: Boolean,
    hasAccessibilityPermission: Boolean,
    onDismiss: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onRequestAccessibilityPermission: () -> Unit,
    onLaunchAppAnyway: () -> Unit,
    onContinueWeb: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "⚠️", fontSize = 26.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "도우미 권한이 필요합니다",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = NavyPrimary
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "[${prompt.appName}] 화면 위에 노란 테두리와 말풍선을 띄우려면 아래 두 가지 권한을 먼저 켜주셔야 합니다.",
                    fontSize = 15.sp,
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )

                // 1. 다른 앱 위에 그리기 버튼
                Button(
                    onClick = onRequestOverlayPermission,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (hasOverlayPermission) Color(0xFF16A34A) else AmberAccent,
                        contentColor = if (hasOverlayPermission) Color.White else AmberText
                    )
                ) {
                    Text(
                        text = if (hasOverlayPermission) "✅ 1. [다른 앱 위에 그리기] 허용됨" else "👉 1. [다른 앱 위에 그리기] 지금 켜기",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                // 2. 접근성 서비스 버튼
                Button(
                    onClick = onRequestAccessibilityPermission,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (hasAccessibilityPermission) Color(0xFF16A34A) else BlueAction,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = if (hasAccessibilityPermission) "✅ 2. [접근성] 실버링고 켜짐" else "👉 2. [접근성] 실버링고 켜기 (필수)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                // 태블릿 / 안드로이드 13+ 제한된 설정 안내 배너
                Surface(
                    color = Color(0xFFFEF3C7),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "💡", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "태블릿 / 안드로이드 13+ 필수 확인",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF92400E)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "[접근성]에서 실버링고가 회색으로 비활성화(제한된 설정)되어 있는 경우:\n태블릿 설정 ➔ 애플리케이션 ➔ SilverLingo ➔ 우측 상단 점 3개(⋮) ➔ [제한된 설정 허용]을 먼저 눌러주세요.",
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            color = Color(0xFF78350F)
                        )
                    }
                }

                Text(
                    text = "💡 권한 설정 없이 바로 체험하시려면 아래의 [인터넷 화면으로 열기]를 누르시면 똑닥 단계별 가이드가 즉시 실행됩니다.",
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = Color(0xFF64748B)
                )
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (hasOverlayPermission && hasAccessibilityPermission) {
                    Button(
                        onClick = onLaunchAppAnyway,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text(
                            text = "🚀 준비 완료! [${prompt.appName}] 공식 앱 열기",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                OutlinedButton(
                    onClick = onContinueWeb,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Text(
                        text = "🌐 인터넷 화면으로 열기 (웹 동행)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = NavyPrimary
                    )
                }
            }
        },
        dismissButton = null,
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White
    )
}




@Composable
fun UserProfileCard(
    userProfile: UserProfile,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (userProfile.isRegistered) Color(0xFFEFF6FF) else Color(0xFFF1F5F9)
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(listOf(BlueAction, NavyPrimary))
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (userProfile.isRegistered) BlueAction else SurfaceBorder),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (userProfile.isRegistered) "👤" else "➕",
                    fontSize = 24.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                if (userProfile.isRegistered) {
                    Text(
                        text = "${userProfile.name} 어르신 (만 ${userProfile.calculateAge()}세)",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    Text(
                        text = "${userProfile.address.ifBlank { "주소 등록됨" }} · 기기 내 안전 보관",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                } else {
                    Text(
                        text = "내 기본 정보를 등록해 보세요",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    Text(
                        text = "한 번만 적어두시면 병원 접수할 때 편리합니다.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }
            }

            Text(
                text = if (userProfile.isRegistered) "수정 ›" else "입력 ›",
                color = BlueAction,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun CompanionStatusCard(
    hasOverlayPermission: Boolean,
    hasAccessibilityPermission: Boolean,
    onRequestOverlayPermission: () -> Unit,
    onRequestAccessibilityPermission: () -> Unit,
    onStartFloatingService: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NavyPrimary)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "🤖", fontSize = 28.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "실버링고 1:1 동행 시스템",
                        color = AmberAccent,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "똑닥 화면 위에서 누를 곳을 직접 짚어드립니다.",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 1. 권한 준비 완료 배너
            if (hasOverlayPermission && hasAccessibilityPermission) {
                Surface(
                    color = Color(0xFF14532D),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "✅", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "스마트폰 동행 준비 완료!",
                                color = Color(0xFF86EFAC),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "똑닥 앱을 켜시면 화면 위에서 버튼을 자동으로 짚어드립니다.",
                                color = Color(0xFFDCFCE7),
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            // 2. 화면 위에 그리기 권한
            if (!hasOverlayPermission) {
                Button(
                    onClick = onRequestOverlayPermission,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AmberAccent)
                ) {
                    Text(
                        text = "⚠️ 1. [다른 앱 위에 그리기] 권한 허용",
                        color = AmberText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 3. 접근성 권한 (외부 앱 화면 탐색용)
            if (!hasAccessibilityPermission) {
                OutlinedButton(
                    onClick = onRequestAccessibilityPermission,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.linearGradient(listOf(AmberAccent, AmberAccent))
                    )
                ) {
                    Text(
                        text = "⚙️ 2. [접근성 서비스] 실버링고 켜기 (필수)",
                        color = AmberAccent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "💡 삼성 갤럭시: 설정 화면 ➔ [설치된 앱] ➔ [SilverLingo 도우미] ➔ [사용 중] 켜기",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 3. 플로팅 도우미 상시 실행 버튼
            Button(
                onClick = onStartFloatingService,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BlueAction)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "둥둥 떠다니는 도우미 버블 띄우기",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}


@Composable
fun ServiceActionCard(
    icon: String,
    title: String,
    category: String,
    badgeText: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(BackgroundLight),
                contentAlignment = Alignment.Center
            ) {
                Text(text = icon, fontSize = 28.sp)
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = category,
                        fontSize = 13.sp,
                        color = TextMuted,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFFEF08A))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badgeText,
                            fontSize = 11.sp,
                            color = AmberText,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
            }

            Text(
                text = "시작 ›",
                color = BlueAction,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp
            )
        }
    }
}
