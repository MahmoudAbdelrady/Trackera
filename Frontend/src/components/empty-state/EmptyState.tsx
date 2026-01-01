import type React from "react";
import classes from "./scss/empty-state.module.css";

interface EmptyStateProps {
  imgSrc: string;
  alt: string;
  title: string;
  message: string | React.ReactNode;
  children?: React.ReactNode;
  fitParent?: boolean;
}

const EmptyState = (props: EmptyStateProps) => {
  const { imgSrc, alt, title, message, children, fitParent } = props;

  return (
    <div className={`${classes.empty_state_container} ${fitParent ? classes.fit_parent : ""}`}>
      <div className={`${classes.content} ${fitParent ? classes.fit_parent : ""}`}>
        <img src={imgSrc} alt={alt} className={classes.img} />
        <h2 className={classes.title}>{title}</h2>
        <div className={classes.message}>{message}</div>
        {children && <div className={classes.extra}>{children}</div>}
      </div>
    </div>
  );
};

export default EmptyState;
