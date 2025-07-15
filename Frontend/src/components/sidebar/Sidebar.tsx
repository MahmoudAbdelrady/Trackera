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
      to: "/tasks",
      icon: <SquareCheckBig />,
      text: "Tasks",
    },
    {
      to: "/settings",
      icon: <Settings />,
      text: "Settings",
    },
  ];

  const getPageItem = (to: string, icon: React.ReactNode, text: string) => {
    return (
      <Link
        to={to}
        className={`${classes.page_item} ${isPageActive(to) && classes.active}`}
      >
        <div className={classes.page_item_icn}>{icon}</div>
        <span className={classes.page_item_text}>{text}</span>
      </Link>
    );
  };

  return (
    <div className={classes.sidebar}>
      <div className={classes.page_items}>
        {pages.map((page) => getPageItem(page.to, page.icon, page.text))}
      </div>
    </div>
  );
};

export default Sidebar;
