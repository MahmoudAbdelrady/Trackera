package com.mdevs.trackera.shared.search_filter;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum SearchOperator {
    EQUALS("="),
    NOT_EQUALS("!="),
    GREATER_THAN(">"),
    GREATER_THAN_EQUAL(">="),
    LESS_THAN("<"),
    LESS_THAN_EQUAL("<="),
    LIKE("LIKE"),
    BETWEEN("BETWEEN"),
    IN("IN");

    private final String querySymbol;

    public static SearchOperator fromSymbol(String symbol) {
        for (SearchOperator operator : values()) {
            if (operator.getQuerySymbol().equalsIgnoreCase(symbol)) {
                return operator;
            }
        }
        throw new IllegalArgumentException("Invalid search operator: " + symbol);
    }
}
