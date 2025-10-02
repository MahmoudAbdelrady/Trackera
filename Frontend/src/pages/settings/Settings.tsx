import { Link } from "lucide-react";
import { AppLayout, LinkedAccount, LoadingSpinner } from "../../components";
import { SettingsSection } from "../../components";
import classes from "./scss/settings.module.css";
import { showErrorToast, showSuccessToast } from "../../utils/toast-handler/showToast";
import requestInstance from "../../shared/axios/request-instance";
import { useEffect, useState } from "react";
import { useOAuthFlow } from "../../shared/hooks";

interface OAuthAccount {
  provider: Record<string, string>;
  isLinked: boolean;
  email?: string;
}

const Settings = () => {
  const [isFetching, setIsFetching] = useState(false);
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
      setIsFetching(true);
      try {
        const response = await requestInstance.get("/auth/oauth-providers");
        setOAuthAccounts(response.data);
      } catch (error: any) {
        showErrorToast(error);
      }
      setIsFetching(false);
    };

    if (fetchOAuthAccounts) {
      fetchAccounts();
      setFetchOAuthAccounts(false);
    }
  }, [fetchOAuthAccounts]);

  return (
    <AppLayout>
      <div className={classes.settings_sections}>
        <SettingsSection title="Linked Accounts" icon={<Link />}>
          {isFetching ? (
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
      </div>
    </AppLayout>
  );
};

export default Settings;
