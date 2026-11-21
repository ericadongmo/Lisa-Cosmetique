import Link from "next/link";
import { CartLink } from "@/components/cart-link";
import { PhoneIcon, PinIcon } from "@/components/site-footer";
import { CONTACT } from "@/lib/contact";

/** En-tête commun à toutes les pages publiques. */
export function SiteHeader() {
  return (
    <header className="sticky top-0 z-10 border-b-2 border-gold bg-noir text-white">
      {/* Barre de contact */}
      <div className="border-b border-white/10 text-xs">
        <div className="mx-auto flex max-w-6xl items-center justify-between gap-4 px-4 py-1.5 sm:px-6">
          <a
            href={CONTACT.phoneHref}
            className="inline-flex items-center gap-1.5 font-semibold text-white transition-colors hover:text-rose-light"
          >
            <PhoneIcon />
            {CONTACT.phoneDisplay}
          </a>
          <a
            href={CONTACT.mapsHref}
            target="_blank"
            rel="noopener noreferrer"
            className="inline-flex items-center gap-1.5 text-white/75 transition-colors hover:text-rose-light"
          >
            <PinIcon />
            {CONTACT.address}
          </a>
        </div>
      </div>

      <div className="mx-auto flex max-w-6xl items-center justify-between gap-4 px-4 py-3 sm:px-6">
        <Link href="/" className="leading-none" aria-label="Lisa Cosmétique & Bien-être — accueil">
          <span className="block font-display text-2xl font-extrabold uppercase tracking-[0.12em] sm:text-3xl">
            Lisa
          </span>
          <span className="mt-1 block text-[0.6rem] font-semibold uppercase tracking-[0.3em] text-gold-light">
            Cosmétique &amp; Bien-être
          </span>
        </Link>

        <nav className="flex items-center gap-1 text-sm sm:gap-2" aria-label="Navigation principale">
          <Link
            href="/boutique"
            className="hidden rounded-full px-3 py-2 font-semibold text-white/80 transition-colors hover:text-rose-light sm:inline-block"
          >
            Boutique
          </Link>
          <Link
            href="/suivi-commande"
            className="hidden rounded-full px-3 py-2 font-semibold text-white/80 transition-colors hover:text-rose-light sm:inline-block"
          >
            Suivi
          </Link>

          {/* Icônes (toujours visibles, y compris sur mobile) */}
          <Link
            href="/boutique"
            className="inline-flex size-10 items-center justify-center rounded-full text-white transition-colors hover:text-rose-light sm:hidden"
            aria-label="Boutique"
          >
            <svg viewBox="0 0 24 24" className="size-6" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden>
              <circle cx="11" cy="11" r="7" />
              <path d="m20 20-3.5-3.5" />
            </svg>
          </Link>
          <Link
            href="/suivi-commande"
            className="inline-flex size-10 items-center justify-center rounded-full text-white transition-colors hover:text-rose-light sm:hidden"
            aria-label="Suivi de commande"
          >
            <svg viewBox="0 0 24 24" className="size-6" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden>
              <path d="M3 7h11v9H3z" />
              <path d="M14 10h4l3 3v3h-7z" />
              <circle cx="7" cy="18" r="1.8" />
              <circle cx="17" cy="18" r="1.8" />
            </svg>
          </Link>
          <CartLink />
        </nav>
      </div>
    </header>
  );
}
