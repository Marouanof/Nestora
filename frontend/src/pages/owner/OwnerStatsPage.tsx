// src/pages/owner/OwnerStatsPage.tsx

import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Skeleton } from '@/components/ui/skeleton';
import { TrendingUp, DollarSign, Calendar, XCircle, BarChart3, Lightbulb } from 'lucide-react';
import { useOwnerStats } from '@/hooks/bookings/useOwnerStats';
import { formatMad } from '@/lib/utils';

export const OwnerStatsPage = () => {
  const { stats, loading, error } = useOwnerStats();

  if (loading) {
    return (
      <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
        <Skeleton className="mb-2 h-5 w-40 rounded-md" />
        <Skeleton className="mb-8 h-10 w-72 rounded-md" />
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-4">
          {Array.from({ length: 4 }).map((_, i) => (
            <Skeleton key={i} className="h-32 w-full rounded-2xl" />
          ))}
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="container mx-auto p-6">
        <p className="text-destructive">{error}</p>
      </div>
    );
  }

  if (!stats) {
    return (
      <div className="container mx-auto p-6">
        <p className="text-muted-foreground">No stats available</p>
      </div>
    );
  }

  // Backend sends totalRevenue/cancelledBookings/confirmedBookings (new fields),
  // legacy revenue/cancellations/occupancyRate kept as fallback
  const revenue = stats.totalRevenue ?? stats.revenue ?? 0;
  const cancellations = stats.cancelledBookings ?? stats.cancellations ?? 0;
  const occupancyRate =
    stats.occupancyRate ??
    (stats.totalBookings > 0
      ? Math.round(((stats.confirmedBookings ?? 0) / stats.totalBookings) * 100)
      : 0);

  const metrics = [
    {
      icon: <Calendar className="h-5 w-5 text-info" />,
      label: 'Total Bookings',
      value: stats.totalBookings.toString(),
    },
    {
      icon: <DollarSign className="h-5 w-5 text-success" />,
      label: 'Revenue',
      value: formatMad(revenue),
    },
    {
      icon: <TrendingUp className="h-5 w-5 text-primary" />,
      label: 'Occupancy Rate',
      value: `${occupancyRate}%`,
    },
    {
      icon: <XCircle className="h-5 w-5 text-error" />,
      label: 'Cancellations',
      value: cancellations.toString(),
    },
  ];

  return (
    <div className="min-h-screen bg-background">
      {/* Page header */}
      <header className="border-b border-border/60 bg-gradient-to-b from-primary/[0.04] to-transparent">
        <div className="mx-auto max-w-7xl px-4 pb-8 pt-12 sm:px-6 lg:px-8">
          <p className="landing-eyebrow mb-4 text-primary/70">Nestora · Host</p>
          <h1 className="section-heading text-4xl leading-[1.08] tracking-[-0.02em] text-foreground sm:text-5xl">
            Booking <em className="italic text-champagne-deep dark:text-champagne">statistics</em>
          </h1>
          <p className="mt-3 max-w-xl text-base text-muted-foreground">
            Overview of your property bookings and performance.
          </p>
        </div>
      </header>

      <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
        {/* Key Metrics */}
        <div className="mb-10 grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-4">
          {metrics.map((metric) => (
            <Card
              key={metric.label}
              className="rounded-2xl border-border/60 shadow-sm transition-shadow hover:shadow-md"
            >
              <CardContent className="flex items-center justify-between p-6">
                <div>
                  <p className="text-sm font-medium text-muted-foreground">{metric.label}</p>
                  <p className="font-display mt-1.5 text-3xl font-semibold tracking-tight text-foreground">
                    {metric.value}
                  </p>
                </div>
                <div className="flex h-11 w-11 items-center justify-center rounded-full bg-muted">
                  {metric.icon}
                </div>
              </CardContent>
            </Card>
          ))}
        </div>

        {/* Additional Stats */}
        <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
          <Card className="rounded-2xl border-border/60 shadow-sm">
            <CardHeader>
              <CardTitle className="flex items-center gap-2 text-lg">
                <BarChart3 className="h-5 w-5 text-primary" />
                Performance Overview
              </CardTitle>
            </CardHeader>
            <CardContent>
              <dl className="divide-y divide-border/60">
                {[
                  {
                    label: 'Average Booking Value',
                    value: formatMad(stats.totalBookings > 0 ? revenue / stats.totalBookings : 0),
                  },
                  {
                    label: 'Cancellation Rate',
                    value: `${
                      stats.totalBookings > 0 ? ((cancellations / stats.totalBookings) * 100).toFixed(1) : '0.0'
                    }%`,
                  },
                  {
                    label: 'Revenue per Booking',
                    value: formatMad(stats.totalBookings > 0 ? revenue / stats.totalBookings : 0),
                  },
                ].map((row) => (
                  <div key={row.label} className="flex items-center justify-between py-3.5">
                    <dt className="text-sm text-muted-foreground">{row.label}</dt>
                    <dd className="text-sm font-semibold text-foreground">{row.value}</dd>
                  </div>
                ))}
              </dl>
            </CardContent>
          </Card>

          <Card className="rounded-2xl border-border/60 shadow-sm">
            <CardHeader>
              <CardTitle className="flex items-center gap-2 text-lg">
                <Lightbulb className="h-5 w-5 text-warning" />
                Quick Insights
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-4">
                {occupancyRate >= 80 && (
                  <div className="flex items-center gap-3">
                    <Badge className="rounded-full bg-success-light text-success hover:bg-success-light">
                      High Occupancy
                    </Badge>
                    <span className="text-sm text-muted-foreground">Your properties are performing well!</span>
                  </div>
                )}
                {cancellations > stats.totalBookings * 0.1 && (
                  <div className="flex items-center gap-3">
                    <Badge className="rounded-full bg-warning-light text-warning hover:bg-warning-light">
                      High Cancellations
                    </Badge>
                    <span className="text-sm text-muted-foreground">
                      Consider reviewing your cancellation policy
                    </span>
                  </div>
                )}
                {revenue > 10000 && (
                  <div className="flex items-center gap-3">
                    <Badge className="rounded-full bg-primary-light text-primary hover:bg-primary-light">
                      Top Earner
                    </Badge>
                    <span className="text-sm text-muted-foreground">Great revenue performance!</span>
                  </div>
                )}
                {stats.totalBookings === 0 && (
                  <p className="py-6 text-center text-sm text-muted-foreground">
                    No bookings yet. Start by listing your properties!
                  </p>
                )}
              </div>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
};
