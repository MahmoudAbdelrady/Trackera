import {
  CircleCheckBig,
  Clock,
  Eye,
  Info,
  SquarePen,
  Trash,
  ClipboardPlus,
  Funnel,
} from "lucide-react";
import { AppLayout, WorklogStatusCard } from "../../components";
import classes from "./scss/home.module.css";
import { Button, Collapse, Table, type TableProps } from "antd";

interface Worklog {
  logName: string;
  totalHours: number;
  date: string;
  evaluation: WorkLogEvaluationType;
  status: string;
}

type WorkLogEvaluationType = "EXCELLENT" | "GOOD" | "MODERATE" | "POOR";

interface EvaluationMeta {
  label: string;
  className: string;
}

const evaluationMetadata: Record<WorkLogEvaluationType, EvaluationMeta> = {
  EXCELLENT: { label: "Excellent", className: "excellent" },
  GOOD: { label: "Good", className: "good" },
  MODERATE: { label: "Moderate", className: "moderate" },
  POOR: { label: "Poor", className: "poor" },
};

const Home = () => {
  const workLogStatusCards = [
    {
      cardLabel: "Total Logged Hours",
      cardValue: "34.7",
      cardIcon: <Clock />,
      cardColorTheme: "main",
    },
    {
      cardLabel: "Target Hours",
      cardValue: "40",
      cardIcon: <CircleCheckBig />,
      cardColorTheme: "success",
    },
    {
      cardLabel: "Hours Left",
      cardValue: "5.3",
      cardIcon: <Info />,
      cardColorTheme: "info",
      cardSubLabel: "Approx. 0.7 days",
    },
  ];

  const worklogs: Worklog[] = [
    {
      logName: "Frontend Development",
      totalHours: 10,
      date: "2023-10-01",
      evaluation: "EXCELLENT",
      status: "Synced",
    },
    {
      logName: "Backend Development",
      totalHours: 7.45,
      date: "2023-10-01",
      evaluation: "GOOD",
      status: "Partially",
    },
    {
      logName: "Bug Fixing",
      totalHours: 6.45,
      date: "2023-10-01",
      evaluation: "MODERATE",
      status: "Unsynced",
    },
  ];

  const tableColumns: TableProps<Worklog>["columns"] = [
    {
      title: "Log Name",
      dataIndex: "logName",
      key: "logName",
    },
    {
      title: "Total Hours",
      dataIndex: "totalHours",
      key: "totalHours",
    },
    {
      title: "Date",
      dataIndex: "date",
      key: "date",
    },
    {
      title: "Evaluation",
      dataIndex: "evaluation",
      key: "evaluation",
      render: (_, { evaluation }) => {
        const meta = evaluationMetadata[evaluation as WorkLogEvaluationType];
        return (
          <div
            className={`${classes.evaluation_item} ${classes[meta.className]}`}
          >
            {meta.label}
          </div>
        );
      },
    },
    {
      title: "Status",
      dataIndex: "status",
      key: "status",
    },
    {
      title: "Actions",
      key: "actions",
      render: (_, record) => (
        <div className={classes.actions_container}>
          <Button icon={<SquarePen />} onClick={() => {}} />
          <Button icon={<Eye />} onClick={() => {}} />
          <Button icon={<Trash />} onClick={() => {}} />
        </div>
      ),
    },
  ];

  return (
    <AppLayout>
      <div className={classes.worklog_status_cards_container}>
        {workLogStatusCards.map((card, index) => (
          <WorklogStatusCard
            key={index}
            cardLabel={card.cardLabel}
            cardValue={card.cardValue}
            cardIcon={card.cardIcon}
            cardColorTheme={card.cardColorTheme}
            cardSubLabel={card.cardSubLabel}
          />
        ))}
      </div>
      <div className={classes.worklogs_content}>
        <div className={classes.search_filters}>
          <Collapse
            expandIconPosition="right"
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
                    <Funnel /> Filters
                  </div>
                ),
                children: <p>Search filters</p>,
              },
            ]}
          />
        </div>
        <div className={classes.worklogs_container}>
          <div className={classes.worklogs_actions}>
            <Button
              icon={<ClipboardPlus />}
              className={`${classes.log_button} ${classes.add}`}
            >
              Add Worklog
            </Button>
          </div>
          <div className={classes.worklogs_data}>
            <Table
              columns={tableColumns}
              dataSource={worklogs}
              scroll={{ x: 768 }}
            />
          </div>
        </div>
      </div>
    </AppLayout>
  );
};

export default Home;
