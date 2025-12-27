type BadgeType = "main" | "success" | "warning" | "danger" | "default";

interface TrackeraTableEntity {
  id: string;
}

interface StatusBadgeProps {
  label: string;
  type: BadgeType;
  icon?: React.ReactNode;
}

interface InputFieldProps {
  label?: string;
  icon?: React.ReactNode;
  type: string;
  name: string;
  placeholder?: string;
  value?: string;
  onChange?: (event: React.ChangeEvent<HTMLInputElement>) => void;
  onBlur?: (event: React.FocusEvent<HTMLInputElement>) => void;
  error?: string;
  disabled?: boolean;
}

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

const FILTER_OPERATORS_METADATA = [
  { label: "=", value: OPERATORS.EQ },
  { label: "!=", value: OPERATORS.NE },
  { label: ">", value: OPERATORS.GT },
  { label: ">=", value: OPERATORS.GTE },
  { label: "<", value: OPERATORS.LT },
  { label: "<=", value: OPERATORS.LTE },
  { label: "Between", value: OPERATORS.BETWEEN },
];

export type { TrackeraTableEntity, StatusBadgeProps, InputFieldProps, PaginatedResponse };

export { OPERATORS, FILTER_OPERATORS_METADATA };
