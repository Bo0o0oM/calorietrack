package com.calorietrack.app.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calorietrack.app.R
import com.calorietrack.app.data.local.FoodEntity
import com.calorietrack.app.data.local.RecipeEntity

@Composable
fun FoodSearchScreen(
  mealType: String,
  onBack: () -> Unit,
  onFoodClick: (foodId: Long) -> Unit = {},
  onRecipeClick: (recipeId: Long) -> Unit = {},
  onNavigateToCreateCustomFood: (initialName: String) -> Unit = {},
  onNavigateToEditCustomFood: (foodId: Long) -> Unit = {},
  onNavigateToCreateRecipe: () -> Unit = {},
  onNavigateToEditRecipe: (recipeId: Long) -> Unit = {},
  viewModel: FoodSearchViewModel,
  modifier: Modifier = Modifier,
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()

  FoodSearchContent(
    state = state,
    mealType = mealType,
    onQueryChanged = viewModel::onQueryChanged,
    onClearQuery = viewModel::onClearQuery,
    onTabSelected = viewModel::onTabSelected,
    onArchiveFood = viewModel::archiveFood,
    onArchiveRecipe = viewModel::archiveRecipe,
    onBack = onBack,
    onFoodClick = onFoodClick,
    onRecipeClick = onRecipeClick,
    onNavigateToCreateCustomFood = onNavigateToCreateCustomFood,
    onNavigateToEditCustomFood = onNavigateToEditCustomFood,
    onNavigateToCreateRecipe = onNavigateToCreateRecipe,
    onNavigateToEditRecipe = onNavigateToEditRecipe,
    modifier = modifier,
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodSearchContent(
  state: FoodSearchUiState,
  mealType: String,
  onQueryChanged: (String) -> Unit,
  onClearQuery: () -> Unit,
  onTabSelected: (FoodSearchTab) -> Unit,
  onArchiveFood: (Long) -> Unit,
  onArchiveRecipe: (Long) -> Unit,
  onBack: () -> Unit,
  onFoodClick: (foodId: Long) -> Unit,
  onRecipeClick: (recipeId: Long) -> Unit,
  onNavigateToCreateCustomFood: (String) -> Unit,
  onNavigateToEditCustomFood: (Long) -> Unit,
  onNavigateToCreateRecipe: () -> Unit,
  onNavigateToEditRecipe: (Long) -> Unit,
  modifier: Modifier = Modifier,
) {
  val focusManager = LocalFocusManager.current
  val keyboardController = LocalSoftwareKeyboardController.current
  val subtitle = formatMealContextSubtitle(mealType)
  var foodToArchive by remember { mutableStateOf<FoodEntity?>(null) }
  var recipeToArchive by remember { mutableStateOf<RecipeEntity?>(null) }

  Scaffold(
    modifier = modifier,
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = "Add Food",
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = subtitle,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        },
        navigationIcon = {
          IconButton(
            onClick = onBack,
            modifier = Modifier.semantics { contentDescription = "Back to Dashboard" },
          ) {
            Icon(
              painter = painterResource(id = R.drawable.ic_arrow_back),
              contentDescription = "Back",
            )
          }
        },
        actions = {
          IconButton(
            onClick = {
              if (state.selectedTab == FoodSearchTab.MY_RECIPES) {
                onNavigateToCreateRecipe()
              } else {
                onNavigateToCreateCustomFood(state.query.trim())
              }
            },
            modifier = Modifier.semantics {
              contentDescription = if (state.selectedTab == FoodSearchTab.MY_RECIPES) "Create recipe" else "Create custom food"
            },
          ) {
            Icon(
              painter = painterResource(id = R.drawable.ic_add),
              contentDescription = if (state.selectedTab == FoodSearchTab.MY_RECIPES) "Create recipe" else "Create custom food",
              tint = MaterialTheme.colorScheme.primary,
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.background,
        ),
      )
    },
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(horizontal = 16.dp),
    ) {
      Spacer(modifier = Modifier.height(8.dp))

      // Search TextField
      val searchTextFieldState = rememberTextFieldState(initialText = state.query)

      LaunchedEffect(searchTextFieldState) {
        snapshotFlow { searchTextFieldState.text.toString() }
          .collect { newQuery ->
            onQueryChanged(newQuery)
          }
      }

      val interactionSource = remember { MutableInteractionSource() }

      BasicTextField(
        state = searchTextFieldState,
        modifier = Modifier
          .fillMaxWidth()
          .semantics { contentDescription = "Search foods input field" },
        textStyle = MaterialTheme.typography.bodyLarge.copy(
          color = MaterialTheme.colorScheme.onSurface,
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        lineLimits = TextFieldLineLimits.SingleLine,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        onKeyboardAction = {
          keyboardController?.hide()
          focusManager.clearFocus()
        },
        decorator = { innerTextField ->
          OutlinedTextFieldDefaults.DecorationBox(
            value = searchTextFieldState.text.toString(),
            innerTextField = innerTextField,
            enabled = true,
            singleLine = true,
            visualTransformation = VisualTransformation.None,
            interactionSource = interactionSource,
            placeholder = { Text(if (state.selectedTab == FoodSearchTab.MY_RECIPES) "Search recipes" else "Search foods") },
            leadingIcon = {
              Icon(
                painter = painterResource(id = R.drawable.ic_search),
                contentDescription = "Search",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            },
            trailingIcon = {
              if (searchTextFieldState.text.isNotEmpty()) {
                IconButton(
                  onClick = {
                    searchTextFieldState.clearText()
                    onClearQuery()
                  },
                  modifier = Modifier.semantics { contentDescription = "Clear search query" },
                ) {
                  Icon(
                    painter = painterResource(id = R.drawable.ic_close),
                    contentDescription = "Clear",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            },
            container = {
              OutlinedTextFieldDefaults.Container(
                enabled = true,
                isError = false,
                interactionSource = interactionSource,
                colors = OutlinedTextFieldDefaults.colors(
                  focusedContainerColor = MaterialTheme.colorScheme.surface,
                  unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                ),
                shape = RoundedCornerShape(16.dp),
              )
            },
            contentPadding = OutlinedTextFieldDefaults.contentPadding(),
          )
        },
      )

      Spacer(modifier = Modifier.height(12.dp))

      // Filter Chips: All Foods | My Foods | My Recipes
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        FilterChip(
          selected = state.selectedTab == FoodSearchTab.ALL,
          onClick = { onTabSelected(FoodSearchTab.ALL) },
          label = { Text("All Foods") },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
          ),
          modifier = Modifier.semantics { contentDescription = "All foods filter chip" },
        )

        FilterChip(
          selected = state.selectedTab == FoodSearchTab.MY_FOODS,
          onClick = { onTabSelected(FoodSearchTab.MY_FOODS) },
          label = { Text("My Foods") },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
          ),
          modifier = Modifier.semantics { contentDescription = "My foods filter chip" },
        )

        FilterChip(
          selected = state.selectedTab == FoodSearchTab.MY_RECIPES,
          onClick = { onTabSelected(FoodSearchTab.MY_RECIPES) },
          label = { Text("My Recipes") },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
          ),
          modifier = Modifier.semantics { contentDescription = "My recipes filter chip" },
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Results / Loading / Empty state
      when {
        state.isLoading -> {
          Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
          ) {
            CircularProgressIndicator(
              modifier = Modifier.size(36.dp),
              color = MaterialTheme.colorScheme.primary,
            )
          }
        }
        state.selectedTab == FoodSearchTab.MY_RECIPES -> {
          if (state.isEmptyResult) {
            EmptySearchState(
              query = state.query,
              selectedTab = state.selectedTab,
              onCreateCustomFood = { onNavigateToCreateCustomFood(state.query.trim()) },
              onCreateRecipe = onNavigateToCreateRecipe,
              modifier = Modifier.fillMaxSize(),
            )
          } else {
            LazyColumn(
              modifier = Modifier.fillMaxSize(),
              verticalArrangement = Arrangement.spacedBy(10.dp),
              contentPadding = PaddingValues(bottom = 24.dp),
            ) {
              items(items = state.recipeResults, key = { it.id }) { recipe ->
                RecipeResultCard(
                  recipe = recipe,
                  onClick = { onRecipeClick(recipe.id) },
                  onEdit = { onNavigateToEditRecipe(recipe.id) },
                  onDelete = { recipeToArchive = recipe },
                )
              }
            }
          }
        }
        state.isEmptyResult -> {
          EmptySearchState(
            query = state.query,
            selectedTab = state.selectedTab,
            onCreateCustomFood = { onNavigateToCreateCustomFood(state.query.trim()) },
            onCreateRecipe = onNavigateToCreateRecipe,
            modifier = Modifier.fillMaxSize(),
          )
        }
        else -> {
          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
          ) {
            items(items = state.results, key = { it.id }) { food ->
              FoodResultCard(
                food = food,
                onClick = { onFoodClick(food.id) },
                onEdit = { onNavigateToEditCustomFood(food.id) },
                onDelete = { foodToArchive = food },
              )
            }
          }
        }
      }
    }
  }

  // Confirmation dialog for archiving custom food
  foodToArchive?.let { food ->
    AlertDialog(
      onDismissRequest = { foodToArchive = null },
      title = { Text("Remove Custom Food?") },
      text = {
        Text("Are you sure you want to remove \"${food.name}\" from My Foods? Previous meal logs containing this food will be preserved.")
      },
      confirmButton = {
        TextButton(
          onClick = {
            val id = food.id
            foodToArchive = null
            onArchiveFood(id)
          },
          modifier = Modifier.semantics { contentDescription = "Confirm remove custom food" },
        ) {
          Text("Remove", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(
          onClick = { foodToArchive = null },
          modifier = Modifier.semantics { contentDescription = "Cancel remove custom food" },
        ) {
          Text("Cancel")
        }
      },
    )
  }

  // Confirmation dialog for archiving recipe
  recipeToArchive?.let { recipe ->
    AlertDialog(
      onDismissRequest = { recipeToArchive = null },
      title = { Text("Delete Recipe?") },
      text = {
        Text("Are you sure you want to remove \"${recipe.name}\" from My Recipes? Previous meal logs containing this recipe will be preserved.")
      },
      confirmButton = {
        TextButton(
          onClick = {
            val id = recipe.id
            recipeToArchive = null
            onArchiveRecipe(id)
          },
          modifier = Modifier.semantics { contentDescription = "Confirm remove recipe" },
        ) {
          Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(
          onClick = { recipeToArchive = null },
          modifier = Modifier.semantics { contentDescription = "Cancel remove recipe" },
        ) {
          Text("Cancel")
        }
      },
    )
  }
}

@Composable
fun FoodResultCard(
  food: FoodEntity,
  onClick: () -> Unit,
  onEdit: () -> Unit = {},
  onDelete: () -> Unit = {},
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .semantics { contentDescription = "${food.name}, ${food.caloriesPer100g.toInt()} kcal per 100 grams" },
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(
            text = food.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
          )
          if (food.isCustom) {
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.tertiaryContainer)
                .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
              Text(
                text = "My Food",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
              )
            }
          }
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
          text = food.servingDescription,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = "P %.1fg  ·  C %.1fg  ·  F %.1fg".format(food.proteinPer100g, food.carbsPer100g, food.fatPer100g),
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Medium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      Spacer(modifier = Modifier.width(8.dp))

      Row(verticalAlignment = Alignment.CenterVertically) {
        Column(horizontalAlignment = Alignment.End) {
          Text(
            text = "${food.caloriesPer100g.toInt()}",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
          )
          Text(
            text = "kcal / 100g",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        if (food.isCustom) {
          Spacer(modifier = Modifier.width(4.dp))
          IconButton(
            onClick = onEdit,
            modifier = Modifier
              .size(36.dp)
              .semantics { contentDescription = "Edit ${food.name}" },
          ) {
            Icon(
              painter = painterResource(id = R.drawable.ic_edit),
              contentDescription = "Edit",
              modifier = Modifier.size(18.dp),
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          IconButton(
            onClick = onDelete,
            modifier = Modifier
              .size(36.dp)
              .semantics { contentDescription = "Remove ${food.name}" },
          ) {
            Icon(
              painter = painterResource(id = R.drawable.ic_delete),
              contentDescription = "Delete",
              modifier = Modifier.size(18.dp),
              tint = MaterialTheme.colorScheme.error,
            )
          }
        }
      }
    }
  }
}

@Composable
fun RecipeResultCard(
  recipe: RecipeEntity,
  onClick: () -> Unit,
  onEdit: () -> Unit = {},
  onDelete: () -> Unit = {},
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .semantics { contentDescription = "${recipe.name}, recipe, ${recipe.caloriesPer100g.toInt()} kcal per 100 grams" },
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(
            text = recipe.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
          )
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .background(MaterialTheme.colorScheme.tertiaryContainer)
              .padding(horizontal = 6.dp, vertical = 2.dp),
          ) {
            Text(
              text = "My Recipe",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
          }
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
          text = "Cooked yield: ${recipe.cookedWeightGrams.toInt()} g · Total ${recipe.totalCalories.toInt()} kcal",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = "P %.1fg  ·  C %.1fg  ·  F %.1fg".format(recipe.proteinPer100g, recipe.carbsPer100g, recipe.fatPer100g),
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Medium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      Spacer(modifier = Modifier.width(8.dp))

      Row(verticalAlignment = Alignment.CenterVertically) {
        Column(horizontalAlignment = Alignment.End) {
          Text(
            text = "${recipe.caloriesPer100g.toInt()}",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
          )
          Text(
            text = "kcal / 100g",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        Spacer(modifier = Modifier.width(4.dp))
        IconButton(
          onClick = onEdit,
          modifier = Modifier
            .size(36.dp)
            .semantics { contentDescription = "Edit ${recipe.name}" },
        ) {
          Icon(
            painter = painterResource(id = R.drawable.ic_edit),
            contentDescription = "Edit",
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        IconButton(
          onClick = onDelete,
          modifier = Modifier
            .size(36.dp)
            .semantics { contentDescription = "Delete ${recipe.name}" },
        ) {
          Icon(
            painter = painterResource(id = R.drawable.ic_delete),
            contentDescription = "Delete",
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.error,
          )
        }
      }
    }
  }
}

@Composable
private fun EmptySearchState(
  query: String,
  selectedTab: FoodSearchTab,
  onCreateCustomFood: () -> Unit,
  onCreateRecipe: () -> Unit = {},
  modifier: Modifier = Modifier,
) {
  val isMyFoods = selectedTab == FoodSearchTab.MY_FOODS
  val isMyRecipes = selectedTab == FoodSearchTab.MY_RECIPES
  val isQueryBlank = query.trim().isBlank()

  val title = when {
    isMyRecipes && isQueryBlank -> "No recipes yet"
    isMyRecipes -> "No recipes found"
    isMyFoods && isQueryBlank -> "No custom foods yet"
    isMyFoods -> "No custom foods found"
    else -> "No foods found"
  }

  val description = when {
    isMyRecipes && isQueryBlank -> "You haven't created any recipes yet. Tap below to build a recipe from ingredients."
    isMyRecipes -> "No recipes matching \"${query.trim()}\"."
    isMyFoods && isQueryBlank -> "You haven't created any custom foods yet. Tap below to create your own food."
    isMyFoods -> "No custom foods matching \"${query.trim()}\"."
    else -> "No matching items found for \"${query.trim()}\". You can add it as a custom food or build a recipe."
  }

  Column(
    modifier = modifier.padding(24.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Icon(
      painter = painterResource(id = R.drawable.ic_search),
      contentDescription = null,
      modifier = Modifier.size(56.dp),
      tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
      text = title,
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurface,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
      text = description,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(20.dp))

    if (isMyRecipes) {
      Button(
        onClick = onCreateRecipe,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.semantics { contentDescription = "Create Recipe button" },
      ) {
        Icon(
          painter = painterResource(id = R.drawable.ic_add),
          contentDescription = null,
          modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("Create Recipe")
      }
    } else {
      Button(
        onClick = onCreateCustomFood,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.semantics { contentDescription = "Create Custom Food button" },
      ) {
        Icon(
          painter = painterResource(id = R.drawable.ic_add),
          contentDescription = null,
          modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("Create Custom Food")
      }
    }
  }
}
