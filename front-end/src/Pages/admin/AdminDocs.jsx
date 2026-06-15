// src/Pages/admin/AdminDocs.jsx

/**
 * Page admin /admin/docs.
 *
 * Page de documentation a destination du recruteur et de toute personne
 * voulant comprendre la demarche, l'architecture et l'API du projet.
 *
 * Cinq onglets :
 *   - Ma demarche  : la posture personnelle de l'auteur (par defaut)
 *   - Architecture : schema d'orchestration et vocabulaire Libriciel
 *   - Securite     : OAuth2, OpenID Connect, Keycloak (ajout K5)
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
    ExternalLink,
    Globe,
    ShieldCheck,
} from "lucide-react";

const TABS = [
    { key: "demarche",     label: "Ma demarche", icon: User },
    { key: "architecture", label: "Architecture", icon: Network },
    { key: "securite",     label: "Securite",     icon: ShieldCheck },
    { key: "api",          label: "API",           icon: Code2 },
    { key: "glossaire",    label: "Glossaire",     icon: BookOpen },
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

                <button
                    onClick={() => navigate("/admin")}
                    className="inline-flex items-center gap-2 text-sm text-gray-500 hover:text-[#0EA5E9] mb-4 transition-colors"
                >
                    <ArrowLeft size={16} />
                    Retour au tableau de bord
                </button>

                <div className="mb-6 md:mb-8">
                    <h1 className="text-2xl md:text-3xl font-semibold text-gray-900">
                        Documentation du projet
                    </h1>
                    <p className="text-sm text-gray-600 mt-2 max-w-2xl">
                        Cette page rassemble la demarche, l'architecture, la securite et l'API du projet SpringHotel.
                        Elle est concue pour etre lue dans l'ordre, en commencant par l'onglet
                        <span className="font-medium text-gray-800"> Ma demarche</span>.
                    </p>
                </div>

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

                <div className="bg-white border border-gray-200 rounded-2xl shadow-sm overflow-hidden">
                    <div className="px-6 md:px-8 py-6 md:py-8">
                        {activeTab === "demarche"     && <DemarcheTab />}
                        {activeTab === "architecture" && <ArchitectureTab />}
                        {activeTab === "securite"     && <SecuriteTab />}
                        {activeTab === "api"          && <ApiTab />}
                        {activeTab === "glossaire"    && <GlossaireTab />}
                    </div>
                </div>
            </div>
        </div>
    );
}

// ============================================================
// Composants partages
// ============================================================

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

function Code({ children }) {
    return (
        <code className="font-mono text-xs bg-[#F8FAFC] px-1.5 py-0.5 rounded text-[#0369A1]">
            {children}
        </code>
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
                    Je m'appelle <strong>Malik Ibo</strong>. Je suis concepteur developpeur d'applications, forme chez Dawan Montpellier,
                    apres un Bachelor d'Administrateur des Systemes d'Information chez Keyce Academie et un BTS SIO obtenu en Guadeloupe.
                    Durant ce projet, j'ai ete suivi par un enseignant de ma formation Dawan, qui a apporte son regard
                    pedagogique sur mes choix techniques.
                </p>
            </Section>

            <Section title="Pourquoi ce projet">
                <p>
                    SpringHotel est mon projet portfolio. Je l'ai demarre avec un objectif precis : me confronter,
                    par la pratique, a ce qui fait le coeur de Libriciel. L'interoperabilite,
                    le secteur public, le parapheur electronique, l'orchestration de dossiers entre systemes.
                </p>
                <p>
                    Plutot que d'apprendre Pastell uniquement en lisant la documentation, j'ai prefere construire mon propre bac
                    a sable. Un site d'hotellerie qui transmet ses reservations a un mock Pastell que j'ai developpe moi-meme,
                    en respectant le format snake_case, le workflow document, le polling sur{" "}
                    <Code>journal/since_id_j</Code>,
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
                    <Bullet title="Keycloak pour l'authentification OAuth2 / OpenID Connect.">
                        La fiche de poste mentionne explicitement OAuth2 et OpenID Connect. iparapheur v5 s'appuie sur
                        Keycloak. J'ai donc integre Keycloak pour demontrer cette competence par la pratique : realm
                        configure, client PKCE, Resource Server Spring Boot, coexistence avec le JWT maison existant.
                        L'onglet <strong>Securite</strong> detaille cette integration.
                    </Bullet>
                </ul>
            </Section>

            <Section title="Ce que j'assume avoir laisse de cote">
                <p>
                    Je sais que la dimension <em>concepteur</em> du metier ne se resume pas au code.
                    Diagrammes d'utilisation, cas d'usage UML, maquettes Figma, Gantt previsionnel,
                    grille ISO 25010, glossaire metier multi-audiences. <strong>Rien de tout cela n'est dans ce projet.</strong>{" "}
                    C'est un choix delibere.
                </p>
                <p>
                    J'ai choisi de tout mettre sur la <strong>comprehension technique</strong> du metier :
                    lire des forums sur l'interoperabilite administrative, comprendre les niveaux et defis de l'interoperabilite
                    dans le numerique public, m'approprier le vocabulaire (dossier, etape circuit, parapheur, bus
                    d'orchestration), construire mes propres methodes pour les ressentir de l'interieur.
                </p>
                <p>
                    Cote code, des choses ne sont pas finalisees : pas de paiement reel, pas d'email transactionnel,
                    et un compte admin qui a trop de pouvoirs. Ces points sont identifies, ils ne sont pas oublies.
                </p>
            </Section>

            <Section title="Mon usage de l'IA, sans masquer">
                <p>
                    Je travaille avec des IA pour avancer plus vite. Beaucoup de commentaires dans le code ont ete
                    generes ou co-rediges avec une IA, et je les ai laisses volontairement : ils me servent a me
                    relire et a comprendre rapidement comment une classe s'articule avec les autres.
                </p>
                <p>
                    Je connais les limites de cet usage. Je sais qu'on retient moins quand on delege trop. C'est pour
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
                        Malik Ibo, Concepteur développeur d'application.
                    </p>
                </div>
            </Section>
        </article>
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
                    Quatre services distincts cohabitent dans ce projet. Le frontend React s'authentifie
                    aupres de Keycloak, puis parle au backend Sejour. Sejour orchestre les dossiers vers
                    un mock Pastell deploye comme service satellite. La frontiere entre Sejour et Pastell
                    est strictement HTTP, comme elle le serait avec une vraie instance Libriciel.
                </p>

                <div className="bg-[#F8FAFC] border border-gray-200 rounded-xl p-6 overflow-x-auto">
                    <ArchitectureSvg />
                </div>
            </section>

            <section>
                <h2 className="text-lg font-semibold text-gray-900 mb-3">
                    Les trois moments cles (flux Pastell)
                </h2>
                <div className="space-y-4 text-sm text-gray-700">
                    <FlowItem index="1" title="Creation montante (Sejour vers Pastell)">
                        Quand un client cree une reservation cote Sejour, un evenement{" "}
                        <Code>ReservationCreatedEvent</Code>{" "}
                        declenche un appel HTTP vers Pastell pour creer un dossier. Le mock renvoie un{" "}
                        <Code>id_d</Code>{" "}
                        que Sejour persiste dans la table <Code>pastell_sync</Code>.
                    </FlowItem>
                    <FlowItem index="2" title="Polling descendant (Pastell vers Sejour)">
                        Toutes les 30 secondes, Sejour interroge l'endpoint{" "}
                        <Code>GET /api/v2/journal?since_id_j=N</Code>{" "}
                        du mock pour recuperer les nouvelles entrees de journal. Un curseur en base
                        garantit qu'on ne retraite jamais une entree deja vue.
                    </FlowItem>
                    <FlowItem index="3" title="Relance sur anomalie">
                        Quand un dossier echoue (timeout, 5xx, divergence), il passe en statut{" "}
                        <Code>EN_RETRY</Code>{" "}
                        puis <Code>EN_ERREUR</Code>.
                        Un scheduler de retraitement les rejoue en FIFO, et l'admin peut forcer une relance manuelle
                        depuis la page{" "}
                        <Link to="/admin/pastell" className="text-[#0369A1] hover:text-[#0EA5E9] hover:underline">
                            Dossiers Pastell
                        </Link>.
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
                    le code via la separation stricte entre <Code>Reservation.statut</Code>{" "}
                    (autorite) et <Code>PastellSync.syncStatus</Code> (suivi technique).
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
 * Quatre services : Keycloak (haut centre), Frontend (gauche),
 * Sejour backend (centre), Mock Pastell (droite).
 */
