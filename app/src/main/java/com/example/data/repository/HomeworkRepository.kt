package com.example.data.repository

import com.example.data.local.AppDao
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ChatSessionEntity
import com.example.data.local.UserEntity
import com.example.data.network.GeminiApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.UUID

class HomeworkRepository(
    private val appDao: AppDao,
    private val geminiApiClient: GeminiApiClient = GeminiApiClient()
) {
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser = _currentUser.asStateFlow()

    init {
        // Default guest student session if none logged in
    }

    suspend fun tryAutoLogin(): Boolean = withContext(Dispatchers.IO) {
        val lastUser = appDao.getLastUser()
        if (lastUser != null) {
            _currentUser.value = lastUser
            true
        } else {
            false
        }
    }

    suspend fun login(username: String, password: String): Result<UserEntity> = withContext(Dispatchers.IO) {
        val user = appDao.getUserByUsername(username.trim())
        if (user != null) {
            if (user.passwordHash == password.trim()) {
                _currentUser.value = user
                Result.success(user)
            } else {
                Result.failure(Exception("Incorrect password. Please try again."))
            }
        } else {
            Result.failure(Exception("User not found. Please register an account."))
        }
    }

    suspend fun register(username: String, password: String, displayName: String): Result<UserEntity> = withContext(Dispatchers.IO) {
        val trimmedUsername = username.trim()
        val trimmedPassword = password.trim()
        val trimmedName = displayName.trim().ifBlank { trimmedUsername }

        if (trimmedUsername.isBlank()) {
            return@withContext Result.failure(Exception("Username cannot be empty"))
        }
        if (trimmedPassword.length < 4) {
            return@withContext Result.failure(Exception("Password must be at least 4 characters"))
        }

        val existing = appDao.getUserByUsername(trimmedUsername)
        if (existing != null) {
            return@withContext Result.failure(Exception("Username is already taken"))
        }

        val newUser = UserEntity(
            username = trimmedUsername,
            passwordHash = trimmedPassword,
            displayName = trimmedName
        )
        val id = appDao.insertUser(newUser)
        val savedUser = newUser.copy(id = id)
        _currentUser.value = savedUser
        Result.success(savedUser)
    }

    fun logout() {
        _currentUser.value = null
    }

    // Sessions & Messages
    fun getAllSessions(): Flow<List<ChatSessionEntity>> = appDao.getAllSessionsFlow()

    fun getMessagesForSession(sessionId: String): Flow<List<ChatMessageEntity>> =
        appDao.getMessagesForSessionFlow(sessionId)

    suspend fun createNewSession(title: String = "New Homework"): String = withContext(Dispatchers.IO) {
        val newSessionId = UUID.randomUUID().toString()
        val session = ChatSessionEntity(
            id = newSessionId,
            title = title,
            subject = "General"
        )
        appDao.insertSession(session)
        newSessionId
    }

    suspend fun deleteSession(sessionId: String) = withContext(Dispatchers.IO) {
        appDao.deleteMessagesForSession(sessionId)
        appDao.deleteSessionById(sessionId)
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        appDao.deleteAllMessages()
        appDao.deleteAllSessions()
    }

    suspend fun solveAndSave(
        sessionId: String,
        userPrompt: String,
        imageBase64: String?
    ): Result<String> = withContext(Dispatchers.IO) {
        // 1. Save user message to Room
        val userMsg = ChatMessageEntity(
            sessionId = sessionId,
            isUser = true,
            text = userPrompt,
            imageBase64 = imageBase64,
            timestamp = System.currentTimeMillis()
        )
        appDao.insertMessage(userMsg)

        // Update session title if default
        val existingSession = appDao.getSessionById(sessionId)
        if (existingSession != null && (existingSession.title == "New Homework" || existingSession.title.isBlank())) {
            val autoTitle = if (userPrompt.isNotBlank()) {
                userPrompt.take(30) + if (userPrompt.length > 30) "..." else ""
            } else {
                "Homework Image Solution"
            }
            appDao.updateSession(existingSession.copy(title = autoTitle, updatedAt = System.currentTimeMillis()))
        }

        // 2. Call Gemini AI API
        val aiResult = geminiApiClient.solveHomework(userPrompt, imageBase64)
        val rawResponseText = aiResult.getOrDefault("Could not generate answer. Please try again.")
        val cleanedResponseText = com.example.util.MathTextCleaner.cleanMathFormatting(rawResponseText)

        // 3. Save AI message to Room
        val aiMsg = ChatMessageEntity(
            sessionId = sessionId,
            isUser = false,
            text = cleanedResponseText,
            timestamp = System.currentTimeMillis()
        )
        appDao.insertMessage(aiMsg)

        Result.success(cleanedResponseText)
    }
}
