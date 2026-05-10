package com.megaproject.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Stable page response DTO that replaces Spring's PageImpl serialization.
 * Guarantees a consistent JSON structure regardless of Spring Data version.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageDTO<T> {
    private List<T> content;
    private long totalElements;
    private int totalPages;
    private int size;
    private int number;
    private boolean last;
    private boolean first;
    private boolean empty;

    public static <T> PageDTO<T> from(Page<T> page) {
        return new PageDTO<>(
                page.getContent(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getSize(),
                page.getNumber(),
                page.isLast(),
                page.isFirst(),
                page.isEmpty()
        );
    }
}
