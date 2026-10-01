import { useEffect, useState } from 'react';

import HeroSection from '@/components/landing/HeroSection';
import PropertyShowcase from '@/components/landing/PropertyShowcase';
import AIDiscoverySection from '@/components/landing/AIDiscoverySection';
import HowItWorks from '@/components/landing/HowItWorks';
import MarketTrendsSection from '@/components/landing/MarketTrendsSection';
import BudgetRecommendationsSection from '@/components/landing/BudgetRecommendationsSection';
import CTASection from '@/components/landing/CTASection';
import { useMarketAnalytics, usePropertyRecommendations } from '@/hooks/ai';
import { useUserRole } from '@/hooks/useUserRole';
import { usePropertyList } from '@/hooks/useProperty';
import { PreferencesService } from '@/services/preferences.api';

const Home = () => {
  const { isTenant, isOwner, user } = useUserRole();
  const [budget, setBudget] = useState<number>(0);
  const [budgetSubmitted, setBudgetSubmitted] = useState<boolean>(false);

  // Pré-remplit le budget depuis les préférences de l'onboarding
  useEffect(() => {
    if (isTenant && user) {
      PreferencesService.getPreferences()
        .then((res) => {
          const max = res.data?.budgetMax;
          if (max) setBudget(Number(max));
        })
        .catch(() => {});
    }
  }, [isTenant, user]);

  // AI hooks
  const { data: marketAnalytics, isLoading: analyticsLoading, isError: analyticsError } = useMarketAnalytics();
  const { data: recommendations, isLoading: recsLoading, isError: recsError } = usePropertyRecommendations(
    budget,
    isTenant && !!user && budgetSubmitted
  );

  // Latest properties
  const { data: latestProperties, loading: propertiesLoading } = usePropertyList(0, 4);

  return (
    <div className="min-h-screen bg-[#FCFBF8] text-[#0D0B26] dark:bg-[#0D0B26] dark:text-[#F6F2EC]">
      <HeroSection />

      <PropertyShowcase properties={latestProperties?.content ?? null} loading={propertiesLoading} />

      <HowItWorks />

      <AIDiscoverySection />

      <MarketTrendsSection
        analytics={marketAnalytics ?? []}
        loading={analyticsLoading}
        error={!!analyticsError}
      />

      {user && (
        <BudgetRecommendationsSection
          budget={budget}
          budgetSubmitted={budgetSubmitted}
          onBudgetChange={setBudget}
          onSubmit={() => setBudgetSubmitted(true)}
          recommendations={recommendations ?? []}
          loading={recsLoading}
          error={!!recsError}
          onPropertySelect={(id) => {
            window.location.href = `/properties/${id}`;
          }}
        />
      )}

      <CTASection isOwner={isOwner} />
    </div>
  );
};

export default Home;
