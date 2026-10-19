/* =====================================================================
   U1:T17 รัน Integration Test UI กับ API จริง ตาม T13 (IT-UA-L01–L09)

   ใช้: (รันจากโฟลเดอร์หลักของโปรเจกต์ ต้องเปิด Docker Desktop ไว้)
     npm i -D playwright
     npx playwright install chromium
     docker compose up -d --build
     node tools/u1-t17-run.mjs                 (ทุกเคส)
     node tools/u1-t17-run.mjs --only L07,L08  (เฉพาะบางเคส)

   - เปิด frontend ด้วย static server ของสคริปต์เอง (port 5599 ไม่ชนกับ Live Server)
   - แต่ละเคสใช้ browser context ใหม่ (sessionStorage ว่าง)
   - บันทึกรูปไว้ที่ .doc/UserStory/U1/ ชื่อตาม T17.md ทุกรูปมีแถบด้านล่างแสดง
     request /api/* ที่หน้าเว็บส่ง + status code และ key ใน sessionStorage (แทน DevTools)
   - L01 / L07 ต้องใช้บัญชีนักศึกษา มธ. จริง ส่งผ่าน environment variable เท่านั้น
     (สคริปต์ไม่ถามรหัสผ่านบนจอ) ถ้าไม่ตั้งค่าจะข้ามสองเคสนี้:
       $env:T17_USER = Read-Host "รหัสนักศึกษา"
       $s = Read-Host "รหัสผ่าน มธ." -AsSecureString
       $env:T17_PASS = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($s))
       node tools/u1-t17-run.mjs
       Remove-Item Env:T17_PASS, Env:T17_USER      # ลบทิ้งหลังรัน
   - L07 รีสตาร์ท container app โดยไม่ส่ง TU_API_KEY แล้วรีสตาร์ทกลับให้อัตโนมัติ
   - L08 หยุด container app แล้วเปิดกลับให้อัตโนมัติ
   ===================================================================== */
import { chromium } from 'playwright';
import { createServer } from 'node:http';
import { readFile, writeFile, mkdir, rm } from 'node:fs/promises';
import { execSync } from 'node:child_process';
import { join, extname, resolve } from 'node:path';

const ROOT = resolve('.');
const FRONT = join(ROOT, 'frontend');
const OUT = join(ROOT, '.doc', 'UserStory', 'U1');
const PORT = Number(process.env.T17_PORT) || 5599;
const BASE = `http://localhost:${PORT}`;
const API = 'http://localhost:8080';
const NOKEY = join(ROOT, 'tools', '.t17-no-tu-key.yml');   // override ชั่วคราวสำหรับ L07 (ลบทิ้งหลังใช้)
const MSG = {
   blank: 'กรุณากรอก Username และ Password',
   network: 'ไม่สามารถเชื่อมต่อระบบได้ กรุณาลองใหม่อีกครั้ง',
   tuDown: 'ไม่สามารถเชื่อมต่อระบบของมหาวิทยาลัยได้ กรุณาลองใหม่อีกครั้ง'
};

/* ---------- static server สำหรับ frontend ---------- */
const TYPES = { '.html': 'text/html; charset=utf-8', '.js': 'text/javascript; charset=utf-8', '.css': 'text/css; charset=utf-8', '.png': 'image/png', '.svg': 'image/svg+xml', '.json': 'application/json' };
const server = createServer(async (req, res) => {
   const path = decodeURIComponent(new URL(req.url, BASE).pathname);
   const file = resolve(FRONT, '.' + (path === '/' ? '/login.html' : path));
   if (!file.startsWith(FRONT)) { res.writeHead(403).end(); return; }
   try {
      const body = await readFile(file);
      res.writeHead(200, { 'Content-Type': TYPES[extname(file)] || 'application/octet-stream' }).end(body);
   } catch { res.writeHead(404).end('not found'); }
});

/* ---------- helpers ---------- */
const sleep = ms => new Promise(r => setTimeout(r, ms));
const sh = (cmd, env) => execSync(cmd, { cwd: ROOT, stdio: 'pipe', env: { ...process.env, ...env } }).toString();

