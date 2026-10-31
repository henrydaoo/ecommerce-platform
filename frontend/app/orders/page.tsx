"use client";
import { useEffect, useState } from "react";
import Link from "next/link";
import { ApiError, api, Order } from "../../lib/api";
import { CustomerOnly } from "../../components/CustomerOnly";

export default function Orders() {
  return (
    <CustomerOnly>
      <OrdersContent />
    </CustomerOnly>
  );
}

function OrdersContent() {
  const [orders, setOrders] = useState<Order[]>([]);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let active = true;
    api
      .orders()
      .then((response) => {
        if (active) setOrders(response.content);
      })
      .catch((cause: unknown) => {
        if (!active) return;
        setError(
          cause instanceof ApiError && cause.status === 401
            ? "Sign in to see your order history."
            : cause instanceof Error
              ? cause.message
              : "Unable to load your orders.",
        );
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, []);

  return (
    <main>
      <p className="eyebrow">YOUR ACCOUNT</p>
      <h1>Orders</h1>
      <p className="muted">A record of the things you have chosen.</p>
      {loading ? (
        <p className="muted" role="status">
          Loading your orders...
        </p>
      ) : error ? (
        <section className="empty-state">
          <p className="error" role="alert">
            {error}
          </p>
          {error.startsWith("Sign in") && (
            <Link className="button button-primary" href="/login">
              Sign in
            </Link>
          )}
        </section>
      ) : orders.length === 0 ? (
        <section className="empty-state">
          <p>Your order history is empty for now.</p>
          <Link className="button button-primary" href="/">
            Explore the collection
          </Link>
        </section>
      ) : (
        <section className="order-list" aria-label="Order history">
          {orders.map((order) => (
            <article className="order-card panel" key={order.id}>
              <div className="order-card-heading">
                <div>
                  <p className="eyebrow">ORDER #{order.id}</p>
                  <h2>{new Date(order.createdAt).toLocaleDateString()}</h2>
                </div>
                <span
                  className={`status-pill status-${order.status.toLowerCase()}`}
                >
                  {order.status}
                </span>
              </div>
              <p className="muted">
                {order.items.length}{" "}
                {order.items.length === 1 ? "item" : "items"} ·{" "}
                {order.paymentMethod}
              </p>
              <div className="row order-card-footer">
                <strong>{order.totalAmount.toLocaleString()} VND</strong>
                <Link
                  className="button button-quiet"
                  href={`/orders/${order.id}`}
                >
                  View order
                </Link>
              </div>
            </article>
          ))}
        </section>
      )}
    </main>
  );
}
