// -----------------------------------------------------------------------
// 中身のあるアコーディオンを開いた状態にする
// ・登録済みのデータがある場合 … たたまれていると気づけないため
// ・エラーがある場合           … 項目直下のメッセージが見えないため
// 日付欄は common.js が当日を自動セットするため、値ではなく
// data-initial-value（サーバから渡ってきた値）で判定する
// -----------------------------------------------------------------------
function openAccordionsWithContent() {
    document.querySelectorAll('.accordion-collapse').forEach(el => {
        const hasError = el.querySelector('.is-invalid') !== null;
        const hasValue = Array.from(el.querySelectorAll('input, textarea, select')).some(hasInputValue);
        if (!hasError && !hasValue) return;
        bootstrap.Collapse.getOrCreateInstance(el, { toggle: false }).show();
    });
}

/** 入力欄に値が入っているか。ボタン類と hidden は対象外 */
function hasInputValue(el) {
    if (el.type === 'checkbox' || el.type === 'radio') return el.checked;
    if (el.type === 'hidden' || el.type === 'button' || el.type === 'submit') return false;
    // 日付欄は common.js が当日を自動セットするため value では判定できない。
    // サーバから渡ってきた値（data-initial-value）だけを見る
    if (el.type === 'date') return (el.dataset.initialValue ?? '').trim() !== '';
    return (el.value ?? '').trim() !== '';
}

