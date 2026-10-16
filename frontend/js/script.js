/* =========================================================
   script.js
   ใช้ร่วมกัน: login, check, submit, ranking
   ========================================================= */

/* ---------- ตัวช่วยค้นหา HTML ---------- */
const $ = (selector, root = document) =>
    root.querySelector(selector);

const $$ = (selector, root = document) => [...root.querySelectorAll(selector)];

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

const fresh = () => JSON.parse(JSON.stringify(DEF));

function load() {
    try {
        return {
            ...fresh(),
            ...JSON.parse(sessionStorage.getItem(KEY) || '{}')
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
        } else if (step < cur || (step === cur && allDone)) {
            className = 'done';
            label = '✓';
        } else if (step === cur) {
            className = 'cur';
        }

        element.className = 'st ' + className;
        $('.dot', element).textContent = label;
    });
}

const showView = (ids, currentId) => {
    ids.forEach(id => {
        $('#' + id).hidden = id !== currentId;
    });
};

const DELAY = 1400;

const NEED = {
    check: 1,
    submit: 2,
    ranking: 3
};

const HOME = () => {
    const index = Math.min(Math.max(S.unlocked, 1), 3) - 1;
    return ['check', 'submit', 'ranking'][index] + '.html';
};

function initShell() {
    $('#who').textContent = S.user;

    $$('nav a').forEach((link, index) => {
        if (index + 1 > S.unlocked) {
            link.classList.add('disabled');
            link.setAttribute('aria-disabled', 'true');
            link.tabIndex = -1;
        }
    });

    $('#logout').onclick = () => {
        S.user = null;
        save();
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

    if (S.unlocked < NEED[page]) {
        return location.replace('check.html');
    }

    initShell();

    const initializers = {
        check: initCheck,
        submit: initSubmit,
        ranking: initRanking
    };

    initializers[page]();
});

/* =========================================================
   หน้า Login
   ========================================================= */
