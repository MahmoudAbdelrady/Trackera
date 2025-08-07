import axios from "axios";

const requestInstance = axios.create({
  baseURL: import.meta.env.VITE_TRACKERA_BACKEND_URL,
});

export default requestInstance;
