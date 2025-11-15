package com.mdevs.trackera.shared.search_filter;

import com.mdevs.trackera.shared.exceptions.types.BusinessException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class SearchFilter<T> {
    private String operator;

    private T value;

    private T secondValue;

    public void validate(String fieldName) {
        if (operator == null) {
            throw new BusinessException("Operator is required for " + fieldName);
        }
        if (value == null) {
            throw new BusinessException("Value is required for " + fieldName);
        }
        if (getOperator().equals(SearchOperator.BETWEEN)) {
            if (secondValue == null) {
                throw new BusinessException("Second value is required for BETWEEN operator on " + fieldName);
            }
            if (value instanceof Comparable && ((Comparable) value).compareTo(secondValue) > 0) {
                throw new BusinessException("First value must be less than or equal to second value for BETWEEN operator on " + fieldName);
            }
        }
    }

    public SearchOperator getOperator() {
        return SearchOperator.fromSymbol(operator);
    }
}
