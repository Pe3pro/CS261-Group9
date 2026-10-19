/* =========================================================
   script.js
   ใช้ร่วมกัน: login, check, submit, ranking
   ========================================================= */

/* ---------- ตัวช่วยค้นหา HTML ---------- */
const $ = (selector, root = document) =>
    root.querySelector(selector);

const $$ = (selector, root = document) => [...root.querySelectorAll(selector)];

/* ---------- API สำหรับพัฒนาในเครื่อง ---------- */
const ELIGIBILITY_API =
    'http://localhost:8080/api/eligibility/check';

/* ---------- สถานะของระบบ ---------- */
const KEY = 'coop-state';

const DEF = {
    user: null,
    unlocked: 1,

    form: {
        id: '6410000001',
        fac: 'วิทยาศาสตร์',
        major: 'วิทยาการคอมพิวเตอร์',
        gpax: '3.10'
    },

    cur: '61',
    val: {},
    alt: {},
    ack: [false, false, false],

    prio: {
        A: '',
        B: '',
        C: '',
        D: ''
    }
};

const fresh = () =>
    JSON.parse(JSON.stringify(DEF));

function load() {
    try {
        const defaults = fresh();
        const stored =
            JSON.parse(sessionStorage.getItem(KEY) || '{}');

        return {
            ...defaults,
            ...stored,

            form: {
                ...defaults.form,
                ...(stored.form || {})
            },

            val: stored.val || {},
            alt: stored.alt || {},

            ack: Array.isArray(stored.ack) &&
                stored.ack.length === 3 ?
                stored.ack :
                defaults.ack,

            prio: {
                ...defaults.prio,
                ...(stored.prio || {})
            }
        };
    } catch (error) {
        return fresh();
    }
}

let S = load();

function save() {
    try {
        sessionStorage.setItem(KEY, JSON.stringify(S));
    } catch (error) {
        // หากบันทึกไม่ได้ ยังคงใช้สถานะในหน่วยความจำ
    }
}

/* ---------- ส่วนประกอบร่วม ---------- */
function setStepper(cur, errAt = 0, allDone = false) {
    $$('.steps .st').forEach((element, index) => {
        const step = index + 1;
        let className = '';
        let label = step;

        if (errAt === step) {
            className = 'err';
            label = '✕';
        } else if (
            step < cur ||
            (step === cur && allDone)
        ) {
            className = 'done';
            label = '✓';
        } else if (step === cur) {
            className = 'cur';
        }

        element.className = 'st ' + className;

        const dot = $('.dot', element);

        if (dot) {
            dot.textContent = label;
        }
    });
}

const showView = (ids, currentId) => {
    ids.forEach(id => {
        const element = $('#' + id);

        if (element) {
            element.hidden = id !== currentId;
        }
    });
};

const DELAY = 1400;

const NEED = {
    check: 1,
    submit: 2,
    ranking: 3
};

const HOME = () => {
    const index =
        Math.min(Math.max(S.unlocked, 1), 3) - 1;

    return ['check', 'submit', 'ranking'][index] + '.html';
};

/* U1: role มาจาก backend (POST /api/auth/login) เก็บไว้ใน sessionStorage 'u5-user' โดย js/login.js
   ไม่ใช่นักศึกษา → ไปหน้าตรวจใบสมัคร (ถ้ายังไม่มี role ใช้วิธีเดิมของ U5: ดูจากชื่อ staff/advisor) */
const loginRole = u => { try { const x = JSON.parse(sessionStorage.getItem('u5-user') || 'null'); return x && x.username === u ? x.role : null } catch (e) { return null } };
const isStaffLogin = u => { const r = loginRole(u); return r ? r !== 'STUDENT' : /^(staff|advisor)/i.test(u || '') };
const HOME_OF = u => isStaffLogin(u) ? 'staff-review.html' : HOME();

function initShell() {
    $('#who').textContent = S.user;

    $$('nav a:not([data-free])').forEach((link, index) => {
        if (index + 1 > S.unlocked) {
            link.classList.add('disabled');
            link.setAttribute('aria-disabled', 'true');
            link.tabIndex = -1;
        }
    });

    $('#logout').onclick = () => {
        S.user = null;
        save();
        try { sessionStorage.removeItem('u1-auth'); sessionStorage.removeItem('u5-user') } catch (e) { }
        location.href = 'login.html';
    };
}

