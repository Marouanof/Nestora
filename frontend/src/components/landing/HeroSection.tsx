import type { CSSProperties, FormEvent } from 'react';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useParallax } from '@/hooks/useParallax';

const delayed = (ms: number) => ({ '--d': `${ms}ms` }) as CSSProperties;

export default function HeroSection() {
  const navigate = useNavigate();
  const parallaxRef = useParallax<HTMLDivElement>(0.16);
  const [query, setQuery] = useState('');

  const onSubmit = (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const q = query.trim();
    navigate(q ? `/search/results?location=${encodeURIComponent(q)}` : '/search');
  };

  return (
    <section className="relative min-h-[88vh] lg:min-h-[92vh] flex items-end overflow-hidden bg-ink-950">
      {/* Photo layer — parallax wrapper + slow cinematic zoom */}
      <div className="absolute inset-0 will-change-transform" ref={parallaxRef}>
        <img
          src="/images/nestora/hero-nestora.webp"
          alt="An elegant, light-filled residence at dusk"
          width={1376}
          height={768}
          fetchPriority="high"
          decoding="async"
          className="landing-zoom h-full w-full object-cover object-center"
        />
      </div>
      {/* Overlay — stronger left scrim for text legibility, preserving photography on the right */}
      <div
        aria-hidden="true"
        className="absolute inset-0 bg-[linear-gradient(100deg,rgba(13,11,38,0.82)_0%,rgba(13,11,38,0.62)_35%,rgba(13,11,38,0.18)_65%,transparent_100%)]"
      />
      {/* Bottom gradient for smooth transition into the next section */}
      <div
        aria-hidden="true"
        className="absolute inset-0 bg-[linear-gradient(180deg,transparent_50%,rgba(13,11,38,0.5)_85%,rgba(13,11,38,0.88)_100%)]"
      />

      <div className="relative w-full max-w-[1240px] mx-auto px-4 sm:px-6 lg:px-8 pt-36 pb-20 lg:pb-28">
        <p
          className="landing-eyebrow landing-hero-anim text-lavender-300/80 mb-5"
          style={delayed(250)}
        >
          Nestora — Intelligent property discovery
        </p>

        <h1 className="font-display font-normal text-[clamp(2.75rem,8vw,7.375rem)] leading-[1.04] tracking-[-0.02em] text-[#F6F2EC] text-balance" style={{ textShadow: '0 2px 24px rgba(0,0,0,0.3)' }}>
          <span className="landing-hero-anim block" style={delayed(400)}>
            Find a home
          </span>
          <span className="landing-hero-anim block" style={delayed(540)}>
            that feels
          </span>
          <span className="landing-hero-anim block" style={delayed(680)}>
            like <em className="font-normal italic text-champagne-deep">yours</em>.
          </span>
        </h1>

        <p
          className="landing-hero-anim max-w-[520px] mt-6 text-base sm:text-lg leading-relaxed text-[#D5CDE8]"
          style={{ ...delayed(900), textShadow: '0 1px 8px rgba(0,0,0,0.4)' }}
        >
          Verified rentals, trusted owners. Search by lifestyle, not filters — then move in with secure, hassle-free payments.
        </p>

        {/* Primary search — the main action */}
        <form
          role="search"
          onSubmit={onSubmit}
          className="landing-hero-anim mt-8 flex max-w-[620px] items-center gap-1.5 rounded-full border border-[#CBBEFB]/25 bg-[#16113A]/50 p-2 pl-5 shadow-[0_24px_60px_-30px_rgba(5,3,20,0.8)] backdrop-blur-xl transition-colors focus-within:border-[#C9BEFB]/60 focus-within:bg-[#16113A]/65 focus-within:ring-4 focus-within:ring-[#8A5CF6]/15"
          style={delayed(850)}
        >
          <svg
            className="flex-none text-lavender-300"
            viewBox="0 0 24 24"
            width="18"
            height="18"
            aria-hidden="true"
            fill="none"
          >
            <path
              d="M21 21l-4.35-4.35M19 11a8 8 0 1 1-16 0 8 8 0 0 1 16 0Z"
              stroke="currentColor"
              strokeWidth="1.7"
              strokeLinecap="round"
            />
            <path
              d="M10 7.5c-1.1.3-1.8 1-2 2.1M7 15.2a6 6 0 0 0 5.4-2"
              stroke="currentColor"
              strokeWidth="1.7"
              strokeLinecap="round"
              strokeDasharray="1.5 4"
            />
          </svg>
          <input
            type="text"
            className="min-w-0 flex-1 border-none bg-transparent px-2 py-3 text-[15px] text-[#F6F2EC] outline-none placeholder:text-[#B5ABC9]/80"
            placeholder="A quiet apartment near the city with natural light…"
            aria-label="Describe the home you are looking for"
            enterKeyHint="search"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
          />
          <button
            type="submit"
            className="inline-flex flex-none items-center gap-2 rounded-full bg-[#F8F7F2] px-6 py-3 text-sm font-semibold text-ink-900 transition-all duration-300 hover:-translate-y-px hover:shadow-[0_12px_26px_-14px_rgba(8,6,26,0.6)]"
          >
            Search
            <svg viewBox="0 0 16 16" width="14" height="14" aria-hidden="true" fill="none">
              <path
                d="M2 8h11M9 4l4 4-4 4"
                stroke="currentColor"
                strokeWidth="1.6"
                strokeLinecap="round"
                strokeLinejoin="round"
              />
            </svg>
          </button>
        </form>

        {/* Secondary actions — supporting, not competing */}
        <div className="landing-hero-anim flex flex-wrap items-center gap-3.5 mt-5" style={delayed(1000)}>
          <button
            onClick={() => navigate('/properties')}
            className="inline-flex items-center justify-center gap-2 rounded-full border border-[#F6F2EC]/35 bg-white/[0.10] px-6 py-3 text-[14px] font-semibold text-[#F6F2EC] backdrop-blur transition-all duration-300 hover:-translate-y-0.5 hover:bg-white/18"
          >
            Browse all properties
          </button>
          <a
            href="#discovery"
            className="inline-flex items-center justify-center gap-2 rounded-full border border-[#F6F2EC]/35 bg-white/[0.10] px-6 py-3 text-[14px] font-semibold text-[#E0DAF0] backdrop-blur transition-all duration-300 hover:-translate-y-0.5 hover:bg-white/18 hover:text-[#F6F2EC]"
          >
            How Nestora works
          </a>
        </div>

        {/* Trust line — understated */}
        <p
          className="landing-hero-anim mt-6 text-[13px] tracking-wide text-[#D5CDE8]"
          style={delayed(1100)}
        >
          Secure payments&nbsp;&nbsp;·&nbsp;&nbsp;Verified owners&nbsp;&nbsp;·&nbsp;&nbsp;No hidden fees
        </p>
      </div>

      {/* Scroll cue */}
      <a
        href="#properties"
        aria-label="Scroll to properties"
        className="landing-hero-anim absolute bottom-9 right-[clamp(1.25rem,4vw,2.5rem)] hidden items-center gap-3 text-[11px] font-semibold uppercase tracking-[0.28em] text-[#B5ABC9] transition-colors hover:text-[#F6F2EC] sm:inline-flex"
        style={{ animationDelay: '1.5s' }}
      >
        <span className="landing-scroll-line" />
        Scroll
      </a>
    </section>
  );
}
