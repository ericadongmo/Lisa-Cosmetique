export type Category = {
  id: string;
  slug: string;
  name: string;
  description: string | null;
};

export type ProductSummary = {
  id: string;
  slug: string;
  name: string;
  shortDescription: string | null;
  priceXaf: number;
  /** Prix avant remise (prix barré), null hors promotion. */
  compareAtPriceXaf?: number | null;
  inStock: boolean;
  categorySlug: string;
  categoryName: string;
  featured: boolean;
  imageUrl: string | null;
};

export type ProductDetail = ProductSummary & {
  description: string | null;
  stock: number;
  images: ProductImage[];
};

export type ProductImage = {
  id: string;
  imageUrl: string | null;
  altText: string | null;
  position: number;
  primary: boolean;
};

export type PageResponse<T> = {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};
