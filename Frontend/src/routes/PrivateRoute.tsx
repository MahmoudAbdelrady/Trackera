import { Navigate } from "react-router-dom";
import { userQueries } from "../state/queries";
import { AccessDenied, LoadingSpinner, ServerError } from "../components";
import { useAuthStore } from "../state/store";
import { categorizeAxiosError } from "../utils";

const PrivateRoute = ({ children }: { children: React.JSX.Element }) => {
  const { isAuthenticated, authChecked, errorType } = useAuthStore((state) => state);
  const meQuery = userQueries.useMeQuery();
  const meError = meQuery.isError ? categorizeAxiosError(meQuery.error) : null;

  const existingError = errorType ?? meError;

  if (!authChecked || meQuery.isLoading) return <LoadingSpinner />;

  if (!isAuthenticated && existingError === "AUTH") return <Navigate to="/login" />;

  if (existingError === "SERVER") return <ServerError />;

  if (existingError === "FORBIDDEN")
    return <AccessDenied message="You do not have permission to access this resource." />;

  return children;
};

export default PrivateRoute;
