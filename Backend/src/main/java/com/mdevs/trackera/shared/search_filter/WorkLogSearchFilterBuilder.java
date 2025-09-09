package com.mdevs.trackera.shared.search_filter;

import com.mdevs.trackera.entity.WorkLog;
import com.mdevs.trackera.shared.exceptions.types.BusinessException;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class WorkLogSearchFilterBuilder {
    private final List<SearchFilter> filters = new ArrayList<>();

    private final static Set<String> ALLOWED_FIELDS = Set.of("name", "totalHours", "status", "dateFrom", "dateTo");

    public WorkLogSearchFilterBuilder(List<SearchFilter> searchFilters) {
        if (searchFilters != null && !searchFilters.isEmpty()) {
            validateDateFilters(searchFilters);
            for (SearchFilter filter : searchFilters) {
                validateSearchFilter(filter);

                if (filter.getFieldName().equals("dateFrom") || filter.getFieldName().equals("dateTo")) {
                    filter.setFieldName("workDate");
                }

                addFilter(filter);
            }
        }
    }

    private void validateDateFilters(List<SearchFilter> searchFilters) {
        SearchFilter dateFromFilter = searchFilters.stream().filter(filter -> !StringUtils.isEmpty(filter.getFieldName()) && filter.getFieldName().equals("dateFrom")).findFirst().orElse(null);
        SearchFilter dateToFilter = searchFilters.stream().filter(filter -> !StringUtils.isEmpty(filter.getFieldName()) && filter.getFieldName().equals("dateTo")).findFirst().orElse(null);

        if (dateFromFilter == null || dateToFilter == null) {
            return;
        }

        validateSearchFilter(dateFromFilter);
        validateSearchFilter(dateToFilter);

        LocalDate dateFrom = LocalDate.parse(dateFromFilter.getValue().toString());
        LocalDate dateTo = LocalDate.parse(dateToFilter.getValue().toString());
        if (dateFrom.isAfter(dateTo)) {
            throw new BusinessException("'Date From' cannot be after 'Date To'");
        }
        if (dateTo.isAfter(dateFrom.plusYears(1))) {
            throw new BusinessException("Date range cannot exceed 1 year");
        }
    }

    private void validateSearchFilter(SearchFilter searchFilter) {
        if (StringUtils.isEmpty(searchFilter.getFieldName())) {
            throw new BusinessException("Field name is required in search filter");
        }
        if (!ALLOWED_FIELDS.contains(searchFilter.getFieldName())) {
            throw new BusinessException("Filtering by field '" + searchFilter.getFieldName() + "' is not supported");
        }
        if (searchFilter.getValue() == null || StringUtils.isEmpty(searchFilter.getValue().toString())) {
            throw new BusinessException("Value is required for field: " + searchFilter.getFieldName());
        }

        if (searchFilter.getOperator() == null) {
            searchFilter.setOperator(getDefaultSearchOperator(searchFilter.getFieldName()));
        }
        if (searchFilter.getOperator().equals(SearchOperator.BETWEEN) && (searchFilter.getExtraValue() == null || StringUtils.isEmpty(searchFilter.getExtraValue().toString()))) {
            throw new BusinessException("Extra value is required for BETWEEN operator on field: " + searchFilter.getFieldName());
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
