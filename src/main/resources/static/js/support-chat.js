(function () {
  var widget = document.getElementById('supportChat');
  if (!widget) return;

  var toggle = document.getElementById('supportChatToggle');
  var panel = document.getElementById('supportChatPanel');
  var close = document.getElementById('supportChatClose');
  var form = document.getElementById('supportChatForm');
  var input = document.getElementById('supportChatInput');
  var send = document.getElementById('supportChatSend');
  var messages = document.getElementById('supportChatMessages');
  var history = [];
  var statusChecked = false;

  function addMessage(text, type) {
    var bubble = document.createElement('p');
    bubble.className = 'support-chat-bubble ' + type;
    bubble.textContent = text;
    messages.appendChild(bubble);
    messages.scrollTop = messages.scrollHeight;
    return bubble;
  }

  function setOpen(open) {
    panel.hidden = !open;
    toggle.setAttribute('aria-expanded', String(open));
    if (open) {
      input.focus();
      if (!statusChecked) checkAvailability();
    }
  }

  function checkAvailability() {
    statusChecked = true;
    fetch('/api/support-chat/status', { headers: { 'Accept': 'application/json' } })
      .then(function (response) {
        if (!response.ok) throw new Error('No se pudo comprobar la disponibilidad del asistente.');
        return response.json();
      })
      .then(function (status) {
        if (!status.enabled) {
          addMessage('El asistente aún no está configurado. El refugio puede atenderte por sus canales de contacto.', 'error');
          input.disabled = true;
          send.disabled = true;
        }
      })
      .catch(function () {
        addMessage('No se pudo comprobar la disponibilidad del asistente. Inténtalo más tarde.', 'error');
      });
  }

  toggle.addEventListener('click', function () {
    setOpen(panel.hidden);
  });
  close.addEventListener('click', function () {
    setOpen(false);
    toggle.focus();
  });

  form.addEventListener('submit', function (event) {
    event.preventDefault();
    var message = input.value.trim();
    if (!message || send.disabled) return;

    addMessage(message, 'user');
    input.value = '';
    input.disabled = true;
    send.disabled = true;

    var csrfToken = document.getElementById('supportChatCsrfToken');
    var headers = { 'Content-Type': 'application/json', 'Accept': 'application/json' };
    if (csrfToken) {
      headers['X-CSRF-TOKEN'] = csrfToken.value;
    }

    fetch('/api/support-chat', {
      method: 'POST',
      headers: headers,
      body: JSON.stringify({ message: message, history: history })
    })
      .then(function (response) {
        return response.json().then(function (body) {
          if (!response.ok) throw new Error(body.error || 'No se pudo enviar la pregunta.');
          return body;
        });
      })
      .then(function (body) {
        history.push({ role: 'user', text: message }, { role: 'model', text: body.reply });
        if (history.length > 10) history = history.slice(history.length - 10);
        addMessage(body.reply, 'assistant');
      })
      .catch(function (error) {
        addMessage(error.message || 'No fue posible contactar al asistente. Inténtalo más tarde.', 'error');
      })
      .finally(function () {
        input.disabled = false;
        send.disabled = false;
        input.focus();
      });
  });
}());
