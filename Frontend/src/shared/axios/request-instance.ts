import axios from "axios";
import { csrfInterceptor, refreshJwtInterceptor } from "./interceptors";

const requestInstance = axios.create({
  baseURL: import.meta.env.VITE_TRACKERA_BACKEND_URL,
  withCredentials: true,
});

requestInstance.interceptors.request.use(csrfInterceptor);
requestInstance.interceptors.response.use((res) => res, refreshJwtInterceptor);

export default requestInstance;