async function backendUp(timeoutMs = 120000) {
   const end = Date.now() + timeoutMs;
   while (Date.now() < end) {
      try { await fetch(API + '/api/auth/login', { method: 'OPTIONS' }); return true; } catch { await sleep(2000); }
   }
   return false;
}

/* เปิดหน้า login ใน context ใหม่ และเก็บ request /api/* ทุกตัว */
async function openLogin(browser) {
   const ctx = await browser.newContext({ viewport: { width: 1280, height: 800 } });
   const page = await ctx.newPage();
   const log = [];
   page.on('requestfinished', async rq => {
      if (!rq.url().startsWith(API)) return;
      const rs = await rq.response();
      log.push({ method: rq.method(), url: rq.url().replace(API, ''), status: rs ? rs.status() : '-', userId: rq.headers()['x-user-id'] });
   });
   page.on('requestfailed', rq => { if (rq.url().startsWith(API)) log.push({ method: rq.method(), url: rq.url().replace(API, ''), status: 'FAILED (' + (rq.failure()?.errorText || 'network') + ')' }); });
   await page.goto(BASE + '/login.html');
   return { ctx, page, log };
}

async function login(page, u, p) {
   await page.fill('#u', u);
   await page.fill('#p', p);
   await page.click('#loginBtn');
}

const apiLog = log => log.filter(x => x.method !== 'OPTIONS');
const errText = async page => (await page.locator('#loginErr').isVisible()) ? (await page.locator('#loginErrMsg').textContent()).trim() : '';
const session = page => page.evaluate(() => Object.fromEntries(Object.keys(sessionStorage).map(k => [k, sessionStorage.getItem(k)])));

/* แปะแถบหลักฐาน (network + sessionStorage) ไว้ล่างหน้าแล้วแคปจอ */
async function shot(page, name, title, log) {
   const ss = await session(page).catch(() => ({}));
   const lines = [
      `<b>${title}</b> · ${new Date().toLocaleString('th-TH')} · ${page.url().replace(BASE, '')}`,
      'Network (/api/*): ' + (apiLog(log).map(x => `${x.method} ${x.url} → ${x.status}${x.userId ? ` [X-User-Id: ${x.userId}]` : ''}`).join(' | ') || '(ไม่มี request)'),
      'sessionStorage: ' + (Object.keys(ss).length ? Object.entries(ss).map(([k, v]) => k === 'u1-auth' ? `${k}={role:${JSON.parse(v).role}, userId:${JSON.parse(v).userId}, accessToken:•••}` : `${k}=${v.length > 80 ? v.slice(0, 80) + '…' : v}`).join(' · ') : '(ว่าง)')
   ];
   await page.evaluate(html => {
      const d = document.createElement('div');
      d.id = '__t17';
      d.style.cssText = 'position:fixed;left:0;right:0;bottom:0;z-index:99999;background:#111;color:#0f0;font:12px/1.5 Consolas,monospace;padding:8px 12px;white-space:pre-wrap;word-break:break-all';
      d.innerHTML = html;
      document.body.appendChild(d);
   }, lines.join('<br>')).catch(() => { });
   await page.screenshot({ path: join(OUT, name) });
   await page.evaluate(() => document.getElementById('__t17')?.remove()).catch(() => { });
}

/* ---------- เคสทดสอบ ---------- */
const results = [];
const ONLY = (() => { const i = process.argv.indexOf('--only'); return i > 0 ? process.argv[i + 1].toUpperCase().split(',').map(x => x.trim()) : null; })();
const want = (...ids) => !ONLY || ids.some(id => ONLY.includes(id));
function record(id, expected, actual, pass, img) {
   results.push({ id, expected, actual, pass, img });
   console.log(`${pass === null ? '⏭️ ' : pass ? '✅' : '❌'} ${id}  ${actual}`);
}

