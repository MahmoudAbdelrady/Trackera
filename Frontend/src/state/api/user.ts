import requestInstance from "../../shared/axios/request-instance";
import type { ChangePasswordFormFields } from "../../shared/types";
import type { UserInfo } from "../../shared/types/auth";

const fetchMe = async () => {
  const response = await requestInstance.get<UserInfo>("/user/me");
  return response.data;
};

const getOAuthProviders = async () => {
  const response = await requestInstance.get("/user/oauth-providers");
  return response.data;
};

const getPreferences = async () => {
  const response = await requestInstance.get("/user/preferences");
  return response.data;
};

const changePassword = async (passwordData: ChangePasswordFormFields) => {
  const response = await requestInstance.post("/user/password", { ...passwordData });
  return response.data;
};

const requestEmailChange = async (email: string) => {
  const response = await requestInstance.post("/user/email/request-change", { email });
  return response.data;
};

const sendEmailVerification = async () => {
  const response = await requestInstance.post("/user/email/send-verification");
  return response.data;
};

const removePendingEmail = async () => {
  const response = await requestInstance.delete("/user/email/pending");
  return response.data;
};

const updatePreferences = async (preferences: Record<string, any>) => {
  const response = await requestInstance.post("/user/preferences", { ...preferences });
  return response.data;
};

const userApis = {
  fetchMe,
  getOAuthProviders,
  getPreferences,
  changePassword,
  requestEmailChange,
  sendEmailVerification,
  removePendingEmail,
  updatePreferences,
};

export default userApis;
