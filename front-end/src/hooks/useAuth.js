/**
 * useAuth.js
 * Hook React qui expose le contexte d'authentification.
 *
 * Pourquoi un fichier dedie : permet de centraliser la verification que le
 * composant qui appelle est bien sous un AuthProvider. Lance une erreur
 * explicite sinon, plutot que des bugs silencieux.
 *
 * Usage :
 *   const { user, isAuthenticated, isAdmin, login, logout } = useAuth();
 */

import { useContext } from 'react';
import { AuthContext } from '../context/AuthContext';

export function useAuth() {
    const context = useContext(AuthContext);
    if (context === null) {
        throw new Error(
            'useAuth doit etre utilise dans un composant enfant de <AuthProvider>'
        );
    }
    return context;
}