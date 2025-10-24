import { Select } from "antd";
import { UserPreference } from "../../../";
import classes from "./scss/preferences-section.module.css";

const PreferencesSection = () => {
  const jiraSites = [
    { label: "Main", value: "https://test.atlassian.net" },
    { label: "Work", value: "https://work.atlassian.net" },
  ];

  return (
    <div className={classes.preferences_section}>
      <UserPreference label="Jira Main Site">
        <Select options={jiraSites} defaultValue={"https://test.atlassian.net"} disabled={jiraSites.length === 1} className={classes.preference_select} />
      </UserPreference>
    </div>
  );
};

export default PreferencesSection;
