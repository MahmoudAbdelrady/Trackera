package com.mdevs.trackera.shared.search_filter;

import com.mdevs.trackera.entity.WorkLog;
import com.mdevs.trackera.shared.exceptions.types.BusinessException;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class WorkLogSearchFilterBuilder {
    private final List<SearchFilter> filters = new ArrayList<>();

    public WorkLogSearchFilterBuilder(List<SearchFilter> searchFilters) {
        if (searchFilters != null && !searchFilters.isEmpty()) {
            for (SearchFilter filter : searchFilters) {
                if (filter.getOperator() == null) {
                    filter.setOperator(getDefaultSearchOperator(filter.getFieldName()));
                }
                if (StringUtils.isEmpty(filter.getFieldName())) {
                    throw new BusinessException("Field name is required in search filter");
                }
                if (filter.getValue() == null || StringUtils.isEmpty(filter.getValue().toString())) {
                    throw new BusinessException("Value is required for field: " + filter.getFieldName());
                }
                if (filter.getOperator().equals(SearchOperator.BETWEEN) && (filter.getExtraValue() == null || StringUtils.isEmpty(filter.getExtraValue().toString()))) {
                    throw new BusinessException("Extra value is required for BETWEEN operator on field: " + filter.getFieldName());
                }

                if (filter.getFieldName().equals("dateFrom") || filter.getFieldName().equals("dateTo")) {
                    if (filter.getFieldName().equals("dateFrom") && !filter.getOperator().equals(SearchOperator.GREATER_THAN_EQUAL)) {
                        throw new BusinessException("Unsupported operator for 'Date From'");
                    }
                    if (filter.getFieldName().equals("dateTo") && !filter.getOperator().equals(SearchOperator.LESS_THAN_EQUAL)) {
                        throw new BusinessException("Unsupported operator for 'Date To'");
                    }
                    filter.setFieldName("workDate");
                }
                addFilter(filter);
            }
        }
    }

    public void addFilter(SearchFilter filter) {
        filters.add(filter);
    }

    public Specification<WorkLog> build() {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            for (SearchFilter filter : filters) {
                Expression<String> path = root.get(filter.getFieldName()).as(String.class);
                String filterValue = filter.getValue().toString();
                switch (filter.getOperator()) {
                    case EQUALS -> predicates.add(criteriaBuilder.equal(path, filterValue));
                    case NOT_EQUALS -> predicates.add(criteriaBuilder.notEqual(path, filterValue));
                    case GREATER_THAN -> predicates.add(criteriaBuilder.greaterThan(path, filterValue));
                    case GREATER_THAN_EQUAL -> predicates.add(criteriaBuilder.greaterThanOrEqualTo(path, filterValue));
                    case LESS_THAN -> predicates.add(criteriaBuilder.lessThan(path, filterValue));
                    case LESS_THAN_EQUAL -> predicates.add(criteriaBuilder.lessThanOrEqualTo(path, filterValue));
                    case LIKE -> predicates.add(criteriaBuilder.like(path, "%" + filterValue + "%"));
                    case BETWEEN ->
                            predicates.add(criteriaBuilder.between(path, filterValue, filter.getExtraValue().toString()));
                    case IN -> predicates.add(path.in((List<?>) filter.getValue()));
                }
            }

            if (query != null) {
                query.orderBy(criteriaBuilder.desc(root.get("workDate")));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private SearchOperator getDefaultSearchOperator(String fieldName) {
        return switch (fieldName) {
            case "name" -> SearchOperator.LIKE;
            case "dateFrom" -> SearchOperator.GREATER_THAN_EQUAL;
            case "dateTo" -> SearchOperator.LESS_THAN_EQUAL;
            default -> SearchOperator.EQUALS;
        };
    }
}
