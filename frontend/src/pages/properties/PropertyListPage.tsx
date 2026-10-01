import React, { useState } from 'react';
import { usePropertyList } from '@/hooks/useProperty';
import { PropertyService } from '@/services/property.service';
import { PropertyCard } from '@/components/properties/PropertyCard';
import { PropertyFilters } from '@/components/properties/PropertyFilters';
import { Skeleton } from '@/components/ui/skeleton';
import { Button } from '@/components/ui/button';
import { ChevronLeft, ChevronRight, SearchX, AlertTriangle, SlidersHorizontal } from 'lucide-react';
import type { PropertySummary, PropertySearchParams, PaginatedResponse } from '@/types/property.types';

export const PropertyListPage: React.FC = () => {
  const [page, setPage] = useState(0);
  const [searchFilters, setSearchFilters] = useState<PropertySearchParams | null>(null);
  const [searchResults, setSearchResults] = useState<PaginatedResponse<PropertySummary> | null>(null);
  const [searchLoading, setSearchLoading] = useState(false);
  const [searchError, setSearchError] = useState<string | null>(null);

  const { data: defaultData, loading: defaultLoading, error: defaultError, refetch } = usePropertyList(page, 12);

  // Use search results if available, otherwise use default data
  const data = searchResults || defaultData;
  const loading = searchLoading || defaultLoading;
  const error = searchError || defaultError;

  const handleSearch = async (filters: PropertySearchParams) => {
    setPage(0);
    setSearchLoading(true);
    setSearchError(null);
    try {
      const result = await PropertyService.search(filters, 0, 12);
      setSearchResults(result);
      setSearchFilters(filters);
    } catch {
      setSearchError('Failed to search properties');
      setSearchResults(null);
    } finally {
      setSearchLoading(false);
    }
  };

  const handleReset = () => {
    setSearchFilters(null);
    setSearchResults(null);
    setSearchError(null);
    setPage(0);
    refetch();
  };

  const handlePageChange = (newPage: number) => {
    setPage(newPage);
    if (searchFilters) {
      handleSearch(searchFilters);
    }
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  return (
    <div className="min-h-screen bg-background">
      {/* Editorial page header */}
      <header className="border-b border-border/60 bg-gradient-to-b from-primary/[0.04] to-transparent">
        <div className="mx-auto max-w-7xl px-4 pt-12 pb-8 sm:px-6 lg:px-8">
          <p className="landing-eyebrow text-primary/70 mb-4">Nestora · Stays</p>
          <h1 className="section-heading text-4xl leading-[1.08] tracking-[-0.02em] text-foreground sm:text-5xl">
            Discover <em className="italic text-champagne-deep dark:text-champagne">properties</em>
          </h1>
          <p className="mt-3 max-w-xl text-base text-muted-foreground">
            {loading
              ? 'Finding the best stays for you…'
              : searchFilters
                ? `${data?.totalElements ?? 0} properties match your search`
                : `${data?.totalElements ?? 0} homes available for your next stay`}
          </p>
        </div>
      </header>

      <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
        <PropertyFilters onSearch={handleSearch} onReset={handleReset} isLoading={loading} />

        {error && (
          <div className="flex flex-col items-center gap-4 rounded-2xl border border-destructive/20 bg-destructive/5 px-6 py-16 text-center">
            <AlertTriangle className="h-8 w-8 text-destructive" />
            <p className="text-sm font-medium text-destructive">{error}</p>
            <Button onClick={refetch}>Try Again</Button>
          </div>
        )}

        {!error && !loading && (!data || !data.content || data.content.length === 0) && (
          <div className="flex flex-col items-center gap-4 rounded-2xl border border-border/60 bg-card px-6 py-20 text-center">
            <div className="flex h-14 w-14 items-center justify-center rounded-full bg-muted">
              <SearchX className="h-6 w-6 text-muted-foreground" />
            </div>
            <div>
              <h2 className="section-heading text-2xl text-foreground">No properties found</h2>
              <p className="mt-1.5 text-sm text-muted-foreground">
                {searchFilters
                  ? 'Nothing matches your filters yet — try widening your search.'
                  : 'No properties are available right now. Check back soon.'}
              </p>
            </div>
            {searchFilters && (
              <Button onClick={handleReset} variant="outline">
                <SlidersHorizontal className="mr-2 h-4 w-4" />
                Clear Filters
              </Button>
            )}
          </div>
        )}

        {!error && loading && (
          <div className="grid grid-cols-1 gap-x-6 gap-y-10 sm:grid-cols-2 lg:grid-cols-3">
            {Array.from({ length: 6 }).map((_, index) => (
              <div key={index}>
                <Skeleton className="mb-4 aspect-[4/3] w-full rounded-2xl" />
                <Skeleton className="mb-2 h-5 w-3/4 rounded-md" />
                <Skeleton className="mb-4 h-4 w-1/2 rounded-md" />
                <Skeleton className="h-5 w-1/3 rounded-md" />
              </div>
            ))}
          </div>
        )}

        {!error && !loading && data?.content && data.content.length > 0 && (
          <>
            <div className="grid grid-cols-1 gap-x-6 gap-y-10 sm:grid-cols-2 lg:grid-cols-3">
              {data.content.map((property) => (
                <PropertyCard key={property.id} property={property} />
              ))}
            </div>

            {/* Pagination */}
            {data.totalPages && data.totalPages > 1 && (
              <nav aria-label="Pagination" className="mt-14 flex items-center justify-center gap-3">
                <Button
                  variant="outline"
                  size="icon"
                  onClick={() => handlePageChange(page - 1)}
                  disabled={data.first || loading}
                  aria-label="Previous page"
                >
                  <ChevronLeft className="h-4 w-4" />
                </Button>

                <div className="flex items-center gap-1.5">
                  {Array.from({ length: Math.min(5, data.totalPages || 1) }).map((_, i) => {
                    const pageNum = Math.max(0, (data.number || 0) - 2 + i);
                    if (pageNum >= (data.totalPages || 0)) return null;
                    return (
                      <Button
                        key={pageNum}
                        variant={pageNum === page ? 'default' : 'ghost'}
                        size="icon"
                        onClick={() => handlePageChange(pageNum)}
                        disabled={loading}
                        aria-current={pageNum === page ? 'page' : undefined}
                      >
                        {pageNum + 1}
                      </Button>
                    );
                  })}
                </div>

                <Button
                  variant="outline"
                  size="icon"
                  onClick={() => handlePageChange(page + 1)}
                  disabled={data.last || loading}
                  aria-label="Next page"
                >
                  <ChevronRight className="h-4 w-4" />
                </Button>

                <span className="ml-2 hidden text-sm text-muted-foreground sm:inline">
                  Page {(data.number || 0) + 1} of {data.totalPages || 1}
                </span>
              </nav>
            )}
          </>
        )}
      </div>
    </div>
  );
};