document.addEventListener('DOMContentLoaded', function() {
    openAccordionsWithContent();

    const fukaKbnEl = document.getElementById('fukaKbnHidden');
    const fukaKbn = fukaKbnEl ? fukaKbnEl.value : '';

    const triggerFlag = document.getElementById('modalTriggerFlag');
    if (triggerFlag && triggerFlag.value === 'true') {
        const modalElement = document.getElementById('taxWarningModal');
        if (typeof bootstrap !== 'undefined' && modalElement) {
            new bootstrap.Modal(modalElement).show();
        } else {
            alert("金額のズレを検知しました。「そのまま保存する」を押すと登録を続行します。");
        }
    }

    const btnForceSave = document.getElementById('btnForceSave');
    const taxCheckBypassed = document.getElementById('taxCheckBypassed');
    if (btnForceSave && taxCheckBypassed) {
        btnForceSave.addEventListener('click', function(e) {
            e.preventDefault();
            taxCheckBypassed.value = 'true';
            const form = this.closest('form') || document.getElementById('fukaDeclarationForm');
            if (form) form.submit();
        });
    }

    const monthlyTallyModal = document.getElementById('monthlyTallyModal');
    if (monthlyTallyModal) {
        monthlyTallyModal.addEventListener('keydown', function(e) {
            if (e.key === 'Enter') {
                e.preventDefault();
                const btnApplyTally = document.getElementById('btnApplyTally');
                if (btnApplyTally) btnApplyTally.click();
            }
        });
    }

    const parseIntSafe = (val) => {
        const num = parseInt(val.replace(/,/g, ''), 10);
        return isNaN(num) ? 0 : num;
    };

    // 【定額制】の計算
    const tableTeigaku = document.getElementById('monthlyTallyTableTeigaku');
    const calculateTeigaku = () => {
        if (!tableTeigaku) return;
        const rows = tableTeigaku.querySelectorAll('tbody tr');
        let columnTotals = [];
        let totalExempt = 0;
        let totalZeigaku = 0;

        rows.forEach(row => {
            const seqInputs = row.querySelectorAll('input[class*="seq"]');
            seqInputs.forEach((input, index) => {
                const val = parseIntSafe(input.value);
                if (columnTotals[index] === undefined) columnTotals[index] = 0;
                columnTotals[index] += val;
            });
            const exemptInput = row.querySelector('.exempt');
            if (exemptInput) totalExempt += parseIntSafe(exemptInput.value);
            const rowZeigakuInput = row.querySelector('.teigaku-zeigaku');
            if (rowZeigakuInput) totalZeigaku += parseIntSafe(rowZeigakuInput.value);
        });

        columnTotals.forEach((total, index) => {
            const totalCountInput = document.getElementById(`modal-total-count-${index}`);
            if (totalCountInput) totalCountInput.value = total.toLocaleString();

            const rateInput = document.getElementById(`modal-tax-rate-${index}`);
            const taxAmountInput = document.getElementById(`modal-tax-amount-${index}`);
            if (rateInput && taxAmountInput) {
                const rate = parseIntSafe(rateInput.value);
                const taxAmount = total * rate;
                taxAmountInput.value = taxAmount.toLocaleString();
            }
        });

        const modalTotalExempt = document.getElementById('modal-total-exempt');
        if (modalTotalExempt) modalTotalExempt.value = totalExempt.toLocaleString();

        const modalTotalZeigaku = document.getElementById('modal-total-zeigaku');
        if (modalTotalZeigaku) modalTotalZeigaku.value = totalZeigaku.toLocaleString();
    };

    // 【定率制】の計算（複数区分・動的ループ対応）
    const tableTeiritsu = document.getElementById('monthlyTallyTableTeiritsu');
    const calculateTeiritsu = () => {
        if (!tableTeiritsu) return;
        const rows = tableTeiritsu.querySelectorAll('tbody tr');

        // 隠し税率項目の数から、何区分（何列）あるかを動的に取得
        const rateInputs = tableTeiritsu.querySelectorAll('input[id^="modal-teiritsu-tax-rate-"]');
        const tierCount = rateInputs.length;

        let columnTotals = {
            sogaku: Array(tierCount).fill(0),
            hakusu: Array(tierCount).fill(0),
            ryokin: Array(tierCount).fill(0)
        };
        let totalMenjoHakusu = 0;
        let totalMenjoRyokin = 0;
        let totalZeigaku = 0;

        rows.forEach(row => {
            for (let i = 0;i < tierCount;i++) {
                const sogakuInput = row.querySelector(`.teiritsu-kazei-sogaku-${i}`);
                if (sogakuInput) columnTotals.sogaku[i] += parseIntSafe(sogakuInput.value);

                const hakusuInput = row.querySelector(`.teiritsu-kazei-hakusu-${i}`);
                if (hakusuInput) columnTotals.hakusu[i] += parseIntSafe(hakusuInput.value);

                const ryokinInput = row.querySelector(`.teiritsu-kazei-ryokin-${i}`);
                if (ryokinInput) columnTotals.ryokin[i] += parseIntSafe(ryokinInput.value);
            }
            const menjoHakusuInput = row.querySelector('.teiritsu-menjo-hakusu');
            if (menjoHakusuInput) totalMenjoHakusu += parseIntSafe(menjoHakusuInput.value);

            const menjoRyokinInput = row.querySelector('.teiritsu-menjo-ryokin');
            if (menjoRyokinInput) totalMenjoRyokin += parseIntSafe(menjoRyokinInput.value);

            const zeigakuInput = row.querySelector('.teiritsu-zeigaku');
            if (zeigakuInput) totalZeigaku += parseIntSafe(zeigakuInput.value);
        });

        // 合計と税額の計算反映
        for (let i = 0;i < tierCount;i++) {
            const totalSogakuInput = document.getElementById(`modal-teiritsu-total-kazei-sogaku-${i}`);
            if (totalSogakuInput) totalSogakuInput.value = columnTotals.sogaku[i].toLocaleString();

            const totalHakusuInput = document.getElementById(`modal-teiritsu-total-kazei-hakusu-${i}`);
            if (totalHakusuInput) totalHakusuInput.value = columnTotals.hakusu[i].toLocaleString();

            const totalRyokinInput = document.getElementById(`modal-teiritsu-total-kazei-ryokin-${i}`);
            if (totalRyokinInput) totalRyokinInput.value = columnTotals.ryokin[i].toLocaleString();

            const rateInput = document.getElementById(`modal-teiritsu-tax-rate-${i}`);
            const taxAmountInput = document.getElementById(`modal-teiritsu-total-tax-${i}`);

            if (rateInput && taxAmountInput) {
                const ratePercent = parseFloat(rateInput.value) || 0;
                // 定率計算：料金合計 × (税率/100) の切り捨て
                taxAmountInput.value = Math.floor(columnTotals.ryokin[i] * (ratePercent / 100)).toLocaleString();
            }
        }

        const totalMenjoHakusuInput = document.getElementById('modal-teiritsu-total-menjo-hakusu');
        if (totalMenjoHakusuInput) totalMenjoHakusuInput.value = totalMenjoHakusu.toLocaleString();

        const totalMenjoRyokinInput = document.getElementById('modal-teiritsu-total-menjo-ryokin');
        if (totalMenjoRyokinInput) totalMenjoRyokinInput.value = totalMenjoRyokin.toLocaleString();

        const totalZeigakuInput = document.getElementById('modal-teiritsu-total-zeigaku');
        if (totalZeigakuInput) totalZeigakuInput.value = totalZeigaku.toLocaleString();
    };

    // イベントバインド
    if (tableTeigaku) {
        tableTeigaku.querySelector('tbody').addEventListener('input', function(e) {
            if (e.target && e.target.tagName === 'INPUT') calculateTeigaku();
        });
    }
    if (tableTeiritsu) {
        tableTeiritsu.querySelector('tbody').addEventListener('input', function(e) {
            if (e.target && e.target.tagName === 'INPUT') calculateTeiritsu();
        });
    }

    if (monthlyTallyModal) {
        // 照会時は「入力内容を決定」ボタンが存在しない
        const isViewMode = !document.getElementById('btnApplyTally');
        // モーダルを開いた時点の入力値。変更有無の判定と破棄時の復元に使う
        let openedSnapshot = null;

        // モーダル内の入力欄（日付などの hidden は対象外）
        const getTallyInputs = () =>
            monthlyTallyModal.querySelectorAll('input:not([type="hidden"])');

        // 現在の入力値を記録する
        const takeSnapshot = () => {
            const snapshot = new Map();
            getTallyInputs().forEach(input => snapshot.set(input, input.value));
            return snapshot;
        };

        // 開いた時点から入力値が変わっているか
        const isTallyChanged = () => {
            if (!openedSnapshot) return false;
            for (const [input, value] of openedSnapshot) {
                if (input.value !== value) return true;
            }
            return false;
        };

        // 開いた時点の入力値に戻す
        const restoreSnapshot = () => {
            if (!openedSnapshot) return;
            openedSnapshot.forEach((value, input) => {
                input.value = value;
            });
            if (fukaKbn === '1') calculateTeigaku();
            else if (fukaKbn === '2') calculateTeiritsu();

            // 変更マーク（黄色の枠）も開いた時点の状態に戻す
            // 自動計算欄は利用者が編集した箇所ではないため対象外とする
            openedSnapshot.forEach((value, input) => {
                if (input.readOnly || input.disabled) return;
                checkValue(input);
            });
        };

        monthlyTallyModal.addEventListener('shown.bs.modal', function() {
            if (fukaKbn === '1') calculateTeigaku();
            else if (fukaKbn === '2') calculateTeiritsu();

            // 自動計算後の状態を「開いた時点の入力内容」として記録する
            openedSnapshot = takeSnapshot();

            // 先頭の入力欄にフォーカスを当てる
            const table = fukaKbn === '1' ? tableTeigaku : tableTeiritsu;
            const firstInput = table?.querySelector(
                'tbody input[type="text"]:not([readonly]):not([disabled])');
            firstInput?.focus();
        });

        // ===== 月計表の桁数チェック =====
        // 保存時のサーバ側チェックと同じ桁数を見る。ここで弾けば、モーダルを閉じて保存してから
        // やり直す流れにならずに済む
        const TALLY_MAX_DIGITS = {
            hakusu: 8, menjoHakusu: 8,
            sogaku: 13, ryokin: 13, menjoRyokin: 13, zeigaku: 13
        };

        const TALLY_ITEM_NAMES = {
            hakusu: '宿泊数',
            menjoHakusu: '課税対象外の宿泊数',
            sogaku: '宿泊料金総額',
            ryokin: '宿泊料金',
            menjoRyokin: '課税対象外の宿泊料金',
            zeigaku: '税額'
        };

        // 区分名は monthlyDetail.taxDetails[N].label に hidden で入っている
        const tallyKbnName = (index) => {
            const el = document.getElementsByName(
                'monthlyDetail.taxDetails[' + index + '].label')[0];
            return el && el.value ? el.value + 'の区分' : '区分' + (Number(index) + 1);
        };

        // 桁数を超えている欄を赤くして、メッセージの配列を返す
        const collectTallyDigitErrors = () => {
            const messages = [];
            monthlyTallyModal.querySelectorAll('input[name^="monthlyTally.dailyItems"]')
                .forEach(input => {
                    const m = input.name.match(
                        /^monthlyTally\.dailyItems\[(\d+)\]\.([A-Za-z]+)(?:\[(\d+)\])?$/);
                    if (!m) return;
                    const maxDigits = TALLY_MAX_DIGITS[m[2]];
                    if (!maxDigits) return;

                    // カンマなどを除いた桁数で判定する。先頭の0は桁数に数えない
                    const digits = input.value.replace(/[^0-9]/g, '').replace(/^0+(?=\d)/, '').length;
                    if (digits <= maxDigits) {
                        setInputError(input, 'js-digit-error', false);
                        return;
                    }
                    setInputError(input, 'js-digit-error', true);
                    const kbn = m[3] !== undefined ? '（' + tallyKbnName(m[3]) + '）' : '';
                    messages.push('月計表 ' + (Number(m[1]) + 1) + '日 '
                        + TALLY_ITEM_NAMES[m[2]] + kbn + 'は' + maxDigits + '桁以内で入力してください');
                });
            return messages;
        };

        const showTallyDigitErrors = (messages) => {
            const alertEl = document.getElementById('tallyDigitAlert');
            const listEl = document.getElementById('tallyDigitAlertList');
            if (!alertEl || !listEl) return;

            listEl.innerHTML = '';
            messages.forEach(message => {
                const li = document.createElement('li');
                li.className = 'small';
                li.textContent = message;
                listEl.appendChild(li);
            });
            alertEl.classList.toggle('d-none', messages.length === 0);

            // アラートはモーダルの先頭にあるので、見えるところまで戻す
            if (messages.length > 0) {
                const body = monthlyTallyModal.querySelector('.modal-body');
                if (body) body.scrollTop = 0;
            }
        };

        // 「入力内容を決定」を押した時点で入力内容を確定させてから閉じる
        // 確定後は「変更なし」となるため、閉じる際に確認ダイアログは出ない
        // 閉じる順序を明確にするため data-bs-dismiss は使わずJSから閉じる
        document.getElementById('btnApplyTally')?.addEventListener('click', function() {
            const digitErrors = collectTallyDigitErrors();
            showTallyDigitErrors(digitErrors);
            // 桁数エラーがある間はモーダルを閉じない
            if (digitErrors.length > 0) return;

            openedSnapshot = takeSnapshot();
            bootstrap.Modal.getOrCreateInstance(monthlyTallyModal).hide();
        });

        // 閉じる操作（×ボタン・閉じるボタン）は、入力内容に変更がある場合のみ確認する
        monthlyTallyModal.addEventListener('hide.bs.modal', function(e) {
            if (isViewMode) return;
            if (!isTallyChanged()) return;

            if (confirm('入力内容は破棄されます。よろしいですか？')) {
                restoreSnapshot();
            } else {
                e.preventDefault();
            }
        });
    }

    // ===== 内訳試算（市区町村税額・都道府県税額） =====
    const ESTIMATE_BREAKDOWN_API = '/accommodation-tax/declaration/estimate-breakdown';

    function getFieldValue(name) {
        const els = document.getElementsByName(name);
        return els.length ? els[0].value : '';
    }

    function setFieldValue(name, value) {
        const num = (value === null || value === undefined) ? 0 : value;
        document.getElementsByName(name).forEach(el => {
            el.value = Number(num).toLocaleString();
        });
    }

    function toNumberOrNull(value) {
        if (value === null || value === undefined) return null;
        const cleaned = String(value).replace(/,/g, '').trim();
        if (cleaned === '') return null;
        const num = Number(cleaned);
        return isNaN(num) ? null : num;
    }

    async function callEstimateBreakdown(fukaKbnValue, monthlyDetailPayload) {
        const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;
        const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
        const headers = { 'Content-Type': 'application/json' };
        if (csrfHeader && csrfToken) headers[csrfHeader] = csrfToken;

        const res = await fetch(`${ESTIMATE_BREAKDOWN_API}?fukaKbn=${encodeURIComponent(fukaKbnValue)}`, {
            method: 'POST',
            headers: headers,
            body: JSON.stringify(monthlyDetailPayload)
        });
        if (!res.ok) {
            // サーバー側のエラーメッセージがあれば取得して表示する（デバッグしやすくするため）
            let serverMessage = '';
            try {
                const errorBody = await res.json();
                serverMessage = errorBody && errorBody.message ? errorBody.message : '';
            } catch (parseErr) {
                // レスポンスがJSONでない場合（HTMLのエラーページ等）は無視する
            }
            throw new Error(serverMessage || `内訳試算に失敗しました（HTTP ${res.status}）`);
        }
        return res.json();
    }

    // 【定額制】内訳試算ボタン：区分ごとの宿泊数・税額から市区町村/都道府県税額を試算
    const btnEstimateTeigaku = document.getElementById('btnEstimateBreakdownTeigaku');
    if (btnEstimateTeigaku) {
        btnEstimateTeigaku.addEventListener('click', async () => {
            const rows = document.querySelectorAll('.tax-detail-row');
            const taxDetails = [];
            rows.forEach((row, i) => {
                taxDetails.push({
                    zeiritsuSeq: toNumberOrNull(getFieldValue(`monthlyDetail.taxDetails[${i}].zeiritsuSeq`)),
                    taxRate: toNumberOrNull(getFieldValue(`monthlyDetail.taxDetails[${i}].taxRate`)),
                    taxKenRate: toNumberOrNull(getFieldValue(`monthlyDetail.taxDetails[${i}].taxKenRate`)),
                    hakusu: toNumberOrNull(getFieldValue(`monthlyDetail.taxDetails[${i}].hakusu`)),
                    zeigaku: toNumberOrNull(getFieldValue(`monthlyDetail.taxDetails[${i}].zeigaku`))
                });
            });

            btnEstimateTeigaku.disabled = true;
            try {
                const result = await callEstimateBreakdown('1', { taxDetails: taxDetails });
                (result.taxDetails || []).forEach((detail, i) => {
                    setFieldValue(`monthlyDetail.taxDetails[${i}].cityZeigaku`, detail.cityZeigaku);
                    setFieldValue(`monthlyDetail.taxDetails[${i}].kenZeigaku`, detail.kenZeigaku);
                });
                setFieldValue('monthlyDetail.totalCityZeigaku', result.totalCityZeigaku);
                setFieldValue('monthlyDetail.totalKenZeigaku', result.totalKenZeigaku);
            } catch (err) {
                console.error(err);
                alert(err.message || '内訳試算に失敗しました。入力内容をご確認ください。');
            } finally {
                btnEstimateTeigaku.disabled = false;
            }
        });
    }

    // 【定率制】内訳試算ボタン：合計の課税対象料金からレート表を参照して市区町村/都道府県税額を試算
    const btnEstimateTeiritsu = document.getElementById('btnEstimateBreakdownTeiritsu');
    if (btnEstimateTeiritsu) {
        btnEstimateTeiritsu.addEventListener('click', async () => {
            const payload = {
                paymentYearMonth: getFieldValue('monthlyDetail.paymentYearMonth'),
                totalStayCount: toNumberOrNull(getFieldValue('monthlyDetail.totalStayCount')),
                exemptStayCount: toNumberOrNull(getFieldValue('monthlyDetail.exemptStayCount')),
                kazeiRyokin: toNumberOrNull(getFieldValue('monthlyDetail.kazeiRyokin')),
                totalPaymentAmount: toNumberOrNull(getFieldValue('monthlyDetail.totalPaymentAmount'))
            };

            btnEstimateTeiritsu.disabled = true;
            try {
                const result = await callEstimateBreakdown('2', payload);
                setFieldValue('monthlyDetail.totalCityZeigaku', result.totalCityZeigaku);
                setFieldValue('monthlyDetail.totalKenZeigaku', result.totalKenZeigaku);
            } catch (err) {
                console.error(err);
                alert(err.message || '内訳試算に失敗しました。入力内容をご確認ください。');
            } finally {
                btnEstimateTeiritsu.disabled = false;
            }
        });
    }

    // 編集モードでなければ処理を行わない
    const contentContainer = document.querySelector('[data-is-edit]');
    const isEdit = contentContainer ? contentContainer.getAttribute('data-is-edit') === 'true' : false;
    if (!isEdit) return;

    function checkValue(input) {

        // チェックボックスとラジオボタンは対象外
        if (input.type === 'checkbox' || input.type === 'radio') return;

        const initialValue = input.getAttribute('data-initial-value') || '';

        // nullという文字列になってしまうのを防ぐ
        let initialStr = (initialValue === null || initialValue === 'null') ? '' : String(initialValue).trim();

        // 比較のためにカンマを除去
        initialStr = initialStr.replace(/,/g, '');

        let currentStr = String(input.value).trim();

        // 比較のために入力値からもカンマを除去
        currentStr = currentStr.replace(/,/g, '');

        // 要素がモーダル内にあるか判定
        if (input.closest('#monthlyTallyModal')) {
			
            // 初期値が空なら '0' に統一
            if (initialStr === '') {
                initialStr = '0';
            }
			
            // 現在の入力値が空なら '0' に統一
            if (currentStr === '') {
                currentStr = '0';
            }
        }

        // 変更があったか判定
        const isChanged = (currentStr !== initialStr);

        if (isChanged) {
			input.classList.add('form-control-edited');
        } else {
			input.classList.remove('form-control-edited');
            input.style.border = '';
        }
    }

    // 画面表示時に最初から値が変わっているものを検知、および各イベントへの登録
    const inputs = document.querySelectorAll('.form-control, .form-select');
    inputs.forEach(input => {

        // 画面を開いた瞬間にズレがあるかチェック
        checkValue(input);

        // イベント登録
        input.addEventListener('input', () => checkValue(input));
        input.addEventListener('change', () => checkValue(input));
        input.addEventListener('blur', () => checkValue(input));
    });
});

