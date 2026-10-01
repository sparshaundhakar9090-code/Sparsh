package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.ChatViewModel
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.MainChatScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    HomeworkAppContent()
                }
            }
        }
    }
}

@Composable
fun HomeworkAppContent(
    chatViewModel: ChatViewModel = viewModel()
) {
    val context = LocalContext.current
    val authUiState by chatViewModel.authUiState.collectAsStateWithLifecycle()
    val currentUser by chatViewModel.currentUser.collectAsStateWithLifecycle()
    val messages by chatViewModel.currentMessages.collectAsStateWithLifecycle()
    val sessions by chatViewModel.sessions.collectAsStateWithLifecycle()
    val currentSessionId by chatViewModel.currentSessionId.collectAsStateWithLifecycle()
    val inputText by chatViewModel.inputText.collectAsStateWithLifecycle()
    val selectedImageBitmap by chatViewModel.selectedImageBitmap.collectAsStateWithLifecycle()
    val isGenerating by chatViewModel.isGenerating.collectAsStateWithLifecycle()
    val toastMessage by chatViewModel.toastMessage.collectAsStateWithLifecycle()

    if (!authUiState.isLoggedIn) {
        LoginScreen(
            authUiState = authUiState,
            onLogin = { username, password ->
                chatViewModel.login(username, password)
            },
            onRegister = { username, password, displayName ->
                chatViewModel.register(username, password, displayName)
            },
            onGuestLogin = {
                chatViewModel.continueAsGuest()
            },
            onToggleMode = {
                chatViewModel.toggleAuthMode()
            }
        )
    } else {
        MainChatScreen(
            currentUser = currentUser,
            messages = messages,
            sessions = sessions,
            currentSessionId = currentSessionId,
            inputText = inputText,
            selectedImageBitmap = selectedImageBitmap,
            isGenerating = isGenerating,
            toastMessage = toastMessage,
            onUpdateInputText = { chatViewModel.updateInputText(it) },
            onSendPrompt = { chatViewModel.sendPrompt() },
            onSelectImageBitmap = { chatViewModel.onImageSelected(it) },
            onSelectImageUri = { chatViewModel.onImageUriSelected(context, it) },
            onClearSelectedImage = { chatViewModel.clearSelectedImage() },
            onNewChat = { chatViewModel.createNewChat() },
            onClearChat = { chatViewModel.clearCurrentChat() },
            onSelectSession = { chatViewModel.selectSession(it) },
            onDeleteSession = { chatViewModel.deleteSession(it) },
            onLogout = { chatViewModel.logout() },
            onCopyText = { chatViewModel.copyToClipboard(context, it) },
            onClearToast = { chatViewModel.clearToast() }
        )
    }
}
