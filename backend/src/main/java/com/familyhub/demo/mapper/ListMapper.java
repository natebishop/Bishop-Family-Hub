package com.familyhub.demo.mapper;

import com.familyhub.demo.dto.ListCategoryOption;
import com.familyhub.demo.dto.ListDetailResponse;
import com.familyhub.demo.dto.ListItemResponse;
import com.familyhub.demo.dto.ListSummaryResponse;
import com.familyhub.demo.model.ListCategory;
import com.familyhub.demo.model.SharedList;
import com.familyhub.demo.model.SharedListItem;

import java.util.List;

public final class ListMapper {
    private ListMapper() {
    }

    public static ListSummaryResponse toSummaryDto(SharedList list) {
        int completedItems = (int) list.getItems().stream().filter(SharedListItem::isCompleted).count();
        return new ListSummaryResponse(
                list.getId(),
                list.getName(),
                list.getKind(),
                list.getItems().size(),
                completedItems
        );
    }

    public static ListCategoryOption toCategoryDto(ListCategory category) {
        return new ListCategoryOption(
                category.getId(),
                category.getKind(),
                category.getName(),
                category.getSortOrder()
        );
    }

    public static ListItemResponse toItemDto(SharedListItem item) {
        return new ListItemResponse(
                item.getId(),
                item.getText(),
                item.isCompleted(),
                item.getCompletedAt(),
                item.getCategory() != null ? item.getCategory().getId() : null,
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }

    public static ListDetailResponse toDetailDto(SharedList list, List<ListCategoryOption> categories) {
        return new ListDetailResponse(
                list.getId(),
                list.getName(),
                list.getKind(),
                list.getCategoryDisplayMode(),
                list.getShowCompletedOverride(),
                categories,
                list.getItems().stream().map(ListMapper::toItemDto).toList(),
                list.getCreatedAt(),
                list.getUpdatedAt()
        );
    }
}
