import { useFormik } from "formik";
import { useState } from "react";
import { emailSchema } from "../../../../shared/yup-schemas";
import { Button } from "antd";
import InputField from "../../../input-field/InputField";
import { Mail } from "lucide-react";
import inputFieldClasses from "../../../input-field/scss/input-field.module.css";
import classes from "./scss/email-section.module.css";
import { showErrorToast, showSuccessToast } from "../../../../utils/toast-handler/showToast";
import { userQueries } from "../../../../state/queries";
import StatusBadge from "../../../status-badge/StatusBadge";
import { isEqual } from "lodash";
import { userApis } from "../../../../state/api";

const EmailSection = () => {
  const [isPerformingAction, setIsPerformingAction] = useState(false);
  const [showChangeEmail, setShowChangeEmail] = useState(false);
  const meQuery = userQueries.useMeQuery();
  const { data: userData } = meQuery;

  const changeEmailFormik = useFormik({
    initialValues: {
      email: userData?.primaryEmail || "",
    },
    validationSchema: emailSchema,
    onSubmit: async (values) => {
      setIsPerformingAction(true);
      try {
        const result = await userApis.requestEmailChange(values.email);
        showSuccessToast(result);
        setShowChangeEmail(false);
        changeEmailFormik.resetForm();
        meQuery.refetch();
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
      showSuccessToast(result);
      meQuery.refetch();
    } catch (error: any) {
      showErrorToast(error);
    }
    setIsPerformingAction(false);
  };

  return (
    <div className={classes.user_emails}>
      <div className={classes.email_item}>
        <div className={classes.info}>
          {showChangeEmail ? (
            <InputField
              icon={<Mail className={inputFieldClasses.input_icon} />}
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
              <StatusBadge badgeProps={{ label: "Primary", type: "main" }} />
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
                  isPerformingAction ||
                  !changeEmailFormik.isValid ||
                  isEqual(changeEmailFormik.initialValues, changeEmailFormik.values)
                }
                onClick={() => {
                  changeEmailFormik.handleSubmit();
                }}
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
              <Button
                type="primary"
                htmlType="submit"
                className={classes.action_btn}
                onClick={() => setShowChangeEmail(true)}
              >
                Change Email
              </Button>
            )
          )}
        </div>
      </div>
      {userData?.pendingEmail && (
        <div className={classes.email_item}>
          <div className={classes.info}>
            <span>{userData?.pendingEmail}</span>
            <StatusBadge badgeProps={{ label: "Pending", type: "warning" }} />
          </div>
          <div className={classes.actions}>
            <Button
              type="primary"
              htmlType="submit"
              className={classes.action_btn}
              loading={isPerformingAction}
              disabled={isPerformingAction || !changeEmailFormik.isValid}
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
