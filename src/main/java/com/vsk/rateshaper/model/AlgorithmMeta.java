package com.vsk.rateshaper.model;

import java.util.List;

/**
 * Metadata describing a rate-limiting algorithm for dynamic frontend form generation.
 */
public record AlgorithmMeta(
    String id,
    String displayName,
    String description,
    List<String> configFields
) {
}
