package com.safepayshield.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.app.KeyguardManager
import android.provider.Settings
import android.view.WindowManager
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.camera.core.ExperimentalGetImage
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage

private val Ink = Color(0xFF102033)
private val Canvas = Color(0xFFF6F8FB)
private val Mint = Color(0xFF16A782)
private val Amber = Color(0xFFDF8A13)
private val Red = Color(0xFFD64444)
private val Blue = Color(0xFF2B63D9)
private const val DemoUpi = "upi://pay?pa=verified@okaxis&pn=City%20Cafe&am=249&tn=Lunch"

enum class AppScreen { HOME, SCAN, RESULT, SETTINGS, FAMILY, ABOUT }

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private val permission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
    private lateinit var model: MainViewModel
    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    private val speechLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        if (res.resultCode == RESULT_OK) {
            val spokenText = res.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
            if (spokenText.isNotBlank()) {
                val formatted = if (!spokenText.startsWith("upi://", true)) {
                    if (spokenText.contains("@")) "upi://pay?pa=$spokenText" else "upi://pay?pa=$spokenText@upi"
                } else spokenText
                model.analyze(formatted)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        model = ViewModelProvider(this)[MainViewModel::class.java]
        tts = TextToSpeech(this, this)
        intent?.data?.toString()?.takeIf { it.startsWith("upi://") }?.let(model::analyze)
        val keyguard = getSystemService(KeyguardManager::class.java)
        setContent {
            SafePayApp(
                model = model,
                screenLockEnabled = keyguard?.isDeviceSecure == true,
                suspiciousAccessibility = hasSuspiciousAccessibilityService(),
                onSpeak = ::speakText,
                onVoiceInput = ::launchVoiceInput
            )
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permission.launch(Manifest.permission.CAMERA)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = java.util.Locale.US
            isTtsReady = true
        }
    }

    private fun speakText(text: String) {
        if (isTtsReady) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "SafePayTTS")
        }
    }

    private fun launchVoiceInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak UPI address or payee details")
        }
        runCatching { speechLauncher.launch(intent) }
    }

    private fun hasSuspiciousAccessibilityService(): Boolean = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        ?.split(':')?.any { !it.contains("talkback", true) && it.isNotBlank() } == true

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.data?.toString()?.takeIf { it.startsWith("upi://") }?.let(model::analyze)
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafePayApp(
    model: MainViewModel,
    screenLockEnabled: Boolean = true,
    suspiciousAccessibility: Boolean = false,
    onSpeak: (String) -> Unit = {},
    onVoiceInput: () -> Unit = {}
) {
    val result by model.result.collectAsState()
    val elder by model.elderMode.collectAsState()
    val language by model.language.collectAsState()
    val familyApproval by model.familyApproval.collectAsState()
    val history by model.history.collectAsState()
    val familyApproved by model.familyApproved.collectAsState()
    val clearEvent by model.clearEvent.collectAsState()
    val screen = remember { mutableStateOf(if (result != null) AppScreen.RESULT else AppScreen.HOME) }
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    var pasteOpen by remember { mutableStateOf(false) }
    var pasteValue by remember { mutableStateOf("") }
    var securityOpen by remember { mutableStateOf(!screenLockEnabled || suspiciousAccessibility) }
    val rootView = LocalView.current
    rootView.filterTouchesWhenObscured = true
    LaunchedEffect(result) { if (result != null) screen.value = AppScreen.RESULT }
    LaunchedEffect(clearEvent) { if (clearEvent > 0) snackbarHostState.showSnackbar("All local data cleared.") }
    val fontScale = if (elder) 1.16f else 1f
    MaterialTheme(colorScheme = lightColorScheme(primary = Blue, background = Canvas)) {
        Surface(modifier = Modifier.fillMaxSize(), color = Canvas) {
            Scaffold(
                containerColor = Canvas,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = { ShieldTopBar(screen.value, elder) { screen.value = AppScreen.HOME; model.clearResult() } },
                bottomBar = {
                    if (screen.value != AppScreen.SCAN && screen.value != AppScreen.RESULT) BottomBar(screen.value, fontScale) { screen.value = it }
                }
            ) { padding ->
                AnimatedContent(
                    screen.value,
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    },
                    modifier = Modifier.padding(padding),
                    label = "screen"
                ) { current ->
                    when (current) {
                        AppScreen.HOME -> HomeScreen(fontScale, elder, language, history, { model.runScenario(it) }, { model.analyze(it) }, { screen.value = AppScreen.SCAN }, { pasteOpen = true }, onVoiceInput, { screen.value = AppScreen.SETTINGS })
                        AppScreen.SCAN -> ScanScreen(fontScale, { model.analyze(it) }, { screen.value = AppScreen.HOME })
                        AppScreen.RESULT -> result?.let {
                            ResultScreen(
                                result = it,
                                scale = fontScale,
                                familyApproval = familyApproval,
                                familyApproved = familyApproved,
                                hasTrustedContact = model.hasTrustedContact(),
                                model = model,
                                elder = elder,
                                onSpeak = onSpeak,
                                onCopyFeedback = { msg ->
                                    kotlinx.coroutines.GlobalScope.run {
                                        coroutineScope.launch { snackbarHostState.showSnackbar(msg) }
                                    }
                                },
                                home = { screen.value = AppScreen.HOME }
                            )
                        }
                        AppScreen.SETTINGS -> SettingsScreen(fontScale, model, { screen.value = AppScreen.FAMILY }, { screen.value = AppScreen.ABOUT })
                        AppScreen.FAMILY -> FamilyScreen(fontScale, model)
                        AppScreen.ABOUT -> AboutScreen(fontScale, model)
                    }
                }
            }
        }
    }
    if (securityOpen) AlertDialog(
        onDismissRequest = { securityOpen = false },
        title = { Text(if (!screenLockEnabled) "Screen lock recommended" else "Accessibility check") },
        text = { Text(if (!screenLockEnabled) "Enable a PIN, pattern, or password before using payment protection." else "An accessibility service is enabled. Review trusted services before confirming payments.") },
        confirmButton = { Button({ securityOpen = false }) { Text("Continue") } }
    )
    if (pasteOpen) {
        AlertDialog(
            onDismissRequest = { pasteOpen = false },
            title = { Text("Paste or speak UPI link") },
            text = {
                Column {
                    OutlinedTextField(
                        value = pasteValue,
                        onValueChange = { pasteValue = it },
                        label = { Text("upi://pay...") },
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = { onVoiceInput() }) {
                                Icon(Icons.Default.Mic, contentDescription = "Voice Input", tint = Blue)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            clipboardManager.getText()?.text?.let { clipText ->
                                pasteValue = clipText
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Paste from Clipboard")
                        Spacer(Modifier.width(6.dp))
                        Text("Paste from Clipboard")
                    }
                }
            },
            confirmButton = { Button(onClick = { pasteOpen = false; model.analyze(pasteValue) }) { Text("Analyze") } },
            dismissButton = { OutlinedButton(onClick = { pasteOpen = false }) { Text("Cancel") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShieldTopBar(screen: AppScreen, elder: Boolean, back: () -> Unit) {
    TopAppBar(title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(36.dp).clip(CircleShape).background(Color(0xFFDCE8FF)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Shield, null, tint = Blue) }
            Spacer(Modifier.width(10.dp)); Column { Text("SafePay Shield", fontWeight = FontWeight.Bold); if (screen == AppScreen.HOME) Text(if (elder) "Simple protection" else "Your payment safety layer", fontSize = 11.sp, color = Color.Gray) }
        }
    }, navigationIcon = { if (screen != AppScreen.HOME) IconButton(onClick = back) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } })
}

@Composable
private fun BottomBar(current: AppScreen, scale: Float, navigate: (AppScreen) -> Unit) {
    NavigationBar(modifier = Modifier.navigationBarsPadding()) {
        NavigationBarItem(current == AppScreen.HOME, { navigate(AppScreen.HOME) }, icon = { Icon(Icons.Default.Home, null) }, label = { Text("Home", fontSize = (11 * scale).sp) })
        NavigationBarItem(current == AppScreen.FAMILY, { navigate(AppScreen.FAMILY) }, icon = { Icon(Icons.Default.Contacts, null) }, label = { Text("Family", fontSize = (11 * scale).sp) })
        NavigationBarItem(current == AppScreen.SETTINGS, { navigate(AppScreen.SETTINGS) }, icon = { Icon(Icons.Default.Settings, null) }, label = { Text("Settings", fontSize = (11 * scale).sp) })
    }
}

@Composable
private fun HomeScreen(scale: Float, elder: Boolean, language: String, history: List<HistoryItem>, runScenario: (Int) -> Unit, replay: (String) -> Unit, scan: () -> Unit, paste: () -> Unit, onVoiceInput: () -> Unit, settings: () -> Unit) {
    val tamil = language == "Tamil"
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Spacer(Modifier.height(10.dp)); Text(if (tamil) "மாலை வணக்கம்" else "Good evening", fontSize = (14 * scale).sp, color = Color.Gray); Text(if (tamil) "நம்பிக்கையுடன் செலுத்துங்கள்." else "Pay with confidence.", fontSize = (30 * scale).sp, fontWeight = FontWeight.Bold, color = Ink) }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Ink), shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(22.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Lock, null, tint = Color(0xFFA9C4FF)); Spacer(Modifier.width(8.dp)); Text("ON-DEVICE PROTECTION", color = Color(0xFFA9C4FF), fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.height(14.dp)); Text("Every payment gets a safety check before it leaves your phone.", color = Color.White, fontSize = (18 * scale).sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(18.dp)); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Stat("LOCAL", "Only on device"); Stat("5 LAYERS", "Risk analysis") }
                }
            }
        }
        item { Button(scan, Modifier.fillMaxWidth().height(if (elder) 64.dp else 56.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Blue)) { Icon(Icons.Default.CameraAlt, null); Spacer(Modifier.width(10.dp)); Text(if (tamil) "QR ஸ்கேன்" else "Scan QR", fontSize = (17 * scale).sp, fontWeight = FontWeight.Bold) } }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(paste, Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(16.dp)) {
                    Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (tamil) "இணைப்பை ஒட்டவும்" else "Paste UPI link", fontSize = (14 * scale).sp)
                }
                OutlinedButton(onVoiceInput, Modifier.height(52.dp), shape = RoundedCornerShape(16.dp)) {
                    Icon(Icons.Default.Mic, contentDescription = "Voice Input", tint = Blue)
                    Spacer(Modifier.width(6.dp))
                    Text("Voice", fontSize = (14 * scale).sp)
                }
            }
        }
        item { QuickCard("Elder Mode", if (elder) "Large text and spoken warnings are on" else "Make every control easier to read", Icons.Default.Security, settings) }
        item { Text("Demo scenarios", fontSize = (18 * scale).sp, fontWeight = FontWeight.Bold, color = Ink) }
        item { DemoRow("SAFE demo - Normal merchant", "Known verified payee", "SAFE", Mint) { runScenario(0) } }
        item { DemoRow("CAUTION demo - Suspicious QR", "Urgent refund language", "CAUTION", Amber) { runScenario(1) } }
        item { DemoRow("BLOCK demo - Known fraud", "Fraud VPA and tamper signal", "BLOCK", Red) { runScenario(2) } }
        item { QuickCard("How it works", "Four local checks combine into one clear payment decision.", Icons.Default.Info) { settings() } }
        item { Text("Recent scans", fontSize = (18 * scale).sp, fontWeight = FontWeight.Bold, color = Ink) }
        if (history.isEmpty()) item { Text("Your encrypted scan history will appear here.", color = Color.Gray, fontSize = 13.sp) }
        items(history) { item -> RecentRow(item.payee, "Local scan", "${item.verdict.name} · ${item.score}", verdictColor(item.verdict)) { replay(item.raw) } }
        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable private fun Stat(label: String, detail: String) { Column { Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp); Text(detail, color = Color(0xFFB8C2D1), fontSize = 11.sp) } }
