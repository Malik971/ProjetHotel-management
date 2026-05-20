// src/Pages/admin/AdminDocs.jsx

/**
 * Page admin /admin/docs.
 *
 * Page de documentation a destination du recruteur et de toute personne
 * voulant comprendre la demarche, l'architecture et l'API du projet.
 *
 * Quatre onglets :
 *   - Ma demarche  : la posture personnelle de l'auteur (par defaut)
 *   - Architecture : schema d'orchestration et vocabulaire Libriciel
 *   - API          : endpoints admin Pastell avec exemples curl
 *   - Glossaire    : termes metier expliques par audience
 *
 * L'onglet actif est synchronise avec le query param ?tab= pour permettre
 * de partager une URL pointant directement sur un onglet precis.
 */

import { useEffect, useMemo, useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import {
    ArrowLeft,
    User,
    Network,
    Code2,
    BookOpen,
    Github,
    ExternalLink,
    Mail,
    Globe,
} from "lucide-react";

const TABS = [
    { key: "demarche", label: "Ma demarche", icon: User },
    { key: "architecture", label: "Architecture", icon: Network },
    { key: "api", label: "API", icon: Code2 },
    { key: "glossaire", label: "Glossaire", icon: BookOpen },
];

const DEFAULT_TAB = "demarche";

export default function AdminDocs() {
    const navigate = useNavigate();
    const [searchParams, setSearchParams] = useSearchParams();

    const tabFromUrl = searchParams.get("tab");
    const initialTab = TABS.some((t) => t.key === tabFromUrl)
        ? tabFromUrl
        : DEFAULT_TAB;
    const [activeTab, setActiveTab] = useState(initialTab);

    // Synchronise l'URL quand on change d'onglet
    useEffect(() => {
        const current = searchParams.get("tab");
        if (current !== activeTab) {
            const next = new URLSearchParams(searchParams);
            next.set("tab", activeTab);
            setSearchParams(next, { replace: true });
        }
    }, [activeTab, searchParams, setSearchParams]);

    return (
        <div className="min-h-screen bg-[#F8FAFC]">
            <div className="max-w-5xl mx-auto px-4 md:px-8 py-6 md:py-10">

                {/* Retour */}
                <button
                    onClick={() => navigate("/admin")}
                    className="inline-flex items-center gap-2 text-sm text-gray-500 hover:text-[#0EA5E9] mb-4 transition-colors"
                >
                    <ArrowLeft size={16} />
                    Retour au tableau de bord
                </button>

                {/* En-tete */}
                <div className="mb-6 md:mb-8">
                    <h1 className="text-2xl md:text-3xl font-semibold text-gray-900">
                        Documentation du projet
                    </h1>
                    <p className="text-sm text-gray-600 mt-2 max-w-2xl">
                        Cette page rassemble la demarche, l'architecture et l'API du projet SpringHotel.
                        Elle est concue pour etre lue dans l'ordre, en commencant par l'onglet
                        <span className="font-medium text-gray-800"> Ma demarche</span>.
                    </p>
                </div>

                {/* Onglets */}
                <div className="border-b border-gray-200 mb-6 overflow-x-auto">
                    <nav className="flex gap-1" aria-label="Onglets de documentation">
                        {TABS.map((t) => {
                            const Icon = t.icon;
                            const isActive = t.key === activeTab;
                            return (
                                <button
                                    key={t.key}
                                    onClick={() => setActiveTab(t.key)}
                                    className={
                                        isActive
                                            ? "inline-flex items-center gap-2 px-4 py-3 text-sm font-medium text-[#0369A1] border-b-2 border-[#0EA5E9] -mb-px whitespace-nowrap"
                                            : "inline-flex items-center gap-2 px-4 py-3 text-sm font-medium text-gray-500 hover:text-gray-800 border-b-2 border-transparent -mb-px whitespace-nowrap"
                                    }
                                >
                                    <Icon size={16} />
                                    {t.label}
                                </button>
                            );
                        })}
                    </nav>
                </div>

                {/* Contenu de l'onglet actif */}
                <div className="bg-white border border-gray-200 rounded-2xl shadow-sm overflow-hidden">
                    <div className="px-6 md:px-8 py-6 md:py-8">
                        {activeTab === "demarche" && <DemarcheTab />}
                        {activeTab === "architecture" && <ArchitectureTab />}
                        {activeTab === "api" && <ApiTab />}
                        {activeTab === "glossaire" && <GlossaireTab />}
                    </div>
                </div>
            </div>
        </div>
    );
}

// ============================================================
// Onglet 1 : Ma demarche
// ============================================================

function DemarcheTab() {
    return (
        <article className="prose-custom space-y-8">

            <Section title="Qui je suis">
                <p>
                    Je m'appelle <strong>Malik Ibo</strong>. Je suis concepteur developpeur d'applications, formé chez Dawan Montpellier,
                    apres un Bachelor d'Administrateur des Systemes d'Information chez Keyce Academie et un BTS SIO obtenu en Guadeloupe.
                    Durant ce projet, j'ai ete suivi par un enseignant de ma formation Dawan, qui a apporte son regard
                    pedagogique sur mes choix techniques.
                </p>
                <p>
                    Je candidate au poste de DevRel chez Libriciel SCOP.
                </p>
            </Section>

            <Section title="Pourquoi ce projet">
                <p>
                    SpringHotel est mon projet portfolio. Je l'ai demarre il y a environ <strong>un mois et demi</strong>, avec
                    un objectif precis : me confronter, par la pratique, a ce qui fait le coeur de Libriciel. L'interoperabilite,
                    le secteur public, le parapheur electronique, l'orchestration de dossiers entre systemes.
                </p>
                <p>
                    Plutot que d'apprendre Pastell uniquement en lisant la documentation, j'ai prefere construire mon propre bac
                    a sable. Un site d'hotellerie qui transmet ses reservations a un mock Pastell que j'ai developpe moi-meme,
                    en respectant le format snake_case, le workflow document, le polling sur{" "}
                    <code className="font-mono text-xs bg-[#F8FAFC] px-1.5 py-0.5 rounded text-[#0369A1]">journal/since_id_j</code>,
                    et la rotation de credentials. L'idee n'etait pas de produire un logiciel commercial, mais
                    de <strong>comprendre par la fabrication</strong>.
                </p>
            </Section>

            <Section title="Mes choix techniques, en quelques mots">
                <ul className="space-y-3 not-prose">
                    <Bullet title="Mock Pastell maison plutot que connecteur direct.">
                        Je ne voulais pas dependre d'un acces Libriciel pour valider ma logique. Le mock m'a permis de
                        simuler les transitions, les erreurs, les divergences, et de tester mon connecteur de bout en bout.
                    </Bullet>
                    <Bullet title="Multi-module Maven.">
                        Le mock est un service distinct, comme Pastell le serait en production. Cela force a respecter
                        la frontiere reseau et a raisonner en HTTP, pas en appel direct.
                    </Bullet>
                    <Bullet title="Polling 30 secondes plutot que webhooks.">
                        Pastell ne pousse pas, il faut aller chercher. Le polling m'a permis d'implementer la convergence
                        descendante de facon realiste.
                    </Bullet>
                    <Bullet title="Spring est l'autorite metier, Pastell est un satellite.">
                        Le statut d'une reservation cote Sejour ne depend jamais d'une reponse Pastell. C'est une regle
                        que j'ai posee tot et que je n'ai jamais cassee.
                    </Bullet>
                </ul>
            </Section>

            <Section title="Ce que j'assume avoir laisse de cote">
                <p>
                    Je sais que la dimension <em>concepteur</em> du metier ne se resume pas au code.
                    Diagrammes d'utilisation, cas d'usage UML, maquettes Figma, Gantt previsionnel,
                    grille ISO 25010, glossaire metier multi-audiences. <strong>Rien de tout cela n'est dans cette V2.</strong>{" "}
                    C'est un choix.
                </p>
                <p>
                    Pendant ce mois et demi, j'avais des cours en parallele et des missions en interim. J'ai du arbitrer.
                    J'ai choisi de tout mettre sur la <strong>comprehension technique</strong> du metier :
                    lire des forums sur l'interoperabilite administrative, comprendre les niveaux et defis de l'interoperabilite
                    dans le numerique public, m'approprier le vocabulaire (dossier, etape circuit, parapheur, bus
                    d'orchestration), construire mes propres methodes pour les ressentir de l'interieur.
                </p>
                <p>
                    Quand j'ai envoye la V1 il y a une semaine, j'avais deja conscience d'avoir privilegie la fabrication
                    sur la documentation formelle. Je le redis ici, plus posement.
                </p>
                <p>
                    Cote code, des choses ne sont pas finalisees : pas de paiement reel, pas d'email transactionnel,
                    et un compte admin qui a trop de pouvoirs (un admin peut supprimer un autre admin, ce qui n'est pas
                    une bonne pratique metier). Ces points sont identifies, ils ne sont pas oublies.
                </p>
            </Section>

            <Section title="Mon usage de l'IA, sans masquer">
                <p>
                    Je travaille avec des IA pour avancer plus vite. Beaucoup de commentaires dans le code ont ete
                    generes ou co-rediges avec une IA, et je les ai laisses volontairement : ils me servent a me
                    relire et a comprendre rapidement comment une classe s'articule avec les autres. Ils servent
                    aussi a l'IA elle-meme, pour garder le fil quand je reviens sur un fichier apres plusieurs jours.
                </p>
                <p>
                    Je connais les limites de cet usage. Je sais qu'on retient moins quand on delegue trop. C'est pour
                    ca que je redige a la main les definitions du vocabulaire technique du metier, que je lis des blogs
                    et des forums, et que je note les concepts pour les apprendre durablement. L'IA est un outil
                    de <strong>velocite</strong>, pas un substitut d'apprentissage.
                </p>
            </Section>

            <Section title="Pour aller plus loin">
                <div className="not-prose space-y-2 text-sm">
                    <p className="text-gray-700">
                        Si vous voulez prolonger la lecture, voici les liens utiles :
                    </p>
                    <div className="flex flex-col gap-2 mt-3">
                        <a
                            href="https://malik-ibo.netlify.app/"
                            target="_blank"
                            rel="noopener noreferrer"
                            className="inline-flex items-center gap-2 text-[#0369A1] hover:text-[#0EA5E9] hover:underline"
                        >
                            <Globe size={14} />
                            Mon portfolio : malik-ibo.netlify.app
                            <ExternalLink size={12} />
                        </a>
                    </div>
                    <p className="text-xs text-gray-500 mt-4">
                        Malik Ibo, candidat au poste de DevRel chez Libriciel SCOP.
                    </p>
                </div>
            </Section>
        </article>
    );
}

function Section({ title, children }) {
    return (
        <section>
            <h2 className="text-lg font-semibold text-gray-900 mb-3">{title}</h2>
            <div className="space-y-3 text-sm text-gray-700 leading-relaxed">
                {children}
            </div>
        </section>
    );
}

function Bullet({ title, children }) {
    return (
        <li className="flex gap-3">
            <span className="flex-shrink-0 mt-1.5 w-1.5 h-1.5 rounded-full bg-[#0EA5E9]" />
            <div>
                <span className="font-medium text-gray-900">{title}</span>{" "}
                <span className="text-gray-700">{children}</span>
            </div>
        </li>
    );
}

// ============================================================
// Onglet 2 : Architecture
// ============================================================

function ArchitectureTab() {
    return (
        <div className="space-y-8">
            <section>
                <h2 className="text-lg font-semibold text-gray-900 mb-3">
                    Vue d'ensemble du flux
                </h2>
                <p className="text-sm text-gray-700 leading-relaxed mb-6">
                    Trois services distincts cohabitent dans ce projet. Le frontend React parle au backend
                    Sejour, qui orchestre les dossiers vers un mock Pastell deploye comme un service satellite.
                    La frontiere entre Sejour et Pastell est strictement HTTP, comme elle le serait avec une
                    vraie instance Libriciel.
                </p>

                <div className="bg-[#F8FAFC] border border-gray-200 rounded-xl p-6 overflow-x-auto">
                    <ArchitectureSvg />
                </div>
            </section>

            <section>
                <h2 className="text-lg font-semibold text-gray-900 mb-3">
                    Les trois moments cles
                </h2>
                <div className="space-y-4 text-sm text-gray-700">
                    <FlowItem
                        index="1"
                        title="Creation montante (Sejour vers Pastell)"
                    >
                        Quand un client cree une reservation cote Sejour, un evenement{" "}
                        <code className="font-mono text-xs bg-[#F8FAFC] px-1.5 py-0.5 rounded text-[#0369A1]">ReservationCreatedEvent</code>{" "}
                        declenche un appel HTTP vers Pastell pour creer un dossier. Le mock renvoie un{" "}
                        <code className="font-mono text-xs bg-[#F8FAFC] px-1.5 py-0.5 rounded text-[#0369A1]">id_d</code>{" "}
                        que Sejour persiste dans la table <code className="font-mono text-xs bg-[#F8FAFC] px-1.5 py-0.5 rounded text-[#0369A1]">pastell_sync</code>.
                    </FlowItem>
                    <FlowItem
                        index="2"
                        title="Polling descendant (Pastell vers Sejour)"
                    >
                        Toutes les 30 secondes, Sejour interroge l'endpoint{" "}
                        <code className="font-mono text-xs bg-[#F8FAFC] px-1.5 py-0.5 rounded text-[#0369A1]">GET /api/v2/journal?since_id_j=N</code>{" "}
                        du mock pour recuperer les nouvelles entrees de journal. Un curseur en base
                        garantit qu'on ne retraite jamais une entree deja vue.
                    </FlowItem>
                    <FlowItem
                        index="3"
                        title="Relance sur anomalie"
                    >
                        Quand un dossier echoue (timeout, 5xx, divergence), il passe en statut{" "}
                        <code className="font-mono text-xs bg-[#F8FAFC] px-1.5 py-0.5 rounded text-[#0369A1]">EN_RETRY</code>{" "}
                        puis <code className="font-mono text-xs bg-[#F8FAFC] px-1.5 py-0.5 rounded text-[#0369A1]">EN_ERREUR</code>.
                        Un scheduler de retraitement les rejoue en FIFO, et l'admin peut forcer une relance manuelle
                        depuis la page <Link to="/admin/pastell" className="text-[#0369A1] hover:text-[#0EA5E9] hover:underline">Dossiers Pastell</Link>.
                    </FlowItem>
                </div>
            </section>

            <section>
                <h2 className="text-lg font-semibold text-gray-900 mb-3">
                    Regle d'or : Spring est l'autorite metier
                </h2>
                <div className="bg-amber-50 border border-amber-200 rounded-xl p-4 text-sm text-amber-900 leading-relaxed">
                    Le statut metier d'une reservation (EN_ATTENTE, CONFIRMEE, ANNULEE, TERMINEE) est
                    decide cote Spring, jamais cote Pastell. Pastell est un satellite : il enregistre,
                    il transmet, il signale. Il ne dicte pas le metier. Cette regle est inscrite dans
                    le code via la separation stricte entre <code className="font-mono text-xs">Reservation.statut</code>{" "}
                    (autorite) et <code className="font-mono text-xs">PastellSync.syncStatus</code> (suivi technique).
                </div>
            </section>
        </div>
    );
}

function FlowItem({ index, title, children }) {
    return (
        <div className="flex gap-4">
            <div className="flex-shrink-0 w-8 h-8 rounded-full bg-[#0EA5E9] text-white text-sm font-semibold flex items-center justify-center">
                {index}
            </div>
            <div className="flex-1 pt-1">
                <h3 className="text-sm font-semibold text-gray-900 mb-1">{title}</h3>
                <p className="text-sm text-gray-700 leading-relaxed">{children}</p>
            </div>
        </div>
    );
}

/**
 * Schema d'architecture en SVG inline.
 * Trois boites (Frontend, Sejour, Pastell Mock) avec trois fleches annotees.
 */
function ArchitectureSvg() {
    return (
        <svg
            viewBox="0 0 800 360"
            xmlns="http://www.w3.org/2000/svg"
            className="w-full h-auto"
            role="img"
            aria-label="Schema d'architecture du projet"
        >
            <defs>
                <marker
                    id="arrowhead"
                    markerWidth="10"
                    markerHeight="7"
                    refX="9"
                    refY="3.5"
                    orient="auto"
                >
                    <polygon points="0 0, 10 3.5, 0 7" fill="#0EA5E9" />
                </marker>
                <marker
                    id="arrowhead-amber"
                    markerWidth="10"
                    markerHeight="7"
                    refX="9"
                    refY="3.5"
                    orient="auto"
                >
                    <polygon points="0 0, 10 3.5, 0 7" fill="#F59E0B" />
                </marker>
            </defs>

            {/* Service 1 : Frontend */}
            <g>
                <rect
                    x="30"
                    y="140"
                    width="180"
                    height="80"
                    rx="12"
                    fill="white"
                    stroke="#0EA5E9"
                    strokeWidth="2"
                />
                <text x="120" y="170" textAnchor="middle" fontSize="14" fontWeight="600" fill="#0369A1">
                    Frontend React
                </text>
                <text x="120" y="190" textAnchor="middle" fontSize="11" fill="#64748B">
                    Netlify
                </text>
                <text x="120" y="208" textAnchor="middle" fontSize="11" fill="#64748B">
                    hotel-montpellier
                </text>
            </g>

            {/* Service 2 : Sejour backend */}
            <g>
                <rect
                    x="310"
                    y="140"
                    width="180"
                    height="80"
                    rx="12"
                    fill="white"
                    stroke="#0EA5E9"
                    strokeWidth="2"
                />
                <text x="400" y="170" textAnchor="middle" fontSize="14" fontWeight="600" fill="#0369A1">
                    Sejour backend
                </text>
                <text x="400" y="190" textAnchor="middle" fontSize="11" fill="#64748B">
                    Spring Boot 4 + JPA
                </text>
                <text x="400" y="208" textAnchor="middle" fontSize="11" fill="#64748B">
                    Render + PostgreSQL
                </text>
            </g>

            {/* Service 3 : Pastell Mock */}
            <g>
                <rect
                    x="590"
                    y="140"
                    width="180"
                    height="80"
                    rx="12"
                    fill="white"
                    stroke="#F59E0B"
                    strokeWidth="2"
                />
                <text x="680" y="170" textAnchor="middle" fontSize="14" fontWeight="600" fill="#B45309">
                    Mock Pastell
                </text>
                <text x="680" y="190" textAnchor="middle" fontSize="11" fill="#64748B">
                    Service satellite
                </text>
                <text x="680" y="208" textAnchor="middle" fontSize="11" fill="#64748B">
                    Render, port 8090
                </text>
            </g>

            {/* Fleche 1 : Frontend vers Sejour (HTTPS + JWT) */}
            <line
                x1="210"
                y1="170"
                x2="310"
                y2="170"
                stroke="#0EA5E9"
                strokeWidth="2"
                markerEnd="url(#arrowhead)"
            />
            <text x="260" y="160" textAnchor="middle" fontSize="11" fill="#0369A1" fontWeight="500">
                HTTPS + JWT
            </text>

            {/* Fleche 2 : Sejour vers Pastell (creation montante) */}
            <line
                x1="490"
                y1="160"
                x2="590"
                y2="160"
                stroke="#0EA5E9"
                strokeWidth="2"
                markerEnd="url(#arrowhead)"
            />
            <text x="540" y="150" textAnchor="middle" fontSize="11" fill="#0369A1" fontWeight="500">
                creer dossier
            </text>

            {/* Fleche 3 : Pastell vers Sejour (polling descendant) */}
            <line
                x1="590"
                y1="200"
                x2="490"
                y2="200"
                stroke="#F59E0B"
                strokeWidth="2"
                markerEnd="url(#arrowhead-amber)"
            />
            <text x="540" y="220" textAnchor="middle" fontSize="11" fill="#B45309" fontWeight="500">
                polling journal 30s
            </text>

            {/* Legendes en bas */}
            <g transform="translate(40, 290)">
                <line x1="0" y1="6" x2="30" y2="6" stroke="#0EA5E9" strokeWidth="2" />
                <text x="40" y="10" fontSize="11" fill="#475569">
                    Flux montant (creation, validation, mise a jour)
                </text>
            </g>
            <g transform="translate(40, 320)">
                <line x1="0" y1="6" x2="30" y2="6" stroke="#F59E0B" strokeWidth="2" />
                <text x="40" y="10" fontSize="11" fill="#475569">
                    Flux descendant (convergence par polling)
                </text>
            </g>
        </svg>
    );
}

// ============================================================
// Onglet 3 : API
// ============================================================

const API_GROUPS = [
    {
        title: "Etat global du bus Pastell",
        endpoints: [
            {
                method: "GET",
                path: "/api/admin/pastell/status",
                description: "Snapshot global : compteurs par statut, curseur de polling, ping du mock.",
                curl: `curl -H "Authorization: Bearer $TOKEN" \\
  https://projethotel-management.onrender.com/api/admin/pastell/status`,
            },
            {
                method: "GET",
                path: "/api/admin/pastell/cursor",
                description: "Curseur de polling courant (dernier id_j traite, date du dernier poll).",
                curl: `curl -H "Authorization: Bearer $TOKEN" \\
  https://projethotel-management.onrender.com/api/admin/pastell/cursor`,
            },
            {
                method: "POST",
                path: "/api/admin/pastell/poll",
                description: "Force un cycle de polling immediat. Necessite le header X-Demo-Token.",
                curl: `curl -X POST \\
  -H "Authorization: Bearer $TOKEN" \\
  -H "X-Demo-Token: $DEMO_TOKEN" \\
  https://projethotel-management.onrender.com/api/admin/pastell/poll`,
            },
        ],
    },
    {
        title: "Dossiers et journal",
        endpoints: [
            {
                method: "GET",
                path: "/api/admin/pastell-sync?status=&page=&size=",
                description: "Page de dossiers, filtrable par statut. Renvoie un PagedResponseDTO.",
                curl: `curl -H "Authorization: Bearer $TOKEN" \\
  "https://projethotel-management.onrender.com/api/admin/pastell-sync?status=EN_ERREUR&page=0&size=20"`,
            },
            {
                method: "GET",
                path: "/api/admin/pastell-sync/{syncId}/journal",
                description: "Journal d'orchestration d'un dossier, ordonne du plus ancien au plus recent.",
                curl: `curl -H "Authorization: Bearer $TOKEN" \\
  https://projethotel-management.onrender.com/api/admin/pastell-sync/42/journal`,
            },
            {
                method: "POST",
                path: "/api/admin/pastell-sync/{syncId}/retry",
                description: "Relance manuelle d'un dossier en anomalie. Necessite X-Demo-Token.",
                curl: `curl -X POST \\
  -H "Authorization: Bearer $TOKEN" \\
  -H "X-Demo-Token: $DEMO_TOKEN" \\
  https://projethotel-management.onrender.com/api/admin/pastell-sync/42/retry`,
            },
            {
                method: "GET",
                path: "/api/admin/activity?limit=10",
                description: "Flux d'activite recente du bus, alimente par le journal Pastell.",
                curl: `curl -H "Authorization: Bearer $TOKEN" \\
  "https://projethotel-management.onrender.com/api/admin/activity?limit=10"`,
            },
        ],
    },
    {
        title: "Utilisateurs",
        endpoints: [
            {
                method: "GET",
                path: "/api/admin/users",
                description: "Liste de tous les utilisateurs avec leurs roles.",
                curl: `curl -H "Authorization: Bearer $TOKEN" \\
  https://projethotel-management.onrender.com/api/admin/users`,
            },
            {
                method: "POST",
                path: "/api/admin/users?role=USER|EMPLOYE|ADMIN",
                description: "Cree un utilisateur. Le role est obligatoire en query param.",
                curl: `curl -X POST \\
  -H "Authorization: Bearer $TOKEN" \\
  -H "Content-Type: application/json" \\
  -d '{"email":"nouveau@example.fr","password":"pass1234","firstName":"Jean","lastName":"Dupont"}' \\
  "https://projethotel-management.onrender.com/api/admin/users?role=USER"`,
            },
            {
                method: "DELETE",
                path: "/api/admin/users/{id}",
                description: "Supprime un utilisateur par son identifiant.",
                curl: `curl -X DELETE \\
  -H "Authorization: Bearer $TOKEN" \\
  https://projethotel-management.onrender.com/api/admin/users/42`,
            },
        ],
    },
];

function ApiTab() {
    return (
        <div className="space-y-8">
            <section>
                <h2 className="text-lg font-semibold text-gray-900 mb-3">
                    Endpoints admin
                </h2>
                <p className="text-sm text-gray-700 leading-relaxed">
                    Tous les endpoints ci-dessous requierent un token JWT obtenu via{" "}
                    <code className="font-mono text-xs bg-[#F8FAFC] px-1.5 py-0.5 rounded text-[#0369A1]">POST /api/auth/login</code>{" "}
                    avec un compte de role <code className="font-mono text-xs">ROLE_ADMIN</code>. Les operations destructives
                    (force poll, retry) demandent en plus le header{" "}
                    <code className="font-mono text-xs bg-[#F8FAFC] px-1.5 py-0.5 rounded text-[#0369A1]">X-Demo-Token</code>{" "}
                    pour eviter les manipulations involontaires depuis l'exterieur.
                </p>
            </section>

            {API_GROUPS.map((group) => (
                <section key={group.title}>
                    <h3 className="text-base font-semibold text-gray-900 mb-4">
                        {group.title}
                    </h3>
                    <div className="space-y-4">
                        {group.endpoints.map((ep, idx) => (
                            <EndpointCard key={idx} endpoint={ep} />
                        ))}
                    </div>
                </section>
            ))}
        </div>
    );
}

function EndpointCard({ endpoint }) {
    const methodConfig = {
        GET: "bg-emerald-50 text-emerald-700 border-emerald-200",
        POST: "bg-sky-50 text-[#0369A1] border-sky-200",
        DELETE: "bg-red-50 text-red-700 border-red-200",
        PUT: "bg-amber-50 text-amber-800 border-amber-200",
    }[endpoint.method] || "bg-gray-50 text-gray-700 border-gray-200";

    return (
        <div className="border border-gray-200 rounded-xl overflow-hidden">
            <div className="px-4 py-3 bg-[#F8FAFC] border-b border-gray-200">
                <div className="flex items-center gap-3 flex-wrap">
                    <span
                        className={`inline-block px-2 py-0.5 rounded text-xs font-semibold border ${methodConfig}`}
                    >
                        {endpoint.method}
                    </span>
                    <code className="font-mono text-sm text-gray-800 break-all">
                        {endpoint.path}
                    </code>
                </div>
                <p className="text-xs text-gray-600 mt-2">{endpoint.description}</p>
            </div>
            <pre className="px-4 py-3 text-xs font-mono text-gray-700 bg-white overflow-x-auto whitespace-pre">
                {endpoint.curl}
            </pre>
        </div>
    );
}

// ============================================================
// Onglet 4 : Glossaire
// ============================================================

const GLOSSARY = [
    {
        term: "Dossier",
        audiences: ["Dev", "Admin"],
        def: "Unite de circulation dans Pastell. Chez Libriciel, c'est l'entite qui transite entre services (parapheur, GED, SAE). Dans notre projet, un dossier = une reservation transmise au mock.",
    },
    {
        term: "Bus d'orchestration",
        audiences: ["Dev", "Direction"],
        def: "Composant central qui route les dossiers entre les briques metier. Pastell joue ce role pour les collectivites territoriales. Il ne fait pas de metier lui-meme, il oriente.",
    },
    {
        term: "Etape circuit",
        audiences: ["Dev", "Admin"],
        def: "Position courante d'un dossier dans son cycle de vie Pastell (creation, en-attente-validation, validee, confirmee, terminee, annulee). C'est l'equivalent technique d'un statut workflow.",
    },
    {
        term: "Parapheur electronique",
        audiences: ["Client", "Direction"],
        def: "Solution logicielle qui permet a un agent autorise de signer et valider numeriquement un document avec la meme valeur juridique qu'une signature manuscrite.",
    },
    {
        term: "Interoperabilite",
        audiences: ["Dev", "Direction", "Commercial"],
        def: "Capacite de plusieurs systemes a echanger des informations de maniere fluide, structuree et sans intervention humaine. Au coeur du metier de Libriciel.",
    },
    {
        term: "SCOP",
        audiences: ["Direction", "Commercial"],
        def: "Societe Cooperative et Participative. Forme juridique de Libriciel : les salaries sont associes et participent aux decisions strategiques de l'entreprise.",
    },
    {
        term: "Anomalie",
        audiences: ["Admin", "Dev"],
        def: "Vocabulaire prefere a 'erreur'. Une anomalie est un etat d'un dossier qui necessite une intervention (timeout, divergence, refus du parapheur). Le mot 'erreur' est evite car il a une connotation de defaut produit.",
    },
    {
        term: "Relance",
        audiences: ["Admin"],
        def: "Action de remettre en file d'attente un dossier en anomalie pour qu'il soit retraite par le bus. Equivalent metier d'un 'retry' technique.",
    },
    {
        term: "Convergence",
        audiences: ["Dev", "Admin"],
        def: "Etat ou l'image qu'un service satellite (Sejour) a d'un dossier correspond a la realite chez Pastell. La divergence est l'inverse : les deux systemes ne sont plus alignes.",
    },
    {
        term: "Satellite",
        audiences: ["Dev"],
        def: "Service applicatif qui consomme ou alimente Pastell sans avoir d'autorite metier dessus. Dans ce projet, Sejour est satellite, Pastell est central.",
    },
    {
        term: "id_d",
        audiences: ["Dev"],
        def: "Identifiant d'un document Pastell. Retourne par l'endpoint de creation, persiste cote Sejour pour permettre la convergence ulterieure.",
    },
    {
        term: "id_j",
        audiences: ["Dev"],
        def: "Identifiant monotone d'une entree de journal Pastell. Sert de curseur pour le polling : on demande toutes les entrees apres le dernier id_j connu.",
    },
    {
        term: "Polling",
        audiences: ["Dev"],
        def: "Strategie ou un service va periodiquement chercher des nouvelles aupres d'un autre service, plutot que d'attendre qu'on lui en envoie. Pastell ne pousse pas, donc Sejour interroge toutes les 30 secondes.",
    },
    {
        term: "Rotation de credentials",
        audiences: ["Dev", "Admin"],
        def: "Mecanisme par lequel un client et un serveur partagent un secret commun, et derivent a la volee des identifiants temporaires sans avoir besoin d'echanger un mot de passe statique. Implemente dans le mock via PASTELL_MASTER_SECRET.",
    },
    {
        term: "JWT",
        audiences: ["Dev"],
        def: "JSON Web Token. Format standard de token signe utilise par Sejour pour authentifier les requetes API. Inclut le role de l'utilisateur, ce qui permet aux endpoints d'autoriser ou refuser sans relire la base.",
    },
    {
        term: "Mock",
        audiences: ["Dev", "Direction"],
        def: "Implementation simplifiee d'un service tiers, utilisee pour developper et tester sans dependre du vrai service. Dans ce projet, le pastell-mock simule un Pastell reel sans avoir besoin d'une instance Libriciel.",
    },
    {
        term: "Idempotence",
        audiences: ["Dev"],
        def: "Propriete d'une operation qui, appelee plusieurs fois, produit le meme effet qu'un seul appel. Critique pour la creation de dossiers : si le reseau echoue et qu'on rejoue, on ne veut pas creer deux dossiers en double.",
    },
    {
        term: "Profil Spring",
        audiences: ["Dev"],
        def: "Mecanisme Spring Boot qui permet de charger des configurations differentes selon l'environnement (dev, test, prod). Le profil 'prod' active Flyway en mode validate, des origines CORS restreintes, et la rotation de credentials Pastell.",
    },
];

function GlossaireTab() {
    const groupedByAudience = useMemo(() => GLOSSARY, []);

    const audienceColors = {
        Client: "bg-emerald-50 text-emerald-700",
        Dev: "bg-sky-50 text-[#0369A1]",
        Admin: "bg-amber-50 text-amber-800",
        Direction: "bg-purple-50 text-purple-700",
        Commercial: "bg-pink-50 text-pink-700",
    };

    return (
        <div className="space-y-6">
            <section>
                <h2 className="text-lg font-semibold text-gray-900 mb-3">
                    Glossaire metier
                </h2>
                <p className="text-sm text-gray-700 leading-relaxed mb-2">
                    Une selection de termes utilises dans le projet et au sein du secteur de Libriciel.
                    Chaque terme est etiquete par audience : a qui ce mot parle en priorite.
                </p>
                <div className="flex flex-wrap gap-2 mt-3">
                    {Object.entries(audienceColors).map(([audience, classes]) => (
                        <span
                            key={audience}
                            className={`inline-block px-2 py-0.5 rounded-full text-[10px] font-semibold uppercase tracking-wider ${classes}`}
                        >
                            {audience}
                        </span>
                    ))}
                </div>
            </section>

            <section className="space-y-3">
                {groupedByAudience.map((entry) => (
                    <div
                        key={entry.term}
                        className="border border-gray-200 rounded-xl p-4 hover:border-[#0EA5E9] transition-colors"
                    >
                        <div className="flex items-center gap-2 flex-wrap mb-2">
                            <h3 className="text-base font-semibold text-gray-900">
                                {entry.term}
                            </h3>
                            {entry.audiences.map((a) => (
                                <span
                                    key={a}
                                    className={`inline-block px-2 py-0.5 rounded-full text-[10px] font-semibold uppercase tracking-wider ${audienceColors[a] || "bg-gray-100 text-gray-700"}`}
                                >
                                    {a}
                                </span>
                            ))}
                        </div>
                        <p className="text-sm text-gray-700 leading-relaxed">
                            {entry.def}
                        </p>
                    </div>
                ))}
            </section>
        </div>
    );
}