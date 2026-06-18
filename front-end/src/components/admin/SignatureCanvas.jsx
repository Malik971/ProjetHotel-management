/**
 * SignatureCanvas.jsx
 * Composant de signature manuscrite sur canvas HTML5.
 *
 * Props :
 *   onSignature(base64: string)  callback appele quand l'admin confirme
 *                                la signature. Recoit l'image PNG en base64.
 *   disabled (bool)              desactive le canvas pendant le chargement
 *
 * Usage interne :
 *   - Souris et touch (mobile) sont supportes.
 *   - Le bouton "Effacer" remet le canvas a zero.
 *   - Le bouton "Confirmer la signature" appelle onSignature(base64).
 *   - La signature est refusee si le canvas est vide (detection par pixel).
 *
 * Point de migration niveau 3 :
 *   Ce composant ne change pas. Au niveau 3, la base64 generee ici est
 *   simplement envoyee au mock parapheur au lieu d'etre traitee localement.
 */

import { useRef, useEffect, useState, useCallback } from "react";

export default function SignatureCanvas({ onSignature, disabled = false }) {
  const canvasRef = useRef(null);
  const [dessin, setDessin] = useState(false);
  const [vide, setVide] = useState(true);

  // Initialise le canvas : fond blanc, curseur stylo
  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext("2d");
    ctx.fillStyle = "#ffffff";
    ctx.fillRect(0, 0, canvas.width, canvas.height);
    ctx.strokeStyle = "#0f172a";
    ctx.lineWidth = 2;
    ctx.lineCap = "round";
    ctx.lineJoin = "round";
  }, []);

  // --- Helpers coordonnees (souris et touch) ---

  function coords(e) {
    const rect = canvasRef.current.getBoundingClientRect();
    const scaleX = canvasRef.current.width  / rect.width;
    const scaleY = canvasRef.current.height / rect.height;
    const source = e.touches ? e.touches[0] : e;
    return {
      x: (source.clientX - rect.left) * scaleX,
      y: (source.clientY - rect.top)  * scaleY,
    };
  }

  // --- Gestion du trace ---

  const commencer = useCallback((e) => {
    if (disabled) return;
    e.preventDefault();
    const ctx = canvasRef.current.getContext("2d");
    const { x, y } = coords(e);
    ctx.beginPath();
    ctx.moveTo(x, y);
    setDessin(true);
    setVide(false);
  }, [disabled]);

  const tracer = useCallback((e) => {
    if (!dessin || disabled) return;
    e.preventDefault();
    const ctx = canvasRef.current.getContext("2d");
    const { x, y } = coords(e);
    ctx.lineTo(x, y);
    ctx.stroke();
  }, [dessin, disabled]);

  const terminer = useCallback(() => {
    setDessin(false);
  }, []);

  // --- Actions ---

  function effacer() {
    const canvas = canvasRef.current;
    const ctx = canvas.getContext("2d");
    ctx.fillStyle = "#ffffff";
    ctx.fillRect(0, 0, canvas.width, canvas.height);
    setVide(true);
  }

  function confirmer() {
    if (vide) return;
    const base64 = canvasRef.current.toDataURL("image/png");
    onSignature(base64);
  }

  return (
    <div className="flex flex-col gap-3">
      {/* Zone de dessin */}
      <div
        className={[
          "rounded-lg border-2 overflow-hidden",
          disabled
            ? "border-gray-200 opacity-50 cursor-not-allowed"
            : "border-sky-400 cursor-crosshair",
        ].join(" ")}
        style={{ touchAction: "none" }}
      >
        <canvas
          ref={canvasRef}
          width={600}
          height={160}
          style={{ width: "100%", display: "block" }}
          onMouseDown={commencer}
          onMouseMove={tracer}
          onMouseUp={terminer}
          onMouseLeave={terminer}
          onTouchStart={commencer}
          onTouchMove={tracer}
          onTouchEnd={terminer}
        />
      </div>

      {/* Indication */}
      <p className="text-xs text-slate-500 text-center select-none">
        Tracez votre signature dans la zone ci-dessus
      </p>

      {/* Boutons */}
      <div className="flex gap-3 justify-end">
        <button
          type="button"
          onClick={effacer}
          disabled={disabled || vide}
          className="px-4 py-2 text-sm rounded-lg border border-slate-300 text-slate-600
                     hover:bg-slate-50 disabled:opacity-40 disabled:cursor-not-allowed
                     transition-colors"
        >
          Effacer
        </button>
        <button
          type="button"
          onClick={confirmer}
          disabled={disabled || vide}
          className="px-5 py-2 text-sm font-semibold rounded-lg
                     bg-sky-500 text-white hover:bg-sky-600
                     disabled:opacity-40 disabled:cursor-not-allowed
                     transition-colors"
        >
          Confirmer la signature
        </button>
      </div>
    </div>
  );
}