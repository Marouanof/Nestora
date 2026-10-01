import Reveal from './Reveal';
import { MarketTrendsWidget } from '@/components/ai/MarketTrendsWidget';
import type { CityAnalytics } from '@/services/aiService';

interface MarketTrendsSectionProps {
  analytics: CityAnalytics[];
  loading: boolean;
  error: boolean;
}

export default function MarketTrendsSection({
  analytics,
  loading,
  error,
}: MarketTrendsSectionProps) {
  return (
    <section id="ai-market-trends" className="relative border-t border-[#0D0B26]/8 bg-[#F3F0EA] py-24 lg:py-36 dark:border-white/8 dark:bg-[#100C2E]/40">
      <div className="mx-auto max-w-[1240px] px-4 sm:px-6 lg:px-8">
        <Reveal className="grid grid-cols-12 items-end gap-x-6">
          <div className="col-span-12 lg:col-span-7">
            <p className="landing-eyebrow text-[#8B5CF6] dark:text-[#C9BEFB]">Market intelligence</p>
            <h2 className="font-display mt-6 text-4xl font-light leading-[1.08] text-[#0D0B26] dark:text-[#F6F2EC] sm:text-5xl lg:text-[56px]">
              Where the market is
              <em className="italic text-[#C8A868]"> heading</em>
            </h2>
          </div>
          <div className="col-span-12 mt-6 lg:col-span-5 lg:mt-0">
            <p className="text-lg leading-relaxed text-[#5A5370] dark:text-[#B5ABC9]">
              AI-forecasted price trends across major cities — computed daily from live listings.
            </p>
          </div>
        </Reveal>

        <Reveal delay={150} className="mt-16">
          <div className="overflow-hidden rounded-3xl border border-[#0D0B26]/8 bg-white p-2 shadow-[0_32px_64px_-48px_rgba(13,11,38,0.35)] dark:border-white/10 dark:bg-[#16113A] sm:p-3">
            <div className="rounded-[20px] bg-[#FCFBF8] p-4 dark:bg-[#100C2E]/60 sm:p-6">
              <MarketTrendsWidget
                analytics={analytics}
                loading={loading}
                error={error}
                maxCitiesToShow={5}
              />
            </div>
          </div>
        </Reveal>
      </div>
    </section>
  );
}
