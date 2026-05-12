package com.example.neuro

import android.content.Context
import android.content.SharedPreferences

object UserManager {

    private const val PREF_NAME = "user_prefs"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_ACCOUNT = "account"
    private const val KEY_NICKNAME = "nickname"
    private const val KEY_AVATAR = "avatar"
    private const val KEY_TOKEN = "token"
    private const val KEY_REFRESH_TOKEN = "refresh_token"
    private const val KEY_LOGIN_TIME = "login_time"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"

    private const val SESSION_DURATION = 7 * 24 * 60 * 60 * 1000L

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun saveLoginInfo(context: Context, userId: String, account: String, nickname: String, avatar: String, token: String, refreshToken: String) {
        getPrefs(context).edit().apply {
            putString(KEY_USER_ID, userId)
            putString(KEY_ACCOUNT, account)
            putString(KEY_NICKNAME, nickname)
            putString(KEY_AVATAR, avatar)
            putString(KEY_TOKEN, token)
            putString(KEY_REFRESH_TOKEN, refreshToken)
            putLong(KEY_LOGIN_TIME, System.currentTimeMillis())
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }
    }

    fun updateProfile(context: Context, nickname: String? = null, avatar: String? = null) {
        getPrefs(context).edit().apply {
            nickname?.let { putString(KEY_NICKNAME, it) }
            avatar?.let { putString(KEY_AVATAR, it) }
            apply()
        }
    }

    fun isLoggedIn(context: Context): Boolean {
        val prefs = getPrefs(context)
        if (!prefs.getBoolean(KEY_IS_LOGGED_IN, false)) {
            return false
        }

        val loginTime = prefs.getLong(KEY_LOGIN_TIME, 0)
        val currentTime = System.currentTimeMillis()

        return if (currentTime - loginTime > SESSION_DURATION) {
            clearLoginInfo(context)
            false
        } else {
            true
        }
    }

    fun getUserId(context: Context): String? {
        return getPrefs(context).getString(KEY_USER_ID, null)
    }

    fun getAccount(context: Context): String? {
        return getPrefs(context).getString(KEY_ACCOUNT, null)
    }

    fun getNickname(context: Context): String? {
        return getPrefs(context).getString(KEY_NICKNAME, null)
    }

    fun getAvatar(context: Context): String? {
        return getPrefs(context).getString(KEY_AVATAR, null)
    }

    fun getToken(context: Context): String? {
        return getPrefs(context).getString(KEY_TOKEN, null)
    }

    fun getRefreshToken(context: Context): String? {
        return getPrefs(context).getString(KEY_REFRESH_TOKEN, null)
    }

    fun updateToken(context: Context, token: String, refreshToken: String) {
        getPrefs(context).edit().apply {
            putString(KEY_TOKEN, token)
            putString(KEY_REFRESH_TOKEN, refreshToken)
            putLong(KEY_LOGIN_TIME, System.currentTimeMillis())
            apply()
        }
    }

    fun clearLoginInfo(context: Context) {
        getPrefs(context).edit().apply {
            remove(KEY_USER_ID)
            remove(KEY_ACCOUNT)
            remove(KEY_NICKNAME)
            remove(KEY_AVATAR)
            remove(KEY_TOKEN)
            remove(KEY_REFRESH_TOKEN)
            remove(KEY_LOGIN_TIME)
            putBoolean(KEY_IS_LOGGED_IN, false)
            apply()
        }
    }

    fun getRemainingDays(context: Context): Int {
        val loginTime = getPrefs(context).getLong(KEY_LOGIN_TIME, 0)
        val currentTime = System.currentTimeMillis()
        val elapsed = currentTime - loginTime
        val remaining = SESSION_DURATION - elapsed
        return if (remaining > 0) {
            (remaining / (24 * 60 * 60 * 1000L)).toInt()
        } else {
            0
        }
    }
}
