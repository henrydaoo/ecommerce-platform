"use client";

import { useCallback, useEffect, useState } from "react";
import { admin, PageResponse, User } from "../../../lib/api";

export default function AdminUsers() {
  const [userPage, setUserPage] = useState<PageResponse<User>>();
  const [page, setPage] = useState(0);
  const [message, setMessage] = useState("");
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setUserPage(await admin.users(page));
    } finally {
      setLoading(false);
    }
  }, [page]);

  useEffect(() => {
    load().catch((cause: unknown) =>
      setMessage(
        cause instanceof Error ? cause.message : "Unable to load users.",
      ),
    );
  }, [load]);

  return (
    <main className="admin-page">
      <div className="section-heading">
        <div>
          <p className="eyebrow">CUSTOMER MANAGEMENT</p>
          <h1>Users</h1>
        </div>
        <button
          className="button button-quiet"
          onClick={() =>
            load().catch((cause: unknown) =>
              setMessage(
                cause instanceof Error
                  ? cause.message
                  : "Unable to refresh users.",
              ),
            )
          }
        >
          Refresh
        </button>
      </div>
      <p className="muted">View registered customers and administrator accounts.</p>
      {message && (
        <p className="error" role="alert">
          {message}
        </p>
      )}
      <section className="panel admin-list">
        <h2>
          All users{" "}
          <span className="count-pill">{userPage?.totalElements ?? 0}</span>
        </h2>
        {loading ? (
          <p className="muted" role="status">
            Loading users...
          </p>
        ) : !userPage || userPage.content.length === 0 ? (
          <p className="muted">No users yet.</p>
        ) : (
          userPage.content.map((user) => (
            <div className="admin-list-row" key={user.id}>
              <div>
                <strong>{user.fullName}</strong>
                <small>{user.email}</small>
              </div>
              <span className="status-pill">{user.role}</span>
            </div>
          ))
        )}
      </section>
      {userPage && userPage.totalPages > 1 && (
        <div className="row admin-pagination">
          <button
            className="button button-quiet"
            disabled={page === 0 || loading}
            onClick={() => setPage((current) => current - 1)}
          >
            Previous
          </button>
          <span className="muted">
            Page {page + 1} of {userPage.totalPages}
          </span>
          <button
            className="button button-quiet"
            disabled={page + 1 >= userPage.totalPages || loading}
            onClick={() => setPage((current) => current + 1)}
          >
            Next
          </button>
        </div>
      )}
    </main>
  );
}
