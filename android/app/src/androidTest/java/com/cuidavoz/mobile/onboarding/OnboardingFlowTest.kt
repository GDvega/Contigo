package com.cuidavoz.mobile.onboarding

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cuidavoz.mobile.MainActivity
import com.cuidavoz.mobile.data.local.ContigoDatabase
import com.cuidavoz.mobile.data.repository.OnboardingRepository
import com.cuidavoz.mobile.testing.InstrumentationTestHelpers
import com.cuidavoz.mobile.ui.screens.OnboardingTestTags
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class OnboardingFlowTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createEmptyComposeRule()

    @Inject
    lateinit var onboardingRepository: OnboardingRepository

    @Inject
    lateinit var database: ContigoDatabase

    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before
    fun setUp() {
        hiltRule.inject()
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database.clearAllTables()
        InstrumentationTestHelpers.clearLocalAppData(context)
        runBlocking {
            onboardingRepository.resetOnboardingState()
        }
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After
    fun tearDown() {
        if (::scenario.isInitialized) {
            scenario.close()
        }
    }

    @Test
    fun firstLaunch_showsRoleSelection_thenPatientForm() {
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule
                .onAllNodesWithTag(OnboardingTestTags.TITLE)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeRule.onNodeWithTag(OnboardingTestTags.ROLE_PATIENT).performClick()
        composeRule.onNodeWithTag(OnboardingTestTags.CONTINUE).performClick()

        composeRule.onNodeWithTag(OnboardingTestTags.PATIENT_DETAILS).assertIsDisplayed()
    }

    @Test
    fun firstLaunch_canSelectCaregiverProfile() {
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule
                .onAllNodesWithTag(OnboardingTestTags.TITLE)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeRule.onNodeWithTag(OnboardingTestTags.ROLE_CAREGIVER).performClick()
        composeRule.onNodeWithTag(OnboardingTestTags.CONTINUE).performClick()

        composeRule.onNodeWithTag(OnboardingTestTags.PATIENT_DETAILS).assertDoesNotExist()
    }
}
