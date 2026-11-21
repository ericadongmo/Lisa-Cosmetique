"use client";

import Link from "next/link";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useCallback, useEffect, useState, type FormEvent } from "react";
import {
  createAdminCredentials,
  getAdminOrder,
  getAdminOrders,
  updateAdminOrderStatus,
  type AdminCredentials,
} from "@/lib/admin-api";
import type {
  AdminOrderStatus,
  AdminOrderSummary,
  AdminPaymentStatus,
} from "@/lib/admin-types";
import { ApiError } from "@/lib/api-client";
import { formatXaf } from "@/lib/format";

const STATUS_LABELS: Record<AdminOrderStatus, string> = {
  PENDING: "En attente",
  CONFIRMED: "Confirmée",
  PROCESSING: "En préparation",
  SHIPPED: "Expédiée",
  DELIVERED: "Livrée",
  CANCELLED: "Annulée",
};

const STATUS_STYLES: Record<AdminOrderStatus, string> = {
  PENDING: "bg-amber-50 text-amber-800",
  CONFIRMED: "bg-sky-50 text-sky-800",
  PROCESSING: "bg-indigo-50 text-indigo-800",
  SHIPPED: "bg-violet-50 text-violet-800",
  DELIVERED: "bg-emerald-50 text-emerald-800",
  CANCELLED: "bg-red-50 text-red-700",
};

const PAYMENT_LABELS: Record<AdminPaymentStatus, string> = {
  PENDING: "À convenir",
  PROCESSING: "En cours",
  SUCCESS: "Payé",
  FAILED: "Échoué",
  CANCELLED: "Annulé",
  REFUNDED: "Remboursé",
};

const NEXT_STATUS: Partial<
  Record<AdminOrderStatus, { status: AdminOrderStatus; label: string }>
> = {
  PENDING: { status: "CONFIRMED", label: "Confirmer la commande" },
  CONFIRMED: { status: "PROCESSING", label: "Passer en préparation" },
  PROCESSING: { status: "SHIPPED", label: "Marquer comme expédiée" },
  SHIPPED: { status: "DELIVERED", label: "Marquer comme livrée" },
};

const ADDRESS_FIELDS: { key: string; label: string }[] = [
  { key: "recipientName", label: "Destinataire" },
  { key: "phoneNumber", label: "Téléphone" },
  { key: "city", label: "Ville" },
  { key: "neighborhood", label: "Quartier" },
  { key: "streetAddress", label: "Adresse" },
  { key: "additionalInformation", label: "Complément" },
];

const dateFormatter = new Intl.DateTimeFormat("fr-FR", {
  dateStyle: "medium",
  timeStyle: "short",
});

function isUnauthorized(error: unknown): boolean {
  return error instanceof ApiError && (error.status === 401 || error.status === 403);
}

function errorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.status === 404) {
      return "Service indisponible ou commande introuvable. Veuillez réessayer dans quelques instants.";
    }
    return error.message;
  }
  return "Une erreur réseau est survenue. Réessayez.";
}

export function AdminOrdersView() {
  const [auth, setAuth] = useState<AdminCredentials | null>(null);
  const handleLogout = useCallback(() => setAuth(null), []);

  if (!auth) {
    return <AdminLogin onAuthenticated={setAuth} />;
  }

  return <AdminConsole auth={auth} onLogout={handleLogout} />;
}

