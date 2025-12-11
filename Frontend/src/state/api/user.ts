import requestInstance from "../../shared/axios/request-instance";
import { useAuthStore } from "../store";

export interface UserInfo {
  firstname: string;
  lastname: string;
  primaryEmail: string;
  pendingEmail: string | null;
  profilePicture: string | null;
  avatarColor: string;
  passwordSet: boolean;
  jiraLinked: boolean;
}

const fetchMe = async () => {
  try {
    const response = await requestInstance.get<UserInfo>("/user/me");
    return response.data;
  } catch (error) {
    console.log("Error fetching user data:", error);
    throw error;
  }
};

const refreshToken = async () => {
  const response = await requestInstance.post("/auth/jwt/refresh");
  useAuthStore.getState().login(response.data.token);
};

const userApis = {
  fetchMe,
  refreshToken,
};

export default userApis;
