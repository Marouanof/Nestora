import { api } from '@/lib/axios';

export type AdminRole = 'ROLE_TENANT' | 'ROLE_OWNER' | 'ROLE_ADMIN';

export interface AdminUser {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  phone?: string;
  /** Backend `UserResponse.enabled` — `active` gardé en alias déprécié pour compat. */
  enabled: boolean;
  /** @deprecated Utiliser `enabled` (backend `UserResponse.enabled`). Renseigné par normalisation. */
  active?: boolean;
  roles: AdminRole[];
  role?: AdminRole;
  city?: string;
  country?: string;
  dateNaissance?: string;
  createdAt: string;
  emailVerified?: boolean;
  description?: string;
  photoUrl?: string;
  kycStatus?: string;
  kycVerified?: boolean;
  kycRectoUrl?: string;
  kycVersoUrl?: string;
  rejectionReason?: string;
}

/** Backend `UserResponse.enabled` — à utiliser pour badges/filtres statut. */
export function isUserActive(user?: Pick<AdminUser, 'enabled' | 'active'> | null): boolean {
  if (!user) return false;
  return user.enabled ?? user.active ?? false;
}

export function getPrimaryRole(user: { roles?: AdminRole[]; role?: AdminRole }): AdminRole | undefined {
  if (Array.isArray(user?.roles) && user.roles.length > 0) {
    return user.roles[0];
  }
  return user?.role;
}

/** Tous les rôles (un user peut être TENANT + OWNER). */
export function getUserRoles(user: { roles?: AdminRole[]; role?: AdminRole }): AdminRole[] {
  if (Array.isArray(user?.roles) && user.roles.length > 0) {
    return user.roles;
  }
  return user?.role ? [user.role] : [];
}

export interface CreateUserData {
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  role: AdminRole;
  country: string;
  city: string;
  dateNaissance: string;
  password: string;
}

export interface UpdateUserProfileData {
  firstName?: string;
  lastName?: string;
  description?: string;
  phone?: string;
  city?: string;
  country?: string;
  dateNaissance?: string;
  accountType?: 'INDIVIDUAL' | 'COMPANY';
}

export interface PaginatedResponse<T> {
  content: T[];
  /** Numéro de page Spring (`Page.number`), 0-indexé. */
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
  /** @deprecated Alias de `number` pour compat avec l'ancien code. */
  page?: number;
}

function normalizeUser(user: AdminUser): AdminUser {
  const enabled = (user as AdminUser).enabled ?? (user as unknown as { active?: boolean }).active ?? false;
  return { ...user, enabled, active: enabled };
}

function normalizePage<T>(page: PaginatedResponse<T> & { content: AdminUser[] } | PaginatedResponse<T>): PaginatedResponse<T> {
  const raw = page as PaginatedResponse<T> & { number?: number; page?: number };
  const number = raw.number ?? raw.page ?? 0;
  const content = Array.isArray(raw.content) && raw.content.length > 0 && 'enabled' in (raw.content[0] as object)
    ? (raw.content as AdminUser[]).map(normalizeUser) as unknown as T[]
    : raw.content;
  return { ...raw, content, number, page: number };
}

export const AdminUserService = {
  // List users with pagination
  listUsers(page = 0, size = 20): Promise<PaginatedResponse<AdminUser>> {
    return api.get(`/admin/users?page=${page}&size=${size}`).then(res => normalizePage<AdminUser>(res.data));
  },

  // Search users by keyword
  searchUsers(query: string, page = 0, size = 20): Promise<PaginatedResponse<AdminUser>> {
    return api.get(`/admin/users/search?query=${encodeURIComponent(query)}&page=${page}&size=${size}`).then(res => normalizePage<AdminUser>(res.data));
  },

  // Filter users by criteria (backend: role, city, country, kycStatus).
  // Le filtre `enabled` est appliqué côté client par le composant (voir AdminUsersOverview).
  filterUsers(filters: {
    role?: string;
    city?: string;
    country?: string;
    kycStatus?: string;
  }, page = 0, size = 20): Promise<PaginatedResponse<AdminUser>> {
    const params = new URLSearchParams({
      page: page.toString(),
      size: size.toString(),
      ...Object.fromEntries(
        Object.entries(filters).filter(([_, v]) => v !== undefined && v !== null && v !== '')
      ),
    });
    return api.get(`/admin/users?${params}`).then(res => normalizePage<AdminUser>(res.data));
  },

  // Get user by ID
  getUserById(id: number): Promise<AdminUser> {
    return api.get(`/admin/users/${id}`).then(res => normalizeUser(res.data));
  },

  // Create new user
  createUser(data: CreateUserData): Promise<AdminUser> {
    return api.post('/admin/users', data).then(res => res.data);
  },

  // Update user profile
  updateUserProfile(id: number, data: UpdateUserProfileData): Promise<AdminUser> {
    return api.put(`/admin/users/${id}/profile`, data).then(res => res.data);
  },

  // Enable user
  enableUser(id: number): Promise<void> {
    return api.put(`/admin/users/${id}/enable`);
  },

  // Disable user
  disableUser(id: number): Promise<void> {
    return api.put(`/admin/users/${id}/disable`);
  },

  // Change user role
  changeRole(id: number, newRole: AdminRole): Promise<void> {
    return api.put(`/admin/users/${id}/role?roles=${newRole}`);
  },

  // Force logout user
  forceLogout(id: number): Promise<void> {
    return api.post(`/admin/users/${id}/force-logout`);
  },

  // Approve KYC verification
  approveKyc(id: number): Promise<void> {
    return api.post(`/admin/users/${id}/kyc/approve`);
  },

  // Reject KYC verification
  rejectKyc(id: number, reason: string): Promise<void> {
    return api.post(`/admin/users/${id}/kyc/reject`, null, {
      params: { reason },
    });
  },

  // Delete user
  deleteUser(id: number): Promise<void> {
    return api.delete(`/admin/users/${id}`);
  },
};