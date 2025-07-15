import { CircleCheckBig, Clock } from "lucide-react";
import { AppLayout, WorklogStatusCard } from "../../components";
import classes from "./scss/home.module.css";

const Home = () => {
  const workLogStatusCards = [
    {
      cardLabel: "Total Logged Hours",
      cardValue: "34.7",
      cardIcon: <Clock />,
      cardColor: "#e4f0ff",
    },
    {
      cardLabel: "Target Hours",
      cardValue: "40",
      cardIcon: <CircleCheckBig />,
      cardColor: "#e4fced",
    },
    {
      cardLabel: "Hours Left",
      cardValue: "5.3",
      cardIcon: <Clock />,
      cardColor: "#fef9e1",
      cardSubLabel: "Approx. 0.7 days",
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
            cardColor={card.cardColor}
            cardSubLabel={card.cardSubLabel}
          />
        ))}
      </div>
    </AppLayout>
  );
};

export default Home;
