// Hợp đồng KH1/TI4: chạy bằng Playwright sẵn có, không cần sửa template của Trí.
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const assert = require('node:assert/strict');
const path = require('node:path');

function row(station, minutes) {
    return `<div data-route-row><span data-route-order></span>
        <select name="stationIds"><option value="${station}">${station}</option></select>
        <input name="minutesFromStart" type="number" min="0" step="1" required value="${minutes}">
        <button type="button" data-route-up>Up</button><button type="button" data-route-down>Down</button>
        <button type="button" data-route-remove>Remove</button></div>`;
}

(async () => {
    const browser = await chromium.launch({ headless: true, executablePath: process.env.CHROMIUM_PATH });
    try {
        const page = await browser.newPage();
        const errors = [];
        page.on('pageerror', error => errors.push(error.message));
        await page.setContent(`<form><fieldset data-route-editor><div data-route-rows>
            ${row('A', 0)}${row('B', 15)}${row('C', 35)}</div>
            <template data-route-row-template>${row('D', '')}</template>
            <button type="button" data-route-add>Add</button><p data-route-message></p></fieldset></form>`);
        await page.addScriptTag({ path: path.resolve('src/main/resources/static/js/app.js') });
        const minutes = page.locator('form input[name="minutesFromStart"]');
        const valid = () => page.locator('form').evaluate(form => form.checkValidity());
        assert.equal(await valid(), true);

        // Sửa số phút: cả phần tử đầu và quan hệ với bến trước đều được kiểm tra lại.
        await minutes.nth(1).fill('0');
        assert.equal(await valid(), false);
        await minutes.nth(1).fill('20');
        assert.equal(await valid(), true);
        await minutes.nth(0).fill('5');
        assert.equal(await valid(), false);
        await minutes.nth(0).fill('0');

        // Đảo bến phải giữ cặp bến-phút, không tự sửa giờ để che dữ liệu sai.
        await page.locator('[data-route-row]').nth(2).locator('[data-route-up]').click();
        assert.deepEqual(await page.locator('form select').evaluateAll(items => items.map(item => item.value)), ['A', 'C', 'B']);
        assert.deepEqual(await minutes.evaluateAll(items => items.map(item => item.value)), ['0', '35', '20']);
        assert.equal(await valid(), false);
        await page.locator('[data-route-row]').nth(1).locator('[data-route-down]').click();
        assert.equal(await valid(), true);

        await page.locator('[data-route-add]').click();
        assert.equal(await valid(), false);
        await minutes.nth(3).fill('50');
        assert.equal(await valid(), true);
        await page.locator('[data-route-row]').nth(3).locator('[data-route-remove]').click();
        assert.equal(await valid(), true);
        assert.deepEqual(errors, []);
        console.log('Route minutes: validation, reorder, add and remove passed.');
    } finally {
        await browser.close();
    }
})().catch(error => { console.error(error); process.exitCode = 1; });
