import { AuthService } from '@/services/auth.api';
import { create } from 'zustand';
import { persist } from 'zustand/middleware';

export interface User {
  email: string;
  firstName: string;
  lastName: string;
  role: "ROLE_TENANT" | "ROLE_OWNER" | "ROLE_ADMIN";
  roles?: Array<"ROLE_TENANT" | "ROLE_OWNER" | "ROLE_ADMIN">;
  enabled?: boolean;
  description?: string;
  dateNaissance?: string;
  phone?: string;
  country?: string;
  city?: string;
  accountType?: "INDIVIDUAL" | "COMPANY";
  photoUrl?: string;
  kycRectoUrl?: string;
  kycVersoUrl?: string;
  kycStatus?: "NOT_STARTED" | "PENDING" | "IN_REVIEW" | "VERIFIED" | "REJECTED" | "EXPIRED";
  kycVerified?: boolean;
  kycRejectionReason?: string;
}

interface AuthState {
  token: string | null;
  refreshToken?: string | null;
  isAuthenticated: boolean;
  user?: User;
  emailVerified?: boolean;
  login: (email: string, password: string) => Promise<void>;
  logout: () => void;
  loadUser: () => Promise<void>;
  refreshSession: () => Promise<boolean>;
  validateSession: () => Promise<boolean>;
}

interface ApiUserData {
  roles?: Array<string | User["role"]>;
  role?: User["role"];
}

// Backend returns `roles` as an array; keep a fallback to the legacy singular `role`.
export function getUserRoles(user?: User | null): Array<User["role"]> {
  if (Array.isArray(user?.roles) && user.roles.length > 0) {
    return user.roles;
  }
  return user?.role ? [user.role] : [];
}

function getPrimaryRole(data?: ApiUserData): User["role"] {
  if (Array.isArray(data?.roles) && data.roles.length > 0) {
    return data.roles[0] as User["role"];
  }
  return data?.role ?? 'ROLE_TENANT';
}

export const authStore = create<AuthState>()(
  persist(
    (set, get) => ({
      token: null,
      isAuthenticated: false,
      user: undefined,
      login: async (email, password) => {
        const response = await AuthService.login(email, password);
        const token = response.data.token;
        const refreshToken = response.data.refreshToken;
        const user = { email: response.data.email, firstName: response.data.firstName, lastName: response.data.lastName, role: getPrimaryRole(response.data), roles: response.data.roles };
        const emailVerified = response.data.emailVerified;
        set({ token, refreshToken, isAuthenticated: true, user, emailVerified });
      },
      
      logout: () => {
        // Révoque le refresh token côté serveur (best-effort) puis purge l'état local
        const refreshToken = get().refreshToken
        if (refreshToken) {
          AuthService.logout(refreshToken).catch(() => {})
        }
        set({ token: null, refreshToken: null, isAuthenticated: false, user: undefined , emailVerified: false});
      },
      loadUser: async () => {
        const response = await AuthService.me();
        const user = { 
          email: response.data.email, 
          firstName: response.data.firstName, 
          lastName: response.data.lastName, 
          role: getPrimaryRole(response.data),
          roles: response.data.roles, 
          enabled: response.data.enabled, 
          description: response.data.description, 
          dateNaissance: response.data.dateNaissance, 
          phone: response.data.phone, 
          country: response.data.country,
          city: response.data.city,
          accountType: response.data.accountType,
          photoUrl: response.data.photoUrl, 
          kycRectoUrl: response.data.kycRectoUrl,
          kycVersoUrl: response.data.kycVersoUrl,
          kycStatus: response.data.kycStatus,
          kycVerified: response.data.kycVerified,
          kycRejectionReason: response.data.rejectionReason };
        const emailVerified = response.data.emailVerified;
        set({ user, emailVerified });
      },
      validateSession: async () => {
        if (!get().token) {
          return false;
        }
        try {
          await AuthService.me();
          return true;
        } catch {
          get().logout();
          return false;
        }
      },
      // Régénère un JWT frais (rôles relus en base). À appeler après
      // tout changement de rôles (ex : becomeOwner) car les rôles sont
      // figés dans le JWT au login et propagés tels quels par la gateway.
      refreshSession: async () => {
        const refreshToken = get().refreshToken;
        if (!refreshToken) return false;
        try {
          const response = await AuthService.refreshToken(refreshToken);
          const token = response.data?.token as string | undefined;
          const newRefreshToken = response.data?.refreshToken as string | undefined;
          if (!token) return false;
          set({ token, refreshToken: newRefreshToken ?? null });
          return true;
        } catch {
          return false;
        }
      },
    }),
    {
      name: 'auth-storage',
      partialize: (state) => ({
        token: state.token,
        refreshToken: state.refreshToken,
        isAuthenticated: state.isAuthenticated,
        user: state.user,
        emailVerified: state.emailVerified,
      }),
    }
  )
);
