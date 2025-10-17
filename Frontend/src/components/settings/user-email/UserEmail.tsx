import { Dropdown, Modal, type MenuProps } from "antd";
import StatusBadge from "../../status-badge/StatusBadge";
import classes from "./scss/user-email.module.css";
import { useState } from "react";
import { EllipsisVertical, RefreshCw, Star, Trash } from "lucide-react";

const UserEmail = () => {
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [showRemoveEmail, setShowRemoveEmail] = useState(false);
  const [isRemovingEmail, setIsRemovingEmail] = useState(false);

  const userEmailItems: MenuProps["items"] = [
    {
      label: (
        <div className={`${classes.email_item} ${classes.primary} ${isLoading && classes.disabled}`}>
          <Star />
          <span className={classes.email_item_label}>Make Primary</span>
        </div>
      ),
      key: "makePrimary",
      onClick: () => {},
    },
    {
      label: (
        <div className={`${classes.email_item} ${classes.resend} ${isLoading && classes.disabled}`}>
          <RefreshCw />
          <span className={classes.email_item_label}>Resend Verification</span>
        </div>
      ),
      key: "resendVerification",
      onClick: () => {},
    },
    {
      label: (
        <div className={`${classes.email_item} ${classes.remove} ${isLoading && classes.disabled}`}>
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

  return (
    <>
      {showRemoveEmail && (
        <Modal
          title="Remove tester@mail.com Email"
          open={true}
          centered={true}
          closable={!isRemovingEmail}
          keyboard={!isRemovingEmail}
          maskClosable={!isRemovingEmail}
          okText={"Remove"}
          okButtonProps={{ loading: isRemovingEmail, danger: true, disabled: isRemovingEmail }}
          cancelButtonProps={{ disabled: isRemovingEmail }}
          onOk={() => {}}
          onCancel={() => setShowRemoveEmail(false)}
        >
          Are you sure you want to remove this email? This action cannot be undone.
        </Modal>
      )}
      <div className={classes.user_email}>
        <div className={classes.info}>
          <span className={classes.email}>tester@mail.com</span>
          <div className={classes.tags}>
            <StatusBadge badgeProps={{ label: "Primary", type: "main" }} />
          </div>
        </div>
        <div className={classes.action_btn}>
          <Dropdown trigger={["click"]} menu={{ items: userEmailItems }} className={classes.email_actions_dropdown}>
            <span className={classes.action_trigger}>
              <EllipsisVertical />
            </span>
          </Dropdown>
        </div>
      </div>
    </>
  );
};

export default UserEmail;
