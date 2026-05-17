package com.example.neuro.util

import com.example.neuro.Constants

object UrlUtils {
    
    private const val BASE_URL = "http://47.118.22.220:8080"
    
    fun normalize(url: String?): String {
        if (url.isNullOrEmpty()) return ""
        
        var normalizedUrl = url.replace(
            Constants.Network.PLACEHOLDER_IP,
            Constants.Network.REAL_IP
        )
        
        if (normalizedUrl.startsWith("/uploads/") || 
            normalizedUrl.startsWith("uploads/")) {
            normalizedUrl = BASE_URL + (if (normalizedUrl.startsWith("/")) "" else "/") + normalizedUrl
        }
        
        if (!normalizedUrl.startsWith("http")) {
            normalizedUrl = BASE_URL + (if (normalizedUrl.startsWith("/")) "" else "/") + normalizedUrl
        }
        
        return normalizedUrl
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
