// src/components/admin/PastellLifecycleDemo.jsx
import { useEffect, useRef, useState } from "react";
import {
    Play, Pause, RotateCcw, Info,
    UserPlus, BedDouble, Clock, LogIn, ClipboardList,
    PenLine, CheckCircle2, ArrowRight,
} from "lucide-react";

/**
 * Demo guidee et animee du parcours complet de la plateforme.
 *
 * Concue pour un recruteur qui decouvre le site. Chaque scene explique
 * ce qu'il doit faire, dans quel ordre, et pourquoi. L'accent est mis
 * sur le workflow de validation par signature electronique, qui est le
 * coeur differenciant du projet.
 *
 * La scene 6 inclut une animation SVG de signature manuscrite pour
 * rendre le moment de validation visuellement marquant.
 */

const SCENE_MS = 6000;

const SCENES = [
    {
        key: "welcome",
        icon: Info,
        iconColor: "text-[#0EA5E9]",
        title: "Bienvenue dans la demo guidee",
        text: "Ce guide vous montre en 7 etapes comment fonctionne la plateforme, de la reservation client jusqu'a la validation par signature electronique. Suivez les etapes dans l'ordre pour vivre le parcours complet.",
        action: null,
        visual: "intro",
    },
    {
        key: "create-account",
        icon: UserPlus,
        iconColor: "text-[#0EA5E9]",
        title: "1. Creez votre propre compte",
        text: "Cliquez sur Inscription en haut a droite. Remplissez le formulaire avec votre email et un mot de passe. Ce compte sera votre profil client pour la suite de la demo.",
        action: { label: "Page d'inscription", path: "/Inscription" },
        visual: "step1",
    },
    {
        key: "reserve",
        icon: BedDouble,
        iconColor: "text-[#0EA5E9]",
        title: "2. Faites une reservation",
        text: "Retournez a l'accueil, choisissez un hotel, selectionnez une chambre et des dates, puis confirmez. Votre reservation est creee avec le statut En attente : elle n'est pas encore validee.",
        action: { label: "Voir les hotels", path: "/" },
        visual: "step2",
    },
    {
        key: "en-attente",
        icon: Clock,
        iconColor: "text-amber-500",
        title: "3. Votre reservation est en attente",
        text: "Allez dans Mes reservations. Vous verrez que votre dossier affiche \"Validation par notre equipe\" en cours. C'est normal : un agent doit valider et signer avant que la reservation soit confirmee. C'est cette etape qui donne du sens au bus d'orchestration.",
        action: { label: "Mes reservations", path: "/mes-reservations" },
        visual: "step3",
    },
    {
        key: "switch",
        icon: LogIn,
        iconColor: "text-purple-500",
        title: "4. Connectez-vous en tant qu'employe",
        text: "Deconnectez-vous de votre compte, puis connectez-vous avec le compte employe de demo ci-dessous. Ce compte a acces a l'espace d'administration et peut valider les reservations.",
        credentials: { email: "employe@springhotel.fr", password: "Employe971*" },
        action: { label: "Page de connexion", path: "/Connexion" },
        visual: "step4",
    },
    {
        key: "valider",
        icon: ClipboardList,
        iconColor: "text-[#0EA5E9]",
        title: "5. Ouvrez les dossiers a valider",
        text: "Dans l'espace admin, cliquez sur \"Dossiers a valider\". Vous verrez la reservation que vous venez de creer, en attente de signature. Cliquez sur Signer pour ouvrir la page de validation.",
        action: { label: "Dossiers a valider", path: "/admin/reservations/en-attente" },
        visual: "step5",
    },
    {
        key: "signer",
        icon: PenLine,
        iconColor: "text-[#0369A1]",
        title: "6. Apposez votre signature electronique",
        text: "Saisissez votre nom, tracez votre signature dans le canvas, puis confirmez. Le systeme genere un recepisse PDF signe, passe le dossier en CONFIRMEE et notifie le client par email. C'est le coeur du workflow : la validation humaine avec tracabilite.",
        action: null,
        visual: "signature",
    },
    {
        key: "confirme",
        icon: CheckCircle2,
        iconColor: "text-emerald-500",
        title: "7. Verifiez cote client",
        text: "Deconnectez-vous du compte employe, reconnectez-vous avec votre compte personnel, puis allez dans Mes reservations. Votre dossier est desormais CONFIRMEE. La timeline affiche toutes les etapes franchies, et vous pouvez telecharger le recepisse PDF signe.",
        action: { label: "Mes reservations", path: "/mes-reservations" },
        visual: "step7",
    },
    {
        key: "recap",
        icon: Info,
        iconColor: "text-[#0EA5E9]",
        title: "Ce que vous venez de voir",
        text: "Un client soumet une demande. Un agent la valide en apposant sa signature electronique. Le client est notifie et peut telecharger la preuve. Pastell orchestre le tout : suivi de chaque etape, relance automatique sur les pannes, et anomalies signalees quand un humain doit arbitrer. Rien ne se perd en silence.",
        action: null,
        visual: "recap",
    },
];

