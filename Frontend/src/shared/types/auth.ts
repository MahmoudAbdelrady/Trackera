interface AuthLayoutProps {
  title: string;
  description: string;
  submitButtonText: string;
  isSubmitBtnLoading?: boolean;
  isSubmitBtnDisabled?: boolean;
  children: React.ReactNode;
  onSubmit: (event: React.FormEvent<HTMLFormElement>) => void;
  showResponseContent?: boolean;
  responseContent?: AuthResponseContent;
  footer?: React.ReactNode;
}

interface AuthResponseContent {
  title: string;
  description: string;
  message: string;
  icon: React.ReactNode;
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

export type { AuthLayoutProps, AuthFooterProps, AuthFooterOAuthBtn };
