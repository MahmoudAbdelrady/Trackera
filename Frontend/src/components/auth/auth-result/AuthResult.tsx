import { Button } from "antd";
import type { AuthResultFields } from "../auth.types";
import classes from "./scss/auth-result.module.css";
import { Check, X } from "lucide-react";

interface AuthResultProps extends AuthResultFields {
  message: string;
  buttonText: string;
  onClick: () => void;
}

const AuthResult = (props: AuthResultProps) => {
  const { title, description, message, buttonText, onClick, isError } = props;

  return (
    <div className={classes.auth_result_container}>
      <div className={classes.ar_header}>
        <h3>{title}</h3>
        <p>{description}</p>
      </div>
      <div className={classes.ar_content}>
        <div className={`${classes.ar_icon_container} ${isError && classes.error}`}>
          {isError ? <X className={classes.ar_icon} /> : <Check className={classes.ar_icon} />}
        </div>
        <p className={classes.ar_message}>{message}</p>
        <Button type="primary" onClick={onClick} className={classes.ar_button}>
          {buttonText}
        </Button>
      </div>
    </div>
  );
};

export default AuthResult;
