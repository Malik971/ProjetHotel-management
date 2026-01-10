import { useEffect, useState } from "react";

export default function AdminDashboard() {
    const [hotels, setHotels] = useState([]);
    const [form, setForm] = useState({ nom: "", adresse: "", ville: "", description: "", noteMoyenne: 0 });
    const [imageFile, setImageFile] = useState(null);

    // Charger les hôtels
    const loadHotels = () => {
        fetch("http://localhost:8080/api/hotels")
            .then(res => res.json())
            .then(data => setHotels(data))
            .catch(err => console.error("Erreur chargement:", err));
    };

    useEffect(() => {
        loadHotels();
    }, []);

    // Ajouter un hôtel
    const createHotel = () => {
        const formData = new FormData();
        formData.append("nom", form.nom);
        formData.append("adresse", form.adresse);
        formData.append("ville", form.ville);
        formData.append("description", form.description);
        formData.append("noteMoyenne", form.noteMoyenne);

        if (imageFile) {
            formData.append("image", imageFile);
        }

        fetch("http://localhost:8080/api/hotels", {
            method: "POST",
            body: formData // ⚠️ PAS de Content-Type (automatique avec FormData)
        })
            .then(res => {
                if (!res.ok) throw new Error("Erreur lors de la création");
                return res.json();
            })
            .then(() => {
                loadHotels();
                setForm({ nom: "", adresse: "", ville: "", description: "", noteMoyenne: 0 });
                setImageFile(null);
            })
            .catch(err => console.error("Erreur création:", err));
    };

    // Supprimer un hôtel
    const deleteHotel = (id) => {
        fetch(`http://localhost:8080/api/hotels/${id}`, {
            method: "DELETE"
        })
            .then(() => loadHotels())
            .catch(err => console.error("Erreur suppression:", err));
    };

    return (
        <div className="p-10">
            <h1 className="text-3xl font-bold mb-5">Admin Dashboard — Gestion des Hôtels</h1>

            {/* Formulaire d'ajout */}
            <div className="bg-white shadow p-5 rounded mb-10 w-[400px]">
                <h2 className="text-xl font-bold mb-3">Ajouter un hôtel</h2>

                <input className="border p-2 w-full mb-2" placeholder="Nom"
                       value={form.nom} onChange={(e) => setForm({ ...form, nom: e.target.value })} />

                <input className="border p-2 w-full mb-2" placeholder="Adresse"
                       value={form.adresse} onChange={(e) => setForm({ ...form, adresse: e.target.value })} />

                <input className="border p-2 w-full mb-2" placeholder="Ville"
                       value={form.ville} onChange={(e) => setForm({ ...form, ville: e.target.value })} />

                <textarea className="border p-2 w-full mb-2" placeholder="Description"
                          value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })}></textarea>

                <input className="border p-2 w-full mb-2" placeholder="Note moyenne"
                       type="number" step="0.1"
                       value={form.noteMoyenne} onChange={(e) => setForm({ ...form, noteMoyenne: e.target.value })} />

                {/* 🆕 Input pour l'image */}
                <input
                    className="border p-2 w-full mb-2"
                    type="file"
                    accept="image/*"
                    onChange={(e) => setImageFile(e.target.files[0])}
                />

                <button
                    className="bg-blue-600 text-white py-2 px-4 rounded"
                    onClick={createHotel}
                >
                    Ajouter
                </button>
            </div>

            {/* Liste des hôtels */}
            <table className="w-full bg-white shadow rounded">
                <thead>
                <tr className="bg-gray-200 font-bold">
                    <td className="p-3">Nom</td>
                    <td className="p-3">Ville</td>
                    <td className="p-3">Note</td>
                    <td className="p-3">Actions</td>
                </tr>
                </thead>
                <tbody>
                {hotels.map(h => (
                    <tr key={h.id} className="border-b">
                        <td className="p-3">{h.nom}</td>
                        <td className="p-3">{h.ville}</td>
                        <td className="p-3">{h.noteMoyenne}</td>
                        <td className="p-3 flex gap-2">
                            <button className="bg-red-500 text-white px-3 py-1 rounded"
                                    onClick={() => deleteHotel(h.id)}>
                                Supprimer
                            </button>

                            <button className="bg-yellow-500 text-white px-3 py-1 rounded">
                                Modifier (à faire)
                            </button>
                        </td>
                    </tr>
                ))}
                </tbody>
            </table>
        </div>
    );
}
