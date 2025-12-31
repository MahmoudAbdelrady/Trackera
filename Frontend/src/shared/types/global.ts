interface PaginatedResponse<T> {
  content: T[];
  page: {
    size: number;
    number: number;
    totalElements: number;
    totalPages: number;
  };
}

const OPERATORS = {
  BETWEEN: "BETWEEN",
  EQ: "=",
  NE: "!=",
  GT: ">",
  GTE: ">=",
  LT: "<",
  LTE: "<=",
} as const;

const filterOperatorsMetadata = [
  { label: "=", value: OPERATORS.EQ },
  { label: "!=", value: OPERATORS.NE },
  { label: ">", value: OPERATORS.GT },
  { label: ">=", value: OPERATORS.GTE },
  { label: "<", value: OPERATORS.LT },
  { label: "<=", value: OPERATORS.LTE },
  { label: "Between", value: OPERATORS.BETWEEN },
];

export type { PaginatedResponse };

export { OPERATORS, filterOperatorsMetadata };
