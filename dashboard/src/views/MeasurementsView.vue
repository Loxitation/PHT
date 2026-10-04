<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Line } from 'vue-chartjs'
import {
  CategoryScale,
  Chart as ChartJS,
  Legend,
  LinearScale,
  LineElement,
  PointElement,
  TimeScale,
  Title,
  Tooltip,
} from 'chart.js'
import 'chartjs-adapter-date-fns'
import { fetchBloodPressureMeasurements } from '../api/measurements'
import type { BloodPressureMeasurement } from '../types/measurement'

ChartJS.register(
  CategoryScale,
  LinearScale,
  TimeScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend,
)

const measurements = ref<BloodPressureMeasurement[]>([])
const isLoading = ref(true)
const errorMessage = ref('')

onMounted(async () => {
  try {
    measurements.value = await fetchBloodPressureMeasurements()
  } catch {
    errorMessage.value = 'Messwerte konnten nicht geladen werden.'
  } finally {
    isLoading.value = false
  }
})

// Chart.js expects chronological order; the API returns newest-first.
const chronological = computed(() => [...measurements.value].reverse())

const chartData = computed(() => ({
  labels: chronological.value.map((m) => m.recorded_at),
  datasets: [
    {
      label: 'Systolisch',
      data: chronological.value.map((m) => m.systolic),
      borderColor: '#ef5350',
      backgroundColor: '#ef5350',
      tension: 0.2,
    },
    {
      label: 'Diastolisch',
      data: chronological.value.map((m) => m.diastolic),
      borderColor: '#42a5f5',
      backgroundColor: '#42a5f5',
      tension: 0.2,
    },
    {
      label: 'Puls',
      data: chronological.value.map((m) => m.pulse),
      borderColor: '#66bb6a',
      backgroundColor: '#66bb6a',
      tension: 0.2,
      yAxisID: 'pulse',
    },
  ],
}))

const chartOptions = {
  responsive: true,
  scales: {
    x: { type: 'time' as const, time: { unit: 'day' as const } },
    y: { title: { display: true, text: 'mmHg' } },
    pulse: {
      position: 'right' as const,
      title: { display: true, text: 'bpm' },
      grid: { drawOnChartArea: false },
    },
  },
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleString('de-DE', { dateStyle: 'short', timeStyle: 'short' })
}
</script>

<template>
  <div class="measurements-page">
    <h1>Blutdruck-Verlauf</h1>

    <p v-if="isLoading">Lade Messwerte…</p>
    <p v-else-if="errorMessage" class="error">{{ errorMessage }}</p>
    <p v-else-if="measurements.length === 0">Noch keine Messwerte vorhanden.</p>

    <template v-else>
      <div class="chart-wrapper">
        <Line :data="chartData" :options="chartOptions" />
      </div>

      <table>
        <thead>
          <tr>
            <th>Zeitpunkt</th>
            <th>Systolisch</th>
            <th>Diastolisch</th>
            <th>Puls</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="m in measurements" :key="m.id">
            <td>{{ formatDate(m.recorded_at) }}</td>
            <td>{{ m.systolic ?? '–' }}</td>
            <td>{{ m.diastolic ?? '–' }}</td>
            <td>{{ m.pulse ?? '–' }}</td>
          </tr>
        </tbody>
      </table>
    </template>
  </div>
</template>

<style scoped>
.measurements-page {
  max-width: 900px;
  margin: 0 auto;
  padding: 1.5rem;
}
.chart-wrapper {
  margin-bottom: 2rem;
}
table {
  width: 100%;
  border-collapse: collapse;
}
th,
td {
  padding: 0.5rem;
  text-align: left;
  border-bottom: 1px solid #333;
}
.error {
  color: #ef5350;
}
</style>
