"use client";
import { useCallback, useEffect, useState } from "react";
import {
  admin,
  Category,
  NewProduct,
  PageResponse,
  Product,
  UpdateProduct,
} from "../../../lib/api";

export default function AdminProducts() {
  const [categories, setCategories] = useState<Category[]>([]);
  const [productPage, setProductPage] = useState<PageResponse<Product>>();
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [price, setPrice] = useState("");
  const [stock, setStock] = useState("");
  const [imageUrl, setImageUrl] = useState("");
  const [categoryId, setCategoryId] = useState("");
  const [editingProduct, setEditingProduct] = useState<Product>();
  const [message, setMessage] = useState("");
  const [busy, setBusy] = useState(false);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [categoryResult, productResult] = await Promise.all([
        admin.categories(),
        admin.products(page),
      ]);
      setCategories(categoryResult);
      setProductPage(productResult);
    } finally {
      setLoading(false);
    }
  }, [page]);
  useEffect(() => {
    load().catch((cause: unknown) =>
      setMessage(
        cause instanceof Error ? cause.message : "Unable to load products.",
      ),
    );
  }, [load]);

  const resetForm = () => {
    setName("");
    setDescription("");
    setPrice("");
    setStock("");
    setImageUrl("");
    setCategoryId("");
    setEditingProduct(undefined);
  };

  const save = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setBusy(true);
    setMessage("");
    try {
      const product: NewProduct = {
        name: name.trim(),
        description: description.trim(),
        price: Number(price),
        stockQuantity: Number(stock),
        categoryId: Number(categoryId),
        imageUrl: imageUrl.trim() || undefined,
      };
      if (editingProduct) {
        const update: UpdateProduct = {
          ...product,
          version: editingProduct.version,
        };
        await admin.updateProduct(editingProduct.id, update);
      } else {
        await admin.createProduct(product);
      }
      resetForm();
      await load();
    } catch (cause) {
      setMessage(
        cause instanceof Error
          ? cause.message
          : `Unable to ${editingProduct ? "update" : "create"} product.`,
      );
    } finally {
      setBusy(false);
    }
  };

  return (
    <main className="admin-page">
      <p className="eyebrow">CATALOG</p>
      <h1>Products</h1>
      <p className="muted">
        Create and edit products, and manage whether they appear in the store.
      </p>
      {message && (
        <p className="error" role="alert">
          {message}
        </p>
      )}
      <section className="admin-two-column">
        <form className="panel admin-form" onSubmit={save}>
          <h2>{editingProduct ? "Edit product" : "Add a product"}</h2>
          <label>
            Name
            <input
              required
              value={name}
              onChange={(event) => setName(event.target.value)}
            />
          </label>
          <label>
            Description
            <textarea
              value={description}
              onChange={(event) => setDescription(event.target.value)}
            />
          </label>
          <label>
            Image URL
            <input
              type="url"
              value={imageUrl}
              onChange={(event) => setImageUrl(event.target.value)}
            />
          </label>
          <div className="admin-input-row">
            <label>
              Price (VND)
              <input
                required
                type="number"
                min="1"
                value={price}
                onChange={(event) => setPrice(event.target.value)}
              />
            </label>
            <label>
              Stock
              <input
                required
                type="number"
                min="0"
                value={stock}
                onChange={(event) => setStock(event.target.value)}
              />
            </label>
          </div>
          <label>
            Category
            <select
              required
              value={categoryId}
              onChange={(event) => setCategoryId(event.target.value)}
            >
              <option value="">Choose category</option>
              {categories.map((category) => (
                <option value={category.id} key={category.id}>
                  {category.name}
                </option>
              ))}
            </select>
          </label>
          <div className="row">
            <button disabled={busy || categories.length === 0}>
              {busy
                ? "Saving..."
                : editingProduct
                  ? "Save changes"
                  : "Create product"}
            </button>
            {editingProduct && (
              <button
                className="button button-quiet"
                type="button"
                disabled={busy}
                onClick={resetForm}
              >
                Cancel
              </button>
            )}
          </div>
        </form>
        <section className="panel admin-list">
          <h2>
            All products{" "}
            <span className="count-pill">
              {productPage?.totalElements ?? 0}
            </span>
          </h2>
          {loading ? (
            <p className="muted" role="status">
              Loading products...
            </p>
          ) : !productPage || productPage.content.length === 0 ? (
            <p className="muted">No products yet.</p>
          ) : (
            productPage.content.map((product) => (
              <div className="admin-list-row" key={product.id}>
                <div>
                  <strong>{product.name}</strong>
                  <small>
                    {product.stockQuantity} in stock ·{" "}
                    {product.price.toLocaleString()} VND
                  </small>
                  <small>
                    <span
                      className={`status-pill ${product.active ? "" : "status-cancelled"}`}
                    >
                      {product.active ? "Active" : "Inactive"}
                    </span>
                  </small>
                </div>
                <div className="row">
                  <button
                    className="button button-quiet"
                    disabled={busy}
                    onClick={() => {
                      setEditingProduct(product);
                      setName(product.name);
                      setDescription(product.description ?? "");
                      setPrice(String(product.price));
                      setStock(String(product.stockQuantity));
                      setImageUrl(product.imageUrl ?? "");
                      setCategoryId(String(product.category.id));
                      setMessage("");
                    }}
                  >
                    Edit
                  </button>
                  <button
                    className={`button ${product.active ? "button-quiet" : "button-primary"}`}
                    disabled={busy}
                    onClick={async () => {
                      setBusy(true);
                      setMessage("");
                      try {
                        if (product.active) {
                          await admin.deleteProduct(product.id);
                        } else {
                          await admin.activateProduct(product.id);
                        }
                        await load();
                      } catch (cause) {
                        setMessage(
                          cause instanceof Error
                            ? cause.message
                            : `Unable to ${product.active ? "deactivate" : "activate"} product.`,
                        );
                      } finally {
                        setBusy(false);
                      }
                    }}
                  >
                    {product.active ? "Deactivate" : "Activate"}
                  </button>
                </div>
              </div>
            ))
          )}
          {productPage && productPage.totalPages > 1 && (
            <div className="row admin-pagination">
              <button
                className="button button-quiet"
                disabled={page === 0 || loading}
                onClick={() => setPage((current) => current - 1)}
              >
                Previous
              </button>
              <span className="muted">
                Page {page + 1} of {productPage.totalPages}
              </span>
              <button
                className="button button-quiet"
                disabled={page + 1 >= productPage.totalPages || loading}
                onClick={() => setPage((current) => current + 1)}
              >
                Next
              </button>
            </div>
          )}
        </section>
      </section>
    </main>
  );
}
