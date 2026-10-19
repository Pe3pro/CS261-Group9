/* =====================================================================
   login.js — U1 หน้าเข้าสู่ระบบ (login.html)
   ส่ง Username / Password ไปที่ POST /api/auth/login (Backend ตรวจกับ TU REST API)
   สำเร็จ: เก็บข้อมูลผู้ใช้ใน sessionStorage แล้วพาไปหน้าหลักตาม role
     - u1-auth    : token และข้อมูลผู้ใช้ (U1)
     - u5-user    : {id, role, name, username} ให้หน้า U5 ส่ง header X-User-Id
     - coop-state : .user = username ให้หน้า check/submit/ranking (js/script.js)
   ===================================================================== */
(function () {
   'use strict';

   const $ = (s, r = document) => r.querySelector(s);
   const API = window.U1_API_BASE || window.U5_API_BASE || 'http://localhost:8080';
   const AUTH_KEY = 'u1-auth';
   const U5_KEY = 'u5-user';
   const TEAM_KEY = 'coop-state';
   const MSG = {
      blank: 'กรุณากรอก Username และ Password',
      network: 'ไม่สามารถเชื่อมต่อระบบได้ กรุณาลองใหม่อีกครั้ง',
      fallback: 'Username หรือ Password ไม่ถูกต้อง กรุณาลองใหม่อีกครั้ง',
      busy: 'กำลังเข้าสู่ระบบ...'
   };

   const store = {
      get(k) { try { return JSON.parse(sessionStorage.getItem(k) || 'null'); } catch (e) { return null; } },
      set(k, v) { try { v == null ? sessionStorage.removeItem(k) : sessionStorage.setItem(k, JSON.stringify(v)); } catch (e) { } }
   };

   /* expiresAt จาก backend เป็น ISO string (หรือวินาทีถ้า Jackson ตั้งค่าเป็นตัวเลข) */
   function expiryMs(v) {
      if (typeof v === 'number') return v < 1e12 ? v * 1000 : v;
      const t = Date.parse(v);
      return isNaN(t) ? 0 : t;
   }

   /* ข้อมูล login ที่ยังไม่หมดอายุ และตรงกับ username ใน state ของทีม */
   function readAuth() {
      const a = store.get(AUTH_KEY);
      if (!a || !a.accessToken || expiryMs(a.expiresAt) <= Date.now()) return null;
      const team = store.get(TEAM_KEY) || {};
      return team.user === a.username ? a : null;
   }

   /* หน้าแรกหลัง login: นักศึกษาไปขั้นแรกที่ยังทำไม่เสร็จ (เหมือน HOME ใน script.js) อื่น ๆ ไปหน้าตรวจใบสมัคร */
   function homeOf(role) {
      if (role !== 'STUDENT') return 'staff-review.html';
      const unlocked = Number((store.get(TEAM_KEY) || {}).unlocked) || 1;
      return ['check', 'submit', 'ranking'][Math.min(Math.max(unlocked, 1), 3) - 1] + '.html';
   }

   function saveSession(d) {
      store.set(AUTH_KEY, {
         accessToken: d.accessToken, tokenType: d.tokenType, expiresAt: d.expiresAt,
         userId: d.userId, username: d.username, fullName: d.fullName, role: d.role
      });
      store.set(U5_KEY, { id: d.userId, role: d.role, name: d.fullName, username: d.username });
      const team = store.get(TEAM_KEY) || {};
      team.user = d.username;
      store.set(TEAM_KEY, team);
   }

   function showError(msg) {
      $('#loginErrMsg').textContent = msg;
      $('#loginErr').hidden = false;
   }

   async function submit(e) {
      e.preventDefault();
      const u = $('#u').value.trim(), p = $('#p').value;
      $('#loginErr').hidden = true;
      if (!u || !p) return showError(MSG.blank);

      const btn = $('#loginBtn'), label = btn.textContent;
      btn.disabled = true; btn.textContent = MSG.busy;
      let res = null, data = null;
      try {
         res = await fetch(API + '/api/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username: u, password: p })
         });
         try { data = await res.json(); } catch (err) { data = null; }
      } catch (err) {
         res = null;
      }
      btn.disabled = false; btn.textContent = label;

      if (!res) return showError(MSG.network);
      if (res.ok && data && data.userId != null && data.role) {
         saveSession(data);
         return window.U1Login.go(homeOf(data.role));
      }
      if (res.status === 401) { $('#p').value = ''; $('#p').focus(); }
      showError((data && data.message) || MSG.fallback);
   }

   let started = false;
   function init() {
      if (started) return;   // กันเรียกซ้ำ (DOMContentLoaded มากกว่า 1 ครั้ง)
      started = true;
      const auth = readAuth();
      if (auth) return window.U1Login.go(homeOf(auth.role));   // login ค้างอยู่แล้ว ไปหน้าหลักเลย
      store.set(AUTH_KEY, null);                                // ข้อมูลเก่าหมดอายุหรือไม่ตรงกัน ล้างทิ้ง
      store.set(U5_KEY, null);
      $('#loginForm').addEventListener('submit', submit);
   }

   /* go แยกออกมาเพื่อให้ unit test ตรวจหน้าปลายทางได้ */
   window.U1Login = { go: url => window.location.assign(url), homeOf, readAuth, expiryMs };

   if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', init);
   else init();
})();