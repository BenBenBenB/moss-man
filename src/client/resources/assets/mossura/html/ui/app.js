(() => {
  const app = document.getElementById('app');
  let uiState = window.__MOSSURA_STATE__ || { screen: 'project', loading: true };
  let viewMode = 'board';
  let searchTerm = '';
  let screenOverride = null;
  let activeModal = null;
  let pendingSearchFocus = false;
  let pendingSearchSelection = null;

  const escapeHtml = (value) => {
    if (value === null || value === undefined) return '';
    return String(value)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
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

  const openModal = (type, data = {}) => {
    activeModal = { type, ...data };
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
    const canEdit = uiState.canEdit !== false;
    return `
      <div class="topbar">
        <div class="brand">
          <span>${escapeHtml(subtitle || 'Workspace')}</span>
          <h1>${escapeHtml(projectName())}</h1>
        </div>
        <div class="top-actions">
          ${showBack ? '<button class="btn secondary" data-action="back">Back</button>' : ''}
          <div class="pill">${escapeHtml(screen || 'project')}</div>
          <button class="btn" data-action="new-ticket" ${canEdit ? '' : 'disabled'}>New Ticket</button>
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
                    <div class="ticket-card" data-action="open-ticket" data-ticket-id="${ticket.id}">
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
            <tr data-action="open-ticket" data-ticket-id="${ticket.id}">
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
    const project = uiState.project || {};
    const subtasks = ticket.subtasks || [];
    const comments = ticket.comments || [];
    const statuses = project.statuses || [];
    const canEdit = uiState.canEdit !== false;

    return `
      <div class="app-shell">
        ${renderTopbar('Ticket Detail')}
        <div class="layout">
          ${renderSidebar()}
          <div class="main-panel">
            <div class="panel-header">
              <h2>#${ticket.number || '-'} ${escapeHtml(ticket.title || 'Ticket')}</h2>
              <div class="panel-actions">
                ${canEdit ? '<button class="btn secondary" data-action="edit-ticket">Edit Ticket</button>' : ''}
                ${canEdit ? '<button class="btn" data-action="add-ticket-comment">Add Comment</button>' : ''}
                ${canEdit ? '<button class="btn" data-action="add-subtask">Add Subtask</button>' : ''}
              </div>
            </div>
            ${canEdit && statuses.length ? `
              <div class="status-row">
                <span>Move to:</span>
                ${statuses.map((state) => `<button class="ghost-btn" data-action="set-status" data-status="${escapeHtml(state)}">${escapeHtml(state)}</button>`).join('')}
              </div>
            ` : ''}
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
                    <div class="subtask-row">
                      ${canEdit ? `<button class="ghost-btn" data-action="toggle-subtask" data-subtask-id="${s.id}" data-completed="${s.completed}">${s.completed ? '[x]' : '[ ]'}</button>` : '<span class="subtask-toggle">' + (s.completed ? '[x]' : '[ ]') + '</span>'}
                      <span>${escapeHtml(s.name)}</span>
                      ${canEdit ? `
                        <div class="subtask-actions">
                          <button class="ghost-btn" data-action="edit-subtask" data-subtask-id="${s.id}">Edit</button>
                          <button class="ghost-btn" data-action="comment-subtask" data-subtask-id="${s.id}">Comment</button>
                        </div>
                      ` : ''}
                    </div>
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

    const renderSettings = () => {
    const project = uiState.project || {};
    const members = project.members || [];
    const isOwner = uiState.isOwner === true;
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
                <input data-field="project-name" type="text" value="${escapeHtml(project.name || '')}" ${isOwner ? '' : 'disabled'} />
              </div>
              <div class="field-group">
                <label>Statuses (comma separated)</label>
                <textarea data-field="project-statuses" ${isOwner ? '' : 'disabled'}>${escapeHtml((project.statuses || []).join(', '))}</textarea>
              </div>
              <div class="field-group">
                <label>Ticket Types (comma separated)</label>
                <textarea data-field="project-types" ${isOwner ? '' : 'disabled'}>${escapeHtml((project.ticketTypes || []).join(', '))}</textarea>
              </div>
              <div class="field-group">
                <label>Team Members</label>
                <div class="list">
                  ${members.length ? members.map((m) => `
                    <div class="list-item team-row">
                      <span>${escapeHtml(m.name)}</span>
                      ${isOwner ? `<button class="ghost-btn" data-action="remove-member" data-member-name="${escapeHtml(m.name)}">Remove</button>` : ''}
                    </div>
                  `).join('') : '<div class="list-item">No members yet.</div>'}
                </div>
              </div>
              ${isOwner ? `
                <div class="field-group">
                  <label>Add Member (online player name)</label>
                  <div class="inline-row">
                    <input data-field="member-name" type="text" placeholder="Player name" />
                    <button class="btn secondary" data-action="add-member">Add</button>
                  </div>
                </div>
              ` : '<div class="notice">Only the project owner can change settings or manage team members.</div>'}
              <div class="modal-actions">
                <button class="btn secondary" data-action="back">Close</button>
                ${isOwner ? '<button class="btn" data-action="save-settings">Save Settings</button>' : ''}
              </div>
            </div>
          </div>
        </div>
      </div>
    `;
  };

  const renderModal = () => {
    if (!activeModal) return '';
    const project = uiState.project || {};
    const types = project.ticketTypes || ['Task'];
    const statuses = project.statuses || ['To Do'];
    const priorities = ['LOW', 'MEDIUM', 'HIGH'];

    if (activeModal.type === 'new-ticket') {
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
            <div class="field-group">
              <label>Subtasks (one per line, optional description after |)</label>
              <textarea data-field="subtasks" placeholder="Task name | description"></textarea>
            </div>
            <div class="modal-actions">
              <button type="button" class="btn secondary" data-action="close-modal">Cancel</button>
              <button type="submit" class="btn">Create</button>
            </div>
          </form>
        </div>
      `;
    }

    if (activeModal.type === 'edit-ticket') {
      const ticket = activeModal.ticket || {};
      return `
        <div class="modal-backdrop" data-role="modal-backdrop">
          <form class="modal" data-role="edit-ticket-form" data-ticket-id="${ticket.id}">
            <div class="modal-header">
              <h3>Edit Ticket</h3>
              <button type="button" class="modal-close" data-action="close-modal">x</button>
            </div>
            <div class="field-group">
              <label>Title</label>
              <input data-field="title" type="text" value="${escapeHtml(ticket.title || '')}" required />
            </div>
            <div class="field-group">
              <label>Description</label>
              <textarea data-field="description">${escapeHtml(ticket.description || '')}</textarea>
            </div>
            <div class="field-group">
              <label>Type</label>
              <select data-field="type">
                ${types.map((type) => `<option value="${escapeHtml(type)}" ${type === ticket.type ? 'selected' : ''}>${escapeHtml(type)}</option>`).join('')}
              </select>
            </div>
            <div class="field-group">
              <label>Status</label>
              <select data-field="state">
                ${statuses.map((state) => `<option value="${escapeHtml(state)}" ${state === ticket.state ? 'selected' : ''}>${escapeHtml(state)}</option>`).join('')}
              </select>
            </div>
            <div class="field-group">
              <label>Priority</label>
              <select data-field="priority">
                ${priorities.map((priority) => `<option value="${priority}" ${priority === ticket.priority ? 'selected' : ''}>${priority}</option>`).join('')}
              </select>
            </div>
            <div class="field-group">
              <label>Assignee</label>
              <input data-field="assignee" type="text" value="${escapeHtml(ticket.assigneeName || '')}" />
            </div>
            <div class="modal-actions">
              <button type="button" class="btn secondary" data-action="close-modal">Cancel</button>
              <button type="submit" class="btn">Save</button>
            </div>
          </form>
        </div>
      `;
    }

    if (activeModal.type === 'ticket-comment') {
      return `
        <div class="modal-backdrop" data-role="modal-backdrop">
          <form class="modal" data-role="ticket-comment-form" data-ticket-id="${activeModal.ticketId}">
            <div class="modal-header">
              <h3>Add Comment</h3>
              <button type="button" class="modal-close" data-action="close-modal">x</button>
            </div>
            <div class="field-group">
              <label>Comment</label>
              <textarea data-field="message"></textarea>
            </div>
            <div class="modal-actions">
              <button type="button" class="btn secondary" data-action="close-modal">Cancel</button>
              <button type="submit" class="btn">Send</button>
            </div>
          </form>
        </div>
      `;
    }

    if (activeModal.type === 'subtask-new') {
      return `
        <div class="modal-backdrop" data-role="modal-backdrop">
          <form class="modal" data-role="subtask-form" data-ticket-id="${activeModal.ticketId}">
            <div class="modal-header">
              <h3>New Subtask</h3>
              <button type="button" class="modal-close" data-action="close-modal">x</button>
            </div>
            <div class="field-group">
              <label>Name</label>
              <input data-field="name" type="text" required />
            </div>
            <div class="field-group">
              <label>Description</label>
              <textarea data-field="description"></textarea>
            </div>
            <div class="modal-actions">
              <button type="button" class="btn secondary" data-action="close-modal">Cancel</button>
              <button type="submit" class="btn">Add</button>
            </div>
          </form>
        </div>
      `;
    }

    if (activeModal.type === 'subtask-edit') {
      const subtask = activeModal.subtask || {};
      return `
        <div class="modal-backdrop" data-role="modal-backdrop">
          <form class="modal" data-role="subtask-edit-form" data-ticket-id="${activeModal.ticketId}" data-subtask-id="${subtask.id}">
            <div class="modal-header">
              <h3>Edit Subtask</h3>
              <button type="button" class="modal-close" data-action="close-modal">x</button>
            </div>
            <div class="field-group">
              <label>Name</label>
              <input data-field="name" type="text" value="${escapeHtml(subtask.name || '')}" required />
            </div>
            <div class="field-group">
              <label>Description</label>
              <textarea data-field="description">${escapeHtml(subtask.description || '')}</textarea>
            </div>
            <div class="modal-actions">
              <button type="button" class="btn secondary" data-action="close-modal">Cancel</button>
              <button type="submit" class="btn">Save</button>
            </div>
          </form>
        </div>
      `;
    }

    if (activeModal.type === 'subtask-comment') {
      return `
        <div class="modal-backdrop" data-role="modal-backdrop">
          <form class="modal" data-role="subtask-comment-form" data-ticket-id="${activeModal.ticketId}" data-subtask-id="${activeModal.subtaskId}">
            <div class="modal-header">
              <h3>Subtask Comment</h3>
              <button type="button" class="modal-close" data-action="close-modal">x</button>
            </div>
            <div class="field-group">
              <label>Comment</label>
              <textarea data-field="message"></textarea>
            </div>
            <div class="modal-actions">
              <button type="button" class="btn secondary" data-action="close-modal">Cancel</button>
              <button type="submit" class="btn">Send</button>
            </div>
          </form>
        </div>
      `;
    }

    return '';
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
      case 'settings':
        markup = renderSettings();
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
    if (search && pendingSearchFocus) {
      search.focus();
      if (pendingSearchSelection) {
        try {
          search.setSelectionRange(pendingSearchSelection[0], pendingSearchSelection[1]);
        } catch (e) {
          // ignore
        }
      }
      pendingSearchFocus = false;
      pendingSearchSelection = null;
    }
  };

  app.addEventListener('input', (event) => {
    if (event.target && event.target.matches('[data-role="search"]')) {
      searchTerm = event.target.value || '';
      pendingSearchFocus = true;
      pendingSearchSelection = [event.target.selectionStart || 0, event.target.selectionEnd || 0];
      render();
    }
  });

  app.addEventListener('click', (event) => {
    const actionEl = event.target.closest('[data-action]');
    if (!actionEl) return;
    const action = actionEl.dataset.action;
    const ticket = uiState.ticket || {};

    if (action === 'new-ticket') {
      openModal('new-ticket');
      return;
    }
    if (action === 'open-settings') {
      setScreen('settings');
      return;
    }
    if (action === 'back') {
      setScreen(null);
      return;
    }
    if (action === 'close-modal') {
      closeModal();
      return;
    }
    if (action === 'edit-ticket') {
      openModal('edit-ticket', { ticket });
      return;
    }
    if (action === 'add-ticket-comment') {
      openModal('ticket-comment', { ticketId: ticket.id });
      return;
    }
    if (action === 'add-subtask') {
      openModal('subtask-new', { ticketId: ticket.id });
      return;
    }
    if (action === 'edit-subtask') {
      const subtaskId = actionEl.dataset.subtaskId;
      const subtask = (ticket.subtasks || []).find((s) => s.id === subtaskId);
      if (subtask) {
        openModal('subtask-edit', { ticketId: ticket.id, subtask });
      }
      return;
    }
    if (action === 'comment-subtask') {
      const subtaskId = actionEl.dataset.subtaskId;
      openModal('subtask-comment', { ticketId: ticket.id, subtaskId });
      return;
    }
    if (action === 'toggle-subtask') {
      const subtaskId = actionEl.dataset.subtaskId;
      const completed = actionEl.dataset.completed === 'true';
      sendAction('toggle-subtask', { ticketId: ticket.id, subtaskId, completed: !completed });
      return;
    }
    if (action === 'open-ticket') {
      const ticketId = actionEl.dataset.ticketId;
      if (ticketId) {
        sendAction('open-ticket', { ticketId });
      }
      return;
    }
    if (action === 'set-status') {
      const status = actionEl.dataset.status || '';
      const ticket = uiState.ticket || {};
      if (status && ticket.id) {
        sendAction('update-ticket', { ticketId: ticket.id, state: status });
      }
      return;
    }
    if (action === 'add-member') {
      const nameInput = app.querySelector('[data-field="member-name"]');
      const memberName = nameInput ? nameInput.value.trim() : '';
      if (memberName) {
        sendAction('add-member', { memberName });
        nameInput.value = '';
      }
      return;
    }
    if (action === 'remove-member') {
      const memberName = actionEl.dataset.memberName;
      if (memberName) {
        sendAction('remove-member', { memberName });
      }
      return;
    }
    if (action === 'save-settings') {
      const name = (app.querySelector('[data-field="project-name"]') || {}).value || '';
      const statusesRaw = (app.querySelector('[data-field="project-statuses"]') || {}).value || '';
      const typesRaw = (app.querySelector('[data-field="project-types"]') || {}).value || '';
      const statuses = statusesRaw.split(',').map((s) => s.trim()).filter(Boolean);
      const ticketTypes = typesRaw.split(',').map((s) => s.trim()).filter(Boolean);
      sendAction('update-project-settings', { name, statuses, ticketTypes });
      setScreen(null);
      return;
    }
  });

  app.addEventListener('submit', (event) => {
    const form = event.target;
    if (!(form instanceof HTMLFormElement)) return;

    if (form.matches('[data-role="new-ticket-form"]')) {
      event.preventDefault();
      const title = form.querySelector('[data-field="title"]').value.trim();
      const description = form.querySelector('[data-field="description"]').value.trim();
      const type = form.querySelector('[data-field="type"]').value;
      const state = form.querySelector('[data-field="state"]').value;
      const priority = form.querySelector('[data-field="priority"]').value;
      const assigneeName = form.querySelector('[data-field="assignee"]').value.trim();
      const subtasksRaw = form.querySelector('[data-field="subtasks"]').value || '';
      const subtasks = subtasksRaw.split('\n').map((line) => line.trim()).filter(Boolean).map((line) => {
        const parts = line.split('|');
        return { name: parts[0].trim(), description: (parts[1] || '').trim() };
      }).filter((entry) => entry.name);
      if (!title) return;
      sendAction('create-ticket', { title, description, type, state, priority, assigneeName, subtasks });
      closeModal();
      return;
    }

    if (form.matches('[data-role="edit-ticket-form"]')) {
      event.preventDefault();
      const ticketId = form.dataset.ticketId;
      const title = form.querySelector('[data-field="title"]').value.trim();
      const description = form.querySelector('[data-field="description"]').value.trim();
      const type = form.querySelector('[data-field="type"]').value;
      const state = form.querySelector('[data-field="state"]').value;
      const priority = form.querySelector('[data-field="priority"]').value;
      const assigneeName = form.querySelector('[data-field="assignee"]').value.trim();
      if (!title) return;
      sendAction('update-ticket', { ticketId, title, description, type, state, priority, assigneeName });
      closeModal();
      return;
    }

    if (form.matches('[data-role="ticket-comment-form"]')) {
      event.preventDefault();
      const ticketId = form.dataset.ticketId;
      const message = form.querySelector('[data-field="message"]').value.trim();
      if (!message) return;
      sendAction('add-ticket-comment', { ticketId, message });
      closeModal();
      return;
    }

    if (form.matches('[data-role="subtask-form"]')) {
      event.preventDefault();
      const ticketId = form.dataset.ticketId;
      const name = form.querySelector('[data-field="name"]').value.trim();
      const description = form.querySelector('[data-field="description"]').value.trim();
      if (!name) return;
      sendAction('add-subtask', { ticketId, name, description });
      closeModal();
      return;
    }

    if (form.matches('[data-role="subtask-edit-form"]')) {
      event.preventDefault();
      const ticketId = form.dataset.ticketId;
      const subtaskId = form.dataset.subtaskId;
      const name = form.querySelector('[data-field="name"]').value.trim();
      const description = form.querySelector('[data-field="description"]').value.trim();
      if (!name) return;
      sendAction('update-subtask', { ticketId, subtaskId, name, description });
      closeModal();
      return;
    }

    if (form.matches('[data-role="subtask-comment-form"]')) {
      event.preventDefault();
      const ticketId = form.dataset.ticketId;
      const subtaskId = form.dataset.subtaskId;
      const message = form.querySelector('[data-field="message"]').value.trim();
      if (!message) return;
      sendAction('add-subtask-comment', { ticketId, subtaskId, message });
      closeModal();
      return;
    }
  });

  window.MossuraUI = {
    setState(next) {
      if (next && typeof next === 'object') {
        uiState = next;
      }
      render();
    }
  };

  render();
})();
