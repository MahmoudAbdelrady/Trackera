interface AuthLayoutProps {
  title: string;
  description: string;
  submitButtonText: string;
  isSubmitBtnLoading?: boolean;
  isSubmitBtnDisabled?: boolean;
  children: React.ReactNode;
  onSubmit: (event: React.FormEvent<HTMLFormElement>) => void;
  footer?: React.ReactNode;
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
