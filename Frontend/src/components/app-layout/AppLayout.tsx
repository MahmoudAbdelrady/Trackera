import { ChevronDown, Moon } from "lucide-react";
import classes from "./scss/app-layout.module.css";
import { Avatar } from "antd";
import { Sidebar } from "..";

const AppLayout = ({ children }: { children: React.ReactNode }) => {
  return (
    <div className={classes.app_layout}>
      <div className={classes.header}>
        <h3 className={classes.site_name}>Trackera</h3>
        <div className={classes.actions}>
          <div className={classes.action_buttons}>
            <Moon className={classes.action_icn} />
          </div>
          <div className={classes.user_profile}>
            <Avatar style={{ backgroundColor: "blue" }}>T</Avatar>
            <div className={classes.user_name}>
              <span>Tester</span>
              <ChevronDown />
            </div>
          </div>
        </div>
      </div>
      <div className={classes.body_container}>
        <div className={classes.sidebar_container}>
          <Sidebar />
        </div>
        <div className={classes.content}>{children}</div>
      </div>
    </div>
  );
};

export default AppLayout;
