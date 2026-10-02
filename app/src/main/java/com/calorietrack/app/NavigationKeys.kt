package com.calorietrack.app

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** Root Dashboard / Home destination */
@Serializable data object Main : NavKey

/** Food Search screen destination */
@Serializable data class FoodSearchNavKey(val mealType: String = "") : NavKey

/** Food Details screen destination */
@Serializable data class FoodDetailsNavKey(val foodId: Long, val mealType: String = "") : NavKey

/** Daily History screen destination */
@Serializable data object HistoryNavKey : NavKey

/** Settings screen destination */
@Serializable data object SettingsNavKey : NavKey
