package com.mdevs.trackera.dto;

import com.mdevs.trackera.shared.search_filter.SearchOperator;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class OperatorSearchDTO<T> {
    private SearchOperator operator;

    private T value;

    private T secondValue;
}
