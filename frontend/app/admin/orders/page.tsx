"use client";
import { useCallback, useEffect, useState } from "react";
import { admin, Order } from "../../../lib/api";

export default function AdminOrders() {
  const [orders, setOrders] = useState<Order[]>([]);
  const [message, setMessage] = useState("");
  const [busy, setBusy] = useState<number>();

  const load = useCallback(async () => {
    const result = await admin.orders();
    setOrders(result.content);
  }, []);
  useEffect(() => {
    load().catch((cause: unknown) =>
      setMessage(
        cause instanceof Error ? cause.message : "Unable to load orders.",
      ),
    );
  }, [load]);

  const nextStatuses = (order: Order): string[] =>
    order.status === "PENDING"
      ? order.paymentMethod === "COD"
        ? ["PAID", "CANCELLED"]
        : []
      : order.status === "PAID"
        ? ["SHIPPED"]
        : order.status === "SHIPPED"
          ? ["DELIVERED"]
          : [];

  const statusHelp = (order: Order) => {
    if (order.status === "PENDING" && order.paymentMethod === "VNPAY") {
      return "Waiting for VNPay. Only a verified gateway callback can mark this order paid or cancelled.";
    }
    if (order.status === "PENDING" && order.paymentMethod === "COD") {
      return "Mark COD as paid when collected, or cancel to restore reserved stock.";
    }
    if (order.status === "PAID") {
      return "Ready to move into shipping.";
    }
    if (order.status === "SHIPPED") {
      return "Mark delivered when the customer receives the order.";
    }
    return "This order is in a final state.";
  };

  return (
    <main className="admin-page">
      <div className="section-heading">
        <div>
          <p className="eyebrow">FULFILLMENT</p>
          <h1>Orders</h1>
        </div>
        <button
          className="button button-quiet"
          onClick={() =>
            load().catch((cause: unknown) =>
              setMessage(
                cause instanceof Error
                  ? cause.message
                  : "Unable to refresh orders.",
              ),
            )
          }
        >
          Refresh
        </button>
      </div>
      <p className="muted">
        Review order totals and move orders through fulfillment.
      </p>
      {message && (
        <p className="error" role="alert">
          {message}
        </p>
      )}
      {orders.length === 0 ? (
        <p className="empty-state">No orders to show.</p>
      ) : (
        <section className="admin-order-list">
          {orders.map((order) => (
            <article className="panel admin-order-card" key={order.id}>
              <div>
                <p className="eyebrow">ORDER #{order.id}</p>
                <h2>{order.totalAmount.toLocaleString()} VND</h2>
                <p className="muted">
                  {order.items.length} items · {order.paymentMethod} ·{" "}
                  {new Date(order.createdAt).toLocaleString()}
                </p>
              </div>
              <div className="admin-order-actions">
                <span
                  className={`status-pill status-${order.status.toLowerCase()}`}
                >
                  {order.status}
                </span>
                <div className="admin-status-control">
                  <select
                    aria-label={`Update order ${order.id} status`}
                    value={order.status}
                    disabled={
                      busy === order.id || nextStatuses(order).length === 0
                    }
                    onChange={async (event) => {
                      const status = event.target.value;
                      setBusy(order.id);
                      setMessage("");
                      try {
                        await admin.status(order.id, status);
                        await load();
                      } catch (cause) {
                        setMessage(
                          cause instanceof Error
                            ? cause.message
                            : "Unable to update order status.",
                        );
                      } finally {
                        setBusy(undefined);
                      }
                    }}
                  >
                    <option value={order.status}>{order.status}</option>
                    {nextStatuses(order).map((status) => (
                      <option value={status} key={status}>
                        Move to {status}
                      </option>
                    ))}
                  </select>
                  <small className="muted">{statusHelp(order)}</small>
                </div>
              </div>
            </article>
          ))}
        </section>
      )}
    </main>
  );
}
