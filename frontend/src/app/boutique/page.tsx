import Link from "next/link";
import { Suspense } from "react";
import { ProductCard } from "@/components/product-card";
import { SiteFooter } from "@/components/site-footer";
import { SiteHeader } from "@/components/site-header";
import { getCategories, getProducts } from "@/lib/catalog";
import type { Category } from "@/lib/catalog-types";

export const metadata = {
  title: "Boutique",
  description:
    "Cosmétiques, soins corporels, perruques, mèches et accessoires de beauté — livrés au Cameroun.",
};

type SearchParams = Promise<Record<string, string | string[] | undefined>>;

const SORT_OPTIONS = [
  { value: "name", label: "Nom" },
  { value: "price-asc", label: "Prix croissant" },
  { value: "price-desc", label: "Prix décroissant" },
  { value: "newest", label: "Nouveautés" },
];

export default function BoutiquePage({ searchParams }: { searchParams: SearchParams }) {
  return (
    <>
      <SiteHeader />
      <main className="mx-auto w-full max-w-6xl flex-1 px-4 py-12 sm:px-6 sm:py-16">
        <p className="text-xs font-semibold uppercase tracking-[0.3em] text-rose-deep">La boutique</p>
        <h1 className="mt-3 font-display text-3xl font-bold sm:text-4xl">Nos produits</h1>
        <p className="mt-4 max-w-xl text-sm leading-relaxed text-noir-soft">
          Une sélection de cosmétiques, soins et accessoires capillaires, livrée à Yaoundé et
          dans les autres villes du Cameroun.
        </p>

        <Suspense fallback={<CatalogSkeleton />}>
          <Catalog searchParams={searchParams} />
        </Suspense>
      </main>
      <SiteFooter />
    </>
  );
}

async function Catalog({ searchParams }: { searchParams: SearchParams }) {
  const params = await searchParams;
  const category = first(params.categorie);
  const query = first(params.q);
  const sort = first(params.tri) ?? "name";

  const [categories, result] = await Promise.all([
    getCategories(),
    getProducts({ category, q: query, sort, size: 24 }),
  ]);

  return (
    <>
      <div className="mt-8 flex flex-col gap-4 border-y border-noir/5 py-4 sm:flex-row sm:items-center sm:justify-between">
        <CategoryFilters categories={categories} active={category} query={query} sort={sort} />
        <form action="/boutique" className="flex items-center gap-2">
          {category ? <input type="hidden" name="categorie" value={category} /> : null}
          <input
            type="search"
            name="q"
            defaultValue={query ?? ""}
            placeholder="Rechercher un produit…"
            aria-label="Rechercher un produit"
            className="w-full rounded-full border border-noir/10 bg-white px-4 py-2 text-sm outline-none focus:border-rose sm:w-56"
          />
          <button
            type="submit"
            className="rounded-full bg-noir px-4 py-2 text-sm text-blanc font-semibold transition-colors hover:bg-rose"
          >
            Chercher
          </button>
        </form>
      </div>

      {result.status === "unavailable" ? (
        <Unavailable />
      ) : result.data.content.length === 0 ? (
        <div className="py-16 text-center">
          <p className="font-display text-xl font-bold">Aucun produit trouvé</p>
          <p className="mt-2 text-sm text-noir-soft">
            Essayez une autre recherche ou parcourez toutes nos catégories.
          </p>
          <Link
            href="/boutique"
            className="mt-6 inline-block border-b-2 border-rose pb-1 text-sm font-semibold"
          >
            Voir tous les produits
          </Link>
        </div>
      ) : (
        <>
          <div className="mt-6 flex flex-wrap items-center justify-between gap-3 text-sm text-noir-soft">
            <p>
              {result.data.totalElements} produit{result.data.totalElements > 1 ? "s" : ""}
            </p>
            <div className="flex flex-wrap items-center gap-3">
              <span className="text-xs font-semibold uppercase tracking-[0.2em] text-noir">Trier</span>
              {SORT_OPTIONS.map((option) => (
                <Link
                  key={option.value}
                  href={buildHref({ category, query, sort: option.value })}
                  className={
                    sort === option.value
                      ? "border-b-2 border-rose pb-0.5 font-semibold text-noir"
                      : "font-medium text-noir-soft hover:text-rose-deep"
                  }
                >
                  {option.label}
                </Link>
              ))}
            </div>
          </div>

          <ul className="mt-6 grid grid-cols-2 gap-3 sm:gap-5 lg:grid-cols-3">
            {result.data.content.map((product) => (
              <li key={product.id}>
                <ProductCard product={product} />
              </li>
            ))}
          </ul>
        </>
      )}
    </>
  );
}

function CategoryFilters({
  categories,
  active,
  query,
  sort,
}: {
  categories: Category[];
  active?: string;
  query?: string;
  sort: string;
}) {
  if (categories.length === 0) {
    return <p className="text-sm text-noir-soft">Toutes nos catégories</p>;
  }

  return (
    <ul className="flex flex-wrap gap-2">
      <li>
        <Link
          href={buildHref({ query, sort })}
          className={
            active === undefined
              ? "inline-block rounded-full bg-noir px-4 py-2 text-sm text-blanc font-semibold"
              : "inline-block rounded-full border border-noir/10 bg-white px-4 py-2 text-sm font-medium text-noir hover:border-rose hover:text-rose-deep"
          }
        >
          Tous
        </Link>
      </li>
      {categories.map((category) => (
        <li key={category.id}>
          <Link
            href={buildHref({ category: category.slug, query, sort })}
            className={
              active === category.slug
                ? "inline-block rounded-full bg-noir px-4 py-2 text-sm text-blanc font-semibold"
                : "inline-block rounded-full border border-noir/10 bg-white px-4 py-2 text-sm font-medium text-noir hover:border-rose hover:text-rose-deep"
            }
          >
            {category.name}
          </Link>
        </li>
      ))}
    </ul>
  );
}

function Unavailable() {
  return (
    <div className="py-16 text-center">
      <p className="font-display text-xl font-bold">La boutique est momentanément indisponible</p>
      <p className="mx-auto mt-2 max-w-md text-sm text-noir-soft">
        Le catalogue n&apos;a pas pu être chargé. Veuillez réessayer dans quelques instants.
      </p>
    </div>
  );
}

function CatalogSkeleton() {
  return (
    <div className="mt-10 grid grid-cols-2 gap-3 sm:gap-5 lg:grid-cols-3" aria-hidden>
      {Array.from({ length: 6 }).map((_, index) => (
        <div key={index} className="animate-pulse overflow-hidden rounded-2xl bg-white">
          <div className="aspect-[4/5] bg-blush-soft" />
          <div className="space-y-2 p-5">
            <div className="h-4 w-3/4 rounded bg-blush-soft" />
            <div className="h-3 w-1/2 rounded bg-blush-soft" />
          </div>
        </div>
      ))}
    </div>
  );
}

function first(value: string | string[] | undefined): string | undefined {
  const raw = Array.isArray(value) ? value[0] : value;
  const trimmed = raw?.trim();
  return trimmed ? trimmed : undefined;
}

function buildHref({
  category,
  query,
  sort,
}: {
  category?: string;
  query?: string;
  sort?: string;
}): string {
  const params = new URLSearchParams();
  if (category) params.set("categorie", category);
  if (query) params.set("q", query);
  if (sort && sort !== "name") params.set("tri", sort);
  const search = params.toString();
  return search ? `/boutique?${search}` : "/boutique";
}
