package com.familyhub.demo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BulkCreateListItemsRequest(
        @Valid
        @NotEmpty(message = "At least one item is required")
        @Size(max = MAX_BULK_ITEMS, message = "A bulk append may contain at most " + MAX_BULK_ITEMS + " items")
        List<CreateListItemRequest> items
) {
    /**
     * Upper bound on items per bulk append. Sized to cover a full week of recipe-backed dinners
     * (with extras) while bounding payload and transaction size. Enforced both here via {@code @Size}
     * (request-boundary validation) and defensively in {@code ListService.createItemsBulk}.
     */
    public static final int MAX_BULK_ITEMS = 100;
}
