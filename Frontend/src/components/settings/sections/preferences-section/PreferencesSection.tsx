import classes from "./scss/preferences-section.module.css";

const PreferencesSection = () => {
  return (
    <div className={classes.preferences_section}>
      <div className={classes.preference}>
        <span className={classes.preference_label}>Jira Main Site:</span>
        <span className={classes.preference_value}>Test</span>
      </div>
    </div>
  );
};

export default PreferencesSection;
