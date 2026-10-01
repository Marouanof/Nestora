import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { usePropertyDetails } from '@/hooks/useProperty';
import { PropertyService } from '@/services/property.service';
import { Card, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar';
import { Separator } from '@/components/ui/separator';
import { Skeleton } from '@/components/ui/skeleton';
import { PropertyImageGallery } from '@/components/properties/PropertyImageGallery';
import { ReviewList } from '@/components/properties/ReviewList';
import { ReviewForm } from '@/components/properties/ReviewForm';
import { BookingForm } from '@/components/bookings/BookingForm';
import {
  Calendar,
  Star,
  MapPin,
  Users,
  BedDouble,
  Bath,
  Heart,
  Share2,
  Shield,
  CheckCircle,
  BadgeCheck,
  Wifi as WifiIcon,
  CookingPot as CookingPotIcon,
  Car as CarIcon,
  Waves as WavesIcon,
  Tv as TvIcon,
  Shirt as ShirtIcon,
  Snowflake as SnowflakeIcon,
  Dumbbell as DumbbellIcon,
  Laptop as LaptopIcon,
  PawPrint as PawPrintIcon,
  House as HomeIcon,
} from 'lucide-react';
import { toast } from 'sonner';
import { formatMad } from '@/lib/utils';
import { authStore } from '@/store/auth.store';
import { AiBadge } from '@/components/ai/AiBadge';
import { useWishlist } from '@/hooks/useWishlist';
import type { PaginatedResponse, Review } from '@/types/property.types';

const AMENITY_ICONS: { pattern: RegExp; icon: React.ReactNode }[] = [
  { pattern: /wifi|internet/i, icon: <WifiIcon /> },
  { pattern: /kitchen/i, icon: <CookingPotIcon /> },
  { pattern: /park/i, icon: <CarIcon /> },
  { pattern: /pool/i, icon: <WavesIcon /> },
  { pattern: /tv|screen|netflix/i, icon: <TvIcon /> },
  { pattern: /wash|laundry|machine/i, icon: <ShirtIcon /> },
  { pattern: /air.?cond|ac\b|clim/i, icon: <SnowflakeIcon /> },
  { pattern: /gym|fitness/i, icon: <DumbbellIcon /> },
  { pattern: /work|desk|office/i, icon: <LaptopIcon /> },
  { pattern: /pet/i, icon: <PawPrintIcon /> },
];

function AmenityIcon({ amenity }: { amenity: string }) {
  const match = AMENITY_ICONS.find(({ pattern }) => pattern.test(amenity));
  return (
    <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-muted text-muted-foreground">
      {match?.icon ?? <HomeIcon />}
    </span>
  );
}

export const PropertyDetailsPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const propertyId = id ? parseInt(id) : null;
  const { property, loading, error, refetch } = usePropertyDetails(propertyId);
  const [unavailableDates, setUnavailableDates] = useState<string[]>([]);
  const [reviews, setReviews] = useState<PaginatedResponse<Review> | null>(null);
  const { liked, toggling, toggle } = useWishlist(propertyId);
  const { user } = authStore();
  const isTenant = user?.role === 'ROLE_TENANT';
  const navigate = useNavigate();

  // Fetch unavailable dates when property loads
  useEffect(() => {
    if (propertyId && !loading) {
      PropertyService.getUnavailableDates(propertyId)
        .then((dates) => setUnavailableDates(dates))
        .catch(() => setUnavailableDates([]));
    }
  }, [propertyId, loading]);

  // Fetch reviews when property loads
  useEffect(() => {
    if (propertyId) {
      PropertyService.getReviews(propertyId, 0, 10)
        .then((data) => setReviews(data))
        .catch(() => setReviews(null));
    }
  }, [propertyId]);

  const handleShare = async () => {
    try {
      await navigator.clipboard.writeText(window.location.href);
      toast.success('Link copied to clipboard');
    } catch {
      toast.error('Could not copy the link');
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-background">
        <div className="mx-auto max-w-7xl px-4 pt-6 sm:px-6 lg:px-8">
          <Skeleton className="aspect-[21/9] w-full rounded-3xl" />
        </div>
        <div className="mx-auto max-w-7xl space-y-6 px-4 py-10 sm:px-6 lg:px-8">
          <Skeleton className="h-10 w-2/3 rounded-md" />
          <Skeleton className="h-5 w-1/3 rounded-md" />
          <div className="grid grid-cols-1 gap-12 lg:grid-cols-3">
            <div className="space-y-4 lg:col-span-2">
              <Skeleton className="h-24 w-full rounded-2xl" />
              <Skeleton className="h-32 w-full rounded-2xl" />
            </div>
            <Skeleton className="h-72 w-full rounded-3xl" />
          </div>
        </div>
      </div>
    );
  }

  if (error || !property) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-background px-4">
        <Card className="w-full max-w-md rounded-3xl border-border/60 shadow-sm">
          <CardContent className="p-8 text-center">
            <h1 className="section-heading mb-2 text-2xl text-foreground">Something went wrong</h1>
            <p className="mb-6 text-sm text-muted-foreground">{error || 'Property not found'}</p>
            <Button onClick={refetch}>Try Again</Button>
          </CardContent>
        </Card>
      </div>
    );
  }

  const averageRating =
    reviews && reviews.content && reviews.content.length > 0
      ? reviews.content.reduce((sum: number, review: Review) => sum + review.rating, 0) / reviews.content.length
      : 0;

  const stats = [
    { icon: <Users className="h-5 w-5" />, label: `${property.maxGuests} guests` },
    { icon: <BedDouble className="h-5 w-5" />, label: `${property.bedrooms} bedrooms` },
    { icon: <Bath className="h-5 w-5" />, label: `${property.bathrooms} bathrooms` },
    { icon: <Calendar className="h-5 w-5" />, label: `Min ${property.minStayNights} nights` },
  ];

  return (
    <div className="min-h-screen bg-background">
      {/* Hero Section with Images */}
      <div className="relative mx-auto max-w-7xl px-4 pt-6 sm:px-6 lg:px-8">
        <PropertyImageGallery images={property.images || []} />

        {/* Overlay Actions */}
        <div className="absolute right-7 top-7 flex gap-2">
          <Button
            variant="secondary"
            size="sm"
            onClick={handleShare}
            className="rounded-full bg-white/90 shadow-sm backdrop-blur hover:bg-white dark:bg-black/50 dark:text-white dark:hover:bg-black/70"
          >
            <Share2 className="mr-2 h-4 w-4" />
            Share
          </Button>
          <Button
            variant="secondary"
            size="icon"
            aria-label={liked ? 'Remove from favorites' : 'Add to favorites'}
            onClick={toggle}
            disabled={toggling}
            className="h-9 w-9 rounded-full bg-white/90 shadow-sm backdrop-blur hover:bg-white dark:bg-black/50 dark:text-white dark:hover:bg-black/70"
          >
            <Heart className={`h-4 w-4 transition-colors ${liked ? 'fill-red-500 text-red-500' : ''}`} />
          </Button>
        </div>
      </div>

      {/* Main Content */}
      <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
        <div className="grid grid-cols-1 gap-12 lg:grid-cols-3">
          {/* Left Column - Property Details */}
          <div className="space-y-10 lg:col-span-2">
            {/* Property Header */}
            <header>
              <p className="landing-eyebrow mb-3 text-primary/70">Nestora · Stays</p>
              <h1 className="section-heading mb-3 text-3xl leading-tight tracking-[-0.02em] text-foreground sm:text-4xl">
                {property.title}
              </h1>
              <div className="flex flex-wrap items-center gap-x-5 gap-y-2 text-muted-foreground">
                <span className="flex items-center gap-1.5">
                  <MapPin className="h-4 w-4" />
                  {property.address.city}, {property.address.country}
                </span>
                {averageRating > 0 && (
                  <span className="flex items-center gap-1.5 font-medium text-foreground">
                    <Star className="h-4 w-4 fill-yellow-400 text-yellow-400" />
                    {averageRating.toFixed(1)}
                    <span className="font-normal text-muted-foreground">({reviews?.totalElements || 0} reviews)</span>
                  </span>
                )}
              </div>

              {/* Price + AI suggestion */}
              <div className="mt-6 flex flex-wrap items-end gap-x-8 gap-y-3">
                <div>
                  <span className="font-display text-3xl font-semibold text-foreground">
                    {formatMad(property.pricePerNight)}
                  </span>
                  <span className="ml-1.5 text-muted-foreground">per night</span>
                </div>
                {property.suggestedPricePerNight && property.suggestedPricePerNight !== property.pricePerNight && (
                  <div className="flex items-center gap-2 rounded-full border border-warning/30 bg-warning/10 px-3.5 py-1.5">
                    <AiBadge size="sm" />
                    <span className="text-sm font-semibold text-warning">
                      AI suggests {formatMad(property.suggestedPricePerNight)}/night
                    </span>
                  </div>
                )}
              </div>
            </header>

            {/* Property Stats */}
            <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
              {stats.map((stat) => (
                <div
                  key={stat.label}
                  className="flex items-center gap-2.5 rounded-xl border border-border/60 bg-card px-3.5 py-3"
                >
                  <span className="text-muted-foreground">{stat.icon}</span>
                  <span className="text-sm font-medium text-foreground">{stat.label}</span>
                </div>
              ))}
            </div>

            {/* Owner Information */}
            <div className="flex flex-wrap items-center gap-x-5 gap-y-4 rounded-2xl border border-border/60 bg-card p-5">
              <Avatar className="h-14 w-14 ring-2 ring-primary/15">
                <AvatarImage
                  src={property.ownerProfilePicture}
                  alt={`${property.ownerFirstName} ${property.ownerLastName}`}
                />
                <AvatarFallback className="bg-gradient-to-br from-primary/20 to-secondary/20">
                  {property.ownerFirstName?.[0]}
                  {property.ownerLastName?.[0]}
                </AvatarFallback>
              </Avatar>
              <div className="min-w-0 flex-1">
                <p className="text-xs uppercase tracking-wide text-muted-foreground">Hosted by</p>
                <p className="flex items-center gap-1.5 truncate text-lg font-medium text-foreground">
                  {property.ownerFirstName} {property.ownerLastName}
                  <BadgeCheck className="h-4 w-4 shrink-0 text-secondary" aria-label="Verified host" />
                </p>
              </div>
              <Button variant="outline" size="sm" className="rounded-full" onClick={() => navigate(`/users/${property.ownerId}`)}>
                View Profile
              </Button>
            </div>

            <Separator />

            {/* Description */}
            <section>
              <h2 className="section-heading mb-4 text-2xl text-foreground">About this place</h2>
              <p className="leading-relaxed text-muted-foreground">{property.description}</p>
            </section>

            <Separator />

            {/* Amenities */}
            <section>
              <h2 className="section-heading mb-6 text-2xl text-foreground">What this place offers</h2>
              <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
                {property.amenities.map((amenity, index) => (
                  <div key={index} className="flex items-center gap-3 rounded-xl p-2 transition-colors hover:bg-muted/60">
                    <AmenityIcon amenity={amenity} />
                    <span className="text-foreground">{amenity}</span>
                  </div>
                ))}
              </div>
            </section>

            <Separator />

            {/* Property Rules & Policies */}
            <section>
              <h2 className="section-heading mb-6 text-2xl text-foreground">Things to know</h2>
              <div className="grid grid-cols-1 gap-6 md:grid-cols-3">
                <div className="rounded-2xl border border-border/60 bg-card p-5">
                  <h3 className="mb-3 flex items-center gap-2 text-sm font-semibold uppercase tracking-wide text-muted-foreground">
                    <Shield className="h-4 w-4" />
                    House rules
                  </h3>
                  <ul className="space-y-2.5 text-muted-foreground">
                    <li className="flex items-center gap-2">
                      <CheckCircle className="h-4 w-4 shrink-0 text-success" />
                      Check-in after 3:00 PM
                    </li>
                    <li className="flex items-center gap-2">
                      <CheckCircle className="h-4 w-4 shrink-0 text-success" />
                      Checkout before 11:00 AM
                    </li>
                    <li className="flex items-center gap-2">
                      <CheckCircle className="h-4 w-4 shrink-0 text-success" />
                      {property.maxGuests} guests maximum
                    </li>
                  </ul>
                </div>

                <div className="rounded-2xl border border-border/60 bg-card p-5">
                  <h3 className="mb-3 text-sm font-semibold uppercase tracking-wide text-muted-foreground">
                    Cancellation policy
                  </h3>
                  <p className="text-muted-foreground">
                    Free cancellation up to{' '}
                    <span className="font-semibold text-foreground">{property.cancellationPolicyDays} days</span>{' '}
                    before check-in.
                  </p>
                </div>

                <div className="rounded-2xl border border-border/60 bg-card p-5">
                  <h3 className="mb-3 text-sm font-semibold uppercase tracking-wide text-muted-foreground">
                    Safety & property
                  </h3>
                  <ul className="space-y-2.5 text-muted-foreground">
                    <li className="flex items-center gap-2">
                      <CheckCircle className="h-4 w-4 shrink-0 text-success" />
                      Security deposit: {formatMad(property.securityDeposit || 0)}
                    </li>
                    <li className="flex items-center gap-2">
                      <CheckCircle className="h-4 w-4 shrink-0 text-success" />
                      Verified host
                    </li>
                  </ul>
                </div>
              </div>
            </section>

            <Separator />

            {/* Reviews Section */}
            {reviews && reviews.content && reviews.content.length > 0 && (
              <section>
                <div className="mb-6 flex items-center gap-3">
                  <Star className="h-6 w-6 fill-yellow-400 text-yellow-400" />
                  <span className="section-heading text-2xl text-foreground">
                    {averageRating.toFixed(1)}
                    <span className="ml-2 font-body text-base font-normal text-muted-foreground">
                      · {reviews.totalElements} reviews
                    </span>
                  </span>
                </div>
                <ReviewList
                  reviews={reviews}
                  onPageChange={() => {
                    if (propertyId) {
                      PropertyService.getReviews(propertyId, 0, 10).then(setReviews);
                    }
                  }}
                />
              </section>
            )}

            {/* Review Form - Only for Tenants */}
            {isTenant && propertyId && (
              <>
                <Separator />
                <ReviewForm propertyId={propertyId} onReviewSubmitted={() => refetch()} />
              </>
            )}
          </div>

          {/* Right Column - Booking Form */}
          <div className="lg:col-span-1">
            <BookingForm
              property={property}
              averageRating={averageRating}
              reviewCount={reviews?.totalElements || 0}
              unavailableDates={unavailableDates}
            />
          </div>
        </div>
      </div>
    </div>
  );
};
