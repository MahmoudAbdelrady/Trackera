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

interface PreferencesProps {
  jiraLinked: boolean;
  fetchPreferences: boolean;
  setFetchPreferences: (fetch: boolean) => void;
}

export type { SettingsSectionProps, LinkedAccountProps, UserEmailType, UserEmailProps, PreferencesProps };
