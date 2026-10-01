import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { isAxiosError } from 'axios';
import { Building2, Check, ChevronLeft, ShieldCheck, User } from 'lucide-react';
import { authStore } from '@/store/auth.store';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import {
  OwnerOnboardingService,
  type AccountType,
  type ListingTypePreference,
} from '@/services/preferences.api';

const COUNTRIES = ['Morocco', 'France', 'Spain', 'Portugal', 'United Arab Emirates'];

const CITIES_BY_COUNTRY: Record<string, string[]> = {
  Morocco: ['Casablanca', 'Rabat', 'Marrakech', 'Tanger', 'Fès', 'Agadir', 'Meknès', 'Oujda'],
};

// Aligné sur PropertyType du property-service
const LISTING_TYPES: Array<{ value: ListingTypePreference; label: string }> = [
  { value: 'APARTMENT', label: 'Apartment' },
  { value: 'HOUSE', label: 'House' },
  { value: 'VILLA', label: 'Villa' },
  { value: 'CONDO', label: 'Condo' },
  { value: 'STUDIO', label: 'Studio' },
  { value: 'LOFT', label: 'Loft' },
  { value: 'TOWNHOUSE', label: 'Townhouse' },
  { value: 'BUNGALOW', label: 'Bungalow' },
  { value: 'CABIN', label: 'Cabin' },
  { value: 'CASTLE', label: 'Castle' },
  { value: 'ROOM', label: 'Room' },
];

