import React, { useState, useEffect } from 'react';

export default function ChambreForm({ chambre, onSubmit, onCancel }) {
  const [formData, setFormData] = useState({
    nom: '',
    prixParNuit: '',
    capacite: 1,
    superficie: '',
    typeLit: '',
    description: '',
    equipements: [],
    imageUrls: [],
    hotelId: ''
  });

  const [equipementInput, setEquipementInput] = useState('');
  const[imageInput, setImageInput] = useState('');
useEffect(() => {
if (chambre) {
setFormData(chambre);
}
}, [chambre]);
const handleChange = (e) => {
const { name, value } = e.target;
setFormData({ ...formData, [name]: value });
};
const handleAddEquipement = () => {
if (equipementInput.trim()) {
setFormData({
...formData,
equipements: [...formData.equipements, equipementInput.trim()]
});
setEquipementInput('');
}
};
const handleRemoveEquipement = (index) => {
setFormData({
...formData,
equipements: formData.equipements.filter((_, i) => i !== index)
});
};
const handleAddImage = () => {
if (imageInput.trim()) {
setFormData({
...formData,
imageUrls: [...formData.imageUrls, imageInput.trim()]
});
setImageInput('');
}
};
const handleRemoveImage = (index) => {
setFormData({
...formData,
imageUrls: formData.imageUrls.filter((_, i) => i !== index)
});
};
const handleSubmit = (e) => {
e.preventDefault();
onSubmit({
...formData,
prixParNuit: parseFloat(formData.prixParNuit),
capacite: parseInt(formData.capacite),
superficie: parseInt(formData.superficie),
hotelId: parseInt(formData.hotelId)
});
};
return (
<div className="bg-white p-6 rounded-lg shadow">
<h2 className="text-xl font-bold mb-4">
{chambre ? 'Modifier' : 'Créer'} une chambre
</h2>
  <form onSubmit={handleSubmit} className="space-y-4">
    <div className="grid grid-cols-2 gap-4">
      <div>
        <label className="block text-sm font-medium mb-1">Nom de la chambre</label>
        <input
          type="text"
          name="nom"
          value={formData.nom}
          onChange={handleChange}
          required
          className="w-full border rounded-lg px-3 py-2"
        />
      </div>

      <div>
        <label className="block text-sm font-medium mb-1">Prix par nuit (€)</label>
        <input
          type="number"
          name="prixParNuit"
          value={formData.prixParNuit}
          onChange={handleChange}
          required
          step="0.01"
          className="w-full border rounded-lg px-3 py-2"
        />
      </div>

      <div>
        <label className="block text-sm font-medium mb-1">Capacité (personnes)</label>
        <input
          type="number"
          name="capacite"
          value={formData.capacite}
          onChange={handleChange}
          required
          min="1"
          className="w-full border rounded-lg px-3 py-2"
        />
      </div>

      <div>
        <label className="block text-sm font-medium mb-1">Superficie (m²)</label>
        <input
          type="number"
          name="superficie"
          value={formData.superficie}
          onChange={handleChange}
          required
          className="w-full border rounded-lg px-3 py-2"
        />
      </div>

      <div>
        <label className="block text-sm font-medium mb-1">Type de lit</label>
        <input
          type="text"
          name="typeLit"
          value={formData.typeLit}
          onChange={handleChange}
          required
          placeholder="Ex: Lit Queen"
          className="w-full border rounded-lg px-3 py-2"
        />
      </div>

      <div>
        <label className="block text-sm font-medium mb-1">ID Hôtel</label>
        <input
          type="number"
          name="hotelId"
          value={formData.hotelId}
          onChange={handleChange}
          required
          className="w-full border rounded-lg px-3 py-2"
        />
      </div>
    </div>

    <div>
      <label className="block text-sm font-medium mb-1">Description</label>
      <textarea
        name="description"
        value={formData.description}
        onChange={handleChange}
        rows="3"
        className="w-full border rounded-lg px-3 py-2"
      />
    </div>

    {/* Équipements */}
    <div>
      <label className="block text-sm font-medium mb-1">Équipements</label>
      <div className="flex gap-2 mb-2">
        <input
          type="text"
          value={equipementInput}
          onChange={(e) => setEquipementInput(e.target.value)}
          placeholder="Ex: Climatisation"
          className="flex-1 border rounded-lg px-3 py-2"
        />
        <button
          type="button"
          onClick={handleAddEquipement}
          className="bg-green-600 text-white px-4 py-2 rounded-lg"
        >
          Ajouter
        </button>
      </div>
      <div className="flex flex-wrap gap-2">
        {formData.equipements.map((eq, idx) => (
          <span
            key={idx}
            className="bg-gray-100 px-3 py-1 rounded-full text-sm flex items-center gap-2"
          >
            {eq}
            <button
              type="button"
              onClick={() => handleRemoveEquipement(idx)}
              className="text-red-600 font-bold"
            >
              ×
            </button>
          </span>
        ))}
      </div>
    </div>

    {/* Images */}
    <div>
      <label className="block text-sm font-medium mb-1">Images (URLs)</label>
      <div className="flex gap-2 mb-2">
        <input
          type="url"
          value={imageInput}
          onChange={(e) => setImageInput(e.target.value)}
          placeholder="https://example.com/image.jpg"
          className="flex-1 border rounded-lg px-3 py-2"
        />
        <button
          type="button"
          onClick={handleAddImage}
          className="bg-green-600 text-white px-4 py-2 rounded-lg"
        >
          Ajouter
        </button>
      </div>
      <div className="space-y-2">
        {formData.imageUrls.map((url, idx) => (
          <div key={idx} className="flex items-center gap-2">
            <img src={url} alt="" className="w-16 h-16 object-cover rounded" />
            <span className="flex-1 text-sm text-gray-600 truncate">{url}</span>
            <button
              type="button"
              onClick={() => handleRemoveImage(idx)}
              className="text-red-600"
            >
              Supprimer
            </button>
          </div>
        ))}
      </div>
    </div>

    <div className="flex gap-2">
      <button
        type="submit"
        className="flex-1 bg-blue-600 text-white py-2 rounded-lg hover:bg-blue-700"
      >
        {chambre ? 'Modifier' : 'Créer'}
      </button>
      <button
        type="button"
        onClick={onCancel}
        className="flex-1 bg-gray-300 text-gray-700 py-2 rounded-lg hover:bg-gray-400"
      >
        Annuler
      </button>
    </div>
  </form>
</div>
);
}