async function caseLogin(browser, { id, user, pass, expectStatus, expectRole, expectPage, img, title }) {
   const { ctx, page, log } = await openLogin(browser);
   await login(page, user, pass);
   if (expectPage) await page.waitForURL('**/' + expectPage, { timeout: 15000 }).catch(() => { });
   else await page.locator('#loginErr').waitFor({ state: 'visible', timeout: 15000 }).catch(() => { });
   await page.waitForLoadState('networkidle').catch(() => { });
   const req = apiLog(log).find(x => x.url === '/api/auth/login');
   const ss = await session(page);
   const role = ss['u1-auth'] ? JSON.parse(ss['u1-auth']).role : null;
   const at = page.url().replace(BASE + '/', '');
   await shot(page, img, title, log);
   return { ctx, page, log, req, role, at, ss };
}

async function main() {
   await mkdir(OUT, { recursive: true });
   if (!(await backendUp(5000))) { console.error('❌ เรียก backend ที่ ' + API + ' ไม่ได้ ให้รัน docker compose up -d --build ก่อน'); process.exit(1); }
   await new Promise(r => server.listen(PORT, r));

   const studentUser = (process.env.T17_USER || '').trim();
   const studentPass = studentUser ? (process.env.T17_PASS || '') : '';
   if (studentUser && !studentPass) console.log('⚠️ ตั้ง T17_USER แต่ไม่มี T17_PASS จะข้าม L01/L07');
   const real = studentUser && studentPass;

   const browser = await chromium.launch();
   try {
      /* L01 นักศึกษาจริง */
      if (!want('L01')) { } else if (real) {
         const r = await caseLogin(browser, { id: 'L01', user: studentUser, pass: studentPass, expectPage: 'check.html', img: 'U1-T17 L01 student.png', title: 'IT-UA-L01 นักศึกษา login บัญชี มธ. จริง' });
         record('IT-UA-L01', '200 STUDENT → check.html', `POST /api/auth/login → ${r.req?.status} role ${r.role} → ${r.at}`, r.req?.status === 200 && r.role === 'STUDENT' && r.at.startsWith('check.html'), r.img);
         await r.ctx.close();
      } else record('IT-UA-L01', '200 STUDENT → check.html', 'ข้าม (ไม่ได้กรอกบัญชี มธ.)', null);

      /* L02 + L04 staff01 */
      if (want('L02', 'L04')) {
         const r = await caseLogin(browser, { user: 'staff01', pass: '1234', expectPage: 'staff-review.html', img: 'U1-T17 L02 staff.png', title: 'IT-UA-L02/L04 staff01 login → staff-review โหลดคำร้อง' });
         await r.page.waitForResponse(x => x.url().includes('/api/applications'), { timeout: 10000 }).catch(() => { });
         await r.page.waitForLoadState('networkidle').catch(() => { });
         const nav = (await r.page.locator('#u5nav').textContent().catch(() => '')).replace(/\s+/g, ' ').trim();
         const uid = r.ss['u1-auth'] ? JSON.parse(r.ss['u1-auth']).userId : null;
         await shot(r.page, 'U1-T17 L02 staff.png', 'IT-UA-L02/L04 staff01 login → staff-review โหลดคำร้อง', r.log);
         record('IT-UA-L02', '200 STAFF userId 1 → staff-review เมนูตรวจใบสมัคร + แบบฟอร์มคำร้อง', `→ ${r.req?.status} role ${r.role} userId ${uid} → ${r.at} · เมนู: ${nav}`,
            r.req?.status === 200 && r.role === 'STAFF' && r.at.startsWith('staff-review.html') && nav.includes('ตรวจใบสมัคร') && nav.includes('แบบฟอร์มคำร้อง'));
         const apps = apiLog(r.log).find(x => x.url.startsWith('/api/applications'));
         record('IT-UA-L04', 'GET /api/applications?status=SUBMITTED X-User-Id = userId → 200', apps ? `${apps.method} ${apps.url} [X-User-Id: ${apps.userId}] → ${apps.status}` : 'ไม่พบ request /api/applications',
            !!apps && apps.status === 200 && String(apps.userId) === String(uid));
         await r.ctx.close();
      }

      /* L03 advisor01 */
      if (want('L03')) {
         const r = await caseLogin(browser, { user: 'advisor01', pass: '1234', expectPage: 'staff-review.html', img: 'U1-T17 L03 advisor.png', title: 'IT-UA-L03 advisor01 login' });
         await r.page.waitForLoadState('networkidle').catch(() => { });
         const nav = (await r.page.locator('#u5nav').textContent().catch(() => '')).replace(/\s+/g, ' ').trim();
         await shot(r.page, 'U1-T17 L03 advisor.png', 'IT-UA-L03 advisor01 login', r.log);
         record('IT-UA-L03', '200 ADVISOR → staff-review เมนูพิจารณาใบสมัคร', `→ ${r.req?.status} role ${r.role} → ${r.at} · เมนู: ${nav}`,
            r.req?.status === 200 && r.role === 'ADVISOR' && r.at.startsWith('staff-review.html') && nav.includes('พิจารณาใบสมัคร'));
         await r.ctx.close();
      }

      /* L05 รหัสผิด */
      if (want('L05')) {
         const r = await caseLogin(browser, { user: 'staff01', pass: '0000', img: 'U1-T17 L05 wrong password.png', title: 'IT-UA-L05 staff01 รหัสผ่านผิด' });
         const msg = await errText(r.page), pw = await r.page.inputValue('#p');
         record('IT-UA-L05', '401 INVALID_CREDENTIALS กล่องแดง ช่อง Password ว่าง', `→ ${r.req?.status} · ข้อความ "${msg}" · Password ว่าง: ${pw === ''} · อยู่หน้า ${r.at}`,
            r.req?.status === 401 && !!msg && pw === '' && r.at.startsWith('login.html'));
         await r.ctx.close();
      }

      /* L06 ไม่กรอก */
      if (want('L06')) {
         const { ctx, page, log } = await openLogin(browser);
         await page.click('#loginBtn');
         await sleep(800);
         const msg = await errText(page);
         await shot(page, 'U1-T17 L06 blank.png', 'IT-UA-L06 ไม่กรอกข้อมูล กดเข้าสู่ระบบ', log);
         const n = apiLog(log).length;
         record('IT-UA-L06', `ไม่มี request · "${MSG.blank}"`, `request /api/*: ${n} · ข้อความ "${msg}"`, n === 0 && msg === MSG.blank);
         await ctx.close();
      }

      /* L09 logout (ใช้ staff01) */
      if (want('L09')) {
         const r = await caseLogin(browser, { user: 'staff01', pass: '1234', expectPage: 'staff-review.html', img: 'U1-T17 L09 logout.png', title: 'IT-UA-L09 (ก่อน logout)' });
         await r.page.click('#logout');
         await r.page.waitForURL('**/login.html', { timeout: 10000 }).catch(() => { });
         await sleep(500);
         const ss = await session(r.page);
         await shot(r.page, 'U1-T17 L09 logout.png', 'IT-UA-L09 หลังกดออกจากระบบ', r.log);
         const at = r.page.url().replace(BASE + '/', '');
         record('IT-UA-L09', 'กลับ login.html · u1-auth / u5-user ถูกลบ', `อยู่หน้า ${at} · u1-auth: ${'u1-auth' in ss ? 'ยังอยู่' : 'ลบแล้ว'} · u5-user: ${'u5-user' in ss ? 'ยังอยู่' : 'ลบแล้ว'}`,
            at.startsWith('login.html') && !('u1-auth' in ss) && !('u5-user' in ss));
         await r.ctx.close();
      }

      /* L07 ไม่มี TU_API_KEY (รีสตาร์ท app ชั่วคราว) */
      if (!want('L07')) { } else if (real) {
         console.log('… L07: รีสตาร์ท app โดยไม่ส่ง TU_API_KEY');
         await writeFile(NOKEY, 'services:\n  app:\n    environment:\n      TU_API_KEY: ""\n');
         sh(`docker compose -f docker-compose.yml -f "${NOKEY}" up -d --no-deps --force-recreate app`);
         await sleep(3000);
         if (!(await backendUp())) throw new Error('backend ไม่ขึ้นหลังรีสตาร์ท (L07)');
         await sleep(5000);
         const r = await caseLogin(browser, { user: studentUser, pass: studentPass, img: 'U1-T17 L07 tu unavailable.png', title: 'IT-UA-L07 ไม่ตั้ง TU_API_KEY → login บัญชีจริง' });
         const msg = await errText(r.page);
         record('IT-UA-L07', `502 TU_API_UNAVAILABLE · "${MSG.tuDown}"`, `→ ${r.req?.status} · ข้อความ "${msg}"`, r.req?.status === 502 && msg === MSG.tuDown);
         await r.ctx.close();
         console.log('… L07: คืนค่า TU_API_KEY จาก .env');
         await rm(NOKEY, { force: true });
         sh('docker compose up -d --no-deps --force-recreate app');
         await sleep(3000);
         await backendUp();
      } else record('IT-UA-L07', '502 TU_API_UNAVAILABLE', 'ข้าม (ไม่ได้กรอกบัญชี มธ.)', null);

      /* L08 backend หยุด */
      if (want('L08')) {
         console.log('… L08: หยุด container app');
         sh('docker compose stop app');
         const { ctx, page, log } = await openLogin(browser);
         await login(page, 'staff01', '1234');
         await page.locator('#loginErr').waitFor({ state: 'visible', timeout: 15000 }).catch(() => { });
         const msg = await errText(page), enabled = await page.isEnabled('#loginBtn');
         await shot(page, 'U1-T17 L08 backend down.png', 'IT-UA-L08 docker compose stop app แล้วกด login', log);
         const req = apiLog(log).find(x => x.url === '/api/auth/login');
         record('IT-UA-L08', `network error · "${MSG.network}" ปุ่มกดใหม่ได้`, `request: ${req ? req.status : '-'} · ข้อความ "${msg}" · ปุ่มกดได้: ${enabled}`, msg === MSG.network && enabled);
         await ctx.close();
         console.log('… L08: เปิด container app กลับ');
         sh('docker compose start app');
         await backendUp();
      }
   } catch (e) {
      console.error('❌ ' + e.message);
   } finally {
      await rm(NOKEY, { force: true }).catch(() => { });
      await browser.close();
      server.close();
   }

   /* ---------- สรุป ---------- */
   const order = ['IT-UA-L01', 'IT-UA-L02', 'IT-UA-L03', 'IT-UA-L04', 'IT-UA-L05', 'IT-UA-L06', 'IT-UA-L07', 'IT-UA-L08', 'IT-UA-L09'];
   results.sort((a, b) => order.indexOf(a.id) - order.indexOf(b.id));
   const md = ['| TC ID | ผลที่ได้จริง | ผล |', '|---|---|---|',
      ...results.map(r => `| ${r.id} | ${r.actual.replace(/\|/g, '\\|')} | ${r.pass === null ? 'ไม่ได้รัน' : r.pass ? 'ผ่าน' : 'ไม่ผ่าน'} |`)].join('\n');
   const ran = results.filter(r => r.pass !== null), ok = ran.filter(r => r.pass).length;
   const summary = `${md}\n\nสรุป: ผ่าน ${ok}/${ran.length} เคสที่รัน${ONLY ? ' (--only ' + ONLY.join(',') + ')' : ''} · ${new Date().toLocaleString('th-TH')}\n`;
   await writeFile(join(OUT, 'T17-results.md'), summary);
   console.log('\n' + summary + `\nรูปและผลอยู่ที่ ${OUT}`);
   process.exit(ran.length && ran.every(r => r.pass) ? 0 : 1);
}

main().catch(e => { console.error('❌', e.message); try { server.close(); } catch { } process.exit(2); });
