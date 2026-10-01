import React, { useState, useEffect } from 'react';
import { AdminStatsService } from '@/services/adminStats.service';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';
import {
  BarChart3,
  XCircle,
  Users,
  UserCheck,
  Calendar,
  Star,
  Shield,
  Building,
  CheckCircle,
  Clock,
  AlertTriangle,
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import type { AdminDashboardStats } from '@/services/adminStats.service';

export const AdminDashboardPage: React.FC = () => {
  const navigate = useNavigate();
  const [stats, setStats] = useState<AdminDashboardStats | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchStats();
  }, []);

  const fetchStats = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await AdminStatsService.getDashboardStats();
      setStats(data);
    } catch (err) {
      console.error('Failed to fetch stats:', err);
      setError('Failed to load dashboard statistics');
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-[#0D0B26]">
        <div className="mx-auto max-w-[1240px] px-4 sm:px-6 lg:px-8 py-10">
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
            {Array.from({ length: 12 }).map((_, i) => (
              <Skeleton key={i} className="h-32 w-full rounded-2xl bg-white/[0.06] border border-white/[0.08]" />
            ))}
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[#0D0B26]">
      <div className="mx-auto max-w-[1240px] px-4 sm:px-6 lg:px-8 py-10">
        {/* Header */}
        <div className="mb-10">
          <p className="text-xs font-semibold uppercase tracking-[0.2em] text-[#C9BEFB] mb-3">Administration</p>
          <h1 className="font-display text-4xl font-light leading-[1.08] text-[#F6F2EC] sm:text-5xl">
            Admin Dashboard
          </h1>
          <p className="mt-3 text-base text-[#B5ABC9]">
            Overview of platform statistics and activity
          </p>
        </div>

        {/* Error state */}
        {error && (
          <div className="mb-8 flex items-start gap-4 rounded-2xl border border-red-500/20 bg-red-500/10 px-6 py-5">
            <AlertTriangle className="h-5 w-5 text-red-400 mt-0.5 shrink-0" />
            <div className="flex-1">
              <p className="text-sm font-medium text-red-300">{error}</p>
              <Button
                onClick={fetchStats}
                variant="outline"
                size="sm"
                className="mt-3 border-white/10 bg-white/5 text-[#D5CDE8] hover:bg-white/10 hover:text-[#F6F2EC]"
              >
                Retry
              </Button>
            </div>
          </div>
        )}

        {stats && (
          <>
            {/* Platform Overview */}
            <section className="mb-10">
              <h2 className="text-xs font-semibold uppercase tracking-[0.2em] text-[#C9BEFB] mb-5">Platform Overview</h2>
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
                <StatCard
                  icon={<Users className="w-5 h-5 text-blue-400" />}
                  label="Total Users"
                  value={stats.totalUsers}
                  sub={`${stats.activeUsers} active`}
                />
                <StatCard
                  icon={<Building className="w-5 h-5 text-green-400" />}
                  label="Total Properties"
                  value={stats.totalProperties}
                  sub={`${stats.activeProperties} active`}
                />
                <StatCard
                  icon={<Calendar className="w-5 h-5 text-purple-400" />}
                  label="Total Bookings"
                  value={stats.totalBookings}
                  sub={`${stats.activeBookings} active`}
                />
                <StatCard
                  icon={<Star className="w-5 h-5 text-yellow-400" />}
                  label="Average Rating"
                  value={stats.averageRating > 0 ? stats.averageRating.toFixed(1) : 'N/A'}
                  sub={`${stats.totalReviews} reviews`}
                />
              </div>
            </section>

            {/* User Management Stats */}
            <section className="mb-10">
              <h2 className="text-xs font-semibold uppercase tracking-[0.2em] text-[#C9BEFB] mb-5">User Management</h2>
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
                <StatCard
                  icon={<UserCheck className="w-5 h-5 text-green-400" />}
                  label="Active Users"
                  value={stats.activeUsers}
                  sub={`${((stats.activeUsers / stats.totalUsers) * 100).toFixed(1)}% of total`}
                />
                <StatCard
                  icon={<Users className="w-5 h-5 text-blue-400" />}
                  label="Tenants"
                  value={stats.tenantUsers}
                  sub={`${((stats.tenantUsers / stats.totalUsers) * 100).toFixed(1)}% of users`}
                />
                <StatCard
                  icon={<Building className="w-5 h-5 text-orange-400" />}
                  label="Property Owners"
                  value={stats.ownerUsers}
                  sub={`${((stats.ownerUsers / stats.totalUsers) * 100).toFixed(1)}% of users`}
                />
                <StatCard
                  icon={<Shield className="w-5 h-5 text-red-400" />}
                  label="Administrators"
                  value={stats.adminUsers}
                  sub={`${((stats.adminUsers / stats.totalUsers) * 100).toFixed(1)}% of users`}
                />
              </div>
            </section>

            {/* Property Management Stats */}
            <section className="mb-10">
              <h2 className="text-xs font-semibold uppercase tracking-[0.2em] text-[#C9BEFB] mb-5">Property Management</h2>
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
                <StatCard
                  icon={<CheckCircle className="w-5 h-5 text-green-400" />}
                  label="Active Properties"
                  value={stats.activeProperties}
                  sub={stats.totalProperties > 0 ? `${((stats.activeProperties / stats.totalProperties) * 100).toFixed(1)}% approved` : '0% approved'}
                />
                <StatCard
                  icon={<Clock className="w-5 h-5 text-orange-400" />}
                  label="Pending Approvals"
                  value={stats.pendingProperties}
                  sub="Awaiting review"
                  badge={stats.pendingProperties > 0 ? stats.pendingProperties : undefined}
                />
                <StatCard
                  icon={<XCircle className="w-5 h-5 text-red-400" />}
                  label="Rejected Properties"
                  value={stats.rejectedProperties}
                  sub="Need revision"
                />
                <StatCard
                  icon={<BarChart3 className="w-5 h-5 text-blue-400" />}
                  label="Total Listed"
                  value={stats.totalProperties}
                  sub="All property listings"
                />
              </div>
            </section>

            {/* Quick Actions */}
            <Card className="border-white/[0.08] bg-white/[0.04] backdrop-blur-xl">
              <CardHeader>
                <CardTitle className="text-lg font-normal text-[#F6F2EC]">Quick Actions</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                  {/* User Management */}
                  <div className="space-y-3">
                    <h4 className="text-xs font-semibold uppercase tracking-[0.2em] text-[#C9BEFB]">User Management</h4>
                    <div className="flex flex-col gap-2">
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => navigate('/admin/users')}
                        className="justify-start border-white/10 bg-white/5 text-[#D5CDE8] hover:bg-white/10 hover:text-[#F6F2EC]"
                      >
                        View All Users
                      </Button>
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => navigate('/admin/users/create')}
                        className="justify-start border-white/10 bg-white/5 text-[#D5CDE8] hover:bg-white/10 hover:text-[#F6F2EC]"
                      >
                        Create New User
                      </Button>
                    </div>
                  </div>

                  {/* Property Management */}
                  <div className="space-y-3">
                    <h4 className="text-xs font-semibold uppercase tracking-[0.2em] text-[#C9BEFB]">Property Management</h4>
                    <div className="flex flex-col gap-2">
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => navigate('/admin/properties')}
                        className="justify-start border-white/10 bg-white/5 text-[#D5CDE8] hover:bg-white/10 hover:text-[#F6F2EC]"
                      >
                        View All Properties
                      </Button>
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => navigate('/admin/properties?page=0&status=PENDING_ADMIN')}
                        disabled={stats.pendingProperties === 0}
                        className="justify-start border-white/10 bg-white/5 text-[#D5CDE8] hover:bg-white/10 hover:text-[#F6F2EC] disabled:opacity-40"
                      >
                        Review Pending ({stats.pendingProperties})
                      </Button>
                    </div>
                  </div>

                  {/* Content Management */}
                  <div className="space-y-3">
                    <h4 className="text-xs font-semibold uppercase tracking-[0.2em] text-[#C9BEFB]">Content Management</h4>
                    <div className="flex flex-col gap-2">
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => navigate('/admin/reviews')}
                        className="justify-start border-white/10 bg-white/5 text-[#D5CDE8] hover:bg-white/10 hover:text-[#F6F2EC]"
                      >
                        Manage Reviews
                      </Button>
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => navigate('/admin/bookings')}
                        className="justify-start border-white/10 bg-white/5 text-[#D5CDE8] hover:bg-white/10 hover:text-[#F6F2EC]"
                      >
                        View Bookings
                      </Button>
                    </div>
                  </div>
                </div>
              </CardContent>
            </Card>
          </>
        )}
      </div>
    </div>
  );
};

function StatCard({ icon, label, value, sub, badge }: {
  icon: React.ReactNode;
  label: string;
  value: string | number;
  sub: string;
  badge?: number;
}) {
  return (
    <Card className="border-white/[0.08] bg-white/[0.04] hover:bg-white/[0.06] transition-colors">
      <CardContent className="p-5">
        <div className="flex items-center justify-between mb-3">
          {icon}
          {badge !== undefined && (
            <span className="bg-orange-500/20 text-orange-300 text-xs font-bold px-2 py-0.5 rounded-full">
              {badge}
            </span>
          )}
        </div>
        <p className="text-sm text-[#B5ABC9] mb-1">{label}</p>
        <p className="text-2xl font-display font-light text-[#F6F2EC]">{value}</p>
        <p className="text-xs text-[#8D94A8] mt-1">{sub}</p>
      </CardContent>
    </Card>
  );
}
