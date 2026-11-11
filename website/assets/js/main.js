/* Scroll reveal — staggered, prefers-reduced-motion aware */
document.addEventListener('DOMContentLoaded', function () {
    var els = document.querySelectorAll('.reveal');

    /* Respect user's motion preferences */
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
        els.forEach(function (el) { el.classList.add('visible'); });
        return;
    }

    if (!('IntersectionObserver' in window)) {
        els.forEach(function (el) { el.classList.add('visible'); });
        return;
    }

    var observer = new IntersectionObserver(function (entries) {
        entries.forEach(function (entry) {
            if (!entry.isIntersecting) { return; }

            var target = entry.target;
            observer.unobserve(target);

            /* Stagger by sibling index within the same .features-list */
            var parent = target.closest('.features-list');
            var delay = 0;

            if (parent) {
                var siblings = parent.querySelectorAll('.reveal');
                for (var i = 0; i < siblings.length; i++) {
                    if (siblings[i] === target) {
                        delay = i * 75;
                        break;
                    }
                }
            }

            if (delay > 0) {
                setTimeout(function () { target.classList.add('visible'); }, delay);
            } else {
                target.classList.add('visible');
            }
        });
    }, { threshold: 0.12, rootMargin: '0px 0px -50px 0px' });

    els.forEach(function (el) { observer.observe(el); });
});

/* Demo click attribution — which placement actually sends people into the
   demo. Fires a GA4 event with the placement from data-demo-cta. */
document.addEventListener('click', function (event) {
    var link = event.target.closest ? event.target.closest('[data-demo-cta]') : null;
    if (!link || typeof window.gtag !== 'function') { return; }

    window.gtag('event', 'demo_open', {
        placement: link.getAttribute('data-demo-cta'),
        page: window.location.pathname
    });
});
