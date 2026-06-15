// ============================================================
// main.js - Point d'entree de l'application
// ============================================================
//
// Met en place Vue, Vue Router et Vuetify. Le theme "abes" donne au dashboard
// une identite visuelle propre (bleu marine et orange), volontairement
// differente de la palette cyan du frontend React principal.
// ============================================================

import { createApp } from "vue";
import { createVuetify } from "vuetify";
import * as components from "vuetify/components";
import * as directives from "vuetify/directives";

import "vuetify/styles";
import "@mdi/font/css/materialdesignicons.css";
import "./assets/style.css";

import App from "./App.vue";
import router from "./router";

// Theme inspire de la charte ABES : marine profond en primaire, orange en accent.
const abesTheme = {
    dark: false,
    colors: {
        background: "#f3f5f8",
        surface: "#ffffff",
        primary: "#16284a",
        secondary: "#e0531f",
        info: "#2f6fb0",
        success: "#2e7d57",
        warning: "#e0a020",
        error: "#c5402b",
        "on-primary": "#ffffff",
        "on-secondary": "#ffffff",
    },
};

const vuetify = createVuetify({
    components,
    directives,
    theme: {
        defaultTheme: "abes",
        themes: { abes: abesTheme },
    },
    icons: {
        defaultSet: "mdi",
    },
    defaults: {
        VCard: { rounded: "lg" },
        VBtn: { rounded: "lg" },
    },
});

createApp(App).use(router).use(vuetify).mount("#app");
