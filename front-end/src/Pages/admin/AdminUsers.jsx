// src/Pages/admin/AdminUsers.jsx

/**
 * Page admin /admin/add-users.
 *
 * Refonte complete par rapport a la version Lot 0 :
 *   - Migration vers httpClient (gestion automatique du JWT et de la baseURL)
 *   - Appel du bon endpoint GET /api/admin/users (precedemment /api/hotels par erreur)
 *   - Design aligne sur AdminPastellList : palette, bordures, tableau dans une carte
 *   - Modale d'ajout pour ne pas encombrer la page
 *   - Modale de confirmation pour la suppression (pattern coherent avec l'annulation
 *     de reservation cote client)
 *   - Affichage complet : nom, prenom, email, telephone, roles avec badges
 *   - Etat actif/inactif visible
 *
 * Source de donnees :
 *   GET    /api/admin/users          (liste de tous les utilisateurs)
 *   POST   /api/admin/users?role=X   (creation, X dans {USER, EMPLOYE, ADMIN})
 *   DELETE /api/admin/users/{id}     (suppression)
 */

import { useCallback, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { toast } from "sonner";
import {
    UserPlus,
    Trash2,
    X,
    Loader2,
    AlertCircle,
    Inbox,
    Mail,
    Phone,
    User as UserIcon,
    Shield,
    CheckCircle2,
    XCircle,
} from "lucide-react";

import { httpClient } from "../../api/httpClient";

const ROLE_OPTIONS = [
    { value: "USER", label: "Utilisateur" },
    { value: "EMPLOYE", label: "Employe" },
    { value: "ADMIN", label: "Administrateur" },
];

const EMPTY_FORM = {
    firstName: "",
    lastName: "",
    email: "",
    telephone: "",
    password: "",
    role: "USER",
};

export default function AdminUsers() {
    const navigate = useNavigate();

    const [users, setUsers] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    const [addOpen, setAddOpen] = useState(false);
    const [deleteTarget, setDeleteTarget] = useState(null);
    const [actionInProgress, setActionInProgress] = useState(false);

    const fetchUsers = useCallback(async () => {
        setLoading(true);
        setError(null);
        try {
            const { data } = await httpClient.get("/api/admin/users");
            setUsers(Array.isArray(data) ? data : []);
        } catch (e) {
            console.error("Echec chargement utilisateurs:", e);
            setError("Impossible de charger les utilisateurs. Reessayez dans un instant.");
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        fetchUsers();
    }, [fetchUsers]);

    async function handleCreate(form) {
        setActionInProgress(true);
        try {
            const payload = {
                firstName: form.firstName?.trim() || null,
                lastName: form.lastName?.trim() || null,
                email: form.email.trim(),
                telephone: form.telephone?.trim() || null,
                password: form.password,
            };
            await httpClient.post(
                `/api/admin/users?role=${encodeURIComponent(form.role)}`,
                payload
            );
            toast.success("Utilisateur cree avec succes.");
            setAddOpen(false);
            await fetchUsers();
        } catch (e) {
            console.error("Echec creation utilisateur:", e);
            const backMessage = e.response?.data?.error;
            toast.error(backMessage || "Echec de la creation.");
        } finally {
            setActionInProgress(false);
        }
    }

    async function handleDeleteConfirmed() {
        if (!deleteTarget) return;
        setActionInProgress(true);
        try {
            await httpClient.delete(`/api/admin/users/${deleteTarget.id}`);
            toast.success("Utilisateur supprime.");
            setDeleteTarget(null);
            await fetchUsers();
        } catch (e) {
            console.error("Echec suppression utilisateur:", e);
            const backMessage = e.response?.data?.error;
            toast.error(backMessage || "Echec de la suppression.");
        } finally {
            setActionInProgress(false);
        }
    }

    return (
        <div className="min-h-screen bg-[#F8FAFC]">
            <div className="max-w-7xl mx-auto px-4 md:px-8 py-6 md:py-10">

                {/* En-tete */}
                <div className="mb-6 md:mb-8">
                    <div className="flex items-center justify-between gap-4 flex-wrap">
                        <div>
                            <h1 className="text-2xl md:text-3xl font-semibold text-gray-900">
                                Utilisateurs
                            </h1>
                            <p className="text-sm text-gray-600 mt-1 max-w-2xl">
                                Gestion des comptes du portail. Creer un administrateur,
                                un employe d'hotel ou un client, et supprimer les comptes
                                obsoletes.
                            </p>
                        </div>
                        <div className="flex gap-2">
                            <button
                                onClick={() => navigate("/admin")}
                                className="text-sm text-[#0369A1] hover:text-[#0EA5E9] font-medium"
                            >
                                Retour au tableau de bord
                            </button>
                        </div>
                    </div>
                </div>

                {/* Barre d'action */}
                <div className="mb-4 flex justify-end">
                    <button
                        onClick={() => setAddOpen(true)}
                        className="inline-flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium bg-[#0EA5E9] hover:bg-[#0284C7] text-white shadow-sm transition"
                    >
                        <UserPlus size={16} />
                        Ajouter un utilisateur
                    </button>
                </div>

                {/* Conteneur tableau */}
                <div className="bg-white border border-gray-200 rounded-2xl shadow-sm overflow-hidden">

                    {loading && users.length === 0 && (
                        <div className="flex items-center justify-center py-20 text-gray-500">
                            <Loader2 className="animate-spin mr-2" size={18} />
                            <span className="text-sm">Chargement des utilisateurs...</span>
                        </div>
                    )}

                    {error && (
                        <div className="flex items-center justify-center py-20 text-red-600 gap-2">
                            <AlertCircle size={18} />
                            <span className="text-sm">{error}</span>
                        </div>
                    )}

                    {!loading && !error && users.length === 0 && (
                        <div className="flex flex-col items-center justify-center py-20 text-gray-500 gap-2">
                            <Inbox size={32} className="text-gray-300" />
                            <p className="text-sm">Aucun utilisateur enregistre.</p>
                        </div>
                    )}

                    {users.length > 0 && (
                        <div className="overflow-x-auto">
                            <table className="w-full text-sm">
                                <thead className="bg-[#F8FAFC] border-b border-gray-200">
                                    <tr>
                                        <Th>ID</Th>
                                        <Th>Nom</Th>
                                        <Th>Email</Th>
                                        <Th>Telephone</Th>
                                        <Th>Roles</Th>
                                        <Th>Statut</Th>
                                        <Th className="text-right">Actions</Th>
                                    </tr>
                                </thead>
                                <tbody className="divide-y divide-gray-100">
                                    {users.map((u) => (
                                        <UserRow
                                            key={u.id}
                                            user={u}
                                            onDelete={() => setDeleteTarget(u)}
                                        />
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    )}
                </div>
            </div>

            {/* Modale d'ajout */}
            {addOpen && (
                <AddUserModal
                    onClose={() => setAddOpen(false)}
                    onSubmit={handleCreate}
                    inProgress={actionInProgress}
                />
            )}

            {/* Modale de suppression */}
            {deleteTarget && (
                <DeleteUserModal
                    user={deleteTarget}
                    onCancel={() => setDeleteTarget(null)}
                    onConfirm={handleDeleteConfirmed}
                    inProgress={actionInProgress}
                />
            )}
        </div>
    );
}

/**
 * En-tete de colonne.
 */
function Th({ children, className = "" }) {
    return (
        <th
            className={`px-4 md:px-6 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider whitespace-nowrap ${className}`}
        >
            {children}
        </th>
    );
}

function Td({ children, className = "" }) {
    return (
        <td className={`px-4 md:px-6 py-3 whitespace-nowrap text-gray-700 ${className}`}>
            {children}
        </td>
    );
}

/**
 * Une ligne du tableau utilisateurs.
 */
function UserRow({ user, onDelete }) {
    const fullName = [user.firstName, user.lastName]
        .filter(Boolean)
        .join(" ")
        .trim();

    return (
        <tr className="hover:bg-[#F8FAFC] transition">
            <Td>
                <span className="font-mono text-xs text-gray-500">#{user.id}</span>
            </Td>
            <Td>
                {fullName ? (
                    <span className="font-medium text-gray-900">{fullName}</span>
                ) : (
                    <span className="text-xs text-gray-400 italic">non renseigne</span>
                )}
            </Td>
            <Td>
                <span className="text-gray-700">{user.email}</span>
            </Td>
            <Td>
                {user.telephone ? (
                    <span className="text-gray-700">{user.telephone}</span>
                ) : (
                    <span className="text-xs text-gray-400 italic">-</span>
                )}
            </Td>
            <Td>
                <div className="flex flex-wrap gap-1">
                    {(user.roles || []).length === 0 ? (
                        <span className="text-xs text-gray-400 italic">aucun</span>
                    ) : (
                        (user.roles || []).map((r) => (
                            <RoleBadge key={r.id || r.name} roleName={r.name} />
                        ))
                    )}
                </div>
            </Td>
            <Td>
                {user.enabled ? (
                    <span className="inline-flex items-center gap-1.5 text-xs font-semibold text-emerald-700">
                        <CheckCircle2 size={14} />
                        Actif
                    </span>
                ) : (
                    <span className="inline-flex items-center gap-1.5 text-xs font-semibold text-gray-500">
                        <XCircle size={14} />
                        Inactif
                    </span>
                )}
            </Td>
            <Td className="text-right">
                <button
                    onClick={onDelete}
                    className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium bg-white border border-red-200 text-red-600 hover:bg-red-50 hover:border-red-300 transition"
                >
                    <Trash2 size={12} />
                    Supprimer
                </button>
            </Td>
        </tr>
    );
}

/**
 * Badge colore pour un role. Trois couleurs distinctes pour distinguer
 * d'un coup d'oeil les admin, les employes et les clients.
 */
function RoleBadge({ roleName }) {
    const cleanName = (roleName || "").replace(/^ROLE_/, "");
    const config = {
        ADMIN: {
            bg: "bg-amber-50",
            text: "text-amber-800",
            label: "Administrateur",
            icon: <Shield size={10} />,
        },
        EMPLOYE: {
            bg: "bg-sky-50",
            text: "text-[#0369A1]",
            label: "Employe",
            icon: <UserIcon size={10} />,
        },
        USER: {
            bg: "bg-gray-100",
            text: "text-gray-700",
            label: "Utilisateur",
            icon: <UserIcon size={10} />,
        },
    }[cleanName] || {
        bg: "bg-gray-100",
        text: "text-gray-700",
        label: cleanName || "Inconnu",
        icon: null,
    };

    return (
        <span
            className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold uppercase tracking-wider ${config.bg} ${config.text}`}
        >
            {config.icon}
            {config.label}
        </span>
    );
}

/**
 * Modale de creation d'utilisateur.
 *
 * Tous les champs de l'entite Users sont disponibles, plus le role en
 * query param. Le back exige email + password (min 6 caracteres) et un
 * role parmi USER, EMPLOYE, ADMIN.
 */
function AddUserModal({ onClose, onSubmit, inProgress }) {
    const [form, setForm] = useState(EMPTY_FORM);
    const [touched, setTouched] = useState(false);

    function handleChange(field, value) {
        setForm((prev) => ({ ...prev, [field]: value }));
    }

    function handleSubmit() {
        setTouched(true);
        if (!isValid()) return;
        onSubmit(form);
    }

    function isValid() {
        return (
            form.email.trim().length > 0 &&
            form.email.includes("@") &&
            form.password.length >= 6 &&
            ROLE_OPTIONS.some((r) => r.value === form.role)
        );
    }

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40 backdrop-blur-sm overflow-y-auto">
            <div className="bg-white rounded-2xl shadow-2xl max-w-lg w-full my-8">

                {/* En-tete modale */}
                <div className="flex items-start justify-between p-6 border-b border-gray-100">
                    <div className="flex items-center gap-3">
                        <div className="w-10 h-10 rounded-xl bg-sky-50 flex items-center justify-center">
                            <UserPlus size={20} className="text-[#0EA5E9]" />
                        </div>
                        <div>
                            <h2 className="text-base font-semibold text-gray-900">
                                Nouvel utilisateur
                            </h2>
                            <p className="text-xs text-gray-500 mt-0.5">
                                Le compte est immediatement actif apres creation.
                            </p>
                        </div>
                    </div>
                    <button
                        onClick={onClose}
                        disabled={inProgress}
                        className="text-gray-400 hover:text-gray-600 transition-colors disabled:opacity-50"
                    >
                        <X size={20} />
                    </button>
                </div>

                {/* Formulaire */}
                <div className="p-6 space-y-4">
                    <div className="grid grid-cols-2 gap-3">
                        <Field
                            label="Prenom"
                            icon={<UserIcon size={14} />}
                            value={form.firstName}
                            onChange={(v) => handleChange("firstName", v)}
                            placeholder="Optionnel"
                        />
                        <Field
                            label="Nom"
                            icon={<UserIcon size={14} />}
                            value={form.lastName}
                            onChange={(v) => handleChange("lastName", v)}
                            placeholder="Optionnel"
                        />
                    </div>

                    <Field
                        label="Email"
                        required
                        icon={<Mail size={14} />}
                        type="email"
                        value={form.email}
                        onChange={(v) => handleChange("email", v)}
                        placeholder="utilisateur@example.fr"
                        error={
                            touched && (!form.email.trim() || !form.email.includes("@"))
                                ? "Email invalide"
                                : null
                        }
                    />

                    <Field
                        label="Telephone"
                        icon={<Phone size={14} />}
                        value={form.telephone}
                        onChange={(v) => handleChange("telephone", v)}
                        placeholder="Optionnel"
                    />

                    <Field
                        label="Mot de passe"
                        required
                        icon={<Shield size={14} />}
                        type="password"
                        value={form.password}
                        onChange={(v) => handleChange("password", v)}
                        placeholder="6 caracteres minimum"
                        error={
                            touched && form.password.length < 6
                                ? "Au moins 6 caracteres requis"
                                : null
                        }
                    />

                    <div>
                        <label className="block text-xs font-semibold text-gray-500 uppercase tracking-wider mb-1.5">
                            Role
                        </label>
                        <select
                            value={form.role}
                            onChange={(e) => handleChange("role", e.target.value)}
                            className="w-full px-3 py-2 rounded-lg border border-gray-200 text-sm text-gray-800 bg-white focus:outline-none focus:ring-2 focus:ring-[#0EA5E9] focus:border-[#0EA5E9] transition"
                        >
                            {ROLE_OPTIONS.map((r) => (
                                <option key={r.value} value={r.value}>
                                    {r.label}
                                </option>
                            ))}
                        </select>
                    </div>
                </div>

                {/* Actions */}
                <div className="flex flex-col-reverse sm:flex-row gap-2 p-6 border-t border-gray-100">
                    <button
                        onClick={onClose}
                        disabled={inProgress}
                        className="flex-1 px-4 py-2.5 rounded-xl border border-gray-200 text-gray-700 text-sm font-medium hover:bg-gray-50 disabled:opacity-50 transition-colors"
                    >
                        Annuler
                    </button>
                    <button
                        onClick={handleSubmit}
                        disabled={inProgress}
                        className="flex-1 inline-flex items-center justify-center gap-2 px-4 py-2.5 rounded-xl bg-[#0EA5E9] hover:bg-[#0284C7] text-white text-sm font-medium disabled:opacity-50 transition-colors"
                    >
                        {inProgress ? (
                            <>
                                <Loader2 size={14} className="animate-spin" />
                                Creation en cours...
                            </>
                        ) : (
                            <>
                                <UserPlus size={14} />
                                Creer l'utilisateur
                            </>
                        )}
                    </button>
                </div>
            </div>
        </div>
    );
}

/**
 * Champ de formulaire reutilisable dans la modale.
 */
function Field({ label, required, icon, type = "text", value, onChange, placeholder, error }) {
    return (
        <div>
            <label className="flex items-center gap-1.5 text-xs font-semibold text-gray-500 uppercase tracking-wider mb-1.5">
                {icon}
                {label}
                {required && <span className="text-red-500">*</span>}
            </label>
            <input
                type={type}
                value={value}
                onChange={(e) => onChange(e.target.value)}
                placeholder={placeholder}
                className={
                    error
                        ? "w-full px-3 py-2 rounded-lg border border-red-300 text-sm text-gray-800 bg-white focus:outline-none focus:ring-2 focus:ring-red-300 transition"
                        : "w-full px-3 py-2 rounded-lg border border-gray-200 text-sm text-gray-800 bg-white focus:outline-none focus:ring-2 focus:ring-[#0EA5E9] focus:border-[#0EA5E9] transition"
                }
            />
            {error && (
                <p className="text-xs text-red-600 mt-1">{error}</p>
            )}
        </div>
    );
}

/**
 * Modale de confirmation de suppression.
 */
function DeleteUserModal({ user, onCancel, onConfirm, inProgress }) {
    const displayName =
        [user.firstName, user.lastName].filter(Boolean).join(" ").trim() || user.email;

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40 backdrop-blur-sm">
            <div className="bg-white rounded-2xl shadow-2xl max-w-md w-full p-6">
                <div className="flex items-start gap-3 mb-4">
                    <div className="w-10 h-10 rounded-xl bg-red-50 flex items-center justify-center flex-shrink-0">
                        <Trash2 size={20} className="text-red-600" />
                    </div>
                    <div className="flex-1">
                        <h2 className="text-base font-semibold text-gray-900">
                            Supprimer cet utilisateur ?
                        </h2>
                        <p className="text-sm text-gray-600 mt-1">
                            Le compte de{" "}
                            <span className="font-medium text-gray-900">{displayName}</span>{" "}
                            sera supprime definitivement. Cette action est irreversible.
                        </p>
                    </div>
                </div>

                <div className="flex flex-col-reverse sm:flex-row gap-2 mt-6">
                    <button
                        onClick={onCancel}
                        disabled={inProgress}
                        className="flex-1 px-4 py-2.5 rounded-xl border border-gray-200 text-gray-700 text-sm font-medium hover:bg-gray-50 disabled:opacity-50 transition-colors"
                    >
                        Annuler
                    </button>
                    <button
                        onClick={onConfirm}
                        disabled={inProgress}
                        className="flex-1 inline-flex items-center justify-center gap-2 px-4 py-2.5 rounded-xl bg-red-600 text-white text-sm font-medium hover:bg-red-700 disabled:opacity-50 transition-colors"
                    >
                        {inProgress ? (
                            <>
                                <Loader2 size={14} className="animate-spin" />
                                Suppression en cours...
                            </>
                        ) : (
                            "Confirmer la suppression"
                        )}
                    </button>
                </div>
            </div>
        </div>
    );
}