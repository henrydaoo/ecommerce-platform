"use client";
import Link from "next/link";
import { useAuth } from "../../context/AuthContext";

export function AdminShell({ children }: { children: React.ReactNode }) {
  const { user, isLoading } = useAuth();

  if (isLoading)
    return <main className="muted">Checking administrator access...</main>;
  if (!user) {
    return (
      <main className="panel">
        <h1>Sign in required</h1>
        <p className="muted">
          Sign in with an administrator account to continue.
        </p>
        <Link className="button button-primary" href="/login">
          Sign in
        </Link>
      </main>
    );
  }
  if (user.role !== "ADMIN") {
    return (
      <main className="panel">
        <h1>Access restricted</h1>
        <p className="muted">This area is available to administrators only.</p>
        <Link className="button button-quiet" href="/">
          Return to shop
        </Link>
      </main>
    );
  }

  return (
    <div className="admin-shell">
      <aside className="admin-sidebar">
        <p className="eyebrow">STORE MANAGEMENT</p>
        <h2>Admin</h2>
        <nav aria-label="Administration">
          <Link href="/admin">Overview</Link>
          <Link href="/admin/categories">Categories</Link>
          <Link href="/admin/products">Products</Link>
          <Link href="/admin/orders">Orders</Link>
          <Link href="/admin/users">Users</Link>
          <Link href="/admin/messages">Email queue</Link>
        </nav>
      </aside>
      <div className="admin-content">{children}</div>
    </div>
  );
}
