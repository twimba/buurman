package com.buurman.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Constants")
class ConstantsTest {

  @Test
  @DisplayName("SYSTEM_USER_ID is the well-known UUID 00000000-0000-0000-0000-000000000001")
  void systemUserId() {
    assertThat(Constants.SYSTEM_USER_ID)
        .isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000001"));
  }

  @Test
  @DisplayName("SYSTEM_USER_ID is not the nil UUID")
  void systemUserIdNotNil() {
    assertThat(Constants.SYSTEM_USER_ID).isNotEqualTo(new UUID(0, 0));
  }
}
