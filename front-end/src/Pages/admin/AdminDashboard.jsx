import { useNavigate } from "react-router-dom";
import "./AdminDashboard.css";

export default function AdminDashboard() {
  const navigate = useNavigate();

  return (
    <div className="admin-container">
      <h1>🛠️ Administration</h1>

      <div className="admin-grid">
        <div className="admin-card" onClick={() => navigate("/admin/add-user")}>
          <h2>👤 Ajouter utilisateur</h2>
          <p>Créer un employé ou un admin</p>
        </div>

        <div className="admin-card" onClick={() => navigate("/admin/add-hotel")}>
          <h2>🏨 Ajouter hôtel</h2>
          <p>Créer un nouvel hôtel</p>
        </div>

        <div className="admin-card" onClick={() => navigate("/chambres")}>
          <h2>🛏️ Ajouter chambre</h2>
          <p>Ajouter une chambre à un hôtel</p>
        </div>

        <div className="admin-card disabled">
          <h2>📊 Statistiques</h2>
          <p>Bientôt disponible</p>
        </div>
      </div>
    </div>
  );
}
