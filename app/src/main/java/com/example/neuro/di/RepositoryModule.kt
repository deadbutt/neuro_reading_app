package com.example.neuro.di

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
    fun provideArticleRepository(): ArticleRepository {
        return ArticleRepository()
    }
    
    @Provides
    @Singleton
    fun provideUserRepository(): UserRepository {
        return UserRepository()
    }
    
    @Provides
    @Singleton
    fun provideBookRepository(): BookRepository {
        return BookRepository()
    }
}
