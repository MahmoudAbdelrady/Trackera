import { RouterProvider, createBrowserRouter } from "react-router-dom";
import { GuestRoute, PrivateRoute } from "./routes";
import {
  Login,
  SignUp,
  SecurityVerification,
  Home,
  Tasks,
  Settings,
  Test,
  WorklogDetails,
  ForgotPassword,
  ChangePassword,
} from "./pages";
import { Toaster } from "react-hot-toast";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { GoogleOAuthProvider } from "@react-oauth/google";
import { userQueries } from "./state/queries";
import { LoadingSpinner } from "./components";
import { showErrorToast } from "./utils/toast-handler/show-toast";

const router = createBrowserRouter([
  {
    path: "/login",
    element: (
      <GuestRoute>
        <Login />
      </GuestRoute>
    ),
  },
  {
    path: "/sign-up",
    element: (
      <GuestRoute>
        <SignUp />
      </GuestRoute>
    ),
  },
  {
    path: "/forgot-password",
    element: (
      <GuestRoute>
        <ForgotPassword />
      </GuestRoute>
    ),
  },
  {
    path: "/change-password",
    element: (
      <GuestRoute>
        <ChangePassword />
      </GuestRoute>
    ),
  },
  {
    path: "/security-verification",
    element: <SecurityVerification />,
  },
  {
    path: "/",
    element: (
      <PrivateRoute>
        <Home />
      </PrivateRoute>
    ),
  },
  {
    path: "/worklog-details/:worklogId",
    element: (
      <PrivateRoute>
        <WorklogDetails />
      </PrivateRoute>
    ),
  },
  {
    path: "/tasks",
    element: (
      <PrivateRoute>
        <Tasks />
      </PrivateRoute>
    ),
  },
  {
    path: "/settings",
    element: (
      <PrivateRoute>
        <Settings />
      </PrivateRoute>
    ),
  },
  {
    path: "/test",
    element: (
      <PrivateRoute>
        <Test />
      </PrivateRoute>
    ),
  },
]);

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 0,
      refetchOnMount: "always",
      refetchOnWindowFocus: "always",
    },
  },
});

const App = () => {
  const meQuery = userQueries.useMeQuery();
  if (meQuery.isLoading) return <LoadingSpinner />;
  else if (meQuery.isError) showErrorToast(meQuery.error);
  else
    return (
      <GoogleOAuthProvider
        clientId={import.meta.env.VITE_TRACKERA_GOOGLE_CLIENT_ID}
      >
        <QueryClientProvider client={queryClient}>
          <Toaster />
          <RouterProvider router={router} />
        </QueryClientProvider>
      </GoogleOAuthProvider>
    );
};

export default App;
