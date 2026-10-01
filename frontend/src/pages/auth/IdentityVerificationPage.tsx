import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Check,
  ChevronLeft,
  CreditCard,
  FileCheck,
  IdCard,
  Plane,
  ScanFace,
  ShieldAlert,
  ShieldX,
  Upload,
} from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Label } from '@/components/ui/label';
import { api } from '@/lib/axios';
import { authStore } from '@/store/auth.store';

type KycStatus =
  | 'NOT_STARTED'
  | 'PENDING'
  | 'IN_REVIEW'
  | 'VERIFIED'
  | 'REJECTED'
  | 'EXPIRED';

const DOCUMENT_TYPES = [
  { value: 'NATIONAL_ID', label: 'CIN / National ID', icon: IdCard },
  { value: 'PASSPORT', label: 'Passport', icon: Plane },
  { value: 'DRIVING_LICENSE', label: 'Driving License', icon: CreditCard },
];

type UploadStep = {
  side: 'kyc-recto' | 'kyc-verso' | 'kyc-selfie';
  title: string;
  description: string;
};

const UPLOAD_STEPS: UploadStep[] = [
  {
    side: 'kyc-recto',
    title: 'Document front',
    description: 'Upload the front side of your document',
  },
  {
    side: 'kyc-verso',
    title: 'Document back',
    description: 'Upload the back side of your document',
  },
  {
    side: 'kyc-selfie',
    title: 'Selfie verification',
    description: 'Take a clear selfie holding your document',
  },
];

