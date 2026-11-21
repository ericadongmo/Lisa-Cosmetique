import { formatXaf } from "@/lib/format";

/** Pourcentage de remise arrondi, ou null si le produit n'est pas en promotion. */
export function discountPercent(price: number, compareAt?: number | null): number | null {
  if (!compareAt || compareAt <= price) return null;
  return Math.round(((compareAt - price) / compareAt) * 100);
}

/**
 * Prix d'un produit : en promotion, le nouveau prix passe en rose et l'ancien est barré.
 * `compareAtPriceXaf` est optionnel pour rester compatible avec une API plus ancienne.
 */
export function ProductPrice({
  priceXaf,
  compareAtPriceXaf,
  size = "sm",
}: {
  priceXaf: number;
  compareAtPriceXaf?: number | null;
  size?: "sm" | "lg";
}) {
  const percent = discountPercent(priceXaf, compareAtPriceXaf);
  const main = size === "lg" ? "text-2xl sm:text-3xl" : "text-base";
  const old = size === "lg" ? "text-base" : "text-xs";

  return (
    <span className="inline-flex flex-wrap items-baseline gap-x-2 gap-y-0.5">
      <span className={`${main} font-bold ${percent ? "text-rose" : "text-noir"}`}>
        {formatXaf(priceXaf)}
      </span>
      {percent ? (
        <>
          <span className={`${old} text-noir-soft line-through`}>
            {formatXaf(compareAtPriceXaf!)}
          </span>
          <span className="sr-only">Remise de {percent} %</span>
        </>
      ) : null}
    </span>
  );
}

/** Étiquette rose « -20 % » posée sur le visuel du produit. */
export function DiscountBadge({
  priceXaf,
  compareAtPriceXaf,
}: {
  priceXaf: number;
  compareAtPriceXaf?: number | null;
}) {
  const percent = discountPercent(priceXaf, compareAtPriceXaf);
  if (!percent) return null;
  return (
    <span
      className="absolute top-3 left-3 z-[1] rounded-md bg-rose px-2 py-1 text-xs font-bold text-white"
      aria-hidden
    >
      −{percent} %
    </span>
  );
}
