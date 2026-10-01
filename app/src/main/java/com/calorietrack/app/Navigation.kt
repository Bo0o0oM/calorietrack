package com.calorietrack.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.calorietrack.app.data.local.CalorieTrackDatabase
import com.calorietrack.app.ui.main.MainScreen
import com.calorietrack.app.ui.search.FoodSearchScreen
import com.calorietrack.app.ui.search.FoodSearchViewModel

@Composable
fun MainNavigation() {
  val backStack = rememberNavBackStack(Main)

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider =
      entryProvider {
        entry<Main> {
          MainScreen(
            onNavigateToSearch = { meal -> backStack.add(FoodSearchNavKey(meal)) },
            onNavigateToHistory = { backStack.add(HistoryNavKey) },
            onNavigateToSettings = { backStack.add(SettingsNavKey) },
            modifier = Modifier.safeDrawingPadding(),
          )
        }
        entry<FoodSearchNavKey> { key ->
          val context = LocalContext.current
          val foodDao = CalorieTrackDatabase.getInstance(context).foodDao()
          val searchViewModel: FoodSearchViewModel =
            viewModel(
              key = "FoodSearchViewModel_${key.mealType}",
              factory = FoodSearchViewModel.Factory(foodDao, key.mealType),
            )
          FoodSearchScreen(
            mealType = key.mealType,
            onBack = { backStack.removeLastOrNull() },
            onFoodClick = { foodId ->
              // Defined placeholder for Milestone 3 (Food Details & Serving Selection)
            },
            viewModel = searchViewModel,
            modifier = Modifier.safeDrawingPadding(),
          )
        }
        entry<HistoryNavKey> {
          PlaceholderScreen(
            title = "Daily History",
            subtitle = "Dietary Trends & Past Days",
            description =
              "Reviewing past meals, historical calorie totals, and weekly macronutrient trends will be available in upcoming milestones.",
            onBack = { backStack.removeLastOrNull() },
          )
        }
        entry<SettingsNavKey> {
          PlaceholderScreen(
            title = "Settings",
            subtitle = "Preferences & Targets",
            description =
              "Custom calorie target editing, macro goals, and offline data management will be available in upcoming milestones.",
            onBack = { backStack.removeLastOrNull() },
          )
        }
      },
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceholderScreen(
  title: String,
  subtitle: String,
  description: String,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Scaffold(
    modifier = modifier.safeDrawingPadding(),
    topBar = {
      TopAppBar(
        title = { Text(title, fontWeight = FontWeight.Bold) },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(
              painter = painterResource(id = R.drawable.ic_arrow_back),
              contentDescription = "Back to Dashboard",
            )
          }
        },
        colors =
          TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
          ),
      )
    },
  ) { innerPadding ->
    Column(
      modifier =
        Modifier.fillMaxSize()
          .padding(innerPadding)
          .padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Text(
        text = subtitle,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        textAlign = TextAlign.Center,
      )
      Spacer(modifier = Modifier.height(12.dp))
      Text(
        text = description,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
      Spacer(modifier = Modifier.height(24.dp))
      Button(onClick = onBack) {
        Text("Back to Dashboard")
      }
    }
  }
}
