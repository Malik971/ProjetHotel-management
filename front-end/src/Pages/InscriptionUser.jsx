import { useState } from "react";
import InputField from "../components/InputField";

export default function InscriptionUser() {
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [message, setMessage] = useState("");
    const [debug, setDebug] = useState(null); // 🔍 debug détaillé

    const handleRegister = async (e) => {
        e.preventDefault();
        setMessage("");
        setDebug(null);

        const userData = {
            email,
            password
        };

        console.log("📤 Données envoyées :", userData);

        try {
            const response = await fetch("http://localhost:8080/api/v1/register", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify(userData)
            });

            console.log("📡 Status HTTP :", response.status);

            const contentType = response.headers.get("content-type");

            let responseBody;
            if (contentType && contentType.includes("application/json")) {
                responseBody = await response.json();
            } else {
                responseBody = await response.text();
            }

            console.log("📥 Réponse backend :", responseBody);

            if (!response.ok) {
                setMessage("❌ Erreur backend");
                setDebug({
                    status: response.status,
                    body: responseBody
                });
                return;
            }

            setMessage("✅ Inscription réussie !");
            setDebug(responseBody);

            setTimeout(() => {
                window.location.href = "/Connextion";
            }, 1000);

        } catch (error) {
            console.error("🚨 Erreur FETCH :", error);
            setMessage("❌ Erreur serveur / réseau");
            setDebug(error.message);
        }
    };

    return (
        <div className="min-h-screen flex items-center justify-center bg-gray-50">
            <div className="bg-white p-10 rounded-2xl shadow-xl w-[380px] space-y-6">
                <h2 className="text-3xl font-bold text-center text-gray-800">
                    Inscription
                </h2>

                {message && (
                    <p className="text-center text-sm text-red-600">
                        {message}
                    </p>
                )}

                {/* 🔍 DEBUG VISUEL */}
                {debug && (
                    <pre className="bg-gray-100 text-xs p-3 rounded text-red-700 overflow-auto">
                        {JSON.stringify(debug, null, 2)}
                    </pre>
                )}

                <form onSubmit={handleRegister} className="space-y-5">
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
                        className="w-full bg-black text-white py-3 rounded-lg hover:bg-gray-800 font-medium"
                    >
                        Créer un compte
                    </button>
                </form>

                <p className="text-center text-sm text-gray-600">
                    Déjà un compte ?{" "}
                    <a
                        href="/Connexion"
                        className="text-blue-600 font-medium hover:underline"
                    >
                        Connecte-toi
                    </a>
                </p>
            </div>
        </div>
    );
}
