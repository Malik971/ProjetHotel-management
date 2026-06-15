<script setup>
// ============================================================
// DashboardView - Le parcours guide de demonstration
// ============================================================
//
// Coeur de l'application. Une action principale, "Lancer la demonstration",
// charge un dossier exploitable et allume le circuit sur son etape courante.
// Le visiteur fait ensuite avancer le dossier (validation, confirmation,
// terminaison), puis active le polling pour voir Sejour rattraper l'etat.
//
// A cet increment, l'affichage est inline. Les composants sur-mesure
// (WorkflowDiagram, TutorialGuide, panels) seront extraits ensuite.
// ============================================================

import { ref, computed } from "vue";
import { useApi } from "../composables/useApi";
import { usePolling } from "../composables/usePolling";

const api = useApi();

const loading = ref(false);
const errorMessage = ref("");

const sync = ref(null);
const reservation = ref(null);
const pastellDoc = ref(null);
const journal = ref([]);

const idD = computed(() => sync.value?.pastellDocumentId || null);
const syncId = computed(() => sync.value?.syncId || null);

// Etape circuit courante : on prefere l'etat frais renvoye par le connecteur,
// avec repli sur l'etape dernier connu cote Sejour.
const etapeCircuit = computed(
    () => pastellDoc.value?.info?.last_action || sync.value?.etapeCircuit || null
);
const actionsPossibles = computed(() => pastellDoc.value?.action_possible || []);

// Circuit lineaire du dossier. L'annulation est une branche a part.
const WORKFLOW_STEPS = [
    { key: "creation", label: "Creation" },
    { key: "en-attente-validation", label: "En attente validation" },
    { key: "validee", label: "Validee" },
    { key: "confirmee", label: "Confirmee" },
    { key: "terminee", label: "Terminee" },
];

const ACTIONS = [
    { name: "validation", label: "Validation", icon: "mdi-check", color: "primary" },
    { name: "confirmation", label: "Confirmation", icon: "mdi-check-all", color: "primary" },
    { name: "terminaison", label: "Terminaison", icon: "mdi-flag-checkered", color: "success" },
    { name: "annulation", label: "Annulation", icon: "mdi-close-octagon", color: "error" },
];

const activeIndex = computed(() =>
    WORKFLOW_STEPS.findIndex((s) => s.key === etapeCircuit.value)
);

function stepState(index) {
    if (etapeCircuit.value === "annulee") {
        return "annulee";
    }
    if (index < activeIndex.value) {
        return "past";
    }
    if (index === activeIndex.value) {
        return "current";
    }
    return "future";
}

// Cohrence Sejour vers Pastell : pour un statut de reservation donne, quelles
// etapes circuit sont compatibles. Reprise de l'ancien dashboard.
const COHERENCE_MAP = {
    CONFIRMEE: ["creation", "en-attente-validation", "validee", "confirmee"],
    TERMINEE: ["terminee"],
    ANNULEE: ["annulee"],
    EN_ATTENTE: ["creation"],
};

const aligned = computed(() => {
    if (!reservation.value?.statut || !etapeCircuit.value) {
        return null;
    }
    const allowed = COHERENCE_MAP[reservation.value.statut] || [];
    return allowed.includes(etapeCircuit.value);
});

function resetState() {
    sync.value = null;
    reservation.value = null;
    pastellDoc.value = null;
    journal.value = [];
}

/**
 * Ordonne les candidats : ceux qui ont un id de document Pastell, avancables
 * d'abord (etat non terminal, plus interessants pour la demo), puis les autres.
 * La liste recue est deja triee par synchro la plus recente en premier.
 */
function orderCandidates(items) {
    const withDoc = items.filter((s) => s.pastellDocumentId);
    const isTerminal = (s) => ["terminee", "annulee"].includes((s.etapeCircuit || "").toLowerCase());
    return [...withDoc.filter((s) => !isTerminal(s)), ...withDoc.filter(isTerminal)];
}

async function launchDemo() {
    loading.value = true;
    errorMessage.value = "";
    resetState();
    try {
        const page = await api.listSyncs(0, 50);
        const candidates = orderCandidates(page?.content || []);
        if (candidates.length === 0) {
            errorMessage.value =
                "Aucun dossier a montrer. Creez une reservation depuis l'application, puis relancez.";
            return;
        }

        // Un dossier connu de Sejour peut avoir disparu du connecteur si celui-ci
        // a redemarre : son store est en memoire. On essaie donc les candidats, du
        // plus recent au plus ancien, et on garde le premier qui existe encore.
        let chosen = null;
        let doc = null;
        for (const candidate of candidates.slice(0, 12)) {
            try {
                doc = await api.getDemoDocument(candidate.pastellDocumentId);
                chosen = candidate;
                break;
            } catch {
                // 404 : ce dossier n'est plus dans le connecteur, on tente le suivant.
                continue;
            }
        }

        if (!chosen) {
            errorMessage.value =
                "Les dossiers connus de Sejour n'existent plus dans le connecteur, qui a redemarre "
                + "et vide sa memoire. Creez une nouvelle reservation depuis l'application pour generer "
                + "un dossier frais, puis relancez la demonstration.";
            return;
        }

        sync.value = chosen;
        pastellDoc.value = doc;
        await Promise.all([loadReservation(), loadJournal()]);
    } catch (e) {
        errorMessage.value = e.message;
    } finally {
        loading.value = false;
    }
}

