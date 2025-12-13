import axios from "axios";
import { refreshJwtInterceptor } from "./interceptors";

const requestInstance = axios.create({
  baseURL: import.meta.env.VITE_TRACKERA_BACKEND_URL,
  withCredentials: true,
});

requestInstance.interceptors.response.use((res) => res, refreshJwtInterceptor);

export default requestInstance;
