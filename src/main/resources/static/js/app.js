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
    }

    function lockPaymentMethods(form) {
        form.querySelectorAll('input[name="method"]').forEach(function (input) {
            input.disabled = true;
        });
    }

    /*
     * Trang thanh toán: radio name="method" có data-action-url (URL POST) và data-method-label.
     * Đổi lựa chọn -> đổi action của form và nội dung pop-up xác nhận của nút thanh toán.
     */
    function initPaymentMethods() {
        document.querySelectorAll('[data-payment-form]').forEach(function (form) {
            const methods = Array.from(form.querySelectorAll('input[name="method"][data-action-url]'));
            const submit = form.querySelector('[data-payment-submit]');
            if (!methods.length) {
                return;
            }
            function update() {
                const selected = methods.find(function (input) {
                    return input.checked;
                }) || methods[0];
                form.action = selected.dataset.actionUrl;
                if (submit) {
                    submit.dataset.confirm = 'Thanh toán ' + (form.dataset.paymentAmount || '') + ' qua ' +
                        (selected.dataset.methodLabel || selected.value) + '?';
                }
            }
            methods.forEach(function (input) {
                input.addEventListener('change', update);
            });
            update();
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
            if (paymentForm) {
                lockPaymentMethods(paymentForm);
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
                    lockPaymentMethods(paymentForm);
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

    function initTicketQRCodes() {
        const qrElements = document.querySelectorAll('[data-qr-value]');
        if (!qrElements.length) {
            return;
        }

        function renderAll() {
            if (typeof QRCode === 'undefined') {
                return;
            }
            qrElements.forEach(function (el) {
                const val = el.getAttribute('data-qr-value');
                if (val && !el.querySelector('img, canvas, svg')) {
                    el.innerHTML = '';
                    new QRCode(el, {
                        text: val,
                        width: 88,
                        height: 88,
                        colorDark: '#0B3550',
                        colorLight: '#ffffff',
                        correctLevel: QRCode.CorrectLevel.M
                    });
                }
            });
        }

        if (typeof QRCode !== 'undefined') {
            renderAll();
        } else {
            window.addEventListener('load', renderAll);
        }
    }
    window.initTicketQRCodes = initTicketQRCodes;

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

    /*
     * Checkout vé ưu đãi: kiểm tra năm sinh từng hành khách (backend vẫn kiểm tra lại).
     * [data-passenger-section][data-ticket-code][data-travel-year] > input[data-birth-year] + [data-birth-year-error]
     */
    function initPassengerForm() {
        const minAgeByCode = { SENIOR_70: 70, PRIORITY: 13 };
        document.querySelectorAll('[data-passenger-section]').forEach(function (section) {
            const code = (section.dataset.ticketCode || '').toUpperCase();
            const minAge = minAgeByCode[code] || 0;
            const currentYear = new Date().getFullYear();
            const travelYear = Number(section.dataset.travelYear) || currentYear;
            section.querySelectorAll('input[data-birth-year]').forEach(function (input) {
                const error = input.parentElement.querySelector('[data-birth-year-error]');
                function check() {
                    const raw = input.value.trim();
                    if (!raw) {
                        return '';
                    }
                    const year = Number(raw);
                    if (!Number.isInteger(year) || year < 1900 || year > currentYear) {
                        return 'Năm sinh phải từ 1900 đến ' + currentYear + '.';
                    }
                    if (minAge && travelYear - year < minAge) {
                        return 'Loại vé này dành cho hành khách từ ' + minAge + ' tuổi (sinh năm ' +
                            (travelYear - minAge) + ' trở về trước).';
                    }
                    return '';
                }
                function show(message) {
                    if (error) {
                        error.textContent = message;
                    }
                    input.classList.toggle('is-invalid', Boolean(message));
                }
                if (error) {
                    error.id = error.id || input.id + 'Error';
                    input.setAttribute('aria-describedby', error.id);
                }
                input.addEventListener('input', function () {
                    const message = check();
                    input.setCustomValidity(message);
                    if (!message) {
                        show('');
                    }
                });
                input.addEventListener('blur', function () {
                    show(check());
                });
                input.addEventListener('invalid', function () {
                    show(check());
                });
                input.setCustomValidity(check());
            });
        });
    }

    function init() {
        initNavigation();
        initSearchForms();
        initPassengerForm();
        initPaymentMethods();
        initSeatSelection();
        initBackButton();
        initCountdown();
        initCopyButtons();
        initPrintButton();
        initTicketQRCodes();
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
        const path = window.location.pathname.replace(/\/+$/, '') || '/';
        const previewSections = {
            trips: 'trip', waterbuses: 'waterbus', stations: 'station',
            routes: 'route', promotions: 'promotion', 'ticket-types': 'tickettype',
            revenue: 'revenue', refunds: 'refund', accounts: 'account', checkin: 'checkin'
        };
        const links = Array.from(document.querySelectorAll('[data-admin-nav]'));
        // Đường dẫn con (VD /admin/accounts/5/edit) vẫn sáng mục cha; nhiều mục khớp thì lấy mục dài nhất.
        const matches = function (target) {
            const section = target.split('/').filter(Boolean).pop();
            return path === target || path.startsWith(target + '/') ||
                (previewSections[section] !== undefined &&
                    path.startsWith('/templates/admin/' + previewSections[section] + '/'));
        };
        const best = links.map(function (link) {
            return link.dataset.adminNav.replace(/\/+$/, '');
        }).filter(matches).sort(function (a, b) {
            return b.length - a.length;
        })[0];
        links.forEach(function (link) {
            const active = best !== undefined && link.dataset.adminNav.replace(/\/+$/, '') === best;
            link.classList.toggle('is-active', active);
            if (active) {
                link.setAttribute('aria-current', 'page');
            } else {
                link.removeAttribute('aria-current');
            }
        });
    }

    /*
     * Pop-up xác nhận dùng chung (modal "confirmModal" trong layout.html).
     * data-confirm="Nội dung"           đặt trên <form>, nút submit (ưu tiên nút) hoặc <a>
     * data-confirm-title="Tiêu đề"      mặc định "Xác nhận thao tác"
     * data-confirm-variant="danger"     nút Đồng ý màu đỏ
     * data-confirm-ok="Chữ trên nút"    mặc định "Đồng ý"
     */
    function initConfirmations() {
        const modalElement = document.querySelector('[data-confirm-modal]');
        const titleElement = modalElement && modalElement.querySelector('[data-confirm-modal-title]');
        const messageElement = modalElement && modalElement.querySelector('[data-confirm-modal-message]');
        const okButton = modalElement && modalElement.querySelector('[data-confirm-modal-ok]');
        const cancelButton = modalElement && modalElement.querySelector('[data-confirm-modal-cancel]');
        const hasModal = Boolean(modalElement && titleElement && messageElement && okButton &&
            window.bootstrap && window.bootstrap.Modal);
        const bypass = new WeakSet();
        let pendingAction = null;
        let focusDanger = false;

        function isSubmitControl(control) {
            return (control.tagName === 'BUTTON' || control.tagName === 'INPUT') &&
                control.type === 'submit' && Boolean(control.form);
        }

        function readOption(source, fallback, name) {
            if (source && source.hasAttribute(name)) {
                return source.getAttribute(name);
            }
            return fallback && fallback.hasAttribute(name) ? fallback.getAttribute(name) : null;
        }

        function ask(source, fallback, action) {
            if (!hasModal) {
                action();
                return;
            }
            const danger = readOption(source, fallback, 'data-confirm-variant') === 'danger';
            titleElement.textContent = readOption(source, fallback, 'data-confirm-title') || 'Xác nhận thao tác';
            messageElement.textContent = source.getAttribute('data-confirm');
            okButton.textContent = readOption(source, fallback, 'data-confirm-ok') || 'Đồng ý';
            okButton.classList.toggle('btn-wb-accent', !danger);
            okButton.classList.toggle('btn-wb-danger', danger);
            focusDanger = danger;
            pendingAction = action;
            window.bootstrap.Modal.getOrCreateInstance(modalElement).show();
        }

        function lockSubmit(form, submitter) {
            const button = submitter || form.querySelector('button[type="submit"], input[type="submit"]');
            form.classList.add('is-submitting');
            form.setAttribute('aria-busy', 'true');
            if (!button) {
                return;
            }
            // Khoá sau khi trình duyệt đã lấy dữ liệu form, để nút bấm vẫn gửi name/value của nó.
            window.setTimeout(function () {
                button.disabled = true;
                if (button.tagName === 'BUTTON' && !button.querySelector('[data-confirm-spinner]')) {
                    const spinner = document.createElement('span');
                    spinner.className = 'spinner-border spinner-border-sm me-2';
                    spinner.setAttribute('aria-hidden', 'true');
                    spinner.setAttribute('data-confirm-spinner', '');
                    button.prepend(spinner);
                }
            }, 0);
        }

        if (hasModal) {
            okButton.addEventListener('click', function () {
                const action = pendingAction;
                pendingAction = null;
                window.bootstrap.Modal.getOrCreateInstance(modalElement).hide();
                if (action) {
                    action();
                }
            });
            modalElement.addEventListener('shown.bs.modal', function () {
                (focusDanger && cancelButton ? cancelButton : okButton).focus();
            });
            modalElement.addEventListener('hidden.bs.modal', function () {
                pendingAction = null;
            });
        }

        document.addEventListener('submit', function (event) {
            const form = event.target;
            if (!(form instanceof HTMLFormElement) || event.defaultPrevented) {
                return;
            }
            const submitter = event.submitter && event.submitter.form === form ? event.submitter : null;
            if (form.classList.contains('is-submitting')) {
                event.preventDefault();
                return;
            }
            if (bypass.has(form)) {
                bypass.delete(form);
                lockSubmit(form, submitter);
                return;
            }
            const source = submitter && submitter.hasAttribute('data-confirm') ? submitter :
                (form.hasAttribute('data-confirm') ? form : null);
            if (!source) {
                return;
            }
            event.preventDefault();
            ask(source, form, function () {
                bypass.add(form);
                if (typeof form.requestSubmit === 'function') {
                    form.requestSubmit(submitter || undefined);
                } else {
                    form.submit();
                }
                bypass.delete(form);
            });
        });

        document.addEventListener('click', function (event) {
            const control = event.target.closest('[data-confirm]');
            if (!control || control.tagName === 'FORM' || isSubmitControl(control) ||
                control.closest('[data-confirm-modal]')) {
                return;
            }
            if (bypass.has(control)) {
                bypass.delete(control);
                return;
            }
            event.preventDefault();
            ask(control, null, function () {
                bypass.add(control);
                control.click();
                bypass.delete(control);
            });
        });

        // Quay lại trang bằng nút Back (bfcache): mở khoá form đã gửi trước đó.
        window.addEventListener('pageshow', function (event) {
            if (!event.persisted) {
                return;
            }
            document.querySelectorAll('form.is-submitting').forEach(function (form) {
                form.classList.remove('is-submitting');
                form.removeAttribute('aria-busy');
                form.querySelectorAll('[data-confirm-spinner]').forEach(function (spinner) {
                    const button = spinner.closest('button');
                    spinner.remove();
                    if (button) {
                        button.disabled = false;
                    }
                });
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
        document.querySelectorAll('[data-demo-login]').forEach(function (button) {
            button.addEventListener('click', function () {
                const form = button.closest('.wb-card').querySelector('form');
                const email = form && form.querySelector('[name="email"]');
                const password = form && form.querySelector('[name="password"]');
                if (!email || !password) {
                    return;
                }
                email.value = button.dataset.email;
                password.value = button.dataset.password;
                if (button.dataset.demoLogin === 'submit') {
                    form.submit();
                } else {
                    password.focus();
                }
            });
        });
    }

    /*
     * Màn soát vé (STAFF) - hợp đồng data-attribute ở docs/PAGES.md "Soát vé – hợp đồng JS".
     * [data-checkin-form] > [data-checkin-input]; [data-qr-start] + [data-qr-reader];
     * [data-checkin-preview] (modal xem trước, nút [data-checkin-confirm]);
     * [data-checkin-result][data-result="success|error"][data-message].
     */
    function initCheckin() {
        const form = document.querySelector('[data-checkin-form]');
        const input = document.querySelector('[data-checkin-input]');
        if (!form || !input) {
            return;
        }
        const DEDUP_MS = 3000;
        const RESULT_MS = 2000;
        const dedupKey = 'wb-checkin-last';
        const previewElement = document.querySelector('[data-checkin-preview]');
        let resultShowing = false;

        function normalize(value) {
            return String(value || '').replace(/\s+/g, '').toUpperCase();
        }
        function focusInput() {
            if (!previewElement || !previewElement.classList.contains('show')) {
                input.focus({ preventScroll: true });
            }
        }
        function readLast() {
            try {
                return JSON.parse(window.sessionStorage.getItem(dedupKey) || 'null');
            } catch (e) {
                return null;
            }
        }
        function writeLast(code) {
            try {
                window.sessionStorage.setItem(dedupKey, JSON.stringify({ code: code, at: Date.now() }));
            } catch (e) {
                // Trình duyệt chặn storage: bỏ qua, vẫn chặn trùng trong trang bằng biến.
            }
        }
        let lastInPage = null;

        // Ô nhập: viết hoa, bỏ khoảng trắng; máy quét USB/Bluetooth gõ như bàn phím rồi Enter -> form tự gửi.
        input.setAttribute('autocomplete', 'off');
        input.setAttribute('autocapitalize', 'characters');
        input.setAttribute('spellcheck', 'false');
        input.addEventListener('input', function () {
            const clean = normalize(input.value);
            if (clean !== input.value) {
                const end = clean.length;
                input.value = clean;
                input.setSelectionRange(end, end);
            }
        });
        form.addEventListener('submit', function (event) {
            const code = normalize(input.value);
            input.value = code;
            if (!code) {
                event.preventDefault();
                focusInput();
                return;
            }
            const last = lastInPage || readLast();
            if (last && last.code === code && Date.now() - last.at < DEDUP_MS) {
                event.preventDefault();
                input.select();
                return;
            }
            lastInPage = { code: code, at: Date.now() };
            writeLast(code);
        });

        // Âm báo bằng Web Audio API: thành công 2 tiếng cao, lỗi 1 tiếng trầm dài.
        function beep(success) {
            try {
                const AudioCtx = window.AudioContext || window.webkitAudioContext;
                if (!AudioCtx) {
                    return;
                }
                const ctx = new AudioCtx();
                const tones = success ? [[880, 0, 0.12], [1320, 0.15, 0.16]] : [[220, 0, 0.45]];
                const play = function () {
                    tones.forEach(function (tone) {
                        const osc = ctx.createOscillator();
                        const gain = ctx.createGain();
                        osc.type = success ? 'sine' : 'square';
                        osc.frequency.value = tone[0];
                        gain.gain.setValueAtTime(0.0001, ctx.currentTime + tone[1]);
                        gain.gain.exponentialRampToValueAtTime(0.25, ctx.currentTime + tone[1] + 0.01);
                        gain.gain.exponentialRampToValueAtTime(0.0001, ctx.currentTime + tone[1] + tone[2]);
                        osc.connect(gain).connect(ctx.destination);
                        osc.start(ctx.currentTime + tone[1]);
                        osc.stop(ctx.currentTime + tone[1] + tone[2] + 0.02);
                    });
                    window.setTimeout(function () { ctx.close(); }, 1000);
                };
                if (ctx.state === 'suspended') {
                    ctx.resume().then(play).catch(function () {});
                } else {
                    play();
                }
            } catch (e) {
                // Không phát được âm thanh thì chỉ hiển thị.
            }
        }

        // Kết quả soát vé: khung to giữa màn hình, tự ẩn sau 2 giây.
        const resultElement = document.querySelector('[data-checkin-result]');
        if (resultElement) {
            const success = resultElement.dataset.result === 'success';
            const message = resultElement.dataset.message || resultElement.textContent.trim() ||
                (success ? 'Soát vé thành công' : 'Vé không hợp lệ');
            const flash = document.createElement('div');
            flash.className = 'wb-checkin-flash ' + (success ? 'is-success' : 'is-error');
            flash.setAttribute('role', 'alert');
            flash.setAttribute('data-checkin-flash', '');
            const icon = document.createElement('i');
            icon.className = 'bi ' + (success ? 'bi-check-circle-fill' : 'bi-x-octagon-fill');
            icon.setAttribute('aria-hidden', 'true');
            const title = document.createElement('strong');
            title.textContent = success ? 'HỢP LỆ' : 'KHÔNG HỢP LỆ';
            const text = document.createElement('p');
            text.textContent = message;
            flash.append(icon, title, text);
            document.body.appendChild(flash);
            resultShowing = true;
            beep(success);
            if (navigator.vibrate) {
                navigator.vibrate(success ? 120 : [200, 100, 200]);
            }
            input.value = '';
            const hide = function () {
                if (!resultShowing) {
                    return;
                }
                resultShowing = false;
                flash.classList.add('is-hiding');
                window.setTimeout(function () { flash.remove(); }, 200);
                focusInput();
            };
            const timer = window.setTimeout(hide, RESULT_MS);
            flash.addEventListener('click', function () {
                window.clearTimeout(timer);
                hide();
            });
            // Lượt kế tiếp bắt đầu (gõ/quét mã mới) thì ẩn ngay.
            input.addEventListener('input', function () {
                window.clearTimeout(timer);
                hide();
            }, { once: true });
        }

        // Modal xem trước do server render: tự mở, nút xác nhận tự focus (bấm 1 lần hoặc Enter).
        if (previewElement && window.bootstrap && window.bootstrap.Modal) {
            const confirmButton = previewElement.querySelector('[data-checkin-confirm]') ||
                previewElement.querySelector('button[type="submit"]');
            previewElement.addEventListener('shown.bs.modal', function () {
                if (confirmButton) {
                    confirmButton.focus();
                }
            });
            previewElement.addEventListener('hidden.bs.modal', function () {
                input.value = '';
                input.focus();
            });
            window.bootstrap.Modal.getOrCreateInstance(previewElement).show();
        }

        // Quét QR bằng camera sau của điện thoại (thư viện html5-qrcode, nạp qua fragment scriptsCheckin).
        const qrStart = document.querySelector('[data-qr-start]');
        const qrReader = document.querySelector('[data-qr-reader]');
        if (qrStart && qrReader) {
            qrReader.id = qrReader.id || 'wbQrReader';
            qrReader.hidden = true;
            let message = document.querySelector('[data-qr-message]');
            if (!message) {
                message = document.createElement('p');
                message.className = 'wb-qr-message';
                message.setAttribute('data-qr-message', '');
                message.setAttribute('aria-live', 'polite');
                qrReader.after(message);
            }
            let stopButton = document.querySelector('[data-qr-stop]');
            if (!stopButton) {
                stopButton = document.createElement('button');
                stopButton.type = 'button';
                stopButton.className = 'btn btn-wb-outline wb-qr-stop';
                stopButton.setAttribute('data-qr-stop', '');
                stopButton.innerHTML = '<i class="bi bi-camera-video-off me-2" aria-hidden="true"></i>Tắt camera';
                message.after(stopButton);
            }
            stopButton.hidden = true;
            let scanner = null;
            let scanning = false;

            function setMessage(text, isError) {
                message.textContent = text || '';
                message.classList.toggle('is-error', Boolean(isError));
            }
            function stopCamera() {
                const done = function () {
                    scanning = false;
                    qrReader.hidden = true;
                    stopButton.hidden = true;
                    qrStart.hidden = false;
                };
                let stopping = Promise.resolve();
                if (scanner && scanning) {
                    try {
                        stopping = scanner.stop().then(function () {
                            scanner.clear();
                        });
                    } catch (e) {
                        // Camera chưa kịp chạy: không có gì để dừng.
                    }
                }
                return stopping.catch(function () {}).then(done);
            }
            function cameraError(error) {
                const name = error && (error.name || String(error));
                if (/NotAllowed|Permission/i.test(name)) {
                    return 'Chưa có quyền dùng camera. Hãy bấm biểu tượng ổ khoá trên thanh địa chỉ, cho phép Camera rồi thử lại.';
                }
                if (/NotFound|DevicesNotFound|OverconstrainedError/i.test(name)) {
                    return 'Không tìm thấy camera trên thiết bị này. Hãy nhập mã vé bằng tay.';
                }
                if (/NotReadable|TrackStart/i.test(name)) {
                    return 'Camera đang được ứng dụng khác sử dụng. Hãy đóng ứng dụng đó rồi thử lại.';
                }
                return 'Không mở được camera. Hãy nhập mã vé bằng tay.';
            }

            qrStart.addEventListener('click', function () {
                if (scanning) {
                    return;
                }
                if (!window.isSecureContext) {
                    setMessage('Camera chỉ hoạt động khi mở trang qua HTTPS hoặc http://localhost. Hãy nhập mã vé bằng tay.', true);
                    return;
                }
                if (typeof window.Html5Qrcode === 'undefined') {
                    setMessage('Chưa nạp thư viện quét QR (trang cần dùng fragment scriptsCheckin).', true);
                    return;
                }
                if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
                    setMessage('Trình duyệt này không hỗ trợ camera. Hãy nhập mã vé bằng tay.', true);
                    return;
                }
                qrReader.hidden = false;
                stopButton.hidden = false;
                qrStart.hidden = true;
                setMessage('Đưa mã QR trên vé vào giữa khung hình.');
                scanner = scanner || new window.Html5Qrcode(qrReader.id);
                scanning = true;
                scanner.start({ facingMode: 'environment' }, { fps: 10, qrbox: { width: 240, height: 240 } },
                    function (decodedText) {
                        const code = normalize(decodedText);
                        if (!code) {
                            return;
                        }
                        stopCamera().then(function () {
                            input.value = code;
                            setMessage('');
                            if (typeof form.requestSubmit === 'function') {
                                form.requestSubmit();
                            } else {
                                form.submit();
                            }
                        });
                    },
                    function () {
                        // Khung hình chưa có QR: bỏ qua.
                    }).catch(function (error) {
                    stopCamera();
                    setMessage(cameraError(error), true);
                });
            });
            stopButton.addEventListener('click', function () {
                stopCamera().then(function () {
                    setMessage('');
                    focusInput();
                });
            });
        }

        if (!previewElement) {
            focusInput();
        }
    }

    function init() {
        initAdminNavigation();
        initConfirmations();
        initCheckin();
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
