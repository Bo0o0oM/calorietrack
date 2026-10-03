package com.calorietrack.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calorietrack.app.R
import com.calorietrack.app.theme.CalorieAccentColor
import com.calorietrack.app.theme.CarbsColor
import com.calorietrack.app.theme.FatColor
import com.calorietrack.app.theme.ProteinColor

@Composable
fun SettingsScreen(
  onBack: () -> Unit,
  viewModel: SettingsViewModel,
  modifier: Modifier = Modifier,
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()

  SettingsContent(
    state = state,
    onBack = onBack,
    onCaloriesChanged = viewModel::onCaloriesChanged,
    onProteinChanged = viewModel::onProteinChanged,
    onCarbsChanged = viewModel::onCarbsChanged,
    onFatChanged = viewModel::onFatChanged,
    onResetToDefaults = viewModel::onResetToDefaults,
    onSaveGoals = { viewModel.saveGoals(onSaved = onBack) },
    modifier = modifier,
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
  state: SettingsUiState,
  onBack: () -> Unit,
  onCaloriesChanged: (String) -> Unit,
  onProteinChanged: (String) -> Unit,
  onCarbsChanged: (String) -> Unit,
  onFatChanged: (String) -> Unit,
  onResetToDefaults: () -> Unit,
  onSaveGoals: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Scaffold(
    modifier = modifier,
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = "Settings",
            fontWeight = FontWeight.Bold,
          )
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
            containerColor = MaterialTheme.colorScheme.background,
          ),
      )
    },
  ) { innerPadding ->
    Column(
      modifier =
        Modifier.fillMaxSize()
          .padding(innerPadding)
          .padding(horizontal = 20.dp)
          .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Spacer(modifier = Modifier.height(4.dp))

      // Section: Daily Nutrition Goals
      Text(
        text = "Daily Nutrition Goals",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
      )

      Text(
        text =
          "Configure your daily targets for today. The Dashboard updates your remaining calories and progress bars automatically.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      // Nutrition Inputs Card
      ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors =
          CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
          ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          GoalInputField(
            label = "Calories",
            value = state.caloriesInput,
            suffix = "kcal",
            error = state.caloriesError,
            accentColor = CalorieAccentColor,
            imeAction = ImeAction.Next,
            onValueChange = onCaloriesChanged,
          )

          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

          GoalInputField(
            label = "Protein",
            value = state.proteinInput,
            suffix = "g",
            error = state.proteinError,
            accentColor = ProteinColor,
            imeAction = ImeAction.Next,
            onValueChange = onProteinChanged,
          )

          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

          GoalInputField(
            label = "Carbohydrates",
            value = state.carbsInput,
            suffix = "g",
            error = state.carbsError,
            accentColor = CarbsColor,
            imeAction = ImeAction.Next,
            onValueChange = onCarbsChanged,
          )

          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

          GoalInputField(
            label = "Fat",
            value = state.fatInput,
            suffix = "g",
            error = state.fatError,
            accentColor = FatColor,
            imeAction = ImeAction.Done,
            onValueChange = onFatChanged,
          )
        }
      }

      // Disclaimer & Baseline Info Card
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors =
          CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
          ),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Text(
            text = "Application Baseline Notice",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            text =
              "Default values (2,000 kcal, 140g protein, 250g carbs, 70g fat) are standard application defaults, not medical or clinical dietary recommendations. Adjust targets according to your personal nutritional requirements.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      // Actions
      Button(
        onClick = onSaveGoals,
        enabled = state.isValid && !state.isSaving,
        modifier =
          Modifier.fillMaxWidth()
            .height(50.dp)
            .semantics { contentDescription = "Save Goals button" },
        shape = RoundedCornerShape(12.dp),
      ) {
        if (state.isSaving) {
          CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.onPrimary,
          )
        } else {
          Text(
            text = "Save Goals",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium,
          )
        }
      }

      OutlinedButton(
        onClick = onResetToDefaults,
        modifier =
          Modifier.fillMaxWidth()
            .height(48.dp)
            .semantics { contentDescription = "Reset to defaults button" },
        shape = RoundedCornerShape(12.dp),
      ) {
        Text(
          text = "Reset to defaults",
          fontWeight = FontWeight.Medium,
          style = MaterialTheme.typography.bodyLarge,
        )
      }

      Spacer(modifier = Modifier.height(24.dp))
    }
  }
}

@Composable
private fun GoalInputField(
  label: String,
  value: String,
  suffix: String,
  error: String?,
  accentColor: Color,
  imeAction: ImeAction,
  onValueChange: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text(
          text = label,
          style = MaterialTheme.typography.bodyLarge,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface,
        )
      }

      OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        suffix = {
          Text(
            text = suffix,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        },
        singleLine = true,
        isError = error != null,
        keyboardOptions =
          KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = imeAction,
          ),
        shape = RoundedCornerShape(10.dp),
        modifier =
          Modifier.width(160.dp).semantics {
            contentDescription = "$label input in $suffix"
          },
      )
    }

    if (error != null) {
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = error,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.padding(start = 4.dp),
      )
    }
  }
}
