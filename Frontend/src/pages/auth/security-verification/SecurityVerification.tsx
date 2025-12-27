import { AuthLayout, AuthResult, LoadingSpinner, type AuthResultFields } from "../../../components";
import { useNavigate, useSearchParams } from "react-router-dom";
import { useCallback, useEffect, useState } from "react";
import { useAuthStore } from "../../../state/store";
import { authApis } from "../../../state/api";

const verificationTypeMessage: Record<string, string> = {
  "Account Activation": "You can now log in to your account.",
  "Change Password": "You can now log in with your new password.",
  "Password Reset": "You can now log in with your new password.",
};

const SecurityVerification = () => {
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const [isVerifying, setIsVerifying] = useState<boolean>(true);
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const token = searchParams.get("token");

  const [verificationResult, setVerificationResult] = useState<AuthResultFields>({});

  const consumeToken = useCallback(async (token: string) => {
    if (!token) {
      setVerificationResult({
        title: "Url is expired or invalid",
        isError: true,
      });
      return;
    }

    try {
      const result = await authApis.consumeToken(token);
      setVerificationResult({
        title: result.title,
        description: result.desc,
      });
    } catch (error: any) {
      setVerificationResult({
        title: error.response?.data?.message || "Verification Failed",
        isError: true,
      });
    }
  }, []);

  useEffect(() => {
    if (!token) {
      setVerificationResult({
        title: "Url is expired or invalid",
        isError: true,
      });
    } else {
      consumeToken(token);
    }
    setIsVerifying(false);
  }, [token]);

  return isVerifying ? (
    <LoadingSpinner />
  ) : (
    <AuthLayout>
      <AuthResult
        title={verificationResult.title || "Verification Failed"}
        description={verificationResult.description}
        message={verificationResult.title ? verificationTypeMessage[verificationResult.title] : "Something went wrong."}
        buttonText={`Back to ${isAuthenticated ? "Home" : "Login"}`}
        isError={verificationResult.isError}
        onClick={() => navigate(isAuthenticated ? "/" : "/login")}
      />
    </AuthLayout>
  );
};

export default SecurityVerification;
