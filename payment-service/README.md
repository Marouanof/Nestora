# payment-service — Stripe Checkout + webhooks

Microservice `:8084` : crée des Checkout Sessions Stripe pour un booking `PENDING_PAYMENT`,
vérifie les webhooks (`Stripe-Signature`) et publie `PAYMENT_SUCCESS / PAYMENT_FAILED`
sur `booking.exchange` vers `booking-service` + `notification-service`.

## Endpoints
- `POST /api/payments/init {bookingId}` (headers `Authorization: Bearer`, `X-Auth-*` via gateway) → `{checkoutUrl, sessionId}`
- `GET /api/payments/booking/{bookingId}`
- `POST /api/webhook/payment` (public, signature Stripe obligatoire)

## Config (env)
```
SERVER_PORT=8084
DB_HOST/DB_PORT/DB_NAME=payment_db/DB_USER/DB_PASS
RABBIT_HOST/RABBIT_PORT/RABBIT_USER/RABBIT_PASS
GATEWAY_SHARED_SECRET=...
BOOKING_SERVICE_URL=http://localhost:8083
STRIPE_SECRET_KEY=sk_test_...
STRIPE_WEBHOOK_SECRET=whsec_...
STRIPE_SUCCESS_URL=http://localhost:5173/bookings/success?session_id={CHECKOUT_SESSION_ID}
STRIPE_CANCEL_URL=http://localhost:5173/bookings/cancel
STRIPE_CURRENCY=mad
STRIPE_STUB=false
```

Sans `STRIPE_SECRET_KEY`, `/init` répond `503` (volontaire). Webhook local :
`stripe listen --forward-to localhost:8084/api/webhook/payment`

## Mode stub (local/test, Stripe indisponible au Maroc)

`STRIPE_STUB=true` → `/init` crée une session fictive `stub_<uuid>` (même
`InitPaymentResponse`, `checkoutUrl` vers le frontend) sans appeler Stripe.
Simuler le webhook ensuite (refusé `403` si stub désactivé) :
```powershell
$env:STRIPE_STUB="true"
# 1. init
curl.exe -X POST http://localhost:8084/api/payments/init `
  -H "Content-Type: application/json" `
  -H "X-Gateway-Secret: $env:GATEWAY_SHARED_SECRET" -H "X-Auth-User-Id: 11" `
  -d '{"bookingId":2}'
# 2. simuler checkout.session.completed (ou "success":false pour FAILED)
curl.exe -X POST http://localhost:8084/api/test/payments/stub/complete `
  -H "Content-Type: application/json" `
  -H "X-Gateway-Secret: $env:GATEWAY_SHARED_SECRET" -H "X-Auth-User-Id: 11" `
  -d '{"sessionId":"stub_...","success":true}'
# -> PAYMENT_SUCCESS sur booking.exchange -> booking CONFIRMED + notifs
```
Ne jamais activer en production.
