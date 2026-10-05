package com.bluedigi.bluememo.common.domain;

import java.util.List;

import org.springframework.data.domain.Page;

public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        int numberOfElements,
        Long totalElements
) {
    public static <T> PageResponse<T> from(Page<T> result) {
        return new PageResponse<>(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getNumberOfElements(),
                result.getTotalElements()
        );
    }
}