/* ---------- เริ่มทำงานตามหน้าที่เปิด ---------- */
document.addEventListener('DOMContentLoaded', () => {
    const page = document.body.dataset.page;

    if (page === 'login') {
        return initLogin();
    }

    if (!S.user) {
        return location.replace('login.html');
    }

    // U5: หน้านักศึกษาไม่ใช่ของเจ้าหน้าที่
    if (isStaffLogin(S.user)) {
        return location.replace('staff-review.html');
    }

    if (S.unlocked < NEED[page]) {
        return location.replace('check.html');
    }

    const initializers = {
        check: initCheck,
        submit: initSubmit,
        ranking: initRanking
    };

    if (!initializers[page]) {
        return;
    }

    initShell();
    initializers[page]();
});

/* =========================================================
   หน้า Login
   ========================================================= */
function initLogin() {
    if (S.user) {
        return location.replace(HOME_OF(S.user));
    }

    $('#loginForm').addEventListener('submit', event => {
        event.preventDefault();

        const username = $('#u').value.trim();
        const password = $('#p').value;

        // การล็อกอินจำลองสำหรับต้นแบบ
        if (username && password === '1234') {
            S.user = username;
            save();
            location.href = HOME_OF(username);
        } else {
            $('#loginErr').hidden = false;
        }
    });
}

/* =========================================================
   หน้า U2: ตรวจสอบคุณสมบัติ
   ========================================================= */

/* ---------- รายวิชาของแต่ละหลักสูตร ---------- */
const CUR = {
    '61': {
        core: [
            'คพ.101',
            'คพ.102',
            'คพ.111', ['คพ.213', 'คพ.216'],
            'คพ.251',
            'คพ.264'
        ],

        now: [
            'คพ.384', ['คพ.266', 'คพ.322', 'คพ.348']
        ],

        must: 'คพ.302'
    },

    '66': {
        core: [
            'คพ.100',
            'คพ.101',
            'คพ.102',
            'คพ.111', ['คพ.213', 'คพ.216'],
            'คพ.251',
            'คพ.261'
        ],

        now: [
            'คพ.180', ['คพ.262', 'คพ.331', 'คพ.240']
        ],

        must: 'คพ.301'
    }
};

const GRADE = {
    'A': 4,
    'B+': 3.5,
    'B': 3,
    'C+': 2.5,
    'C': 2,
    'D+': 1.5,
    'D': 1,
    'F': 0
};

const GRADE_OPTS = [
    'ยังไม่เคยเรียน',
    ...Object.keys(GRADE)
];

const MIN_GRADE = 2.5;

const STAT_OPTS = [
    'ยังไม่เคยลง',
    'กำลังศึกษา',
    'เคยศึกษาแล้ว'
];

/* ---------- ตัวช่วยอ่านข้อมูลรายวิชา ---------- */
const K = (section, index) =>
    S.cur + section + index;

const valOf = (section, index, defaultValue) => {
    const key = K(section, index);

    return key in S.val ?
        S.val[key] :
        defaultValue;
};

const codeOf = (section, item, index) => {
    if (Array.isArray(item)) {
        return item[S.alt[K(section, index)] || 0];
    }

    return item;
};

/* ---------- สร้างแถวรายวิชา ---------- */
function rowHtml(section, item, index, list, defaultValue) {
    const key = K(section, index);
    const value = valOf(section, index, defaultValue);
    const isGroup = Array.isArray(item);

    const courseSelector = isGroup ?
        `
         <select data-t="alt"
                 data-k="${key}"
                 aria-label="เลือกรายวิชา">
            ${item.map((code, choiceIndex) => `
               <option
                  value="${choiceIndex}"
                  ${(S.alt[key] || 0) === choiceIndex
                     ? 'selected'
                     : ''}>
                  ${code}
               </option>
            `).join('')}
         </select>
      `
      : '<small></small>';

   const valueSelector = `
      <select data-t="val"
              data-k="${key}"
              aria-label="${isGroup ? 'ผล' : item}">
         ${list.map(option => `
            <option ${option === value ? 'selected' : ''}>
               ${option}
            </option>
         `).join('')}
      </select>
   `;

   return `
      <div class="row">
         <b>${isGroup ? 'เลือก 1' : item}</b>
         ${courseSelector}
         ${valueSelector}
      </div>
   `;
}

