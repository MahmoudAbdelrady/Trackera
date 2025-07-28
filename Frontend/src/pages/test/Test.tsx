import { Button, Input } from "antd";
import { AuthLayout } from "../../components";
import classes from "./scss/test.module.css";
import { Lock, Mail } from "lucide-react";
import { Link } from "react-router-dom";

const Test = () => {
  return (
    <AuthLayout>
      <div className={classes.input_group}>
        <div className={classes.input_label}>Email</div>
        <Input
          prefix={<Mail className={classes.input_icon} />}
          placeholder="Enter your email"
          className={classes.input_field}
        />
      </div>
      <div className={classes.input_group}>
        <div className={classes.input_label}>Password</div>
        <Input.Password
          prefix={<Lock className={classes.input_icon} />}
          placeholder="Enter your password"
          className={classes.input_field}
        />
      </div>
      <div className={classes.forget_password_box}>
        <Link to="/forgot-password" className={classes.forget_password_link}>
          Forgot Password?
        </Link>
      </div>
      <Button type="primary" className={classes.submit_button}>
        Login
      </Button>
    </AuthLayout>
  );
};

export default Test;
