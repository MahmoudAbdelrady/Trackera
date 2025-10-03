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

export type { SettingsSectionProps, LinkedAccountProps };