function ArchitectureSvg() {
    return (
        <svg
            viewBox="0 0 800 420"
            xmlns="http://www.w3.org/2000/svg"
            className="w-full h-auto"
            role="img"
            aria-label="Schema d'architecture du projet avec Keycloak"
        >
            <defs>
                <marker id="arr-blue" markerWidth="10" markerHeight="7" refX="9" refY="3.5" orient="auto">
                    <polygon points="0 0, 10 3.5, 0 7" fill="#0EA5E9" />
                </marker>
                <marker id="arr-amber" markerWidth="10" markerHeight="7" refX="9" refY="3.5" orient="auto">
                    <polygon points="0 0, 10 3.5, 0 7" fill="#F59E0B" />
                </marker>
                <marker id="arr-purple" markerWidth="10" markerHeight="7" refX="9" refY="3.5" orient="auto">
                    <polygon points="0 0, 10 3.5, 0 7" fill="#8B5CF6" />
                </marker>
            </defs>

            {/* Keycloak : en haut, centre */}
            <g>
                <rect x="300" y="20" width="200" height="70" rx="12"
                    fill="white" stroke="#8B5CF6" strokeWidth="2" />
                <text x="400" y="48" textAnchor="middle" fontSize="13" fontWeight="600" fill="#6D28D9">
                    Keycloak
                </text>
                <text x="400" y="65" textAnchor="middle" fontSize="11" fill="#64748B">
                    OAuth2 / OIDC
                </text>
                <text x="400" y="80" textAnchor="middle" fontSize="11" fill="#64748B">
                    realm springhotel
                </text>
            </g>

            {/* Frontend React : milieu gauche */}
            <g>
                <rect x="30" y="190" width="170" height="75" rx="12"
                    fill="white" stroke="#0EA5E9" strokeWidth="2" />
                <text x="115" y="218" textAnchor="middle" fontSize="13" fontWeight="600" fill="#0369A1">
                    Frontend React
                </text>
                <text x="115" y="235" textAnchor="middle" fontSize="11" fill="#64748B">
                    Vite + Tailwind v4
                </text>
                <text x="115" y="252" textAnchor="middle" fontSize="11" fill="#64748B">
                    Netlify
                </text>
            </g>

            {/* Sejour backend : milieu centre */}
            <g>
                <rect x="300" y="190" width="200" height="75" rx="12"
                    fill="white" stroke="#0EA5E9" strokeWidth="2" />
                <text x="400" y="218" textAnchor="middle" fontSize="13" fontWeight="600" fill="#0369A1">
                    Sejour backend
                </text>
                <text x="400" y="235" textAnchor="middle" fontSize="11" fill="#64748B">
                    Spring Boot 4 + JPA
                </text>
                <text x="400" y="252" textAnchor="middle" fontSize="11" fill="#64748B">
                    Render + PostgreSQL
                </text>
            </g>

            {/* Mock Pastell : milieu droite */}
            <g>
                <rect x="600" y="190" width="170" height="75" rx="12"
                    fill="white" stroke="#F59E0B" strokeWidth="2" />
                <text x="685" y="218" textAnchor="middle" fontSize="13" fontWeight="600" fill="#B45309">
                    Mock Pastell
                </text>
                <text x="685" y="235" textAnchor="middle" fontSize="11" fill="#64748B">
                    Service satellite
                </text>
                <text x="685" y="252" textAnchor="middle" fontSize="11" fill="#64748B">
                    Render, port 8090
                </text>
            </g>

            {/* Fleche 1 : Frontend vers Keycloak (PKCE login) */}
            <line x1="175" y1="210" x2="300" y2="80"
                stroke="#8B5CF6" strokeWidth="1.5" strokeDasharray="5,3"
                markerEnd="url(#arr-purple)" />
            <text x="215" y="150" textAnchor="middle" fontSize="10" fill="#6D28D9" fontWeight="500">
                PKCE login
            </text>

            {/* Fleche 2 : Keycloak vers Frontend (token OIDC) */}
            <line x1="300" y1="65" x2="150" y2="205"
                stroke="#8B5CF6" strokeWidth="1.5" strokeDasharray="5,3"
                markerEnd="url(#arr-purple)" />
            <text x="185" y="165" textAnchor="middle" fontSize="10" fill="#6D28D9" fontWeight="500">
                access token
            </text>

            {/* Fleche 3 : Sejour vers Keycloak (validation JWKS) */}
            <line x1="400" y1="190" x2="400" y2="90"
                stroke="#8B5CF6" strokeWidth="1.5" strokeDasharray="5,3"
                markerEnd="url(#arr-purple)" />
            <text x="440" y="145" textAnchor="middle" fontSize="10" fill="#6D28D9" fontWeight="500">
                JWKS
            </text>

            {/* Fleche 4 : Frontend vers Sejour (HTTPS + Bearer) */}
            <line x1="200" y1="227" x2="300" y2="227"
                stroke="#0EA5E9" strokeWidth="2"
                markerEnd="url(#arr-blue)" />
            <text x="250" y="218" textAnchor="middle" fontSize="10" fill="#0369A1" fontWeight="500">
                Bearer token
            </text>

            {/* Fleche 5 : Sejour vers Pastell (creation montante) */}
            <line x1="500" y1="215" x2="600" y2="215"
                stroke="#0EA5E9" strokeWidth="2"
                markerEnd="url(#arr-blue)" />
            <text x="550" y="206" textAnchor="middle" fontSize="10" fill="#0369A1" fontWeight="500">
                creer dossier
            </text>

            {/* Fleche 6 : Pastell vers Sejour (polling descendant) */}
            <line x1="600" y1="245" x2="500" y2="245"
                stroke="#F59E0B" strokeWidth="2"
                markerEnd="url(#arr-amber)" />
            <text x="550" y="262" textAnchor="middle" fontSize="10" fill="#B45309" fontWeight="500">
                polling 30s
            </text>

            {/* Legendes */}
            <g transform="translate(30, 330)">
                <line x1="0" y1="6" x2="28" y2="6" stroke="#0EA5E9" strokeWidth="2" />
                <text x="36" y="10" fontSize="11" fill="#475569">Flux metier (reservations, dossiers)</text>
            </g>
            <g transform="translate(30, 355)">
                <line x1="0" y1="6" x2="28" y2="6" stroke="#F59E0B" strokeWidth="2" />
                <text x="36" y="10" fontSize="11" fill="#475569">Flux descendant (polling journal)</text>
            </g>
            <g transform="translate(30, 380)">
                <line x1="0" y1="6" x2="28" y2="6" stroke="#8B5CF6" strokeWidth="1.5"
                    strokeDasharray="5,3" />
                <text x="36" y="10" fontSize="11" fill="#475569">Flux authentification (OAuth2 / OIDC)</text>
            </g>
        </svg>
    );
}

