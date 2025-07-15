import { Navigate } from "react-router-dom";

const PrivateRoute = ({ children }: { children: React.JSX.Element }) => {
  const isAuthenticated = true;

  return isAuthenticated ? children : <Navigate to="/login" />;
};

export default PrivateRoute;
