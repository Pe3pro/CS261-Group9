// @vitest-environment node

import {
    describe,
    test,
    expect,
    vi
} from 'vitest';

import { JSDOM } from 'jsdom';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const testDir = path.dirname(
    fileURLToPath(
        import.meta.url)
);

const html = fs.readFileSync(
    path.join(testDir, '..', 'check.html'),
    'utf8'
);

const script = fs.readFileSync(
    path.join(testDir, '..', 'js', 'script.js'),
    'utf8'
);

/*
 * Response จำลองสำหรับกรณีผ่านคุณสมบัติ
 */
const passedResponse = {
    valid: true,
    eligible: true,
    message: 'ผ่านคุณสมบัติ',
    errors: [],
    reasons: [],
    warnings: [
        'ต้องสอบได้วิชา คพ.302 ก่อนออกปฏิบัติงาน'
    ]
};

/*
 * Response จำลองสำหรับกรณีไม่ผ่านคุณสมบัติ
 */
const failedResponse = {
    valid: true,
    eligible: false,
    message: 'ไม่ผ่านคุณสมบัติ',
    errors: [],
    reasons: [
        'GPA ต้องไม่ต่ำกว่า 2.75'
    ],
    warnings: [
        'ต้องสอบได้วิชา คพ.302 ก่อนออกปฏิบัติงาน'
    ]
};

function openCheckPage(
    apiResponse = passedResponse,
    status = 200
) {
    const dom = new JSDOM(html, {
        url: 'http://localhost/check.html',
        runScripts: 'outside-only'
    });

    const { window } = dom;

    window.sessionStorage.setItem(
        'coop-state',
        JSON.stringify({
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
        })
    );

    /*
     * จำลอง API ไม่ให้ Unit Test เรียก Backend จริง
     */
    window.fetch = vi.fn().mockResolvedValue({
        status,

        json: vi.fn().mockResolvedValue(
            apiResponse
        )
    });

    /*
     * ให้หน้าจอแสดงผลทันที
     * โดยไม่ต้องรอ animation 1.4 วินาที
     */
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
    window.document
        .querySelectorAll('.cf')
        .forEach(checkbox => {
            checkbox.checked = true;

            checkbox.dispatchEvent(
                new window.Event('change', {
                    bubbles: true
                })
            );
        });
}

function enter(window, selector, value) {
    const input =
        window.document.querySelector(selector);

    input.value = value;

    input.dispatchEvent(
        new window.Event('input', {
            bubbles: true
        })
    );
}

/*
 * รอให้ Promise ของ fetch() และ response.json()
 * ทำงานเสร็จก่อนตรวจผล
 */
async function finishAsyncWork() {
    await Promise.resolve();
    await Promise.resolve();

    await new Promise(resolve => {
        setImmediate(resolve);
    });
}

describe(
    'U2:T7 หน้า UI ตรวจสอบคุณสมบัติ',
    () => {

        test(
            'ปุ่มตรวจสอบถูกปิดเมื่อยังยืนยันเงื่อนไขไม่ครบ',
            () => {
                const window = openCheckPage();

                const button =
                    window.document.querySelector(
                        '#btnCheck'
                    );

                expect(button.disabled).toBe(true);

                window.close();
            }
        );

        test(
            'ปุ่มตรวจสอบเปิดเมื่อยืนยันครบทั้ง 3 ข้อ',
            () => {
                const window = openCheckPage();

                confirmAll(window);

                const button =
                    window.document.querySelector(
                        '#btnCheck'
                    );

                expect(button.disabled).toBe(false);

                window.close();
            }
        );

        test(
            'GPA 2.75 และข้อมูลอื่นผ่าน แสดงผลว่าผ่าน',
            async() => {
                const window = openCheckPage(
                    passedResponse,
                    200
                );

                enter(window, '#f_gpax', '2.75');
                confirmAll(window);

                window.document
                    .querySelector('#btnCheck')
                    .click();

                await finishAsyncWork();

                const result =
                    window.document
                    .querySelector('#rAlert')
                    .textContent;

                expect(result).toContain(
                    'ผ่านคุณสมบัติ'
                );

                expect(window.fetch)
                    .toHaveBeenCalledTimes(1);

                const [
                    requestUrl,
                    requestOptions
                ] = window.fetch.mock.calls[0];

                expect(requestUrl).toBe(
                    'http://localhost:8080/api/eligibility/check'
                );

                expect(requestOptions.method).toBe(
                    'POST'
                );

                const requestBody =
                    JSON.parse(requestOptions.body);

                expect(requestBody.gpax).toBe(
                    '2.75'
                );

                expect(
                    requestBody.acknowledgements
                ).toEqual([
                    true,
                    true,
                    true
                ]);

                window.close();
            }
        );

        test(
            'GPA 2.74 แสดงผลว่าไม่ผ่าน',
            async() => {
                const window = openCheckPage(
                    failedResponse,
                    200
                );

                enter(window, '#f_gpax', '2.74');
                confirmAll(window);

                window.document
                    .querySelector('#btnCheck')
                    .click();

                await finishAsyncWork();

                const result =
                    window.document
                    .querySelector('#rAlert')
                    .textContent;

                expect(result).toContain(
                    'ไม่ผ่านคุณสมบัติ'
                );

                const reason =
                    window.document
                    .querySelector('#rList')
                    .textContent;

                expect(reason).toContain(
                    'GPA ต้องไม่ต่ำกว่า 2.75'
                );

                expect(window.fetch)
                    .toHaveBeenCalledTimes(1);

                window.close();
            }
        );

        test(
            'กรอกข้อมูลไม่ครบ แสดงข้อความผิดพลาด',
            () => {
                const window = openCheckPage();

                enter(window, '#f_id', '');
                confirmAll(window);

                window.document
                    .querySelector('#btnCheck')
                    .click();

                const error =
                    window.document.querySelector(
                        '#formError'
                    );

                expect(error.textContent).toContain(
                    'กรอกข้อมูลให้ครบถ้วน'
                );

                expect(
                    window.document
                    .querySelector('#vForm')
                    .hidden
                ).toBe(false);

                /*
                 * ข้อมูลไม่ครบต้องไม่เรียก API
                 */
                expect(window.fetch)
                    .not.toHaveBeenCalled();

                window.close();
            }
        );

        test(
            'กรอก GPA ผิดรูปแบบ แสดงข้อความผิดพลาด',
            () => {
                const window = openCheckPage();

                enter(
                    window,
                    '#f_gpax',
                    '3.10abc'
                );

                confirmAll(window);

                window.document
                    .querySelector('#btnCheck')
                    .click();

                const error =
                    window.document.querySelector(
                        '#formError'
                    );

                expect(error.textContent).toContain(
                    'GPA ต้องเป็นตัวเลข'
                );

                expect(
                    window.document
                    .querySelector('#vForm')
                    .hidden
                ).toBe(false);

                /*
                 * ข้อมูลผิดรูปแบบต้องไม่เรียก API
                 */
                expect(window.fetch)
                    .not.toHaveBeenCalled();

                window.close();
            }
        );
    }
);