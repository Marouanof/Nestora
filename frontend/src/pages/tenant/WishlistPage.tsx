// src/pages/tenant/WishlistPage.tsx

import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Card, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';
import { Heart, HeartOff } from 'lucide-react';
import { WishlistService } from '@/services/wishlist.service';
import { PropertyService } from '@/services/property.service';
import { PropertyCard } from '@/components/properties/PropertyCard';
import type { Property } from '@/types/property.types';

export const WishlistPage = () => {
  const [properties, setProperties] = useState<Property[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchWishlist = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await WishlistService.getWishlist();
      const ids = res.data?.propertyIds ?? [];
      const results = await Promise.allSettled(ids.map((id) => PropertyService.getById(id)));
      setProperties(
        results
          .filter((r): r is PromiseFulfilledResult<Property> => r.status === 'fulfilled')
          .map((r) => r.value)
      );
    } catch {
      setError('Could not load your wishlist');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchWishlist();
  }, [fetchWishlist]);

  // Re-sync when coming back (e.g. after unliking a card on this page)
  useEffect(() => {
    const onFocus = () => fetchWishlist();
    window.addEventListener('focus', onFocus);
    return () => window.removeEventListener('focus', onFocus);
  }, [fetchWishlist]);

  return (
    <div className="container mx-auto p-6">
      <div className="mb-6">
        <h1 className="flex items-center gap-2 text-3xl font-bold">
          <Heart className="h-7 w-7 fill-red-500 text-red-500" />
          My Wishlist
        </h1>
        <p className="text-gray-600">Properties you liked</p>
      </div>

      {loading && (
        <div className="grid grid-cols-1 gap-x-6 gap-y-10 sm:grid-cols-2 lg:grid-cols-3">
          {Array.from({ length: 6 }).map((_, i) => (
            <div key={i}>
              <Skeleton className="mb-4 aspect-[4/3] w-full rounded-2xl" />
              <Skeleton className="mb-2 h-5 w-3/4 rounded-md" />
              <Skeleton className="h-4 w-1/2 rounded-md" />
            </div>
          ))}
        </div>
      )}

      {!loading && error && (
        <Card>
          <CardContent className="p-6 text-center">
            <p className="mb-4 text-red-600">{error}</p>
            <Button onClick={fetchWishlist}>Try Again</Button>
          </CardContent>
        </Card>
      )}

      {!loading && !error && properties.length === 0 && (
        <Card>
          <CardContent className="flex flex-col items-center gap-4 p-10 text-center">
            <HeartOff className="h-10 w-10 text-muted-foreground" />
            <p className="text-gray-500">You haven't liked any property yet.</p>
            <Button asChild>
              <Link to="/properties">Browse Properties</Link>
            </Button>
          </CardContent>
        </Card>
      )}

      {!loading && !error && properties.length > 0 && (
        <div className="grid grid-cols-1 gap-x-6 gap-y-10 sm:grid-cols-2 lg:grid-cols-3">
          {properties.map((property) => (
            <PropertyCard key={property.id} property={property} />
          ))}
        </div>
      )}
    </div>
  );
};
