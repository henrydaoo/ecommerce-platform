export const API_URL =
  process.env.NEXT_PUBLIC_API_URL ?? "/api/v1";

function apiBaseUrl() {
  const apiUrl = new URL(API_URL, window.location.origin);
  return apiUrl.toString().replace(/\/$/, "");
}

export type User = {
  id: number;
  email: string;
  fullName: string;
  role: "CUSTOMER" | "ADMIN";
};

export type AuthSession = {
  accessToken: string;
  user: User;
};

export type Product = {
  id: number;
  name: string;
  description?: string;
  price: number;
  imageUrl?: string;
  stockQuantity: number;
  category: Category;
  active: boolean;
  version: number;
};

export type NewProduct = {
  name: string;
  description?: string;
  price: number;
  stockQuantity: number;
  categoryId: number;
  imageUrl?: string;
};
export type UpdateProduct = NewProduct & { version: number };

export type Cart = {
  items: {
    id: number;
    productId: number;
    productName: string;
    unitPrice: number;
    quantity: number;
    lineTotal: number;
  }[];
  totalAmount: number;
};

export type Order = {
  id: number;
  status: string;
  paymentMethod: string;
  totalAmount: number;
  createdAt: string;
  paymentUrl?: string;
  items: OrderItem[];
};
export type OrderItem = {
  productId: number;
  productName: string;
  quantity: number;
  unitPrice: number;
  lineTotal: number;
};

export type Category = { id: number; name: string; slug: string };
export type PageResponse<T> = {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};
export type EmailDeadLetter = {
  eventId: string;
  eventType: string;
  firstFailedAt: string;
  lastFailedAt: string;
  replayCount: number;
  replayState: "OPEN" | "PUBLISHING" | "REPLAY_QUEUED" | "RESOLVED";
};

export class ApiError extends Error {
  constructor(
    message: string,
    public readonly status: number,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

let accessToken: string | undefined;
let refreshRequest: Promise<AuthSession | undefined> | undefined;

export const setAccessToken = (token?: string) => {
  accessToken = token;
};

async function send<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers);
  if (init.body && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }
  if (accessToken && !path.startsWith("/auth/")) {
    headers.set("Authorization", `Bearer ${accessToken}`);
  }

  const response = await fetch(`${apiBaseUrl()}${path}`, {
    ...init,
    credentials: "include",
    headers,
  });

  if (!response.ok) {
    const body = await response.json().catch(() => null);
    throw new ApiError(
      body?.message ?? body?.detail ?? "Request failed",
      response.status,
    );
  }

  return response.status === 204 ? (undefined as T) : response.json();
}

function refreshSession(): Promise<AuthSession | undefined> {
  if (!refreshRequest) {
    refreshRequest = send<AuthSession | undefined>("/auth/refresh", {
      method: "POST",
      headers: { "X-Refresh-Intent": "1" },
    })
      .then((session) => {
        if (session) setAccessToken(session.accessToken);
        return session;
      })
      .finally(() => {
        refreshRequest = undefined;
      });
  }
  return refreshRequest;
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  try {
    return await send<T>(path, init);
  } catch (error) {
    if (
      !(error instanceof ApiError) ||
      error.status !== 401 ||
      path.startsWith("/auth/")
    ) {
      throw error;
    }

    try {
      await refreshSession();
    } catch (refreshError) {
      if (refreshError instanceof ApiError && refreshError.status === 401) {
        setAccessToken();
        throw error;
      }
      throw refreshError;
    }

    return send<T>(path, init);
  }
}

export const api = {
  request,
  auth: {
    login: (email: string, password: string) =>
      request<AuthSession>("/auth/login", {
        method: "POST",
        body: JSON.stringify({ email, password }),
      }),
    register: (email: string, password: string, fullName: string) =>
      request<AuthSession>("/auth/register", {
        method: "POST",
        body: JSON.stringify({ email, password, fullName }),
      }),
    refresh: refreshSession,
    logout: () =>
      request<void>("/auth/logout", {
        method: "POST",
        headers: { "X-Refresh-Intent": "1" },
      }),
  },
  products: (search = "", categoryId?: number) => {
    const query = new URLSearchParams();
    if (search) query.set("search", search);
    if (categoryId !== undefined) query.set("category", String(categoryId));
    const suffix = query.size > 0 ? `?${query.toString()}` : "";
    return request<PageResponse<Product>>(`/products${suffix}`);
  },
  categories: () => request<Category[]>("/categories"),
  cart: () => request<Cart>("/cart"),
  add: (productId: number, quantity = 1) =>
    request<Cart>("/cart/items", {
      method: "POST",
      body: JSON.stringify({ productId, quantity }),
    }),
  updateCartItem: (itemId: number, quantity: number) =>
    request<Cart>(`/cart/items/${itemId}`, {
      method: "PUT",
      body: JSON.stringify({ quantity }),
    }),
  removeCartItem: (itemId: number) =>
    request<Cart>(`/cart/items/${itemId}`, { method: "DELETE" }),
  checkout: (paymentMethod: "COD" | "VNPAY") =>
    request<Order>("/orders", {
      method: "POST",
      headers: { "Idempotency-Key": crypto.randomUUID() },
      body: JSON.stringify({ paymentMethod }),
    }),
  orders: () => request<{ content: Order[] }>("/orders"),
  order: (id: number) => request<Order>(`/orders/${id}`),
};

export const admin = {
  categories: () => request<Category[]>("/categories"),
  products: (page = 0, size = 20) =>
    request<PageResponse<Product>>(`/products/admin?page=${page}&size=${size}`),
  createCategory: (name: string, slug: string) =>
    request<Category>("/categories", {
      method: "POST",
      body: JSON.stringify({ name, slug }),
    }),
  deleteCategory: (id: number) =>
    request<void>(`/categories/${id}`, { method: "DELETE" }),
  createProduct: (product: NewProduct) =>
    request<Product>("/products", {
      method: "POST",
      body: JSON.stringify(product),
    }),
  updateProduct: (id: number, product: UpdateProduct) =>
    request<Product>(`/products/${id}`, {
      method: "PUT",
      body: JSON.stringify(product),
    }),
  deleteProduct: (id: number) =>
    request<void>(`/products/${id}`, { method: "DELETE" }),
  activateProduct: (id: number) =>
    request<void>(`/products/${id}/activate`, { method: "PUT" }),
  orders: () => request<{ content: Order[] }>("/orders/admin/all"),
  status: (id: number, status: string) =>
    request<Order>(`/orders/${id}/status`, {
      method: "PUT",
      body: JSON.stringify({ status }),
    }),
  emailDeadLetters: () =>
    request<EmailDeadLetter[]>("/admin/email-dead-letters"),
  replayEmailDeadLetter: (eventId: string) =>
    request<void>(`/admin/email-dead-letters/${eventId}/replay`, {
      method: "POST",
    }),
  users: (page = 0, size = 20) =>
    request<PageResponse<User>>(`/admin/users?page=${page}&size=${size}`),
};
