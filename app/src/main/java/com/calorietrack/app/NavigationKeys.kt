package com.calorietrack.app

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** Root Dashboard / Home destination */
@Serializable data object Main : NavKey

/** Food Search screen destination */
@Serializable data class FoodSearchNavKey(val mealType: String = "") : NavKey

/** Food Details screen destination */
@Serializable data class FoodDetailsNavKey(val foodId: Long, val mealType: String = "", val mealEntryId: Long = 0L) : NavKey

/** Meal Details & Management screen destination */
@Serializable data class MealDetailsNavKey(val mealType: String, val date: String = "") : NavKey

/** Daily History screen destination */
@Serializable data object HistoryNavKey : NavKey

/** Historical Day Details screen destination */
@Serializable data class HistoricalDayDetailsNavKey(val date: String) : NavKey

/** Settings screen destination */
@Serializable data object SettingsNavKey : NavKey