function initLogin() {
    if (S.user) {
        return location.replace(HOME());
    }

    $('#loginForm').addEventListener('submit', event => {
        event.preventDefault();

        const username = $('#u').value.trim();
        const password = $('#p').value;

        // การล็อกอินจำลองสำหรับต้นแบบ
        if (username && password === '1234') {
            S.user = username;
            save();
            location.href = HOME();
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
const K = (section, index) => S.cur + section + index;

const valOf = (section, index, defaultValue) => {
    const key = K(section, index);

    return key in S.val ? S.val[key] : defaultValue;
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
                  ${(S.alt[key] || 0) === choiceIndex ? 'selected' : ''}>
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

/* ---------- ประเมินคุณสมบัติ ---------- */
function evaluate() {
   const curriculum = CUR[S.cur];
   const gpa = Number(S.form.gpax);
   const results = [];

   // สาขาวิชาตามข้อมูลต้นแบบ
   results.push({
      ok: 1,
      t: 'สาขาวิชา',
      d: 'นักศึกษาสาขาวิทยาการคอมพิวเตอร์ คณะวิทยาศาสตร์ฯ ม.ธรรมศาสตร์'
   });

   // แต่ละวิชาแกนต้องได้ C+ ขึ้นไป
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

   // รายวิชาที่กำลังศึกษา หรือเคยศึกษา
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

   // GPA ต้องไม่ต่ำกว่า 2.75
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

   // ข้อความเตือนก่อนออกปฏิบัติงาน
   results.push({
      ok: 'w',
      t: 'ต้องสอบได้วิชา ' + curriculum.must,
      d: 'ก่อนออกไปปฏิบัติงานจริง ณ สถานประกอบการ'
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
   let ok = true;

   $('#f_id').value = S.form.id;
   $('#f_gpax').value = S.form.gpax;
   $('#f_prog').value = S.cur;

   const sync = () => {
      $('#btnCheck').disabled = !S.ack.every(Boolean);
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
            rowHtml('s', item, index, STAT_OPTS, 'กำลังศึกษา')
         )
         .join('');

      $('#mustTxt').textContent = curriculum.must;

      $$('.cf').forEach((checkbox, index) => {
         checkbox.checked = S.ack[index];
      });

      sync();
   }

   draw();

   /* เก็บค่าจากช่องกรอกข้อมูล */
   const form = $('#vForm');

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

   /* เพิ่ม: ตรวจข้อมูลก่อนประเมินคุณสมบัติ */
   function validateInput() {
      const id = S.form.id.trim();
      const gpax = S.form.gpax.trim();

      if (!id || !gpax) {
         return 'กรุณากรอกข้อมูลให้ครบถ้วน';
      }

      // ตรวจทั้งข้อความ ไม่ยอมรับค่าอย่าง 3.10abc
      if (!/^\d+(\.\d+)?$/.test(gpax)) {
         return 'GPA ต้องเป็นตัวเลข เช่น 2.75 หรือ 3.10';
      }

      const gpaValue = Number(gpax);

      if (
         !Number.isFinite(gpaValue) ||
         gpaValue < 0 ||
         gpaValue > 4
      ) {
         return 'GPA ต้องอยู่ระหว่าง 0.00 ถึง 4.00';
      }

      return '';
   }

   /* เริ่มตรวจสอบ */
   function run() {
      const error = validateInput();
      const errorBox = $('#formError');

      errorBox.textContent = error;
      errorBox.hidden = error === '';

      if (error !== '') {
         return;
      }

      // ใช้ค่าจริงในการประเมิน ไม่ปัดเศษก่อนเทียบเกณฑ์
      // เช่น 2.749 ต้องไม่ถูกปัดเป็น 2.75 แล้วผ่าน
      res = evaluate();
      ok = res.every(result => result.ok !== 0);

      save();

      setStepper(2);
      show('vLoad');

      setTimeout(result, DELAY);
   }

   /* แสดงผล */
   function result() {
      const failedCount = res.filter(
         item => item.ok === 0
      ).length;

      setStepper(3, ok ? 0 : 2);
      show('vResult');

      const alert = $('#rAlert');

      alert.className = 'alert ' + (ok ? 'ok' : 'bad');

      alert.textContent = ok
         ? 'คุณมีคุณสมบัติครบถ้วนตามเกณฑ์'
         : `ไม่ผ่านคุณสมบัติ ${failedCount} รายการ กรุณาตรวจสอบข้อมูลและลองใหม่อีกครั้ง`;

      $('#rList').innerHTML = res.map(item => {
         const resultClass = item.ok === 1
            ? 'ok'
            : item.ok === 0
               ? 'bad'
               : 'warn';

         const symbol = item.ok === 1
            ? '✓'
            : item.ok === 0
               ? '✕'
               : '!';

         return `
            <div class="res ${resultClass}">
               <i class="dot">${symbol}</i>

               <p>
                  <b>${item.t}</b>
                  <small>${item.d}</small>
               </p>

               ${item.v ? `<em>${item.v}</em>` : ''}
            </div>
         `;
      }).join('');

      $('#rNext').hidden = !ok;
      $('#rRetry').hidden = ok;
      $('#rEdit').hidden = ok;
   }

   const toForm = () => {
      setStepper(1);
      show('vForm');
   };

   $('#btnCheck').onclick = run;
   $('#rRetry').onclick = run;
   $('#rBack').onclick = toForm;
   $('#rEdit').onclick = toForm;

   $('#rNext').onclick = () => {
      setStepper(3, 0, true);
      show('vDone');
   };

   $('#dBack').onclick = result;

   $('#dNext').onclick = () => {
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
      alert.className = 'alert ' + (ok ? 'ok' : 'bad');

      alert.textContent = ok
         ? 'คำร้องของคุณผ่านการตรวจสอบแล้ว'
         : 'คำร้องไม่ผ่านการตรวจสอบ 1 รายการ กรุณาแก้ไขข้อมูล';

      const transcript = $('#sTr');
      transcript.className = 'res ' + (ok ? 'ok' : 'bad');

      $('.dot', transcript).textContent = ok ? '✓' : '✕';

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
      const values = Object.values(S.prio).filter(Boolean);

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