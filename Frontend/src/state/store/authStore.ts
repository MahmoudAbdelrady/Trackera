import { create } from "zustand";

interface AuthState {
  isAuthenticated: boolean;
  authChecked: boolean;
  isError: boolean;
  setAuthenticated: (isAuthenticated: boolean) => void;
  setAuthChecked: (authChecked: boolean) => void;
  setError: (isError: boolean) => void;
}

const useAuthStore = create<AuthState>((set) => ({
  isAuthenticated: false,
  isError: false,
  authChecked: false,
  setAuthenticated: (val: boolean) => set({ isAuthenticated: val }),
  setAuthChecked: (val: boolean) => set({ authChecked: val }),
  setError: (val: boolean) => set({ isError: val }),
}));

export default useAuthStore;
