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

interface ChangePasswordFormFields {
  currentPassword?: string;
  newPassword: string;
  confirmNewPassword: string;
}

interface OAuthAccount {
  provider: Record<string, string>;
  isLinked: boolean;
  email?: string;
}

const OAUTH_PROVIDERS = {
  GOOGLE: "google",
  JIRA: "jira",
} as const;

type OAuthProvider = (typeof OAUTH_PROVIDERS)[keyof typeof OAUTH_PROVIDERS];

export type { LoginFormFields, SignUpFormFields, ChangePasswordFormFields, OAuthAccount, UserInfo, OAuthProvider };

export { OAUTH_PROVIDERS };
