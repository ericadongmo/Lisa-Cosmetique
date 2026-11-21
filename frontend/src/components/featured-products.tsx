import { ProductCard } from "@/components/product-card";
import { getProducts } from "@/lib/catalog";

/** Produits mis en avant (`featured`) affichés sur la page d'accueil. */
export async function FeaturedProducts() {
  const result = await getProducts({ featured: true, size: 8, sort: "newest" });

  if (result.status === "unavailable") {
    return (
      <p className="mt-8 rounded-2xl bg-blush-soft px-5 py-6 text-sm text-noir-soft">
        Les produits sont momentanément indisponibles. Réessayez dans quelques instants.
      </p>
    );
  }

  if (result.data.content.length === 0) {
    return null;
  }

  return (
    <ul className="mt-8 grid grid-cols-2 gap-3 sm:gap-5 lg:grid-cols-4">
      {result.data.content.map((product) => (
        <li key={product.id}>
          <ProductCard product={product} />
        </li>
      ))}
    </ul>
  );
}

export function FeaturedProductsSkeleton() {
  return (
    <ul className="mt-8 grid grid-cols-2 gap-3 sm:gap-5 lg:grid-cols-4" aria-hidden>
      {Array.from({ length: 4 }, (_, i) => (
        <li key={i} className="overflow-hidden rounded-2xl border border-noir/5 bg-white">
          <div className="aspect-[4/5] animate-pulse bg-blush-soft" />
          <div className="space-y-2 p-4">
            <div className="h-4 w-3/4 rounded bg-blush-soft" />
            <div className="h-3 w-1/2 rounded bg-blush-soft" />
          </div>
        </li>
      ))}
    </ul>
  );
}
