const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const assert = require('node:assert/strict');
const path = require('node:path');
const fs = require('node:fs');

(async () => {
    const browser = await chromium.launch({ headless: true, executablePath: process.env.CHROMIUM_PATH });
    const errors = [];
    async function load(html) {
        const page = await browser.newPage();
        page.on('pageerror', error => errors.push(error.message));
        await page.clock.install({ time: new Date('2026-11-13T12:00:00') });
        await page.setContent(html);
        await page.addScriptTag({ path: path.resolve('src/main/resources/static/js/app.js') });
        return page;
    }
    try {
        for (const [code, year, valid] of [['PRIORITY', '2020', true], ['SENIOR_70', '1957', false],
                ['SENIOR_70', '1956', true], ['PRIORITY', '2027', false], ['PRIORITY', '1899', false]]) {
            const page = await load(`<form><section data-passenger-section data-ticket-code="${code}" data-travel-year="2026">
                <div><input data-birth-year id="birth" value="${year}" required><span data-birth-year-error></span></div>
                </section></form>`);
            assert.equal(await page.locator('#birth').evaluate(input => input.checkValidity()), valid, code + '/' + year);
            await page.close();
        }
        for (const deadline of ['2026-11-13T12:00:02', 'invalid', '2026-11-13T11:59:00']) {
            const page = await load(`<div data-countdown data-expires-at="${deadline}"><span data-countdown-time></span></div>
                <form data-payment-form><button data-payment-submit>VNPay</button><p data-payment-expired></p></form>
                <form data-payment-form><button data-payment-submit>Cash</button><p data-payment-expired></p></form>`);
            if (deadline.endsWith('12:00:02')) {
                assert.deepEqual(await page.locator('button').evaluateAll(items => items.map(item => item.disabled)), [false, false]);
                await page.clock.fastForward(3000);
            }
            assert.deepEqual(await page.locator('button').evaluateAll(items => items.map(item => item.disabled)), [true, true]);
            assert.deepEqual(await page.locator('form').evaluateAll(forms => forms.map(form => {
                const event = new Event('submit', { bubbles: true, cancelable: true });
                form.dispatchEvent(event);
                return event.defaultPrevented;
            })), [true, true]);
            await page.close();
        }

        const verify = await browser.newPage();
        verify.on('pageerror', error => errors.push(error.message));
        await verify.route('**/*', route => route.abort());
        await verify.setContent(fs.readFileSync('target/be1-preview/verify.html', 'utf8'));
        for (const file of ['bootstrap.min.css', 'app.css']) {
            await verify.addStyleTag({ path: path.resolve('src/main/resources/static/css', file) });
        }
        for (const width of [360, 1280]) {
            await verify.setViewportSize({ width, height: 900 });
            assert(await verify.evaluate(() => document.documentElement.scrollWidth <= innerWidth));
        }
        const phone = verify.locator('#contactPhone');
        for (const [value, valid] of [['0900000000', true], [' 0900000000 ', true],
                ['090 000 0000', false], ['   ', false], ['090000000', false]]) {
            await phone.fill(value);
            assert.equal(await phone.evaluate(input => input.checkValidity()), valid, 'Phone: ' + value);
        }
        assert.deepEqual(errors, []);
        console.log('Customer rules: passenger age, both payment locks and verification layout passed.');
    } finally {
        await browser.close();
    }
})().catch(error => { console.error(error); process.exitCode = 1; });
