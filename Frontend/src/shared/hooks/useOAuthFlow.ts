import { useEffect, useCallback } from "react";
import requestInstance from "../axios/request-instance";

type OAuthCallbacks = {
  onSuccess: (data: any) => void;
  onError?: (error: string) => void;
};

export function useOAuthFlow({ onSuccess, onError }: OAuthCallbacks) {
  const fetchOAuthFlowLink = useCallback(async (provider: string) => {
    try {
      const response = await requestInstance.get(`/auth/oauth/${provider}`);
      return response.data.url;
    } catch (error: any) {
      throw error;
    }
  }, []);

  const linkProviderAccount = useCallback(
    async (provider: string) => {
      try {
        const providerOAuthLink = await fetchOAuthFlowLink(provider);
        window.open(providerOAuthLink, `Link ${provider} Account`, "width=600,height=600");
      } catch (error: any) {
        onError?.(error.message || "Failed to link account");
      }
    },
    [fetchOAuthFlowLink, onError]
  );

  useEffect(() => {
    const handleMessage = (event: MessageEvent<any>) => {
      if (event.origin !== window.location.origin || event.data?.type !== "OAUTH_RESULT") return;
      const { success, error, data } = event.data;
      if (success) {
        onSuccess(data);
      } else {
        onError?.(error);
      }
    };
    window.addEventListener("message", handleMessage);
    return () => window.removeEventListener("message", handleMessage);
  }, [onSuccess, onError]);

  return { linkProviderAccount };
}
