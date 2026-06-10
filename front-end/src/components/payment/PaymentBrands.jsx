// src/components/payment/PaymentBrands.jsx
//
// Logos de marques de paiement en SVG inline (aucune dependance externe).
// Volontairement sobres mais reconnaissables : ils servent de reperes de
// confiance sur la page de paiement (demo portfolio, aucun paiement reel).
//
// Chaque logo accepte une prop `className` pour la taille (hauteur via Tailwind,
// ex. "h-5"). Le viewBox fixe le ratio, la largeur s'adapte.

export function VisaLogo({ className = "h-5" }) {
    return (
        <svg className={className} viewBox="0 0 48 16" role="img" aria-label="Visa">
            <text x="24" y="13" textAnchor="middle" fontFamily="Arial, Helvetica, sans-serif"
                fontWeight="700" fontStyle="italic" fontSize="14" letterSpacing="0.5" fill="#1A1F71">
                VISA
            </text>
        </svg>
    );
}

export function MastercardLogo({ className = "h-5" }) {
    return (
        <svg className={className} viewBox="0 0 36 24" role="img" aria-label="Mastercard">
            <circle cx="14" cy="12" r="8" fill="#EB001B" />
            <circle cx="22" cy="12" r="8" fill="#F79E1B" />
            <path d="M18 6.2a8 8 0 0 0 0 11.6 8 8 0 0 0 0-11.6z" fill="#FF5F00" />
        </svg>
    );
}

export function CbLogo({ className = "h-5" }) {
    return (
        <svg className={className} viewBox="0 0 36 24" role="img" aria-label="Carte Bancaire">
            <rect width="36" height="24" rx="3" fill="#15457A" />
            <rect x="3" y="3" width="30" height="18" rx="2" fill="#1B7A4B" />
            <text x="18" y="16" textAnchor="middle" fontFamily="Arial, Helvetica, sans-serif"
                fontWeight="700" fontStyle="italic" fontSize="11" fill="#fff">
                CB
            </text>
        </svg>
    );
}

export function PaypalLogo({ className = "h-5" }) {
    return (
        <svg className={className} viewBox="0 0 64 16" role="img" aria-label="PayPal">
            <text x="0" y="13" fontFamily="Arial, Helvetica, sans-serif" fontWeight="700" fontStyle="italic" fontSize="14">
                <tspan fill="#003087">Pay</tspan><tspan fill="#009CDE">Pal</tspan>
            </text>
        </svg>
    );
}

export function ApplePayLogo({ className = "h-5" }) {
    return (
        <svg className={className} viewBox="0 0 52 24" role="img" aria-label="Apple Pay">
            <g transform="translate(1 2) scale(0.85)">
                <path
                    d="M16.37 6.17c.05.99-.39 1.95-1.02 2.62-.62.67-1.65 1.18-2.6 1.1-.11-.94.39-1.93 1-2.57.63-.66 1.71-1.16 2.62-1.15zm2.7 6.27c-.02-2.36 1.93-3.49 2.02-3.55-1.1-1.6-2.81-1.82-3.42-1.85-1.46-.15-2.84.86-3.58.86-.74 0-1.88-.84-3.08-.81-1.59.02-3.05.92-3.86 2.34-1.65 2.86-.42 7.1 1.18 9.42.78 1.14 1.72 2.42 2.95 2.37 1.18-.05 1.63-.76 3.06-.76 1.43 0 1.83.76 3.08.74 1.27-.02 2.08-1.16 2.86-2.31.9-1.32 1.27-2.6 1.29-2.67-.03-.01-2.48-.95-2.5-3.77z"
                    fill="#000"
                />
            </g>
            <text x="22" y="17" fontFamily="Arial, Helvetica, sans-serif" fontWeight="600" fontSize="13" fill="#000">
                Pay
            </text>
        </svg>
    );
}

export function GooglePayLogo({ className = "h-5" }) {
    return (
        <svg className={className} viewBox="0 0 60 16" role="img" aria-label="Google Pay">
            <text x="0" y="13" fontFamily="Arial, Helvetica, sans-serif" fontSize="14">
                <tspan fill="#4285F4" fontWeight="700">G</tspan>
                <tspan fill="#5F6368" fontWeight="500"> Pay</tspan>
            </text>
        </svg>
    );
}
