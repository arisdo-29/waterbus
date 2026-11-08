(function () {
    const canvas = document.getElementById('revenueChart');
    const container = document.querySelector('[data-revenue-data]');
    if (!canvas || !container || typeof Chart === 'undefined') return;

    const days = Array.from(container.querySelectorAll('[data-day]'));
    const labels = days.map(day => {
        const value = day.getAttribute('data-day') || '';
        const parts = value.split('-');
        return parts.length === 3 ? `${parts[2]}/${parts[1]}` : value;
    });
    const values = days.map(day => Number(day.getAttribute('data-amount')) || 0);

    new Chart(canvas, {
        type: 'bar',
        data: {labels, datasets: [{label: 'Doanh thu (đ)', data: values, backgroundColor: '#147D75', borderRadius: 5}]},
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {legend: {display: false}, tooltip: {callbacks: {label: context => `${Number(context.raw).toLocaleString('vi-VN')} đ`}}},
            scales: {y: {beginAtZero: true, ticks: {callback: value => Number(value).toLocaleString('vi-VN')}}}
        }
    });
}());
