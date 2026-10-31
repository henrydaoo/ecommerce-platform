"use client";
import { useCallback, useEffect, useState } from "react";
import { admin, Category } from "../../../lib/api";

export default function AdminCategories() {
  const [categories, setCategories] = useState<Category[]>([]);
  const [name, setName] = useState("");
  const [slug, setSlug] = useState("");
  const [message, setMessage] = useState("");
  const [busy, setBusy] = useState(false);

  const load = useCallback(
    async () => setCategories(await admin.categories()),
    [],
  );
  useEffect(() => {
    load().catch((cause: unknown) =>
      setMessage(
        cause instanceof Error ? cause.message : "Unable to load categories.",
      ),
    );
  }, [load]);

  const create = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setBusy(true);
    setMessage("");
    try {
      await admin.createCategory(name.trim(), slug.trim());
      setName("");
      setSlug("");
      await load();
    } catch (cause) {
      setMessage(
        cause instanceof Error ? cause.message : "Unable to create category.",
      );
    } finally {
      setBusy(false);
    }
  };

  return (
    <main className="admin-page">
      <p className="eyebrow">CATALOG</p>
      <h1>Categories</h1>
      <p className="muted">Keep products organized into clear collections.</p>
      {message && (
        <p className="error" role="alert">
          {message}
        </p>
      )}
      <section className="admin-two-column">
        <form className="panel admin-form" onSubmit={create}>
          <h2>Add a category</h2>
          <label>
            Name
            <input
              required
              value={name}
              onChange={(event) => setName(event.target.value)}
            />
          </label>
          <label>
            Slug
            <input
              required
              value={slug}
              onChange={(event) => setSlug(event.target.value)}
            />
          </label>
          <button disabled={busy}>
            {busy ? "Saving..." : "Create category"}
          </button>
        </form>
        <section className="panel admin-list">
          <h2>
            All categories{" "}
            <span className="count-pill">{categories.length}</span>
          </h2>
          {categories.length === 0 ? (
            <p className="muted">No categories yet.</p>
          ) : (
            categories.map((category) => (
              <div className="admin-list-row" key={category.id}>
                <div>
                  <strong>{category.name}</strong>
                  <small>Slug: {category.slug}</small>
                </div>
                <button
                  className="button button-quiet"
                  disabled={busy}
                  onClick={async () => {
                    setBusy(true);
                    setMessage("");
                    try {
                      await admin.deleteCategory(category.id);
                      await load();
                    } catch (cause) {
                      setMessage(
                        cause instanceof Error
                          ? cause.message
                          : "Unable to delete category.",
                      );
                    } finally {
                      setBusy(false);
                    }
                  }}
                >
                  Delete
                </button>
              </div>
            ))
          )}
        </section>
      </section>
    </main>
  );
}
