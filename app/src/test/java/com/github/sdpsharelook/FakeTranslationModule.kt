package com.github.sdpsharelook

import com.github.sdpsharelook.di.MLKitModule
import com.github.sdpsharelook.di.TranslationModule
import com.github.sdpsharelook.downloads.TranslatorDownloader
import com.github.sdpsharelook.language.Language
import com.github.sdpsharelook.translate.TranslationProvider
import com.google.mlkit.nl.languageid.LanguageIdentifier
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import org.mockito.kotlin.mock
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class], replaces = [TranslationModule::class, MLKitModule::class]
)
object FakeTranslationModule {
    @Provides
    @Singleton
    fun providesTranslationProvider() = object : TranslationProvider {
        override val availableLanguages: Set<Language>
            get() = listOf("en", "fr").map { Language(it) }.toSet()

        override suspend fun detectLanguage(text: String): String =
            "en"

        override suspend fun translate(text: String, src: String, dst: String): String =
            when (text + src + dst) {
                "helloenfr" -> "bonjour"
                else -> "test"
            }
    }

    @Provides
    @Singleton
    fun providesTranslationDownloader() = object : TranslatorDownloader {
        private val downloads = hashMapOf("en" to true, "fr" to true)
        override suspend fun downloadedLanguages(): List<Language> =
            downloads.filter { it.value }.keys.map { Language(it) }
        override suspend fun deleteLanguage(language: Language): Boolean {
            downloads[language.tag] = false
            return true
        }
        override suspend fun downloadLanguage(language: Language, requireWifi: Boolean): Boolean {
            downloads[language.tag] = false
            return true
        }
    }

    @Provides
    @Singleton
    fun providesLanguageIdentifier() : LanguageIdentifier = mock()
}