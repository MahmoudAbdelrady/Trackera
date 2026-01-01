import classes from "./scss/policy-item.module.css";

interface PolicyItemProps {
  id: string;
  index: number;
  title: string;
  children: React.ReactNode;
}

const PolicyItem = (props: PolicyItemProps) => {
  const { id, index, title, children } = props;
  return (
    <section className={classes.policy_item}>
      <h3 id={id} className={classes.policy_title}>
        {index}. {title}
      </h3>
      {children}
    </section>
  );
};

export default PolicyItem;
