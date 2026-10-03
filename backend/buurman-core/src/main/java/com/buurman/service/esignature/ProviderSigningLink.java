package com.buurman.service.esignature;

import java.util.Optional;

/** A provider recipient with the link they sign through. The URL is a bearer credential. */
public record ProviderSigningLink(
    String providerSignerId, String email, String name, Optional<String> signingUrl) {

  @Override
  public String toString() {
    return "ProviderSigningLink[providerSignerId=" + providerSignerId + "]";
  }
}