async function loadReservation() {
    if (sync.value?.reservationId) {
        reservation.value = await api.getReservation(sync.value.reservationId);
    }
}

async function loadDocument() {
    if (idD.value) {
        pastellDoc.value = await api.getDemoDocument(idD.value);
    }
}

async function loadJournal() {
    if (syncId.value) {
        journal.value = await api.getSyncJournal(syncId.value);
    }
}

async function runAction(action) {
    errorMessage.value = "";
    try {
        // Le connecteur applique la transition et renvoie le nouvel etat : le
        // circuit s'allume aussitot. Le journal cote Sejour, lui, ne bougera
        // qu'au prochain polling : c'est justement ce que la suite illustre.
        pastellDoc.value = await api.doDemoAction(idD.value, action);
    } catch (e) {
        errorMessage.value = `Action refusee par le bus : ${e.message}`;
    }
}

const polling = usePolling(30, async () => {
    // allSettled : un rafraichissement qui echoue (ex: dossier disparu du
    // connecteur) ne doit pas empecher les deux autres de se mettre a jour.
    await Promise.allSettled([loadDocument(), loadJournal(), loadReservation()]);
});

async function forcePollNow() {
    errorMessage.value = "";
    try {
        await api.forcePoll();
        await Promise.all([loadJournal(), loadReservation(), loadDocument()]);
    } catch (e) {
        errorMessage.value = e.message;
    }
}

function formatWhen(value) {
    if (!value) {
        return "";
    }
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? String(value) : date.toLocaleString("fr-FR");
}

const journalSorted = computed(() =>
    [...journal.value].sort((a, b) => (b.idJ || 0) - (a.idJ || 0))
);
</script>

