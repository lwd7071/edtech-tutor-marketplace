import { useEffect, useState, useCallback, useRef } from 'react';
import { Client, IMessage } from '@stomp/stompjs';
import { useAuthStore } from '@/features/auth';
import Cookies from 'js-cookie';

export function useChatStomp() {
  const [client, setClient] = useState<Client | null>(null);
  const [isConnected, setIsConnected] = useState(false);
  const [reconnecting, setReconnecting] = useState(false);
  const [lastMessage, setLastMessage] = useState<any>(null);
  const subscriptionRef = useRef<any>(null);

  useEffect(() => {
    const token = Cookies.get('accessToken') || useAuthStore.getState().accessToken;
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
      console.log('Connected to STOMP');
      setIsConnected(true);
      setReconnecting(false);

      // Subscribe to user specific queue
      subscriptionRef.current = stompClient.subscribe('/user/queue/messages', (message: IMessage) => {
        if (message.body) {
          const parsed = JSON.parse(message.body);
          setLastMessage(parsed);
        }
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
  }, []);

  const sendMessage = useCallback((destination: string, body: any) => {
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
