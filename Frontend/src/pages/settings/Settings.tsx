import { Link } from "lucide-react";
import { AppLayout, LinkedAccount } from "../../components";
import { SettingsSection } from "../../components";
import classes from "./scss/settings.module.css";

const Settings = () => {
  return (
    <AppLayout>
      <div className={classes.settings_sections}>
        <SettingsSection title="Linked Accounts" icon={<Link />}>
          <LinkedAccount platform="Jira" icon={<img src="/Assets/jira_icon.svg" alt="Jira" />} accountIdentifier="user@example.com" isLinked={true} onLink={() => {}} onUnlink={() => {}} />
        </SettingsSection>
      </div>
    </AppLayout>
  );
};

export default Settings;
