import { useEffect } from "react";
import { useParams, useSearchParams } from "react-router-dom";
import { LoadingSpinner } from "../../components";
import { authApis } from "../../state/api";

const OAuthCallback = () => {
  const [searchParams] = useSearchParams();
  const { provider } = useParams();

  useEffect(() => {
    const code = searchParams.get("code");
    const state = searchParams.get("state");
    const fetchOAuthUserInfo = async () => {
      try {
        const response = await authApis.oAuthCallback(provider!, code!, state!);
        window.opener.postMessage({ type: "OAUTH_RESULT", success: true, data: response.data }, window.location.origin);
        window.close();
      } catch (error: any) {
        window.opener.postMessage(
          { type: "OAUTH_RESULT", success: false, error: error.response?.data?.message },
          window.location.origin
        );
        window.close();
      }
    };

    if (provider && code && state) {
      fetchOAuthUserInfo();
    } else {
      window.opener.postMessage(
        { type: "OAUTH_RESULT", success: false, error: `Failed to authenticate with ${provider}` },
        window.location.origin
      );
      window.close();
    }
  }, []);

  return <LoadingSpinner />;
};

export default OAuthCallback;
