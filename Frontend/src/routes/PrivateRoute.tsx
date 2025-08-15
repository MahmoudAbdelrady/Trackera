import { Navigate } from "react-router-dom";
import { useAuthStore } from "../state/store";

const PrivateRoute = ({ children }: { children: React.JSX.Element }) => {
  const authStore = useAuthStore();
  return authStore.isAuthenticated ? children : <Navigate to="/login" />;
};

export default PrivateRoute;
