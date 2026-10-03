package com.calorietrack.app.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calorietrack.app.R
import com.calorietrack.app.theme.CalorieAccentColor
import com.calorietrack.app.theme.CarbsColor
import com.calorietrack.app.theme.FatColor
import com.calorietrack.app.theme.ProteinColor

@Composable
fun HistoryScreen(
  onBack: () -> Unit,
  onSelectDate: (dateIso: String) -> Unit,
  viewModel: HistoryViewModel,
  modifier: Modifier = Modifier,
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()

  HistoryContent(
    state = state,
    onBack = onBack,
    onSelectDate = onSelectDate,
    modifier = modifier,
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryContent(
  state: HistoryUiState,
  onBack: () -> Unit,
  onSelectDate: (dateIso: String) -> Unit,
  modifier: Modifier = Modifier,
) {
  Scaffold(
    modifier = modifier,
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = "Daily History",
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
    if (state.isEmpty) {
      EmptyHistoryView(
        onBack = onBack,
        modifier = Modifier.padding(innerPadding),
      )
    } else {
      LazyColumn(
        modifier = Modifier.fillMaxSize().padding(innerPadding),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        items(state.days, key = { it.dateIso }) { day ->
          HistoryDayCard(
            item = day,
            onClick = { onSelectDate(day.dateIso) },
          )
        }
      }
    }
  }
}

@Composable
private fun HistoryDayCard(
  item: HistoryDayItem,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  ElevatedCard(
    modifier =
      modifier
        .fillMaxWidth()
        .clickable(onClick = onClick)
        .semantics {
          contentDescription = "History for ${item.formattedDate}"
        },
    shape = RoundedCornerShape(16.dp),
    colors =
      CardDefaults.elevatedCardColors(
        containerColor = MaterialTheme.colorScheme.surface,
      ),
    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = item.formattedDate,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = "${item.totalCalories} kcal",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = CalorieAccentColor,
          )
          if (item.calorieGoal != null) {
            Text(
              text = " / ${item.calorieGoal} kcal",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }

      if (item.calorieGoal == null) {
        Text(
          text = "No goal set for this date",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      // Compact Macros
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        MacroSummaryText(
          label = "Protein",
          consumed = item.totalProtein,
          goal = item.proteinGoal,
          color = ProteinColor,
        )
        MacroSummaryText(
          label = "Carbs",
          consumed = item.totalCarbs,
          goal = item.carbsGoal,
          color = CarbsColor,
        )
        MacroSummaryText(
          label = "Fat",
          consumed = item.totalFat,
          goal = item.fatGoal,
          color = FatColor,
        )
      }
    }
  }
}

@Composable
private fun MacroSummaryText(
  label: String,
  consumed: Int,
  goal: Int?,
  color: androidx.compose.ui.graphics.Color,
) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Text(
      text = "$label: ",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
      text = "${consumed}g",
      style = MaterialTheme.typography.bodySmall,
      fontWeight = FontWeight.Bold,
      color = color,
    )
    if (goal != null) {
      Text(
        text = "/${goal}g",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun EmptyHistoryView(
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier =
      modifier
        .fillMaxSize()
        .padding(32.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Icon(
      painter = painterResource(id = R.drawable.ic_history),
      contentDescription = null,
      tint = MaterialTheme.colorScheme.primary,
      modifier = Modifier.size(56.dp),
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
      text = "No History Yet",
      style = MaterialTheme.typography.titleLarge,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurface,
      textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
      text = "Days with logged foods or configured daily goals will appear here as you track your nutrition.",
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
