import { useEffect } from "react";
import requestInstance from "../../shared/axios/request-instance";
import { useParams, useSearchParams } from "react-router-dom";
import { LoadingSpinner } from "../../components";

const OAuthCallback = () => {
  const [searchParams] = useSearchParams();
  const { provider } = useParams();

  useEffect(() => {
    const code = searchParams.get("code");
    const state = searchParams.get("state");
    const fetchOAuthUserInfo = async () => {
      try {
        const response = await requestInstance.post(`/auth/oauth-v2/${provider}/callback`, {
          authCode: code,
          state,
        });
        window.opener.postMessage({ type: "OAUTH_RESULT", success: true, data: response.data }, window.location.origin);
        window.close();
      } catch (error: any) {
        window.opener.postMessage({ type: "OAUTH_RESULT", success: false, error: error.response?.data?.message }, window.location.origin);
        window.close();
      }
    };

    if (provider && code && state) {
      fetchOAuthUserInfo();
    } else {
      window.opener.postMessage({ type: "OAUTH_RESULT", success: false, error: "missing provider/code/state" }, window.location.origin);
      window.close();
    }
  }, []);

  return <LoadingSpinner />;
};

export default OAuthCallback;
