package de.loxitation.pht.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.loxitation.pht.R
import de.loxitation.pht.data.local.BloodPressureMeasurementEntity
import de.loxitation.pht.data.local.SyncStatus
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun MeasurementsScreen(viewModel: MeasurementsViewModel) {
    val measurements by viewModel.measurements.collectAsState()

    if (measurements.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(R.string.measurements_empty))
        }
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        items(measurements, key = { it.measurementUuid }) { entity ->
            MeasurementCard(entity)
        }
    }
}

@Composable
private fun MeasurementCard(entity: BloodPressureMeasurementEntity) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "${entity.systolic.toInt()} / ${entity.diastolic.toInt()} mmHg",
                style = MaterialTheme.typography.titleLarge,
            )
            entity.pulse?.let { pulse ->
                Text(text = "Puls: $pulse bpm", style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                text = formatRecordedAt(entity.recordedAtEpochSeconds),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = stringResource(entity.syncStatus.labelRes()),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

private fun formatRecordedAt(epochSeconds: Long): String {
    val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
    return Instant.ofEpochSecond(epochSeconds).atZone(ZoneId.systemDefault()).format(formatter)
}

private fun SyncStatus.labelRes(): Int = when (this) {
    SyncStatus.PENDING -> R.string.measurements_sync_pending
    SyncStatus.SYNCED -> R.string.measurements_sync_synced
    SyncStatus.FAILED -> R.string.measurements_sync_failed
}
