// ===================================================================
// Goal Decomposition Engine - Production Client Logic
// Pure Vanilla JavaScript (ES6+) • Zero External Frameworks
// ===================================================================

const API_BASE = ''; // Uses root endpoints (/goal, /tasks, /goals)

let currentGoalData = null;
let currentFilter = 'all'; // 'all' | 'pending' | 'completed'
let cachedGoals = [];

// DOM References
const goalForm = document.getElementById('goalForm');
const goalTitleInput = document.getElementById('goalTitle');
const decomposeBtn = document.getElementById('decomposeBtn');
const btnText = decomposeBtn.querySelector('.btn-text');
const btnSpinner = decomposeBtn.querySelector('.btn-spinner');
const errorBox = document.getElementById('errorBox');

const emptyPlanState = document.getElementById('emptyPlanState');
const resultPanel = document.getElementById('resultPanel');
const resultTitle = document.getElementById('resultTitle');
const resultDifficulty = document.getElementById('resultDifficulty');
const resultTimeline = document.getElementById('resultTimeline');
const resultTaskStats = document.getElementById('resultTaskStats');

const ruleMatchType = document.getElementById('ruleMatchType');
const ruleExplanation = document.getElementById('ruleExplanation');
const ruleKeyword = document.getElementById('ruleKeyword');

const progressBar = document.getElementById('progressBar');
const progressPercent = document.getElementById('progressPercent');
const taskTree = document.getElementById('taskTree');

const goalHistoryList = document.getElementById('goalHistory');
const historySearch = document.getElementById('historySearch');
const historyCount = document.getElementById('historyCount');

const expandAllBtn = document.getElementById('expandAllBtn');
const collapseAllBtn = document.getElementById('collapseAllBtn');
const exportMdBtn = document.getElementById('exportMdBtn');
const exportJsonBtn = document.getElementById('exportJsonBtn');

// ===================================================================
// Difficulty Radio Selection Sync
// ===================================================================
document.querySelectorAll('input[name="difficulty"]').forEach(radio => {
  radio.addEventListener('change', () => {
    document.querySelectorAll('.diff-btn').forEach(btn => btn.classList.remove('active'));
    const parentLabel = radio.closest('.diff-option');
    if (parentLabel) {
      const btn = parentLabel.querySelector('.diff-btn');
      if (btn) btn.classList.add('active');
    }
  });
});

// ===================================================================
// Preset Chips
// ===================================================================
document.querySelectorAll('.preset-chip').forEach(chip => {
  chip.addEventListener('click', () => {
    const goal = chip.dataset.goal;
    const diff = chip.dataset.diff || 'medium';
    
    goalTitleInput.value = goal;
    
    const radio = document.querySelector(`input[name="difficulty"][value="${diff}"]`);
    if (radio) {
      radio.checked = true;
      radio.dispatchEvent(new Event('change'));
    }
    
    goalForm.dispatchEvent(new Event('submit'));
  });
});

// ===================================================================
// Form Submit -> POST /goal
// ===================================================================
goalForm.addEventListener('submit', async (e) => {
  e.preventDefault();
  hideError();

  const title = goalTitleInput.value.trim();
  const diffRadio = document.querySelector('input[name="difficulty"]:checked');
  const difficulty = diffRadio ? diffRadio.value : 'medium';

  if (!title) {
    showError('Please enter a valid goal statement.');
    return;
  }

  setLoading(true);

  try {
    const response = await fetch(`${API_BASE}/goal`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ title, difficulty })
    });

    const data = await response.json();

    if (!response.ok) {
      showError(data.error || 'Failed to decompose goal.');
      return;
    }

    currentGoalData = data;
    renderPlan(data);
    await loadHistory();
    highlightActiveHistory(data.goalId);

  } catch (err) {
    console.error('API Error:', err);
    showError('Unable to connect to the backend server. Make sure Spring Boot is running on port 8080.');
  } finally {
    setLoading(false);
  }
});

