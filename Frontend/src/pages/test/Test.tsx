import { useEffect } from "react";

const Test = () => {
  const testData: Record<string, any>[] = [
    {
      id: 1,
      name: "Test 1",
      status: "in_progress",
    },
    {
      id: 2,
      name: "Test 2",
      status: "completed",
    },
    {
      id: 3,
      name: "Test 3",
      status: "not_started",
    },
    {
      id: 4,
      name: "Test 4",
      status: "in_progress",
    },
  ];

  useEffect(() => {
    const subscribeToTestUpdates = (data: Record<string, any>[]) => {
      data.forEach((test) => {
        const es = new EventSource(`${import.meta.env.VITE_TRACKERA_BACKEND_URL}/trackera/test/subscribe/${test.id}`);
        es.onmessage = (event) => {
          const updatedTest = JSON.parse(event.data);
          console.log("Received update for test:", updatedTest);
        };
        es.onerror = (error) => {
          console.error("EventSource failed:", error);
          es.close();
        };
      });
    };

    const inProgressTests = testData.filter((test) => test.status === "in_progress");
    if (inProgressTests.length > 0) {
      subscribeToTestUpdates(inProgressTests);
    }
  }, []);

  return (
    <div>
      <h1>Test Elements:</h1>
      {testData.map((test) => (
        <div key={test.id}>
          <p>
            ID: {test.id}, Name: {test.name}, Status: {test.status}
          </p>
        </div>
      ))}
    </div>
  );
};

export default Test;
