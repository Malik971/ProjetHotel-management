// src/Pages/admin/AdminHotel.jsx
import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { toast } from "sonner";
import {
    ArrowLeft, Plus, Pencil, Trash2, Search, X, Building2, MapPin, Star,
    Image as ImageIcon,
} from "lucide-react";
import { httpClient } from "../../api/httpClient";

const EMPTY_FORM = { nom: "", adresse: "", ville: "", description: "", noteMoyenne: 0, prixMoyenNuit: 0, categorie: 3, latitude: null, longitude: null };

/**
 * Page admin de gestion des hotels.
 * Refonte complete avec design coherent (palette #0EA5E9).
 *
 * Fonctionnalites :
 *   - Liste de tous les hotels avec recherche
 *   - Creation et modification (avec photo via FormData)
 *   - Suppression avec confirmation
 *   - Plus d'URL hardcode : utilise VITE_API_URL
 */
export default function AdminHotel() {
    const [hotels, setHotels] = useState([]);
    const [loading, setLoading] = useState(true);
    const [search, setSearch] = useState("");
    const [editing, setEditing] = useState(null); // null = ferme, {} = creation, {...hotel} = edition

    const loadHotels = async () => {
        try {
            const { data } = await httpClient.get("/api/hotels");
            setHotels(data || []);
        } catch {
            toast.error("Impossible de charger les hotels");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => { loadHotels(); }, []);

    const handleSubmit = async (form) => {
        const isEdit = !!form.id;
        try {
            const payload = {
                ...form,
                noteMoyenne: form.noteMoyenne ?? 0,
                prixMoyenNuit: form.prixMoyenNuit ?? 0,
                categorie: form.categorie ?? 3,
            };
            if (isEdit) {
                await httpClient.put(`/api/hotels/${form.id}`, payload);
            } else {
                await httpClient.post(`/api/hotels`, payload);
            }
            toast.success(isEdit ? "Hotel modifie" : "Hotel cree");
            setEditing(null);
            await loadHotels();
        } catch {
            toast.error("Erreur lors de l'enregistrement");
        }
    };

    const handleDelete = async (hotel) => {
        if (!window.confirm(`Supprimer "${hotel.nom}" ? Les chambres associees seront aussi supprimees.`)) return;
        try {
            await httpClient.delete(`/api/hotels/${hotel.id}`);
            toast.success("Hotel supprime");
            await loadHotels();
        } catch {
            toast.error("Suppression impossible");
        }
    };

    const filtered = hotels.filter((h) =>
        !search || h.nom?.toLowerCase().includes(search.toLowerCase())
        || h.ville?.toLowerCase().includes(search.toLowerCase())
    );

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
                            Gestion des hotels
                        </h1>
                        <p className="text-sm text-gray-500 mt-1">
                            {hotels.length} hotel{hotels.length > 1 ? "s" : ""} au total
                        </p>
                    </div>
                    <button
                        onClick={() => setEditing({})}
                        className="flex items-center gap-2 bg-[#0EA5E9] hover:bg-[#0284C7] text-white text-sm font-semibold px-4 py-2.5 rounded-xl transition-colors shadow-sm"
                    >
                        <Plus size={16} />
                        Nouvel hotel
                    </button>
                </div>

                {/* Recherche */}
                <div className="bg-white border border-gray-200 rounded-2xl p-4 shadow-sm mb-4">
                    <div className="flex items-center gap-2 border border-gray-200 rounded-xl px-3 py-2 hover:border-[#0EA5E9] transition-colors">
                        <Search size={14} className="text-[#0EA5E9]" />
                        <input
                            type="text"
                            placeholder="Rechercher par nom ou ville..."
                            value={search}
                            onChange={(e) => setSearch(e.target.value)}
                            className="flex-1 text-sm outline-none bg-transparent"
                        />
                    </div>
                </div>

                {/* Formulaire d'edition */}
                {editing !== null && (
                    <HotelFormCard
                        hotel={editing}
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
                        <Building2 size={32} className="text-gray-300 mx-auto mb-3" />
                        <p className="text-sm text-gray-500">Aucun hotel trouve</p>
                    </div>
                ) : (
                    <div className="bg-white border border-gray-200 rounded-2xl overflow-hidden shadow-sm">
                        <div className="overflow-x-auto">
                            <table className="w-full">
                                <thead className="bg-[#F8FAFC] border-b border-gray-200">
                                    <tr>
                                        <Th>Hotel</Th>
                                        <Th>Ville</Th>
                                        <Th>Categorie</Th>
                                        <Th>Note</Th>
                                        <Th>Prix moy.</Th>
                                        <Th>Photo</Th>
                                        <Th>Actions</Th>
                                    </tr>
                                </thead>
                                <tbody className="divide-y divide-gray-100">
                                    {filtered.map((h) => (
                                        <tr key={h.id} className="hover:bg-[#F8FAFC] transition-colors">
                                            <td className="px-4 py-3">
                                                <p className="text-sm font-medium text-gray-900">{h.nom}</p>
                                                {h.adresse && <p className="text-xs text-gray-400">{h.adresse}</p>}
                                            </td>
                                            <td className="px-4 py-3 text-sm text-gray-700">
                                                <span className="inline-flex items-center gap-1">
                                                    <MapPin size={12} className="text-gray-400" />
                                                    {h.ville || "-"}
                                                </span>
                                            </td>
                                            <td className="px-4 py-3 text-sm text-gray-600">
                                                {h.categorie ? `${h.categorie} étoiles` : "-"}
                                            </td>
                                            <td className="px-4 py-3 text-sm">
                                                <span className="inline-flex items-center gap-1">
                                                    <Star size={12} className="text-[#F59E0B] fill-[#F59E0B]" />
                                                    <span className="font-semibold text-gray-700">{h.noteMoyenne || "-"}</span>
                                                </span>
                                            </td>
                                            <td className="px-4 py-3 text-sm font-semibold text-[#0369A1]">
                                                {h.prixMoyenNuit ? `${h.prixMoyenNuit}€` : "-"}
                                            </td>
                                            <td className="px-4 py-3">
                                                <span className={`inline-flex items-center gap-1 text-xs px-2 py-1 rounded-full ${
                                                    h.imageUrl
                                                        ? "bg-emerald-50 text-emerald-700"
                                                        : "bg-amber-50 text-amber-700"
                                                }`}>
                                                    <ImageIcon size={11} />
                                                    {h.imageUrl ? "OK" : "Aucune"}
                                                </span>
                                            </td>
                                            <td className="px-4 py-3">
                                                <div className="flex gap-1">
                                                    <button onClick={() => setEditing(h)}
                                                        className="p-1.5 text-[#0EA5E9] hover:bg-sky-50 rounded-lg transition-colors" title="Modifier">
                                                        <Pencil size={14} />
                                                    </button>
                                                    <button onClick={() => handleDelete(h)}
                                                        className="p-1.5 text-red-500 hover:bg-red-50 rounded-lg transition-colors" title="Supprimer">
                                                        <Trash2 size={14} />
                                                    </button>
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

function Th({ children }) {
    return (
        <th className="px-4 py-3 text-left text-[10px] font-semibold text-gray-500 uppercase tracking-wider">
            {children}
        </th>
    );
}

// ─── Formulaire d'edition/creation ────────────────────────────────────────

function HotelFormCard({ hotel, onSubmit, onCancel }) {
    const isEdit = !!hotel.id;
    const [form, setForm] = useState({
        ...EMPTY_FORM,
        imageUrls: [],
        ...hotel,
    });

    const update = (key, value) => setForm((p) => ({ ...p, [key]: value }));
    const [imageInput, setImageInput] = useState("");
    const addImage = () => {
        if (imageInput.trim()) {
            update("imageUrls", [...(form.imageUrls || []), imageInput.trim()]);
            setImageInput("");
        }
    };
    const removeImage = (i) => update("imageUrls", (form.imageUrls || []).filter((_, idx) => idx !== i));

    const handleSubmit = (e) => {
        e.preventDefault();
        onSubmit(form);
    };

    return (
        <div className="bg-white border border-gray-200 rounded-2xl p-5 shadow-sm mb-4">
            <div className="flex items-center justify-between mb-4 pb-3 border-b border-gray-100">
                <h2 className="text-base font-semibold text-gray-900">
                    {isEdit ? "Modifier l'hotel" : "Nouvel hotel"}
                </h2>
                <button onClick={onCancel} className="p-1.5 text-gray-400 hover:bg-gray-50 rounded-lg">
                    <X size={16} />
                </button>
            </div>

            <form onSubmit={handleSubmit} className="space-y-4">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                    <Field label="Nom de l'hotel">
                        <input type="text" value={form.nom} onChange={(e) => update("nom", e.target.value)} required
                            className="w-full border border-gray-200 rounded-xl px-3 py-2.5 text-sm outline-none focus:border-[#0EA5E9]" />
                    </Field>
                    <Field label="Ville">
                        <input type="text" value={form.ville} onChange={(e) => update("ville", e.target.value)} required
                            className="w-full border border-gray-200 rounded-xl px-3 py-2.5 text-sm outline-none focus:border-[#0EA5E9]" />
                    </Field>
                </div>

                <Field label="Adresse">
                    <input type="text" value={form.adresse} onChange={(e) => update("adresse", e.target.value)}
                        className="w-full border border-gray-200 rounded-xl px-3 py-2.5 text-sm outline-none focus:border-[#0EA5E9]" />
                </Field>

                {/* Coordonnees GPS (zone Montpellier Metropole) */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                    <Field label="Latitude (43.55 a 43.70)">
                        <input type="number" step="0.0001" min="43.55" max="43.70"
                            placeholder="ex: 43.6112"
                            value={form.latitude ?? ""}
                            onChange={(e) => update("latitude", e.target.value === "" ? null : parseFloat(e.target.value))}
                            className="w-full border border-gray-200 rounded-xl px-3 py-2.5 text-sm outline-none focus:border-[#0EA5E9]" />
                    </Field>
                    <Field label="Longitude (3.75 a 4.05)">
                        <input type="number" step="0.0001" min="3.75" max="4.05"
                            placeholder="ex: 3.8703"
                            value={form.longitude ?? ""}
                            onChange={(e) => update("longitude", e.target.value === "" ? null : parseFloat(e.target.value))}
                            className="w-full border border-gray-200 rounded-xl px-3 py-2.5 text-sm outline-none focus:border-[#0EA5E9]" />
                    </Field>
                </div>

                <div className="grid grid-cols-3 gap-3">
                    <Field label="Categorie (étoiles)">
                        <select value={form.categorie || 3} onChange={(e) => update("categorie", parseInt(e.target.value, 10))}
                            className="w-full border border-gray-200 rounded-xl px-3 py-2.5 text-sm outline-none focus:border-[#0EA5E9]">
                            <option value={2}>2 étoiles</option>
                            <option value={3}>3 étoiles</option>
                            <option value={4}>4 étoiles</option>
                            <option value={5}>5 étoiles</option>
                        </select>
                    </Field>
                    <Field label="Note moyenne /5">
                        <input type="number" step="0.1" min="0" max="5" value={form.noteMoyenne}
                            onChange={(e) => update("noteMoyenne", e.target.value === "" ? null : parseFloat(e.target.value))} required
                            className="w-full border border-gray-200 rounded-xl px-3 py-2.5 text-sm outline-none focus:border-[#0EA5E9]" />
                    </Field>
                    <Field label="Prix moyen / nuit (€)">
                        <input type="number" step="1" value={form.prixMoyenNuit || ""}
                            onChange={(e) => update("prixMoyenNuit", e.target.value === "" ? null : parseFloat(e.target.value))}
                            className="w-full border border-gray-200 rounded-xl px-3 py-2.5 text-sm outline-none focus:border-[#0EA5E9]" />
                    </Field>
                </div>

                <Field label="Description">
                    <textarea value={form.description} onChange={(e) => update("description", e.target.value)} rows="3"
                        className="w-full border border-gray-200 rounded-xl px-3 py-2.5 text-sm outline-none focus:border-[#0EA5E9] resize-none" />
                </Field>

                <Field label={`Photos (${form.imageUrls?.length ?? 0})`}>
                    <div className="flex gap-2 mb-2">
                        <input
                            type="url"
                            value={imageInput}
                            onChange={(e) => setImageInput(e.target.value)}
                            onKeyDown={(e) => e.key === "Enter" && (e.preventDefault(), addImage())}
                            placeholder="https://images.unsplash.com/..."
                            className="flex-1 border border-gray-200 rounded-xl px-3 py-2 text-sm outline-none focus:border-[#0EA5E9]"
                        />
                        <button type="button" onClick={addImage}
                            className="bg-[#0EA5E9] hover:bg-[#0284C7] text-white text-sm font-medium px-3 py-2 rounded-xl">
                            Ajouter
                        </button>
                    </div>
                    {form.imageUrls?.length > 0 && (
                        <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
                            {form.imageUrls.map((url, idx) => (
                                <div key={idx} className="relative group rounded-xl overflow-hidden border border-gray-200">
                                    <img src={url} alt={`Photo ${idx + 1}`}
                                        className="w-full h-24 object-cover object-center block"
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

                <div className="flex gap-2 pt-2">
                    <button type="submit"
                        className="flex-1 bg-[#0EA5E9] hover:bg-[#0284C7] text-white font-semibold py-2.5 rounded-xl text-sm transition-colors">
                        {isEdit ? "Enregistrer les modifications" : "Creer l'hotel"}
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

function Field({ label, children }) {
    return (
        <div>
            <p className="text-[10px] font-semibold text-gray-500 uppercase tracking-wider mb-1.5">{label}</p>
            {children}
        </div>
    );
}