import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";

// Configuration Vite minimale : un seul plugin Vue. Vuetify est charge en
// import complet dans main.js (styles precompiles), ce qui evite d'ajouter un
// plugin de build supplementaire et garde la chaine de build simple.
export default defineConfig({
    plugins: [vue()],
    server: {
        port: 5174,
    },
});
