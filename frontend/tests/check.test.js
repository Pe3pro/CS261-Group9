// @vitest-environment node

import {
    describe,
    test,
    expect,
    vi,
    afterEach
} from 'vitest';

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

const openedWindows = [];

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

const invalidResponse = {
    valid: false,
    eligible: null,
    message: 'ข้อมูลไม่ถูกต้อง',
    errors: [
        'ไม่พบผลการเรียนวิชา คพ.101'
    ],
    reasons: [],
    warnings: []
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
    openedWindows.push(window);

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

    // จำลอง API โดยไม่เรียก Backend จริง
    window.fetch = vi.fn().mockResolvedValue({
        status,
        ok: status >= 200 && status < 300,
        json: vi.fn().mockResolvedValue(apiResponse)
    });

    /*
     * ข้ามเวลารอ animation แต่ไม่เรียก timeout
     * ของ AbortController ทันที เพราะจะทำให้ Request
     * ถูกยกเลิกก่อนเริ่มทดสอบ
     */
    const originalSetTimeout = window.setTimeout.bind(window);

    window.setTimeout = (callback, delay = 0, ...args) => {
        if (delay >= 15000) {
            return originalSetTimeout(callback, delay, ...args);
        }

        return originalSetTimeout(callback, 0, ...args);
    };

    window.eval(script);

    window.document.dispatchEvent(
        new window.Event('DOMContentLoaded')
    );

    return window;
}

afterEach(() => {
    for (const window of openedWindows) {
        window.close();
    }

    openedWindows.length = 0;
});

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
    const input = window.document.querySelector(selector);

    expect(input).not.toBeNull();

    input.value = value;

    input.dispatchEvent(
        new window.Event('input', {
            bubbles: true
        })
    );
}

function clickCheck(window) {
    const button = window.document.querySelector('#btnCheck');

    expect(button).not.toBeNull();
    expect(button.disabled).toBe(false);

    button.click();
}

function textOf(window, selector) {
    const element = window.document.querySelector(selector);

    expect(element).not.toBeNull();

    return element.textContent;
}

function assertPostRequest(window, expectedGpax) {
    expect(window.fetch).toHaveBeenCalledTimes(1);

    const [requestUrl, requestOptions] =
    window.fetch.mock.calls[0];

    expect(requestUrl).toBe(
        'http://localhost:8080/api/eligibility/check'
    );

    expect(requestOptions.method).toBe('POST');

    expect(requestOptions.headers['Content-Type']).toBe(
        'application/json'
    );

    const body = JSON.parse(requestOptions.body);

    expect(body.studentId).toBe('6410000001');
    expect(body.curriculum).toBe('61');
    expect(body.gpax).toBe(expectedGpax);

    expect(body.acknowledgements).toEqual([
        true,
        true,
        true
    ]);
}

