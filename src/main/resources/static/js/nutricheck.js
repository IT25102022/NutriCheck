/**
 * NutriCheck - Global Client Scripts & Theme Controller
 */

(function () {
    'use strict';

    // Theme Management
    const STORAGE_KEY = 'nutricheck_theme';

    function getPreferredTheme() {
        const saved = localStorage.getItem(STORAGE_KEY);
        if (saved === 'dark' || saved === 'light') {
            return saved;
        }
        return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
    }

    function applyTheme(theme) {
        document.documentElement.setAttribute('data-theme', theme);
        const toggles = document.querySelectorAll('.theme-toggle-btn');
        toggles.forEach(btn => {
            const isDark = theme === 'dark';
            btn.setAttribute('aria-label', isDark ? 'Switch to light mode' : 'Switch to dark mode');
            btn.innerHTML = isDark
                ? `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><circle cx="12" cy="12" r="5"></circle><line x1="12" y1="1" x2="12" y2="3"></line><line x1="12" y1="21" x2="12" y2="23"></line><line x1="4.22" y1="4.22" x2="5.64" y2="5.64"></line><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"></line><line x1="1" y1="12" x2="3" y2="12"></line><line x1="21" y1="12" x2="23" y2="12"></line><line x1="4.22" y1="19.78" x2="5.64" y2="18.36"></line><line x1="18.36" y1="5.64" x2="19.78" y2="4.22"></line></svg><span class="theme-label">Light</span>`
                : `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"></path></svg><span class="theme-label">Dark</span>`;
        });
    }

    // Initialize theme immediately
    const initialTheme = getPreferredTheme();
    applyTheme(initialTheme);

    window.toggleNutriCheckTheme = function () {
        const current = document.documentElement.getAttribute('data-theme') || initialTheme;
        const next = current === 'dark' ? 'light' : 'dark';
        localStorage.setItem(STORAGE_KEY, next);
        applyTheme(next);
    };

    // Listen for OS preference changes if no manual preference stored
    window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', e => {
        if (!localStorage.getItem(STORAGE_KEY)) {
            applyTheme(e.matches ? 'dark' : 'light');
        }
    });

    // DOM Ready helpers
    document.addEventListener('DOMContentLoaded', () => {
        // Re-apply to update any newly rendered buttons
        applyTheme(document.documentElement.getAttribute('data-theme') || getPreferredTheme());

        // Auto-dismiss alerts after 6 seconds
        const alerts = document.querySelectorAll('.alert');
        alerts.forEach(alert => {
            setTimeout(() => {
                alert.style.transition = 'opacity 0.4s ease, transform 0.4s ease';
                alert.style.opacity = '0';
                alert.style.transform = 'translateY(-6px)';
                setTimeout(() => alert.remove(), 400);
            }, 6000);
        });

        // Active nav link highlighting — matches current path to nav-link href
        const currentPath = window.location.pathname.replace(/\/+$/, '') || '/';
        const navLinks = document.querySelectorAll('.nav-menu .nav-link');
        let bestMatch = null;
        let bestLength = 0;

        navLinks.forEach(link => {
            const href = (link.getAttribute('href') || '').replace(/\/+$/, '') || '/';
            // Exact match or longest prefix match (to handle nested paths)
            if (currentPath === href || (currentPath.startsWith(href) && href !== '/')) {
                if (href.length > bestLength) {
                    bestLength = href.length;
                    bestMatch = link;
                }
            }
        });

        if (bestMatch) {
            bestMatch.classList.add('active');
        }
    });
})();

/**
 * Client-side Quick BMI Calculation helper
 */
function calculateQuickBmi(heightCm, weightKg) {
    if (!heightCm || !weightKg || heightCm <= 0 || weightKg <= 0) {
        return null;
    }
    const heightM = heightCm / 100.0;
    const bmi = weightKg / (heightM * heightM);
    return Math.round(bmi * 100) / 100;
}
