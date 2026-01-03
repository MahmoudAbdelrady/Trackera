import { Button } from "antd";
import classes from "./scss/linked-account.module.css";
import { OAUTH_PROVIDERS, type OAuthProviderInfo } from "../../../shared/types";

const providerIconMap: Record<string, React.ReactNode> = {
  [OAUTH_PROVIDERS.JIRA]: <img src="/Assets/jira_icon.svg" alt="Jira" />,
  [OAUTH_PROVIDERS.GOOGLE]: <img src="/Assets/google_logo.webp" alt="Google" />,
};

interface LinkedAccountProps {
  providerInfo: OAuthProviderInfo;
  accountIdentifier?: string;
  linked: boolean;
  isRevoked?: boolean;
  onLink: () => void;
  onUnlink: () => void;
}

const LinkedAccount = (props: LinkedAccountProps) => {
  const { providerInfo, accountIdentifier, linked, onLink, onUnlink } = props;

  return (
    <div className={classes.linked_account}>
      <div className={classes.account_info}>
        <div className={classes.platform}>
          <div className={classes.icon}>{providerIconMap[providerInfo.code]}</div>
          <div className={classes.platform_name}>{providerInfo.displayName}</div>
        </div>
        <div className={classes.account_identifier}>{accountIdentifier}</div>
      </div>
      <div className={classes.actions}>
        <Button
          color={`${linked ? "danger" : "primary"}`}
          variant={`${linked ? "outlined" : "solid"}`}
          className={classes.link_button}
          onClick={linked ? onUnlink : onLink}
        >
          {`${linked ? "Unlink" : "Link"} Account`}
        </Button>
      </div>
    </div>
  );
};

export default LinkedAccount;
