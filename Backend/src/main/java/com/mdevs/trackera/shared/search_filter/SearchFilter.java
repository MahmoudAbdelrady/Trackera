package com.mdevs.trackera.shared.search_filter;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SearchFilter {
    private String fieldName;
    private SearchOperator operator;
    private Object value;
    private Object extraValue;

    public SearchFilter(String fieldName, SearchOperator operator, Object value) {
        this(fieldName, operator, value, null);
    }
}
