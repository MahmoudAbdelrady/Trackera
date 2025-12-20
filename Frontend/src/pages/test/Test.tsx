import { Button, Divider } from "antd";
import { useEffect, useRef, useState } from "react";
import { authApis } from "../../state/api";

const Test = () => {
  const testData: Record<string, any>[] = [
    { id: 1, name: "Test 1", status: "in_progress" },
    { id: 2, name: "Test 2", status: "completed" },
    { id: 3, name: "Test 3", status: "not_started" },
    { id: 4, name: "Test 4", status: "in_progress" },
    { id: 5, name: "Test 5", status: "completed" },
    { id: 6, name: "Test 6", status: "not_started" },
    { id: 7, name: "Test 7", status: "in_progress" },
    { id: 8, name: "Test 8", status: "completed" },
    { id: 9, name: "Test 9", status: "not_started" },
    { id: 10, name: "Test 10", status: "in_progress" },
    { id: 11, name: "Test 11", status: "completed" },
    { id: 12, name: "Test 12", status: "not_started" },
    { id: 13, name: "Test 13", status: "in_progress" },
    { id: 14, name: "Test 14", status: "completed" },
    { id: 15, name: "Test 15", status: "not_started" },
    { id: 16, name: "Test 16", status: "in_progress" },
    { id: 17, name: "Test 17", status: "completed" },
    { id: 18, name: "Test 18", status: "not_started" },
    { id: 19, name: "Test 19", status: "in_progress" },
    { id: 20, name: "Test 20", status: "completed" },
  ];
  const [tests, setTests] = useState<Record<string, any>[]>(testData);
  const eventSourceRef = useRef<EventSource | null>(null);

  // useEffect(() => {
  //   if (tests.some((test) => test.status === "in_progress")) {
  //     subscribeForTestUpdates();
  //   }

  //   return () => {
  //     if (eventSourceRef.current) {
  //       eventSourceRef.current.close();
  //       eventSourceRef.current = null;
  //     }
  //   };
  // }, []);

  const subscribeForTestUpdates = () => {
    if (eventSourceRef.current) return;

    const es = new EventSource(`${import.meta.env.VITE_TRACKERA_BACKEND_URL}/test/subscribe`, {
      withCredentials: true,
    });
    es.addEventListener("log-status", (event: MessageEvent) => {
      const updatedTest = JSON.parse(event.data);
      setTests((prev) => prev.map((t) => (t.id == updatedTest.logId ? { ...t, status: updatedTest.status } : t)));
    });
    es.addEventListener("error", async (event: MessageEvent) => {
      const errorResponse = JSON.parse(event.data);
      if (errorResponse.status === 401) {
        await authApis.refreshToken();
        closeEventSource(es);
        subscribeForTestUpdates();
        return;
      }
      closeEventSource(es);
    });

    eventSourceRef.current = es;
  };

  const closeEventSource = (event: EventSource) => {
    event.close();
    eventSourceRef.current = null;
  };

  return (
    <div>
      <Button type="primary" onClick={subscribeForTestUpdates}>
        Subscribe for Test Updates
      </Button>
      <Divider />
      <h1>Test Elements:</h1>
      {tests.map((test) => (
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
