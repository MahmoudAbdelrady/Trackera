import { Link } from "lucide-react";
import { AppLayout, LinkedAccount, LoadingSpinner } from "../../components";
import { SettingsSection } from "../../components";
import classes from "./scss/settings.module.css";
import { showErrorToast, showSuccessToast } from "../../utils/toast-handler/showToast";
import requestInstance from "../../shared/axios/request-instance";
import { useEffect, useState } from "react";

interface OAuthAccount {
  provider: Record<string, string>;
  isLinked: boolean;
  email?: string;
}

const Settings = () => {
  const [isFetching, setIsFetching] = useState(false);
  const [fetchOAuthAccounts, setFetchOAuthAccounts] = useState(true);
  const [oAuthAccounts, setOAuthAccounts] = useState<OAuthAccount[]>([]);

  const linkProviderAccount = async (provider: Record<string, string>) => {
    try {
      const providerOAuthLink = await fetchOAuthFlowLink(provider.code);
      window.open(providerOAuthLink, `Link ${provider.name} Account`, "width=600,height=600");
    } catch (error: any) {
      showErrorToast(error);
    }
  };

  const unlinkProviderAccount = async (provider: string) => {
    try {
      const response = await requestInstance.post(`/auth/unlink-oauth/${provider}`);
      showSuccessToast(response.data);
      setFetchOAuthAccounts(true);
    } catch (error: any) {
      showErrorToast(error);
    }
  };

  const fetchOAuthFlowLink = async (provider: string) => {
    try {
      const response = await requestInstance.get(`/auth/oauth/${provider}`);
      return response.data.url;
    } catch (error: any) {
      throw error;
    }
  };

  useEffect(() => {
    const handleMessage = (event: MessageEvent<any>) => {
      if (event.origin !== window.location.origin || event.data?.type !== "OAUTH_RESULT") return;
      const { success, error } = event.data;
      if (success) {
        showSuccessToast("Account linked successfully");
        setFetchOAuthAccounts(true);
      } else {
        showErrorToast(error || "Failed to link account");
      }
    };
    window.addEventListener("message", handleMessage);
    return () => window.removeEventListener("message", handleMessage);
  }, []);

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
                onLink={() => linkProviderAccount(account.provider)}
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
