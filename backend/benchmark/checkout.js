import http from "k6/http";
import { check } from "k6";
import { Counter, Rate, Trend } from "k6/metrics";

const tokens = JSON.parse(__ENV.K6_BEARER_TOKENS || "[]");
const mode = __ENV.MODE || "burst";
const vus = Number(__ENV.VUS || tokens.length);
const duration = __ENV.DURATION || "30s";
const allowInsufficientStock = __ENV.ALLOW_INSUFFICIENT_STOCK === "true";
const checkoutSuccesses = new Counter("checkout_successes");
const checkoutStockRejections = new Counter("checkout_stock_rejections");
const checkoutUnexpectedErrors = new Counter("checkout_unexpected_errors");
const checkoutDuration = new Trend("checkout_duration", true);
const checkoutAcceptedOutcomes = new Rate("checkout_accepted_outcomes");

export const options = {
  summaryTrendStats: [
    "min",
    "med",
    "avg",
    "p(50)",
    "p(90)",
    "p(95)",
    "p(99)",
    "max",
  ],
  scenarios: {
    checkout:
      mode === "burst"
        ? {
            executor: "per-vu-iterations",
            vus,
            iterations: 1,
            maxDuration: __ENV.MAX_DURATION || "2m",
          }
        : {
            executor: "constant-vus",
            vus,
            duration,
            gracefulStop: "10s",
          },
  },
  thresholds: {
    checkout_unexpected_errors: ["count==0"],
    checkout_accepted_outcomes: ["rate==1"],
  },
};

export function setup() {
  if (!["burst", "sustained"].includes(mode)) {
    throw new Error('MODE must be either "burst" or "sustained".');
  }
  if (
    !Array.isArray(tokens) ||
    tokens.length === 0 ||
    tokens.some((token) => typeof token !== "string" || token.length === 0)
  ) {
    throw new Error(
      "K6_BEARER_TOKENS must be a JSON array of access-token strings.",
    );
  }

  if (!Number.isInteger(vus) || vus < 1 || vus > tokens.length) {
    throw new Error("The token array must contain at least one token per VU.");
  }

  if (mode === "sustained" && !/^\d+$/.test(__ENV.PRODUCT_ID || "")) {
    throw new Error(
      "Sustained mode requires PRODUCT_ID so each successful checkout can refill its cart.",
    );
  }

  return {
    baseUrl: (__ENV.BASE_URL || "http://localhost:8080").replace(/\/$/, ""),
    tokens,
    productId: Number(__ENV.PRODUCT_ID),
    checkoutBody: __ENV.CHECKOUT_BODY || '{"paymentMethod":"COD"}',
  };
}

export default function (data) {
  const token = data.tokens[__VU - 1];
  const response = http.post(
    `${data.baseUrl}/api/v1/orders`,
    data.checkoutBody,
    {
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
        "Idempotency-Key": `k6-${__VU}-${__ITER}-${Date.now()}`,
      },
      tags: { name: "POST /api/v1/orders" },
    },
  );
  checkoutDuration.add(response.timings.duration);

  if (response.status === 201) {
    checkoutSuccesses.add(1);
    checkoutAcceptedOutcomes.add(true);
    if (mode === "sustained") {
      const refill = http.post(
        `${data.baseUrl}/api/v1/cart/items`,
        JSON.stringify({ productId: data.productId, quantity: 1 }),
        {
          headers: {
            Authorization: `Bearer ${token}`,
            "Content-Type": "application/json",
          },
          tags: { name: "POST /api/v1/cart/items (refill)" },
        },
      );
      if (refill.status !== 201) {
        checkoutUnexpectedErrors.add(1);
        check(refill, {
          "successful checkout cart refill returns 201": (res) =>
            res.status === 201,
        });
      }
    }
  } else if (response.status === 409 && allowInsufficientStock) {
    checkoutStockRejections.add(1);
    checkoutAcceptedOutcomes.add(true);
  } else {
    checkoutUnexpectedErrors.add(1);
    checkoutAcceptedOutcomes.add(false);
  }

  check(response, {
    "checkout returns 201 or explicitly allowed stock rejection": (res) =>
      res.status === 201 || (res.status === 409 && allowInsufficientStock),
  });
}
