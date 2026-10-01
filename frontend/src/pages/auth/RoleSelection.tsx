import { Button } from '@/components/ui/button';
import tenantImg from '@/assets/tenant.png';
import ownerImg from '@/assets/owner.png';

interface RoleSelectionProps {
  selectedRole: "ROLE_TENANT" | "ROLE_OWNER" | null;
  onSelectRole: (role: "ROLE_TENANT" | "ROLE_OWNER") => void;
  onContinue: () => void;
}

function RoleSelection({ selectedRole, onSelectRole, onContinue }: RoleSelectionProps) {
  return (
    <div className="flex flex-col items-center space-y-6">
      <div className="flex sm:flex-row flex-col gap-4 w-full">
        {/* Tenant Card */}
        <button
          type="button"
          onClick={() => onSelectRole('ROLE_TENANT')}
          className={`group relative flex-1 overflow-hidden rounded-2xl border-2 transition-all duration-300 ${
            selectedRole === 'ROLE_TENANT'
              ? 'border-primary ring-2 ring-primary/20 bg-primary/10'
              : 'border-white/10 bg-white/[0.03] hover:border-white/20 hover:bg-white/[0.06]'
          }`}
        >
          <div className="relative h-48 overflow-hidden">
            <img
              src={tenantImg}
              alt="Tenant"
              className="h-full w-full object-cover transition-transform duration-500 group-hover:scale-105"
            />
            <div className="absolute inset-0 bg-gradient-to-t from-[#0D0B26]/80 via-[#0D0B26]/20 to-transparent" />
          </div>
          <div className="p-5">
            <h3 className="font-display text-xl font-normal text-[#F6F2EC]">I am a Tenant</h3>
            <p className="mt-1.5 text-sm text-[#B5ABC9]">I am looking to rent a property.</p>
          </div>
        </button>

        {/* Owner Card */}
        <button
          type="button"
          onClick={() => onSelectRole('ROLE_OWNER')}
          className={`group relative flex-1 overflow-hidden rounded-2xl border-2 transition-all duration-300 ${
            selectedRole === 'ROLE_OWNER'
              ? 'border-primary ring-2 ring-primary/20 bg-primary/10'
              : 'border-white/10 bg-white/[0.03] hover:border-white/20 hover:bg-white/[0.06]'
          }`}
        >
          <div className="relative h-48 overflow-hidden">
            <img
              src={ownerImg}
              alt="Owner"
              className="h-full w-full object-cover transition-transform duration-500 group-hover:scale-105"
            />
            <div className="absolute inset-0 bg-gradient-to-t from-[#0D0B26]/80 via-[#0D0B26]/20 to-transparent" />
          </div>
          <div className="p-5">
            <h3 className="font-display text-xl font-normal text-[#F6F2EC]">I am an Owner</h3>
            <p className="mt-1.5 text-sm text-[#B5ABC9]">I own properties to rent out.</p>
          </div>
        </button>
      </div>

      <Button
        onClick={onContinue}
        disabled={!selectedRole}
        className="w-full h-12 rounded-xl bg-primary text-primary-foreground font-semibold hover:bg-primary-hover transition-all duration-300 hover:-translate-y-px hover:shadow-[0_12px_26px_-14px_rgba(81,70,229,0.5)] disabled:opacity-40"
      >
        Continue
      </Button>
    </div>
  );
}

export default RoleSelection;
