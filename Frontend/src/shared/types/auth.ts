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

interface OAuthProviderInfo {
  displayName: string;
  code: string;
}

interface OAuthAccount {
  provider: OAuthProviderInfo;
  linked: boolean;
  email?: string;
  isRevoked?: boolean;
}

const OAUTH_PROVIDERS = {
  GOOGLE: "google",
  JIRA: "jira",
} as const;

type OAuthProvider = (typeof OAUTH_PROVIDERS)[keyof typeof OAUTH_PROVIDERS];

export type { LoginFormFields, SignUpFormFields, OAuthAccount, OAuthProvider, OAuthProviderInfo };

export { OAUTH_PROVIDERS };
