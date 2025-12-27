import { Button } from "antd";
import { showErrorToast } from "../../../utils/toast-handler/showToast";
import { useNavigate } from "react-router-dom";
import { useAuthStore } from "../../../state/store";
import classes from "./scss/oauth-btns.module.css";
import { useOAuthFlow } from "../../../shared/hooks";

interface OAuthBtnProps {
  label: string;
  icon: React.ReactNode;
  onClick: () => void;
}

const OAuthBtns = ({ disabled }: { disabled?: boolean }) => {
  const authStore = useAuthStore();
  const navigate = useNavigate();
  const { linkProviderAccount } = useOAuthFlow({
    onSuccess: () => {
      authStore.setAuthenticated(true);
      navigate("/");
    },
    onError: (error) => {
      showErrorToast(error);
    },
  });

  const oAuthButtons: OAuthBtnProps[] = [
    {
      label: "Continue with Google",
      icon: <img src="./Assets/google_logo.webp" alt="Google Icon" />,
      onClick: () => linkProviderAccount("google"),
    },
    {
      label: "Continue with Jira",
      icon: <img src="./Assets/jira_icon.svg" alt="Jira Icon" />,
      onClick: () => linkProviderAccount("jira"),
    },
  ];

  return (
    <div className={classes.auth_buttons}>
      {oAuthButtons.map((btn, index) => (
        <Button
          key={index}
          className={classes.auth_btn}
          onClick={btn.onClick}
          icon={<div className={classes.icon_container}>{btn.icon}</div>}
          disabled={disabled}
        >
          {btn.label}
        </Button>
      ))}
    </div>
  );
};

export default OAuthBtns;
