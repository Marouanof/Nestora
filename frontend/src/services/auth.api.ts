import { api } from "@/lib/axios";

export interface RegisterData {
  email: string
  password: string
  confirmPassword: string
  firstName: string
  lastName: string
  phone: string
  acceptTerms: boolean
  role: "ROLE_TENANT" | "ROLE_OWNER"
}

export interface LoginData {
  email: string;
  password: string;
}

export interface UpdateProfileData {
  firstName?: string;
  lastName?: string;
  phone?: string;
  description?: string;
  dateNaissance?: string;
  country?: string;
  city?: string;
  accountType?: "INDIVIDUAL" | "COMPANY";
}

export const AuthService = {
  register(data: RegisterData) {
    return api.post("/auth/register", data)
  },

  login(email: string, password: string) {
    return api.post("/auth/login", { email, password })
  },

  refreshToken(refreshToken: string) {
    return api.post("/auth/refresh", { refreshToken })
  },

  logout(refreshToken?: string | null) {
    return api.post("/auth/logout", { refreshToken })
  },
  me() {
    return api.get("/users/me")
  },

  updateProfile(data: UpdateProfileData) {
    return api.put("/users/me", data)
  },

  verifyEmail(token: string) {
    return api.get("/auth/verify-email", { params: { token } })
  },

  resendVerification(email: string) {
    return api.post("/auth/resend-verification", null, { params: { email } })
  },
  ChangePassword(oldPassword: string, newPassword: string) {
    return api.post("/auth/change-password", { oldPassword, newPassword });
  }
};
