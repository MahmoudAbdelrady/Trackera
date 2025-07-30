import { Link } from "react-router-dom";
import classes from "./scss/auth-footer.module.css";
import { Button } from "antd";
import type { AuthFooterProps } from "../../../utils/types";

const AuthFooter = (props: AuthFooterProps) => {
  return (
    <div className={classes.auth_footer_container}>
      <div className={classes.separator}>
        <span className={classes.separator_text}>or</span>
      </div>
      {props.oAuthButtons && props.oAuthButtons.length > 0 && (
        <div className={classes.auth_buttons}>
          {props.oAuthButtons.map((btn, index) => (
            <Button
              key={index}
              className={classes.auth_btn}
              onClick={btn.onClick}
              icon={<div className={classes.icon_container}>{btn.icon}</div>}
            >
              {btn.label}
            </Button>
          ))}
        </div>
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
