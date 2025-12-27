interface UserEmailType {
  email: string;
  primary: boolean;
  verified: boolean;
  oauthLinked: boolean;
  tags: string[];
}

interface UserEmailProps {
  userEmail: UserEmailType;
  setFetchUserEmails: (fetch: boolean) => void;
  setFetchLinkedAccounts: (fetch: boolean) => void;
}

export type { UserEmailType, UserEmailProps };