// ============================================================
// Onglet 3 : Securite (OAuth2 / OpenID Connect / Keycloak)
// ============================================================

function SecuriteTab() {
    return (
        <div className="space-y-8">

            <section>
                <h2 className="text-lg font-semibold text-gray-900 mb-3">
                    Pourquoi Keycloak
                </h2>
                <p className="text-sm text-gray-700 leading-relaxed">
                    Le projet utilisait initialement un JWT maison : au login, le backend generait
                    un token signe avec une cle HMAC symetrique, le stockait cote client, et le validait
                    a chaque requete. Ca fonctionnait, mais c'etait du JWT artisanal, deconnecte des
                    standards du secteur.
                </p>
                <p className="text-sm text-gray-700 leading-relaxed mt-3">
                    Deux elements ont conduit a ajouter Keycloak. D'une part, la fiche de poste mentionne
                    explicitement OAuth2 et OpenID Connect parmi les connaissances requises. D'autre part,
                    iparapheur v5 de Libriciel s'appuie precisement sur Keycloak pour l'authentification
                    de ses utilisateurs. Integrer Keycloak dans SpringHotel permet de demonstrer cette
                    competence par la pratique, pas seulement par la lecture de documentation.
                </p>
                <div className="bg-sky-50 border border-sky-200 rounded-xl p-4 mt-4 text-sm text-sky-900 leading-relaxed">
                    <strong>Principe de coexistence :</strong> les deux systemes d'authentification coexistent.
                    Les comptes de demo existants (JWT maison) continuent de fonctionner sans modification.
                    Les comptes Keycloak utilisent le flow Authorization Code PKCE. Le backend accepte
                    les deux types de tokens via un decodeur composite qui dispatche selon le claim{" "}
                    <Code>iss</Code> du token.
                </div>
            </section>

            <section>
                <h2 className="text-lg font-semibold text-gray-900 mb-3">
                    OAuth2 et OpenID Connect, en bref
                </h2>
                <div className="space-y-4 text-sm text-gray-700">
                    <FlowItem index="1" title="OAuth2 : le protocole de delegation">
                        OAuth2 est un protocole qui permet a une application d'obtenir un acces limite
                        a une ressource au nom d'un utilisateur, sans que cet utilisateur donne son mot
                        de passe a l'application. L'analogie : un voiturier qui deplace votre voiture
                        avec un badge temporaire, sans avoir besoin des cles permanentes.
                    </FlowItem>
                    <FlowItem index="2" title="OpenID Connect : l'identite par-dessus OAuth2">
                        OpenID Connect ajoute une couche d'identite au-dessus d'OAuth2. La ou OAuth2
                        dit "cet utilisateur peut faire telle action", OpenID Connect dit aussi
                        "voici qui est cet utilisateur" via un id_token signe. C'est le standard
                        qu'utilisent Google, Microsoft, et Keycloak pour le SSO.
                    </FlowItem>
                    <FlowItem index="3" title="Keycloak : le serveur d'autorisation">
                        Keycloak est le serveur qui centralise les identites et emet les tokens.
                        Une collectivite qui utilise Pastell, iparapheur et d'autres logiciels
                        metier peut pointer tous ces logiciels vers un seul Keycloak. Quand un agent
                        est desactive dans Keycloak, il perd l'acces a tous les systemes en meme temps.
                    </FlowItem>
                </div>
            </section>

            <section>
                <h2 className="text-lg font-semibold text-gray-900 mb-3">
                    Le flow Authorization Code PKCE, etape par etape
                </h2>
                <div className="bg-[#F8FAFC] border border-gray-200 rounded-xl overflow-hidden">
                    <PkceSvg />
                </div>
                <div className="space-y-3 mt-4 text-sm text-gray-700">
                    <p>
                        <strong>Pourquoi PKCE</strong> (Proof Key for Code Exchange) : le client frontend
                        est public, il n'a pas de secret cote serveur. Sans PKCE, un attaquant qui
                        intercepte le code OAuth2 dans l'URL pourrait l'echanger contre un token.
                        PKCE ajoute un verifier secret connu uniquement du navigateur initiateur,
                        rendant le code inutilisable sans ce verifier.
                    </p>
                    <p>
                        <strong>Pourquoi pas keycloak-js</strong> : la bibliotheque officielle Keycloak
                        ajoute une dependance externe et impose ses propres conventions de gestion d'etat.
                        Le flow PKCE est implementable en JavaScript natif via la Web Crypto API
                        (disponible dans tous les navigateurs modernes). C'est ce choix qui a ete fait
                        ici : le fichier <Code>keycloak.js</Code> fait environ 150 lignes,
                        sans aucune dependance.
                    </p>
                </div>
            </section>

            <section>
                <h2 className="text-lg font-semibold text-gray-900 mb-3">
                    Le decodeur composite
                </h2>
                <p className="text-sm text-gray-700 leading-relaxed">
                    Le backend Spring Boot recoit des tokens Bearer de deux origines differentes.
                    Un token Keycloak est signe en RS256 et contient un claim{" "}
                    <Code>iss</Code> valant{" "}
                    <Code>http://localhost:8180/realms/springhotel</Code>.
                    Un token JWT maison est signe en HS256 et n'a pas de claim <Code>iss</Code>.
                </p>
                <p className="text-sm text-gray-700 leading-relaxed mt-3">
                    Le <Code>CompositeJwtDecoder</Code> lit le payload base64 du token entrant
                    sans verifier la signature, uniquement pour lire le claim <Code>iss</Code>.
                    Selon sa valeur, il delegue la validation au decodeur Keycloak (qui verifie
                    la signature RSA via le JWKS de Keycloak) ou au decodeur maison (qui verifie
                    la signature HMAC via JwtService). La verification de signature reste dans
                    le decodeur specialise, pas dans le routeur.
                </p>
                <div className="bg-[#F8FAFC] border border-gray-200 rounded-xl p-4 mt-4 font-mono text-xs text-gray-700 leading-relaxed">
                    <span className="text-gray-400">// Logique de dispatch dans CompositeJwtDecoder</span><br />
                    <span className="text-[#0369A1]">if</span> (keycloakIssuer.<span className="text-[#0369A1]">equals</span>(issuer)) {"{"}<br />
                    &nbsp;&nbsp;<span className="text-gray-500">// token Keycloak : validation RS256 via JWKS</span><br />
                    &nbsp;&nbsp;<span className="text-[#0369A1]">return</span> keycloakDecoder.<span className="text-[#0369A1]">decode</span>(token);<br />
                    {"}"}<br />
                    <span className="text-gray-500">// token maison : validation HS256 via JwtService</span><br />
                    <span className="text-[#0369A1]">return</span> decodeHomeMadeToken(token);
                </div>
            </section>

            <section>
                <h2 className="text-lg font-semibold text-gray-900 mb-3">
                    Le scope pastell-admin
                </h2>
                <p className="text-sm text-gray-700 leading-relaxed">
                    Les endpoints d'administration Pastell (status, poll) sont proteges par un scope
                    OAuth2 dedie : <Code>pastell-admin</Code>. Ce scope est declare optionnel
                    dans le realm Keycloak : le frontend doit le demander explicitement lors du login.
                    Un token Keycloak sans ce scope recevra un 403 sur ces endpoints, meme si
                    l'utilisateur a le role <Code>ROLE_ADMIN</Code>.
                </p>
                <p className="text-sm text-gray-700 leading-relaxed mt-3">
                    Ce mecanisme illustre la separation entre <strong>authentification</strong> (qui
                    etes-vous) et <strong>autorisation</strong> (a quoi avez-vous droit dans ce
                    contexte precis). C'est un pattern courant dans les APIs publiques de collectivites
                    ou certaines operations sensibles exigent un consentement explicite.
                </p>
            </section>

            <section>
                <h2 className="text-lg font-semibold text-gray-900 mb-3">
                    Le provisioning JIT
                </h2>
                <p className="text-sm text-gray-700 leading-relaxed">
                    Keycloak gere les identites, SpringHotel gere les reservations. Les deux bases
                    de donnees sont separees. Quand un utilisateur Keycloak fait sa premiere reservation,
                    le backend a besoin d'un enregistrement local pour rattacher la reservation a un
                    identifiant en base.
                </p>
                <p className="text-sm text-gray-700 leading-relaxed mt-3">
                    Le <Code>KeycloakUserProvisioningService</Code> ecoute l'evenement{" "}
                    <Code>AuthenticationSuccessEvent</Code> de Spring Security. A la premiere
                    connexion Keycloak, il cree automatiquement un profil local en base, avec le claim{" "}
                    <Code>sub</Code> du token comme identifiant stable. Les connexions suivantes
                    retrouvent ce profil directement. C'est le pattern JIT (Just-In-Time) provisioning.
                </p>
            </section>

        </div>
    );
}

