import js from "@eslint/js";
import pluginVue from "eslint-plugin-vue";
import globals from "globals";

// Configuration ESLint a plat (flat config). On combine les regles JS de base et
// le preset Vue 3 "essential" : il cible les vraies erreurs (correction) sans
// imposer de regles de mise en forme arbitraires (nombre d'attributs par ligne,
// sauts de ligne), qui ne sont pas du ressort d'ESLint ici.
export default [
    {
        ignores: ["dist/**", "node_modules/**"],
    },
    js.configs.recommended,
    ...pluginVue.configs["flat/essential"],
    {
        languageOptions: {
            ecmaVersion: "latest",
            sourceType: "module",
            globals: {
                ...globals.browser,
            },
        },
        rules: {
            "vue/multi-word-component-names": "off",
        },
    },
];
