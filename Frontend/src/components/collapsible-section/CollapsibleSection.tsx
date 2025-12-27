import { Collapse } from "antd";

interface CollapsibleSectionProps {
  title: string;
  icon?: React.ReactNode;
  children: React.ReactNode;
}

const CollapsibleSection = (props: CollapsibleSectionProps) => {
  const { title, icon, children } = props;

  return (
    <Collapse
      expandIconPosition="end"
      items={[
        {
          key: "1",
          label: (
            <div
              style={{
                display: "flex",
                alignItems: "center",
                gap: "8px",
              }}
            >
              {icon} {title}
            </div>
          ),
          children,
        },
      ]}
    />
  );
};

export default CollapsibleSection;
