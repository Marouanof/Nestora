import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { isAxiosError } from 'axios';
import { Eye, EyeOff, ArrowLeft } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { AuthService } from '@/services/auth.api';

const registerSchema = z.object({
  email: z.string().email('Invalid email address'),
  password: z.string().min(6, 'Password must be at least 6 characters'),
  confirmPassword: z.string(),
  firstName: z.string().min(2, 'First name must be between 2 and 50 characters').max(50, 'First name must be between 2 and 50 characters'),
  lastName: z.string().min(2, 'Last name must be between 2 and 50 characters').max(50, 'Last name must be between 2 and 50 characters'),
  phone: z.string().regex(/^[+]?[0-9]{10,15}$/, 'Phone number should be valid (10-15 digits, optional +)'),
  acceptTerms: z.boolean().refine((val) => val === true, {
    message: 'You must accept the terms and conditions',
  }),
}).refine((data) => data.password === data.confirmPassword, {
  message: "Passwords don't match",
  path: ["confirmPassword"],
});

type RegisterFormData = z.infer<typeof registerSchema>;

interface RegisterFormProps {
  role: "ROLE_TENANT" | "ROLE_OWNER";
  onSuccess: (email: string) => void;
  onBack: () => void;
}

function RegisterForm({ role, onSuccess, onBack }: RegisterFormProps) {
  const { register, handleSubmit, formState: { errors } } = useForm<RegisterFormData>({
    resolver: zodResolver(registerSchema),
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [showPassword, setShowPassword] = useState(false);

  const onSubmit = async (data: RegisterFormData) => {
    setLoading(true);
    setError(null);
    try {
      await AuthService.register({ ...data, role });
      onSuccess(data.email);
    } catch (err) {
      if (isAxiosError(err)) {
        if (err.response?.status === 400) {
          const backend = err.response?.data as { message?: string; validationErrors?: Record<string, string> };
          const details = backend?.validationErrors ? ` (${Object.values(backend.validationErrors).join(', ')})` : '';
          setError(`Validation error. Please check your inputs.${details}`);
        } else if (err.response?.status === 409) {
          setError('Email already exists. Please use a different email.');
        } else {
          console.log(err);
          setError('Registration failed. Please try again.');
        }
      } else {
        setError('Registration failed. Please try again.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      {/* Header with back button + title */}
      <div className="flex items-center gap-4 mb-8">
        <button
          type="button"
          onClick={onBack}
          className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full border border-white/10 bg-white/5 text-[#B5ABC9] transition-colors hover:bg-white/10 hover:text-[#F6F2EC]"
        >
          <ArrowLeft className="h-4 w-4" />
        </button>
        <div>
          <h2 className="font-display text-2xl font-normal text-[#F6F2EC]">Create Your Account</h2>
          <p className="mt-0.5 text-sm text-[#8D94A8]">
            {role === 'ROLE_OWNER' ? 'List your properties on Nestora' : 'Start exploring properties'}
          </p>
        </div>
      </div>

      {error && (
        <div className="flex items-start gap-3 rounded-xl border border-red-500/20 bg-red-500/10 px-4 py-3 mb-6">
          <Alert className="border-0 bg-transparent p-0">
            <AlertDescription className="text-sm text-red-300">{error}</AlertDescription>
          </Alert>
        </div>
      )}

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
        {/* Identity */}
        <div>
          <h3 className="text-xs font-semibold uppercase tracking-[0.2em] text-[#C9BEFB] mb-4">Identity</h3>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="space-y-1.5">
              <Label htmlFor="firstName" className="text-[13px] font-medium text-[#D5CDE8]">First Name</Label>
              <Input
                id="firstName"
                {...register('firstName')}
                placeholder="John"
                className="h-11 rounded-xl border-white/[0.08] bg-white/[0.04] text-[#F6F2EC] placeholder:text-[#8D94A8] focus-visible:border-primary/50 focus-visible:ring-primary/20"
              />
              {errors.firstName && <p className="text-[13px] text-red-400">{errors.firstName.message}</p>}
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="lastName" className="text-[13px] font-medium text-[#D5CDE8]">Last Name</Label>
              <Input
                id="lastName"
                {...register('lastName')}
                placeholder="Doe"
                className="h-11 rounded-xl border-white/[0.08] bg-white/[0.04] text-[#F6F2EC] placeholder:text-[#8D94A8] focus-visible:border-primary/50 focus-visible:ring-primary/20"
              />
              {errors.lastName && <p className="text-[13px] text-red-400">{errors.lastName.message}</p>}
            </div>
          </div>
        </div>

        {/* Divider */}
        <div className="border-t border-white/[0.06]" />

        {/* Contact */}
        <div>
          <h3 className="text-xs font-semibold uppercase tracking-[0.2em] text-[#C9BEFB] mb-4">Contact</h3>
          <div className="space-y-4">
            <div className="space-y-1.5">
              <Label htmlFor="email" className="text-[13px] font-medium text-[#D5CDE8]">Email</Label>
              <Input
                id="email"
                type="email"
                {...register('email')}
                placeholder="you@example.com"
                className="h-11 rounded-xl border-white/[0.08] bg-white/[0.04] text-[#F6F2EC] placeholder:text-[#8D94A8] focus-visible:border-primary/50 focus-visible:ring-primary/20"
              />
              {errors.email && <p className="text-[13px] text-red-400">{errors.email.message}</p>}
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="phone" className="text-[13px] font-medium text-[#D5CDE8]">Phone</Label>
              <Input
                id="phone"
                {...register('phone')}
                placeholder="+1 234 567 890"
                className="h-11 rounded-xl border-white/[0.08] bg-white/[0.04] text-[#F6F2EC] placeholder:text-[#8D94A8] focus-visible:border-primary/50 focus-visible:ring-primary/20"
              />
              {errors.phone && <p className="text-[13px] text-red-400">{errors.phone.message}</p>}
            </div>
          </div>
        </div>

        {/* Divider */}
        <div className="border-t border-white/[0.06]" />

        {/* Security */}
        <div>
          <h3 className="text-xs font-semibold uppercase tracking-[0.2em] text-[#C9BEFB] mb-4">Security</h3>
          <div className="space-y-4">
            <div className="space-y-1.5">
              <Label htmlFor="password" className="text-[13px] font-medium text-[#D5CDE8]">Password</Label>
              <div className="relative">
                <Input
                  id="password"
                  type={showPassword ? "text" : "password"}
                  {...register('password')}
                  placeholder="At least 6 characters"
                  className="h-11 rounded-xl border-white/[0.08] bg-white/[0.04] text-[#F6F2EC] placeholder:text-[#8D94A8] pr-11 focus-visible:border-primary/50 focus-visible:ring-primary/20"
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
              {errors.password && <p className="text-[13px] text-red-400">{errors.password.message}</p>}
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="confirmPassword" className="text-[13px] font-medium text-[#D5CDE8]">Confirm Password</Label>
              <div className="relative">
                <Input
                  id="confirmPassword"
                  type={showPassword ? "text" : "password"}
                  {...register('confirmPassword')}
                  placeholder="Repeat your password"
                  className="h-11 rounded-xl border-white/[0.08] bg-white/[0.04] text-[#F6F2EC] placeholder:text-[#8D94A8] pr-11 focus-visible:border-primary/50 focus-visible:ring-primary/20"
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
              {errors.confirmPassword && <p className="text-[13px] text-red-400">{errors.confirmPassword.message}</p>}
            </div>
          </div>
        </div>

        {/* Divider */}
        <div className="border-t border-white/[0.06]" />

        {/* Consent */}
        <div className="flex items-start gap-3">
          <input
            id="acceptTerms"
            type="checkbox"
            {...register('acceptTerms')}
            className="mt-0.5 h-4 w-4 shrink-0 rounded border-white/20 bg-white/5 accent-primary"
          />
          <Label htmlFor="acceptTerms" className="text-[13px] text-[#B5ABC9] leading-relaxed cursor-pointer">
            I accept the{' '}
            <a href="#" className="font-medium text-[#C9BEFB] hover:text-[#F6F2EC] transition-colors underline underline-offset-2">Terms of Use</a>
            {' '}and the{' '}
            <a href="#" className="font-medium text-[#C9BEFB] hover:text-[#F6F2EC] transition-colors underline underline-offset-2">Privacy Policy</a>
          </Label>
        </div>
        {errors.acceptTerms && <p className="text-[13px] text-red-400 -mt-4">{errors.acceptTerms.message}</p>}

        {/* Submit */}
        <Button
          type="submit"
          disabled={loading}
          className="w-full h-12 rounded-xl bg-primary text-primary-foreground font-semibold text-[15px] hover:bg-primary-hover transition-all duration-300 hover:-translate-y-px hover:shadow-[0_12px_26px_-14px_rgba(81,70,229,0.5)]"
        >
          {loading ? (
            <span className="flex items-center gap-2">
              <span className="h-4 w-4 border-2 border-primary-foreground/30 border-t-primary-foreground rounded-full animate-spin" />
              Creating Account…
            </span>
          ) : (
            'Create my account'
          )}
        </Button>
      </form>
    </div>
  );
}

export default RegisterForm;
