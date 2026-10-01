import { clsx, type ClassValue } from "clsx"
import { twMerge } from "tailwind-merge"

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

export function formatMad(mad: number): string {
  return mad.toLocaleString('fr-MA', { style: 'currency', currency: 'MAD' });
}
