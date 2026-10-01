import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Card, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Separator } from '@/components/ui/separator';
import { Star, Lock, ShieldCheck, ArrowRight } from 'lucide-react';
import { useCreateBooking } from '@/hooks/bookings/useCreateBooking';
import { BookingCalendar } from './BookingCalendar';
import { authStore } from '@/store/auth.store';
import { toast } from 'sonner';
import { formatMad } from '@/lib/utils';
import type { Property } from '@/types/property.types';

interface BookingFormProps {
  property: Property;
  averageRating?: number;
  reviewCount?: number;
  unavailableDates?: string[];
}

export const BookingForm = ({ property, averageRating, reviewCount, unavailableDates = [] }: BookingFormProps) => {
  const [checkIn, setCheckIn] = useState('');
  const [checkOut, setCheckOut] = useState('');
  const [guests, setGuests] = useState(1);
  const { createBooking, loading } = useCreateBooking();
  const navigate = useNavigate();
  const { user } = authStore();

  // Check if user is tenant
  const isTenant = user?.role === 'ROLE_TENANT';
  const isAuthenticated = !!user;

  const handleDateRangeSelect = (newCheckIn: string, newCheckOut: string) => {
    setCheckIn(newCheckIn);
    setCheckOut(newCheckOut);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!checkIn || !checkOut) {
      toast.error('Please select check-in and check-out dates');
      return;
    }

    if (new Date(checkIn) >= new Date(checkOut)) {
      toast.error('Check-out date must be after check-in date');
      return;
    }

    const bookingData = {
      propertyId: property.id,
      checkIn,
      checkOut,
      numberOfGuests: guests,
    };

    const result = await createBooking(bookingData);
    if (result) {
      toast.success('Booking created successfully!');
      navigate('/bookings');
    }
  };

  const calculateNights = () => {
    if (!checkIn || !checkOut) return 0;
    const start = new Date(checkIn);
    const end = new Date(checkOut);
    const diffTime = Math.abs(end.getTime() - start.getTime());
    return Math.ceil(diffTime / (1000 * 60 * 60 * 24));
  };

  // Single source of truth for the price breakdown
  const nights = calculateNights();
  const subtotal = nights * property.pricePerNight;
  const serviceFee = Math.round(subtotal * 0.14);
  const securityDeposit = property.securityDeposit || 0;
  const total = subtotal + serviceFee + securityDeposit;

  const cardClass =
    'rounded-3xl border border-border/60 bg-card shadow-[0_24px_56px_-40px_rgba(13,11,38,0.35)]';

  // Don't show anything if not a tenant
  if (!isTenant) {
    if (!isAuthenticated) {
      return (
        <Card className={`sticky top-8 ${cardClass}`}>
          <CardContent className="p-7 text-center">
            <div className="mx-auto mb-4 flex h-12 w-12 items-center justify-center rounded-full bg-muted">
              <Lock className="h-5 w-5 text-muted-foreground" />
            </div>
            <h3 className="section-heading mb-1.5 text-xl text-foreground">Sign in to book</h3>
            <p className="mb-6 text-sm text-muted-foreground">
              You need to be logged in as a tenant to book this property.
            </p>
            <Button onClick={() => navigate('/login')} size="lg" className="w-full rounded-full">
              Sign In
              <ArrowRight className="ml-2 h-4 w-4" />
            </Button>
          </CardContent>
        </Card>
      );
    }

    return (
      <Card className={`sticky top-8 ${cardClass}`}>
        <CardContent className="p-7 text-center">
          <div className="mx-auto mb-4 flex h-12 w-12 items-center justify-center rounded-full bg-muted">
            <Lock className="h-5 w-5 text-muted-foreground" />
          </div>
          <h3 className="section-heading mb-1.5 text-xl text-foreground">Booking not available</h3>
          <p className="text-sm text-muted-foreground">
            Only tenants can book properties. If you're an owner, switch to your tenant account to book.
          </p>
        </CardContent>
      </Card>
    );
  }

  return (
    <div className="sticky top-8 space-y-5">
      {/* Price Card */}
      <Card className={cardClass}>
        <CardContent className="p-6">
          <div className="mb-6 flex items-center justify-between">
            <div>
              <span className="font-display text-3xl font-semibold text-foreground">
                {formatMad(property.pricePerNight)}
              </span>
              <span className="ml-1 text-muted-foreground">night</span>
            </div>
            {averageRating != null && reviewCount != null && (
              <div className="flex items-center gap-1 rounded-full border border-border/60 px-3 py-1 text-sm">
                <Star className="h-4 w-4 fill-yellow-400 text-yellow-400" />
                <span className="font-medium">{averageRating > 0 ? averageRating.toFixed(1) : 'New'}</span>
                {reviewCount > 0 && (
                  <span className="text-muted-foreground">({reviewCount})</span>
                )}
              </div>
            )}
          </div>

          {/* Guests Selection */}
          <div className="mb-5">
            <label className="mb-2 block text-xs font-semibold uppercase tracking-wide text-muted-foreground">
              Guests
            </label>
            <Select value={guests.toString()} onValueChange={(value) => setGuests(parseInt(value))}>
              <SelectTrigger className="h-11 rounded-xl">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {Array.from({ length: property.maxGuests }, (_, i) => (
                  <SelectItem key={i + 1} value={(i + 1).toString()}>
                    {i + 1} guest{i !== 0 ? 's' : ''}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          {/* Summary */}
          {nights > 0 && (
            <>
              <Separator className="my-5" />
              <div className="space-y-3 text-sm">
                <div className="flex justify-between">
                  <span className="text-muted-foreground underline decoration-border underline-offset-4">
                    {formatMad(property.pricePerNight)} × {nights} night{nights > 1 ? 's' : ''}
                  </span>
                  <span>{formatMad(subtotal)}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-muted-foreground underline decoration-border underline-offset-4">
                    Service fee
                  </span>
                  <span>{formatMad(serviceFee)}</span>
                </div>
                {securityDeposit > 0 && (
                  <div className="flex justify-between">
                    <span className="text-muted-foreground underline decoration-border underline-offset-4">
                      Security deposit
                    </span>
                    <span>{formatMad(securityDeposit)}</span>
                  </div>
                )}
                <Separator />
                <div className="flex justify-between text-base font-semibold text-foreground">
                  <span>Total</span>
                  <span>{formatMad(total)}</span>
                </div>
              </div>
            </>
          )}
        </CardContent>
      </Card>

      {/* Calendar */}
      <BookingCalendar
        unavailableDates={unavailableDates}
        onDateRangeSelect={handleDateRangeSelect}
        selectedCheckIn={checkIn}
        selectedCheckOut={checkOut}
        minNights={property.minStayNights}
      />

      {/* Book Button */}
      <form onSubmit={handleSubmit}>
        <Button type="submit" size="lg" disabled={loading || !checkIn || !checkOut} className="h-12 w-full rounded-full text-base font-semibold transition-all duration-300 hover:-translate-y-px hover:shadow-[0_16px_32px_-16px_rgba(81,70,229,0.55)] disabled:hover:translate-y-0 disabled:hover:shadow-none">
          {loading ? 'Creating booking…' : property.instantBookable ? 'Reserve' : 'Request to book'}
        </Button>

        {!property.instantBookable ? (
          <p className="mt-3 flex items-center justify-center gap-1.5 text-xs text-muted-foreground">
            <Lock className="h-3 w-3" />
            You won't be charged yet
          </p>
        ) : (
          <p className="mt-3 flex items-center justify-center gap-1.5 text-xs text-muted-foreground">
            <ShieldCheck className="h-3.5 w-3.5 text-success" />
            Free cancellation up to {property.cancellationPolicyDays} days before check-in
          </p>
        )}
      </form>
    </div>
  );
};
