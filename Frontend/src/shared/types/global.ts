interface InputFieldProps {
  label: string;
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

export type { InputFieldProps };
