package com.calorietrack.app.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calorietrack.app.R
import com.calorietrack.app.theme.CalorieAccentColor
import com.calorietrack.app.theme.CarbsColor
import com.calorietrack.app.theme.FatColor
import com.calorietrack.app.theme.ProteinColor
import kotlin.math.roundToInt

@Composable
fun HistoricalDayDetailsScreen(
  onBack: () -> Unit,
  viewModel: HistoricalDayDetailsViewModel,
  modifier: Modifier = Modifier,
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()

  HistoricalDayDetailsContent(
    state = state,
    onBack = onBack,
    modifier = modifier,
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoricalDayDetailsContent(
  state: HistoricalDayDetailsUiState,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Scaffold(
    modifier = modifier,
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = state.formattedDate,
              fontWeight = FontWeight.Bold,
              style = MaterialTheme.typography.titleMedium,
            )
            Text(
              text = "Historical Summary (Read-Only)",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(
              painter = painterResource(id = R.drawable.ic_arrow_back),
              contentDescription = "Back to History",
            )
          }
        },
        colors =
          TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
          ),
      )
    },
  ) { innerPadding ->
    Column(
      modifier =
        Modifier.fillMaxSize()
          .padding(innerPadding)
          .padding(horizontal = 16.dp)
          .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Spacer(modifier = Modifier.height(4.dp))

      // Daily Nutrition Totals Card
      DailyTotalsSummaryCard(state = state)

      // 4 Meal Sections
      state.meals.forEach { meal ->
        HistoricalMealCard(meal = meal)
      }

      // Read-Only Disclaimer Note
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors =
          CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
          ),
      ) {
        Text(
          text = "Historical records are read-only to preserve your past nutrition logs accurately.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(14.dp),
        )
      }

      Spacer(modifier = Modifier.height(24.dp))
    }
  }
}

@Composable
private fun DailyTotalsSummaryCard(
  state: HistoricalDayDetailsUiState,
  modifier: Modifier = Modifier,
) {
  ElevatedCard(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors =
      CardDefaults.elevatedCardColors(
        containerColor = MaterialTheme.colorScheme.surface,
      ),
    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "Daily Total",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
        )

        Row(verticalAlignment = Alignment.Bottom) {
          Text(
            text = "${state.totalCalories} kcal",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = CalorieAccentColor,
          )
          if (state.calorieGoal != null) {
            Text(
              text = " / ${state.calorieGoal} kcal",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(bottom = 2.dp),
            )
          }
        }
      }

      if (state.calorieGoal == null) {
        Text(
          text = "No calorie goal recorded for this date",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

      // Macro Pills
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        MacroDetailBox(
          label = "Protein",
          consumed = state.totalProtein,
          goal = state.proteinGoal,
          color = ProteinColor,
          modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.padding(horizontal = 4.dp))
        MacroDetailBox(
          label = "Carbs",
          consumed = state.totalCarbs,
          goal = state.carbsGoal,
          color = CarbsColor,
          modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.padding(horizontal = 4.dp))
        MacroDetailBox(
          label = "Fat",
          consumed = state.totalFat,
          goal = state.fatGoal,
          color = FatColor,
          modifier = Modifier.weight(1f),
        )
      }
    }
  }
}

@Composable
private fun MacroDetailBox(
  label: String,
  consumed: Int,
  goal: Int?,
  color: androidx.compose.ui.graphics.Color,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier,
    shape = RoundedCornerShape(10.dp),
    colors =
      CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
      ),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(8.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = "${consumed}g",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = color,
      )
      Text(
        text = if (goal != null) "Goal: ${goal}g" else "Goal: —",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun HistoricalMealCard(
  meal: HistoricalMealSection,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors =
      CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface,
      ),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = meal.displayName,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = "${meal.consumedCalories} kcal",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = if (meal.consumedCalories > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      if (meal.items.isEmpty()) {
        Text(
          text = "No foods logged for ${meal.displayName}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      } else {
        meal.items.forEachIndexed { index, item ->
          if (index > 0) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
          }
          HistoricalFoodRow(item = item)
        }
      }
    }
  }
}

@Composable
private fun HistoricalFoodRow(
  item: HistoricalFoodItem,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = item.name,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text =
          "${formatQuantity(item.quantityGrams)} · P ${item.protein.roundToInt()}g · C ${item.carbs.roundToInt()}g · F ${item.fat.roundToInt()}g",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }

    Text(
      text = "${item.calories} kcal",
      style = MaterialTheme.typography.bodyMedium,
      fontWeight = FontWeight.SemiBold,
      color = MaterialTheme.colorScheme.onSurface,
    )
  }
}

private fun formatQuantity(grams: Double): String {
  return if (grams % 1.0 == 0.0) {
    "${grams.toLong()} g"
  } else {
    String.format(java.util.Locale.US, "%.1f g", grams)
  }
}
