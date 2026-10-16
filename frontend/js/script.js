/* =====================================================================
   script.js — logic ทั้งหมดของระบบสหกิจศึกษา (ใช้ร่วมกัน 4 หน้า)
   หน้า: login.html · check.html · submit.html · ranking.html
   แต่ละหน้าบอกตัวเองด้วย <body data-page="..."> แล้วเรียก init ของหน้านั้น
   ===================================================================== */

/* ---------- ตัวช่วยพื้นฐาน ---------- */
const $=(s,r=document)=>r.querySelector(s);
const $$=(s,r=document)=>[...r.querySelectorAll(s)];

/* ---------- สถานะที่ใช้ร่วมกันทุกหน้า (เก็บใน sessionStorage เพื่อข้ามหน้าได้) ---------- */
const KEY='coop-state';
const DEF={user:null,unlocked:1,
 form:{id:'6410000001',fac:'วิทยาศาสตร์',major:'วิทยาการคอมพิวเตอร์',gpax:'3.10'},
 cur:'61',val:{},alt:{},ack:[false,false,false],prio:{A:'',B:'',C:'',D:''}};
const fresh=()=>JSON.parse(JSON.stringify(DEF));
function load(){try{return {...fresh(),...JSON.parse(sessionStorage.getItem(KEY)||'{}')}}catch(e){return fresh()}}
function save(){try{sessionStorage.setItem(KEY,JSON.stringify(S))}catch(e){}}
let S=load();

/* ---------- ส่วนประกอบร่วม ---------- */
/* stepper: cur = ขั้นปัจจุบัน, errAt = ขั้นที่ล้มเหลว, allDone = ทุกขั้นเสร็จ */
function setStepper(cur,errAt=0,allDone=false){
 $$('.steps .st').forEach((el,i)=>{
  const n=i+1;let c='',d=n;
  if(errAt===n){c='err';d='✕'}
  else if(n<cur||(n===cur&&allDone)){c='done';d='✓'}
  else if(n===cur)c='cur';
  el.className='st '+c;$('.dot',el).textContent=d;
 });
}
/* แสดงเฉพาะ view ที่เลือก (ซ่อนที่เหลือ) */
const showView=(ids,id)=>ids.forEach(v=>$('#'+v).hidden=v!==id);
/* จำลองเวลาประมวลผล */
const DELAY=1400;

/* หน้าที่ต้องปลดล็อกก่อนถึงเข้าได้ */
const NEED={check:1,submit:2,ranking:3};
/* หน้าแรกหลัง login = ขั้นแรกที่ยังทำไม่เสร็จ (unlocked 1→check, 2→submit, 3→ranking) */
const HOME=()=>['check','submit','ranking'][Math.min(Math.max(S.unlocked,1),3)-1]+'.html';

function initShell(){
 $('#who').textContent=S.user;
 $$('nav a').forEach((a,i)=>{if(i+1>S.unlocked){a.classList.add('disabled');a.setAttribute('aria-disabled','true');a.tabIndex=-1}});
 $('#logout').onclick=()=>{S.user=null;save();location.href='login.html'};   // เก็บความคืบหน้าไว้ เพื่อให้ login ครั้งหน้าไปต่อขั้นเดิม
}

document.addEventListener('DOMContentLoaded',()=>{
 const page=document.body.dataset.page;
 if(page==='login')return initLogin();
 if(!S.user)return location.replace('login.html');
 if(S.unlocked<NEED[page])return location.replace('check.html');
 initShell();
 ({check:initCheck,submit:initSubmit,ranking:initRanking})[page]();
});

/* =====================================================================
   หน้า 1: LOGIN (login.html)
   ===================================================================== */
function initLogin(){
 if(S.user)return location.replace(HOME());
 $('#loginForm').addEventListener('submit',e=>{
  e.preventDefault();
  const u=$('#u').value.trim(),p=$('#p').value;
  if(u&&p==='1234'){S.user=u;save();location.href=HOME()}   // ทดลอง: รหัสผ่าน 1234
  else $('#loginErr').hidden=false;
 });
}

