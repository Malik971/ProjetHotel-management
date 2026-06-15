<script setup>
// Coquille de l'application : barre d'en-tete, tiroir de configuration et zone
// de contenu pilotee par le routeur. Le tiroir reste ferme par defaut : les
// reglages techniques ne sont jamais au premier plan.
import { ref } from "vue";
import ConfigDrawer from "./components/ConfigDrawer.vue";

const configOpen = ref(false);
</script>

<template>
  <v-app>
    <v-app-bar color="primary" flat density="comfortable">
      <v-app-bar-title>
        <span class="font-weight-bold">Pastell</span>
        <span class="text-medium-emphasis text-body-2 ml-2">bus d'orchestration, demonstration</span>
      </v-app-bar-title>

      <v-spacer />

      <v-btn variant="text" :to="{ name: 'dashboard' }" prepend-icon="mdi-play-circle-outline">
        Demonstration
      </v-btn>
      <v-btn variant="text" :to="{ name: 'status' }" prepend-icon="mdi-gauge">
        Etat du bus
      </v-btn>
      <v-btn icon="mdi-cog-outline" variant="text" aria-label="Configuration"
             @click="configOpen = true" />
    </v-app-bar>

    <ConfigDrawer v-model="configOpen" />

    <v-main>
      <router-view />
    </v-main>
  </v-app>
</template>
