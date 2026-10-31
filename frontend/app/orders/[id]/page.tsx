"use client";
import { useEffect, useState } from "react";
import Link from "next/link";
import { ApiError, api, Order } from "../../../lib/api";
import { CustomerOnly } from "../../../components/CustomerOnly";

export default function OrderDetail({ params }: { params: { id: string } }) {
  return (
    <CustomerOnly>
      <OrderDetailContent params={params} />
    </CustomerOnly>
  );
}

function OrderDetailContent({ params }: { params: { id: string } }) {
  const [order, setOrder] = useState<Order>();
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let active = true;
    api
      .order(Number(params.id))
      .then((result) => {
        if (active) setOrder(result);
      })
      .catch((cause: unknown) => {
        if (active) {
          setError(
            cause instanceof ApiError && cause.status === 404
              ? "We couldn't find that order."
              : cause instanceof Error
                ? cause.message
                : "Unable to load this order.",
          );
        }
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [params.id]);

  if (loading) return <main className="muted">Loading order...</main>;
  if (!order) {
    return (
      <main>
        <p className="error" role="alert">
          {error}
        </p>
        <Link className="button button-quiet" href="/orders">
          Back to orders
        </Link>
      </main>
    );
  }

  return (
    <main>
      <Link className="back-link" href="/orders">
        ← All orders
      </Link>
      <section className="order-detail panel">
        <div className="order-card-heading">
          <div>
            <p className="eyebrow">ORDER #{order.id}</p>
            <h1>Order details</h1>
          </div>
          <span className={`status-pill status-${order.status.toLowerCase()}`}>
            {order.status}
          </span>
        </div>
        <div className="order-facts">
          <div>
            <span>Placed</span>
            <strong>{new Date(order.createdAt).toLocaleString()}</strong>
          </div>
          <div>
            <span>Payment</span>
            <strong>{order.paymentMethod}</strong>
          </div>
        </div>
        <h2 className="order-items-title">Items</h2>
        <div className="order-items">
          {order.items.map((item) => (
            <div className="order-item-row" key={item.productId}>
              <div>
                <strong>{item.productName}</strong>
                <span>
                  {item.quantity} × {item.unitPrice.toLocaleString()} VND
                </span>
              </div>
              <strong>{item.lineTotal.toLocaleString()} VND</strong>
            </div>
          ))}
        </div>
        <div className="row order-total">
          <span>Total</span>
          <strong>{order.totalAmount.toLocaleString()} VND</strong>
        </div>
        {order.status === "PENDING" && order.paymentMethod === "VNPAY" && (
          <p className="muted">
            Payment status is confirmed by VNPay. Please allow a short time for
            the order to update.
          </p>
        )}
      </section>
    </main>
  );
}
