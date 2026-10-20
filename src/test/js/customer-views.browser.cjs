// Run after mvnw test. Uses an existing Playwright installation, with no project dependency changes.
// PLAYWRIGHT_MODULE may point to playwright-core when it is installed outside this repository.
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const http = require('node:http');

const preview = path.resolve('target/be1-preview');
const assets = path.resolve('src/main/resources/static');
const server = http.createServer((request, response) => {
    const pathname = new URL(request.url, 'http://localhost').pathname;
    const asset = /^\/(css|js|img)\//.test(pathname);
    const root = asset ? assets : preview;
    const file = path.resolve(root, '.' + pathname);
    if (!file.startsWith(root + path.sep) || !fs.existsSync(file) || !fs.statSync(file).isFile()) {
        response.writeHead(404).end();
        return;
    }
    const types = { '.css': 'text/css', '.js': 'text/javascript', '.svg': 'image/svg+xml', '.html': 'text/html; charset=utf-8' };
    response.setHeader('Content-Type', types[path.extname(file)] || 'application/octet-stream');
    response.end(fs.readFileSync(file));
});

(async function () {
    let browser;
    try {
        await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
        const base = 'http://127.0.0.1:' + server.address().port;
        browser = await chromium.launch({ headless: true });
        const page = await browser.newPage();
        const errors = [];
        page.on('pageerror', error => errors.push(error.message));
        // These checks exercise local assets; external fonts/icons are not required.
        await page.route(/^https:\/\//, route => route.abort());
        for (const width of [360, 1280]) {
            await page.setViewportSize({ width, height: 900 });
            for (const file of ['home', 'checkout-PENDING', 'payment-PENDING', 'detail-PAID', 'history-PAID']) {
                await page.goto(base + '/' + file + '.html');
                assert(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), file + ' overflows at ' + width);
                if (file === 'history-PAID') {
                    assert.equal(await page.locator('.wb-booking-cards').isVisible(), width === 360);
                    assert.equal(await page.locator('.wb-booking-table-wrap').isVisible(), width === 1280);
                    const visibleOrder = width === 360 ? '.wb-booking-cards' : '.wb-booking-table-wrap';
                    assert.match(await page.locator(visibleOrder).innerText(), /REAL-42/);
                    assert.match(await page.locator(visibleOrder).innerText(), /42.000/);
                }
                await page.screenshot({ path: path.join(preview, file + '-' + width + '.png'), fullPage: true });
            }
        }

        await page.goto(base + '/home.html');
        const options = await page.locator('#fromStationId option[value]:not([value=""])').evaluateAll(items => items.map(item => item.value));
        assert.equal(options.length, 2);
        await page.selectOption('#fromStationId', options[0]);
        await page.selectOption('#toStationId', options[1]);
        await page.click('[data-swap-stations]');
        assert.equal(await page.inputValue('#fromStationId'), options[1]);
        assert.equal(await page.inputValue('#toStationId'), options[0]);
        await page.selectOption('#toStationId', options[1]);
        const prevented = await page.locator('[data-trip-search]').evaluate(form => {
            const event = new Event('submit', { bubbles: true, cancelable: true });
            form.dispatchEvent(event);
            return event.defaultPrevented;
        });
        assert(prevented, 'Identical stations must prevent submission');
        assert(await page.locator('[data-search-error]').isVisible());

        // Freeze the browser clock so fixtures stay reproducible after the Maven run.
        await page.clock.install({ time: new Date('2026-10-20T00:00:00+07:00') });
        for (const state of ['PENDING', 'PAID', 'EXPIRED', 'CANCELLED']) {
            await page.goto(base + '/payment-' + state + '.html');
            assert.equal(await page.locator('[data-payment-submit]').isDisabled(), state !== 'PENDING');
            await page.clock.fastForward(2000);
            assert.equal(await page.locator('[data-payment-submit]').isDisabled(), state !== 'PENDING');
            assert(!(await page.locator('body').innerText()).includes('NaN'));
        }
        for (const value of ['missing', 'invalid', 'elapsed']) {
            await page.goto(base + '/payment-' + value + '.html');
            if (value === 'elapsed') {
                const expires = await page.locator('[data-countdown]').getAttribute('data-expires-at');
                await page.clock.setSystemTime(new Date(new Date(expires).getTime() + 60000));
            }
            await page.clock.fastForward(2000);
            assert(await page.locator('[data-payment-submit]').isDisabled(), value + ' deadline must disable payment');
            assert(!(await page.locator('[data-countdown-time]').innerText()).includes('NaN'));
        }
        assert.deepEqual(errors, []);
        console.log('PASS: rendered fixtures at 360/1280px, history layouts, swap/same-station guard, payment status/deadline guards; no page errors.');
    } finally {
        if (browser) await browser.close();
        await new Promise(resolve => server.close(resolve));
    }
})().catch(error => { console.error(error); process.exitCode = 1; });
