interface TableActionButtonOptions {
  label: string;
  onClick: () => void;
  disabled?: boolean;
  icon?: React.ReactNode;
  customClasses?: string[];
}

interface TableActionButtonProps {
  label: string;
  icon?: React.ReactNode;
  customClasses?: string[];
  disabled?: boolean;
  onClick?: () => void;
  options?: TableActionButtonOptions[];
  loading?: boolean;
}

export type { TableActionButtonProps };