function IdentityVerificationPage() {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [step, setStep] = useState(0); // 0 = doc type, 1..3 = uploads, 4 = done
  const [documentType, setDocumentType] = useState('');
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const kycStatus = authStore((s) => s.user?.kycStatus);
  const rejectionReason = authStore((s) => s.user?.kycRejectionReason);

  useEffect(() => {
    authStore
      .getState()
      .loadUser()
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  // Si déjà soumis/vérifié/rejeté, on affiche directement le bon écran
  const effectiveStatus: KycStatus | undefined = step >= 1 ? 'NOT_STARTED' : kycStatus;

  const handleSaveDocumentType = async () => {
    if (!documentType) return;
    setUploading(true);
    setError(null);
    try {
      await api.put('/users/me/kyc/document-type', null, {
        params: { documentType },
      });
      setStep(1);
    } catch {
      setError('Failed to save document type. Please try again.');
    } finally {
      setUploading(false);
    }
  };

  const handleUpload = async (side: string, file: File) => {
    setUploading(true);
    setError(null);
    try {
      const formData = new FormData();
      formData.append('file', file);
      await api.post(`/users/me/${side}`, formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });
      await authStore.getState().loadUser();
      setStep((s) => s + 1);
    } catch {
      setError('Upload failed. Please try again.');
    } finally {
      setUploading(false);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center" style={{ background: '#0D0B26', minHeight: 'calc(100vh - 4rem)' }}>
        <p className="text-sm text-[#8D94A8]">Loading…</p>
      </div>
    );
  }

  // ── Écran VERIFIED ────────────────────────────────────────────
  if (effectiveStatus === 'VERIFIED') {
    return (
      <VerificationShell>
        <div className="mx-auto mb-5 flex h-16 w-16 items-center justify-center rounded-full bg-emerald-500/15">
          <ShieldAlert className="h-8 w-8 text-emerald-400" />
        </div>
        <h1 className="font-display text-2xl font-normal text-[#F6F2EC]">KYC Verified ✓</h1>
        <p className="mt-2 text-sm text-[#B5ABC9]">
          Your identity has been verified. You can now publish properties on Nestora.
        </p>
        <Button
          onClick={() => navigate('/owner/properties')}
          className="mt-6 w-full h-12 rounded-xl bg-primary text-primary-foreground font-semibold text-[15px] hover:bg-primary-hover"
        >
          Go to Owner Dashboard
        </Button>
      </VerificationShell>
    );
  }

  // ── Écran REJECTED ────────────────────────────────────────────
  if (effectiveStatus === 'REJECTED') {
    return (
      <VerificationShell>
        <div className="mx-auto mb-5 flex h-16 w-16 items-center justify-center rounded-full bg-red-500/15">
          <ShieldX className="h-8 w-8 text-red-400" />
        </div>
        <h1 className="font-display text-2xl font-normal text-[#F6F2EC]">Verification failed</h1>
        <p className="mt-2 text-sm text-[#B5ABC9]">
          Unfortunately, your documents were not approved.
        </p>
        {rejectionReason && (
          <div className="mt-4 rounded-xl border border-red-500/20 bg-red-500/10 px-4 py-3 text-left">
            <p className="text-xs font-semibold uppercase tracking-wide text-red-300">Reason</p>
            <p className="mt-1 text-sm text-red-200">{rejectionReason}</p>
          </div>
        )}
        <Button
          onClick={() => {
            setStep(0);
            setDocumentType('');
          }}
          className="mt-6 w-full h-12 rounded-xl bg-primary text-primary-foreground font-semibold text-[15px] hover:bg-primary-hover"
        >
          Resubmit documents
        </Button>
      </VerificationShell>
    );
  }

  // ── Écran PENDING / IN_REVIEW / EXPIRED ───────────────────────
  if (effectiveStatus === 'PENDING' || effectiveStatus === 'IN_REVIEW') {
    return (
      <VerificationShell>
        <div className="mx-auto mb-5 flex h-16 w-16 items-center justify-center rounded-full bg-primary/15">
          <FileCheck className="h-8 w-8 text-primary" />
        </div>
        <h1 className="font-display text-2xl font-normal text-[#F6F2EC]">Documents submitted</h1>
        <p className="mt-2 text-sm text-[#B5ABC9]">
          Your documents are under review. We'll notify you once the verification is complete —
          usually within 24–48 hours.
        </p>
        <Button
          variant="outline"
          onClick={() => navigate('/')}
          className="mt-6 w-full h-11 rounded-xl border-white/10 bg-white/[0.04] text-[#D5CDE8] hover:bg-white/10 hover:text-[#F6F2EC]"
        >
          Back to home
        </Button>
      </VerificationShell>
    );
  }

  const currentUpload = step >= 1 && step <= 3 ? UPLOAD_STEPS[step - 1] : null;

  return (
    <div
      className="relative flex items-center justify-center overflow-hidden"
      style={{ background: '#0D0B26', minHeight: 'calc(100vh - 4rem)' }}
    >
      <div
        aria-hidden="true"
        className="absolute inset-0 bg-[radial-gradient(ellipse_at_30%_50%,rgba(81,70,229,0.12)_0%,transparent_60%)]"
      />

      <div className="relative z-10 w-full max-w-md mx-auto px-4 py-12">
        <div className="rounded-2xl border border-white/[0.08] bg-white/[0.04] backdrop-blur-xl p-6 sm:p-8 shadow-[0_32px_64px_-48px_rgba(0,0,0,0.5)]">
          {/* Header */}
          <div className="mb-6">
            {step > 0 && (
              <button
                type="button"
                onClick={() => setStep(step - 1)}
                disabled={uploading}
                className="mb-4 flex h-9 w-9 items-center justify-center rounded-full border border-white/10 bg-white/5 text-[#B5ABC9] transition-colors hover:bg-white/10 hover:text-[#F6F2EC]"
              >
                <ChevronLeft className="h-4 w-4" />
              </button>
            )}
            <h1 className="font-display text-2xl font-normal text-[#F6F2EC]">
              {step === 0 && 'Identity Verification'}
              {currentUpload && currentUpload.title}
              {step === 4 && 'All set!'}
            </h1>
            <p className="mt-1.5 text-sm text-[#8D94A8]">
              {step === 0 && 'Choose the document you want to verify your identity with.'}
              {currentUpload && currentUpload.description}
              {step === 4 &&
                'Your documents have been submitted and are now under review.'}
            </p>

            <div className="mt-5 flex gap-2">
              {[0, 1, 2, 3].map((s) => (
                <div
                  key={s}
                  className={`h-1 flex-1 rounded-full transition-colors ${
                    s <= step ? 'bg-primary' : 'bg-white/10'
                  }`}
                />
              ))}
            </div>
          </div>

          {error && <p className="mb-4 text-[13px] text-red-400">{error}</p>}

          {/* Step 0 — Document type */}
          {step === 0 && (
            <div className="space-y-6">
              <Label className="text-[13px] font-medium text-[#D5CDE8]">Document type</Label>
              <div className="space-y-3">
                {DOCUMENT_TYPES.map(({ value, label, icon: Icon }) => (
                  <button
                    key={value}
                    type="button"
                    onClick={() => setDocumentType(value)}
                    className={`flex w-full items-center gap-3 rounded-xl border px-4 py-3.5 transition-all ${
                      documentType === value
                        ? 'border-primary/60 bg-primary/10'
                        : 'border-white/[0.08] bg-white/[0.04] hover:bg-white/[0.08]'
                    }`}
                  >
                    <Icon
                      className={`h-5 w-5 ${
                        documentType === value ? 'text-primary' : 'text-[#8D94A8]'
                      }`}
                    />
                    <span
                      className={`flex-1 text-left text-sm font-medium ${
                        documentType === value ? 'text-[#F6F2EC]' : 'text-[#B5ABC9]'
                      }`}
                    >
                      {label}
                    </span>
                    {documentType === value && <Check className="h-4 w-4 text-primary" />}
                  </button>
                ))}
              </div>

              <Button
                onClick={handleSaveDocumentType}
                disabled={!documentType || uploading}
                className="w-full h-12 rounded-xl bg-primary text-primary-foreground font-semibold text-[15px] hover:bg-primary-hover transition-all duration-300 hover:-translate-y-px"
              >
                {uploading ? 'Saving…' : 'Continue'}
              </Button>
            </div>
          )}

          {/* Steps 1-3 — Uploads */}
          {currentUpload && (
            <FileUploadTile
              key={currentUpload.side}
              side={currentUpload.side}
              uploading={uploading}
              onFile={(file) => handleUpload(currentUpload.side, file)}
            />
          )}

          {/* Step 4 — Done */}
          {step === 4 && (
            <div className="space-y-6 text-center">
              <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-emerald-500/15">
                <Check className="h-8 w-8 text-emerald-400" />
              </div>
              <Button
                onClick={() => navigate('/')}
                className="w-full h-12 rounded-xl bg-primary text-primary-foreground font-semibold text-[15px] hover:bg-primary-hover"
              >
                Back to home
              </Button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

function VerificationShell({ children }: { children: React.ReactNode }) {
  return (
    <div
      className="relative flex items-center justify-center overflow-hidden"
      style={{ background: '#0D0B26', minHeight: 'calc(100vh - 4rem)' }}
    >
      <div
        aria-hidden="true"
        className="absolute inset-0 bg-[radial-gradient(ellipse_at_30%_50%,rgba(81,70,229,0.12)_0%,transparent_60%)]"
      />
      <div className="relative z-10 w-full max-w-md mx-auto px-4 py-12">
        <div className="rounded-2xl border border-white/[0.08] bg-white/[0.04] backdrop-blur-xl p-6 sm:p-8 shadow-[0_32px_64px_-48px_rgba(0,0,0,0.5)] text-center">
          {children}
        </div>
      </div>
    </div>
  );
}

function FileUploadTile({
  side,
  uploading,
  onFile,
}: {
  side: string;
  uploading: boolean;
  onFile: (file: File) => void;
}) {
  const isSelfie = side === 'kyc-selfie';

  const handleFileSelect = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;
    if (!file.type.startsWith('image/') && !file.type.includes('pdf')) {
      return;
    }
    if (file.size > 10 * 1024 * 1024) {
      return;
    }
    onFile(file);
  };

  return (
    <label
      htmlFor={`upload-${side}`}
      className={`flex cursor-pointer flex-col items-center justify-center gap-3 rounded-xl border border-dashed border-white/15 bg-white/[0.03] px-6 py-12 text-center transition-colors hover:border-primary/40 hover:bg-primary/5 ${
        uploading ? 'pointer-events-none opacity-60' : ''
      }`}
    >
      {isSelfie ? (
        <ScanFace className="h-10 w-10 text-[#8D94A8]" />
      ) : (
        <Upload className="h-10 w-10 text-[#8D94A8]" />
      )}
      <span className="text-sm font-medium text-[#D5CDE8]">
        {uploading ? 'Uploading…' : 'Click to upload'}
      </span>
      <span className="text-xs text-[#8D94A8]">JPG, PNG or PDF — max 10MB</span>
      <input
        id={`upload-${side}`}
        type="file"
        accept={isSelfie ? 'image/*' : 'image/*,.pdf'}
        onChange={handleFileSelect}
        disabled={uploading}
        className="hidden"
      />
    </label>
  );
}

export default IdentityVerificationPage;
