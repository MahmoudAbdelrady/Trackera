import { ChevronDown, LogOut } from "lucide-react";
import classes from "./scss/app-layout.module.css";
import { Avatar, Dropdown, type MenuProps } from "antd";
import { Sidebar } from "..";
import { useState } from "react";
import { useAuthStore } from "../../state/store";
import { showErrorToast, showSuccessToast } from "../../utils/toast-handler/showToast";
import requestInstance from "../../shared/axios/request-instance";
import { useNavigate } from "react-router-dom";
import { userQueries } from "../../state/queries";
import { getContrastColor } from "../../utils";

const AppLayout = ({ children }: { children: React.ReactNode }) => {
  const { data: userData } = userQueries.useMeQuery();
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const authStore = useAuthStore();
  const navigate = useNavigate();

  const handleLogout = async () => {
    setIsLoading(true);
    try {
      const response = await requestInstance.post("/auth/logout");
      authStore.logout();
      showSuccessToast(response.data);
      navigate("/login");
    } catch (error: any) {
      showErrorToast(error);
    }
    setIsLoading(false);
  };

  const userProfileItems: MenuProps["items"] = [
    {
      label: (
        <div className={`${classes.profile_item} ${isLoading && classes.disabled}`}>
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
              {userData?.profilePicture ? (
                <Avatar src={userData?.profilePicture} />
              ) : (
                <Avatar
                  style={{
                    backgroundColor: userData?.avatarColor,
                    color: getContrastColor(userData?.avatarColor),
                    fontSize: "18px",
                    userSelect: "none",
                  }}
                >
                  {userData?.firstname.charAt(0).toUpperCase()}
                </Avatar>
              )}
              <div className={classes.user_name}>
                <span>{userData?.firstname}</span>
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
