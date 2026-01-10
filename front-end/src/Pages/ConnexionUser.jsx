import { useState } from "react";
import InputField from "../components/InputField";

export default function ConnextionUser() {
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [message, setMessage] = useState("");

    const handleLogin = async (e) => {
        e.preventDefault();

        const loginData = { email, password };

        try {
            const response = await fetch("http://localhost:8080/api/v1/login", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(loginData)
            });

            if (response.ok) {
                const data = await response.json();

                // 🔹 Stocke l'ID, l'email et le premier rôle (Admin / Employé / User)
                localStorage.setItem("id", data.id);
                localStorage.setItem("email", data.email);
                localStorage.setItem("role", data.roles[0]); // <- très important

                setMessage("Connexion réussie !");
                window.location.href = "/"; // redirige vers l'accueil
            } else {
                const errorText = await response.text();
                setMessage("Erreur : " + errorText);
            }
        } catch (error) {
            console.error("Erreur fetch :", error);
            setMessage("Erreur serveur");
        }
    };

    return (
        <div className="min-h-screen flex items-center justify-center bg-gray-50">
            <div className="bg-white p-10 rounded-2xl shadow-xl w-[380px] space-y-6">
                <h2 className="text-3xl font-bold text-center text-gray-800">
                    Connexion
                </h2>

                {message && (
                    <p className="text-center text-sm text-red-600">{message}</p>
                )}

                <form onSubmit={handleLogin} className="space-y-5">
                    <InputField
                        label="Email"
                        type="email"
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                    />

                    <InputField
                        label="Mot de passe"
                        type="password"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                    />

                    <button
                        type="submit"
                        className="w-full bg-blue-600 text-white py-3 rounded-lg hover:bg-blue-700 font-medium"
                    >
                        Se connecter
                    </button>
                </form>

                <p className="text-center text-sm text-gray-600">
                    Pas de compte ?{" "}
                    <a
                        href="/Inscription"
                        className="text-blue-600 font-medium hover:underline"
                    >
                        Créer un compte
                    </a>
                </p>
            </div>
        </div>
    );
}
