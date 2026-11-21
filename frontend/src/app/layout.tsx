import type { Metadata } from "next";
import localFont from "next/font/local";
import { Providers } from "@/components/providers";
import "./globals.css";

/*
 * Polices auto-hébergées (src/assets/fonts) : aucune dépendance à Google Fonts
 * ni à la compilation ni à l'affichage. Sous-ensemble « latin » (accents français inclus).
 */

/** Titres — Montserrat, police variable (graisses 100 à 900 dans un seul fichier). */
const montserrat = localFont({
  src: "../assets/fonts/Montserrat-Variable.woff2",
  variable: "--font-montserrat",
  weight: "100 900",
  display: "swap",
});

/** Texte courant — Poppins, une graisse par fichier. */
const poppins = localFont({
  src: [
    { path: "../assets/fonts/Poppins-400.woff2", weight: "400", style: "normal" },
    { path: "../assets/fonts/Poppins-500.woff2", weight: "500", style: "normal" },
    { path: "../assets/fonts/Poppins-600.woff2", weight: "600", style: "normal" },
    { path: "../assets/fonts/Poppins-700.woff2", weight: "700", style: "normal" },
  ],
  variable: "--font-poppins",
  display: "swap",
});

export const metadata: Metadata = {
  title: {
    default: "Lisa Cosmétique et Bien-être",
    template: "%s · Lisa Cosmétique et Bien-être",
  },
  description:
    "Cosmétiques, soins corporels, perruques, mèches et accessoires de beauté. Livraison au Cameroun.",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html
      lang="fr"
      className={`${montserrat.variable} ${poppins.variable} h-full antialiased`}
    >
      <body className="flex min-h-full flex-col font-sans">
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}
