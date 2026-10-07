package com.calorietrack.app.data.preferences

import android.content.Context
import android.content.SharedPreferences

interface UserPreferencesRepository {
  fun isOnboardingCompleted(): Boolean
  fun setOnboardingCompleted(completed: Boolean)
}

class SharedPreferencesUserPreferencesRepository(
  private val sharedPreferences: SharedPreferences,
) : UserPreferencesRepository {

  constructor(context: Context) : this(
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
  )

  override fun isOnboardingCompleted(): Boolean {
    return sharedPreferences.getBoolean(KEY_ONBOARDING_COMPLETED, false)
  }

  override fun setOnboardingCompleted(completed: Boolean) {
    sharedPreferences.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply()
  }

  companion object {
    const val PREFS_NAME = "calorietrack_user_prefs"
    const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
  }
}
