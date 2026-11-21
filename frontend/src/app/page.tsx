import Link from "next/link";
import { Suspense } from "react";
import { FeaturedProducts, FeaturedProductsSkeleton } from "@/components/featured-products";
import { SiteFooter } from "@/components/site-footer";
import { SiteHeader } from "@/components/site-header";

const categories = [
  {
    slug: "produits-cosmetiques",
    name: "Produits cosmétiques",
    hint: "Visage, maquillage, soins ciblés",
  },
  {
    slug: "laits-soins-corporels",
    name: "Laits et soins corporels",
    hint: "Hydratation et éclat au quotidien",
  },
  { slug: "perruques", name: "Perruques", hint: "Naturelles, lisses, bouclées" },
  {
    slug: "meches-extensions",
    name: "Mèches et extensions",
    hint: "Volume et longueur",
  },
  {
    slug: "accessoires-beaute",
    name: "Accessoires de beauté",
    hint: "Pinceaux, outils, petits essentiels",
  },
  {
    slug: "entretien-capillaire",
    name: "Entretien capillaire",
    hint: "Shampoings, huiles, soins",
  },
];

const services = [
  { title: "Livraison à Yaoundé", text: "Et dans les autres villes du Cameroun." },
  { title: "Commande simple", text: "Commandez en quelques étapes, sans créer de compte." },
  { title: "Suivi de commande", text: "Suivez son avancement en ligne à tout moment." },
];

export default function HomePage() {
  return (
    <>
      <SiteHeader />

      <main className="flex-1">
        <section className="relative overflow-hidden">
          <div
            className="absolute -top-32 right-[-10%] size-[28rem] rounded-full bg-blush/50 blur-3xl"
            aria-hidden
          />
          <div className="relative mx-auto max-w-6xl px-4 py-20 sm:px-6 sm:py-28">
            <p className="text-xs font-semibold uppercase tracking-[0.3em] text-rose-deep">
              Beauté · Soins · Cheveux
            </p>
            <h1 className="mt-5 max-w-2xl font-display text-4xl leading-[1.1] font-bold sm:text-5xl">
              La beauté, simplement.
            </h1>
            <p className="mt-6 max-w-lg text-base leading-relaxed text-noir-soft">
              Cosmétiques, soins corporels, perruques et mèches sélectionnés avec soin,
              livrés partout au Cameroun.
            </p>
            <Link
              href="/boutique"
              className="mt-10 inline-block border-b-2 border-rose pb-1 text-sm font-semibold"
            >
              Découvrir la boutique
            </Link>
          </div>
        </section>

        <section className="mx-auto max-w-6xl px-4 pb-16 sm:px-6">
          <div className="flex items-end justify-between gap-4">
            <div>
              <p className="text-xs font-semibold uppercase tracking-[0.3em] text-gold-deep">
                Sélection Lisa
              </p>
              <h2 className="mt-2 font-display text-2xl font-bold sm:text-3xl">
                Produits populaires
              </h2>
            </div>
            <Link
              href="/boutique"
              className="shrink-0 border-b-2 border-rose pb-1 text-sm font-semibold"
            >
              Tout voir
            </Link>
          </div>
          <Suspense fallback={<FeaturedProductsSkeleton />}>
            <FeaturedProducts />
          </Suspense>
        </section>

        <section className="mx-auto max-w-6xl px-4 pb-20 sm:px-6">
          <h2 className="font-display text-2xl font-bold">Nos univers</h2>
          <ul className="mt-8 grid grid-cols-2 gap-3 sm:gap-4 lg:grid-cols-3">
            {categories.map((category) => (
              <li key={category.slug}>
                <Link
                  href={`/boutique?categorie=${category.slug}`}
                  className="block h-full rounded-2xl border border-noir/5 bg-white p-5 transition-colors hover:border-gold sm:p-7"
                >
                  <span className="block h-0.5 w-10 bg-gold" aria-hidden />
                  <h3 className="mt-4 font-display text-lg font-bold sm:text-xl">
                    {category.name}
                  </h3>
                  <p className="mt-1 text-sm text-noir-soft">{category.hint}</p>
                </Link>
              </li>
            ))}
          </ul>
        </section>

        <section className="border-y border-gold/30 bg-blush-soft">
          <ul className="mx-auto grid max-w-6xl gap-8 px-4 py-14 sm:grid-cols-3 sm:px-6">
            {services.map((service) => (
              <li key={service.title}>
                <h3 className="font-display text-lg font-bold">{service.title}</h3>
                <p className="mt-1 text-sm text-noir-soft">{service.text}</p>
              </li>
            ))}
          </ul>
        </section>
      </main>

      <SiteFooter />
    </>
  );
}
