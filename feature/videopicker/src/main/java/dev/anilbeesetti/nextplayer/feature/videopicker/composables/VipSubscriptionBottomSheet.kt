package dev.anilbeesetti.nextplayer.feature.videopicker.composables

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.QrCode
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class VipPlan(
    val title: String,
    val duration: String,
    val price: String,
    val amount: String,
    val discountTag: String? = null,
) {
    MONTHLY(
        title = "Super Monthly",
        duration = "1 Month",
        price = "₹99",
        amount = "99.00"
    ),
    ANNUAL(
        title = "Premium Annual",
        duration = "12 Months",
        price = "₹499",
        amount = "499.00",
        discountTag = "58% OFF • BEST VALUE"
    )
}

data class UpiAppItem(
    val name: String,
    val packageName: String,
    val color: Color,
    val badge: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VipSubscriptionBottomSheet(
    onDismissRequest: () -> Unit,
    onSubscriptionActivated: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedPlan by remember { mutableStateOf(VipPlan.ANNUAL) }
    var showQrDialog by remember { mutableStateOf(false) }

    val upiApps = remember {
        listOf(
            UpiAppItem("Google Pay", "com.google.android.apps.nbu.paisa.user", Color(0xFF4285F4), "GPay"),
            UpiAppItem("PhonePe", "com.phonepe.app", Color(0xFF5F259F), "PhonePe"),
            UpiAppItem("Paytm", "net.one97.paytm", Color(0xFF00B9F5), "Paytm"),
            UpiAppItem("BHIM UPI", "in.org.npci.upiapp", Color(0xFF00796B), "BHIM")
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Color(0xFF0E1118),
        tonalElevation = 8.dp,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(42.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.25f))
            )
        },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFFFFD700), Color(0xFFFF8C00))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.WorkspacePremium,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "NextPlayer VIP",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Unlock Cinema Streaming & 4K Quality",
                            fontSize = 12.sp,
                            color = Color(0xFF9EABB8)
                        )
                    }
                }
                IconButton(onClick = onDismissRequest) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = Color.White.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Plan Selection Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                VipPlan.entries.forEach { plan ->
                    val isSelected = selectedPlan == plan
                    val borderColor by animateColorAsState(
                        targetValue = if (isSelected) Color(0xFFFFB800) else Color(0xFF222938),
                        label = "planBorderColor"
                    )
                    val bgColor = if (isSelected) Color(0xFF191D28) else Color(0xFF12151E)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(bgColor)
                            .border(BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor), RoundedCornerShape(16.dp))
                            .clickable { selectedPlan = plan }
                            .padding(14.dp)
                    ) {
                        Column {
                            if (plan.discountTag != null) {
                                Surface(
                                    color = Color(0xFFFFB800),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.padding(bottom = 6.dp)
                                ) {
                                    Text(
                                        text = plan.discountTag,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.Black,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = plan.title,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) Color.White else Color(0xFFB0BAC7)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = plan.price,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color(0xFFFFD700) else Color.White
                                )
                                Text(
                                    text = "/${plan.duration}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF8692A6),
                                    modifier = Modifier.padding(bottom = 2.dp, start = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Feature Highlights
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF131722))
                    .border(BorderStroke(1.dp, Color(0xFF222838)), RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FeatureRow(text = "4K Ultra HD & 60fps Smooth Playback")
                FeatureRow(text = "Dolby Atmos & Spatial Audio Equalizer")
                FeatureRow(text = "Zero Ads & High-Speed Telegram Cloud Stream")
                FeatureRow(text = "Unlimited Offline Video Downloads & Sync")
            }

            Spacer(modifier = Modifier.height(20.dp))

            // UPI Payment Section
            Text(
                text = "PAY VIA INSTANT UPI",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF8E9BB0),
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 4 UPI App Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                upiApps.forEach { upi ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF161B26))
                            .border(BorderStroke(1.dp, Color(0xFF263044)), RoundedCornerShape(12.dp))
                            .clickable {
                                launchUpiPayment(
                                    context = context,
                                    plan = selectedPlan,
                                    packageName = upi.packageName
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = upi.badge,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = upi.color
                            )
                            Text(
                                text = "Instant",
                                fontSize = 9.sp,
                                color = Color(0xFF758195)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // QR Code / Other UPI Option
            Button(
                onClick = {
                    launchUpiPayment(context = context, plan = selectedPlan, packageName = null)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1E2535),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.QrCode,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Pay via Any UPI App / QR Code (${selectedPlan.price})",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Instant Activation Simulation Button (for immediate feedback)
            Button(
                onClick = {
                    Toast.makeText(context, "VIP Activated Successfully!", Toast.LENGTH_SHORT).show()
                    onSubscriptionActivated()
                    onDismissRequest()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFB800),
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Star,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Activate VIP Instantly",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun FeatureRow(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Rounded.CheckCircle,
            contentDescription = null,
            tint = Color(0xFF38B6FF),
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = 12.sp,
            color = Color(0xFFD6DFEC)
        )
    }
}

private fun launchUpiPayment(context: Context, plan: VipPlan, packageName: String?) {
    try {
        val upiUri = Uri.parse(
            "upi://pay?pa=nextplayer.vip@upi&pn=NextPlayer%20VIP&am=${plan.amount}&cu=INR&tn=VIP%20Subscription%20${plan.duration}"
        )
        val intent = Intent(Intent.ACTION_VIEW, upiUri).apply {
            if (!packageName.isNullOrBlank()) {
                setPackage(packageName)
            }
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(
            context,
            "Could not open UPI app. You can also pay via QR code.",
            Toast.LENGTH_SHORT
        ).show()
    }
}
