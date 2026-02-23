/* Pricing billing toggle */
var isAnnual = true;
function toggleBilling() {
    isAnnual = !isAnnual;
    var toggle = document.getElementById('billing-toggle');
    var labelMonthly = document.getElementById('label-monthly');
    var labelAnnual = document.getElementById('label-annual');

    toggle.classList.toggle('active', isAnnual);
    labelAnnual.classList.toggle('pricing-toggle-label--active', isAnnual);
    labelMonthly.classList.toggle('pricing-toggle-label--active', !isAnnual);

    var prices = document.querySelectorAll('.pricing-price[data-annual]');
    var billedLabels = document.querySelectorAll('.pricing-billed');
    var annualTotals = { '1': '12', '10': '120', '50': '600' };

    prices.forEach(function (el, i) {
        var val = isAnnual ? el.dataset.annual : el.dataset.monthly;
        var formatted = parseFloat(val) % 1 === 0 ? parseInt(val) : parseFloat(val).toFixed(2);
        el.innerHTML = '&euro;' + formatted + ' <span class="period">/month</span>';
    });

    billedLabels.forEach(function (el, i) {
        var priceEl = el.previousElementSibling;
        var annualVal = priceEl.dataset.annual;
        if (annualVal === '0') {
            el.textContent = 'Perfect to get started';
        } else if (isAnnual) {
            el.innerHTML = 'Billed annually at &euro;' + annualTotals[annualVal];
        } else {
            el.textContent = 'Billed monthly, cancel anytime';
        }
    });
}
