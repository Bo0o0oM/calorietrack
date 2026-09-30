package com.calorietrack.app.ui.main

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.calorietrack.app.theme.CalorieTrackTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** UI tests for CalorieTrack Dashboard. */
class MainScreenTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Before
  fun setup() {
    composeTestRule.setContent {
      CalorieTrackTheme {
        DashboardContent(
          state =
            DashboardUiState(
              greeting = "Good morning",
              dateText = "Today, Wed, Sep 30",
              consumedCalories = 0,
              targetCalories = 2000,
              isEmptyDay = true,
            ),
          onNavigateToSearch = {},
          onNavigateToHistory = {},
          onNavigateToSettings = {},
        )
      }
    }
  }

  @Test
  fun dashboard_brandingAndHeaders_exist() {
    composeTestRule.onNodeWithText("CalorieTrack").assertIsDisplayed()
    composeTestRule.onNodeWithText("Today's Calories").assertIsDisplayed()
    composeTestRule.onNodeWithText("2000").assertIsDisplayed()
    composeTestRule.onNodeWithText("kcal remaining").assertIsDisplayed()
  }

  @Test
  fun dashboard_emptyState_isDisplayed() {
    composeTestRule.onNodeWithText("No foods logged today").assertIsDisplayed()
  }

  @Test
  fun dashboard_mealSections_areDisplayed() {
    composeTestRule.onNodeWithText("Breakfast").assertIsDisplayed()
    composeTestRule.onNodeWithText("Lunch").assertIsDisplayed()
    composeTestRule.onNodeWithText("Dinner").assertIsDisplayed()
    composeTestRule.onNodeWithText("Snacks").assertIsDisplayed()
  }

  @Test
  fun dashboard_primaryAction_exists() {
    composeTestRule.onNodeWithContentDescription("Add Food").assertIsDisplayed()
  }
}