/**
 * Schema SVG du flow Authorization Code PKCE.
 * Trois acteurs : Navigateur, Keycloak, Backend Sejour.
 */
function PkceSvg() {
    return (
        <svg
            viewBox="0 0 760 310"
            xmlns="http://www.w3.org/2000/svg"
            className="w-full h-auto"
            role="img"
            aria-label="Flow Authorization Code PKCE"
        >
            <defs>
                <marker id="arr-seq" markerWidth="8" markerHeight="6" refX="7" refY="3" orient="auto">
                    <polygon points="0 0, 8 3, 0 6" fill="#0EA5E9" />
                </marker>
                <marker id="arr-seq-purple" markerWidth="8" markerHeight="6" refX="7" refY="3" orient="auto">
                    <polygon points="0 0, 8 3, 0 6" fill="#8B5CF6" />
                </marker>
            </defs>

            {/* Acteurs */}
            <rect x="40"  y="10" width="140" height="36" rx="8" fill="#EFF6FF" stroke="#0EA5E9" strokeWidth="1.5" />
            <text x="110" y="33" textAnchor="middle" fontSize="12" fontWeight="600" fill="#0369A1">Navigateur</text>

            <rect x="300" y="10" width="160" height="36" rx="8" fill="#F5F3FF" stroke="#8B5CF6" strokeWidth="1.5" />
            <text x="380" y="33" textAnchor="middle" fontSize="12" fontWeight="600" fill="#6D28D9">Keycloak</text>

            <rect x="580" y="10" width="150" height="36" rx="8" fill="#EFF6FF" stroke="#0EA5E9" strokeWidth="1.5" />
            <text x="655" y="33" textAnchor="middle" fontSize="12" fontWeight="600" fill="#0369A1">Sejour backend</text>

            {/* Lignes de vie */}
            <line x1="110" y1="46" x2="110" y2="300" stroke="#CBD5E1" strokeWidth="1" strokeDasharray="4,3" />
            <line x1="380" y1="46" x2="380" y2="300" stroke="#CBD5E1" strokeWidth="1" strokeDasharray="4,3" />
            <line x1="655" y1="46" x2="655" y2="300" stroke="#CBD5E1" strokeWidth="1" strokeDasharray="4,3" />

            {/* Etape 1 */}
            <line x1="110" y1="75" x2="374" y2="75" stroke="#8B5CF6" strokeWidth="1.5" markerEnd="url(#arr-seq-purple)" />
            <text x="245" y="68" textAnchor="middle" fontSize="10" fill="#6D28D9">1. redirect + code_challenge (S256)</text>

            {/* Etape 2 */}
            <line x1="374" y1="105" x2="116" y2="105" stroke="#8B5CF6" strokeWidth="1.5" markerEnd="url(#arr-seq-purple)" />
            <text x="245" y="98" textAnchor="middle" fontSize="10" fill="#6D28D9">2. page de login Keycloak</text>

            {/* Etape 3 */}
            <line x1="110" y1="135" x2="374" y2="135" stroke="#8B5CF6" strokeWidth="1.5" markerEnd="url(#arr-seq-purple)" />
            <text x="245" y="128" textAnchor="middle" fontSize="10" fill="#6D28D9">3. identifiants utilisateur</text>

            {/* Etape 4 */}
            <line x1="374" y1="165" x2="116" y2="165" stroke="#8B5CF6" strokeWidth="1.5" markerEnd="url(#arr-seq-purple)" />
            <text x="245" y="158" textAnchor="middle" fontSize="10" fill="#6D28D9">4. redirect vers /Connexion?code=...</text>

            {/* Etape 5 */}
            <line x1="110" y1="195" x2="374" y2="195" stroke="#0EA5E9" strokeWidth="1.5" markerEnd="url(#arr-seq)" />
            <text x="245" y="188" textAnchor="middle" fontSize="10" fill="#0369A1">5. echange code + code_verifier</text>

            {/* Etape 6 */}
            <line x1="374" y1="225" x2="116" y2="225" stroke="#0EA5E9" strokeWidth="1.5" markerEnd="url(#arr-seq)" />
            <text x="245" y="218" textAnchor="middle" fontSize="10" fill="#0369A1">6. access_token (RS256)</text>

            {/* Etape 7 */}
            <line x1="110" y1="255" x2="649" y2="255" stroke="#0EA5E9" strokeWidth="1.5" markerEnd="url(#arr-seq)" />
            <text x="380" y="248" textAnchor="middle" fontSize="10" fill="#0369A1">7. requete API + Bearer token</text>

            {/* Etape 8 */}
            <line x1="649" y1="280" x2="116" y2="280" stroke="#0EA5E9" strokeWidth="1.5" markerEnd="url(#arr-seq)" />
            <text x="380" y="273" textAnchor="middle" fontSize="10" fill="#0369A1">8. reponse (token valide via JWKS)</text>
        </svg>
    );
}

