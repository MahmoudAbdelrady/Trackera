import { ClipboardList, Settings, SquareCheckBig } from "lucide-react";
import classes from "./scss/sidebar.module.css";
import { Link } from "react-router-dom";

interface SidebarPage {
  to: string;
  icon: React.ReactNode;
  label: string;
  code: string;
}

const Sidebar = () => {
  const isPageActive = (path: string) => location.pathname === path;

  const pages: SidebarPage[] = [
    {
      to: "/",
      icon: <ClipboardList />,
      label: "Worklogs",
      code: "worklogs",
    },
    {
      to: "/jira-tasks",
      icon: <SquareCheckBig />,
      label: "Jira Tasks",
      code: "jira-tasks",
    },
    {
      to: "/settings",
      icon: <Settings />,
      label: "Settings",
      code: "settings",
    },
  ];

  const getPageItem = ({ to, icon, label, code }: SidebarPage) => {
    return (
      <Link to={to} className={`${classes.page_item} ${isPageActive(to) && classes.active}`} key={code}>
        <div className={classes.page_item_icn}>{icon}</div>
        <span className={classes.page_item_text}>{label}</span>
      </Link>
    );
  };

  return (
    <div className={classes.sidebar}>
      <div className={classes.page_items}>{pages.map((page) => getPageItem(page))}</div>
    </div>
  );
};

export default Sidebar;
