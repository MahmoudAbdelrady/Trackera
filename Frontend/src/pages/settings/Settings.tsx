import { Link } from "lucide-react";
import { AppLayout, LinkedAccount } from "../../components";
import { SettingsSection } from "../../components";
import classes from "./scss/settings.module.css";
import { showErrorToast, showSuccessToast } from "../../utils/toast-handler/showToast";
import requestInstance from "../../shared/axios/request-instance";
import { useEffect } from "react";

const Settings = () => {
  const linkJiraAccount = async () => {
    const jiraOAuthLink = await fetchOAuthFlowLink("jira");
    window.open(jiraOAuthLink, "Link Jira Account", "width=600,height=600");
  };

  const fetchOAuthFlowLink = async (provider: string) => {
    try {
      const response = await requestInstance.get(`/auth/oauth-v2/${provider}`);
      return response.data.url;
    } catch (error: any) {
      showErrorToast(error);
    }
  };

  useEffect(() => {
    const handleMessage = (event: MessageEvent<any>) => {
      if (event.origin !== window.location.origin || event.data?.type !== "OAUTH_RESULT") return;
      console.log("Received event:", event.data);
      const { success, error } = event.data;
      if (success) {
        showSuccessToast("Account linked successfully");
        // refetch linked accounts
      } else {
        showErrorToast(error || "Failed to link account");
      }
    };
    window.addEventListener("message", handleMessage);
    return () => window.removeEventListener("message", handleMessage);
  }, []);

  return (
    <AppLayout>
      <div className={classes.settings_sections}>
        <SettingsSection title="Linked Accounts" icon={<Link />}>
          <LinkedAccount platform="Jira" icon={<img src="/Assets/jira_icon.svg" alt="Jira" />} accountIdentifier="user@example.com" isLinked={false} onLink={linkJiraAccount} onUnlink={() => {}} />
        </SettingsSection>
      </div>
    </AppLayout>
  );
};

export default Settings;
