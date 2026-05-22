import { useRef, useState } from 'react';

export default function MessageInput({ onSend, onTyping, onShareLocation }) {
  const [text, setText] = useState('');
  const [files, setFiles] = useState([]);
  const inputRef = useRef(null);

  const submit = async (e) => {
    e?.preventDefault?.();
    await onSend({ text, files });
    setText(''); setFiles([]);
    if (inputRef.current) inputRef.current.value = '';
  };

  const onKey = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      submit();
    } else {
      onTyping?.();
    }
  };

  return (
    <form className="composer" onSubmit={submit}>
      <button type="button" className="composer__btn" title="Приложить грамоту"
              onClick={() => inputRef.current?.click()}>📎</button>
      <input ref={inputRef} type="file" multiple style={{ display: 'none' }}
             onChange={(e) => setFiles([...e.target.files])} />
      {onShareLocation && (
        <button type="button" className="composer__btn" title="Указать место на карте"
                onClick={onShareLocation}>📍</button>
      )}
      <textarea
        className="composer__textarea"
        placeholder="Молвите слово…"
        value={text}
        onChange={(e) => setText(e.target.value)}
        onKeyDown={onKey}
        rows={1}
      />
      <button type="submit" className="btn btn--primary">Послать</button>
      {files.length > 0 && (
        <div className="composer__files">{files.length} грамот(ы) приложено</div>
      )}
    </form>
  );
}
