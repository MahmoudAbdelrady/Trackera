import { ChevronDown, LogOut } from "lucide-react";
import classes from "./scss/app-layout.module.css";
import { Avatar, Dropdown, type MenuProps } from "antd";
import { Sidebar } from "..";

const AppLayout = ({ children }: { children: React.ReactNode }) => {
  const userProfileItems: MenuProps["items"] = [
    {
      label: (
        <div className={classes.profile_item}>
          <LogOut />
          <span className={classes.profile_item_label}>Logout</span>
        </div>
      ),
      key: "logout",
      onClick: () => {},
    },
  ];
  return (
    <div className={classes.app_layout}>
      <div className={classes.header}>
        <h3 className={classes.site_name}>Trackera</h3>
        <div className={classes.actions}>
          <Dropdown trigger={["click"]} menu={{ items: userProfileItems }}>
            <div className={classes.user_profile}>
              <Avatar style={{ backgroundColor: "blue" }}>T</Avatar>
              <div className={classes.user_name}>
                <span>Tester</span>
                <ChevronDown />
              </div>
            </div>
          </Dropdown>
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
