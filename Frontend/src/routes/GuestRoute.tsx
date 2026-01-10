import type React from "react";
import { Navigate } from "react-router-dom";
import { useAuthStore } from "../state/store";
import { LoadingSpinner, ServerError } from "../components";

const GuestRoute = ({ children }: { children: React.JSX.Element }) => {
  const { isAuthenticated, isLoading: isAuthLoading, errorType } = useAuthStore((state) => state);

  if (isAuthLoading) return <LoadingSpinner />;

  if (isAuthenticated) return <Navigate to="/" />;

  return errorType === "SERVER" ? <ServerError /> : children;
};

export default GuestRoute;
