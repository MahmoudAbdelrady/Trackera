import { useNavigate } from "react-router-dom";
import { userQueries } from "../state/queries";
import { LoadingSpinner } from "../components";
import { showErrorToast } from "../utils/toast-handler/showToast";
import { useEffect } from "react";

const PrivateRoute = ({ children }: { children: React.JSX.Element }) => {
  const meQuery = userQueries.useMeQuery();
  const navigate = useNavigate();

  useEffect(() => {
    if (meQuery.isError) {
      showErrorToast(meQuery.error);
      navigate("/login");
    }
  }, [meQuery.isError]);

  if (!meQuery.isEnabled || meQuery.isLoading) return <LoadingSpinner />;

  return children;
};

export default PrivateRoute;
