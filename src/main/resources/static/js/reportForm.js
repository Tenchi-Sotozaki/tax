/**
 * 帳票発行画面 共通JavaScript
 *
 * PDF・プレビュー・印刷の送信先は各画面のHTML（formaction）で指定する。
 * このファイルはレスポンスの受け取りだけを担当し、サーバーがエラーを返した場合は
 * 画面上部の alert-danger 領域にメッセージを表示する。
 */
(function () {
	'use strict';

	/** ステータスコード別の既定メッセージ */
	var STATUS_MESSAGES = {
		400: '入力内容に誤りがあります。入力内容をご確認のうえ、再度実行してください。',
		403: 'この操作を行う権限がありません。',
		404: '対象のデータが見つかりませんでした。',
		500: '帳票の作成中にエラーが発生しました。時間をおいて再度お試しください。'
	};

	/** 上記以外の場合に表示するメッセージ */
	var DEFAULT_MESSAGE = '帳票の作成に失敗しました。';

	/** 直前に押された送信ボタン（event.submitter未対応ブラウザ向けの控え） */
	var lastSubmitter = null;

	document.addEventListener('DOMContentLoaded', function () {
		document.addEventListener('click', function (event) {
			var target = event.target;
			if (!target || !target.closest) {
				return;
			}
			var button = target.closest('button[type="submit"], input[type="submit"]');
			if (button) {
				lastSubmitter = button;
			}
		}, true);

		Array.prototype.forEach.call(document.querySelectorAll('form'), function (form) {
			if (form.querySelector('[formaction]')) {
				form.addEventListener('submit', handleSubmit);
			}
		});
	});

	/**
	 * 帳票フォームの送信を受け取る
	 */
	function handleSubmit(event) {
		// onsubmit属性のバリデーションで中止された場合は何もしない
		if (event.defaultPrevented) {
			return;
		}

		var form = event.currentTarget;
		var submitter = event.submitter || lastSubmitter;
		var action = (submitter && submitter.getAttribute('formaction')) || form.getAttribute('action');
		if (!action) {
			return;
		}

		event.preventDefault();
		hideError();

		// 別タブで開くボタンは、ポップアップブロックを避けるため
		// ユーザー操作中である今のうちにタブを開いておく
		var newTab = submitter && submitter.getAttribute('formtarget') === '_blank'
			? window.open('', '_blank')
			: null;

		send(form, action, newTab);
	}

	/**
	 * 帳票を要求し、成功時はPDFを表示・保存、失敗時はエラーを表示する
	 */
	function send(form, action, newTab) {
		var fileName = '';

		fetch(action, { method: 'POST', body: new FormData(form) })
			.then(function (response) {
				fileName = fileNameOf(response.headers.get('Content-Disposition'));
				if (!response.ok) {
					return response.text()
						.catch(function () {
							return '';
						})
						.then(function (text) {
							var message = messageOf(response.status, text);
							var error = new Error(message);
							// 通信エラーと区別するための目印
							error.appMessage = message;
							throw error;
						});
				}
				return response.blob();
			})
			.then(function (blob) {
				var url = URL.createObjectURL(blob);
				if (newTab) {
					newTab.location.href = url;
				} else {
					saveAs(url, fileName || 'report.pdf');
				}
			})
			.catch(function (error) {
				if (newTab) {
					newTab.close();
				}
				console.error('帳票の作成に失敗しました', error);
				showError(error && error.appMessage ? error.appMessage : DEFAULT_MESSAGE);
			});
	}

	/**
	 * レスポンス本文が短いテキストならそれを、HTMLなどであれば既定文言を返す
	 */
	function messageOf(status, text) {
		var body = (text || '').trim();
		if (body && body.length <= 200 && body.charAt(0) !== '<') {
			return body;
		}
		return STATUS_MESSAGES[status] || DEFAULT_MESSAGE;
	}

	/**
	 * Content-Dispositionヘッダーからファイル名を取り出す
	 */
	function fileNameOf(contentDisposition) {
		if (!contentDisposition) {
			return '';
		}
		var utf8 = contentDisposition.match(/filename\*=UTF-8''([^;]+)/i);
		if (utf8) {
			try {
				return decodeURIComponent(utf8[1]);
			} catch (e) {
				// デコードできない場合はファイル名なしとして扱う
			}
		}
		var plain = contentDisposition.match(/filename="?([^";]+)"?/i);
		return plain ? plain[1] : '';
	}

	/**
	 * PDFをファイルとして保存する
	 */
	function saveAs(url, fileName) {
		var link = document.createElement('a');
		link.href = url;
		link.download = fileName;
		document.body.appendChild(link);
		link.click();
		link.remove();
		// 保存が始まる前に解放されないよう、少し待ってから破棄する
		setTimeout(function () {
			URL.revokeObjectURL(url);
		}, 60000);
	}

	/**
	 * エラーメッセージを画面上部のalert-danger領域に表示する
	 */
	function showError(message) {
		var alertArea = document.getElementById('reportError');
		var textArea = document.getElementById('reportErrorText');
		if (!alertArea || !textArea) {
			alert(message);
			return;
		}
		textArea.textContent = message;
		alertArea.classList.remove('d-none');
		alertArea.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
	}

	/**
	 * エラーメッセージを消す
	 */
	function hideError() {
		var alertArea = document.getElementById('reportError');
		if (alertArea) {
			alertArea.classList.add('d-none');
		}
	}

	// 納入書のように独自に送信している画面からも同じ領域を使えるように公開する
	window.ReportError = { show: showError, hide: hideError };
})();
