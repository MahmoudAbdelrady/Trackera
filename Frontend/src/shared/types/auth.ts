interface ChangePasswordFormFields {
  currentPassword?: string;
  newPassword: string;
  confirmNewPassword: string;
}

interface AuthResultFields {
  title?: string;
  description?: string;
  isError?: boolean;
}

interface OAuthBtnProps {
  label: string;
  icon: React.ReactNode;
  onClick: () => void;
}

interface OAuthAccount {
  provider: Record<string, string>;
  isLinked: boolean;
  email?: string;
}

interface UserInfo {
  firstname: string;
  lastname: string;
  primaryEmail: string;
  pendingEmail: string | null;
  profilePicture: string | null;
  avatarColor: string;
  passwordSet: boolean;
  jiraLinked: boolean;
}

export type { ChangePasswordFormFields, AuthResultFields, OAuthBtnProps, OAuthAccount, UserInfo };
