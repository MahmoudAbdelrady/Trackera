import { useContext } from "react";
import SSEContext from "../contexts/SSEContext";

export const useSSEContext = () => {
  const context = useContext(SSEContext);
  if (!context) {
    throw new Error("useSSEContext must be used within SSEProvider");
  }
  return context;
};