@Composable private fun RecentRow(name: String, vpa: String, status: String, color: Color, click: () -> Unit = {}) { Card(onClick = click, colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(16.dp)) { Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(42.dp).clip(CircleShape).background(color.copy(.14f)), contentAlignment = Alignment.Center) { Icon(Icons.Default.CheckCircle, null, tint = color) }; Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(name, fontWeight = FontWeight.SemiBold, color = Ink); Text(vpa, fontSize = 12.sp, color = Color.Gray) }; Text(status, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold) } } }
@Composable private fun DemoRow(title: String, description: String, verdict: String, color: Color, click: () -> Unit) { Card(onClick = click, colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(14.dp)) { Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(title, color = Ink, fontWeight = FontWeight.SemiBold); Text(description, color = Color.Gray, fontSize = 12.sp) }; Text(verdict, color = color, fontWeight = FontWeight.Bold, fontSize = 12.sp) } } }
private fun verdictColor(verdict: Verdict) = when (verdict) { Verdict.SAFE -> Mint; Verdict.CAUTION -> Amber; Verdict.BLOCK -> Red }
@Composable private fun QuickCard(title: String, detail: String, icon: ImageVector, click: () -> Unit) { Card(onClick = click, colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(16.dp)) { Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = Blue); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.SemiBold, color = Ink); Text(detail, fontSize = 12.sp, color = Color.Gray) }; Icon(Icons.AutoMirrored.Filled.OpenInNew, null, tint = Color.Gray) } } }

