package com.example.neuro.util

import com.example.neuro.Constants

object UrlUtils {
    
    fun normalize(url: String?): String {
        if (url.isNullOrEmpty()) return ""
        
        return url.replace(
            Constants.Network.PLACEHOLDER_IP,
            Constants.Network.REAL_IP
        )
    }
    
    fun normalizeAvatar(avatarUrl: String?): String {
        return normalize(avatarUrl)
    }
    
    fun normalizeCover(coverUrl: String?): String {
        return normalize(coverUrl)
    }
    
    fun isUrlValid(url: String?): Boolean {
        return !url.isNullOrBlank() && url.startsWith("http")
    }
}
