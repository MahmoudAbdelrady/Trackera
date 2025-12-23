interface LoginFormFields {
  email: string;
  password: string;
}

interface SignUpFormFields {
  firstname: string;
  lastname: string;
  email: string;
  password: string;
  confirmPassword: string;
}

interface ChangePasswordFormFields {
  currentPassword?: string;
  newPassword: string;
  confirmNewPassword: string;
}

interface AuthLayoutProps {
  children: React.ReactNode;
}

interface AuthFormProps {
  title: string;
  description: string;
  children: React.ReactNode;
  submitButtonText: string;
  onSubmit: (event: React.FormEvent<HTMLFormElement>) => void;
  isSubmitBtnLoading?: boolean;
  isSubmitBtnDisabled?: boolean;
  footer?: React.ReactNode;
}

interface AuthResultFields {
  title?: string;
  description?: string;
  isError?: boolean;
}

interface AuthResultProps extends AuthResultFields {
  message: string;
  buttonText: string;
  onClick: () => void;
}

interface AuthFooterProps {
  hasOAuthBtns?: boolean;
  isOAuthBtnsDisabled?: boolean;
  footerText: string;
  footerLink: string;
  footerLinkText: string;
}

interface OAuthBtnProps {
  label: string;
  icon: React.ReactNode;
  onClick: () => void;
}

export type {
  LoginFormFields,
  SignUpFormFields,
  ChangePasswordFormFields,
  AuthLayoutProps,
  AuthFormProps,
  AuthResultFields,
  AuthResultProps,
  AuthFooterProps,
  OAuthBtnProps,
};