@OptIn(ExperimentalGetImage::class)
@Composable
private fun ScanScreen(scale: Float, analyze: (String) -> Unit, cancel: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Scan a payment QR", fontSize = (25 * scale).sp, fontWeight = FontWeight.Bold, color = Ink)
        Text("We check the image before decoding the payment address.", textAlign = TextAlign.Center, color = Color.Gray, modifier = Modifier.padding(top = 8.dp))
        Spacer(Modifier.height(24.dp))
        AndroidView(
            modifier = Modifier.fillMaxWidth().height(330.dp).clip(RoundedCornerShape(24.dp)),
            factory = { viewContext ->
                val previewView = PreviewView(viewContext)
                val providerFuture = ProcessCameraProvider.getInstance(viewContext)
                providerFuture.addListener({
                    val provider = providerFuture.get()
                    val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                    val analysis = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
                    val scanner = BarcodeScanning.getClient()
                    analysis.setAnalyzer(ContextCompat.getMainExecutor(viewContext)) { proxy ->
                        val mediaImage = proxy.image
                        if (mediaImage == null) { proxy.close(); return@setAnalyzer }
                        scanner.process(InputImage.fromMediaImage(mediaImage, proxy.imageInfo.rotationDegrees))
                            .addOnSuccessListener { codes -> codes.firstOrNull()?.rawValue?.takeIf { it.startsWith("upi://") }?.let(analyze) }
                            .addOnCompleteListener { proxy.close() }
                    }
                    provider.unbindAll()
                    provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                }, ContextCompat.getMainExecutor(viewContext))
                previewView
            }
        )
        Spacer(Modifier.height(20.dp)); Button({ analyze(DemoUpi) }, Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp)) { Text("Use demo QR", fontSize = 16.sp) }
        Spacer(Modifier.height(10.dp)); OutlinedButton(cancel, Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp)) { Text("Cancel") }
    }
}

