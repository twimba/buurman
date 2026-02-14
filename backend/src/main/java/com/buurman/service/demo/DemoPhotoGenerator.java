package com.buurman.service.demo;

import com.buurman.config.models.AwsS3Properties;
import com.buurman.util.EntityPrefix;
import com.buurman.util.UlidGenerator;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

import static com.buurman.jooq.generated.Tables.PHOTOS;

@Component
public class DemoPhotoGenerator {

    private static final Logger log = LoggerFactory.getLogger(DemoPhotoGenerator.class);

    private final DSLContext dsl;
    private final S3Client s3Client;
    private final String bucketName;

    private final Random random = new Random(42);

    // Picsum photo IDs mapped to property-type relevant photos
    // These are curated Picsum IDs that show buildings/architecture/interiors
    private static final int[] APARTMENT_PHOTO_IDS = {164, 188, 260, 274, 323, 357, 365, 399, 416, 429};
    private static final int[] HOUSE_PHOTO_IDS = {49, 134, 164, 188, 271, 323, 357, 365, 399, 416};
    private static final int[] STUDIO_PHOTO_IDS = {164, 188, 260, 274, 323, 357, 365, 399, 416, 429};
    private static final int[] COMMERCIAL_PHOTO_IDS = {164, 180, 260, 274, 323, 365, 399, 416, 429, 452};

    private static final int PHOTOS_PER_PROPERTY = 4;

    public DemoPhotoGenerator(DSLContext dsl, S3Client s3Client, AwsS3Properties s3Properties) {
        this.dsl = dsl;
        this.s3Client = s3Client;
        this.bucketName = s3Properties.bucketName();
    }

    public void generate(DemoDataContext ctx) {
        LocalDateTime now = LocalDateTime.now();
        int totalPhotos = 0;
        int photoSeed = 0;

        HttpClient httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        for (var teamEntry : ctx.getTeamIds().entrySet()) {
            String teamKey = teamEntry.getKey();
            UUID teamId = teamEntry.getValue();
            UUID uploadedBy = ctx.getAdminUserForTeam(teamKey);
            List<UUID> propertyIds = ctx.getPropertyIdsByTeam().get(teamId);

            if (propertyIds == null) continue;

            // Fetch property types for photo selection
            Map<UUID, String> propertyTypes = new HashMap<>();
            for (UUID propertyId : propertyIds) {
                var record = dsl.select(
                                com.buurman.jooq.generated.tables.Properties.PROPERTIES.PROPERTY_TYPE)
                        .from(com.buurman.jooq.generated.tables.Properties.PROPERTIES)
                        .where(com.buurman.jooq.generated.tables.Properties.PROPERTIES.ID.eq(propertyId))
                        .fetchOne();
                if (record != null) {
                    propertyTypes.put(propertyId, record.value1());
                }
            }

            for (UUID propertyId : propertyIds) {
                String propertyType = propertyTypes.getOrDefault(propertyId, "APARTMENT");
                int[] photoIds = getPhotoIdsForType(propertyType);

                for (int i = 0; i < PHOTOS_PER_PROPERTY; i++) {
                    photoSeed++;
                    int picId = photoIds[(photoSeed) % photoIds.length];
                    boolean isMainPhoto = (i == 0);

                    try {
                        byte[] imageData = downloadPhoto(httpClient, picId, photoSeed);
                        if (imageData == null || imageData.length == 0) {
                            log.warn("Empty response for photo seed={}, skipping", photoSeed);
                            continue;
                        }

                        String fileName = String.format("property_%s_photo_%d.jpg",
                                propertyType.toLowerCase(), i + 1);
                        String fileKey = String.format("%s/PROPERTY/%s/%s_%s",
                                teamId, propertyId, UUID.randomUUID(), fileName);

                        // Upload to S3
                        PutObjectRequest putRequest = PutObjectRequest.builder()
                                .bucket(bucketName)
                                .key(fileKey)
                                .contentType("image/jpeg")
                                .contentLength((long) imageData.length)
                                .build();

                        s3Client.putObject(putRequest, RequestBody.fromBytes(imageData));

                        // Create document record
                        String title = switch (i) {
                            case 0 -> "Front view";
                            case 1 -> "Living area";
                            case 2 -> "Kitchen";
                            case 3 -> "Bedroom";
                            default -> "Photo " + (i + 1);
                        };

                        dsl.insertInto(PHOTOS)
                                .set(PHOTOS.ID, UUID.randomUUID())
                                .set(PHOTOS.IDENTIFIER, UlidGenerator.generate(EntityPrefix.PHO))
                                .set(PHOTOS.TEAM_ID, teamId)
                                .set(PHOTOS.ENTITY_TYPE, "PROPERTY")
                                .set(PHOTOS.ENTITY_ID, propertyId)
                                .set(PHOTOS.FILE_KEY, fileKey)
                                .set(PHOTOS.FILE_NAME, fileName)
                                .set(PHOTOS.FILE_SIZE, (long) imageData.length)
                                .set(PHOTOS.MIME_TYPE, "image/jpeg")
                                .set(PHOTOS.TITLE, title)
                                .set(PHOTOS.IS_MAIN_PHOTO, isMainPhoto)
                                .set(PHOTOS.UPLOADED_BY, uploadedBy)
                                .set(PHOTOS.UPLOADED_AT, now)
                                .execute();

                        totalPhotos++;

                    } catch (Exception e) {
                        log.warn("Failed to download/upload photo for property {}, seed={}: {}",
                                propertyId, photoSeed, e.getMessage());
                    }
                }
            }

            log.info("Uploaded photos for {} properties in team {}", propertyIds.size(), teamKey);
        }

        log.info("Total property photos created: {}", totalPhotos);
    }

    private byte[] downloadPhoto(HttpClient httpClient, int picId, int seed) {
        // Use picsum.photos with specific ID for consistent architectural photos
        // 800x600 is a good resolution for property listings
        String url = String.format("https://picsum.photos/id/%d/800/600", picId);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();

            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() == 200) {
                return response.body();
            }

            // If specific ID fails, fall back to random seeded photo
            log.debug("Photo ID {} returned {}, falling back to seed-based", picId, response.statusCode());
            String fallbackUrl = String.format("https://picsum.photos/seed/property%d/800/600", seed);
            HttpRequest fallbackRequest = HttpRequest.newBuilder()
                    .uri(URI.create(fallbackUrl))
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();

            HttpResponse<byte[]> fallbackResponse = httpClient.send(fallbackRequest,
                    HttpResponse.BodyHandlers.ofByteArray());
            if (fallbackResponse.statusCode() == 200) {
                return fallbackResponse.body();
            }

            log.warn("Failed to download photo (status {})", fallbackResponse.statusCode());
            return null;
        } catch (IOException | InterruptedException e) {
            log.warn("Error downloading photo: {}", e.getMessage());
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return null;
        }
    }

    private int[] getPhotoIdsForType(String propertyType) {
        return switch (propertyType) {
            case "HOUSE" -> HOUSE_PHOTO_IDS;
            case "STUDIO" -> STUDIO_PHOTO_IDS;
            case "COMMERCIAL" -> COMMERCIAL_PHOTO_IDS;
            default -> APARTMENT_PHOTO_IDS;
        };
    }
}
