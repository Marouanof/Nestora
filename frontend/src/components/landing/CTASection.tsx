import { useNavigate } from 'react-router-dom';
import Reveal from './Reveal';

interface CTASectionProps {
  isOwner: boolean;
}

export default function CTASection({ isOwner }: CTASectionProps) {
  const navigate = useNavigate();

  return (
    <section id="owner-cta" className="relative overflow-hidden bg-[#0D0B26] py-28 lg:py-40">
      <div className="absolute inset-0" aria-hidden="true">
        <img
          src="/images/nestora/cta-nestora.webp"
          alt=""
          loading="lazy"
          decoding="async"
          className="landing-zoom h-full w-full object-cover object-center"
        />
        <div className="absolute inset-0 bg-[#0D0B26]/72" />
      </div>

      <div className="relative mx-auto max-w-[900px] px-4 text-center sm:px-6">
        <Reveal>
          <p className="landing-eyebrow justify-center text-[#C9BEFB]">For hosts</p>
          <h2 className="font-display mt-6 text-4xl font-light leading-[1.05] text-[#F6F2EC] sm:text-5xl lg:text-[64px]">
            Own a property?
            <br />
            Let it <em className="italic text-champagne">work</em> for you.
          </h2>
          <p className="mx-auto mt-7 max-w-[54ch] text-lg leading-relaxed text-[#C3BCD2]">
            List once and let verification and AI pricing do the heavy lifting.
            No intermediaries — just direct, simple rentals.
          </p>
          <div className="mt-10 flex flex-wrap items-center justify-center gap-3.5">
            <button
              type="button"
              onClick={() => navigate(isOwner ? '/owner/properties' : '/register')}
              className="inline-flex items-center justify-center gap-2.5 rounded-full bg-[#F8F7F2] px-8 py-4 text-[15px] font-semibold text-[#0D0B26] transition-all duration-300 hover:-translate-y-0.5 hover:shadow-[0_22px_46px_-20px_rgba(8,6,26,0.7)]"
            >
              {isOwner ? 'Manage listings' : 'List your property'}
            </button>
            <a
              href="#how-it-works"
              className="inline-flex items-center justify-center gap-2.5 rounded-full border border-[#F6F2EC]/30 bg-white/5 px-8 py-4 text-[15px] font-semibold text-[#F6F2EC] backdrop-blur transition-all duration-300 hover:-translate-y-0.5 hover:bg-white/10"
            >
              How it works
            </a>
          </div>
        </Reveal>
      </div>
    </section>
  );
}
