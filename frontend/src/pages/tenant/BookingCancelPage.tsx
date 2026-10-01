// src/pages/tenant/BookingCancelPage.tsx
// Landing page when the payer abandons checkout (Stripe cancel_url).

import { Link } from 'react-router-dom';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { XCircle, ArrowLeft, RotateCcw } from 'lucide-react';

export const BookingCancelPage = () => {
  let bookingId: number | null = null;
  try {
    const raw = sessionStorage.getItem('pendingPaymentBookingId');
    bookingId = raw ? parseInt(raw, 10) : null;
    if (bookingId != null && Number.isNaN(bookingId)) bookingId = null;
  } catch {
    bookingId = null;
  }

  return (
    <div className="container mx-auto max-w-xl p-6">
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <XCircle className="h-6 w-6 text-red-600" />
            Payment cancelled
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <p className="text-sm text-gray-600">
            You left the payment page before completing it. Your booking is kept
            in pending state for a short while — no money was taken.
          </p>
          <div className="flex gap-2">
            {bookingId != null && (
              <Button asChild>
                <Link to={`/bookings/${bookingId}`}>
                  <RotateCcw className="mr-2 h-4 w-4" /> Retry payment
                </Link>
              </Button>
            )}
            <Button asChild variant="outline">
              <Link to="/bookings">
                <ArrowLeft className="mr-2 h-4 w-4" /> Back to my bookings
              </Link>
            </Button>
          </div>
        </CardContent>
      </Card>
    </div>
  );
};
