$base = "src\main\resources\templates"

# nozeiShukiConfig.html
$f = "$base\admin\nozeiShukiConfig.html"
$c = [System.IO.File]::ReadAllText($f, [System.Text.Encoding]::UTF8)
$old = "<a th:href=`"@{/admin/nozei-shuki/list}`"`r`n`r`n`t`t`t`t`t`tclass=`"btn btn-outline-primary me-2`"`r`n`t`t`t`t`t`tonclick=`"return confirm('入力内容は破棄されます。よろしいですか？')`"> <i`r`n`t`t`t`t`t`tclass=`"bi bi-arrow-left me-1`"></i>一覧に戻る`r`n`t`t`t`t`t</a>"
$new = "<a href=`"#`" class=`"btn btn-outline-primary me-2`" data-bs-toggle=`"modal`" data-bs-target=`"#backConfirmModal`"> <i class=`"bi bi-arrow-left me-1`"></i>一覧に戻る`r`n`t`t`t`t`t</a>"
$c2 = $c.Replace($old, $new)
if ($c2 -eq $c) { Write-Host "nozeiShukiConfig: NO MATCH" } else { [System.IO.File]::WriteAllText($f, $c2, [System.Text.Encoding]::UTF8); Write-Host "nozeiShukiConfig: OK" }

# linkConfirmModal追加
$f = "$base\admin\nozeiShukiConfig.html"
$c = [System.IO.File]::ReadAllText($f, [System.Text.Encoding]::UTF8)
$old2 = "<div`r`n`t`t`tth:replace=`"~{layout/components :: confirmModal('deleteModal', '削除確認', 'この納税周期を削除します。削除した納税周期は復元できません。よろしいですか？', 'deleteNozeiShukiForm', 'btn-danger', '削除する')}`">`r`n`t`t</div>"
$new2 = "<div`r`n`t`t`tth:replace=`"~{layout/components :: confirmModal('deleteModal', '削除確認', 'この納税周期を削除します。削除した納税周期は復元できません。よろしいですか？', 'deleteNozeiShukiForm', 'btn-danger', '削除する')}`">`r`n`t`t</div>`r`n`t`t<div th:replace=`"~{layout/components :: linkConfirmModal('backConfirmModal', '確認', '入力内容が破棄されます。よろしいですか？', @{/admin/nozei-shuki/list}, '一覧に戻る')}`"></div>"
$c2 = $c.Replace($old2, $new2)
if ($c2 -eq $c) { Write-Host "nozeiShukiConfig modal: NO MATCH" } else { [System.IO.File]::WriteAllText($f, $c2, [System.Text.Encoding]::UTF8); Write-Host "nozeiShukiConfig modal: OK" }

# tTaxManagerConfig.html
$f = "$base\tokugimu\tTaxManagerConfig.html"
$c = [System.IO.File]::ReadAllText($f, [System.Text.Encoding]::UTF8)
$old = "onclick=`"return confirm('入力内容は破棄されます。よろしいですか？')`"> <i`r`n`t`t`t`t`tclass=`"bi bi-arrow-left me-2`"></i>一覧に戻る`r`n`t`t`t`t</a>"
$new = "data-bs-toggle=`"modal`" data-bs-target=`"#backConfirmModal`"> <i`r`n`t`t`t`t`tclass=`"bi bi-arrow-left me-2`"></i>一覧に戻る`r`n`t`t`t`t</a>"
$c2 = $c.Replace($old, $new)
if ($c2 -eq $c) { Write-Host "tTaxManagerConfig btn: NO MATCH" } else { [System.IO.File]::WriteAllText($f, $c2, [System.Text.Encoding]::UTF8); Write-Host "tTaxManagerConfig btn: OK" }

