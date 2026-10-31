"use client";

import Link from "next/link";
import type { ReactNode } from "react";
import { useAuth } from "../context/AuthContext";

export function CustomerOnly({ children }: { children: ReactNode }) {
  const { user, isLoading } = useAuth();

  if (isLoading) {
    return (
      <main className="muted" role="status">
        Restoring your session...
      </main>
    );
  }

  if (!user) {
    return (
      <main className="panel">
        <h1>Sign in required</h1>
        <p className="muted">Sign in with a customer account to continue.</p>
        <Link className="button button-primary" href="/login">
          Sign in
        </Link>
      </main>
    );
  }

  if (user.role !== "CUSTOMER") {
    return (
      <main className="panel">
        <h1>Customer account required</h1>
        <p className="muted">Cart and order history are customer-only pages.</p>
        <Link className="button button-quiet" href="/admin">
          Return to administration
        </Link>
      </main>
    );
  }

  return children;
}
