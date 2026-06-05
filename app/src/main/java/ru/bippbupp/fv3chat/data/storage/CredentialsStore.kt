package ru.bippbupp.fv3chat.data.storage

import android.content.Context

data class Credentials(
    val username: String,
    val password: String,
)

class CredentialsStore(context: Context) {
    private val preferences = context.getSharedPreferences("credentials", Context.MODE_PRIVATE)

    fun read(): Credentials? {
        val username = preferences.getString(KEY_USERNAME, null)?.takeIf { it.isNotBlank() }
        val password = preferences.getString(KEY_PASSWORD, null)?.takeIf { it.isNotBlank() }
        return if (username != null && password != null) {
            Credentials(username = username, password = password)
        } else {
            null
        }
    }

    fun save(username: String, password: String) {
        preferences.edit()
            .putString(KEY_USERNAME, username)
            .putString(KEY_PASSWORD, password)
            .apply()
    }

    fun clear() {
        preferences.edit().clear().apply()
    }

    private companion object {
        const val KEY_USERNAME = "username"
        const val KEY_PASSWORD = "password"
    }
}
