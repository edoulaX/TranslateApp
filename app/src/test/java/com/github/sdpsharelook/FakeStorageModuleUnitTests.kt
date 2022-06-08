package com.github.sdpsharelook

import com.github.sdpsharelook.di.StorageBindsModule
import com.github.sdpsharelook.section.Section
import com.github.sdpsharelook.storage.IRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [StorageBindsModule::class]
)
class FakeStorageModuleUnitTests {
    @Provides
    @Singleton
    fun anyRepo(): IRepository<Any> = object : IRepository<Any> {
        override fun flow(name: String): Flow<Result<Any?>> =
            flowOf(Result.success("Hello World!"))

        override suspend fun insert(name: String, entity: Any) = Unit
        override suspend fun read(name: String): Any? = null
        override suspend fun update(name: String, entity: Any) = Unit
        override suspend fun delete(name: String, entity: Any) = Unit
    }

    @Provides
    @Singleton
    fun stringListRepo(): IRepository<List<String>> = object : IRepository<List<String>> {
        override fun flow(name: String): Flow<Result<List<String>?>> =
            flowOf(Result.success(listOf("Hello World!")))

        override suspend fun insert(name: String, entity: List<String>) = Unit
        override suspend fun read(name: String): List<String>? = null
        override suspend fun update(name: String, entity: List<String>) = Unit
        override suspend fun delete(name: String, entity: List<String>) = Unit
    }

    @Provides
    @Singleton
    fun wordListRepo(): IRepository<List<Word>> = object : IRepository<List<Word>> {
        override fun flow(name: String): Flow<Result<List<Word>?>> =
            flowOf(
                Result.success(
                    listOf(
                        Word(source = "test", target = "test")
                    )
                )
            )
        override suspend fun insert(name: String, entity: List<Word>) = Unit
        override suspend fun read(name: String): List<Word>? = null
        override suspend fun update(name: String, entity: List<Word>) = Unit
        override suspend fun delete(name: String, entity: List<Word>) = Unit
    }

    @Provides
    @Singleton
    fun sectionRepo(): IRepository<List<Section>> = object : IRepository<List<Section>> {
        override fun flow(name: String): Flow<Result<List<Section>?>> =
            flowOf(Result.failure(CancellationException("test")))

        override suspend fun insert(name: String, entity: List<Section>) = Unit
        override suspend fun read(name: String): List<Section>? = null
        override suspend fun update(name: String, entity: List<Section>) = Unit
        override suspend fun delete(name: String, entity: List<Section>) = Unit
    }
}