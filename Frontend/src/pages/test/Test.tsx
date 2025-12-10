import { Client } from "@stomp/stompjs";
import { useRef } from "react";

const Test = () => {
  const clientRef = useRef<Client | null>(null);

  const connectWs = () => {
    const token = localStorage.getItem("token");
    if (!token) {
      console.error("No token found in localStorage");
      return;
    }

    // Create a new STOMP client using native WebSocket
    const client = new Client({
      brokerURL: "ws://localhost:8080/trackera/ws", // WebSocket URL with context path
      connectHeaders: {
        Authorization: `Bearer ${token}`,
      },
      debug: (str) => {
        console.log("[STOMP]", str);
      },
      onConnect: () => {
        console.log("Connected to WebSocket!");

        // Subscribe to a topic
        client.subscribe("/topic/log-status/e2035da6-93e3-4c3b-b45e-8da4e6ce00ec", (message) => {
          console.log("Received:", message.body);
        });
      },
      onStompError: (frame) => {
        console.error("Broker error:", frame.headers["message"]);
      },
    });

    client.activate();
    clientRef.current = client;
  };

  const disconnectWs = () => {
    if (clientRef.current) {
      clientRef.current.deactivate();
      console.log("WebSocket disconnected");
    }
  };

  return (
    <div>
      <button onClick={connectWs}>Connect to WebSocket</button>
      <button onClick={disconnectWs}>Disconnect</button>
    </div>
  );
};

export default Test;
