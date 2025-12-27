import classes from "./scss/user-preference.module.css";

interface UserPreferenceProps {
  label: string;
  children: React.ReactNode;
}

const UserPreference = (props: UserPreferenceProps) => {
  const { label, children } = props;
  return (
    <div className={classes.preference}>
      <span className={classes.preference_label}>{label}:</span>
      <span className={classes.preference_value}>{children}</span>
    </div>
  );
};

export default UserPreference;
