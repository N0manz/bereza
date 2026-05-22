import { useCallback, useEffect, useRef, useState } from 'react';
import { useParams } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { getChat, listMessages, markRead, sendMessage } from '../api/chats';
import { uploadFile, fileUrl } from '../api/files';
import { createGeoPoint } from '../api/geo';
import { getStompClient, awaitConnected } from '../ws/stomp';
import MessageList from '../components/MessageList.jsx';
import MessageInput from '../components/MessageInput.jsx';
import LocationPickerModal from '../components/LocationPickerModal.jsx';

export default function ChatRoomPage() {
  const { chatId } = useParams();
  const { user } = useAuth();
  const [chat, setChat]         = useState(null);
  const [messages, setMessages] = useState([]);
  const [typingUsers, setTyping] = useState([]);
  const [showGeoPicker, setShowGeoPicker] = useState(false);
  const subsRef = useRef([]);
  const canShareLocation = user?.role === 'GUIDE' || user?.role === 'HOTEL' || user?.role === 'ADMIN';

  const mergeMessage = useCallback((m) => {
    setMessages((prev) => {
      const exists = prev.find(x => x.id === m.id);
      if (exists) return prev.map(x => x.id === m.id ? m : x);
      return [...prev, m];
    });
  }, []);

  const loadHistory = useCallback(async () => {
    const msgs = await listMessages(chatId, { size: 50 });
    setMessages([...msgs].reverse());
    const last = msgs[0];
    if (last) markRead(chatId, last.id).catch(() => {});
  }, [chatId]);

  useEffect(() => {
    if (!user) return;
    getChat(chatId).then(setChat).catch(() => setChat(null));
    loadHistory();
    const client = getStompClient(user.username);

    let cleaned = false;
    (async () => {
      await awaitConnected();
      if (cleaned) return;
      subsRef.current = [
        client.subscribe(`/topic/chats.${chatId}`, (frame) => {
          const m = JSON.parse(frame.body);
          mergeMessage(m);
          if (m.senderId !== user.id) {
            markRead(chatId, m.id).catch(() => {});
          }
        }),
        client.subscribe(`/topic/chats.${chatId}.typing`, (frame) => {
          const data = JSON.parse(frame.body);
          if (data.userId === user.id) return;
          setTyping((t) => {
            const exists = t.find(x => x.userId === data.userId);
            const ts = Date.now();
            return exists
              ? t.map(x => x.userId === data.userId ? { ...x, ts } : x)
              : [...t, { ...data, ts }];
          });
        }),
        client.subscribe(`/topic/chats.${chatId}.reads`, (frame) => {
          const { messageId, userId } = JSON.parse(frame.body);
          setMessages((prev) => prev.map(m =>
            m.id === messageId && !m.readBy?.includes(userId)
              ? { ...m, readBy: [...(m.readBy || []), userId] }
              : m));
        }),
      ];
    })();

    const ti = setInterval(() => {
      const now = Date.now();
      setTyping((t) => t.filter(x => now - x.ts < 3000));
    }, 1000);

    return () => {
      cleaned = true;
      subsRef.current.forEach(s => s.unsubscribe?.());
      subsRef.current = [];
      clearInterval(ti);
    };
  }, [chatId, user, loadHistory, mergeMessage]);

  const onSend = async ({ text, files }) => {
    let attachmentIds = [];
    for (const f of files) {
      try {
        const att = await uploadFile(f);
        attachmentIds.push(att.id);
      } catch (e) { console.error(e); }
    }
    if (!text.trim() && attachmentIds.length === 0) return;
    const saved = await sendMessage(chatId, {
      type: attachmentIds.length > 0 ? 'FILE' : 'TEXT',
      content: text,
      attachmentIds,
    });
    if (saved && saved.id) mergeMessage(saved);
  };

  const onTyping = () => {
    const client = getStompClient(user.username);
    try {
      client.publish({ destination: `/app/chats/${chatId}/typing`, body: '{}' });
    } catch {}
  };

  const onShareLocation = () => {
    if (!canShareLocation) return;
    setShowGeoPicker(true);
  };

  const onPickLocation = async ({ lat, lng, title }) => {
    setShowGeoPicker(false);
    const label = title || `Метка (${lat.toFixed(5)}, ${lng.toFixed(5)})`;
    try {
      await createGeoPoint({
        chatId: Number(chatId),
        type: 'USER_LOCATION',
        title: label,
        latitude: lat, longitude: lng,
        expiresAt: new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString(),
      });
    } catch (e) { console.error('createGeoPoint failed', e); }
    const saved = await sendMessage(chatId, {
      type: 'GEO',
      content: label,
      payload: { latitude: lat, longitude: lng, kind: 'USER_LOCATION', title: label },
    });
    if (saved && saved.id) mergeMessage(saved);
  };

  return (
    <div className="chat">
      <header className="chat__header">
        <div>
          <div className="strong">{chat?.title || (chat?.members || []).map(m => m.displayName).join(', ')}</div>
          <div className="muted small">
            {chat?.type === 'PERSONAL' ? 'Личная беседа' : `Дружина • ${chat?.members?.length} душ`}
          </div>
        </div>
      </header>

      <MessageList
        messages={messages}
        currentUserId={user?.id}
        fileUrl={fileUrl}
      />

      {typingUsers.length > 0 && (
        <div className="typing">
          {typingUsers.map(t => t.displayName).join(', ')} печатает…
        </div>
      )}

      <MessageInput
        onSend={onSend}
        onTyping={onTyping}
        onShareLocation={canShareLocation ? onShareLocation : null}
      />

      {showGeoPicker && (
        <LocationPickerModal
          onClose={() => setShowGeoPicker(false)}
          onPick={onPickLocation}
        />
      )}
    </div>
  );
}
