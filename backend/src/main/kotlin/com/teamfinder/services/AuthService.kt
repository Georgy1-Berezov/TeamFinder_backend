package com.teamfinder.services

import com.teamfinder.models.* // Импортируем User, UserRole и т.д.
import com.teamfinder.repositories.UserRepository
import com.teamfinder.security.JwtConfig
import java.time.LocalDateTime

/**
 * Service for handling user authentication and registration
 */
class AuthService(
    private val jwtConfig: JwtConfig,
    private val userRepository: UserRepository = UserRepository()
) {
    
    /**
     * Register a new user
     */
    suspend fun register(username: String, email: String, password: String): AuthResult {
        // 1. Check if email exists
        if (userRepository.findByEmail(email) != null) {
            return AuthResult.Error("User with this email already exists")
        }
        
        // 2. Check if username exists
        if (userRepository.findByUsername(username) != null) {
            return AuthResult.Error("Username is already taken")
        }
        
        // 3. Validate password complexity
        if (!isPasswordValid(password)) {
            return AuthResult.Error("Password must be at least 8 characters long and contain both letters and numbers")
        }
        
        // 4. Create NEW user object (Sychronized with 10-table schema)
        val user = User(
            username = username,
            email = email,
            firstName = username, // Use username as default first name
            role = UserRole.USER,
            isActive = true,
            createdAt = LocalDateTime.now().toString() // Convert to String for API consistency
        )
        
        val created = userRepository.create(user, password)
        
        return if (created != null) {
            AuthResult.Success(
                accessToken = jwtConfig.generateAccessToken(created),
                refreshToken = jwtConfig.generateRefreshToken(created.id!!),
                user = created
            )
        } else {
            AuthResult.Error("Failed to create user account")
        }
    }
    
    /**
     * User login
     */
    suspend fun login(email: String, password: String): AuthResult {
        val user = userRepository.validateCredentials(email, password)
        
        return if (user != null) {
            userRepository.updateLastLogin(user.id!!)
            AuthResult.Success(
                accessToken = jwtConfig.generateAccessToken(user),
                refreshToken = jwtConfig.generateRefreshToken(user.id),
                user = user
            )
        } else {
            AuthResult.Error("Invalid email or password")
        }
    }
    
    /**
     * Refresh access token
     */
    suspend fun refreshToken(refreshToken: String): AuthResult {
        val userId = jwtConfig.extractUserId(refreshToken)
            ?: return AuthResult.Error("Invalid refresh token")
        
        val user = userRepository.findById(userId)
            ?: return AuthResult.Error("User not found")
        
        return AuthResult.Success(
            accessToken = jwtConfig.generateAccessToken(user),
            refreshToken = jwtConfig.generateRefreshToken(userId),
            user = user
        )
    }
    
    /**
     * Password validation logic
     */
    private fun isPasswordValid(password: String): Boolean {
        return password.length >= 8 && 
               password.any { it.isLetter() } && 
               password.any { it.isDigit() }
    }
}

/**
 * Result of authentication operations
 */
sealed class AuthResult {
    data class Success(
        val accessToken: String,
        val refreshToken: String,
        val user: User
    ) : AuthResult()
    
    data class Error(val message: String) : AuthResult()
}