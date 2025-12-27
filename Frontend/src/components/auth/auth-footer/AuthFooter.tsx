import { Link } from "react-router-dom";
import OAuthBtns from "../oauth-btns/OAuthBtns";
import classes from "./scss/auth-footer.module.css";

interface AuthFooterProps {
  hasOAuthBtns?: boolean;
  isOAuthBtnsDisabled?: boolean;
  footerText: string;
  footerLink: string;
  footerLinkText: string;
}

const AuthFooter = (props: AuthFooterProps) => {
  return (
    <div className={classes.auth_footer_container}>
      {props.hasOAuthBtns && (
        <>
          <div className={classes.separator}>
            <span className={classes.separator_text}>or</span>
          </div>
          <OAuthBtns disabled={props.isOAuthBtnsDisabled} />
        </>
      )}
      <div className={classes.auth_redirection}>
        <p>
          {props.footerText}
          <Link to={props.footerLink}>{props.footerLinkText}</Link>
        </p>
      </div>
    </div>
  );
};

export default AuthFooter;
