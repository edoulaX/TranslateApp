package com.github.sdpsharelook

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.sdpsharelook.di.TextDetectionModule
import com.github.sdpsharelook.textDetection.TextDetectionFragment
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognizer
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.hamcrest.Matchers.allOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.shadows.ShadowLooper

@ExperimentalCoroutinesApi
@UninstallModules(TextDetectionModule::class)
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class TextDetectionFragmentTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @BindValue
    val textReco: TextRecognizer = mock {
        on { process(any<InputImage>()) } doReturn Tasks.forResult(txtMock)
    }
    private val txtMock: Text = mock {
        on { text } doReturn "test"
    }

    @Before
    fun init() {
        hiltRule.inject()
        launchFragmentInHiltContainer<TextDetectionFragment>()
    }
    @Test
    fun textDetectionActivityTest() {
        val textView = onView(allOf(withId(R.id.text_data), withText("Detect the text")))
        textView.check(matches(isDisplayed()))
        val detectButton = onView(withId(R.id.detectButton))
        detectButton.perform(click())

        ShadowLooper.runUiThreadTasks()

        verify(textReco).process(any<InputImage>())
        onView(withText("test")).check(matches(isDisplayed()))
    }

}
