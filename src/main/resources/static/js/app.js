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
        document.querySelectorAll('[data-action="reload"]').forEach(function (button) {
            button.addEventListener('click', function () {
                window.location.reload();
            });
        });
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

        if (!Number.isFinite(expiresAt)) {
            if (output) {
                output.textContent = 'Chưa có thời hạn giữ chỗ';
            }
            if (submit) {
                submit.disabled = true;
            }
            return;
        }

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

    function initStationFilter() {
        const filter = document.querySelector('[data-station-filter]');
        const grid = document.querySelector('[data-station-grid]');
        if (!filter || !grid) {
            return;
        }
        const count = document.querySelector('[data-station-count]');
        const empty = document.querySelector('[data-station-empty]');
        const cards = Array.from(grid.children);
        function update() {
            const query = filter.value.trim().toLocaleLowerCase('vi');
            let visible = 0;
            cards.forEach(function (card) {
                const matches = card.textContent.toLocaleLowerCase('vi').includes(query);
                card.hidden = !matches;
                if (matches) {
                    visible++;
                }
            });
            if (count) {
                count.textContent = 'Có ' + visible + ' bến tàu';
            }
            if (empty) {
                empty.hidden = visible > 0;
            }
        }
        filter.addEventListener('input', update);
        update();
    }

    function init() {
        initNavigation();
        initSearchForms();
        initSeatSelection();
        initBackButton();
        initCountdown();
        initCopyButtons();
        initPrintButton();
        initStationFilter();
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();

/* Admin and handoff pages opt into these features through data attributes. */
(function () {
    function initAdminNavigation() {
        const path = window.location.pathname;
        const previewSections = {
            trips: 'trip', waterbuses: 'waterbus', stations: 'station',
            routes: 'route', promotions: 'promotion', 'ticket-types': 'tickettype'
        };
        document.querySelectorAll('[data-admin-nav]').forEach(function (link) {
            const target = link.dataset.adminNav;
            const section = target.split('/').filter(Boolean).pop();
            const active = path === target || path.startsWith(target + '/') ||
                path.startsWith('/templates/admin/' + previewSections[section] + '/');
            link.classList.toggle('is-active', active);
            if (active) {
                link.setAttribute('aria-current', 'page');
            } else {
                link.removeAttribute('aria-current');
            }
        });
    }

    function initConfirmations() {
        document.addEventListener('submit', function (event) {
            const form = event.target;
            const submitter = event.submitter;
            const message = (submitter && submitter.dataset.confirm) || form.dataset.confirm;
            if (message && !window.confirm(message)) {
                event.preventDefault();
            }
        });
        document.querySelectorAll('[data-confirm]').forEach(function (control) {
            if (control.tagName === 'FORM' ||
                (control.tagName === 'BUTTON' && control.type === 'submit' && control.form) ||
                (control.tagName === 'INPUT' && control.type === 'submit' && control.form)) {
                return;
            }
            control.addEventListener('click', function (event) {
                if (!window.confirm(control.dataset.confirm)) {
                    event.preventDefault();
                }
            });
        });
    }

    function initRouteEditors() {
        document.querySelectorAll('[data-route-editor]').forEach(function (editor) {
            const container = editor.querySelector('[data-route-rows]');
            const template = editor.querySelector('[data-route-row-template]');
            const message = editor.querySelector('[data-route-message]');
            if (!container || !template) {
                return;
            }
            function rows() {
                return Array.from(container.querySelectorAll('[data-route-row]'));
            }
            function appendRow() {
                const fragment = template.content.cloneNode(true);
                container.appendChild(fragment);
                return rows().at(-1);
            }
            function update() {
                const allRows = rows();
                const selected = allRows.map(function (row) {
                    return row.querySelector('select[name="stationIds"]').value;
                });
                allRows.forEach(function (row, index) {
                    row.querySelector('[data-route-order]').textContent = String(index + 1);
                    const select = row.querySelector('select[name="stationIds"]');
                    select.setAttribute('aria-label', 'Bến thứ ' + (index + 1));
                    const duplicate = select.value && selected.filter(function (value) {
                        return value === select.value;
                    }).length > 1;
                    select.setCustomValidity(duplicate ? 'Mỗi bến chỉ được chọn một lần trong tuyến.' : '');
                    row.querySelector('[data-route-up]').disabled = index === 0;
                    row.querySelector('[data-route-down]').disabled = index === allRows.length - 1;
                    row.querySelector('[data-route-remove]').disabled = allRows.length <= 2;
                });
            }
            while (rows().length < 2) {
                appendRow();
            }
            editor.addEventListener('change', update);
            editor.addEventListener('click', function (event) {
                const button = event.target.closest('button');
                if (!button || button.disabled) {
                    return;
                }
                let focusTarget;
                if (button.hasAttribute('data-route-add')) {
                    focusTarget = appendRow().querySelector('select');
                } else {
                    const row = button.closest('[data-route-row]');
                    if (!row || row.parentElement !== container) {
                        return;
                    }
                    if (button.hasAttribute('data-route-remove') && rows().length > 2) {
                        focusTarget = (row.nextElementSibling || row.previousElementSibling).querySelector('select');
                        row.remove();
                    } else if (button.hasAttribute('data-route-up') && row.previousElementSibling) {
                        container.insertBefore(row, row.previousElementSibling);
                        focusTarget = row.querySelector('select');
                    } else if (button.hasAttribute('data-route-down') && row.nextElementSibling) {
                        container.insertBefore(row.nextElementSibling, row);
                        focusTarget = row.querySelector('select');
                    }
                }
                update();
                if (focusTarget) {
                    focusTarget.focus();
                    if (message) {
                        message.textContent = 'Hành trình hiện có ' + rows().length + ' bến.';
                    }
                }
            });
            update();
        });
    }

    function initPromotions() {
        document.querySelectorAll('[data-promo-form]').forEach(function (form) {
            const percentFields = form.querySelectorAll('[data-promo-percent]');
            const discountValue = form.querySelector('[data-discount-value]');
            const start = form.querySelector('[name="startAt"]');
            const end = form.querySelector('[name="endAt"]');
            function update() {
                const selected = form.querySelector('[name="discountType"]:checked');
                const percent = selected && selected.value === 'PERCENT';
                form.classList.toggle('is-percent', Boolean(percent));
                percentFields.forEach(function (field) {
                    field.hidden = !percent;
                    field.querySelectorAll('input').forEach(function (input) {
                        input.disabled = !percent;
                    });
                });
                if (discountValue) {
                    if (percent) {
                        discountValue.max = '100';
                    } else {
                        discountValue.removeAttribute('max');
                    }
                }
                if (start && end) {
                    end.setCustomValidity(start.value && end.value && end.value <= start.value ?
                        'Thời gian kết thúc phải sau thời gian bắt đầu.' : '');
                }
            }
            form.addEventListener('change', update);
            form.addEventListener('input', update);
            update();
        });
    }

    function initAdminSidebar() {
        const sidebar = document.querySelector('.wb-admin .wb-admin-sidebar');
        const toggle = document.querySelector('[data-admin-sidebar-toggle]');
        const backdrop = document.querySelector('.wb-admin [data-admin-sidebar-close]');
        if (!sidebar || !toggle || !backdrop) {
            return;
        }
        sidebar.id = 'wbAdminSidebar';
        const close = document.createElement('button');
        close.type = 'button';
        close.className = 'btn btn-wb-outline wb-admin-sidebar-close';
        close.textContent = 'Đóng menu';
        close.setAttribute('aria-label', 'Đóng menu quản trị');
        sidebar.prepend(close);
        const mobile = window.matchMedia('(max-width: 991.98px)');
        const outside = Array.from(document.querySelectorAll('.wb-admin-content, .wb-navbar, .wb-footer'));
        const previousInert = new Map();
        let opened = false;
        let previousFocus;
        function setOpen(value) {
            opened = value && mobile.matches;
            sidebar.classList.toggle('is-open', opened);
            backdrop.hidden = !opened;
            toggle.setAttribute('aria-expanded', String(opened));
            document.body.classList.toggle('wb-admin-sidebar-open', opened);
            if (opened) {
                previousFocus = document.activeElement;
                sidebar.setAttribute('role', 'dialog');
                sidebar.setAttribute('aria-modal', 'true');
                sidebar.setAttribute('aria-label', 'Menu quản trị');
                outside.forEach(function (element) {
                    previousInert.set(element, element.inert);
                    element.inert = true;
                });
                close.focus();
            } else {
                sidebar.removeAttribute('role');
                sidebar.removeAttribute('aria-modal');
                sidebar.removeAttribute('aria-label');
                previousInert.forEach(function (value, element) {
                    element.inert = value;
                });
                previousInert.clear();
                if (previousFocus && mobile.matches) {
                    previousFocus.focus();
                }
                previousFocus = null;
            }
        }
        toggle.addEventListener('click', function () { setOpen(!opened); });
        close.addEventListener('click', function () { setOpen(false); });
        backdrop.addEventListener('click', function () { setOpen(false); });
        sidebar.querySelectorAll('a').forEach(function (link) {
            link.addEventListener('click', function () { if (opened) { setOpen(false); } });
        });
        document.addEventListener('keydown', function (event) {
            if (!opened) {
                return;
            }
            if (event.key === 'Escape') {
                event.preventDefault();
                setOpen(false);
            } else if (event.key === 'Tab') {
                const focusable = Array.from(sidebar.querySelectorAll('a[href], button:not(:disabled)'));
                const first = focusable[0];
                const last = focusable.at(-1);
                if (event.shiftKey && document.activeElement === first) {
                    event.preventDefault();
                    last.focus();
                } else if (!event.shiftKey && document.activeElement === last) {
                    event.preventDefault();
                    first.focus();
                }
            }
        });
        mobile.addEventListener('change', function () { if (!mobile.matches) { setOpen(false); } });
    }

    function initAuthForms() {
        document.querySelectorAll('[data-password-toggle]').forEach(function (button) {
            const input = document.getElementById(button.dataset.passwordToggle);
            if (!input) {
                return;
            }
            button.setAttribute('aria-pressed', 'false');
            button.addEventListener('click', function () {
                const reveal = input.type === 'password';
                input.type = reveal ? 'text' : 'password';
                button.setAttribute('aria-pressed', String(reveal));
                button.setAttribute('aria-label', reveal ? 'Ẩn mật khẩu' : 'Hiện mật khẩu');
                const icon = button.querySelector('i');
                if (icon) {
                    icon.classList.toggle('bi-eye', !reveal);
                    icon.classList.toggle('bi-eye-slash', reveal);
                }
            });
        });
        document.querySelectorAll('[data-register-form]').forEach(function (form) {
            const password = form.querySelector('[name="password"]');
            const confirm = form.querySelector('[data-password-confirm]');
            const error = form.querySelector('[data-password-match-error]');
            if (!password || !confirm) {
                return;
            }
            function update() {
                const mismatch = confirm.value && password.value !== confirm.value;
                const text = mismatch ? 'Mật khẩu nhập lại chưa khớp.' : '';
                confirm.setCustomValidity(text);
                if (error) {
                    error.textContent = text;
                }
            }
            password.addEventListener('input', update);
            confirm.addEventListener('input', update);
            update();
        });
    }

    function init() {
        initAdminNavigation();
        initConfirmations();
        initRouteEditors();
        initPromotions();
        initAdminSidebar();
        initAuthForms();
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
