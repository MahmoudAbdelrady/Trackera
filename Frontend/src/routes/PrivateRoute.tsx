import { Navigate } from "react-router-dom";
import { userQueries } from "../state/queries";
import { LoadingSpinner } from "../components";
import { showErrorToast } from "../utils/toast-handler/showToast";

const PrivateRoute = ({ children }: { children: React.JSX.Element }) => {
  const meQuery = userQueries.useMeQuery();
  let navigateToLogin: boolean = false;
  if (!meQuery.isEnabled) navigateToLogin = true;
  else if (meQuery.isLoading) return <LoadingSpinner />;
  else if (meQuery.isError) {
    showErrorToast(meQuery.error);
    navigateToLogin = true;
  }

  return navigateToLogin ? <Navigate to="/login" /> : children;
};

export default PrivateRoute;