// ===================================================================
// Render Goal Plan & Inspector
// ===================================================================
function renderPlan(goalResponse) {
  emptyPlanState.classList.add('hidden');
  resultPanel.classList.remove('hidden');

  resultTitle.textContent = goalResponse.title;
  resultDifficulty.textContent = goalResponse.difficulty;
  resultTimeline.textContent = `~${goalResponse.totalEstimatedDays} days estimated`;

  // Rule Inspector Trace
  ruleMatchType.textContent = goalResponse.matchType || 'TOPIC_MATCH';
  ruleExplanation.textContent = goalResponse.explanation || 'Rule matching applied.';
  ruleKeyword.textContent = goalResponse.matchedKeyword || 'none';

  // Strategy pill coloring
  ruleMatchType.className = 'strategy-badge';
  if (goalResponse.matchType === 'TOPIC') {
    ruleMatchType.style.color = '#34d399';
    ruleMatchType.style.borderColor = 'rgba(52, 211, 153, 0.4)';
  } else if (goalResponse.matchType === 'VERB') {
    ruleMatchType.style.color = '#60a5fa';
    ruleMatchType.style.borderColor = 'rgba(96, 165, 250, 0.4)';
  } else {
    ruleMatchType.style.color = '#fbbf24';
    ruleMatchType.style.borderColor = 'rgba(251, 191, 36, 0.4)';
  }

  renderTaskTree();
  updateProgressStats();

  resultPanel.scrollIntoView({ behavior: 'smooth', block: 'start' });
}

// ===================================================================
// Render Task Tree (Top-Level & Subtasks)
// ===================================================================
function renderTaskTree() {
  if (!currentGoalData || !currentGoalData.tasks) return;

  taskTree.innerHTML = '';
  const rootTasks = currentGoalData.tasks;

  rootTasks.forEach((task, idx) => {
    const taskEl = createTaskElement(task, `${idx + 1}`);
    if (taskEl) {
      taskTree.appendChild(taskEl);
    }
  });
}

function createTaskElement(task, indexStr) {
  // Check filter
  const isCompleted = !!task.completed;
  if (currentFilter === 'completed' && !isCompleted) return null;
  if (currentFilter === 'pending' && isCompleted) return null;

  const hasChildren = task.children && task.children.length > 0;

  const li = document.createElement('li');
  li.className = 'task-item';

  const row = document.createElement('div');
  row.className = `task-row ${isCompleted ? 'completed' : ''}`;
  row.dataset.taskId = task.id;

  // Left Content
  const left = document.createElement('div');
  left.className = 'task-left';

  // Checkbox
  const cbWrap = document.createElement('label');
  cbWrap.className = 'task-checkbox-wrap';
  const cb = document.createElement('input');
  cb.type = 'checkbox';
  cb.className = 'task-checkbox';
  cb.checked = isCompleted;
  cb.addEventListener('change', async (e) => {
    e.stopPropagation();
    await toggleTaskStatus(task.id, cb, row);
  });
  cbWrap.appendChild(cb);
  left.appendChild(cbWrap);

  // Index numbering
  const idxSpan = document.createElement('span');
  idxSpan.className = 'task-index';
  idxSpan.textContent = `${indexStr}.`;
  left.appendChild(idxSpan);

  // Content
  const content = document.createElement('div');
  content.className = 'task-content';

  const title = document.createElement('span');
  title.className = 'task-title';
  title.textContent = task.description;
  content.appendChild(title);

  // Subtask toggle button if children exist
  let toggleBtn = null;
  if (hasChildren) {
    toggleBtn = document.createElement('button');
    toggleBtn.type = 'button';
    toggleBtn.className = 'toggle-btn';
    toggleBtn.innerHTML = `<span class="toggle-arrow open">▶</span> ${task.children.length} sub-steps`;
    content.appendChild(toggleBtn);
  }

  // Dependency note
  if (task.dependsOnTaskId) {
    const depChip = document.createElement('span');
    depChip.className = 'task-dep-chip';
    depChip.textContent = `🔗 Step ${task.dependsOnTaskId}`;
    content.appendChild(depChip);
  }

  left.appendChild(content);
  row.appendChild(left);

  // Right Metadata Pills
  const right = document.createElement('div');
  right.className = 'task-right';

  const prioPill = document.createElement('span');
  prioPill.className = `priority-pill priority-${task.priority}`;
  prioPill.textContent = task.priority;
  right.appendChild(prioPill);

  const daysPill = document.createElement('span');
  daysPill.className = 'days-pill';
  daysPill.textContent = `${task.estimatedDays}d`;
  right.appendChild(daysPill);

  const scorePill = document.createElement('span');
  scorePill.className = 'score-pill';
  scorePill.textContent = `Score ${Math.round(task.score)}`;
  right.appendChild(scorePill);

  row.appendChild(right);
  li.appendChild(row);

  // Recursive Subtasks
  if (hasChildren) {
    const subUl = document.createElement('ul');
    subUl.className = 'task-sub-tree';

    task.children.forEach((child, cIdx) => {
      const childEl = createTaskElement(child, `${indexStr}.${cIdx + 1}`);
      if (childEl) subUl.appendChild(childEl);
    });

    li.appendChild(subUl);

    if (toggleBtn) {
      toggleBtn.addEventListener('click', (e) => {
        e.stopPropagation();
        const arrow = toggleBtn.querySelector('.toggle-arrow');
        subUl.classList.toggle('hidden');
        arrow.classList.toggle('open');
      });
    }
  }

  return li;
}

