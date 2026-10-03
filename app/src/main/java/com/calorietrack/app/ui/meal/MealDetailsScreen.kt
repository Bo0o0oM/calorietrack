package com.calorietrack.app.ui.meal

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calorietrack.app.R
import com.calorietrack.app.data.local.MealEntryWithFood
import com.calorietrack.app.theme.CarbsColor
import com.calorietrack.app.theme.FatColor
import com.calorietrack.app.theme.ProteinColor
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun MealDetailsScreen(
  onBack: () -> Unit,
  onAddFood: (mealType: String) -> Unit,
  onEditEntry: (entry: MealEntryWithFood) -> Unit,
  viewModel: MealDetailsViewModel,
  modifier: Modifier = Modifier,
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()

  MealDetailsContent(
    state = state,
    onBack = onBack,
    onAddFood = onAddFood,
    onEditEntry = onEditEntry,
    onRequestDelete = viewModel::onRequestDelete,
    onDismissDelete = viewModel::onDismissDelete,
    onConfirmDelete = viewModel::onConfirmDelete,
    modifier = modifier,
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealDetailsContent(
  state: MealDetailsUiState,
  onBack: () -> Unit,
  onAddFood: (mealType: String) -> Unit,
  onEditEntry: (entry: MealEntryWithFood) -> Unit,
  onRequestDelete: (entry: MealEntryWithFood) -> Unit,
  onDismissDelete: () -> Unit,
  onConfirmDelete: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Scaffold(
    modifier = modifier,
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = state.displayName,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = state.subtitle,
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
        actions = {
          if (!state.isEmpty && !state.isLoading) {
            IconButton(onClick = { onAddFood(state.mealType) }) {
              Icon(
                painter = painterResource(id = R.drawable.ic_add),
                contentDescription = "Add food to ${state.displayName}",
              )
            }
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.background,
        ),
      )
    },
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

      state.isEmpty -> {
        EmptyMealState(
          mealName = state.displayName,
          onAddFood = { onAddFood(state.mealType) },
          modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(24.dp),
        )
      }

      else -> {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          // Meal Total Summary Card
          MealTotalCard(
            totalCalories = state.totalCalories,
            protein = state.totalProtein,
            carbs = state.totalCarbs,
            fat = state.totalFat,
          )

          // Section Title
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "Logged Foods (${state.entries.size})",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onBackground,
            )
          }

          // List of Entries
          state.entries.forEach { entry ->
            MealEntryCard(
              entry = entry,
              onEdit = { onEditEntry(entry) },
              onDelete = { onRequestDelete(entry) },
            )
          }

          Spacer(modifier = Modifier.height(24.dp))
        }
      }
    }

    // Delete Confirmation Dialog
    state.entryPendingDelete?.let { entry ->
      AlertDialog(
        onDismissRequest = onDismissDelete,
        title = {
          Text(text = "Remove ${entry.foodName}?")
        },
        text = {
          Text(text = "Remove ${entry.foodName} from ${state.displayName}?")
        },
        confirmButton = {
          Button(
            onClick = onConfirmDelete,
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.error,
              contentColor = MaterialTheme.colorScheme.onError,
            ),
          ) {
            Text("Remove")
          }
        },
        dismissButton = {
          TextButton(onClick = onDismissDelete) {
            Text("Cancel")
          }
        },
      )
    }
  }
}

@Composable
private fun MealTotalCard(
  totalCalories: Int,
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
        verticalAlignment = Alignment.Bottom,
      ) {
        Column {
          Text(
            text = "Meal Total",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "$totalCalories kcal",
            fontSize = 32.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary,
            lineHeight = 36.sp,
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
      Spacer(modifier = Modifier.height(16.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        MealMacroBox(
          label = "Protein",
          value = String.format(Locale.US, "%.1fg", protein),
          accentColor = ProteinColor,
          modifier = Modifier.weight(1f),
        )
        MealMacroBox(
          label = "Carbs",
          value = String.format(Locale.US, "%.1fg", carbs),
          accentColor = CarbsColor,
          modifier = Modifier.weight(1f),
        )
        MealMacroBox(
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
private fun MealMacroBox(
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
private fun MealEntryCard(
  entry: MealEntryWithFood,
  onEdit: () -> Unit,
  onDelete: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val quantityText = if (entry.quantityGrams % 1.0 == 0.0) {
    "${entry.quantityGrams.toInt()} g"
  } else {
    String.format(Locale.US, "%.1f g", entry.quantityGrams)
  }

  Card(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface,
    ),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = entry.foodName,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
          text = quantityText,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = String.format(
            Locale.US,
            "P %.1fg  ·  C %.1fg  ·  F %.1fg",
            entry.protein,
            entry.carbs,
            entry.fat,
          ),
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Medium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column(horizontalAlignment = Alignment.End) {
        Text(
          text = "${entry.calories.roundToInt()} kcal",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          IconButton(
            onClick = onEdit,
            modifier = Modifier
              .size(36.dp)
              .semantics {
                contentDescription = "Edit ${entry.foodName}"
              },
          ) {
            Icon(
              painter = painterResource(id = R.drawable.ic_edit),
              contentDescription = null,
              modifier = Modifier.size(20.dp),
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

          IconButton(
            onClick = onDelete,
            modifier = Modifier
              .size(36.dp)
              .semantics {
                contentDescription = "Delete ${entry.foodName}"
              },
          ) {
            Icon(
              painter = painterResource(id = R.drawable.ic_delete),
              contentDescription = null,
              modifier = Modifier.size(20.dp),
              tint = MaterialTheme.colorScheme.error,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun EmptyMealState(
  mealName: String,
  onAddFood: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier,
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
      text = "No foods logged in $mealName",
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurface,
      textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
      text = "Tap below to search and add food to your ${mealName.lowercase()}.",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(24.dp))
    Button(
      onClick = onAddFood,
      shape = RoundedCornerShape(12.dp),
    ) {
      Icon(
        painter = painterResource(id = R.drawable.ic_add),
        contentDescription = null,
        modifier = Modifier.size(18.dp),
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text("Add Food")
    }
  }
}
