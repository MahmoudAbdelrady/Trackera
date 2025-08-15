import { useAuthStore } from "../../../state/store";

const jwtInterceptor = (config: any) => {
  const token = useAuthStore.getState().token;
  if (token) {
    config.headers["Authorization"] = `Bearer ${token}`;
  }
  return config;
};

export default jwtInterceptor;
