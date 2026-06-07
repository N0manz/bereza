import { useCallback, useEffect, useRef, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import {
  getChat, listMessages, listMembers, markRead, sendMessage,
  addMember, removeMember, leaveChat,
} from '../api/chats';
import { searchUsers } from '../api/users';
import { uploadFile, fileUrl } from '../api/files';
import { createGeoPoint } from '../api/geo';
import { getStompClient, awaitConnected } from '../ws/stomp';
import MessageList from '../components/MessageList.jsx';
import MessageInput from '../components/MessageInput.jsx';
import LocationPickerModal from '../components/LocationPickerModal.jsx';
import { roleLabel } from '../utils/labels';

export default function ChatRoomPage() {
  const { chatId } = useParams();
  const { user } = useAuth();
  const navigate = useNavigate();

  const [chat, setChat]         = useState(null);
  const [messages, setMessages] = useState([]);
  const [typingUsers, setTyping] = useState([]);
  const [showGeoPicker, setShowGeoPicker] = useState(false);
  const [showMembers, setShowMembers]     = useState(false);
  const [members, setMembers]             = useState([]);
  const subsRef = useRef([]);

  const myMember = members.find(m => m.userId === user?.id);
  const isGroup  = chat?.type === 'GROUP';
  const canManageMembers = isGroup && (
    user?.role === 'GUIDE'
    || myMember?.memberRole === 'OWNER'
    || myMember?.memberRole === 'ADMIN'
  );

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

  const loadMembers = useCallback(async () => {
    try {
      const list = await listMembers(chatId);
      setMembers(list);
    } catch {}
  }, [chatId]);

  useEffect(() => {
    if (!user) return;
    getChat(chatId).then(setChat).catch(() => setChat(null));
    loadHistory();
    loadMembers();

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
  }, [chatId, user, loadHistory, loadMembers, mergeMessage]);

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

  const onLeave = async () => {
    if (!window.confirm('Выйти из беседы?')) return;
    try {
      await leaveChat(chatId);
      navigate('/chats');
    } catch (e) {
      alert(e?.response?.data?.detail || 'Не удалось выйти из беседы');
    }
  };

  const onRemoveMember = async (userId) => {
    try {
      await removeMember(chatId, userId);
      await loadMembers();
    } catch (e) {
      alert(e?.response?.data?.detail || 'Не удалось удалить участника');
    }
  };

  const onAddMember = async (userId) => {
    try {
      await addMember(chatId, userId);
      await loadMembers();
    } catch (e) {
      alert(e?.response?.data?.detail || 'Не удалось добавить участника');
    }
  };

  const chatTitle = chat?.title || members.filter(m => m.userId !== user?.id).map(m => m.displayName).join(', ');

  return (
    <div className="chat">
      <header className="chat__header">
        <div>
          <div className="strong">{chatTitle}</div>
          <div className="muted small">
            {isGroup ? `Дружина • ${members.length} душ` : 'Личная беседа'}
          </div>
        </div>
        <button
          className="btn btn--ghost btn--sm"
          onClick={() => setShowMembers(true)}
          title="Участники"
        >
          👥 {members.length}
        </button>
      </header>

      <MessageList messages={messages} currentUserId={user?.id} fileUrl={fileUrl} />

      {typingUsers.length > 0 && (
        <div className="typing">
          {typingUsers.map(t => t.displayName).join(', ')} печатает…
        </div>
      )}

      <MessageInput
        onSend={onSend}
        onTyping={onTyping}
        onShareLocation={() => setShowGeoPicker(true)}
      />

      {showGeoPicker && (
        <LocationPickerModal
          onClose={() => setShowGeoPicker(false)}
          onPick={onPickLocation}
        />
      )}

      {showMembers && (
        <ChatMembersPanel
          members={members}
          currentUserId={user?.id}
          isGroup={isGroup}
          myMemberRole={myMember?.memberRole}
          canManage={canManageMembers}
          onClose={() => setShowMembers(false)}
          onRemove={onRemoveMember}
          onAdd={onAddMember}
          onLeave={isGroup ? onLeave : null}
        />
      )}
    </div>
  );
}

function ChatMembersPanel({ members, currentUserId, isGroup, myMemberRole, canManage, onClose, onRemove, onAdd, onLeave }) {
  const [showSearch, setShowSearch] = useState(false);
  const [q, setQ]                   = useState('');
  const [results, setResults]        = useState([]);

  useEffect(() => {
    if (!showSearch) { setQ(''); setResults([]); return; }
    const t = setTimeout(async () => {
      if (!q.trim()) { setResults([]); return; }
      try {
        const page = await searchUsers(q, 0, 10);
        const existingIds = new Set(members.map(m => m.userId));
        setResults((page.items || []).filter(u => !existingIds.has(u.id)));
      } catch { setResults([]); }
    }, 250);
    return () => clearTimeout(t);
  }, [q, showSearch, members]);

  const memberRoleLabel = (r) => ({ OWNER: 'владелец', ADMIN: 'админ', MEMBER: '' }[r] ?? '');

  return (
    <div className="modal" role="dialog" aria-modal="true" onClick={onClose}>
      <div className="modal__card modal__card--side" onClick={e => e.stopPropagation()}>
        <div className="modal__head">
          <h3>Участники беседы</h3>
          <button className="btn btn--ghost btn--icon" onClick={onClose}>✕</button>
        </div>

        <ul className="members-list">
          {members.map(m => {
            const isSelf   = m.userId === currentUserId;
            const isOwner  = m.memberRole === 'OWNER';
            const canKick  = canManage && !isSelf && !isOwner;
            return (
              <li key={m.userId} className="members-list__item">
                <div className="avatar">{(m.displayName || '?')[0]}</div>
                <div className="list__main">
                  <div className="strong">
                    {m.displayName}
                    {isSelf && <span className="muted"> (вы)</span>}
                  </div>
                  <div className="muted small">
                    @{m.username} • {roleLabel(m.role)}
                    {memberRoleLabel(m.memberRole) && ` • ${memberRoleLabel(m.memberRole)}`}
                  </div>
                </div>
                {canKick && (
                  <button
                    className="btn btn--ghost btn--sm btn--danger"
                    onClick={() => onRemove(m.userId)}
                    title="Удалить из беседы"
                  >
                    Удалить
                  </button>
                )}
              </li>
            );
          })}
        </ul>

        {canManage && (
          <div className="members-add">
            {!showSearch ? (
              <button className="btn btn--primary btn--sm" onClick={() => setShowSearch(true)}>
                + Добавить участника
              </button>
            ) : (
              <>
                <input
                  className="input"
                  placeholder="Поиск по имени…"
                  value={q}
                  onChange={e => setQ(e.target.value)}
                  autoFocus
                />
                <ul className="picklist">
                  {results.map(u => (
                    <li key={u.id} className="picklist__item" onClick={() => { onAdd(u.id); setShowSearch(false); }}>
                      <div className="avatar">{u.displayName[0]}</div>
                      <div className="list__main">
                        <div className="strong">{u.displayName}</div>
                        <div className="muted small">@{u.username} • {roleLabel(u.role)}</div>
                      </div>
                    </li>
                  ))}
                  {q.trim() && results.length === 0 && (
                    <li className="muted small" style={{ padding: '8px 12px' }}>Никого не найдено</li>
                  )}
                </ul>
                <button className="btn btn--ghost btn--sm" onClick={() => setShowSearch(false)}>Отмена</button>
              </>
            )}
          </div>
        )}

        {onLeave && myMemberRole !== 'OWNER' && (
          <div className="members-footer">
            <button className="btn btn--ghost btn--sm btn--danger" onClick={onLeave}>
              Выйти из беседы
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
