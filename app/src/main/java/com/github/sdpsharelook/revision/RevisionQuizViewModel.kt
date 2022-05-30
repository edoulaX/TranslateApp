package com.github.sdpsharelook.revision

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.sdpsharelook.Word
import com.github.sdpsharelook.storage.IRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RevisionQuizViewModel @Inject constructor(
    private val repo: IRepository<List<Word>>,
    private val app: Application
) : AndroidViewModel(app) {
    init {
        sendUiEvent(UiEvent.UpdateBadge)
        viewModelScope.launch {
            repo.flow().collect { result ->
                result.onSuccess { list ->
                    list?.onEach {
                        wordsToQuiz += RevisionWord(it.uid, System.currentTimeMillis())
                    }
                }
                sendUiEvent(UiEvent.UpdateBadge)
            }
        }
    }

    private var wordsToQuiz: MutableList<RevisionWord> =
        SRAlgo.loadRevWordsFromLocal(app.applicationContext)
            .plus(RevisionWord("1"))
            .plus(RevisionWord("2"))
            .toMutableList()
    private var indexIntoQuiz = -1
    private var quizLength = -1
    private var _current: RevisionWord = wordsToQuiz.firstOrNull()?: RevisionWord("")
    set(value) {
        current = getWordFromRevision(value)
        field = value
    }
    lateinit var current: Word
    val size
    get() = wordsToQuiz.size

    private fun getWordFromRevision(revisionWord: RevisionWord): Word {
        TODO("Not yet implemented")
    }

    private var launched: Boolean = false
    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    fun onEvent(event: QuizEvent) {
        when (event) {
            is QuizEvent.Continue -> {
                assert(launched)
                sendUiEvent(UiEvent.ShowAnswer)
            }
            is QuizEvent.ClickEffortButton -> {
                SRAlgo.calcNextReviewTime(_current, event.quality)
                nextWord()
                sendUiEvent(UiEvent.NewWord)
            }
            is QuizEvent.StartQuiz -> startQuiz(event)
            QuizEvent.Ping -> sendUiEvent(UiEvent.UpdateBadge)
        }
    }

    private fun startQuiz(event: QuizEvent.StartQuiz) {
        if (event.length > wordsToQuiz.size) {
            sendUiEvent(
                UiEvent.ShowSnackbar(
                    "Not enough words (${wordsToQuiz.size})"
                )
            )
            return
        }
        launched = true
        quizLength = event.length
        indexIntoQuiz = 0
        _current = wordsToQuiz[indexIntoQuiz]
        sendUiEvent(UiEvent.Navigate(Routes.QUIZ))
    }

    private fun sendUiEvent(event: UiEvent) {
        viewModelScope.launch { _uiEvent.send(event) }
    }

    private fun nextWord() {
        _current.saveToStorage(app.applicationContext)
        _current = wordsToQuiz[++indexIntoQuiz]
    }
}