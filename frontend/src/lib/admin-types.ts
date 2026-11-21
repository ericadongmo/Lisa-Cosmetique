export type AdminOrderStatus =
  | "PENDING"
  | "CONFIRMED"
  | "PROCESSING"
  | "SHIPPED"
  | "DELIVERED"
  | "CANCELLED";

export type AdminPaymentStatus =
  | "PENDING"
  | "PROCESSING"
  | "SUCCESS"
  | "FAILED"
  | "CANCELLED"
  | "REFUNDED";

export type AdminOrderSummary = {
  id: string;
  orderNumber: string;
  orderStatus: AdminOrderStatus;
  paymentStatus: AdminPaymentStatus;
  customerName: string;
  customerPhone: string;
  deliveryCity: string;
  totalXaf: number;
  createdAt: string;
};

export type AdminProductSummary = {
  id: string;
  name: string;
  slug: string;
  sku: string;
  active: boolean;
};

export type AdminProductImage = {
  id: string;
  imageUrl: string | null;
  altText: string | null;
  position: number;
  primary: boolean;
};

export type AdminOrderItem = {
  productId: string | null;
  productName: string;
  sku: string;
  unitPriceXaf: number;
  quantity: number;
  subtotalXaf: number;
};

export type AdminShipment = {
  id: string;
  trackingNumber: string | null;
  status: "PENDING" | "SHIPPED" | "DELIVERED" | "CANCELLED";
  shippedAt: string | null;
  deliveredAt: string | null;
};

export type AdminOrderDetail = {
  id: string;
  orderNumber: string;
  orderStatus: AdminOrderStatus;
  paymentStatus: AdminPaymentStatus;
  customerName: string;
  customerPhone: string;
  customerEmail: string | null;
  deliveryAddress: Record<string, string | null> | null;
  deliveryZoneName: string;
  deliveryCity: string;
  subtotalXaf: number;
  deliveryFeeXaf: number;
  discountXaf: number;
  totalXaf: number;
  notes: string | null;
  items: AdminOrderItem[];
  shipment: AdminShipment | null;
  createdAt: string;
  updatedAt: string;
};

export type AdminPage<T> = {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export type UpdateOrderStatusPayload = {
  status: AdminOrderStatus;
  trackingNumber?: string;
};
