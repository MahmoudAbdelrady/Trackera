import { create } from "zustand";

interface AuthState {
  isAuthenticated: boolean;
  isLoading: boolean;
  isError: boolean;
  setAuthenticated: (isAuthenticated: boolean) => void;
  setIsLoading: (isLoading: boolean) => void;
  setError: (isError: boolean) => void;
}

const useAuthStore = create<AuthState>((set) => ({
  isAuthenticated: false,
  isError: false,
  isLoading: false,
  setAuthenticated: (val: boolean) => set({ isAuthenticated: val }),
  setIsLoading: (val: boolean) => set({ isLoading: val }),
  setError: (val: boolean) => set({ isError: val }),
}));

export default useAuthStore;
