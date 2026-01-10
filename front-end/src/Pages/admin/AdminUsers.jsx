import { useEffect, useState } from "react";

export default function AdminUsers() {
    const [users, setUsers] = useState([]);
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [role, setRole] = useState("EMPLOYE");
    const [message, setMessage] = useState("");

    // Charger tous les utilisateurs
    const fetchUsers = () => {
        fetch("http://localhost:8080/api/hotels")
            .then(res => res.json())
            .then(data => setUsers(data))
            .catch(err => console.error("Erreur fetch:", err));
    };

    useEffect(() => {
        fetchUsers();
    }, []);

    // Ajouter un utilisateur
    const handleAddUser = () => {
        setMessage("");
        fetch(`http://localhost:8080/api/v1/register?role=${role}`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ email, password })
        })
            .then(async res => {
                if (res.ok) return res.json();
                const text = await res.text();
                throw new Error(text);
            })
            .then(userEntity => {
                setEmail("");
                setPassword("");
                fetchUsers();
            })
            .catch(err => setMessage(err.message));
    };

    // Supprimer un utilisateur
    const handleDeleteUser = (id) => {
        fetch(`http://localhost:8080/api/admin/users/${id}`, {
            method: "DELETE"
        })
            .then(() => fetchUsers())
            .catch(err => console.error(err));
    };

    return (
        <div className="p-8">
            <h2 className="text-2xl font-bold mb-4">Admin Dashboard — Gestion des utilisateurs</h2>

            {/* Formulaire ajout */}
            <div className="mb-6 bg-white p-5 shadow rounded w-[400px]">
                <h3 className="font-semibold mb-2">Ajouter un utilisateur</h3>
                {message && <p className="text-red-600 text-sm mb-2">{message}</p>}
                <input
                    type="email"
                    placeholder="Email"
                    value={email}
                    onChange={e => setEmail(e.target.value)}
                    className="border p-2 mb-2 w-full"
                />
                <input
                    type="password"
                    placeholder="Mot de passe"
                    value={password}
                    onChange={e => setPassword(e.target.value)}
                    className="border p-2 mb-2 w-full"
                />
                <select
                    value={role}
                    onChange={e => setRole(e.target.value)}
                    className="border p-2 mb-2 w-full"
                >
                    <option value="ADMIN">Admin</option>
                    <option value="EMPLOYE">Employé</option>
                    <option value="USER">Utilisateur</option>
                </select>
                <button
                    onClick={handleAddUser}
                    className="bg-blue-500 text-white py-2 px-4 rounded"
                >
                    Ajouter
                </button>
            </div>

            {/* Liste des utilisateurs */}
            <div className="bg-white p-5 shadow rounded">
                <h3 className="font-semibold mb-2">Liste des utilisateurs</h3>
                <table className="w-full border-collapse border border-gray-300">
                    <thead>
                        <tr className="bg-gray-200 font-bold">
                            <th className="border p-2">ID</th>
                            <th className="border p-2">Email</th>
                            <th className="border p-2">Rôle</th>
                            <th className="border p-2">Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        {users.map(u => (
                            <tr key={u.id} className="border-b">
                                <td className="p-2">{u.id}</td>
                                <td className="p-2">{u.email}</td>
                                <td className="p-2">{u.role}</td>
                                <td className="p-2 flex gap-2">
                                    <button
                                        className="bg-red-500 text-white px-2 py-1 rounded"
                                        onClick={() => handleDeleteUser(u.id)}
                                    >
                                        Supprimer
                                    </button>
                                </td>
                            </tr>
                        ))}
                        {users.length === 0 && (
                            <tr>
                                <td colSpan="4" className="text-center p-2 text-gray-500">
                                    Aucun utilisateur
                                </td>
                            </tr>
                        )}
                    </tbody>
                </table>
            </div>
        </div>
    );
}

