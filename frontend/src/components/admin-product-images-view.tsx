"use client";

import Image from "next/image";
import Link from "next/link";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useCallback, useEffect, useState, type FormEvent } from "react";
import {
  createAdminCredentials,
  deleteAdminProductImage,
  getAdminProductImages,
  getAdminProducts,
  uploadAdminProductImage,
  type AdminCredentials,
} from "@/lib/admin-api";
import type { AdminProductImage } from "@/lib/admin-types";
import { ApiError } from "@/lib/api-client";

const MAX_IMAGE_SIZE = 5 * 1024 * 1024;

function isUnauthorized(error: unknown): boolean {
  return error instanceof ApiError && (error.status === 401 || error.status === 403);
}

function messageFor(error: unknown): string {
  if (error instanceof ApiError) return error.message;
  return "Une erreur réseau est survenue. Vérifiez votre connexion, puis réessayez.";
}

export function AdminProductImagesView() {
  const [auth, setAuth] = useState<AdminCredentials | null>(null);
  const logout = useCallback(() => setAuth(null), []);
  if (!auth) return <AdminImageLogin onAuthenticated={setAuth} />;
  return <AdminImageConsole auth={auth} onLogout={logout} />;
}

function AdminImageLogin({
  onAuthenticated,
}: {
  onAuthenticated: (auth: AdminCredentials) => void;
}) {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    setPending(true);
    const credentials = createAdminCredentials(email.trim(), password);
    try {
      await getAdminProducts(credentials, 1);
      onAuthenticated(credentials);
      setPassword("");
    } catch (caught) {
      setError(isUnauthorized(caught) ? "Identifiants administrateur incorrects." : messageFor(caught));
    } finally {
      setPending(false);
    }
  }

  return (
    <section className="mx-auto mt-8 max-w-md rounded-2xl border border-noir/5 bg-white p-6 sm:p-8">
      <h2 className="font-display text-xl font-bold">Connexion administrateur</h2>
      <p className="mt-2 text-sm leading-relaxed text-noir-soft">
        Les identifiants ne sont conservés que dans la mémoire de cet onglet.
      </p>
      <form onSubmit={submit} className="mt-6 grid gap-4">
        <label className="block text-sm">
          <span className="mb-1.5 block text-noir-soft">E-mail</span>
          <input
            type="email"
            autoComplete="username"
            required
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            className="w-full rounded-xl border border-noir/10 bg-white px-4 py-2.5 outline-none focus:border-rose"
          />
        </label>
        <label className="block text-sm">
          <span className="mb-1.5 block text-noir-soft">Mot de passe</span>
          <input
            type="password"
            autoComplete="current-password"
            required
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            className="w-full rounded-xl border border-noir/10 bg-white px-4 py-2.5 outline-none focus:border-rose"
          />
        </label>
        {error ? <p className="rounded-xl bg-red-50 px-4 py-3 text-sm text-red-700" role="alert">{error}</p> : null}
        <button
          type="submit"
          disabled={pending}
          className="rounded-full bg-noir px-6 py-3 text-sm text-blanc font-semibold hover:bg-rose disabled:opacity-60"
        >
          {pending ? "Connexion…" : "Se connecter"}
        </button>
      </form>
      <Link href="/admin/commandes" className="mt-5 inline-block text-sm text-noir-soft underline-offset-4 hover:underline">
        Retour aux commandes
      </Link>
    </section>
  );
}

