package com.github.sdpsharelook

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.annotation.Nullable
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.test.espresso.IdlingResource
import androidx.test.espresso.idling.CountingIdlingResource
import com.github.sdpsharelook.language.Language
import com.github.sdpsharelook.language.LanguageSelectionDialog
import com.github.sdpsharelook.speechRecognition.RecognitionListener
import com.github.sdpsharelook.speechRecognition.SpeechRecognizer
import com.github.sdpsharelook.textToSpeech.TextToSpeech
import com.github.sdpsharelook.translate.Translator
import com.google.mlkit.nl.translate.TranslateLanguage
import kotlinx.coroutines.*


class TranslateActivity : AppCompatActivity() {
    private lateinit var targetTextView: TextView
    private lateinit var textToSpeech: TextToSpeech
    private lateinit var sourceLanguage: Language
    private lateinit var targetLanguage: Language
    private lateinit var speechRecognizer: SpeechRecognizer
    private var targetTextString: String? = null

    @Nullable
    private var mIdlingResource: CountingIdlingResource? = null
    private lateinit var sourceText: TextView
    private lateinit var ttsButton: ImageButton
    private lateinit var srButton: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_translate)
        targetTextView = findViewById(R.id.targetText)
        sourceText = findViewById(R.id.sourceText)
        ttsButton = findViewById(R.id.imageButtonTTS)
        srButton = findViewById(R.id.imageButtonSR)
        initTranslator()
        initTextToSpeech()
        initSpeechRecognizer()
        setSource(Language.auto)
        setTarget(Language("en"), true)
        findViewById<Button>(R.id.buttonSourceLang).apply {
            setOnClickListener { selectLanguage(this) }
        }
        findViewById<Button>(R.id.buttonTargetLang).apply {
            setOnClickListener { selectLanguage(this) }
        }
        val ctx = this
        findViewById<ImageButton>(R.id.imageButtonHamburger).setOnClickListener {
            val intent = Intent(ctx, NavigationMenuActivity::class.java)
            ctx.startActivity(intent)
        }
    }

    private fun setSource(language: Language) {
        sourceLanguage = language
        val buttonSource = findViewById<Button>(R.id.buttonSourceLang)
        buttonSource.text = language.displayName
        speechRecognizer.language = language
    }

    private fun setTarget(language: Language, forceEnableTTS: Boolean = false) {
        targetLanguage = language
        val buttonTarget = findViewById<Button>(R.id.buttonTargetLang)
        buttonTarget.text = language.displayName
        textToSpeech.language = language
        ttsButton.isEnabled = forceEnableTTS || textToSpeech.isLanguageAvailable(language)
    }


    private fun selectLanguage(button: Button) {
        val activity = this
        val buttonSource = findViewById<Button>(R.id.buttonSourceLang)
        val buttonTarget = findViewById<Button>(R.id.buttonTargetLang)
        CoroutineScope(Dispatchers.Main).launch {
            val translatorLanguages = when (button) {
                buttonSource -> Translator.availableLanguages.union(setOf(Language.auto))
                buttonTarget -> Translator.availableLanguages
                else -> setOf()
            }
            val ttsLanguages =
                translatorLanguages.filter { textToSpeech.isLanguageAvailable(it) }.toSet()

            LanguageSelectionDialog.selectLanguage(
                activity,
                translatorLanguages,
                translatorLanguages,
                ttsLanguages,
                translatorLanguages
            )?.let {
                when (button) {
                    buttonSource -> {
                        setSource(it)
                    }
                    buttonTarget -> {
                        setTarget(it)
                        textToSpeech.language = it
                    }
                }
            }
            if (sourceText.text.isNotEmpty())
                updateTranslation(sourceText.text.toString())
        }
    }

    private fun initTranslator() {
        val buttonSwitchLang = findViewById<ImageButton>(R.id.buttonSwitchLang)
        sourceText.addTextChangedListener { afterTextChanged ->
            updateTranslation(afterTextChanged.toString())
        }
        buttonSwitchLang.setOnClickListener {
            when (sourceLanguage) {
                Language.auto ->
                    Toast.makeText(this, "Cannot switch language in auto mode", Toast.LENGTH_SHORT)
                        .show()
                else -> {
                    val sourceEditText = findViewById<EditText>(R.id.sourceText)
                    val tempSource = sourceEditText.text.toString()
                    sourceEditText.setText(targetTextString ?: "")
                    targetTextView.text = tempSource
                    val tempLanguage = sourceLanguage
                    setSource(targetLanguage)
                    setTarget(tempLanguage)
                    if (sourceText.text.isNotEmpty())
                        updateTranslation(sourceText.text.toString())
                }
            }
        }
    }

    private fun initTextToSpeech() {
        textToSpeech = TextToSpeech(this)
        findViewById<ImageButton>(R.id.imageButtonTTS).setOnClickListener {
            targetTextString?.let { textToSpeech.speak(it) }
        }
    }

    private fun initSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer(this)
        val ctx = this
        srButton.setOnClickListener {
            speechRecognizer.cancel()
            speechRecognizer.recognizeSpeech(object : RecognitionListener {
                override fun onResults(s: String) =
                    if (s.trim().isEmpty()) sourceText.setText("...")
                    else sourceText.setText(s)

                override fun onReady() {
                    sourceText.isEnabled = false
                    srButton.isEnabled = false
                    sourceText.text = "..."
                }

                override fun onBegin() {}

                override fun onEnd() {
                    sourceText.isEnabled = true
                    srButton.isEnabled = true
                }

                override fun onError() {
                    Toast.makeText(ctx, "Error recognition", Toast.LENGTH_SHORT).show()
                    sourceText.text = ""
                    onEnd()
                }
            })

        }
    }

    private var translatorLanguagesTag = TranslateLanguage.getAllLanguages().toSet()

    /** Call to update the text to translate and translate it.
     * @param textToTranslate [String] | The text to translate.
     */
    private fun updateTranslation(textToTranslate: String) {
        mIdlingResource?.increment()
        val activity = this
        CoroutineScope(Dispatchers.IO).launch {
            var sourceLang = sourceLanguage
            val destLang = targetLanguage
            var coroutineCanceled=false
            if (sourceLang == Language.auto) {
                val sourceLangTag = Translator.detectLanguage(textToTranslate)
                if (!translatorLanguagesTag.contains(sourceLangTag)) {
                    targetTextString = null
                    targetTextView.text = getString(R.string.unrecognized_source_language)
                    mIdlingResource?.decrement()
                    // println("source language unrecognized")
                    coroutineCanceled=true
                }
                else sourceLang=Language(sourceLangTag)
            }
            if (!coroutineCanceled) {
                val t = Translator(sourceLang.tag, destLang.tag)
                // println("source language recognized ${sourceLang.tag}")
                targetTextString = null
                targetTextView.text = getString(R.string.translation_running)
                targetTextString = t.translate(textToTranslate)
                activity.targetTextView.text = targetTextString
                mIdlingResource?.decrement()
            }
        }
    }

    /**
     * Only called from test, creates and returns a new [CountingIdlingResource].
     */
    @VisibleForTesting
    fun getIdlingResource(): IdlingResource {
        if (mIdlingResource == null) {
            mIdlingResource = CountingIdlingResource("Translation")
        }
        return mIdlingResource!!
    }
}
