package com.example.voicebillai

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.voicebillai.ui.AppTab
import com.example.voicebillai.ui.VoiceBillViewModel
import com.example.voicebillai.ui.screens.BillingScreen
import com.example.voicebillai.ui.screens.CheckoutModal
import com.example.voicebillai.ui.screens.HistoryScreen
import com.example.voicebillai.ui.screens.KhataScreen
import com.example.voicebillai.ui.screens.SettingsScreen
import com.example.voicebillai.ui.theme.*

class MainActivity : ComponentActivity() {

    private val viewModel: VoiceBillViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            VoiceBillAITheme {
                VoiceBillApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun VoiceBillApp(viewModel: VoiceBillViewModel) {
    val context = LocalContext.current
    val activeTab by viewModel.activeTab.collectAsState()
    val showCheckout by viewModel.showCheckout.collectAsState()
    val activeBill by viewModel.activeBill.collectAsState()
    val settings by viewModel.settings.collectAsState()

    // Audio Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            viewModel.postMessage("Microphone permission needed for voice billing")
        }
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Main Screen Content
        when (activeTab) {
            AppTab.BILLING -> BillingScreen(
                viewModel = viewModel,
                onNavigateToCheckout = { viewModel.showCheckoutDialog(true) }
            )
            AppTab.HISTORY -> HistoryScreen(viewModel = viewModel)
            AppTab.KHATA -> KhataScreen(viewModel = viewModel)
            AppTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
        }

        // Floating Frosted Capsule Bottom Navigation Bar with All 4 Tabs
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .shadow(12.dp, RoundedCornerShape(32.dp), spotColor = Color(0x18000000))
                    .clip(RoundedCornerShape(32.dp))
                    .border(1.dp, GlassCardBorder, RoundedCornerShape(32.dp)),
                color = Color(0xF2FFFFFF) // Frosted milky glass
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FloatingNavItem(
                        icon = Icons.Outlined.Home,
                        label = "New Bill",
                        selected = (activeTab == AppTab.BILLING),
                        onClick = { viewModel.setTab(AppTab.BILLING) }
                    )

                    FloatingNavItem(
                        icon = Icons.Outlined.AccessTime,
                        label = "History",
                        selected = (activeTab == AppTab.HISTORY),
                        onClick = { viewModel.setTab(AppTab.HISTORY) }
                    )

                    FloatingNavItem(
                        icon = Icons.Outlined.MenuBook,
                        label = "Khata",
                        selected = (activeTab == AppTab.KHATA),
                        onClick = { viewModel.setTab(AppTab.KHATA) }
                    )

                    FloatingNavItem(
                        icon = Icons.Outlined.Settings,
                        label = "Settings",
                        selected = (activeTab == AppTab.SETTINGS),
                        onClick = { viewModel.setTab(AppTab.SETTINGS) }
                    )
                }
            }
        }
    }

    if (showCheckout) {
        CheckoutModal(
            bill = activeBill,
            settings = settings,
            onDismiss = { viewModel.showCheckoutDialog(false) },
            onFinalize = { mode -> viewModel.finalizeBill(mode) }
        )
    }
}

@Composable
fun FloatingNavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val activeColor = PrimaryGreen
    val inactiveColor = TextSecondary

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) activeColor else inactiveColor,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 11.5.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) activeColor else inactiveColor
        )
    }
}
