import { create } from "zustand";
import type { AxiosErrorType } from "../../shared/types";

interface AuthState {
  isLoading: boolean;
  isAuthenticated: boolean;
  authChecked: boolean;
  errorType?: AxiosErrorType;
  setIsLoading: (val: boolean) => void;
  setAuthenticated: (isAuthenticated: boolean) => void;
  setAuthChecked: (authChecked: boolean) => void;
  setErrorType: (errorType?: AxiosErrorType) => void;
}

const useAuthStore = create<AuthState>((set) => ({
  isLoading: false,
  isAuthenticated: false,
  errorType: undefined,
  authChecked: false,
  setIsLoading: (val: boolean) => set({ isLoading: val }),
  setAuthenticated: (val: boolean) => set({ isAuthenticated: val }),
  setAuthChecked: (val: boolean) => set({ authChecked: val }),
  setErrorType: (errorType?: AxiosErrorType) => set({ errorType }),
}));

export default useAuthStore;
