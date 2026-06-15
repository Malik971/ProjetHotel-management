// ============================================================
// useConfig - Reglages reactifs persistes en localStorage
// ============================================================
//
// Choix Vue : on expose un etat reactif unique (singleton au niveau module)
// plutot que d'instancier un nouvel objet a chaque appel du composable. Tous
// les composants partagent ainsi la meme configuration, et une modification
// dans le ConfigDrawer se repercute partout immediatement.
//
// Pas de Pinia : un reactive() partage suffit largement pour trois champs.
// ============================================================

import { reactive, watch } from "vue";
import { DEFAULT_CONFIG, STORAGE_KEY } from "../config";

/**
 * Lit la configuration persistee, fusionnee avec les valeurs par defaut.
 * Toute cle absente du stockage retombe sur le defaut.
 *
 * @returns {object} configuration initiale
 */
function loadInitialConfig() {
    try {
        const stored = localStorage.getItem(STORAGE_KEY);
        if (!stored) {
            return { ...DEFAULT_CONFIG };
        }
        return { ...DEFAULT_CONFIG, ...JSON.parse(stored) };
    } catch {
        // localStorage indisponible ou JSON corrompu : on repart des defauts.
        return { ...DEFAULT_CONFIG };
    }
}

// Etat reactif partage par toute l'application (singleton de module).
const config = reactive(loadInitialConfig());

// Persistance automatique a chaque modification d'un champ.
watch(
    config,
    (current) => {
        try {
            localStorage.setItem(STORAGE_KEY, JSON.stringify(current));
        } catch {
            // Stockage indisponible (navigation privee stricte) : on ignore,
            // l'application continue de fonctionner avec l'etat en memoire.
        }
    },
    { deep: true }
);

/**
 * Restaure les reglages par defaut et efface le stockage.
 */
function resetConfig() {
    Object.assign(config, DEFAULT_CONFIG);
}

/**
 * Composable d'acces a la configuration partagee.
 *
 * @returns {{config: object, resetConfig: function}}
 */
export function useConfig() {
    return { config, resetConfig };
}
