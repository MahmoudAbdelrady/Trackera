import type React from "react";
import { Navigate } from "react-router-dom";

const GuestRoute = ({ children }: { children: React.JSX.Element }) => {
  const isAuthenticated = false;
  return isAuthenticated ? <Navigate to="/" /> : children;
};

export default GuestRoute;
