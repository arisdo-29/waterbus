const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const http = require('node:http');

const assets = path.resolve('src/main/resources/static');
const output = path.resolve('target/checkin-preview');
fs.mkdirSync(output, { recursive: true });
const requests = [];

function page(body) {
    return '<!doctype html><html lang="vi"><head><meta charset="utf-8">' +
        '<meta name="viewport" content="width=device-width, initial-scale=1">' +
        '<link rel="stylesheet" href="/css/bootstrap.min.css"><link rel="stylesheet" href="/css/bootstrap-icons.min.css">' +
        '<link rel="stylesheet" href="/css/app.css"></head><body><main class="container py-4">' +
        '<form data-checkin-form method="get" action="/admin/checkin" class="d-flex gap-2">' +
        '<input class="form-control" data-checkin-input name="ticketCode" aria-label="Mã vé hoặc mã đơn">' +
        '<button class="btn btn-primary" type="submit">Tra mã</button></form>' +
        '<button class="btn btn-wb-outline w-100 mt-3" type="button" data-qr-start>Quét QR</button>' +
        '<div data-qr-reader></div>' + body + '</main>' +
        '<script src="/js/html5-qrcode.min.js"></script><script src="/js/bootstrap.bundle.min.js"></script>' +
        '<script src="/js/app.js"></script></body></html>';
}

function preview(code) {
    return '<div class="modal fade" tabindex="-1" data-checkin-preview aria-labelledby="pvTitle">' +
        '<div class="modal-dialog modal-dialog-centered"><div class="modal-content">' +
        '<div class="modal-header"><h2 class="modal-title fs-5" id="pvTitle">Vé ' + code + '</h2></div>' +
        '<div class="modal-body">Ghế B3 · Bạch Đằng → Thanh Đa</div>' +
        '<form method="post" action="/admin/checkin" class="modal-footer">' +
        '<input type="hidden" name="ticketCode" value="' + code + '">' +
        '<button type="button" class="btn btn-wb-outline" data-bs-dismiss="modal">Huỷ</button>' +
        '<button type="submit" class="btn btn-wb-accent" data-checkin-confirm>Xác nhận lên tàu</button>' +
        '</form></div></div></div>';
}

const server = http.createServer((request, response) => {
    const url = new URL(request.url, 'http://localhost');
    if (/^\/(css|js|img)\//.test(url.pathname)) {
        const file = path.resolve(assets, '.' + url.pathname);
        if (!file.startsWith(assets + path.sep) || !fs.existsSync(file)) {
            response.writeHead(404).end();
            return;
        }
        const types = { '.css': 'text/css', '.js': 'text/javascript' };
        response.setHeader('Content-Type', types[path.extname(file)] || 'application/octet-stream');
        response.end(fs.readFileSync(file));
        return;
    }
    let body = '';
    request.on('data', chunk => { body += chunk; });
    request.on('end', () => {
        response.setHeader('Content-Type', 'text/html; charset=utf-8');
        if (request.method === 'POST') {
            const code = new URLSearchParams(body).get('ticketCode') || '';
            requests.push('POST ' + code);
            const ok = code.startsWith('TK');
            response.end(page('<div data-checkin-result data-result="' + (ok ? 'success' : 'error') +
                '" data-message="' + (ok ? 'Vé ' + code + ' - ghế B3, mời lên tàu' : 'Mã ' + code + ' không tồn tại') + '"></div>'));
            return;
        }
        const code = url.searchParams.get('ticketCode');
        if (code !== null) {
            requests.push('GET ' + code);
        }
        response.end(page(code ? preview(code) : ''));
    });
});

