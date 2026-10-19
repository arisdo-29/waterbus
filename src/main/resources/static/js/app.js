(function () {
    function formatMoney(amount) {
        return Number(amount || 0).toLocaleString('vi-VN') + ' đ';
    }

    function initNavigation() {
        const path = window.location.pathname;
        document.querySelectorAll('.wb-nav-links .nav-link').forEach(function (link) {
            const linkPath = new URL(link.href, window.location.origin).pathname;
            const isHome = linkPath === '/' || linkPath === '/templates/index.html';
            const currentIsHome = path === '/' || path === '/templates/index.html';
            if ((isHome && currentIsHome) || (!isHome && path.startsWith(linkPath))) {
                link.classList.add('active');
                link.setAttribute('aria-current', 'page');
            }
        });
    }

    function initSearchForms() {
        const dateInput = document.querySelector('#date');
        if (dateInput) {
            const today = new Date();
            const localDate = new Date(today.getTime() - today.getTimezoneOffset() * 60000).toISOString().split('T')[0];
            dateInput.min = localDate;
            if (!dateInput.value) {
                dateInput.value = localDate;
            }
        }

        const swapButton = document.querySelector('[data-swap-stations]');
        if (swapButton) {
            swapButton.addEventListener('click', function () {
                const from = document.querySelector('#fromStationId');
                const to = document.querySelector('#toStationId');
                if (!from || !to) {
                    return;
                }
                const fromValue = from.value;
                from.value = to.value;
                to.value = fromValue;
            });
        }

        const searchForm = document.querySelector('[data-trip-search]');
        if (searchForm) {
            searchForm.addEventListener('submit', function (event) {
                const from = document.querySelector('#fromStationId');
                const to = document.querySelector('#toStationId');
                const error = document.querySelector('[data-search-error]');
                if (from && to && error && from.value && to.value && from.value === to.value) {
                    event.preventDefault();
                    error.textContent = 'Vui lòng chọn hai bến khác nhau.';
                    error.classList.add('is-visible');
                    to.focus();
                }
            });
        }
    }

    function initSeatSelection() {
        const form = document.querySelector('[data-seat-form]');
        if (!form) {
            return;
        }

        const seats = Array.from(form.querySelectorAll('input[name="seatIds"]'));
        const chips = form.querySelector('[data-seat-chips]');
        const count = form.querySelector('[data-seat-count]');
        const total = form.querySelector('[data-seat-total]');
        const submit = form.querySelector('[data-seat-submit]');
        const message = form.querySelector('[data-seat-message]');
        const ticketTypes = Array.from(form.querySelectorAll('input[name="ticketTypeId"]'));

        function getPrice() {
            const selectedType = ticketTypes.find(function (ticketType) {
                return ticketType.checked;
            });
            return selectedType ? Number(selectedType.dataset.price || 0) : 0;
        }

        function updateSummary() {
            const selectedSeats = seats.filter(function (seat) {
                return seat.checked;
            });
            if (chips) {
                chips.innerHTML = '';
                if (!selectedSeats.length) {
                    const empty = document.createElement('span');
                    empty.className = 'wb-muted-text';
                    empty.textContent = 'Chưa chọn ghế';
                    chips.appendChild(empty);
                } else {
                    selectedSeats.forEach(function (seat) {
                        const chip = document.createElement('span');
                        chip.className = 'wb-chip';
                        chip.textContent = seat.dataset.seatCode || seat.value;
                        chips.appendChild(chip);
                    });
                }
            }
            if (count) {
                count.textContent = String(selectedSeats.length);
            }
            if (total) {
                total.textContent = formatMoney(selectedSeats.length * getPrice());
            }
            if (submit) {
                submit.disabled = selectedSeats.length === 0 || selectedSeats.length > 10;
            }
        }

        seats.forEach(function (seat) {
            seat.addEventListener('change', function () {
                const selectedCount = seats.filter(function (item) {
                    return item.checked;
                }).length;
                if (selectedCount > 10) {
                    seat.checked = false;
                    if (message) {
                        message.textContent = 'Bạn chỉ có thể chọn tối đa 10 ghế trong một lần đặt.';
                    }
                } else if (message) {
                    message.textContent = '';
                }
                updateSummary();
            });
        });
        ticketTypes.forEach(function (ticketType) {
            ticketType.addEventListener('change', updateSummary);
        });
        updateSummary();
    }

    function initBackButton() {
        document.querySelectorAll('[data-action="back"]').forEach(function (button) {
            button.addEventListener('click', function () {
                window.history.back();
            });
        });
    }

    function initCountdown() {
        const countdown = document.querySelector('[data-countdown]');
        if (!countdown) {
            return;
        }
        const output = countdown.querySelector('[data-countdown-time]');
        const paymentForm = document.querySelector('[data-payment-form]');
        const submit = document.querySelector('[data-payment-submit]');
        const expiredMessage = document.querySelector('[data-payment-expired]');
        const expiresAt = new Date(countdown.dataset.expiresAt).getTime();

        function render() {
            const remaining = Math.max(0, expiresAt - Date.now());
            const seconds = Math.floor(remaining / 1000);
            const minutesPart = String(Math.floor(seconds / 60)).padStart(2, '0');
            const secondsPart = String(seconds % 60).padStart(2, '0');
            if (output) {
                output.textContent = minutesPart + ':' + secondsPart;
            }
            if (remaining === 0) {
                countdown.classList.add('is-expired');
                if (submit) {
                    submit.disabled = true;
                }
                if (expiredMessage) {
                    expiredMessage.classList.add('is-visible');
                }
                if (paymentForm) {
                    paymentForm.classList.add('is-expired');
                }
                return false;
            }
            return true;
        }

        if (render()) {
            const intervalId = window.setInterval(function () {
                if (!render()) {
                    window.clearInterval(intervalId);
                }
            }, 1000);
        }
    }

    function initCopyButtons() {
        document.querySelectorAll('[data-copy-code]').forEach(function (button) {
            button.addEventListener('click', function () {
                const value = button.dataset.copyCode;
                const label = button.querySelector('[data-copy-label]');
                if (!value) {
                    return;
                }
                const copied = function () {
                    if (label) {
                        label.textContent = 'Đã sao chép';
                    }
                    window.setTimeout(function () {
                        if (label) {
                            label.textContent = 'Sao chép';
                        }
                    }, 2200);
                };
                if (navigator.clipboard && navigator.clipboard.writeText) {
                    navigator.clipboard.writeText(value).then(copied);
                } else {
                    copied();
                }
            });
        });
    }

    function initPrintButton() {
        document.querySelectorAll('[data-print-ticket]').forEach(function (button) {
            button.addEventListener('click', function () {
                window.print();
            });
        });
    }

    function init() {
        initNavigation();
        initSearchForms();
        initSeatSelection();
        initBackButton();
        initCountdown();
        initCopyButtons();
        initPrintButton();
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
