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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.calorietrack.app.data.local.CalorieTrackDatabase
import com.calorietrack.app.ui.custom.CustomFoodScreen
import com.calorietrack.app.ui.custom.CustomFoodViewModel
import com.calorietrack.app.ui.details.FoodDetailsScreen
import com.calorietrack.app.ui.details.FoodDetailsViewModel
import com.calorietrack.app.ui.history.HistoricalDayDetailsScreen
import com.calorietrack.app.ui.history.HistoricalDayDetailsViewModel
import com.calorietrack.app.ui.history.HistoryScreen
import com.calorietrack.app.ui.history.HistoryViewModel
import com.calorietrack.app.ui.main.MainScreen
import com.calorietrack.app.ui.main.MainScreenViewModel
import com.calorietrack.app.ui.meal.MealDetailsScreen
import com.calorietrack.app.ui.meal.MealDetailsViewModel
import com.calorietrack.app.ui.recipe.RecipeBuilderScreen
import com.calorietrack.app.ui.recipe.RecipeBuilderViewModel
import com.calorietrack.app.ui.recipe.RecipeDetailsScreen
import com.calorietrack.app.ui.recipe.RecipeDetailsViewModel
import com.calorietrack.app.ui.search.FoodSearchScreen
import com.calorietrack.app.ui.search.FoodSearchViewModel
import com.calorietrack.app.ui.settings.SettingsScreen
import com.calorietrack.app.ui.settings.SettingsViewModel

