import { Link, Lock, Mail, Sliders } from "lucide-react";
import { AppLayout, ChangePasswordSection, EmailSection, LinkedAccount, LoadingSpinner, PreferencesSection } from "../../components";
import { SettingsSection } from "../../components";
import { showErrorToast, showSuccessToast } from "../../utils/toast-handler/showToast";
import requestInstance from "../../shared/axios/request-instance";
import { useEffect, useState } from "react";
import { useOAuthFlow } from "../../shared/hooks";
import { userQueries } from "../../state/queries";
import classes from "./scss/settings.module.css";

interface OAuthAccount {
  provider: Record<string, string>;
  isLinked: boolean;
  email?: string;
}

const Settings = () => {
  const { data: userData } = userQueries.useMeQuery();
  const [isFetchingAccounts, setIsFetchingAccounts] = useState(false);
  const [fetchOAuthAccounts, setFetchOAuthAccounts] = useState(true);
  const [oAuthAccounts, setOAuthAccounts] = useState<OAuthAccount[]>([]);

  const { linkProviderAccount } = useOAuthFlow({
    onSuccess: () => {
      showSuccessToast("Account linked successfully");
      setFetchOAuthAccounts(true);
    },
    onError: (error) => {
      showErrorToast(error);
    },
  });

  const unlinkProviderAccount = async (provider: string) => {
    try {
      const response = await requestInstance.post(`/auth/unlink-oauth/${provider}`);
      showSuccessToast(response.data);
      setFetchOAuthAccounts(true);
    } catch (error: any) {
      showErrorToast(error);
    }
  };

  useEffect(() => {
    const fetchAccounts = async () => {
      setIsFetchingAccounts(true);
      try {
        const response = await requestInstance.get("/user/oauth-providers");
        setOAuthAccounts(response.data);
      } catch (error: any) {
        showErrorToast(error);
      }
      setIsFetchingAccounts(false);
    };

    if (fetchOAuthAccounts) {
      fetchAccounts();
      setFetchOAuthAccounts(false);
    }
  }, [fetchOAuthAccounts]);

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
            <LoadingSpinner />
          ) : (
            oAuthAccounts.map((account, idx) => (
              <LinkedAccount
                key={idx}
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
          <PreferencesSection />
        </SettingsSection>
      </div>
    </AppLayout>
  );
};

export default Settings;