describe('U2 หน้า UI ตรวจสอบคุณสมบัติ — Mock API', () => {
    test(
        'ปุ่มตรวจสอบถูกปิดเมื่อยังยืนยันเงื่อนไขไม่ครบ',
        () => {
            const window = openCheckPage();

            const button =
                window.document.querySelector('#btnCheck');

            expect(button.disabled).toBe(true);
            expect(window.fetch).not.toHaveBeenCalled();
        }
    );

    test(
        'ปุ่มตรวจสอบเปิดเมื่อยืนยันครบทั้ง 3 ข้อ',
        () => {
            const window = openCheckPage();

            confirmAll(window);

            const button =
                window.document.querySelector('#btnCheck');

            expect(button.disabled).toBe(false);
            expect(window.fetch).not.toHaveBeenCalled();
        }
    );

    test(
        'UI-API-01: GPA 3.10 ส่ง Request และแสดงผลผ่าน',
        async() => {
            const window = openCheckPage(passedResponse, 200);

            enter(window, '#f_gpax', '3.10');
            confirmAll(window);
            clickCheck(window);

            await vi.waitFor(() => {
                expect(textOf(window, '#rAlert').trim()).toBe(
                    'ผ่านคุณสมบัติ'
                );
            });

            assertPostRequest(window, '3.10');

            expect(textOf(window, '#rList')).toContain(
                'ต้องสอบได้วิชา คพ.302 ก่อนออกปฏิบัติงาน'
            );
        }
    );

    test(
        'GPA 2.75 ส่ง Request และแสดงผลผ่านจาก API',
        async() => {
            const window = openCheckPage(passedResponse, 200);

            enter(window, '#f_gpax', '2.75');
            confirmAll(window);
            clickCheck(window);

            await vi.waitFor(() => {
                expect(textOf(window, '#rAlert').trim()).toBe(
                    'ผ่านคุณสมบัติ'
                );
            });

            assertPostRequest(window, '2.75');
        }
    );

    test(
        'UI-API-02: GPA 2.74 แสดงผลไม่ผ่านพร้อมเหตุผล',
        async() => {
            const window = openCheckPage(failedResponse, 200);

            enter(window, '#f_gpax', '2.74');
            confirmAll(window);
            clickCheck(window);

            await vi.waitFor(() => {
                expect(textOf(window, '#rAlert').trim()).toBe(
                    'ไม่ผ่านคุณสมบัติ'
                );

                expect(textOf(window, '#rList')).toContain(
                    'GPA ต้องไม่ต่ำกว่า 2.75'
                );
            });

            assertPostRequest(window, '2.74');
        }
    );

    test(
        'UI-API-03: กรอกข้อมูลไม่ครบ แสดงข้อผิดพลาดและไม่เรียก API',
        () => {
            const window = openCheckPage();

            enter(window, '#f_id', '');
            confirmAll(window);
            clickCheck(window);

            expect(textOf(window, '#formError')).toContain(
                'กรอกข้อมูลให้ครบถ้วน'
            );

            expect(
                window.document.querySelector('#vForm').hidden
            ).toBe(false);

            expect(window.fetch).not.toHaveBeenCalled();
        }
    );

    test(
        'UI-API-04: GPA ผิดรูปแบบ แสดงข้อผิดพลาดและไม่เรียก API',
        () => {
            const window = openCheckPage();

            enter(window, '#f_gpax', '3.10abc');
            confirmAll(window);
            clickCheck(window);

            expect(textOf(window, '#formError')).toContain(
                'GPA ต้องเป็นตัวเลข'
            );

            expect(
                window.document.querySelector('#vForm').hidden
            ).toBe(false);

            expect(window.fetch).not.toHaveBeenCalled();
        }
    );

    test(
        'UI-API-05: API ตอบ 400 แสดง errors และไม่แสดงผลผ่าน',
        async() => {
            const window = openCheckPage(invalidResponse, 400);

            confirmAll(window);
            clickCheck(window);

            await vi.waitFor(() => {
                expect(textOf(window, '#rAlert').trim()).toBe(
                    'ข้อมูลไม่ถูกต้อง'
                );

                expect(textOf(window, '#rList')).toContain(
                    'ไม่พบผลการเรียนวิชา คพ.101'
                );
            });

            assertPostRequest(window, '3.10');

            expect(textOf(window, '#rAlert').trim()).not.toBe(
                'ผ่านคุณสมบัติ'
            );
        }
    );

    test(
        'UI-API-06: Network Error แสดงข้อความเชื่อมต่อไม่ได้',
        async() => {
            const window = openCheckPage();

            // ใช้ TypeError ของ window เดียวกับ script.js
            window.fetch.mockRejectedValue(
                new window.TypeError('Failed to fetch')
            );

            confirmAll(window);
            clickCheck(window);

            await vi.waitFor(() => {
                expect(textOf(window, '#rAlert').trim()).toBe(
                    'ไม่สามารถตรวจสอบคุณสมบัติได้'
                );

                expect(textOf(window, '#rList')).toContain(
                    'ไม่สามารถเชื่อมต่อ API ได้'
                );
            });

            expect(window.fetch).toHaveBeenCalledTimes(1);

            expect(textOf(window, '#rAlert').trim()).not.toBe(
                'ผ่านคุณสมบัติ'
            );
        }
    );
});