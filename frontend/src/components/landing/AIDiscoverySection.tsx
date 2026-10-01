import Reveal from './Reveal';

const values = [
  {
    title: 'Matches your life',
    copy: 'Forecasts and pricing models that understand seasonality, location and the market you are moving into.',
  },
  {
    title: 'Secure payments',
    copy: 'Your payment is held safely until you check in — released to the host only once the stay is honoured.',
  },
  {
    title: 'Every host verified',
    copy: 'Identity checks behind every listing. No anonymous inboxes, no disappearing landlords.',
  },
];

export default function AIDiscoverySection() {
  return (
    <section id="discovery" className="relative overflow-hidden bg-[#0D0B26] py-24 lg:py-36">
      {/* quiet ambient light */}
      <div
        aria-hidden="true"
        className="pointer-events-none absolute inset-0 bg-[radial-gradient(55%_45%_at_12%_8%,rgba(138,92,246,0.22),transparent_65%)]"
      />

      <div className="relative mx-auto max-w-[1240px] px-4 sm:px-6 lg:px-8">
        <div className="grid grid-cols-12 items-center gap-x-6 gap-y-16">
          {/* Copy */}
          <div className="col-span-12 lg:col-span-6">
            <Reveal>
              <p className="landing-eyebrow text-[#C9BEFB]">Intelligence, quietly applied</p>
              <h2 className="font-display mt-6 text-4xl font-light leading-[1.08] text-[#F6F2EC] sm:text-5xl lg:text-[60px]">
                Discovery that reads the
                <em className="italic text-champagne"> signal</em>, not the noise.
              </h2>
              <p className="mt-8 max-w-[46ch] text-lg leading-relaxed text-[#B5ABC9]">
                Nestora pairs a purpose-built property network with an AI that turns city-level
                market data into personal guidance — then lets secure payments settle the trust.
              </p>
            </Reveal>

            <div className="mt-14 space-y-10">
              {values.map((v, i) => (
                <Reveal key={v.title} delay={i * 120} className="group flex gap-6 border-t border-white/10 pt-8">
                  <span className="font-display text-5xl font-light italic leading-none text-[#C9BEFB]/50 transition-colors duration-300 group-hover:text-[#C9BEFB]">
                    0{i + 1}
                  </span>
                  <div>
                    <h3 className="font-display text-2xl font-light text-[#F6F2EC]">{v.title}</h3>
                    <p className="mt-2 max-w-[44ch] leading-relaxed text-[#8F86A6]">{v.copy}</p>
                  </div>
                </Reveal>
              ))}
            </div>
          </div>

          {/* Imagery */}
          <div className="col-span-12 lg:col-span-6">
            <Reveal delay={150} className="relative">
              <div className="overflow-hidden rounded-3xl">
                <img
                  src="/images/nestora/ai-discovery.webp"
                  alt="A calm, light-filled interior of a Nestora-listed home"
                  loading="lazy"
                  decoding="async"
                  className="aspect-[4/5] w-full object-cover lg:aspect-[5/6]"
                />
              </div>
              <div className="absolute -left-5 bottom-10 max-w-[280px] rounded-2xl border border-white/10 bg-[#16113A]/85 p-5 backdrop-blur-xl sm:-left-10">
                <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[#C9BEFB]">
                  Live forecast
                </p>
                <p className="font-display mt-2 text-2xl font-light text-[#F6F2EC]">−2.3%</p>
                <p className="mt-1 text-sm leading-snug text-[#B5ABC9]">
                  Price trajectory for the next 30 days in your chosen city.
                </p>
              </div>
            </Reveal>
          </div>
        </div>
      </div>
    </section>
  );
}
