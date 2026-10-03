package com.calorietrack.app.ui.history

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object HistoryDateFormatter {
  private val fullDateFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.US)
  private val monthDayFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.US)

  fun formatFriendlyDate(dateIso: String, today: LocalDate = LocalDate.now()): String {
    val date =
      try {
        LocalDate.parse(dateIso)
      } catch (_: Exception) {
        return dateIso
      }
    return when (date) {
      today -> "Today"
      today.minusDays(1) -> "Yesterday"
      else -> date.format(fullDateFormatter)
    }
  }

  fun formatDayDetailHeader(dateIso: String, today: LocalDate = LocalDate.now()): String {
    val date =
      try {
        LocalDate.parse(dateIso)
      } catch (_: Exception) {
        return dateIso
      }
    return when (date) {
      today -> "Today, ${date.format(monthDayFormatter)}"
      today.minusDays(1) -> "Yesterday, ${date.format(monthDayFormatter)}"
      else -> date.format(fullDateFormatter)
    }
  }
}
