package com.propertyservice.propertyservice.seed;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.propertyservice.propertyservice.entity.Address;
import com.propertyservice.propertyservice.entity.Property;
import com.propertyservice.propertyservice.entity.PropertyImage;
import com.propertyservice.propertyservice.enu.ListingStatus;
import com.propertyservice.propertyservice.enu.PropertyType;
import com.propertyservice.propertyservice.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Seed de démo : 24 biens rédigés + ~100 annonces réelles (DeRent5 Airbnb
 * Maroc, seed/real-listings.json), ACTIVE avec vraies photos, pour les tests.
 *
 * Actif UNIQUEMENT avec le profil Spring {@code seeder} :
 * <pre>
 *   java -jar target/property-service-*.jar --spring.profiles.active=local,seeder
 * </pre>
 * Idempotent et additif : ne crée que les biens du seed dont le titre
 * n'existe pas encore (ne touche jamais aux biens créés manuellement).
 * Désactivable via {@code --app.seed.enabled=false}.
 *
 * Les photos sources (libres de droits) sont dans
 * {@code src/main/resources/seed-images/} (voir SOURCES.md). Elles sont
 * copiées vers {@code uploads/properties/} et exposées via
 * {@code {gatewayUrl}/files/properties/...} (route gateway déjà en place).
 */
