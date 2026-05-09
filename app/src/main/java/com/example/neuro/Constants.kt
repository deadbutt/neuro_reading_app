package com.example.neuro

object Constants {

    object Network {
        const val BASE_URL = "http://47.118.22.220:9091/"
        const val PLACEHOLDER_IP = "0.0.0.0"
        const val REAL_IP = "47.118.22.220"
        const val CONNECT_TIMEOUT_SECONDS = 15L
        const val READ_TIMEOUT_SECONDS = 15L
        const val WRITE_TIMEOUT_SECONDS = 15L
        const val DEFAULT_PAGE_SIZE = 10
        const val COMMENT_PAGE_SIZE = 20
    }

    object CommentSort {
        const val HOT = "hot"
        const val NEW = "new"
        const val AUTHOR = "author"
    }

    object ApiCode {
        const val SUCCESS = 0
    }

    object CountFormat {
        const val TEN_THOUSAND = 10000
        const val THOUSAND = 1000
    }
    
    object Validation {
        const val MIN_PASSWORD_LENGTH = 6
        const val MAX_PASSWORD_LENGTH = 20
        const val VERIFICATION_CODE_LENGTH = 6
        const val MIN_NICKNAME_LENGTH = 2
        const val MAX_NICKNAME_LENGTH = 20
    }
    
    object Time {
        const val SESSION_DURATION_DAYS = 7
        const val COUNTDOWN_SECONDS = 60
    }
    
    object Message {
        const val NETWORK_ERROR = "网络错误，请检查网络连接"
        const val LOGIN_SUCCESS = "登录成功"
        const val REGISTER_SUCCESS = "注册成功"
        const val OPERATION_SUCCESS = "操作成功"
        const val OPERATION_FAILED = "操作失败"
    }
}
