package com.calorietrack.app.ui.recipe

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calorietrack.app.R
import com.calorietrack.app.data.local.RecipeEntity
import com.calorietrack.app.data.local.RecipeIngredientWithFood
import com.calorietrack.app.theme.CarbsColor
import com.calorietrack.app.theme.FatColor
import com.calorietrack.app.theme.ProteinColor
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun RecipeDetailsScreen(
  onBack: () -> Unit,
  onMealLogged: () -> Unit,
  onEditRecipe: (Long) -> Unit,
  onRecipeArchived: () -> Unit,
  viewModel: RecipeDetailsViewModel,
  modifier: Modifier = Modifier,
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()

  RecipeDetailsContent(
    state = state,
    onBack = onBack,
    onQuantityChanged = viewModel::onQuantityChanged,
    onIncrement = { viewModel.onIncrementQuantity(10.0) },
    onDecrement = { viewModel.onDecrementQuantity(10.0) },
    onSelectPreset = viewModel::onSelectPresetGrams,
    onMealTypeSelected = viewModel::onMealTypeSelected,
    onConfirmAdd = { viewModel.logMeal(onSuccess = onMealLogged) },
    onEditRecipe = { state.recipe?.let { onEditRecipe(it.id) } },
    onArchiveRecipe = { viewModel.archiveRecipe(onSuccess = onRecipeArchived) },
    modifier = modifier,
  )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RecipeDetailsContent(
  state: RecipeDetailsUiState,
  onBack: () -> Unit,
  onQuantityChanged: (String) -> Unit,
  onIncrement: () -> Unit,
  onDecrement: () -> Unit,
  onSelectPreset: (Double) -> Unit,
  onMealTypeSelected: (String) -> Unit,
  onConfirmAdd: () -> Unit,
  onEditRecipe: () -> Unit,
  onArchiveRecipe: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var showDeleteDialog by remember { mutableStateOf(false) }

  Scaffold(
    modifier = modifier,
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = if (state.isEditMode) "Edit Recipe Entry" else "Recipe Details",
            fontWeight = FontWeight.Bold,
          )
        },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(
              painter = painterResource(id = R.drawable.ic_arrow_back),
              contentDescription = "Back",
            )
          }
        },
        actions = {
          if (state.recipe != null) {
            IconButton(
              onClick = onEditRecipe,
              modifier = Modifier.semantics { contentDescription = "Edit recipe" },
            ) {
              Icon(
                painter = painterResource(id = R.drawable.ic_edit),
                contentDescription = "Edit recipe",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            IconButton(
              onClick = { showDeleteDialog = true },
              modifier = Modifier.semantics { contentDescription = "Delete recipe" },
            ) {
              Icon(
                painter = painterResource(id = R.drawable.ic_delete),
                contentDescription = "Delete recipe",
                tint = MaterialTheme.colorScheme.error,
              )
            }
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.background,
        ),
      )
    },
    bottomBar = {
      if (state.recipe != null && !state.isLoading) {
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
          elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp)
          ) {
            Button(
              onClick = onConfirmAdd,
              enabled = state.isValidQuantity && !state.isSaving,
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .semantics { contentDescription = state.addButtonText },
            ) {
              if (state.isSaving) {
                CircularProgressIndicator(
                  modifier = Modifier.size(24.dp),
                  color = MaterialTheme.colorScheme.onPrimary,
                  strokeWidth = 2.5.dp,
                )
              } else {
                Text(
                  text = state.addButtonText,
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                )
              }
            }
          }
        }
      }
    }
  ) { innerPadding ->
    when {
      state.isLoading -> {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
          contentAlignment = Alignment.Center,
        ) {
          CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
      }

      state.recipe == null -> {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center,
        ) {
          Text(
            text = state.errorMessage ?: "Recipe not found.",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
          )
          Spacer(modifier = Modifier.height(16.dp))
          Button(onClick = onBack) {
            Text("Go Back")
          }
        }
      }

      else -> {
        val recipe = state.recipe
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          // Recipe Header Card
          RecipeHeaderCard(recipe = recipe)

          // Per 100g Reference Values Card
          RecipeReferenceValuesCard(recipe = recipe)

          // Ingredients Breakdown Card
          RecipeIngredientsBreakdownCard(
            ingredients = state.ingredients,
            totalCookedWeight = recipe.cookedWeightGrams,
            totalCalories = recipe.totalCalories,
            totalProtein = recipe.totalProtein,
            totalCarbs = recipe.totalCarbs,
            totalFat = recipe.totalFat,
          )

          // Destination Meal Selection
          RecipeMealSelectionCard(
            selectedMealType = state.selectedMealType,
            onMealTypeSelected = onMealTypeSelected,
          )

          // Quantity Selection Card (Grams Consumed)
          RecipeQuantitySelectionCard(
            recipe = recipe,
            quantityInput = state.quantityInput,
            isValid = state.isValidQuantity,
            errorMessage = state.quantityErrorMessage,
            onQuantityChanged = onQuantityChanged,
            onIncrement = onIncrement,
            onDecrement = onDecrement,
            onSelectPreset = onSelectPreset,
          )

          // Calculated Nutrition Preview Card
          RecipeCalculatedNutritionCard(
            quantityGrams = state.quantityGrams,
            calories = state.calculatedCalories,
            protein = state.calculatedProtein,
            carbs = state.calculatedCarbs,
            fat = state.calculatedFat,
          )

          Spacer(modifier = Modifier.height(16.dp))
        }
      }
    }
  }

  // Confirmation dialog for archiving recipe
  if (showDeleteDialog && state.recipe != null) {
    AlertDialog(
      onDismissRequest = { showDeleteDialog = false },
      title = { Text("Delete Recipe?") },
      text = {
        Text("Are you sure you want to remove \"${state.recipe.name}\" from My Recipes? Previous meal logs containing this recipe will be preserved.")
      },
      confirmButton = {
        TextButton(
          onClick = {
            showDeleteDialog = false
            onArchiveRecipe()
          },
          modifier = Modifier.semantics { contentDescription = "Confirm remove recipe" },
        ) {
          Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(
          onClick = { showDeleteDialog = false },
          modifier = Modifier.semantics { contentDescription = "Cancel remove recipe" },
        ) {
          Text("Cancel")
        }
      },
    )
  }
}

