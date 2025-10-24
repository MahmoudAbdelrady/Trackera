import classes from "./scss/user-preference.module.css";

const UserPreference = ({ label, children }: { label: string; children: React.ReactNode }) => {
  return (
    <div className={classes.preference}>
      <span className={classes.preference_label}>{label}:</span>
      <span className={classes.preference_value}>{children}</span>
    </div>
  );
};

export default UserPreference;
