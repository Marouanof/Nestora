import { api } from "@/lib/axios";

export interface WishlistProperty {
  id: number;
  title: string;
  description: string;
  type: string;
  pricePerNight: number;
  status: string;
  amenities: string[];
}

export interface WishlistResponse {
  propertyIds: number[];
  properties: WishlistProperty[];
}

export const WishlistService = {
  getWishlist() {
    return api.get<WishlistResponse>("/users/me/wishlists");
  },
  add(propertyId: number) {
    return api.post<WishlistResponse>(`/users/me/wishlists/${propertyId}`);
  },
  remove(propertyId: number) {
    return api.delete(`/users/me/wishlists/${propertyId}`);
  },
  isLiked(propertyId: number) {
    return api.get<boolean>(`/users/me/wishlists/check/${propertyId}`);
  },
};
