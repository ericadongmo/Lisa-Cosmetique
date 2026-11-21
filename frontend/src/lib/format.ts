const amountFormatter = new Intl.NumberFormat("fr-FR", { maximumFractionDigits: 0 });

/** Les prix sont des entiers en francs CFA : jamais de centimes. */
export function formatXaf(amount: number): string {
  return `${amountFormatter.format(amount)} FCFA`;
}
