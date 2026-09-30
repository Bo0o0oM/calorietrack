package com.calorietrack.app.ui.main

import java.time.LocalDate
import java.time.LocalTime
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class MainScreenViewModelTest {

  @Test
  fun uiState_initialState_hasZeroConsumedAndTargetRemaining() = runTest {
    val fixedTime = LocalTime.of(9, 30) // 9:30 AM
    val fixedDate = LocalDate.of(2026, 9, 30)
    val viewModel = MainScreenViewModel(timeProvider = { fixedTime }, dateProvider = { fixedDate })

    val state = viewModel.uiState.first()

    assertEquals(0, state.consumedCalories)
    assertEquals(2000, state.targetCalories)
    assertEquals(2000, state.remainingCalories)
    assertEquals(0f, state.calorieProgress)
    assertTrue(state.isEmptyDay)
  }

  @Test
  fun uiState_initialState_hasExpectedMacrosAndZeroConsumed() = runTest {
    val viewModel = MainScreenViewModel()
    val state = viewModel.uiState.first()

    assertEquals("Protein", state.protein.name)
    assertEquals(0, state.protein.consumed)
    assertEquals(140, state.protein.target)

    assertEquals("Carbs", state.carbs.name)
    assertEquals(0, state.carbs.consumed)
    assertEquals(250, state.carbs.target)

    assertEquals("Fat", state.fat.name)
    assertEquals(0, state.fat.consumed)
    assertEquals(70, state.fat.target)
  }

  @Test
  fun uiState_initialState_containsAllFourMealSections() = runTest {
    val viewModel = MainScreenViewModel()
    val state = viewModel.uiState.first()

    assertEquals(4, state.meals.size)
    val mealKeys = state.meals.map { it.key }
    assertEquals(listOf("breakfast", "lunch", "dinner", "snack"), mealKeys)
    state.meals.forEach { meal ->
      assertEquals(0, meal.consumedCalories)
    }
  }

  @Test
  fun greeting_returnsAppropriateGreetingByHour() {
    assertEquals("Good morning", calculateGreeting(5))
    assertEquals("Good morning", calculateGreeting(11))
    assertEquals("Good afternoon", calculateGreeting(12))
    assertEquals("Good afternoon", calculateGreeting(16))
    assertEquals("Good evening", calculateGreeting(17))
    assertEquals("Good evening", calculateGreeting(22))
    assertEquals("Good evening", calculateGreeting(2))
  }

  @Test
  fun dashboardUiState_remainingAndProgress_computeAccurately() {
    val state = DashboardUiState(
      greeting = "Good morning",
      dateText = "Today, Sep 30",
      consumedCalories = 750,
      targetCalories = 2000,
      isEmptyDay = false,
    )

    assertEquals(1250, state.remainingCalories)
    assertEquals(0.375f, state.calorieProgress, 0.001f)
  }
}
