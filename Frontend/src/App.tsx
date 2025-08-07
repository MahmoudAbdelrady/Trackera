import { RouterProvider, createBrowserRouter } from "react-router-dom";
import { GuestRoute, PrivateRoute } from "./routes";
import {
  Login,
  SignUp,
  Home,
  Tasks,
  Settings,
  Test,
  WorklogDetails,
  ForgotPassword,
  ChangePassword,
} from "./pages";
import { Toaster } from "react-hot-toast";

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

const App = () => {
  return (
    <>
      <Toaster />
      <RouterProvider router={router} />
    </>
  );
};

export default App;
