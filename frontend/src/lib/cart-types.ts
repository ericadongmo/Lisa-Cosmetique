export type CartItem = {
  productId: string;
  productName: string;
  productSlug: string;
  unitPriceXaf: number;
  quantity: number;
  lineTotalXaf: number;
  availableStock: number;
};

export type Cart = {
  id: string;
  items: CartItem[];
  subtotalXaf: number;
  totalQuantity: number;
};

export type DeliveryZone = {
  id: string;
  name: string;
  city: string;
  deliveryFeeXaf: number;
  estimatedDeliveryDays: number;
};

export type CreateOrderPayload = {
  items: { productId: string; quantity: number }[];
  deliveryZoneId: string;
  customerName: string;
  customerPhone: string;
  customerEmail: string | null;
  deliveryAddress: {
    recipientName: string;
    phoneNumber: string;
    city: string;
    neighborhood: string;
    streetAddress: string;
    additionalInformation: string | null;
  };
  notes: string | null;
};

export type OrderItem = {
  productId: string | null;
  productName: string;
  productSku: string;
  unitPriceXaf: number;
  quantity: number;
  subtotalXaf: number;
};

export type Order = {
  id: string;
  orderNumber: string;
  orderStatus: string;
  paymentStatus: string;
  subtotalXaf: number;
  deliveryFeeXaf: number;
  discountXaf: number;
  totalXaf: number;
  items: OrderItem[];
  createdAt: string;
};

export type OrderTrackingPayload = {
  orderNumber: string;
  phone: string;
};

/** Vue publique : la réponse de suivi ne contient ni adresse ni téléphone. */
export type OrderTracking = {
  orderNumber: string;
  orderStatus: string;
  paymentStatus: string;
  deliveryZoneName: string;
  deliveryCity: string;
  totalXaf: number;
  items: OrderItem[];
  createdAt: string;
};