/*
 * ฟังก์ชันประเมินเดิม เก็บไว้สำหรับโค้ดหรือ Test เดิม
 * การกดตรวจสอบบนหน้าเว็บจะใช้ผลจาก API เท่านั้น
 */
function evaluate() {
   const curriculum = CUR[S.cur];
   const gpa = Number(S.form.gpax);
   const results = [];

   results.push({
      ok: 1,
      t: 'สาขาวิชา',
      d: 'นักศึกษาสาขาวิทยาการคอมพิวเตอร์ คณะวิทยาศาสตร์ฯ ม.ธรรมศาสตร์'
   });

   curriculum.core.forEach((item, index) => {
      const code = codeOf('g', item, index);
      const grade = valOf('g', index, 'B');

      if (!(grade in GRADE)) {
         results.push({
            ok: 0,
            t: code,
            d: 'ต้องเคยเรียนและได้เกรดไม่ต่ำกว่า 2.50',
            v: 'ยังไม่เคยเรียน'
         });
      } else if (GRADE[grade] < MIN_GRADE) {
         results.push({
            ok: 0,
            t: code,
            d: 'ต้องได้เกรดไม่ต่ำกว่า 2.50 (C+ ขึ้นไป)',
            v: 'เกรด ' + grade
         });
      } else {
         results.push({
            ok: 1,
            t: code,
            d: 'เกรด ' + grade + ' (ผ่านเกณฑ์ไม่ต่ำกว่า 2.50)'
         });
      }
   });

   const missingCourses = [];
   const courseDetails = [];

   curriculum.now.forEach((item, index) => {
      const code = codeOf('s', item, index);
      const status = valOf('s', index, 'กำลังศึกษา');

      courseDetails.push(code + ' (' + status + ')');

      if (status === 'ยังไม่เคยลง') {
         missingCourses.push(code);
      }
   });

   results.push({
      ok: missingCourses.length ? 0 : 1,
      t: 'กำลังศึกษา หรือเคยศึกษารายวิชา',
      d: courseDetails.join(' · '),
      v: missingCourses.length
         ? 'ยังไม่ลง ' + missingCourses.join(', ')
         : ''
   });

   const validGpa =
      Number.isFinite(gpa) && gpa >= 0 && gpa <= 4;

   const passedGpa = validGpa && gpa >= 2.75;

   results.push({
      ok: passedGpa ? 1 : 0,
      t: 'เกรดเฉลี่ยสะสม (GPA) ไม่ต่ำกว่า 2.75',
      d: validGpa
         ? 'ของคุณ ' + gpa.toFixed(2)
         : 'กรอก GPA ไม่ถูกต้อง',
      v: passedGpa
         ? ''
         : validGpa
            ? gpa.toFixed(2)
            : 'ไม่ถูกต้อง'
   });

   results.push({
      ok: 'w',
      t: 'ต้องสอบได้วิชา ' + curriculum.must,
      d: 'ก่อนออกไปปฏิบัติงานจริง ณ สถานประกอบการ'
   });

   return results;
}

/* ---------- สร้าง Request สำหรับ API ---------- */
function buildEligibilityRequest() {
   const curriculum = CUR[S.cur];

   if (!curriculum) {
      throw new Error('หลักสูตรต้องเป็น 61 หรือ 66');
   }

   const grades = {};
   const courseStatuses = {};

   curriculum.core.forEach((item, index) => {
      const course = codeOf('g', item, index);
      grades[course] = valOf('g', index, 'B');
   });

   curriculum.now.forEach((item, index) => {
      const course = codeOf('s', item, index);

      courseStatuses[course] =
         valOf('s', index, 'กำลังศึกษา');
   });

   return {
      studentId: String(S.form.id ?? '').trim(),
      curriculum: S.cur,
      gpax: String(S.form.gpax ?? '').trim(),
      grades,
      courseStatuses,
      acknowledgements: [...S.ack]
   };
}

/* ---------- ตรวจรูปแบบ Response ---------- */
function isEligibilityResponse(data) {
   if (!data || typeof data !== 'object') {
      return false;
   }

   const stringArray = value =>
      Array.isArray(value) &&
      value.every(item => typeof item === 'string');

   return (
      typeof data.valid === 'boolean' &&
      (
         data.valid
            ? typeof data.eligible === 'boolean'
            : data.eligible === null
      ) &&
      typeof data.message === 'string' &&
      stringArray(data.errors) &&
      stringArray(data.reasons) &&
      stringArray(data.warnings)
   );
}

