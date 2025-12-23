import { AuthLayout, AuthResult, LoadingSpinner } from "../../../components";
import { useNavigate, useSearchParams } from "react-router-dom";
import { useEffect, useState } from "react";
import type { AuthResultFields } from "../../../shared/types";
import { useAuthStore } from "../../../state/store";
import { authApis } from "../../../state/api";

const SecurityVerification = () => {
  const verificationTypeMessage: Record<string, string> = {
    "Account Activation": "You can now log in to your account.",
    "Change Password": "You can now log in with your new password.",
    "Password Reset": "You can now log in with your new password.",
  };

  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const [isVerifying, setIsVerifying] = useState<boolean>(true);
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const token = searchParams.get("token");

  const [verificationResult, setVerificationResult] = useState<AuthResultFields>({});

  useEffect(() => {
    if (!token) {
      setVerificationResult({
        title: "Url is expired or invalid",
        isError: true,
      });
    } else {
      const consumeToken = async () => {
        try {
          const result = await authApis.consumeToken(token);
          setVerificationResult({
            title: result.title,
            description: result.desc,
          });
        } catch (error: any) {
          setVerificationResult({
            title: error.response?.data?.message,
            isError: true,
          });
        }
      };

      consumeToken();
    }
    setIsVerifying(false);
  }, [navigate, token]);

  return isVerifying ? (
    <LoadingSpinner />
  ) : (
    <AuthLayout>
      <AuthResult
        title={verificationResult.title!}
        description={verificationResult.description}
        message={verificationTypeMessage[verificationResult.title!]}
        buttonText={`Back to ${isAuthenticated ? "Home" : "Login"}`}
        isError={verificationResult.isError}
        onClick={() => navigate(isAuthenticated ? "/" : "/login")}
      />
    </AuthLayout>
  );
};

export default SecurityVerification;
