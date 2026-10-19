/* =====================================================================
   U1:T19 รัน System Test ตาม Sequence Diagram ตาม T15 (ST-L01–L07)
   ทดสอบทั้งระบบ: หน้าเว็บ → API → TU REST API → MySQL ใน Docker แล้วตรวจทั้งหน้าจอและฐานข้อมูล

   ใช้: (รันจากโฟลเดอร์หลักของโปรเจกต์ เปิด Docker Desktop ไว้ · ต้องติดตั้ง playwright แล้วจาก T17)
     docker compose up -d --build
     $env:T17_USER = Read-Host "รหัสนักศึกษา"
     $s = Read-Host "รหัสผ่าน มธ." -AsSecureString
     $env:T17_PASS = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($s))
     node tools/u1-t19-run.mjs                    (ทุกเคส)
     node tools/u1-t19-run.mjs --only ST-L06      (เฉพาะบางเคส)
     Remove-Item Env:T17_PASS, Env:T17_USER

   - ST-L01 ต้องเป็น "ผู้ใช้ใหม่" สคริปต์จึงลบแถวของบัญชี T17_USER ใน users / students
     ของ MySQL บนเครื่องนี้ก่อนเริ่ม (ไม่กระทบเครื่องอื่น ข้อมูลจะถูกสร้างใหม่ตอน login)
   - ST-L04 ส่งรหัสผ่านผิดไป TU REST API 1 ครั้ง (ตาม T15)
   - ST-L06 ตั้ง advisor01 เป็น active = 0 ชั่วคราว แล้วคืนเป็น 1 ให้เสมอ
   - ST-L07 รีสตาร์ท app โดยไม่มี TU_API_KEY แล้วคืนค่าจาก .env ให้อัตโนมัติ
   - รูปบันทึกไว้ที่ .doc/UserStory/U1/ แถบดำใต้รูปแสดง request + status, sessionStorage และข้อมูลใน MySQL
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
const NOKEY = join(ROOT, 'tools', '.t19-no-tu-key.yml');
const MSG = {
   blank: 'กรุณากรอก Username และ Password',
   closed: 'บัญชีนี้ถูกปิดการใช้งาน กรุณาติดต่อเจ้าหน้าที่',
   tuDown: 'ไม่สามารถเชื่อมต่อระบบของมหาวิทยาลัยได้ กรุณาลองใหม่อีกครั้ง'
};

/* ---------- static server ---------- */
const TYPES = { '.html': 'text/html; charset=utf-8', '.js': 'text/javascript; charset=utf-8', '.css': 'text/css; charset=utf-8', '.png': 'image/png', '.svg': 'image/svg+xml', '.json': 'application/json' };
const server = createServer(async (req, res) => {
   const path = decodeURIComponent(new URL(req.url, BASE).pathname);
   const file = resolve(FRONT, '.' + (path === '/' ? '/login.html' : path));
   if (!file.startsWith(FRONT)) { res.writeHead(403).end(); return; }
   try { res.writeHead(200, { 'Content-Type': TYPES[extname(file)] || 'application/octet-stream' }).end(await readFile(file)); }
   catch { res.writeHead(404).end('not found'); }
});

/* ---------- helpers ---------- */
const sleep = ms => new Promise(r => setTimeout(r, ms));
const sh = cmd => execSync(cmd, { cwd: ROOT, stdio: ['pipe', 'pipe', 'pipe'] }).toString();

