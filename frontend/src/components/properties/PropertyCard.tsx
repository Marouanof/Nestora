import React, { useState } from 'react';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Heart, Star, MapPin } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { formatMad } from '@/lib/utils';
import { AiBadge } from '@/components/ai/AiBadge';
import type { PropertySummary } from '@/types/property.types';

interface PropertyCardProps {
  property: PropertySummary;
  showOwnerActions?: boolean;
  onEdit?: (id: number) => void;
  onDelete?: (id: number) => void;
}

export const PropertyCard: React.FC<PropertyCardProps> = ({
  property,
  showOwnerActions = false,
  onEdit,
  onDelete,
}) => {
  const navigate = useNavigate();
  const [liked, setLiked] = useState(false);

  // Get the main image (first image with displayOrder 0, or first image if no displayOrder 0)
  const mainImage = property.images?.find((img) => img.displayOrder === 0) || property.images?.[0];
  const FALLBACK_IMAGE = '/images/nestora/property-01.webp';

  return (
    <article
      className="group cursor-pointer"
      onClick={() => navigate(`/properties/${property.id}`)}
    >
      {/* Image */}
      <div className="card-zoom relative aspect-[4/3] w-full rounded-2xl border border-border/50 bg-muted shadow-[0_16px_40px_-28px_rgba(13,11,38,0.35)]">
        {mainImage ? (
          <img
            src={mainImage.imageUrl}
            alt={mainImage.caption || property.title}
            loading="lazy"
            className="h-full w-full object-cover"
            onError={(e) => {
              if (e.currentTarget.src !== window.location.origin + FALLBACK_IMAGE) {
                e.currentTarget.src = FALLBACK_IMAGE;
              }
            }}
          />
        ) : (
          <img
            src={FALLBACK_IMAGE}
            alt={property.title}
            loading="lazy"
            className="h-full w-full object-cover"
          />
        )}

        {/* Bottom scrim for badges */}
        <div
          aria-hidden="true"
          className="pointer-events-none absolute inset-x-0 bottom-0 h-20 bg-gradient-to-t from-black/45 to-transparent"
        />

        {/* Favorite */}
        <Button
          variant="ghost"
          size="icon"
          tabIndex={-1}
          aria-label={liked ? 'Remove from favorites' : 'Add to favorites'}
          className="absolute right-3 top-3 h-9 w-9 rounded-full bg-white/85 text-foreground shadow-sm backdrop-blur transition-transform hover:scale-105 hover:bg-white active:scale-95 dark:bg-black/45 dark:text-white dark:hover:bg-black/65"
          onClick={(e) => {
            e.stopPropagation();
            setLiked((v) => !v);
          }}
        >
          <Heart className={`h-4 w-4 transition-colors ${liked ? 'fill-red-500 text-red-500' : ''}`} />
        </Button>

        {/* Rating pill */}
        {property.averageRating != null && (
          <div className="absolute bottom-3 left-3 flex items-center gap-1 rounded-full bg-white/90 px-2.5 py-1 text-xs font-semibold text-foreground shadow-sm backdrop-blur dark:bg-black/55 dark:text-white">
            <Star className="h-3.5 w-3.5 fill-yellow-400 text-yellow-400" />
            {property.averageRating.toFixed(1)}
          </div>
        )}

        {!property.instantBookable && (
          <Badge className="absolute left-3 top-3 rounded-full bg-primary/95 px-2.5 text-[11px] font-medium text-primary-foreground shadow-sm">
            Request to book
          </Badge>
        )}
      </div>

      {/* Content */}
      <div className="px-1 pt-4">
        <div className="flex items-start justify-between gap-3">
          <h3 className="line-clamp-1 text-base font-semibold tracking-tight text-foreground">
            {property.title}
          </h3>
          {property.suggestedPricePerNight && property.suggestedPricePerNight !== property.pricePerNight && (
            <span title={`AI suggested: ${formatMad(property.suggestedPricePerNight)}/night`}>
              <AiBadge size="sm" />
            </span>
          )}
        </div>

        <p className="mt-0.5 flex items-center gap-1 text-sm text-muted-foreground">
          <MapPin className="h-3.5 w-3.5" />
          {property.address.city}, {property.address.country}
        </p>

        <p className="mt-1.5 line-clamp-1 text-sm text-muted-foreground">
          {property.bedrooms} bed · {property.bathrooms} bath · Up to {property.maxGuests} guests
        </p>

        <div className="mt-2.5 flex flex-wrap items-center gap-1.5">
          {property.amenities.slice(0, 3).map((amenity, index) => (
            <span
              key={index}
              className="rounded-full border border-border/70 px-2 py-0.5 text-[11px] font-medium text-muted-foreground"
            >
              {amenity}
            </span>
          ))}
          {property.amenities.length > 3 && (
            <span className="rounded-full border border-border/70 px-2 py-0.5 text-[11px] font-medium text-muted-foreground">
              +{property.amenities.length - 3}
            </span>
          )}
        </div>

        <div className="mt-3 pt-1">
          <span className="font-display text-lg font-semibold text-foreground">
            {formatMad(property.pricePerNight)}
          </span>
          <span className="ml-1 text-sm text-muted-foreground">/ night</span>
        </div>

        {showOwnerActions && (
          <div className="mt-4 flex gap-2" onClick={(e) => e.stopPropagation()}>
            <Button
              variant="outline"
              size="sm"
              className="rounded-full"
              onClick={() => onEdit?.(property.id)}
            >
              Edit
            </Button>
            <Button
              variant="destructive"
              size="sm"
              className="rounded-full"
              onClick={() => onDelete?.(property.id)}
            >
              Delete
            </Button>
          </div>
        )}
      </div>
    </article>
  );
};