// ===================================================================
// Toggle Task Completion -> PATCH /tasks/{taskId}/toggle
// ===================================================================
async function toggleTaskStatus(taskId, checkbox, rowElement) {
  try {
    const response = await fetch(`${API_BASE}/tasks/${taskId}/toggle`, {
      method: 'PATCH'
    });

    if (!response.ok) {
      checkbox.checked = !checkbox.checked;
      showError('Failed to update task completion status.');
      return;
    }

    const res = await response.json();
    const newStatus = res.completed;

    // Mutate in-memory tree state
    updateTaskCompletedState(currentGoalData.tasks, taskId, newStatus);

    if (newStatus) {
      rowElement.classList.add('completed');
    } else {
      rowElement.classList.remove('completed');
    }

    updateProgressStats();

    // Re-render if filter is active
    if (currentFilter !== 'all') {
      renderTaskTree();
    }

  } catch (err) {
    console.error('Failed to toggle task:', err);
    checkbox.checked = !checkbox.checked;
    showError('Network error while updating task.');
  }
}

function updateTaskCompletedState(tasks, taskId, completed) {
  for (const t of tasks) {
    if (t.id === taskId) {
      t.completed = completed;
      return true;
    }
    if (t.children && t.children.length > 0) {
      if (updateTaskCompletedState(t.children, taskId, completed)) return true;
    }
  }
  return false;
}

// ===================================================================
// Progress Calculation
// ===================================================================
function updateProgressStats() {
  if (!currentGoalData || !currentGoalData.tasks) return;

  const allTasks = flattenTasks(currentGoalData.tasks);
  const total = allTasks.length;
  const completed = allTasks.filter(t => t.completed).length;
  const percent = total === 0 ? 0 : Math.round((completed / total) * 100);

  progressBar.style.width = `${percent}%`;
  progressPercent.textContent = `${percent}% (${completed} of ${total} completed)`;
  resultTaskStats.textContent = `${total} tasks`;
}

function flattenTasks(tasks) {
  let list = [];
  tasks.forEach(t => {
    list.push(t);
    if (t.children && t.children.length > 0) {
      list = list.concat(flattenTasks(t.children));
    }
  });
  return list;
}

// ===================================================================
// Filter Tabs
// ===================================================================
document.querySelectorAll('.filter-tab').forEach(tab => {
  tab.addEventListener('click', () => {
    document.querySelectorAll('.filter-tab').forEach(t => t.classList.remove('active'));
    tab.classList.add('active');
    currentFilter = tab.dataset.filter;
    renderTaskTree();
  });
});

// Expand / Collapse All
expandAllBtn.addEventListener('click', () => {
  document.querySelectorAll('.task-sub-tree').forEach(tree => tree.classList.remove('hidden'));
  document.querySelectorAll('.toggle-arrow').forEach(a => a.classList.add('open'));
});

collapseAllBtn.addEventListener('click', () => {
  document.querySelectorAll('.task-sub-tree').forEach(tree => tree.classList.add('hidden'));
  document.querySelectorAll('.toggle-arrow').forEach(a => a.classList.remove('open'));
});

// ===================================================================
// History Management -> GET /goals & DELETE /goals/{id}
// ===================================================================
async function loadHistory() {
  try {
    const res = await fetch(`${API_BASE}/goals`);
    if (!res.ok) return;

    cachedGoals = await res.json();
    renderHistoryList(cachedGoals);
  } catch (err) {
    console.error('Failed to load history:', err);
  }
}

function renderHistoryList(goals) {
  goalHistoryList.innerHTML = '';
  historyCount.textContent = goals.length;

  if (goals.length === 0) {
    goalHistoryList.innerHTML = '<li class="empty-state">No goals stored yet.</li>';
    return;
  }

  goals.forEach(goal => {
    const li = document.createElement('li');
    li.className = 'goal-history-item';
    li.dataset.goalId = goal.id;

    if (currentGoalData && currentGoalData.goalId === goal.id) {
      li.classList.add('active');
    }

    const left = document.createElement('div');
    left.className = 'history-item-left';

    const title = document.createElement('span');
    title.className = 'history-item-title';
    title.textContent = goal.title;
    left.appendChild(title);

    const meta = document.createElement('span');
    meta.className = 'history-item-meta';
    const dateStr = goal.createdAt ? new Date(goal.createdAt).toLocaleDateString() : '';
    meta.textContent = `${goal.matchType || 'Rule'} • ${dateStr}`;
    left.appendChild(meta);

    const right = document.createElement('div');
    right.className = 'history-item-right';

    const diff = document.createElement('span');
    diff.className = 'diff-badge-mini';
    diff.textContent = goal.difficulty;
    right.appendChild(diff);

    const delBtn = document.createElement('button');
    delBtn.type = 'button';
    delBtn.className = 'delete-goal-btn';
    delBtn.title = 'Delete Goal';
    delBtn.innerHTML = '🗑️';
    delBtn.addEventListener('click', async (e) => {
      e.stopPropagation();
      if (confirm(`Delete "${goal.title}"?`)) {
        await deleteGoal(goal.id);
      }
    });
    right.appendChild(delBtn);

    li.appendChild(left);
    li.appendChild(right);

    li.addEventListener('click', () => fetchAndDisplayGoal(goal.id));

    goalHistoryList.appendChild(li);
  });
}

