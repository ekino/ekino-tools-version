'use strict';

const hslColorPercent = (percent, start, end) => {
    //Return a CSS HSL string
    const a = percent / 100, b = end * a, c = b + start;
    return 'hsl(' + c + ',80%,50%)';
};

window.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('.label[data-color]').forEach(item => {
        const percent = item.getAttribute('data-color');
        item.style.backgroundColor = hslColorPercent(percent, 0, 120);
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
    if (projectName) {
        tab = document.getElementById(`tab${projectName}`);
        nav = document.getElementById(`nav${projectName}`);
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
