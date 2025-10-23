interface SettingsSectionProps {
  title: string;
  icon: React.ReactNode;
  children: React.ReactNode;
}

interface LinkedAccountProps {
  platform: Record<string, string>;
  accountIdentifier?: string;
  isLinked: boolean;
  onLink: () => void;
  onUnlink: () => void;
}

interface EmailSectionProps {
  userEmails: UserEmailType[];
  isFetchingEmails: boolean;
  setFetchUserEmails: (fetch: boolean) => void;
  setFetchLinkedAccounts: (fetch: boolean) => void;
}

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

export type { SettingsSectionProps, LinkedAccountProps, EmailSectionProps, UserEmailType, UserEmailProps };