/* =====================================================================
   หน้า 2: ตรวจสอบคุณสมบัติ (check.html) — เกณฑ์ปีการศึกษา 2568
   ===================================================================== */
const CUR={
 '61':{core:['คพ.101','คพ.102','คพ.111',['คพ.213','คพ.216'],'คพ.251','คพ.264'],now:['คพ.384',['คพ.266','คพ.322','คพ.348']],must:'คพ.302'},
 '66':{core:['คพ.100','คพ.101','คพ.102','คพ.111',['คพ.213','คพ.216'],'คพ.251','คพ.261'],now:['คพ.180',['คพ.262','คพ.331','คพ.240']],must:'คพ.301'}};
const GRADE={'A':4,'B+':3.5,'B':3,'C+':2.5,'C':2,'D+':1.5,'D':1,'F':0};
const GRADE_OPTS=['ยังไม่เคยเรียน',...Object.keys(GRADE)];
const MIN_GRADE=2.5;   // เกรดขั้นต่ำของแต่ละรายวิชาในข้อ 2
const STAT_OPTS=['ยังไม่เคยลง','กำลังศึกษา','เคยศึกษาแล้ว'];

/* sec: 'g' = วิชาที่เคยศึกษา, 's' = วิชากำลังศึกษา/เคยศึกษา */
const K=(sec,i)=>S.cur+sec+i;
const valOf=(sec,i,def)=>{const k=K(sec,i);return k in S.val?S.val[k]:def};
const codeOf=(sec,it,i)=>Array.isArray(it)?it[S.alt[K(sec,i)]||0]:it;

function rowHtml(sec,it,i,list,def){
 const k=K(sec,i),v=valOf(sec,i,def),grp=Array.isArray(it);
 return `<div class="row"><b>${grp?'เลือก 1':it}</b>${grp
  ?`<select data-t="alt" data-k="${k}" aria-label="เลือกรายวิชา">${it.map((c,j)=>`<option value="${j}" ${(S.alt[k]||0)===j?'selected':''}>${c}</option>`).join('')}</select>`
  :'<small></small>'}
  <select data-t="val" data-k="${k}" aria-label="${grp?'ผล':it}">${list.map(x=>`<option ${x===v?'selected':''}>${x}</option>`).join('')}</select></div>`;
}

/* ตรวจตามเกณฑ์: ok = 1 ผ่าน | 0 ไม่ผ่าน | 'w' เตือน */
function evaluate(){
 const c=CUR[S.cur],g=parseFloat(S.form.gpax),out=[];
 // ข้อ 1: สาขาวิชา
 out.push({ok:1,t:'สาขาวิชา',d:'นักศึกษาสาขาวิทยาการคอมพิวเตอร์ คณะวิทยาศาสตร์ฯ ม.ธรรมศาสตร์'});
 // ข้อ 2: เคยศึกษาวิชาแกน — "แต่ละวิชา" ต้องได้เกรดไม่ต่ำกว่า 2.50 (C+ ขึ้นไป)
 // ไม่เฉลี่ยรวมกัน: วิชาไหนไม่ถึงเกณฑ์ = ไม่ผ่านทั้งหมวด และแสดงเป็นรายวิชา
 c.core.forEach((it,i)=>{
  const code=codeOf('g',it,i),gr=valOf('g',i,'B');
  if(!(gr in GRADE))out.push({ok:0,t:code,d:'ต้องเคยเรียนและได้เกรดไม่ต่ำกว่า 2.50',v:'ยังไม่เคยเรียน'});
  else if(GRADE[gr]<MIN_GRADE)out.push({ok:0,t:code,d:'ต้องได้เกรดไม่ต่ำกว่า 2.50 (C+ ขึ้นไป)',v:'เกรด '+gr});
  else out.push({ok:1,t:code,d:'เกรด '+gr+' (ผ่านเกณฑ์ไม่ต่ำกว่า 2.50)'});
 });
 // ข้อ 3: กำลังศึกษา หรือเคยศึกษา
 const nm=[],nd=[];
 c.now.forEach((it,i)=>{const code=codeOf('s',it,i),st=valOf('s',i,'กำลังศึกษา');nd.push(code+' ('+st+')');if(st==='ยังไม่เคยลง')nm.push(code)});
 out.push({ok:nm.length?0:1,t:'กำลังศึกษา หรือเคยศึกษารายวิชา',d:nd.join(' · '),v:nm.length?'ยังไม่ลง '+nm.join(', '):''});
 // ข้อ 5: GPA >= 2.75
 const valid=isFinite(g)&&g>=0&&g<=4;
 out.push({ok:valid&&g>=2.75?1:0,t:'เกรดเฉลี่ยสะสม (GPA) ไม่ต่ำกว่า 2.75',d:valid?'ของคุณ '+g.toFixed(2):'กรอก GPA ไม่ถูกต้อง',v:valid&&g>=2.75?'':(valid?g.toFixed(2):'ไม่ถูกต้อง')});
 // ข้อ 4: แจ้งเตือนวิชาที่ต้องสอบได้
 out.push({ok:'w',t:'ต้องสอบได้วิชา '+c.must,d:'ก่อนออกไปปฏิบัติงานจริง ณ สถานประกอบการ'});
 return out;
}

