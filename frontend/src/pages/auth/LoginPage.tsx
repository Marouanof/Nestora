import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { isAxiosError } from 'axios';
import { useNavigate, Link } from 'react-router-dom';
import { Eye, EyeOff, ArrowRight, AlertCircle } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { authStore, getUserRoles } from '@/store/auth.store';
import { PreferencesService } from '@/services/preferences.api';
import { OwnerOnboardingService } from '@/services/preferences.api';

const loginSchema = z.object({
  email: z.string().email('Invalid email address'),
  password: z.string().min(1, 'Password is required'),
});

type LoginFormData = z.infer<typeof loginSchema>;

function Login() {
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginFormData>({
    resolver: zodResolver(loginSchema),
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [showPassword, setShowPassword] = useState(false);
  const navigate = useNavigate();

  // Tenant sans préférences -> onboarding tenant ; Owner sans préférences -> onboarding owner.
  // 401 sur le GET préférences = session invalide -> logout + /login (pas de home silencieux).
  const resolvePostLoginRoute = async () => {
    const { user, logout } = authStore.getState();
    const roles = getUserRoles(user);

    if (roles.includes('ROLE_OWNER')) {
      try {
        const response = await OwnerOnboardingService.getListingPreferences();
        if (response.status === 204 || !response.data) {
          return '/owner/onboarding';
        }
        // Préférences OK mais identité non vérifiée -> vérification KYC
        const { kycStatus } = authStore.getState().user ?? {};
        if (kycStatus !== 'VERIFIED') {
          return '/owner/verification';
        }
      } catch (err) {
        if (isAxiosError(err) && err.response?.status === 401) {
          logout();
          return '/login';
        }
        return '/';
      }
    }
    if (roles.includes('ROLE_TENANT')) {
      try {
        const response = await PreferencesService.getPreferences();
        if (response.status === 204 || !response.data) {
          return '/onboarding';
        }
      } catch (err) {
        if (isAxiosError(err) && err.response?.status === 401) {
          logout();
          return '/login';
        }
        return '/';
      }
    }
    return '/';
  };

  useEffect(() => {
    if (authStore.getState().isAuthenticated) {
      resolvePostLoginRoute().then((route) => navigate(route, { replace: true }));
    }
  }, [navigate]);

  const onSubmit = async (data: LoginFormData) => {
    setLoading(true);
    setError(null);
    try {
      await authStore.getState().login(data.email, data.password);
      await authStore.getState().loadUser();
      navigate(await resolvePostLoginRoute());
    } catch (err) {
      if (isAxiosError(err)) {
        if (err.response?.status === 401) {
          setError('Invalid email or password.');
        } else {
          setError('Login failed. Please try again.');
        }
      } else {
        setError('Login failed. Please try again.');
      }
    } finally {
      setLoading(false);
    }
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
      <div className="relative z-10 flex w-full max-w-[1100px] mx-auto px-4 sm:px-6 lg:px-8 gap-8 lg:gap-16 items-center">
        {/* Left: Welcome message */}
        <div className="hidden lg:flex flex-col flex-1 max-w-md">
          <p className="landing-eyebrow text-lavender-300/70 mb-5">Welcome back</p>
          <h1 className="font-display text-5xl font-normal leading-[1.08] tracking-[-0.02em] text-[#F6F2EC]" style={{ textShadow: '0 2px 24px rgba(0,0,0,0.3)' }}>
            Sign in to<br />
            your <em className="italic text-champagne-deep">home</em>.
          </h1>
          <p className="mt-6 text-base leading-relaxed text-[#B5ABC9] max-w-sm">
            Access your Nestora account, manage bookings, and discover properties with secure payments.
          </p>
        </div>

        {/* Right: Login form */}
        <div className="w-full max-w-md mx-auto lg:mx-0">
          {/* Mobile heading */}
          <div className="lg:hidden mb-8 text-center">
            <h1 className="font-display text-3xl font-normal leading-tight text-[#F6F2EC]">
              Welcome back
            </h1>
            <p className="mt-2 text-sm text-[#B5ABC9]">
              Sign in to your Nestora account
            </p>
          </div>

          {/* Form card */}
          <div className="rounded-2xl border border-white/[0.08] bg-white/[0.04] backdrop-blur-xl p-8 shadow-[0_32px_64px_-48px_rgba(0,0,0,0.5)]">
            {/* Card header */}
            <div className="mb-8">
              <h2 className="font-display text-2xl font-normal text-[#F6F2EC]">Login to Nestora</h2>
              <p className="mt-1.5 text-sm text-[#8D94A8]">Enter your credentials to continue</p>
            </div>

            {/* Error alert */}
            {error && (
              <div className="flex items-start gap-3 rounded-xl border border-red-500/20 bg-red-500/10 px-4 py-3 mb-6">
                <AlertCircle className="h-4 w-4 text-red-400 mt-0.5 shrink-0" />
                <p className="text-sm text-red-300">{error}</p>
              </div>
            )}

            {/* Form */}
            <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
              <div className="space-y-2">
                <Label htmlFor="email" className="text-[13px] font-medium text-[#D5CDE8]">Email</Label>
                <Input
                  id="email"
                  type="email"
                  {...register('email')}
                  placeholder="you@example.com"
                  className="h-11 rounded-xl border-white/[0.08] bg-white/[0.04] text-[#F6F2EC] placeholder:text-[#8D94A8] focus-visible:border-primary/50 focus-visible:ring-primary/20 focus-visible:ring-[3px]"
                />
                {errors.email && (
                  <p className="text-[13px] text-red-400">{errors.email.message}</p>
                )}
              </div>

              <div className="space-y-2">
                <Label htmlFor="password" className="text-[13px] font-medium text-[#D5CDE8]">Password</Label>
                <div className="relative">
                  <Input
                    id="password"
                    type={showPassword ? 'text' : 'password'}
                    {...register('password')}
                    placeholder="Enter your password"
                    className="h-11 rounded-xl border-white/[0.08] bg-white/[0.04] text-[#F6F2EC] placeholder:text-[#8D94A8] pr-11 focus-visible:border-primary/50 focus-visible:ring-primary/20 focus-visible:ring-[3px]"
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-[#8D94A8] hover:text-[#D5CDE8] transition-colors"
                    tabIndex={-1}
                  >
                    {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                  </button>
                </div>
                {errors.password && (
                  <p className="text-[13px] text-red-400">{errors.password.message}</p>
                )}
              </div>

              <Button
                type="submit"
                disabled={loading}
                className="w-full h-11 rounded-xl bg-primary text-primary-foreground font-semibold hover:bg-primary-hover transition-all duration-300 hover:-translate-y-px hover:shadow-[0_12px_26px_-14px_rgba(81,70,229,0.5)]"
              >
                {loading ? (
                  <span className="flex items-center gap-2">
                    <span className="h-4 w-4 border-2 border-primary-foreground/30 border-t-primary-foreground rounded-full animate-spin" />
                    Logging in…
                  </span>
                ) : (
                  <span className="flex items-center gap-2">
                    Login
                    <ArrowRight className="h-4 w-4" />
                  </span>
                )}
              </Button>
            </form>

            {/* Footer link */}
            <p className="mt-6 text-center text-sm text-[#8D94A8]">
              Don&apos;t have an account?{' '}
              <Link
                to="/register"
                className="font-semibold text-primary hover:text-primary-hover transition-colors"
              >
                Register
              </Link>
            </p>
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

export default Login;
