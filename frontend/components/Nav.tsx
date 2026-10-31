"use client";
import Link from "next/link";
import { useState } from "react";
import { useAuth } from "../context/AuthContext";
export function Nav() {
  const { user, logout, isLoading, error } = useAuth();
  const [logoutError, setLogoutError] = useState("");

  return (
    <>
      <header className="site-header">
        <nav className="nav" aria-label="Main navigation">
          <Link className="brand" href="/">
            <span className="brand-mark">S</span>
            <span>Studio Store</span>
          </Link>
          <div className="nav-links">
            <Link href="/">Shop</Link>
            {!isLoading && user?.role === "CUSTOMER" && (
              <>
                <Link href="/cart">Cart</Link>
                <Link href="/orders">Orders</Link>
              </>
            )}
            {!isLoading && user?.role === "ADMIN" && (
              <Link href="/admin">Admin</Link>
            )}
          </div>
          <div className="nav-account">
            {isLoading ? (
              <span className="muted">Restoring session...</span>
            ) : user ? (
              <>
                <span className="account-name">{user.fullName}</span>
                <button
                  className="button button-quiet"
                  onClick={() => {
                    setLogoutError("");
                    logout().catch((cause: unknown) => {
                      setLogoutError(
                        cause instanceof Error
                          ? cause.message
                          : "Unable to sign out.",
                      );
                    });
                  }}
                >
                  Sign out
                </button>
              </>
            ) : (
              <>
                <Link href="/login">Sign in</Link>
                <Link
                  className="button button-primary button-small"
                  href="/register"
                >
                  Create account
                </Link>
              </>
            )}
          </div>
        </nav>
      </header>
      {(error || logoutError) && (
        <p className="site-notice" role="status">
          {error || logoutError}
        </p>
      )}
    </>
  );
}
