import { ChevronDown, LogOut } from "lucide-react";
import classes from "./scss/app-layout.module.css";
import { Avatar, Dropdown, type MenuProps } from "antd";
import { Sidebar } from "..";
import { useState } from "react";
import { useAuthStore } from "../../state/store";
import {
  showErrorToast,
  showSuccessToast,
} from "../../utils/toast-handler/show-toast";
import requestInstance from "../../shared/api/request-instance";
import { useNavigate } from "react-router-dom";

const AppLayout = ({ children }: { children: React.ReactNode }) => {
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const authStore = useAuthStore();
  const navigate = useNavigate();

  const handleLogout = async () => {
    setIsLoading(true);
    try {
      const response = await requestInstance.post("/auth/logout");
      authStore.removeToken();
      showSuccessToast(response.data.message);
      navigate("/login");
    } catch (error: any) {
      showErrorToast(error);
    }
    setIsLoading(false);
  };

  const userProfileItems: MenuProps["items"] = [
    {
      label: (
        <div
          className={`${classes.profile_item} ${isLoading && classes.disabled}`}
        >
          <LogOut />
          <span className={classes.profile_item_label}>Logout</span>
        </div>
      ),
      key: "logout",
      onClick: handleLogout,
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