// ============================================================
// Onglet 4 : API
// ============================================================

const API_GROUPS = [
    {
        title: "Authentification",
        endpoints: [
            {
                method: "POST",
                path: "/api/v1/login",
                description: "Flux JWT maison : retourne un token HS256 pour les comptes locaux (demo@springhotel.fr, test@test.com).",
                curl: `curl -s -X POST https://projethotel-management.onrender.com/api/v1/login \\
  -H "Content-Type: application/json" \\
  -d '{"email":"employe@springhotel.fr","password":"Employe971*"}' | jq .`,
            },
            {
                method: "GET",
                path: "/realms/springhotel/protocol/openid-connect/token (Keycloak)",
                description: "Flux Keycloak : obtenir un access_token via le grant password (activation Direct Access Grants requise, usage local uniquement). En production, utiliser Authorization Code PKCE depuis le frontend.",
                curl: `curl -s -X POST \\
  "http://localhost:8180/realms/springhotel/protocol/openid-connect/token" \\
  -H "Content-Type: application/x-www-form-urlencoded" \\
  -d "grant_type=password&client_id=springhotel-frontend" \\
  -d "username=admin-demo&password=Admin1234!&scope=openid pastell-admin" | jq .`,
            },
        ],
    },
    {
        title: "Etat global du bus Pastell",
        endpoints: [
            {
                method: "GET",
                path: "/api/admin/pastell/status",
                description: "Snapshot global : compteurs par etape circuit, curseur de polling, ping du mock. Requiert SCOPE_pastell-admin (token Keycloak) ou ROLE_ADMIN (token JWT maison).",
                curl: `# Avec un token Keycloak (scope pastell-admin requis)
curl -s "https://projethotel-management.onrender.com/api/admin/pastell/status" \\
  -H "Authorization: Bearer $KEYCLOAK_TOKEN" | jq .`,
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
                description: "Force un cycle de polling immediat. Requiert SCOPE_pastell-admin.",
                curl: `curl -X POST \\
  -H "Authorization: Bearer $KEYCLOAK_TOKEN" \\
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
                description: "Page de dossiers, filtrable par etape circuit. Renvoie un PagedResponseDTO.",
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
                description: "Relance manuelle d'un dossier en anomalie.",
                curl: `curl -X POST \\
  -H "Authorization: Bearer $TOKEN" \\
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
                description: "Cree un utilisateur local (flux JWT maison). Le role est obligatoire en query param.",
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
                    Le projet supporte deux types de tokens Bearer en coexistence. Un token JWT maison
                    obtenu via <Code>POST /api/v1/login</Code> avec un compte local
                    (role <Code>ROLE_ADMIN</Code>). Un token Keycloak obtenu via le flow
                    Authorization Code PKCE, qui peut porter le scope optionnel{" "}
                    <Code>pastell-admin</Code> pour acceder aux endpoints d'administration Pastell.
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
        GET:    "bg-emerald-50 text-emerald-700 border-emerald-200",
        POST:   "bg-sky-50 text-[#0369A1] border-sky-200",
        DELETE: "bg-red-50 text-red-700 border-red-200",
        PUT:    "bg-amber-50 text-amber-800 border-amber-200",
    }[endpoint.method] || "bg-gray-50 text-gray-700 border-gray-200";

    return (
        <div className="border border-gray-200 rounded-xl overflow-hidden">
            <div className="px-4 py-3 bg-[#F8FAFC] border-b border-gray-200">
                <div className="flex items-center gap-3 flex-wrap">
                    <span className={`inline-block px-2 py-0.5 rounded text-xs font-semibold border ${methodConfig}`}>
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
// Onglet 5 : Glossaire
// ============================================================

const GLOSSARY = [
    {
        term: "Dossier",
        audiences: ["Dev", "Admin"],
        def: "Unite de circulation dans Pastell. Chez Libriciel, c'est l'entite qui transite entre services (parapheur, GED, SAE). Dans notre projet, un dossier correspond a une reservation transmise au mock.",
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
        term: "OAuth2",
        audiences: ["Dev", "Direction"],
        def: "Protocole standard d'autorisation qui permet a une application d'obtenir un acces limite a une ressource au nom d'un utilisateur, sans que cet utilisateur donne son mot de passe a l'application. Base de l'authentification moderne dans les APIs publiques.",
    },
    {
        term: "OpenID Connect",
        audiences: ["Dev", "Direction"],
        def: "Extension d'OAuth2 qui ajoute une couche d'identite : en plus de l'autorisation, le serveur renvoie un id_token qui contient des informations sur l'utilisateur (nom, email, sub). Utilise par Keycloak, Google, Microsoft pour le SSO.",
    },
    {
        term: "Keycloak",
        audiences: ["Dev", "Admin"],
        def: "Serveur d'autorisation open source qui centralise les identites et emet des tokens OAuth2 / OpenID Connect. iparapheur v5 de Libriciel s'appuie sur Keycloak. Dans SpringHotel, Keycloak gere un realm 'springhotel' avec des roles, des scopes et un client public PKCE.",
    },
    {
        term: "Resource Server",
        audiences: ["Dev"],
        def: "Dans la terminologie OAuth2, le service qui expose des ressources protegees et valide les tokens Bearer. Dans SpringHotel, sejour-backend est le Resource Server : il valide les tokens via le JWKS de Keycloak (RS256) ou via JwtService (HS256).",
    },
    {
        term: "Scope OAuth2",
        audiences: ["Dev", "Admin"],
        def: "Permission granulaire qu'un client demande explicitement lors du login. Dans SpringHotel, le scope 'pastell-admin' est optionnel : un utilisateur authentifie sans ce scope ne peut pas acceder aux endpoints d'administration Pastell, meme s'il a le role ADMIN.",
    },
    {
        term: "PKCE",
        audiences: ["Dev"],
        def: "Proof Key for Code Exchange. Extension du flow Authorization Code OAuth2 pour les clients publics (navigateur, mobile) qui n'ont pas de secret cote serveur. Un code_verifier secret est genere cote client, son hash est envoye a Keycloak, et le client prouve qu'il connait le verifier lors de l'echange du code.",
    },
    {
        term: "JIT Provisioning",
        audiences: ["Dev"],
        def: "Just-In-Time provisioning. Pattern ou un profil utilisateur local est cree automatiquement lors de la premiere connexion via un fournisseur d'identite externe (Keycloak ici). Evite de pre-creer tous les comptes en base : le profil existe quand il est necessaire.",
    },
    {
        term: "JWT",
        audiences: ["Dev"],
        def: "JSON Web Token. Format standard de token signe. Dans SpringHotel deux variantes coexistent : HS256 (secret symetrique, tokens maison) et RS256 (cle publique/privee, tokens Keycloak). Le CompositeJwtDecoder dispatche selon le claim 'iss'.",
    },
    {
        term: "Mock",
        audiences: ["Dev", "Direction"],
        def: "Implementation simplifiee d'un service tiers, utilisee pour developper et tester sans dependre du vrai service. Dans ce projet, pastell-mock simule un Pastell reel sans avoir besoin d'une instance Libriciel.",
    },
    {
        term: "Idempotence",
        audiences: ["Dev"],
        def: "Propriete d'une operation qui, appelee plusieurs fois, produit le meme effet qu'un seul appel. Critique pour la creation de dossiers : si le reseau echoue et qu'on rejoue, on ne veut pas creer deux dossiers en double.",
    },
    {
        term: "Profil Spring",
        audiences: ["Dev"],
        def: "Mecanisme Spring Boot qui permet de charger des configurations differentes selon l'environnement (dev, test, prod, local). Le profil 'prod' active Flyway en mode validate et la rotation de credentials Pastell. Le profil 'local' active le mode rotatif du mock sans polluer les tests.",
    },
];

function GlossaireTab() {
    const groupedByAudience = useMemo(() => GLOSSARY, []);

    const audienceColors = {
        Client:    "bg-emerald-50 text-emerald-700",
        Dev:       "bg-sky-50 text-[#0369A1]",
        Admin:     "bg-amber-50 text-amber-800",
        Direction: "bg-purple-50 text-purple-700",
        Commercial:"bg-pink-50 text-pink-700",
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