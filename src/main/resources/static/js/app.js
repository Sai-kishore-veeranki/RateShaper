let eventSource = null;
let algorithms = [];
let allowedCount = 0;
let blockedCount = 0;
let currentConfig = {};
let currentAlgorithmId = '';

document.addEventListener('DOMContentLoaded', () => {
    fetchAlgorithms();
    document.getElementById('algorithm').addEventListener('change', updateConfigFields);
    document.getElementById('startBtn').addEventListener('click', startSimulation);
    document.getElementById('resetBtn').addEventListener('click', reset);
});

async function fetchAlgorithms() {
    try {
        const res = await fetch('/api/algorithms');
        algorithms = await res.json();
        const select = document.getElementById('algorithm');
        select.innerHTML = '';
        algorithms.forEach(a => {
            const opt = document.createElement('option');
            opt.value = a.id;
            opt.textContent = a.displayName;
            select.appendChild(opt);
        });
        updateConfigFields();
    } catch (err) {
        console.error('Failed to load algorithms:', err);
    }
}

function updateConfigFields() {
    const algoId = document.getElementById('algorithm').value;
    const algo = algorithms.find(a => a.id === algoId);
    const container = document.getElementById('config-fields');
    container.innerHTML = '';

    if (!algo) return;

    algo.configFields.forEach(field => {
        const div = document.createElement('div');
        div.className = 'form-group';

        const label = document.createElement('label');
        label.htmlFor = field;
        label.textContent = formatLabel(field);

        const input = document.createElement('input');
        input.type = 'number';
        input.id = field;
        input.step = field === 'refillRatePerSec' ? '0.1' : '1';
        input.value = getDefaultValue(field);

        div.appendChild(label);
        div.appendChild(input);
        container.appendChild(div);
    });
}

function formatLabel(field) {
    return field
        .replace(/([A-Z])/g, ' $1')
        .replace(/^./, str => str.toUpperCase())
        .trim();
}

function getDefaultValue(field) {
    switch (field) {
        case 'capacity': return '10';
        case 'refillRatePerSec': return '2';
        case 'windowSizeMs': return '1000';
        case 'requestLimit': return '5';
        default: return '1';
    }
}

async function startSimulation() {
    if (eventSource) {
        eventSource.close();
        eventSource = null;
    }

    const algoId = document.getElementById('algorithm').value;
    const algo = algorithms.find(a => a.id === algoId);
    if (!algo) return;

    currentAlgorithmId = algoId;
    currentConfig = {};

    const body = {
        algorithm: algoId,
        pattern: document.getElementById('pattern').value,
        totalRequests: parseInt(document.getElementById('totalRequests').value, 10),
        durationMs: parseInt(document.getElementById('durationMs').value, 10)
    };

    algo.configFields.forEach(field => {
        const val = document.getElementById(field).value;
        body[field] = field === 'refillRatePerSec' ? parseFloat(val) : parseInt(val, 10);
        currentConfig[field] = body[field];
    });

    try {
        const res = await fetch('/api/simulate', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body)
        });

        if (!res.ok) {
            const err = await res.json();
            alert('Error: ' + err.error);
            return;
        }

        const data = await res.json();
        const simulationId = data.simulationId;

        resetStats();
        document.getElementById('timeline').innerHTML = '';

        eventSource = new EventSource('/api/simulate/' + simulationId + '/stream');

        eventSource.onmessage = (e) => {
            const event = JSON.parse(e.data);
            handleEvent(event);
        };

        eventSource.onerror = () => {
            if (eventSource) {
                eventSource.close();
                eventSource = null;
            }
        };
    } catch (err) {
        alert('Failed to start simulation: ' + err.message);
    }
}

function handleEvent(event) {
    const timeline = document.getElementById('timeline');
    const block = document.createElement('div');
    block.className = 'block ' + (event.allowed ? 'allowed' : 'blocked');
    block.title = 'Request ' + event.requestId + ' @ ' + event.timestampOffsetMs + 'ms';
    timeline.appendChild(block);

    requestAnimationFrame(() => {
        block.classList.add('visible');
    });

    if (event.allowed) {
        allowedCount++;
        document.getElementById('allowedCount').textContent = allowedCount;
    } else {
        blockedCount++;
        document.getElementById('blockedCount').textContent = blockedCount;
    }

    updateStateDisplay(event);
}

function updateStateDisplay(event) {
    const display = document.getElementById('stateDisplay');
    const remaining = event.remaining;

    switch (currentAlgorithmId) {
        case 'TOKEN_BUCKET':
            display.textContent = 'Tokens remaining: ' + remaining + '/' + currentConfig.capacity;
            break;
        case 'FIXED_WINDOW':
            display.textContent = 'Window count: ' + (currentConfig.requestLimit - remaining) + '/' + currentConfig.requestLimit;
            break;
        case 'SLIDING_LOG':
            display.textContent = 'Log size: ' + (currentConfig.requestLimit - remaining) + '/' + currentConfig.requestLimit;
            break;
        case 'SLIDING_COUNTER':
            display.textContent = 'Window count: ' + (currentConfig.requestLimit - remaining) + '/' + currentConfig.requestLimit;
            break;
        default:
            display.textContent = 'Remaining: ' + remaining;
    }
}

function resetStats() {
    allowedCount = 0;
    blockedCount = 0;
    document.getElementById('allowedCount').textContent = '0';
    document.getElementById('blockedCount').textContent = '0';
    document.getElementById('stateDisplay').textContent = 'Ready';
}

function reset() {
    if (eventSource) {
        eventSource.close();
        eventSource = null;
    }
    document.getElementById('timeline').innerHTML = '';
    resetStats();
}
