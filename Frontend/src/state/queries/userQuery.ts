import { useQuery } from "@tanstack/react-query";
import { userApis } from "../api";
import type { UserInfo } from "../api/user";

const useMeQuery = () => {
  return useQuery<UserInfo>({
    queryKey: ["me"],
    queryFn: userApis.fetchMe,
    retry: false,
  });
};

const userQueries = {
  useMeQuery,
};

export default userQueries;
