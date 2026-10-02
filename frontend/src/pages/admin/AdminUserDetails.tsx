import React, { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { Badge } from '@/components/ui/badge';
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Skeleton } from '@/components/ui/skeleton';
import { AlertDialog, AlertDialogContent, AlertDialogHeader, AlertDialogFooter, AlertDialogTitle, AlertDialogDescription, AlertDialogCancel, AlertDialogAction } from '@/components/ui/alert-dialog';
import { ArrowLeft, Save, UserCheck, UserX, Shield, LogOut, CheckCircle2, XCircle, ShieldCheck } from 'lucide-react';
import { useUser, useUpdateUserProfile, useEnableUser, useDisableUser, useChangeRole, useForceLogout, useApproveKyc, useRejectKyc } from '@/hooks/admin/useAdminUsers';
import { getPrimaryRole, isUserActive } from '@/services/adminUser.service';

export const AdminUserDetails: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const userId = id ? parseInt(id) : null;

  const { data: user, isLoading, error } = useUser(userId!);
  const updateProfileMutation = useUpdateUserProfile();
  const enableUserMutation = useEnableUser();
  const disableUserMutation = useDisableUser();
  const changeRoleMutation = useChangeRole();
  const forceLogoutMutation = useForceLogout();
  const approveKycMutation = useApproveKyc();
  const rejectKycMutation = useRejectKyc();

  const [isEditing, setIsEditing] = useState(false);
  const [rejectDialogOpen, setRejectDialogOpen] = useState(false);
  const [rejectReason, setRejectReason] = useState('');
  const [formData, setFormData] = useState({
    firstName: '',
    lastName: '',
    email: '',
    phone: '',
    description: '',
    city: '',
    country: '',
    dateNaissance: '',
    role: 'ROLE_TENANT' as 'ROLE_TENANT' | 'ROLE_OWNER' | 'ROLE_ADMIN',
  });

  React.useEffect(() => {
    if (user) {
      setFormData({
        firstName: user.firstName || '',
        lastName: user.lastName || '',
        email: user.email || '',
        phone: user.phone || '',
        description: user.description || '',
        city: user.city || '',
        country: user.country || '',
        dateNaissance: user.dateNaissance || '',
        role: getPrimaryRole(user) ?? 'ROLE_TENANT',
      });
    }
  }, [user]);

  const handleSave = async () => {
    if (!userId) return;

    try {
      const initialRole = user ? getPrimaryRole(user) : undefined;
      await updateProfileMutation.mutateAsync({
        id: userId,
        data: {
          firstName: formData.firstName,
          lastName: formData.lastName,
          phone: formData.phone,
          description: formData.description,
          city: formData.city,
          country: formData.country,
          dateNaissance: formData.dateNaissance,
        },
      });
      // Le rôle passe par la route dédiée PUT /admin/users/:id/role (UpdateProfileRequest n'a pas de champ role)
      if (formData.role && formData.role !== initialRole) {
        await changeRoleMutation.mutateAsync({ id: userId, newRole: formData.role });
      }
      setIsEditing(false);
    } catch (error) {
      // Error is handled by the mutation
    }
  };

  const handleEnableUser = async () => {
    if (!userId) return;
    await enableUserMutation.mutateAsync(userId);
  };

  const handleDisableUser = async () => {
    if (!userId) return;
    await disableUserMutation.mutateAsync(userId);
  };

  const handleChangeRole = async (newRole: string) => {
    if (!userId) return;
    await changeRoleMutation.mutateAsync({ id: userId, newRole: newRole as any });
  };

  const handleForceLogout = async () => {
    if (!userId) return;
    await forceLogoutMutation.mutateAsync(userId);
  };

  const handleApproveKyc = async () => {
    if (!userId) return;
    await approveKycMutation.mutateAsync(userId);
  };

  const handleRejectKyc = async () => {
    if (!userId || !rejectReason.trim()) return;
    await rejectKycMutation.mutateAsync({ id: userId, reason: rejectReason.trim() });
    setRejectDialogOpen(false);
    setRejectReason('');
  };

  const getKycStatusBadgeVariant = (status?: string) => {
    switch (status) {
      case 'VERIFIED': return 'default';
      case 'PENDING':
      case 'IN_REVIEW': return 'secondary';
      case 'REJECTED': return 'destructive';
      default: return 'outline';
    }
  };

  const getKycStatusLabel = (status?: string) => {
    switch (status) {
      case 'VERIFIED': return 'Verified';
      case 'PENDING': return 'Pending';
      case 'IN_REVIEW': return 'In Review';
      case 'REJECTED': return 'Rejected';
      case 'NOT_STARTED': return 'Not Started';
      case 'EXPIRED': return 'Expired';
      default: return status ?? 'N/A';
    }
  };

  const kycStatus = user?.kycStatus;
  const kycPending = kycStatus === 'PENDING' || kycStatus === 'IN_REVIEW';
  const kycVerified = user?.kycVerified || kycStatus === 'VERIFIED';

  const getRoleBadgeVariant = (role?: string) => {
    switch (role) {
      case 'ROLE_ADMIN': return 'destructive';
      case 'ROLE_OWNER': return 'default';
      case 'ROLE_TENANT': return 'secondary';
      default: return 'outline';
    }
  };

  const getRoleDisplayName = (role?: string) => {
    switch (role) {
      case 'ROLE_ADMIN': return 'Admin';
      case 'ROLE_OWNER': return 'Owner';
      case 'ROLE_TENANT': return 'Tenant';
      default: return role ?? 'Unknown';
    }
  };

  if (isLoading) {
    return (
      <div className="container mx-auto p-4">
        <div className="mb-6">
          <Skeleton className="h-10 w-32 mb-4" />
          <Skeleton className="h-8 w-64" />
        </div>
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          <div className="lg:col-span-2">
            <Skeleton className="h-96 w-full" />
          </div>
          <div>
            <Skeleton className="h-80 w-full" />
          </div>
        </div>
      </div>
    );
  }

  if (error || !user) {
    return (
      <div className="container mx-auto p-4">
        <div className="mb-6">
          <Button variant="outline" onClick={() => navigate('/admin/users')}>
            <ArrowLeft className="w-4 h-4 mr-2" />
            Back to Users
          </Button>
        </div>
        <Card>
          <CardContent className="p-6">
            <p className="text-destructive">{error?.message || 'User not found'}</p>
            <Button onClick={() => navigate('/admin/users')} className="mt-4">Back to Users</Button>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="container mx-auto p-4">
      <div className="mb-6">
        <Button variant="outline" onClick={() => navigate('/admin/users')} className="mb-4">
          <ArrowLeft className="w-4 h-4 mr-2" />
          Back to Users
        </Button>
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold mb-2">{user.firstName} {user.lastName}</h1>
            <div className="flex items-center gap-4 text-muted-foreground">
              <Badge variant={getRoleBadgeVariant(getPrimaryRole(user))}>
                {getRoleDisplayName(getPrimaryRole(user))}
              </Badge>
              <Badge variant={isUserActive(user) ? 'default' : 'secondary'}>
                {isUserActive(user) ? 'Active' : 'Inactive'}
              </Badge>
              {user.emailVerified && (
                <Badge variant="outline" className="text-green-600">
                  ✓ Email Verified
                </Badge>
              )}
            </div>
          </div>
          <div className="flex gap-2">
            {isUserActive(user) ? (
              <Button variant="outline" onClick={handleDisableUser}>
                <UserX className="w-4 h-4 mr-2" />
                Disable
              </Button>
            ) : (
              <Button variant="outline" onClick={handleEnableUser}>
                <UserCheck className="w-4 h-4 mr-2" />
                Enable
              </Button>
            )}
            <Button variant="outline" onClick={handleForceLogout}>
              <LogOut className="w-4 h-4 mr-2" />
              Force Logout
            </Button>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Main Content */}
        <div className="lg:col-span-2 space-y-6">
          {/* Profile Information */}
          <Card>
            <CardHeader>
              <div className="flex items-center justify-between">
                <CardTitle>Profile Information</CardTitle>
                <Button
                  variant={isEditing ? "default" : "outline"}
                  size="sm"
                  onClick={() => isEditing ? handleSave() : setIsEditing(true)}
                  disabled={updateProfileMutation.isPending}
                >
                  {isEditing ? (
                    <>
                      <Save className="w-4 h-4 mr-2" />
                      Save Changes
                    </>
                  ) : (
                    'Edit Profile'
                  )}
                </Button>
              </div>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <Label htmlFor="firstName">First Name</Label>
                  <Input
                    id="firstName"
                    value={formData.firstName}
                    onChange={(e) => setFormData({ ...formData, firstName: e.target.value })}
                    disabled={!isEditing}
                  />
                </div>
                <div>
                  <Label htmlFor="lastName">Last Name</Label>
                  <Input
                    id="lastName"
                    value={formData.lastName}
                    onChange={(e) => setFormData({ ...formData, lastName: e.target.value })}
                    disabled={!isEditing}
                  />
                </div>
              </div>

              <div>
                <Label htmlFor="email">Email</Label>
                <Input
                  id="email"
                  type="email"
                  value={formData.email}
                  disabled
                  className="bg-muted"
                />
                <p className="text-xs text-muted-foreground mt-1">Email cannot be changed</p>
              </div>

              <div>
                <Label htmlFor="phone">Phone</Label>
                <Input
                  id="phone"
                  value={formData.phone}
                  onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
                  disabled={!isEditing}
                />
              </div>

              <div>
                <Label htmlFor="description">Description</Label>
                <Textarea
                  id="description"
                  value={formData.description}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                  disabled={!isEditing}
                  rows={3}
                />
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <Label htmlFor="country">Country</Label>
                  <Input
                    id="country"
                    value={formData.country}
                    onChange={(e) => setFormData({ ...formData, country: e.target.value })}
                    disabled={!isEditing}
                  />
                </div>
                <div>
                  <Label htmlFor="city">City</Label>
                  <Input
                    id="city"
                    value={formData.city}
                    onChange={(e) => setFormData({ ...formData, city: e.target.value })}
                    disabled={!isEditing}
                  />
                </div>
              </div>

              <div>
                <Label htmlFor="dateNaissance">Date of Birth</Label>
                <Input
                  id="dateNaissance"
                  type="date"
                  value={formData.dateNaissance}
                  onChange={(e) => setFormData({ ...formData, dateNaissance: e.target.value })}
                  disabled={!isEditing}
                />
              </div>

              <div>
                <Label htmlFor="role">Role</Label>
                <Select
                  value={formData.role}
                  onValueChange={(value) => setFormData({ ...formData, role: value as any })}
                  disabled={!isEditing}
                >
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="ROLE_TENANT">Tenant</SelectItem>
                    <SelectItem value="ROLE_OWNER">Owner</SelectItem>
                    <SelectItem value="ROLE_ADMIN">Admin</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </CardContent>
          </Card>

          {/* Account Information */}
          <Card>
            <CardHeader>
              <CardTitle>Account Information</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <Label>User ID</Label>
                  <p className="text-sm font-mono bg-muted p-2 rounded">{user.id}</p>
                </div>
              </div>
              <div>
                <Label>Joined</Label>
                <p className="text-sm">{new Date(user.createdAt).toLocaleDateString()}</p>
              </div>
            </CardContent>
          </Card>
        </div>

        {/* Sidebar */}
        <div className="space-y-6">
          {/* User Avatar */}
          <Card>
            <CardContent className="p-6">
              <div className="flex flex-col items-center text-center">
                <Avatar className="w-24 h-24 mb-4">
                  <AvatarImage src={user.photoUrl} alt={`${user.firstName} ${user.lastName}`} />
                  <AvatarFallback className="text-2xl">
                    {user.firstName?.[0]}{user.lastName?.[0]}
                  </AvatarFallback>
                </Avatar>
                <h3 className="text-lg font-semibold">{user.firstName} {user.lastName}</h3>
                <p className="text-muted-foreground">{user.email}</p>
                <Badge variant={getRoleBadgeVariant(getPrimaryRole(user))} className="mt-2">
                  {getRoleDisplayName(getPrimaryRole(user))}
                </Badge>
              </div>
            </CardContent>
          </Card>

          {/* KYC Verification */}
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <ShieldCheck className="w-4 h-4" />
                KYC Verification
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="flex items-center justify-between">
                <span className="text-sm text-muted-foreground">Status</span>
                <Badge variant={getKycStatusBadgeVariant(kycStatus)}>
                  {kycVerified ? '✓ ' : ''}{getKycStatusLabel(kycStatus)}
                </Badge>
              </div>

              <div className="flex items-center justify-between text-sm">
                <span className="text-muted-foreground">Documents</span>
                <span>
                  {user?.kycRectoUrl ? '✓' : '✗'} recto &nbsp;
                  {user?.kycVersoUrl ? '✓' : '✗'} verso
                </span>
              </div>

              {(user?.kycRectoUrl || user?.kycVersoUrl) && (
                <div className="grid grid-cols-2 gap-3">
                  {user?.kycRectoUrl && (
                    <a href={user.kycRectoUrl} target="_blank" rel="noreferrer" className="group block">
                      <p className="mb-1 text-xs font-medium text-muted-foreground">Recto — cliquer pour agrandir</p>
                      <img
                        src={user.kycRectoUrl}
                        alt="KYC recto"
                        loading="lazy"
                        className="aspect-[3/2] w-full rounded-lg border object-cover transition group-hover:opacity-90"
                      />
                    </a>
                  )}
                  {user?.kycVersoUrl && (
                    <a href={user.kycVersoUrl} target="_blank" rel="noreferrer" className="group block">
                      <p className="mb-1 text-xs font-medium text-muted-foreground">Verso — cliquer pour agrandir</p>
                      <img
                        src={user.kycVersoUrl}
                        alt="KYC verso"
                        loading="lazy"
                        className="aspect-[3/2] w-full rounded-lg border object-cover transition group-hover:opacity-90"
                      />
                    </a>
                  )}
                </div>
              )}

              {user?.rejectionReason && (
                <div className="rounded-md border border-destructive/30 bg-destructive/10 px-3 py-2 text-xs text-destructive">
                  <p className="font-semibold">Rejection reason</p>
                  <p className="mt-0.5">{user.rejectionReason}</p>
                </div>
              )}

              <div className="flex gap-2">
                <Button
                  variant="default"
                  className="flex-1"
                  disabled={!kycPending || approveKycMutation.isPending}
                  onClick={handleApproveKyc}
                >
                  <CheckCircle2 className="w-4 h-4 mr-2" />
                  Approve
                </Button>
                <Button
                  variant="destructive"
                  className="flex-1"
                  disabled={!kycPending || rejectKycMutation.isPending}
                  onClick={() => setRejectDialogOpen(true)}
                >
                  <XCircle className="w-4 h-4 mr-2" />
                  Reject
                </Button>
              </div>
            </CardContent>
          </Card>

          {/* Quick Actions */}
          <Card>
            <CardHeader>
              <CardTitle>Quick Actions</CardTitle>
            </CardHeader>
            <CardContent className="space-y-3">
              <Button
                variant="outline"
                className="w-full"
                onClick={() => {
                  const current = getPrimaryRole(user);
                  handleChangeRole(current === 'ROLE_ADMIN' ? 'ROLE_OWNER' : current === 'ROLE_OWNER' ? 'ROLE_TENANT' : 'ROLE_OWNER');
                }}
              >
                <Shield className="w-4 h-4 mr-2" />
                Change Role
              </Button>
              <Button
                variant="outline"
                className="w-full"
                onClick={handleForceLogout}
              >
                <LogOut className="w-4 h-4 mr-2" />
                Force Logout
              </Button>
            </CardContent>
          </Card>
        </div>
      </div>

      <AlertDialog open={rejectDialogOpen} onOpenChange={setRejectDialogOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Reject KYC verification</AlertDialogTitle>
            <AlertDialogDescription>
              Provide a reason why the KYC verification of {user.firstName} {user.lastName} is being rejected. This feedback will be shown to the user.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <Textarea
            value={rejectReason}
            onChange={(e) => setRejectReason(e.target.value)}
            placeholder="Reason for rejection (required)"
            rows={3}
          />
          <AlertDialogFooter>
            <AlertDialogCancel onClick={() => setRejectReason('')}>Cancel</AlertDialogCancel>
            <AlertDialogAction
              className="bg-destructive text-white hover:bg-destructive/90"
              disabled={!rejectReason.trim() || rejectKycMutation.isPending}
              onClick={handleRejectKyc}
            >
              {rejectKycMutation.isPending ? 'Rejecting...' : 'Reject'}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
};