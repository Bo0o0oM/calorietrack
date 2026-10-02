package com.calorietrack.app.ui.details

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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.calorietrack.app.data.local.FoodEntity
import com.calorietrack.app.theme.CarbsColor
import com.calorietrack.app.theme.FatColor
import com.calorietrack.app.theme.ProteinColor
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun FoodDetailsScreen(
  onBack: () -> Unit,
  onMealLogged: () -> Unit,
  viewModel: FoodDetailsViewModel,
  modifier: Modifier = Modifier,
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()

  FoodDetailsContent(
    state = state,
    onBack = onBack,
    onQuantityChanged = viewModel::onQuantityChanged,
    onIncrement = { viewModel.onIncrementQuantity(10.0) },
    onDecrement = { viewModel.onDecrementQuantity(10.0) },
    onSelectPreset = viewModel::onSelectPresetGrams,
    onMealTypeSelected = viewModel::onMealTypeSelected,
    onConfirmAdd = { viewModel.logMeal(onSuccess = onMealLogged) },
    modifier = modifier,
  )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FoodDetailsContent(
  state: FoodDetailsUiState,
  onBack: () -> Unit,
  onQuantityChanged: (String) -> Unit,
  onIncrement: () -> Unit,
  onDecrement: () -> Unit,
  onSelectPreset: (Double) -> Unit,
  onMealTypeSelected: (String) -> Unit,
  onConfirmAdd: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Scaffold(
    modifier = modifier,
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = "Food Details",
            fontWeight = FontWeight.Bold,
          )
        },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(
              painter = painterResource(id = R.drawable.ic_arrow_back),
              contentDescription = "Back to Search",
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.background,
        ),
      )
    },
    bottomBar = {
      if (state.food != null && !state.isLoading) {
        Card(
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
          ),
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
                .semantics {
                  contentDescription = state.addButtonText
                },
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

      state.food == null -> {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center,
        ) {
          Text(
            text = state.errorMessage ?: "Food item not found.",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
          )
          Spacer(modifier = Modifier.height(16.dp))
          Button(onClick = onBack) {
            Text("Back to Search")
          }
        }
      }

      else -> {
        val food = state.food
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          // Food Header Card
          FoodHeaderCard(food = food)

          // Per 100g Reference Values Card
          ReferenceValuesCard(food = food)

          // Destination Meal Selection
          MealSelectionCard(
            selectedMealType = state.selectedMealType,
            onMealTypeSelected = onMealTypeSelected,
          )

          // Quantity Selection Card
          QuantitySelectionCard(
            food = food,
            quantityInput = state.quantityInput,
            isValid = state.isValidQuantity,
            errorMessage = state.quantityErrorMessage,
            onQuantityChanged = onQuantityChanged,
            onIncrement = onIncrement,
            onDecrement = onDecrement,
            onSelectPreset = onSelectPreset,
          )

          // Calculated Nutrition Preview Card
          CalculatedNutritionCard(
            quantityGrams = state.quantityGrams,
            calories = state.calculatedCalories,
            protein = state.calculatedProtein,
            carbs = state.calculatedCarbs,
            fat = state.calculatedFat,
          )

          // Spacer for bottom button clearance
          Spacer(modifier = Modifier.height(16.dp))
        }
      }
    }
  }
}

@Composable
private fun FoodHeaderCard(
  food: FoodEntity,
  modifier: Modifier = Modifier,
) {
  ElevatedCard(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.elevatedCardColors(
      containerColor = MaterialTheme.colorScheme.surface,
    ),
    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(20.dp)
    ) {
      Text(
        text = food.name,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
      )

      if (food.servingDescription.isNotBlank()) {
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = food.servingDescription,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      if (food.dataSource.isNotBlank()) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = food.dataSource,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.Medium,
        )
      }
    }
  }
}

@Composable
private fun ReferenceValuesCard(
  food: FoodEntity,
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
        text = "Reference Values (per 100 g)",
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
      )

      Spacer(modifier = Modifier.height(10.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        NutrientStat(
          label = "Calories",
          value = "${food.caloriesPer100g.toInt()} kcal",
        )
        NutrientStat(
          label = "Protein",
          value = String.format(Locale.US, "%.1fg", food.proteinPer100g),
        )
        NutrientStat(
          label = "Carbs",
          value = String.format(Locale.US, "%.1fg", food.carbsPer100g),
        )
        NutrientStat(
          label = "Fat",
          value = String.format(Locale.US, "%.1fg", food.fatPer100g),
        )
      }
    }
  }
}

@Composable
private fun NutrientStat(
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

@Composable
private fun MealSelectionCard(
  selectedMealType: String,
  onMealTypeSelected: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface,
    ),
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
private fun QuantitySelectionCard(
  food: FoodEntity,
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
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface,
    ),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
    ) {
      Text(
        text = "Quantity (grams)",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
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
            .semantics {
              contentDescription = "Quantity in grams input"
            },
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

        if (food.servingGrams > 0.0 && food.servingGrams !in presets) {
          val servingLabel = if (food.servingGrams % 1.0 == 0.0) {
            "1 serving (${food.servingGrams.toInt()}g)"
          } else {
            "1 serving (%.1fg)".format(Locale.US, food.servingGrams)
          }
          SuggestionChip(
            onClick = { onSelectPreset(food.servingGrams) },
            label = { Text(servingLabel) },
          )
        }
      }
    }
  }
}

@Composable
private fun CalculatedNutritionCard(
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
    colors = CardDefaults.elevatedCardColors(
      containerColor = MaterialTheme.colorScheme.surface,
    ),
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
          text = "Calculated Nutrition",
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
            text = "Total Calories (kcal)",
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
        CalculatedMacroBox(
          label = "Protein",
          value = String.format(Locale.US, "%.1fg", protein),
          accentColor = ProteinColor,
          modifier = Modifier.weight(1f),
        )
        CalculatedMacroBox(
          label = "Carbs",
          value = String.format(Locale.US, "%.1fg", carbs),
          accentColor = CarbsColor,
          modifier = Modifier.weight(1f),
        )
        CalculatedMacroBox(
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
private fun CalculatedMacroBox(
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
