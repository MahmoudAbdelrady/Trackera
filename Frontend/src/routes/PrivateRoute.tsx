import { Navigate } from "react-router-dom";
import { userQueries } from "../state/queries";
import { LoadingSpinner, ServerError } from "../components";
import { useAuthStore } from "../state/store";

const PrivateRoute = ({ children }: { children: React.JSX.Element }) => {
  const { isAuthenticated, authChecked, isError: isAuthError } = useAuthStore((state) => state);
  const meQuery = userQueries.useMeQuery();

  if (!authChecked || meQuery.isLoading) return <LoadingSpinner />;

  if (!isAuthenticated && !isAuthError) return <Navigate to="/login" />;

  if (isAuthError || meQuery.isError) return <ServerError />;

  return children;
};

export default PrivateRoute;
