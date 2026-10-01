// src/pages/tenant/BookingSuccessPage.tsx
// Landing page after Stripe (or stub) checkout: polls the booking until CONFIRMED.

import { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { CheckCircle2, Loader2, ArrowRight } from 'lucide-react';
import { BookingService } from '@/services/booking.service';
import { formatMad } from '@/lib/utils';

const POLL_INTERVAL_MS = 3000;
const MAX_ATTEMPTS = 10;

function readStoredBookingId(): number | null {
  try {
    const raw = sessionStorage.getItem('pendingPaymentBookingId');
    const parsed = raw ? parseInt(raw, 10) : NaN;
    return Number.isNaN(parsed) ? null : parsed;
  } catch {
    return null;
  }
}

export const BookingSuccessPage = () => {
  const [searchParams] = useSearchParams();
  const sessionId = searchParams.get('session_id');
  const [bookingId] = useState<number | null>(() => readStoredBookingId());
  const [status, setStatus] = useState<'loading' | 'confirmed' | 'pending' | 'unknown'>(() =>
    readStoredBookingId() == null ? 'unknown' : 'loading',
  );
  const [totalPrice, setTotalPrice] = useState<number | null>(null);

  useEffect(() => {
    if (bookingId == null) return;
    let attempts = 0;
    let cancelled = false;

    const poll = async () => {
      attempts += 1;
      try {
        const booking = await BookingService.getBookingById(bookingId);
        if (cancelled) return;
        setTotalPrice(booking.totalPrice);
        if (booking.status === 'CONFIRMED' || booking.status === 'ACTIVE' || booking.status === 'COMPLETED') {
          setStatus('confirmed');
          return;
        }
        if (booking.status === 'CANCELLED' || booking.status === 'DISPUTED') {
          setStatus('unknown');
          return;
        }
      } catch {
        if (cancelled) return;
      }
      if (attempts < MAX_ATTEMPTS && !cancelled) {
        setTimeout(poll, POLL_INTERVAL_MS);
      } else if (!cancelled) {
        setStatus('pending');
      }
    };

    void poll();
    return () => {
      cancelled = true;
    };
  }, [bookingId]);

  return (
    <div className="container mx-auto max-w-xl p-6">
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            {status === 'confirmed' ? (
              <CheckCircle2 className="h-6 w-6 text-green-600" />
            ) : (
              <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
            )}
            Payment {status === 'confirmed' ? 'confirmed' : 'processing'}
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          {sessionId && (
            <p className="text-xs text-muted-foreground">Session: {sessionId}</p>
          )}
          {status === 'loading' && (
            <p className="text-sm text-gray-600">
              Waiting for payment confirmation…
            </p>
          )}
          {status === 'pending' && (
            <p className="text-sm text-gray-600">
              Your payment is being processed. It can take a few seconds —
              check your bookings in a moment.
            </p>
          )}
          {status === 'confirmed' && (
            <p className="text-sm text-gray-600">
              Your booking{bookingId ? ` #${bookingId}` : ''}
              {totalPrice != null ? ` (${formatMad(totalPrice)})` : ''} is confirmed. Enjoy your stay!
            </p>
          )}
          {status === 'unknown' && (
            <p className="text-sm text-gray-600">
              We could not link this payment to a booking. Please check your bookings.
            </p>
          )}
          <div className="flex gap-2">
            <Button asChild>
              <Link to={bookingId ? `/bookings/${bookingId}` : '/bookings'}>
                View my booking <ArrowRight className="ml-2 h-4 w-4" />
              </Link>
            </Button>
            <Button asChild variant="outline">
              <Link to="/bookings">All bookings</Link>
            </Button>
          </div>
        </CardContent>
      </Card>
    </div>
  );
};
