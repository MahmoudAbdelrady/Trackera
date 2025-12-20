import requestInstance from "../../shared/axios/request-instance";

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
  const response = await requestInstance.get<UserInfo>("/user/me");
  return response.data;
};

const userApis = {
  fetchMe,
};

export default userApis;