$f = "$base\tokugimu\tTaxManagerConfig.html"
$c = [System.IO.File]::ReadAllText($f, [System.Text.Encoding]::UTF8)
$old2 = "<div th:replace=`"~{layout/components :: addressSearchModal}`"></div>`r`n`t`t<!-- 特別徴収義務者指定モーダル -->`r`n`t`t<div th:replace=`"~{layout/components :: shiteiGassanSearchModal}`"></div>`r`n`t</div>`r`n`r`n`t<th:block layout:fragment=`"scripts`">`r`n`t`t<script th:src=`"@{/js/tTaxManagerConfig.js}`">"
$new2 = "<div th:replace=`"~{layout/components :: addressSearchModal}`"></div>`r`n`t`t<!-- 特別徴収義務者指定モーダル -->`r`n`t`t<div th:replace=`"~{layout/components :: shiteiGassanSearchModal}`"></div>`r`n`t`t<div th:unless=`"`${isView ?: false}`" th:replace=`"~{layout/components :: linkConfirmModal('backConfirmModal', '確認', '入力内容が破棄されます。よろしいですか？', @{/tokugimu/list}, '一覧に戻る')}`"></div>`r`n`t</div>`r`n`r`n`t<th:block layout:fragment=`"scripts`">`r`n`t`t<script th:src=`"@{/js/tTaxManagerConfig.js}`">"
$c2 = $c.Replace($old2, $new2)
if ($c2 -eq $c) { Write-Host "tTaxManagerConfig modal: NO MATCH" } else { [System.IO.File]::WriteAllText($f, $c2, [System.Text.Encoding]::UTF8); Write-Host "tTaxManagerConfig modal: OK" }

# tTokugimuConfig.html
$f = "$base\tokugimu\tTokugimuConfig.html"
$c = [System.IO.File]::ReadAllText($f, [System.Text.Encoding]::UTF8)
$old = "onclick=`"return confirm('入力内容は破棄されます。よろしいですか？')`"> <i`r`n`t`t`t`t`tclass=`"bi bi-arrow-left me-2`"></i>一覧に戻る`r`n`t`t`t`t</a>`r`n`r`n`t`t`t`t<!-- 編集時のみ削除ボタンを表示（台帳から移設） -->"
$new = "data-bs-toggle=`"modal`" data-bs-target=`"#backConfirmModal`"> <i`r`n`t`t`t`t`tclass=`"bi bi-arrow-left me-2`"></i>一覧に戻る`r`n`t`t`t`t</a>`r`n`r`n`t`t`t`t<!-- 編集時のみ削除ボタンを表示（台帳から移設） -->"
$c2 = $c.Replace($old, $new)
if ($c2 -eq $c) { Write-Host "tTokugimuConfig btn: NO MATCH" } else { [System.IO.File]::WriteAllText($f, $c2, [System.Text.Encoding]::UTF8); Write-Host "tTokugimuConfig btn: OK" }

