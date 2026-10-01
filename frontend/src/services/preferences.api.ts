import { api } from "@/lib/axios";

export type PropertyTypePreference =
  | "APARTMENT"
  | "HOUSE"
  | "VILLA"
  | "CONDO"
  | "STUDIO"
  | "LOFT"
  | "TOWNHOUSE"
  | "BUNGALOW"
  | "CABIN"
  | "CASTLE"
  | "ROOM";

export type ListingTypePreference =
  | "APARTMENT"
  | "HOUSE"
  | "VILLA"
  | "CONDO"
  | "STUDIO"
  | "LOFT"
  | "TOWNHOUSE"
  | "BUNGALOW"
  | "CABIN"
  | "CASTLE"
  | "ROOM";

export type AccountType = "INDIVIDUAL" | "COMPANY";

export interface TenantPreferences {
  id?: number;
  userId?: number;
  city?: string;
  propertyTypes: PropertyTypePreference[];
  budgetMin?: number | null;
  budgetMax?: number | null;
}

export interface TenantPreferencesPayload {
  city?: string;
  propertyTypes: PropertyTypePreference[];
  budgetMin?: number | null;
  budgetMax?: number | null;
}

export const PreferencesService = {
  getPreferences() {
    return api.get<TenantPreferences>("/users/me/preferences");
  },

  savePreferences(data: TenantPreferencesPayload) {
    return api.put<TenantPreferences>("/users/me/preferences", data);
  },
};

export interface OwnerListingPreferences {
  id?: number;
  userId?: number;
  listingTypes: ListingTypePreference[];
}

export const OwnerOnboardingService = {
  getListingPreferences() {
    return api.get<OwnerListingPreferences>("/users/me/listing-preferences");
  },

  saveListingPreferences(listingTypes: ListingTypePreference[]) {
    return api.put<OwnerListingPreferences>("/users/me/listing-preferences", {
      listingTypes,
    });
  },

  updateAboutYou(data: { country?: string; city?: string; accountType?: AccountType }) {
    return api.put("/users/me", data);
  },

  startKycVerification() {
    return api.post("/users/me/kyc/start");
  },

  becomeOwner() {
    return api.post("/users/me/become-owner");
  },
};