@Slf4j
@Component
@Profile("seeder")
@Order(Ordered.LOWEST_PRECEDENCE)
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class PropertySeedRunner implements ApplicationRunner {

    private final PropertyRepository propertyRepository;
    private final com.propertyservice.propertyservice.service.AvailabilityService availabilityService;

    @Value("${app.file.upload-dir:uploads}")
    private String uploadDir;

    @Value("${app.gateway.url:http://localhost:8080}")
    private String gatewayUrl;

    private static final List<String> COVERS = List.of(
            "mar-01", "ext-04", "ext-09",
            "ext-01", "ext-10", "ext-06", "pex-01",
            "ext-02", "ext-11", "liv-04", "pex-04",
            "ext-05", "pex-02", "liv-01", "ext-03",
            "ext-07", "pex-03", "liv-02",
            "mar-02", "ext-12", "liv-03",
            "ext-08", "liv-05", "pex-05");

    private static final List<String> LIV = List.of(
            "liv-01", "liv-02", "liv-03", "liv-04", "liv-05", "liv-06",
            "liv-07", "liv-08", "liv-09", "liv-10", "liv-11", "liv-12");
    private static final List<String> BED = List.of("bed-01", "bed-02", "bed-03", "bed-04", "bed-05");
    private static final List<String> KIT = List.of("kit-01", "kit-02", "kit-03");
    private static final List<String> BAT = List.of("bat-01", "bat-02");

    private record SeedProp(String title, String description, PropertyType type,
                            String street, String city, double lat, double lng,
                            int priceMad, int guests, int bedrooms, int bathrooms,
                            String[] amenities) {
    }

    private static final List<SeedProp> PROPS = List.of(
            new SeedProp("Riad Jasmin — patio & piscine", "Riad authentique rénové au cœur de la médina, patio ombragé et piscine.",
                    PropertyType.HOUSE, "12 Derb El Ferrane, Médina", "Marrakech", 31.6295, -7.9811, 950, 6, 3, 2,
                    new String[]{"Wifi", "Piscine", "Climatisation", "Petit-déjeuner", "Terrasse"}),
            new SeedProp("Riad Zitoune — suite avec terrasse", "Suite calme avec terrasse panoramique sur les toits de la médina.",
                    PropertyType.ROOM, "25 Rue Zitoune, Médina", "Marrakech", 31.6310, -7.9840, 480, 2, 1, 1,
                    new String[]{"Wifi", "Climatisation", "Terrasse", "Petit-déjeuner"}),
            new SeedProp("Villa Palmeraie 4 chambres", "Villa avec jardin et piscine dans la Palmeraie, idéale en famille.",
                    PropertyType.VILLA, "Route de la Palmeraie", "Marrakech", 31.6520, -7.9850, 2200, 8, 4, 3,
                    new String[]{"Wifi", "Piscine", "Parking", "Jardin", "Climatisation", "Cuisine équipée"}),
            new SeedProp("Appartement Marina — vue mer", "Bel appartement lumineux face à la marina, 2 chambres.",
                    PropertyType.APARTMENT, "Boulevard de la Marina", "Casablanca", 33.5731, -7.5898, 750, 4, 2, 2,
                    new String[]{"Wifi", "Vue mer", "Climatisation", "Ascenseur", "Parking"}),
            new SeedProp("Studio Gauthier meublé", "Studio moderne meublé au quartier Gauthier, proche tram.",
                    PropertyType.STUDIO, "Rue Gauthier", "Casablanca", 33.5900, -7.6300, 380, 2, 1, 1,
                    new String[]{"Wifi", "Climatisation", "Machine à laver", "Cuisine équipée"}),
            new SeedProp("Appartement Maârif 3 chambres", "Spacieux appartement familial au Maârif, proche commerces.",
                    PropertyType.APARTMENT, "Rue du Maârif", "Casablanca", 33.5850, -7.6200, 650, 6, 3, 2,
                    new String[]{"Wifi", "Parking", "Ascenseur", "Balcon", "Climatisation"}),
            new SeedProp("Loft Ain Diab — front de mer", "Loft ouvert avec grande baie vitrée face à l'océan.",
                    PropertyType.LOFT, "Corniche Ain Diab", "Casablanca", 33.5600, -7.6700, 1100, 4, 2, 2,
                    new String[]{"Wifi", "Vue mer", "Climatisation", "Parking", "Terrasse"}),
            new SeedProp("Appartement Agdal proche gare", "Appartement fonctionnel à Agdal, 5 min de la gare Rabat-Agdal.",
                    PropertyType.APARTMENT, "Avenue de France, Agdal", "Rabat", 34.0209, -6.8416, 550, 4, 2, 1,
                    new String[]{"Wifi", "Ascenseur", "Climatisation", "Parking"}),
            new SeedProp("Studio Océan — vue mer", "Petit studio avec vue océan au quartier de l'Océan.",
                    PropertyType.STUDIO, "Rue de l'Océan", "Rabat", 34.0300, -6.8500, 320, 2, 1, 1,
                    new String[]{"Wifi", "Vue mer", "Cuisine équipée"}),
            new SeedProp("Appartement Souissi standing", "Appartement standing au quartier Souissi, calme et verdoyant.",
                    PropertyType.APARTMENT, "Avenue Souissi", "Rabat", 34.0000, -6.8300, 800, 5, 3, 2,
                    new String[]{"Wifi", "Parking", "Jardin", "Climatisation", "Gardien"}),
            new SeedProp("Studio Hay Riad neuf", "Studio neuf dans résidence sécurisée à Hay Riad.",
                    PropertyType.STUDIO, "Hay Riad", "Rabat", 33.9900, -6.8600, 350, 2, 1, 1,
                    new String[]{"Wifi", "Résidence sécurisée", "Parking", "Climatisation"}),
            new SeedProp("Appartement Founty — vue mer", "Appartement avec terrasse vue mer à Founty.",
                    PropertyType.APARTMENT, "Cité Founty", "Agadir", 30.4278, -9.5981, 600, 4, 2, 2,
                    new String[]{"Wifi", "Vue mer", "Piscine", "Climatisation", "Parking"}),
            new SeedProp("Appartement Hay Essalam familial", "Grand appartement familial proche de la plage.",
                    PropertyType.APARTMENT, "Hay Essalam", "Agadir", 30.4200, -9.5800, 450, 6, 3, 2,
                    new String[]{"Wifi", "Balcon", "Parking", "Climatisation"}),
            new SeedProp("Studio Tamraght surf", "Studio simple à 5 min des spots de surf de Tamraght.",
                    PropertyType.STUDIO, "Tamraght", "Agadir", 30.5400, -9.7100, 280, 2, 1, 1,
                    new String[]{"Wifi", "Terrasse", "Proche plage"}),
            new SeedProp("Villa Tikiouine avec piscine", "Villa avec piscine privée, idéale pour les groupes.",
                    PropertyType.VILLA, "Tikiouine", "Agadir", 30.4300, -9.5600, 1600, 8, 4, 3,
                    new String[]{"Wifi", "Piscine privée", "Parking", "Jardin", "Climatisation"}),
            new SeedProp("Appartement centre — vue détroit", "Appartement au centre avec vue sur le détroit par temps clair.",
                    PropertyType.APARTMENT, "Boulevard Mohammed V", "Tanger", 35.7595, -5.8340, 500, 4, 2, 1,
                    new String[]{"Wifi", "Ascenseur", "Climatisation", "Balcon"}),
            new SeedProp("Studio Marshan calme", "Studio calme au Marshan, proche de la grotte d'Hercule.",
                    PropertyType.STUDIO, "Le Marshan", "Tanger", 35.7700, -5.8400, 300, 2, 1, 1,
                    new String[]{"Wifi", "Calme", "Cuisine équipée"}),
            new SeedProp("Villa Californie Tanger", "Belle villa au quartier Californie avec jardin.",
                    PropertyType.VILLA, "Californie", "Tanger", 35.7500, -5.8200, 1900, 8, 4, 3,
                    new String[]{"Wifi", "Piscine", "Jardin", "Parking", "Climatisation"}),
            new SeedProp("Dar Fès — maison d'hôtes médina", "Maison traditionnelle avec patio dans la médina de Fès.",
                    PropertyType.HOUSE, "Derb Bouzid, Fès El Bali", "Fès", 34.0181, -5.0078, 420, 5, 3, 2,
                    new String[]{"Wifi", "Patio", "Petit-déjeuner", "Terrasse"}),
            new SeedProp("Appartement Atlas neuf", "Appartement neuf lumineux, proche de toutes commodités.",
                    PropertyType.APARTMENT, "Avenue Atlas", "Fès", 34.0500, -5.0000, 380, 4, 2, 1,
                    new String[]{"Wifi", "Ascenseur", "Parking", "Balcon"}),
            new SeedProp("Appartement médina Essaouira", "Appartement de charme dans la médina, 3 min de la plage.",
                    PropertyType.APARTMENT, "Rue Laâloudj, Médina", "Essaouira", 31.5085, -9.7574, 520, 4, 2, 1,
                    new String[]{"Wifi", "Terrasse", "Proche plage", "Climatisation"}),
            new SeedProp("Studio port de pêche", "Studio avec vue sur le port de pêche et l'océan.",
                    PropertyType.STUDIO, "Quartier du port", "Essaouira", 31.5100, -9.7700, 300, 2, 1, 1,
                    new String[]{"Wifi", "Vue mer", "Terrasse"}),
            new SeedProp("Maison bleue Chefchaouen", "Maison typique dans la médina bleue, terrasse avec vue montagne.",
                    PropertyType.HOUSE, "Ras El Ma, Médina", "Chefchaouen", 35.1688, -5.2636, 350, 4, 2, 1,
                    new String[]{"Wifi", "Terrasse", "Vue montagne", "Petit-déjeuner"}),
            new SeedProp("Chambreriad — nuit chez l'habitant", "Chambre confortable chez l'habitant, accueil chaleureux.",
                    PropertyType.ROOM, "Avenue Mohammed V", "Chefchaouen", 35.1700, -5.2600, 180, 2, 1, 1,
                    new String[]{"Wifi", "Petit-déjeuner", "Terrasse"}));

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        try {
            Path targetDir = Paths.get(uploadDir).toAbsolutePath().normalize().resolve("properties");
            Files.createDirectories(targetDir);

            log.info("Seed : vérification des {} biens de démo (création + disponibilités manquantes)", PROPS.size());

            int created = 0;
            for (int i = 0; i < PROPS.size(); i++) {
                SeedProp def = PROPS.get(i);
                Property existing = propertyRepository.findByTitle(def.title()).orElse(null);
                if (existing != null) {
                    // Backfill : génère les disponibilités manquantes (idempotent)
                    availabilityService.generateAvailabilityForYear(existing.getId());
                    continue;
                }
                Property property = Property.builder()
                        .title(def.title())
                        .description(def.description())
                        .type(def.type())
                        .address(new Address(def.street(), def.city(), null, null, "Maroc",
                                def.lat() + (i % 5) * 0.0012, def.lng() + (i % 7) * 0.0012))
                        .pricePerNight(BigDecimal.valueOf(def.priceMad()))
                        .securityDeposit(BigDecimal.valueOf(1000))
                        .maxGuests(def.guests())
                        .bedrooms(def.bedrooms())
                        .bathrooms(def.bathrooms())
                        .ownerId(1L + (i % 3))
                        .status(ListingStatus.ACTIVE)
                        .minStayNights(1)
                        .cancellationPolicyDays(7)
                        .amenities(new ArrayList<>(Arrays.asList(def.amenities())))
                        .instantBookable(i % 2 == 0)
                        .build();

                List<String> keys = List.of(
                        COVERS.get(i % COVERS.size()),
                        LIV.get(i % LIV.size()),
                        BED.get((i * 2 + 1) % BED.size()),
                        (i % 2 == 0) ? KIT.get(i % KIT.size()) : BAT.get(i % BAT.size()));

                List<PropertyImage> images = new ArrayList<>();
                for (int order = 0; order < keys.size(); order++) {
                    String imageUrl = copySeedImage(keys.get(order), targetDir);
                    images.add(PropertyImage.builder()
                            .imageUrl(imageUrl)
                            .caption(captionFor(keys.get(order)))
                            .displayOrder(order)
                            .property(property)
                            .build());
                }
                property.setImages(images);
                propertyRepository.save(property);
                // Disponibilités 365 j comme lors d'une approbation admin
                // (sinon invisible en recherche par dates)
                availabilityService.generateAvailabilityForYear(property.getId());
                created++;
                log.info("Seed : [{}] {} ({} MAD/nuit, {} photos)",
                        property.getId(), def.title(), def.priceMad(), images.size());
            }
            log.info("Seed terminé : {} bien(s) ACTIVE créé(s) avec photos", created);

            // Annonces réelles (DeRent5 Airbnb Maroc) : seed/real-listings.json
            int createdReal = seedRealListings(targetDir, PROPS.size());
            log.info("Seed réel terminé : {} bien(s) ACTIVE créé(s)", createdReal);
        } catch (Exception e) {
            log.error("Échec du seed de démo", e);
            throw new RuntimeException("Échec du seed de démo", e);
        }
    }

    /**
     * Charge les annonces réelles échantillonnées (titres Airbnb, prix/ville/GPS
     * réels). Idempotent comme le reste : ne crée que les titres absents.
     */
    @SuppressWarnings("unchecked")
    private int seedRealListings(Path targetDir, int indexOffset) throws Exception {
        ClassPathResource json = new ClassPathResource("seed/real-listings.json");
        if (!json.exists()) {
            log.warn("Seed réel ignoré : seed/real-listings.json absent du classpath");
            return 0;
        }
        List<Map<String, Object>> defs;
        try (InputStream in = json.getInputStream()) {
            defs = new ObjectMapper().readValue(in, new TypeReference<List<Map<String, Object>>>() {
            });
        }
        int created = 0;
        for (int i = 0; i < defs.size(); i++) {
            Map<String, Object> def = defs.get(i);
            String title = String.valueOf(def.get("title"));
            Property existingReal = propertyRepository.findByTitle(title).orElse(null);
            if (existingReal != null) {
                // Backfill : disponibilités manquantes (ex : seed interrompu)
                availabilityService.generateAvailabilityForYear(existingReal.getId());
                continue;
            }
            PropertyType type;
            try {
                type = PropertyType.valueOf(String.valueOf(def.get("type")));
            } catch (Exception e) {
                type = PropertyType.APARTMENT;
            }
            int idx = indexOffset + i;
            Property property = Property.builder()
                    .title(title)
                    .description(String.valueOf(def.get("description")))
                    .type(type)
                    .address(new Address(
                            String.valueOf(def.get("street")),
                            String.valueOf(def.get("city")), null, null, "Maroc",
                            toDouble(def.get("lat"), 33.0), toDouble(def.get("lng"), -7.0)))
                    .pricePerNight(BigDecimal.valueOf(toInt(def.get("priceMad"), 500)))
                    .securityDeposit(BigDecimal.valueOf(1000))
                    .maxGuests(toInt(def.get("guests"), 2))
                    .bedrooms(toInt(def.get("bedrooms"), 1))
                    .bathrooms(toInt(def.get("bathrooms"), 1))
                    .ownerId(1L + (idx % 3))
                    .status(ListingStatus.ACTIVE)
                    .minStayNights(1)
                    .cancellationPolicyDays(7)
                    .amenities(new ArrayList<>((List<String>) def.getOrDefault("amenities", List.of("Wifi"))))
                    .instantBookable(idx % 2 == 0)
                    .build();

            List<String> keys = List.of(
                    COVERS.get(idx % COVERS.size()),
                    LIV.get(idx % LIV.size()),
                    BED.get((idx * 2 + 1) % BED.size()),
                    (idx % 2 == 0) ? KIT.get(idx % KIT.size()) : BAT.get(idx % BAT.size()));

            List<PropertyImage> images = new ArrayList<>();
            for (int order = 0; order < keys.size(); order++) {
                String imageUrl = copySeedImage(keys.get(order), targetDir);
                images.add(PropertyImage.builder()
                        .imageUrl(imageUrl)
                        .caption(captionFor(keys.get(order)))
                        .displayOrder(order)
                        .property(property)
                        .build());
            }
            property.setImages(images);
            propertyRepository.save(property);
            availabilityService.generateAvailabilityForYear(property.getId());
            created++;
        }
        return created;
    }

    private static int toInt(Object value, int fallback) {
        if (value instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception e) {
            return fallback;
        }
    }

    private static double toDouble(Object value, double fallback) {
        if (value instanceof Number n) {
            return n.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception e) {
            return fallback;
        }
    }

    private String copySeedImage(String key, Path targetDir) throws Exception {
        ClassPathResource resource = new ClassPathResource("seed-images/" + key + ".jpg");
        String fileName = UUID.randomUUID() + ".jpg";
        Path target = targetDir.resolve(fileName);
        try (InputStream in = resource.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return gatewayUrl + "/files/properties/" + fileName;
    }

    private static String captionFor(String key) {
        if (key.startsWith("bed")) {
            return "Chambre confortable";
        }
        if (key.startsWith("kit")) {
            return "Cuisine équipée";
        }
        if (key.startsWith("bat")) {
            return "Salle de bain moderne";
        }
        if (key.startsWith("liv") || key.startsWith("pex-04") || key.startsWith("pex-05")) {
            return "Salon lumineux";
        }
        return "Vue extérieure";
    }
}
