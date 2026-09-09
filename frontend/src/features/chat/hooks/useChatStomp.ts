import { useEffect, useState, useCallback, useRef } from 'react';
import { Client, IMessage, type StompSubscription } from '@stomp/stompjs';
import { useAuthStore } from '@/features/auth';
import { parseIncomingMessage, type IncomingMessage } from '../model/messageSchema';

export function useChatStomp() {
  const [client, setClient] = useState<Client | null>(null);
  const [isConnected, setIsConnected] = useState(false);
  const [reconnecting, setReconnecting] = useState(false);
  const [lastMessage, setLastMessage] = useState<IncomingMessage | null>(null);
  const subscriptionRef = useRef<StompSubscription | null>(null);
  const token = useAuthStore((state) => state.accessToken);

  useEffect(() => {
    if (!token) return;

    // We assume backend WebSocket is on /ws of the same domain, or from environment variable
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const brokerURL = process.env.NEXT_PUBLIC_WS_URL || `${protocol}//${window.location.host}/ws`;

    const stompClient = new Client({
      brokerURL,
      connectHeaders: {
        Authorization: `Bearer ${token}`
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
    });

    stompClient.onConnect = (frame) => {
      setIsConnected(true);
      setReconnecting(false);

      // Subscribe to user specific queue
      subscriptionRef.current?.unsubscribe();
      subscriptionRef.current = stompClient.subscribe('/user/queue/messages', (message: IMessage) => {
        const parsed = parseIncomingMessage(message.body);
        if (parsed) setLastMessage(parsed);
        else console.error('Invalid STOMP message payload', { destination: message.headers.destination });
      });
    };

    stompClient.onStompError = (frame) => {
      console.error('STOMP Error:', frame.headers['message']);
      console.error('Details:', frame.body);
    };

    stompClient.onWebSocketClose = () => {
      setIsConnected(false);
      setReconnecting(true);
    };

    stompClient.activate();
    setClient(stompClient);

    return () => {
      if (subscriptionRef.current) {
        subscriptionRef.current.unsubscribe();
      }
      stompClient.deactivate();
    };
  }, [token]);

  const sendMessage = useCallback((destination: string, body: Record<string, unknown>) => {
    if (client && isConnected) {
      client.publish({
        destination,
        body: JSON.stringify(body),
      });
      return true;
    }
    return false;
  }, [client, isConnected]);

  return {
    isConnected,
    reconnecting,
    lastMessage,
    sendMessage
  };
}
