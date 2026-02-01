(() => {
  const app = document.getElementById('app');
  let uiState = window.__MOSSURA_STATE__ || { screen: 'project', loading: true };
  let viewMode = 'board';
  let searchTerm = '';
  let selectedSprintId = 'all';
  let selectedLabels = [];
  let screenOverride = null;
  let activeModal = null;
  let pendingSearchFocus = false;
  let pendingSearchSelection = null;
  let projectsList = [];
  let authStatus = { authenticated: false };
  let authRequestToken = null;

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
        onSuccess: () => { },
        onFailure: () => { }
      });
    } else {
      console.log('Web Action:', action, payload);
    }
  };

  const isWebMode = () => !window.cefQuery;

  const getProjectIdFromUrl = () => {
    const hash = window.location.hash.replace('#', '').trim();
    const parts = hash.split('/').filter(Boolean);
    if (parts.length >= 2 && parts[0] === 'project') return parts[1];
    const params = new URLSearchParams(window.location.search);
    return params.get('projectId');
  };

  const refreshState = async () => {
    if (!isWebMode()) return;
    const projectId = getProjectIdFromUrl();
    if (!projectId) return;

    try {
      const resp = await fetch(`/api/project/${projectId}`);
      if (resp.ok) {
        uiState = await resp.json();
        render();
      }
    } catch (e) {
      console.error('Fetch failed', e);
    }
  };

  const checkAuth = async () => {
    if (!isWebMode()) return;
    try {
      const resp = await fetch('/api/auth/status');
      if (resp.ok) {
        const status = await resp.json();
        const wasAuth = authStatus.authenticated;
        authStatus = status;
        if (authStatus.authenticated) {
          if (!wasAuth) {
            closeModal();
            fetchProjects();
          }
        } else if (!activeModal || activeModal.type !== 'login') {
          requestAuthToken();
        }
      }
    } catch (e) {
      console.error('Auth check failed', e);
    }
  };

  const requestAuthToken = async () => {
    try {
      const resp = await fetch('/api/auth/request');
      if (resp.ok) {
        const data = await resp.json();
        authRequestToken = data.token;
        openModal('login');
      }
    } catch (e) { }
  };

  const fetchProjects = async () => {
    try {
      const resp = await fetch('/api/projects');
      if (resp.ok) {
        projectsList = await resp.json();
        render();
      }
    } catch (e) { }
  };

  if (isWebMode()) {
    setInterval(refreshState, 5000);
    setInterval(checkAuth, 3000);
    refreshState();
    checkAuth();
  }

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
          ${isWebMode() && authStatus.authenticated ? `<button class="btn ghost-btn" data-action="logout">Logout</button>` : ''}
          ${isWebMode() && !authStatus.authenticated ? `<button class="btn" data-action="open-login">Login</button>` : ''}
          <button class="btn" data-action="new-ticket" ${canEdit ? '' : 'disabled'}>New Ticket</button>
          <button class="btn secondary" data-action="open-settings">Settings</button>
        </div>
      </div>
    `;
  };

  const renderSidebar = () => {
    const project = uiState.project || {};
    const members = project.members || [];
    const sprints = project.sprints || [];
    const activeSprint = sprints.find(s => s.status === 'ACTIVE');

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
        ${isWebMode() && projectsList.length > 0 ? `
          <div class="card">
            <h3>My Projects</h3>
            <div class="list">
              ${projectsList.map(p => `
                <div class="list-item project-switcher ${p.id === project.id ? 'active' : ''}" data-action="switch-project" data-project-id="${p.id}">
                  <span>${escapeHtml(p.name)}</span>
                  <span class="tag xsmall">${escapeHtml(p.role)}</span>
                </div>
              `).join('')}
            </div>
          </div>
        ` : ''}
        ${activeSprint ? `
          <div class="card sprint-card">
            <h3>Active Sprint</h3>
            <strong>${escapeHtml(activeSprint.name)}</strong>
            <small>${new Date(activeSprint.endTime).toLocaleDateString()}</small>
          </div>
        ` : ''}
        <div class="card">
          <h3>Sprints</h3>
          <div class="list">
            ${sprints.length ? sprints.map(s => `
              <div class="list-item sprint-item ${s.status === 'ACTIVE' ? 'active' : ''}">
                <span>${escapeHtml(s.name)}</span>
                <span class="pill small">${escapeHtml(s.status)}</span>
              </div>
            `).join('') : '<div class="list-item">No sprints yet</div>'}
          </div>
        </div>
        <div class="card">
          <h3>Team</h3>
          <div class="list">
            ${(members.length ? members : [{ name: 'No members yet' }])
        .map((m) => `
                <div class="list-item member-item">
                  <span>${escapeHtml(m.name || '')}</span>
                  ${m.permission ? `<span class="tag small">${escapeHtml(m.permission)}</span>` : ''}
                </div>
              `)
        .join('')}
          </div>
        </div>
      </aside>
    `;
  };

  const filterTickets = (tickets) => {
    let filtered = tickets;

    // Sprint filter
    if (selectedSprintId !== 'all') {
      if (selectedSprintId === 'none') {
        filtered = filtered.filter(t => !t.sprintId);
      } else {
        filtered = filtered.filter(t => t.sprintId === selectedSprintId);
      }
    }

    // Label filter
    if (selectedLabels.length > 0) {
      filtered = filtered.filter(t => {
        const tLabels = t.labels || [];
        return selectedLabels.every(l => tLabels.includes(l));
      });
    }

    // Search term
    if (!searchTerm.trim()) return filtered;
    const needle = searchTerm.trim().toLowerCase();
    return filtered.filter((ticket) => {
      const haystack = `${ticket.number} ${ticket.title} ${ticket.description || ''} ${(ticket.labels || []).join(' ')}`.toLowerCase();
      return haystack.includes(needle);
    });
  };

  const renderBoard = (tickets, statuses) => {
    const byStatus = {};
    statuses.forEach((s) => (byStatus[s] = []));
    tickets.forEach((ticket) => {
      const state = ticket.state || statuses[0];
      if (!byStatus[state]) byStatus[state] = [];
      byStatus[state].push(ticket);
    });

    return `
      <div class="board">
        ${statuses.map((status) => {
      const stack = byStatus[status];
      return `
            <div class="board-column" data-role="dropzone" data-status="${escapeHtml(status)}">
              <div class="column-title">${escapeHtml(status)}</div>
              ${stack.length
          ? stack.map((ticket) => `
                    <div class="ticket-card" draggable="true" data-role="draggable-ticket" data-ticket-id="${ticket.id}" data-action="open-ticket">
                      <strong>#${ticket.number} ${escapeHtml(ticket.title)}</strong>
                      <small>${escapeHtml(ticket.description || 'No description')}</small>
                      <div class="labels">
                        ${(ticket.labels || []).map(l => `<span class="tag xsmall">${escapeHtml(l)}</span>`).join('')}
                      </div>
                      <div class="meta">
                        <span>${escapeHtml(ticket.type || 'Task')}</span>
                        <span>${escapeHtml(ticket.assigneeName || 'Unassigned')}</span>
                      </div>
                    </div>
                  `).join('')
          : '<div class="ticket-card empty-zone"><small>Drop items here</small></div>'}
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
            <th>Labels</th>
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
              <td>${(ticket.labels || []).map(l => `<span class="tag xsmall">${escapeHtml(l)}</span>`).join(' ')}</td>
            </tr>
          `).join('')}
        </tbody>
      </table>
    `;
  };

  const renderProject = () => {
    const project = uiState.project || {};
    const allLabels = [...new Set((project.tickets || []).flatMap(t => t.labels || []))].sort();
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
                <select class="filter-select" data-role="sprint-filter">
                  <option value="all" ${selectedSprintId === 'all' ? 'selected' : ''}>All Sprints</option>
                  <option value="none" ${selectedSprintId === 'none' ? 'selected' : ''}>No Sprint</option>
                  ${(project.sprints || []).map(s => `<option value="${s.id}" ${selectedSprintId === s.id ? 'selected' : ''}>${escapeHtml(s.name)}</option>`).join('')}
                </select>
                <input class="search-input" data-role="search" type="search" placeholder="Search tickets" value="${escapeHtml(searchTerm)}" />
                <div class="switcher">
                  <button data-view="board" class="${viewMode === 'board' ? 'active' : ''}">Board</button>
                  <button data-view="list" class="${viewMode === 'list' ? 'active' : ''}">List</button>
                </div>
              </div>
            </div>
            ${allLabels.length ? `
              <div class="filter-row">
                <span>Labels:</span>
                ${allLabels.map(l => `
                  <button class="tag small ${selectedLabels.includes(l) ? 'active' : ''}" data-role="label-filter" data-label="${escapeHtml(l)}">${escapeHtml(l)}</button>
                `).join('')}
              </div>
            ` : ''}
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
    const history = ticket.history || [];
    const statuses = project.statuses || [];
    const canEdit = uiState.canEdit !== false;
    const isMember = uiState.isMember !== false;

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
                ${isMember ? '<button class="btn" data-action="add-ticket-comment">Add Comment</button>' : ''}
                ${canEdit ? '<button class="btn" data-action="add-subtask">Add Subtask</button>' : ''}
              </div>
            </div>
            ${isMember && statuses.length ? `
              <div class="status-row">
                <span>Move to:</span>
                ${statuses.map((state) => `<button class="ghost-btn ${ticket.state === state ? 'active' : ''}" data-action="set-status" data-status="${escapeHtml(state)}">${escapeHtml(state)}</button>`).join('')}
              </div>
            ` : ''}
            <div class="detail-grid">
              <div class="detail-card">
                <h3>Summary</h3>
                <p>${escapeHtml(ticket.description || 'No description provided.')}</p>
                <div class="labels">
                  ${(ticket.labels || []).map(l => `<span class="tag">${escapeHtml(l)}</span>`).join('')}
                </div>
                <div class="list">
                  <div class="list-item">Type: ${escapeHtml(ticket.type || 'Task')}</div>
                  <div class="list-item">State: ${escapeHtml(ticket.state || '-')}</div>
                  <div class="list-item">Priority: ${escapeHtml(ticket.priority || '-')}</div>
                  <div class="list-item">Assignee: ${escapeHtml(ticket.assigneeName || 'Unassigned')}</div>
                  <div class="list-item">Sprint: ${escapeHtml((project.sprints || []).find(s => s.id === ticket.sprintId)?.name || 'None')}</div>
                </div>
              </div>
              <div class="detail-card">
                <h3>Subtasks</h3>
                <div class="list">
                  ${subtasks.length ? subtasks.map((s) => `
                    <div class="subtask-row">
                      ${isMember ? `<button class="ghost-btn" data-action="toggle-subtask" data-subtask-id="${s.id}" data-completed="${s.completed}">${s.completed ? '[x]' : '[ ]'}</button>` : '<span class="subtask-toggle">' + (s.completed ? '[x]' : '[ ]') + '</span>'}
                      <span>${escapeHtml(s.name)}</span>
                      ${canEdit ? `
                        <div class="subtask-actions">
                          <button class="ghost-btn" data-action="edit-subtask" data-subtask-id="${s.id}">Edit</button>
                        </div>
                      ` : ''}
                      ${isMember ? `<button class="ghost-btn" data-action="comment-subtask" data-subtask-id="${s.id}">Comment</button>` : ''}
                    </div>
                  `).join('') : '<div class="list-item">No subtasks yet.</div>'}
                </div>
              </div>
            </div>
            <div class="detail-grid">
              <div class="detail-card">
                <h3>Comments</h3>
                <div class="list">
                  ${comments.length ? comments.map((c) => `
                    <div class="list-item">
                      <strong>${escapeHtml(c.authorName)}</strong> <small>${new Date(c.createdAt).toLocaleString()}</small>
                      <p>${escapeHtml(c.message)}</p>
                    </div>
                  `).join('') : '<div class="list-item">No comments yet.</div>'}
                </div>
              </div>
              <div class="detail-card">
                <details>
                  <summary><h3>Activity History</h3></summary>
                  <div class="list history-list">
                    ${history.length ? history.map((e) => `
                      <div class="list-item history-item">
                        <small>${new Date(e.timestamp).toLocaleString()}</small><br/>
                        <strong>${escapeHtml(e.actorName)}</strong> changed <strong>${escapeHtml(e.field)}</strong>
                        ${e.before ? `from <em>${escapeHtml(e.before)}</em>` : ''}
                        to <em>${escapeHtml(e.after)}</em>
                      </div>
                    `).join('') : '<div class="list-item">No history recorded.</div>'}
                  </div>
                </details>
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
    const isAdmin = uiState.isAdmin === true;
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
              <h3>Project Information</h3>
              <div class="field-group">
                <label>Project Name</label>
                <input data-field="project-name" type="text" value="${escapeHtml(project.name || '')}" ${isAdmin ? '' : 'disabled'} />
              </div>
              <div class="field-group">
                <label>Description</label>
                <textarea data-field="project-description" ${isAdmin ? '' : 'disabled'}>${escapeHtml(project.description || '')}</textarea>
              </div>
              <div class="field-group">
                <label>Ticket Prefix (min 2 chars)</label>
                <input data-field="project-prefix" type="text" value="${escapeHtml(project.ticketPrefix || '')}" ${isAdmin ? '' : 'disabled'} />
              </div>
              <div class="field-group">
                <label>Statuses (comma separated)</label>
                <textarea data-field="project-statuses" ${isAdmin ? '' : 'disabled'}>${escapeHtml((project.statuses || []).join(', '))}</textarea>
              </div>
              <div class="field-group">
                <label>Ticket Types (comma separated)</label>
                <textarea data-field="project-types" ${isAdmin ? '' : 'disabled'}>${escapeHtml((project.ticketTypes || []).join(', '))}</textarea>
              </div>
              <div class="field-group">
                <label>Team Members</label>
                <div class="list">
                  ${members.length ? members.map((m) => `
                    <div class="list-item team-row">
                      <div class="member-info">
                        <span>${escapeHtml(m.name)}</span>
                        <span class="tag small">${escapeHtml(m.permission || '')}</span>
                      </div>
                      ${isOwner ? `
                        <div class="member-actions">
                          <select class="small-select" data-action="change-permission" data-member-name="${escapeHtml(m.name)}">
                            <option value="VIEWER" ${m.permission === 'VIEWER' ? 'selected' : ''}>VIEWER</option>
                            <option value="EDITOR" ${m.permission === 'EDITOR' ? 'selected' : ''}>EDITOR</option>
                            <option value="ADMIN" ${m.permission === 'ADMIN' ? 'selected' : ''}>ADMIN</option>
                          </select>
                          <button class="ghost-btn danger" data-action="remove-member" data-member-name="${escapeHtml(m.name)}">Remove</button>
                        </div>
                      ` : ''}
                    </div>
                  `).join('') : '<div class="list-item">No members yet.</div>'}
                </div>
              </div>
              <div class="field-group">
                <label>Sprints</label>
                <div class="list">
                  ${(project.sprints || []).length ? project.sprints.map(s => `
                    <div class="list-item team-row">
                      <span>${escapeHtml(s.name)} [${escapeHtml(s.status)}]</span>
                      ${isAdmin ? `<button class="ghost-btn" data-action="edit-sprint" data-sprint-id="${s.id}">Edit</button>` : ''}
                    </div>
                  `).join('') : '<div class="list-item">No sprints.</div>'}
                </div>
                ${isAdmin ? `<button class="btn secondary small" data-action="add-sprint">Add Sprint</button>` : ''}
              </div>
              ${isOwner ? `
                <div class="field-group">
                  <label>Add Member (player name)</label>
                  <div class="inline-row">
                    <input data-field="member-name" type="text" placeholder="Player name" />
                    <button class="btn secondary" data-action="add-member">Add</button>
                  </div>
                </div>
              ` : ''}
              <div class="modal-actions">
                <button class="btn secondary" data-action="back">Close</button>
                ${isAdmin ? '<button class="btn" data-action="save-settings">Save Settings</button>' : ''}
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
              <label>Labels (comma separated)</label>
              <input data-field="labels" type="text" placeholder="bug, ui, backend" />
            </div>
            <div class="field-group">
              <label>Assignee</label>
              <input data-field="assignee" type="text" placeholder="Optional" />
            </div>
            <div class="field-group">
              <label>Sprint</label>
              <select data-field="sprintId">
                <option value="">None</option>
                ${(project.sprints || []).map(s => `<option value="${s.id}">${escapeHtml(s.name)} [${escapeHtml(s.status)}]</option>`).join('')}
              </select>
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
              <label>Labels (comma separated)</label>
              <input data-field="labels" type="text" value="${escapeHtml((ticket.labels || []).join(', '))}" />
            </div>
            <div class="field-group">
              <label>Assignee</label>
              <input data-field="assignee" type="text" value="${escapeHtml(ticket.assigneeName || '')}" />
            </div>
            <div class="field-group">
              <label>Sprint</label>
              <select data-field="sprintId">
                <option value="" ${!ticket.sprintId ? 'selected' : ''}>None</option>
                ${(project.sprints || []).map(s => `<option value="${s.id}" ${s.id === ticket.sprintId ? 'selected' : ''}>${escapeHtml(s.name)} [${escapeHtml(s.status)}]</option>`).join('')}
              </select>
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

    if (activeModal.type === 'sprint-new') {
      return `
        <div class="modal-backdrop" data-role="modal-backdrop">
          <form class="modal" data-role="sprint-form">
            <div class="modal-header">
              <h3>New Sprint</h3>
              <button type="button" class="modal-close" data-action="close-modal">x</button>
            </div>
            <div class="field-group">
              <label>Sprint Name</label>
              <input data-field="name" type="text" placeholder="e.g. Iteration 1" required />
            </div>
            <div class="field-group">
              <label>Duration (weeks)</label>
              <input data-field="weeks" type="number" value="2" min="1" max="12" />
            </div>
            <div class="modal-actions">
              <button type="button" class="btn secondary" data-action="close-modal">Cancel</button>
              <button type="submit" class="btn">Create</button>
            </div>
          </form>
        </div>
      `;
    }

    if (activeModal.type === 'sprint-edit') {
      const sprint = activeModal.sprint || {};
      return `
        <div class="modal-backdrop" data-role="modal-backdrop">
          <form class="modal" data-role="sprint-edit-form" data-sprint-id="${sprint.id}">
            <div class="modal-header">
              <h3>Edit Sprint: ${escapeHtml(sprint.name)}</h3>
              <button type="button" class="modal-close" data-action="close-modal">x</button>
            </div>
            <div class="field-group">
              <label>Name</label>
              <input data-field="name" type="text" value="${escapeHtml(sprint.name)}" required />
            </div>
            <div class="field-group">
              <label>Status</label>
              <select data-field="status">
                <option value="PLANNED" ${sprint.status === 'PLANNED' ? 'selected' : ''}>PLANNED</option>
                <option value="ACTIVE" ${sprint.status === 'ACTIVE' ? 'selected' : ''}>ACTIVE</option>
                <option value="COMPLETED" ${sprint.status === 'COMPLETED' ? 'selected' : ''}>COMPLETED</option>
              </select>
            </div>
            <div class="modal-actions">
              <button type="button" class="btn secondary" data-action="close-modal">Cancel</button>
              <button type="submit" class="btn">Save</button>
            </div>
          </form>
      `;
    }

    if (activeModal.type === 'login') {
      return `
        <div class="modal-backdrop">
          <div class="modal auth-modal">
            <div class="modal-header">
              <h3>Web Authentication</h3>
              <button type="button" class="modal-close" data-action="close-modal">x</button>
            </div>
            <div class="auth-content">
              <p>To access your projects, please verify your identity in-game.</p>
              <div class="token-display">
                <small>Enter this command in Minecraft:</small>
                <code>/mossura verify ${authRequestToken}</code>
              </div>
              <p class="notice">Waiting for verification...</p>
            </div>
            <div class="modal-actions">
              <button class="btn secondary" data-action="close-modal">Dismiss</button>
            </div>
          </div>
        </div>
      `;
    }

    return '';
  };

  const initDragAndDrop = () => {
    const draggables = app.querySelectorAll('[data-role="draggable-ticket"]');
    const zones = app.querySelectorAll('[data-role="dropzone"]');

    draggables.forEach(el => {
      el.addEventListener('dragstart', (e) => {
        e.dataTransfer.setData('text/plain', el.dataset.ticketId);
        el.classList.add('dragging');
      });
      el.addEventListener('dragend', () => el.classList.remove('dragging'));
    });

    zones.forEach(zone => {
      zone.addEventListener('dragover', (e) => {
        e.preventDefault();
        zone.classList.add('drag-over');
      });
      zone.addEventListener('dragleave', () => zone.classList.remove('drag-over'));
      zone.addEventListener('drop', (e) => {
        e.preventDefault();
        zone.classList.remove('drag-over');
        const ticketId = e.dataTransfer.getData('text/plain');
        const status = zone.dataset.status;
        if (ticketId && status) {
          sendAction('update-ticket', { ticketId, state: status });
        }
      });
    });
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
    initDragAndDrop();

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

    const sprintFilter = app.querySelector('[data-role="sprint-filter"]');
    if (sprintFilter) {
      sprintFilter.addEventListener('change', (e) => {
        selectedSprintId = e.target.value;
        render();
      });
    }

    const labelFilters = app.querySelectorAll('[data-role="label-filter"]');
    labelFilters.forEach(btn => {
      btn.addEventListener('click', () => {
        const label = btn.dataset.label;
        if (selectedLabels.includes(label)) {
          selectedLabels = selectedLabels.filter(l => l !== label);
        } else {
          selectedLabels.push(label);
        }
        render();
      });
    });
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
    const project = uiState.project || {};

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
    if (action === 'change-permission') {
      const memberName = actionEl.dataset.memberName;
      const level = event.target.value;
      if (memberName && level) {
        sendAction('add-member', { memberName, level });
      }
      return;
    }
    if (action === 'add-sprint') {
      openModal('sprint-new');
      return;
    }
    if (action === 'edit-sprint') {
      const sprintId = actionEl.dataset.sprintId;
      const sprint = (project.sprints || []).find(s => s.id === sprintId);
      if (sprint) {
        openModal('sprint-edit', { sprint });
      }
      return;
    }
    if (action === 'save-settings') {
      const name = (app.querySelector('[data-field="project-name"]') || {}).value || '';
      const description = (app.querySelector('[data-field="project-description"]') || {}).value || '';
      const ticketPrefix = (app.querySelector('[data-field="project-prefix"]') || {}).value || '';
      const statusesRaw = (app.querySelector('[data-field="project-statuses"]') || {}).value || '';
      const typesRaw = (app.querySelector('[data-field="project-types"]') || {}).value || '';
      const statuses = statusesRaw.split(',').map((s) => s.trim()).filter(Boolean);
      const ticketTypes = typesRaw.split(',').map((s) => s.trim()).filter(Boolean);
      sendAction('update-project-settings', { name, description, ticketPrefix, statuses, ticketTypes });
      setScreen(null);
      return;
    }
    if (action === 'logout') {
      fetch('/api/auth/logout').then(() => {
        authStatus = { authenticated: false };
        projectsList = [];
        checkAuth();
      });
      return;
    }
    if (action === 'open-login') {
      requestAuthToken();
      return;
    }
    if (action === 'switch-project') {
      const projectId = actionEl.dataset.projectId;
      if (projectId) {
        window.location.hash = `#project/${projectId}`;
        refreshState();
      }
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
      const labelsRaw = form.querySelector('[data-field="labels"]').value || '';
      const labels = labelsRaw.split(',').map(s => s.trim()).filter(Boolean);
      const assigneeName = form.querySelector('[data-field="assignee"]').value.trim();
      const subtasksRaw = form.querySelector('[data-field="subtasks"]').value || '';
      const subtasks = subtasksRaw.split('\n').map((line) => line.trim()).filter(Boolean).map((line) => {
        const parts = line.split('|');
        return { name: parts[0].trim(), description: (parts[1] || '').trim() };
      }).filter((entry) => entry.name);
      if (!title) return;
      const sprintId = form.querySelector('[data-field="sprintId"]').value;
      sendAction('create-ticket', { title, description, type, state, priority, labels, assigneeName, subtasks, sprintId });
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
      const labelsRaw = form.querySelector('[data-field="labels"]').value || '';
      const labels = labelsRaw.split(',').map(s => s.trim()).filter(Boolean);
      const assigneeName = form.querySelector('[data-field="assignee"]').value.trim();
      if (!title) return;
      const sprintId = form.querySelector('[data-field="sprintId"]').value;
      sendAction('update-ticket', { ticketId, title, description, type, state, priority, labels, assigneeName, sprintId });
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

    if (form.matches('[data-role="sprint-form"]')) {
      event.preventDefault();
      const name = form.querySelector('[data-field="name"]').value.trim();
      const weeks = parseInt(form.querySelector('[data-field="weeks"]').value) || 2;
      const startTime = Date.now();
      const endTime = startTime + (weeks * 7 * 24 * 60 * 60 * 1000);
      sendAction('create-sprint', { name, startTime, endTime });
      closeModal();
      return;
    }

    if (form.matches('[data-role="sprint-edit-form"]')) {
      event.preventDefault();
      const sprintId = form.dataset.sprintId;
      const name = form.querySelector('[data-field="name"]').value.trim();
      const status = form.querySelector('[data-field="status"]').value;
      const project = uiState.project || {};
      const sprint = (project.sprints || []).find(s => s.id === sprintId);
      if (sprint) {
        sendAction('update-sprint', { sprintId, name, status, startTime: sprint.startTime, endTime: sprint.endTime });
      }
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
