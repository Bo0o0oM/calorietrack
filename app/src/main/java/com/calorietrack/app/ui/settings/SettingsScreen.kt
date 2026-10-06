package com.calorietrack.app.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import java.time.LocalDate

@Composable
fun SettingsScreen(
  onBack: () -> Unit,
  viewModel: SettingsViewModel,
  modifier: Modifier = Modifier,
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  val context = LocalContext.current

  // Pending JSON content to write when user selects save location
  var pendingExportJson by remember { mutableStateOf<String?>(null) }

  val exportLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("application/json")
  ) { uri: Uri? ->
    if (uri != null && pendingExportJson != null) {
      try {
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
          outputStream.write(pendingExportJson!!.toByteArray(Charsets.UTF_8))
        }
        viewModel.onExportCompleted()
      } catch (e: Exception) {
        viewModel.onExportFailed(e.message ?: "Failed to write backup file.")
      } finally {
        pendingExportJson = null
      }
    } else {
      pendingExportJson = null
    }
  }

  val importLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri: Uri? ->
    if (uri != null) {
      try {
        val content = context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
          it.readText()
        }
        if (content != null) {
          viewModel.onBackupFileLoaded(content)
        } else {
          viewModel.onExportFailed("Unable to read selected file.")
        }
      } catch (e: Exception) {
        viewModel.onExportFailed("Error reading file: ${e.message}")
      }
    }
  }

  SettingsContent(
    state = state,
    onBack = onBack,
    onCaloriesChanged = viewModel::onCaloriesChanged,
    onProteinChanged = viewModel::onProteinChanged,
    onCarbsChanged = viewModel::onCarbsChanged,
    onFatChanged = viewModel::onFatChanged,
    onResetToDefaults = viewModel::onResetToDefaults,
    onSaveGoals = { viewModel.saveGoals(onSaved = onBack) },
    onRequestExport = {
      viewModel.exportBackupJson { jsonString ->
        pendingExportJson = jsonString
        val filename = "CalorieTrack-backup-${LocalDate.now()}.json"
        exportLauncher.launch(filename)
      }
    },
    onRequestImport = {
      importLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
    },
    onConfirmRestore = viewModel::onConfirmRestore,
    onDismissRestoreDialog = viewModel::onDismissRestoreDialog,
    onDismissFeedback = viewModel::onDismissBackupFeedback,
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
  onRequestExport: () -> Unit,
  onRequestImport: () -> Unit,
  onConfirmRestore: () -> Unit,
  onDismissRestoreDialog: () -> Unit,
  onDismissFeedback: () -> Unit,
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
        .padding(horizontal = 20.dp)
        .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Spacer(modifier = Modifier.height(4.dp))

      // Feedback cards
      state.backupMessage?.let { msg ->
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = msg,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
              style = MaterialTheme.typography.bodyMedium,
              modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onDismissFeedback, modifier = Modifier.size(24.dp)) {
              Icon(painter = painterResource(id = R.drawable.ic_close), contentDescription = "Dismiss", modifier = Modifier.size(16.dp))
            }
          }
        }
      }

      state.backupError?.let { err ->
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = err,
              color = MaterialTheme.colorScheme.onErrorContainer,
              style = MaterialTheme.typography.bodyMedium,
              modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onDismissFeedback, modifier = Modifier.size(24.dp)) {
              Icon(painter = painterResource(id = R.drawable.ic_close), contentDescription = "Dismiss", modifier = Modifier.size(16.dp))
            }
          }
        }
      }

      // Section: Daily Nutrition Goals
      Text(
        text = "Daily Nutrition Goals",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
      )

      Text(
        text = "Configure your daily targets for today. The Dashboard updates your remaining calories and progress bars automatically.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      // Nutrition Inputs Card
      ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
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

      // Actions
      Button(
        onClick = onSaveGoals,
        enabled = state.isValid && !state.isSaving,
        modifier = Modifier
          .fillMaxWidth()
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
        modifier = Modifier
          .fillMaxWidth()
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

      Spacer(modifier = Modifier.height(12.dp))
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
      Spacer(modifier = Modifier.height(8.dp))

      // Section: Data & Backup
      Text(
        text = "Data & Backup",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
      )

      Text(
        text = "Backups contain your custom foods, recipes, meal history, and daily goals. Built-in foods are included with the app and are not exported.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          // Export row
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Export My Data",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = "Create an offline backup of your CalorieTrack data",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Button(
              onClick = onRequestExport,
              enabled = !state.isExporting && !state.isRestoring,
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.semantics { contentDescription = "Export My Data button" },
            ) {
              if (state.isExporting) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
              } else {
                Text("Export")
              }
            }
          }

          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

          // Import row
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Import Backup",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = "Restore data from a CalorieTrack backup",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            OutlinedButton(
              onClick = onRequestImport,
              enabled = !state.isExporting && !state.isRestoring,
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.semantics { contentDescription = "Import Backup button" },
            ) {
              if (state.isRestoring) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp)
              } else {
                Text("Import")
              }
            }
          }
        }
      }

      // Baseline notice card
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
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
            text = "Default values (2,000 kcal, 140g protein, 250g carbs, 70g fat) are standard application defaults, not medical or clinical dietary recommendations. Adjust targets according to your personal nutritional requirements.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      Spacer(modifier = Modifier.height(28.dp))
    }
  }

  // Strong Confirmation Dialog for Restore
  if (state.showRestoreConfirmDialog) {
    AlertDialog(
      onDismissRequest = onDismissRestoreDialog,
      title = {
        Text("Replace User Data from Backup", fontWeight = FontWeight.Bold)
      },
      text = {
        Text("This will replace your current custom foods, recipes, meal history, and daily goals with the selected backup. Your built-in CalorieTrack food catalogue will not be removed.")
      },
      confirmButton = {
        Button(
          onClick = onConfirmRestore,
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
          modifier = Modifier.semantics { contentDescription = "Replace and Restore button" },
        ) {
          Text("Replace & Restore")
        }
      },
      dismissButton = {
        TextButton(
          onClick = onDismissRestoreDialog,
          modifier = Modifier.semantics { contentDescription = "Cancel restore button" },
        ) {
          Text("Cancel")
        }
      },
    )
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
      Text(
        text = label,
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
      )

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
        keyboardOptions = KeyboardOptions(
          keyboardType = KeyboardType.Number,
          imeAction = imeAction,
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.width(160.dp).semantics {
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