/* SQL ใน container db (รหัสผ่าน root ส่งผ่าน MYSQL_PWD ไม่ขึ้นบน command line ของ mysql) */
function sql(q) {
   const out = sh(`docker compose exec -T -e MYSQL_PWD=root1234 db mysql -uroot cs261db -N -B -e "${q.replace(/"/g, '\\"')}"`);
   return out.trim().split('\n').filter(Boolean).map(l => l.split('\t'));
}
const esc = s => String(s).replace(/'/g, "''");
const count = table => Number(sql(`SELECT COUNT(*) FROM ${table}`)[0][0]);
function userRow(username) {
   const r = sql(`SELECT id, role, active, IF(password_hash IS NULL,'NULL','SET'), full_name FROM users WHERE username='${esc(username)}'`)[0];
   return r ? { id: Number(r[0]), role: r[1], active: r[2], pw: r[3], name: r[4] } : null;
}
const studentRows = userId => userId == null ? 0 : Number(sql(`SELECT COUNT(*) FROM students WHERE user_id=${Number(userId)}`)[0][0]);
const dbLine = (username, mask) => {
   const u = userRow(username), uc = count('users'), sc = count('students');
   const who = mask ? 'นักศึกษาที่ทดสอบ' : username;
   return `MySQL: users=${uc} · students=${sc} · ${who}: ` + (u ? `id=${u.id} role=${u.role} active=${u.active} password_hash=${u.pw} students(user_id=${u.id})=${studentRows(u.id)}` : 'ไม่มีแถว');
};

async function backendUp(timeoutMs = 120000) {
   const end = Date.now() + timeoutMs;
   while (Date.now() < end) {
      try { await fetch(API + '/api/auth/login', { method: 'OPTIONS' }); return true; } catch { await sleep(2000); }
   }
   return false;
}

async function openPage(browser, path = '/login.html', ctx = null) {
   ctx = ctx || await browser.newContext({ viewport: { width: 1280, height: 800 } });
   const page = await ctx.newPage();
   const log = [];
   page.on('requestfinished', async rq => {
      if (!rq.url().startsWith(API) || rq.method() === 'OPTIONS') return;
      const rs = await rq.response();
      log.push(`${rq.method()} ${rq.url().replace(API, '')} → ${rs ? rs.status() : '-'}`);
   });
   page.on('requestfailed', rq => { if (rq.url().startsWith(API)) log.push(`${rq.method()} ${rq.url().replace(API, '')} → FAILED`); });
   await page.goto(BASE + path);
   return { ctx, page, log };
}
async function login(page, u, p, expectPage) {
   await page.fill('#u', u);
   await page.fill('#p', p);
   await page.click('#loginBtn');
   if (expectPage) await page.waitForURL('**/' + expectPage, { timeout: 20000 }).catch(() => { });
   else await page.locator('#loginErr').waitFor({ state: 'visible', timeout: 20000 }).catch(() => { });
   await page.waitForLoadState('networkidle').catch(() => { });
}
const errText = async page => (await page.locator('#loginErr').isVisible().catch(() => false)) ? (await page.locator('#loginErrMsg').textContent()).trim() : '';
const at = page => page.url().replace(BASE + '/', '').split('?')[0];
const authOf = async page => page.evaluate(() => { try { return JSON.parse(sessionStorage.getItem('u1-auth') || 'null'); } catch { return null; } });
const navText = async page => (await page.locator('#u5nav').textContent().catch(() => '')).replace(/\s+/g, ' ').trim();

/* แถบหลักฐาน: network + session + MySQL · mask = ซ่อนรหัสนักศึกษา/ชื่อ */
async function shot(page, name, title, log, db, mask) {
   const a = await authOf(page).catch(() => null);
   const lines = [
      `<b>${title}</b> · ${new Date().toLocaleString('th-TH')} · /${at(page)}`,
      'Network (/api/*): ' + (log.join(' | ') || '(ไม่มี request)'),
      'Session: ' + (a ? `u1-auth={role:${a.role}, userId:${a.userId}${mask ? '' : ', username:' + a.username}, accessToken:•••}` : '(ไม่มี u1-auth)'),
      db
   ];
   await page.evaluate(html => {
      const d = document.createElement('div'); d.id = '__t19';
      d.style.cssText = 'position:fixed;left:0;right:0;bottom:0;z-index:99999;background:#111;color:#0f0;font:12px/1.5 Consolas,monospace;padding:8px 12px;white-space:pre-wrap;word-break:break-all';
      d.innerHTML = html; document.body.appendChild(d);
   }, lines.join('<br>')).catch(() => { });
   if (mask) await page.addStyleTag({ content: '#who{filter:blur(6px)}' }).catch(() => { });
   await page.screenshot({ path: join(OUT, name) });
   await page.evaluate(() => document.getElementById('__t19')?.remove()).catch(() => { });
}

/* ---------- เคส ---------- */
const results = [];
const ONLY = (() => { const i = process.argv.indexOf('--only'); return i > 0 ? process.argv[i + 1].toUpperCase().split(',').map(x => x.trim()) : null; })();
const want = id => !ONLY || ONLY.includes(id);
function record(id, actual, pass) {
   results.push({ id, actual, pass });
   console.log(`${pass === null ? '⏭️ ' : pass ? '✅' : '❌'} ${id}  ${actual}`);
}

async function main() {
   await mkdir(OUT, { recursive: true });
   if (!(await backendUp(5000))) { console.error('❌ เรียก backend ไม่ได้ ให้รัน docker compose up -d --build ก่อน'); process.exit(1); }
   try { count('users'); } catch (e) { console.error('❌ query MySQL ใน container db ไม่ได้: ' + e.message); process.exit(1); }
   await new Promise(r => server.listen(PORT, r));

   const SU = (process.env.T17_USER || '').trim(), SP = process.env.T17_PASS || '';
   const real = !!(SU && SP);
   if (!real) console.log('⚠️ ไม่ได้ตั้ง T17_USER / T17_PASS จะข้าม ST-L01, L02, L04, L07');

   const browser = await chromium.launch();
   try {
      /* ST-L01 ผู้ใช้ใหม่ → save User + Student */
      if (want('ST-L01')) {
         if (!real) record('ST-L01', 'ข้าม (ไม่มีบัญชี มธ.)', null);
         else {
            const old = userRow(SU);
            if (old) { sql(`DELETE FROM students WHERE user_id=${old.id} OR student_id='${esc(SU)}'`); sql(`DELETE FROM users WHERE id=${old.id}`); }
            const u0 = count('users'), s0 = count('students');
            const { ctx, page, log } = await openPage(browser);
            await login(page, SU, SP, 'check.html');
            const row = userRow(SU), u1 = count('users'), s1 = count('students');
            await shot(page, 'U1-T19 ST-L01 new student.png', 'ST-L01 นักศึกษาที่ยังไม่เคย login', log,
               `MySQL ก่อน: users=${u0} students=${s0} → หลัง: users=${u1} students=${s1} · แถวใหม่: role=${row?.role} password_hash=${row?.pw} students=${studentRows(row?.id)}`, true);
            const a = await authOf(page);
            record('ST-L01', `200 role ${a?.role} → ${at(page)} · users ${u0}→${u1} · students ${s0}→${s1} · password_hash ${row?.pw}`,
               a?.role === 'STUDENT' && at(page) === 'check.html' && u1 === u0 + 1 && s1 === s0 + 1 && row?.pw === 'NULL' && studentRows(row?.id) === 1);
            await ctx.close();
         }
      }

      /* ST-L02 ผู้ใช้เดิม → update (logout แล้ว login ซ้ำ) */
      if (want('ST-L02')) {
         if (!real) record('ST-L02', 'ข้าม (ไม่มีบัญชี มธ.)', null);
         else {
            const { ctx, page, log } = await openPage(browser);
            await login(page, SU, SP, 'check.html');
            const first = (await authOf(page))?.userId;
            const u0 = count('users'), s0 = count('students');
            await page.click('#logout'); await page.waitForURL('**/login.html', { timeout: 10000 }).catch(() => { });
            await login(page, SU, SP, 'check.html');
            const second = (await authOf(page))?.userId, u1 = count('users'), s1 = count('students');
            await shot(page, 'U1-T19 ST-L02 login again.png', 'ST-L02 logout แล้ว login ซ้ำ', log,
               `MySQL ก่อน login ซ้ำ: users=${u0} students=${s0} → หลัง: users=${u1} students=${s1} · userId ครั้งแรก=${first} ครั้งที่สอง=${second}`, true);
            record('ST-L02', `userId ${first} → ${second} · users ${u0}→${u1} · students ${s0}→${s1}`, first != null && first === second && u1 === u0 && s1 === s0);
            await ctx.close();
         }
      }

      /* ST-L03 staff01 / advisor01 → staff-review เมนูตาม role · เปิด check.html ถูกพากลับ */
      if (want('ST-L03')) {
         const out = [];
         let ok = true;
         for (const [user, role, menu, img] of [['staff01', 'STAFF', 'แบบฟอร์มคำร้อง', 'U1-T19 ST-L03 staff.png'], ['advisor01', 'ADVISOR', 'พิจารณาใบสมัคร', 'U1-T19 ST-L03 advisor.png']]) {
            const { ctx, page, log } = await openPage(browser);
            await login(page, user, '1234', 'staff-review.html');
            const a = await authOf(page), nav = await navText(page), landed = at(page);
            await page.goto(BASE + '/check.html'); await page.waitForURL('**/staff-review.html', { timeout: 10000 }).catch(() => { });
            await page.waitForLoadState('networkidle').catch(() => { });
            const back = at(page);
            await shot(page, img, `ST-L03 ${user} login แล้วพิมพ์ URL check.html`, log, dbLine(user) + ` · หลังพิมพ์ check.html อยู่ที่ /${back}`);
            const pass = a?.role === role && landed === 'staff-review.html' && nav.includes(menu) && back === 'staff-review.html' && (role === 'STAFF' || !nav.includes('แบบฟอร์มคำร้อง'));
            ok = ok && pass;
            out.push(`${user}: ${a?.role} → ${landed} เมนู "${nav}" · check.html → ${back}`);
            await ctx.close();
         }
         record('ST-L03', out.join(' ; '), ok);
      }

      /* ST-L04 บัญชีจริง + รหัสผิด (ครั้งเดียว) */
      if (want('ST-L04')) {
         if (!real) record('ST-L04', 'ข้าม (ไม่มีบัญชี มธ.)', null);
         else {
            const before = userRow(SU), u0 = count('users'), s0 = count('students');
            const { ctx, page, log } = await openPage(browser);
            await login(page, SU, SP + '#wrong');
            const msg = await errText(page), after = userRow(SU), u1 = count('users'), s1 = count('students');
            await shot(page, 'U1-T19 ST-L04 wrong password.png', 'ST-L04 บัญชีจริง + รหัสผ่านผิด', log,
               `MySQL ก่อน: users=${u0} students=${s0} → หลัง: users=${u1} students=${s1} · แถวผู้ใช้เปลี่ยนไหม: ${JSON.stringify(before) === JSON.stringify(after) ? 'ไม่เปลี่ยน' : 'เปลี่ยน'}`, true);
            record('ST-L04', `${log.join(' ')} · "${msg}" · users ${u0}→${u1} · students ${s0}→${s1} · แถวเดิม${JSON.stringify(before) === JSON.stringify(after) ? 'ไม่เปลี่ยน' : 'เปลี่ยน'}`,
               log.some(l => l.endsWith('401')) && !!msg && u1 === u0 && s1 === s0 && JSON.stringify(before) === JSON.stringify(after) && at(page) === 'login.html');
            await ctx.close();
         }
      }

      /* ST-L05 ไม่กรอก Password (หน้าเว็บไม่ส่ง) + ยิง API body ว่าง → 400 */
      if (want('ST-L05')) {
         const { ctx, page, log } = await openPage(browser);
         await page.fill('#u', 'staff01'); await page.click('#loginBtn'); await sleep(800);
         const msg = await errText(page);
         const r1 = await fetch(API + '/api/auth/login', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: '{}' });
         const b1 = await r1.json().catch(() => ({}));
         const r2 = await fetch(API + '/api/auth/login', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: '' });
         const b2 = await r2.json().catch(() => ({}));
         const apiLine = `API body {} → ${r1.status} ${b1.code} "${b1.message}" · API body ว่าง → ${r2.status} ${b2.code}`;
         await shot(page, 'U1-T19 ST-L05 validation.png', 'ST-L05 ไม่กรอก Password + ยิง API body ว่าง', log, apiLine);
         record('ST-L05', `หน้าเว็บ request ${log.length} · "${msg}" · ${apiLine}`,
            log.length === 0 && msg === MSG.blank && r1.status === 400 && b1.code === 'VALIDATION_ERROR' && r2.status === 400);
         await ctx.close();
      }

      /* ST-L06 advisor01 active = 0 → 403 (คืนค่า active = 1 เสมอ) */
      if (want('ST-L06')) {
         try {
            sql(`UPDATE users SET active = 0 WHERE username = 'advisor01'`);
            const { ctx, page, log } = await openPage(browser);
            await login(page, 'advisor01', '1234');
            const msg = await errText(page);
            await shot(page, 'U1-T19 ST-L06 inactive.png', "ST-L06 UPDATE users SET active = 0 WHERE username = 'advisor01' แล้ว login", log, dbLine('advisor01'));
            record('ST-L06', `${log.join(' ')} · "${msg}"`, log.some(l => l.endsWith('403')) && msg === MSG.closed && at(page) === 'login.html');
            await ctx.close();
         } finally {
            sql(`UPDATE users SET active = 1 WHERE username = 'advisor01'`);
            console.log('… ST-L06: คืนค่า advisor01 active = 1 แล้ว');
         }
      }

      /* ST-L07 ไม่มี TU_API_KEY → 502 · คืน key แล้ว login ได้ */
      if (want('ST-L07')) {
         if (!real) record('ST-L07', 'ข้าม (ไม่มีบัญชี มธ.)', null);
         else {
            let down = '', downOk = false;
            try {
               console.log('… ST-L07: รีสตาร์ท app โดยไม่มี TU_API_KEY');
               await writeFile(NOKEY, 'services:\n  app:\n    environment:\n      TU_API_KEY: ""\n');
               sh(`docker compose -f docker-compose.yml -f "${NOKEY}" up -d --no-deps --force-recreate app`);
               await sleep(3000); if (!(await backendUp())) throw new Error('backend ไม่ขึ้น (ST-L07)'); await sleep(5000);
               const { ctx, page, log } = await openPage(browser);
               await login(page, SU, SP);
               const msg = await errText(page);
               await shot(page, 'U1-T19 ST-L07 tu down.png', 'ST-L07 ไม่มี TU_API_KEY แล้ว login บัญชีจริง', log, dbLine(SU, true), true);
               down = `${log.join(' ')} · "${msg}"`; downOk = log.some(l => l.endsWith('502')) && msg === MSG.tuDown;
               await ctx.close();
            } finally {
               await rm(NOKEY, { force: true });
               console.log('… ST-L07: คืนค่า TU_API_KEY จาก .env');
               sh('docker compose up -d --no-deps --force-recreate app');
               await sleep(3000); await backendUp(); await sleep(5000);
            }
            const { ctx, page, log } = await openPage(browser);
            await login(page, SU, SP, 'check.html');
            const a = await authOf(page);
            await shot(page, 'U1-T19 ST-L07 key restored.png', 'ST-L07 ใส่ key คืนแล้ว login ได้', log, dbLine(SU, true), true);
            record('ST-L07', `${down} · ใส่ key คืน → ${a?.role} ${at(page)}`, downOk && a?.role === 'STUDENT' && at(page) === 'check.html');
            await ctx.close();
         }
      }
   } catch (e) {
      console.error('❌ ' + e.message);
   } finally {
      await rm(NOKEY, { force: true }).catch(() => { });
      await browser.close();
      server.close();
   }

   const md = ['| TC ID | ผลที่ได้จริง | ผล |', '|---|---|---|',
      ...results.map(r => `| ${r.id} | ${r.actual.replace(/\|/g, '\\|').replace(new RegExp(SU || '\\b\\B', 'g'), '<รหัสนักศึกษา>')} | ${r.pass === null ? 'ไม่ได้รัน' : r.pass ? 'ผ่าน' : 'ไม่ผ่าน'} |`)].join('\n');
   const ran = results.filter(r => r.pass !== null), ok = ran.filter(r => r.pass).length;
   const summary = `${md}\n\nสรุป: ผ่าน ${ok}/${ran.length} เคสที่รัน · ${new Date().toLocaleString('th-TH')}\n`;
   await writeFile(join(OUT, 'T19-results.md'), summary);
   console.log('\n' + summary);
   process.exit(ran.length && ran.every(r => r.pass) ? 0 : 1);
}

main().catch(e => { console.error('❌', e.message); try { server.close(); } catch { } process.exit(2); });
