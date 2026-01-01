import { Link } from "react-router-dom";
import OAuthBtns from "../oauth-btns/OAuthBtns";
import classes from "./scss/auth-footer.module.css";

interface AuthFooterProps {
  hasOAuthBtns?: boolean;
  isOAuthBtnsDisabled?: boolean;
  footerText: string;
  footerLink: string;
  footerLinkText: string;
  footerAdditionalInfo?: React.ReactNode;
}

const AuthFooter = (props: AuthFooterProps) => {
  const { hasOAuthBtns, isOAuthBtnsDisabled, footerText, footerLink, footerLinkText, footerAdditionalInfo } = props;
  return (
    <div className={classes.auth_footer_container}>
      {hasOAuthBtns && (
        <>
          <div className={classes.separator}>
            <span className={classes.separator_text}>or</span>
          </div>
          <OAuthBtns disabled={isOAuthBtnsDisabled} />
        </>
      )}
      <div className={classes.auth_redirection}>
        <p>
          {footerText}
          <Link to={footerLink}>{footerLinkText}</Link>
        </p>
      </div>
      {footerAdditionalInfo && <div className={classes.footer_additional_info}>{footerAdditionalInfo}</div>}
    </div>
  );
};

export default AuthFooter;
