import Link from "next/link";
import { CONTACT } from "@/lib/contact";

/** Pied de page commun à toutes les pages publiques. */
export function SiteFooter() {
  return (
    <footer className="border-t-2 border-gold bg-noir text-white">
      <div className="mx-auto grid max-w-6xl gap-8 px-4 py-10 sm:grid-cols-3 sm:px-6">
        <div>
          <p className="font-display text-xl font-extrabold uppercase tracking-[0.12em]">Lisa</p>
          <p className="mt-1 text-[0.6rem] font-semibold uppercase tracking-[0.3em] text-gold-light">
            Cosmétique &amp; Bien-être
          </p>
          <p className="mt-4 text-sm text-white/70">
            Livraison à Yaoundé et dans les autres villes du Cameroun.
          </p>
        </div>

        <div>
          <h2 className="font-display text-sm font-bold uppercase tracking-[0.2em] text-gold-light">
            Nous contacter
          </h2>
          <ul className="mt-4 space-y-3 text-sm">
            <li>
              <a
                href={CONTACT.phoneHref}
                className="inline-flex items-center gap-2 font-semibold transition-colors hover:text-rose-light"
              >
                <PhoneIcon />
                {CONTACT.phoneDisplay}
              </a>
            </li>
            <li>
              <a
                href={CONTACT.whatsappHref}
                target="_blank"
                rel="noopener noreferrer"
                className="inline-flex items-center gap-2 text-white/80 transition-colors hover:text-rose-light"
              >
                <ChatIcon />
                Écrire sur WhatsApp
              </a>
            </li>
            <li>
              <a
                href={CONTACT.mapsHref}
                target="_blank"
                rel="noopener noreferrer"
                className="inline-flex items-center gap-2 text-white/80 transition-colors hover:text-rose-light"
              >
                <PinIcon />
                {CONTACT.address}
              </a>
            </li>
          </ul>
        </div>

        <div>
          <h2 className="font-display text-sm font-bold uppercase tracking-[0.2em] text-gold-light">
            La boutique
          </h2>
          <ul className="mt-4 space-y-3 text-sm text-white/80">
            <li>
              <Link href="/boutique" className="transition-colors hover:text-rose-light">
                Tous nos produits
              </Link>
            </li>
            <li>
              <Link href="/suivi-commande" className="transition-colors hover:text-rose-light">
                Suivre ma commande
              </Link>
            </li>
            <li>
              <Link href="/panier" className="transition-colors hover:text-rose-light">
                Mon panier
              </Link>
            </li>
          </ul>
        </div>
      </div>

      <div className="border-t border-white/10">
        <p className="mx-auto max-w-6xl px-4 py-4 text-xs text-white/60 sm:px-6">
          © Lisa Cosmétique et Bien-être · Cameroun
        </p>
      </div>
    </footer>
  );
}

const iconProps = {
  viewBox: "0 0 24 24",
  className: "size-4 shrink-0 text-rose-light",
  fill: "none",
  stroke: "currentColor",
  strokeWidth: 2,
  strokeLinecap: "round",
  strokeLinejoin: "round",
  "aria-hidden": true,
} as const;

export function PhoneIcon() {
  return (
    <svg {...iconProps}>
      <path d="M22 16.9v3a2 2 0 0 1-2.2 2 19.8 19.8 0 0 1-8.6-3.1 19.5 19.5 0 0 1-6-6A19.8 19.8 0 0 1 2.1 4.2 2 2 0 0 1 4.1 2h3a2 2 0 0 1 2 1.7c.1 1 .4 1.9.7 2.8a2 2 0 0 1-.5 2.1L8 9.9a16 16 0 0 0 6 6l1.3-1.3a2 2 0 0 1 2.1-.4c.9.3 1.8.6 2.8.7a2 2 0 0 1 1.7 2z" />
    </svg>
  );
}

function ChatIcon() {
  return (
    <svg {...iconProps}>
      <path d="M21 11.5a8.4 8.4 0 0 1-12.3 7.5L3 21l2-5.5A8.4 8.4 0 1 1 21 11.5z" />
    </svg>
  );
}

export function PinIcon() {
  return (
    <svg {...iconProps}>
      <path d="M12 22s7-6.2 7-12a7 7 0 1 0-14 0c0 5.8 7 12 7 12z" />
      <circle cx="12" cy="10" r="2.5" />
    </svg>
  );
}