@Composable
private fun RecipeHeaderCard(
  recipe: RecipeEntity,
  modifier: Modifier = Modifier,
) {
  ElevatedCard(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(20.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = recipe.name,
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.weight(1f, fill = false),
        )

        Spacer(modifier = Modifier.width(8.dp))

        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.tertiaryContainer)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
          Text(
            text = "My Recipe",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
          )
        }
      }

      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = "Cooked weight: ${recipe.cookedWeightGrams.toInt()} g",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      if (recipe.searchKeywords.isNotBlank()) {
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = "Keywords: ${recipe.searchKeywords}",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
        )
      }
    }
  }
}

@Composable
private fun RecipeReferenceValuesCard(
  recipe: RecipeEntity,
  modifier: Modifier = Modifier,
) {
  OutlinedCard(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.outlinedCardColors(
      containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
    ),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
    ) {
      Text(
        text = "Reference Values (per 100 g cooked)",
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
      )

      Spacer(modifier = Modifier.height(10.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        RecipeNutrientStat(
          label = "Calories",
          value = "${recipe.caloriesPer100g.toInt()} kcal",
        )
        RecipeNutrientStat(
          label = "Protein",
          value = String.format(Locale.US, "%.1fg", recipe.proteinPer100g),
        )
        RecipeNutrientStat(
          label = "Carbs",
          value = String.format(Locale.US, "%.1fg", recipe.carbsPer100g),
        )
        RecipeNutrientStat(
          label = "Fat",
          value = String.format(Locale.US, "%.1fg", recipe.fatPer100g),
        )
      }
    }
  }
}

