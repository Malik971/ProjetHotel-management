#!/usr/bin/env bash
#
# scripts/seed-demo.sh
# ============================================================
# Script de seeding pour la demo SpringHotel x Pastell.
#
# Objectif : peupler la base de prod avec environ 100 reservations
# couvrant tous les statuts Pastell (OK, PENDING, EN_RETRY, EN_ERREUR),
# pour que le recruteur ouvre la page /admin/pastell sur un tableau
# vivant et representatif.
#
# Pre-requis :
#   - bash, curl, jq, openssl
#   - Variable d'env API_URL pointant vers le backend
#   - Variable d'env ADMIN_EMAIL / ADMIN_PASSWORD pour le login admin
#   - Le rate limit DemoRateLimitFilter doit etre desactive
#     (DEMO_RATE_LIMIT_ENABLED=false sur Render avant de lancer)
#
# Usage local :
#   API_URL=http://localhost:8080 \
#   ADMIN_EMAIL=test@test.com \
#   ADMIN_PASSWORD=test123 \
#   ./scripts/seed-demo.sh
#
# Usage prod :
#   API_URL=https://projethotel-management.onrender.com \
#   ADMIN_EMAIL=test@test.com \
#   ADMIN_PASSWORD=test123 \
#   ./scripts/seed-demo.sh
#
# Le script procede en trois vagues :
#   Vague 1 : 60 reservations avec Pastell fonctionnel (statuts OK / PENDING)
#   Vague 2 : 15 reservations avec Pastell casse (vous changez PASTELL_URL
#             sur Render vers une URL bidon, puis vous remettez)
#   Vague 3 : 25 reservations supplementaires avec Pastell fonctionnel
# ============================================================

set -euo pipefail

# ============================================================
# Configuration
# ============================================================
API_URL="${API_URL:-http://localhost:8080}"
ADMIN_EMAIL="${ADMIN_EMAIL:-test@test.com}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-test123}"

PARALLELISM="${PARALLELISM:-5}"
DEFAULT_USER_PASSWORD="Demo2026*"

VAGUE_1_COUNT="${VAGUE_1_COUNT:-60}"
VAGUE_2_COUNT="${VAGUE_2_COUNT:-15}"
VAGUE_3_COUNT="${VAGUE_3_COUNT:-25}"

# Couleurs pour la lisibilite du log
BLUE="\033[0;34m"
GREEN="\033[0;32m"
YELLOW="\033[0;33m"
RED="\033[0;31m"
RESET="\033[0m"

log_info()    { echo -e "${BLUE}[INFO]${RESET} $*"; }
log_ok()      { echo -e "${GREEN}[OK]${RESET} $*"; }
log_warn()    { echo -e "${YELLOW}[WARN]${RESET} $*"; }
log_error()   { echo -e "${RED}[ERR]${RESET} $*" >&2; }

# ============================================================
# Verifications prealables
# ============================================================
for cmd in curl jq openssl; do
    if ! command -v "$cmd" >/dev/null 2>&1; then
        log_error "$cmd est requis mais introuvable. Installez-le avant de relancer."
        exit 1
    fi
done

# ============================================================
# Tableaux de donnees fictives
# ============================================================
PRENOMS=(
    "Lucas" "Emma" "Hugo" "Lea" "Louis" "Jade" "Gabriel" "Chloe" "Raphael" "Manon"
    "Arthur" "Camille" "Adam" "Ines" "Jules" "Sarah" "Maxime" "Louise" "Noah" "Alice"
    "Liam" "Eva" "Tom" "Romane" "Nathan" "Anaelle" "Theo" "Lila" "Antoine" "Mia"
)

NOMS=(
    "Martin" "Bernard" "Dubois" "Thomas" "Robert" "Richard" "Petit" "Durand" "Leroy" "Moreau"
    "Simon" "Laurent" "Lefebvre" "Michel" "Garcia" "David" "Bertrand" "Roux" "Vincent" "Fournier"
    "Morel" "Girard" "Andre" "Lefevre" "Mercier" "Dupont" "Lambert" "Bonnet" "Francois" "Martinez"
)

VILLES_PHONE=(
    "06" "07"
)

# ============================================================
# Helpers
# ============================================================

# Genere un email unique base sur un timestamp pour eviter les collisions
# meme si le script est lance plusieurs fois.
generate_email() {
    local prefix="$1"
    local ts="$2"
    local id="$3"
    echo "${prefix}.${ts}${id}@demo-springhotel.fr"
}

