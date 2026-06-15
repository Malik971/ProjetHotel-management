// ============================================================
// usePolling - Compte a rebours et declenchement periodique
// ============================================================
//
// Reproduit cote interface ce que fait le scheduler de Sejour cote serveur :
// un rafraichissement toutes les N secondes. Le compte a rebours visible aide
// le visiteur a comprendre que la synchronisation est periodique, pas instantanee.
//
// Choix Vue : on nettoie l'intervalle dans onUnmounted pour ne pas laisser de
// timer actif si le composant qui utilise ce composable est detruit.
// ============================================================

import { ref, onUnmounted } from "vue";

/**
 * @param {number} intervalSeconds periode entre deux rafraichissements
 * @param {function} onTick callback asynchrone execute a chaque fin de cycle
 * @returns objet reactif de pilotage du polling
 */
export function usePolling(intervalSeconds, onTick) {
    const enabled = ref(false);
    const countdown = ref(intervalSeconds);
    let timer = null;

    function tick() {
        countdown.value -= 1;
        if (countdown.value <= 0) {
            countdown.value = intervalSeconds;
            // On ne bloque pas le timer sur la promesse : le cycle suivant
            // continue meme si un rafraichissement est lent.
            Promise.resolve(onTick()).catch(() => {
                // Les erreurs de rafraichissement sont gerees par l'appelant via
                // son propre etat ; ici on evite juste de casser le timer.
            });
        }
    }

    function start() {
        if (enabled.value) {
            return;
        }
        enabled.value = true;
        countdown.value = intervalSeconds;
        timer = setInterval(tick, 1000);
    }

    function stop() {
        enabled.value = false;
        countdown.value = intervalSeconds;
        if (timer) {
            clearInterval(timer);
            timer = null;
        }
    }

    function toggle() {
        if (enabled.value) {
            stop();
        } else {
            start();
        }
    }

    onUnmounted(stop);

    return { enabled, countdown, start, stop, toggle };
}
