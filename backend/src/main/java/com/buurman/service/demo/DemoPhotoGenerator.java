package com.buurman.service.demo;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import com.buurman.domain.Photo;
import com.buurman.repository.PhotoRepository;
import com.buurman.service.S3StorageService;
import com.buurman.service.ThumbnailService;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class DemoPhotoGenerator {

  private static final String RESOURCE_BASE = "classpath:demo/photos/";

  private static final List<String> ALL_CATEGORIES =
      List.of(
          "exteriors",
          "living-rooms",
          "kitchens",
          "bathrooms",
          "bedrooms",
          "offices",
          "retail",
          "warehouses",
          "agricultural",
          "mixed-use");

  private final PhotoRepository photoRepository;
  private final S3StorageService s3StorageService;
  private final ThumbnailService thumbnailService;
  private final Random random = new Random(42);

  /** Photo pool loaded once from classpath, keyed by room category. */
  private final Map<String, List<byte[]>> photoPool;

  public DemoPhotoGenerator(
      PhotoRepository photoRepository,
      S3StorageService s3StorageService,
      ThumbnailService thumbnailService) {
    this.photoRepository = photoRepository;
    this.s3StorageService = s3StorageService;
    this.thumbnailService = thumbnailService;
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
      Optional<UUID> uploadedByOpt = ctx.getAdminUserForTeam(teamKey);
      if (uploadedByOpt.isEmpty()) {
        continue;
      }
      UUID uploadedBy = uploadedByOpt.get();
      List<UUID> propertyIds = ctx.getPropertyIdsByTeam().get(teamId);
      if (propertyIds == null) {
        continue;
      }

      for (UUID propertyId : propertyIds) {
        String category = ctx.getPropertyCategory(propertyId);
        totalPhotos +=
            generatePropertyPhotos(
                teamId,
                ctx.getIdentifier(teamId),
                propertyId,
                ctx.getIdentifier(propertyId),
                uploadedBy,
                category);
      }

      log.info("Uploaded photos for {} properties in team {}", propertyIds.size(), teamKey);
    }

    log.info("Total property photos created: {}", totalPhotos);
  }

  private int generatePropertyPhotos(
      UUID teamId,
      String teamIdentifier,
      UUID propertyId,
      String propertyIdentifier,
      UUID uploadedBy,
      String propertyCategory) {
    record PhotoSlot(String category, String title, boolean isMain) {}

    List<PhotoSlot> slots =
        switch (propertyCategory) {
          case "COMMERCIAL" ->
              List.of(
                  new PhotoSlot("offices", "Building exterior", true),
                  new PhotoSlot("offices", "Workspace", false),
                  new PhotoSlot("offices", "Meeting room", false));
          case "INDUSTRIAL" ->
              List.of(
                  new PhotoSlot("warehouses", "Exterior", true),
                  new PhotoSlot("warehouses", "Loading area", false),
                  new PhotoSlot("warehouses", "Storage space", false));
          case "AGRICULTURAL" ->
              List.of(
                  new PhotoSlot("agricultural", "Overview", true),
                  new PhotoSlot("agricultural", "Detail", false));
          case "MIXED_USE" ->
              List.of(
                  new PhotoSlot("mixed-use", "Street view", true),
                  new PhotoSlot("mixed-use", "Commercial space", false),
                  new PhotoSlot("living-rooms", "Residential unit", false));
          default ->
              List.of( // RESIDENTIAL
                  new PhotoSlot("exteriors", "Front view", true),
                  new PhotoSlot("living-rooms", "Living area", false),
                  new PhotoSlot("kitchens", "Kitchen", false),
                  new PhotoSlot(
                      random.nextBoolean() ? "bedrooms" : "bathrooms",
                      random.nextBoolean() ? "Bedroom" : "Bathroom",
                      false));
        };

    int count = 0;
    for (PhotoSlot slot : slots) {
      byte[] imageData = pickRandom(slot.category);
      if (imageData == null) {
        continue;
      }

      try {
        String fileName = slot.title.toLowerCase(Locale.ROOT).replace(" ", "-") + ".jpg";
        String fileKey =
            s3StorageService.uploadFile(
                imageData, "image/jpeg", teamIdentifier, "PROPERTY", propertyIdentifier, fileName);

        // Generate thumbnail
        String thumbnailFileKey = null;
        var thumbData = thumbnailService.generateThumbnail(new ByteArrayInputStream(imageData));
        if (thumbData.isPresent()) {
          thumbnailFileKey =
              s3StorageService.uploadFile(
                  thumbData.get(), "image/jpeg", S3StorageService.deriveThumbnailKey(fileKey));
        }

        Photo photo = new Photo();
        photo.setTeamId(teamId);
        photo.setEntityType("PROPERTY");
        photo.setEntityId(propertyId);
        photo.setFileKey(fileKey);
        photo.setThumbnailFileKey(Optional.ofNullable(thumbnailFileKey));
        photo.setFileName(fileName);
        photo.setFileSize((long) imageData.length);
        photo.setMimeType("image/jpeg");
        photo.setTitle(Optional.of(slot.title));
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

  @SuppressWarnings("NullAway")
  private byte @Nullable [] pickRandom(String category) {
    List<byte[]> pool = photoPool.get(category);
    if (pool == null || pool.isEmpty()) {
      pool = photoPool.values().stream().filter(l -> !l.isEmpty()).findFirst().orElse(null);
    }
    return pool != null ? pool.get(random.nextInt(pool.size())) : null;
  }

  private static Map<String, List<byte[]>> loadPhotoPool() {
    Map<String, List<byte[]>> pool = new LinkedHashMap<>();
    var resolver = new PathMatchingResourcePatternResolver();

    for (String category : ALL_CATEGORIES) {
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

    log.info(
        "Loaded {} demo photos from classpath ({})",
        pool.values().stream().mapToInt(List::size).sum(),
        String.join(", ", pool.keySet()));
    return pool;
  }
}