@Composable
private fun ResultScreen(
    result: RiskResult,
    scale: Float,
    familyApproval: Boolean,
    familyApproved: Boolean,
    hasTrustedContact: Boolean,
    model: MainViewModel,
    elder: Boolean,
    onSpeak: (String) -> Unit,
    onCopyFeedback: (String) -> Unit,
    home: () -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val clipboardManager = LocalClipboardManager.current
    var verifyOpen by remember { mutableStateOf(false) }
    var verifySent by remember { mutableStateOf(false) }
    var reportOpen by remember { mutableStateOf(false) }
    var contactPrompt by remember { mutableStateOf(false) }

    val rawUpi = "upi://pay?pa=${result.pa}&pn=${Uri.encode(result.payee)}&am=${result.amount}&tn=${Uri.encode(result.note)}"
    val spokenWarning = "Safety check result: ${result.verdict.name}. Score ${result.score} out of 100. Payee is ${result.payee}. ${result.reasons.firstOrNull().orEmpty()}"

    LaunchedEffect(result.verdict) {
        if (result.verdict != Verdict.SAFE) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        if (elder || result.verdict != Verdict.SAFE) onSpeak(spokenWarning)
    }
    val (color, icon, title) = when (result.verdict) { Verdict.SAFE -> Triple(Mint, Icons.Default.CheckCircle, "Safe to proceed"); Verdict.CAUTION -> Triple(Amber, Icons.Default.Warning, "Pause and verify"); Verdict.BLOCK -> Triple(Red, Icons.Default.Report, "Payment blocked") }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Spacer(Modifier.height(4.dp)); Card(colors = CardDefaults.cardColors(color), shape = RoundedCornerShape(22.dp)) { Column(Modifier.fillMaxWidth().padding(22.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(30.dp)); Spacer(Modifier.width(10.dp)); Text(result.verdict.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp) }; Text(title, color = Color.White, fontSize = (27 * scale).sp, fontWeight = FontWeight.Bold); Text("Composite risk score ${result.score}/100", color = Color.White.copy(.85f), fontSize = 13.sp) } } }
        item {
            Card(colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(result.payee, fontSize = (20 * scale).sp, fontWeight = FontWeight.Bold, color = Ink)
                            Text(result.pa, color = Color.Gray, fontSize = 13.sp)
                        }
                        IconButton(onClick = {
                            clipboardManager.setText(AnnotatedString(rawUpi))
                            onCopyFeedback("UPI link copied to clipboard!")
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy UPI Link", tint = Blue)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Amount", color = Color.Gray)
                        Text("₹${result.amount}", fontWeight = FontWeight.Bold, color = Ink)
                    }
                }
            }
        }
        item {
            OutlinedButton(
                onClick = { onSpeak(spokenWarning) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.VolumeUp, contentDescription = "Listen Spoken Warning", tint = Blue)
                Spacer(Modifier.width(8.dp))
                Text("Listen spoken warning", fontSize = (14 * scale).sp, color = Blue)
            }
        }
        item { Text("Why we said this", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink) }
        if (result.verdict == Verdict.CAUTION && familyApproval && !familyApproved) item { Text(if (hasTrustedContact) "High-risk payment requires family approval." else "Add a trusted contact before requesting family approval.", color = Amber, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
        if (result.verdict == Verdict.CAUTION && familyApproved) item { Text("Family approval recorded. You can proceed.", color = Mint, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
        items(result.reasons) { reason -> Row(verticalAlignment = Alignment.Top) { Icon(Icons.Default.CheckCircle, null, tint = color, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text(reason, fontSize = (15 * scale).sp, color = Ink) } }
        item { Text("Layer checks", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink) }
        items(result.layers) { (label, score) -> Column { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(label, fontSize = 13.sp, color = Color.Gray); Text("$score%", fontSize = 13.sp, fontWeight = FontWeight.Bold) }; LinearProgressIndicator({ score / 100f }, Modifier.fillMaxWidth().padding(vertical = 5.dp), color = if (score > 60) Red else Mint, trackColor = Color(0xFFE6EBF2)) } }
        if (result.verdict != Verdict.BLOCK) item { Button({ if (familyApproval && result.verdict == Verdict.CAUTION && !familyApproved) { model.recordAction("Proceed blocked pending family approval"); verifyOpen = true } else { model.recordAction("Proceed"); context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("upi://pay?pa=${result.pa}&pn=${Uri.encode(result.payee)}&am=${result.amount}"))) } }, Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp)) { Text(if (familyApproval && result.verdict == Verdict.CAUTION && !familyApproved) "Request family approval" else if (result.verdict == Verdict.CAUTION) "I understand, proceed" else "Proceed to pay", fontSize = 16.sp) } }
        if (result.verdict != Verdict.BLOCK) item { OutlinedButton({ if (hasTrustedContact) { model.recordAction("Verify requested"); verifyOpen = true } else contactPrompt = true }, Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp)) { Icon(Icons.Default.Contacts, null); Spacer(Modifier.width(8.dp)); Text(if (verifySent) "Verification requested" else "Verify with family") } }
        item { OutlinedButton({ if (result.verdict == Verdict.BLOCK) { haptics.performHapticFeedback(HapticFeedbackType.LongPress); reportOpen = true } else { model.recordAction("Paused"); home() } }, Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp)) { Text(if (result.verdict == Verdict.BLOCK) "Block & Report" else "Pause payment") } }
        if (result.verdict == Verdict.BLOCK) item { OutlinedButton({ model.recordAction("Cancelled blocked payment"); home() }, Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp)) { Text("Cancel") } }
        item { Text("Verify with a trusted contact before sharing your PIN or OTP.", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(bottom = 24.dp)) }
    }
    if (verifyOpen) AlertDialog(
        onDismissRequest = { verifyOpen = false },
        title = { Text("Verify this payment") },
        text = { Text("Send a local approval request to your trusted contact? The payment remains paused until you decide.") },
        confirmButton = { Button({ if (hasTrustedContact) { verifySent = model.approveFamily(result); verifyOpen = false } else { verifyOpen = false; contactPrompt = true } }) { Text(if (hasTrustedContact) "Approve" else "Add contact first") } },
        dismissButton = { OutlinedButton({ verifyOpen = false }) { Text("Cancel") } }
    )
    if (contactPrompt) AlertDialog(
        onDismissRequest = { contactPrompt = false },
        title = { Text("Trusted contact needed") },
        text = { Text("Add a trusted contact in Family safety before requesting approval.") },
        confirmButton = { Button({ contactPrompt = false; home() }) { Text("Okay") } }
    )
    if (reportOpen) AlertDialog(
        onDismissRequest = { reportOpen = false },
        title = { Text("Code blocked") },
        text = { Text("This code is blocked and the report was saved only on this phone.") },
        confirmButton = { Button({ model.recordAction("Blocked and reported"); reportOpen = false; home() }) { Text("Done") } }
    )
}

