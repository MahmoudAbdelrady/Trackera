type BadgeType = "main" | "success" | "warning" | "danger" | "default";

interface StatusBadgeProps {
  label: string;
  type: BadgeType;
  icon?: React.ReactNode;
}

export type { StatusBadgeProps };
