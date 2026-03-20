package com.buurman.service.demo;

import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import javax.imageio.ImageIO;

import org.jspecify.annotations.Nullable;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import com.buurman.domain.Photo;
import com.buurman.repository.PhotoRepository;
import com.buurman.service.S3StorageService;

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
  private final Random random = new Random(42);

  private record PhotoFile(byte[] data, String mimeType) {}

  /** Photo pool loaded once from classpath, keyed by room category. */
  private final Map<String, List<PhotoFile>> photoPool;

  /**
   * Minimum number of unique images per category. If fewer than this exist on classpath, generated
   * placeholders fill the gap. With 20 real photos bundled per category, this is a safety net.
   */
  private static final int MIN_IMAGES_PER_CATEGORY = 20;

  public DemoPhotoGenerator(PhotoRepository photoRepository, S3StorageService s3StorageService) {
    this.photoRepository = photoRepository;
    this.s3StorageService = s3StorageService;
    this.photoPool = loadAndAugmentPhotoPool();
  }

  /**
   * Loads real photos from classpath, then pads each category with generated placeholders to ensure
   * at least {@link #MIN_IMAGES_PER_CATEGORY} unique images per category.
   */
  private Map<String, List<PhotoFile>> loadAndAugmentPhotoPool() {
    Map<String, List<PhotoFile>> pool = loadPhotoPool();
    Random genRandom = new Random(42);

    // Room-type labels used for placeholder text per category
    Map<String, String[]> categoryLabels =
        Map.ofEntries(
            Map.entry(
                "exteriors",
                new String[] {
                  "Front Facade",
                  "Street View",
                  "Side Elevation",
                  "Garden View",
                  "Entrance",
                  "Rooftop",
                  "Parking",
                  "Backyard"
                }),
            Map.entry(
                "living-rooms",
                new String[] {
                  "Living Room",
                  "Lounge Area",
                  "Open Plan Living",
                  "Family Room",
                  "Sitting Area",
                  "Reading Corner",
                  "Fireplace View",
                  "Window Seat"
                }),
            Map.entry(
                "kitchens",
                new String[] {
                  "Kitchen",
                  "Modern Kitchen",
                  "Kitchen Island",
                  "Cooking Area",
                  "Breakfast Bar",
                  "Kitchen Storage",
                  "Appliances",
                  "Dining Kitchen"
                }),
            Map.entry(
                "bathrooms",
                new String[] {
                  "Main Bathroom",
                  "En-Suite",
                  "Guest WC",
                  "Shower Room",
                  "Bathroom Detail",
                  "Vanity Area",
                  "Bathtub",
                  "Wet Room"
                }),
            Map.entry(
                "bedrooms",
                new String[] {
                  "Master Bedroom",
                  "Guest Bedroom",
                  "Second Bedroom",
                  "Bedroom View",
                  "Wardrobe",
                  "Dressing Area",
                  "Children's Room",
                  "Study Bedroom"
                }),
            Map.entry(
                "offices",
                new String[] {
                  "Office Space",
                  "Reception",
                  "Meeting Room",
                  "Workspace",
                  "Break Room",
                  "Conference Room",
                  "Hot Desk Area",
                  "Executive Suite"
                }),
            Map.entry(
                "retail",
                new String[] {
                  "Shop Front",
                  "Sales Floor",
                  "Display Area",
                  "Stock Room",
                  "Counter",
                  "Window Display",
                  "Fitting Room",
                  "Retail Space"
                }),
            Map.entry(
                "warehouses",
                new String[] {
                  "Warehouse Floor",
                  "Loading Bay",
                  "Storage Racks",
                  "Office Area",
                  "Dispatch Zone",
                  "Mezzanine",
                  "Cold Storage",
                  "Workshop Area"
                }),
            Map.entry(
                "agricultural",
                new String[] {
                  "Field Overview",
                  "Barn",
                  "Greenhouse",
                  "Irrigation",
                  "Pasture",
                  "Farm Buildings",
                  "Storage Silo",
                  "Equipment Shed"
                }),
            Map.entry(
                "mixed-use",
                new String[] {
                  "Building Exterior",
                  "Commercial Unit",
                  "Residential Floor",
                  "Shared Entrance",
                  "Courtyard",
                  "Retail Ground Floor",
                  "Upper Floors",
                  "Roof Terrace"
                }));

    for (String category : ALL_CATEGORIES) {
      List<PhotoFile> existing = pool.computeIfAbsent(category, k -> new ArrayList<>());
      String[] labels = categoryLabels.getOrDefault(category, new String[] {"View " + category});
      int needed = MIN_IMAGES_PER_CATEGORY - existing.size();

      for (int i = 0; i < needed; i++) {
        String label = labels[i % labels.length];
        PhotoFile placeholder = generatePlaceholderImage(category, label, i, genRandom);
        if (placeholder != null) {
          existing.add(placeholder);
        }
      }
    }

    log.info(
        "Photo pool ready: {} total images across {} categories (real + generated)",
        pool.values().stream().mapToInt(List::size).sum(),
        pool.size());
    return pool;
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

      for (int pi = 0; pi < propertyIds.size(); pi++) {
        UUID propertyId = propertyIds.get(pi);
        String category = ctx.getPropertyCategory(propertyId);
        totalPhotos +=
            generatePropertyPhotos(
                teamId,
                ctx.getIdentifier(teamId),
                propertyId,
                ctx.getIdentifier(propertyId),
                uploadedBy,
                category,
                pi);
      }

      log.info("Uploaded photos for {} properties in team {}", propertyIds.size(), teamKey);
    }

    log.info("Total property photos created: {}", totalPhotos);
  }

  private int generatePropertyPhotos(
      UUID teamId,
      com.buurman.domain.Sid teamIdentifier,
      UUID propertyId,
      com.buurman.domain.Sid propertyIdentifier,
      UUID uploadedBy,
      String propertyCategory,
      int propertyIndex) {
    record PhotoSlot(String category, String title, boolean isMain) {}

    List<PhotoSlot> slots =
        switch (propertyCategory) {
          case "COMMERCIAL" ->
              List.of(
                  new PhotoSlot("offices", "Building exterior", true),
                  new PhotoSlot("offices", "Reception area", false),
                  new PhotoSlot("offices", "Open workspace", false),
                  new PhotoSlot("offices", "Meeting room", false),
                  new PhotoSlot("offices", "Break room", false));
          case "INDUSTRIAL" ->
              List.of(
                  new PhotoSlot("warehouses", "Exterior", true),
                  new PhotoSlot("warehouses", "Loading dock", false),
                  new PhotoSlot("warehouses", "Storage space", false),
                  new PhotoSlot("warehouses", "Office area", false));
          case "AGRICULTURAL" ->
              List.of(
                  new PhotoSlot("agricultural", "Aerial overview", true),
                  new PhotoSlot("agricultural", "Field detail", false),
                  new PhotoSlot("agricultural", "Farm buildings", false));
          case "MIXED_USE" ->
              List.of(
                  new PhotoSlot("mixed-use", "Street view", true),
                  new PhotoSlot("mixed-use", "Commercial space", false),
                  new PhotoSlot("living-rooms", "Residential unit", false),
                  new PhotoSlot("exteriors", "Building entrance", false));
          default -> // RESIDENTIAL — 7 photos
              List.of(
                  new PhotoSlot("exteriors", "Front view", true),
                  new PhotoSlot("living-rooms", "Living room", false),
                  new PhotoSlot("kitchens", "Kitchen", false),
                  new PhotoSlot("bedrooms", "Master bedroom", false),
                  new PhotoSlot("bathrooms", "Bathroom", false),
                  new PhotoSlot("bedrooms", "Second bedroom", false),
                  new PhotoSlot("living-rooms", "Dining area", false));
        };

    int count = 0;
    for (PhotoSlot slot : slots) {
      PhotoFile photoFile = pickPhoto(slot.category, slot.title, propertyIndex);
      if (photoFile == null) {
        continue;
      }

      try {
        String ext = photoFile.mimeType.equals("image/png") ? ".png" : ".jpg";
        String fileName = slot.title.toLowerCase(Locale.ROOT).replace(" ", "-") + ext;
        String fileKey =
            s3StorageService.uploadFile(
                photoFile.data,
                photoFile.mimeType,
                teamIdentifier,
                "PROPERTY",
                propertyIdentifier,
                fileName);

        Photo photo = new Photo();
        photo.setTeamId(teamId);
        photo.setEntityType("PROPERTY");
        photo.setEntityId(propertyId);
        photo.setFileKey(fileKey);

        photo.setFileName(fileName);
        photo.setFileSize((long) photoFile.data.length);
        photo.setMimeType(photoFile.mimeType);
        photo.setTitle(Optional.of(slot.title));
        photo.setIsMainPhoto(slot.isMain);
        photo.setUploadedBy(uploadedBy);

        // Thumbnail will be generated asynchronously by ThumbnailBackfillJob
        photoRepository.save(photo);
        count++;
      } catch (Exception e) {
        log.warn("Failed to upload photo for property {}: {}", propertyId, e.getMessage());
      }
    }
    return count;
  }

  @SuppressWarnings("NullAway")
  private @Nullable PhotoFile pickPhoto(String category, String title, int propertyIndex) {
    List<PhotoFile> pool = photoPool.get(category);
    if (pool != null && !pool.isEmpty()) {
      // Use property index to spread across the pool, avoiding repeats for sequential properties
      int idx = (propertyIndex + random.nextInt(pool.size())) % pool.size();
      return pool.get(idx);
    }
    // Shouldn't happen since loadAndAugmentPhotoPool pre-generates for all categories
    return photoPool.values().stream()
        .filter(l -> !l.isEmpty())
        .findFirst()
        .map(l -> l.get(random.nextInt(l.size())))
        .orElse(null);
  }

  /**
   * Generates a unique placeholder image with a gradient background and property info label. Each
   * image is visually distinct based on the category and property index.
   */
  @SuppressWarnings("NullAway")
  private @Nullable PhotoFile generatePlaceholderImage(
      String category, String title, int index, Random rng) {
    int width = 1200;
    int height = 800;
    BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    Graphics2D g = img.createGraphics();
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g.setRenderingHint(
        RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

    // Category-specific gradient colors, shifted per index for variety
    Color[] colors = gradientColorsForCategory(category);
    float hueShift = (index * 0.07f) % 1.0f;
    Color c1 = shiftHue(colors[0], hueShift);
    Color c2 = shiftHue(colors[1], hueShift);

    GradientPaint gradient = new GradientPaint(0, 0, c1, width, height, c2);
    g.setPaint(gradient);
    g.fillRect(0, 0, width, height);

    // Add subtle geometric shapes for texture — unique per image
    g.setColor(new Color(255, 255, 255, 20));
    for (int i = 0; i < 8; i++) {
      int x = rng.nextInt(width);
      int y = rng.nextInt(height);
      int size = 80 + rng.nextInt(200);
      g.fillRoundRect(x - size / 2, y - size / 2, size, size, 30, 30);
    }

    // Draw title label
    g.setFont(new Font("SansSerif", Font.BOLD, 42));
    java.awt.FontMetrics fm = g.getFontMetrics();
    int textX = (width - fm.stringWidth(title)) / 2;
    int textY = height / 2;

    // Shadow
    g.setColor(new Color(0, 0, 0, 80));
    g.drawString(title, textX + 2, textY + 2);
    // Text
    g.setColor(Color.WHITE);
    g.drawString(title, textX, textY);

    // Category label at bottom
    g.setFont(new Font("SansSerif", Font.PLAIN, 24));
    g.setColor(new Color(255, 255, 255, 150));
    String catLabel = category.replace("-", " ").toUpperCase(Locale.ROOT);
    g.drawString(catLabel, 40, height - 40);

    g.dispose();

    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ImageIO.write(img, "JPEG", baos);
      return new PhotoFile(baos.toByteArray(), "image/jpeg");
    } catch (IOException e) {
      log.warn("Failed to generate placeholder image: {}", e.getMessage());
      return null;
    }
  }

  private Color[] gradientColorsForCategory(String category) {
    return switch (category) {
      case "exteriors" ->
          new Color[] {new Color(70, 130, 180), new Color(25, 25, 112)}; // Steel blue to midnight
      case "living-rooms" ->
          new Color[] {new Color(139, 90, 43), new Color(101, 67, 33)}; // Warm brown tones
      case "kitchens" ->
          new Color[] {new Color(210, 180, 140), new Color(139, 119, 101)}; // Tan to taupe
      case "bathrooms" ->
          new Color[] {
            new Color(176, 224, 230), new Color(95, 158, 160)
          }; // Powder blue to cadet blue
      case "bedrooms" ->
          new Color[] {new Color(188, 143, 143), new Color(128, 0, 0)}; // Rosy brown to maroon
      case "offices" ->
          new Color[] {
            new Color(119, 136, 153), new Color(47, 79, 79)
          }; // Light slate to dark slate
      case "retail" ->
          new Color[] {new Color(255, 165, 0), new Color(178, 34, 34)}; // Orange to firebrick
      case "warehouses" ->
          new Color[] {new Color(128, 128, 128), new Color(54, 69, 79)}; // Gray to charcoal
      case "agricultural" ->
          new Color[] {new Color(107, 142, 35), new Color(34, 139, 34)}; // Olive to forest green
      case "mixed-use" ->
          new Color[] {
            new Color(153, 50, 204), new Color(72, 61, 139)
          }; // Purple to dark slate blue
      default ->
          new Color[] {
            new Color(100, 149, 237), new Color(65, 105, 225)
          }; // Cornflower to royal blue
    };
  }

  private Color shiftHue(Color c, float shift) {
    float[] hsb = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
    hsb[0] = (hsb[0] + shift) % 1.0f;
    return Color.getHSBColor(hsb[0], hsb[1], hsb[2]);
  }

  private static Map<String, List<PhotoFile>> loadPhotoPool() {
    Map<String, List<PhotoFile>> pool = new LinkedHashMap<>();
    var resolver = new PathMatchingResourcePatternResolver();

    for (String category : ALL_CATEGORIES) {
      List<PhotoFile> images = new ArrayList<>();
      try {
        for (String ext : List.of("*.jpg", "*.png")) {
          Resource[] resources = resolver.getResources(RESOURCE_BASE + category + "/" + ext);
          String mimeType = ext.equals("*.png") ? "image/png" : "image/jpeg";
          for (Resource resource : resources) {
            images.add(new PhotoFile(resource.getContentAsByteArray(), mimeType));
          }
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
