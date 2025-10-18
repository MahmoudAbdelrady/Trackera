import { Alert, Dropdown, Modal, type MenuProps } from "antd";
import StatusBadge from "../../status-badge/StatusBadge";
import classes from "./scss/user-email.module.css";
import { useState } from "react";
import { EllipsisVertical, RefreshCw, Star, Trash } from "lucide-react";
import type { UserEmailProps } from "../../../shared/types";
import { showErrorToast, showSuccessToast } from "../../../utils/toast-handler/showToast";
import requestInstance from "../../../shared/axios/request-instance";

const UserEmail = (props: UserEmailProps) => {
  const { userEmail, setFetchUserEmails } = props;
  const [isPerformingAction, setIsPerformingAction] = useState<boolean>(false);
  const [showRemoveEmail, setShowRemoveEmail] = useState(false);
  const [isRemovingEmail, setIsRemovingEmail] = useState(false);

  const userEmailItems: MenuProps["items"] = [
    {
      label: (
        <div className={`${classes.email_item} ${classes.primary} ${isPerformingAction && classes.disabled}`}>
          <Star />
          <span className={classes.email_item_label}>Make Primary</span>
        </div>
      ),
      key: "makePrimary",
      onClick: () => {
        makeEmailPrimary();
      },
    },
    {
      label: (
        <div className={`${classes.email_item} ${classes.resend} ${isPerformingAction && classes.disabled}`}>
          <RefreshCw />
          <span className={classes.email_item_label}>Resend Verification</span>
        </div>
      ),
      key: "resendVerification",
      onClick: () => {
        resendEmailVerification();
      },
    },
    {
      label: (
        <div className={`${classes.email_item} ${classes.remove} ${isPerformingAction && classes.disabled}`}>
          <Trash />
          <span className={classes.email_item_label}>Remove</span>
        </div>
      ),
      key: "remove",
      onClick: () => {
        setShowRemoveEmail(true);
      },
    },
  ];

  const makeEmailPrimary = async () => {
    setIsPerformingAction(true);
    try {
      const response = await requestInstance.post("/user/emails/primary", { email: userEmail.email });
      showSuccessToast(response.data);
      setFetchUserEmails(true);
    } catch (error: any) {
      showErrorToast(error);
    }
    setIsPerformingAction(false);
  };

  const resendEmailVerification = async () => {
    setIsPerformingAction(true);
    try {
      const response = await requestInstance.post("/user/emails/send-verification", { email: userEmail.email });
      showSuccessToast(response.data);
    } catch (error: any) {
      showErrorToast(error);
    }
    setIsPerformingAction(false);
  };

  const removeUserEmail = async () => {
    setIsRemovingEmail(true);
    try {
      const response = await requestInstance.delete("/user/emails", { data: { email: userEmail.email } });
      showSuccessToast(response.data);
      setFetchUserEmails(true);
    } catch (error: any) {
      showErrorToast(error);
    }
    setIsRemovingEmail(false);
  };

  const getUserEmailActions = (): MenuProps["items"] => {
    const actions = [...userEmailItems];
    return userEmail.verified ? actions.filter((action) => action?.key !== "resendVerification") : actions.filter((action) => action?.key !== "makePrimary");
  };

  return (
    <>
      {showRemoveEmail && (
        <Modal
          title={`Remove ${userEmail.email} Email`}
          open={true}
          centered={true}
          closable={!isRemovingEmail}
          keyboard={!isRemovingEmail}
          maskClosable={!isRemovingEmail}
          okText={"Remove"}
          okButtonProps={{ loading: isRemovingEmail, danger: true, disabled: isRemovingEmail }}
          cancelButtonProps={{ disabled: isRemovingEmail }}
          onOk={removeUserEmail}
          onCancel={() => setShowRemoveEmail(false)}
        >
          <p>Are you sure you want to remove this email? This action cannot be undone.</p>
          {userEmail.oauthLinked && (
            <Alert message="This email is linked to an OAuth account and removing it may affect your ability to log in." type="warning" showIcon style={{ marginTop: "10px", marginBottom: "20px" }} />
          )}
        </Modal>
      )}
      <div className={classes.user_email}>
        <div className={classes.info}>
          <span className={classes.email}>{userEmail.email}</span>
          <div className={classes.tags}>
            {userEmail.primary && <StatusBadge badgeProps={{ label: "Primary", type: "main" }} />}
            {!userEmail.primary && <StatusBadge badgeProps={{ label: `${userEmail.verified ? "Verified" : "Not Verified"}`, type: `${userEmail.verified ? "success" : "warning"}` }} />}
            {userEmail.tags.map((tag, idx) => (
              <StatusBadge key={idx + 1} badgeProps={{ label: tag, type: "default" }} />
            ))}
          </div>
        </div>
        <div className={classes.action_btn}>
          {!userEmail.primary && (
            <Dropdown trigger={["click"]} menu={{ items: getUserEmailActions() }} className={classes.email_actions_dropdown}>
              <span className={classes.action_trigger}>
                <EllipsisVertical />
              </span>
            </Dropdown>
          )}
        </div>
      </div>
    </>
  );
};

export default UserEmail;