function OnboardingOwnerPage() {
  const navigate = useNavigate();
  const [step, setStep] = useState(1);

  // Step 1 — About you
  const [country, setCountry] = useState('Morocco');
  const [city, setCity] = useState('');
  const [accountType, setAccountType] = useState<AccountType>('INDIVIDUAL');

  // Reprise : si le profil est déjà complété, démarrer au step 2
  useEffect(() => {
    const u = authStore.getState().user;
    if (u?.country && u?.city) {
      setCountry(u.country);
      setCity(u.city);
      if (u.accountType) setAccountType(u.accountType);
      setStep(2);
    }
  }, []);

  // Step 2 — What do you want to list?
  const [listingTypes, setListingTypes] = useState<Set<ListingTypePreference>>(new Set());

  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const toggleListingType = (type: ListingTypePreference) => {
    setListingTypes((prev) => {
      const next = new Set(prev);
      if (next.has(type)) {
        next.delete(type);
      } else {
        next.add(type);
      }
      return next;
    });
  };

  const handleAboutYou = async () => {
    setSaving(true);
    setError(null);
    try {
      await OwnerOnboardingService.updateAboutYou({ country, city, accountType });
      setStep(2);
    } catch (err) {
      setError(
        isAxiosError(err) && err.response?.status === 400
          ? 'Invalid values. Please check your inputs.'
          : 'Failed to save. Please try again.'
      );
    } finally {
      setSaving(false);
    }
  };

  const handleListingTypes = async () => {
    setSaving(true);
    setError(null);
    try {
      await OwnerOnboardingService.saveListingPreferences(Array.from(listingTypes));
      setStep(3);
    } catch (err) {
      setError(
        isAxiosError(err) && err.response?.status === 400
          ? 'Invalid values. Please check your inputs.'
          : 'Failed to save. Please try again.'
      );
    } finally {
      setSaving(false);
    }
  };

  const handleStartVerification = () => {
    navigate('/owner/verification');
  };

  const cities = CITIES_BY_COUNTRY[country] ?? [];

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
            {step > 1 && (
              <button
                type="button"
                onClick={() => setStep(step - 1)}
                className="mb-4 flex h-9 w-9 items-center justify-center rounded-full border border-white/10 bg-white/5 text-[#B5ABC9] transition-colors hover:bg-white/10 hover:text-[#F6F2EC]"
              >
                <ChevronLeft className="h-4 w-4" />
              </button>
            )}
            <h1 className="font-display text-2xl font-normal text-[#F6F2EC]">
              {step === 1 && 'Tell us about yourself'}
              {step === 2 && 'What would you like to rent?'}
              {step === 3 && "You're almost ready!"}
            </h1>
            <p className="mt-1.5 text-sm text-[#8D94A8]">
              {step === 1 && 'Welcome to Nestora 👋 Let’s set up your host account.'}
              {step === 2 && 'These are preferences — you’ll add properties later.'}
              {step === 3 &&
                'To publish a property on Nestora, you’ll need to verify your identity.'}
            </p>

            {/* Progress */}
            <div className="mt-5 flex gap-2">
              {[1, 2, 3].map((s) => (
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

          {/* Step 1 — About you */}
          {step === 1 && (
            <div className="space-y-6">
              <div className="space-y-1.5">
                <Label className="text-[13px] font-medium text-[#D5CDE8]">Country</Label>
                <Select
                  value={country}
                  onValueChange={(v) => {
                    setCountry(v);
                    setCity('');
                  }}
                >
                  <SelectTrigger className="w-full h-11 rounded-xl border-white/[0.08] bg-white/[0.04] text-[#F6F2EC] data-[placeholder]:text-[#8D94A8]">
                    <SelectValue placeholder="Select a country" />
                  </SelectTrigger>
                  <SelectContent className="rounded-xl border-white/10 bg-[#16132e] text-[#F6F2EC]">
                    {COUNTRIES.map((c) => (
                      <SelectItem key={c} value={c}>
                        {c}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              <div className="space-y-1.5">
                <Label className="text-[13px] font-medium text-[#D5CDE8]">City</Label>
                {cities.length > 0 ? (
                  <Select value={city} onValueChange={setCity}>
                    <SelectTrigger className="w-full h-11 rounded-xl border-white/[0.08] bg-white/[0.04] text-[#F6F2EC] data-[placeholder]:text-[#8D94A8]">
                      <SelectValue placeholder="Select a city" />
                    </SelectTrigger>
                    <SelectContent className="rounded-xl border-white/10 bg-[#16132e] text-[#F6F2EC]">
                      {cities.map((c) => (
                        <SelectItem key={c} value={c}>
                          {c}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                ) : (
                  <Input
                    value={city}
                    onChange={(e) => setCity(e.target.value)}
                    placeholder="Enter your city"
                    className="h-11 rounded-xl border-white/[0.08] bg-white/[0.04] text-[#F6F2EC] placeholder:text-[#8D94A8]"
                  />
                )}
              </div>

              <div className="space-y-1.5">
                <Label className="text-[13px] font-medium text-[#D5CDE8]">Account type</Label>
                <div className="grid grid-cols-2 gap-3 pt-1">
                  {(
                    [
                      { value: 'INDIVIDUAL', label: 'Individual', icon: User },
                      { value: 'COMPANY', label: 'Company', icon: Building2 },
                    ] as const
                  ).map(({ value, label, icon: Icon }) => (
                    <button
                      key={value}
                      type="button"
                      onClick={() => setAccountType(value)}
                      className={`flex flex-col items-center gap-2 rounded-xl border px-4 py-4 transition-all ${
                        accountType === value
                          ? 'border-primary/60 bg-primary/10'
                          : 'border-white/[0.08] bg-white/[0.04] hover:bg-white/[0.08]'
                      }`}
                    >
                      <Icon
                        className={`h-5 w-5 ${
                          accountType === value ? 'text-primary' : 'text-[#8D94A8]'
                        }`}
                      />
                      <span
                        className={`text-sm font-medium ${
                          accountType === value ? 'text-[#F6F2EC]' : 'text-[#B5ABC9]'
                        }`}
                      >
                        {label}
                      </span>
                    </button>
                  ))}
                </div>
              </div>

              <Button
                onClick={handleAboutYou}
                disabled={saving || !city}
                className="w-full h-12 rounded-xl bg-primary text-primary-foreground font-semibold text-[15px] hover:bg-primary-hover transition-all duration-300 hover:-translate-y-px"
              >
                {saving ? 'Saving…' : 'Continue'}
              </Button>
            </div>
          )}

          {/* Step 2 — What do you want to list? */}
          {step === 2 && (
            <div className="space-y-6">
              <div className="grid grid-cols-2 gap-3">
                {LISTING_TYPES.map(({ value, label }) => (
                  <label
                    key={value}
                    className="flex cursor-pointer items-center gap-2.5 rounded-xl border border-white/[0.08] bg-white/[0.04] px-4 py-3 transition-colors hover:bg-white/[0.08] has-[:checked]:border-primary/50 has-[:checked]:bg-primary/10"
                  >
                    <input
                      type="checkbox"
                      checked={listingTypes.has(value)}
                      onChange={() => toggleListingType(value)}
                      className="h-4 w-4 shrink-0 rounded border-white/20 bg-white/5 accent-primary"
                    />
                    <span className="text-sm text-[#D5CDE8]">{label}</span>
                  </label>
                ))}
              </div>

              <Button
                onClick={handleListingTypes}
                disabled={saving || listingTypes.size === 0}
                className="w-full h-12 rounded-xl bg-primary text-primary-foreground font-semibold text-[15px] hover:bg-primary-hover transition-all duration-300 hover:-translate-y-px"
              >
                {saving ? 'Saving…' : 'Continue'}
              </Button>
            </div>
          )}

          {/* Step 3 — Finish */}
          {step === 3 && (
            <div className="space-y-6 text-center">
              <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-emerald-500/15">
                <Check className="h-8 w-8 text-emerald-400" />
              </div>

              <div className="space-y-3">
                <Button
                  onClick={handleStartVerification}
                  disabled={saving}
                  className="w-full h-12 rounded-xl bg-primary text-primary-foreground font-semibold text-[15px] hover:bg-primary-hover transition-all duration-300 hover:-translate-y-px"
                >
                  <span className="flex items-center gap-2">
                    <ShieldCheck className="h-4 w-4" />
                    {saving ? 'Starting…' : 'Start verification'}
                  </span>
                </Button>
                <Button
                  variant="ghost"
                  onClick={() => navigate('/')}
                  disabled={saving}
                  className="w-full h-11 rounded-xl text-[#B5ABC9] hover:bg-white/5 hover:text-[#F6F2EC]"
                >
                  I'll do this later
                </Button>
              </div>

              <p className="text-xs leading-relaxed text-[#8D94A8]">
                Identity verification is required before publishing. You can complete it anytime
                from your profile.
              </p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

export default OnboardingOwnerPage;
