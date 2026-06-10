// src/Pages/admin/AdminChambres.jsx
import { useState, useEffect } from "react";
import { Link } from "react-router-dom";
import { toast } from "sonner";
import {
    ArrowLeft, Plus, Pencil, Search, X, Bed, Users, Maximize2,
    Image as ImageIcon, Building2
} from "lucide-react";
import {
    getAllChambres, creerChambre, updateChambre, deleteChambre,
} from "../../services/chambreService";
import { getAllHotels } from "../../services/hotelSearchService";
import { useAuth } from "../../hooks/useAuth";
import EmployeModeBanner from "../../components/admin/EmployeModeBanner";
import DeleteButton from "../../components/admin/DeleteButton";

/**
 * Page admin de gestion des chambres.
 * Refonte complete avec design coherent avec /admin (palette #0EA5E9).
 *
 * Fonctionnalites :
 *   - Liste de toutes les chambres avec filtres (recherche + hotel)
 *   - Creation, modification, suppression
 *   - Gestion des photos via URL (ajout/suppression dynamique)
 *   - Indication visuelle nombre d'images par chambre
 */
export default function AdminChambres() {
    // Bouton supprimer reserve aux ADMIN. L'employe peut creer/modifier mais
    // pas supprimer (double barriere avec le DELETE ADMIN-only du backend).
    const { isAdmin } = useAuth();
    const [chambres, setChambres] = useState([]);
    const [hotels, setHotels] = useState([]);
    const [loading, setLoading] = useState(true);
    const [search, setSearch] = useState("");
    const [filterHotelId, setFilterHotelId] = useState("");
    const [editing, setEditing] = useState(null); // null = mode "creation", sinon objet chambre

    const loadAll = async () => {
        try {
            const [c, h] = await Promise.all([getAllChambres(), getAllHotels()]);
            setChambres(c || []);
            setHotels(h || []);
        } catch {
            toast.error("Impossible de charger les donnees");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => { loadAll(); }, []);

    const handleSubmit = async (chambre) => {
        try {
            if (editing && editing.id) {
                await updateChambre(editing.id, chambre);
                toast.success("Chambre modifiee");
            } else {
                await creerChambre(chambre);
                toast.success("Chambre creee");
            }
            setEditing(null);
            await loadAll();
        } catch (err) {
            const msg = err.response?.data?.message || "Erreur lors de l'enregistrement";
            toast.error(msg);
        }
    };

    const handleDelete = async (chambre) => {
        if (!window.confirm(`Supprimer "${chambre.nom}" ? Cette action est irreversible.`)) return;
        try {
            await deleteChambre(chambre.id);
            toast.success("Chambre supprimee");
            await loadAll();
        } catch {
            toast.error("Suppression impossible");
        }
    };

    // Filtrage
    const filtered = chambres.filter((c) => {
        if (filterHotelId && String(c.hotelId) !== String(filterHotelId)) return false;
        if (search && !c.nom?.toLowerCase().includes(search.toLowerCase())) return false;
        return true;
    });

    return (
        <div className="min-h-screen bg-[#F8FAFC] py-6 md:py-10">
            <div className="max-w-7xl mx-auto px-4 md:px-8">

                {/* Retour */}
                <Link to="/admin" className="inline-flex items-center gap-2 text-gray-500 hover:text-[#0EA5E9] text-sm font-medium transition-colors mb-4">
                    <ArrowLeft size={16} />
                    Retour au tableau de bord
                </Link>

                {/* Header */}
                <div className="flex flex-wrap items-end justify-between gap-3 pb-4 mb-6 border-b border-gray-200">
                    <div>
                        <p className="text-xs font-semibold text-[#0EA5E9] uppercase tracking-wider mb-1">
                            Espace administrateur
                        </p>
                        <h1 className="text-xl md:text-2xl font-semibold text-gray-900">
                            Gestion des chambres
                        </h1>
                        <p className="text-sm text-gray-500 mt-1">
                            {chambres.length} chambre{chambres.length > 1 ? "s" : ""} au total
                        </p>
                    </div>
                    <button
                        onClick={() => setEditing({})}
                        className="flex items-center gap-2 bg-[#0EA5E9] hover:bg-[#0284C7] text-white text-sm font-semibold px-4 py-2.5 rounded-xl transition-colors shadow-sm"
                    >
                        <Plus size={16} />
                        Nouvelle chambre
                    </button>
                </div>

                <EmployeModeBanner />

                {/* Filtres */}
                <div className="bg-white border border-gray-200 rounded-2xl p-4 shadow-sm mb-4 flex flex-wrap gap-3">
                    <div className="flex items-center gap-2 flex-1 min-w-[200px] border border-gray-200 rounded-xl px-3 py-2 hover:border-[#0EA5E9] transition-colors">
                        <Search size={14} className="text-[#0EA5E9]" />
                        <input
                            type="text"
                            placeholder="Rechercher par nom..."
                            value={search}
                            onChange={(e) => setSearch(e.target.value)}
                            className="flex-1 text-sm outline-none bg-transparent"
                        />
                    </div>
                    <select
                        value={filterHotelId}
                        onChange={(e) => setFilterHotelId(e.target.value)}
                        className="border border-gray-200 rounded-xl px-3 py-2 text-sm outline-none hover:border-[#0EA5E9] transition-colors cursor-pointer"
                    >
                        <option value="">Tous les hotels</option>
                        {hotels.map((h) => (
                            <option key={h.id} value={h.id}>{h.nom}</option>
                        ))}
                    </select>
                </div>

                {/* Formulaire d'edition */}
                {editing !== null && (
                    <ChambreFormCard
                        chambre={editing}
                        hotels={hotels}
                        onSubmit={handleSubmit}
                        onCancel={() => setEditing(null)}
                    />
                )}

                {/* Tableau */}
                {loading ? (
                    <div className="text-center py-10">
                        <div className="w-10 h-10 border-2 border-[#BAE6FD] border-t-[#0EA5E9] rounded-full animate-spin mx-auto" />
                    </div>
                ) : filtered.length === 0 ? (
                    <div className="text-center py-12 bg-white border border-gray-200 rounded-2xl">
                        <Bed size={32} className="text-gray-300 mx-auto mb-3" />
                        <p className="text-sm text-gray-500">Aucune chambre trouvee</p>
                    </div>
                ) : (
                    <div className="bg-white border border-gray-200 rounded-2xl overflow-hidden shadow-sm">
                        <div className="overflow-x-auto">
                            <table className="w-full">
                                <thead className="bg-[#F8FAFC] border-b border-gray-200">
                                    <tr>
                                        <Th>Chambre</Th>
                                        <Th>Hotel</Th>
                                        <Th>Prix / nuit</Th>
                                        <Th>Capacite</Th>
                                        <Th>Superficie</Th>
                                        <Th>Photos</Th>
                                        <Th>Actions</Th>
                                    </tr>
                                </thead>
                                <tbody className="divide-y divide-gray-100">
                                    {filtered.map((c) => (
                                        <tr key={c.id} className="hover:bg-[#F8FAFC] transition-colors">
                                            <td className="px-4 py-3">
                                                <p className="text-sm font-medium text-gray-900">{c.nom}</p>
                                                <p className="text-xs text-gray-400">{c.typeLit}</p>
                                            </td>
                                            <td className="px-4 py-3 text-sm text-gray-700">
                                                {c.hotelNom || hotels.find((h) => h.id === c.hotelId)?.nom || "—"}
                                            </td>
                                            <td className="px-4 py-3 text-sm font-semibold text-[#0369A1]">
                                                {c.prixParNuit}€
                                            </td>
                                            <td className="px-4 py-3 text-sm text-gray-600">
                                                <span className="inline-flex items-center gap-1">
                                                    <Users size={12} className="text-gray-400" />
                                                    {c.capacity}
                                                </span>
                                            </td>
                                            <td className="px-4 py-3 text-sm text-gray-600">
                                                <span className="inline-flex items-center gap-1">
                                                    <Maximize2 size={12} className="text-gray-400" />
                                                    {c.superficie} m²
                                                </span>
                                            </td>
                                            <td className="px-4 py-3">
                                                <span className={`inline-flex items-center gap-1 text-xs px-2 py-1 rounded-full ${
                                                    c.imageUrls && c.imageUrls.length > 0
                                                        ? "bg-emerald-50 text-emerald-700"
                                                        : "bg-amber-50 text-amber-700"
                                                }`}>
                                                    <ImageIcon size={11} />
                                                    {c.imageUrls?.length || 0}
                                                </span>
                                            </td>
                                            <td className="px-4 py-3">
                                                <div className="flex gap-1">
                                                    <button
                                                        onClick={() => setEditing(c)}
                                                        className="p-1.5 text-[#0EA5E9] hover:bg-sky-50 rounded-lg transition-colors"
                                                        title="Modifier"
                                                    >
                                                        <Pencil size={14} />
                                                    </button>
                                                    <DeleteButton
                                                        variant="icon"
                                                        canDelete={isAdmin}
                                                        onDelete={() => handleDelete(c)}
                                                    />
                                                </div>
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
}

// ─── Sous-composant : entete de colonne ───────────────────────────────────

function Th({ children }) {
    return (
        <th className="px-4 py-3 text-left text-[10px] font-semibold text-gray-500 uppercase tracking-wider">
            {children}
        </th>
    );
}

// ─── Sous-composant : formulaire d'edition/creation ───────────────────────

function ChambreFormCard({ chambre, hotels, onSubmit, onCancel }) {
    const isEdit = chambre && chambre.id;

    const [form, setForm] = useState({
        nom: chambre?.nom || "",
        prixParNuit: chambre?.prixParNuit || "",
        capacity: chambre?.capacity || 1,
        superficie: chambre?.superficie || "",
        typeLit: chambre?.typeLit || "",
        description: chambre?.description || "",
        equipment: chambre?.equipment || [],
        imageUrls: chambre?.imageUrls || [],
        hotelId: chambre?.hotelId || "",
    });

    const [equipementInput, setEquipementInput] = useState("");
    const [imageInput, setImageInput] = useState("");

    const update = (key, value) => setForm((p) => ({ ...p, [key]: value }));

    const addEquipement = () => {
        if (equipementInput.trim()) {
            update("equipment", [...form.equipment, equipementInput.trim()]);
            setEquipementInput("");
        }
    };
    const removeEquipement = (i) => update("equipment", form.equipment.filter((_, idx) => idx !== i));

    const addImage = () => {
        if (imageInput.trim()) {
            update("imageUrls", [...form.imageUrls, imageInput.trim()]);
            setImageInput("");
        }
    };
    const removeImage = (i) => update("imageUrls", form.imageUrls.filter((_, idx) => idx !== i));

    const handleSubmit = (e) => {
        e.preventDefault();
        if (!form.hotelId) {
            toast.error("Selectionnez un hotel");
            return;
        }
        onSubmit({
            ...form,
            prixParNuit: parseFloat(form.prixParNuit),
            capacity: parseInt(form.capacity, 10),
            superficie: parseInt(form.superficie, 10),
            hotelId: parseInt(form.hotelId, 10),
        });
    };

    return (
        <div className="bg-white border border-gray-200 rounded-2xl p-5 shadow-sm mb-4">
            <div className="flex items-center justify-between mb-4 pb-3 border-b border-gray-100">
                <h2 className="text-base font-semibold text-gray-900">
                    {isEdit ? "Modifier la chambre" : "Nouvelle chambre"}
                </h2>
                <button onClick={onCancel} className="p-1.5 text-gray-400 hover:bg-gray-50 rounded-lg">
                    <X size={16} />
                </button>
            </div>

            <form onSubmit={handleSubmit} className="space-y-4">

                {/* Selection hotel */}
                <div>
                    <Label icon={<Building2 size={12} />}>Hotel</Label>
                    <select
                        value={form.hotelId}
                        onChange={(e) => update("hotelId", e.target.value)}
                        required
                        className="w-full border border-gray-200 rounded-xl px-3 py-2.5 text-sm outline-none focus:border-[#0EA5E9] transition-colors"
                    >
                        <option value="">Choisir un hotel...</option>
                        {hotels.map((h) => (
                            <option key={h.id} value={h.id}>{h.nom} ({h.ville})</option>
                        ))}
                    </select>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                    <Field label="Nom de la chambre">
                        <input type="text" value={form.nom} onChange={(e) => update("nom", e.target.value)} required
                            className="w-full border border-gray-200 rounded-xl px-3 py-2.5 text-sm outline-none focus:border-[#0EA5E9]" />
                    </Field>
                    <Field label="Type de lit">
                        <input type="text" value={form.typeLit} onChange={(e) => update("typeLit", e.target.value)} required placeholder="Ex: Lit Queen"
                            className="w-full border border-gray-200 rounded-xl px-3 py-2.5 text-sm outline-none focus:border-[#0EA5E9]" />
                    </Field>
                </div>

                <div className="grid grid-cols-3 gap-3">
                    <Field label="Prix / nuit (€)">
                        <input type="number" step="0.01" value={form.prixParNuit} onChange={(e) => update("prixParNuit", e.target.value)} required
                            className="w-full border border-gray-200 rounded-xl px-3 py-2.5 text-sm outline-none focus:border-[#0EA5E9]" />
                    </Field>
                    <Field label="Capacite">
                        <input type="number" min="1" value={form.capacity} onChange={(e) => update("capacity", e.target.value)} required
                            className="w-full border border-gray-200 rounded-xl px-3 py-2.5 text-sm outline-none focus:border-[#0EA5E9]" />
                    </Field>
                    <Field label="Superficie (m²)">
                        <input type="number" value={form.superficie} onChange={(e) => update("superficie", e.target.value)} required
                            className="w-full border border-gray-200 rounded-xl px-3 py-2.5 text-sm outline-none focus:border-[#0EA5E9]" />
                    </Field>
                </div>

                <Field label="Description">
                    <textarea value={form.description} onChange={(e) => update("description", e.target.value)} rows="3"
                        className="w-full border border-gray-200 rounded-xl px-3 py-2.5 text-sm outline-none focus:border-[#0EA5E9] resize-none" />
                </Field>

                {/* Equipements */}
                <Field label={`Equipements (${form.equipment.length})`}>
                    <div className="flex gap-2 mb-2">
                        <input type="text" value={equipementInput} onChange={(e) => setEquipementInput(e.target.value)}
                            onKeyDown={(e) => e.key === "Enter" && (e.preventDefault(), addEquipement())}
                            placeholder="Ex: Climatisation"
                            className="flex-1 border border-gray-200 rounded-xl px-3 py-2 text-sm outline-none focus:border-[#0EA5E9]" />
                        <button type="button" onClick={addEquipement}
                            className="bg-[#0EA5E9] hover:bg-[#0284C7] text-white text-sm font-medium px-3 py-2 rounded-xl">
                            Ajouter
                        </button>
                    </div>
                    {form.equipment.length > 0 && (
                        <div className="flex flex-wrap gap-1.5">
                            {form.equipment.map((eq, idx) => (
                                <span key={idx} className="inline-flex items-center gap-1.5 bg-[#E0F2FE] text-[#0369A1] text-xs px-2.5 py-1 rounded-full">
                                    {eq}
                                    <button type="button" onClick={() => removeEquipement(idx)} className="hover:text-red-600">
                                        <X size={11} />
                                    </button>
                                </span>
                            ))}
                        </div>
                    )}
                </Field>

                {/* Images */}
                <Field label={`Photos (${form.imageUrls.length})`}>
                    <div className="flex gap-2 mb-2">
                        <input type="url" value={imageInput} onChange={(e) => setImageInput(e.target.value)}
                            onKeyDown={(e) => e.key === "Enter" && (e.preventDefault(), addImage())}
                            placeholder="https://exemple.com/photo.jpg"
                            className="flex-1 border border-gray-200 rounded-xl px-3 py-2 text-sm outline-none focus:border-[#0EA5E9]" />
                        <button type="button" onClick={addImage}
                            className="bg-[#0EA5E9] hover:bg-[#0284C7] text-white text-sm font-medium px-3 py-2 rounded-xl">
                            Ajouter
                        </button>
                    </div>
                    {form.imageUrls.length > 0 && (
                        <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
                            {form.imageUrls.map((url, idx) => (
                                <div key={idx} className="relative group rounded-xl overflow-hidden border border-gray-200">
                                    <img src={url} alt={`Photo ${idx + 1}`} className="w-full h-24 object-cover"
                                        onError={(e) => { e.target.src = "https://placehold.co/200x100/CBD5E1/64748B?text=Image"; }} />
                                    <button type="button" onClick={() => removeImage(idx)}
                                        className="absolute top-1 right-1 bg-red-500 text-white rounded-full p-1 opacity-0 group-hover:opacity-100 transition-opacity">
                                        <X size={11} />
                                    </button>
                                </div>
                            ))}
                        </div>
                    )}
                </Field>

                {/* Boutons */}
                <div className="flex gap-2 pt-2">
                    <button type="submit"
                        className="flex-1 bg-[#0EA5E9] hover:bg-[#0284C7] text-white font-semibold py-2.5 rounded-xl text-sm transition-colors">
                        {isEdit ? "Enregistrer les modifications" : "Creer la chambre"}
                    </button>
                    <button type="button" onClick={onCancel}
                        className="px-5 py-2.5 border border-gray-200 text-gray-700 font-medium rounded-xl text-sm hover:bg-gray-50">
                        Annuler
                    </button>
                </div>
            </form>
        </div>
    );
}

function Label({ icon, children }) {
    return (
        <p className="text-[10px] font-semibold text-gray-500 uppercase tracking-wider mb-1.5 flex items-center gap-1.5">
            {icon}
            {children}
        </p>
    );
}

function Field({ label, children }) {
    return (
        <div>
            <Label>{label}</Label>
            {children}
        </div>
    );
}