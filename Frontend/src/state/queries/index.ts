import { QueryClient } from "@tanstack/react-query";
import userQueries from "./userQuery";

export { userQueries };

const queryClient = new QueryClient();
export default queryClient;
