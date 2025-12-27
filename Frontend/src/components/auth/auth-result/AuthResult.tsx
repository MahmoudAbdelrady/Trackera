import { Button } from "antd";
import type { AuthResultFields } from "../../../shared/types";
import classes from "./scss/auth-result.module.css";
import { Check, X } from "lucide-react";

interface AuthResultProps extends AuthResultFields {
  message: string;
  buttonText: string;
  onClick: () => void;
}

const AuthResult = (props: AuthResultProps) => {
  return (
    <div className={classes.auth_result_container}>
      <div className={classes.ar_header}>
        <h3>{props.title}</h3>
        <p>{props.description}</p>
      </div>
      <div className={classes.ar_content}>
        <div className={`${classes.ar_icon_container} ${props.isError && classes.error}`}>
          {props.isError ? <X className={classes.ar_icon} /> : <Check className={classes.ar_icon} />}
        </div>
        <p className={classes.ar_message}>{props.message}</p>
        <Button type="primary" onClick={props.onClick} className={classes.ar_button}>
          {props.buttonText}
        </Button>
      </div>
    </div>
  );
};

export default AuthResult;
