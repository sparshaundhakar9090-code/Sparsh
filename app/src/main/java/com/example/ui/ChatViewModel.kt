package com.example.ui

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ChatSessionEntity
import com.example.data.local.UserEntity
import com.example.data.repository.HomeworkRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

data class AuthUiState(
    val isLoggedIn: Boolean = false,
    val isLoading: Boolean = false,
    val isRegisterMode: Boolean = false,
    val errorMessage: String? = null
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    private val repository = HomeworkRepository(database.appDao())

    val currentUser: StateFlow<UserEntity?> = repository.currentUser

    private val _authUiState = MutableStateFlow(AuthUiState())
    val authUiState: StateFlow<AuthUiState> = _authUiState.asStateFlow()

    private val _currentSessionId = MutableStateFlow<String?>(null)
    val currentSessionId: StateFlow<String?> = _currentSessionId.asStateFlow()

    val sessions: StateFlow<List<ChatSessionEntity>> = repository.getAllSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentMessages: StateFlow<List<ChatMessageEntity>> = _currentSessionId.flatMapLatest { sessionId ->
        if (sessionId != null) {
            repository.getMessagesForSession(sessionId)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _selectedImageBitmap = MutableStateFlow<Bitmap?>(null)
    val selectedImageBitmap: StateFlow<Bitmap?> = _selectedImageBitmap.asStateFlow()

    private val _selectedImageBase64 = MutableStateFlow<String?>(null)
    val selectedImageBase64: StateFlow<String?> = _selectedImageBase64.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    init {
        checkAutoLogin()
    }

    private fun checkAutoLogin() {
        viewModelScope.launch {
            val autoLoggedIn = repository.tryAutoLogin()
            if (autoLoggedIn) {
                _authUiState.value = _authUiState.value.copy(isLoggedIn = true)
                ensureActiveSession()
            }
        }
    }

    fun toggleAuthMode() {
        _authUiState.value = _authUiState.value.copy(
            isRegisterMode = !_authUiState.value.isRegisterMode,
            errorMessage = null
        )
    }

    fun login(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _authUiState.value = _authUiState.value.copy(errorMessage = "Please enter both username and password")
            return
        }

        viewModelScope.launch {
            _authUiState.value = _authUiState.value.copy(isLoading = true, errorMessage = null)
            val result = repository.login(username, password)
            if (result.isSuccess) {
                _authUiState.value = _authUiState.value.copy(isLoggedIn = true, isLoading = false, errorMessage = null)
                ensureActiveSession()
            } else {
                _authUiState.value = _authUiState.value.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "Login failed"
                )
            }
        }
    }

    fun register(username: String, password: String, displayName: String) {
        if (username.isBlank() || password.isBlank()) {
            _authUiState.value = _authUiState.value.copy(errorMessage = "Please fill in all required fields")
            return
        }

        viewModelScope.launch {
            _authUiState.value = _authUiState.value.copy(isLoading = true, errorMessage = null)
            val result = repository.register(username, password, displayName)
            if (result.isSuccess) {
                _authUiState.value = _authUiState.value.copy(isLoggedIn = true, isLoading = false, errorMessage = null)
                ensureActiveSession()
            } else {
                _authUiState.value = _authUiState.value.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "Registration failed"
                )
            }
        }
    }

    fun continueAsGuest() {
        viewModelScope.launch {
            val guestResult = repository.register("student_sparsh", "sparsh123", "Sparsh Student")
            val user = guestResult.getOrElse {
                repository.login("student_sparsh", "sparsh123").getOrNull()
            }
            if (user != null) {
                _authUiState.value = _authUiState.value.copy(isLoggedIn = true, errorMessage = null)
                ensureActiveSession()
            }
        }
    }

    fun logout() {
        repository.logout()
        _authUiState.value = AuthUiState(isLoggedIn = false)
        _currentSessionId.value = null
    }

    private fun ensureActiveSession() {
        viewModelScope.launch {
            if (_currentSessionId.value == null) {
                val newId = repository.createNewSession("Sparsh Homework Session")
                _currentSessionId.value = newId
            }
        }
    }

    fun createNewChat() {
        viewModelScope.launch {
            val newId = repository.createNewSession("New Homework")
            _currentSessionId.value = newId
            _selectedImageBitmap.value = null
            _selectedImageBase64.value = null
            _inputText.value = ""
        }
    }

    fun selectSession(sessionId: String) {
        _currentSessionId.value = sessionId
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
            if (_currentSessionId.value == sessionId) {
                createNewChat()
            }
        }
    }

    fun clearCurrentChat() {
        val currentId = _currentSessionId.value ?: return
        viewModelScope.launch {
            repository.deleteSession(currentId)
            val newId = repository.createNewSession("New Homework")
            _currentSessionId.value = newId
            _toastMessage.value = "Chat cleared"
        }
    }

    fun updateInputText(newText: String) {
        _inputText.value = newText
    }

    fun onImageSelected(bitmap: Bitmap) {
        _selectedImageBitmap.value = bitmap
        viewModelScope.launch(Dispatchers.Default) {
            val base64 = bitmapToBase64(bitmap)
            _selectedImageBase64.value = base64
        }
    }

    fun onImageUriSelected(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val originalBitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (originalBitmap != null) {
                    // Resize to standard resolution for optimal Gemini vision analysis
                    val scaledBitmap = scaleBitmapDown(originalBitmap, 1024)
                    _selectedImageBitmap.value = scaledBitmap
                    _selectedImageBase64.value = bitmapToBase64(scaledBitmap)
                }
            } catch (e: Exception) {
                _toastMessage.value = "Failed to load image: ${e.message}"
            }
        }
    }

    fun clearSelectedImage() {
        _selectedImageBitmap.value = null
        _selectedImageBase64.value = null
    }

    fun sendPrompt() {
        val text = _inputText.value.trim()
        val imageB64 = _selectedImageBase64.value

        if (text.isBlank() && imageB64.isNullOrBlank()) {
            return
        }

        val sessionId = _currentSessionId.value ?: return

        _isGenerating.value = true
        _inputText.value = ""
        val tempImage = imageB64
        _selectedImageBitmap.value = null
        _selectedImageBase64.value = null

        viewModelScope.launch {
            repository.solveAndSave(sessionId, text, tempImage)
            _isGenerating.value = false
        }
    }

    fun selectPresetSubject(subjectTitle: String, sampleQuestion: String) {
        _inputText.value = sampleQuestion
    }

    fun copyToClipboard(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val cleanText = com.example.util.MathTextCleaner.cleanMathFormatting(text)
        val clip = ClipData.newPlainText("AI Homework Solution", cleanText)
        clipboard.setPrimaryClip(clip)
        _toastMessage.value = "Solution copied to clipboard!"
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    private fun scaleBitmapDown(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val originalWidth = bitmap.width
        val originalHeight = bitmap.height
        var resizedWidth = maxDimension
        var resizedHeight = maxDimension

        if (originalHeight > originalWidth) {
            resizedHeight = maxDimension
            resizedWidth = (resizedHeight * originalWidth.toFloat() / originalHeight.toFloat()).toInt()
        } else if (originalWidth > originalHeight) {
            resizedWidth = maxDimension
            resizedHeight = (resizedWidth * originalHeight.toFloat() / originalWidth.toFloat()).toInt()
        } else {
            resizedHeight = maxDimension
            resizedWidth = maxDimension
        }
        return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, true)
    }
}
