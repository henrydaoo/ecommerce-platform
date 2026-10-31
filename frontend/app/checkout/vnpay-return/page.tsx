"use client";
import { Suspense, useEffect, useState } from "react";
import { useSearchParams } from "next/navigation";
import Link from "next/link";
import { api } from "../../../lib/api";
export default function VnpayReturn() {
  return (
    <Suspense fallback={<main className="panel">Checking payment result...</main>}>
      <VnpayReturnResult />
    </Suspense>
  );
}

function VnpayReturnResult() {
  const searchParams = useSearchParams();
  const query = searchParams.toString();
  const [result, setResult] = useState<{
    message: string;
    paymentCancelled: boolean;
  }>();
  const [error, setError] = useState("");
  useEffect(() => {
    let active = true;
    api
      .request<{ message: string; paymentCancelled: boolean }>(
        `/payment/vnpay/return?${query}`,
      )
      .then((response) => active && setResult(response))
      .catch((cause: unknown) => {
        if (active) {
          setError(
            cause instanceof Error
              ? cause.message
              : "Unable to verify the payment result.",
          );
        }
      });
    return () => {
      active = false;
    };
  }, [query]);
  return (
    <main className="panel">
      <h1>VNPay result</h1>
      <p role={error ? "alert" : "status"}>
        {error || result?.message || "Verifying payment..."}
      </p>
      {result?.paymentCancelled && (
        <Link className="button button-primary" href="/orders">
          View cancelled order
        </Link>
      )}
    </main>
  );
}
