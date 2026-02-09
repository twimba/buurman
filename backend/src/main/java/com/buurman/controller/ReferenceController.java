package com.buurman.controller;

import com.buurman.util.EntityPrefix;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/reference")
@Tag(name = "Reference", description = "Technical reference data for integrators and developers")
public class ReferenceController {

    public record EntityPrefixInfo(
            String prefix,
            String entity
    ) {}

    @Operation(summary = "Entity identifier prefixes",
               description = "Returns the 3-character prefix codes used in entity identifiers (format: <prefix><ULID>)")
    @GetMapping("/entity-prefixes")
    public List<EntityPrefixInfo> getEntityPrefixes() {
        return Arrays.stream(EntityPrefix.values())
                .map(p -> new EntityPrefixInfo(p.getCode(), p.getEntityName()))
                .toList();
    }
}
