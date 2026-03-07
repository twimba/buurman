package com.buurman.domain.metadata;

import java.io.IOException;
import java.math.BigDecimal;

import com.buurman.util.CurrencyUtils;
import com.buurman.util.MoneyAmount;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * Jackson module that serializes {@link MoneyAmount} as minor units for JSONB storage.
 *
 * <ul>
 *   <li>Serialize: {@code MoneyAmount(250.00, "EUR")} → {@code {"value": 25000, "currency": "EUR"}}
 *   <li>Deserialize: {@code {"value": 25000, "currency": "EUR"}} → {@code MoneyAmount(250.00,
 *       "EUR")}
 * </ul>
 *
 * <p>This module is only registered on the dedicated CountryMetadataSerializer ObjectMapper, NOT on
 * the global Spring ObjectMapper (which uses default record serialization with major units).
 */
public class MoneyAmountJsonbModule extends SimpleModule {

  public MoneyAmountJsonbModule() {
    super("MoneyAmountJsonb");
    addSerializer(MoneyAmount.class, new MinorUnitSerializer());
    addDeserializer(MoneyAmount.class, new MinorUnitDeserializer());
  }

  private static final class MinorUnitSerializer extends JsonSerializer<MoneyAmount> {
    @Override
    public void serialize(MoneyAmount money, JsonGenerator gen, SerializerProvider serializers)
        throws IOException {
      gen.writeStartObject();
      gen.writeNumberField("value", money.toMinorUnits());
      gen.writeStringField("currency", money.currency());
      gen.writeEndObject();
    }
  }

  private static final class MinorUnitDeserializer extends JsonDeserializer<MoneyAmount> {
    @Override
    public MoneyAmount deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
      JsonNode node = p.getCodec().readTree(p);
      long minorUnits = node.get("value").asLong();
      String currency = node.get("currency").asText();
      int digits = CurrencyUtils.getFractionalDigits(currency);
      BigDecimal majorUnits = BigDecimal.valueOf(minorUnits, digits);
      return MoneyAmount.of(majorUnits, currency);
    }
  }
}
