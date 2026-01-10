import { useNavigate } from "react-router-dom";
import "./EmployeDashboard.css";

export default function EmployeDashboard() {
  const navigate = useNavigate();

  return (
    <div className="employe-container">
      <h1>👔 Espace Employé</h1>

      <div className="employe-grid">
        <div
          className="employe-card"
          onClick={() => navigate("/employe/reservations")}
        >
          <h2>📅 Gérer réservations</h2>
          <p>Valider, modifier, annuler</p>
        </div>

        <div
          className="employe-card"
          onClick={() => navigate("/employe/chambres")}
        >
          <h2>🛏️ Gérer chambres</h2>
          <p>Disponibilité et état</p>
        </div>

        <div
          className="employe-card"
          onClick={() => navigate("/employe/clients")}
        >
          <h2>👥 Gérer clients</h2>
          <p>Informations clients</p>
        </div>
      </div>
    </div>
  );
}