// 入力欄の赤枠は「半角数字エラー」と「桁数エラー」の2種類が付きうる。
// どちらか一方が解消しただけで赤枠が消えないよう、種類ごとのマーカーで管理する
const setInputError = (input, marker, hasError) => {
    input.classList.toggle(marker, hasError);
    input.classList.toggle('is-invalid',
        input.classList.contains('js-hankaku-error')
        || input.classList.contains('js-digit-error'));
};

const showHankakuErrorModal = () => {
    const el = document.getElementById('hankakuErrorModal');
    if (el) bootstrap.Modal.getOrCreateInstance(el).show();
};

document.querySelectorAll('.js-comma-format').forEach(input => {
    // ページ読み込み時に初期値があればカンマ変換
    if (input.value) {
        input.value = Number(input.value.replace(/,/g, '')).toLocaleString();
    }

    // 数値バリデーション関数
    const validateNumericInput = (value) => {
        // カンマ区切りの数値パターン（空文字も許可）
        return /^[0-9,]*$/.test(value);
    };

    // 入力時の数値チェック
    input.addEventListener('input', (e) => {
        if (!validateNumericInput(e.target.value)) {
            // 全角数字・記号などが入った場合。取り除いたうえでモーダルを出す
            e.target.value = e.target.value.replace(/[^0-9,]/g, '');
            setInputError(e.target, 'js-hankaku-error', false);
            showHankakuErrorModal();
            return;
        }
        setInputError(e.target, 'js-hankaku-error', false);
    });

    // フォーカスが当たったらカンマを消す（数値だけにして入力しやすくする）
    input.addEventListener('focus', (e) => {
        const numValue = e.target.value.replace(/,/g, '');
        e.target.value = numValue;
    });

    // フォーカスが外れたらカンマを入れる
    input.addEventListener('blur', (e) => {
        const cleanValue = e.target.value.replace(/,/g, '');
        // 半角数字のみの場合だけカンマ変換を実行
        if (/^\d+$/.test(cleanValue)) {
            e.target.value = Number(cleanValue).toLocaleString();
        }
    });
});
