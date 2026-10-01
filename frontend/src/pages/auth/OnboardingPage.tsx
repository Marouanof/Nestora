import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { isAxiosError } from 'axios';
import { Sparkles } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import {
  PreferencesService,
  type PropertyTypePreference,
} from '@/services/preferences.api';

const CITY_SUGGESTIONS = [
  'Casablanca',
  'Rabat',
  'Marrakech',
  'Tanger',
  'Fès',
  'Agadir',
  'Meknès',
  'Oujda',
  'Essaouira',
  'Chefchaouen',
];

// Aligné sur PropertyType du property-service
const PROPERTY_TYPES: Array<{ value: PropertyTypePreference; label: string }> = [
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

function OnboardingPage() {
  const navigate = useNavigate();
  const [city, setCity] = useState<string>('');
  const [propertyTypes, setPropertyTypes] = useState<Set<PropertyTypePreference>>(new Set());
  const [budgetMin, setBudgetMin] = useState('');
  const [budgetMax, setBudgetMax] = useState('');
  const [saving, setSaving] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Pré-remplit le formulaire si des préférences existent déjà (retour sur /onboarding)
  useEffect(() => {
    PreferencesService.getPreferences()
      .then((response) => {
        const data = response.data;
        if (data) {
          if (data.city) setCity(data.city);
          if (Array.isArray(data.propertyTypes)) {
            setPropertyTypes(new Set(data.propertyTypes));
          }
          if (data.budgetMin != null) setBudgetMin(String(data.budgetMin));
          if (data.budgetMax != null) setBudgetMax(String(data.budgetMax));
        }
      })
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  const togglePropertyType = (type: PropertyTypePreference) => {
    setPropertyTypes((prev) => {
      const next = new Set(prev);
      if (next.has(type)) {
        next.delete(type);
      } else {
        next.add(type);
      }
      return next;
    });
  };

  const handleSave = async (): Promise<boolean> => {
    // Validation budget
    const min = budgetMin ? Number(budgetMin) : null;
    const max = budgetMax ? Number(budgetMax) : null;
    if (min !== null && max !== null && min > max) {
      setError('Minimum budget must be less than or equal to maximum budget.');
      return false;
    }

    setSaving(true);
    setError(null);
    try {
      await PreferencesService.savePreferences({
        city: city || undefined,
        propertyTypes: Array.from(propertyTypes),
        budgetMin: min,
        budgetMax: max,
      });
      return true;
    } catch (err) {
      if (isAxiosError(err) && err.response?.status === 400) {
        setError('Invalid values. Please check your inputs.');
      } else {
        setError('Failed to save preferences. Please try again.');
      }
      return false;
    } finally {
      setSaving(false);
    }
  };

  // Save explicite -> redirection vers la recherche pré-filtrée
  const handleSaveAndSearch = async () => {
    if (await handleSave()) {
      navigate(buildSearchUrl());
    }
  };

  // Skip : on enregistre quand même pour ne pas re-déclencher l'onboarding au prochain login.
  // Ne navigue que si la sauvegarde a réussi, sinon l'erreur reste affichée.
  const handleSkip = async () => {
    if (await handleSave()) {
      navigate('/search');
    }
  };

  // Pré-remplit la recherche avec les choix de l'onboarding.
  // La recherche backend ne filtre que sur un seul type : on transmet le premier
  // via `propertyType` (compat) + la liste complète via `propertyTypes`.
  const buildSearchUrl = () => {
    const params = new URLSearchParams();
    if (city) params.set('location', city);
    if (budgetMin) params.set('minPrice', budgetMin);
    if (budgetMax) params.set('maxPrice', budgetMax);
    const types = Array.from(propertyTypes);
    if (types.length >= 1) {
      params.set('propertyType', types[0]);
      params.set('propertyTypes', types.join(','));
    }
    const query = params.toString();
    return query ? `/search?${query}` : '/search';
  };

  return (
    <div
      className="relative flex items-center justify-center overflow-hidden"
      style={{ background: '#0D0B26', minHeight: 'calc(100vh - 4rem)' }}
    >
      {/* Gradient overlays */}
      <div
        aria-hidden="true"
        className="absolute inset-0 bg-[radial-gradient(ellipse_at_30%_50%,rgba(81,70,229,0.12)_0%,transparent_60%)]"
      />

      <div className="relative z-10 w-full max-w-md mx-auto px-4 py-12">
        <div className="rounded-2xl border border-white/[0.08] bg-white/[0.04] backdrop-blur-xl p-6 sm:p-8 shadow-[0_32px_64px_-48px_rgba(0,0,0,0.5)]">
          {/* Header */}
          <div className="mb-8 text-center">
            <div className="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-full bg-primary/15">
              <Sparkles className="h-7 w-7 text-primary" />
            </div>
            <h1 className="font-display text-2xl font-normal text-[#F6F2EC]">
              Welcome to Nestora 👋
            </h1>
            <p className="mt-1.5 text-sm text-[#8D94A8]">
              Let&apos;s personalize your experience.
            </p>
          </div>

          {error && <p className="mb-6 text-[13px] text-red-400">{error}</p>}

          {loading ? (
            <p className="py-8 text-center text-sm text-[#8D94A8]">Loading your preferences…</p>
          ) : (
          <div className="space-y-6">
            {/* City : champ libre avec suggestions */}
            <div className="space-y-1.5">
              <Label className="text-[13px] font-medium text-[#D5CDE8]">
                Where are you looking?
              </Label>
              <Input
                list="city-suggestions"
                value={city}
                onChange={(e) => setCity(e.target.value)}
                maxLength={100}
                placeholder="Enter a city"
                className="h-11 rounded-xl border-white/[0.08] bg-white/[0.04] text-[#F6F2EC] placeholder:text-[#8D94A8] focus-visible:border-primary/50 focus-visible:ring-primary/20"
              />
              <datalist id="city-suggestions">
                {CITY_SUGGESTIONS.map((c) => (
                  <option key={c} value={c} />
                ))}
              </datalist>
            </div>

            {/* Property types */}
            <div className="space-y-1.5">
              <Label className="text-[13px] font-medium text-[#D5CDE8]">
                What are you looking for?
              </Label>
              <div className="grid grid-cols-2 gap-3 pt-1">
                {PROPERTY_TYPES.map(({ value, label }) => (
                  <label
                    key={value}
                    className="flex cursor-pointer items-center gap-2.5 rounded-xl border border-white/[0.08] bg-white/[0.04] px-4 py-3 transition-colors hover:bg-white/[0.08] has-[:checked]:border-primary/50 has-[:checked]:bg-primary/10"
                  >
                    <input
                      type="checkbox"
                      checked={propertyTypes.has(value)}
                      onChange={() => togglePropertyType(value)}
                      className="h-4 w-4 shrink-0 rounded border-white/20 bg-white/5 accent-primary"
                    />
                    <span className="text-sm text-[#D5CDE8]">{label}</span>
                  </label>
                ))}
              </div>
              {propertyTypes.size > 1 && (
                <p className="pt-1 text-[12px] text-[#8D94A8]">
                  Search will be filtered on the first type — all selected types are saved for your recommendations.
                </p>
              )}
            </div>

            {/* Budget */}
            <div className="space-y-1.5">
              <Label className="text-[13px] font-medium text-[#D5CDE8]">
                What&apos;s your approximate budget? (MAD/night)
              </Label>
              <div className="flex items-center gap-3 pt-1">
                <Input
                  type="number"
                  min={0}
                  value={budgetMin}
                  onChange={(e) => setBudgetMin(e.target.value)}
                  placeholder="Min"
                  className="h-11 rounded-xl border-white/[0.08] bg-white/[0.04] text-[#F6F2EC] placeholder:text-[#8D94A8] focus-visible:border-primary/50 focus-visible:ring-primary/20"
                />
                <span className="text-[#8D94A8]">—</span>
                <Input
                  type="number"
                  min={0}
                  value={budgetMax}
                  onChange={(e) => setBudgetMax(e.target.value)}
                  placeholder="Max"
                  className="h-11 rounded-xl border-white/[0.08] bg-white/[0.04] text-[#F6F2EC] placeholder:text-[#8D94A8] focus-visible:border-primary/50 focus-visible:ring-primary/20"
                />
              </div>
            </div>

            {/* Actions */}
            <div className="space-y-3 pt-2">
              <Button
                onClick={handleSaveAndSearch}
                disabled={saving}
                className="w-full h-12 rounded-xl bg-primary text-primary-foreground font-semibold text-[15px] hover:bg-primary-hover transition-all duration-300 hover:-translate-y-px hover:shadow-[0_12px_26px_-14px_rgba(81,70,229,0.5)]"
              >
                {saving ? (
                  <span className="flex items-center gap-2">
                    <span className="h-4 w-4 border-2 border-primary-foreground/30 border-t-primary-foreground rounded-full animate-spin" />
                    Saving…
                  </span>
                ) : (
                  'Save preferences'
                )}
              </Button>
              <Button
                variant="ghost"
                onClick={handleSkip}
                disabled={saving}
                className="w-full h-11 rounded-xl text-[#B5ABC9] hover:bg-white/5 hover:text-[#F6F2EC]"
              >
                Continue exploring
              </Button>
            </div>
          </div>
          )}
        </div>
      </div>
    </div>
  );
}

export default OnboardingPage;
