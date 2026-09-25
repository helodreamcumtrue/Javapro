// ===================================================================
// Project Graveyard • Goal Decomposition Engine Client Logic
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

// Hero & Navigation Interactive DOM Elements
const demoSignalCard = document.getElementById('demoSignalCard');
const heroBrowseBtn = document.getElementById('heroBrowseBtn');
const heroSubmitBtn = document.getElementById('heroSubmitBtn');
const topSubmitBtn = document.getElementById('topSubmitBtn');
const soundToggleBtn = document.getElementById('soundToggleBtn');

// ===================================================================
// Web Audio Synthesizer (BGM / Ambient Sound & Interaction Audio FX)
// ===================================================================
let audioCtx = null;
let soundEnabled = localStorage.getItem('sound_fx_enabled') === 'true';

function initAudio() {
  if (!audioCtx) {
    const AudioContext = window.AudioContext || window.webkitAudioContext;
    if (AudioContext) {
      audioCtx = new AudioContext();
    }
  }
  if (audioCtx && audioCtx.state === 'suspended') {
    audioCtx.resume();
  }
}

function playSound(type) {
  if (!soundEnabled) return;
  try {
    initAudio();
    if (!audioCtx) return;

    const now = audioCtx.currentTime;

    if (type === 'click') {
      const osc = audioCtx.createOscillator();
      const gain = audioCtx.createGain();
      osc.type = 'sine';
      osc.frequency.setValueAtTime(620, now);
      osc.frequency.exponentialRampToValueAtTime(240, now + 0.05);
      gain.gain.setValueAtTime(0.08, now);
      gain.gain.exponentialRampToValueAtTime(0.001, now + 0.05);
      osc.connect(gain);
      gain.connect(audioCtx.destination);
      osc.start(now);
      osc.stop(now + 0.05);
    } else if (type === 'check') {
      const osc = audioCtx.createOscillator();
      const gain = audioCtx.createGain();
      osc.type = 'triangle';
      osc.frequency.setValueAtTime(880, now);
      osc.frequency.exponentialRampToValueAtTime(1320, now + 0.09);
      gain.gain.setValueAtTime(0.12, now);
      gain.gain.exponentialRampToValueAtTime(0.001, now + 0.12);
      osc.connect(gain);
      gain.connect(audioCtx.destination);
      osc.start(now);
      osc.stop(now + 0.12);
    } else if (type === 'decompose') {
      // Futuristic 3-note ascending chord chime
      const notes = [440, 554.37, 659.25, 880];
      notes.forEach((freq, idx) => {
        const osc = audioCtx.createOscillator();
        const gain = audioCtx.createGain();
        const noteStart = now + idx * 0.06;
        osc.type = 'sine';
        osc.frequency.setValueAtTime(freq, noteStart);
        gain.gain.setValueAtTime(0.09, noteStart);
        gain.gain.exponentialRampToValueAtTime(0.0001, noteStart + 0.35);
        osc.connect(gain);
        gain.connect(audioCtx.destination);
        osc.start(noteStart);
        osc.stop(noteStart + 0.36);
      });
    } else if (type === 'chime') {
      const osc = audioCtx.createOscillator();
      const gain = audioCtx.createGain();
      osc.type = 'sine';
      osc.frequency.setValueAtTime(523.25, now);
      osc.frequency.exponentialRampToValueAtTime(1046.5, now + 0.18);
      gain.gain.setValueAtTime(0.12, now);
      gain.gain.exponentialRampToValueAtTime(0.001, now + 0.22);
      osc.connect(gain);
      gain.connect(audioCtx.destination);
      osc.start(now);
      osc.stop(now + 0.22);
    }
  } catch (err) {
    // Audio context may be restricted before user gesture
  }
}

// Update Audio Toggle Button State
function updateSoundButton() {
  if (!soundToggleBtn) return;
  if (soundEnabled) {
    soundToggleBtn.classList.add('active');
    soundToggleBtn.querySelector('.sound-icon').textContent = '🔊';
    soundToggleBtn.querySelector('.sound-text').textContent = 'Sound ON';
  } else {
    soundToggleBtn.classList.remove('active');
    soundToggleBtn.querySelector('.sound-icon').textContent = '🔇';
    soundToggleBtn.querySelector('.sound-text').textContent = 'Sound OFF';
  }
}

if (soundToggleBtn) {
  updateSoundButton();
  soundToggleBtn.addEventListener('click', () => {
    soundEnabled = !soundEnabled;
    localStorage.setItem('sound_fx_enabled', soundEnabled);
    updateSoundButton();
    if (soundEnabled) {
      playSound('decompose');
    }
  });
}

// ===================================================================
// Hero Interactivity (Reference Card & Buttons)
// ===================================================================

