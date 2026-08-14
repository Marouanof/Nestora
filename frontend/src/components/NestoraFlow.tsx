import { useEffect, useRef, type CSSProperties } from "react";

type LineDef = {
  d: string;
  w?: number;
  o?: number;
  dash?: boolean;
  delay?: number;
};

type GuideDef = {
  d: string;
  o?: number;
};

type DotDef = {
  cx: number;
  cy: number;
  r: number;
  o?: number;
  delay?: number;
  teal?: boolean;
};

const flowLines: LineDef[] = [
  { d: "M -60 820 L 260 140", w: 1, o: 0.14, dash: true, delay: 0 },
  { d: "M 140 900 L 460 220", w: 1, o: 0.09 },
  { d: "M 60 760 L 340 180", w: 0.75, o: 0.08, dash: true, delay: -14 },
  { d: "M 240 880 L 500 340", w: 1, o: 0.06 },
  { d: "M 700 -80 L 980 620", w: 1, o: 0.12, dash: true, delay: -28 },
  { d: "M 860 -60 L 1140 700", w: 0.75, o: 0.07 },
  { d: "M 1080 -40 L 1360 780", w: 1, o: 0.1, dash: true, delay: -8 },
  { d: "M 560 -100 L 820 560", w: 0.75, o: 0.06 },
  { d: "M 60 60 L 240 -40", w: 0.75, o: 0.06 },
  { d: "M 1180 940 L 1360 860", w: 0.75, o: 0.06 },
];

const guideLines: GuideDef[] = [
  { d: "M 0 220 H 1440", o: 0.05 },
  { d: "M 0 520 H 1440", o: 0.04 },
  { d: "M 0 760 H 1440", o: 0.05 },
  { d: "M 320 0 V 900", o: 0.05 },
  { d: "M 1120 0 V 900", o: 0.05 },
];

const dots: DotDef[] = [
  { cx: 260, cy: 140, r: 2, o: 0.7, delay: 0 },
  { cx: 340, cy: 180, r: 1.6, o: 0.6, delay: 2.2 },
  { cx: 980, cy: 620, r: 1.8, o: 0.7, delay: 4.1 },
  { cx: 1140, cy: 700, r: 2.2, o: 0.8, delay: 1.4 },
  { cx: 500, cy: 340, r: 1.5, o: 0.6, delay: 5.5 },
  { cx: 700, cy: 120, r: 1.8, o: 0.65, delay: 3.2, teal: true },
  { cx: 1180, cy: 260, r: 1.7, o: 0.7, delay: 6.1 },
  { cx: 460, cy: 220, r: 1.5, o: 0.6, delay: 0.8, teal: true },
];

const flowStyle = `
@keyframes nf-dash { to { stroke-dashoffset: -156; } }
@keyframes nf-pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.12; } }
@keyframes nf-glowp { 0%, 100% { opacity: 1; } 50% { opacity: 0.25; } }
@keyframes nf-breathe { 0%, 100% { transform: translateY(0px); } 50% { transform: translateY(-12px); } }
.nf-flow { stroke-dasharray: 1.6 14; animation: nf-dash 48s linear infinite; }
.nf-dot { transform-box: fill-box; transform-origin: center; animation: nf-pulse 9s ease-in-out infinite; animation-delay: var(--nd, 0s); }
.nf-glow { transform-box: fill-box; transform-origin: center; animation: nf-glowp 11s ease-in-out infinite; animation-delay: var(--nd, 0s); }
.nf-breathe { animation: nf-breathe 22s ease-in-out infinite; }
@media (prefers-reduced-motion: reduce) {
  .nf-flow, .nf-dot, .nf-glow, .nf-breathe { animation: none !important; }
}
`;

const NestoraFlow = () => {
  const parallaxRef = useRef<SVGGElement>(null);
  const state = useRef({ tx: 0, ty: 0, dx: 0, dy: 0 });

  useEffect(() => {
    if (typeof window === "undefined") return;
    if (window.matchMedia("(pointer: coarse)").matches) return;
    if (window.matchMedia("(prefers-reduced-motion: reduce)").matches) return;

    let raf = 0;
    const onMove = (e: PointerEvent) => {
      state.current.tx = (e.clientX / window.innerWidth - 0.5) * 2;
      state.current.ty = (e.clientY / window.innerHeight - 0.5) * 2;
    };
    const tick = () => {
      const s = state.current;
      s.dx += (s.tx - s.dx) * 0.04;
      s.dy += (s.ty - s.dy) * 0.04;
      if (parallaxRef.current) {
        parallaxRef.current.style.transform = `translate(${(s.dx * 7).toFixed(2)}px, ${(s.dy * 5).toFixed(2)}px)`;
      }
      raf = requestAnimationFrame(tick);
    };
    window.addEventListener("pointermove", onMove, { passive: true });
    raf = requestAnimationFrame(tick);
    return () => {
      window.removeEventListener("pointermove", onMove);
      cancelAnimationFrame(raf);
    };
  }, []);

  return (
    <div aria-hidden="true" className="pointer-events-none absolute inset-0 select-none overflow-hidden">
      <style>{flowStyle}</style>
      <svg className="h-full w-full" viewBox="0 0 1440 900" preserveAspectRatio="xMidYMid slice" fill="none">
        <defs>
          <filter id="nf-glow" x="-60%" y="-60%" width="220%" height="220%">
            <feGaussianBlur stdDeviation="2.6" />
          </filter>
        </defs>
        <g ref={parallaxRef}>
          <g className="nf-breathe">
            {guideLines.map((line, i) => (
              <path key={`g-${i}`} d={line.d} stroke="var(--primary)" strokeWidth={0.75} strokeOpacity={line.o ?? 0.05} />
            ))}
            {flowLines.map((line, i) => (
              <path
                key={`l-${i}`}
                d={line.d}
                stroke="var(--primary)"
                strokeWidth={line.w ?? 1}
                strokeOpacity={line.o ?? 0.08}
                className={line.dash ? "nf-flow" : undefined}
                style={line.dash ? ({ animationDelay: `${line.delay ?? 0}s` } as CSSProperties) : undefined}
              />
            ))}
            {dots.map((dot, i) => (
              <g key={`d-${i}`} className="nf-dot" style={{ "--nd": `${dot.delay ?? 0}s` } as CSSProperties}>
                <circle cx={dot.cx} cy={dot.cy} r={dot.r * 3.2} fill={dot.teal ? "var(--secondary)" : "var(--primary)"} fillOpacity={(dot.o ?? 0.6) * 0.22} filter="url(#nf-glow)" />
                <circle cx={dot.cx} cy={dot.cy} r={dot.r} fill={dot.teal ? "var(--secondary)" : "var(--primary)"} fillOpacity={dot.o ?? 0.6} />
              </g>
            ))}
          </g>
        </g>
      </svg>
    </div>
  );
};

export default NestoraFlow;
