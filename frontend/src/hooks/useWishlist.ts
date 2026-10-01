// src/hooks/useWishlist.ts

import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'sonner';
import { WishlistService } from '@/services/wishlist.service';
import { authStore } from '@/store/auth.store';

export const useWishlist = (propertyId: number | null) => {
  const navigate = useNavigate();
  const { user } = authStore();
  const [liked, setLiked] = useState(false);
  const [checking, setChecking] = useState(true);
  const [toggling, setToggling] = useState(false);

  useEffect(() => {
    let cancelled = false;
    if (!user || !propertyId) {
      setLiked(false);
      setChecking(false);
      return;
    }
    setChecking(true);
    WishlistService.isLiked(propertyId)
      .then((res) => {
        if (!cancelled) setLiked(res.data === true);
      })
      .catch(() => {
        if (!cancelled) setLiked(false);
      })
      .finally(() => {
        if (!cancelled) setChecking(false);
      });
    return () => {
      cancelled = true;
    };
  }, [propertyId, user?.email]);

  const toggle = useCallback(async () => {
    if (!propertyId) return;
    if (!user) {
      toast.error('Connecte-toi pour liker une propriété');
      navigate('/login');
      return;
    }
    if (toggling) return;
    const previous = liked;
    setLiked(!previous);
    setToggling(true);
    try {
      if (previous) {
        await WishlistService.remove(propertyId);
        toast.success('Retiré de tes favoris');
      } else {
        await WishlistService.add(propertyId);
        toast.success('Ajouté à tes favoris');
      }
    } catch {
      setLiked(previous);
      toast.error('Impossible de mettre à jour tes favoris');
    } finally {
      setToggling(false);
    }
  }, [user, liked, toggling, propertyId, navigate]);

  return { liked, checking, toggling, toggle };
};
