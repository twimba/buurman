package com.buurman.service.demo;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * Guards the bundled demo photo pool.
 *
 * <p>These images ship inside the application jar and are the largest single contributor to its
 * size, so they are stored as WebP. Nothing else verifies that they are present, intact and in a
 * format {@code DemoPhotoGenerator} knows how to serve, which is exactly what a bulk re-encode can
 * silently break.
 */
@DisplayName("Demo photo resources")
class DemoPhotoResourcesTest {

  private static final List<String> CATEGORIES =
      List.of(
          "agricultural",
          "bathrooms",
          "bedrooms",
          "exteriors",
          "kitchens",
          "living-rooms",
          "mixed-use",
          "offices",
          "retail",
          "warehouses");

  /** Extensions DemoPhotoGenerator can discover and map to a MIME type. */
  private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(".webp", ".jpg", ".png");

  private static final PathMatchingResourcePatternResolver RESOLVER =
      new PathMatchingResourcePatternResolver();

  private static Resource[] photosIn(String category) throws IOException {
    return RESOLVER.getResources("classpath:demo/photos/" + category + "/*");
  }

  @Test
  @DisplayName("every category ships photos")
  void everyCategoryHasPhotos() throws IOException {
    for (String category : CATEGORIES) {
      assertThat(photosIn(category)).as("photos for category '%s'", category).isNotEmpty();
    }
  }

  @Test
  @DisplayName("every bundled photo uses a format the generator understands")
  void everyPhotoUsesASupportedFormat() throws IOException {
    for (String category : CATEGORIES) {
      for (Resource photo : photosIn(category)) {
        String name = Objects.requireNonNull(photo.getFilename(), "resource filename");
        assertThat(SUPPORTED_EXTENSIONS)
            .as("extension of '%s' must be one DemoPhotoGenerator maps to a MIME type", name)
            .anyMatch(name::endsWith);
      }
    }
  }

  @Test
  @DisplayName("WebP photos are intact, not truncated by a bad re-encode")
  void webpPhotosAreValid() throws IOException {
    for (String category : CATEGORIES) {
      for (Resource photo : photosIn(category)) {
        String name = photo.getFilename();
        if (name == null || !name.endsWith(".webp")) {
          continue;
        }
        byte[] header = new byte[12];
        try (var in = photo.getInputStream()) {
          assertThat(in.read(header)).as("header of '%s'", name).isEqualTo(header.length);
        }
        // RIFF container: "RIFF" <4-byte size> "WEBP"
        assertThat(new String(Arrays.copyOfRange(header, 0, 4), StandardCharsets.US_ASCII))
            .as("RIFF magic of '%s'", name)
            .isEqualTo("RIFF");
        assertThat(new String(Arrays.copyOfRange(header, 8, 12), StandardCharsets.US_ASCII))
            .as("WEBP magic of '%s'", name)
            .isEqualTo("WEBP");
        assertThat(photo.contentLength()).as("size of '%s'", name).isGreaterThan(1024L);
      }
    }
  }

  @Test
  @DisplayName("the pool is large enough to vary across generated properties")
  void poolIsLargeEnoughToVary() throws IOException {
    List<String> all =
        CATEGORIES.stream()
            .flatMap(
                category -> {
                  try {
                    return Arrays.stream(photosIn(category)).map(Resource::getFilename);
                  } catch (IOException e) {
                    throw new IllegalStateException(e);
                  }
                })
            .collect(Collectors.toList());

    assertThat(all).hasSizeGreaterThanOrEqualTo(150);
  }
}
