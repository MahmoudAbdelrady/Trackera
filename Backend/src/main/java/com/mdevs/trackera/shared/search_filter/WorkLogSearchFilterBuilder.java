package com.mdevs.trackera.shared.search_filter;

import com.mdevs.trackera.entity.WorkLog;
import com.mdevs.trackera.shared.exceptions.types.BusinessException;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class WorkLogSearchFilterBuilder {
    private final List<SearchFilter> filters = new ArrayList<>();

    private final static Set<String> ALLOWED_FIELDS = Set.of("name", "totalHours", "status", "dateFrom", "dateTo", "evaluation");

    public WorkLogSearchFilterBuilder(List<SearchFilter> searchFilters) {
        if (searchFilters != null && !searchFilters.isEmpty()) {
            validateDateFilters(searchFilters);
            List<SearchFilter> processedFilters = new ArrayList<>();
            for (SearchFilter filter : searchFilters) {
                validateSearchFilter(filter);

                if (filter.getFieldName().equals("dateFrom") || filter.getFieldName().equals("dateTo")) {
                    filter.setFieldName("workDate");
                }

                processedFilters.addAll(handleFilter(filter));
            }
            processedFilters.forEach(this::addFilter);
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

    private List<SearchFilter> handleFilter(SearchFilter filter) {
        if (filter.getFieldName().equals("evaluation")) {
            return validateEvaluationFilter(filter);
        } else {
            return List.of(filter);
        }
    }

    private List<SearchFilter> validateEvaluationFilter(SearchFilter filter) {
        if (!EnumUtils.isValidEnum(WorkLog.Evaluation.class, filter.getValue().toString())) {
            throw new BusinessException("Invalid evaluation type: " + filter.getValue());
        }
        if (this.filters.stream().anyMatch(f -> f.getFieldName().equals("totalHours"))) {
            throw new BusinessException("Cannot filter by both evaluation and total hours");
        }
        WorkLog.Evaluation evaluationFilter = WorkLog.Evaluation.valueOf(filter.getValue().toString());
        filter.setFieldName("totalHours");
        switch (evaluationFilter) {
            case EXCELLENT -> {
                filter.setOperator(SearchOperator.GREATER_THAN_EQUAL);
                filter.setValue("8");
                return List.of(filter);
            }
            case GOOD -> {
                SearchFilter greaterThanFilter = new SearchFilter("totalHours", SearchOperator.GREATER_THAN_EQUAL, "7.5");
                SearchFilter lessThanFilter = new SearchFilter("totalHours", SearchOperator.LESS_THAN, "8");
                return List.of(greaterThanFilter, lessThanFilter);
            }
            case MODERATE -> {
                SearchFilter greaterThanFilter = new SearchFilter("totalHours", SearchOperator.GREATER_THAN_EQUAL, "7");
                SearchFilter lessThanFilter = new SearchFilter("totalHours", SearchOperator.LESS_THAN, "7.5");
                return List.of(greaterThanFilter, lessThanFilter);
            }
            case POOR -> {
                filter.setOperator(SearchOperator.LESS_THAN);
                filter.setValue("7");
                return List.of(filter);
            }
        }
        return List.of();
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
