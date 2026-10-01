import { useNavigate } from 'react-router-dom';
import { Star, BedDouble, Users } from 'lucide-react';
import Reveal from './Reveal';
import { formatMad } from '@/lib/utils';
import type { PropertySummary } from '@/types/property.types';

const FALLBACK_IMAGES = [
  '/images/nestora/property-01.webp',
  '/images/nestora/property-02.webp',
  '/images/nestora/property-03.webp',
  '/images/nestora/property-04.webp',
];

interface PropertyShowcaseProps {
  properties: PropertySummary[] | null;
  loading: boolean;
}

function firstImage(property: PropertySummary, fallback: string): string {
  const sorted = [...(property.images ?? [])]
    .sort((a, b) => (a.displayOrder ?? 0) - (b.displayOrder ?? 0))
    .find((img) => img.imageUrl);
  return sorted?.imageUrl || fallback;
}

export default function PropertyShowcase({ properties, loading }: PropertyShowcaseProps) {
  const navigate = useNavigate();
  const items = (properties ?? []).slice(0, 4);

  if (loading) {
    return (
    <section id="properties" className="relative -mt-8 lg:-mt-12 pt-10 pb-24 lg:pt-16 lg:pb-36">
        <div className="mx-auto max-w-[1240px] px-4 sm:px-6 lg:px-8">
          <div className="grid grid-cols-12 gap-6">
            {[0, 1].map((i) => (
              <div
                key={i}
                className={`col-span-12 lg:col-span-7 ${i % 2 === 1 ? 'lg:mt-20' : ''} h-[420px] animate-pulse rounded-3xl bg-white/[0.06] border border-white/[0.08]`}
              />
            ))}
          </div>
        </div>
      </section>
    );
  }

  if (!items.length && !loading) {
    return (
      <section id="properties" className="relative -mt-8 lg:-mt-12 pt-10 pb-24 lg:pt-16 lg:pb-36">
        <div className="mx-auto max-w-[1240px] px-4 sm:px-6 lg:px-8">
          <Reveal className="grid grid-cols-12 gap-x-6 items-end">
            <div className="col-span-12 lg:col-span-7">
              <p className="landing-eyebrow text-[#8B5CF6] dark:text-[#C9BEFB]">Selected homes</p>
              <h2 className="font-display mt-6 text-4xl font-light leading-[1.08] text-[#0D0B26] dark:text-[#F6F2EC] sm:text-5xl lg:text-[64px]">
                Homes worth writing
                <br />
                home <em className="italic text-[#C8A868]">about</em>.
              </h2>
            </div>
            <div className="col-span-12 mt-8 lg:col-span-5 lg:mt-0">
              <p className="text-lg leading-relaxed text-[#5A5370] dark:text-[#B5ABC9]">
                A living selection from our network — each stay backed by secure payments,
                each host identity-verified.
              </p>
              <button
                type="button"
                onClick={() => navigate('/properties')}
                className="group mt-6 inline-flex items-center gap-3 text-[15px] font-semibold text-[#0D0B26] dark:text-[#F6F2EC]"
              >
                View all properties
                <span className="grid h-9 w-9 place-items-center rounded-full border border-current transition-transform duration-300 group-hover:translate-x-1.5">
                  <svg viewBox="0 0 16 16" width="14" height="14" aria-hidden="true" fill="none">
                    <path d="M2 8h11M9 4l4 4-4 4" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" />
                  </svg>
                </span>
              </button>
            </div>
          </Reveal>

          <div className="mt-16 grid grid-cols-1 sm:grid-cols-12 gap-6 lg:gap-8">
            {FALLBACK_IMAGES.map((img, i) => (
              <div
                key={i}
                className={`group relative overflow-hidden rounded-3xl bg-white/[0.07] border border-white/[0.1] shadow-[0_8px_32px_-8px_rgba(0,0,0,0.35)] ${
                  i % 2 === 1
                    ? 'sm:col-span-5 lg:col-span-5'
                    : 'sm:col-span-7 lg:col-span-7'
                }`}
              >
                <div className="relative w-full h-[280px] sm:h-[360px] lg:h-[560px] overflow-hidden">
                  <img
                    src={img}
                    alt="Featured property"
                    loading="lazy"
                    decoding="async"
                    className="h-full w-full object-cover transition-transform duration-700 ease-out group-hover:scale-[1.05]"
                  />
                  <div className="absolute inset-0 bg-gradient-to-t from-black/70 via-black/10 to-transparent" />
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>
    );
  }

  const cards = items.map((property, i) => {
    const isPortrait = i % 2 === 1;
    const image = firstImage(property, FALLBACK_IMAGES[i % FALLBACK_IMAGES.length]);
    const price = property.pricePerNight ?? 0;

    return (
      <Reveal
        as="article"
        key={property.id}
        delay={(i % 2) * 120}
        className={`group relative cursor-pointer overflow-hidden rounded-3xl bg-white/[0.07] border border-white/[0.1] shadow-[0_8px_32px_-8px_rgba(0,0,0,0.35)] ${
          isPortrait
            ? 'col-span-12 lg:col-span-5'
            : 'col-span-12 lg:col-span-7'
        }`}
      >
        <button
          type="button"
          onClick={() => navigate(`/properties/${property.id}`)}
          aria-label={`View ${property.title}`}
          className="block h-full w-full text-left"
        >
          <div className="relative w-full h-[280px] sm:h-[360px] lg:h-[560px] overflow-hidden">
            <img
              src={image}
              alt={property.title}
              loading="lazy"
              decoding="async"
              className="h-full w-full object-cover transition-transform duration-700 ease-out group-hover:scale-[1.05]"
            />
            <div className="absolute inset-0 bg-gradient-to-t from-black/70 via-black/10 to-transparent" />

            <span className="absolute left-5 top-5 inline-flex items-center gap-1.5 rounded-full bg-black/35 px-3 py-1.5 text-xs font-semibold text-[#F6F2EC] backdrop-blur-md">
              <Star className="h-3.5 w-3.5 fill-champagne text-champagne" />
              {property.averageRating ? property.averageRating.toFixed(1) : 'New'}
            </span>

            <span className="absolute right-5 top-5 rounded-full bg-white/10 px-3 py-1.5 text-[11px] font-medium uppercase tracking-[0.18em] text-[#F6F2EC] backdrop-blur-md">
              {property.type}
            </span>

            <div className="absolute inset-x-0 bottom-0 p-7">
              <p className="text-[11px] font-semibold uppercase tracking-[0.26em] text-[#CBBEFB]">
                {property.address.city}
              </p>
              <h3 className="font-display mt-2 max-w-[34ch] text-2xl font-light leading-snug text-[#F6F2EC] sm:text-[28px]">
                {property.title}
              </h3>
              <div className="mt-4 flex items-end justify-between gap-4 border-t border-white/15 pt-4">
                <p className="text-[#F6F2EC]">
                  <span className="font-display text-2xl font-light text-champagne">
                    {formatMad(price)}
                  </span>
                  <span className="ml-2 text-sm text-[#B5ABC9]">
                    / night
                  </span>
                </p>
                <div className="flex items-center gap-4 text-sm text-[#B5ABC9]">
                  <span className="inline-flex items-center gap-1.5">
                    <BedDouble className="h-4 w-4" />
                    {property.bedrooms}
                  </span>
                  <span className="inline-flex items-center gap-1.5">
                    <Users className="h-4 w-4" />
                    {property.maxGuests}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </button>
      </Reveal>
    );
  });

  return (
      <section id="properties" className="relative -mt-8 lg:-mt-12 pt-10 pb-24 lg:pt-16 lg:pb-36">
      <div className="mx-auto max-w-[1240px] px-4 sm:px-6 lg:px-8">
        <Reveal className="grid grid-cols-12 gap-x-6 items-end">
          <div className="col-span-12 lg:col-span-7">
            <p className="landing-eyebrow text-[#8B5CF6] dark:text-[#C9BEFB]">Selected homes</p>
            <h2 className="font-display mt-6 text-4xl font-light leading-[1.08] text-[#0D0B26] dark:text-[#F6F2EC] sm:text-5xl lg:text-[64px]">
              Homes worth writing
              <br />
              home <em className="italic text-[#C8A868]">about</em>.
            </h2>
          </div>
          <div className="col-span-12 mt-8 lg:col-span-5 lg:mt-0">
            <p className="text-lg leading-relaxed text-[#5A5370] dark:text-[#B5ABC9]">
              A living selection from our network — each stay backed by secure payments,
              each host identity-verified.
            </p>
            <button
              type="button"
              onClick={() => navigate('/properties')}
              className="group mt-6 inline-flex items-center gap-3 text-[15px] font-semibold text-[#0D0B26] dark:text-[#F6F2EC]"
            >
              View all properties
              <span className="grid h-9 w-9 place-items-center rounded-full border border-current transition-transform duration-300 group-hover:translate-x-1.5">
                <svg viewBox="0 0 16 16" width="14" height="14" aria-hidden="true" fill="none">
                  <path d="M2 8h11M9 4l4 4-4 4" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" />
                </svg>
              </span>
            </button>
          </div>
        </Reveal>

        <div className="mt-16 grid grid-cols-12 gap-6 lg:gap-8">{cards}</div>
      </div>
    </section>
  );
}
