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

interface AuthResultProps {
  title: string;
  description?: string;
  message: string;
  isError?: boolean;
  buttonText: string;
  onClick: () => void;
}

interface AuthFooterProps {
  oAuthButtons?: AuthFooterOAuthBtn[];
  footerText: string;
  footerLink: string;
  footerLinkText: string;
}

interface AuthFooterOAuthBtn {
  label: string;
  icon: React.ReactNode;
  onClick: () => void;
}

export type {
  AuthLayoutProps,
  AuthFormProps,
  AuthResultProps,
  AuthFooterProps,
  AuthFooterOAuthBtn,
};
