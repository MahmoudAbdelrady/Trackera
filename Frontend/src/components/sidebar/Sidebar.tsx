import { ClipboardList, Settings, SquareCheckBig } from "lucide-react";
import classes from "./scss/sidebar.module.css";
import { Link } from "react-router-dom";

const Sidebar = () => {
  const isPageActive = (path: string) => location.pathname === path;

  const pages = [
    {
      to: "/",
      icon: <ClipboardList />,
      text: "Worklogs",
    },
    {
      to: "/jira-tasks",
      icon: <SquareCheckBig />,
      text: "Jira Tasks",
    },
    {
      to: "/settings",
      icon: <Settings />,
      text: "Settings",
    },
  ];

  const getPageItem = (to: string, icon: React.ReactNode, text: string, index: number) => {
    return (
      <Link to={to} className={`${classes.page_item} ${isPageActive(to) && classes.active}`} key={index}>
        <div className={classes.page_item_icn}>{icon}</div>
        <span className={classes.page_item_text}>{text}</span>
      </Link>
    );
  };

  return (
    <div className={classes.sidebar}>
      <div className={classes.page_items}>{pages.map((page, index) => getPageItem(page.to, page.icon, page.text, index))}</div>
    </div>
  );
};

export default Sidebar;