@Composable
private fun RecipeIngredientsBreakdownCard(
  ingredients: List<RecipeIngredientWithFood>,
  totalCookedWeight: Double,
  totalCalories: Double,
  totalProtein: Double,
  totalCarbs: Double,
  totalFat: Double,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "Ingredients (${ingredients.size})",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
        )

        Text(
          text = "Total ${totalCalories.toInt()} kcal",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.primary,
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ingredients.forEach { item ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = item.foodName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                text = "${item.quantityGrams.toInt()} g · P %.1fg · C %.1fg · F %.1fg".format(
                  item.protein, item.carbs, item.fat
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }

            Text(
              text = "${item.calories.toInt()} kcal",
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurface,
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
      Spacer(modifier = Modifier.height(8.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = "Cooked yield: ${totalCookedWeight.toInt()} g",
          style = MaterialTheme.typography.bodySmall,
          fontWeight = FontWeight.Medium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
          text = "P %.1fg · C %.1fg · F %.1fg".format(totalProtein, totalCarbs, totalFat),
          style = MaterialTheme.typography.bodySmall,
          fontWeight = FontWeight.Medium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
private fun RecipeMealSelectionCard(
  selectedMealType: String,
  onMealTypeSelected: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
    ) {
      Text(
        text = "Destination Meal",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
      )

      Spacer(modifier = Modifier.height(10.dp))

      val meals = listOf(
        "breakfast" to "Breakfast",
        "lunch" to "Lunch",
        "dinner" to "Dinner",
        "snack" to "Snacks",
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        meals.forEach { (key, label) ->
          val isSelected = selectedMealType.trim().lowercase(Locale.ROOT) == key
          FilterChip(
            selected = isSelected,
            onClick = { onMealTypeSelected(key) },
            label = {
              Text(
                text = label,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
              )
            },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
              selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecipeQuantitySelectionCard(
  recipe: RecipeEntity,
  quantityInput: String,
  isValid: Boolean,
  errorMessage: String?,
  onQuantityChanged: (String) -> Unit,
  onIncrement: () -> Unit,
  onDecrement: () -> Unit,
  onSelectPreset: (Double) -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
    ) {
      Text(
        text = "Amount Eaten (grams)",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = "Enter the actual cooked portion weight you are logging",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      Spacer(modifier = Modifier.height(12.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        FilledTonalIconButton(
          onClick = onDecrement,
          modifier = Modifier.size(48.dp),
        ) {
          Icon(
            painter = painterResource(id = R.drawable.ic_remove),
            contentDescription = "Decrease 10 grams",
          )
        }

        OutlinedTextField(
          value = quantityInput,
          onValueChange = onQuantityChanged,
          modifier = Modifier
            .weight(1f)
            .semantics { contentDescription = "Recipe portion grams input" },
          suffix = { Text("g", fontWeight = FontWeight.Bold) },
          singleLine = true,
          isError = !isValid,
          keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal,
            imeAction = ImeAction.Done,
          ),
          shape = RoundedCornerShape(12.dp),
        )

        FilledTonalIconButton(
          onClick = onIncrement,
          modifier = Modifier.size(48.dp),
        ) {
          Icon(
            painter = painterResource(id = R.drawable.ic_add),
            contentDescription = "Increase 10 grams",
          )
        }
      }

      if (!isValid && errorMessage != null) {
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = errorMessage,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.error,
          modifier = Modifier.padding(start = 56.dp),
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Preset quick chips
      FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        val presets = listOf(50.0, 100.0, 150.0, 200.0)
        presets.forEach { grams ->
          SuggestionChip(
            onClick = { onSelectPreset(grams) },
            label = { Text("${grams.toInt()} g") },
          )
        }

        if (recipe.cookedWeightGrams > 0.0 && recipe.cookedWeightGrams !in presets) {
          SuggestionChip(
            onClick = { onSelectPreset(recipe.cookedWeightGrams) },
            label = { Text("Entire recipe (${recipe.cookedWeightGrams.toInt()}g)") },
          )
        }
      }
    }
  }
}

@Composable
private fun RecipeCalculatedNutritionCard(
  quantityGrams: Double?,
  calories: Double,
  protein: Double,
  carbs: Double,
  fat: Double,
  modifier: Modifier = Modifier,
) {
  ElevatedCard(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(20.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "Portion Nutrition",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
        )

        val weightDisplay = if (quantityGrams != null && quantityGrams > 0.0) {
          if (quantityGrams % 1.0 == 0.0) "${quantityGrams.toInt()} g" else "%.1f g".format(Locale.US, quantityGrams)
        } else {
          "—"
        }
        Text(
          text = weightDisplay,
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.primary,
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Calories highlight
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
      ) {
        Column {
          Text(
            text = "${calories.roundToInt()}",
            fontSize = 36.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary,
            lineHeight = 40.sp,
          )
          Text(
            text = "Total Portion Calories (kcal)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
      Spacer(modifier = Modifier.height(16.dp))

      // Macronutrients row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        RecipeMacroBox(
          label = "Protein",
          value = String.format(Locale.US, "%.1fg", protein),
          accentColor = ProteinColor,
          modifier = Modifier.weight(1f),
        )
        RecipeMacroBox(
          label = "Carbs",
          value = String.format(Locale.US, "%.1fg", carbs),
          accentColor = CarbsColor,
          modifier = Modifier.weight(1f),
        )
        RecipeMacroBox(
          label = "Fat",
          value = String.format(Locale.US, "%.1fg", fat),
          accentColor = FatColor,
          modifier = Modifier.weight(1f),
        )
      }
    }
  }
}

@Composable
private fun RecipeMacroBox(
  label: String,
  value: String,
  accentColor: Color,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier,
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
    ),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(10.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Box(
          modifier = Modifier
            .size(8.dp)
            .background(color = accentColor, shape = CircleShape)
        )
        Text(
          text = label,
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = value,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
      )
    }
  }
}

@Composable
private fun RecipeNutrientStat(
  label: String,
  value: String,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
    Text(
      text = value,
      style = MaterialTheme.typography.bodyMedium,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurface,
    )
    Spacer(modifier = Modifier.height(2.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}