async function makeQrVideo(browser, text) {
    const tab = await browser.newPage();
    await tab.setContent('<div id="q"></div>');
    await tab.addScriptTag({ path: path.join(assets, 'js/qrcode.min.js') });
    const gray = await tab.evaluate(value => {
        new QRCode(document.getElementById('q'), { text: value, width: 360, height: 360, correctLevel: QRCode.CorrectLevel.M });
        const qr = document.querySelector('#q canvas');
        const canvas = document.createElement('canvas');
        canvas.width = 640;
        canvas.height = 480;
        const ctx = canvas.getContext('2d');
        ctx.fillStyle = '#fff';
        ctx.fillRect(0, 0, 640, 480);
        ctx.drawImage(qr, 140, 60);
        const data = ctx.getImageData(0, 0, 640, 480).data;
        const result = [];
        for (let i = 0; i < data.length; i += 4) {
            result.push(Math.round(0.299 * data[i] + 0.587 * data[i + 1] + 0.114 * data[i + 2]));
        }
        return result;
    }, text);
    await tab.close();
    const frame = Buffer.concat([Buffer.from('FRAME\n'), Buffer.from(gray), Buffer.alloc(640 * 480 / 2, 128)]);
    const file = path.join(os.tmpdir(), 'wb-checkin-qr.y4m');
    fs.writeFileSync(file, Buffer.concat([Buffer.from('YUV4MPEG2 W640 H480 F10:1 Ip A1:1 C420jpeg\n'), frame, frame, frame]));
    return file;
}

async function countBeeps(page) {
    return page.evaluate(() => window.__beeps || 0);
}

