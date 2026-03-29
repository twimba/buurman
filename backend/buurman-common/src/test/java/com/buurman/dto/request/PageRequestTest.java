package com.buurman.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.buurman.domain.SortDirection;

@DisplayName("PageRequest")
class PageRequestTest {

  @Nested
  @DisplayName("of(Integer, Integer, String, SortDirection)")
  class OfWithSortDirection {

    @Test
    @DisplayName("creates with provided values")
    void createsWithProvidedValues() {
      PageRequest pr = PageRequest.of(2, 50, "name", SortDirection.ASC);

      assertThat(pr.page()).isEqualTo(2);
      assertThat(pr.size()).isEqualTo(50);
      assertThat(pr.sort()).hasValue("name");
      assertThat(pr.direction()).hasValue(SortDirection.ASC);
    }

    @Test
    @DisplayName("null page defaults to 0")
    void nullPageDefaultsToZero() {
      PageRequest pr = PageRequest.of(null, 10, null, (SortDirection) null);

      assertThat(pr.page()).isZero();
    }

    @Test
    @DisplayName("negative page defaults to 0")
    void negativePageDefaultsToZero() {
      PageRequest pr = PageRequest.of(-1, 10, null, (SortDirection) null);

      assertThat(pr.page()).isZero();
    }

    @Test
    @DisplayName("null size defaults to 25")
    void nullSizeDefaultsTo25() {
      PageRequest pr = PageRequest.of(0, null, null, (SortDirection) null);

      assertThat(pr.size()).isEqualTo(PageRequest.DEFAULT_SIZE);
    }

    @Test
    @DisplayName("zero size defaults to 25")
    void zeroSizeDefaultsTo25() {
      PageRequest pr = PageRequest.of(0, 0, null, (SortDirection) null);

      assertThat(pr.size()).isEqualTo(PageRequest.DEFAULT_SIZE);
    }

    @Test
    @DisplayName("negative size defaults to 25")
    void negativeSizeDefaultsTo25() {
      PageRequest pr = PageRequest.of(0, -5, null, (SortDirection) null);

      assertThat(pr.size()).isEqualTo(PageRequest.DEFAULT_SIZE);
    }

    @Test
    @DisplayName("size exceeding MAX_SIZE is capped")
    void sizeCappedAtMax() {
      PageRequest pr = PageRequest.of(0, 1000, null, (SortDirection) null);

      assertThat(pr.size()).isEqualTo(PageRequest.MAX_SIZE);
    }

    @Test
    @DisplayName("size at MAX_SIZE is preserved")
    void sizeAtMaxPreserved() {
      PageRequest pr = PageRequest.of(0, PageRequest.MAX_SIZE, null, (SortDirection) null);

      assertThat(pr.size()).isEqualTo(PageRequest.MAX_SIZE);
    }

    @Test
    @DisplayName("null sort produces empty Optional")
    void nullSortProducesEmpty() {
      PageRequest pr = PageRequest.of(0, 10, null, (SortDirection) null);

      assertThat(pr.sort()).isEmpty();
    }

    @Test
    @DisplayName("null direction produces empty Optional")
    void nullDirectionProducesEmpty() {
      PageRequest pr = PageRequest.of(0, 10, "name", (SortDirection) null);

      assertThat(pr.direction()).isEmpty();
    }
  }

  @Nested
  @DisplayName("of(Integer, Integer, String, String)")
  class OfWithStringDirection {

    @Test
    @DisplayName("parses ASC string direction")
    void parsesAsc() {
      PageRequest pr = PageRequest.of(0, 10, "name", "asc");

      assertThat(pr.direction()).hasValue(SortDirection.ASC);
    }

    @Test
    @DisplayName("parses DESC string direction (case-insensitive)")
    void parsesDescCaseInsensitive() {
      PageRequest pr = PageRequest.of(0, 10, "name", "Desc");

      assertThat(pr.direction()).hasValue(SortDirection.DESC);
    }

    @Test
    @DisplayName("null string direction produces empty Optional")
    void nullStringDirection() {
      PageRequest pr = PageRequest.of(0, 10, "name", (String) null);

      assertThat(pr.direction()).isEmpty();
    }

    @Test
    @DisplayName("blank string direction produces empty Optional")
    void blankStringDirection() {
      PageRequest pr = PageRequest.of(0, 10, "name", "  ");

      assertThat(pr.direction()).isEmpty();
    }
  }

  @Nested
  @DisplayName("offset")
  class Offset {

    @Test
    @DisplayName("page 0, size 25 produces offset 0")
    void firstPage() {
      PageRequest pr = PageRequest.of(0, 25, null, (SortDirection) null);

      assertThat(pr.offset()).isZero();
    }

    @Test
    @DisplayName("page 3, size 10 produces offset 30")
    void thirdPage() {
      PageRequest pr = PageRequest.of(3, 10, null, (SortDirection) null);

      assertThat(pr.offset()).isEqualTo(30);
    }
  }

  @Nested
  @DisplayName("constants")
  class ConstantsCheck {

    @Test
    @DisplayName("DEFAULT_PAGE is 0")
    void defaultPage() {
      assertThat(PageRequest.DEFAULT_PAGE).isZero();
    }

    @Test
    @DisplayName("DEFAULT_SIZE is 25")
    void defaultSize() {
      assertThat(PageRequest.DEFAULT_SIZE).isEqualTo(25);
    }

    @Test
    @DisplayName("MAX_SIZE is 500")
    void maxSize() {
      assertThat(PageRequest.MAX_SIZE).isEqualTo(500);
    }
  }
}
