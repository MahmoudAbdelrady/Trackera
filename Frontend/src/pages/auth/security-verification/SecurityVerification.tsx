import { AuthLayout, AuthResult, LoadingSpinner } from "../../../components";
import { useNavigate, useSearchParams } from "react-router-dom";
import { useEffect, useState } from "react";
import requestInstance from "../../../shared/api/request-instance";

interface SecurityVerificationResult {
  title?: string;
  description?: string;
  isError?: boolean;
}

const SecurityVerification = () => {
  const verificationTypeMessage: Record<string, string> = {
    "Account Activation": "You can now log in to your account.",
    "Change Password": "You can now log in with your new password.",
    "Password Reset": "You can now log in with your new password.",
  };

  const [isVerifying, setIsVerifying] = useState<boolean>(true);
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const token = searchParams.get("token");

  const [verificationResult, setVerificationResult] =
    useState<SecurityVerificationResult>({});

  useEffect(() => {
    if (!token) {
      navigate("/login");
    } else {
      const processToken = async () => {
        try {
          const response = await requestInstance.post(
            `/auth/process-token?token=${token}`
          );
          setVerificationResult({
            title: response.data.data.title,
            description: response.data.data.desc,
          });
        } catch (error: any) {
          setVerificationResult({
            title: error.response?.data?.message,
            isError: true,
          });
        }
        setIsVerifying(false);
      };

      processToken();
    }
  }, [navigate, token]);

  return isVerifying ? (
    <LoadingSpinner />
  ) : (
    <AuthLayout>
      <AuthResult
        title={verificationResult.title!}
        description={verificationResult.description}
        message={verificationTypeMessage[verificationResult.title!]}
        buttonText="Go to Login"
        isError={verificationResult.isError}
        onClick={() => navigate("/login")}
      />
    </AuthLayout>
  );
};

export default SecurityVerification;
