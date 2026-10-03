package com.calorietrack.app.ui.custom

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calorietrack.app.R

@Composable
fun CustomFoodScreen(
  onBack: () -> Unit,
  onFoodSaved: (foodId: Long) -> Unit,
  viewModel: CustomFoodViewModel,
  modifier: Modifier = Modifier,
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()

  CustomFoodContent(
    state = state,
    onBack = onBack,
    onNameChanged = viewModel::onNameChanged,
    onServingDescriptionChanged = viewModel::onServingDescriptionChanged,
    onServingGramsChanged = viewModel::onServingGramsChanged,
    onCaloriesChanged = viewModel::onCaloriesChanged,
    onProteinChanged = viewModel::onProteinChanged,
    onCarbsChanged = viewModel::onCarbsChanged,
    onFatChanged = viewModel::onFatChanged,
    onSearchKeywordsChanged = viewModel::onSearchKeywordsChanged,
    onSave = { viewModel.saveCustomFood(onFoodSaved) },
    modifier = modifier,
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomFoodContent(
  state: CustomFoodUiState,
  onBack: () -> Unit,
  onNameChanged: (String) -> Unit,
  onServingDescriptionChanged: (String) -> Unit,
  onServingGramsChanged: (String) -> Unit,
  onCaloriesChanged: (String) -> Unit,
  onProteinChanged: (String) -> Unit,
  onCarbsChanged: (String) -> Unit,
  onFatChanged: (String) -> Unit,
  onSearchKeywordsChanged: (String) -> Unit,
  onSave: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val title = if (state.isEditMode) "Edit Custom Food" else "Create Custom Food"

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
            modifier = Modifier.semantics { contentDescription = "Navigate back" },
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

        // Section: Basic Info
        Text(
          text = "Food Details",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
          value = state.name,
          onValueChange = onNameChanged,
          label = { Text("Food Name *") },
          placeholder = { Text("e.g. Homemade Paneer") },
          singleLine = true,
          isError = state.nameError != null,
          supportingText = {
            if (state.nameError != null) {
              Text(state.nameError, color = MaterialTheme.colorScheme.error)
            }
          },
          modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Food name input" },
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          OutlinedTextField(
            value = state.servingDescription,
            onValueChange = onServingDescriptionChanged,
            label = { Text("Serving Description *") },
            placeholder = { Text("e.g. 100 g or 1 bowl") },
            singleLine = true,
            isError = state.servingDescriptionError != null,
            supportingText = {
              if (state.servingDescriptionError != null) {
                Text(state.servingDescriptionError, color = MaterialTheme.colorScheme.error)
              }
            },
            modifier = Modifier
              .weight(1.4f)
              .semantics { contentDescription = "Serving description input" },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
          )

          OutlinedTextField(
            value = state.servingGrams,
            onValueChange = onServingGramsChanged,
            label = { Text("Serving (g) *") },
            placeholder = { Text("100") },
            singleLine = true,
            isError = state.servingGramsError != null,
            supportingText = {
              if (state.servingGramsError != null) {
                Text(state.servingGramsError, color = MaterialTheme.colorScheme.error)
              }
            },
            modifier = Modifier
              .weight(1f)
              .semantics { contentDescription = "Serving size grams input" },
            keyboardOptions = KeyboardOptions(
              keyboardType = KeyboardType.Decimal,
              imeAction = ImeAction.Next,
            ),
          )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Section: Nutrition Per 100 Grams
        Text(
          text = "Nutrition per 100 grams",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = "Enter normalized nutritional values per 100 grams of food.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
          value = state.caloriesPer100g,
          onValueChange = onCaloriesChanged,
          label = { Text("Calories (kcal) *") },
          placeholder = { Text("e.g. 265") },
          singleLine = true,
          isError = state.caloriesError != null,
          supportingText = {
            if (state.caloriesError != null) {
              Text(state.caloriesError, color = MaterialTheme.colorScheme.error)
            }
          },
          modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Calories per 100 grams input" },
          keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal,
            imeAction = ImeAction.Next,
          ),
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          OutlinedTextField(
            value = state.proteinPer100g,
            onValueChange = onProteinChanged,
            label = { Text("Protein (g) *") },
            placeholder = { Text("18") },
            singleLine = true,
            isError = state.proteinError != null,
            supportingText = {
              if (state.proteinError != null) {
                Text(state.proteinError, color = MaterialTheme.colorScheme.error)
              }
            },
            modifier = Modifier
              .weight(1f)
              .semantics { contentDescription = "Protein per 100 grams input" },
            keyboardOptions = KeyboardOptions(
              keyboardType = KeyboardType.Decimal,
              imeAction = ImeAction.Next,
            ),
          )

          OutlinedTextField(
            value = state.carbsPer100g,
            onValueChange = onCarbsChanged,
            label = { Text("Carbs (g) *") },
            placeholder = { Text("6") },
            singleLine = true,
            isError = state.carbsError != null,
            supportingText = {
              if (state.carbsError != null) {
                Text(state.carbsError, color = MaterialTheme.colorScheme.error)
              }
            },
            modifier = Modifier
              .weight(1f)
              .semantics { contentDescription = "Carbs per 100 grams input" },
            keyboardOptions = KeyboardOptions(
              keyboardType = KeyboardType.Decimal,
              imeAction = ImeAction.Next,
            ),
          )

          OutlinedTextField(
            value = state.fatPer100g,
            onValueChange = onFatChanged,
            label = { Text("Fat (g) *") },
            placeholder = { Text("20") },
            singleLine = true,
            isError = state.fatError != null,
            supportingText = {
              if (state.fatError != null) {
                Text(state.fatError, color = MaterialTheme.colorScheme.error)
              }
            },
            modifier = Modifier
              .weight(1f)
              .semantics { contentDescription = "Fat per 100 grams input" },
            keyboardOptions = KeyboardOptions(
              keyboardType = KeyboardType.Decimal,
              imeAction = ImeAction.Next,
            ),
          )
        }

        if (state.macroSumError != null) {
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = state.macroSumError,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 4.dp),
          )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Section: Keywords (optional)
        Text(
          text = "Search Keywords (Optional)",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "Comma-separated synonyms or alternative names to help find this food.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
          value = state.searchKeywords,
          onValueChange = onSearchKeywordsChanged,
          label = { Text("Keywords") },
          placeholder = { Text("e.g. cottage cheese, paneer, chena") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Search keywords input" },
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        )

        Spacer(modifier = Modifier.height(28.dp))

        val buttonText = when {
          state.isEditMode -> "Save Changes"
          state.mealType.isNotBlank() -> "Save & Continue to Log"
          else -> "Save Custom Food"
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
}