$f = "$base\tokugimu\tTokugimuConfig.html"
$c = [System.IO.File]::ReadAllText($f, [System.Text.Encoding]::UTF8)
$old2 = "<!-- 宛名検索モーダル -->`r`n`t`t<div th:replace=`"~{layout/components :: addressSearchModal}`"></div>`r`n`r`n`t`t<!-- 特別徴収義務者指定モーダル -->`r`n`t`t<div th:replace=`"~{layout/components :: shiteiGassanSearchModal}`"></div>`r`n`t</div>`r`n`r`n`t<!-- JavaScript読み込み -->"
$new2 = "<!-- 宛名検索モーダル -->`r`n`t`t<div th:replace=`"~{layout/components :: addressSearchModal}`"></div>`r`n`r`n`t`t<!-- 特別徴収義務者指定モーダル -->`r`n`t`t<div th:replace=`"~{layout/components :: shiteiGassanSearchModal}`"></div>`r`n`t`t<div th:if=`"`!`${isView}`" th:replace=`"~{layout/components :: linkConfirmModal('backConfirmModal', '確認', '入力内容が破棄されます。よろしいですか？', @{/tokugimu/list}, '一覧に戻る')}`"></div>`r`n`t</div>`r`n`r`n`t<!-- JavaScript読み込み -->"
$c2 = $c.Replace($old2, $new2)
if ($c2 -eq $c) { Write-Host "tTokugimuConfig modal: NO MATCH" } else { [System.IO.File]::WriteAllText($f, $c2, [System.Text.Encoding]::UTF8); Write-Host "tTokugimuConfig modal: OK" }

# tTekiyoNozeiShukiConfig.html
$f = "$base\tokugimu\tTekiyoNozeiShukiConfig.html"
$c = [System.IO.File]::ReadAllText($f, [System.Text.Encoding]::UTF8)
$old = "onclick=`"return confirm('入力内容は破棄されます。よろしいですか？')`"> <i`r`n`t`t`t`t`tclass=`"bi bi-arrow-left me-2`"></i>照会に戻る`r`n`t`t`t`t</a>`r`n`t`t`t</div>`r`n`r`n`t`t</form>`r`n`r`n`t`t<!-- 特別徴収義務者指定モーダル -->`r`n`t`t<div th:replace=`"~{layout/components :: shiteiGassanSearchModal}`"></div>`r`n`t</div>"
$new = "data-bs-toggle=`"modal`" data-bs-target=`"#backConfirmModal`"> <i`r`n`t`t`t`t`tclass=`"bi bi-arrow-left me-2`"></i>照会に戻る`r`n`t`t`t`t</a>`r`n`t`t`t</div>`r`n`r`n`t`t</form>`r`n`r`n`t`t<!-- 特別徴収義務者指定モーダル -->`r`n`t`t<div th:replace=`"~{layout/components :: shiteiGassanSearchModal}`"></div>`r`n`t`t<div th:unless=`"`${isView ?: false}`" th:replace=`"~{layout/components :: linkConfirmModal('backConfirmModal', '確認', '入力内容が破棄されます。よろしいですか？', @{/tekiyo-nozei-shuki/view/{id}(id=`${tekiyoNozeiShukiForm.shiteiNo})}, '照会に戻る')}`"></div>`r`n`t</div>"
$c2 = $c.Replace($old, $new)
if ($c2 -eq $c) { Write-Host "tTekiyoNozeiShukiConfig: NO MATCH" } else { [System.IO.File]::WriteAllText($f, $c2, [System.Text.Encoding]::UTF8); Write-Host "tTekiyoNozeiShukiConfig: OK" }

# furikomiKoza.html
$f = "$base\shoreikin\furikomiKoza.html"
$c = [System.IO.File]::ReadAllText($f, [System.Text.Encoding]::UTF8)
$old = "onclick=`"return confirm('入力内容は破棄されます。よろしいですか？')`"> <i`r`n`t`t`t`t`t`tclass=`"bi bi-arrow-left`"></i> 戻る`r`n`t`t`t`t`t</a>"
$new = "data-bs-toggle=`"modal`" data-bs-target=`"#backConfirmModal`"> <i`r`n`t`t`t`t`t`tclass=`"bi bi-arrow-left`"></i> 戻る`r`n`t`t`t`t`t</a>"
$c2 = $c.Replace($old, $new)
if ($c2 -eq $c) { Write-Host "furikomiKoza btn: NO MATCH" } else { [System.IO.File]::WriteAllText($f, $c2, [System.Text.Encoding]::UTF8); Write-Host "furikomiKoza btn: OK" }

$f = "$base\shoreikin\furikomiKoza.html"
$c = [System.IO.File]::ReadAllText($f, [System.Text.Encoding]::UTF8)
$old2 = "<!-- 特別徴収義務者指定モーダル -->`r`n`t`t<div th:replace=`"~{layout/components :: shiteiGassanSearchModal}`"></div>`r`n`t</div>`r`n`r`n`t<th:block layout:fragment=`"scripts`">`r`n`t`t<script th:src=`"@{/js/furikomiKoza.js}`">"
$new2 = "<!-- 特別徴収義務者指定モーダル -->`r`n`t`t<div th:replace=`"~{layout/components :: shiteiGassanSearchModal}`"></div>`r`n`t`t<div th:unless=`"`${kozaForm.viewMode}`" th:replace=`"~{layout/components :: linkConfirmModal('backConfirmModal', '確認', '入力内容が破棄されます。よろしいですか？', @{/shoreikin/list}, '戻る')}`"></div>`r`n`t</div>`r`n`r`n`t<th:block layout:fragment=`"scripts`">`r`n`t`t<script th:src=`"@{/js/furikomiKoza.js}`">"
$c2 = $c.Replace($old2, $new2)
if ($c2 -eq $c) { Write-Host "furikomiKoza modal: NO MATCH" } else { [System.IO.File]::WriteAllText($f, $c2, [System.Text.Encoding]::UTF8); Write-Host "furikomiKoza modal: OK" }
