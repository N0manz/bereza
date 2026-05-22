import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

let client = null;
let connectingPromise = null;
const subscribers = new Set();

export function getStompClient(username) {
  if (client && client.connected) return client;
  if (!client) {
    client = new Client({
      webSocketFactory: () => new SockJS(import.meta.env.VITE_WS_BASE || '/ws'),
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      debug: () => {},
      onConnect: () => subscribers.forEach((cb) => cb('connected')),
      onWebSocketClose: () => subscribers.forEach((cb) => cb('closed')),
    });
    client.activate();
  }
  return client;
}

export function onStompStatus(cb) {
  subscribers.add(cb);
  return () => subscribers.delete(cb);
}

export async function awaitConnected() {
  if (client?.connected) return;
  if (!connectingPromise) {
    connectingPromise = new Promise((resolve) => {
      const off = onStompStatus((s) => {
        if (s === 'connected') {
          off();
          connectingPromise = null;
          resolve();
        }
      });
    });
  }
  return connectingPromise;
}

export function deactivate() {
  if (client) {
    client.deactivate();
    client = null;
  }
}
