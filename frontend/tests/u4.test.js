// @vitest-environment node
/*
 * U4:T7 Unit test ส่วน UI : หน้าจับคู่บริษัท (ranking.html)
 * test cases อ้างอิง .doc/UserStory/U4/T11.md (ลำดับ 1–9)
 * รัน: npm run test:ui -- frontend/tests/u4.test.js
 */

import { describe, test, expect, afterEach } from 'vitest';
import { JSDOM } from 'jsdom';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const testDir = path.dirname(fileURLToPath(import.meta.url));
const html = fs.readFileSync(path.join(testDir, '..', 'ranking.html'), 'utf8');
const script = fs.readFileSync(path.join(testDir, '..', 'js', 'script.js'), 'utf8');

const openedWindows = [];

afterEach(() => {
    for (const window of openedWindows) {
        window.close();
    }
    openedWindows.length = 0;
});

/* นักศึกษาที่ผ่านขั้นตรวจคุณสมบัติและยื่นคำร้องแล้ว (unlocked = 3) จึงเข้าหน้า ranking ได้ */
function openRankingPage(prio = { A: '', B: '', C: '', D: '' }, storage = null) {
    const dom = new JSDOM(html, {
        url: 'http://localhost/ranking.html',
        runScripts: 'outside-only'
    });
    const { window } = dom;
    openedWindows.push(window);

    const state = storage ?? JSON.stringify({
        user: '6609650003',
        unlocked: 3,
        prio
    });
    window.sessionStorage.setItem('coop-state', state);

    window.eval(script);
    window.document.dispatchEvent(new window.Event('DOMContentLoaded'));
    return window;
}

const $ = (window, selector) => window.document.querySelector(selector);

function choose(window, key, rank) {
    const select = $(window, `select[data-k="${key}"]`);
    expect(select).not.toBeNull();
    select.value = rank;
    select.dispatchEvent(new window.Event('change', { bubbles: true }));
}

const chosenLines = window =>
    [...$(window, '#chosen').querySelectorAll('div')].map(d => d.textContent.trim());

describe('U4-UI หน้าจับคู่บริษัท (ranking.html)', () => {
    test('1 เปิดหน้า แสดงบริษัท A–D และกล่องลำดับที่เลือกขึ้น "ยังไม่ได้เลือก"', () => {
        const window = openRankingPage();
        const keys = [...window.document.querySelectorAll('select[data-k]')].map(s => s.dataset.k);

        expect(keys).toEqual(['A', 'B', 'C', 'D']);
        expect($(window, '#chosen').textContent).toContain('ยังไม่ได้เลือก');
    });

    test('2 dropdown ของบริษัท A มีตัวเลือก ไม่เลือก, อันดับ 1, 2, 3', () => {
        const window = openRankingPage();
        const options = [...$(window, 'select[data-k="A"]').options].map(o => o.value);

        expect(options).toEqual(['', '1', '2', '3']);
    });

    test('3 A = อันดับ 2, B = อันดับ 1 กล่องลำดับที่เลือกเรียงตามอันดับ', () => {
        const window = openRankingPage();
        choose(window, 'A', '2');
        choose(window, 'B', '1');

        expect(chosenLines(window)).toEqual(['1. บริษัท B', '2. บริษัท A']);
    });

    test('4 เปลี่ยนบริษัท A กลับเป็น ไม่เลือก บริษัท A หายจากกล่องลำดับที่เลือก', () => {
        const window = openRankingPage();
        choose(window, 'A', '1');
        choose(window, 'A', '');

        expect(chosenLines(window)).toEqual([]);
        expect($(window, '#chosen').textContent).toContain('ยังไม่ได้เลือก');
    });

    test('5 ไม่เลือกบริษัทเลยแล้วกดยืนยัน แสดง ไม่สำเร็จ และ "กรุณาเลือกบริษัทอย่างน้อย 1 แห่ง"', () => {
        const window = openRankingPage();
        $(window, '#btnConfirm').click();

        expect($(window, '#msgBad').hidden).toBe(false);
        expect($(window, '#msgOk').hidden).toBe(true);
        expect($(window, '#msgBadTxt').textContent).toBe('กรุณาเลือกบริษัทอย่างน้อย 1 แห่ง');
    });

    test('6 A = 1, B = 1 แล้วกดยืนยัน แสดง ไม่สำเร็จ และแจ้งว่ามีลำดับซ้ำ', () => {
        const window = openRankingPage();
        choose(window, 'A', '1');
        choose(window, 'B', '1');
        $(window, '#btnConfirm').click();

        expect($(window, '#msgBad').hidden).toBe(false);
        expect($(window, '#msgOk').hidden).toBe(true);
        expect($(window, '#msgBadTxt').textContent)
            .toBe('มีลำดับซ้ำกันมากกว่า 1 บริษัท กรุณาเลือกลำดับใหม่');
    });

    test('7 A = 1, B = 2, C = 3 แล้วกดยืนยัน แสดง "สำเร็จ บันทึกลำดับเรียบร้อยแล้ว"', () => {
        const window = openRankingPage();
        choose(window, 'A', '1');
        choose(window, 'B', '2');
        choose(window, 'C', '3');
        $(window, '#btnConfirm').click();

        expect($(window, '#msgOk').hidden).toBe(false);
        expect($(window, '#msgBad').hidden).toBe(true);
        expect($(window, '#msgOk').textContent).toContain('สำเร็จ');
        expect($(window, '#msgOk').textContent).toContain('บันทึกลำดับเรียบร้อยแล้ว');
    });

    test('8 หลังมีข้อความแจ้งผล เปลี่ยนอันดับใหม่ ข้อความสำเร็จ / ไม่สำเร็จถูกซ่อน', () => {
        const window = openRankingPage();
        $(window, '#btnConfirm').click();
        expect($(window, '#msgBad').hidden).toBe(false);

        choose(window, 'A', '1');

        expect($(window, '#msgBad').hidden).toBe(true);
        expect($(window, '#msgOk').hidden).toBe(true);
    });

    test('9 เลือกอันดับแล้วออกจากหน้า กลับมาใหม่ dropdown ยังแสดงอันดับเดิม', () => {
        const first = openRankingPage();
        choose(first, 'A', '1');
        choose(first, 'C', '2');
        const saved = first.sessionStorage.getItem('coop-state');

        const second = openRankingPage(undefined, saved);

        expect($(second, 'select[data-k="A"]').value).toBe('1');
        expect($(second, 'select[data-k="C"]').value).toBe('2');
        expect(chosenLines(second)).toEqual(['1. บริษัท A', '2. บริษัท C']);
    });
});