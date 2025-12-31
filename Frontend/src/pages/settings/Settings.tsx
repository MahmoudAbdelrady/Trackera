import { Link, Lock, Mail, Sliders } from "lucide-react";
import { ChangePasswordSection, EmailSection, LinkedAccount, PreferencesSection } from "../../components";
import { SettingsSection } from "../../components";
import { showErrorToast, showSuccessToast } from "../../utils/toast-handler/showToast";
import { useEffect, useState } from "react";
import { useOAuthFlow } from "../../shared/hooks";
import { userQueries } from "../../state/queries";
import classes from "./scss/settings.module.css";
import { authApis, userApis } from "../../state/api";
import type { OAuthAccount } from "../../shared/types";
import { Spin } from "antd";
import { AppLayout } from "../../layouts";

const Settings = () => {
  const { data: userData, refetch: refetchUser } = userQueries.useMeQuery();

  // oauth accounts
  const [isFetchingAccounts, setIsFetchingAccounts] = useState<boolean>(false);
  const [oAuthAccounts, setOAuthAccounts] = useState<OAuthAccount[]>([]);

  // preferences
  const [fetchPreferences, setFetchPreferences] = useState<boolean>(true);

  const { linkProviderAccount } = useOAuthFlow({
    onSuccess: () => {
      showSuccessToast("Account linked successfully");
      refetchUser();
      fetchOAuthAccounts();
      setFetchPreferences(true);
    },
    onError: (error) => {
      showErrorToast(error);
    },
  });

  const fetchOAuthAccounts = async () => {
    setIsFetchingAccounts(true);
    try {
      const result = await userApis.getOAuthProviders();
      setOAuthAccounts(result);
    } catch (error: any) {
      showErrorToast(error);
    }
    setIsFetchingAccounts(false);
  };

  const unlinkProviderAccount = async (provider: string) => {
    try {
      const result = await authApis.unLinkOAuthProvider(provider);
      showSuccessToast(result);
      refetchUser();
      fetchOAuthAccounts();
    } catch (error: any) {
      showErrorToast(error);
    }
  };

  useEffect(() => {
    fetchOAuthAccounts();
  }, []);

  return (
    <AppLayout>
      <div className={classes.settings_sections}>
        <SettingsSection title={`${userData?.passwordSet ? "Change" : "Set"} Password`} icon={<Lock />}>
          <ChangePasswordSection />
        </SettingsSection>
        <SettingsSection title="Emails" icon={<Mail />}>
          <EmailSection />
        </SettingsSection>
        <SettingsSection title="Linked Accounts" icon={<Link />}>
          {isFetchingAccounts ? (
            <Spin />
          ) : (
            oAuthAccounts.map((account) => (
              <LinkedAccount
                key={account.provider.code}
                platform={account.provider}
                accountIdentifier={account.email}
                isLinked={account.isLinked}
                onLink={() => linkProviderAccount(account.provider.code)}
                onUnlink={() => unlinkProviderAccount(account.provider.code)}
              />
            ))
          )}
        </SettingsSection>
        <SettingsSection title="Preferences" icon={<Sliders />}>
          <PreferencesSection
            jiraLinked={userData?.jiraLinked || false}
            fetchPreferences={fetchPreferences}
            setFetchPreferences={setFetchPreferences}
          />
        </SettingsSection>
      </div>
    </AppLayout>
  );
};

export default Settings;
