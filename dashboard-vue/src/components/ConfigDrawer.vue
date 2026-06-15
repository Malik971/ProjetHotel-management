<script setup>
// Tiroir de configuration. Ferme par defaut, ouvert seulement si le visiteur
// clique sur l'engrenage. Il contient les reglages techniques (URL du backend,
// token de demo) que l'on veut hors du chemin principal pour ne pas distraire
// un visiteur non technique.
import { useConfig } from "../composables/useConfig";

defineProps({
    modelValue: {
        type: Boolean,
        default: false,
    },
});

const emit = defineEmits(["update:modelValue"]);

const { config, resetConfig } = useConfig();

function close() {
    emit("update:modelValue", false);
}
</script>

<template>
  <v-navigation-drawer
    :model-value="modelValue"
    location="right"
    temporary
    width="380"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <div class="pa-4">
      <div class="d-flex align-center mb-2">
        <v-icon icon="mdi-cog-outline" class="mr-2" />
        <span class="text-subtitle-1 font-weight-bold">Configuration</span>
        <v-spacer />
        <v-btn icon="mdi-close" variant="text" size="small" aria-label="Fermer" @click="close" />
      </div>

      <p class="text-body-2 text-medium-emphasis mb-4">
        Reglages techniques de la connexion a sejour-backend. Inutiles pour suivre
        la demonstration, ils servent a pointer vers un autre environnement.
      </p>

      <v-text-field
        v-model="config.backendUrl"
        label="URL de sejour-backend"
        variant="outlined"
        density="comfortable"
        prepend-inner-icon="mdi-server-network"
      />

      <v-text-field
        v-model="config.demoToken"
        label="X-Demo-Token"
        variant="outlined"
        density="comfortable"
        prepend-inner-icon="mdi-key-variant"
        hint="Autorise les actions qui font avancer le bus"
        persistent-hint
      />

      <v-alert
        type="info"
        variant="tonal"
        density="comfortable"
        class="mt-4 text-body-2"
      >
        Le dashboard ne contacte jamais le connecteur en direct. Tout passe par
        sejour-backend, qui porte les credentials Pastell cote serveur.
      </v-alert>

      <v-btn
        variant="text"
        color="secondary"
        prepend-icon="mdi-restore"
        class="mt-4"
        @click="resetConfig"
      >
        Reglages par defaut
      </v-btn>
    </div>
  </v-navigation-drawer>
</template>
