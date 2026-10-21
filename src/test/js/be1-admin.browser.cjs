// Run after mvnw test; reuses an existing Playwright installation without adding dependencies.
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const http = require('node:http');
const output = path.resolve('target/be1-admin-preview');
const roots = { m3: output, m1: path.resolve('target/be1-preview'), assets: path.resolve('src/main/resources/static') };
const server = http.createServer((request, response) => {
    const pathname = new URL(request.url, 'http://localhost').pathname;
    const asset = /^\/(css|js|img)\//.test(pathname);
    const root = asset ? roots.assets : roots[pathname.split('/')[1]];
    if (!root) return response.writeHead(404).end();
    const relative = asset ? pathname : pathname.replace(/^\/(m1|m3)/, '');
    const file = path.resolve(root, '.' + relative);
    if (!file.startsWith(root + path.sep) || !fs.existsSync(file) || !fs.statSync(file).isFile()) {
        return response.writeHead(404).end();
    }
    const types = { '.css': 'text/css', '.js': 'text/javascript', '.svg': 'image/svg+xml', '.html': 'text/html; charset=utf-8' };
    response.setHeader('Content-Type', types[path.extname(file)] || 'application/octet-stream');
    response.end(fs.readFileSync(file));
});

(async () => {
    let browser;
    try {
        await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
        const base = 'http://127.0.0.1:' + server.address().port;
        browser = await chromium.launch({ headless: true });
        const page = await browser.newPage();
        await page.route(/^https:\/\//, route => route.abort());
        const errors = [];
        page.on('pageerror', error => errors.push(error.message));
        const pages = [
            'm1/home', 'm1/checkout-PENDING', 'm1/payment-PENDING', 'm1/detail-PAID', 'm1/history-PAID',
            'm3/routes', 'm3/routes-empty', 'm3/stations', 'm3/station', 'm3/error404', 'm3/error500',
            'm3/admin-station-list', 'm3/admin-station-edit', 'm3/admin-route-list', 'm3/admin-route-edit'
        ];
        for (const width of [360, 768, 1280]) {
            await page.setViewportSize({ width, height: 900 });
            for (const name of pages) {
                await page.goto(base + '/' + name + '.html');
                assert(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), name + ' overflows at ' + width);
                await page.screenshot({ path: path.join(output, name.replace('/', '-') + '-' + width + '.png'), fullPage: true });
            }
            await page.goto(base + '/m3/routes.html');
            const toggle = page.locator('.navbar-toggler');
            if (width < 992) {
                await toggle.click();
                await page.locator('a[href="/auth/login"]').waitFor({ state: 'visible' });
                await toggle.click();
            }
        }
        await page.goto(base + '/m3/stations.html');
        const filter = page.locator('[data-station-filter]');
        await filter.fill('Bến A');
        assert.equal(await page.locator('[data-station-grid] > div:visible').count(), 1);
        assert.match(await page.locator('[data-station-count]').innerText(), /1/);
        await filter.fill('no-station-matches');
        assert.equal(await page.locator('[data-station-grid] > div:visible').count(), 0);
        assert(await page.locator('[data-station-empty]').isVisible());
        await filter.fill('');
        assert.equal(await page.locator('[data-station-grid] > div:visible').count(), 2);

        await page.goto(base + '/m3/error500.html');
        await Promise.all([page.waitForEvent('load'), page.locator('[data-action="reload"]').click()]);
        assert(await page.locator('[data-action="reload"]').isVisible());
        assert.deepEqual(errors, []);
        console.log('PASS: 15 BE1 pages at 360/768/1280px, no overflow/page errors, navbar, station filtering, 500 reload.');
    } finally {
        if (browser) await browser.close();
        await new Promise(resolve => server.close(resolve));
    }
})().catch(error => { console.error(error); process.exitCode = 1; });
