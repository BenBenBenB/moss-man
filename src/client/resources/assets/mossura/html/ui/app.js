(() => {
  const app = document.getElementById('app');
  let uiState = window.__MOSSURA_STATE__ || { screen: 'project', loading: true };
  let viewMode = 'board';
  let searchTerm = '';
  let screenOverride = null;
  let activeModal = null;

  const escapeHtml = (value) => {
    if (value === null || value === undefined) return '';
    return String(value)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  };

  const formatDate = (ts) => {
    if (!ts) return '-';
    try {
      return new Date(ts).toLocaleString();
    } catch (e) {
      return '-';
    }
  };

  const sendAction = (action, payload = {}) => {
    if (window.cefQuery) {
      window.cefQuery({
        request: JSON.stringify({ action, payload }),
        onSuccess: () => {},
        onFailure: () => {}
      });
    }
  };

  const setScreen = (screen) => {
    screenOverride = screen;
    render();
  };

  const openModal = (name) => {
    activeModal = name;
    render();
  };

  const closeModal = () => {
    activeModal = null;
    render();
  };

  const projectName = () => (uiState.project && uiState.project.name) || 'Mossura Project';

  const resolveScreen = () => {
    if (screenOverride !== null) {
      return screenOverride;
    }
    if (uiState && uiState.screen) {
      return uiState.screen;
    }
    const hash = window.location.hash.replace('#', '').trim();
    const parts = hash.split('/').filter(Boolean);
    if (!parts.length) return 'project';
    if (parts[0] === 'ticket' && parts[2] === 'edit') return 'ticket-edit';
    if (parts[0] === 'ticket' && parts[2] === 'comment') return 'ticket-comment';
    if (parts[0] === 'settings') return 'settings';
    return parts[0];
  };

  const renderTopbar = (subtitle) => {
    const screen = resolveScreen();
    const showBack = screen !== 'project';
    return `
      <div class="topbar">
        <div class="brand">
          <span>${escapeHtml(subtitle || 'Workspace')}</span>
          <h1>${escapeHtml(projectName())}</h1>
        </div>
        <div class="top-actions">
          ${showBack ? '<button class="btn secondary" data-action="back">Back</button>' : ''}
          <div class="pill">${escapeHtml(screen || 'project')}</div>
          <button class="btn" data-action="new-ticket">New Ticket</button>
          <button class="btn secondary" data-action="open-settings">Settings</button>
        </div>
      </div>
    `;
  };

  const renderSidebar = () => {
    const project = uiState.project || {};
    const members = project.members || [];
    const statuses = project.statuses || [];
    return `
      <aside class="sidebar">
        <div class="card">
          <h3>Overview</h3>
          <div class="kpi">
            <strong>${project.ticketCount || 0}</strong>
            <span>Total tickets</span>
          </div>
          <div class="kpi">
            <strong>${members.length}</strong>
            <span>Team members</span>
          </div>
        </div>
        <div class="card">
          <h3>Statuses</h3>
          <div class="tag-list">
            ${statuses.map((status) => `<span class="tag">${escapeHtml(status)}</span>`).join('')}
          </div>
        </div>
        <div class="card">
          <h3>Team</h3>
          <div class="list">
            ${(members.length ? members : [{ name: 'No members yet' }])
              .map((m) => `<div class="list-item">${escapeHtml(m.name || '')}</div>`)
              .join('')}
          </div>
        </div>
      </aside>
    `;
  };

  const filterTickets = (tickets) => {
    if (!searchTerm.trim()) return tickets;
    const needle = searchTerm.trim().toLowerCase();
    return tickets.filter((ticket) => {
      const haystack = `${ticket.number} ${ticket.title} ${ticket.description || ''}`.toLowerCase();
      return haystack.includes(needle);
    });
  };

  const renderBoard = (tickets, statuses) => {
    const byStatus = {};
    statuses.forEach((s) => (byStatus[s] = []));
    tickets.forEach((ticket) => {
      const state = ticket.state || 'Unsorted';
      if (!byStatus[state]) byStatus[state] = [];
      byStatus[state].push(ticket);
    });

    return `
      <div class="board">
        ${Object.keys(byStatus).map((status) => {
          const stack = byStatus[status];
          return `
            <div class="board-column">
              <div class="column-title">${escapeHtml(status)}</div>
              ${stack.length
                ? stack.map((ticket) => `
                    <div class="ticket-card">
                      <strong>#${ticket.number} ${escapeHtml(ticket.title)}</strong>
                      <small>${escapeHtml(ticket.description || 'No description')}</small>
                      <div class="meta">
                        <span>${escapeHtml(ticket.type || 'Task')}</span>
                        <span>${escapeHtml(ticket.assigneeName || 'Unassigned')}</span>
                      </div>
                    </div>
                  `).join('')
                : '<div class="ticket-card"><small>Nothing here yet.</small></div>'}
            </div>
          `;
        }).join('')}
      </div>
    `;
  };

  const renderList = (tickets) => {
    return `
      <table class="table">
        <thead>
          <tr>
            <th>#</th>
            <th>Title</th>
            <th>Status</th>
            <th>Type</th>
            <th>Assignee</th>
          </tr>
        </thead>
        <tbody>
          ${tickets.map((ticket) => `
            <tr>
              <td>${ticket.number}</td>
              <td>${escapeHtml(ticket.title)}</td>
              <td>${escapeHtml(ticket.state)}</td>
              <td>${escapeHtml(ticket.type)}</td>
              <td>${escapeHtml(ticket.assigneeName || '-')}</td>
            </tr>
          `).join('')}
        </tbody>
      </table>
    `;
  };

  const renderProject = () => {
    const project = uiState.project || {};
    const tickets = filterTickets(project.tickets || []);
    const statuses = project.statuses || ['Backlog', 'In Progress', 'Done'];

    return `
      <div class="app-shell">
        ${renderTopbar('Project Hub')}
        <div class="layout">
          ${renderSidebar()}
          <div class="main-panel">
            <div class="panel-header">
              <h2>${escapeHtml(project.name || 'Project Board')}</h2>
              <div class="panel-actions">
                <input class="search-input" data-role="search" type="search" placeholder="Search tickets" value="${escapeHtml(searchTerm)}" />
                <div class="switcher">
                  <button data-view="board" class="${viewMode === 'board' ? 'active' : ''}">Board</button>
                  <button data-view="list" class="${viewMode === 'list' ? 'active' : ''}">List</button>
                </div>
              </div>
            </div>
            ${uiState.loading ? '<div class="notice">Loading project data...</div>' : ''}
            ${viewMode === 'board' ? renderBoard(tickets, statuses) : renderList(tickets)}
          </div>
        </div>
      </div>
    `;
  };

  const renderTicket = () => {
    const ticket = uiState.ticket || {};
    const subtasks = ticket.subtasks || [];
    const comments = ticket.comments || [];

    return `
      <div class="app-shell">
        ${renderTopbar('Ticket Detail')}
        <div class="layout">
          ${renderSidebar()}
          <div class="main-panel">
            <div class="panel-header">
              <h2>#${ticket.number || '-'} ${escapeHtml(ticket.title || 'Ticket')}</h2>
            </div>
            <div class="detail-grid">
              <div class="detail-card">
                <h3>Summary</h3>
                <p>${escapeHtml(ticket.description || 'No description provided.')}</p>
                <div class="list">
                  <div class="list-item">Type: ${escapeHtml(ticket.type || 'Task')}</div>
                  <div class="list-item">State: ${escapeHtml(ticket.state || '-')}</div>
                  <div class="list-item">Priority: ${escapeHtml(ticket.priority || '-')}</div>
                  <div class="list-item">Assignee: ${escapeHtml(ticket.assigneeName || 'Unassigned')}</div>
                </div>
              </div>
              <div class="detail-card">
                <h3>Subtasks</h3>
                <div class="list">
                  ${subtasks.length ? subtasks.map((s) => `
                    <div class="list-item">${s.completed ? '[x]' : '[ ]'} ${escapeHtml(s.name)}</div>
                  `).join('') : '<div class="list-item">No subtasks yet.</div>'}
                </div>
              </div>
            </div>
            <div class="detail-card">
              <h3>Comments</h3>
              <div class="list">
                ${comments.length ? comments.map((c) => `
                  <div class="list-item"><strong>${escapeHtml(c.authorName)}</strong>: ${escapeHtml(c.message)}</div>
                `).join('') : '<div class="list-item">No comments yet.</div>'}
              </div>
            </div>
          </div>
        </div>
      </div>
    `;
  };

  const renderEditor = () => {
    const ticket = uiState.ticket || {};
    return `
      <div class="app-shell">
        ${renderTopbar('Ticket Editor')}
        <div class="layout">
          ${renderSidebar()}
          <div class="main-panel">
            <div class="panel-header">
              <h2>${uiState.screen === 'ticket-new' ? 'Create Ticket' : 'Edit Ticket'}</h2>
            </div>
            <div class="detail-card">
              <h3>Details</h3>
              <div class="list">
                <div class="list-item">Title: ${escapeHtml(ticket.title || '')}</div>
                <div class="list-item">Description: ${escapeHtml(ticket.description || '')}</div>
                <div class="list-item">Type: ${escapeHtml(ticket.type || '')}</div>
                <div class="list-item">State: ${escapeHtml(ticket.state || '')}</div>
                <div class="list-item">Priority: ${escapeHtml(ticket.priority || '')}</div>
              </div>
              <div class="notice">Editor interactions are ready to be wired to game actions.</div>
            </div>
          </div>
        </div>
      </div>
    `;
  };

  const renderSettings = () => {
    const project = uiState.project || {};
    return `
      <div class="app-shell">
        ${renderTopbar('Settings')}
        <div class="layout">
          ${renderSidebar()}
          <div class="main-panel">
            <div class="panel-header">
              <h2>Project Settings</h2>
            </div>
            <div class="detail-card">
              <h3>Project</h3>
              <div class="field-group">
                <label>Project Name</label>
                <input data-field="project-name" type="text" value="${escapeHtml(project.name || '')}" />
              </div>
              <div class="field-group">
                <label>Statuses (comma separated)</label>
                <textarea data-field="project-statuses">${escapeHtml((project.statuses || []).join(', '))}</textarea>
              </div>
              <div class="field-group">
                <label>Ticket Types (comma separated)</label>
                <textarea data-field="project-types">${escapeHtml((project.ticketTypes || []).join(', '))}</textarea>
              </div>
              <div class="modal-actions">
                <button class="btn secondary" data-action="back">Cancel</button>
                <button class="btn" data-action="save-settings">Save Settings</button>
              </div>
            </div>
          </div>
        </div>
      </div>
    `;
  };

  const renderComment = () => {
    return `
      <div class="app-shell">
        ${renderTopbar('Add Comment')}
        <div class="layout">
          ${renderSidebar()}
          <div class="main-panel">
            <div class="panel-header">
              <h2>Compose Comment</h2>
            </div>
            <div class="detail-card">
              <h3>Note</h3>
              <div class="notice">Comment form is rendered here. Hook this to Minecraft actions when ready.</div>
            </div>
          </div>
        </div>
      </div>
    `;
  };

  const renderSubtask = () => {
    return `
      <div class="app-shell">
        ${renderTopbar('Subtask')}
        <div class="layout">
          ${renderSidebar()}
          <div class="main-panel">
            <div class="panel-header">
              <h2>Subtask Flow</h2>
            </div>
            <div class="detail-card">
              <h3>Subtask</h3>
              <div class="notice">Subtask editor UI is ready for wiring.</div>
            </div>
          </div>
        </div>
      </div>
    `;
  };

  const renderModal = () => {
    if (activeModal !== 'new-ticket') return '';
    const project = uiState.project || {};
    const types = project.ticketTypes || ['Task'];
    const statuses = project.statuses || ['To Do'];
    const priorities = ['LOW', 'MEDIUM', 'HIGH'];
    return `
      <div class="modal-backdrop" data-role="modal-backdrop">
        <form class="modal" data-role="new-ticket-form">
          <div class="modal-header">
            <h3>New Ticket</h3>
            <button type="button" class="modal-close" data-action="close-modal">x</button>
          </div>
          <div class="field-group">
            <label>Title</label>
            <input data-field="title" type="text" required />
          </div>
          <div class="field-group">
            <label>Description</label>
            <textarea data-field="description"></textarea>
          </div>
          <div class="field-group">
            <label>Type</label>
            <select data-field="type">
              ${types.map((type) => `<option value="${escapeHtml(type)}">${escapeHtml(type)}</option>`).join('')}
            </select>
          </div>
          <div class="field-group">
            <label>Status</label>
            <select data-field="state">
              ${statuses.map((state) => `<option value="${escapeHtml(state)}">${escapeHtml(state)}</option>`).join('')}
            </select>
          </div>
          <div class="field-group">
            <label>Priority</label>
            <select data-field="priority">
              ${priorities.map((priority) => `<option value="${priority}">${priority}</option>`).join('')}
            </select>
          </div>
          <div class="field-group">
            <label>Assignee</label>
            <input data-field="assignee" type="text" placeholder="Optional" />
          </div>
          <div class="modal-actions">
            <button type="button" class="btn secondary" data-action="close-modal">Cancel</button>
            <button type="submit" class="btn">Create</button>
          </div>
        </form>
      </div>
    `;
  };

  const render = () => {
    const screen = resolveScreen();
    let markup = '';
    switch (screen) {
      case 'project':
        markup = renderProject();
        break;
      case 'ticket':
        markup = renderTicket();
        break;
      case 'ticket-new':
      case 'ticket-edit':
        markup = renderEditor();
        break;
      case 'settings':
        markup = renderSettings();
        break;
      case 'ticket-comment':
        markup = renderComment();
        break;
      case 'subtask-new':
      case 'subtask-edit':
      case 'subtask-comment':
        markup = renderSubtask();
        break;
      default:
        markup = renderProject();
    }
    app.innerHTML = markup + renderModal();

    const switcher = app.querySelector('.switcher');
    if (switcher) {
      switcher.querySelectorAll('button').forEach((btn) => {
        btn.addEventListener('click', () => {
          viewMode = btn.dataset.view || 'board';
          render();
        });
      });
    }

    const search = app.querySelector('[data-role="search"]');
    if (search) {
      search.addEventListener('input', (event) => {
        searchTerm = event.target.value || '';
        render();
      });
    }

    const newTicketBtn = app.querySelector('[data-action="new-ticket"]');
    if (newTicketBtn) {
      newTicketBtn.addEventListener('click', () => openModal('new-ticket'));
    }

    const settingsBtn = app.querySelector('[data-action="open-settings"]');
    if (settingsBtn) {
      settingsBtn.addEventListener('click', () => setScreen('settings'));
    }

    const backBtn = app.querySelector('[data-action="back"]');
    if (backBtn) {
      backBtn.addEventListener('click', () => setScreen(null));
    }

    const modalClose = app.querySelectorAll('[data-action="close-modal"]');
    modalClose.forEach((btn) => btn.addEventListener('click', closeModal));

    const modalBackdrop = app.querySelector('[data-role="modal-backdrop"]');
    if (modalBackdrop) {
      modalBackdrop.addEventListener('click', (event) => {
        if (event.target === modalBackdrop) closeModal();
      });
    }

    const newTicketForm = app.querySelector('[data-role="new-ticket-form"]');
    if (newTicketForm) {
      newTicketForm.addEventListener('submit', (event) => {
        event.preventDefault();
        const title = newTicketForm.querySelector('[data-field="title"]').value.trim();
        const description = newTicketForm.querySelector('[data-field="description"]').value.trim();
        const type = newTicketForm.querySelector('[data-field="type"]').value;
        const state = newTicketForm.querySelector('[data-field="state"]').value;
        const priority = newTicketForm.querySelector('[data-field="priority"]').value;
        const assigneeName = newTicketForm.querySelector('[data-field="assignee"]').value.trim();
        if (!title) return;
        sendAction('create-ticket', { title, description, type, state, priority, assigneeName });
        closeModal();
      });
    }

    const saveSettings = app.querySelector('[data-action="save-settings"]');
    if (saveSettings) {
      saveSettings.addEventListener('click', () => {
        const name = (app.querySelector('[data-field="project-name"]') || {}).value || '';
        const statusesRaw = (app.querySelector('[data-field="project-statuses"]') || {}).value || '';
        const typesRaw = (app.querySelector('[data-field="project-types"]') || {}).value || '';
        const statuses = statusesRaw.split(',').map((s) => s.trim()).filter(Boolean);
        const ticketTypes = typesRaw.split(',').map((s) => s.trim()).filter(Boolean);
        sendAction('update-project-settings', { name, statuses, ticketTypes });
        setScreen(null);
      });
    }
  };

  window.MossuraUI = {
    setState(next) {
      if (next && typeof next === 'object') {
        uiState = next;
      }
      render();
    },
    navigate(screen) {
      setScreen(screen);
    }
  };

  render();
})();