# Genere un numero de telephone francais fictif
generate_phone() {
    local prefix="${VILLES_PHONE[$((RANDOM % ${#VILLES_PHONE[@]}))]}"
    printf "%s%08d" "$prefix" "$((RANDOM % 100000000))"
}

# Genere un couple de dates (debut, fin) selon le profil temporel demande.
# Profils : "futur" (3 a 60 jours dans le futur), "en_cours" (en cours aujourd'hui),
# "passe" (15 a 90 jours dans le passe).
# Sortie : "YYYY-MM-DD YYYY-MM-DD"
generate_dates() {
    local profil="$1"
    local duree=$((1 + RANDOM % 5))
    local offset_debut

    case "$profil" in
        futur)
            offset_debut=$((3 + RANDOM % 58))
            ;;
        en_cours)
            offset_debut=$((-1 * (RANDOM % 3)))
            duree=$((3 + RANDOM % 4))
            ;;
        passe)
            offset_debut=$((-15 - RANDOM % 76))
            ;;
        *)
            offset_debut=$((3 + RANDOM % 58))
            ;;
    esac

    local debut
    local fin
    debut=$(date -u -d "${offset_debut} days" +%Y-%m-%d 2>/dev/null || date -u -v"${offset_debut}d" +%Y-%m-%d)
    fin=$(date -u -d "${offset_debut} days + ${duree} days" +%Y-%m-%d 2>/dev/null || date -u -v"${offset_debut}d" -v"+${duree}d" +%Y-%m-%d)

    echo "$debut $fin"
}

# ============================================================
# Etape 1 : connexion admin et recuperation du JWT
# ============================================================
log_info "Connexion admin sur ${API_URL}..."

LOGIN_RESPONSE=$(curl -s -X POST "${API_URL}/api/v1/login" \
    -H "Content-Type: application/json" \
    -d "{\"email\":\"${ADMIN_EMAIL}\",\"password\":\"${ADMIN_PASSWORD}\"}")

ADMIN_TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r '.token // .accessToken // empty')

if [ -z "$ADMIN_TOKEN" ] || [ "$ADMIN_TOKEN" = "null" ]; then
    log_error "Echec de la connexion admin. Reponse : $LOGIN_RESPONSE"
    log_error "Verifiez ADMIN_EMAIL, ADMIN_PASSWORD et le chemin de login."
    exit 1
fi

log_ok "Connecte en tant que ${ADMIN_EMAIL}"

# ============================================================
# Etape 2 : recuperation des hotels et chambres existants
# ============================================================
log_info "Recuperation des hotels..."

HOTELS_JSON=$(curl -s "${API_URL}/api/hotels")
HOTEL_IDS=($(echo "$HOTELS_JSON" | jq -r '.[].id'))