/* ---------- แปลงผล API สำหรับแสดงบน UI ---------- */
function convertApiResponse(data) {
   const results = [];

   if (!data.valid) {
      const errors = data.errors.length
         ? data.errors
         : [data.message];

      errors.forEach(error => {
         results.push({
            ok: 0,
            t: 'ข้อมูลไม่ถูกต้อง',
            d: error
         });
      });

      return results;
   }

   if (data.eligible) {
      results.push({
         ok: 1,
         t: 'ผลการตรวจสอบ',
         d: data.message
      });
   } else {
      // แสดงเฉพาะเหตุผล เพื่อไม่นับรายการไม่ผ่านซ้ำ
      const reasons = data.reasons.length
         ? data.reasons
         : [data.message];

      reasons.forEach(reason => {
         results.push({
            ok: 0,
            t: 'ไม่ผ่านคุณสมบัติ',
            d: reason
         });
      });
   }

   data.warnings.forEach(warning => {
      results.push({
         ok: 'w',
         t: 'คำเตือน',
         d: warning
      });
   });

   return results;
}

/* ---------- ควบคุมหน้าตรวจสอบคุณสมบัติ ---------- */
function initCheck() {
   const views = [
      'vForm',
      'vLoad',
      'vResult',
      'vDone'
   ];

   const show = id => showView(views, id);

   let res = [];
   let ok = false;
   let busy = false;
   let resultMessage = '';

   if (!Object.prototype.hasOwnProperty.call(CUR, S.cur)) {
      S.cur = '61';
   }

   $('#f_id').value = S.form.id;
   $('#f_gpax').value = S.form.gpax;
   $('#f_prog').value = S.cur;

   const sync = () => {
      $('#btnCheck').disabled =
         busy || !S.ack.every(value => value === true);
   };

   function draw() {
      const curriculum = CUR[S.cur];

      $('#coreRows').innerHTML = curriculum.core
         .map((item, index) =>
            rowHtml('g', item, index, GRADE_OPTS, 'B')
         )
         .join('');

      $('#nowRows').innerHTML = curriculum.now
         .map((item, index) =>
            rowHtml(
               's',
               item,
               index,
               STAT_OPTS,
               'กำลังศึกษา'
            )
         )
         .join('');

      $('#mustTxt').textContent = curriculum.must;

      $$('.cf').forEach((checkbox, index) => {
         checkbox.checked = S.ack[index];
      });

      sync();
   }

   draw();

   const form = $('#vForm');

   /* เก็บค่าจากช่องกรอกข้อมูล */
   form.addEventListener('input', event => {
      if (event.target.id === 'f_id') {
         S.form.id = event.target.value;
      }

      if (event.target.id === 'f_gpax') {
         S.form.gpax = event.target.value;
      }

      save();
   });

   /* เก็บค่าจากตัวเลือกและ checkbox */
   form.addEventListener('change', event => {
      const target = event.target;
      const data = target.dataset;

      if (data.t === 'cur') {
         S.cur = target.value;
         draw();
      } else if (data.t === 'alt') {
         S.alt[data.k] = Number(target.value);
      } else if (data.t === 'val') {
         S.val[data.k] = target.value;
      } else if (target.classList.contains('cf')) {
         S.ack[data.i] = target.checked;
         sync();
      }

      save();
   });

   /* ตรวจข้อมูลเบื้องต้นให้ตรงกับ Backend */
   function validateInput() {
      const id = String(S.form.id ?? '').trim();
      const gpax = String(S.form.gpax ?? '').trim();

      if (!id || !gpax) {
         return 'กรุณากรอกข้อมูลให้ครบถ้วน';
      }

      if (!/^\d{10}$/.test(id)) {
         return 'รหัสนักศึกษาต้องเป็นตัวเลข 10 หลัก';
      }

      if (
         !/^(?:[0-3](?:\.\d{1,2})?|4(?:\.0{1,2})?)$/.test(gpax)
      ) {
         return 'GPA ต้องเป็นตัวเลข 0.00–4.00 และมีทศนิยมไม่เกิน 2 ตำแหน่ง';
      }

      if (
         !Object.prototype.hasOwnProperty.call(CUR, S.cur)
      ) {
         return 'หลักสูตรต้องเป็น 61 หรือ 66';
      }

      if (!S.ack.every(value => value === true)) {
         return 'กรุณายืนยันเงื่อนไขให้ครบทั้ง 3 ข้อ';
      }

      return '';
   }

   /* เรียก API จริง */
   async function run(event) {
      if (event) {
         event.preventDefault();
      }

      if (busy) {
         return;
      }

      // อ่านค่าปัจจุบันจากหน้าฟอร์ม
      S.form.id = $('#f_id').value;
      S.form.gpax = $('#f_gpax').value;
      S.cur = $('#f_prog').value;

      const error = validateInput();
      const errorBox = $('#formError');

      errorBox.textContent = error;
      errorBox.hidden = error === '';

      if (error !== '') {
         setStepper(1);
         show('vForm');
         return;
      }

      busy = true;
      ok = false;
      sync();
      save();

      setStepper(2);
      show('vLoad');

      const controller = new AbortController();
      const timeoutId = setTimeout(
         () => controller.abort(),
         15000
      );

      try {
         const response = await fetch(
            ELIGIBILITY_API,
            {
               method: 'POST',

               headers: {
                  'Content-Type': 'application/json'
               },

               body: JSON.stringify(
                  buildEligibilityRequest()
               ),

               signal: controller.signal
            }
         );

         if (
            response.status !== 200 &&
            response.status !== 400
         ) {
            throw new Error(
               'เซิร์ฟเวอร์ตอบกลับ HTTP ' +
               response.status +
               ' กรุณาลองใหม่หรือตรวจสอบ Backend'
            );
         }

         const data = await response.json();

         if (!isEligibilityResponse(data)) {
            throw new Error(
               'รูปแบบข้อมูลตอบกลับจาก API ไม่ถูกต้อง'
            );
         }

         if (
            (response.status === 200 && !data.valid) ||
            (response.status === 400 && data.valid)
         ) {
            throw new Error(
               'HTTP Status ไม่ตรงกับผลการตรวจข้อมูล'
            );
         }

         res = convertApiResponse(data);

         ok =
            response.status === 200 &&
            data.valid === true &&
            data.eligible === true;

         resultMessage = data.message;
      } catch (error) {
         ok = false;
         resultMessage = 'ไม่สามารถตรวจสอบคุณสมบัติได้';

         let detail =
            'ไม่สามารถเชื่อมต่อ API ได้ กรุณาตรวจสอบ Backend ที่ localhost:8080';

         if (error.name === 'AbortError') {
            detail =
               'ระบบตอบกลับช้าเกิน 15 วินาที กรุณาลองใหม่';
         } else if (error instanceof SyntaxError) {
            detail =
               'API ไม่ได้ส่งข้อมูล JSON ที่ถูกต้อง';
         } else if (
            error instanceof Error &&
            !(error instanceof TypeError)
         ) {
            detail = error.message;
         }

         res = [
            {
               ok: 0,
               t: 'เกิดข้อผิดพลาดในการตรวจสอบ',
               d: detail
            }
         ];
      } finally {
         clearTimeout(timeoutId);
         busy = false;
         sync();
      }

      result();
   }

   /* แสดงผลโดยใช้ textContent กับข้อมูลจาก API */
   function result() {
      setStepper(3, ok ? 0 : 2);
      show('vResult');

      const alert = $('#rAlert');

      alert.className =
         'alert ' + (ok ? 'ok' : 'bad');

      alert.textContent = resultMessage;

      const list = $('#rList');
      list.replaceChildren();

      res.forEach(item => {
         const resultClass =
            item.ok === 1
               ? 'ok'
               : item.ok === 0
                  ? 'bad'
                  : 'warn';

         const symbol =
            item.ok === 1
               ? '✓'
               : item.ok === 0
                  ? '✕'
                  : '!';

         const row = document.createElement('div');
         row.className = 'res ' + resultClass;

         const dot = document.createElement('i');
         dot.className = 'dot';
         dot.textContent = symbol;

         const paragraph = document.createElement('p');

         const title = document.createElement('b');
         title.textContent = item.t;

         const detail = document.createElement('small');
         detail.textContent = item.d;

         paragraph.append(title, detail);
         row.append(dot, paragraph);

         if (item.v) {
            const value = document.createElement('em');
            value.textContent = item.v;
            row.append(value);
         }

         list.append(row);
      });

      $('#rNext').hidden = !ok;
      $('#rRetry').hidden = ok;
      $('#rEdit').hidden = ok;
   }

   const toForm = event => {
      if (event) {
         event.preventDefault();
      }

      setStepper(1);
      show('vForm');
   };

   $('#btnCheck').onclick = run;
   $('#rRetry').onclick = run;
   $('#rBack').onclick = toForm;
   $('#rEdit').onclick = toForm;

   $('#rNext').onclick = event => {
      event.preventDefault();

      if (!ok) {
         return;
      }

      setStepper(3, 0, true);
      show('vDone');
   };

   $('#dBack').onclick = event => {
      event.preventDefault();
      result();
   };

   $('#dNext').onclick = event => {
      if (!ok) {
         event.preventDefault();
         return;
      }

      S.unlocked = Math.max(S.unlocked, 2);
      save();
   };
}

