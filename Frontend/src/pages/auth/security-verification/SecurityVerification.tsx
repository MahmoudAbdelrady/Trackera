import { Check, X } from "lucide-react";
import { AuthLayout, LoadingSpinner } from "../../../components";
import authClasses from "../scss/auth.module.css";
import { useNavigate, useSearchParams } from "react-router-dom";
import { useEffect, useState } from "react";
import requestInstance from "../../../shared/api/request-instance";

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

  const [verificationTitle, setVerificationTitle] = useState<string>("");
  const [verificationDesc, setVerificationDesc] = useState<string>("");
  const [verificationIcon, setVerificationIcon] = useState<React.ReactNode>(
    <Check className={authClasses.response_icon} />
  );

  useEffect(() => {
    if (!token) {
      navigate("/login");
    } else {
      const processToken = async () => {
        try {
          const response = await requestInstance.post(
            `/auth/process-token?token=${token}`
          );
          setVerificationTitle(response.data.data.title);
          setVerificationDesc(response.data.data.description);
        } catch (error: any) {
          setVerificationTitle(error.response?.data?.message);
          setVerificationIcon(
            <X
              className={`${authClasses.response_icon} ${authClasses.error}`}
            />
          );
          console.log("Error processing token:", error);
        }
        setIsVerifying(false);
      };

      processToken();
    }
  }, [navigate, token]);

  return isVerifying ? (
    <LoadingSpinner />
  ) : (
    <AuthLayout
      title=""
      description=""
      submitButtonText=""
      onSubmit={() => {}}
      isSubmitBtnDisabled={false}
      isSubmitBtnLoading={false}
      showResponseContent={true}
      responseContent={{
        title: verificationTitle,
        description: verificationDesc,
        message: verificationTypeMessage[verificationTitle],
        icon: verificationIcon,
        buttonText: "Go to Login",
        onClick: () => navigate("/login"),
      }}
    >
      <></>
    </AuthLayout>
  );
};

export default SecurityVerification;
