import { clsx, type ClassValue } from "clsx"
import { twMerge } from "tailwind-merge"

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

export function formatUsd(usd: number): string {
  return usd.toLocaleString('en-US', { style: 'currency', currency: 'USD' });
}

export function formatMad(mad: number): string {
  return mad.toLocaleString('fr-MA', { style: 'currency', currency: 'MAD' });
}
