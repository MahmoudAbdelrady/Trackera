type BadgeType = "main" | "success" | "warning" | "danger" | "default";

interface TrackeraTableEntity {
  id: string;
}

interface StatusBadgeProps {
  label: string;
  type: BadgeType;
  icon?: React.ReactNode;
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
export type { TrackeraTableEntity, StatusBadgeProps, PaginatedResponse };
