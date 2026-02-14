package com.buurman.service.demo;

import com.buurman.domain.Photo;
import com.buurman.repository.PhotoRepository;
import com.buurman.service.S3StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

@Component
public class DemoPhotoGenerator {

    private static final Logger log = LoggerFactory.getLogger(DemoPhotoGenerator.class);
    private static final String RESOURCE_BASE = "classpath:demo/photos/";

    private final PhotoRepository photoRepository;
    private final S3StorageService s3StorageService;
    private final Random random = new Random(42);

    /** Photo pool loaded once from classpath, keyed by room category. */
    private final Map<String, List<byte[]>> photoPool;

    public DemoPhotoGenerator(PhotoRepository photoRepository, S3StorageService s3StorageService) {
        this.photoRepository = photoRepository;
        this.s3StorageService = s3StorageService;
        this.photoPool = loadPhotoPool();
    }

    public void generate(DemoDataContext ctx) {
        if (photoPool.isEmpty()) {
            log.warn("No demo photos found on classpath — skipping photo generation");
            return;
        }

        int totalPhotos = 0;

        for (var teamEntry : ctx.getTeamIds().entrySet()) {
            String teamKey = teamEntry.getKey();
            UUID teamId = teamEntry.getValue();
            UUID uploadedBy = ctx.getAdminUserForTeam(teamKey);
            List<UUID> propertyIds = ctx.getPropertyIdsByTeam().get(teamId);
            if (propertyIds == null) continue;

            for (UUID propertyId : propertyIds) {
                totalPhotos += generatePropertyPhotos(teamId, propertyId, uploadedBy);
            }

            log.info("Uploaded photos for {} properties in team {}", propertyIds.size(), teamKey);
        }

        log.info("Total property photos created: {}", totalPhotos);
    }

    private int generatePropertyPhotos(UUID teamId, UUID propertyId, UUID uploadedBy) {
        record PhotoSlot(String category, String title, boolean isMain) {}

        List<PhotoSlot> slots = List.of(
                new PhotoSlot("exteriors", "Front view", true),
                new PhotoSlot("living-rooms", "Living area", false),
                new PhotoSlot("kitchens", "Kitchen", false),
                new PhotoSlot(random.nextBoolean() ? "bedrooms" : "bathrooms",
                        random.nextBoolean() ? "Bedroom" : "Bathroom", false)
        );

        int count = 0;
        for (PhotoSlot slot : slots) {
            byte[] imageData = pickRandom(slot.category);
            if (imageData == null) continue;

            try {
                String fileName = slot.title.toLowerCase().replace(" ", "-") + ".jpg";
                String fileKey = s3StorageService.uploadFile(
                        imageData, "image/jpeg", teamId, "PROPERTY", propertyId, fileName);

                Photo photo = new Photo();
                photo.setTeamId(teamId);
                photo.setEntityType("PROPERTY");
                photo.setEntityId(propertyId);
                photo.setFileKey(fileKey);
                photo.setFileName(fileName);
                photo.setFileSize((long) imageData.length);
                photo.setMimeType("image/jpeg");
                photo.setTitle(slot.title);
                photo.setIsMainPhoto(slot.isMain);
                photo.setUploadedBy(uploadedBy);

                photoRepository.save(photo);
                count++;
            } catch (Exception e) {
                log.warn("Failed to upload photo for property {}: {}", propertyId, e.getMessage());
            }
        }
        return count;
    }

    private byte[] pickRandom(String category) {
        List<byte[]> pool = photoPool.get(category);
        if (pool == null || pool.isEmpty()) {
            pool = photoPool.values().stream()
                    .filter(l -> !l.isEmpty())
                    .findFirst()
                    .orElse(null);
        }
        return pool != null ? pool.get(random.nextInt(pool.size())) : null;
    }

    private static Map<String, List<byte[]>> loadPhotoPool() {
        Map<String, List<byte[]>> pool = new LinkedHashMap<>();
        var resolver = new PathMatchingResourcePatternResolver();

        for (String category : List.of("exteriors", "living-rooms", "kitchens", "bathrooms", "bedrooms")) {
            List<byte[]> images = new ArrayList<>();
            try {
                Resource[] resources = resolver.getResources(RESOURCE_BASE + category + "/*.jpg");
                for (Resource resource : resources) {
                    images.add(resource.getContentAsByteArray());
                }
            } catch (IOException e) {
                // Category not found — skip
            }
            if (!images.isEmpty()) {
                pool.put(category, images);
            }
        }

        log.info("Loaded {} demo photos from classpath ({})",
                pool.values().stream().mapToInt(List::size).sum(),
                String.join(", ", pool.keySet()));
        return pool;
    }
}
