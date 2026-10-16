// @vitest-environment node
import { describe, test, expect } from 'vitest';
import { JSDOM } from 'jsdom';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const testDir = path.dirname(fileURLToPath(
    import.meta.url));

const html = fs.readFileSync(
    path.join(testDir, '..', 'check.html'),
    'utf8'
);

const script = fs.readFileSync(
    path.join(testDir, '..', 'js', 'script.js'),
    'utf8'
);

function openCheckPage() {
    const dom = new JSDOM(html, {
        url: 'http://localhost/check.html',
        runScripts: 'outside-only'
    });

    const { window } = dom;

    window.sessionStorage.setItem('coop-state', JSON.stringify({
        user: '6410000001',
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
    }));

    // ให้การจำลองโหลดผลเสร็จทันที
    window.setTimeout = callback => {
        callback();
        return 1;
    };

    window.eval(script);

    window.document.dispatchEvent(
        new window.Event('DOMContentLoaded')
    );

    return window;
}

function confirmAll(window) {
    window.document.querySelectorAll('.cf').forEach(checkbox => {
        checkbox.checked = true;

        checkbox.dispatchEvent(
            new window.Event('change', {
                bubbles: true
            })
        );
    });
}

function enter(window, selector, value) {
    const input = window.document.querySelector(selector);

    input.value = value;

    input.dispatchEvent(
        new window.Event('input', {
            bubbles: true
        })
    );
}

describe('U2:T7 หน้า UI ตรวจสอบคุณสมบัติ', () => {

    test('ปุ่มตรวจสอบถูกปิดเมื่อยังยืนยันเงื่อนไขไม่ครบ', () => {
        const window = openCheckPage();

        const button =
            window.document.querySelector('#btnCheck');

        expect(button.disabled).toBe(true);
    });

    test('ปุ่มตรวจสอบเปิดเมื่อยืนยันครบทั้ง 3 ข้อ', () => {
        const window = openCheckPage();

        confirmAll(window);

        const button =
            window.document.querySelector('#btnCheck');

        expect(button.disabled).toBe(false);
    });

    test('GPA 2.75 และข้อมูลอื่นผ่าน แสดงผลว่าผ่าน', () => {
        const window = openCheckPage();

        enter(window, '#f_gpax', '2.75');
        confirmAll(window);

        window.document.querySelector('#btnCheck').click();

        const result =
            window.document.querySelector('#rAlert').textContent;

        expect(result).toContain(
            'คุณมีคุณสมบัติครบถ้วน'
        );
    });

    test('GPA 2.74 แสดงผลว่าไม่ผ่าน', () => {
        const window = openCheckPage();

        enter(window, '#f_gpax', '2.74');
        confirmAll(window);

        window.document.querySelector('#btnCheck').click();

        const result =
            window.document.querySelector('#rAlert').textContent;

        expect(result).toContain(
            'ไม่ผ่านคุณสมบัติ'
        );
    });

    test('กรอกข้อมูลไม่ครบ แสดงข้อความผิดพลาด', () => {
        const window = openCheckPage();

        enter(window, '#f_id', '');
        confirmAll(window);

        window.document.querySelector('#btnCheck').click();

        const error =
            window.document.querySelector('#formError');

        expect(error.textContent).toContain(
            'กรอกข้อมูลให้ครบถ้วน'
        );

        expect(
            window.document.querySelector('#vForm').hidden
        ).toBe(false);
    });

    test('กรอก GPA ผิดรูปแบบ แสดงข้อความผิดพลาด', () => {
        const window = openCheckPage();

        enter(window, '#f_gpax', '3.10abc');
        confirmAll(window);

        window.document.querySelector('#btnCheck').click();

        const error =
            window.document.querySelector('#formError');

        expect(error.textContent).toContain(
            'GPA ต้องเป็นตัวเลข'
        );

        expect(
            window.document.querySelector('#vForm').hidden
        ).toBe(false);
    });

});