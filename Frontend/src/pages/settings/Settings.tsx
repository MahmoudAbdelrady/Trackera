import { Link } from "lucide-react";
import { AppLayout, LinkedAccount } from "../../components";
import { SettingsSection } from "../../components";
import classes from "./scss/settings.module.css";

const Settings = () => {
  const linkJiraAccount = () => {
    window.location.href = `${import.meta.env.VITE_TRACKERA_BACKEND_URL}/auth/oauth-v2/jira`;
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
