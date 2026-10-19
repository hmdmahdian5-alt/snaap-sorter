package com.delivery.assistant

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DashboardScreen(
                onOpenAccessibility = {
                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                },
                onToggleFloatingScanner = { enable ->
                    if (enable) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:$packageName")
                            )
                            startActivity(intent)
                        } else {
                            startService(Intent(this, FloatingScannerService::class.java))
                        }
                    } else {
                        stopService(Intent(this, FloatingScannerService::class.java))
                    }
                }
            )
        }
    }
}

@Composable
fun DashboardScreen(
    onOpenAccessibility: () -> Unit,
    onToggleFloatingScanner: (Boolean) -> Unit
) {
    var scannerEnabled by remember { mutableStateOf(false) }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text("سامانه هوشمند توزیع و سورت مرسولات", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onOpenAccessibility,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("فعال‌سازی سرویس اتوماسیون (Accessibility)")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("دوربین و اسکنر شناور بارکد")
                Switch(
                    checked = scannerEnabled,
                    onCheckedChange = {
                        scannerEnabled = it
                        onToggleFloatingScanner(it)
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider()
            Spacer(modifier = Modifier.height(8.dp))

            Text("لیست مقاصد بارگذاری شده (${StopRepository.destinationMap.size} مقصد):")

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(StopRepository.destinationMap.toList()) { (orderId, stopNum) ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("مقصد شماره: $stopNum")
                            Text("کد سفارش: $orderId")
                        }
                    }
                }
            }
        }
    }
}
