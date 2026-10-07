package com.calorietrack.app.ui.onboarding

import com.calorietrack.app.data.preferences.UserPreferencesRepository
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OnboardingTest {

  private class FakeUserPreferencesRepository : UserPreferencesRepository {
    private var completed: Boolean = false

    override fun isOnboardingCompleted(): Boolean = completed

    override fun setOnboardingCompleted(completed: Boolean) {
      this.completed = completed
    }
  }

  private lateinit var fakePrefsRepo: UserPreferencesRepository

  @Before
  fun setUp() {
    fakePrefsRepo = FakeUserPreferencesRepository()
  }

  @Test
  fun firstRun_whenPreferencesNotSet_isOnboardingCompletedIsFalse() {
    assertFalse(fakePrefsRepo.isOnboardingCompleted())
  }

  @Test
  fun getStarted_completesOnboarding_andPersistsTrue() {
    assertFalse(fakePrefsRepo.isOnboardingCompleted())

    fakePrefsRepo.setOnboardingCompleted(true)

    assertTrue(fakePrefsRepo.isOnboardingCompleted())
  }

  @Test
  fun returningUser_whenOnboardingCompleted_isOnboardingCompletedIsTrue() {
    fakePrefsRepo.setOnboardingCompleted(true)

    val isCompleted = fakePrefsRepo.isOnboardingCompleted()

    assertTrue(isCompleted)
  }

  @Test
  fun onboardingState_canBeResetIfExplicitlyRequested() {
    fakePrefsRepo.setOnboardingCompleted(true)
    assertTrue(fakePrefsRepo.isOnboardingCompleted())

    fakePrefsRepo.setOnboardingCompleted(false)
    assertFalse(fakePrefsRepo.isOnboardingCompleted())
  }
}
