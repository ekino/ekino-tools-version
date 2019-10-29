'use strict';

// Return a CSS HSL string
const hslColorPercent = (percent, start, end) => `hsl(${start + (percent / 100) * end},80%,50%)`;

window.addEventListener('DOMContentLoaded', async () => {
    document.querySelectorAll('.badge[data-color]').forEach(item => {
        const percent = item.getAttribute('data-color');
        item.style.backgroundColor = hslColorPercent(percent, 0, 120);

        // in order to keep a high contrast
        if (percent > 30) {
            item.style.color = '#343a40'
        } else {
            item.style.color = '#ffffff'
        }
    });

    const searchInput = document.getElementById('search-input');
    if (searchInput) {
        searchInput.addEventListener('keyup', () => {
            const filterValue = searchInput.value.toLowerCase();

            const allRepositories = document.querySelectorAll('.list-group-item');

            allRepositories.forEach(repository => {
                if (repository.textContent.includes(filterValue)) {
                    repository.style.display = '';
                } else {
                    repository.style.display = 'none';
                }
            });

        }, false);
    }

    activateTab();

    const response = await fetch(`${window.location.origin}/initialized`);
    const initialized = await response.json();
    if (!initialized) {
        websocket('status')
    }
}, false);

function activateTab(projectName) {
    // Hide all repositories
    document.querySelectorAll('.tabcontent').forEach(tab => {
        tab.style.display = 'none';
    });
    document.querySelectorAll('.nav-item').forEach(nav => {
        nav.classList.remove('active');
    });

    // Show the specific tab content
    let tab, nav;
    const selectedTab = projectName || localStorage.getItem("selectedTab");
    if (selectedTab) {
        tab = document.getElementById(`tab${selectedTab}`);
        nav = document.getElementById(`nav${selectedTab}`);
        // Store
        localStorage.setItem("selectedTab", selectedTab);
    } else {
        tab = document.getElementsByClassName('tabcontent')[0];
        nav = document.getElementsByClassName('nav-item')[0];
    }
    if (tab) {
        tab.style.display = 'block';
    }
    if (nav) {
        nav.classList.add('active');
    }
}

function websocket(command) {
    document.getElementById('loaderImg').style.display = 'block';
    document.getElementById('clearButton').style.display = 'none';

    const wsUrl = window.location.origin.replace('http', 'ws') + '/websocket';
    const socket = new WebSocket(wsUrl);
    socket.onopen = () => socket.send(command);
    socket.onclose = () => window.location.reload();
}