async function fetchAndDisplayGoal(goalId) {
  setLoading(true);
  try {
    const res = await fetch(`${API_BASE}/tasks/${goalId}`);
    if (!res.ok) throw new Error('Goal not found');
    const data = await res.json();
    currentGoalData = data;
    renderPlan(data);
    highlightActiveHistory(goalId);
  } catch (err) {
    showError('Could not load goal details.');
  } finally {
    setLoading(false);
  }
}

async function deleteGoal(goalId) {
  try {
    const res = await fetch(`${API_BASE}/goals/${goalId}`, { method: 'DELETE' });
    if (!res.ok) throw new Error('Failed to delete');

    if (currentGoalData && currentGoalData.goalId === goalId) {
      currentGoalData = null;
      resultPanel.classList.add('hidden');
      emptyPlanState.classList.remove('hidden');
    }

    await loadHistory();
  } catch (err) {
    showError('Error deleting goal.');
  }
}

function highlightActiveHistory(goalId) {
  document.querySelectorAll('.goal-history-item').forEach(item => {
    if (parseInt(item.dataset.goalId) === goalId) {
      item.classList.add('active');
    } else {
      item.classList.remove('active');
    }
  });
}

// History Search Filter
historySearch.addEventListener('input', (e) => {
  const query = e.target.value.toLowerCase().trim();
  const filtered = cachedGoals.filter(g => g.title.toLowerCase().includes(query));
  renderHistoryList(filtered);
});

// ===================================================================
// Export Functions (Markdown Checklist & JSON)
// ===================================================================
exportMdBtn.addEventListener('click', () => {
  if (!currentGoalData) return;

  let md = `# Goal: ${currentGoalData.title}\n`;
  md += `**Difficulty:** ${currentGoalData.difficulty} | **Estimated Days:** ~${currentGoalData.totalEstimatedDays}d\n`;
  md += `**Strategy:** ${currentGoalData.matchType || 'Rule-Based'} (${currentGoalData.matchedKeyword || ''})\n\n`;
  md += `## Task Plan\n\n`;

  function buildMdTree(tasks, depth = 0) {
    const indent = '  '.repeat(depth);
    tasks.forEach((t) => {
      const check = t.completed ? '[x]' : '[ ]';
      md += `${indent}- ${check} **${t.description}** (${t.priority}, ~${t.estimatedDays}d, score: ${Math.round(t.score)})\n`;
      if (t.children && t.children.length > 0) {
        buildMdTree(t.children, depth + 1);
      }
    });
  }

  buildMdTree(currentGoalData.tasks);
  md += `\n---\n*Generated by Goal Decomposition Engine (Rule-Based, 0% External AI)*\n`;

  downloadFile(`${slugify(currentGoalData.title)}-plan.md`, md, 'text/markdown');
});

exportJsonBtn.addEventListener('click', () => {
  if (!currentGoalData) return;
  const jsonStr = JSON.stringify(currentGoalData, null, 2);
  downloadFile(`${slugify(currentGoalData.title)}-tasks.json`, jsonStr, 'application/json');
});

function downloadFile(filename, content, mimeType) {
  const blob = new Blob([content], { type: mimeType });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
}

function slugify(text) {
  return text.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-+|-+$/g, '');
}

// ===================================================================
// UI Helpers
// ===================================================================
function showError(msg) {
  errorBox.textContent = msg;
  errorBox.classList.remove('hidden');
}

function hideError() {
  errorBox.classList.add('hidden');
}

function setLoading(isLoading) {
  decomposeBtn.disabled = isLoading;
  btnText.textContent = isLoading ? 'Processing Rules...' : 'Decompose Goal';
  if (isLoading) {
    btnSpinner.classList.remove('hidden');
  } else {
    btnSpinner.classList.add('hidden');
  }
}

// ===================================================================
// Initialization
// ===================================================================
loadHistory();
