import Reveal from './Reveal';
import { RecommendedProperties } from '@/components/ai/RecommendedProperties';
import type { PropertyRecommendation } from '@/services/aiService';

interface BudgetRecommendationsSectionProps {
  budget: number;
  budgetSubmitted: boolean;
  onBudgetChange: (value: number) => void;
  onSubmit: () => void;
  recommendations: PropertyRecommendation[];
  loading: boolean;
  error: boolean;
  onPropertySelect: (id: string | number) => void;
}

export default function BudgetRecommendationsSection({
  budget,
  budgetSubmitted,
  onBudgetChange,
  onSubmit,
  recommendations,
  loading,
  error,
  onPropertySelect,
}: BudgetRecommendationsSectionProps) {
  const handleSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    onSubmit();
  };

  return (
    <section id="budget-recommendations" className="relative py-24 lg:py-36">
      <div className="mx-auto max-w-[1240px] px-4 sm:px-6 lg:px-8">
        <Reveal className="max-w-[620px]">
          <p className="landing-eyebrow text-[#8B5CF6] dark:text-[#C9BEFB]">Personalised for you</p>
          <h2 className="font-display mt-6 text-4xl font-light leading-[1.08] text-[#0D0B26] dark:text-[#F6F2EC] sm:text-5xl">
            Tell us your budget.
            <br />
            We&rsquo;ll <em className="italic text-[#C8A868]">read the market</em> for you.
          </h2>
          <p className="mt-6 text-lg leading-relaxed text-[#5A5370] dark:text-[#B5ABC9]">
            Set a daily budget and Nestora&rsquo;s recommendation engine will surface the
            homes that fit — priced fairly, ranked honestly.
          </p>
        </Reveal>

        <Reveal delay={140} className="mt-12">
          <form
            onSubmit={handleSubmit}
            className="flex max-w-[620px] flex-col gap-3 rounded-3xl border border-[#0D0B26]/8 bg-white p-6 shadow-[0_32px_64px_-48px_rgba(13,11,38,0.35)] dark:border-white/10 dark:bg-[#16113A] sm:flex-row sm:items-center"
          >
            <label htmlFor="daily-budget" className="sr-only">
              Daily budget
            </label>
            <div className="flex min-w-0 flex-1 items-center gap-3 rounded-full border border-[#0D0B26]/12 bg-[#F3F0EA] px-5 py-3.5 dark:border-white/12 dark:bg-[#0D0B26]">
              <span className="text-sm font-semibold text-[#8B5CF6] dark:text-[#C9BEFB]">$</span>
              <input
                id="daily-budget"
                type="number"
                min="0"
                step="0.0001"
                inputMode="decimal"
                placeholder="0.05"
                value={budget || ''}
                onChange={(e) => onBudgetChange(Number(e.target.value))}
                className="min-w-0 flex-1 bg-transparent text-lg font-medium text-[#0D0B26] outline-none placeholder:text-[#8F86A6] dark:text-[#F6F2EC]"
              />
            </div>
            <button
              type="submit"
              className="inline-flex items-center justify-center gap-2.5 rounded-full bg-[#0D0B26] px-8 py-4 text-[15px] font-semibold text-[#F8F7F2] transition-all duration-300 hover:-translate-y-0.5 dark:bg-[#F8F7F2] dark:text-[#0D0B26]"
            >
              Find homes
            </button>
          </form>
        </Reveal>

        {budgetSubmitted && (
          <Reveal delay={120} className="mt-14">
            <div className="overflow-hidden rounded-3xl border border-[#0D0B26]/8 bg-white p-2 shadow-[0_32px_64px_-48px_rgba(13,11,38,0.35)] dark:border-white/10 dark:bg-[#16113A] sm:p-3">
              <div className="rounded-[20px] bg-[#FCFBF8] p-4 dark:bg-[#100C2E]/60 sm:p-6">
                <RecommendedProperties
                  properties={recommendations}
                  loading={loading}
                  error={error}
                  onPropertySelect={onPropertySelect}
                />
              </div>
            </div>
          </Reveal>
        )}
      </div>
    </section>
  );
}