<template>
  <v-container class="py-6" style="max-width: 1100px;">

    <!-- Accroche et action principale -->
    <div class="text-center mb-6">
      <h1 class="text-h5 font-weight-bold mb-1">Le voyage d'un dossier dans le bus Pastell</h1>
      <p class="text-body-1 text-medium-emphasis mb-4">
        Suivez une reservation pendant qu'elle circule dans le bus d'orchestration,
        et regardez Sejour se resynchroniser par polling.
      </p>

      <v-btn
        v-if="!sync"
        color="secondary"
        size="large"
        prepend-icon="mdi-play"
        :loading="loading"
        @click="launchDemo"
      >
        Lancer la demonstration
      </v-btn>

      <v-btn
        v-else
        variant="text"
        prepend-icon="mdi-refresh"
        :loading="loading"
        @click="launchDemo"
      >
        Choisir un autre dossier
      </v-btn>
    </div>

    <v-alert
      v-if="errorMessage"
      type="warning"
      variant="tonal"
      class="mb-4"
      closable
      @click:close="errorMessage = ''"
    >
      {{ errorMessage }}
    </v-alert>

    <!-- Guide simple, en attendant le TutorialGuide dedie -->
    <v-alert v-if="sync" type="info" variant="tonal" density="comfortable" class="mb-6">
      <strong>Comment lire cette page :</strong>
      1. le circuit ci-dessous montre ou en est le dossier.
      2. faites-le avancer avec les actions.
      3. activez le polling : Sejour rattrape l'etat et la coherence passe au vert.
    </v-alert>

    <template v-if="sync">
      <!-- Circuit du dossier -->
      <v-card class="pa-4 mb-4">
        <div class="d-flex align-center justify-space-between mb-3">
          <span class="text-subtitle-1 font-weight-bold">Circuit du dossier</span>
          <v-chip
            v-if="aligned !== null"
            :color="aligned ? 'success' : 'warning'"
            variant="flat"
            size="small"
          >
            <v-icon start :icon="aligned ? 'mdi-check-circle' : 'mdi-sync-alert'" />
            {{ aligned ? 'Aligne' : 'Desynchronise' }}
          </v-chip>
        </div>

        <div class="d-flex align-center workflow-scroll pb-2">
          <template v-for="(step, i) in WORKFLOW_STEPS" :key="step.key">
            <v-chip
              :color="stepState(i) === 'future' ? undefined : 'primary'"
              :variant="stepState(i) === 'current' ? 'flat' : 'tonal'"
              size="small"
              class="flex-shrink-0"
            >
              {{ step.label }}
            </v-chip>
            <v-icon
              v-if="i < WORKFLOW_STEPS.length - 1"
              icon="mdi-chevron-right"
              size="small"
              class="mx-1 text-medium-emphasis flex-shrink-0"
            />
          </template>
          <v-divider vertical class="mx-3" />
          <v-chip
            :color="etapeCircuit === 'annulee' ? 'error' : undefined"
            :variant="etapeCircuit === 'annulee' ? 'flat' : 'tonal'"
            size="small"
            class="flex-shrink-0"
          >
            Annulee
          </v-chip>
        </div>
      </v-card>

      <!-- Panneaux Reservation et Pastell -->
      <v-row>
        <v-col cols="12" md="6">
          <v-card class="pa-4 h-100">
            <div class="text-subtitle-1 font-weight-bold mb-3">
              <v-icon icon="mdi-bed-outline" class="mr-1" /> Reservation, cote Sejour
            </div>
            <template v-if="reservation">
              <div class="d-flex justify-space-between py-1">
                <span class="text-medium-emphasis">Client</span>
                <span>{{ reservation.nomClient || '-' }}</span>
              </div>
              <div class="d-flex justify-space-between py-1">
                <span class="text-medium-emphasis">Hotel</span>
                <span>{{ reservation.hotelNom || '-' }}</span>
              </div>
              <div class="d-flex justify-space-between py-1">
                <span class="text-medium-emphasis">Sejour</span>
                <span>{{ reservation.dateDebut }} au {{ reservation.dateFin }}</span>
              </div>
              <div class="d-flex justify-space-between py-1 align-center">
                <span class="text-medium-emphasis">Statut</span>
                <v-chip size="small" variant="tonal" color="primary">{{ reservation.statut }}</v-chip>
              </div>
            </template>
            <v-skeleton-loader v-else type="list-item-three-line" />
          </v-card>
        </v-col>

        <v-col cols="12" md="6">
          <v-card class="pa-4 h-100">
            <div class="text-subtitle-1 font-weight-bold mb-3">
              <v-icon icon="mdi-transit-connection-variant" class="mr-1" /> Dossier, dans le bus
            </div>
            <template v-if="pastellDoc">
              <div class="d-flex justify-space-between py-1">
                <span class="text-medium-emphasis">Identifiant</span>
                <span class="mono">{{ pastellDoc.info?.id_d }}</span>
              </div>
              <div class="d-flex justify-space-between py-1 align-center">
                <span class="text-medium-emphasis">Etape circuit</span>
                <v-chip size="small" variant="tonal" color="secondary">{{ etapeCircuit }}</v-chip>
              </div>
              <div class="d-flex justify-space-between py-1">
                <span class="text-medium-emphasis">Derniere transition</span>
                <span>{{ pastellDoc.info?.last_action_date || '-' }}</span>
              </div>
            </template>
            <v-skeleton-loader v-else type="list-item-three-line" />
          </v-card>
        </v-col>
      </v-row>

      <!-- Actions : faire avancer le dossier -->
      <v-card class="pa-4 mt-4">
        <div class="text-subtitle-1 font-weight-bold mb-3">Faire avancer le dossier</div>
        <div class="d-flex flex-wrap ga-2">
          <v-btn
            v-for="action in ACTIONS"
            :key="action.name"
            :color="action.color"
            :prepend-icon="action.icon"
            variant="flat"
            :disabled="!actionsPossibles.includes(action.name)"
            @click="runAction(action.name)"
          >
            {{ action.label }}
          </v-btn>
        </div>
        <p v-if="actionsPossibles.length === 0" class="text-body-2 text-medium-emphasis mt-3 mb-0">
          Etat terminal du circuit : plus aucune action possible. Le polling Sejour
          va bientot synchroniser cet etat.
        </p>
      </v-card>

      <!-- Journal et polling -->
      <v-card class="pa-4 mt-4">
        <div class="d-flex align-center justify-space-between mb-3">
          <span class="text-subtitle-1 font-weight-bold">Journal du dossier, vu par Sejour</span>
          <div class="d-flex align-center ga-2">
            <span v-if="polling.enabled.value" class="text-caption text-medium-emphasis">
              Prochain poll dans {{ polling.countdown.value }}s
            </span>
            <v-btn
              size="small"
              :color="polling.enabled.value ? 'error' : 'primary'"
              :variant="polling.enabled.value ? 'tonal' : 'flat'"
              :prepend-icon="polling.enabled.value ? 'mdi-stop' : 'mdi-sync'"
              @click="polling.toggle()"
            >
              {{ polling.enabled.value ? 'Arreter le polling' : 'Activer le polling (30s)' }}
            </v-btn>
            <v-btn size="small" variant="text" prepend-icon="mdi-refresh" @click="forcePollNow">
              Forcer un poll
            </v-btn>
          </div>
        </div>

        <v-table v-if="journalSorted.length" density="compact">
          <thead>
            <tr>
              <th class="text-left">id_j</th>
              <th class="text-left">Action</th>
              <th class="text-left">Quand</th>
              <th class="text-left">Message</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="entry in journalSorted" :key="entry.id || entry.idJ">
              <td class="mono">#{{ entry.idJ }}</td>
              <td>{{ entry.action || '-' }}</td>
              <td>{{ formatWhen(entry.occurredAt) }}</td>
              <td class="text-medium-emphasis">{{ entry.message || '' }}</td>
            </tr>
          </tbody>
        </v-table>
        <p v-else class="text-body-2 text-medium-emphasis mb-0">
          Journal vide cote Sejour. Faites avancer le dossier, puis activez le
          polling : les entrees apparaissent quand Sejour interroge le bus.
        </p>
      </v-card>
    </template>
  </v-container>
</template>
