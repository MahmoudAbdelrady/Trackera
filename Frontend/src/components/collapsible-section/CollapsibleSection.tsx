import { Collapse } from "antd";

interface CollapsibleSectionProps {
  title: string;
  icon?: React.ReactNode;
  children: React.ReactNode;
}

const CollapsibleSection = (props: CollapsibleSectionProps) => {
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
              {props.icon} {props.title}
            </div>
          ),
          children: props.children,
        },
      ]}
    />
  );
};

export default CollapsibleSection;
