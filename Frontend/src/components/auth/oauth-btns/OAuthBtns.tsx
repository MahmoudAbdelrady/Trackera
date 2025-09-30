import { Button } from "antd";
import type { OAuthBtnProps } from "../../../shared/types";
import { showErrorToast } from "../../../utils/toast-handler/showToast";
import requestInstance from "../../../shared/axios/request-instance";
import { useNavigate } from "react-router-dom";
import { useAuthStore } from "../../../state/store";
import classes from "./scss/oauth-btns.module.css";
import { useEffect } from "react";

const OAuthBtns = ({ disabled }: { disabled?: boolean }) => {
  const authStore = useAuthStore();
  const navigate = useNavigate();

  const linkProviderAccount = async (provider: string) => {
    try {
      const providerOAuthLink = await fetchOAuthFlowLink(provider);
      window.open(providerOAuthLink, `Link ${provider} Account`, "width=600,height=600");
    } catch (error: any) {
      showErrorToast(error);
    }
  };

  const fetchOAuthFlowLink = async (provider: string) => {
    try {
      const response = await requestInstance.get(`/auth/oauth/${provider}`);
      return response.data.url;
    } catch (error: any) {
      throw error;
    }
  };

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

  useEffect(() => {
    const handleMessage = (event: MessageEvent<any>) => {
      if (event.origin !== window.location.origin || event.data?.type !== "OAUTH_RESULT") return;
      const { success, error, data } = event.data;
      if (success) {
        authStore.login(data.token);
        navigate("/");
      } else {
        showErrorToast(error || "Failed to authenticate");
      }
    };
    window.addEventListener("message", handleMessage);
    return () => window.removeEventListener("message", handleMessage);
  }, []);

  return (
    <div className={classes.auth_buttons}>
      {oAuthButtons.map((btn, index) => (
        <Button key={index} className={classes.auth_btn} onClick={btn.onClick} icon={<div className={classes.icon_container}>{btn.icon}</div>} disabled={disabled}>
          {btn.label}
        </Button>
      ))}
    </div>
  );
};

export default OAuthBtns;
