import Reveal from './Reveal';

const steps = [
  {
    n: '01',
    title: 'Search & book',
    copy: 'Browse transparent listings and reserve the home you want with honest, all-in pricing.',
  },
  {
    n: '02',
    title: 'Pay securely',
    copy: 'Complete your payment through our secure platform — fast, simple, and reliable.',
  },
  {
    n: '03',
    title: 'Booking confirmed',
    copy: 'Your payment is held safely until check-in. Both parties are protected by our guarantee.',
  },
  {
    n: '04',
    title: 'Release to owner',
    copy: 'After a successful stay, funds are released to the verified host automatically.',
  },
];

export default function HowItWorks() {
  return (
    <section id="how-it-works" className="relative bg-transparent py-24 lg:py-36">
      <div className="mx-auto max-w-[1240px] px-4 sm:px-6 lg:px-8">
        <Reveal className="text-center">
          <p className="landing-eyebrow justify-center text-[#8B5CF6] dark:text-[#C9BEFB]">
            A quiet, four-step promise
          </p>
          <h2 className="font-display mt-6 text-4xl font-light leading-[1.08] text-[#0D0B26] dark:text-[#F6F2EC] sm:text-5xl lg:text-[56px]">
            How it works
          </h2>
        </Reveal>

        <div className="mt-20 grid grid-cols-1 gap-y-16 sm:grid-cols-2 lg:grid-cols-4 lg:gap-y-0 lg:gap-x-8">
          {steps.map((step, i) => (
            <Reveal
              key={step.n}
              delay={i * 110}
              className="group relative text-center"
            >
              <span className="font-display text-6xl font-light italic text-[#0D0B26]/15 transition-colors duration-300 group-hover:text-[#C8A868]/40 dark:text-[#F6F2EC]/10 dark:group-hover:text-[#C8A868]/40">
                {step.n}
              </span>
              {i < steps.length - 1 && (
                <span
                  aria-hidden="true"
                  className="pointer-events-none absolute left-1/2 top-8 hidden h-px w-[calc(100%+2rem)] -translate-y-1/2 lg:block lg:bg-[#0D0B26]/10 dark:lg:bg-white/10"
                  style={{ transform: 'translateX(50%)' }}
                />
              )}
              <h3 className="font-display mt-5 text-2xl font-light text-[#0D0B26] dark:text-[#F6F2EC]">
                {step.title}
              </h3>
              <p className="mx-auto mt-3 max-w-[30ch] leading-relaxed text-[#5A5370] dark:text-[#B5ABC9]">
                {step.copy}
              </p>
            </Reveal>
          ))}
        </div>
      </div>
    </section>
  );
}
