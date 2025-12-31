import { create } from "zustand";

interface AuthState {
  isLoading: boolean;
  isAuthenticated: boolean;
  authChecked: boolean;
  isError: boolean;
  setIsLoading: (val: boolean) => void;
  setAuthenticated: (isAuthenticated: boolean) => void;
  setAuthChecked: (authChecked: boolean) => void;
  setError: (isError: boolean) => void;
}

const useAuthStore = create<AuthState>((set) => ({
  isLoading: false,
  isAuthenticated: false,
  isError: false,
  authChecked: false,
  setIsLoading: (val: boolean) => set({ isLoading: val }),
  setAuthenticated: (val: boolean) => set({ isAuthenticated: val }),
  setAuthChecked: (val: boolean) => set({ authChecked: val }),
  setError: (val: boolean) => set({ isError: val }),
}));

export default useAuthStore;
