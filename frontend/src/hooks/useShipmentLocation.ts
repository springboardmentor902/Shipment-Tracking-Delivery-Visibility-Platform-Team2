"use client";

import { useEffect, useState } from "react";
import { Client } from "@stomp/stompjs";

export type LiveLocation = {
  routeId: number;
  shipmentId: number;
  latitude: number;
  longitude: number;
  location?: string;
  recordedAt: string;
};

export function useShipmentLocation(
  apiUrl: string,
  token: string,
  shipmentId?: number,
) {
  const [latest, setLatest] = useState<LiveLocation>();
  const [activeShipmentId, setActiveShipmentId] = useState<number>();

  useEffect(() => {
    if (!token || !shipmentId) return;

    const client = new Client({
      brokerURL: `${apiUrl.replace(/^http/, "ws")}/api/ws/tracking`,
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 4000,
      onConnect: () => {
        setActiveShipmentId(shipmentId);
        client.subscribe(`/topic/shipments/${shipmentId}/location`, (message) => {
          setLatest(JSON.parse(message.body) as LiveLocation);
        });
      },
      onDisconnect: () => setActiveShipmentId(undefined),
      onWebSocketClose: () => setActiveShipmentId(undefined),
    });

    client.activate();
    return () => { void client.deactivate(); };
  }, [apiUrl, token, shipmentId]);

  return {
    location: latest?.shipmentId === shipmentId ? latest : undefined,
    connected: activeShipmentId === shipmentId,
  };
}
