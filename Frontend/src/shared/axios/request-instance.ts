import axios from "axios";
import { csrfInterceptor, refreshJwtInterceptor } from "./interceptors";
import { API_BASE_URL } from "../constants";

const requestInstance = axios.create({
  baseURL: API_BASE_URL,
  withCredentials: true,
});

requestInstance.interceptors.request.use(csrfInterceptor);
requestInstance.interceptors.response.use((res) => res, refreshJwtInterceptor);

export default requestInstance;