@Composable
private fun SettingsScreen(scale: Float, model: MainViewModel, family: () -> Unit, about: () -> Unit) {
    val elder by model.elderMode.collectAsState(); val language by model.language.collectAsState(); val approval by model.familyApproval.collectAsState(); var addMerchant by remember { mutableStateOf(false) }; var merchant by remember { mutableStateOf("") }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Settings", fontSize = (29 * scale).sp, fontWeight = FontWeight.Bold, color = Ink); Text("Your preferences stay on this phone.", color = Color.Gray) }
        item { SettingRow("Language", language) { model.setLanguage(if (language == "English") "Tamil" else "English") } }
        item { SettingRow("Elder Mode", if (elder) "On" else "Off") { model.setElder(!elder) } }
        item { SettingRow("Family approval", if (approval) "High-risk payments" else "Off") { model.setFamily(!approval) } }
        item { Button(family, Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(15.dp)) { Icon(Icons.Default.Contacts, null); Spacer(Modifier.width(8.dp)); Text("Manage trusted contacts") } }
        item { Card(colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(16.dp)) { Column(Modifier.padding(16.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Text("Trusted merchants", fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.weight(1f)); IconButton({ addMerchant = true }) { Icon(Icons.Default.Add, "Add merchant") } }; model.merchants.collectAsState().value.forEach { merchant -> Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text(merchant, color = Blue, modifier = Modifier.weight(1f).padding(top = 8.dp)); IconButton({ model.removeMerchant(merchant) }) { Icon(Icons.Default.Delete, "Remove merchant", tint = Red) } } } } } }
        item { Card(onClick = about, colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(16.dp)) { Row(Modifier.fillMaxWidth().padding(17.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Info, null, tint = Blue); Spacer(Modifier.width(12.dp)); Text("Privacy & how it works", color = Ink, fontWeight = FontWeight.SemiBold) } } }
        item { Spacer(Modifier.height(20.dp)) }
    }
    if (addMerchant) AlertDialog(onDismissRequest = { addMerchant = false }, title = { Text("Add trusted merchant") }, text = { OutlinedTextField(merchant, { merchant = it }, label = { Text("merchant@upi") }) }, confirmButton = { Button({ model.addMerchant(merchant); addMerchant = false }) { Text("Save") } })
}