function AdminImageConsole({
  auth,
  onLogout,
}: {
  auth: AdminCredentials;
  onLogout: () => void;
}) {
  const [selectedProductId, setSelectedProductId] = useState("");
  const [altText, setAltText] = useState("");
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [formError, setFormError] = useState<string | null>(null);
  const queryClient = useQueryClient();
  const productsQuery = useQuery({
    queryKey: ["admin-products-for-images", auth.header],
    queryFn: () => getAdminProducts(auth),
  });
  const activeProductId = selectedProductId || productsQuery.data?.content[0]?.id || "";
  const imagesQuery = useQuery({
    queryKey: ["admin-product-images", auth.header, activeProductId],
    queryFn: () => getAdminProductImages(auth, activeProductId),
    enabled: Boolean(activeProductId),
  });
  const uploadMutation = useMutation({
    mutationFn: (input: { file: File; primary: boolean }) =>
      uploadAdminProductImage(auth, activeProductId, input.file, altText.trim(), input.primary),
    onSuccess: async () => {
      setSelectedFile(null);
      setAltText("");
      setFormError(null);
      await queryClient.invalidateQueries({ queryKey: ["admin-product-images", auth.header, activeProductId] });
      await queryClient.invalidateQueries({ queryKey: ["catalog-products"] });
    },
  });
  const deleteMutation = useMutation({
    mutationFn: (imageId: string) => deleteAdminProductImage(auth, activeProductId, imageId),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["admin-product-images", auth.header, activeProductId] });
      await queryClient.invalidateQueries({ queryKey: ["catalog-products"] });
    },
  });

  useEffect(() => {
    const error = productsQuery.error ?? imagesQuery.error ?? uploadMutation.error ?? deleteMutation.error;
    if (isUnauthorized(error)) onLogout();
  }, [productsQuery.error, imagesQuery.error, uploadMutation.error, deleteMutation.error, onLogout]);

  function submitUpload(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setFormError(null);
    if (!selectedFile) {
      setFormError("Choisissez une image.");
      return;
    }
    if (selectedFile.size > MAX_IMAGE_SIZE) {
      setFormError("L’image ne doit pas dépasser 5 Mo.");
      return;
    }
    uploadMutation.mutate({
      file: selectedFile,
      primary: new FormData(event.currentTarget).get("primary") === "on",
    });
  }

  const products = productsQuery.data?.content ?? [];

  return (
    <section className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <Link href="/admin/commandes" className="text-sm text-noir-soft underline-offset-4 hover:underline">
          ← Commandes
        </Link>
        <button type="button" onClick={onLogout} className="text-sm text-noir-soft underline-offset-4 hover:underline">
          Se déconnecter
        </button>
      </div>

      {productsQuery.isPending ? (
        <p className="rounded-2xl bg-white p-6 text-sm text-noir-soft">Chargement des produits…</p>
      ) : productsQuery.isError ? (
        <p className="rounded-2xl bg-red-50 p-6 text-sm text-red-700" role="alert">{messageFor(productsQuery.error)}</p>
      ) : products.length === 0 ? (
        <p className="rounded-2xl bg-white p-6 text-sm text-noir-soft">Aucun produit à illustrer.</p>
      ) : (
        <>
          <div className="rounded-2xl border border-noir/5 bg-white p-5 sm:p-7">
            <label className="block max-w-xl text-sm">
              <span className="mb-1.5 block text-noir-soft">Produit</span>
              <select
                value={activeProductId}
                onChange={(event) => setSelectedProductId(event.target.value)}
                className="w-full rounded-xl border border-noir/10 bg-white px-4 py-3 outline-none focus:border-rose"
              >
                {products.map((product) => (
                  <option key={product.id} value={product.id}>
                    {product.name}{product.active ? "" : " (inactif)"}
                  </option>
                ))}
              </select>
            </label>

            <form onSubmit={submitUpload} className="mt-6 grid gap-4 md:grid-cols-2">
              <label className="block text-sm">
                <span className="mb-1.5 block text-noir-soft">Fichier image</span>
                <input
                  type="file"
                  accept="image/jpeg,image/png,image/webp"
                  required
                  onChange={(event) => setSelectedFile(event.target.files?.[0] ?? null)}
                  className="block w-full rounded-xl border border-noir/10 bg-white px-3 py-2.5 text-sm file:mr-3 file:rounded-full file:border-0 file:bg-blush-soft file:px-3 file:py-1.5"
                />
                <span className="mt-1.5 block text-xs text-noir-soft">JPEG, PNG ou WebP · 5 Mo maximum</span>
              </label>
              <label className="block text-sm">
                <span className="mb-1.5 block text-noir-soft">Texte alternatif</span>
                <input
                  type="text"
                  maxLength={200}
                  value={altText}
                  onChange={(event) => setAltText(event.target.value)}
                  placeholder="Ex. : pot de beurre de karité"
                  className="w-full rounded-xl border border-noir/10 bg-white px-4 py-2.5 outline-none focus:border-rose"
                />
              </label>
              <label className="flex items-center gap-2 text-sm text-noir-soft md:col-span-2">
                <input type="checkbox" name="primary" className="size-4 accent-noir" />
                Définir comme photo principale
              </label>
              {formError || uploadMutation.isError ? (
                <p className="rounded-xl bg-red-50 px-4 py-3 text-sm text-red-700 md:col-span-2" role="alert">
                  {formError ?? messageFor(uploadMutation.error)}
                </p>
              ) : null}
              {uploadMutation.isSuccess ? (
                <p className="rounded-xl bg-emerald-50 px-4 py-3 text-sm text-emerald-800 md:col-span-2" role="status">
                  Image ajoutée au produit.
                </p>
              ) : null}
              <button
                type="submit"
                disabled={uploadMutation.isPending || !activeProductId}
                className="rounded-full bg-noir px-6 py-3 text-sm text-blanc font-semibold transition-colors hover:bg-rose disabled:cursor-not-allowed disabled:opacity-60 md:col-span-2 md:justify-self-start"
              >
                {uploadMutation.isPending ? "Envoi de l’image…" : "Ajouter l’image"}
              </button>
            </form>
          </div>

          <div>
            <h2 className="font-display text-xl font-bold">Images de ce produit</h2>
            {imagesQuery.isPending ? (
              <p className="mt-3 rounded-2xl bg-white p-5 text-sm text-noir-soft">Chargement des images…</p>
            ) : imagesQuery.isError ? (
              <p className="mt-3 rounded-2xl bg-red-50 p-5 text-sm text-red-700" role="alert">{messageFor(imagesQuery.error)}</p>
            ) : imagesQuery.data?.length ? (
              <ul className="mt-4 grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4">
                {imagesQuery.data.map((image) => (
                  <ImageTile
                    key={image.id}
                    image={image}
                    deleting={deleteMutation.isPending}
                    onDelete={() => {
                      if (window.confirm("Supprimer cette image du produit ?")) deleteMutation.mutate(image.id);
                    }}
                  />
                ))}
              </ul>
            ) : (
              <p className="mt-3 rounded-2xl bg-white p-5 text-sm text-noir-soft">Aucune image pour ce produit.</p>
            )}
            {deleteMutation.isError && !isUnauthorized(deleteMutation.error) ? (
              <p className="mt-3 text-sm text-red-700" role="alert">{messageFor(deleteMutation.error)}</p>
            ) : null}
          </div>
        </>
      )}
    </section>
  );
}

function ImageTile({
  image,
  deleting,
  onDelete,
}: {
  image: AdminProductImage;
  deleting: boolean;
  onDelete: () => void;
}) {
  return (
    <li className="overflow-hidden rounded-2xl border border-noir/5 bg-white">
      <div className="relative aspect-square bg-blush-soft">
        {image.imageUrl ? (
          <Image src={image.imageUrl} alt={image.altText ?? "Image produit"} fill sizes="(max-width: 640px) 50vw, 25vw" unoptimized className="object-cover" />
        ) : <span className="grid h-full place-items-center text-xs text-noir-soft">Image indisponible</span>}
        {image.primary ? <span className="absolute left-2 top-2 rounded-full bg-white/90 px-2.5 py-1 text-[0.65rem] font-medium">Principale</span> : null}
      </div>
      <div className="flex items-center justify-between gap-2 p-3">
        <p className="truncate text-xs text-noir-soft">{image.altText || "Sans texte alternatif"}</p>
        <button type="button" onClick={onDelete} disabled={deleting} aria-label="Supprimer l’image" className="shrink-0 text-xs text-red-700 underline-offset-4 hover:underline disabled:opacity-50">
          Supprimer
        </button>
      </div>
    </li>
  );
}
