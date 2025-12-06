type BadgeType = "main" | "success" | "warning" | "danger" | "default";

interface TrackeraTableEntity {
  id: string | number;
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

export type { TrackeraTableEntity, StatusBadgeProps, InputFieldProps, PaginatedResponse };
