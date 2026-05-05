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
}
