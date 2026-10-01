// src/types/booking.types.ts

export type BookingStatus =
  | 'PENDING_PAYMENT'
  | 'PAYMENT_PROCESSING'
  | 'CONFIRMED'
  | 'ACTIVE'
  | 'COMPLETED'
  | 'CANCELLED'
  | 'DISPUTED'
  // Legacy alias (backend never sends PENDING, kept for compat)
  | 'PENDING';

export interface Booking {
  id: number;
  propertyId: number;
  tenantId: number;
  ownerId?: number;
  checkIn: string; // YYYY-MM-DD
  checkOut: string; // YYYY-MM-DD
  numberOfGuests: number;
  totalPrice: number;
  securityDeposit?: number;
  numberOfNights?: number;
  status: BookingStatus;
  createdAt: string;
  updatedAt?: string;
  confirmedAt?: string | null;
  cancelledAt?: string | null;
  completedAt?: string | null;
  paymentConfirmedAt?: string | null;
  propertyTitle?: string;
  tenantName?: string;
  ownerName?: string;
  property?: {
    id: number;
    title: string;
    address: {
      street: string;
      city: string;
      country: string;
    };
  };
  tenant?: {
    id: number;
    name: string;
    email: string;
  };
  cancellationReason?: string;
}

export interface CreateBookingData {
  propertyId: number;
  checkIn: string; // YYYY-MM-DD
  checkOut: string; // YYYY-MM-DD
  numberOfGuests: number;
}

export interface CancelBookingData {
  reason: string;
}

export interface OwnerStats {
  totalBookings: number;
  activeBookings?: number;
  pendingBookings?: number;
  confirmedBookings?: number;
  cancelledBookings?: number;
  totalRevenue: number;
  bookingsByStatus?: Record<string, number>;
  // Legacy fields (kept for compat)
  revenue?: number;
  cancellations?: number;
  occupancyRate?: number;
  // Add more as needed
}

export interface InitPaymentResponse {
  paymentId: number;
  bookingId: number;
  amount: number;
  currency: string;
  checkoutUrl: string;
  sessionId: string;
  status: string;
}

export interface PaymentInfo {
  id: number;
  bookingId: number;
  amount: number;
  currency: string;
  status: string;
  checkoutUrl?: string;
}