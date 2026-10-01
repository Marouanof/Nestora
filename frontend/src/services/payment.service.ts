// src/services/payment.service.ts

import { api } from '@/lib/axios';
import type { InitPaymentResponse, PaymentInfo } from '@/types/booking.types';

export class PaymentService {
  static async initPayment(bookingId: number): Promise<InitPaymentResponse> {
    const response = await api.post('/payments/init', { bookingId });
    return response.data;
  }

  static async getPaymentsByBooking(bookingId: number): Promise<PaymentInfo[]> {
    const response = await api.get(`/payments/booking/${bookingId}`);
    return response.data;
  }
}
