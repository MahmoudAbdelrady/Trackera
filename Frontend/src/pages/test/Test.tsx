import { Badge, Card } from "antd";

const Test = () => {
  return (
    <div>
      <Badge.Ribbon text="Test Ribbon" color="#9bb9f8" styles={{ content: { color: "#000" } }}>
        <Card>Test</Card>
      </Badge.Ribbon>
    </div>
  );
};

export default Test;
