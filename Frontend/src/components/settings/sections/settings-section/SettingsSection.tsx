import classes from "./scss/settings-section.module.css";

interface SettingsSectionProps {
  title: string;
  icon: React.ReactNode;
  children: React.ReactNode;
}

const SettingsSection = (props: SettingsSectionProps) => {
  const { title, icon, children } = props;

  return (
    <div className={classes.settings_section}>
      <div className={classes.header}>
        <div className={classes.header_icon}>{icon}</div>
        <div className={classes.header_title}>{title}</div>
      </div>
      <div className={classes.content}>{children}</div>
    </div>
  );
};

export default SettingsSection;