@Composable
fun MainNavigation() {
  val backStack = rememberNavBackStack(Main)

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider =
      entryProvider {
        entry<Main> {
          val context = LocalContext.current
          val db = CalorieTrackDatabase.getInstance(context)
          val mainViewModel: MainScreenViewModel =
            viewModel(
              factory =
                MainScreenViewModel.Factory(
                  mealEntryDao = db.mealEntryDao(),
                  dailyGoalDao = db.dailyGoalDao(),
                ),
            )
          MainScreen(
            onNavigateToSearch = { meal -> backStack.add(FoodSearchNavKey(meal)) },
            onNavigateToMealDetails = { meal -> backStack.add(MealDetailsNavKey(mealType = meal)) },
            onNavigateToHistory = { backStack.add(HistoryNavKey) },
            onNavigateToSettings = { backStack.add(SettingsNavKey) },
            viewModel = mainViewModel,
            modifier = Modifier.safeDrawingPadding(),
          )
        }
        entry<FoodSearchNavKey> { key ->
          val context = LocalContext.current
          val db = CalorieTrackDatabase.getInstance(context)
          val searchViewModel: FoodSearchViewModel =
            viewModel(
              key = "FoodSearchViewModel_${key.mealType}",
              factory = FoodSearchViewModel.Factory(
                foodDao = db.foodDao(),
                recipeDao = db.recipeDao(),
                mealType = key.mealType,
              ),
            )
          FoodSearchScreen(
            mealType = key.mealType,
            onBack = { backStack.removeLastOrNull() },
            onFoodClick = { foodId ->
              backStack.add(FoodDetailsNavKey(foodId = foodId, mealType = key.mealType))
            },
            onRecipeClick = { recipeId ->
              backStack.add(RecipeDetailsNavKey(recipeId = recipeId, mealType = key.mealType))
            },
            onNavigateToCreateCustomFood = { initialName ->
              backStack.add(CustomFoodNavKey(foodId = 0L, mealType = key.mealType, initialName = initialName))
            },
            onNavigateToEditCustomFood = { foodId ->
              backStack.add(CustomFoodNavKey(foodId = foodId, mealType = key.mealType))
            },
            onNavigateToCreateRecipe = {
              backStack.add(RecipeBuilderNavKey(recipeId = 0L, mealType = key.mealType))
            },
            onNavigateToEditRecipe = { recipeId ->
              backStack.add(RecipeBuilderNavKey(recipeId = recipeId, mealType = key.mealType))
            },
            viewModel = searchViewModel,
            modifier = Modifier.safeDrawingPadding(),
          )
        }
        entry<CustomFoodNavKey> { key ->
          val context = LocalContext.current
          val foodDao = CalorieTrackDatabase.getInstance(context).foodDao()
          val customFoodViewModel: CustomFoodViewModel =
            viewModel(
              key = "CustomFoodViewModel_${key.foodId}_${key.mealType}_${key.initialName}",
              factory = CustomFoodViewModel.Factory(
                foodId = key.foodId,
                mealType = key.mealType,
                initialName = key.initialName,
                foodDao = foodDao,
              ),
            )
          CustomFoodScreen(
            onBack = { backStack.removeLastOrNull() },
            onFoodSaved = { savedFoodId ->
              if (key.mealType.isNotBlank()) {
                backStack.removeLastOrNull()
                backStack.add(FoodDetailsNavKey(foodId = savedFoodId, mealType = key.mealType))
              } else {
                backStack.removeLastOrNull()
              }
            },
            viewModel = customFoodViewModel,
            modifier = Modifier.safeDrawingPadding(),
          )
        }
        entry<RecipeBuilderNavKey> { key ->
          val context = LocalContext.current
          val db = CalorieTrackDatabase.getInstance(context)
          val builderViewModel: RecipeBuilderViewModel =
            viewModel(
              key = "RecipeBuilderViewModel_${key.recipeId}_${key.mealType}",
              factory =
                RecipeBuilderViewModel.Factory(
                  recipeId = key.recipeId,
                  mealType = key.mealType,
                  recipeDao = db.recipeDao(),
                  foodDao = db.foodDao(),
                ),
            )
          RecipeBuilderScreen(
            onBack = { backStack.removeLastOrNull() },
            onRecipeSaved = { savedRecipeId ->
              if (key.mealType.isNotBlank()) {
                backStack.removeLastOrNull()
                backStack.add(RecipeDetailsNavKey(recipeId = savedRecipeId, mealType = key.mealType))
              } else {
                backStack.removeLastOrNull()
              }
            },
            viewModel = builderViewModel,
            modifier = Modifier.safeDrawingPadding(),
          )
        }
        entry<RecipeDetailsNavKey> { key ->
          val context = LocalContext.current
          val db = CalorieTrackDatabase.getInstance(context)
          val detailsViewModel: RecipeDetailsViewModel =
            viewModel(
              key = "RecipeDetailsViewModel_${key.recipeId}_${key.mealType}_${key.mealEntryId}",
              factory =
                RecipeDetailsViewModel.Factory(
                  recipeId = key.recipeId,
                  initialMealType = key.mealType,
                  mealEntryId = key.mealEntryId,
                  recipeDao = db.recipeDao(),
                  mealEntryDao = db.mealEntryDao(),
                ),
            )
          RecipeDetailsScreen(
            onBack = { backStack.removeLastOrNull() },
            onMealLogged = {
              if (key.mealEntryId > 0L) {
                backStack.removeLastOrNull()
              } else {
                val hasMealDetails = backStack.any { it is MealDetailsNavKey }
                if (hasMealDetails) {
                  while (backStack.size > 1 && backStack.last() !is MealDetailsNavKey) {
                    backStack.removeLastOrNull()
                  }
                } else {
                  while (backStack.size > 1) {
                    backStack.removeLastOrNull()
                  }
                }
              }
            },
            onEditRecipe = { recipeId ->
              backStack.add(RecipeBuilderNavKey(recipeId = recipeId, mealType = key.mealType))
            },
            onRecipeArchived = {
              backStack.removeLastOrNull()
            },
            viewModel = detailsViewModel,
            modifier = Modifier.safeDrawingPadding(),
          )
        }
        entry<MealDetailsNavKey> { key ->
          val context = LocalContext.current
          val db = CalorieTrackDatabase.getInstance(context)
          val mealViewModel: MealDetailsViewModel =
            viewModel(
              key = "MealDetailsViewModel_${key.mealType}_${key.date}",
              factory =
                MealDetailsViewModel.Factory(
                  mealType = key.mealType,
                  date = key.date,
                  mealEntryDao = db.mealEntryDao(),
                ),
            )
          MealDetailsScreen(
            onBack = { backStack.removeLastOrNull() },
            onAddFood = { meal -> backStack.add(FoodSearchNavKey(mealType = meal)) },
            onEditEntry = { entry ->
              if (entry.recipeId != null && entry.recipeId > 0L) {
                backStack.add(
                  RecipeDetailsNavKey(
                    recipeId = entry.recipeId,
                    mealType = entry.mealType,
                    mealEntryId = entry.id,
                  )
                )
              } else {
                backStack.add(
                  FoodDetailsNavKey(
                    foodId = entry.foodId,
                    mealType = entry.mealType,
                    mealEntryId = entry.id,
                  )
                )
              }
            },
            viewModel = mealViewModel,
            modifier = Modifier.safeDrawingPadding(),
          )
        }
        entry<FoodDetailsNavKey> { key ->
          val context = LocalContext.current
          val db = CalorieTrackDatabase.getInstance(context)
          val detailsViewModel: FoodDetailsViewModel =
            viewModel(
              key = "FoodDetailsViewModel_${key.foodId}_${key.mealType}_${key.mealEntryId}",
              factory =
                FoodDetailsViewModel.Factory(
                  foodId = key.foodId,
                  initialMealType = key.mealType,
                  mealEntryId = key.mealEntryId,
                  foodDao = db.foodDao(),
                  mealEntryDao = db.mealEntryDao(),
                ),
            )
          FoodDetailsScreen(
            onBack = { backStack.removeLastOrNull() },
            onMealLogged = {
              if (key.mealEntryId > 0L) {
                backStack.removeLastOrNull()
              } else {
                val hasMealDetails = backStack.any { it is MealDetailsNavKey }
                if (hasMealDetails) {
                  while (backStack.size > 1 && backStack.last() !is MealDetailsNavKey) {
                    backStack.removeLastOrNull()
                  }
                } else {
                  while (backStack.size > 1) {
                    backStack.removeLastOrNull()
                  }
                }
              }
            },
            viewModel = detailsViewModel,
            modifier = Modifier.safeDrawingPadding(),
          )
        }
        entry<HistoryNavKey> {
          val context = LocalContext.current
          val db = CalorieTrackDatabase.getInstance(context)
          val historyViewModel: HistoryViewModel =
            viewModel(
              factory =
                HistoryViewModel.Factory(
                  mealEntryDao = db.mealEntryDao(),
                  dailyGoalDao = db.dailyGoalDao(),
                ),
            )
          HistoryScreen(
            onBack = { backStack.removeLastOrNull() },
            onSelectDate = { dateIso -> backStack.add(HistoricalDayDetailsNavKey(date = dateIso)) },
            viewModel = historyViewModel,
            modifier = Modifier.safeDrawingPadding(),
          )
        }
        entry<HistoricalDayDetailsNavKey> { key ->
          val context = LocalContext.current
          val db = CalorieTrackDatabase.getInstance(context)
          val detailsViewModel: HistoricalDayDetailsViewModel =
            viewModel(
              key = "HistoricalDayDetailsViewModel_${key.date}",
              factory =
                HistoricalDayDetailsViewModel.Factory(
                  dateIso = key.date,
                  mealEntryDao = db.mealEntryDao(),
                  dailyGoalDao = db.dailyGoalDao(),
                ),
            )
          HistoricalDayDetailsScreen(
            onBack = { backStack.removeLastOrNull() },
            viewModel = detailsViewModel,
            modifier = Modifier.safeDrawingPadding(),
          )
        }
        entry<SettingsNavKey> {
          val context = LocalContext.current
          val db = CalorieTrackDatabase.getInstance(context)
          val backupRepository = com.calorietrack.app.data.backup.BackupRepository(
            database = db,
            foodDao = db.foodDao(),
            recipeDao = db.recipeDao(),
            mealEntryDao = db.mealEntryDao(),
            dailyGoalDao = db.dailyGoalDao(),
          )
          val settingsViewModel: SettingsViewModel =
            viewModel(
              factory =
                SettingsViewModel.Factory(
                  dailyGoalDao = db.dailyGoalDao(),
                  backupRepository = backupRepository,
                ),
            )
          SettingsScreen(
            onBack = { backStack.removeLastOrNull() },
            viewModel = settingsViewModel,
            modifier = Modifier.safeDrawingPadding(),
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