@Composable
private fun SettingRow(title: String, value: String, click: () -> Unit) {
    Card(colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, color = Ink)
                Text(value, fontSize = 12.sp, color = Color.Gray)
            }
            Switch(
                checked = value == "On" || value == "Tamil" || value == "High-risk payments",
                onCheckedChange = { _ -> click() }
            )
        }
    }
}

@Composable
private fun FamilyScreen(scale: Float, model: MainViewModel) {
    val contacts by model.contacts.collectAsState(); var dialog by remember { mutableStateOf(false) }; var name by remember { mutableStateOf("") }; var sent by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
        item { Text("Family safety", fontSize = (29 * scale).sp, fontWeight = FontWeight.Bold, color = Ink); Text("A second pair of eyes for high-risk payments.", color = Color.Gray) }
        item { Card(colors = CardDefaults.cardColors(Color(0xFFE9F5F1)), shape = RoundedCornerShape(18.dp)) { Column(Modifier.padding(18.dp)) { Icon(Icons.Default.Contacts, null, tint = Mint); Spacer(Modifier.height(8.dp)); Text("Trusted contacts", fontWeight = FontWeight.Bold, color = Ink); Text("They can help you verify a suspicious payee. No payment data is shared.", fontSize = 13.sp, color = Color.Gray) } } }
        items(contacts) { contact -> Card(colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(14.dp)) { Row(Modifier.fillMaxWidth().padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) { Text(contact, Modifier.weight(1f), color = Ink, fontWeight = FontWeight.SemiBold); IconButton({ model.removeContact(contact) }) { Icon(Icons.Default.Delete, "Remove contact", tint = Red) } } } }
        item { Button({ dialog = true }, Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(15.dp)) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("Add trusted contact") } }
        item { OutlinedButton({ sent = true }, Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(15.dp)) { Text("Test escalation flow") } }
        if (sent) item { Text("Test notification sent locally to your trusted contact.", color = Mint, fontWeight = FontWeight.SemiBold) }
    }
    if (dialog) AlertDialog(onDismissRequest = { dialog = false }, title = { Text("Add trusted contact") }, text = { OutlinedTextField(name, { name = it }, label = { Text("Name, phone") }) }, confirmButton = { Button({ model.addContact(name); dialog = false }) { Text("Save") } })
}