function initCheck(){
 const V=['vForm','vLoad','vResult','vDone'],show=id=>showView(V,id);
 let res=[],ok=true;

 /* เติมค่าเริ่มต้นและวาดแถวรายวิชาตามหลักสูตร */
 $('#f_id').value=S.form.id;$('#f_gpax').value=S.form.gpax;$('#f_prog').value=S.cur;
 const sync=()=>{$('#btnCheck').disabled=!S.ack.every(Boolean)};
 function draw(){
  const c=CUR[S.cur];
  $('#coreRows').innerHTML=c.core.map((it,i)=>rowHtml('g',it,i,GRADE_OPTS,'B')).join('');
  $('#nowRows').innerHTML=c.now.map((it,i)=>rowHtml('s',it,i,STAT_OPTS,'กำลังศึกษา')).join('');
  $('#mustTxt').textContent=c.must;
  $$('.cf').forEach((x,i)=>x.checked=S.ack[i]);
  sync();
 }
 draw();

 /* เก็บค่าที่กรอก */
 const form=$('#vForm');
 form.addEventListener('input',e=>{
  if(e.target.id==='f_id')S.form.id=e.target.value;
  if(e.target.id==='f_gpax')S.form.gpax=e.target.value;
  save();
 });
 form.addEventListener('change',e=>{
  const t=e.target,d=t.dataset;
  if(d.t==='cur'){S.cur=t.value;draw()}
  else if(d.t==='alt')S.alt[d.k]=+t.value;
  else if(d.t==='val')S.val[d.k]=t.value;
  else if(t.classList.contains('cf')){S.ack[d.i]=t.checked;sync()}
  save();
 });

 /* ขั้น 2: กำลังตรวจสอบ -> ขั้น 3: ผลลัพธ์ */
 function run(){
  const g=parseFloat(S.form.gpax);if(isFinite(g))S.form.gpax=g.toFixed(2);
  res=evaluate();ok=res.every(r=>r.ok!==0);save();
  setStepper(2);show('vLoad');
  setTimeout(result,DELAY);
 }
 function result(){
  const bad=res.filter(r=>r.ok===0).length;
  setStepper(3,ok?0:2);show('vResult');
  const a=$('#rAlert');a.className='alert '+(ok?'ok':'bad');
  a.textContent=ok?'คุณมีคุณสมบัติครบถ้วนตามเกณฑ์':`ไม่ผ่านคุณสมบัติ ${bad} รายการ กรุณาตรวจสอบข้อมูลและลองใหม่อีกครั้ง`;
  $('#rList').innerHTML=res.map(r=>`<div class="res ${r.ok===1?'ok':r.ok===0?'bad':'warn'}"><i class="dot">${r.ok===1?'✓':r.ok===0?'✕':'!'}</i><p><b>${r.t}</b><small>${r.d}</small></p>${r.v?`<em>${r.v}</em>`:''}</div>`).join('');
  $('#rNext').hidden=!ok;$('#rRetry').hidden=ok;$('#rEdit').hidden=ok;
 }
 const toForm=()=>{setStepper(1);show('vForm')};

 $('#btnCheck').onclick=run;
 $('#rRetry').onclick=run;
 $('#rBack').onclick=toForm;
 $('#rEdit').onclick=toForm;
 $('#rNext').onclick=()=>{setStepper(3,0,true);show('vDone')};   // ตรวจสอบสำเร็จ
 $('#dBack').onclick=result;
 $('#dNext').onclick=()=>{S.unlocked=Math.max(S.unlocked,2);save()}; // ปลดล็อกหน้ายื่นคำร้อง แล้วลิงก์ไป submit.html
}

