// src/hooks/bookings/useInitPayment.ts

import { useState } from 'react';
import { PaymentService } from '@/services/payment.service';

export const useInitPayment = () => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const initPayment = async (bookingId: number): Promise<string | null> => {
    setLoading(true);
    setError(null);
    try {
      const res = await PaymentService.initPayment(bookingId);
      if (!res.checkoutUrl) {
        setError('Aucune URL de paiement reçue');
        return null;
      }
      // Mémorise le booking pour la page /bookings/success (retour checkout)
      try {
        sessionStorage.setItem('pendingPaymentBookingId', String(bookingId));
        if (res.sessionId) sessionStorage.setItem('pendingPaymentSessionId', res.sessionId);
      } catch {
        /* stockage indisponible : la page success affichera un message générique */
      }
      // Redirection directe : Stripe Checkout (ou page succès stub) fait office de récap
      window.location.href = res.checkoutUrl;
      return res.checkoutUrl;
    } catch (err: unknown) {
      const data = (err as { response?: { data?: { error?: string; message?: string } } })
        ?.response?.data;
      setError(data?.error || data?.message || 'Failed to init payment');
      return null;
    } finally {
      setLoading(false);
    }
  };

  return { initPayment, loading, error };
};
