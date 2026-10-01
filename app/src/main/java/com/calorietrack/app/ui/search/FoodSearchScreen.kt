package com.calorietrack.app.ui.search

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calorietrack.app.R
import com.calorietrack.app.data.local.FoodEntity

@Composable
fun FoodSearchScreen(
  mealType: String,
  onBack: () -> Unit,
  onFoodClick: (foodId: Long) -> Unit = {},
  viewModel: FoodSearchViewModel,
  modifier: Modifier = Modifier,
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()

  FoodSearchContent(
    state = state,
    mealType = mealType,
    onQueryChanged = viewModel::onQueryChanged,
    onClearQuery = viewModel::onClearQuery,
    onBack = onBack,
    onFoodClick = onFoodClick,
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
  onBack: () -> Unit,
  onFoodClick: (foodId: Long) -> Unit,
  modifier: Modifier = Modifier,
) {
  val focusManager = LocalFocusManager.current
  val keyboardController = LocalSoftwareKeyboardController.current
  val subtitle = formatMealContextSubtitle(mealType)

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
          .padding(horizontal = 16.dp),
    ) {
      Spacer(modifier = Modifier.height(8.dp))

      // Search TextField
      OutlinedTextField(
        value = state.query,
        onValueChange = onQueryChanged,
        modifier =
          Modifier.fillMaxWidth().semantics {
            contentDescription = "Search foods input field"
          },
        placeholder = { Text("Search foods") },
        leadingIcon = {
          Icon(
            painter = painterResource(id = R.drawable.ic_search),
            contentDescription = "Search",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        },
        trailingIcon = {
          if (state.query.isNotEmpty()) {
            IconButton(
              onClick = onClearQuery,
              modifier =
                Modifier.semantics {
                  contentDescription = "Clear search query"
                },
            ) {
              Icon(
                painter = painterResource(id = R.drawable.ic_close),
                contentDescription = "Clear",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions =
          KeyboardActions(
            onSearch = {
              keyboardController?.hide()
              focusManager.clearFocus()
            }
          ),
        shape = RoundedCornerShape(16.dp),
        colors =
          OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
          ),
      )

      Spacer(modifier = Modifier.height(16.dp))

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
        state.isEmptyResult -> {
          EmptySearchState(
            query = state.query,
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
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun FoodResultCard(
  food: FoodEntity,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier =
      modifier
        .fillMaxWidth()
        .clickable(onClick = onClick)
        .semantics { contentDescription = "${food.name}, ${food.caloriesPer100g.toInt()} kcal per 100 grams" },
    shape = RoundedCornerShape(14.dp),
    colors =
      CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface
      ),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = food.name,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
          text = food.servingDescription,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text =
            "P %.1fg  ·  C %.1fg  ·  F %.1fg"
              .format(food.proteinPer100g, food.carbsPer100g, food.fatPer100g),
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Medium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      Spacer(modifier = Modifier.width(12.dp))

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
    }
  }
}

@Composable
private fun EmptySearchState(
  query: String,
  modifier: Modifier = Modifier,
) {
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
      text = "No foods found",
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurface,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
      text =
        "No matching items found for \"${query.trim()}\". The search currently covers the built-in offline catalogue (104 items).",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
    )
  }
}