function AdminLogin({
  onAuthenticated,
}: {
  onAuthenticated: (auth: AdminCredentials) => void;
}) {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    setPending(true);
    const credentials = createAdminCredentials(email.trim(), password);
    try {
      await getAdminOrders(credentials, { size: 1 });
      onAuthenticated(credentials);
    } catch (caught) {
      setError(
        isUnauthorized(caught)
          ? "Identifiants administrateur incorrects."
          : errorMessage(caught),
      );
    } finally {
      setPending(false);
    }
  }

  return (
    <div className="mx-auto mt-8 max-w-md rounded-2xl border border-noir/5 bg-white p-6 sm:p-8">
      <h2 className="font-display text-xl font-bold">Connexion administrateur</h2>
      <p className="mt-2 text-sm leading-relaxed text-noir-soft">
        Accès réservé à l&apos;équipe de la boutique. Les identifiants ne sont conservés que
        dans la mémoire de cet onglet.
      </p>
      <form onSubmit={handleSubmit} className="mt-6 grid gap-4">
        <label className="block text-sm">
          <span className="mb-1.5 block text-noir-soft">E-mail</span>
          <input
            type="email"
            required
            autoComplete="username"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            className="w-full rounded-xl border border-noir/10 bg-white px-4 py-2.5 text-sm outline-none focus:border-rose"
          />
        </label>
        <label className="block text-sm">
          <span className="mb-1.5 block text-noir-soft">Mot de passe</span>
          <input
            type="password"
            required
            autoComplete="current-password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            className="w-full rounded-xl border border-noir/10 bg-white px-4 py-2.5 text-sm outline-none focus:border-rose"
          />
        </label>
        {error ? (
          <p className="rounded-xl bg-red-50 px-4 py-3 text-sm text-red-700" role="alert">
            {error}
          </p>
        ) : null}
        <button
          type="submit"
          disabled={pending}
          className="mt-1 w-full rounded-full bg-noir px-6 py-3 text-sm text-blanc font-semibold hover:bg-rose disabled:cursor-not-allowed disabled:opacity-60"
        >
          {pending ? "Connexion…" : "Se connecter"}
        </button>
      </form>
      <p className="mt-5 text-center text-xs text-noir-soft">
        <Link href="/" className="underline-offset-4 hover:underline">
          Retour à la boutique
        </Link>
      </p>
    </div>
  );
}

function AdminConsole({
  auth,
  onLogout,
}: {
  auth: AdminCredentials;
  onLogout: () => void;
}) {
  const [status, setStatus] = useState<AdminOrderStatus | "">("");
  const [search, setSearch] = useState("");
  const [query, setQuery] = useState("");
  const [page, setPage] = useState(0);
  const [selectedId, setSelectedId] = useState<string | null>(null);

  const ordersQuery = useQuery({
    queryKey: ["admin-orders", auth.header, status, query, page],
    queryFn: () => getAdminOrders(auth, { status, q: query, page, size: 12 }),
  });

  useEffect(() => {
    if (isUnauthorized(ordersQuery.error)) {
      onLogout();
    }
  }, [ordersQuery.error, onLogout]);

  function handleSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setPage(0);
    setQuery(search.trim());
  }

  const orders = ordersQuery.data?.content ?? [];

  return (
    <div>
      <div className="flex flex-wrap items-center justify-between gap-3 border-b border-noir/5 pb-5">
        <div>
          <p className="text-xs font-semibold uppercase tracking-[0.25em] text-rose-deep">Administration</p>
          <p className="mt-1 text-sm text-noir-soft">Connecté en tant que {auth.email}</p>
        </div>
        <div className="flex flex-wrap items-center gap-3 text-sm">
          <Link href="/" className="text-noir-soft hover:text-noir">
            Voir la boutique
          </Link>
          <button
            type="button"
            onClick={onLogout}
            className="rounded-full border border-noir/10 px-4 py-2 text-xs hover:border-rose"
          >
            Se déconnecter
          </button>
        </div>
      </div>

      <form onSubmit={handleSearch} className="mt-6 flex flex-wrap gap-3">
        <input
          type="search"
          value={search}
          onChange={(event) => setSearch(event.target.value)}
          placeholder="Numéro, nom, téléphone…"
          className="min-w-56 flex-1 rounded-xl border border-noir/10 bg-white px-4 py-2.5 text-sm outline-none focus:border-rose"
        />
        <select
          value={status}
          onChange={(event) => {
            setStatus(event.target.value as AdminOrderStatus | "");
            setPage(0);
          }}
          className="rounded-xl border border-noir/10 bg-white px-3 py-2.5 text-sm outline-none focus:border-rose"
        >
          <option value="">Tous les statuts</option>
          {(Object.keys(STATUS_LABELS) as AdminOrderStatus[]).map((value) => (
            <option key={value} value={value}>
              {STATUS_LABELS[value]}
            </option>
          ))}
        </select>
        <button
          type="submit"
          className="rounded-full bg-noir px-5 py-2.5 text-sm text-blanc font-semibold hover:bg-rose"
        >
          Rechercher
        </button>
      </form>

      {ordersQuery.isError && !isUnauthorized(ordersQuery.error) ? (
        <p className="mt-5 rounded-xl bg-red-50 px-4 py-3 text-sm text-red-700" role="alert">
          {errorMessage(ordersQuery.error)}
        </p>
      ) : null}

      <div className="mt-6 grid gap-3 lg:grid-cols-2">
        {ordersQuery.isLoading
          ? Array.from({ length: 4 }).map((_, index) => (
              <div key={index} className="h-32 animate-pulse rounded-2xl bg-white" aria-hidden />
            ))
          : orders.map((order) => (
              <OrderCard
                key={order.id}
                order={order}
                onOpen={() => setSelectedId(order.id)}
              />
            ))}
      </div>

      {!ordersQuery.isLoading && orders.length === 0 ? (
        <p className="mt-8 rounded-2xl border border-dashed border-noir/10 px-5 py-10 text-center text-sm text-noir-soft">
          Aucune commande ne correspond à ces critères.
        </p>
      ) : null}

      {ordersQuery.data && ordersQuery.data.totalPages > 1 ? (
        <div className="mt-6 flex items-center justify-between text-sm">
          <button
            type="button"
            disabled={page === 0}
            onClick={() => setPage((current) => Math.max(0, current - 1))}
            className="rounded-full border border-noir/10 px-4 py-2 disabled:opacity-40"
          >
            Précédent
          </button>
          <span className="text-xs text-noir-soft">
            Page {ordersQuery.data.page + 1} sur {ordersQuery.data.totalPages} ·{" "}
            {ordersQuery.data.totalElements} commande(s)
          </span>
          <button
            type="button"
            disabled={ordersQuery.data.page + 1 >= ordersQuery.data.totalPages}
            onClick={() => setPage((current) => current + 1)}
            className="rounded-full border border-noir/10 px-4 py-2 disabled:opacity-40"
          >
            Suivant
          </button>
        </div>
      ) : null}

      {selectedId ? (
        <OrderDetailPanel
          auth={auth}
          orderId={selectedId}
          onClose={() => setSelectedId(null)}
          onLogout={onLogout}
        />
      ) : null}
    </div>
  );
}

