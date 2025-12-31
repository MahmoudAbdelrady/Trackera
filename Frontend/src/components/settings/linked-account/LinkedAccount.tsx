import { Button } from "antd";
import classes from "./scss/linked-account.module.css";
import { OAUTH_PROVIDERS } from "../../../shared/types";

const providerIconMap: Record<string, React.ReactNode> = {
  [OAUTH_PROVIDERS.JIRA]: <img src="/Assets/jira_icon.svg" alt="Jira" />,
  [OAUTH_PROVIDERS.GOOGLE]: <img src="/Assets/google_logo.webp" alt="Google" />,
};

interface LinkedAccountProps {
  platform: Record<string, string>;
  accountIdentifier?: string;
  isLinked: boolean;
  onLink: () => void;
  onUnlink: () => void;
}

const LinkedAccount = (props: LinkedAccountProps) => {
  const { platform, accountIdentifier, isLinked, onLink, onUnlink } = props;

  return (
    <div className={classes.linked_account}>
      <div className={classes.account_info}>
        <div className={classes.platform}>
          <div className={classes.icon}>{providerIconMap[platform.code]}</div>
          <div className={classes.platform_name}>{platform.name}</div>
        </div>
        <div className={classes.account_identifier}>{accountIdentifier}</div>
      </div>
      <div className={classes.actions}>
        <Button
          color={`${isLinked ? "danger" : "primary"}`}
          variant={`${isLinked ? "outlined" : "solid"}`}
          className={classes.link_button}
          onClick={isLinked ? onUnlink : onLink}
        >
          {`${isLinked ? "Unlink" : "Link"} Account`}
        </Button>
      </div>
    </div>
  );
};

export default LinkedAccount;
