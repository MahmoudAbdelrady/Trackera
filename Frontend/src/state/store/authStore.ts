import { create } from "zustand";
import queryClient from "../queries";

interface AuthState {
  token: string | null;
  isAuthenticated: boolean;
  login: (token: string | null) => void;
  logout: () => void;
}

const useAuthStore = create<AuthState>((set) => ({
  token: localStorage.getItem("token"),
  isAuthenticated: !!localStorage.getItem("token"),
  login: (token) => {
    localStorage.setItem("token", token!);
    set({ token, isAuthenticated: true });
  },
  logout: () => {
    localStorage.removeItem("token");
    set({ token: null, isAuthenticated: false });
    queryClient.removeQueries({ queryKey: ["me"] });
  },
}));

export default useAuthStore;
