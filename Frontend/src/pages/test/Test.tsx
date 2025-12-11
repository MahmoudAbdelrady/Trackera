import { Client } from "@stomp/stompjs";
import { Divider } from "antd";
import { useRef, useState } from "react";
import { userApis } from "../../state/api";

const Test = () => {
  const clientRef = useRef<Client | null>(null);
  const [status, setStatus] = useState("Unsynced");

  const connectWs = () => {
    // Create a new STOMP client using native WebSocket
    const client = new Client({
      brokerURL: "ws://localhost:8080/trackera/ws", // WebSocket URL with context path
      beforeConnect: () => {
        client.connectHeaders = {
          Authorization: `Bearer ${localStorage.getItem("token")}`,
        };
      },
      debug: (str) => {
        console.log("[STOMP]", str);
      },
      onConnect: () => {
        console.log("Connected to WebSocket!");

        // Subscribe to a topic
        client.subscribe("/topic/log-status/e2035da6-93e3-4c3b-b45e-8da4e6ce00ec", (message) => {
          const data: Record<string, any> = JSON.parse(message.body);
          setStatus(data.message);
          console.log("Received:", data.message);
        });
      },
      onStompError: async (frame) => {
        const errorMsg = frame.headers["message"];
        if (errorMsg.startsWith("401")) {
          await userApis.refreshToken();
          return;
        }
        console.error("Broker error:", errorMsg);
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
      <Divider />
      <div>
        Status: <span>{status}</span>
      </div>
    </div>
  );
};

export default Test;
