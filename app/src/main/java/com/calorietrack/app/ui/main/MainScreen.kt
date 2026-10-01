package com.calorietrack.app.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.calorietrack.app.R
import com.calorietrack.app.theme.CalorieTrackTheme

import com.calorietrack.app.theme.CarbsColor
import com.calorietrack.app.theme.FatColor
import com.calorietrack.app.theme.ProteinColor

@Composable
fun MainScreen(
  onNavigateToSearch: (mealType: String) -> Unit,
  onNavigateToHistory: () -> Unit,
  onNavigateToSettings: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: MainScreenViewModel = viewModel(),
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()

  DashboardContent(
    state = state,
    onNavigateToSearch = onNavigateToSearch,
    onNavigateToHistory = onNavigateToHistory,
    onNavigateToSettings = onNavigateToSettings,
    modifier = modifier,
  )
}

@Composable
fun DashboardContent(
  state: DashboardUiState,
  onNavigateToSearch: (mealType: String) -> Unit,
  onNavigateToHistory: () -> Unit,
  onNavigateToSettings: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Scaffold(
    modifier = modifier,
    floatingActionButton = {
      ExtendedFloatingActionButton(
        onClick = { onNavigateToSearch("") },
        icon = {
          Icon(
            painter = painterResource(id = R.drawable.ic_add),
            contentDescription = null,
          )
        },
        text = { Text("Add Food", fontWeight = FontWeight.SemiBold) },
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier.semantics { contentDescription = "Add Food" },
      )
    },
  ) { innerPadding ->
    Column(
      modifier =
        Modifier.fillMaxSize()
          .padding(innerPadding)
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 16.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      DashboardHeader(
        greeting = state.greeting,
        dateText = state.dateText,
        onHistoryClick = onNavigateToHistory,
        onSettingsClick = onNavigateToSettings,
      )

      CalorieSummaryCard(
        consumedCalories = state.consumedCalories,
        targetCalories = state.targetCalories,
        remainingCalories = state.remainingCalories,
        progress = state.calorieProgress,
      )

      MacroSummaryRow(
        protein = state.protein,
        carbs = state.carbs,
        fat = state.fat,
      )

      if (state.isEmptyDay) {
        EmptyDayCard(onAddFoodClick = { onNavigateToSearch("") })
      }

      Text(
        text = "Meals",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
      )

      state.meals.forEach { meal ->
        MealSectionCard(
          meal = meal,
          onAddClick = { onNavigateToSearch(meal.key) },
        )
      }

      // Bottom spacing so scrollable content is not obscured by the FAB
      Spacer(modifier = Modifier.height(88.dp))
    }
  }
}

@Composable
private fun DashboardHeader(
  greeting: String,
  dateText: String,
  onHistoryClick: () -> Unit,
  onSettingsClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier.fillMaxWidth().padding(top = 8.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column {
      Text(
        text = "CalorieTrack",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.primary,
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = "$greeting \u2022 $dateText",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }

    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
      IconButton(
        onClick = onHistoryClick,
        modifier = Modifier.size(48.dp),
      ) {
        Icon(
          painter = painterResource(id = R.drawable.ic_history),
          contentDescription = "Daily History",
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      IconButton(
        onClick = onSettingsClick,
        modifier = Modifier.size(48.dp),
      ) {
        Icon(
          painter = painterResource(id = R.drawable.ic_settings),
          contentDescription = "Settings",
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
private fun CalorieSummaryCard(
  consumedCalories: Int,
  targetCalories: Int,
  remainingCalories: Int,
  progress: Float,
  modifier: Modifier = Modifier,
) {
  ElevatedCard(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(20.dp),
    colors =
      CardDefaults.elevatedCardColors(
        containerColor = MaterialTheme.colorScheme.surface
      ),
    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(20.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "Today's Calories",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.secondaryContainer,
        ) {
          Text(
            text = "Target: $targetCalories kcal",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
      ) {
        Column {
          Text(
            text = "$remainingCalories",
            fontSize = 38.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary,
            lineHeight = 42.sp,
          )
          Text(
            text = "kcal remaining",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        Column(horizontalAlignment = Alignment.End) {
          Text(
            text = "$consumedCalories / $targetCalories",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            text = "kcal consumed",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      LinearProgressIndicator(
        progress = { progress },
        modifier =
          Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
      )
    }
  }
}

@Composable
private fun MacroSummaryRow(
  protein: MacroInfo,
  carbs: MacroInfo,
  fat: MacroInfo,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    MacroCard(
      macro = protein,
      accentColor = ProteinColor,
      modifier = Modifier.weight(1f),
    )
    MacroCard(
      macro = carbs,
      accentColor = CarbsColor,
      modifier = Modifier.weight(1f),
    )
    MacroCard(
      macro = fat,
      accentColor = FatColor,
      modifier = Modifier.weight(1f),
    )
  }
}

@Composable
private fun MacroCard(
  macro: MacroInfo,
  accentColor: Color,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier,
    shape = RoundedCornerShape(16.dp),
    colors =
      CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface
      ),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(12.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Box(
          modifier =
            Modifier.size(8.dp).background(color = accentColor, shape = CircleShape)
        )
        Text(
          text = macro.name,
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = "${macro.consumed} / ${macro.target} ${macro.unit}",
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
      )

      Spacer(modifier = Modifier.height(8.dp))

      LinearProgressIndicator(
        progress = { macro.progress },
        modifier =
          Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
        color = accentColor,
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
      )
    }
  }
}

@Composable
private fun EmptyDayCard(
  onAddFoodClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  OutlinedCard(
    onClick = onAddFoodClick,
    modifier =
      modifier
        .fillMaxWidth()
        .semantics { contentDescription = "No foods logged today. Tap to add food." },
    shape = RoundedCornerShape(16.dp),
    colors =
      CardDefaults.outlinedCardColors(
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
      ),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
        text = "No foods logged today",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = "Tap here or '+ Add Food' to record your meals and track your calories.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
    }
  }
}

@Composable
private fun MealSectionCard(
  meal: MealSection,
  onAddClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors =
      CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface
      ),
  ) {
    Row(
      modifier =
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column {
        Text(
          text = meal.displayName,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = "${meal.consumedCalories} kcal",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      FilledTonalIconButton(
        onClick = onAddClick,
        modifier = Modifier.size(48.dp),
      ) {
        Icon(
          painter = painterResource(id = R.drawable.ic_add),
          contentDescription = "Add food to ${meal.displayName}",
        )
      }
    }
  }
}

@Preview(showBackground = true)
@Composable
fun DashboardPreviewLight() {
  CalorieTrackTheme(darkTheme = false) {
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

@Preview(showBackground = true)
@Composable
fun DashboardPreviewDark() {
  CalorieTrackTheme(darkTheme = true) {
    DashboardContent(
      state =
        DashboardUiState(
          greeting = "Good evening",
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