// Clicking Demo Signal Card decomposes "AI Study Planner"
if (demoSignalCard) {
  demoSignalCard.addEventListener('click', () => {
    playSound('chime');
    goalTitleInput.value = 'AI Study Planner';
    const radio = document.querySelector('input[name="difficulty"][value="medium"]');
    if (radio) {
      radio.checked = true;
      radio.dispatchEvent(new Event('change'));
    }
    goalForm.dispatchEvent(new Event('submit'));
  });

  demoSignalCard.addEventListener('keydown', (e) => {
    if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault();
      demoSignalCard.click();
    }
  });
}

// Jump to Decomposer from hero action buttons
function scrollToInputStudio() {
  playSound('click');
  const inputCard = document.querySelector('.input-card');
  if (inputCard) {
    inputCard.scrollIntoView({ behavior: 'smooth', block: 'center' });
    setTimeout(() => {
      goalTitleInput.focus();
    }, 450);
  }
}

if (heroSubmitBtn) heroSubmitBtn.addEventListener('click', scrollToInputStudio);
if (topSubmitBtn) topSubmitBtn.addEventListener('click', scrollToInputStudio);

if (heroBrowseBtn) {
  heroBrowseBtn.addEventListener('click', (e) => {
    e.preventDefault();
    playSound('click');
    const target = document.getElementById('decomposerSection');
    if (target) {
      target.scrollIntoView({ behavior: 'smooth' });
    }
  });
}

// ===================================================================
// Difficulty Radio Selection Sync
// ===================================================================
document.querySelectorAll('input[name="difficulty"]').forEach(radio => {
  radio.addEventListener('change', () => {
    playSound('click');
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
    playSound('click');
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
  playSound('click');

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
    playSound('decompose');
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

  // Strategy pill coloring (Neon Lime / Cyan / Amber)
  ruleMatchType.className = 'strategy-badge';
  if (goalResponse.matchType === 'TOPIC') {
    ruleMatchType.style.color = '#c3fa3b';
    ruleMatchType.style.borderColor = 'rgba(195, 250, 59, 0.4)';
    ruleMatchType.style.backgroundColor = 'rgba(195, 250, 59, 0.08)';
  } else if (goalResponse.matchType === 'VERB') {
    ruleMatchType.style.color = '#38bdf8';
    ruleMatchType.style.borderColor = 'rgba(56, 189, 248, 0.4)';
    ruleMatchType.style.backgroundColor = 'rgba(56, 189, 248, 0.08)';
  } else {
    ruleMatchType.style.color = '#fbbf24';
    ruleMatchType.style.borderColor = 'rgba(251, 191, 36, 0.4)';
    ruleMatchType.style.backgroundColor = 'rgba(251, 191, 36, 0.08)';
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

  // Custom Checkbox
  const cbWrap = document.createElement('label');
  cbWrap.className = 'task-checkbox-wrap';
  const cb = document.createElement('input');
  cb.type = 'checkbox';
  cb.className = 'task-checkbox';
  cb.checked = isCompleted;
  cb.addEventListener('change', async (e) => {
    e.stopPropagation();
    playSound('check');
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
        playSound('click');
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
    playSound('click');
    document.querySelectorAll('.filter-tab').forEach(t => t.classList.remove('active'));
    tab.classList.add('active');
    currentFilter = tab.dataset.filter;
    renderTaskTree();
  });
});

// Expand / Collapse All
expandAllBtn.addEventListener('click', () => {
  playSound('click');
  document.querySelectorAll('.task-sub-tree').forEach(tree => tree.classList.remove('hidden'));
  document.querySelectorAll('.toggle-arrow').forEach(a => a.classList.add('open'));
});

collapseAllBtn.addEventListener('click', () => {
  playSound('click');
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
    goalHistoryList.innerHTML = '<li class="empty-state">No goals stored yet. Decompose your first project above!</li>';
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
      playSound('click');
      if (confirm(`Delete "${goal.title}"?`)) {
        await deleteGoal(goal.id);
      }
    });
    right.appendChild(delBtn);

    li.appendChild(left);
    li.appendChild(right);

    li.addEventListener('click', () => {
      playSound('click');
      fetchAndDisplayGoal(goal.id);
    });

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
  playSound('click');

  let md = `# Project Plan: ${currentGoalData.title}\n`;
  md += `**Difficulty:** ${currentGoalData.difficulty} | **Estimated Days:** ~${currentGoalData.totalEstimatedDays}d\n`;
  md += `**Strategy:** ${currentGoalData.matchType || 'Rule-Based'} (${currentGoalData.matchedKeyword || ''})\n\n`;
  md += `## Execution Tree Hierarchy\n\n`;

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
  md += `\n---\n*Generated by Project Graveyard Goal Decomposition Engine (Rule-Based, 0% External AI)*\n`;

  downloadFile(`${slugify(currentGoalData.title)}-plan.md`, md, 'text/markdown');
});

exportJsonBtn.addEventListener('click', () => {
  if (!currentGoalData) return;
  playSound('click');
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
