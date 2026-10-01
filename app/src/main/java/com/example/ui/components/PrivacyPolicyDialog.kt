package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.SaiBluePrimary

const val PRIVACY_POLICY_URL = "https://sparshaundhakar9090.github.io/privacy-policy"

@Composable
fun PrivacyPolicyDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = "Privacy Policy",
                tint = SaiBluePrimary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Privacy Policy",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                )
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(scrollState)
            ) {
                // Public URL Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        Text(
                            text = "Official Privacy Policy Web URL:",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = PRIVACY_POLICY_URL,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = SaiBluePrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                PolicySection(
                    title = "1. Introduction",
                    body = "Sparsh Aundhakar Intelligence (\"we\", \"our\", or \"the App\") respects and protects your privacy. This Privacy Policy outlines how your information is handled when using our AI Homework Solver application."
                )

                PolicySection(
                    title = "2. Information We Collect & Use",
                    body = "• Academic Questions: Text queries and equations you submit are processed exclusively to provide step-by-step educational explanations.\n" +
                            "• Homework Images: Photos captured via your camera or selected from your gallery are transmitted over secure HTTPS solely for visual homework analysis.\n" +
                            "• Account & Chat History: Saved locally on your device in an encrypted SQLite database to maintain your learning history."
                )

                PolicySection(
                    title = "3. Third-Party AI Services",
                    body = "The app utilizes Google Gemini AI APIs to generate educational tutoring content. Your submitted queries and images are not used to train public AI models, nor are they shared with third-party advertisers."
                )

                PolicySection(
                    title = "4. Device Permissions",
                    body = "• Camera: Requested strictly on-demand when you choose to take a photo of a textbook or worksheet problem.\n" +
                            "• Photos: Utilizes the zero-permission Android Photo Picker; the app never requests broad device storage access."
                )

                PolicySection(
                    title = "5. Children's Privacy",
                    body = "The application complies with student privacy requirements. We do not knowingly collect personally identifiable information from children under 13."
                )

                PolicySection(
                    title = "6. Developer Contact",
                    body = "If you have questions regarding this policy, contact the developer at:\nEmail: sparshaundhakar9090@gmail.com"
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.testTag("dialog_privacy_close")
            ) {
                Text("Close")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Privacy Policy URL", PRIVACY_POLICY_URL)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "Privacy Policy URL copied to clipboard!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.testTag("dialog_privacy_copy_url")
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Copy URL")
            }
        },
        modifier = modifier.testTag("privacy_policy_dialog")
    )
}

@Composable
private fun PolicySection(
    title: String,
    body: String
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )
        )
    }
}
