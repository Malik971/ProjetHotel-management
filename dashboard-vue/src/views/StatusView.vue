<script setup>
// ============================================================
// StatusView - Etat du bus d'orchestration
// ============================================================
//
// Vue de supervision : compteurs de dossiers par etape de synchronisation,
// sante du connecteur, position du curseur de polling. Equivalent de l'ancienne
// page status.html, mais alimentee par l'endpoint status de sejour-backend.
// ============================================================

import { ref, onMounted, onUnmounted } from "vue";
import { useApi } from "../composables/useApi";

const api = useApi();

const status = ref(null);
const loading = ref(false);
const errorMessage = ref("");
let timer = null;

const COUNTERS = [
    { key: "syncCountOk", label: "Synchronises", icon: "mdi-check-circle", color: "success" },
    { key: "syncCountPending", label: "En traitement", icon: "mdi-clock-outline", color: "info" },
    { key: "syncCountEnRetry", label: "Relances", icon: "mdi-refresh", color: "warning" },
    { key: "syncCountEnErreur", label: "Anomalies", icon: "mdi-alert", color: "error" },
    { key: "syncCountDivergence", label: "Divergences", icon: "mdi-call-split", color: "secondary" },
];

async function refresh() {
    loading.value = true;
    errorMessage.value = "";
    try {
        status.value = await api.getStatus();
    } catch (e) {
        errorMessage.value = e.message;
    } finally {
        loading.value = false;
    }
}

function formatWhen(value) {
    if (!value) {
        return "-";
    }
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? String(value) : date.toLocaleString("fr-FR");
}

onMounted(() => {
    refresh();
    // Rafraichissement automatique leger toutes les 10 secondes.
    timer = setInterval(refresh, 10000);
});

onUnmounted(() => {
    if (timer) {
        clearInterval(timer);
    }
});
</script>

<template>
  <v-container class="py-6" style="max-width: 1100px;">
    <div class="d-flex align-center justify-space-between mb-4">
      <div>
        <h1 class="text-h5 font-weight-bold">Etat du bus d'orchestration</h1>
        <p class="text-body-2 text-medium-emphasis mb-0">
          Compteurs de synchronisation et sante du connecteur, rafraichis toutes les 10 secondes.
        </p>
      </div>
      <v-btn variant="text" prepend-icon="mdi-refresh" :loading="loading" @click="refresh">
        Rafraichir
      </v-btn>
    </div>

    <v-alert v-if="errorMessage" type="warning" variant="tonal" class="mb-4">
      {{ errorMessage }}
    </v-alert>

    <template v-if="status">
      <v-row>
        <v-col v-for="counter in COUNTERS" :key="counter.key" cols="6" md="4" lg="2">
          <v-card class="pa-4 text-center h-100">
            <v-icon :icon="counter.icon" :color="counter.color" size="28" class="mb-1" />
            <div class="text-h5 font-weight-bold">{{ status[counter.key] ?? 0 }}</div>
            <div class="text-caption text-medium-emphasis">{{ counter.label }}</div>
          </v-card>
        </v-col>
        <v-col cols="6" md="4" lg="2">
          <v-card class="pa-4 text-center h-100">
            <v-icon icon="mdi-bed" color="primary" size="28" class="mb-1" />
            <div class="text-h5 font-weight-bold">{{ status.reservationCount ?? 0 }}</div>
            <div class="text-caption text-medium-emphasis">Reservations</div>
          </v-card>
        </v-col>
      </v-row>

      <v-row class="mt-1">
        <v-col cols="12" md="6">
          <v-card class="pa-4 h-100">
            <div class="text-subtitle-1 font-weight-bold mb-3">Connecteur Pastell</div>
            <div class="d-flex justify-space-between py-1 align-center">
              <span class="text-medium-emphasis">Integration active</span>
              <v-chip size="small" :color="status.pastellEnabled ? 'success' : 'error'" variant="tonal">
                {{ status.pastellEnabled ? 'oui' : 'non' }}
              </v-chip>
            </div>
            <div class="d-flex justify-space-between py-1 align-center">
              <span class="text-medium-emphasis">Connecteur joignable</span>
              <v-chip size="small" :color="status.mockHealth?.reachable ? 'success' : 'error'" variant="tonal">
                {{ status.mockHealth?.reachable ? 'oui' : 'non' }}
              </v-chip>
            </div>
            <div class="d-flex justify-space-between py-1">
              <span class="text-medium-emphasis">Temps de reponse</span>
              <span>{{ status.mockHealth?.responseTimeMs != null ? status.mockHealth.responseTimeMs + ' ms' : '-' }}</span>
            </div>
            <div v-if="status.mockHealth?.errorMessage" class="text-caption text-error mt-1">
              {{ status.mockHealth.errorMessage }}
            </div>
          </v-card>
        </v-col>

        <v-col cols="12" md="6">
          <v-card class="pa-4 h-100">
            <div class="text-subtitle-1 font-weight-bold mb-3">Polling descendant</div>
            <div class="d-flex justify-space-between py-1">
              <span class="text-medium-emphasis">Derniere entree traitee</span>
              <span class="mono">{{ status.lastProcessedIdJ ?? '-' }}</span>
            </div>
            <div class="d-flex justify-space-between py-1">
              <span class="text-medium-emphasis">Dernier polling</span>
              <span>{{ formatWhen(status.lastPolledAt) }}</span>
            </div>
            <div class="d-flex justify-space-between py-1">
              <span class="text-medium-emphasis">Genere a</span>
              <span>{{ formatWhen(status.generatedAt) }}</span>
            </div>
          </v-card>
        </v-col>
      </v-row>
    </template>

    <v-skeleton-loader v-else-if="loading" type="card" />
  </v-container>
</template>