function OrderCard({
  order,
  onOpen,
}: {
  order: AdminOrderSummary;
  onOpen: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onOpen}
      className="rounded-2xl border border-noir/5 bg-white p-5 text-left transition-shadow hover:shadow-[0_16px_40px_-24px_rgba(17,17,17,0.35)]"
    >
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="font-mono text-sm font-medium">{order.orderNumber}</p>
          <p className="mt-1 text-xs text-noir-soft">
            {order.customerName} · {order.customerPhone}
          </p>
        </div>
        <span className={`rounded-full px-2.5 py-1 text-[0.65rem] ${STATUS_STYLES[order.orderStatus]}`}>
          {STATUS_LABELS[order.orderStatus]}
        </span>
      </div>
      <div className="mt-4 flex items-end justify-between gap-3 text-sm">
        <div>
          <p className="text-xs text-noir-soft">
            {order.deliveryCity} · {dateFormatter.format(new Date(order.createdAt))}
          </p>
          <p className="mt-1 text-xs text-noir-soft">
            Paiement : {PAYMENT_LABELS[order.paymentStatus]}
          </p>
        </div>
        <span className="font-medium">{formatXaf(order.totalXaf)}</span>
      </div>
    </button>
  );
}

function OrderDetailPanel({
  auth,
  orderId,
  onClose,
  onLogout,
}: {
  auth: AdminCredentials;
  orderId: string;
  onClose: () => void;
  onLogout: () => void;
}) {
  const queryClient = useQueryClient();
  const [trackingNumber, setTrackingNumber] = useState("");

  const detailQuery = useQuery({
    queryKey: ["admin-order", auth.header, orderId],
    queryFn: () => getAdminOrder(auth, orderId),
  });

  const statusMutation = useMutation({
    mutationFn: (payload: { status: AdminOrderStatus; trackingNumber?: string }) =>
      updateAdminOrderStatus(auth, orderId, payload),
    onSuccess: (updated) => {
      queryClient.setQueryData(["admin-order", auth.header, orderId], updated);
      queryClient.invalidateQueries({ queryKey: ["admin-orders"] });
    },
  });

  useEffect(() => {
    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") {
        onClose();
      }
    }
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [onClose]);

  useEffect(() => {
    if (isUnauthorized(detailQuery.error) || isUnauthorized(statusMutation.error)) {
      onLogout();
    }
  }, [detailQuery.error, statusMutation.error, onLogout]);

  const detail = detailQuery.data;
  const next = detail ? NEXT_STATUS[detail.orderStatus] : undefined;
  const cancellable =
    detail?.orderStatus === "PENDING" || detail?.orderStatus === "CONFIRMED";

  function advance() {
    if (!detail || !next) return;
    statusMutation.mutate({
      status: next.status,
      trackingNumber:
        next.status === "SHIPPED" && trackingNumber.trim()
          ? trackingNumber.trim()
          : undefined,
    });
  }

  function cancelOrder() {
    if (!detail) return;
    if (
      !window.confirm(
        "Annuler cette commande ? Le stock sera restitué. Cette action est définitive.",
      )
    ) {
      return;
    }
    statusMutation.mutate({ status: "CANCELLED" });
  }

  return (
    <div className="fixed inset-0 z-30 flex justify-end">
      <button
        type="button"
        aria-label="Fermer le détail"
        onClick={onClose}
        className="absolute inset-0 bg-noir/30 backdrop-blur-[1px]"
      />
      <aside
        role="dialog"
        aria-modal="true"
        aria-label="Détail de la commande"
        className="relative h-full w-full max-w-xl overflow-y-auto bg-blanc p-5 shadow-2xl sm:p-7"
      >
        <div className="flex items-center justify-between gap-3">
          <h2 className="font-display text-xl font-bold">Détail de la commande</h2>
          <button
            type="button"
            onClick={onClose}
            className="rounded-full border border-noir/10 px-3 py-1.5 text-xs hover:border-rose"
          >
            Fermer
          </button>
        </div>

        {detailQuery.isLoading ? (
          <div className="mt-6 h-96 animate-pulse rounded-2xl bg-white" aria-hidden />
        ) : detailQuery.isError ? (
          <p className="mt-6 rounded-xl bg-red-50 px-4 py-3 text-sm text-red-700" role="alert">
            {errorMessage(detailQuery.error)}
          </p>
        ) : detail ? (
          <>
            <div className="mt-5 flex flex-wrap items-center justify-between gap-3">
              <p className="font-mono text-sm">{detail.orderNumber}</p>
              <span
                className={`rounded-full px-3 py-1 text-xs ${STATUS_STYLES[detail.orderStatus]}`}
              >
                {STATUS_LABELS[detail.orderStatus]}
              </span>
            </div>

            <section className="mt-5 rounded-2xl border border-noir/5 bg-white p-5 text-sm">
              <h3 className="text-xs uppercase tracking-wide text-noir-soft">Client</h3>
              <p className="mt-2 font-medium">{detail.customerName}</p>
              <p className="mt-1">{detail.customerPhone}</p>
              {detail.customerEmail ? <p className="mt-1">{detail.customerEmail}</p> : null}
              <p className="mt-1 text-xs text-noir-soft">
                Paiement : {PAYMENT_LABELS[detail.paymentStatus]}
              </p>
            </section>

            <section className="mt-4 rounded-2xl border border-noir/5 bg-white p-5 text-sm">
              <h3 className="text-xs uppercase tracking-wide text-noir-soft">
                Livraison — {detail.deliveryCity}
              </h3>
              <p className="mt-2 font-medium">{detail.deliveryZoneName}</p>
              <dl className="mt-2 space-y-1">
                {ADDRESS_FIELDS.flatMap(({ key, label }) => {
                  const value = detail.deliveryAddress?.[key];
                  return value ? (
                    <div key={key} className="flex gap-2">
                      <dt className="min-w-28 text-noir-soft">{label}</dt>
                      <dd>{value}</dd>
                    </div>
                  ) : (
                    []
                  );
                })}
              </dl>
              {detail.notes ? (
                <p className="mt-3 rounded-xl bg-blush-soft/50 px-3 py-2 text-xs">
                  Note : {detail.notes}
                </p>
              ) : null}
            </section>

            <section className="mt-4 rounded-2xl border border-noir/5 bg-white p-5">
              <h3 className="text-xs uppercase tracking-wide text-noir-soft">Articles</h3>
              <ul className="mt-3 space-y-2 text-sm">
                {detail.items.map((item, index) => (
                  <li key={`${item.sku}-${index}`} className="flex justify-between gap-3">
                    <span>
                      {item.productName}{" "}
                      <span className="text-xs text-noir-soft">× {item.quantity}</span>
                    </span>
                    <span>{formatXaf(item.subtotalXaf)}</span>
                  </li>
                ))}
              </ul>
              <dl className="mt-4 space-y-1 border-t border-noir/5 pt-3 text-sm">
                <div className="flex justify-between">
                  <dt className="text-noir-soft">Sous-total</dt>
                  <dd>{formatXaf(detail.subtotalXaf)}</dd>
                </div>
                <div className="flex justify-between">
                  <dt className="text-noir-soft">Livraison</dt>
                  <dd>{formatXaf(detail.deliveryFeeXaf)}</dd>
                </div>
                {detail.discountXaf > 0 ? (
                  <div className="flex justify-between">
                    <dt className="text-noir-soft">Remise</dt>
                    <dd>−{formatXaf(detail.discountXaf)}</dd>
                  </div>
                ) : null}
                <div className="flex justify-between pt-1 text-base font-medium">
                  <dt>Total</dt>
                  <dd>{formatXaf(detail.totalXaf)}</dd>
                </div>
              </dl>
            </section>

            {detail.shipment ? (
              <section className="mt-4 rounded-2xl border border-noir/5 bg-white p-5 text-sm">
                <h3 className="text-xs uppercase tracking-wide text-noir-soft">Expédition</h3>
                <p className="mt-2">
                  {detail.shipment.trackingNumber
                    ? `Suivi : ${detail.shipment.trackingNumber}`
                    : "Sans numéro de suivi transporteur"}
                </p>
                <p className="mt-1 text-xs text-noir-soft">
                  {detail.shipment.shippedAt
                    ? `Expédiée le ${dateFormatter.format(new Date(detail.shipment.shippedAt))}`
                    : "Pas encore expédiée"}
                  {detail.shipment.deliveredAt
                    ? ` · Livrée le ${dateFormatter.format(new Date(detail.shipment.deliveredAt))}`
                    : ""}
                </p>
              </section>
            ) : null}

            <section className="mt-5 rounded-2xl border border-noir/5 bg-white p-5">
              <h3 className="text-xs uppercase tracking-wide text-noir-soft">Actions</h3>

              {detail.orderStatus === "PROCESSING" ? (
                <label className="mt-3 block text-sm">
                  <span className="mb-1.5 block text-noir-soft">
                    Numéro de suivi transporteur (facultatif)
                  </span>
                  <input
                    value={trackingNumber}
                    onChange={(event) => setTrackingNumber(event.target.value)}
                    maxLength={64}
                    placeholder="Ex. TRACK-001"
                    className="w-full rounded-xl border border-noir/10 bg-white px-4 py-2.5 text-sm outline-none focus:border-rose"
                  />
                </label>
              ) : null}

              {statusMutation.error && !isUnauthorized(statusMutation.error) ? (
                <p className="mt-3 rounded-xl bg-red-50 px-4 py-3 text-sm text-red-700" role="alert">
                  {errorMessage(statusMutation.error)}
                </p>
              ) : null}

              <div className="mt-4 flex flex-wrap gap-3">
                {next ? (
                  <button
                    type="button"
                    onClick={advance}
                    disabled={statusMutation.isPending}
                    className="rounded-full bg-noir px-5 py-2.5 text-sm text-blanc font-semibold hover:bg-rose disabled:opacity-60"
                  >
                    {statusMutation.isPending ? "Mise à jour…" : next.label}
                  </button>
                ) : null}
                {cancellable ? (
                  <button
                    type="button"
                    onClick={cancelOrder}
                    disabled={statusMutation.isPending}
                    className="rounded-full border border-red-200 px-5 py-2.5 text-sm text-red-700 hover:bg-red-50 disabled:opacity-60"
                  >
                    Annuler la commande
                  </button>
                ) : null}
                {!next && !cancellable ? (
                  <p className="text-sm text-noir-soft">Cette commande est clôturée.</p>
                ) : null}
              </div>
            </section>
          </>
        ) : null}
      </aside>
    </div>
  );
}
