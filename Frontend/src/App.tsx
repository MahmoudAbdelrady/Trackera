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
  return (
    <QueryClientProvider client={queryClient}>
      <Toaster />
      <RouterProvider router={router} />
    </QueryClientProvider>
  );
};

export default App;
