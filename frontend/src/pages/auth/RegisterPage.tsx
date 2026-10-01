import { useState } from 'react';
import RoleSelection from './RoleSelection';
import RegisterForm from './RegisterForm';
import VerifyEmail from './VerifyEmail';

function RegisterPage() {
  const [step, setStep] = useState(1);
  const [role, setRole] = useState<"ROLE_TENANT" | "ROLE_OWNER" | null>(null);
  const [registeredEmail, setRegisteredEmail] = useState("");

  const handleRoleSelect = (selectedRole: "ROLE_TENANT" | "ROLE_OWNER") => {
    setRole(selectedRole);
  };

  const handleContinue = () => {
    if (role) setStep(2);
  };

  const handleBack = () => {
    setStep(1);
  };
  const handleFormSuccess = (email: string) => {
    setRegisteredEmail(email);
    setStep(3);
  };

  return (
    <div className="relative flex items-center justify-center overflow-hidden" style={{ background: '#0D0B26', minHeight: 'calc(100vh - 4rem)' }}>
      {/* Background image */}
      <div className="absolute inset-0">
        <img
          src="/images/nestora/hero-nestora.webp"
          alt=""
          aria-hidden="true"
          className="h-full w-full object-cover object-center opacity-25"
        />
      </div>

      {/* Gradient overlays */}
      <div
        aria-hidden="true"
        className="absolute inset-0 bg-[linear-gradient(135deg,rgba(13,11,38,0.92)_0%,rgba(13,11,38,0.7)_50%,rgba(13,11,38,0.85)_100%)]"
      />
      <div
        aria-hidden="true"
        className="absolute inset-0 bg-[radial-gradient(ellipse_at_30%_50%,rgba(81,70,229,0.12)_0%,transparent_60%)]"
      />

      {/* Main content */}
      <div className="relative z-10 flex w-full max-w-[1100px] mx-auto px-4 sm:px-6 lg:px-8 gap-8 lg:gap-16 items-center py-12">
        {/* Left: Welcome message */}
        <div className="hidden lg:flex flex-col flex-1 max-w-md">
          <p className="landing-eyebrow text-lavender-300/70 mb-5">Get started</p>
          <h1 className="font-display text-5xl font-normal leading-[1.08] tracking-[-0.02em] text-[#F6F2EC]" style={{ textShadow: '0 2px 24px rgba(0,0,0,0.3)' }}>
            Join <em className="italic text-champagne-deep">Nestora</em>.
          </h1>
          <p className="mt-6 text-base leading-relaxed text-[#B5ABC9] max-w-sm">
            Create an account to start renting or listing properties on the Nestora platform.
          </p>
        </div>

        {/* Right: Form area */}
        <div className="w-full max-w-md mx-auto lg:mx-0">
          {/* Mobile heading */}
          <div className="lg:hidden mb-8 text-center">
            <h1 className="font-display text-3xl font-normal leading-tight text-[#F6F2EC]">
              Join <em className="italic text-champagne-deep">Nestora</em>.
            </h1>
            <p className="mt-2 text-sm text-[#B5ABC9]">
              Create an account to get started
            </p>
          </div>

          {/* Form card */}
          <div className="rounded-2xl border border-white/[0.08] bg-white/[0.04] backdrop-blur-xl p-6 sm:p-8 shadow-[0_32px_64px_-48px_rgba(0,0,0,0.5)]">
            {step === 1 && (
              <RoleSelection
                selectedRole={role}
                onSelectRole={handleRoleSelect}
                onContinue={handleContinue}
              />
            )}
            {step === 2 && (
              <RegisterForm role={role!} onSuccess={handleFormSuccess} onBack={handleBack} />
            )}
            {step === 3 && <VerifyEmail email={registeredEmail} />}
          </div>

          {/* Trust line */}
          <p className="mt-6 text-center text-[12px] tracking-wide text-[#B5ABC9]/60">
            Secure payments&nbsp;&nbsp;·&nbsp;&nbsp;Verified owners&nbsp;&nbsp;·&nbsp;&nbsp;No hidden fees
          </p>
        </div>
      </div>
    </div>
  );
}

export default RegisterPage;
