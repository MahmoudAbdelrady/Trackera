import requestInstance from "../../shared/axios/request-instance";

export interface UserInfo {
  firstname: string;
  lastname: string;
  primaryEmail: string;
  pendingEmail: string | null;
  profilePicture: string | null;
  avatarColor: string;
  passwordSet: boolean;
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

const userApis = {
  fetchMe,
};

export default userApis;
