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

interface TimeZoneOption {
  id: string;
  label: string;
}

export type { UserInfo, ChangePasswordFormFields, TimeZoneOption };
