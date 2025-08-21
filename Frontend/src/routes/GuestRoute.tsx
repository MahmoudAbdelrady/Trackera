import type React from "react";
import { Navigate } from "react-router-dom";
import { useAuthStore } from "../state/store";

const GuestRoute = ({ children }: { children: React.JSX.Element }) => {
  const authStore = useAuthStore();
  return authStore.isAuthenticated ? <Navigate to="/" /> : children;
};

export default GuestRoute;
