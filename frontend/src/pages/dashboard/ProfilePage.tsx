import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { authStore } from "@/store/auth.store";
import { ProfilePictureUpload } from "@/components/profile/ProfilePictureUpload";
import { KYCDocumentUpload } from "@/components/profile/KYCDocumentUpload";
import { ProfileUpdateForm } from "@/components/profile/ProfileUpdateForm";
import { ChangePasswordForm } from "@/components/profile/ChangePasswordForm";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Separator } from "@/components/ui/separator";
import { Label } from "@/components/ui/label";
import { Shield, User, FileText, Home } from 'lucide-react';
import { RiskBadge } from "@/components/ai/RiskBadge";
import { useRiskScore } from "@/hooks/ai";
import { useUserRole } from "@/hooks/useUserRole";
import { OwnerOnboardingService } from "@/services/preferences.api";

const Profile = () => {
  const { user, loadUser } = authStore();
  const { isTenant } = useUserRole();
  const navigate = useNavigate();
  const [becomingOwner, setBecomingOwner] = useState(false);
  const [ownerError, setOwnerError] = useState<string | null>(null);

  const { data: riskScore, isLoading: riskLoading, isError: riskError } = useRiskScore(isTenant && !!user);

  useEffect(() => {
    loadUser();
  }, [loadUser]);

  const handleBecomeOwner = async () => {
    setBecomingOwner(true);
    setOwnerError(null);
    try {
      await OwnerOnboardingService.becomeOwner();
      // Le JWT contient les rôles au login : on le régénère pour que
      // le backend (qui lit les rôles du token) voie le nouveau rôle OWNER.
      const refreshed = await authStore.getState().refreshSession();
      await loadUser();
      // Force le rôle côté store : l'user a désormais plusieurs rôles
      if (authStore.getState().user) {
        authStore.setState({ user: { ...authStore.getState().user!, role: 'ROLE_OWNER' } });
      }
      if (!refreshed) {
        setOwnerError('Rôle mis à jour — reconnecte-toi pour activer toutes les fonctions owner.');
        return;
      }
      navigate('/owner/onboarding');
    } catch {
      setOwnerError('Failed to switch to host mode. Please try again.');
    } finally {
      setBecomingOwner(false);
    }
  };

  const handleUpdate = () => {};

  if (!user) {
    return (
      <div className="container mx-auto py-8 px-4">
        <div className="flex items-center justify-center min-h-[400px]">
          <div className="text-center">
            <h2 className="text-2xl font-semibold mb-2">Please log in</h2>
            <p className="text-muted-foreground">You need to be logged in to view your profile.</p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="container mx-auto py-8 px-4 ">
      <div className="mb-8">
        <h1 className="text-4xl font-bold mb-2">Profile Settings</h1>
        <div className="flex items-center gap-4">
          <p className="text-muted-foreground text-lg">
            Manage your account information and documents
          </p>
          {isTenant && riskScore && (
            <RiskBadge
              score={riskScore.score}
              loading={riskLoading}
              error={riskError}
            />
          )}
        </div>
      </div>

      {/* Become a host (tenants uniquement) */}
      {isTenant && (
        <Card className="mb-6 border-primary/20 bg-primary/[0.04]">
          <CardContent className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 py-5">
            <div className="flex items-center gap-3">
              <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-primary/15">
                <Home className="h-5 w-5 text-primary" />
              </div>
              <div>
                <p className="font-medium">Become a host</p>
                <p className="text-sm text-muted-foreground">
                  List your property and start earning.
                  {ownerError && <span className="ml-2 text-red-400">{ownerError}</span>}
                </p>
              </div>
            </div>
            <Button
              onClick={handleBecomeOwner}
              disabled={becomingOwner}
              className="shrink-0"
            >
              {becomingOwner ? 'Switching…' : 'Switch to hosting'}
            </Button>
          </CardContent>
        </Card>
      )}

      <Tabs defaultValue="personal" className="space-y-6">
        <TabsList className="grid w-full grid-cols-3">
          <TabsTrigger value="personal" className="flex items-center gap-2">
            <User className="h-4 w-4" />
            Personal
          </TabsTrigger>
          <TabsTrigger value="documents" className="flex items-center gap-2">
            <FileText className="h-4 w-4" />
            Documents
          </TabsTrigger>
          <TabsTrigger value="security" className="flex items-center gap-2">
            <Shield className="h-4 w-4" />
            Security
          </TabsTrigger>
        </TabsList>

        <TabsContent value="personal" className="space-y-6">
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            <div className="lg:col-span-1">
              <ProfilePictureUpload
                currentPhotoUrl={user.photoUrl}
                onUpdate={handleUpdate}
              />
            </div>

            <div className="lg:col-span-2">
              <ProfileUpdateForm
                user={user}
                onUpdate={handleUpdate}
              />
            </div>
          </div>

        </TabsContent>

        <TabsContent value="documents" className="space-y-6">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <KYCDocumentUpload
              documentType="recto"
              currentUrl={user.kycRectoUrl}
              onUpdate={handleUpdate}
            />

            <KYCDocumentUpload
              documentType="verso"
              currentUrl={user.kycVersoUrl}
              onUpdate={handleUpdate}
            />
          </div>

          <Card>
            <CardHeader>
              <CardTitle>KYC Verification Status</CardTitle>
              <CardDescription>
                Your documents will be reviewed by our team for verification
              </CardDescription>
            </CardHeader>
            <CardContent>
              <div className="flex items-center gap-4">
                {user.kycStatus === 'VERIFIED' ? (
                  <Badge variant="default">Verified</Badge>
                ) : user.kycStatus === 'REJECTED' ? (
                  <Badge variant="destructive">Rejected</Badge>
                ) : user.kycStatus === 'PENDING' || user.kycStatus === 'IN_REVIEW' ? (
                  <Badge variant="secondary">Under Review</Badge>
                ) : (
                  <Badge variant="secondary">Not Started</Badge>
                )}
                {user.kycStatus === 'REJECTED' && user.kycRejectionReason && (
                  <p className="text-sm text-red-400">Reason: {user.kycRejectionReason}</p>
                )}
                {(user.kycStatus === 'PENDING' || user.kycStatus === 'IN_REVIEW') && (
                  <p className="text-sm text-muted-foreground">
                    Your documents are being reviewed
                  </p>
                )}
                {!user.kycStatus && (
                  <p className="text-sm text-muted-foreground">
                    Upload your documents above to start verification
                  </p>
                )}
              </div>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="security" className="space-y-6">
          <ChangePasswordForm />

          <Card>
            <CardHeader>
              <CardTitle>Account Security</CardTitle>
              <CardDescription>
                Manage your account security settings
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <Label className="text-sm font-medium">Email Verification</Label>
                  <div className="flex items-center gap-2 mt-1">
                    <Badge variant="default">Verified</Badge>
                  </div>
                </div>

                <div>
                  <Label className="text-sm font-medium">Two-Factor Authentication</Label>
                  <div className="flex items-center gap-2 mt-1">
                    <Badge variant="secondary">Not Enabled</Badge>
                  </div>
                </div>
              </div>

              <Separator />

              <div className="space-y-2">
                <h4 className="font-medium">Security Recommendations</h4>
                <ul className="text-sm text-muted-foreground space-y-1">
                  <li>• Keep your password secure and unique</li>
                  <li>• Enable two-factor authentication when available</li>
                  <li>• Regularly review your account activity</li>
                  <li>• Never share your login credentials</li>
                </ul>
              </div>
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>
    </div>
  );
};

export default Profile;
