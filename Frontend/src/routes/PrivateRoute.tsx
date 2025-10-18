import { Navigate } from "react-router-dom";
import { userQueries } from "../state/queries";
import { LoadingSpinner } from "../components";
import { useAuthStore } from "../state/store";

const PrivateRoute = ({ children }: { children: React.JSX.Element }) => {
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const meQuery = userQueries.useMeQuery();

  if (!isAuthenticated) return <Navigate to="/login" />;

  if (meQuery.isLoading) return <LoadingSpinner />;

  return children;
};

export default PrivateRoute;