/* =====================================================================
   หน้า 3: ยื่นคำร้อง (submit.html)
   ===================================================================== */
function initSubmit(){
 const V=['vForm','vLoad','vResult'],show=id=>showView(V,id);
 let ok=true;
 $('#s_id').value=S.form.id;$('#s_fac').value=S.form.fac;
 $('#s_major').value=S.form.major;$('#s_gpax').value=S.form.gpax;

 function go(){setStepper(2);show('vLoad');setTimeout(result,DELAY)}
 function result(){
  setStepper(3,ok?0:2);show('vResult');
  const a=$('#sAlert');a.className='alert '+(ok?'ok':'bad');
  a.textContent=ok?'คำร้องของคุณผ่านการตรวจสอบแล้ว':'คำร้องไม่ผ่านการตรวจสอบ 1 รายการ กรุณาแก้ไขข้อมูล';
  const r=$('#sTr');r.className='res '+(ok?'ok':'bad');
  $('.dot',r).textContent=ok?'✓':'✕';
  $('small',r).textContent=ok?'ไฟล์อ่านได้และข้อมูลตรงกับระบบ':'ไม่พบไฟล์หรือไฟล์อ่านไม่ได้';
  $('#sNext').hidden=!ok;$('#sRetry').hidden=ok;$('#sEdit').hidden=ok;
 }
 $('#btnSend').onclick=()=>{
  const full=['s_name','s_tel','s_mail','s_addr'].every(i=>$('#'+i).value.trim());
  $('#s_err').hidden=full;if(!full)return;
  ok=$('#s_tr').files.length>0;       // ไม่แนบ Transcript = ไม่ผ่าน
  go();
 };
 $('#sRetry').onclick=go;
 $('#sEdit').onclick=()=>{setStepper(1);show('vForm')};
 $('#sNext').onclick=()=>{S.unlocked=3;save()};   // ปลดล็อกหน้าจับคู่บริษัท แล้วลิงก์ไป ranking.html
}

/* =====================================================================
   หน้า 4: จับคู่บริษัท (ranking.html)
   ===================================================================== */
function initRanking(){
 const sels=$$('select[data-k]');
 sels.forEach(s=>s.value=S.prio[s.dataset.k]||'');

 function list(){
  const ch=Object.entries(S.prio).filter(([,v])=>v).sort((a,b)=>a[1]-b[1]);
  $('#chosen').innerHTML=ch.length?ch.map(([k,v])=>`<div>${v}. บริษัท ${k}</div>`).join(''):'<small class="mute">ยังไม่ได้เลือก</small>';
 }
 sels.forEach(s=>s.onchange=()=>{
  S.prio[s.dataset.k]=s.value;save();
  $('#msgOk').hidden=true;$('#msgBad').hidden=true;list();
 });
 $('#btnConfirm').onclick=()=>{
  const v=Object.values(S.prio).filter(Boolean);
  const err=!v.length?'กรุณาเลือกบริษัทอย่างน้อย 1 แห่ง'
   :new Set(v).size!==v.length?'มีลำดับซ้ำกันมากกว่า 1 บริษัท กรุณาเลือกลำดับใหม่':'';
  $('#msgBad').hidden=!err;$('#msgOk').hidden=!!err;
  if(err)$('#msgBadTxt').textContent=err;
 };
 list();
}