(async function () {
    let browser;
    let cameraBrowser;
    const launch = args => chromium.launch({ headless: true, executablePath: process.env.CHROMIUM_PATH || undefined, args });
    try {
        await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
        const port = server.address().port;
        const base = 'http://localhost:' + port;
        browser = await launch(['--host-resolver-rules=MAP wb-insecure.test 127.0.0.1', '--autoplay-policy=no-user-gesture-required',
            '--use-fake-ui-for-media-stream', '--use-fake-device-for-media-stream']);
        const context = await browser.newContext({ viewport: { width: 360, height: 740 } });
        await context.addInitScript(() => {
            const original = window.AudioContext && window.AudioContext.prototype.createOscillator;
            if (original) {
                window.AudioContext.prototype.createOscillator = function () {
                    window.__beeps = (window.__beeps || 0) + 1;
                    return original.apply(this, arguments);
                };
            }
        });
        const tab = await context.newPage();
        const errors = [];
        tab.on('pageerror', error => errors.push(error.message));
        tab.on('dialog', dialog => { errors.push('native dialog ' + dialog.message()); dialog.dismiss(); });
        const focusedIs = selector => tab.evaluate(s => document.activeElement === document.querySelector(s), selector);

        await tab.goto(base + '/admin/checkin');
        assert(await focusedIs('[data-checkin-input]'), 'input focused on load');
        await tab.keyboard.type(' tk 0001 ', { delay: 5 });
        assert.equal(await tab.inputValue('[data-checkin-input]'), 'TK0001');
        await Promise.all([tab.waitForURL(/ticketCode=TK0001/), tab.keyboard.press('Enter')]);
        assert.deepEqual(requests, ['GET TK0001']);

        await tab.locator('[data-checkin-preview]').waitFor({ state: 'visible' });
        await tab.waitForFunction(() => document.activeElement && document.activeElement.hasAttribute('data-checkin-confirm'));
        await tab.screenshot({ path: path.join(output, 'preview-360.png') });

        await tab.waitForTimeout(300);
        await tab.keyboard.press('Escape');
        await tab.locator('[data-checkin-preview]').waitFor({ state: 'hidden' });
        await tab.waitForFunction(() => document.activeElement === document.querySelector('[data-checkin-input]'));
        assert.equal(await tab.inputValue('[data-checkin-input]'), '');
        await tab.keyboard.type('TK0001');
        await tab.keyboard.press('Enter');
        await tab.waitForTimeout(400);
        assert.deepEqual(requests, ['GET TK0001'], 'same code within 3s is not sent again');
        await tab.waitForTimeout(2700);
        await Promise.all([tab.waitForURL(/ticketCode=TK0001/), tab.keyboard.press('Enter')]);
        assert.deepEqual(requests, ['GET TK0001', 'GET TK0001'], 'same code after 3s is sent');

        await tab.waitForFunction(() => document.activeElement && document.activeElement.hasAttribute('data-checkin-confirm'));
        await Promise.all([tab.waitForURL(base + '/admin/checkin'), tab.keyboard.press('Enter')]);
        const flash = tab.locator('[data-checkin-flash]');
        await flash.waitFor({ state: 'visible' });
        assert.match(await flash.getAttribute('class'), /is-success/);
        assert.match(await flash.innerText(), /HỢP LỆ[\s\S]*ghế B3, mời lên tàu/);
        assert(await focusedIs('[data-checkin-input]'));
        await tab.screenshot({ path: path.join(output, 'success-360.png') });
        await tab.waitForTimeout(300);
        assert.equal(await countBeeps(tab), 2, 'success plays 2 tones');
        await flash.waitFor({ state: 'detached', timeout: 3000 });
        assert(await focusedIs('[data-checkin-input]'));

        await tab.evaluate(() => {
            const form = document.createElement('form');
            form.method = 'post';
            form.action = '/admin/checkin';
            form.innerHTML = '<input name="ticketCode" value="BK404">';
            document.body.appendChild(form);
            form.submit();
        });
        await tab.locator('.wb-checkin-flash.is-error').waitFor({ state: 'visible' });
        assert.match(await tab.locator('[data-checkin-flash]').innerText(), /KHÔNG HỢP LỆ[\s\S]*BK404/);
        await tab.screenshot({ path: path.join(output, 'error-360.png') });
        await tab.waitForTimeout(300);
        assert.equal(await countBeeps(tab), 1, 'error plays 1 tone');
        await tab.keyboard.type('T');
        await tab.locator('[data-checkin-flash]').waitFor({ state: 'detached', timeout: 1000 });
        assert(await tab.evaluate(() => document.documentElement.scrollWidth <= innerWidth), 'no overflow at 360px');

        await tab.goto('http://wb-insecure.test:' + port + '/admin/checkin');
        await tab.click('[data-qr-start]');
        assert.match(await tab.locator('[data-qr-message]').innerText(), /HTTPS hoặc http:\/\/localhost/);
        assert(await tab.locator('[data-qr-reader]').isHidden());

        await tab.goto(base + '/admin/checkin');
        await tab.click('[data-qr-start]');
        await tab.locator('[data-qr-reader] video').waitFor({ state: 'visible', timeout: 10000 });
        await tab.locator('[data-qr-stop]').click();
        await tab.locator('[data-qr-reader]').waitFor({ state: 'hidden' });
        assert(await tab.locator('[data-qr-start]').isVisible());
        assert(await focusedIs('[data-checkin-input]'));

        await tab.goto(base + '/admin/checkin');
        await tab.evaluate(() => {
            navigator.mediaDevices.getUserMedia = () => Promise.reject(new DOMException('Permission denied', 'NotAllowedError'));
        });
        await tab.click('[data-qr-start]');
        await tab.locator('.wb-qr-message.is-error').waitFor();
        assert.match(await tab.locator('[data-qr-message]').innerText(), /quyền dùng camera/);
        assert(await tab.locator('[data-qr-start]').isVisible());
        assert.deepEqual(errors, []);

        const video = await makeQrVideo(browser, 'tk0000123');
        cameraBrowser = await launch(['--use-fake-ui-for-media-stream', '--use-fake-device-for-media-stream',
            '--use-file-for-fake-video-capture=' + video]);
        const cameraTab = await cameraBrowser.newPage({ viewport: { width: 360, height: 740 } });
        await cameraTab.goto(base + '/admin/checkin');
        await cameraTab.click('[data-qr-start]');
        await cameraTab.locator('[data-qr-stop]').waitFor({ state: 'visible' });
        await cameraTab.waitForURL(/ticketCode=TK0000123/, { timeout: 15000 });
        assert.equal(requests.at(-1), 'GET TK0000123');
        console.log('PASS: checkin focus/uppercase/Enter, preview focus, 3s dedup, success/error flash + tones, ' +
            'insecure-context and permission messages, camera QR scan and stop.');
    } finally {
        if (browser) await browser.close();
        if (cameraBrowser) await cameraBrowser.close();
        await new Promise(resolve => server.close(resolve));
    }
})().catch(error => { console.error(error); process.exitCode = 1; });
