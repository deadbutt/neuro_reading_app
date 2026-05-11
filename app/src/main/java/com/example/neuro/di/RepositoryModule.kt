package com.example.neuro.di

import com.example.neuro.api.ApiService
import com.example.neuro.repository.ArticleRepository
import com.example.neuro.repository.BookRepository
import com.example.neuro.repository.UserRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideArticleRepository(apiService: ApiService): ArticleRepository {
        return ArticleRepository(apiService)
    }

    @Provides
    @Singleton
    fun provideUserRepository(apiService: ApiService): UserRepository {
        return UserRepository(apiService)
    }

    @Provides
    @Singleton
    fun provideBookRepository(apiService: ApiService): BookRepository {
        return BookRepository(apiService)
    }
}