if [ ${#HOTEL_IDS[@]} -eq 0 ]; then
    log_error "Aucun hotel trouve. Lancez d'abord vos migrations Flyway de seed."
    exit 1
fi

log_ok "${#HOTEL_IDS[@]} hotels disponibles"

# On stocke pour chaque hotel la liste de ses chambres pour piocher dedans
declare -A HOTEL_CHAMBRES
for hotel_id in "${HOTEL_IDS[@]}"; do
    CHAMBRES_JSON=$(curl -s "${API_URL}/api/hotels/${hotel_id}/chambres" || echo "[]")
    CHAMBRE_IDS=($(echo "$CHAMBRES_JSON" | jq -r '.[].id'))
    if [ ${#CHAMBRE_IDS[@]} -gt 0 ]; then
        HOTEL_CHAMBRES[$hotel_id]="${CHAMBRE_IDS[*]}"
    fi
done

HOTELS_WITH_CHAMBRES=("${!HOTEL_CHAMBRES[@]}")
log_ok "${#HOTELS_WITH_CHAMBRES[@]} hotels disposent de chambres reservables"

if [ ${#HOTELS_WITH_CHAMBRES[@]} -eq 0 ]; then
    log_error "Aucun hotel ne possede de chambre. Impossible de creer des reservations."
    exit 1
fi

# ============================================================
# Etape 3 : creation des utilisateurs clients
# ============================================================
TIMESTAMP=$(date +%s)
NB_USERS=80

log_info "Creation de ${NB_USERS} utilisateurs clients (timestamp ${TIMESTAMP})..."

# Fonction de creation d'un user, lancable en parallele
create_user() {
    local user_id="$1"
    local timestamp="$2"
    local prenom="${PRENOMS[$((user_id % ${#PRENOMS[@]}))]}"
    local nom="${NOMS[$(((user_id * 7) % ${#NOMS[@]}))]}"
    local email=$(generate_email "${prenom,,}.${nom,,}" "${timestamp}" "${user_id}")

    curl -s -o /dev/null -X POST "${API_URL}/api/v1/register?role=USER" \
        -H "Content-Type: application/json" \
        -d "{
            \"email\": \"${email}\",
            \"password\": \"${DEFAULT_USER_PASSWORD}\",
            \"firstName\": \"${prenom}\",
            \"lastName\": \"${nom}\"
        }"

    echo "$email"
}
export -f create_user generate_email
export PRENOMS NOMS API_URL DEFAULT_USER_PASSWORD TIMESTAMP

# On exporte les tableaux via un fichier temporaire car bash n'exporte pas les arrays
USERS_FILE=$(mktemp)
trap "rm -f $USERS_FILE" EXIT

seq 1 "$NB_USERS" | xargs -P "$PARALLELISM" -I {} bash -c "
    user_id=\$1
    prenom_idx=\$((user_id % ${#PRENOMS[@]}))
    nom_idx=\$(((user_id * 7) % ${#NOMS[@]}))
    prenoms=(${PRENOMS[*]})
    noms=(${NOMS[*]})
    prenom=\${prenoms[\$prenom_idx]}
    nom=\${noms[\$nom_idx]}
    email=\"\${prenom,,}.\${nom,,}.${TIMESTAMP}\${user_id}@demo-springhotel.fr\"

    curl -s -o /dev/null -X POST \"${API_URL}/api/v1/register?role=USER\" \
        -H 'Content-Type: application/json' \
        -d \"{
            \\\"email\\\": \\\"\$email\\\",
            \\\"password\\\": \\\"${DEFAULT_USER_PASSWORD}\\\",
            \\\"firstName\\\": \\\"\$prenom\\\",
            \\\"lastName\\\": \\\"\$nom\\\"
        }\"

    echo \"\$email\" >> $USERS_FILE
" _ {}

CREATED_USERS=($(cat "$USERS_FILE"))
log_ok "${#CREATED_USERS[@]} utilisateurs crees"

if [ ${#CREATED_USERS[@]} -lt 10 ]; then
    log_error "Trop peu d'utilisateurs crees, le seed s'arrete. Verifiez le rate limit."
    exit 1
fi

# ============================================================
# Fonction qui cree N reservations en parallele
# ============================================================
creer_reservations() {
    local count="$1"
    local label="$2"

    log_info "Creation de ${count} reservations (${label})..."

    seq 1 "$count" | xargs -P "$PARALLELISM" -I {} bash -c '
        i=$1
        users=('"${CREATED_USERS[*]}"')
        hotels_avec_chambres=('"${HOTELS_WITH_CHAMBRES[*]}"')

        # On choisit un user au hasard parmi ceux crees
        user_email=${users[$((RANDOM % ${#users[@]}))]}

        # On se logue en tant que ce user pour avoir un token
        login_resp=$(curl -s -X POST "'"${API_URL}"'/api/v1/login" \
            -H "Content-Type: application/json" \
            -d "{\"email\":\"$user_email\",\"password\":\"'"${DEFAULT_USER_PASSWORD}"'\"}")
        user_token=$(echo "$login_resp" | jq -r ".token // .accessToken // empty")

        if [ -z "$user_token" ] || [ "$user_token" = "null" ]; then
            exit 0
        fi

        # On choisit un hotel et une chambre au hasard
        hotel_id=${hotels_avec_chambres[$((RANDOM % ${#hotels_avec_chambres[@]}))]}

        # Choix du profil temporel : 60% futur, 20% en cours, 20% passe
        rand=$((RANDOM % 10))
        if [ $rand -lt 6 ]; then profil="futur"
        elif [ $rand -lt 8 ]; then profil="en_cours"
        else profil="passe"
        fi

        # Generation des dates (variantes Linux/Mac)
        case "$profil" in
            futur)    offset=$((3 + RANDOM % 58)); duree=$((1 + RANDOM % 5)) ;;
            en_cours) offset=$((-1 * (RANDOM % 3))); duree=$((3 + RANDOM % 4)) ;;
            passe)    offset=$((-15 - RANDOM % 76)); duree=$((1 + RANDOM % 5)) ;;
        esac
        fin_offset=$((offset + duree))
        date_debut=$(date -u -d "${offset} days" +%Y-%m-%d 2>/dev/null || date -u -v"${offset}d" +%Y-%m-%d)
        date_fin=$(date -u -d "${fin_offset} days" +%Y-%m-%d 2>/dev/null || date -u -v"${fin_offset}d" +%Y-%m-%d)

        nb_personnes=$((1 + RANDOM % 4))

        # On recupere la premiere chambre dispo de cet hotel pour ces dates
        chambres_resp=$(curl -s "'"${API_URL}"'/api/hotels/${hotel_id}/chambres")
        chambre_id=$(echo "$chambres_resp" | jq -r ".[0].id // empty")
        if [ -z "$chambre_id" ]; then exit 0; fi

        # Reservation
        curl -s -o /dev/null -X POST "'"${API_URL}"'/api/reservations" \
            -H "Authorization: Bearer $user_token" \
            -H "Content-Type: application/json" \
            -d "{
                \"chambreId\": $chambre_id,
                \"dateDebut\": \"$date_debut\",
                \"dateFin\": \"$date_fin\",
                \"nombrePersonnes\": $nb_personnes,
                \"nomClient\": \"Client demo $i\",
                \"emailClient\": \"$user_email\",
                \"telephoneClient\": \"0612345678\"
            }"
    ' _ {}

    log_ok "Vague ${label} terminee"
}

# ============================================================
# Vague 1 : Pastell fonctionnel
# ============================================================
echo ""
log_info "=========================================="
log_info "VAGUE 1 : ${VAGUE_1_COUNT} reservations avec Pastell fonctionnel"
log_info "=========================================="
creer_reservations "$VAGUE_1_COUNT" "1/3"

# ============================================================
# Vague 2 : Pastell casse pour generer des anomalies
# ============================================================
echo ""
log_warn "=========================================="
log_warn "VAGUE 2 : pause pour casser Pastell"
log_warn "=========================================="
log_warn ""
log_warn "  -> Allez sur Render, service projethotel-management"
log_warn "  -> Modifiez la variable PASTELL_URL"
log_warn "     Valeur actuelle  : https://springhotel-pastell-mock.onrender.com"
log_warn "     Valeur a saisir  : https://springhotel-pastell-mock-bidon.onrender.com"
log_warn "  -> Attendez que le service redemarre (environ 30 secondes)"
log_warn ""
read -r -p "Appuyez sur Entree quand c'est fait pour lancer la vague 2..." _

creer_reservations "$VAGUE_2_COUNT" "2/3 (Pastell casse, attendez-vous a des EN_RETRY puis EN_ERREUR)"

echo ""
log_warn "=========================================="
log_warn "  -> Remettez PASTELL_URL a sa valeur d'origine sur Render"
log_warn "  -> https://springhotel-pastell-mock.onrender.com"
log_warn "  -> Attendez que le service redemarre (environ 30 secondes)"
log_warn "=========================================="
read -r -p "Appuyez sur Entree quand Pastell est restaure pour lancer la vague 3..." _

# ============================================================
# Vague 3 : Pastell fonctionnel de nouveau
# ============================================================
echo ""
log_info "=========================================="
log_info "VAGUE 3 : ${VAGUE_3_COUNT} reservations finales"
log_info "=========================================="
creer_reservations "$VAGUE_3_COUNT" "3/3"

# ============================================================
# Bilan
# ============================================================
echo ""
log_ok "=========================================="
log_ok "  Seed termine !"
log_ok "=========================================="
log_ok "Total cree : ${NB_USERS} utilisateurs + environ $((VAGUE_1_COUNT + VAGUE_2_COUNT + VAGUE_3_COUNT)) reservations"
log_ok ""
log_ok "Verifiez sur ${API_URL}/api/admin/pastell/status"
log_ok "puis ouvrez la page admin /admin/pastell pour voir le tableau."
log_ok ""
log_ok "N'oubliez pas de :"
log_ok "  - Remettre PASTELL_URL a sa valeur normale si pas deja fait"
log_ok "  - Remettre DEMO_RATE_LIMIT_ENABLED=true sur Render"
log_ok ""