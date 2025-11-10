package com.example.cs388finalproject.util

object Validators {
    // ≥ 8 chars, at least 1 lower, 1 upper, 1 digit, 1 special
    private val PASSWORD_REGEX =
        Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#\$%^&*()_+\\-={}\\[\\]|:;\"'<>,.?/]).{8,}\$")

    fun isValidEmail(email: String) =
        android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()

    fun isStrongPassword(pw: String) = PASSWORD_REGEX.matches(pw)
}
