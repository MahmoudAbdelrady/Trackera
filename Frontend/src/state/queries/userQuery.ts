import { useQuery } from "@tanstack/react-query";
import { userApis } from "../api";
import type { UserInfo } from "../../shared/types";
import { useAuthStore } from "../store";

const useMeQuery = () => {
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  return useQuery<UserInfo>({
    queryKey: ["me"],
    queryFn: userApis.fetchMe,
    enabled: isAuthenticated,
    retry: false,
    refetchOnMount: false,
    refetchOnWindowFocus: false,
    refetchOnReconnect: true,
  });
};

const userQueries = {
  useMeQuery,
};

export default userQueries;
