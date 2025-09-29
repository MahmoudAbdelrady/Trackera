import { Link } from "lucide-react";
import { AppLayout, LinkedAccount, LoadingSpinner } from "../../components";
import { SettingsSection } from "../../components";
import classes from "./scss/settings.module.css";
import { showErrorToast, showSuccessToast } from "../../utils/toast-handler/showToast";
import requestInstance from "../../shared/axios/request-instance";
import { useEffect, useState } from "react";

interface OAuthAccount {
  provider: string;
  isLinked: boolean;
  email?: string;
}

const Settings = () => {
  const [isFetching, setIsFetching] = useState(false);
  const [fetchOAuthAccounts, setFetchOAuthAccounts] = useState(true);
  const [oAuthAccounts, setOAuthAccounts] = useState<OAuthAccount[]>([]);

  const linkProviderAccount = async (provider: string) => {
    try {
      const providerOAuthLink = await fetchOAuthFlowLink(provider);
      window.open(providerOAuthLink, `Link ${provider} Account`, "width=600,height=600");
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
      const response = await requestInstance.get(`/auth/oauth-v2/${provider}`);
      return response.data.url;
    } catch (error: any) {
      throw error;
    }
  };

  useEffect(() => {
    const handleMessage = (event: MessageEvent<any>) => {
      if (event.origin !== window.location.origin || event.data?.type !== "OAUTH_RESULT") return;
      console.log("Received event:", event.data);
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
                onUnlink={() => unlinkProviderAccount(account.provider)}
              />
            ))
          )}
        </SettingsSection>
      </div>
    </AppLayout>
  );
};

export default Settings;
