#!/usr/bin/env bash
# End-to-end smoke test against a running docker compose stack.
set -euo pipefail
AUTH=${AUTH:-http://localhost:8081}; CATALOG=${CATALOG:-http://localhost:8082}
CART=${CART:-http://localhost:8083}; CHECKOUT=${CHECKOUT:-http://localhost:8085}; ORDERS=${ORDERS:-http://localhost:8084}
EMAIL="demo$RANDOM@example.com"; PASS="demo-pass-123"
json() { curl -sS -H 'Content-Type: application/json' "$@"; }
token() { sed -E 's/.*"token":"([^"]+)".*/\1/'; }

echo "1) register + login";  json -X POST $AUTH/api/auth/register -d "{\"email\":\"$EMAIL\",\"password\":\"$PASS\"}" >/dev/null
TOKEN=$(json -X POST $AUTH/api/auth/login -d "{\"email\":\"$EMAIL\",\"password\":\"$PASS\"}" | token)
H="Authorization: Bearer $TOKEN"
echo "2) product 1";         json -H "$H" $CATALOG/api/products/1; echo
echo "3) add to cart";       json -H "$H" -X POST $CART/api/cart/items -d '{"productId":1,"quantity":2}'; echo
json -H "$H" -X POST $CART/api/cart/items -d '{"productId":2,"quantity":1}' >/dev/null
KEY="demo-$(date +%s)-$RANDOM"
echo "4) checkout";          json -H "$H" -H "Idempotency-Key: $KEY" -X POST $CHECKOUT/api/checkout; echo
echo "5) same key again (must return the SAME order, stock not taken twice)"
json -H "$H" -H "Idempotency-Key: $KEY" -X POST $CHECKOUT/api/checkout; echo
echo "6) my orders";         json -H "$H" $ORDERS/api/orders; echo
