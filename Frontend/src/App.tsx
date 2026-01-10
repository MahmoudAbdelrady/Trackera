import { RouterProvider, createBrowserRouter } from "react-router-dom";
import { GuestRoute, PrivateRoute } from "./routes";
import {
  Login,
  SignUp,
  SecurityVerification,
  Home,
  JiraTasks,
  Settings,
  WorklogDetails,
  ForgotPassword,
  ChangePassword,
  OAuthCallback,
  PrivacyPolicy,
  NotFound,
} from "./pages";
import { Toaster } from "react-hot-toast";
import { QueryClientProvider } from "@tanstack/react-query";
import { GoogleOAuthProvider } from "@react-oauth/google";
import queryClient from "./state/queries";
import { useEffect } from "react";
import { authApis } from "./state/api";
import { useAuthStore } from "./state/store";
import { SSEContextProvider } from "./shared/contexts";
import { categorizeAxiosError } from "./utils";

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
    element: <ChangePassword />,
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
    path: "/jira-tasks",
    element: (
      <PrivateRoute>
        <JiraTasks />
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
    path: "/oauth/:provider/callback",
    element: <OAuthCallback />,
  },
  {
    path: "/privacy-policy",
    element: <PrivacyPolicy />,
  },
  {
    path: "*",
    element: <NotFound />,
  },
]);

const App = () => {
  useEffect(() => {
    const checkAuth = async () => {
      const authState = useAuthStore.getState();
      authState.setIsLoading(true);
      let isAuthenticated: boolean;
      try {
        await authApis.isAuthenticated();
        isAuthenticated = true;
        authState.setErrorType(undefined);
      } catch (error: unknown) {
        const errorCategory = categorizeAxiosError(error);
        if (errorCategory) {
          authState.setErrorType(errorCategory);
        }
        isAuthenticated = false;
      }
      authState.setIsLoading(false);
      authState.setAuthenticated(isAuthenticated);
      authState.setAuthChecked(true);
    };

    checkAuth();
  }, []);

  return (
    <SSEContextProvider>
      <GoogleOAuthProvider clientId={import.meta.env.VITE_TRACKERA_GOOGLE_CLIENT_ID}>
        <QueryClientProvider client={queryClient}>
          <Toaster />
          <RouterProvider router={router} />
        </QueryClientProvider>
      </GoogleOAuthProvider>
    </SSEContextProvider>
  );
};

export default App;
