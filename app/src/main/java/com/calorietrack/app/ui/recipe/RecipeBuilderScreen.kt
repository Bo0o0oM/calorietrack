package com.calorietrack.app.ui.recipe

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calorietrack.app.R
import com.calorietrack.app.data.local.FoodEntity

@Composable
fun RecipeBuilderScreen(
  onBack: () -> Unit,
  onRecipeSaved: (recipeId: Long) -> Unit,
  viewModel: RecipeBuilderViewModel,
  modifier: Modifier = Modifier,
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  val pickerFoods by viewModel.pickerFoods.collectAsStateWithLifecycle()
  val pickerQuery by viewModel.pickerSearchQuery.collectAsStateWithLifecycle()

  RecipeBuilderContent(
    state = state,
    pickerFoods = pickerFoods,
    pickerQuery = pickerQuery,
    onBack = onBack,
    onNameChanged = viewModel::onNameChanged,
    onOpenFoodPicker = viewModel::onOpenFoodPicker,
    onCloseFoodPicker = viewModel::onCloseFoodPicker,
    onPickerQueryChanged = viewModel::onPickerSearchQueryChanged,
    onSelectFood = viewModel::onAddIngredientFood,
    onIngredientQuantityChanged = viewModel::onIngredientQuantityChanged,
    onIncrementIngredient = viewModel::onIncrementIngredientQuantity,
    onDecrementIngredient = viewModel::onDecrementIngredientQuantity,
    onRemoveIngredient = viewModel::onRemoveIngredient,
    onCookedWeightChanged = viewModel::onCookedWeightChanged,
    onResetCookedWeight = viewModel::onResetCookedWeightToRaw,
    onKeywordsChanged = viewModel::onSearchKeywordsChanged,
    onSave = { viewModel.saveRecipe(onRecipeSaved) },
    modifier = modifier,
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeBuilderContent(
  state: RecipeBuilderUiState,
  pickerFoods: List<FoodEntity>,
  pickerQuery: String,
  onBack: () -> Unit,
  onNameChanged: (String) -> Unit,
  onOpenFoodPicker: () -> Unit,
  onCloseFoodPicker: () -> Unit,
  onPickerQueryChanged: (String) -> Unit,
  onSelectFood: (FoodEntity) -> Unit,
  onIngredientQuantityChanged: (Long, String) -> Unit,
  onIncrementIngredient: (Long) -> Unit,
  onDecrementIngredient: (Long) -> Unit,
  onRemoveIngredient: (Long) -> Unit,
  onCookedWeightChanged: (String) -> Unit,
  onResetCookedWeight: () -> Unit,
  onKeywordsChanged: (String) -> Unit,
  onSave: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val title = if (state.isEditMode) "Edit Recipe" else "Create Recipe"

  Scaffold(
    modifier = modifier,
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
          )
        },
        navigationIcon = {
          IconButton(
            onClick = onBack,
            modifier = Modifier.semantics { contentDescription = "Back" },
          ) {
            Icon(
              painter = painterResource(id = R.drawable.ic_arrow_back),
              contentDescription = "Back",
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.background,
        ),
      )
    },
  ) { innerPadding ->
    if (state.isLoading) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding),
        contentAlignment = Alignment.Center,
      ) {
        CircularProgressIndicator(
          modifier = Modifier.size(36.dp),
          color = MaterialTheme.colorScheme.primary,
        )
      }
    } else {
      val scrollState = rememberScrollState()

      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
          .padding(horizontal = 16.dp)
          .verticalScroll(scrollState),
      ) {
        Spacer(modifier = Modifier.height(8.dp))

        if (state.generalError != null) {
          Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.errorContainer,
            ),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
          ) {
            Text(
              text = state.generalError,
              color = MaterialTheme.colorScheme.onErrorContainer,
              style = MaterialTheme.typography.bodyMedium,
              modifier = Modifier.padding(14.dp),
            )
          }
        }

        // Section: Recipe Info
        Text(
          text = "Recipe Details",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
          value = state.name,
          onValueChange = onNameChanged,
          label = { Text("Recipe Name *") },
          placeholder = { Text("e.g. My Shahi Paneer") },
          singleLine = true,
          isError = state.nameError != null,
          supportingText = {
            if (state.nameError != null) {
              Text(state.nameError, color = MaterialTheme.colorScheme.error)
            }
          },
          modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Recipe name input" },
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Section: Ingredients Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column {
            Text(
              text = "Ingredients (${state.ingredients.size})",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text = "Add raw ingredients to calculate totals",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

          FilledTonalButton(
            onClick = onOpenFoodPicker,
            modifier = Modifier.semantics { contentDescription = "Add ingredient button" },
          ) {
            Icon(
              painter = painterResource(id = R.drawable.ic_add),
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Add")
          }
        }

        if (state.ingredientsError != null) {
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = state.ingredientsError,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
          )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (state.ingredients.isEmpty()) {
          OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              Text(
                text = "No ingredients added yet",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "Tap '+ Add' to select foods from your catalogue or custom foods",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
              )
            }
          }
        } else {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            state.ingredients.forEach { draft ->
              IngredientDraftCard(
                draft = draft,
                onQuantityChanged = { q -> onIngredientQuantityChanged(draft.draftId, q) },
                onIncrement = { onIncrementIngredient(draft.draftId) },
                onDecrement = { onDecrementIngredient(draft.draftId) },
                onRemove = { onRemoveIngredient(draft.draftId) },
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Section: Final Cooked Weight
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = "Final Cooked Weight",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
              )
              if (state.isCookedWeightCustomized) {
                TextButton(onClick = onResetCookedWeight) {
                  Text("Reset to raw (${state.rawWeightGrams.toInt()}g)")
                }
              }
            }

            Text(
              text = "Cooking often reduces or increases final weight. Nutrition density per 100g is derived from this weight.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
              value = state.cookedWeightInput,
              onValueChange = onCookedWeightChanged,
              label = { Text("Cooked Weight (grams) *") },
              placeholder = { Text("e.g. 380") },
              singleLine = true,
              isError = state.cookedWeightError != null,
              supportingText = {
                if (state.cookedWeightError != null) {
                  Text(state.cookedWeightError, color = MaterialTheme.colorScheme.error)
                } else if (!state.isCookedWeightCustomized) {
                  Text("Defaults to sum of raw ingredients (${state.rawWeightGrams.toInt()} g)")
                }
              },
              keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Next,
              ),
              modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Final cooked weight input" },
            )
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Section: Live Nutrition Preview
        Text(
          text = "Nutrition Preview",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "Per 100 g",
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.primary,
                  fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = "${state.caloriesPer100g.toInt()} kcal",
                  style = MaterialTheme.typography.titleLarge,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = "P %.1fg  ·  C %.1fg  ·  F %.1fg".format(
                    state.proteinPer100g, state.carbsPer100g, state.fatPer100g
                  ),
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }

              Spacer(modifier = Modifier.width(12.dp))

              Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Text(
                  text = "Entire Recipe (${state.effectiveCookedWeight.toInt()}g)",
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = "${state.totalCalories.toInt()} kcal",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = "P %.1fg  ·  C %.1fg  ·  F %.1fg".format(
                    state.totalProtein, state.totalCarbs, state.totalFat
                  ),
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Section: Optional Keywords
        OutlinedTextField(
          value = state.searchKeywords,
          onValueChange = onKeywordsChanged,
          label = { Text("Search Keywords (Optional)") },
          placeholder = { Text("e.g. curry, paneer, dinner") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Recipe keywords input" },
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        )

        Spacer(modifier = Modifier.height(28.dp))

        val buttonText = when {
          state.isEditMode -> "Save Changes"
          state.mealType.isNotBlank() -> "Save & Continue to Log"
          else -> "Save Recipe"
        }

        Button(
          onClick = onSave,
          enabled = !state.isSaving,
          modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .semantics { contentDescription = buttonText },
          shape = RoundedCornerShape(14.dp),
        ) {
          if (state.isSaving) {
            CircularProgressIndicator(
              modifier = Modifier.size(22.dp),
              color = MaterialTheme.colorScheme.onPrimary,
              strokeWidth = 2.dp,
            )
          } else {
            Text(
              text = buttonText,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.SemiBold,
            )
          }
        }

        Spacer(modifier = Modifier.height(36.dp))
      }
    }
  }

  // Food Picker Bottom Sheet
  if (state.isFoodPickerOpen) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
      onDismissRequest = onCloseFoodPicker,
      sheetState = sheetState,
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp)
          .padding(bottom = 24.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "Select Food",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
          )
          IconButton(onClick = onCloseFoodPicker) {
            Icon(painter = painterResource(id = R.drawable.ic_close), contentDescription = "Close picker")
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
          value = pickerQuery,
          onValueChange = onPickerQueryChanged,
          placeholder = { Text("Search foods to add...") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
          leadingIcon = {
            Icon(painter = painterResource(id = R.drawable.ic_search), contentDescription = null)
          },
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (pickerFoods.isEmpty()) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(150.dp),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = if (pickerQuery.isNotBlank()) "No matching foods found" else "Type to search foods",
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        } else {
          LazyColumn(
            modifier = Modifier
              .fillMaxWidth()
              .height(350.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            items(items = pickerFoods, key = { it.id }) { food ->
              Card(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable { onSelectFood(food) },
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = food.name,
                      style = MaterialTheme.typography.titleSmall,
                      fontWeight = FontWeight.SemiBold,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                      text = "${food.caloriesPer100g.toInt()} kcal / 100g  ·  ${food.servingDescription}",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                  if (food.isCustom) {
                    Box(
                      modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.tertiaryContainer)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                      Text(
                        text = "My Food",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        fontWeight = FontWeight.Bold,
                      )
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun IngredientDraftCard(
  draft: RecipeIngredientDraft,
  onQuantityChanged: (String) -> Unit,
  onIncrement: () -> Unit,
  onDecrement: () -> Unit,
  onRemove: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = draft.food.name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "P %.1fg  ·  C %.1fg  ·  F %.1fg".format(draft.protein, draft.carbs, draft.fat),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        IconButton(
          onClick = onRemove,
          modifier = Modifier.size(32.dp).semantics { contentDescription = "Remove ${draft.food.name}" },
        ) {
          Icon(
            painter = painterResource(id = R.drawable.ic_close),
            contentDescription = "Remove",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(18.dp),
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
      Spacer(modifier = Modifier.height(8.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(
            onClick = onDecrement,
            modifier = Modifier.size(32.dp).semantics { contentDescription = "Decrement quantity for ${draft.food.name}" },
          ) {
            Icon(painter = painterResource(id = R.drawable.ic_remove), contentDescription = null, modifier = Modifier.size(16.dp))
          }

          OutlinedTextField(
            value = draft.quantityInput,
            onValueChange = onQuantityChanged,
            singleLine = true,
            isError = draft.error != null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.width(90.dp).height(48.dp),
            textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center),
          )

          IconButton(
            onClick = onIncrement,
            modifier = Modifier.size(32.dp).semantics { contentDescription = "Increment quantity for ${draft.food.name}" },
          ) {
            Icon(painter = painterResource(id = R.drawable.ic_add), contentDescription = null, modifier = Modifier.size(16.dp))
          }

          Text(
            text = "g",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp),
          )
        }

        Column(horizontalAlignment = Alignment.End) {
          Text(
            text = "${draft.calories.toInt()} kcal",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
          )
        }
      }

      if (draft.error != null) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = draft.error,
          color = MaterialTheme.colorScheme.error,
          style = MaterialTheme.typography.labelSmall,
        )
      }
    }
  }
}