/* =========================================================
   หน้ายื่นคำร้อง
   ========================================================= */
function initSubmit() {
   const views = [
      'vForm',
      'vLoad',
      'vResult'
   ];

   const show = id => showView(views, id);

   let ok = true;

   $('#s_id').value = S.form.id;
   $('#s_fac').value = S.form.fac;
   $('#s_major').value = S.form.major;
   $('#s_gpax').value = S.form.gpax;

   function go() {
      setStepper(2);
      show('vLoad');
      setTimeout(result, DELAY);
   }

   function result() {
      setStepper(3, ok ? 0 : 2);
      show('vResult');

      const alert = $('#sAlert');

      alert.className =
         'alert ' + (ok ? 'ok' : 'bad');

      alert.textContent = ok
         ? 'คำร้องของคุณผ่านการตรวจสอบแล้ว'
         : 'คำร้องไม่ผ่านการตรวจสอบ 1 รายการ กรุณาแก้ไขข้อมูล';

      const transcript = $('#sTr');

      transcript.className =
         'res ' + (ok ? 'ok' : 'bad');

      $('.dot', transcript).textContent =
         ok ? '✓' : '✕';

      $('small', transcript).textContent = ok
         ? 'ไฟล์อ่านได้และข้อมูลตรงกับระบบ'
         : 'ไม่พบไฟล์หรือไฟล์อ่านไม่ได้';

      $('#sNext').hidden = !ok;
      $('#sRetry').hidden = ok;
      $('#sEdit').hidden = ok;
   }

   $('#btnSend').onclick = () => {
      const full = [
         's_name',
         's_tel',
         's_mail',
         's_addr'
      ].every(id => $('#' + id).value.trim());

      $('#s_err').hidden = full;

      if (!full) {
         return;
      }

      // ต้นแบบตรวจเพียงว่ามีไฟล์แนบ
      ok = $('#s_tr').files.length > 0;

      go();
   };

   $('#sRetry').onclick = go;

   $('#sEdit').onclick = () => {
      setStepper(1);
      show('vForm');
   };

   $('#sNext').onclick = () => {
      S.unlocked = 3;
      save();
   };
}

