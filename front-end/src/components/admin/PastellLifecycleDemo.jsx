// src/components/admin/PastellLifecycleDemo.jsx
import { useEffect, useMemo, useRef, useState } from "react";
import {
    Play, Pause, RotateCcw, Calendar, Cpu, Server,
    CheckCircle2, Clock, RefreshCw, AlertTriangle, Info,
} from "lucide-react";

/**
 * Demo guidee et animee du cycle de vie d'un dossier dans le bus Pastell.
 *
 * Pensee comme une petite video explicative qui tourne en boucle : elle prend
 * le temps de montrer chaque etape, y compris les cas ou le dossier echoue, et
 * elle explique pourquoi. Le but est qu'un admin OU un employe comprenne d'un
 * coup d'oeil a quoi servent les quatre compteurs du tableau de bord :
 * Dossiers OK, En traitement, Relances, Anomalies.
 *
 * Tout est scripte cote frontend (aucun appel reseau) : c'est un support
 * pedagogique, pas un reflet de l'etat reel du bus.
 */

const SCENE_MS = 4600;

// Etat metier illustre a chaque scene, avec son code couleur.
const STATE_STYLE = {
    pending: { label: "En traitement", dot: "bg-[#0EA5E9]", chip: "bg-sky-100 text-sky-800", ring: "ring-[#0EA5E9]" },
    ok: { label: "OK", dot: "bg-emerald-500", chip: "bg-emerald-100 text-emerald-800", ring: "ring-emerald-500" },
    retry: { label: "Relance", dot: "bg-amber-500", chip: "bg-amber-100 text-amber-800", ring: "ring-amber-500" },
    error: { label: "Anomalie", dot: "bg-red-500", chip: "bg-red-100 text-red-800", ring: "ring-red-500" },
};

// Position du "dossier" sur la voie (reservation -> bus -> Pastell).
const STOP = { reservation: "8%", bus: "50%", pastell: "88%" };

const SCENES = [
    {
        key: "intro",
        active: "bus",
        title: "A quoi sert le bus Pastell ?",
        text: "Chaque reservation devient un dossier suivi de bout en bout. Voici son cycle de vie, etape par etape, y compris quand ca coince.",
        counters: { ok: 0, pending: 0, retry: 0, anomalies: 0 },
    },
    {
        key: "create",
        active: "reservation",
        state: "pending",
        title: "1. Une reservation arrive",
        text: "Un client reserve une chambre. Le bus prepare aussitot un dossier et le met En traitement.",
        counters: { ok: 0, pending: 1, retry: 0, anomalies: 0 },
    },
    {
        key: "ok",
        active: "pastell",
        state: "ok",
        title: "2. Pastell recoit le dossier",
        text: "Pastell repond favorablement. Le dossier passe OK : tout est synchronise.",
        counters: { ok: 1, pending: 0, retry: 0, anomalies: 0 },
    },
    {
        key: "down",
        active: "bus",
        state: "retry",
        reason: "Pastell injoignable (timeout reseau).",
        title: "3. Parfois Pastell ne repond pas",
        text: "Une nouvelle reservation arrive, mais Pastell est momentanement injoignable. Le bus passe le dossier en Relance et reessaie tout seul, sans que personne intervienne.",
        counters: { ok: 1, pending: 0, retry: 1, anomalies: 0 },
    },
    {
        key: "recovered",
        active: "pastell",
        state: "ok",
        title: "4. La relance automatique reussit",
        text: "Pastell est revenu. Le bus a retente, et cette fois ca passe. Le dossier rejoint les OK.",
        counters: { ok: 2, pending: 0, retry: 0, anomalies: 0 },
    },
    {
        key: "failed",
        active: "bus",
        state: "error",
        reason: "Pastell reste indisponible apres plusieurs relances.",
        title: "5. Quand la relance n'y arrive pas",
        text: "Si Pastell reste KO apres plusieurs tentatives, le bus arrete de reessayer et marque le dossier en Anomalie. Un humain doit regarder.",
        counters: { ok: 2, pending: 0, retry: 0, anomalies: 1 },
    },
    {
        key: "divergence",
        active: "pastell",
        state: "error",
        reason: "Pastell veut annuler un sejour deja termine.",
        title: "6. Quand Pastell et Sejour ne sont pas d'accord",
        text: "Un agent annule un dossier cote Pastell, mais le sejour est deja termine cote Sejour. Le bus refuse de trancher seul : il signale une divergence pour arbitrage humain.",
        counters: { ok: 2, pending: 0, retry: 0, anomalies: 2 },
    },
    {
        key: "recap",
        active: "bus",
        title: "Le bus garde tout sous controle",
        text: "Succes synchronises, relances automatiques sur les pannes passageres, et anomalies signalees quand un humain doit decider. Rien ne se perd en silence.",
        counters: { ok: 2, pending: 0, retry: 0, anomalies: 2 },
    },
];