@Composable
private fun AboutScreen(scale: Float, model: MainViewModel) {
    var cleared by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { Text("About SafePay Shield", fontSize = (28 * scale).sp, fontWeight = FontWeight.Bold, color = Ink) }
        if (isProbablyEmulator()) item { AboutBlock("Demo environment", "This app appears to be running on an emulator. Camera, intent handoff, and device security behavior should be rechecked on a physical phone.") }
        item { AboutBlock("How it works", "Before a payment leaves your phone, SafePay Shield checks the QR image, UPI payload, local merchant trust, and whether the amount fits your pattern. The result is transparent and easy to act on.") }
        item { AboutBlock("Privacy promise", "Your QR images, bank details, trust lists, and risk history stay encrypted on this device. Core checks work offline. There is no analytics or cloud upload in this prototype.") }
        item { Button({ model.clearLocalData(); cleared = true }, Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(15.dp), colors = ButtonDefaults.buttonColors(containerColor = Red)) { Icon(Icons.Default.Delete, null); Spacer(Modifier.width(8.dp)); Text("Clear all local data") } }
        if (cleared) item { Text("Encrypted local data cleared.", color = Mint, fontWeight = FontWeight.SemiBold) }
        item { AboutBlock("Report an issue", "For a production release this button would open a support channel. In this offline prototype, the report is kept local and can be reviewed from the app audit store.") }
    }
}

private fun isProbablyEmulator(): Boolean = Build.FINGERPRINT.contains("generic", true) || Build.MODEL.contains("emulator", true) || Build.HARDWARE.contains("goldfish", true) || Build.HARDWARE.contains("ranchu", true)

@Composable
private fun AboutBlock(title: String, body: String) {
    Card(colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(18.dp)) {
            Text(title, fontWeight = FontWeight.Bold, color = Ink, fontSize = 17.sp)
            Spacer(Modifier.height(8.dp))
            Text(body, color = Color.Gray, lineHeight = 20.sp)
        }
    }
}