/* =========================================================
   หน้าจัดอันดับบริษัท
   ========================================================= */
function initRanking() {
   const selectors = $$('select[data-k]');

   selectors.forEach(select => {
      select.value = S.prio[select.dataset.k] || '';
   });

   function list() {
      const choices = Object.entries(S.prio)
         .filter(([, value]) => value)
         .sort((a, b) => a[1] - b[1]);

      $('#chosen').innerHTML = choices.length
         ? choices.map(([key, value]) =>
            `<div>${value}. บริษัท ${key}</div>`
         ).join('')
         : '<small class="mute">ยังไม่ได้เลือก</small>';
   }

   selectors.forEach(select => {
      select.onchange = () => {
         S.prio[select.dataset.k] = select.value;

         save();

         $('#msgOk').hidden = true;
         $('#msgBad').hidden = true;

         list();
      };
   });

   $('#btnConfirm').onclick = () => {
      const values =
         Object.values(S.prio).filter(Boolean);

      const error = !values.length
         ? 'กรุณาเลือกบริษัทอย่างน้อย 1 แห่ง'
         : new Set(values).size !== values.length
            ? 'มีลำดับซ้ำกันมากกว่า 1 บริษัท กรุณาเลือกลำดับใหม่'
            : '';

      $('#msgBad').hidden = !error;
      $('#msgOk').hidden = Boolean(error);

      if (error) {
         $('#msgBadTxt').textContent = error;
      }
   };

   list();
}