export default function PastellLifecycleDemo() {
    const [index, setIndex] = useState(0);
    const [playing, setPlaying] = useState(false);
    const timer = useRef(null);

    const scene = SCENES[index];

    useEffect(() => {
        if (!playing) return;
        timer.current = setInterval(() => {
            setIndex((i) => (i + 1) % SCENES.length);
        }, SCENE_MS);
        return () => clearInterval(timer.current);
    }, [playing]);

    const goTo = (i) => {
        setIndex(i);
        if (playing) {
            clearInterval(timer.current);
            timer.current = setInterval(() => setIndex((p) => (p + 1) % SCENES.length), SCENE_MS);
        }
    };

    const restart = () => goTo(0);
    const Icon = scene.icon;

    return (
        <div className="bg-white">
            <style>{`
                @keyframes demoGrow { from { width: 0% } to { width: 100% } }
                @keyframes drawSig {
                    0%   { stroke-dashoffset: 300; }
                    100% { stroke-dashoffset: 0; }
                }
                @keyframes fadeInCheck {
                    0%   { opacity: 0; transform: scale(0.5); }
                    50%  { opacity: 0; transform: scale(0.5); }
                    100% { opacity: 1; transform: scale(1); }
                }
                @keyframes pulseGlow {
                    0%, 100% { opacity: 0.4; }
                    50%      { opacity: 1; }
                }
            `}</style>

            {/* En-tete */}
            <div className="flex items-center justify-between gap-3 mb-1 pr-10">
                <div className="flex items-center gap-2">
                    <Info size={16} className="text-[#0EA5E9]" />
                    <h3 className="text-sm font-semibold text-gray-900">
                        Guide de demonstration
                    </h3>
                </div>
                <div className="flex items-center gap-1">
                    <button onClick={() => setPlaying((p) => !p)} title={playing ? "Pause" : "Lecture automatique"}
                        className="p-1.5 rounded-lg text-gray-500 hover:bg-gray-100 transition-colors">
                        {playing ? <Pause size={15} /> : <Play size={15} />}
                    </button>
                    <button onClick={restart} title="Recommencer"
                        className="p-1.5 rounded-lg text-gray-500 hover:bg-gray-100 transition-colors">
                        <RotateCcw size={15} />
                    </button>
                </div>
            </div>

            {/* Barre de progression */}
            <div className="h-1 w-full bg-gray-100 rounded-full overflow-hidden mb-5">
                <div
                    key={`${index}-${playing}`}
                    className="h-full bg-[#0EA5E9]"
                    style={playing
                        ? { animation: `demoGrow ${SCENE_MS}ms linear forwards` }
                        : { width: `${((index + 1) / SCENES.length) * 100}%`, transition: "width 0.3s" }}
                />
            </div>

            {/* Visuel de la scene */}
            <div className="rounded-2xl border border-gray-100 bg-[#F8FAFC] p-5 mb-4 min-h-[140px] flex items-center justify-center">
                {scene.visual === "signature" ? (
                    <SignatureAnimation key={index} />
                ) : (
                    <StepVisual index={index} />
                )}
            </div>

            {/* Contenu de la scene */}
            <div className="rounded-2xl border border-gray-100 p-4">
                <div className="flex items-center gap-2.5 mb-2">
                    <div className={`w-8 h-8 rounded-lg flex items-center justify-center ${
                        scene.key === "welcome" || scene.key === "recap"
                            ? "bg-sky-50"
                            : "bg-white border border-gray-200"
                    }`}>
                        <Icon size={16} className={scene.iconColor} />
                    </div>
                    <h4 className="text-sm font-semibold text-gray-900">{scene.title}</h4>
                </div>
                <p className="text-sm text-gray-600 leading-relaxed">{scene.text}</p>

                {/* Credentials si necessaire */}
                {scene.credentials && (
                    <div className="mt-3 rounded-xl bg-purple-50 border border-purple-200 px-4 py-3">
                        <p className="text-xs font-semibold text-purple-800 mb-1.5">
                            Identifiants employe de demo
                        </p>
                        <div className="flex flex-col gap-1 text-sm font-mono text-purple-900">
                            <span>Email : {scene.credentials.email}</span>
                            <span>Mot de passe : {scene.credentials.password}</span>
                        </div>
                    </div>
                )}

                {/* Bouton d'action */}
                {scene.action && (
                    <a
                        href={scene.action.path}
                        className="inline-flex items-center gap-2 mt-3 px-3 py-1.5 rounded-lg text-xs font-semibold
                                   bg-[#0EA5E9] text-white hover:bg-[#0369A1] transition"
                    >
                        {scene.action.label}
                        <ArrowRight size={12} />
                    </a>
                )}
            </div>

            {/* Points de navigation */}
            <div className="flex items-center justify-center gap-1.5 mt-4">
                {SCENES.map((s, i) => (
                    <button
                        key={s.key}
                        onClick={() => goTo(i)}
                        aria-label={`Etape ${i + 1}`}
                        className={`h-1.5 rounded-full transition-all ${
                            i === index ? "w-6 bg-[#0EA5E9]" : "w-1.5 bg-gray-200 hover:bg-gray-300"
                        }`}
                    />
                ))}
            </div>

            {/* Navigation prev/next */}
            <div className="flex items-center justify-between mt-4">
                <button
                    onClick={() => goTo(Math.max(0, index - 1))}
                    disabled={index === 0}
                    className="text-xs font-medium text-gray-500 hover:text-[#0EA5E9] disabled:opacity-30 disabled:cursor-not-allowed transition"
                >
                    Precedent
                </button>
                <span className="text-xs text-gray-400">
                    {index + 1} / {SCENES.length}
                </span>
                <button
                    onClick={() => goTo(Math.min(SCENES.length - 1, index + 1))}
                    disabled={index === SCENES.length - 1}
                    className="text-xs font-medium text-gray-500 hover:text-[#0EA5E9] disabled:opacity-30 disabled:cursor-not-allowed transition"
                >
                    Suivant
                </button>
            </div>
        </div>
    );
}

// ============================================================
// Visuels par etape
// ============================================================

function StepVisual({ index }) {
    const steps = [
        { label: "Inscription",  done: index >= 2 },
        { label: "Reservation",  done: index >= 3 },
        { label: "En attente",   done: index >= 4 },
        { label: "Connexion employe", done: index >= 5 },
        { label: "Validation",   done: index >= 6 },
        { label: "Signature",    done: index >= 7 },
        { label: "Confirmee",    done: index >= 8 },
    ];

    // L'etape active correspond a index - 1 (scene 0 = intro, scene 1 = step 0)
    const activeStep = Math.max(0, index - 1);

    return (
        <div className="w-full">
            <div className="flex items-center justify-between gap-1 overflow-x-auto">
                {steps.map((step, i) => {
                    const isActive = i === activeStep && index > 0 && index < SCENES.length - 1;
                    const isDone = step.done;

                    return (
                        <div key={step.label} className="flex items-center gap-1">
                            <div className="flex flex-col items-center gap-1.5 min-w-[64px]">
                                <div className={`w-7 h-7 rounded-full flex items-center justify-center text-xs font-semibold transition-all ${
                                    isDone
                                        ? "bg-emerald-500 text-white"
                                        : isActive
                                            ? "bg-[#0EA5E9] text-white ring-2 ring-[#0EA5E9]/30"
                                            : "bg-gray-200 text-gray-500"
                                }`}>
                                    {isDone ? (
                                        <CheckCircle2 size={14} />
                                    ) : (
                                        i + 1
                                    )}
                                </div>
                                <span className={`text-[10px] text-center leading-tight font-medium ${
                                    isActive ? "text-[#0369A1]" : isDone ? "text-emerald-700" : "text-gray-400"
                                }`}>
                                    {step.label}
                                </span>
                            </div>
                            {i < steps.length - 1 && (
                                <div className={`w-4 h-0.5 mb-4 ${isDone ? "bg-emerald-400" : "bg-gray-200"}`} />
                            )}
                        </div>
                    );
                })}
            </div>
        </div>
    );
}

// ============================================================
// Animation de signature SVG (scene 6)
// ============================================================

function SignatureAnimation() {
    return (
        <div className="flex flex-col items-center gap-4 w-full max-w-sm">
            {/* Canvas simule */}
            <div className="w-full bg-white rounded-xl border-2 border-[#0EA5E9]/40 p-4 relative overflow-hidden">
                <p className="text-[10px] text-gray-400 mb-2 text-center select-none">
                    Signature du valideur
                </p>
                <svg
                    viewBox="0 0 300 80"
                    xmlns="http://www.w3.org/2000/svg"
                    className="w-full h-auto"
                >
                    {/* Trace de signature anime */}
                    <path
                        d="M 20 50 C 40 20, 60 20, 70 40 S 90 70, 110 40 C 120 25, 130 25, 140 40 S 160 65, 175 35 C 185 15, 200 30, 210 45 S 235 60, 260 30"
                        fill="none"
                        stroke="#0f172a"
                        strokeWidth="2"
                        strokeLinecap="round"
                        strokeDasharray="300"
                        strokeDashoffset="300"
                        style={{ animation: "drawSig 2.5s ease-in-out forwards" }}
                    />
                    {/* Petit trait de soulignement */}
                    <path
                        d="M 100 65 L 220 65"
                        fill="none"
                        stroke="#0f172a"
                        strokeWidth="1"
                        strokeLinecap="round"
                        strokeDasharray="120"
                        strokeDashoffset="120"
                        style={{ animation: "drawSig 1s ease-in-out 2.5s forwards" }}
                    />
                </svg>

                {/* Badge de confirmation qui apparait apres la signature */}
                <div
                    className="absolute bottom-2 right-3 flex items-center gap-1"
                    style={{ animation: "fadeInCheck 3.5s ease-out forwards" }}
                >
                    <CheckCircle2 size={14} className="text-emerald-500" />
                    <span className="text-[10px] font-semibold text-emerald-600">Signe</span>
                </div>
            </div>

            {/* Indicateur "PDF genere" */}
            <div
                className="flex items-center gap-2 px-3 py-1.5 rounded-lg bg-emerald-50 border border-emerald-200"
                style={{ animation: "fadeInCheck 4.5s ease-out forwards", opacity: 0 }}
            >
                <span className="text-xs font-medium text-emerald-700">
                    Recepisse PDF genere, client notifie
                </span>
            </div>
        </div>
    );
}