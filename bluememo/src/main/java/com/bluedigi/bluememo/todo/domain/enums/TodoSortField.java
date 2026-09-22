package com.bluedigi.bluememo.todo.domain.enums;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;

@Getter
public enum TodoSortField {
    CREATED_AT("createdAt"),
    UPDATED_AT("updatedAt"),
    TITLE("title"),
    STATUS("status");

    private final String property;

    TodoSortField(String property) {
        this.property = property;
    }

    public static TodoSortField fromValue(String value) {
        return Arrays.stream(values())
                .filter(field ->
                        field.property.equalsIgnoreCase(value)
                                || field.name().equalsIgnoreCase(value)
                )
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid sort field: " + value
                ));
    }
}
