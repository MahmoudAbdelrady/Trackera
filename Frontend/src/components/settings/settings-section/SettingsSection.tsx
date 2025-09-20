import type { SettingsSectionProps } from "../../../shared/types";
import classes from "./scss/settings-section.module.css";

const SettingsSection = (props: SettingsSectionProps) => {
  return (
    <div className={classes.settings_section}>
      <div className={classes.header}>
        <div className={classes.header_icon}>{props.icon}</div>
        <div className={classes.header_title}>{props.title}</div>
      </div>
      <div className={classes.content}>{props.children}</div>
    </div>
  );
};

export default SettingsSection;
