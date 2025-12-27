import type { LoginFormFields } from "../../pages/auth/login/Login";
import type { SignUpFormFields } from "../../pages/auth/sign-up/SignUp";
import requestInstance from "../../shared/axios/request-instance";
import type { ChangePasswordFormFields } from "../../shared/types";

const login = async (authData: LoginFormFields) => {
  await requestInstance.post("/auth/login", authData);
};

const signUp = async (authData: SignUpFormFields) => {
  const response = await requestInstance.post("/auth/signup", authData);
  return response.data;
};

const isAuthenticated = async () => {
  await requestInstance.get("/auth/session");
};

const refreshToken = async () => {
  await requestInstance.post("/auth/jwt/refresh");
};

const logout = async () => {
  const response = await requestInstance.post("/auth/logout");
  return response.data;
};

const validateToken = async (token: string) => {
  await requestInstance.post(`/auth/token/validate?token=${token}`);
};

const consumeToken = async (token: string) => {
  const response = await requestInstance.post(`/auth/token/consume?token=${token}`);
  return response.data;
};

const requestResetPassword = async (email: string) => {
  const response = await requestInstance.post("/auth/password/request-reset", { email });
  return response.data;
};

const changePassword = async (token: string, passwordData: ChangePasswordFormFields) => {
  const { currentPassword, ...newPasswordInfo } = passwordData;
  const response = await requestInstance.post(`/auth/password?token=${token}`, { ...newPasswordInfo });
  return response.data;
};

const getOAuthFlowLink = async (provider: string, forceLink: boolean) => {
  const response = await requestInstance.get(`/auth/oauth/${provider}${forceLink ? "?forceLink=true" : ""}`);
  return response.data;
};

const oAuthCallback = async (provider: string, authCode: string, state: string) => {
  const response = await requestInstance.post(`/auth/oauth/${provider}/callback`, {
    authCode,
    state,
  });
  return response.data;
};

const unLinkOAuthProvider = async (provider: string) => {
  const response = await requestInstance.post(`/auth/oauth/unlink/${provider}`);
  return response.data;
};

const authApis = {
  login,
  signUp,
  isAuthenticated,
  refreshToken,
  logout,
  validateToken,
  consumeToken,
  requestResetPassword,
  changePassword,
  getOAuthFlowLink,
  oAuthCallback,
  unLinkOAuthProvider,
};

export default authApis;
