/* ══════════════════════════════════════════════════
   Buurman — Changelog page interactions
   main.js already handles .reveal scroll animations
   ══════════════════════════════════════════════════ */

(function () {
    'use strict';

    document.addEventListener('DOMContentLoaded', function () {

        // ── Sidebar active state on scroll ───────────
        var navItems = document.querySelectorAll('.changelog-nav-item[data-target]');
        var weekEntries = document.querySelectorAll('.week-entry[id]');

        if (navItems.length && weekEntries.length && 'IntersectionObserver' in window) {
            var setActive = function (id) {
                navItems.forEach(function (item) {
                    item.classList.toggle('active', item.dataset.target === id);
                });
            };

            var scrollObserver = new IntersectionObserver(function (entries) {
                entries.forEach(function (entry) {
                    if (entry.isIntersecting) {
                        setActive(entry.target.id);
                    }
                });
            }, { threshold: 0.15, rootMargin: '-80px 0px -55% 0px' });

            weekEntries.forEach(function (el) { scrollObserver.observe(el); });

            // Activate first by default
            if (navItems[0]) { navItems[0].classList.add('active'); }
        }

        // ── Copy link on week-date click ─────────────
        var toast = document.getElementById('copyToast');
        var toastTimer = null;

        var showToast = function () {
            if (!toast) { return; }
            toast.classList.add('show');
            clearTimeout(toastTimer);
            toastTimer = setTimeout(function () {
                toast.classList.remove('show');
            }, 2200);
        };

        document.querySelectorAll('.week-date[href]').forEach(function (link) {
            link.addEventListener('click', function (e) {
                var href = link.getAttribute('href');
                if (!href || href.charAt(0) !== '#') { return; }

                if (navigator.clipboard && navigator.clipboard.writeText) {
                    e.preventDefault();
                    var url = window.location.origin + window.location.pathname + href;
                    navigator.clipboard.writeText(url).then(function () {
                        history.replaceState(null, '', href);
                        showToast();
                    });
                }
            });
        });

    });
})();
