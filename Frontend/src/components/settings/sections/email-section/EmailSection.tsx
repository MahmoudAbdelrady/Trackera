import { useFormik } from "formik";
import { useState } from "react";
import { emailSchema } from "../../../../shared/yup-schemas";
import { Button } from "antd";
import InputField from "../../../input-field/InputField";
import { Mail } from "lucide-react";
import classes from "./scss/email-section.module.css";
import { showErrorToast, showSuccessToast } from "../../../../utils/toast-handler/showToast";
import { userQueries } from "../../../../state/queries";
import StatusBadge from "../../../status-badge/StatusBadge";
import { isEqual } from "lodash";
import { userApis } from "../../../../state/api";

const EmailSection = () => {
  const { data: userData, refetch: refetchUser } = userQueries.useMeQuery();
  const [isPerformingAction, setIsPerformingAction] = useState(false);
  const [showChangeEmail, setShowChangeEmail] = useState(false);

  const changeEmailFormik = useFormik({
    initialValues: {
      email: userData?.primaryEmail || "",
    },
    validationSchema: emailSchema,
    onSubmit: async (values) => {
      setIsPerformingAction(true);
      try {
        const result = await userApis.requestEmailChange(values.email);
        await refetchUser();
        setShowChangeEmail(false);
        changeEmailFormik.resetForm();
        showSuccessToast(result);
      } catch (error: any) {
        showErrorToast(error);
      }
      setIsPerformingAction(false);
    },
  });

  const resendVerificationEmail = async () => {
    setIsPerformingAction(true);
    try {
      const result = await userApis.sendEmailVerification();
      showSuccessToast(result);
    } catch (error: any) {
      showErrorToast(error);
    }
    setIsPerformingAction(false);
  };

  const removePendingEmail = async () => {
    setIsPerformingAction(true);
    try {
      const result = await userApis.removePendingEmail();
      await refetchUser();
      showSuccessToast(result);
    } catch (error: any) {
      showErrorToast(error);
    }
    setIsPerformingAction(false);
  };

  return (
    <div className={classes.user_emails}>
      <form onSubmit={changeEmailFormik.handleSubmit} className={classes.email_item}>
        <div className={classes.info}>
          {showChangeEmail ? (
            <InputField
              icon={<Mail />}
              placeholder="Enter your email"
              name="email"
              value={changeEmailFormik.values.email}
              onChange={changeEmailFormik.handleChange}
              onBlur={changeEmailFormik.handleBlur}
              type="email"
              disabled={isPerformingAction}
              error={
                changeEmailFormik.touched.email && changeEmailFormik.errors.email
                  ? changeEmailFormik.errors.email
                  : undefined
              }
            />
          ) : (
            <>
              <span>{userData?.primaryEmail}</span>
              <StatusBadge label="Primary" type="main" />
            </>
          )}
        </div>
        <div className={classes.actions}>
          {showChangeEmail ? (
            <>
              <Button
                type="primary"
                htmlType="submit"
                className={classes.action_btn}
                loading={isPerformingAction}
                disabled={
                  !changeEmailFormik.isValid || isEqual(changeEmailFormik.initialValues, changeEmailFormik.values)
                }
              >
                Save Changes
              </Button>
              <Button
                type="default"
                className={classes.action_btn}
                onClick={() => {
                  setShowChangeEmail(false);
                  changeEmailFormik.resetForm();
                }}
              >
                Cancel
              </Button>
            </>
          ) : (
            !userData?.pendingEmail && (
              <Button type="primary" className={classes.action_btn} onClick={() => setShowChangeEmail(true)}>
                Change Email
              </Button>
            )
          )}
        </div>
      </form>
      {userData?.pendingEmail && (
        <div className={classes.email_item}>
          <div className={classes.info}>
            <span>{userData?.pendingEmail}</span>
            <StatusBadge label="Pending" type="warning" />
          </div>
          <div className={classes.actions}>
            <Button
              type="primary"
              className={classes.action_btn}
              loading={isPerformingAction}
              disabled={isPerformingAction}
              onClick={resendVerificationEmail}
            >
              Resend Verification
            </Button>
            <Button
              type="default"
              danger
              className={classes.action_btn}
              onClick={removePendingEmail}
              loading={isPerformingAction}
              disabled={isPerformingAction}
            >
              Delete
            </Button>
          </div>
        </div>
      )}
    </div>
  );
};

export default EmailSection;
