import { Link } from "react-router-dom";
import classes from "./scss/auth-footer.module.css";
import { Button } from "antd";

const AuthFooter = () => {
  return (
    <div className={classes.auth_footer_container}>
      <div className={classes.separator}>
        <span className={classes.separator_text}>or</span>
      </div>
      <div className={classes.auth_buttons}>
        <Button
          icon={<img src="./Assets/google_logo.webp" alt="Google Icon" />}
          className={classes.auth_btn}
        >
          Continue with Google
        </Button>
      </div>
      <div className={classes.auth_redirection}>
        <p>
          Don't have an account? <Link to="/signup">Sign up</Link>
        </p>
      </div>
    </div>
  );
};

export default AuthFooter;
