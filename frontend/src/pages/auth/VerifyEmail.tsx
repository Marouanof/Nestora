import { useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { MailCheck } from 'lucide-react';
import { isAxiosError } from 'axios';
import { AuthService } from '@/services/auth.api';

interface VerifyEmailProps {
  email?: string;
}

function VerifyEmail({ email }: VerifyEmailProps) {
  const [resending, setResending] = useState(false);
  const [resent, setResent] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [cooldown, setCooldown] = useState(0);

  const handleResend = async () => {
    if (!email || resending || cooldown > 0) return;
    setResending(true);
    setError(null);
    try {
      await AuthService.resendVerification(email);
      setResent(true);
      setCooldown(60);
      const interval = setInterval(() => {
        setCooldown((c) => {
          if (c <= 1) clearInterval(interval);
          return c - 1;
        });
      }, 1000);
    } catch (err) {
      if (isAxiosError(err) && err.response?.status === 400) {
        setError('Please wait a moment before requesting a new email.');
      } else {
        setError('Failed to resend the email. Please try again.');
      }
    } finally {
      setResending(false);
    }
  };

  const handleOpenEmail = () => {
    window.open('https://mail.google.com', '_blank', 'noopener');
  };

  return (
    <div className="flex justify-center text-center">
      <Card className="w-full h-fit border-white/[0.08] bg-transparent shadow-none">
        <CardHeader>
          <div className="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-full bg-primary/15">
            <MailCheck className="h-7 w-7 text-primary" />
          </div>
          <CardTitle className="font-display text-2xl font-normal text-[#F6F2EC]">
            Check your email
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-6">
          <p className="text-sm leading-relaxed text-[#B5ABC9]">
            We've sent a verification link to{' '}
            <span className="font-medium text-[#F6F2EC]">{email || 'your email address'}</span>.
            Click the link to activate your account.
          </p>

          {error && <p className="text-[13px] text-red-400">{error}</p>}
          {resent && !error && (
            <p className="text-[13px] text-emerald-400">Verification email sent again.</p>
          )}

          <div className="space-y-3">
            <Button
              onClick={handleOpenEmail}
              className="w-full h-12 rounded-xl bg-primary text-primary-foreground font-semibold text-[15px] hover:bg-primary-hover transition-all duration-300 hover:-translate-y-px hover:shadow-[0_12px_26px_-14px_rgba(81,70,229,0.5)]"
            >
              Open my email
            </Button>
            <Button
              variant="outline"
              onClick={handleResend}
              disabled={resending || cooldown > 0}
              className="w-full h-11 rounded-xl border-white/10 bg-white/[0.04] text-[#D5CDE8] hover:bg-white/10 hover:text-[#F6F2EC]"
            >
              {resending
                ? 'Sending…'
                : cooldown > 0
                  ? `Resend email (${cooldown}s)`
                  : 'Resend email'}
            </Button>
          </div>

          <p className="text-xs text-[#8D94A8]">
            Didn't receive it? Check your spam folder or resend the email.
          </p>
        </CardContent>
      </Card>
    </div>
  );
}

export default VerifyEmail;
