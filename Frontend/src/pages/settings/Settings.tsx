import { Link } from "lucide-react";
import { AppLayout, LinkedAccount } from "../../components";
import { SettingsSection } from "../../components";
import classes from "./scss/settings.module.css";
import { showErrorToast } from "../../utils/toast-handler/showToast";
import requestInstance from "../../shared/axios/request-instance";

const Settings = () => {
  const linkJiraAccount = async () => {
    const jiraOAuthLink = await fetchOAuthFlowLink("jira");
    window.open(jiraOAuthLink, "Link Jira Account", "width=600,height=600");
    window.addEventListener("message", async (event) => {
      if (event.origin !== window.location.origin) return;
      if (event.data?.code) {
        console.log("Received OAuth code:", event.data.code);
      }
    });
  };

  const fetchOAuthFlowLink = async (provider: string) => {
    try {
      const response = await requestInstance.get(`/auth/oauth-v2/${provider}`);
      return response.data.url;
    } catch (error: any) {
      showErrorToast(error);
    }
  };

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
