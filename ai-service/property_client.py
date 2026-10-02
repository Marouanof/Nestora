"""
Client du Property Service.

Récupère les vraies propriétés actives depuis le property-service (REST)
afin que le service IA travaille sur des données réelles (prix MAD, villes
marocaines, coordonnées GPS) au lieu de données mock en dur.

Si le property-service est injoignable (ou en panne), on retombe sur un jeu
de données de secours minimal pour ne jamais faire échouer le démarrage.
"""

import logging
import os
from typing import Dict, List

import requests

from dotenv import load_dotenv

load_dotenv()

logger = logging.getLogger("property_client")

PROPERTY_SERVICE_URL = os.getenv("PROPERTY_SERVICE_URL", "http://localhost:8082").rstrip("/")
TIMEOUT = float(os.getenv("PROPERTY_SERVICE_TIMEOUT_SECONDS", "5"))

# --- Données de secours (utilisées uniquement si le property-service est injoignable) ---
FALLBACK_PROPERTIES: List[Dict] = [
    {"id": 1, "city": "Meknès", "lat": 33.8935, "lon": -5.5473, "pricePerNight": 200.0,
     "bedrooms": 2, "bathrooms": 1, "maxGuests": 4, "type": "APARTMENT", "amenities_count": 6},
    {"id": 2, "city": "Marrakech", "lat": 31.6295, "lon": -7.9811, "pricePerNight": 85.0,
     "bedrooms": 1, "bathrooms": 1, "maxGuests": 2, "type": "STUDIO", "amenities_count": 4},
    {"id": 3, "city": "Casablanca", "lat": 33.5731, "lon": -7.5898, "pricePerNight": 450.0,
     "bedrooms": 4, "bathrooms": 4, "maxGuests": 12, "type": "VILLA", "amenities_count": 10},
    {"id": 4, "city": "Agadir", "lat": 30.4278, "lon": -9.5981, "pricePerNight": 120.0,
     "bedrooms": 2, "bathrooms": 2, "maxGuests": 4, "type": "APARTMENT", "amenities_count": 7},
    {"id": 5, "city": "Fès", "lat": 34.0331, "lon": -5.0003, "pricePerNight": 150.0,
     "bedrooms": 3, "bathrooms": 3, "maxGuests": 6, "type": "HOUSE", "amenities_count": 8},
]


def _normalize_item(item: Dict) -> Dict:
    """Transforme une réponse du property-service en dict normalisé."""
    address = item.get("address") or {}
    amenities = item.get("amenities") or []
    return {
        "id": item.get("id"),
        "city": address.get("city") or item.get("city") or "Inconnu",
        "lat": address.get("latitude"),
        "lon": address.get("longitude"),
        "pricePerNight": float(item.get("pricePerNight") or 0.0),
        "bedrooms": int(item.get("bedrooms") or 1),
        "bathrooms": int(item.get("bathrooms") or 1),
        "maxGuests": int(item.get("maxGuests") or 2),
        "type": item.get("type") or "APARTMENT",
        "amenities_count": int(len(amenities) if amenities else 0),
    }


def fetch_properties() -> List[Dict]:
    """
    Récupère toutes les propriétés actives depuis le property-service.

    Le property-service pagine ; on boucle sur toutes les pages ('totalPages')
    pour récupérer l'ensemble du catalogue.
    """
    page = 0
    size = 100
    collected: List[Dict] = []

    try:
        while True:
            url = f"{PROPERTY_SERVICE_URL}/api/properties?page={page}&size={size}"
            resp = requests.get(url, timeout=TIMEOUT)
            resp.raise_for_status()
            body = resp.json()

            items = body.get("content") or []
            for it in items:
                collected.append(_normalize_item(it))

            total_pages = body.get("totalPages") or 1
            page += 1
            if page >= total_pages or not items:
                break

        if collected:
            logger.info("Loaded %d real properties from %s", len(collected), PROPERTY_SERVICE_URL)
            return collected

        logger.warning("Property service returned 0 properties; using fallback data")
    except Exception as exc:  # noqa: BLE001
        logger.warning("Could not fetch properties from %s (%s); using fallback data",
                       PROPERTY_SERVICE_URL, exc)

    return [dict(p) for p in FALLBACK_PROPERTIES]
