# Pitfalls that cost time on earlier modules

Each item says what happened and what to do. They are real, found while automating the Budget module.

## Locators and the DOM

- **The app is MUI with hashed emotion classes and no `data-testid`; inputs have no `name`/`id`.** Locate by visible text, `role`, `aria-label`, `type`, structure ("the input after the caption X"). Never by hashed class. Tooltip text is on span wrappers (`aria-label='Edit this activity'`), the clickable is the button inside.
- **Captions are CSS-uppercased but the DOM text is not** ("COUNTRY *" on screen, "Country *" in the DOM). Use a case-insensitive helper (`translate()` to lower) or compare the DOM text.
- **Labels change after load.** A page can first render "Zone" and switch to "Country" after the business-unit config loads. Wait for the final caption before acting.
- **Text locators also match `<script>` source.** `//*[not(*)][contains(normalize-space(), 'Delete Activity?')]` matched the page's own JS bundle (never displayed), so a displayed-check on the first match said "not shown". Exclude `script`, `style`, `noscript` in every text XPath, and make `visible()` true if ANY match is displayed (not just the first).
- **A hidden element can stay in the DOM** (collapsed table groups are `visibility:hidden`). Count and read only displayed ones when the user would see only those (`addActivityButtonCount` once counted hidden buttons).
- **MUI selects**: `div[role=combobox]` opens `ul[role=listbox] li[role=option]`. Option text may be "NAME ₦rate" built from two elements; read the visible text (`getText`/visible text), not `textContent`, and match by prefix.
- **A dialog or drawer is a `role=dialog`**; identify it by its content (its Done button or its heading), not by index. Its `h2` may contain inputs and hints: read the first text leaf for the name.
- **Cards/rows with repeated structure**: address by position inside a stable container, e.g. product rows have 3 inputs, manpower rows 6 (positional input addressing), and re-query after every change because React re-renders.
- **Regex in Java strings**: heredocs collapse backslashes. Write Java files with the Write/Edit tools, not shell heredocs with `\\d`.

## Inputs that do not behave like plain text

- **`input[type=month]`**: typing keys produces intermediate values (11 -> 2026-01 -> ... -> 2026-11) and fires change handlers each time. Set the value with the native value setter plus `input` and `change` events (what Playwright's fill does). The field is a plain disabled text input until the dependent selection is made, then becomes `type=month` with `min` = current month.
- **Number fields and silent sanitising**: a field may strip a character, turning `1.5` into `15`. That is a different number, not a rejection. After typing an invalid value, read the field back and assert it was not silently converted.
- **Negative cases must have a control.** Without a valid-value case a "Done is disabled" assertion could pass because the form was never valid. Assert the form is valid first (hard assert), then break one field.

## Waits and threads

- **No `Thread.sleep`.** Use `waitUntil(condition, seconds)`; for values that recalculate (totals, counters), wait for two identical consecutive readings (`settledTotals`).
- **`@Test(timeOut = ...)` breaks the driver**: TestNG runs the test on another thread and `BaseDriver.getDriver()` is thread-local, so the driver is null. Do not use it; bound waits inside the test.
- **`@AfterMethod` cleanup must never throw** and must close open dialogs before cancelling, otherwise one failure cascades.
- A "Draft Saved"-type dialog or toast can be transient: poll for it, do not assert it once.

## Data and state

- **Does "Done" write?** In Budget, Done already saves a pre-draft record. Read the document's expected result for every action: if it says saved/ID/status, the action writes, so the test is a WRITE test.
- **Orphans**: Cancel after a save may leave an orphan record (known defect). Track and report it; do not delete.
- **Shared static state**: a one-off write test must not overwrite the "current draft" the serial chain uses (`trackStandalone`).
- **Case-sensitive Request IDs**: `BGT-XXXXXX` regexes with `+` swallow a following letter from the next cell. Use the exact length from the document, with a word boundary.
- A toggle button that changes caption ("Expand All" / "Collapse All"): check the caption before clicking, otherwise the second call is a no-op or collapses.

## Defects and ambiguity

- A defect row says "Fail (D-xx): <actual>" and gives the expected result in the Expected column. Assert the Expected column. The Actual column is what the test will see while the defect exists, use it only in the failure message.
- Rows marked "(confirm)", "define whether", "Not Run", or with no message wording are open requirements. Assert only what is certain, record the rest, ask.
- Two rows can conflict (live totals while typing vs totals only for saved items). Do not pick a side silently: record the observation, assert the unambiguous part, flag the conflict.

## Stand-in app (when real QA is unreachable)

- The stand-in is a throw-away single page in the scratchpad with an in-memory backend. It reads `page.html` at server start: restart the server after every patch (`taskkill //F //IM node.exe`, start again).
- Reproduce the documented defects exactly, and keep a `page-fixed.html` copy with the defects fixed: the defect tests must fail on one and pass on the other.
- Stand-in artefacts are not defects. Examples seen: a null-safe `$()` helper hiding a missing element, dropdown popups running off a small headless viewport, an unstyled dialog positioned off-screen, a field that stripped `.` (which made a decimal test pass for the wrong reason).
- Never present stand-in results as live results.

## Windows shell

- Git Bash paths: `/c/Users/...`; the short `SAMUEL~1.MAN` form works in some shells and not others. Scratch files go in the session scratchpad, not `/tmp` (a Windows Python process cannot see Git Bash `/tmp`).
- Python on Windows prints with cp1252: set `PYTHONIOENCODING=utf-8` or write to files.
- Run Python on untrusted documents with `-I`.
