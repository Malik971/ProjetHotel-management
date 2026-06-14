// ============================================================
// router - Deux vues : la demonstration et l'etat du bus
// ============================================================
//
// DashboardView : le parcours guide (piece maitresse, vue par defaut).
// StatusView : les compteurs de synchronisation et la sante du connecteur.
//
// Les vues sont chargees en import direct (pas de lazy loading) : l'application
// est petite, deux vues, le gain de decoupage serait negligeable.
// ============================================================

import { createRouter, createWebHistory } from "vue-router";
import DashboardView from "../views/DashboardView.vue";
import StatusView from "../views/StatusView.vue";

const routes = [
    {
        path: "/",
        name: "dashboard",
        component: DashboardView,
    },
    {
        path: "/status",
        name: "status",
        component: StatusView,
    },
    {
        // Toute route inconnue ramene a la demonstration.
        path: "/:pathMatch(.*)*",
        redirect: "/",
    },
];

export default createRouter({
    history: createWebHistory(),
    routes,
});