// eslint-disable-next-line no-unused-vars -- faux positif : Icon est rendu en JSX plus bas
function PipelineNode({ icon: Icon, label, sub, isActive }) {
    return (
        <div className={`flex flex-col items-center gap-1.5 transition-all ${isActive ? "scale-105" : "opacity-60"}`}>
            <div className={`w-14 h-14 rounded-2xl flex items-center justify-center border transition-all ${
                isActive ? "bg-white border-[#0EA5E9] shadow-sm ring-2 ring-[#0EA5E9]/20" : "bg-white border-gray-200"
            }`}>
                <Icon size={22} className={isActive ? "text-[#0EA5E9]" : "text-gray-400"} />
            </div>
            <div className="text-center leading-tight">
                <p className="text-xs font-semibold text-gray-800">{label}</p>
                <p className="text-[10px] text-gray-400">{sub}</p>
            </div>
        </div>
    );
}

// eslint-disable-next-line no-unused-vars -- faux positif : Icon est rendu en JSX plus bas
function Counter({ icon: Icon, iconColor, label, value, hint, highlight, highlightRing }) {
    return (
        <div className={`rounded-xl border bg-white p-3 transition-all ${
            highlight ? `border-transparent ring-2 ${highlightRing} shadow-sm` : "border-gray-200"
        }`}>
            <div className="flex items-center gap-1.5">
                <Icon size={14} className={iconColor} />
                <span className="text-[11px] font-medium text-gray-500">{label}</span>
            </div>
            <p className="mt-1 text-xl font-bold text-gray-900 tabular-nums">{value}</p>
            <p className="text-[10px] text-gray-400 leading-tight">{hint}</p>
        </div>
    );
}

