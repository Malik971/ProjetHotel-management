import React, { useState } from 'react';
import { useChambres } from '../hooks/useChambres';
import ChambreList from '../components/ChambreManagement/ChambreList';
import ChambreForm from '../components/ChambreManagement/ChambreForm';

export default function ChambreManagementPage() {
  const { chambres, loading, error, creerChambre, updateChambre, deleteChambre } = useChambres();
  const [showForm, setShowForm] = useState(false);
  const [editingChambre, setEditingChambre] = useState(null);

  const handleCreate = () => {
    setEditingChambre(null);
    setShowForm(true);
  };

  const handleEdit = (chambre) => {
    setEditingChambre(chambre);
    setShowForm(true);
  };

  const handleDelete = async (id) => {
    if (window.confirm('Êtes-vous sûr de vouloir supprimer cette chambre ?')) {
      const result = await deleteChambre(id);
      if (result.success) {
        alert('Chambre supprimée avec succès');
      } else {
        alert(result.error);
      }
    }
  };

  const handleSubmit = async (chambre) => {
    const result = editingChambre
      ? await updateChambre(editingChambre.id, chambre)
      : await creerChambre(chambre);

    if (result.success) {
      alert(editingChambre ? 'Chambre modifiée' : 'Chambre créée');
      setShowForm(false);
      setEditingChambre(null);
    } else {
      alert(result.error);
    }
  };

  return (
    <div className="max-w-7xl mx-auto px-6 py-10">
      <div className="flex justify-between items-center mb-8">
        <h1 className="text-3xl font-bold">Gestion des Chambres</h1>
        <button
          onClick={handleCreate}
          className="bg-blue-600 text-white px-6 py-2 rounded-lg hover:bg-blue-700"
        >
          + Ajouter une chambre
        </button>
      </div>

      {showForm && (
        <div className="mb-8">
          <ChambreForm
            chambre={editingChambre}
            onSubmit={handleSubmit}
            onCancel={() => {
              setShowForm(false);
              setEditingChambre(null);
            }}
          />
        </div>
      )}

      <ChambreList
        chambres={chambres}
        loading={loading}
        error={error}
        onEdit={handleEdit}
        onDelete={handleDelete}
      />
    </div>
  );
}