'use strict';

const hslColorPercent = (percent, start, end) => {
    //Return a CSS HSL string
    const a = percent / 100, b = end * a, c = b + start;
    return 'hsl(' + c + ',80%,50%)';
};

window.addEventListener('DOMContentLoaded', () => {
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
