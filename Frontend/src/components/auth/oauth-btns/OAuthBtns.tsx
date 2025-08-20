import { Button } from "antd";
import type { OAuthBtnProps } from "../../../shared/types";
import { useGoogleLogin, useGoogleOneTapLogin } from "@react-oauth/google";
import { showErrorToast } from "../../../utils/toast-handler/showToast";
import requestInstance from "../../../shared/axios/request-instance";
import { useNavigate } from "react-router-dom";
import { useAuthStore } from "../../../state/store";
import classes from "./scss/oauth-btns.module.css";

const OAuthBtns = () => {
  const authStore = useAuthStore();
  const navigate = useNavigate();

  const googleAuthHandler = useGoogleLogin({
    flow: "auth-code",
    onSuccess: async (tokenResponse: any) =>
      oAuthHandler(tokenResponse.code, "GOOGLE"),
    onError: () => showErrorToast("Google login failed"),
  });

  const oAuthHandler = async (tokenCode: string, provider: string) => {
    try {
      const response = await requestInstance.post("/auth/oauth", {
        tokenCode,
        provider,
      });
      authStore.login(response.data.token);
      navigate("/");
    } catch (error) {
      showErrorToast(error);
    }
  };

  useGoogleOneTapLogin({
    auto_select: false,
    cancel_on_tap_outside: false,
    onSuccess: async (tokenResponse: any) =>
      oAuthHandler(tokenResponse.credential, "GOOGLE"),
    onError: () => showErrorToast("Google One Tap login failed"),
  });

  const oAuthButtons: OAuthBtnProps[] = [
    {
      label: "Continue with Google",
      icon: <img src="./Assets/google_logo.webp" alt="Google Icon" />,
      provider: "GOOGLE",
      onClick: googleAuthHandler,
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
        >
          {btn.label}
        </Button>
      ))}
    </div>
  );
};

export default OAuthBtns;