export default function PastellLifecycleDemo() {
    const [index, setIndex] = useState(0);
    const [playing, setPlaying] = useState(true);
    const timer = useRef(null);

    const scene = SCENES[index];
    const style = scene.state ? STATE_STYLE[scene.state] : null;

    useEffect(() => {
        if (!playing) return;
        timer.current = setInterval(() => {
            setIndex((i) => (i + 1) % SCENES.length);
        }, SCENE_MS);
        return () => clearInterval(timer.current);
    }, [playing]);

    const goTo = (i) => {
        setIndex(i);
        // Repart proprement depuis cette scene sans accelerer le cycle.
        if (playing) {
            clearInterval(timer.current);
            timer.current = setInterval(() => setIndex((p) => (p + 1) % SCENES.length), SCENE_MS);
        }
    };

    const restart = () => goTo(0);

    const tokenLeft = useMemo(() => STOP[scene.active] ?? "50%", [scene.active]);

    return (
        <div className="bg-white">
            <style>{`@keyframes demoGrow { from { width: 0% } to { width: 100% } }`}</style>

            {/* En-tete + controles */}
            <div className="flex items-center justify-between gap-3 mb-1">
                <div className="flex items-center gap-2">
                    <Info size={16} className="text-[#0EA5E9]" />
                    <h3 className="text-sm font-semibold text-gray-900">Demo guidee : le cycle de vie d'un dossier</h3>
                </div>
                <div className="flex items-center gap-1">
                    <button onClick={() => setPlaying((p) => !p)} title={playing ? "Pause" : "Lecture"}
                        className="p-1.5 rounded-lg text-gray-500 hover:bg-gray-100 transition-colors">
                        {playing ? <Pause size={15} /> : <Play size={15} />}
                    </button>
                    <button onClick={restart} title="Recommencer"
                        className="p-1.5 rounded-lg text-gray-500 hover:bg-gray-100 transition-colors">
                        <RotateCcw size={15} />
                    </button>
                </div>
            </div>

            {/* Barre de progression de la scene (se remplit puis enchaine) */}
            <div className="h-1 w-full bg-gray-100 rounded-full overflow-hidden mb-5">
                <div
                    key={`${index}-${playing}`}
                    className="h-full bg-[#0EA5E9]"
                    style={playing ? { animation: `demoGrow ${SCENE_MS}ms linear forwards` } : { width: "100%" }}
                />
            </div>

            {/* Voie : Reservation -> Bus -> Pastell, avec le dossier qui se deplace */}
            <div className="rounded-2xl border border-gray-100 bg-[#F8FAFC] p-5 mb-4">
                <div className="grid grid-cols-3 items-start">
                    <PipelineNode icon={Calendar} label="Reservation" sub="cote Sejour" isActive={scene.active === "reservation"} />
                    <PipelineNode icon={Cpu} label="Bus Pastell" sub="orchestration" isActive={scene.active === "bus"} />
                    <PipelineNode icon={Server} label="Pastell" sub="dossier" isActive={scene.active === "pastell"} />
                </div>

                {/* La voie + le jeton "dossier" */}
                <div className="relative mt-4 h-7">
                    <div className="absolute top-1/2 left-[8%] right-[8%] h-0.5 -translate-y-1/2 bg-gray-200" />
                    <div
                        className="absolute top-1/2 -translate-x-1/2 -translate-y-1/2 transition-all duration-700 ease-in-out"
                        style={{ left: tokenLeft }}
                    >
                        {style ? (
                            <span className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[11px] font-semibold shadow-sm ${style.chip}`}>
                                <span className={`w-1.5 h-1.5 rounded-full ${style.dot}`} />
                                {style.label}
                            </span>
                        ) : (
                            <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[11px] font-semibold bg-gray-100 text-gray-500 shadow-sm">
                                Dossier
                            </span>
                        )}
                    </div>
                </div>
            </div>

            {/* Les 4 compteurs, celui concerne par la scene est mis en avant */}
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 mb-4">
                <Counter icon={CheckCircle2} iconColor="text-emerald-500" label="Dossiers OK"
                    value={scene.counters.ok} hint="Synchronises"
                    highlight={scene.state === "ok"} highlightRing="ring-emerald-400" />
                <Counter icon={Clock} iconColor="text-[#0EA5E9]" label="En traitement"
                    value={scene.counters.pending} hint="En attente"
                    highlight={scene.state === "pending"} highlightRing="ring-sky-400" />
                <Counter icon={RefreshCw} iconColor="text-amber-500" label="Relances"
                    value={scene.counters.retry} hint="Automatique"
                    highlight={scene.state === "retry"} highlightRing="ring-amber-400" />
                <Counter icon={AlertTriangle} iconColor="text-red-500" label="Anomalies"
                    value={scene.counters.anomalies} hint="Arbitrage humain"
                    highlight={scene.state === "error"} highlightRing="ring-red-400" />
            </div>

            {/* Legende de la scene : titre, explication, et raison si echec */}
            <div className="rounded-2xl border border-gray-100 p-4">
                <div className="flex items-center gap-2 mb-1.5">
                    {style && (
                        <span className={`inline-flex items-center gap-1.5 px-2 py-0.5 rounded-full text-[11px] font-semibold ${style.chip}`}>
                            <span className={`w-1.5 h-1.5 rounded-full ${style.dot}`} />
                            {style.label}
                        </span>
                    )}
                    <h4 className="text-sm font-semibold text-gray-900">{scene.title}</h4>
                </div>
                <p className="text-sm text-gray-600 leading-relaxed">{scene.text}</p>
                {scene.reason && (
                    <div className="mt-2.5 flex items-start gap-2 rounded-lg bg-red-50 border border-red-100 px-3 py-2">
                        <AlertTriangle size={14} className="text-red-500 mt-0.5 shrink-0" />
                        <p className="text-xs text-red-700"><span className="font-semibold">Pourquoi :</span> {scene.reason}</p>
                    </div>
                )}
            </div>

            {/* Points de scene cliquables */}
            <div className="flex items-center justify-center gap-1.5 mt-4">
                {SCENES.map((s, i) => (
                    <button
                        key={s.key}
                        onClick={() => goTo(i)}
                        aria-label={`Aller a l'etape ${i + 1}`}
                        className={`h-1.5 rounded-full transition-all ${
                            i === index ? "w-6 bg-[#0EA5E9]" : "w-1.5 bg-gray-200 hover:bg-gray-300"
                        }`}
                    />
                ))}
            </div>
        </div>
    );
}
