# PageFlow — a local Notion clone for Android

(up to v0.152 it was called **notE**; the internal package is still
`com.gabriele.notionlocal`, unchanged, because changing it would make a
new app, without the data and without the updates)

A Notion-style notes/database app, with **all data saved on the
device** (SQLite via Room). No network, no account, no cloud.

---

## ⚠️ READ ME FIRST — context for whoever picks up the project

This section exists because the project was developed in one long
conversation, and without this context it is easy to "fix" things that
are deliberate choices, or to retry roads that already turned out to be
dead ends. Test environment: **Samsung Galaxy S25 Ultra, Samsung
keyboard** (relevant: see below).

### Where the project lives (since 24/09/2026)

**The reference copy is the GitHub repository
`AbernathyXVII/MyNotesApp`**, no longer the
`Downloads\NotionLocal` folder on the user's PC. That folder is not
connected to GitHub and from that day on may be older than the
repository: to resume work on the PC **clone the repository**
(see "How to open it") and work there.

The project is now developed in **two alternating ways**:

- **Claude Code on the PC**, with the phone connected via adb: build,
  install, read `logcat`, test everything on the device. This is how
  everything in the "Changelog" came about.
- **Cloud sessions** (claude.ai/code), when the weekly limit of Code on
  the PC is used up. They run on a remote server that **sees neither the
  PC nor the phone**: code can be written, but not installed or tested.
  It can be built **only if the cloud environment lets
  `dl.google.com` through** (the SDK and the AndroidX libraries come
  from there): on 24/09/2026 it was blocked at first, then the user
  added it to the allowed domains and from then on it built. How to do
  it: "Building in a cloud session", below.

### Building in a cloud session

The cloud server starts from scratch at every session: the SDK and
Gradle have to be set up every time (about five minutes). The
environment must let `dl.google.com` and `maven.google.com` through
(session menu → "Edit cloud environment" → Network access). Then:

```
# SDK: cmdline-tools from the address given by
# https://dl.google.com/android/repository/repository2-3.xml
# (entry "cmdline-tools;latest", check the sha1), unpacked into
# /opt/android-sdk/cmdline-tools/latest, then:
yes | sdkmanager --sdk_root=/opt/android-sdk --licenses
sdkmanager --sdk_root=/opt/android-sdk "platforms;android-35" "build-tools;34.0.0" "platform-tools"

# Gradle: the project's version (gradle/wrapper/gradle-wrapper.properties,
# today 8.7) from https://services.gradle.org/distributions/, with the sha256.
# The project has no gradlew: use the downloaded one.
ANDROID_HOME=/opt/android-sdk gradle --no-daemon --max-workers=2 \
  -Dorg.gradle.internal.repository.max.retries=8 \
  -Dorg.gradle.internal.repository.initial.backoff=3000 assembleDebug
```

**The Maven Central trap**: from the cloud server `repo.maven.apache.org`
often answers **429 (too many requests)**, and from the afternoon of
24/09/2026 so does `repo1.maven.org`. It is worked around with a script
in `~/.gradle/init.d/` (outside the project, it does not go in the
repository) that replaces those addresses with the **Maven Central
mirror on Google Cloud Storage**,
`https://maven-central.storage-download.googleapis.com/maven2/` (same
files, and it never answered 429), and that passes the same address to
the tests (`systemProperty 'robolectric.dependency.repo.url'` for every
`Test` task: Robolectric downloads its own Android); and with the
parameters above (fewer parallel downloads, more retries with a wait).
Every run keeps in cache what it has already downloaded.

**The tests** (`testDebugUnitTest`) run there too, including the
repository ones on a real database (Robolectric, `PageRepositoryTest`):
they are the only way to test in the cloud the operations that touch the
data. The first time Robolectric downloads about 150 MB.

**Looking at a screen without a phone** *(since 24/09/2026)*:
`WidgetsRenderTest` draws the sidebar widgets with Robolectric and saves
the images in `app/build/widget-screenshots/`, which can then be opened
and looked at. The same pattern (`@GraphicsMode(NATIVE)`, the window
drawn by hand onto a bitmap, because `captureToImage` never finishes
there) works for any other piece of the interface. It already caught a
layout mistake before the phone did.

**The APK built there must not be installed over the app on the
phone**: it is signed with a debug key born on the server, different
from the PC's, and Android would force uninstalling the app — notes
included. Every change made there is marked **"(to be verified on the
phone)"** and the **"Cloud sessions log"** further down lists, session
by session, the user's requests word for word, what was done and what
has to be tested. **Whoever resumes on the PC after a cloud session
starts from there**: first the entries still to be verified are tested
on the phone, then work goes on.

### ⛔ The boundary: work done in the cloud starts here

**Last state tested on the phone** = the code as it was in the
`Downloads\NotionLocal` folder on the PC on **24/09/2026**, uploaded to
GitHub as it was:

- original archive: commit `83f7b61` ("Add files via upload") on the
  `main` branch — it no longer changes, it is the proof of how it was;
- the same code extracted into the repository: commit `4a35c64`
  ("Importa il progetto notE (NotionLocal) dall'archivio caricato"),
  identical to the archive except for line endings.

**Everything after `4a35c64` is cloud work: written and built (for the
24/09/2026 session: see the Log), NEVER installed or tested on the
phone.** It must be treated as a draft until it passes the procedure
below. To see all the code changes made in the cloud at once:
`git diff 4a35c64 HEAD -- app/` (if that commit can no longer be found,
for example after a "Squash and merge", compare with the contents of the
archive in `83f7b61`).

**Update of the evening of 24/09/2026 — resumed on the PC.** The cloud
work was **tested on the phone** with the PC build: migrations 24→29 on
a real database, and most of the "To be verified" entries (outcome entry
by entry in the Log, above the list; the three bugs found and fixed in
the Changelog, "Resumed on the PC on 24/09/2026"). The phone now has the
build **signed with the PC's debug key** (no longer the cloud one: notE
was not installed), with test notes: the next installs from the PC
update it without asking to uninstall. The entries marked 🟡 and ⏸️ are
still to be tested.

**Since 24/09/2026 (request 38) the phone may have the APK built in the
cloud** (commit `0557c35`), installed by the user after uninstalling
notE, with test notes only. It is signed with a debug key born on the
cloud server (and every cloud session creates a new one): **the next
install from Android Studio on the PC will again ask to uninstall**, and
the notes on the phone will be lost — first copy the database (see
"Technical note") if there is anything in it that is needed. From then
on the key is the PC's again and later installs from the PC keep the
notes.

### Resuming on the PC after the cloud — mandatory procedure

Explicit request from the user: the cloud work **must be re-checked to
perfection on the phone** before building on top of it.

1. **Clone the repository** into a new folder (see "How to open it")
   and sync Gradle with JDK 21.
2. **Connect the Galaxy S25 Ultra** with USB debugging or wireless
   debugging and check that `adb devices` sees it.
3. **Copy the database from the phone BEFORE installing** (commands and
   traps in "Technical note": on Windows it goes through `cmd /c`,
   `-wal` and `-shm` are needed too). Mandatory if the cloud work
   changed the schema; always worth it, because there are real notes in
   there.
4. **Install from Android Studio (▶ Run)**. The build signed with the
   PC's debug key installs over the existing app and **the notes stay**.
   An APK built elsewhere (for example in the cloud) has a different
   signature, Android forces an uninstall and **the notes are lost**:
   don't do it.
5. **If the schema changed**, check that the migration really happened
   (`user_version` and the column at the end of the `CREATE TABLE`, see
   "Technical note").
6. **Test one by one, on the phone and with `logcat` open, all the "To
   be verified on the phone" entries** of the Cloud sessions log, from
   the oldest to the newest. Also reread the diff from the point above
   with a critical eye: whoever wrote it could not see it run. Maximum
   attention to `MergedTextRunField`, `updateRun`, the position mapping
   and the migrations — those are the places where a mistake reaches the
   data (see "Roads already tried").
7. **Update the README with the outcome**: next to every tested entry
   "(verified on the phone on dd/mm/yyyy)", and every problem found
   becomes a `[Bug fix]` in the Changelog.
8. Only at that point does new work resume.

### Deliberate design decisions (they are not bugs)

**The "flowing text blocks" are joined in a shared field.**
Consecutive paragraphs, headings and list items live in ONE
`BasicTextField` (see `MergedTextRunField`), not one per block. They are
separate rows in the database, but a single field in the editor. This is
the most important architectural choice of the project and it was made
AFTER trying the alternative: with one field per block, selecting text
across two blocks and using backspace to join them are impossible,
because they are two fields that don't talk to each other. It is not a
problem that a targeted fix can solve — there were several failed
attempts before this was understood.

**Checkboxes, toggles, tables, dividers and links are "islands".** They
stay separate fields, outside the continuous text flow, handled by
`BlockRow`. This is deliberate: an interactive element (a clickable box,
a collapsible toggle) cannot be "typed" inside a text flow with the same
ease. The choice was discussed explicitly with the user, who preferred
this road to the complexity of integrating them into the continuous
text.

**The joined text field starts with an extra, hidden line break, and it
is needed.** `RUN_LEAD`: the text of `MergedTextRunField` is always
`"\n"` + one line per block. That line break belongs to no block, is not
shown (the position mapping skips it) and is not saved. It exists for one
reason only: to give a backspace pressed at the start of the FIRST line a
character to delete, so that gesture produces a text change. Without it,
the text would stay identical and there would be nothing to observe,
since key events don't arrive. **Don't remove it thinking it is a
leftover**, and if it is replaced, replace it only with another word
separator — a character that does not break words gets swallowed by the
keyboard (see "Roads already tried"). Practical consequence: the caret at
position zero is impossible by any other route, and that is exactly what
makes recognising the gesture unambiguous.

**The indentation (Tab) of flowing text is a visual level, not real
nesting.** `BlockEntity.indentLevel` is a number, like Tab in Word. For
the islands (toggles) real nesting via `parentBlockId` stays instead.
Deliberate simplification, a consequence of the point above.

### Roads already tried that do NOT work

**Giving space to a single line inside the shared text field by touching
the visual transformation.** Two roads were tried: a
`ParagraphStyle(lineHeight = ...)` on the lines with the checkbox, and an
enlarged invisible character at the end of the marker. In both cases the
field **stopped drawing**: empty lines on the screen, no error in
`logcat`, no crash. And the real trouble comes after — that emptiness was
taken as a change by the user and **written to the database**, deleting
the text of the blocks and turning the checkboxes into paragraphs. The
space is instead set as `lineHeight` in the `textStyle` **of the whole
field** (see `CHECKBOX_LINE_HEIGHT`), which touches neither the
transformed text nor the translation of positions.

Broader lesson: the shared field **rewrites the blocks from what it has
in hand**. If the transformation is wrong, the damage does not stay on
the screen, it reaches the data. Any change in there must be tested with
the database under control, not just by looking at the page.

**Launching the app with `monkey` to test it via adb.**
`adb shell monkey -p com.gabriele.notionlocal -c android.intent.category.LAUNCHER 1`
is the common idiom for "open the app", but that final number is the
**count of random events to inject**, not a number of repetitions: every
launch fires a random tap or key inside the app. On 22/09/2026 four
launches like that merged five checkboxes of the root page into one and
emptied it — real data loss, which looked like a defect of the app. Launch
with `adb shell am start -n com.gabriele.notionlocal/.MainActivity`.
The general rule applies too: **driving the app with blind taps at fixed
coordinates**, without a check screenshot between one step and the next,
damages real data. Copy the database first.

**Intercepting the Backspace key via `onPreviewKeyEvent` or
`onKeyEvent`.** Tried twice, in both phases (tunnel and bubble). On the
Samsung keyboard no detectable event arrives for "backspace on an empty
field" — the keyboard communicates in a way that skips Compose's key
event system entirely. Worse: the mere presence of that interception
seemed to disturb normal input. Removed completely. Today
backspace-between-blocks works because it is a CONSEQUENCE of the text
changing in the joined field (see `PageEditorViewModel.updateRun`, the
"fewer lines than blocks" branch), not because someone intercepts a key.

**Using a ZERO-WIDTH SPACE as the hidden character at the head of the
joined text field.** Tried on 2026-09-18 to solve "backspace doesn't
remove the marker from the first item". The underlying idea was right
and is in use today (see `RUN_LEAD` above), but **the chosen character
was wrong**: the zero-width space does not break words, so the Samsung
keyboard absorbs it into the word it is composing and replaces it along
with the rest at every key. The code noticed and put the text back, that
is, it fought with the keyboard at every character, resetting the
composing region. Symptoms observed on the device: first Enter ignored
(the second worked), lists that didn't continue, backspace deleting much
more than it should — exactly the input disturbance already seen with
key interception. Replaced with a line break, which always breaks words.
**Moral: whatever hidden character is used in there must be a word
separator, otherwise the IME eats it.**

**Working out "which line changed" from the before/after comparison
alone.** It is not enough, and the case that proves it is trivial: **an
empty line inserted among other empty lines is identical to all its
neighbours.** The comparison then attributed the insertion to the last
line of the group, and the new line inherited the type of the block down
there instead of the one being typed in. Symptom: `0.` (or any number)
created the list, but the Enter after it gave a paragraph and the list
stopped — and only on pages with empty lines below, which is why it
sometimes showed up and sometimes didn't. Now the field also passes
**where the caret is** (`caretLine`): the change cannot be after it, and
the ambiguity disappears.

**Beware of the convenient diagnosis.** The same symptom had first been
attributed to the save delay (the database a few tens of milliseconds
behind the field). It was a plausible explanation, consistent with the
fact that it didn't reproduce when driving the app from adb — and wrong:
the fix made on that basis changed nothing, and only the log showed the
real cause (the Enter ended up at `mid=25` while the caret was on line
20). The comparison with the field's text (`linesBefore`) stayed because
it is more correct than the comparison with the saved text anyway, but
**that was not the problem**.

**Trusting the list of blocks the UI passes to the ViewModel.** The UI
can be an instant behind (e.g. typing right after an Enter, before
recomposition). The original code silently discarded the change in that
case — the root cause of a series of seemingly unrelated symptoms (Enter
to be pressed twice, lists that didn't continue, block type inherited at
random). `updateRun` now rebuilds the real group from the current state.
**If you touch `updateRun` or `MergedTextRunField`, don't reintroduce the
assumption that the received list is up to date.**

**Resyncing the field with the database at every emission.** It made the
line break just typed get overwritten while the save was still on its
way. Now `MergedTextRunField` keeps track of `pendingText` to tell
"database behind" from "content really changed from outside"
(Undo/Redo, signalled by `externalChangeTick`).

**Setting `pendingText` when no save has started.** `onValueChange` also
fires when only the caret moves or something is selected. If
`pendingText` is set there, nothing ever comes back from the database to
clear it, and from that moment the field ignores every change made
**from outside** (block menu: Duplicate, Turn into): it shows the old
lines, and at the first letter `updateRun` compares lines and blocks that
don't match and **deletes or moves blocks** (found on the phone on
24/09/2026: the copy just duplicated disappeared). `pendingText` is set
**only if the text changed**. Whoever adds an action that changes the
blocks of a group from outside must ask themselves what the field will
see when the new state arrives.

**Putting a snapshot of the blocks back in `orderIndex` order.**
`orderIndex` restarts from 0 inside every toggle, so in a list sorted
only by it a child can come before its parent; if it is reinserted that
way, the foreign key to the parent rejects it and the app closes (Undo,
until 24/09/2026). To reinsert blocks use `inDocumentOrder`: parents
first, children right after.

**Saving blocks one at a time when the change touches many.** Room
notifies whoever observes the table **at every write**: renumbering the
twenty blocks below the split one, a single Enter made the page redraw
twenty times in a row. Inside that storm the field being typed in is
recomposed continuously, focus doesn't reach the new block and the
following letters end up in the old one. Changes that touch several
blocks go into **a single transaction**
(`PageRepository.applyBlockChanges`), and renumbering is **a single
query** (`BlockDao.shiftOrderIndexes`), not one row at a time: that way
there is one notification, and the numbers are computed by the database
instead of the UI's in-memory copy — which two quick Enters made stale,
leaving two blocks with the same number.

**Giving focus to a line that isn't visible.** `LazyColumn` builds only
the rows inside the screen: the block born by pressing Enter at the
bottom of the visible part popped up behind the keyboard, was never
built, and a row that doesn't exist cannot take the caret. Verified with
the log: the focus request went out, and the new row was not among the
composed rows. Before focusing, the row has to be **brought inside the
screen** (`animateScrollToItem`); once it is built, focus finds it.

**Keeping the page in a variable read only once, and saving it whole at
every change.** That is how `DatabaseViewModel` and `PageEditorViewModel`
were, and it is silent data loss: the same page can be open in two
screens at once (a database inside a page and the same database full
screen, or the same page twice in the navigation stack), each with its
own ViewModel. The one left behind doesn't just show an old title: at the
first change — even of a completely different field, because the whole
row is written — it puts it back in the database, and the new name
disappears. **If you add a ViewModel that holds a page, follow it with
`PageRepository.observePage`, not with `getPage`.** And the title text
field keeps a local copy while it has focus: the value coming back from
the database arrives a moment later and, if another key was pressed in
the meantime, would put the text back, eating the character (it is the
same trap as `pendingText` above).

**Intercepting the back key with a `BackHandler` while the keyboard is
open.** It doesn't arrive. With the on-screen keyboard the back key (or
gesture) is received **first by the keyboard**, which eats it to close
itself, and nothing reaches the app: the `BackHandler` is never called.
Verified with the log — the focus event arrived (`hasFocus=true`), the
gesture didn't. It is only useful for the case where the caret is on but
the keyboard is already closed. If what you want is to react to "the user
has finished typing", the right signal is **the keyboard closing**
(`WindowInsets.isImeVisible`), not the gesture.

### Measure smoothness, don't eyeball it

The stutter when scrolling the week cost **three wrong fixes in a row**,
all plausible and all useless, because the way of checking was wrong:
`adb shell input swipe` produces a perfectly linear drag with no fling,
much gentler than a real finger, and the frame measurements came out
clean while the user kept seeing the stutter.

The tools that tell the truth instead:

```
adb shell dumpsys gfxinfo com.gabriele.notionlocal reset
(do the gestures)
adb shell dumpsys gfxinfo com.gabriele.notionlocal
```

It counts the frames really dropped. On the calendar in September 2026 it
gave **3.6% dropped frames and a 99th percentile of 23 ms**: on a 120 Hz
screen the budget is 8 ms, so the "stutter" was not a logic error but too
much work per frame.

To see *where* the content moves, the cross-correlation between
successive frames (not the centroid of the light pixels, which moves by
itself as soon as new content comes in):

```
adb shell screenrecord --time-limit 4 /sdcard/r.mp4
ffmpeg -i r.mp4 -vsync 0 -vf "crop=1440:46:0:1488,format=gray" -f rawvideo strip.raw
```

and then look for the shift that maximises the overlap of the column
profiles. A healthy scroll gives a monotonic series that slows down; a
stutter gives a still frame followed by a double step.

**Beware of two traps**: `screenrecord` closes the file by re-emitting
an old frame, so the last frame always shows the starting screen (an
artefact, not the app); and a Compose debug build drops more frames than
a release — here 6.2% against 3.2%, measured by signing the release with
the debug key.

### Quirks of the test environment

- The **Samsung keyboard** has a documented and troublesome behaviour
  with Compose: characters that get duplicated when the active field
  changes too close to a key press. Mitigated with
  `IME_SETTLE_DELAY_MS` (60ms) before every focus move in
  `PageEditorViewModel`. It is not guaranteed to solve every case — the
  cause lives in the keyboard, not in the code.
- `keyboardController?.show()` must be called **only** if the keyboard
  is not already visible: calling it with the keyboard open disturbs
  input.
- **`Modifier.clickable` doesn't respond only to taps**: it also fires
  with the Enter key when an inner element has focus. On a container
  wrapping text fields it is a trap — use `detectTapGestures`. It has
  already caused a bug (see the changelog)
- The caret of `BasicTextField` is **black by default** — invisible on a
  dark background until `cursorBrush` is set explicitly.

### What the user asked to keep for last

- **Text selection that spans the islands** (e.g. from the text inside a
  toggle to the text outside). Discussed: it would require joining the
  contents of the toggles into the shared field too. The user agreed to
  postpone it to the end of the project.
- **Selection highlight tight on the text** instead of widened to the
  full width (Notion does it, Android by default doesn't). Verified
  feasible (drawing the highlight by hand from `TextLayoutResult`), but
  it is purely aesthetic and the user asked to put it at the bottom of
  the list.

### Agreed working rule

**Update this README at every change of the app**, even if not verified
on the device, labelling every entry as `[New feature]` or `[Bug fix]`.

Reaffirmed and extended by the user on 24/09/2026: **ANYTHING must be
written down in full detail** — every change, every addition, every
removal, every bug found (even if not yet fixed), and also the changes
that don't touch the app (configuration, repository, this very README).
For the latter two more labels are used: `[Removed]` for what is taken
out of the app and `[Project]` for what surrounds the code. Since
19/10/2026 (asked by the user) there is also **🔴 [To review]**, in red:
a choice made between two roads, which the user wants to look at again
later to decide whether to keep it, with the other road written next to
it. In the app's Patch Notes the label is red. In the cloud sessions
**the user's requests** are transcribed too (see "Cloud sessions log").
The stated purpose: coming back to Claude Code, nothing must be started
over from scratch.

**Since 30/09/2026 every Changelog entry is a version** (asked by the
user, see v0.84 and v0.85.1): the heading is `### 🆕 v0.N —
dd/mm/yyyy · hh:mm — Title`. **Since 21/10/2026 the number says what the
version brings** (asked by the user, who had the whole Changelog reread
and renumbered):
- 🆕 **v0.N**, one more in the second number: **an addition**, something
  the app didn't do before;
- ✏️ **v0.N.M**, one more in the third: **a change** to something that
  already existed (how it looks, how it behaves, where it is, removing
  it);
- 🔧 **v0.N.M.K**, one more in the fourth: **bug fixes only**.

A version with several things takes the number of the biggest (with an
addition it is 🆕 even if it fixes bugs). Every entry of a version has
its label (`[New feature]`, `[Change]`, `[Removed]`, `[Bug fix]`;
`[Project]` for how it is made) and, **since 21/10/2026, the platform**
it refers to: `[📱 Mobile]` the phone app, `[💻 PC]`, `[🍎 Mac]`,
`[🌐 Web App]`, more than one together or `[🌍 All]`. For now there is
the phone app; the PC one (Windows and Mac) has been under construction
since 04/11/2026 (v0.153). The day and time are those (Italian time) of
the commit that closes the entry; the new entry goes on top. The
Changelog is also **Settings → Patch Notes** in the app, which copies it
at every build, and `PatchNotesTest` checks that numbers, dates and order
add up. Commits before 21/10/2026 have the old number: see "Old
numbering", after the Changelog.

---

## How to open it

1. Install **Android Studio**: https://developer.android.com/studio
2. Get the project from GitHub: `File → New → Project from Version
   Control`, URL `https://github.com/AbernathyXVII/MyNotesApp`, and
   choose a **new** folder (not the old `Downloads\NotionLocal`, which
   is not connected to the repository). If the project has already been
   cloned, just open its folder with `File → Open` and update it with
   `Git → Pull`.
   **The right branch**: the cloud work is on
   `claude/funny-ramanujan-uuwahs` until its pull request is merged into
   `main`. If it isn't, after cloning switch to that branch
   (`git checkout claude/funny-ramanujan-uuwahs`, or bottom right in
   Android Studio) — `main` would have the code from before the cloud
3. If asked, select a JDK 21 for Gradle (Gradle 8.14, since
   03/11/2026). It has to be redone at every fresh checkout of the
   project: the setting lives in `.idea/`
4. Wait for the Gradle sync (the first time it downloads the
   dependencies — an internet connection is needed)
5. Connect the phone with USB debugging on, press ▶ Run
6. **The PC app** (since 04/11/2026, under construction) is tried with
   `gradle :desktop:run`, or in Android Studio from the Gradle panel:
   `desktop → Tasks → compose desktop → run`

**The versions** (since 03/11/2026, v0.152.3.1): Kotlin 2.4.21, Compose
1.11 (BOM 2026.06.01), Android plugin 8.13.2, Gradle 8.14.3. **Compose
stays at 1.11** until moving to Android plugin 9.1 and compileSdk 37,
which 1.12 requires, and it must get along with Compose Multiplatform
(1.11.x), with which the Windows and Mac app will be made.

## What the app does today

- **A single root page** ("My Space", editable title) acts as Home —
  there is no separate Home screen: it behaves like any other page, with
  the same editor
- **Unified flowing text**: consecutive paragraphs, bulleted and
  numbered lists and checkboxes live in a single shared text field —
  selection and backspace-between-blocks work natively, as in a normal
  text editor
- **Separate islands**: checkboxes, nestable toggles, resizable simple
  tables, dividers, Page/Database blocks (they create a subpage or a
  database and navigate into it right away, as in Notion)
- **Backspace at the start of a line** takes the line apart one step at
  a time: first it removes the list marker, then the indents one by one,
  and only when nothing is left does it merge with the line above — as
  in Notion, even on the first item at the top of the page
- **Typing `1.` at the start of a block** creates a numbered list;
  typing another number (`5.`, and even `0.`) the list starts from there
  and goes on normally. Only if the number is the only thing written in
  the block
- **Enter** creates a new block inheriting the type if it is a list;
  **pasting multi-line text** correctly splits it into several blocks
- **Indent (Tab)** for flowing text is a visual level on the paragraph;
  for the islands (toggles) it is real nesting. On numbered lists the
  indent level also decides the numbering: `1.`, `1.1`, `1.1.1`
- **Inline formatting ("Aa")**: bold, italic, underline, strikethrough,
  spoiler and *(cloud session of 24/09/2026, tested on the phone on
  24/09/2026)* subscript and superscript — it works both in the joined
  text (the selection, even across several blocks, is translated for
  each one) and in the islands
- **Undo/Redo** for text and block structure (not table/database
  cells), with continuous typing grouped into a single step
- **List numbering** on several levels (`1.`, `1.1`, `1.1.1`) driven by
  the indent; **long-pressing the number** opens the list menu — restart
  the count from there, put it back in sequence, remove the number,
  switch to the bullet — as in OneNote
- **Indent/outdent, move up/down, Undo/Redo, "+" menu, Aa** from the bar
  above the keyboard (horizontally scrollable)
- **The block menu** *(cloud session of 24/09/2026, partly tested on the
  phone on 24/09/2026)*: the **six dots** at the start of the bar open
  the menu of the line where the caret is — Turn into (the "/" entries),
  Color on the whole line, Duplicate. **Long-pressing the link to a
  page** opens the page's menu: Color, Edit icon, favorites, Rename,
  Duplicate, Move to, Move to trash. For a database (six dots in its
  bar, or long-pressed link) also, at the top, Turn into page, Turn into
  simple / complex database, Lock database, Open as page
- **Tap anywhere below the title** to start/continue writing
- **Simple database** *(cloud session of 24/09/2026, partly tested on
  the phone on 24/09/2026)*: "Simple database" in the "/" and "+" menus
  creates a database with everything the others have — views, filters,
  sorting, grouping, properties — but **whose rows are only text and
  never become pages**: no OPEN, no row icons or covers; tapping a row
  opens its card, with the name to write and all the properties.
  Something Notion doesn't have
- **Search inside a database** *(cloud session of 24/09/2026, partly
  tested on the phone on 24/09/2026)*: the magnifier next to Sort opens
  a bar there; while typing, the rows that have the word in the name, in
  a property or inside their page remain, with the name highlighted in
  yellow if it is there, or the whole row if the word is inside. In all
  six views
- **Linked views** *(cloud session of 24/09/2026, partly tested on the
  phone on 24/09/2026)*: "Linked view of data source" in the "/" and "+"
  menus puts in a page **the same database** that lives in another one —
  rows, properties, pages and what is written inside are the same — with
  its own filter, sorting, grouping, view and hidden properties. The
  title "↗ Name" leads to the main database
- **Widgets at the top of the sidebar** *(cloud session of 24/09/2026,
  partly tested on the phone on 24/09/2026)*: time zones (analog or
  digital clocks, and a list always from west to east), progress of the
  year, month, week, day and custom dates, counter, pomodoro with session
  and break (and since 25/09/2026 its sound, chosen among the phone's
  ringtones), and since 25/09/2026 a **stopwatch** with laps and
  **timers** (even several at once), like those of the Samsung clock;
  timers added with "+", from the second on, are deleted with the trash
  can at the top right, the first one always stays.
  An arrow hides the whole section, and the choice is remembered
- **Databases inside pages**: creating one from the "+" menu, it appears
  in the page and is edited there; the icon with the arrows opens it
  full screen, and it is deleted from the settings
- **Databases** with the screen modelled on Notion's mobile UI: editable
  title, scrollable table, `+ Add property` after the columns, `OPEN` to
  open a row as a real page, `+ New page` at the bottom. Tapping a header
  opens the property window (name, type with search, delete; since
  24/09/2026 no longer "move", which is in the menu of the long-pressed
  header). Twelve types: Text, Number, Select, Multi-select, Date,
  Checkbox, **Color cell** (since 26/09/2026: the cell filled with a
  colour, with some text on top if wanted), URL, Email, Phone, Created
  time, Last edited time. Since 21/10/2026 (v0.125) also **Relation**,
  which links the rows to those of another database (or the same one),
  even both ways, and **Rollup**, which does a calculation on the linked
  rows
- **Six views for databases** — Table, List, Board, **Calendar**,
  **Timeline** and **Gallery**, one at a time, chosen from Settings →
  Layout and remembered for each database
- **Gallery** *(cloud session of 24/09/2026, partly tested on the phone
  on 24/09/2026: covers missing)*: pages as cards in a grid, with at the
  top the page's **cover** (with the framing chosen in "Reposition") or
  **the beginning of its text**, written as in the page. Below, name and
  properties. From the view settings you choose **Card preview** (None /
  Page cover / Page content) and **Card size** (Small / Medium / Large:
  three, two or one card per row on a phone held upright). It is also
  created from the "/" menu with "Gallery view". Since 25/09/2026,
  long-pressing a card, **"Reposition cover"** moves and enlarges the
  cover in that card only
- **Dates with optional time**: the "Include time" switch in the date
  window adds the time to the start and the end. Off, the date is the
  whole day. **Pickers like Samsung's** (since 25/09/2026): the month as
  a grid with arrows and horizontal scrolling, tapping the month the
  day, month and year wheels; the time on wheels, one minute at a time.
  The end date is asked for with the "End date" switch
- **Hand drawing** (since 25/09/2026): "Draw" in a page's three dots.
  Pen, pencil, marker and highlighter, with thickness and colour; stroke
  or point eraser; Undo and Redo. The S Pen (with pressure, and the side
  button to erase) and one finger draw, two fingers scroll; the palm
  resting while using the pen leaves no marks. Drawings follow the
  lines, the database or the title they were made on
- **Notifications** (since 25/09/2026): the line **`/notification
  22.10.2026 19:41 Take out the trash`** (Enter, and it becomes a line
  with the bell; without a time at 9:00; without text it shows the page
  name), database dates with **"Include notification"** (with the name
  of the row's page, at the start and at the end), and the end of timers
  and pomodoro phases. They arrive even with the app closed, and tapping
  them opens the page
- **Notebooks as in OneNote** (since 26/09/2026): "Notebooks" in the
  sidebar, with notebooks, section groups, coloured sections and pages
  that grow as you write, with the day and time under the title. A world
  apart from the main menu: they are not searched from there and are not
  linked to its pages
- **Locked pages and databases** (since 26/09/2026): "Lock with
  password" in the three dots; they open with the fingerprint or with
  the password, chosen the first time and changed or removed from
  Settings. They also lock what they contain; a locked database inside a
  page becomes a link, and in the gallery their card shows a white
  padlock instead of the cover. Open, they have a closed padlock next to
  the three dots
- **Timeline**: the calendar unrolled. A time axis that scrolls
  horizontally, one row per page with the whole bar (not split at the
  end of the week), the names in a fixed column on the left, weekends
  shaded and a vertical line on the current time. It opens already
  positioned on now, and the **Today** button brings you back
- **Seven Timeline zoom levels** — Hours, Day, Week, Bi-week, Month,
  Quarter, Year — chosen from the same window as Notion ("Select a zoom
  level") and remembered for each database
- **Calendar by year, month or week**, with pages drawn as **bars as
  long as the days they cover**: a date property can have a start and an
  end ("Date started"/"Date finished"), and the bar respects them,
  breaking at the end of the week and staying squared on the side where
  it continues. Tapping an empty day creates a page there, in the week
  you move from one to the next **by dragging with the finger**, and the
  year shows twelve miniature months with the busy days circled
- **Icon and cover for every page**, root included: tap them to change
  them, and the image comes from the **phone's gallery** ("Upload"), of
  which the app keeps a copy. A transparent PNG stays transparent. (Until
  25/09/2026 also from a link: removed on request, and with it the
  network permission. The network came back on 06/10/2026, v0.91, only
  for videos taken from a link and for bookmark previews)
- **The cover can be framed**: from "Reposition" you move it with the
  finger in every direction and enlarge it by pinching, up to four
  times. The file is never cropped or reduced — only three numbers end
  up in the database, and the image stays whole
- **Sorting rows** by any property, ascending or descending, from the
  Sort icon: alphabetical for words, numeric for numbers, chronological
  for dates
- **Grouping the table** by any property: every group has its header
  with the value, the count and its own "+ New page", which creates the
  page already inside that group
- **Repeating dates**, with Google Calendar's shortcuts and a screen for
  custom rules. The page stays a single one: it appears on all the dates
  of the rule and editing it changes them all
- **Filters** on properties: a range of numbers (from x to y, with an
  optional end), the days of a date (a single one or from one day to
  another), the tags of a Select or MultiSelect, and checked or
  unchecked boxes. They open from the icon to the left of Sort or from
  the "Filter" entry in the settings — it is the same window — and are
  thrown away with the trash can at the top right
- **Hideable columns**: long-pressing the header of a property shows
  "Hide", and in "Property visibility" (view settings) there are the two
  lists *Shown in table* and *Hidden in table* to put them back, one at
  a time or all together
- **Columns that move from the header menu**: long-pressing it, **at the
  top of the menu**, above "Edit property", there are **"Move to left"**
  and **"Move to right"**. The first column after Name has "Move to
  left" greyed out — Name always stays first — and the last one has
  "Move to right" greyed out *(documented on 24/09/2026: it was in the
  code but not here; tested on the phone on 24/09/2026, see Changelog)*
- **Hideable database title** when it is inside a page, from the view
  settings. Full screen it always stays visible
- **Full-text search** among titles and block content
- **Font and size of the selected text** *(cloud session of 24/09/2026
  for the whole page; **since 25/09/2026 only on the selected text**,
  tested on the phone the same day)*: in the Aa bar, before B, the font
  entry and the size entry, as in OneNote; they show those of the text
  where the caret is. Select a piece of text and choose: **only that
  piece** changes, like the colour or bold, and another piece can have
  another font. With nothing selected they say "Select some text first".
  Sixteen fonts in three groups — Western, Chinese, Japanese — which are
  **free look-alikes** of the commercial fonts asked for; size from 5 to
  72, 16 by default, **typed by hand** in a small window. The settings
  **no longer** have "Font" and "Font size" for the whole Workspace
  (removed on 25/09/2026, on request)
- **No headings** since 24/09/2026: removed from the app at the user's
  request, now that the text size is chosen from the Aa bar. The ones
  that existed became normal text
- **Text count** *(cloud session of 24/09/2026, tested on the phone on
  24/09/2026)*: at the bottom of a page's three-dots menu there is
  **"X words"**; tapping it opens the list right below, as in Word's
  "Word Count" window — words, letters, numbers, letters and numbers
  together, characters (all signs, symbols included) without and with
  spaces, lines (blocks with some text), Japanese and Chinese characters
  and Korean letters. It counts only the text written in the page: no
  databases, no title
- **Move to** *(cloud session of 24/09/2026, partly tested on the phone
  on 24/09/2026)* from the three dots of a page or a database, and from
  the block menu: the sidebar tree with search at the top, from which
  you choose any page, including database pages; the page ends up **at
  the bottom**. A database always arrives as a link to a page. On the
  main page the entry is greyed out
- **Duplicate** from the three-dots menu, with the choice of where to
  put the copy: next to the original (for a database row, a new row), at
  the bottom of the main menu or inside any page, including the row
  pages of all databases. Everything is copied, subpages and images
  included
- ~~**Fixed dark theme across the app**, with Notion's greys: `#191919`
  for pages, `#202020` for bottom sheets, `#252525` for option boxes. No
  light theme and no dynamic colours~~
  **Superseded** (corrected on 24/09/2026): in Settings → Theme there is
  **Dark / Light / System** (see the "Theme" entry in the Changelog).
  Notion's greys above remain those of the dark theme, which is still
  the default one; no dynamic colours
- **The spell checker** (since 08/10/2026), as in OneNote: the words the
  dictionary doesn't know have a wavy red line under them; tapping one,
  up to five words in its place, "Add to dictionary" and "Ignore". It is
  turned on from Settings → Proofing for all pages (Workspace and
  Notebooks) or from the three dots of a single page, which can also
  have its own language: American and British English, Italian, German,
  French, Spanish. Off by default. The dictionaries are Hunspell's
  (LibreOffice), read by an engine written for the purpose
  (`data/proofing/`), without internet

## Known limits

- **Notebooks** (since 26/09/2026):
  - deleting a page, a section or a notebook is forever: the trash
    belongs to the main menu, and there is no notebook trash
  - notebooks are not searched (the search is the main menu's, which
    doesn't see their pages)
  - from OneNote's menu Export, Move or Copy, Merge and Copy Link are
    missing; pages are not sorted by date or name and have no subpages
  - the page is the editor's, written from top to bottom: you can't
    write at any point of the sheet as in OneNote
- **Password-locked pages** (since 26/09/2026):
  - the fingerprint is there only from Android 11 up; before that, only
    the password
  - a notification ("/notification" or a date with "Include
    notification") shows its text even if it is in a locked page
  - if the password is forgotten, it is reset with the phone's screen
    lock: if the phone has none, locked pages stay closed
- **A larger piece of text makes all the lines of its group taller**,
  not just its own (since 25/09/2026, with the size on the selected
  text). Consecutive flowing text lines are in a single field with **one
  line height** — the one that keeps the list markers all at the same
  pace — and giving it line by line would need one `ParagraphStyle` per
  line inside the transformation, the road that already made the field
  stop drawing and lose text (see "Roads already tried"). So the group's
  height grows as much as the largest piece needs (`lineScaleFor`), and
  goes back as soon as that piece goes back to 16. A group ends where
  there is a block that is not flowing text (table, toggle, page link,
  divider…)
- **Copying with "Select all" from a joined text block brings along an
  extra line break** at the head (the `RUN_LEAD`, see above). Removing it
  would require intercepting copy/cut
- **A decimal number at the start of a block becomes a list**: typing
  `1.5` the conversion already fires on the dot, before the `5` arrives.
  A direct consequence of choosing the dot as the trigger instead of the
  space (`1. `, as Notion and Word do). Choice confirmed by the user:
  sublevels are made with the indent, not by typing `1.1` by hand
- **An indented line that wraps on its own** restarts from the margin
  instead of staying aligned under the text. A consequence of indenting
  with spaces in the prefix instead of with `TextIndent`, which could
  also indent the continuation lines
- **Merging two list items takes two backspaces**: the first removes the
  marker, the second merges. It is Notion's behaviour and the direct
  consequence of the choice above, not an oversight
- **Selection and backspace-between-blocks don't work inside a toggle**:
  its contents stay on the old model with a single field for each
- ~~Selection doesn't cross the boundary of a checkbox.~~ **Solved**:
  checkboxes entered the shared text field, so selecting between one
  checkbox and another is selecting text as anywhere else. The limit
  remains inside a *toggle*, where the child checkboxes are still lines
  of their own
- ~~**Text colour** not available anywhere.~~ **Superseded**: there is
  the brush next to Aa (see "Text colour and colour behind the text" in
  the Changelog). It is still true for the fields in the entry below
- **Page title, simple table cells and database cells** are completely
  flat text, without formatting or colour: bringing it there requires
  building for each of them the infrastructure already made for blocks
- ~~In the "+" and "/" menus (since 24/09/2026 they have the same
  entries), **Callout, Quote, Link to page (as an entry separate from
  "Page"), Image, Video, Audio, Code, File, Web bookmark and Database -
  Full page** are there but greyed out: they were not built — some
  require new capabilities (file handling, media embedding).~~
  **Superseded** (06–07/10/2026, from v0.91 to v0.96): they are all
  there. ~~Linked view of data source too.~~ **Superseded** (24/09/2026,
  request 36): the linked view is there
- **Databases: "Property visibility" doesn't reorder and doesn't
  search.** In Notion the two lists have handles to drag the columns and
  a search field at the top; here the order is changed only by
  long-pressing the column header (Move to left / Move to right), and
  there is no search, because with a handful of properties it would be a
  text field to fill in to scroll a list that is already fully visible
- **Simple database: it is chosen when creating it**, and a normal
  database can't be turned into a simple one or vice versa. A normal
  database has rows that may already be pages full of text, and making
  them "text only" would mean deciding what to do with them
- **Simple database: search leads to the database, not to the row.**
  The result says the row's name and which database it is in, and
  opening it opens the whole database: the row has to be looked for
  there. In the sidebar the simple database has no children, because its
  rows are not pages
- **Databases: a hidden column can't be moved** *(since 24/09/2026)*.
  Moving is done by long-pressing the header, and a hidden column has no
  header; its "Move left / Move right" inside the property window, which
  was the other way, was removed at the user's request. To move it, show
  it again (Property visibility), move it and hide it again. The same
  goes for views without headers (board, list, gallery...): there the
  order of the properties on the cards is changed from the table
- **Databases: visibility is one for the whole database**, not one per
  view as in Notion: hiding a column in the table also makes it disappear
  from the board and list cards. Here a database has one view at a time,
  so keeping a copy for each would mean saving settings nobody ever chose
- **Databases: sorting on only one property at a time**. Notion allows
  chaining several sorts; here the second criterion for equal values is
  fixed
- **Databases: sorting doesn't touch calendar and timeline**, where the
  pages are already in date order — which is the order needed there
- ~~**Databases: search doesn't exist.** Notion's icon next to the view
  was deliberately left out until it works, because an icon that does
  nothing is worse than a missing icon.~~ **Superseded** (24/09/2026,
  request 35): the magnifier next to Sort is there. Filters are there,
  on one property at a time
- **Databases: the Chart view is missing.** There are Table, Board,
  List, Calendar, Timeline and (since 24/09/2026) Gallery; Chart will be
  added when it really draws something, like the others
- **Gallery: the cards are all built together**, images included, as in
  the other views: it is not a "lazy" grid, because the database inside
  a page is in a list that already scrolls and a grid scrolling on its
  own can't be put in there. With a few dozen pages it isn't noticeable;
  with hundreds of covers opening slows down and memory grows. Every
  image is read already shrunk to the card's size anyway
- **Gallery: of Notion's settings only Card preview and Card size are
  there.** Missing are "Fit image" (showing the whole image instead of
  filling the box — here the framing chosen in the page always applies),
  "Show page icon", "Wrap all properties", "Open pages in", grouping and
  the preview from a "Files & media" property, which the app doesn't
  have. Cards can't be dragged to reorder them
- **Gallery: the properties on the cards are plain text**, one per line
  and cut if long, as on the board cards: tags are not coloured.
  "Created time" and "Last edited time" don't appear, as in the board
- **Gallery: the "Page content" preview shows only the text**
  (paragraphs, headings, lists, checkboxes, toggles closed with the
  arrow): no tables, dividers, links to pages or databases, nor the
  content inside toggles. The text is unformatted (no bold, italic or
  colours); **spoilers stay covered**, every letter becomes a small
  square `▒`. A page without a cover, with "Page cover", shows the empty
  box
- **Timeline: the "5 years" zoom Notion has is missing.** Left out on
  purpose: at that scale a month is two millimetres wide and nothing can
  be told apart anymore
- **Dates: no date format and time zone** in the date window, which
  Notion has: here they follow the app's settings. The reminder has been
  there since 25/09/2026 ("Include notification"), but at a fixed time —
  the date's, or 9:00 — and not "one day before" or "one hour before" as
  in Notion
- **Dates: the time applies to the start and the end together**, it
  can't be turned on for only one of the two
- **Timeline: the axis covers at most four hundred cells** around the
  existing dates, and if the dates are wider the window narrows around
  today. The cells are not lazy, they are all composed together, so
  without a cap a wrong date in the year three thousand would build a
  million of them
- **Timeline: bars can't be dragged** to move or lengthen a page, as in
  the calendar. The date is changed from the cell
- **Timeline: scrolling starting from the names column does nothing**,
  because that column is fixed on purpose. The gesture has to start on
  the axis
- **Calendar: bars can't be dragged** to move or lengthen a page. The
  date is changed from the cell, as in the other views
- **Calendar: beyond twenty dated pages in the database the others
  become a "+N"** that can't be opened, and are looked at from the
  table. Every page keeps its row forever, even in the weeks where it
  doesn't appear, so there are as many rows as dated pages: without a
  cap a full database would make every week very tall. It is the price
  of rows fixed forever, chosen knowingly
- **Calendar: every week is as tall as the whole grid of rows**, even if
  it is empty. It follows from the same choice: if a week shrank to the
  pages it contains, the same page would no longer be at the same height
  in all of them
- **Calendar: in the year view there are no bars or titles**, only the
  busy days circled. At twelve months per screen there would be no room
  to write anything readable. A day there isn't tapped to create a page:
  you tap the month and open it up close
- **Calendar: only the week can be dragged with the finger.** Month and
  year are changed with the arrows
- **Databases: board cards can't be dragged** from one column to
  another. To move a row, change its tag
- **Databases: property types that can't be done locally.** Person,
  Created by, Last edited by (there are no accounts or users), Files &
  media, Button, Place, and all the Connections (Google Drive, Figma,
  GitHub, Zendesk) and AI Autofill, which require network and external
  services — against the app's principle. **Relation and Rollup** have
  been there since 21/10/2026 (v0.125); **Formula would instead be
  possible** and is the sensible candidate to add: a project of its own
- **Databases: Relation and Rollup can't be filtered and don't group the
  board** (v0.125): the filter chooses among tags, dates, numbers and
  checkboxes, and grouping among the properties with a single value per
  row
- **Databases: opening a row as a page doesn't show its properties.** In
  Notion they are at the top of the page and are edited from there; here
  the values live only in the table
- ~~**Cover: it can't be moved or cropped.**~~ **Superseded**:
  "Reposition" moves and enlarges it (see "Cover — framing" in the
  Changelog)
- **Images: there is no collection to choose from**, like Notion's
  ready-made covers. You put your own
- **An image sent with Telegram as a "photo" loses its transparency.**
  Telegram compresses it to JPEG, which has no transparency: the icon
  arrives with the white background already in it, and the app can't
  remove it without ruining icons that have white parts. It has to be
  sent as a **file** (on Telegram for PC: untick "Compress images"), or
  downloaded directly on the phone. Verified on 25/09/2026 on a Flaticon
  icon: the file received was `IMG_…jpg`, and sent as a file it stayed
  transparent
- **In the small windows that rise from the bottom the Copy / Paste bar
  belongs to the app**, not to the phone (see the Changelog of
  25/09/2026, "four bugs"): it has Cut, Copy, Paste and Select all with
  Android's labels, but not the extra entries Samsung adds elsewhere
- **Fonts: none of them is the real one.** Helvetica, Arial, Calibri,
  Cambria, Consolas, Georgia, Verdana, Times New Roman, Futura, Garamond
  and SimSun (since 25/09/2026 instead of MS YaHei, which with its
  look-alike was identical to the system font) are commercial: the phone
  doesn't have them and the app can't distribute them. Free look-alikes
  draw them (table in the Changelog, "Pages — font and text size"); five
  have exactly the same metrics as the original, the others resemble
  them. **Verdana** has no faithful look-alike (Noto Sans is the
  closest), **Helvetica** uses Inter so as not to be identical to Arial,
  **Kaiti** is a brush kaishu (Ma Shan Zheng). **Lishu** isn't there: a
  free Lishu doesn't exist, and it was removed by the user's choice
- **Chinese and Japanese fonts: the first time internet is needed** and
  Google Play Services. They weigh 10-20 MB each and are downloaded from
  Google when a page uses them for the first time; then they stay on the
  phone. Until they arrive — or if the first time you are offline — the
  text shows in the system font and changes by itself when the font is
  there. In the picker menu their names are not written in their own
  font, so as not to download them all just by opening the menu
- **Font and size apply to the whole page**, not to a selected piece of
  text: it is what the user asked for ("automatically selecting
  everything"). The **page title** takes the font but not the size (at
  72 it wouldn't fit on the screen); the **databases inside the page**
  stay as they are; the **checkbox squares** and the toggle arrows keep
  their size
- **The size is in sp**, Android's unit, not in typographic points like
  Word and OneNote: the app's default 16 doesn't match OneNote's 11
- **Text count: words are counted as in Word**, that is anything
  between two spaces: even "=" or "[]" alone are a word. Chosen to give
  **the same numbers as Word** on the same notes, which is the comparison
  the user showed
- **Text count: a line entirely in kanji, without a single hiragana or
  katakana, ends up among the Chinese characters.** From a lone
  ideogram the language can't be told, and it is decided line by line:
  one kana in the line is enough for its ideograms to count as Japanese
  (see `textStatsOf`)
- **Text count: the page title doesn't count**, nor do the databases
  inside the page or the names of linked pages. The text inside toggles
  instead counts even when they are closed, and spoilers count as normal
  text. The count is made when the menu opens: it doesn't update while
  the menu stays open (which is also the only moment when you can't
  type)
- **Block menu: "Turn into" for a page or a database is not built** (the
  entry is there, greyed out), nor is "Color" for a database inside the
  page. The colour of a link applies only to that link, not to the page.
  ~~**"Edit icon" changes the icon's image**, as everywhere in the app: a
  page's emoji is not chosen anywhere yet~~ **Superseded** (08/10/2026,
  v0.97): "Edit icon", like every icon, opens Notion's emoji picker
- ~~**Undo after a "Duplicate" of a page** (or of a toggle with a
  subpage inside) removes the link to the copy, but the copy stays: it
  can be found again with search. Undo follows the page's blocks, not
  the pages~~ **Superseded for pages** (request 34): Undo sends the copy
  to the Trash. It is still true for the **Duplicate of a toggle** from
  the block menu with a subpage inside: Undo removes the copy of the
  toggle, and the copy of the subpage stays, to be found with search
- **Move to out of a database loses the row's properties**: the page
  leaves the database, as in Notion, and the column values stay with the
  row, which is removed. The explanation line says so when moving a
  row's page. Name and content stay
- ~~**Undo doesn't bring back a "Move to" or a "Move to trash"**~~
  **Superseded** (request 34): it brings them back, and "Duplicate" too.
  When done from the three dots, the Undo goes to the **next page that
  opens** (going back, or entering the copy): if from there you go
  somewhere else without undoing, you find it in the first page that
  opens after — it is "Undo the last thing done". From the full-screen
  database nothing is undone there (it has no bar): it is done from the
  page you go back to
- **Undo doesn't live beyond the open page**: once the app is closed, or
  the page left long enough for Android to forget it, its history goes
  away, as before
- **"Turn into simple database" can't be undone**: the row pages go away
  forever, as asked (request 31). That is why, if they have content,
  there is first a confirmation with their number
- **Search inside a database**: **dates and checkboxes** are not
  searched, nor are the automatic creation and edit dates (the saved
  value is not the one you read). In properties and pages the whole row
  is highlighted, not the word: inside the cell or the page the word is
  not marked. The text of the pages is read when the search opens or
  when coming back to the screen: if a row's page changes while the
  search is open on another screen (the same page open twice), it shows
  on return. The subpages of row pages count only by name, not by what
  they contain
- **Linked views**: **the order of the properties**, their name, type
  and options and "Center" belong to the database, so they apply to all
  its views too (Notion keeps the order per view); per view are layout,
  filter, sorting, grouping, gallery, calendar, timeline and which
  properties are shown. The **filter is a single one** per view (one
  property, one or more values), as in databases. A view has no name of
  its own: it always shows the database's. A view of a view can't be
  made (you link the real database)
- **Drawing: inside a group of lines the drawing doesn't follow the
  single line.** It is attached to the whole group (the lines between two
  islands, like dividers): writing new lines *inside* the group, above
  the drawing, the text goes down and the drawing doesn't. That is why
  the user uses dividers as boundaries. No lasso selection or zoom, and
  you can't draw on full-screen databases
- **Notifications: the "/notification" line reads the date with the day
  first** (`22.10` is 22 October), whatever the date format chosen in
  the settings. And `9.05` is 9 May, not 9:05: for the time, better use
  the colon (`9:05`)
- **Widgets: city names are the system's English ones** ("Rome",
  "Beijing", "Kolkata"), the same in all languages: translating them
  would need a list of hundreds of cities in eight languages. The time
  zone abbreviation is the English one too ("CEST", "PDT"), the only one
  that exists for everybody
- **Widgets: no "Quarter" and "Life" from the photo** among the fixed
  bars: they were not in the request; they are made with "Add a date"
  (for "Life", from a date of birth to an end date)
- **Widgets: no Undo.** A removed widget (after the confirmation) or a
  counter reset doesn't come back
- **Widgets: they move with the three dots**, Move up and Move down, not
  by dragging them
- **Spell checker** (since 08/10/2026): no Chinese, Korean and Japanese
  (they show greyed out, "Not available yet"); it doesn't check database
  and table cells, code, addresses, all-caps acronyms and words with
  numbers. It is only spelling: it doesn't look at grammar ("the houses
  is")
- **Export/backup** (Markdown+CSV, PDF) designed but not implemented.
  Decision already taken with the user: **Markdown+CSV as the main
  format** (PDF doesn't keep the structure, so it is unsuitable for
  recovering content), **PDF as a second separate function**, not two
  branches of the same one
- **Windows app + sync**: discussed, not started. **Decision taken
  (2026-09-18): "maybe one day"** — the existing schema is not touched
  now, but every new entity must be designed ready for sync (stable
  identifier, time of the last change). Almost zero cost now, it avoids
  a painful migration if the goal becomes real

## Cloud sessions log

The log of the cloud session of 24/09/2026 reported the user's
messages: it is not public.

## Changelog

**The app's patch notes** (since 30/09/2026, asked by the user). Every
request of the user is a **version**, from the first, v0.1, to today, and
the number says what it brings (since 21/10/2026, asked by the user: the
whole Changelog was reread and renumbered this way):
- 🆕 **an addition**, something the app didn't do before: one more in the
  second number (from v0.1 to v0.2, v0.3…);
- ✏️ **a change** to something that already existed, how it looks or how
  it behaves, or removing it: one more in the third (from v0.8 to v0.8.1,
  v0.8.2…);
- 🔧 **bug fixes only**: one more in the fourth (from v0.8 to v0.8.0.1,
  from v0.8.1 to v0.8.1.1…).

A version with several things takes the number of the biggest: with an
addition it is 🆕 even if it fixes bugs. The most recent is on top; under
the title come the entries, each with its label (`[New feature]`,
`[Change]`, `[Removed]`, `[Bug fix]`; `[Project]` for how it is made)
and with **the platform** it refers to: **[📱 Mobile]** the phone app,
**[💻 PC]**, **[🍎 Mac]**, **[🌐 Web App]**, more than one together or
**[🌍 All]**. For now there is the phone app; the PC one has been under
construction since 04/11/2026 (v0.153).

Next to the number, the day and the (Italian) time when the version was
closed: that of the commit, or, for the work that has no commit (until
23/09/2026), that of Claude's last answer before the next request, read
from the conversations saved on the PC. Of the first versions, made in
another chat between 8 and 18/09/2026, neither the time nor the order
remains, and they say so; for two of them the moment is known from the
project's files.

The same text, identical, is in the app's **Settings → Patch Notes**,
which copies it from here at every build (see "Technical note"); the
number of the most recent version is also the app's.

The entries born in the cloud sessions (those of 24/09/2026, from
"Databases — Gallery view" to "Subscript and superscript in the Aa bar")
say that **they still have to be verified on the phone**: when they are,
"(verified on the phone on dd/mm/yyyy)" is added next to the title.

### 🔧 v0.153.60.1 — 12/11/2026 · 09:20 — On the PC ☺️, ♥️ and the other emoji Windows also has as letters, in colour
*(Claude Code on the PC; not yet tested by the user; installed on the phone; 1184 tests passed)*

- **[Bug fix]** **[💻 PC]** **☺️, ♥️ ♠️ ♦️ ♣️, ♀️ ♂️, ‼️, the arrows ↔️ ↕️,
  ©️ ®️ ™️, the keys 0️⃣–9️⃣ #️⃣, ✔️ ✖️ and the small squares ▪️ ◻️** were
  white, as letters, in the emoji picker and wherever they were used: Windows
  also has them among the letters of its own font (Segoe UI), which comes
  before Google's in the list. About thirty emoji out of 1906. Now they are
  in colour in the picker, in the icons of pages, databases and callouts, and
  in the text of the pages. Still as letters, for now, in page titles,
  database cells and the sidebar's names. Not yet tested by the user
- **[Note]** The **families and couples** in a white box with grey figures
  (👨‍👩‍👧, 👪…) aren't a fault: it is how Google draws them in the version
  of September 2026. Drawn with both versions of the font (the vector one
  and the one made of Google's own images), they come out the same
- **[Project]** `emojiFontFamily` (common `expect`: Noto on the PC, `null`
  on the phone) asked where an emoji is drawn alone (`PageIconView`, the
  picker's cells and skin tones, the page's and the database's icon,
  callouts); `withEmojiFont()` gives it to the sequences written with U+FE0F
  in the two transformations of the page text — the whole sequence, with
  keycaps, skin tones and the parts joined by U+200D
  (`emojiPresentationRanges`). `PcEmoji` moved to `shared` (desktopMain).
  Tests: `DesktopEmojiTest` (the sequences; ☺️ yellow as an icon, ♥️ red in
  the text, white without the fix)

### ✏️ v0.153.60 — 12/11/2026 · 09:01 — On the PC Google's emoji, with the flags
*(Claude Code on the PC; not yet tested by the user; installed on the phone; 1182 tests passed)*

- **[Change]** **[💻 PC]** **Google's emoji on the PC**, the same as on
  Android phones (Noto Color Emoji), instead of Windows' ones: everywhere in
  the app, in pages, titles, icons, the emoji picker and the widgets. Now
  the **flags** are drawn too: Windows has none, and showed two letters
  instead ("IT" for 🇮🇹), for example in the sports widget. The letters of
  the text stay Windows' fonts. On the phone nothing changes. Apple's
  emoji can't be used: they belong to Apple and may be used only on its
  devices. Not yet tested by the user
- **[Project]** `PcEmoji` in the `desktop` module: the font
  (`fonts/Noto-COLRv1.ttf`, 5 MB, vector, from googlefonts/noto-emoji
  v2026-09-24, SIL Open Font License in `fonts/Noto-COLRv1-OFL.txt`) goes
  at the end of the font lists Compose uses for the generic families
  (Segoe UI, Arial…), so Skia takes it only for what those can't draw.
  Compose has no public way to do it on the PC (Windows always answers
  first, with Segoe UI Emoji): the list and its cache are touched by
  reflection, marked as a shortcut; if a Compose update changes them, the
  Windows emoji come back and `DesktopEmojiTest` (the 🇮🇹 flag in green
  and red with the normal, Serif and Monospace text) fails

### ✏️ v0.153.59 — 12/11/2026 · 08:07 — The README and the Patch Notes in English, without personal data
*(Claude Code on the PC; not yet tested by the user; installed on the phone; 1181 tests passed)*

- **[Change]** **[📱 Mobile]** **[💻 PC]** **The Patch Notes in English**:
  Settings → Patch Notes shows the whole changelog in English, whatever
  language the app is set to (a translation for each language may come
  later). The labels are now [New feature], [Change], [Removed], [Bug fix],
  [Project], [Note] and 🔴 [To review]. Not yet tested by the user
- **[Change]** **[📱 Mobile]** **[💻 PC]** **No personal data in the
  changelog**: the entries that named the user's own databases and pages,
  or the import of their own data, now tell the same changes in general
  terms. No version was removed and the numbers didn't change
- **[Project]** The README, the GitHub project page (`.github/README.md`)
  and the Release notes are in English, and so are the commit messages from
  now on; the code comments stay in Italian. The section is now
  "## Changelog": `PatchNotesTask` in `app/build.gradle.kts` and the
  `patchNotes` task in `desktop/build.gradle.kts` look for it.
  `UNKNOWN_TIME` is "time not recorded", `REVIEW_TAG` is "[To review]", and
  `PatchNotesTest` checks the English labels and platforms (🌍 All)

### ✏️ v0.153.58 — 11/11/2026 · 18:07 — Closed groups stay closed, and on the PC the six dots a bit further away
*(Claude Code on the PC; not yet tested by the user; installed on the phone; 1181 tests passed)*

- **[Change]** **[📱 Mobile]** **[💻 PC]** **A closed group in a database
  stays closed**: in the table and in the board's bands, closed or opened
  with the arrow, it stays that way when changing page and closing the
  app. Before, it was remembered only while the page stayed open. Every
  view has its own closed groups, and they go into backups too. **Block
  toggles** were already remembered: a test confirms it (closed, page
  left and entered again, it is still closed). Not yet tested by the user
- **[Change]** **[💻 PC]** **The blocks' six dots a bit more to the
  left**: there are 3 more points between the dots and the text, so they
  are no longer stuck to it. Not yet tested by the user
- **[Project]** `GroupRuleSpec.collapsed` (the keys of the closed groups,
  in the rules' JSON: no migration) and `GroupLevel.collapsed`;
  `DatabaseViewModel.setGroupCollapsed`; table and board no longer keep
  the closed groups only in memory. `HANDLE_GAP` from 2 to 5 in
  `BlockHandle.kt`. Tests: `DesktopDatabaseTableTest` (a closed group,
  left and entered again, is still closed; saved in the view),
  `DesktopToggleMemoryTest` (the same for a toggle),
  `DesktopBlockSelectionTest` (the dots further from the title);
  `BoardLayoutTest` keeps the closed bands itself, like the view

### 🔧 v0.153.57.1 — 11/11/2026 · 17:38 — A tag in an empty select cell
*(Claude Code on the PC; not yet tested by the user; installed on the phone; 1179 tests passed)*

- **[Bug fix]** **[📱 Mobile]** **[💻 PC]** **In an empty select or
  multi-select cell a tag couldn't be added**: the click opened nothing,
  while on a cell with a tag the picker opened. The clickable part was as
  tall as the tags, and when empty it was zero tall. Now the whole cell
  is clickable, like the date. It applies to the phone and to the PC. In
  a row's window the empty property now has a strip to click (not
  tested). Found by the user; not yet tested by the user
- **[Project]** `TagCell` in `DatabaseViewScreen.kt`: `fillMaxHeight` and
  a minimum of 18. Test: in `DesktopDatabaseTableTest` the click on an
  empty "Day" cell opens the tag picker; before the fix it failed

### ✏️ v0.153.57 — 11/11/2026 · 17:21 — On the PC the full-page database with Notion's margins
*(Claude Code on the PC; not yet tested by the user; installed on the phone; 1178 tests passed)*

- **[Project]** **[💻 PC]** **The database opened full page has the
  pages' margins**, as in Notion: name, views and rows start at 66 from
  the edge on both sides, aligned with a database inside a page. Before
  they were at 16 from the edge. With the wheel on the margins the rows
  don't scroll: the pointer has to be over the database. Nothing changes
  on the phone. Not yet tested by the user
- **[Project]** `PAGE_INSET` of `PageEditorScreen.kt` used by
  `DatabaseViewScreen` too. Tests: in `DesktopNotionSizesTest` the
  database name starts after the margin; `DesktopDatabaseTableTest` with
  the clicks moved and the right margin empty

### ✏️ v0.153.56 — 11/11/2026 · 17:05 — On the PC a long title wraps
*(Claude Code on the PC; not yet tested by the user; installed on the phone; 1178 tests passed)*

- **[Project]** **[💻 PC]** **A page's title wraps** when it is longer
  than the line, as in Notion: before it stayed on a single line and was
  cut at the margin (and with zoom it happened sooner). Line breaks stay
  out of the name, and Enter leaves the title as before. Nothing changes
  on the phone. Not yet tested by the user
- **[Project]** `TitleField` in `PageEditorScreen.kt`: `singleLine` only on
  the phone, Enter handled by hand on the PC. Test: in
  `DesktopNotionSizesTest` a long title takes several lines in a narrow
  window, and Enter doesn't change it

### ✏️ v0.153.55 — 11/11/2026 · 16:53 — On the PC zoom like in a browser: Ctrl +, Ctrl − and Ctrl with the wheel
*(Claude Code on the PC; not yet tested by the user; installed on the phone; 1177 tests passed)*

- **[Project]** **[💻 PC]** **Zoom, like in a browser**: Ctrl + (also
  Ctrl = and the keypad "+") zooms in, Ctrl − zooms out, and so does the
  wheel with Ctrl held (up zooms in). The steps are Chrome's, from 50% to
  300%. It doesn't enlarge an image: the whole app gets bigger (text,
  lines, sidebar, boxes) and the page lays itself out again, with the
  text wrapping sooner and wider margins. With "Full line view" on (as it
  is by default) long lines stay on one line and scroll sideways, as
  always. The zoom stays even when closing and reopening PageFlow.
  Nothing changes on the phone. Not yet tested by the user
- **[Project]** `PcZoom`, `zoomKeyOf` and `ctrlWheelZoom` in
  `ui/theme/PcZoom.kt`; `AppSettings.zoom`; the PC theme multiplies the
  density by the zoom; `Main.kt` takes the keys and the wheel. Tests:
  `DesktopZoomTest` (the keys, the steps, at 150% the same text wraps
  more times, Ctrl with the wheel zooms without scrolling and without
  Ctrl the wheel scrolls)

### ✏️ v0.153.54 — 11/11/2026 · 16:39 — On the PC the sports widget's full standings, with all the teams
*(Claude Code on the PC; not yet tested by the user; installed on the phone; 1174 tests passed)*

- **[Project]** **[💻 PC]** **The standings are shown whole**: they were
  at most 640 tall and scrolled inside themselves, as on the phone, and
  with the zone labels (Champions League, Europa League, Conference
  League, relegation) the last Serie A teams stayed below, with no scroll
  bar to say so. On the PC standings and players use the whole height of
  the window (and scroll only if it isn't enough). Nothing changes on the
  phone. Not yet tested by the user
- **[Project]** `TableHeight` in `SidebarSports.kt`. Tested with the real
  Serie A standings of 11/11: all 20 teams, about 700 tall

### ✏️ v0.153.53 — 11/11/2026 · 16:27 — On the PC "Players" with the cards, on its own, and "Show less"
*(Claude Code on the PC; not yet tested by the user; installed on the phone; 1174 tests passed)*

- **[Project]** **[💻 PC]** **The sports widget's "Players"**: besides
  goals and assists there are again the yellow and red cards, as on the
  phone, complete too (the top ten and then all). The box opened from
  "Players" shows only the players, and the one opened from "Standings"
  only the standings: no more "Matches", "Standings" and "Players" tabs
  at the top. "Show all" is in the title row of each ranking and, when
  open, becomes "Show less". Nothing changes on the phone. Not yet tested
  by the user
- **[Project]** `CompetitionSheet` with `single`; on the PC
  `SportsApi.leaders` asks for all four complete rankings;
  `WidgetStrings.showLess`. Test: in `DesktopSportsWidgetTest` the
  standings box has no tabs

### ✏️ v0.153.52 — 11/11/2026 · 16:12 — On the PC the sports widget with "Today", the arrows, "Standings" and "Players" under the name
*(Claude Code on the PC; not yet tested by the user; installed on the phone; 1174 tests passed)*

- **[Project]** **[💻 PC]** **The sports widget as the user wants it on
  the PC.** With the mouse over it only the match lights up, no longer
  the big box of the whole matchday, which is no longer clickable. The
  calendar icon at the top is gone: under the competition's name there is
  "Today" (the same thing) with arrows on the left and right to move from
  one matchday to another (with the mouse matchdays aren't dragged), and
  on the right "Standings" and "Players" (in tennis "Ranking"), which open
  the standings and the players in a wider box, where the team names fit.
  In "Players" the complete goal and assist rankings: the top ten, and
  "Show all" shows them all (in Serie A, on 11/11, 171 scorers and 150
  assist makers; the first time it takes a few seconds, then the names
  stay in memory). Nothing changes on the phone. Not yet tested by the
  user
- **[Project]** `PcRoundBar` in `SidebarSports.kt`; `CompetitionSheet` with
  `initialTab`; on the PC `SportsApi.leaders` asks ESPN for `limit=500`
  (up to 8 MB) and `parseLeaderRefs` keeps goals and assists above zero,
  with the names in memory. Tests: `DesktopSportsWidgetTest` (the row
  under the name, with the mouse over a match the matchday title doesn't
  change, the arrows change matchday and come back, "Standings" opens the
  standings) and in `SportsTest` the complete ranking without those at
  zero

### ✏️ v0.153.51 — 11/11/2026 · 15:37 — On the PC "OPEN", "⋯" and "+" appear with the mouse, as in Notion
*(Claude Code on the PC; not yet tested by the user; installed on the phone; 1172 tests passed)*

- **[Project]** **[💻 PC]** **Fourth phase of the Notion look: the
  mouse.** In database tables "OPEN" shows only with the mouse over the
  name cell (its place stays, so the name doesn't move). In the sidebar,
  with the mouse over a page of the tree "⋯" and "+" appear on the right:
  "+" puts a new page inside that one and opens it (like Ctrl+N), "⋯"
  opens a menu to add it to favorites or remove it, rename it or throw it
  in the trash (with the same confirmation as the page menu; throwing
  away the open one goes back to the main menu). On databases there is
  only "⋯". Clickable rows light up with the mouse over them, as before.
  Nothing changes on the phone. Not yet tested by the user
- **[Project]** `NameCell` watches the mouse (`hoverable`); `TreeRow` has
  `onMore` and `onAddInside`; `SidebarPageActions` (in `AppNavHost`, only
  on the PC) and `SidebarPageMenu`. Tests: in `DesktopDatabaseTableTest`
  OPEN isn't there without the mouse and is there with the mouse over the
  name; in `DesktopNotionSizesTest` the tree row shows "⋯" and "+" with
  the mouse over it, and the clicks do their things

### ✏️ v0.153.50 — 11/11/2026 · 15:13 — On the PC the top bar on databases opened full page too
*(Claude Code on the PC; not yet tested by the user; installed on the phone; 1171 tests passed)*

- **[Project]** **[💻 PC]** **The database opened full page has the same
  top bar as the pages** (found by the user: it still had the old
  buttons): back and the sidebar flat and grey, the path (for example
  "Workspace / Notes", a click opens the page) and on the right the three
  dots and the padlock of protected databases. Nothing changes on the
  phone. Not yet tested by the user
- **[Project]** `PcTopBar` and `PcBarButton` (in `PageBreadcrumb.kt`)
  instead of the `TopAppBar` on the PC. Test: in `DesktopNotionSizesTest`
  a database linked inside "Madre" has "Madre / Registro" at the top and
  the click on "Madre" opens it

### ✏️ v0.153.49 — 11/11/2026 · 14:58 — On the PC the top bar with the page's path, and Notion's margins
*(Claude Code on the PC; not yet tested by the user; installed on the phone; 1170 tests passed)*

- **[Project]** **[💻 PC]** **Third phase of the Notion look: the page's
  frame.** Instead of the round buttons floating over the page there is a
  top bar 40 tall, as in Notion: on the left back and the sidebar, small,
  flat and grey, and the page's path (the pages that contain it, with
  icon and name, separated by "/"; a click opens one); on the right the
  three dots, the padlock of protected pages and the arrows between the
  rows of a database. The page starts below the bar, and so does the
  cover. The text has Notion's full-width margins: it starts at 86 from
  the left (before 50), and the same space stays on the right; the cover
  still goes from edge to edge. Nothing changes on the phone. Not yet
  tested by the user
- **[Project]** `PageRepository.ancestorsOf` climbs up from the links like
  the sidebar tree (a row's page is in its database); `PageBreadcrumb`;
  `PC_TOP_BAR`; `PAGE_INSET` at 66 on both sides, `bleedSides` for the
  cover. Tests: in `DesktopNotionSizesTest` a page linked inside another
  has "Madre / Figlia" at the top and the click on "Madre" opens it; in
  `DesktopBlockSelectionTest` the text starts at 86 and the six dots are
  in the margin

### ✏️ v0.153.48 — 11/11/2026 · 14:38 — On the PC lines, menus and sidebar dense as in Notion
*(Claude Code on the PC; not yet tested by the user; installed on the phone; 1169 tests passed)*

- **[Project]** **[💻 PC]** **Second phase of the Notion look: the
  sizes**, taken from the user's photos of Notion. The sidebar entries
  (pages, notebooks, Main menu) are 28 tall as in Notion (before 36–40),
  with smaller icons and tighter margins; the "Notebooks" band is half as
  tall. Table rows are 32 tall (before 44), those of the List view about
  28; the view row ("Table ⌄" and the icons on the right) is lower. In
  menus and boxes (the six-dots menu, a database's settings, "Property
  visibility"…) the entries are about 28 tall (before 48), without the
  grey boxes and without the lines between one entry and the next, and
  the box is 320 wide (before 380). Material buttons no longer widen to
  48 for the finger. Nothing changes on the phone. Not yet tested by the
  user
- **[Project]** `pcOr(pc, phone)` for the sizes; `NoRowLinesOnPc` makes
  the lines inside groups transparent (`OptionsGroup`, `SheetGroup`);
  `LocalMinimumInteractiveComponentSize` turned off on the PC by the
  theme. Tests: in `DesktopNotionSizesTest` a tree row is 28 tall, two
  menu entries between 52 and 61 with no line in between, a settings row
  between 26 and 30; in `DesktopDatabaseTableTest` the table rows are 33
  apart (32 plus the line)

### ✏️ v0.153.47 — 11/11/2026 · 14:19 — On the PC the text is as big as in Notion
*(Claude Code on the PC; not yet tested by the user; installed on the phone; 1168 tests passed)*

- **[Project]** **[💻 PC]** **First phase of the Notion look: text
  sizes**, measured in the user's photo (Notion with "Small text" and the
  zoom at 90%) comparing the width of the same words. The page text, the
  entries of the sidebar, menus, lists and tables go to 12.6 (before 16
  and 14); the page title to 28.8, a database title to 21, the small grey
  labels (like "Pages" and "Favorite pages" in the sidebar) to 10.8.
  Bullets, numbers and line heights shrink together with the text. No
  more extra space between letters. Text enlarged by hand from the Aa bar
  stays as big as it was. Nothing changes on the phone. Not yet tested by
  the user
- **[Project]** `PC_TEXT_SCALE` (12.6/16): on the PC `NotionLocalTheme`
  shrinks everything that is in sp (`Density.fontScale`) and uses
  `PcTypography`; `chosenSizeSp` compensates the sizes chosen by hand
  (`fontStyleOf`, `lineScaleFor`). Tests: `DesktopNotionSizesTest` (title,
  text line and database title as wide as in Notion, within 4%)

### ✏️ v0.153.46 — 11/11/2026 · 13:12 — On the PC the six dots straight, halfway up the letters and the databases' title
*(Claude Code on the PC; not yet tested by the user; installed on the phone; 1167 tests passed)*

- **[Project]** **[💻 PC]** **The six dots are straight**: they sit
  halfway up the letters of the first line, not of the whole line (which
  also includes the space between blocks: they were a couple of pixels
  higher), and they are drawn on whole pixels, all the same size (the
  shrunk icon fell between one pixel and the next). Not yet tested by the
  user
- **[Project]** **[💻 PC]** **Database inside a page: the six dots next
  to the title**, halfway up its line and right to the left of the icon
  (or of the name, if there is no icon); before they were higher. With
  the title hidden they go next to the view row. The six dots that were
  in the view row, before "Table" or "List", are gone on the PC; on the
  phone they stay. Not yet tested by the user
- **[Project]** `runLineBounds` measures from the baseline up as much as a
  capital letter (0.7 of the size); `FirstLine` / `reportsFirstLine`: the
  database title tells the dots where it is; `BlockHandle` draws the six
  dots itself. Tests in `DesktopBlockSelectionTest`: on a line in capitals
  and on a database title the dots are halfway within a pixel and a half,
  and to the left of the title without touching it

### ✏️ v0.153.45 — 11/11/2026 · 10:38 — On the PC the page sits a bit more to the right, with the six dots in the margin
*(Claude Code on the PC; not yet tested by the user; installed on the phone; 1166 tests passed)*

- **[Project]** **[💻 PC]** **The whole page moves 30 points to the
  right**: text, title, icon, embedded databases, tables, images and also
  hand drawings, which stay over the text where they were. In the margin
  that is freed the six dots sit, away from the text and from a database's
  icon (before they touched it). The cover stays edge to edge. Nothing
  changes on the phone. Not yet tested by the user
- **[Project]** `PAGE_INSET` (30 dp on the PC, 0 on the phone) as the
  margin of the page's list; the cover widens to the left (`bleedStart`);
  `inkLayer` and `inkInput` have `originX`, so strokes are drawn and saved
  relative to the moved page. Tests in `DesktopBlockSelectionTest`: the
  text starts after the dots' margin, and the cover touches both edges
  (without `bleedStart` it fails)

### ✏️ v0.153.44 — 11/11/2026 · 10:17 — On the PC small windows open next to the click, and blocks have the six dots
*(Claude Code on the PC; not yet tested by the user; installed on the phone; 1165 tests passed)*

- **[Project]** **[💻 PC]** **Small windows no longer rise from the
  bottom**: a database's settings, "Property visibility", filters,
  sorting, colours, icons, a page's menu and all the others open in a box
  that starts from the point that was clicked, like Notion's menus. It
  always stays inside the window (above the click if there is no room
  below) and doesn't darken the page; Esc or a click outside closes it. A
  window opened from another (a colour from a block's menu) takes its
  place. Nothing changes on the phone. Not yet tested by the user
- **[Project]** **[💻 PC]** **Notion's six dots**: moving the mouse over a
  block they appear in the left margin, at the height of its first line,
  and follow the mouse (scrolling with the wheel too). A click opens that
  block's menu, in the box next to it, with the entries of its type: a
  text line (Turn into, Color, Duplicate…), a database (Lock database,
  Open as page, Move to trash…), a divider, an image, a button. It is the
  same menu that on the phone opens from the bar above the keyboard. They
  aren't there in locked pages or while drawing. Dragging them to move the
  block isn't there yet. Not yet tested by the user
- **[Project]** All small windows go through `NoteBottomSheet`: on the PC
  it calls `PcPanel` (`PcPanel.desktop.kt`, a `Popup` next to the last
  click, recorded by `notePresses` on the window). The dots:
  `BlockHandle.kt` (`blockHover` watches the mouse on the page,
  `BlockHandle` draws them) and `BlockPicker.lineAt` (where a block's
  first line is). Tests: `DesktopDatabaseTableTest` (the three dots open
  the box under the click, not the bottom panel; Esc closes it) and
  `DesktopBlockSelectionTest` (the dots appear next to the line and the
  divider, stay with the mouse over them, the click opens the divider's
  menu and "Delete" removes it; with the wheel they move to the line that
  comes under the mouse)

### ✏️ v0.153.43 — 11/11/2026 · 09:36 — Database tables like Notion: lines all the way, and the three dots next to "+"
*(Claude Code on the PC; not yet tested by the user; installed on the phone; 1163 tests passed)*

- **[Project]** **[📱 Mobile]** **[💻 PC]** **The lines between a table's
  rows go all the way**: in a window (or a page) wider than the columns,
  before they stopped after the last column; now they go to the edge, as
  in Notion. It applies to open databases and to those inside a page
- **[Project]** **[📱 Mobile]** **[💻 PC]** **The three dots to the right
  of the columns' "+"**, in the table header: they open "Property
  visibility", where columns are shown and hidden (before it could only
  be reached from the view settings). On PageFlow it is the panel that
  rises from the bottom. Not yet tested by the user
- **[Project]** The table measures the space it has (`onSizeChanged`) and
  the lines are at least that wide; `AddPropertyCell` with the dots.
  Tests: `DesktopDatabaseTableTest` (in a wide window the lines reach the
  right edge, and the three dots open the panel)

### ✏️ v0.153.42 — 11/11/2026 · 09:20 — On the PC the pin's circle shows only with the mouse over it
*(Claude Code on the PC; not yet tested by the user; installed on the phone; 1162 tests passed)*

- **[Project]** **[💻 PC]** **The grey circle behind the button to pin
  or hide the sidebar appears only with the mouse over it**; without, only
  the icon shows. Not yet tested by the user
- **[Project]** `SidebarPinButton` watches the mouse (`hoverable`). Test: in
  `DesktopEditorTest`, behind the pin there is the sidebar's colour, with
  the mouse over it the circle, and after that the sidebar again (with
  the circle always on it fails)

### ✏️ v0.153.41 — 11/11/2026 · 09:06 — A divider between "Favorite pages" and the pages
*(Claude Code on the PC; not yet tested by the user; installed on the phone; 1161 tests passed)*

- **[Project]** **[📱 Mobile]** **[💻 PC]** **In the sidebar a divider
  separates "Favorite pages" from "Pages"**, the page tree, like the one
  under "Main menu". Not yet tested by the user

### ✏️ v0.153.40 — 11/11/2026 · 08:55 — Favorites and pages right under "Main menu"
*(Claude Code on the PC; not yet tested by the user; installed on the phone; 1161 tests passed)*

- **[Project]** **[📱 Mobile]** **[💻 PC]** **The sidebar is now: "Main
  menu", "Favorite pages", the page tree, then "Notebooks", the widgets
  and the other entries** (Search, Templates, Backup, Trash, Connections,
  Settings). Before, favorites and pages were under the widgets
- **[Project]** **[💻 PC]** **No grey background behind the main page in
  the tree on the PC too**, as on the phone since 0.153.39. Not yet tested
  by the user
- **[Project]** `SidebarContent` in the new order, without
  `rootBackground` anymore. The photo of the sidebar on the PC
  (`app_pinned.png`) shows it

### ✏️ v0.153.39 — 11/11/2026 · 08:41 — "Main menu" at the top of the sidebar, the PC's pin in a circle
*(Claude Code on the PC; not yet tested by the user; installed on the phone; 1161 tests passed)*

- **[Project]** **[📱 Mobile]** **[💻 PC]** **"Main menu" is the first
  entry of the sidebar**, above "Notebooks"; before it was under the
  widgets
- **[Project]** **[💻 PC]** **The button to pin or hide the sidebar is to
  the right of "Main menu", in a circle**, so it isn't confused with the
  entries' icons; before it was on its own, at the top right
- **[Project]** **[📱 Mobile]** **In the page tree, no more grey
  background behind the main page** (the first one at the top) when it
  is the open one; the other open pages keep it, and on the PC it stays
  there too. Not yet tested by the user
- **[Project]** `SidebarContent` with `rootBackground`; the circle's
  colour is `SidebarPinCircle` (with the light theme too). The photos of
  the sidebar on the PC (`app_pinned.png`) show it

### ✏️ v0.153.38 — 11/11/2026 · 08:25 — On the PC Ctrl+Z and Ctrl+Y are the page's Undo and Redo
*(Claude Code on the PC; not yet tested by the user; not installed on the phone (not connected); 1161 tests passed)*

- **[Bug fix]** **[💻 PC]** **Ctrl+Z undoes like the page's Undo
  button**, and Ctrl+Y (or Ctrl+Shift+Z) redoes. Before, inside a line,
  Ctrl+Z used the text field's undo, which only knows the field's text: it
  put back an old text of the group of lines, which the page then
  rewrote onto the blocks (lines joined or split), and it knew nothing of
  dividers, tables or deleted blocks. Now it works in lines, code blocks
  and the page's other fields, and also after deleting blocks chosen with
  the mouse. In the title, in table cells and in database cells the
  field's undo stays. Not yet tested by the user
- **[Project]** `undoKeyOf` in `BlockSelection.kt`, taken by the page's
  fields before the text field. Test: in `DesktopBlockSelectionTest`, with
  a divider removed and the caret in a line, Ctrl+Z puts it back and
  Ctrl+Y removes it again (without the fix it fails)

### ✏️ v0.153.37 — 10/11/2026 · 22:33 — With a divider removed the lines stay tight, and selecting with the mouse scrolls the page
*(Claude Code on the PC; not yet tested by the user; installed on the phone; 1160 tests passed)*

- **[Bug fix]** **[📱 Mobile]** **[💻 PC]** **With a divider deleted, the
  lines below no longer take the space of a heading.** The lines before
  and after a divider are two groups; with it removed they become one,
  and the first piece of the group below had no boundary of its own: it
  ended up in the same field as the large heading above the divider, with
  its line height, and stayed that way while typing. Now a group that
  starts different from the line above the divider already has its
  boundary. It also applies to the second Backspace under a divider on
  the phone. Not yet tested by the user
- **[Bug fix]** **[💻 PC]** **Dragging the selection towards the top or
  bottom edge, the page scrolls by itself**, faster the closer you are to
  the edge or beyond; while it scrolls, the selection follows the page by
  whole blocks (as in Notion). The wheel, while holding the button, is no
  longer taken by the selection. Not yet tested by the user
- **[Project]** `rechunk` gives a piece to the first of a group that looks
  different from the line above the island (`lookAboveIsland`);
  `mouseBlockSelection` scrolls with `scrollBy` near the edges, and the
  field leaves the selected text when moving to blocks. Tests:
  `DividerRemovedChunksTest` (with the divider under a large heading
  removed, the lines below stay in a field of their own: without the fix
  it fails), `DesktopBlockSelectionTest` (+2: the wheel while selecting,
  and dragging to the edge, which scrolls the page and brings the
  selection there)

### ✏️ v0.153.36 — 10/11/2026 · 22:10 — On the PC several blocks with the mouse, Ctrl+A, dividers and table cells
*(Claude Code on the PC; not yet tested by the user; installed on the phone; 1157 tests passed)*

- **[Project]** **Several blocks with the mouse, as in Notion**: dragging
  inside a field selects text as always; going out towards other blocks
  (past a heading, an image, a table) whole blocks are chosen, veiled in
  blue, with Copy, Cut and Delete at the bottom. Del or Backspace delete
  them (it can be undone), Ctrl+C and Ctrl+X copy and cut them, Esc or a
  click elsewhere lets them go. Subpages and databases stay, as when
  emptying the whole page
- **[Project]** **Ctrl+A selects the whole page**, not just the piece of
  text with the caret: the same veil as the phone's "Select all", and the
  keys above (Del asks before emptying the page). In the title and in
  database cells Ctrl+A stays the field's
- **[Project]** **A click on a divider chooses it**, and Del or Backspace
  delete it
- **[Project]** **Several cells of a simple table**: dragging from one
  cell to another chooses the rectangle of cells; Del empties them, and
  with all of them chosen the whole table goes away. Ctrl+C copies them
  (one line per row, tabs between columns), Ctrl+A chooses them all.
  Inside a cell text is selected as before. Not yet tested by the user
- **[Project]** Only with the mouse: with the finger on the phone nothing
  changes. `BlockSelection.kt` (who is where, the gesture, the keys), the
  veil line by line in multi-line fields, `pageCopyLines` and
  `deletePageContent` with the chosen blocks. Tests:
  `DesktopBlockSelectionTest` (a real page drawn and driven with the
  mouse: dragging past the large heading and Del three blocks go away,
  click on the divider and Backspace, Ctrl+A veils the page and Esc lets
  it go, two cells emptied, all the cells and the table goes away)

### ✏️ v0.153.35 — 10/11/2026 · 22:08 — Selecting a large heading the lines below no longer widen
*(Claude Code on the PC; not yet tested by the user; installed on the phone; 1152 tests passed)*

- **[Bug fix]** **[📱 Mobile]** **[💻 PC]** **The lines under a large
  heading no longer take its space.** Selecting the end of a larger text
  (the title of a page taken with the Web Clipper, a heading with the size
  chosen in the Aa bar), all the normal lines below moved apart as if they
  were large too, until the selection disappeared. It happened because a
  selection that reaches the end of a field brings the next one along
  (since 19/10/2026), and a field has a single line height, that of the
  largest text. Now it brings along only a field with lines that look the
  same, as for scrolling (v0.131.0.2). To select past a heading, on the
  PC there are whole blocks with the mouse (v0.153.36). Not yet tested by
  the user
- **[Project]** `PageEditorViewModel.joinChunkAfter` looks at
  `lineLookOf`. Test: `LineLookChunksTest` (+1: the selection at the end
  of the large heading doesn't join the lines below, which stay as tall
  as the others)

### ✏️ v0.153.34 — 10/11/2026 · 21:22 — On the PC the "Sync with the phone" button
*(Claude Code on the PC; installed on the phone after publishing; tested by the user with the phone and the PC on 10/11/2026; 1151 tests passed)*

- **[Project]** **"Sync with the phone" on the PC too**, in Connections,
  under "Wait for the phone": the PC asks the paired phone (at last
  time's address and on the whole home network), and the phone, with
  PageFlow open, starts by itself as with its own button — it always
  decides what goes where. On the PC a spinner until it has finished and
  then the notification; on the phone, on any screen, how it went at the
  bottom. If the phone doesn't answer the PC says so. Before, syncing
  could only start from the phone. Tested by the user on 10/11/2026
- **[Project]** The phone listens on port 47391 while PageFlow is open,
  only if it is paired with a PC, and a request from an unknown PC does
  nothing (`WifiSync.listenForPc`, `SyncServer.askPhones`). Test:
  `DesktopSyncTest` (the PC asks, the phone starts and brings home the
  PC's new page)

### ✏️ v0.153.33 — 10/11/2026 · 19:47 — Sync: a page changed on both sides is merged line by line
*(Claude Code on the PC; installed on the phone; tested by the user with the phone and the PC on 10/11/2026; 1150 tests passed)*

- **[Project]** **The same page changed on the phone and on the PC no
  longer picks the whole page**: it is looked at line by line (and the
  title, the drawings, a database's columns). A line changed on one side
  only takes that side, one deleted on one side and unchanged on the
  other goes away, a new one stays; two new lines in the same place go
  one after the other. Only a line changed on both sides is a conflict:
  the last change wins, and the earlier version stays in the page's
  history ("Before syncing"). So two new pages made on either side in the
  same menu both stay too. The first time, without a "before" to compare,
  nothing is deleted. Tested by the user on 10/11/2026
- **[Project]** `SyncMerge`; the phone remembers the fingerprints of every
  line after syncing (`SyncPeer.elements`, in `sync.json`). Tests in
  `DesktopSyncTest`: the shopping list changed on both sides (a line
  changed and one deleted here, another changed and a new one there),
  and the two menus with a new page each

### ✏️ v0.153.32 — 10/11/2026 · 17:53 — Wi‑Fi sync between phone and PC
*(Claude Code on the PC; tested by the user with the phone and the PC on 10/11/2026; 1149 tests passed)*

- **[Project]** **Phone and PC sync over the home network**, without a
  cloud: in the sidebar "Connections" (the entry that was already there,
  greyed out). On the PC you turn on "Wait for the phone"; on the phone
  "Sync with the PC" finds the PC by itself. The first time the two
  screens show the same six-digit code to confirm (the pairing), and both
  make a backup. Pages, databases (row by row), notebooks, files,
  history, "Updates", the text of photos and the spell checker's words go
  both ways; settings and widgets stay per device. Only what changed:
  changed on one side goes to the other; deleted on one side goes to the
  trash on the other; changed on both sides the last change wins, and the
  other stays in the page's history ("Before syncing"). New pages made on
  both sides in the same page don't lose their place. Tested by the user
  on 10/11/2026: a word written in a page on the PC arrived on the phone,
  and another written afterwards on the phone arrived on the PC
- **[Project]** Everything encrypted: pairing with ECDH (P-256) and the
  code to compare, then AES-GCM with new keys at every sync. Rows are
  copied as they are with SQL (`SyncDatabase`), by column name; the plan
  (`SyncPlan`) compares the fingerprints here, there and of the last
  time. On the PC, Windows asks the first time whether PageFlow can use
  the network. Tests: `DesktopSyncTest` (two PageFlows with their own
  folder, on a real connection: pairing, new pages and files, changes,
  conflict, trash, the PC's search on the network, new pages in the same
  menu), `SyncPhoneTest` (the same rows with the phone's database), the
  photo of "Connections" with the code question

### ✏️ v0.153.31 — 10/11/2026 · 17:30 — On the PC Ctrl+N makes a new page
*(Claude Code on the PC; not yet tested by the user; not installed on the phone (not connected); 1144 tests passed)*

- **[Project]** **Ctrl+N on the PC**: a new page, with the link at the
  bottom of the open page (from the main menu, or from a screen that is
  not a page, at the bottom of the main menu), and you enter it with the
  caret on the first line. Like the phone's quick note: left empty it
  goes away, and without a title it takes its first line. It also works
  while typing in another page. Not yet tested by the user
- **[Project]** The shortcut is received by the window (`AppShortcuts`,
  like Ctrl+F) and the page is born with `createQuickNote`. Test: in
  `DesktopEditorTest`, Ctrl+N from the main menu leaves the link at the
  bottom of the menu and enters the new page

### ✏️ v0.153.30 — 10/11/2026 · 17:25 — On the PC locked pages open with Windows Hello
*(Claude Code on the PC; not yet tested by the user; not installed on the phone (not connected); 1143 tests passed)*

- **[Project]** **Windows Hello for locked pages**, like the fingerprint
  on the phone: opening a locked page the Windows Hello window (face,
  fingerprint or Windows PIN) opens in front of PageFlow, and if it is
  cancelled the password remains. With "Forgot password?" Windows Hello
  confirms it's you and then a new password is chosen, as with the
  phone's screen lock. On the PC the labels say "Windows Hello" instead of
  "fingerprint"; the "Also unlock with Windows Hello" switch is in the
  locked pages' password, in Settings. Before, on the PC, there was only
  the password. Not yet tested by the user
- **[Project]** A PowerShell script asks for it
  (`IUserConsentVerifierInterop`, the way for desktop apps, with
  PageFlow's window), the same helper as the text of photos
  (`WinRtScript`). Tested on 10/11 with the PC locked: Windows' window
  starts; cancelling it from PageFlow doesn't close it, so PageFlow waits
  for the answer. Test: `DesktopHelloTest` (the script compiles and
  Windows answers if Windows Hello is ready)

### ✏️ v0.153.29 — 10/11/2026 · 17:20 — On the PC pasting keeps links (and formatting), as on the phone
*(Claude Code on the PC; not yet tested by the user; not installed on the phone (not connected); 1142 tests passed)*

- **[Project]** **Pasting with links on the PC too**: text copied from a
  browser, from Word or from PageFlow arrives with its links on the right
  words; with "Keep Source Formatting" also bold, italic, sizes, lists and
  tables, and with "Keep Text Only" none of it, as on the phone. Copying
  from PageFlow, the HTML with the style and type of the lines also goes
  to the Windows clipboard, so pasting into another page (or into Word)
  the formatting stays. Before, on the PC, only the text was pasted. Not
  yet tested by the user
- **[Project]** On the PC the clipboard is read from Windows' HTML with
  the same functions as the phone (`parseClipHtml`, `matchAnchors`,
  `htmlStyleRuns`). Tests: `DesktopPasteTest` (a link from a web page
  arrives on the right word; copying some bold text the HTML also goes to
  the clipboard)

### ✏️ v0.153.28 — 10/11/2026 · 10:26 — On the PC Notifications in Settings: the test, turning them off, turning them off for a while
*(Claude Code on the PC; not yet tested by the user; not installed on the phone (not connected); 1140 tests passed)*

- **[Project]** **The "Notifications" section of Settings on the PC
  too**: "Send a test notification", "Turn off notifications" and "Turn
  off for a while" (an hour, eight hours, a day, a week, and "Turn back
  on"), as on the phone, with a line saying how they arrive on the PC
  (with PageFlow open, with Windows' sound; when off, timers and pomodoro
  are silent too). Sound, vibration, priority and badge stay the phone's:
  on the PC notifications are Windows' ones. Before, on the PC, the
  section wasn't there and notifications couldn't be turned off. Not yet
  tested by the user
- **[Project]** Via `hasNotificationSettings`: the section is on both.
  The Settings page drawn on the PC (`settings.png`) shows it

### ✏️ v0.153.27 — 10/11/2026 · 10:18 — On the PC the text in photos is read, to find them by searching
*(Claude Code on the PC; not yet tested by the user; not installed on the phone (not connected); 1140 tests passed)*

- **[Project]** **The text of photos on the PC too**: the block photos
  added on the PC (images, scans, Web Clipper captures) are read in the
  background, one at a time, with Windows' text reader (the Snipping
  Tool's, called from PowerShell), and search finds what is written, as
  on the phone. It reads Windows' languages and Japanese, Korean and
  Chinese (for these only the lines with their characters are kept,
  without the spaces Windows puts between ideograms); very tall photos are
  read in pieces, shrunk as on the phone. Those that came from the phone
  already had their text. Not yet tested by the user
- **[Project]** Choosing the photos to read and saving their text
  (`ImageTextScan`) in the common code: the phone uses it as before with
  Google's reader. Test: `DesktopImageTextsTest` (a text drawn there read
  by the real Windows reader and saved, the lines cleaned up, a photo
  20000 pixels tall read in three pieces)

### ✏️ v0.153.26 — 10/11/2026 · 10:11 — Sounds on the PC too: the counter, the alarm of timers and pomodoro
*(Claude Code on the PC; not yet tested by the user; not installed on the phone (not connected); 1137 tests passed)*

- **[Project]** **Sounds on the PC**, with Java Sound: the clicks of the
  counter widget (the same sounds as the phone, with its volume) and the
  **alarm** when a timer ends or the pomodoro changes phase: the sound
  chosen for that phase or, if there is none, a PageFlow ringtone (three
  bell notes, twice). With notifications off the alarm is silent, as on
  the phone. In the pomodoro settings, on the PC, "sound" opens Windows'
  "Open" for a .wav file (also .aiff and .au; Java doesn't play mp3s); a
  sound chosen on the phone becomes, on the PC, the PageFlow ringtone.
  Reminders have no extra sound: they already ring with Windows'
  notification sound. Before, on the PC, everything was silent. Not yet
  tested by the user
- **[Project]** The waves of the counter's sounds (`CounterSoundWaves`)
  in the common code: the phone plays them as before, the PC with
  `DesktopSounds`. Tests: `DesktopSoundsTest` (the alarm lasts two and a
  half seconds, can be heard and is silent between the two rounds; the
  counter's WAVs open with Java Sound); the phone's `CounterSoundsTest` on
  the common waves

### ✏️ v0.153.25 — 10/11/2026 · 10:03 — The Web Clipper on the PC too, with a hidden Edge
*(Claude Code on the PC; tested on the PC by the user (installer 0.153.2400), the final sharpness not yet; not installed on the phone (not connected); 1135 tests passed)*

- **[Project]** **The Web Clipper on the PC**, from a page's three dots:
  the same window as the phone (link, Full Page, Region, Bookmark,
  Article, Text and Images, Dark mode, title, note). Instead of the
  browser inside the app, PageFlow uses **a hidden Microsoft Edge** (or
  Chrome), driven with its developer tools protocol: it opens the site
  like a laptop (1440 wide), removes the banners and makes the images
  arrive with the same scripts as the phone, then photographs the page in
  strips or reads its article. "Region": first the photo of the whole
  page, then with the mouse you drag the rectangle to keep. Edge has its
  own profile, in the data folder: it doesn't see the history or the
  logins of the everyday Edge. Without Edge or Chrome it says so. Tested
  by the user on the PC
- **[Project]** **Sharper photos on the PC**: the page is really drawn at
  2 pixels per CSS pixel (before the browser shot it at 1 and enlarged it)
  and the JPEG has quality 94; in the PC's pages long images are read as
  big as their box on the screen (before always 1200 pixels) and all of
  them, shrunk, with the quality filter (before the edges were jagged).
  The final sharpness has not been tested by the user yet
- **[Project]** The Web Clipper window, its link with the page, the
  selection veil, the download of the "Text and Images" images and the
  constants (width, user agent, scripts) in the common code; the phone
  keeps its capture windows with the WebView. The installer includes
  `java.net.http` (needed to talk to Edge); `-PinstallerVersion` gives a
  number to test installers (with the same number as the installed one,
  Windows' installer didn't start)
- **[Project]** `DesktopWebClipTest`, with a real Edge on a page written
  there: the photo of the whole page (2880 wide, full height, blue at the
  bottom), the "Region" crop, the article read by "Text and Images"

### ✏️ v0.153.24 — 10/11/2026 · 09:21 — On the PC audio is recorded from the microphone, in audio blocks
*(Claude Code on the PC; tested on the PC by the user (installer 0.153.2301) and installed on the phone; 1134 tests passed)*

- **[Project]** **Recording on the PC**, from an empty audio block as on
  the phone: "Record" turns on Windows' microphone, the recording bar
  shows the time and how loud the voice is, "Stop" puts the file in the
  block ("Recording…"), "Discard" throws it away. The file is a .wav
  (about 2.6 MB per minute, at 22 kHz): Java can't write the phone's
  .m4a; both the phone and Windows play it. Closing PageFlow while
  recording, the recording stops and goes into its block before exiting.
  If Windows denies the microphone to desktop apps, the recording stays
  silent. Before, on the PC, "Record" said the microphone couldn't be
  used. Tested by the user on the PC
- **[Project]** Putting the finished recording into its block
  (`placeRecording`) is in the common code: the phone uses it, as before,
  and so does the PC. Test: `DesktopRecordingTest` (how loud the sound
  is, the .wav written without knowing how long it will be that opens
  whole at the end, the file that goes into the block or, discarded, goes
  away); the old test that wanted the PC's recorder off is gone

### 🔧 v0.153.23.1 — 10/11/2026 · 09:05 — On the PC the sidebar just pinned responds to the first click
*(Claude Code on the PC; tested on the PC by the user (installer 0.153.2300) and installed on the phone; 1132 tests passed)*

- **[Bug fix]** **[💻 PC]** **The sidebar just pinned responded only from
  the second click**: to scroll it or close it again with the arrows you
  first had to click once into nothing. Pinning it, the drawer the sidebar
  opens from stayed empty and zero wide, and so "closed" and "open" were
  the same point: the drawer thought it was still open, and its invisible
  veil over the window took the first click and the wheel. Now the drawer
  closes before pinning the sidebar and, empty, stays as wide as it. The
  `DesktopEditorTest` does the same thing (opens the sidebar, pins it, a
  single click on the arrows closes it again): before the fix it failed.
  Tested by the user on the PC

### ✏️ v0.153.23 — 10/11/2026 · 07:44 — On the PC the window reopens as it was, the app uses the whole window and the sidebar pins and widens, as in Notion
*(Claude Code on the PC; tested with the PC tests (installer 0.153.2200) and on the phone (the usual sidebar); 1131 tests passed)*

- **[Project]** **The PC window reopens as it was**: size, position and
  whether it was maximized ("window" file in the data folder). The first
  time 1200 × 800, centred; before it always opened narrow like a phone.
  If the screen it was on is no longer there, it opens centred
- **[Project]** **The app uses the whole window**: no more 900-point
  column in the centre; the sidebar opens from the window's left edge and
  the page is as wide as the remaining space (chosen by the user: "Full
  width")
- **[Project]** **The pinned sidebar, as in Notion**: the pin at the top
  of the sidebar fixes it next to the page, where it stays until it is
  closed again with the two arrows («); dragging its right edge widens or
  narrows it (from 220 to 480). Pinned and width are remembered at
  reopening too. When pinned, pages don't show the button to open it, and
  the page tree updates by itself when pages and rows change. Nothing
  changes on the phone
- **[Project]** In `DesktopEditorTest` the whole app drawn in a 1200 ×
  800 window with the sidebar pinned (photo in `app_pinned.png`)

### ✏️ v0.153.22 — 09/11/2026 · 23:07 — PDF on the PC too, with real text, and "Print"
*(Claude Code on the PC; tested on the PC by the user (installer 0.153.2100) and on the phone (the PDF is prepared and printing opens; the preview didn't, the printer was off); 1130 tests passed)*

- **[Project]** **"Export" to PDF on the PC**, as on the phone: the text
  is real text (it can be selected and searched), with bold, italic,
  colours, highlight, underline, strikethrough, superscripts and
  subscripts, lists, checkboxes, quotes, callouts, code, tables,
  databases, buttons, images, hand drawings ("Include drawings") and
  "1 / 5" at the bottom of the sheets. "Print" sends it to Windows'
  printing (if it can't, it opens it in the PDF program). The fonts are
  Windows' ones embedded in the file: Segoe UI, Consolas for code and,
  for the missing letters, Segoe UI Symbol, Segoe UI Emoji (emoji come out
  in black and white), Malgun Gothic for Korean, Microsoft YaHei for
  Chinese and Japanese. Lines wrap a bit differently from the phone
  (Segoe UI isn't Roboto). Tested by the user on the PC
- **[Project]** **The PDF layout in the common code** (`PdfExport`): it
  is the same on phone and PC; the text is measured and the sheets drawn
  by each system's tools (`PdfBackend`): on the phone the usual Android
  ones (`StaticLayout`, `PdfDocument`), on the PC PDFBox 3.0.5, a new
  library (the installer grows by about 4 MB)
- **[Project]** `DesktopPdfTest`: a page with a bit of everything becomes
  on the PC a PDF of several sheets in which the text can be found, even
  the Korean (photo of the first sheet in `pdf_page1.png`); the phone's
  PDF tests draw the sheets as before

### ✏️ v0.153.21 — 09/11/2026 · 22:45 — "Export" on the PC too, in Markdown
*(Claude Code on the PC; tested on the PC by the user (installer 0.153.2000) and on the phone; 1129 tests passed)*

- **[Project]** **"Export" on the PC**, from the three dots of a page or a
  database: the same Markdown as the phone (a page alone is an .md; with
  images, subpages or databases a .zip with the folder next to it).
  "Save" opens Windows' "Save as" window with the name already written.
  On the PC "Share" isn't there (there is no choice of apps to send it
  to) and PDF not yet: the phone draws it with Android's tools, and on the
  PC it has to be redone. Before, on the PC, the window closed right
  away. Tested by the user on the PC
- **[Project]** `PageExporter` (preparing the files) and the "Export"
  window in the common code; per system `pdfWriter` (null on the PC for
  now), "Save", "Share" and "Print" (`ExportActions` on the phone, with
  the usual gestures). `exportToFile` is a common function, no longer one
  per system
- **[Project]** `DesktopExportTest`: on the PC a page with an image and a
  subpage becomes the same .zip as the phone, and the PDF isn't made; the
  "Export" window drawn on the PC (`export.png`)

### ✏️ v0.153.20 — 09/11/2026 · 22:25 — The full Backup on the PC, and a PC backup opens on the phone too
*(Claude Code on the PC; tested on the PC by the user (backup and upload) and on the phone (a PC backup restored); restoring on the PC not yet; 1127 tests passed)*

- **[Project]** **The phone's Backup page on the PC too**, the same one
  (moved into the common code): "Back up now", "Upload to Proton Drive"
  (on the PC a "Save as" window, to be pointed the first time at the
  Proton Drive folder or at a USB stick: the next time it opens there),
  the daily backup, the reminder to upload one (a Windows notification
  that opens the Backup page), the list of backups with the upload /
  restore / delete menu, and "Restore from a file…". On the PC backups
  are in Documents/PageFlow backup and the last 7 are kept; the daily one
  is made a minute after opening, and then every hour it checks whether
  it has already been made, while PageFlow is open
- **[Project]** **Restoring on the PC as on the phone**: first a backup
  of how it was (in the list, "Before a restore"), then PageFlow closes
  and reopens by itself with the backup in its place. Before, it closed
  and had to be reopened by hand. After "Delete all content" it now
  reopens by itself too
- **[Bug fix]** **[📱 Mobile]** **A backup made on the PC opens on the
  phone**: the PC keeps all settings as text, and the phone, finding
  "true" written as text where it expected a yes/no, closed at every
  start. Now it reads them anyway. Tested by the user: everything is on
  the phone, even an event created on the PC
- **[Project]** Navigation opens Settings and Backup directly, the same
  on both systems (via `PlatformRoutes`); the PC's backups are in
  `Backups` (instead of `DesktopBackups`). Tests: `DesktopBackupTest` (a
  PC backup comes back as it was: pages, images, settings), the Backup
  page drawn on the PC (`backup.png`), `BackupTest` (a backup with the
  settings as text is read on the phone)

### ✏️ v0.153.19 — 09/11/2026 · 21:58 — Windows notifications: reminders, timers, pomodoro and buttons on the PC too
*(Claude Code on the PC; tested on the PC by the user and on the phone; 1124 tests passed)*

- **[Project]** **Notifications on the PC**, as Windows notifications:
  the reminders of pages ("/notification") and of database dates with
  "Include notification", the end of a timer, the pomodoro phases and
  the buttons' "Send notification", with the same texts as the phone.
  Clicking one opens the page, the row or the widgets sidebar, and the
  window comes back in front even if it was minimized. At the first
  notification PageFlow's icon appears next to the clock; with the window
  closed, it disappears. They arrive only with PageFlow open: one missed
  with the PC off arrives at reopening, if no more than 12 hours have
  passed, and none arrives twice. Tested by the user on the PC
- **[Project]** On the PC the "Notifications" section of Settings isn't
  there yet: sound, priority and "turn them off for a while" belong to the
  phone
- **[Project]** `DesktopRemindersTest`: a reminder arrives only once and
  when it is time, at the first opening the past doesn't all arrive
  together, one missed for more than 12 hours no longer arrives

### ✏️ v0.153.18 — 09/11/2026 · 21:36 — Settings on the PC too: theme, language, formats, spell checker, password
*(Claude Code on the PC; tested on the PC by the user and on the phone; 1122 tests passed)*

- **[Project]** **Settings in the common code**: on the PC they are the
  same as the phone's — theme, start window, title, full line, paragraph,
  animations, number, date and time formats, language, spell checker,
  time zone, locked pages' password, cache, Patch Notes — except the
  "Notifications" section, which isn't on the PC until there are Windows
  notifications. Before, on the PC, they were an empty page. Tested by
  the user on the PC
- **[Project]** What was only the phone's goes through each system's
  functions: the app version (on the PC the one at the top of the Patch
  Notes), the restart after "Delete all content" (on the PC PageFlow
  closes) and emptying the tables (on the PC one by one, in a
  transaction). `AppCleanup` (emptying the cache, deleting everything) is
  in the common code; the cache size is written like the files' one
- **[Project]** In `DesktopEditorTest`: Settings are drawn on the PC
  (photo in `settings.png`), and "Delete all content" leaves only an
  empty My Space

### ✏️ v0.153.17 — 09/11/2026 · 18:28 — On the PC right-click opens the menus, like a long press on the phone
*(Claude Code on the PC; tested on the PC by the user and on the phone; 1120 tests passed)*

- **[Project]** **Right-click like a long press** (`onRightClick`): where
  holding a finger opens a menu, on the PC right-click opens it — database
  rows and cards (list, table, board, gallery, calendar, timeline),
  notebooks, image, video, audio, file, bookmark and button blocks, page
  mentions, list numbers, the sidebar widgets. The right-click stops
  there, so the normal click (opening the row) doesn't fire with it.
  Inside text the usual menu stays (cut, copy, paste). With the finger
  nothing changes; tested by the user on the PC
- **[Project]** `DesktopRightClickTest` (right-click opens the menu and
  not the row, the left one stays the usual click); `SubGroupTest` waits
  for both choices of a grouping before checking them (between the two
  writes there is an instant with only the first, and sometimes it
  failed)

### ✏️ v0.153.16 — 09/11/2026 · 18:14 — On the PC: the page in the centre, Esc to go back, Ctrl+F to search
*(Claude Code on the PC; tested on the PC by the user and on the phone; 1118 tests passed)*

- **[Project]** **The PC window on a wide screen**: the app sits in the
  centre, at most as wide as a page (900 points), and the background
  remains at the sides; before, with the window maximized, the page stuck
  to the left
- **[Project]** **Esc is the PC's "back" gesture**: it closes the sidebar,
  removes the caret from the line being typed in, and then goes back to
  the previous screen, like the phone's gesture (the PC's `BackHandler`,
  which goes through Compose's)
- **[Project]** **Ctrl+F opens search** from any screen, even without a
  caret in a field (`AppShortcuts`: the window receives the key,
  navigation opens "Search")
- **[Project]** Tested by the user on the PC with the updated installer
  over the previous one: the update keeps the notes

### ✏️ v0.153.15 — 09/11/2026 · 18:01 — PageFlow installs on Windows: Start menu, icon, desktop shortcut
*(Claude Code on the PC; installed on Windows by the user; installed on the phone, its code doesn't change; 1118 tests passed)*

- **[Project]** **The Windows installer** (`gradle :desktop:packageExe`,
  `PageFlow-<version>.exe`): it installs for the user, without
  administrator rights, with the entry in the Start menu, the shortcut on
  the desktop and PageFlow's icon (`branding/pageflow.ico`). Java is
  inside too, so nothing else needs to be installed. The data stays in
  `%APPDATA%\PageFlow`: installing it, the notes already opened on the PC
  are found again. Installed and tested by the user
- **[Project]** The version number for Windows comes from the README's
  last entry, with the three numbers Windows wants: 0.153.14 becomes
  0.153.1400, 0.153.11.1 becomes 0.153.1101, so it always grows and a new
  installer updates the old one. Among the Java modules brought into the
  program there are also dates in the eight languages, secure connections
  (weather, sports, stocks) and web page encodings

### ✏️ v0.153.14 — 09/11/2026 · 17:50 — On the PC a phone backup opens: your notes on Windows
*(Claude Code on the PC; tested on the PC with a real backup and on the phone; 1118 tests passed)*

- **[Project]** **Your notes on the PC**: in the PC window's "Backup"
  there is "Restore from a file…", which opens a backup made on the phone
  (the same zip file, read with the same code). As on the phone it goes
  in two steps: first it is extracted from the zip and checked, then
  PageFlow closes and, reopening it, the database, the images and the
  settings (theme, widgets, drawing, the locked pages' password) take
  their place. **Nothing is lost**: what the PC had goes into the
  `before-restore-<time>` folder of its data. Tested with a real backup:
  the PC has everything the phone has. For now it is a copy: what is
  written afterwards on one doesn't reach the other (sync will be for
  that)
- **[Project]** The PC opens only backups of its own database version
  (`DATABASE_VERSION`, 70): the migrations of old databases are in the
  phone's code, and an older database would no longer open. An older
  backup says so ("update the app on the phone, make a new backup"), so
  does a newer one. The settings that go into the backup (`PREF_NAMES`)
  are now the same for phone and PC
- **[Project]** `DesktopBackupTest`: a backup made with the phone's code
  takes the place of the PC's data, and the previous data stays aside; an
  older backup and a file that isn't a backup touch nothing

### ✏️ v0.153.13 — 09/11/2026 · 17:21 — Navigation in the common code: on the PC the app opens, from home to the pages
*(Claude Code on the PC; tested on the phone; 1115 tests passed)*

- **[Project]** **Navigation between screens in the common code**
  (`AppNavHost`): instead of Android's navigation (navigation-compose
  2.7.7) there is JetBrains' multiplatform one (2.9.1), with the same
  routes and the same commands; on the phone it is Google's
  navigation-compose 2.9. Route arguments are read the common way
  (`read { getStringOrNull(…) }`). "Settings" and "Backup", still only the
  phone's, go through `SettingsRoute` and `BackupRoute`: on the PC for now
  they are an empty page with the back arrow
- **[Project]** **The PC window opens the app**: home, sidebar, pages and
  databases, with the same code as the phone, instead of the Patch Notes
  alone. At start it does what the phone does for the common parts
  (settings, widgets, drawing, password, formats). The PC's database is
  still its own and empty: the phone's data will arrive with the backup
  and then with sync
- **[Project]** `DesktopEditorTest`: on the PC the whole app reaches home
  (drawn on the window's thread, like the real window); the photo stays
  in `build/desktop-screenshots/app_home.png`

### ✏️ v0.153.12 — 09/11/2026 · 17:05 — The page editor and databases in the common code: the first page drawn on the PC
*(Claude Code on the PC; tested on the phone; 1114 tests passed)*

- **[Project]** **The page editor and databases are in the common code**
  (`PageEditorScreen`, `DatabaseViewScreen`, the buttons and the day
  calendar): they are the same files for phone and PC. What stays only
  the phone's goes through each system's functions: the Web Clipper
  (`WebClipperFlow`, the whole round from choosing the site to the result
  in the page), the document scanner, the "Export" window and the
  editor's test log. On the PC, for now, Web Clipper and export close
  right away and the scanner does nothing
- **[Project]** Now there are 276 common files (90,800 lines) and 60 only
  for the phone (10,900): notifications, alarms, backup, settings, Web
  Clipper, players, export and navigation between screens
- **[Project]** **More memory to build**: the editor in the common code
  didn't build for the phone with 2 GB ("Java heap space"); now Gradle
  and the Kotlin compiler have 4 (`gradle.properties`)
- **[Project]** `DesktopEditorTest`: on the PC the editor draws a real
  page — icon, title, lines — and loads its lines from the database. It
  is the first PageFlow page drawn on Windows

### 🔧 v0.153.11.1 — 09/11/2026 · 16:41 — The name of images downloaded on the PC, and "Save a copy" checked by the tests
*(Claude Code on the PC; installed on the phone, the phone's code doesn't change; 1113 tests passed)*

- **[Bug fix]** **[💻 PC]** **The name of an image downloaded on the PC**
  (found reviewing v0.153.11): it came from the block's name, which for
  Web Clipper images is the web page's descriptive text. One ending with
  a dot ("A chart of the emissions.") gave a file without an extension,
  one with `:` or `?` wasn't saved, and one with `..\` could end up
  outside Pictures/PageFlow. Now only the last piece of the name is kept,
  without the characters Windows doesn't want or the final dots, and with
  the image's real extension
- **[Project]** `SaveCopyTest`: a file block's "Save a copy" passes the
  proposed name to the system's "Save to…" and copies the file where the
  user chose (the test's "Save to…" is fake and answers right away). In
  the PC's `DesktopFilesTest`, the names of downloaded images

### ✏️ v0.153.11 — 04/11/2026 · 15:07 — Images, videos, audio and files of blocks in the common code
*(Claude Code on the PC; tested on the phone; 1111 tests passed)*

- **[Project]** **The Image, Video, Audio, File and Web bookmark blocks in
  the common code.** Choosing an image or a file, copying it into the app
  and saving a copy of it goes through each system's actions
  (`rememberImageImporter`, `rememberFileImporter`, `rememberSaveCopy`):
  on the phone the usual pickers, on the PC the Windows or Mac window.
  `BlockFiles` is split the same way: on the PC a file opens with the
  computer's program and a downloaded image goes into Pictures/PageFlow
- **[Project]** **The players** of videos and audio (`MediaSource`: an app
  file or a link), YouTube and Vimeo inside the page, very tall images and
  the recorder of audio blocks are each system's actions: on the phone the
  usual ones, on the PC for now a line that opens the video or the audio
  with the computer's program, the whole image shrunk, and no recording.
  The recording bar is in the common code
- **[Project]** In the common code also the icon and cover window, the
  databases' image cells and the block actions menu: now there are 268
  files (70,000 lines) and 63 only for the phone (31,600). From the editor
  the Web Clipper, the scanner, export and notifications are still missing
- **[Project]** `DesktopFilesTest`: on the PC an image's type is read from
  its first bytes, YouTube and Vimeo links are recognised as on the phone,
  and the recorder says no

### ✏️ v0.153.10 — 04/11/2026 · 14:30 — Clipboard, fonts, spell checker and Web Clipper towards the common code
*(Claude Code on the PC; tested on the phone; 1107 tests passed)*

- **[Project]** **The editor's clipboard in the common code**: the paste
  modes, the marks of pasted links and styles, the type of copied lines
  and the formatting HTML (`PasteLinks.kt`). `LinkClipboard` is a class
  per system: on the phone the usual one, which reads Android's clipboard
  with its HTML; on the PC, for now, the plain clipboard. The copy, paste
  and cut buttons of the bar above the keyboard also go through a common
  action (`rememberEditShortcuts`), and the selected page's "Copy" through
  `copyRichText` (on the PC the text and the HTML in the Windows or Mac
  clipboard)
- **[Project]** **The pages' fonts**: names, look-alikes and groups in the
  common code (`PageFontNames.kt`); the family to draw is per system: on
  the phone the included fonts and Google's ones as before, on the PC for
  now the most similar system font
- **[Project]** The spell checker reads the dictionaries with
  `openAppAsset` (on the PC from the program's resources), and the content
  of pages saved with the Web Clipper ("Text and Images") is read with
  Kotlin's JSON library instead of Android's. Six more files in the common
  code: now there are 254 (66,000 lines) and 67 only for the phone
  (35,000)
- **[Project]** `DesktopTextTest`: on the PC the Italian dictionary is
  read from the resources and recognises the words, the Web Clipper JSON
  is read, and every font has a name and a family

### ✏️ v0.153.9 — 04/11/2026 · 14:11 — The sidebar with the widgets, notebooks, icons and password lock in the common code
*(Claude Code on the PC; tested on the phone; 1104 tests passed)*

- **[Project]** **The sidebar widgets in the common code**, with
  `WidgetStore`: it saves with `SettingsStore` (on the phone the same
  "widgets" preferences as always, on the PC the `widgets.properties` file
  in the data folder). Timer notifications, the end alert and the alarm go
  through `WidgetEffects`: on the phone they are `TimerNotifications`,
  `Notifier` and `NoteAlarms` as before, on the PC nothing for now. The
  "once only" sound of pomodoros and notifications is `RingtonePlayer`, on
  the phone
- **[Project]** **More actions per system**: the open keyboard and where
  it is going (`isKeyboardVisible`, `keyboardTargetInsets`), the date and
  time pickers' window at the bottom of the screen (`PlaceDialogAtBottom`),
  the cache folder and images from bytes (`cacheDirOf`,
  `decodeImageBitmap`, for the crests), the files that travel with the app
  (`readAppAsset`: on the PC the phone's assets are among the program's
  resources) and the emoji the system can draw, the choice of the
  pomodoro's sounds (`rememberSoundPicker`), the counter's sounds and the
  fingerprint (`Biometrics`: on the PC there is only the password). The
  image as the icon of a page or a notebook is chosen with
  `rememberImagePicker`
- **[Project]** **44 more files in the common code**: the sidebar with all
  the widgets, notebooks, the icon picker and the emoji, the password lock,
  the date and time pickers, the spell checker, many database cells and
  menus. Now there are 247 common files (65,000 lines) and 70 only for the
  phone (36,000), almost all of the editor and the database. Unused
  imports removed from the editor and the database
- **[Project]** `DesktopWidgetsTest`: on the PC a counter is saved in the
  widgets file, the picker's emoji are read from the program's resources,
  and the cache folder and images from bytes work

### ✏️ v0.153.8 — 04/11/2026 · 13:26 — More knots untied towards the editor: "back", photos, export and notifications per platform
*(Claude Code on the PC; tested on the phone; 1101 tests passed)*

- **[Project]** **Each system's things the editor and the database use**
  are declared in the common code: the "back" gesture (`BackHandler`, on
  the phone Android's), choosing an image for icon and cover
  (`rememberImagePicker`: on the phone the system photo picker, on the PC
  choosing a file), export to a file (`exportToFile`: on the phone
  `PageExporter`, on the PC it will come), a button's notification
  (`postButtonNotification`), a link's site and scheme (`urlHost`,
  `urlScheme`: on the phone Android's `Uri`), the view's height
  (`rememberRootHeight`, for the day calendar). `LocalContext` became
  `LocalPlatformContext` in 28 files (on the phone it is the same
  `Context`); `ExportedFile` and `ExportOptions` are in the common code
- **[Project]** Six more files in the common code (the buttons' code,
  code blocks, page mentions, export from the page, notification
  requests, link sites); out of the editor into `EditorParts.kt` went the
  cover, the icon and cover picker, the "/" menu catalogue and list
  numbers, and out of notifications reading the `Intent`
  (`NotificationIntents.kt`). Colour written as text goes through
  `parseColorString` in buttons, calendar and numbers too. Still to untie:
  the widgets (`WidgetStore`), Samsung's date and time pickers and
  Android's keyboard measurements

### ✏️ v0.153.7 — 04/11/2026 · 10:10 — The first knots untied: search, favorites, trash and templates in the common code
*(Claude Code on the PC; tested on the phone; 1101 tests passed)*

- **[Project]** **30 more files in the common code** (now 184, 43,500
  lines; on the phone's side 105 remain, 57,000 lines, almost all the
  editor and the database): the search, favorites, trash, templates and
  start screens, the pieces of the database views (`DatabaseViews`,
  "Group", "Date by", relations, tag order, the arrows between rows),
  mentions, ink (`InkLayer`, `InkTools`), the page's bookmarks, "back to
  top", the screens' common parts (`ScreenParts`) and a page's image
  (`PageImageView`)
- **[Project]** **The knots untied**: the pieces all screens use that were
  inside `DatabaseViewScreen` (`SheetGroup`, `SettingsRow`,
  `SheetTextField`, `TagChip`, row groups, date names…) are now in
  `DatabaseParts.kt`; out of the editor went `PageImage` and `LinePrefix`,
  out of other files the choice of how a link looks, the name of the
  "Name" column, the counter's sounds, the Web Clipper modes, the
  notification permission requests. The 50 `Toast`s go through
  `showMessage` (on the phone they are still `Toast`s, long ones too),
  screen sizes through `screenWidthDp`/`screenHeightDp` (on the phone the
  previous ones), tag colours through `parseColorString` (the same colours
  as Android's `Color.parseColor`, checked by `ColorParseTest` on the tag
  colours), the image to draw through `toImageBitmap`. Dropping the caret
  when the keyboard goes down is the phone's (on the PC there is no
  on-screen keyboard), and the ink tools are remembered with the common
  settings (on the phone in the usual `ink` file)
- **[Project]** How it is done: one tool removes from a file the
  declarations needed elsewhere (with those they use), another cleans up
  the imports, and the usual round moves into the common code everything
  that builds both for the phone and for the PC. A "sealed" class of the
  common code can't have heirs in the phone's code: that is why
  `Widgets.kt` stays with the widgets, until they all move together

### ✏️ v0.153.6 — 04/11/2026 · 09:44 — The whole phone app in the shared module, next to the PC's code
*(Claude Code on the PC; tested on the phone; 1099 tests passed)*

- **[Project]** **The `app` module now has only `MainActivity`,
  `ShareActivity` and the quick note widget** (which uses the app's layout
  and texts). Everything else, 115 files (the editor, databases, the other
  screens, notifications, backups, the Web Clipper, recording…), is in
  `shared/src/androidMain`, with the same libraries as before: on the phone
  the code is the same. The pages' fonts and the notification icon are
  resources of `shared` (`com.gabriele.notionlocal.shared.R`);
  notifications open `MainActivity` by name (`mainActivityIntent`),
  because the class isn't visible from `shared`
- **[Project]** **Why**: the screens use one another, and on their own
  they didn't move to the common code. Now they are in the same module
  and can move one piece at a time, with the same round as before (they
  all move, those that don't build for the PC go back): `UpdatesScreen` is
  the first. A count says where the knots are: the pieces all screens use
  that are inside `DatabaseViewScreen` (`SheetGroup`, `SettingsRow`,
  `SheetTextField`, `TagChip`…), the widgets (`WidgetStore`), the ink tools
  (`InkTools`) and some Android-free functions locked in Android files.
  The common code also has the multiplatform libraries of
  `collectAsStateWithLifecycle` and `viewModel(...)` (on the phone the
  usual 2.9.4); `AutoCapsTest` looks for text fields in the sources of
  `shared` too

### ✏️ v0.153.5 — 04/11/2026 · 09:23 — The first screens in the common code, and each system's actions
*(Claude Code on the PC; tested on the phone; 1099 tests passed)*

- **[Project]** **25 files in `shared`**, among them the first screens:
  the colour picker (`ColorPickerSheet`), date reminders, "Full line
  view", resizing images, "Move to", the page's history, the page's scroll
  bar, the panels' text bar (`SheetTextToolbar`), code colours, Material
  icons (`MaterialIconSet`), synced blocks (`SyncedBlockUi`), frozen
  tables, and then the spell checker (dictionaries and suggestions),
  Markdown export, the backup archive, navigation routes and other pieces.
  Which files move is decided by the compiler: a round moves all those
  without Android, those that don't build go back as they were, and it
  repeats until the common code builds
- **[Project]** **Each system's actions** (`platform/PlatformActions.kt`):
  a short notice, opening a link, copying and reading the clipboard, the
  names of Cut/Copy/Paste/Select all. On the phone they are Android's from
  before (the `Toast`, the `Intent` for the browser, Android's clipboard,
  the command names in the phone's language); on the PC the browser and
  the clipboard of Windows and Mac, and notices at the bottom of the
  window (`DesktopMessages`). `BlockFiles.openUrl`, cell links
  (`CellText`) and the panels' text bar already use them; `ScreenParts`
  takes the common context. `AutoCaps` (the phone keyboard's capital at
  the start of a sentence) is declared in the common code: on the phone it
  is the one from before, on the PC it leaves the field as it is.
  `ViewModelFactory` is in `shared`, and asks for ViewModels by Kotlin
  class (`KClass`), as the library does
- **[Project]** **What remains**: the screens use one another, and many
  pieces they all use (`SheetGroup`, `SettingsRow`, `PageImage`,
  `NoteBottomSheet`…) are inside the two biggest files, the editor
  (`PageEditorScreen`) and the database (`DatabaseViewScreen`). The next
  step moves them together, with the Android parts (videos, the Web
  Clipper, the camera, choosing files) separated per platform

### ✏️ v0.153.4 — 04/11/2026 · 09:06 — ViewModels in the common code: screens move the same way on the PC
*(Claude Code on the PC; tested on the phone; 1099 tests passed)*

- **[Project]** **The ViewModels are in `shared`**: the editor's, the
  databases', search's, the sidebar's, notebooks', ink's and the other
  screens' (the multiplatform ViewModel library 2.9.5, which on the phone
  is Google's 2.9.4, the same as before). `ViewModelFactory` stays in the
  app: the PC will have its own. With them moved `LinkPreviews` and
  `NaturalOrder`, and some pieces taken out of Android files: `EraserMode`
  (from `InkTools`), `linkUri` (from `CellText`), `runGroupKey` and the
  bullet list marks (from `PageEditorScreen`, now `RunGroups.kt` and
  `BulletMarkers.kt`), the default, minimum and maximum text size (from
  `PageFonts`, now `PageFontSizes.kt`) and `QuickNotes` (reading the
  request from the `Intent`, `takeFrom`, stays in `Incoming.kt`)
- **[Project]** What was Android in the editor: export to a file is an
  extension in the app (`PageEditorExport.kt`, called as before); the text
  of photos is asked of `PhotoTextRequests`, where on the phone
  `ImageTexts` registers; the pages of a scanned document arrive with the
  function that saves them (on the phone, from the scanner's `Uri`); the
  time of the moved block is Java's (it only served to tell one move from
  the next). On the PC the ViewModels' "main thread" is the window's
  (`kotlinx-coroutines-swing`): without it, the first test on the PC
  stopped right away. Test: `DesktopViewModelTest` (the editor opens a
  page and search finds it, on the PC's database)

### ✏️ v0.153.3 — 04/11/2026 · 08:54 — Page images in the common code, read on the PC too
*(Claude Code on the PC; tested on the phone; 1097 tests passed)*

- **[Project]** **`PageImageStore` is in `shared`**: icons, covers and
  block files, with the usual rules (the file name in the database, a copy
  for every duplicated page, what isn't an image doesn't stay, at most six
  megapixels in memory). What changes between phone and PC is at the
  bottom of the file: `PlatformBitmap` (Android's `Bitmap`, Skia's on the
  PC), the folder, the sizes, the photo's orientation (EXIF) and the
  shrunk reading. On the phone it is the code from before, in
  `PageImageStore.android.kt`; taking a file from the gallery
  (`saveFromGallery`, `saveFile`, with Android's `Uri`) is an extension
  there, on top of the common `saveFromStream`. On the PC images are in
  `page-images` in the data folder, and Skia reads them, turns them as a
  portrait photo should be looked at (`encodedOrigin`) and shrinks them in
  a single draw. Skia's `toMatrix` wants the sizes of the photo already
  turned: with the file's ones the photo ended up out of the frame, and
  the test found it
- **[Project]** `BlockFiles` stays in the app: it is all Android (opening a
  file with another app, saving it to the gallery, the name the phone
  gives it); the PC's will come with its screens. Test:
  `DesktopImageStoreTest` makes a red and blue photo with the EXIF label
  "turn it 90 degrees" and checks that on the PC it is measured and seen
  upright, that what isn't an image doesn't stay, and that a copy has its
  own file

### ✏️ v0.153.2 — 04/11/2026 · 08:29 — Repositories in the common code: pages and databases are read and written the same way on the PC
*(Claude Code on the PC; tested on the phone; 1094 tests passed)*

- **[Project]** **`PageRepository`, `DatabaseRepository` and
  `NotebookRepository` are in `shared`**, with what they use that doesn't
  need Android: `TextStats`, `DatabaseChanges` (automations), `PageExport`,
  the pages' history (`data/history`), `InkGeometry`, `SyncedBlocks` and
  `AppLock`, which now saves the password through `SettingsStore` like
  `AppSettings` (on the phone in the same `app_lock` file as always;
  `SettingsStore` can also remove a key)
- **[Project]** **Transactions**: the repositories use `withTransaction`
  from `com.gabriele.notionlocal.data` (`Transactions.kt`). On the phone it
  is exactly Room's for Android, as before; on the PC, where it doesn't
  exist, Room's write connection (`useWriterConnection`) with an
  `IMMEDIATE` transaction. **The reminders' alarms**: after a write the
  repositories call `ReminderAlarms.reschedule()`, and on the phone
  `NoteAlarms` answers, registering at start (before that, as before,
  nothing happens); on the PC nobody for now. One more place where Kotlin
  doesn't trust the check on a property coming from another module
  (`DuplicateSheet`). Test: `DesktopRepositoryTest` duplicates a page with
  Undo and Redo, throws it in the trash and restores it, and searches the
  text, on the PC's database

### ✏️ v0.153.1 — 04/11/2026 · 00:38 — The database in the common code: the same on the phone and on the PC
*(Claude Code on the PC; tested on the phone; 1091 tests passed)*

- **[Project]** **The database is in `shared`**: the DAOs and
  `AppDatabase` (the 14 tables, version 70), with Room writing its code
  for phone and PC (`kspAndroid` and `kspDesktop` in
  `shared/build.gradle.kts`; the app no longer has KSP).
  `@ConstructedBy(AppDatabaseConstructor::class)`: an `expect object` that
  Room completes by itself. The file is opened by `buildAppDatabase`,
  different for each: on the phone `notion_local.db` as always, with the
  65 migrations from 5 to 70 (moved from the `companion object` to
  `PhoneMigrations`, in `AppDatabase.android.kt`: they are written with
  `SupportSQLiteDatabase`, which exists only on Android); on the PC
  `notion_local.db` in the data folder, with SQLite inside the app
  (`androidx.sqlite:sqlite-bundled` 2.6.2), where it is born already at
  version 70. The schema fingerprint Room saves in the file is the same as
  before (`13d967df…`), on the phone and on the PC: the phone's data opens
  as always, and the phone's file and the PC's are the same. **Migrations
  from now on** must be written with Room's `SQLiteConnection`, which
  works for both
- **[Project]** With the DAOs `data/notify/Reminders.kt` and
  `NotificationCommand.kt` (the reminder rows two DAOs return) also moved
  into `shared`. The repositories stay in the app, for now: they use
  `withTransaction`, which exists only on Android (on the PC there is
  `useWriterConnection`). Test: `DesktopDatabaseTest` writes a page on the
  PC in a temporary folder, closes the database, reopens it and finds it
  again

### 🆕 v0.153 — 04/11/2026 · 00:20 — PageFlow starts on the PC: the code shared by phone and Windows
*(Claude Code on the PC; tested on the phone; 1090 tests passed)*

- **[New feature]** **[💻 PC]** **PageFlow's first window on the PC**: for
  now it shows the Patch Notes, the list of versions and the page of each,
  with the same code and the same colours as the phone. It can't be
  downloaded yet: it is tried from the project with `gradle :desktop:run`.
  The PC's settings are in `%APPDATA%\PageFlow` (on the Mac in
  `~/Library/Application Support/PageFlow`)
- **[Project]** **Three modules**: `shared` (Kotlin Multiplatform, the
  code shared by phone and PC), `app` (the Android app) and `desktop`
  (Compose Multiplatform 1.11.1, for Windows and Mac). In `shared`, with
  the same packages as before: the database entities (`data/entity`),
  formulas, the texts in 8 languages (except the widgets'), the theme,
  formats, `AppSettings`, buttons, the Patch Notes with their screen and
  the screens' top bar (`WorkspaceTopBar`). The database (DAOs and
  `AppDatabase`) stays in `app`, and Room reads the entities from the
  other module. Since phone and PC are both JVM, the common code uses
  `java.time` and the rest of Java as before
- **[Project]** What changes between phone and PC is in `androidMain` and
  `desktopMain` (`expect`/`actual`): `PlatformContext` (on the phone it is
  exactly Android's `Context`, so whoever passed it didn't change),
  `LocalPlatformContext`, where settings are saved (`SettingsStore`: on
  the phone the usual `SharedPreferences`, on the PC a `.properties` file)
  and where the Patch Notes are read from (the phone's assets, the PC's
  resources: the `patchNotes` task of `desktop/build.gradle.kts` copies
  them from the README like the app's). Split in two: from
  `EmojiCatalog.kt` the categories, from `ButtonRunner.kt`
  `ButtonProblem`, from `ProofingLanguage.kt` the dictionaries
  (`SpellDictionaries.kt`), from `ScreenParts.kt` the top bar. The
  `internal` declarations of the moved files became public (the app is
  another module), and in five places a value has to be put in a variable
  before using it: Kotlin doesn't trust the check made on a property
  coming from another module. Test: `DesktopScreensTest` draws the PC's
  screens in `desktop/build/desktop-screenshots`, without opening windows
  and without touching the real settings folder

### 🔧 v0.152.3.1 — 04/11/2026 · 00:01 — Kotlin 2 and Compose 1.11: the first step towards Windows and Mac
*(Claude Code on the PC; tested on the phone; 1088 tests passed)*

- **[Bug fix]** **[📱 Mobile]** **The caret no longer goes off by itself
  when the on-screen keyboard doesn't open**, for example with a real
  keyboard connected to the phone: after half a second the app thought the
  keyboard had been closed, and removed the caret. Now it removes it only
  when a keyboard that had opened closes. It will also be needed on
  Windows and Mac, where there is no on-screen keyboard
- **[Project]** **The tools updated, first step towards Windows and Mac**:
  the PC app will be made with Compose Multiplatform, which wants Kotlin 2
  and a recent Compose. Kotlin 1.9.24 → 2.4.21 (the Compose compiler is
  the `org.jetbrains.kotlin.plugin.compose` plugin, no longer
  `kotlinCompilerExtensionVersion`), KSP 2.3.13, Compose 1.6.8 → 1.11.4
  (BOM 2026.06.01; material3 1.2.1 → 1.4.0), Room 2.6.1 → 2.8.5,
  kotlinx.serialization 1.6.3 → 1.9.0, Android plugin 8.5.2 → 8.13.2 with
  Gradle 8.14.3 (8.7 is no longer enough), Robolectric 4.13 → 4.17.
  Compose 1.12 asks for Android plugin 9.1 and compileSdk 37: that will
  come later
- **[Project]** What changed in the code with Compose 1.11:
  `beyondBoundsPageCount` is called `beyondViewportPageCount`;
  `CaretInView.kt` says where the caret is with `focusProperties {
  focusRect }` (the internal Compose 1.6 hook it used is gone); fields
  copy and paste from the new clipboard (`LocalClipboard`), which
  `LinkClipboard` now implements: the paste arrives as plain text and
  `formatPaste` puts back links, styles and line types;
  `ClearFocusWhenKeyboardCloses` turns off the caret only if the keyboard
  had been seen (the bug fix above). In the tests: `enterTouchMode`
  (`TouchMode.kt`) puts Robolectric in touch mode like the phone (without
  it, with the caret removed, Android gave it back to the first field),
  and material3 1.4's bottom sheet is a dialog, no longer a popup

### ✏️ v0.152.3 — 03/11/2026 · 20:51 — "Sort groups" in the "Group" page too
*(Claude Code on the PC; tested on the phone; 1088 tests passed)*

- **[Change]** **[📱 Mobile]** **"Sort groups" in the "Group" page too** of
  a database, under the list of groupings: Manual, Ascending, Descending
  and the groups to drag with ≡, without first having to tap the
  property's name. With several groupings there is one for each, with
  the name of its property ("Sort groups · Done"). Dates have their order
  in "Date by" ("Newest first"), in their page
- **[Project]** `GroupSortSection` in `GroupRulesUi.kt`, used by the
  rule's page and by `GroupRulesPage` (which receives each rule's groups
  from `DatabaseViewScreen`). Test: `GroupRulesScreenTest` (the "Group"
  page has "Sort groups" and Descending is saved; with two rules, the
  property's name)

### ✏️ v0.152.2 — 03/11/2026 · 20:04 — The user's requests stay on their phone
*(Claude Code on the PC; tested on the phone; 1086 tests passed)*

- **[Change]** **[📱 Mobile]** The **Patch Notes** of the app published
  on GitHub no longer have the user's requests word for word: only what
  changed, version by version. The app on the user's phone still has
  them all
- **[Project]** The README on GitHub is without the user's requests: the
  quotes removed (with the sentence that introduced them), the log of the
  cloud session of 24/09/2026 and the user's sentences inside the
  entries. The full copy is `private/README.md`, which stays on the PC
  (`.gitignore`). In `app/build.gradle.kts` `-Ppersonal` makes the Patch
  Notes come from there: with it the app for the user's phone is built,
  without it the Release APK. Every new entry goes into both READMEs: with
  the request in the private one, without in the public one

### ✏️ v0.152.1 — 03/11/2026 · 18:33 — The app is called PageFlow, with the new icon
*(Claude Code on the PC; tested on the phone; 1086 tests passed)*

- **[Change]** **[📱 Mobile]** **The app is called PageFlow**: under the
  icon, in "Share", in notifications and in all the app's labels, in the
  8 languages
- **[Change]** **[📱 Mobile]** **The new icon**: the blue and green sheets
  with the line crossing them, on a dark background. It is shrunk just
  enough so that no icon shape (circle, squircle) cuts the arrow or the
  corners of the sheets
- **[Change]** **[📱 Mobile]** New backups are called
  "PageFlow-backup-…" and are in **Download/PageFlow backup**; those in
  "Download/notE backup" that the app created move there by themselves,
  with the new name, the others stay where they are. All of them are
  shown and restored as before. Saved images go to Pictures/PageFlow
- **[Note]** The internal package (`com.gabriele.notionlocal`) and the
  database name don't change: changing them the phone would see another
  app, without the data and without the updates. A desktop or web app
  doesn't exist yet: the icon for them is ready in `branding/` (1024, 512,
  192 and 180 px and the favicons)
- **[Project]** The repository on GitHub is called **PageFlow**
  (`AbernathyXVII/PageFlow`; old links lead there). `app_name`, the labels
  of `BackupStrings`, `BlockStrings`, `HistoryStrings`, `NotifyStrings`,
  `ShareStrings`; `Backups.FOLDER` and `OLD_FOLDERS`, `BACKUP_PREFIX` and
  `OLD_BACKUP_PREFIX`, `BackupArchive.MANIFEST` and `OLD_MANIFEST` (the
  older backups are read too); the mark of lines copied to the clipboard
  (the old one too); `ic_launcher_foreground.png` redone from the image;
  `rootProject.name`. Test: `BackupTest` (a backup from when the app was
  called notE is shown, moved and restored)

### 🆕 v0.152 — 03/11/2026 · 14:14 — Recording audio, scanning documents and searching the text of photos
*(Claude Code on the PC; tested on the phone: a scan and a word found inside a photo; recording up to the button; 1085 tests passed)*

- **[New feature]** **[📱 Mobile]** **"Record audio"** in the "/" and "+"
  menus (and "Record audio" under "Choose file" in an empty audio block):
  the block starts recording right away, asking for the microphone the
  first time. While recording it shows the red dot, the time and how loud
  the sound is; **Stop** and the recording becomes the block's file, to
  listen to there; the **cross** throws it away
- **[New feature]** **[📱 Mobile]** **Recording goes on with the screen
  off** or leaving the page or the app: there is the "Recording audio"
  notification with the running time and **Stop**. Stopped from there, the
  file still goes into its block (or, if it disappeared in the meantime,
  into a new audio block at the bottom of the page). One recording at a
  time
- **[New feature]** **[📱 Mobile]** **"Scan document"** in the "/" and "+"
  menus: Google's scanner — it finds the sheet's edges by itself,
  straightens it, has filters and does several pages in a row (up to 50),
  even from photos already taken. The pages become photos in the page, in
  place of the line if it is empty, otherwise below it; Undo removes them
  in one step
- **[New feature]** **[📱 Mobile]** **Search finds the text of photos**:
  what is written in the images of the pages (scans, receipts, photos of
  the whiteboard…) is read by itself, in the background, one photo at a
  time: new ones right away, those already there after starting. In the
  results the piece found starts with 🖼. Latin, Chinese, Japanese and
  Korean characters are read
- **[Note]** The scanner and the text reader are Google's (ML Kit) and live
  in Google Play Services, which downloads the models the first time: the
  app stays light, and photos don't leave the phone. The Chinese, Japanese
  and Korean models arrive a few seconds after installing: a photo read
  before is read again at the next start. A photo is read only once;
  deleting its page forever, its text goes away with it. Photos arrived
  with "Share" are read right away too
- **[Project]** Database at version 70 (`image_texts`, migration 69→70);
  `ImageTextEntity`, `ImageTextDao`; `data/ocr/ImageTexts.kt` (`TextReader`,
  the ML Kit reader, `readPending`); search in `PageRepository.search`;
  `data/record/AudioRecorder.kt` (`AudioRecorder` and `RecordingService`, a
  foreground service with the microphone); `PageRepository.putRecording`
  and `insertScannedPages`; `CaptureUi.kt` (`RecordingBar`,
  `rememberRecordStarter`, `rememberDocumentScanner`); `SlashAction.Record`
  and `Scan`; `MediaRequestKind.RECORD`; `CaptureStrings`; the
  `RECORD_AUDIO`, `FOREGROUND_SERVICE` and `FOREGROUND_SERVICE_MICROPHONE`
  permissions; the ML Kit libraries. Tests: `CaptureTest` (where a
  recording goes, the scanned pages, a photo read once and found by
  search, one that can't be read yet, the text that goes away with the
  page, the time), `MigrationTest` from 69

### 🆕 v0.151 — 03/11/2026 · 13:49 — Synced blocks: the same lines in several pages, changing together
*(Claude Code on the PC; tested on the phone with two copies on a test page; 1078 tests passed)*

- **[New feature]** **[📱 Mobile]** **"Synced block"** in the "/" and "+"
  menus and in "Turn into": the line becomes a synced block, an orange
  frame with its lines inside (what was written becomes the first), and the
  caret goes inside. Inside you write as in a toggle: text, lists,
  checkboxes, tables, images
- **[New feature]** **[📱 Mobile]** **"Sync to another page"** in the
  block menu (tapping the "Synced block" label at the top, or
  long-pressing): you choose a page and a copy goes at its bottom. At the
  top of the frame you read in how many pages it is ("Synced block · 2
  pages")
- **[New feature]** **[📱 Mobile]** **Write in one copy and they all
  change**: a letter, a new line, a ticked checkbox, a table cell. No copy
  is the original: you can write in any of the pages, even in two copies
  in the same page
- **[New feature]** **[📱 Mobile]** **"Unsync"**: this copy goes back to
  normal lines, in its place; the others stay synced with each other.
  Deleting a copy doesn't touch the others
- **[Note]** A subpage or a database created inside the block lives in the
  page where it was born; in the other copies it is a "Link to page" to
  it. The copies use the same files (images): deleting a page forever, a
  file still used by another copy stays. A synced block doesn't go inside
  another. PDF and Markdown export and the page's history show the block's
  lines like the others
- **[Project]** `BlockType.SYNCED` with `BlockAttrs.syncId` (no migration:
  it is in the JSON of the extra entries); `data/sync/SyncedBlocks.kt`
  (`SyncedLine`, the content's signature, the pause and the expected
  rewrites, and `SyncedWatcher`, which sees what changes in the open
  page); in `PageRepository` `makeSynced`, `addSyncedCopy`, `pushSynced`
  (in place if the shape is the same, otherwise from scratch), `unsync`,
  `syncedPageCount`, and the shared files in `deletePermanently`;
  `SyncedBlockUi.kt` (the frame); `SlashAction.Synced`; the menu in
  `BlockActionsSheet`; `SyncStrings`. Tests: `SyncedBlocksTest` (the first
  line, the copy, a line rewritten in place, a new line and the tables,
  the subpage that stays home, "Unsync", the files), `SyncedBlockTest`
  (the caret inside, a change that reaches the other page, two copies in
  the same page that don't bounce off each other)

### ✏️ v0.150.1 — 03/11/2026 · 13:22 — No greyed-out "Turn into" and "Color" in a database's menu
*(Claude Code on the PC; tested on the phone; 1067 tests passed)*

- **[Removed]** **[📱 Mobile]** In the menu of a database inside a page
  (the one opened from the block) there are no more **"Turn into"** and
  **"Color"**: they were both greyed out, with nothing to do. For the link
  to a page they stay as before
- **[Project]** In `BlockActionsSheet.kt` (`PageBlockMenu`) the group is
  shown only if it isn't a database

### 🆕 v0.150 — 03/11/2026 · 13:09 — Database automations: when a property changes, when a page arrives, on a schedule
*(Claude Code on the PC; tested on the phone on a test database; 1067 tests passed)*

- **[New feature]** **[📱 Mobile]** **"Automations"** in the three dots of
  a full-screen database: the list of its automations, each with its
  "when" in words ("Done becomes Checked", "Every week (Monday) at
  09:00"), how many actions it does, the last time it ran and the switch
  to turn it on and off. "New automation" makes one
- **[New feature]** **[📱 Mobile]** **The "when"**: **Property edited** —
  a chosen property that changes, or that becomes a value (Checked or
  Unchecked for a checkbox, one or more choices for a Select); **Page
  added** — a new page in the database, however born; or **every day**,
  **every week** on the chosen days, **every month** on a day (the last
  one, in shorter months), at the time chosen with the wheel calendar
- **[New feature]** **[📱 Mobile]** **The actions** are the buttons' ones,
  with the same window: "Edit pages in" (also **This page**, the page that
  triggered the automation: "when I tick Done, set today's date"), "Add
  page to" (by default in this database), "Insert blocks" and "Send
  notification". No confirmations or pages to open: an automation works on
  its own
- **[New feature]** **[📱 Mobile]** **"Add page to" with the template**, in
  buttons too: the new page is born with the content of the chosen
  template. And in the name **{date}** becomes today's date and **{week}**
  the week number: "Week {week}" every Monday becomes "Week 45", "Week
  46"…
- **[Note]** A text cell is saved at every letter: the automation starts a
  second after the last one, and with a condition only if it still holds
  (ticked and immediately unticked, nothing). Changes made by an
  automation don't trigger others, as in Notion, so they don't loop
  forever. The scheduled ones start with the notifications' alarm, even
  with the app closed; if the phone was off, at the first chance, only
  once
- **[Project]** `AutomationEntity` (`automations` table, migration 68→69)
  with `AutomationConfig` and `AutomationTrigger` (`PageAdded`,
  `PropertyEdited`, `Schedule`); `AutomationDao`; `data/automations`:
  `DatabaseChanges` (notified by `DatabaseRepository.addRow` and
  `setCellValue`, silent inside `AutomationRun`) and `Automations` (the
  engine, which uses `ButtonRunner` with a windowless `ButtonHost`; the
  scheduled automations in `NoteAlarms`' alarm);
  `ButtonStep.AddPage.templateId` and the name placeholders in
  `ButtonRunner`; `ButtonEditorContent` with optional header, actions and
  title; `AutomationsUi.kt`; `AutomationStrings`. Tests: `AutomationsTest`
  (days and times, when a change triggers it, "tick Done → today's date",
  the new page and two automations that don't chase each other, the week's
  page from the template only once), `AutomationsScreenTest`,
  `MigrationTest` from 68

### ✏️ v0.149.2 — 03/11/2026 · 09:08 — "Startup window" in Settings, above "Title position"
*(Claude Code on the PC; tested on the phone; 1060 tests passed)*

- **[Change]** **[📱 Mobile]** **"Startup window"** is no longer in the
  sidebar: it is in **Settings**, right above "Title position", with the
  current choice next to it ("Last visited page", "Main menu" or "Specific
  page"). Tapping it opens its usual screen, and back returns to Settings
- **[Project]** Removed from `Sidebar.kt` (and `SidebarActions.onStartup`);
  in `SettingsScreen` the entry and `onOpenStartup`, which `AppNavHost`
  connects to the `Routes.STARTUP` screen

### ✏️ v0.149.1 — 03/11/2026 · 09:01 — No more greyed-out "Import" and "Export" in the sidebar
*(Claude Code on the PC; tested on the phone; 1060 tests passed)*

- **[Removed]** **[📱 Mobile]** The sidebar's **"Import"** and **"Export"**
  entries, greyed out and without a function: export is done from the
  three dots of a page or a database (see v0.147 and v0.149)
- **[Project]** Removed from `Sidebar.kt`, with their icons

### 🆕 v0.149 — 03/11/2026 · 08:49 — Complete export: databases like Notion, buttons with their actions, drawings optional
*(Claude Code on the PC; tested on the phone on a test page and on a database; 1060 tests passed)*

- **[New feature]** **[📱 Mobile]** **Databases in the export**, as their
  view shows them: the visible columns (Name in its place), the rows
  filtered and sorted, and the values written as in the cells — dates
  with the Settings format, numbers, ✓ for checkboxes, the names of the
  rows linked by Relations, the results of formulas and Rollups. In the
  **PDF** a table, with the columns as wide as what is written; beyond
  five columns it wouldn't fit, and each row becomes a card with the name
  and the properties below. In **Markdown** a table and, next to it, the
  database's **CSV** with all the columns, the hidden ones too, as Notion
  does
- **[New feature]** **[📱 Mobile]** **Row pages** that have something
  written (or an image, a file, another database…) become files of their
  own: in the PDF a .zip with "Page.pdf" and the folder
  "Page/Database/Row.pdf" — and the 📄 in front of the row's name in the
  table; in Markdown "Page/Database/Row.md", which the row's name links
  to. The databases inside those pages too, and so on
- **[New feature]** **[📱 Mobile]** **Buttons** are exported with their
  name and "When you tap it:", then the numbered actions with the words of
  the window where they are chosen: "Ask for confirmation: «…»", "Insert
  blocks (below the button): Checkbox «Drink water»", "Add page to
  «Tasks»: «Diary» — Status: Add «Home, Urgent»", "Edit pages in «Tasks» —
  only the pages where Done is «Unchecked»: …", "Open page or link", "Send
  a notification". A linked copy states the real button's actions
- **[New feature]** **[📱 Mobile]** **"Include drawings"** in the "Export
  page" window, for the PDF and only if the page has hand drawings: the
  strokes come out as in the app (pen, pencil, marker, highlighter, with
  their colours and the S Pen's pressure). So that they stay in their
  place the page keeps the phone's proportions, on A5 sheets: text and
  lines as tall as in the editor. A stroke that goes past the sheet
  continues at the top of the next sheet
- **[New feature]** **[📱 Mobile]** **"Export" for a full-screen database
  too**, from its three dots: its table and the row pages
- **[Change]** **[📱 Mobile]** **"Print"** always prints a single PDF:
  without the rows' PDFs, which would make a .zip
- **[Note]** Drawings in the PDF fall where they were made relative to
  the line, the group of lines or the block they were drawn on; over an
  image, a table or a database whose height in the PDF differs from the
  app they can move a little. In Markdown there are no drawings
- **[Project]** `data/export`: `DatabaseExport.kt` (`DatabaseReader`,
  which reads the database with the screen's ViewModel and waits for it to
  finish calculating; `databaseRead`, `exportCellText`, `csvOf`),
  `ButtonExport.kt` (`exportButton`, `buttonReferences`), `PdfLayout` (A4
  document or the phone's proportions) and the drawings in `PdfExport`,
  `ExportOptions` and the enrichment of pages in `PageExporter` (databases,
  buttons, strokes, the rows' PDFs in the .zip); in `PageRepository`
  `viewsOf`, `strokesOf`, `columnsOf`, `blockOf`; "Include drawings" in
  `PageExportSheet`; "Export" in `DatabaseViewScreen`. Tests:
  `ExportExtrasTest` (database and button in Markdown, the CSV, the button
  in words, the database's view with hidden columns and Relations, the red
  stroke in the PDF where it was made, the cards of a wide database, the
  .zip with the CSV and only the row pages with something) and
  `PageExportScreenTest` ("Include drawings" only for the PDF)

### 🆕 v0.148 — 03/11/2026 · 08:19 — "@" mentions of pages and dates, and backlinks at the bottom of the page
*(Claude Code on the PC; tested on the phone on a test page; 1052 tests passed)*

- **[New feature]** **[📱 Mobile]** **"@" in the text**: typing the at
  sign at the start of a line or after a space (not in an email address),
  a menu opens with the **pages** — the most recently touched, and then
  those with a name resembling what is typed after the at sign, spaces
  included — and the **dates**: today, tomorrow, yesterday and "Pick a
  date…" with the calendar. Once an entry is chosen, in place of "@…"
  stays the page's title (or the date written in full, "3 November 2026")
  with its link, and a space to keep writing
- **[New feature]** **[📱 Mobile]** **Tapping a mention**: a page's one
  opens it (a database in its view); a date's one opens the calendar to
  change it. A page mentioned and then renamed shows with the new name as
  soon as the page citing it is reopened
- **[New feature]** **[📱 Mobile]** **Backlinks at the bottom of the
  page**: under the last line, the **"Backlinks"** section with how many
  and which pages reference this one — with an "@" mention or with a
  "Link to page" — and for each the line where it does. A tap opens the
  page; the section's title closes and reopens it. It isn't there when no
  page references it, and it updates when coming back to the page
- **[Change]** **[📱 Mobile]** In the Markdown export mentions and links to
  the page's lines stay plain text: outside the app they wouldn't lead
  anywhere
- **[Note]** The link of a subpage in the page that contains it doesn't
  count as a backlink: it is where it is, not a reference. Pages in the
  trash appear neither in the menu nor among the backlinks. The "@" menu
  is in text lines, lists and checkboxes, like the "/" menu
- **[Project]** `data/entity/PageMentions.kt` (the `note://page/…` and
  `note://date/…?m=…` links, `replaceLinkedText`, `refreshPageMentions`);
  in `PageRepository` `backlinksOf`, `mentionCandidates` and
  `refreshMentionTitles`; `BlockDao.getBlocksMentioning` and
  `PageDao.recentlyUpdated`; `ui/screen/Mentions.kt` (`MentionMenu`,
  `BacklinksSection`, `LocalPageMentionOpen`, `LocalDateMentionTap`); the
  "@" menu in the editor's text field next to the "/" one; the labels in
  `MentionStrings`. Tests: `MentionsTest` (the links, titles brought up to
  date, when the menu opens, who references whom, search) and
  `MentionScreenTest` ("@Hot" and the page chosen from the menu; the
  backlinks at the bottom of the cited page, which open it)

### 🆕 v0.147 — 02/11/2026 · 23:40 — "Export": the page as PDF or Markdown, to send, save or print
*(Claude Code on the PC; tested on the phone on a test page; 1045 tests passed)*

- **[New feature]** **[📱 Mobile]** **"Export" in a page's "..." menu** is
  no longer greyed out: it opens the **"Export page"** window, where you
  choose the format (**PDF** or **Markdown**) and what to do with it:
  **Share** sends it with another app (WhatsApp, mail, Drive…), **Save**
  saves it in a folder of the phone (or on Drive) with the system window,
  **Print** (PDF only) opens the system printing, which can also save it
  as a PDF
- **[New feature]** **[📱 Mobile]** **The PDF**: A4 sheets with the page's
  title and its icon, real text (it can be selected and searched) with
  bold, italic, underline, strikethrough, subscript, superscript, sizes
  and colours, lists with the bullet or the number, checkboxes (the ticked
  ones struck through, as in the app), quotes, callouts and code in their
  box, the lines, tables, images as wide as in the page, files, videos and
  bookmarks with their name and address, and the page number at the
  bottom ("2 / 5"). Colours meant for the dark theme that wouldn't be
  readable on the white of paper are darkened or lightened
- **[New feature]** **[📱 Mobile]** **Markdown**: plain text with the
  marks read by Obsidian, GitHub and the other note apps (`**`, `-`,
  `- [ ]`, `>`, tables, code blocks with the language), so as not to stay
  locked inside the app. Lines written large (like titles taken from the
  web) become headings. A page with images or files becomes a **.zip**, as
  Notion does: "Page.md" and next to it the "Page" folder with the files,
  which the text links to
- **[New feature]** **[📱 Mobile]** **"Include subpages"**, if the page
  has subpages: in the PDF each starts on a new sheet after the page; in
  Markdown they go into the .zip, in the page's folder, and the link to
  the subpage opens them. Only child pages: a link to a page that lives
  elsewhere stays its title
- **[Note]** Not exported (for now): the hand drawings over the page,
  buttons and the content of databases: of a database the name remains.
  The fonts chosen for the text become the system one in the PDF. The
  full-screen database still has "Export" greyed out, like "Import" and
  the two sidebar entries
- **[Project]** `data/export`: `PageExport` (the page and its lines in
  reading order, `exportFileName`), `MarkdownExport`, `PdfExport` (lays out
  in two passes and writes with `PdfDocument`), `PageExporter` (the file in
  the cache, the .zip, `share`, `copyTo`, `print` with a
  `PrintDocumentAdapter`) and `SaveExport`; `PageRepository.exportPage`;
  the `PageExportSheet` window; `res/xml/shared_files.xml` also passes the
  "exports" folder to other apps; the labels in `ExportStrings`. Tests:
  `PageExportTest` (a whole page in Markdown, marks one inside the other,
  file names, the PDF's sheets drawn on an image, the .zip with the files
  and only the real subpages) and `PageExportScreenTest`

### 🆕 v0.146 — 02/11/2026 · 22:58 — "Share with notE" and the quick note: quick setting tile, widget and shortcut
*(Claude Code on the PC; tested on the phone on a test page; 1038 tests passed)*

- **[New feature]** **[📱 Mobile]** **"notE" in the Share menu** of other
  apps (Chrome, the Gallery, WhatsApp…): over the app you share from rises
  the **"Save to notE"** window, with the title to change (the web page's,
  or the message's first line), what is arriving (the link with its site,
  the first lines of the text, how many images and files), **"Save to"**
  with the page it goes to, and whether it becomes **a new page** (the
  link at the bottom of that page) or goes **at the bottom of the page**.
  **Save** saves and you go back to the previous app; **Save and open**
  opens notE on the page
- **[New feature]** **[📱 Mobile]** **What it becomes**: text becomes text
  lines; a link a bookmark, which the first time the page opens takes the
  site's title, description and image; photos images; video and audio
  their blocks; the rest file blocks. One or many things together
- **[New feature]** **[📱 Mobile]** **The quick note**, from three places:
  the **"Quick note" tile in the quick settings** (added from the pencil
  of the notification shade), the **"Quick note" widget** for the Home
  screen and the **shortcut** that appears long-pressing notE's icon. You
  enter a new page with the caret already on the first line. Left empty it
  goes away by itself; written without a title, it takes its first line as
  the title
- **[New feature]** **[📱 Mobile]** **Where they end up**: in the page
  chosen in Settings → **"Shared items and quick notes"** or while
  sharing; until one is chosen, in **Inbox** (📥), which is born at the
  bottom of the main menu the first time it is needed
- **[Note]** Images and files are copied into the app when saving, like
  those chosen from the phone. Password-locked pages don't open: only the
  titles show, as in the sidebar. The labels of the tile, the widget and
  the shortcut follow the phone's language
- **[Project]** `ShareActivity` (transparent, with the SEND and
  SEND_MULTIPLE filters) and `ShareSheet`; `Incoming` and `splitSharedText`
  to read what arrives; `QuickNotes`, `QuickNoteTile` (TileService),
  `QuickNoteWidget` and `res/xml/shortcuts.xml`; in `PageRepository`
  `inboxPageId`, `saveIncoming`, `createQuickNote` and `finishQuickNote`;
  `AppSettings.sharePageId`; the labels in `ShareStrings`. Tests:
  `IncomingTest`, `ShareRepositoryTest`, `ShareScreenTest` (a link from
  Chrome in the Inbox, a message at the bottom of the chosen page, the
  changed title), `QuickNoteScreenTest` (the quick note that, left empty,
  goes away)

### 🆕 v0.145 — 02/11/2026 · 16:59 — "Page history": a page's versions, to reread and restore
*(Claude Code on the PC; tested on the phone on a test page; 1022 tests passed)*

- **[New feature]** **[📱 Mobile]** **"Page history"** in a page's three
  dots, under "Updates": the page's saved versions, divided by day (Today,
  Yesterday, then the date), with the time, the title back then and how
  many lines and words it had. Tapping one you read how the page was
  (title, lines indented as in the page, checkboxes, numbered lists,
  toggles, tables, names of images, files and subpages). **"Restore this
  version"**, after the question, puts it back: title, text, tables and
  drawings come back
- **[New feature]** **[📱 Mobile]** **When a version is born**: opening
  the page, every 10 minutes while it is open, leaving the page and when
  the app goes to the background, but only if something changed (opening
  or closing a toggle doesn't count). **How many are kept**: all those of
  the last 24 hours; further back one per day (the last of that day) for
  30 days; a page's last version always stays
- **[Note]** Restoring can be undone: before putting a version back, how
  the page is at that moment ends up in the history, marked "Before
  restoring". The icon, the cover and what is inside databases and
  subpages don't come back (they are pages of their own); subpages and
  databases born after the version aren't lost: their link stays, at the
  bottom of the page. Links to pages in the trash or deleted don't come
  back. With the page locked versions can be read but not restored.
  Full-page databases have no versions (their rows aren't blocks); row
  pages do. Versions are in the app's database, so in backups too
- **[Project]** Database at version 68: `page_versions` table
  (`PageVersionEntity`, `PageVersionDao`, migration 67→68), the content in
  compressed JSON with its fingerprint (`VersionContent`, `VersionCodec`),
  the rules on how many to keep (`VersionRetention`). `saveVersion` and
  `restoreVersion` in `PageRepository`; the saves and the restore in
  `PageEditorViewModel`; `InkViewModel.reload` for restored drawings;
  `PageHistorySheet` and `versionOutline` in `PageHistoryUi.kt`; the labels
  in `HistoryStrings`. Tests: `VersionContentTest` (no block field is left
  out, the fingerprint, how many versions are kept), `PageVersionsTest` (a
  version only if it changes, restoring toggles, tables, drawings and
  title, "Before restoring", subpages born after), `PageHistoryScreenTest`
  (the first version opening the page, the preview and the restore, the
  locked page, the preview of the lines), `MigrationTest` (from 67)

### 🆕 v0.144 — 02/11/2026 · 16:24 — "Sort" in a property's menu sorts only by it: A → Z, or from largest to smallest
*(Claude Code on the PC; tested on the phone on a test page; 1006 tests passed)*

- **[Change]** **[📱 Mobile]** **"Sort" in a property's menu** (it opens
  tapping its header, Name's too) no longer opens the window with all the
  rules: it opens right below, with two choices for that property. Words
  **A → Z** and **Z → A**; numbers **Largest → smallest** (first) and
  **Smallest → largest**. A text or tag property where every value starts
  with a digit ("2017", "3 km") counts as numbers. Dates have **Newest
  first** and **Oldest first**, checkboxes **Checked first** and
  **Unchecked first**. Once a direction is chosen, the table is sorted and
  the menu closes; reopening, the tick says the current direction, and
  **Remove sort** removes that property's sorting
- **[Note]** The chosen property becomes the first rule: the rules that
  were there stay below (they count only among equal rows) and are all
  shown in the window of the "Sort" icon at the top of the database, which
  stays as it was. With "Lock view" the entry is greyed out, like the icon
- **[Project]** `SortKind` and `sortKindOfTexts` in `SortRules.kt`;
  `sortFirstBy` and `sortKindOf` in `DatabaseViewModel` (formulas and
  Rollups by what they give); `propertySortRows` and `sortChoices` in
  `PropertyMenu.kt`, instead of `PropertyMenuActions.sort`. Tests:
  `PropertySortTest` (the two directions in the menu, words, numbers from
  the largest, a text of numbers only, Name, checkboxes and dates, the
  other rules that stay below and "Remove sort"); `PropertyMenuTest`
  updated

### 🆕 v0.143 — 02/11/2026 · 16:05 — "Sort groups": groups in the order you want, even Unchecked above Checked
*(Claude Code on the PC; tested on the phone on a test page: in a test database grouped by a "Done" checkbox, by default Checked above Unchecked; in the rule "Sort groups" with Manual, Ascending and Descending and the two groups with the three lines; Unchecked dragged above Checked, and in the table Unchecked went up; then the test database to the trash; 999 tests passed)*

- **[New feature]** **[📱 Mobile]** **"Sort groups" in every "Group" rule**
  (and the board's "Group by"): tapping the rule, under "Hide empty
  groups", three choices. **Manual**: the list of groups with the three
  lines, to drag in the order you want (for a checkbox, Unchecked above
  Checked); **Ascending** and **Descending**: by name, numbers as numbers.
  It applies to that view and that level: the table, the board's columns
  and bands each have their own
- **[Note]** Without ever dragging them, groups stay in the usual order
  (tags as in their menu, Checked first, texts and numbers from the
  smallest). A new group (a tag added later) goes at the bottom of those
  placed by hand; the one without a value always stays last. Dates already
  have "Newest first" in "Date by". Changing a rule's property, the order
  starts over. The "previous page" and "next page" arrows follow the
  chosen order too
- **[Project]** `GroupRuleSpec` with `order` (`GroupOrder`: MANUAL,
  ASCENDING, DESCENDING) and `manual` (the groups' keys); `orderGroups` and
  `buildLevelGroups` next to `buildRowGroups`; `setGroupRuleOrder` and
  `setGroupRuleManual` in `DatabaseViewModel`; `ReorderList` (the tags'
  three-line list, now for any list). Tests: `GroupOrderTest` (the
  checkbox by hand and reversed, tags, numbers, new ones at the bottom,
  "Hide empty" after the order, the pages' arrows), `SubGroupTest` (saved,
  and from scratch with another property), `GroupRulesScreenTest` (the
  rule's page, and Unchecked going above Checked)

### 🔧 v0.142.0.1 — 28/10/2026 · 14:38 — Typing fast in an empty cell no longer creates a double cell
*(Claude Code on the PC; tested on the phone on a test page: in a test database "Food" typed very fast in two empty cells, then the table grouped by that property: a single group, "Food" with 2 rows (with v0.142 they were "Fo" and "Foo"); the app opens with the user's data after the migration; then the test database to the trash; 991 tests passed)*

Testing v0.142 on the phone, in the test database "Food" typed very fast
in two empty cells had been saved as "Fo" and "Foo", even though the
screen read "Food": it showed when the groups redrew the table. In the
user's databases (counts only) there was no double cell.

- **[Bug fix]** **[📱 Mobile]** **A single cell per row and property**:
  every letter typed saves the cell, and two close saves on a still empty
  cell both found it empty and each created one; the following letters
  ended up in one, and the table showed the other, left behind. Now the
  save reads and writes in one go, and the database no longer accepts two
  cells for the same row and the same property
- **[Note]** Any double cells go away by themselves at opening: the first
  stays, the one the changes ended up on
- **[Project]** `DatabaseRepository.setCellValue` and `setCellLinkLabel` in
  a transaction; `DatabaseCellEntity` with the unique index on (`rowId`,
  `columnId`); migration 66→67 (the doubles removed, then the index).
  Tests: `CellWriteTest` (letters saved one after the other without
  waiting, thirty saves from several threads, the second cell taking the
  place of the first, the link label), `MigrationTest` (from 66, with a
  double cell)

### 🆕 v0.142 — 28/10/2026 · 14:16 — As many groups as you want: "Add a group" inside Group, and no more "Sub groups"
*(Claude Code on the PC; tested on the phone: a board grouped in columns and weekly bands stayed as it was and its "Group by" shows the two rules; on a test page a test database with Type, Bought and Bio: "Add a group" three times, the table with three levels each further right, "Add a group" greyed out when the properties ran out; in the board the columns by Type and the bands by Bought; then the test database to the trash; 986 tests passed)*

- **[New feature]** **[📱 Mobile]** **The table's groups, as many as you
  want**: inside "Group" there is "Add a group" at the top and below the
  rules, numbered in the order they count: the first makes the groups, the
  second divides each group, the third each subgroup, and so on. In the
  table each level is a bit further right than its group and closes by
  tapping it; "New page" inside a group creates the page with the values
  of all the levels, and "Create sub row" copies them all
- **[New feature]** **[📱 Mobile]** **The board too** ("Group by"): the
  first rule makes the columns ("Columns"), the others the bands
  ("Bands"), one inside the other, each with its name fixed on the left
  edge
- **[Change]** **[📱 Mobile]** **Every rule has its own page**, tapping it:
  the property (choosing another rule's, the two swap), "Hide empty
  groups", for a date "Date by", and "Remove group". The ⋮ menu next to it
  moves the rule up or down or removes it; at the bottom "Remove all
  groups"
- **[Removed]** **[📱 Mobile]** **"Sub groups" and "Hide empty sub
  groups"** from the database settings, in the table and the board: the
  groups inside groups are the rules after the first. "Hide empty groups"
  is no longer on the first page either: every rule has its own
- **[Note]** The existing groupings stay as they were: each view's group
  and subgroup became its first two rules, with their "Hide empty" and
  "Date by"; a board with bands but without a property chosen for the
  columns has as its first rule the first single-select one, the one
  already shown
- **[Project]** `data/entity/GroupRules.kt` (`GroupRuleSpec`, `GroupLevel`,
  `levelsIn`); migration 65→66 (`pages.tableGroupRulesJson` and
  `boardGroupRulesJson`, with the group and subgroup fields moved there and
  emptied); in `DatabaseViewModel` `tableGroupLevels`, `boardGroupLevels`,
  `addGroupRule`, `moveGroupRule`, `setGroupRuleColumn`, `addRowInGroups`…;
  `TableLayout` and `BoardLayout` draw one level inside the other;
  `ui/screen/GroupRulesUi.kt` (the list, "Add a group", the rule's page);
  `RowNavigation` follows all the levels. Tests: `SubGroupTest` and
  `BoardSubGroupTest` rewritten for the rules, `GroupRulesScreenTest` (the
  "Group" page, three levels in the table, the rule's page, the board),
  `BoardLayoutTest` (bands inside bands), `RowOrderTest` (three levels),
  `MigrationTest` (from 65)

### 🆕 v0.141 — 28/10/2026 · 10:51 — Formulas in databases, like Notion's Formula property
*(Claude Code on the PC; tested on the phone on a test page: in a test database the "Total" property chosen in "New property" and written with "Edit formula" by tapping properties and operators (`prop("Price") * prop("Qty")` gives 6 with 1.5 × 4); tapping the cell, the window with the preview on that row; the "Days left" example (3 days to 31/10), the error "The formula stops halfway", an "if… then…" (`… > 5 ? "pricey" : "ok"` gives "pricey"), a yes/no that in the cell is a ticked checkbox; the header menu with "Edit formula" and without "Filter"; "Price" renamed "Cost" and the formula rewritten by itself; with the field empty the keyboard stays lowercase; then the test database to the trash; 976 tests passed)*

- **[New feature]** **[📱 Mobile]** **The "Formula" property** (Σ icon): a
  value calculated row by row from the other properties, which updates by
  itself when they change. It is written as in Notion:
  `prop("Price") * prop("Quantity")`, `dateBetween(prop("Due"), today(),
  "days")`, `if(prop("Done"), "✓ Finished", "To do")`. It is chosen among
  the types in "Change type" or when creating a property; under the type
  there is "Edit formula"
- **[New feature]** **[📱 Mobile]** **The formula window**: it opens
  tapping a formula cell, or from "Edit formula" in the header menu. At the
  top the formula; below, while typing, the result on that row, or what is
  wrong ("Missing “)”", "There's no property “Boh”"…). Further down, to
  tap: the database's properties (they write `prop("…")` where the caret
  is), the operators, three examples already with the database's
  properties and the functions, each with what it does. Closing it, the
  formula is saved
- **[New feature]** **[📱 Mobile]** **The language**: `+ - * / % ^`,
  `== != > < >= <=` (also `× ÷ ≥ ≤ ≠` and a single `=`), `and or not` (or
  `&& || !`), `condition ? yes : no`, texts in quotes (also the keyboard's
  curly ones) and about sixty functions: logic (`if`, `ifs`, `empty`…),
  numbers (`round`, `sum`, `min`, `max`…), text (`concat`, `contains`,
  `replaceAll`, `slice`…) and dates (`now`, `today`, `dateAdd`,
  `dateBetween`, `formatDate`, `year`, `day`…). Date units also in Italian
  ("giorni", "mesi"); in `formatDate` the text between `[ ]` stays as it is
- **[New feature]** **[📱 Mobile]** **What it gives**: a number, a text, a
  date or a yes/no, which in the cell is a ticked or empty checkbox. A
  wrong formula writes "Error" in its cell and doesn't stop the others. A
  formula also reads another formula, a Rollup, a Relation's names, the
  creation date…; two formulas reading each other say so instead of going
  round forever
- **[New feature]** **[📱 Mobile]** **Sorting, searching, calculating**: a
  formula is sorted by its number or its date (not as text: 6 comes before
  32), is found by search, at the bottom of the column also has Sum,
  Average, Min, Max and the dates, and makes the charts' groups. It also
  shows in the row's page, in the cards and in the gallery. It can't be
  filtered and doesn't group the table, like Rollups
- **[New feature]** **[📱 Mobile]** **Renaming a property**, the formulas
  that read it are rewritten with the new name
- **[Project]** `data/formula/FormulaSyntax.kt` (how a formula is read, and
  the errors), `FormulaEngine.kt` (the calculation, the functions, each
  row's properties, `renameProperty`); `ColumnType.FORMULA`; in
  `DatabaseViewModel` formulas are calculated after Rollups
  (`DatabaseTableState.formulas`, `previewFormula`, `setFormula`);
  `ui/screen/FormulaUi.kt` (the cell, the window); `FormulaStrings` in the
  eight languages; `RollupFormatter.dateTime`. Tests: `FormulaEngineTest`
  (the user's examples, operators, functions, dates, properties, errors,
  formulas reading each other, renaming), `FormulaViewModelTest` (with the
  real database: the results, the order, search, renaming, the preview),
  `FormulaScreenTest` (the cell, the window with the preview, the header
  menu, "Change type")

### 🔧 v0.140.0.1 — 28/10/2026 · 09:54 — The restore question writes the backup's date
*(Claude Code on the PC; tested on the phone: the question now says «replaced by the backup of 28.10.2026 09:53», then cancelled; 939 tests passed)*

Testing it on the phone (a backup made, restored, and the backup after it
identical to the restored one: the 13 tables with all their rows, the
pages' files and the preferences), the question before the restore used
the label of the backups list.

- **[Bug fix]** **[📱 Mobile]** **"Restore this backup?" says the
  backup's real date**: «replaced by the backup of 28/10/2026 09:51»,
  instead of «replaced by the backup of Today at 09:51», which doesn't
  read well (in Italian even less). The backups list keeps "Today at…" and
  "Yesterday at…"
- **[Project]** In `BackupScreen.kt` the question takes `Formats.dateTime`.
  Test: `BackupScreenTest` (+1 check: the date in the question)

### 🆕 v0.140 — 28/10/2026 · 08:31 — Daily backup, "Upload to Proton Drive" and restore
*(Claude Code on the PC; tested on the phone: "Back up now" makes in 6 seconds a 27 MB backup in Download/notE backup, with inside the healthy database (version 65), the pages' files and the preferences; "Upload to Proton Drive" copied it into Proton Drive/Documents/notE backup, identical byte for byte to the original, and the next time "Save as" already opens there; restore tested only with the tests; 939 tests passed)*

- **[New feature]** **[📱 Mobile]** **"Backup" in the sidebar** (before it
  was greyed out): at the top the last backup and the last upload to
  Proton Drive, "Upload to Proton Drive" and "Back up now"; below the
  daily backup, the reminder, the backups on the phone and "Restore from a
  file…"
- **[New feature]** **[📱 Mobile]** **The daily backup**: once a day,
  leaving the app, if there isn't one yet today. It is a zip file with all
  of notE: the database (copied whole in an instant, even while the app is
  writing), the pages' images and files, the settings, the widgets, the
  pen tools and the locked pages' password. It is in **Download/notE
  backup**, where it stays even if notE is uninstalled; the last 7 are
  kept. It is turned off from the Backup page
- **[New feature]** **[📱 Mobile]** **"Upload to Proton Drive"**: it opens
  "Save as" with the backup's name already written; the first time you
  choose the Proton Drive folder, from the second the window already opens
  there and you just tap "Save". Without a backup, one is made first.
  Every backup in the list also has "Upload to Proton Drive"
- **[New feature]** **[📱 Mobile]** **The reminder**: 7 days after the
  last upload (you choose 3, 7, 14, 30 or never), leaving the app a
  notification arrives, at most one a day, until one is uploaded. Tapped,
  it opens the Backup page
- **[New feature]** **[📱 Mobile]** **Restore**, from a backup in the list
  or from a file (also downloaded from Proton Drive): after the question,
  first a backup of how it is now is made (to go back), then notE restarts
  and at reopening, before reading anything, it puts the backup's database,
  files and settings in their place. A file that isn't a notE backup, or
  one from a newer notE, touches nothing and says so
- **[Project]** `data/backup/BackupArchive.kt` (how the zip is made, and no
  file of the zip can leave its folder), `BackupStorage.kt` (Download/notE
  backup with `MediaStore`, a folder for tests and for phones before
  Android 10), `Backups.kt` (the backup with `VACUUM INTO`, the 7 kept, the
  upload, the reminder, `prepareRestore` and `applyPendingRestore` called
  first in `MainActivity.onCreate`); `ui/screen/BackupScreen.kt`;
  `BackupStrings` in the eight languages; `Notifier.postBackupReminder`;
  `AppDatabase.closeInstance`. Tests: `BackupTest` (everything is inside,
  one a day, the 7 kept, the complete restore, wrong files, the upload,
  the reminder, with real SQLite), `BackupScreenTest` (the page, "Save as"
  with the name and then where it was saved, restore from the menu)

### 🆕 v0.139 — 27/10/2026 · 23:31 — Buttons, as in Notion: one tap and the chosen actions run
*(Claude Code on the PC; tested on the phone on a test page: "/button" opens the window, "Show confirmation" and "Insert blocks" with a checkbox, "Cancel" does nothing and "Continue" puts the checkbox under the button, "Edit button" finds the actions again; in a test database the Button property with "Edit pages in" → "This page" → "Toggle" ticks and unticks its row; then everything removed, the test database to the trash; 924 tests passed)*

- **[New feature]** **[📱 Mobile]** **The "Button" block**, from the "/"
  and "+" menus (or "Turn into", and the line's text becomes the name): a
  grey pill with the icon and the name, as in Notion. At the first tap,
  when it doesn't know yet what to do, it opens the actions window; after
  that, every tap does them all, in order. Long-pressing it there is the
  block menu, with "Edit button" to change them, "Link in another page",
  "Move to", "Duplicate" (a copy with its actions, which doesn't depend on
  the other) and "Delete"
- **[New feature]** **[📱 Mobile]** **The actions window**: at the top the
  name and the icon (emoji, icons or an image, from the same picker as
  pages), below "When tapped" with the actions, each in its box, and "New
  action" to add some. The actions, as in Notion:
  - **"Insert blocks"**: blocks already written (text, lists, checkboxes,
    toggle, quote, callout, divider), above or below the button, or at the
    top or bottom of the page. Below the button, the blocks of a new tap
    go before those of the previous tap;
  - **"Add page to"**: a new row in a database, with the name and the
    properties' values, and if wanted it is opened at the end;
  - **"Edit pages in"**: changes the properties of "This page" (the
    button's row, or the one whose page contains the button), or of a
    database's rows, all of them or only those where a property has a
    certain value. You can tick, untick or toggle a checkbox, increase or
    decrease a number, set the date of now or of today, add or remove a
    tag, write a text or the name, empty;
  - **"Show confirmation"**: a question; "Cancel" stops the actions after
    it;
  - **"Open page or URL"**: at the end a page of the app or a link opens;
  - **"Send notification"**: a notification on the phone, right away.
  With "…" an action moves up or down or is removed. As in Notion a
  half-done action isn't saved: "Done" says what is missing (the database,
  the page, the value). Pages and links open only at the end, so leaving
  the page doesn't leave the following actions half done
- **[New feature]** **[📱 Mobile]** **Databases' "Button" property** (among
  the types, also in "Change type"): a button on every row, with the same
  actions for all, working on "This page", that is the tapped button's
  row. The actions are chosen under the type ("Edit actions"), from the
  header menu, or tapping a button that doesn't have any yet. On the
  button there is the property's name, or the label chosen in the window.
  A button has no value: no filter, sorting, calculation or chart on it,
  and "Fit to content" measures its label
- **[New feature]** **[📱 Mobile]** **Links to buttons**: from a button's
  menu, "Link in another page" puts a linked copy at the bottom of the
  chosen page; from the "/" menu, "Link to button" makes the line the
  linked copy of a button chosen from a list of all of them. The copy has
  the little arrow, shows the real button's name and icon and does its
  actions (the blocks go next to it), and changing them from it changes
  them for all. "Go to original" leads to the real button; if that is
  deleted, the copy reads "Deleted button" and does nothing
- **[New feature]** **[📱 Mobile]** After a tap a notice says what isn't
  visible: the rows added or changed in another database, and what
  couldn't be done (the database deleted or password-protected, a
  property gone, the page locked, notifications off), while the other
  actions go on. A filter on a deleted property changes nothing, instead
  of changing all the rows
- **[Note]** Compared to Notion, the actions that talk with the outside or
  with other people (Gmail, webhooks, Slack, notifications to others) and
  variables with formulas are missing: notE lives on the phone. "Undo"
  removes the blocks put in the open page; database rows changed by a
  button are put back by hand
- **[Project]** `data/buttons/Buttons.kt` (`ButtonConfig`, `ButtonStep`,
  `PropertyEdit.applyTo`, `ButtonFilter.matches`), `ButtonRunner.kt` (the
  engine, `ButtonHost`, `ButtonProblem`); `BlockType.BUTTON` with
  `BlockAttrs.button` and `buttonLink`; `ColumnType.BUTTON` with the
  actions in the `optionsJson`; `PageRepository.insertBlocksAt`,
  `addLinkedButton`, `buttonChoices`; `ui/screen/ButtonEditor.kt` (the
  window), `ButtonUi.kt` (`ButtonRunController`, `ButtonPill`,
  `ButtonBlockContent`, `ButtonEditHost`, `ButtonCell`,
  `LinkToButtonSheet`); `ButtonStrings` in the eight languages;
  `Notifier.postButton`. No migration: the database stays at version 65.
  Tests: `ButtonModelTest` (the calculations of every type, the filter,
  saving), `ButtonRunnerTest` (every action on the real database, the
  confirmation, the order, the problems), `ButtonBlockTest` ("/" menu,
  "Turn into", Undo, linked copies), `ButtonScreenTest` (the real editor:
  tap, question, window, block menu, linked copy, the database property),
  `ButtonsRenderTest` (the image of the buttons and of the window)

### 🆕 v0.138 — 27/10/2026 · 20:04 — The "Countdown" widget, with flipping cards
*(Claude Code on the PC; tested on the phone adding the widget from the sidebar: Christmas with the right count, the seconds flipping (seen in a screen recording), the long press opening the window, "Add countdown" putting one below and "Delete"; then the test widget was removed; 865 tests passed)*

- **[New feature]** **[📱 Mobile]** **"Countdown" among the widgets** (at
  the bottom of the "Add widget" list). It is born with next Christmas at
  midnight, as in the photo. Under the title, six light cards with dark
  digits: the months, weeks, days, hours, minutes and seconds left, with
  the names in light blue below. Months are calendar months (from 31
  January "one month" reaches 28 February) and can be more than twelve, as
  in Indify; what remains goes into weeks, days, hours, minutes and
  seconds. A date already past counts the time elapsed since then
- **[New feature]** **[📱 Mobile]** **Every number that changes flips**
  like the sheets of a calendar: the top half with the old number folds
  forward around the hinge and goes down, and from the other side comes
  the bottom half of the new number, in half a second. Seconds flip every
  second, the other units when they change. Like the other widgets, they
  move only with the sidebar open
- **[New feature]** **[📱 Mobile]** **Long-pressing a countdown changes
  it**: the name and, one per field, year, month, day, hour, minute and
  second, both in the future and in the past. A date that doesn't exist
  (31 April) isn't saved, and it says so. From the same window the
  countdown can also be removed ("Delete")
- **[New feature]** **[📱 Mobile]** **"Add countdown" in the widget's
  three dots**: one more countdown, which goes below the others, one under
  the other and not scrolling horizontally, so they are all visible. The
  new one starts from tomorrow at midnight, to be changed in the same
  window
- **[Project]** `Countdown.kt` (`CountdownWidget` with the name
  `"countdown"` in the saved widgets, `CountdownEntry`, `countdownParts`,
  `christmasCountdown`); `WidgetStore.add(kind, firstCountdown)`,
  `addCountdown`, `updateCountdown`, `removeCountdown`,
  `WidgetKind.COUNTDOWN`; `CountdownWidgetUi.kt` (`CountdownView`,
  `CountdownBlock`, `FlipTile` with the two still halves and the turning
  one, `CountdownEditorDialog`); the labels in `WidgetStrings`, in the
  eight languages. Tests: `CountdownTest` (the calculations, Christmas,
  saving), `CountdownUiTest` (the long press changing the year, "Add
  countdown" putting it below, "Delete"; the card that halfway shows the
  old number and the new, at the end only the new); in `WidgetsRenderTest`
  two countdowns in the widgets image

### 🔧 v0.137.0.1 — 27/10/2026 · 19:42 — In "Change type" the type rows no longer move when choosing one
*(Claude Code on the PC; tested with the tests, which fail without the fix; on the phone only installed; 857 tests passed)*

- **[Bug fix]** **[📱 Mobile]** **Choosing a type, the rows below stay
  still.** Under the chosen type there is the box of its settings (the
  number format, the duration, the Relation, the Rollup); for types that
  have none (Text, Date, Select…) it stayed empty but with its 8-point
  margin below. So the chosen row got a little taller and those below went
  down, to come back up choosing another. Now the box is there only for
  types with settings, and the tick of the chosen type is as big as its
  icon, so the row doesn't grow for it either. It also applies in the
  property window from the database settings
- **[Project]** `TYPES_WITH_SETTINGS` in `DatabaseViewScreen.kt`. Test:
  `PropertyMenuTest` (+1: with "Date" chosen, between its row and the one
  below the same distance as between the others)

### 🆕 v0.137 — 27/10/2026 · 19:32 — A property's menu opens with a tap, in groups as in Notion
*(Claude Code on the PC; tested on the phone opening the menu of a property of a database and closing it without touching anything; 856 tests passed)*

- **[Change]** **[📱 Mobile]** **Tapping a property's header opens its
  menu; long-pressing it no longer does anything.** The menu is no longer
  the previous dropdown but a window rising from the bottom, with groups
  of entries and the little arrow on those that open another window, like
  Notion's. The entries are the previous ones, in the same order: "Move to
  left" and "Move to right"; "Activate all" and "Deactivate all"
  (checkboxes only); "Align left", "Center" and "Align right"; "Resize
  column" and "Fit to content"; "Hide", "Hide icon" and "Calculate". The
  same for the main property (Name): "Rename", the moves, "Calculate",
  "Hide icon" and the width
- **[New feature]** **[📱 Mobile]** In the menu, after the moves:
  **"Change type"**, with the types that were in the property window
  (under the current one, its settings: the number format, the duration,
  the Relation, the Rollup); **"Filter"**, the window of the filter of the
  icons at the top, which already starts from this property's values;
  **"Sort"**, the window of the sorting of the icons at the top. They are
  the same windows, so the filter and the sort rules are the same from
  both sides. With "Lock view" they are greyed out, like the icons. For
  Name there is "Sort" (the filter at the top doesn't work on the name)
- **[Removed]** **[📱 Mobile]** **"Edit property" is no longer in the
  menu.** The name is written directly in the row at the top of the menu,
  next to the icon, as in Notion: closing the menu the property takes it.
  The type's settings are in "Change type"
- **[New feature]** **[📱 Mobile]** **"Delete property" at the bottom of
  the menu**, in red as in Notion (before it was inside "Edit property").
  Before deleting it asks, because the values of all the rows go away too.
  With the database locked it is greyed out, and the name can't be changed
- **[Change]** **[📱 Mobile]** From the database settings (Properties),
  where there is no header to tap (the board, the calendar, the gallery),
  tapping a property opens as before its window with name and types
- **[Project]** `PropertyMenu.kt` (`PropertyMenuSheet` with the name row,
  `PropertyMenuGroup`, `PropertyMenuChevronRow`, `PropertyMenuActions` in
  `LocalPropertyMenuActions`); in `DatabaseViewScreen.kt`
  `PropertyHeaderCell` and `NameHeaderCell` with `combinedClickable` (the
  long press does nothing), `columnWidthRows`, `PropertyMode` (`FULL`,
  `TYPE`), `FilterSheet(startColumnId)`; `DatabaseViewModel.renameColumn`;
  `DbStrings.deletePropertyTitle`, `deletePropertyNote`. Tests:
  `PropertyMenuTest` (the order of the entries, the long press, "Change
  type", the name in the top row, "Delete property" with the question,
  "Filter" and "Sort"), `HideIconTest` with the tap

### 🆕 v0.136 — 27/10/2026 · 19:10 — "Back" to return to the top after going far down a page
*(Claude Code on the PC; tested on the phone on the main page, scrolling from the left margin: going down it appears, going up it goes away, tapped it brings back to the top; 850 tests passed)*

- **[New feature]** **[📱 Mobile]** **"Back" at the bottom centre after
  going far down**, in every page: the same little button as the index,
  with the up arrow. It appears scrolling down with the finger beyond a
  screen and a half from the top; tapping it the page returns to the top
  and the button goes away. **Going up by hand it doesn't appear**, and if
  it was there it goes away: what counts is the last direction in which
  the finger scrolled the page, not the moves the app makes by itself (the
  caret brought into view, a link's jump). It isn't there while typing
  (keyboard open, it would cover the text), with the whole page selected,
  while drawing, and when there is already an index link's "Back"
- **[Project]** `BackToTop.kt` (`BackToTop` with the finger's direction
  from a `NestedScrollConnection` on the page's list and the distance from
  the top estimated like the scroll bar, `ItemSizes`; `BackToTopButton`);
  `BackPill` in `PageAnchors.kt`, the pill common to the two "Back"s.
  Test: `BackToTopTest` (down it appears, up it goes away, down it comes
  back, tapped it goes to the top)

### 🔧 v0.135.0.1 — 27/10/2026 · 18:58 — "---" makes the divider without leaving the dashes or double lines
*(Claude Code on the PC; tested with the tests on the PC; on the phone the user does the test, as they were using the test page; 849 tests passed)*

- **[Bug fix]** **[📱 Mobile]** **Typing "---" the line becomes a divider,
  and the text field shows it right away as it really is.** In the
  database everything was already right: the line became the divider,
  without dashes, and the lines below went under it. The field being typed
  in instead stayed as before, with the dashes, with the list number and
  with the lines that by then were under the divider — which were thus
  shown twice, above in the old field and below the divider. The field
  waited to get back from the database the text without the dashes; on
  the phone the database skips that step and arrives already split, and
  the field, convinced it still had a change on its way, no longer
  updated. Now, once the divider is made, the field takes what is in the
  database, as after Undo. An extra empty line above the divider (the
  first time, at the bottom of the page) came from there: the field left
  behind rewrote it
- **[Project]** `PageEditorViewModel.convertToDividerAndFocusNext` fires
  `externalChangeTick` after the change. Test: `DividerShortcutUiTest`
  (Enter and "---" at the bottom of the page, and "---" in the middle of a
  numbered list: in the database and in the fields every line only once,
  without dashes). On the PC the fault doesn't repeat (the database there
  answers in time): the real test is on the phone

### 🆕 v0.135 — 27/10/2026 · 17:55 — "Hide icon": a property's icon can be hidden
*(Claude Code on the PC; tested on the phone only opening the header menu in a database, without touching the entry: hiding and showing the icon again is tested by the tests on the PC; 847 tests passed)*

- **[New feature]** **[📱 Mobile]** **"Hide icon" long-pressing a
  property's header**, under "Hide", with the icon itself next to it: the
  type's icon (a date's calendar, a link's chain…) goes away and the name
  starts from the column's edge. Long-pressing again the entry has become
  **"Show icon"**, and the icon comes back. It also applies to the **"Aa"
  of the main property** (Name). By default every icon shows: the existing
  properties stay as before, and you choose one by one, database by
  database
- **[Change]** **[📱 Mobile]** A property with the icon hidden doesn't show
  it among a row's properties either (at the top of its page and in the
  row's window): there its place stays empty, so the names stay in a
  column. With "Fit to content" the column no longer counts the icon's
  space. In the menus to choose a property (filters, sorting, settings)
  the icons stay, to recognise the type
- **[Project]** `DatabaseColumnEntity.hideIcon` and
  `PageEntity.hideNameIcon` (database v65, migration 64→65: all icons
  show); `DatabaseViewModel.setPropertyIconHidden`,
  `PageDao.setHideNameIcon`; `LocalPropertyIcons` (`PropertyIcons`) for the
  headers; `nameFitWidth(iconHidden)`. Tests: `HideIconTest` (in a
  database inside a page: "Hide icon", "Show icon" and the "Aa"),
  `MigrationTest` (+1, from v64 with `schema/v64.sql`)

### 🔧 v0.134.0.2 — 27/10/2026 · 14:45 — "Fit to content" measures a URL cell's label, not the whole link
*(Claude Code on the PC; tested on the phone looking at a database's table view, without touching anything: the URL column is now as wide as its short label; 845 tests passed)*

- **[Bug fix]** **[📱 Mobile]** **With "Fit to content", a URL column is as
  wide as what it shows.** In a table view the URL cell showed a short
  label ("Show as…", v0.133), but the column was measured on the whole
  meeting link, 70 letters. A fault of v0.133: the "Fit to content"
  measure didn't know about the label. Now it measures the label when there
  is one, the link when there isn't
- **[Project]** `cellFitWidth(linkLabel)`, which `fitWidths` takes from
  `DatabaseTableState.linkLabels`. Test: `PageLinkTest` (+1)

### 🔧 v0.134.0.1 — 27/10/2026 · 14:35 — The Counter's + and − are instant
*(Claude Code on the PC; measured on the phone on a temporary release, + and − three times on the user's counter, which went back to 0; 844 tests passed)*

- **[Bug fix]** **[📱 Mobile]** **The Counter's number changes as soon as
  + or − is tapped.** The user was right: the counter was born on 24/09
  without sounds, and the sounds arrived with v0.179 (18/10). Measured on
  the phone: at every tap starting the click with `AudioTrack` (stop,
  reload and play) held the app still for 65-74 ms, and only then changed
  the number, about 80 ms after the tap. Now the number changes first and
  then the sound starts, from a `SoundPool` (Android's player for short
  sounds) that makes nobody wait: measured again, the number changes 2-9
  ms after the tap and the tap itself lasts less than 1 ms. The sounds are
  the same as before, with their volume
- **[Bug fix]** **[📱 Mobile]** The widgets' state is written to memory
  from a separate thread, one after the other: a tap on a widget no longer
  waits for the JSON of all the widgets (3-10 ms per tap)
- **[Project]** `CounterSounds`: `prepare` (a WAV per sound in the cache,
  loaded into the `SoundPool`; `CounterView` calls it), `play` that doesn't
  wait (until the `SoundPool` is ready, the previous way on a separate
  thread), `wav`; `WidgetStore.update` saves with `saver`. Test:
  `CounterSoundsTest` (+1, the WAV file)

### 🆕 v0.134 — 27/10/2026 · 14:22 — "Full or page line": every page chooses whether long lines stay whole or wrap
*(Claude Code on the PC; tested on the phone on a test page, with "Full line view" off in Settings: a long line written at the bottom, "Full or page line" in the three dots with "Default (Page line)", then "Full line" (the line no longer wraps) and "Page line" (it wraps again); the page put back on "Default" and the line removed, compared with the database (migration 63→64 included); 843 tests passed)*

- **[New feature]** **[📱 Mobile]** **"Full or page line" in every page's
  three dots**, under "Paragraph", with how it is now written next to it.
  It opens and you choose among three options: **"Default"** does as "Full
  line view" in Settings, and in brackets says what it means now
  ("Default (Full line)" or "Default (Page line)"); **"Full line"** leaves
  long lines whole, to scroll sideways; **"Page line"** wraps them, so the
  whole line fits on the screen. The choice applies right away, even with
  the page open
- **[Change]** **[📱 Mobile]** Settings stays as it was and applies to all
  the pages left on "Default": all the existing ones, and the new ones.
  Changing it, those pages follow it right away
- **[Project]** `PageEntity.fullLineMode` (`FULL_LINE_DEFAULT`,
  `FULL_LINE_ON`, `FULL_LINE_OFF`; database v64, migration 63→64: all
  pages on "Default"); `FullLineChoice.kt` (`fullLinesOf`,
  `fullLineSummary`, `PageFullLineSheet`), which `PageEditorContent` uses
  instead of `AppSettings.fullLineView`;
  `PageEditorViewModel.setFullLineMode`, `PageDao.setFullLineMode` (it
  doesn't change the page's date, like the spell checker). Tests:
  `FullLineChoiceTest` (a long line in the real page: wrapped or whole
  according to the page and Settings), `MigrationTest` (+1, from v63 with
  `schema/v63.sql`)

### ✏️ v0.133.1 — 27/10/2026 · 14:05 — The main row's tick goes down to all its sub rows
*(Claude Code on the PC; tested with the tests, on the phone only installed: the checkboxes in the user's databases weren't touched; 838 tests passed)*

- **[Change]** **[📱 Mobile]** **Ticking the main row ticks all its sub
  rows**, and those under them, all the way down; unticking it unticks
  them all. Together with what was already there, the main row stays
  ticked only when all its sub rows are. Ticking a sub row that has sub
  rows of its own, the tick goes down to them and then goes back up as
  before. Rows that were already right don't change, and don't show up as
  modified now ("Last edited time")
- **[Project]** `DatabaseRepository.setCheckbox`: before going up, the
  checkbox goes down to all the sub rows (with `getSubRows`, in the same
  transaction). Test: `SubRowCheckboxTest` (+2)

### 🆕 v0.133 — 27/10/2026 · 13:44 — What a link shows: the address, the page title or your own text
*(Claude Code on the PC; tested on the phone on a test page: a site written at the end of a line, the link icon with just the caret, "Page title" ("Example Domain", read from the site), "Your text", "Full link" again, and a long-pressed word taking the whole link; the line then put back as it was, compared with the database (migration 62→63 included). URL cells' "Show as…" tested only in the tests on the PC: on the phone there is no test database; 836 tests passed)*

- **[New feature]** **[📱 Mobile]** **"Show" in the text link window** (the
  link icon in the bar above the keyboard): under the address, three
  choices for what is read in the page. **"Full link"** is the whole
  address; **"Page title"** is the web page's title, read from the site
  when it is chosen (the network is needed: without it, the window writes
  "No title found"); **"Your text"** is a label written by hand. The link's
  text changes and keeps its formatting (bold, colour…), the link stays the
  same and the caret goes to the end of the link. It can be changed again
  from the same window, or with Undo
- **[New feature]** **[📱 Mobile]** **A site written as plain text**
  ("https://…" or "www.…", without a link): with just the caret inside the
  link icon appears, and the window starts with that address already
  written. With "Done" it becomes a link, and "Show" applies here too. The
  brackets, quotes and full stop attached around it aren't part of the
  address
- **[Change]** **[📱 Mobile]** Long-pressing a link selects a single word:
  the link icon now takes **the whole link**. Changing it, removing it or
  changing its label applies to the whole link, no longer only to that
  word
- **[New feature]** **[📱 Mobile]** **"Show as…" in databases' URL cells**
  (long press on the cell, with "Edit link", "Copy link" and "Paste
  link"): the same three choices. The cell shows the title or the label in
  place of the address, and so do the cards of the board, gallery and list
  and the properties at the top of a row's page. The tap always opens the
  link, and the magnifier also finds the label. If the cell's link changes
  and it showed the title, the title is read again for the new link
  (without network the whole address comes back); your own label stays.
  With the cell emptied, the label goes away
- **[Project]** `DatabaseCellEntity.linkLabel` and `linkLabelIsTitle`
  (database v63; with migration 62→63 the earlier cells show the link as
  always); `LinkTitles` (the title of the bookmarks' preview,
  `LinkPreviews`); in `TextLinks.kt` `LinkShowChoices`, `LinkShowState`,
  `LinkShowDialog` and `LinkDialog(shown, onSave(url, text))`; `bareLinkAt`
  in `RichText.kt`. In `PageEditorViewModel`, `linkSelection` takes the
  whole link and the site written as text, and there are
  `capturedLinkText`, `replaceLinkText` and `linkTextChanges`: the fields
  take the new text right away, with the caret at the end of the link. In
  `DatabaseViewModel`, `setCellLinkLabel`, `rereadLinkTitle` and
  `DatabaseTableState.linkLabels`, which reach the cells with
  `LocalCellLinkLabels`. Tests: `BareLinkTest`, `LinkShowTest`,
  `CellLinkLabelTest`, `LinkShowDialogTest`, and `MigrationTest` (+1, from
  v62 with `schema/v62.sql`)

### 🆕 v0.132 — 27/10/2026 · 09:42 — "Default" for the size, and under a large heading you write at 16
*(Claude Code on the PC; tested on the phone on a test page: "Default" in the window, a heading at 50, Enter and a line written below, which after the pause goes back to its height with the caret where it was typing; the page then put back as it was, compared with the database; 813 tests passed)*

- **[New feature]** **[📱 Mobile]** **"Default" in the size window** (Aa
  bar → the number), next to the box: it puts back the normal size, 16, on
  the selected text and closes the window. In Italian "Predefinito", in the
  app's other languages their own word
- **[Change]** **[📱 Mobile]** **Under a large heading you write at 16, and
  it shows right away.** In the database new lines were already born
  without a size, that is at 16; what looked like 50 was the text field:
  the new line was in the same field as the heading, and a field has a
  single line height, the one the largest character needs. The lines
  written below thus stayed as tall as the heading, **with the caret as
  tall as the heading**, while the keyboard was open (fields don't split
  again while the caret is in the page). Now, as soon as you stop typing
  for two seconds, the lines that look different from the one with the
  caret leave its field: the heading goes into a field of its own and the
  new lines go back to their height. The field with the caret stays the
  same, so the keyboard doesn't close and the caret stays where you were
  typing. It also works the other way round (typing in the heading, the
  normal lines leave) and for checkboxes among normal lines
- **[Bug fix]** **[📱 Mobile]** Found testing it on the phone: with the
  heading out, in the same round the field with the caret also joined the
  lines below, and changing above and below together it didn't know where
  to put the caret back, which ended up a few lines lower (the letters
  typed afterwards ended up in "1. First number"). Now the field with the
  caret changes on one side at a time, and the rest at the next round; it
  also applies to the joins of the pieces on the screen, which could do
  the same
- **[Change]** **[📱 Mobile]** Typing right after Enter, before the new line
  was in the database, for an instant it showed with the heading's size:
  now that prediction shows it at 16 too. The rest of the formatting (bold,
  colour…) continues as before, and an Enter in the middle of a heading
  leaves its size to the half that goes down
- **[Project]** `EditorStrings.defaultFontSize`, `FontSizeDialog`;
  `insertedSpans` in `spliceSpans` (the text after the first line break of
  an inserted piece doesn't inherit `size`); in
  `PageEditorViewModel.keepVisibleTogether` the field with the caret lets
  the differently looking lines out, one side at a time, keeping its key
  (spelled out with `runGroupKey` if it was the group's; `rechunk` then
  redoes it), and `MergedTextRunField` puts the caret back when lines
  leave. Tests: `NewLineFontSizeTest` (+5), `NewLineSizeUiTest` (Enter and
  letters in the real field: at 16 in the database, then the heading in a
  field of its own and the caret at the end of "More"), `LineLookChunksTest`
  (+1: the key of the field with the caret stays, and after `rechunk` the
  keys are still all different)

### 🔧 v0.131.0.2 — 27/10/2026 · 09:17 — The space between lines no longer changes by itself
*(Claude Code on the PC; tested on the phone on a long page of the user: lines 44 points apart at the top, below a heading and further down, and the page stopped at four points photographed right away and after 3 seconds, identical apart from the scroll bar; 806 tests passed)*

Consecutive text lines live in fields, pieces of 50 lines. While
scrolling, the visible pieces join into a single field and then split
again (v0.113.1 and later). Three things of a field depended on which
lines were inside it, and so changed when lines moved from one field to
another:

- **[Bug fix]** **[📱 Mobile]** **The height of the lines.** A field has a
  single line height: the one the largest character it contains needs,
  wider if there is a checkbox. In a long page, headings with size 20 thus
  made all the lines of their piece taller: under one heading the lines
  were 54 points apart instead of 44. Now a line that looks different from
  the one above (larger size, or a checkbox among normal lines, or the
  other way round) starts a field of its own, and different fields don't
  join
- **[Bug fix]** **[📱 Mobile]** **Where the text sits inside the lines.**
  Compose decides it looking **only at the first line** of the field: if
  there is a bullet there (written at 20, larger than the text), the text
  of the whole field goes down by 11 pixels. Measured: the baseline 60
  pixels from the top of the line in the piece that started with a bullet,
  49 in the one above; joining them the text went back up. Now a piece
  starts only with a line that has the same mark as the first of the piece
  above (if none arrives, at twice the size it is cut anyway), and two
  pieces starting differently don't join
- **[Bug fix]** **[📱 Mobile]** **The space at the field's ends.** When a
  mark of the first or last line sticks out (the bullet, the accent of
  "É"), Compose lengthens the text by that much, but only at the two ends:
  between two pieces there were 5 extra pixels that disappeared joining
  them. Now between two consecutive pieces that addition is removed, and
  they touch like the lines of a single field. It applies in all pages
- **[Note]** While typing fields don't split again (v0.113.1): a new line
  written right under a large heading takes its space until you stop
  typing. And a finger selection that reaches the end of a field brings
  the next one along (since 19/10/2026): at that moment the space can
  still change. At the two points where two pieces now don't join, the
  selection crossing them joins them that way
- **[Project]** `PageEditorViewModel.lineLookOf` (checkbox, larger size)
  and `markerSizeOf` (the bullet's size, even hidden): `rechunk` starts a
  piece where the first changes (even between lines on the screen) and
  cuts the 50 only where the second is the one of the piece's first line;
  `keepVisibleTogether` doesn't join pieces that don't fit. `TextEdges` and
  `trimTextEdges` in `MergedTextRunField`, with the field's line height in
  a single place (`lineHeight`). Found with a temporary log on the phone
  (line height and baseline of every field), then removed. Tests:
  `LineLookChunksTest` (the heading and the checkboxes in fields of their
  own; the page scrolling in windows of 20 lines never changes the line
  height of a normal line; in a bulleted list with normal lines every
  piece starts like the first, even scrolling; equal lines still join),
  which fail without the fixes

### 🔧 v0.131.0.1 — 27/10/2026 · 08:12 — The keyboard on the home page no longer lags
*(Claude Code on the PC; measured on the phone on the home page, before and after, opening the keyboard in the title, in a cell and in an empty line; 802 tests passed)*

- **[Bug fix]** **[📱 Mobile]** **Opening and closing the keyboard the
  page's databases are no longer redone.** When the keyboard opens or
  closes (and when the caret enters or leaves) the page updates a couple
  of times, and that's right; but with it all the databases it contains
  were redone from scratch. On the user's home page there are ten, kept
  ready even off the screen, one of them a large table with thousands of
  checkboxes: up to 60–117 ms per frame, out of 8. Measured on the phone
  opening and closing the keyboard five times (release, `dumpsys
  gfxinfo`), late frames and the slowest (99%):
  - in the home's title: **13.9% → 7.2%**, 69 → 18–20 ms
  - in a cell of the large table: **15% → 6.4%**, 73–85 → 16 ms
  - in an empty line at the bottom, with the bar above the keyboard: **18%
    → 8–10%**, 97–117 → 19–20 ms
  The ones still slow are one or two at the start and at the end: the
  caret's drop (a small window of its own) and the bar appear or disappear
- **[Project]** Three things, found with a system trace and the names of
  Compose's functions turned on only for the test (`Composer.setTracer`):
  `openBlockMenu` redone at every round of the page, which the islands
  read (`PageItemEnv.onOpenBlockMenu`) and which therefore made them all
  redraw (now `remember`); the content of the page's `LazyColumn` redone at
  every round, which restarted all the items kept ready (now
  `listContent`, redone only if what it shows changes); and the keyboard's
  height read for the bar while building it, which with the bar open
  redid the page at every frame of the motion (now it is read in
  `offset { }`). Test: `PageFocusRecomposeTest` (the caret entering and
  leaving the title doesn't restart the page's database: without the fix
  it restarted 24 times)
- **[Project]** So that it doesn't happen again, a second test,
  `theKeyboardMovingLeavesThePageAlone`, sends the page the keyboard's
  heights one after the other, like the phone at every frame, up and down:
  the bar rises with it, and of the app only the caret that goes off with
  the keyboard closed and the scroll bar are redone. Anything else that
  one day read the keyboard while the screen is built makes it fail; with
  the previous reading in the bar the page was redone 18 times. In the
  tests, until someone sends it, for Compose the keyboard is "visible":
  the test closes it first

### 🆕 v0.131 — 26/10/2026 · 23:43 — "Hide “New”" in the board
*(Claude Code on the PC; tested on the phone on a board, where it stayed on; 800 tests passed)*

- **[New feature]** **[📱 Mobile]** **"Hide “New”"** in the board's
  settings (the three lines), under "Hide empty groups": on, under the
  cards of every column there is no more "+ New", neither in the columns
  nor in the bands, with or without "Color columns". In the bands a column
  without cards doesn't leave an empty coloured strip, only its place. It
  applies to the view where it is turned on, like the board's other
  settings; by default it is off
- **[Project]** `PageEntity.boardHideNew` (migration 61→62, off for all
  views); in `BoardColumnCards` `onAdd` null means no "New", and in the
  bands the empty cell stays without colour. Tests: `BoardLayoutTest` (+2),
  `MigrationTest.from61EveryBoardStillShowsNew` (schema `v61.sql`)

### 🔧 v0.130.0.1 — 26/10/2026 · 23:35 — Lines no longer get duplicated when the page splits its pieces again
*(Claude Code on the PC; on the phone the affected page was checked after removing the second copy, the fix tested with the tests; 797 tests passed)*

- **[Bug fix]** **[📱 Mobile]** **Double lines, all as bullet points, under
  a page's fiftieth line.** The lines on the screen are in a single field
  and, when you aren't typing, the page splits them again into pieces of
  50: if at that moment the field received a tap or a change (like a
  heading's size change), its lines beyond the fiftieth looked just written
  and became new lines with the fiftieth's type. In one of the user's
  pages the fiftieth was a second-level bullet point: 34 lines of a
  section had reappeared right under it, all bullet points, with the
  originals further down. Conversely, a field shorter than its piece could
  make the lines it didn't have disappear. Now the change goes to the
  lines the field really has
- **[Note]** From the database copies made on the PC: at 20:01 on 20/10
  the section was in order, the following night it was already doubled.
  So it happened on 20/10 after 20:01, not on 21/10. The first copy, the
  one the user fixed on 21/10, stays; the second (60 lines) was removed.
  Done on 26/10 at 23:33 on the phone: the page went from 323 to 263
  lines, database check ok, the previous copy kept on the PC
- **[Project]** `PageEditorViewModel.runOfField`: if the lines the field
  sends to `updateRun` are still there, one after the other and with the
  same text as before, the group is theirs and not the one `resolveRun`
  rebuilds with the current pieces' boundaries. Tests:
  `ChunkSelectionTest` (+2: the field left with 100 lines and the one left
  with 50), which both fail without the fix

### 🆕 v0.130 — 26/10/2026 · 17:44 — More notifications for a date, as in Google Calendar
*(Claude Code on the PC; tested on the phone opening the date window and closing it without saving; 795 tests passed)*

- **[New feature]** **[📱 Mobile]** **More notifications for a date**, as
  in Google Calendar: with "Include notification" on, below there is a row
  for every notification, with the X to remove it, and "Add notification"
  to add another. With a tap: 10 minutes, 30 minutes, an hour, a day or a
  week before (and at the start, if it had been removed); from "Custom…"
  any number of minutes, hours, days or weeks before, up to a year: "10
  hours before", "3 days before", "2 weeks before". It is both in the Date
  property window and in the calendar events' "More options". With the
  last one removed, the notification turns off
- **[New feature]** **[📱 Mobile]** The early notification says how much
  is left: "Concerts · Starts in 3 days". The one at the start stays as it
  was, and also rings at the end if the date has one; without it, nothing
  rings at the end. A repeating date rings before every time. "One day
  before" is at the same time of the day before even when daylight saving
  time changes in between
- **[Project]** `DateRange.reminders`: the minutes before the start. In the
  cell's value they are between the repetition and the "n", which stays
  the last field (`start|end|t|rule|0,600,4320|n`): the alarm finds them
  with the same search as before, and dates saved before (only the "n")
  count as "at the start". `dateReminderTimes` gives `DateReminderTime`
  (when, whether it is the end, how many minutes before) and counts the
  repetitions even after the window, for the early ones. The words in
  `ReminderTexts` (8 languages), the list in `ReminderList`
  (`DateRemindersUi.kt`). Tests: `DateRemindersTest` (10),
  `ReminderListTest` (3), `NotificationsTest` updated

### 🔧 v0.129.0.1 — 26/10/2026 · 17:29 — The arrow of links inside the page no longer covers the last letter
*(Claude Code on the PC; tested on the phone on a page of the user; 782 tests passed)*

- **[Bug fix]** **[📱 Mobile]** **The arrow of links to a line of the
  page** (the navigable index, since v0.118) **covered the last letter**,
  and that letter stood apart from the others with a space in front:
  "Emergenc y", "Creepy thing s" in a page's summary. Now the last letter
  stays attached to the others and the arrow comes right after; if the
  line continues after the link, it is the next word that moves a bit to
  make room for it
- **[Project]** To make room for the arrow the last letter was widened
  (`letterSpacing`), but Android splits the extra space between before and
  after the letter: half ended up in front of it, and at the end of the
  line (on the phone, Android 17) the half behind wasn't there, so the
  arrow fell on it. Now `markBlockLinkEnd` (in `PageAnchors.kt`) widens the
  character after the link, usually the space, and nothing if the link
  closes the line; `drawBlockLinkArrows` draws the arrow after the letter
  (`blockLinkArrowSpan`). Test: `BlockLinkArrowTest` (3), which measures
  where the letters fall

### 🆕 v0.129 — 26/10/2026 · 17:07 — The standings' zones as in Sofascore: Champions, Europa, Conference and relegation
*(Claude Code on the PC; tested on the phone on the Serie A standings; 779 tests passed)*

- **[New feature]** **[📱 Mobile]** **The zones in the leagues'
  standings**, as in Sofascore: above the first team of every zone its
  name in small letters ("Champions League", "Europa League", "Conference
  League Qualification", "Relegation"), and on the left margin a coloured
  bracket from the name to the zone's last team: green the Champions, blue
  the Europa League, light blue the Conference, red relegation. There are
  also the other zones ESPN marks: light green the Champions qualifiers
  and the promotion playoffs (Serie B), orange the relegation playoff
  (Bundesliga, Ligue 1); in the European cups, the knockout phases with
  ESPN's colour. It also applies to past seasons
- **[Note]** The zones are ESPN's, team by team, and can differ from
  Sofascore's: today in Serie A ESPN gives the Europa League to the 5th and
  6th and the Conference to the 7th, Sofascore the Europa to the 5th and
  the Conference to the 6th. A team that goes to Europe with the national
  cup has its zone even mid-table (in La Liga Villarreal, 10th, with the
  Europa League). NBA, NFL and MLB: ESPN gives no zones and the standings
  stay as they were. Standings already downloaded take the zones when they
  update (at most half an hour, or right away with the arrow)
- **[Project]** `StandingRow` has `zone` and `zoneColor` (ESPN's "note";
  the colour "##B5E7CE", which ESPN sometimes writes that way, is cleaned
  up); `standingZoneOf` and `standingZoneLabel` in `Sports.kt`; the bracket
  is `Modifier.zoneBracket` in `SidebarSports.kt`, and every standings row
  has 12 dp on the left for it. Tests:
  `SportsTest.standingZonesFromTheNotesOfEspn`,
  `SportsWidgetTest.standingsWithTheZonesLikeSofascore`

### ✏️ v0.128.2 — 26/10/2026 · 16:49 — The main row is ticked only when all its sub rows are
*(Claude Code on the PC; tested with the tests, on the phone only installed: the checkboxes in the user's databases weren't touched; 777 tests passed)*

- **[Change]** **[📱 Mobile]** **Unticking a sub row also unticks the main
  row**, and up to the top. A main row ticked by hand with something still
  to do below it fixes itself as soon as one of its sub rows is touched:
  it stays ticked only if they all are
- **[Project]** `DatabaseRepository.setCheckbox`: at every checkbox of a
  sub row, the main row takes "all ticked yes/no", going up until there is
  nothing to change. Test: `SubRowCheckboxTest` (6), which now also moves
  the app's clock forward (rows stay still `CHECKBOX_SETTLE_MS` after a
  tick)

### ✏️ v0.128.1 — 26/10/2026 · 16:43 — Sub rows: the main row ticks itself, and they are sorted as blocks
*(Claude Code on the PC; tested on the phone on a database of the user, looking at the order without touching the checkboxes; the tick going up tested with the tests; 775 tests passed)*

- **[Change]** **[📱 Mobile]** **With all the sub rows ticked, the main
  row ticks itself**, in the same Checkbox column; and if it is a sub row
  too, it goes up to the top. Unticking a sub row, the main row stays as
  it is
- **[Change]** **[📱 Mobile]** **A row with its sub rows is sorted as a
  block**: it sits in the place of the first of its elements (itself or a
  sub row), and inside the block the sub rows follow the same rules. In a
  shopping list sorted with the unticked on top, a main row that was ticked
  went down among the ticked ones bringing along three sub rows still to
  buy: now the block stays at the top with the unticked ones, and when the
  three are bought it goes down
- **[Project]** `DatabaseRepository.setCheckbox` (in a transaction, with
  `DatabaseDao.getSubRows`), `sortedAsBlocks` in `SubRows.kt`, used by
  `DatabaseViewModel.sortRows`. Test: `SubRowCheckboxTest`

### 🔧 v0.128.0.1 — 26/10/2026 · 16:30 — The app no longer closes with a board grouped by date day by day
*(Claude Code on the PC; tested on the phone on the board that made it close, with the database compared before and after; 771 tests passed)*

- **[Bug fix]** **[📱 Mobile]** **The app closed opening a page with a
  board** (21 and 24/10, out of memory): the board had "Group by" on a
  date, which without "Date by" groups day by day, and drew at once a
  column for every day with a row, more than 4,000. Now **the board's
  columns are shown 12 at a time**, with "**Load more**" after the last
  one; the column without a value always stays, because new cards are
  born there. The same for **the groups and subgroups of a table inside a
  page**, which were all drawn at once (full screen only those on the
  screen are already drawn). It wasn't a mistake of the user: the board
  must bear any grouping
- **[Note]** To install this version the app on the phone was
  **uninstalled and reinstalled with the data put back inside**: the key
  the PC signs the app with (`.android\debug.keystore`, also used for the
  release) was gone, the folder had been emptied between 21 and 26/10, and
  the phone refuses an update signed with another key. Before: a copy of
  all the app's data on the PC (132 files, checked one by one with MD5,
  database intact), two copies of the archive; then uninstall, install
  with the new key, data put back (again 132 identical files, same counts
  and same fingerprint of all the blocks). The locked pages' password is
  in the app's settings, and it came back with them
- **[Project]** `DATABASE_GROUP_LIMIT` = 12 (`RowLimits.kt`). Test:
  `BoardLayoutTest`, a board with 30 days: 12 columns, "Load more 18" and
  the one without a date

### 🆕 v0.128 — 26/10/2026 · 16:14 — The type inside "Edit property", shorter settings and "Hide empty groups" in the board
*(Claude Code on the PC; "Edit property" and the settings tested on the phone on a database; "Hide empty groups" tested with the tests, on the phone with v0.128.0.1; 770 tests passed)*

- **[Change]** **[📱 Mobile]** **The type is changed inside "Edit
  property"**: tapping a property everything shows, the name at the top
  and below all the types; under the chosen type its settings (Number:
  format, digits, "Show as"; Duration; Relation; Rollup). The type the
  property has now is at the top of the list, with its settings, the
  others after in the usual order. "Change type", in the menu that opens
  long-pressing the header, is no longer there
- **[Removed]** **[📱 Mobile]** From the database settings (the icon with
  the three lines and the dots) **Favorite**, **Turn into page** and
  **Delete** are gone: they are in the six dots and, full screen, in the
  three dots at the top. **Icon** stays only full screen, where there are
  no six dots and otherwise a database without an icon would have no way
  to get one
- **[New feature]** **[📱 Mobile]** **"Hide empty groups" in the board
  too**, under "Group by": the columns without a single card aren't
  shown, including the one without a value ("No …"). By default off, in
  every board, new or existing: all the columns show, as before. If a
  card is born without a value, its column comes back
- **[Note]** On question 3: in a database's six dots "**Turn into**" is
  the entry that on text lines turns them into another block type
  (heading, list, checkbox…); for a page or a database it was never built,
  and it is greyed out as in the "/" and "+" menus. "**Color**" colours a
  page's name in its link; a database inside the page has no such name to
  colour. On a database neither does anything. In the user's board the
  "No …" column wasn't empty: there was an untitled row in it, and "Hide
  empty groups" leaves it while it is there
- **[Project]** `PageEntity.boardHideEmptyGroups` (migration 60→61, test
  schema `v60.sql`), copied also when removing the starting view. Tests:
  `BoardLayoutTest` (by default all the columns; with the switch only
  those with cards), `MigrationTest` from 60

### 🆕 v0.127 — 21/10/2026 · 13:35 — "Data source": a view with the rows of another database
*(Claude Code on the PC; tested on the phone on a database of the user, with the database compared before and after; 767 tests passed)*

- **[New feature]** **[📱 Mobile]** **"Data source" in the "New view"
  window**, like Notion's database sources: by default the view shows its
  database's rows; tapping the entry you choose another database from the
  list (with the name of the page it is in), and the view, which stays in
  this one, shows that one's rows, properties, Rollups and Relations.
  Without a name of its own it takes the other database's. A row created
  from there is born in that database
- **[Change]** **[📱 Mobile]** The starting view can be removed only if
  another one with its same rows remains, which takes its place: a view
  that shows another database can't become the database
- **[Bug fix]** **[📱 Mobile]** Removing the starting view, the database
  took the next view's settings but not "Date by" (weeks, months or years,
  since v0.126)
- **[Project]** `PageRepository.createView(…, sourceId)`;
  `deleteStartingView` looks for the first view with the same source;
  every view puts its own Relations and "Page links" (`LocalRelated`,
  `LocalLinkedPages`) instead of the host's; `databaseViews` says whether
  the starting view can be removed (`deletable`). Test: `ViewSourceTest`

### ✏️ v0.126.2 — 21/10/2026 · 13:15 — Touches seen on the phone: weeks from Monday, Relation cells on one line
*(Claude Code on the PC; tested on the phone on three databases of the user, with the database compared before and after; 763 tests passed)*

- **[Change]** **[📱 Mobile]** **"Date by" weeks always go from Monday to
  Sunday.** They followed the phone, and on the user's, in American
  English, they started on Sunday: a Sunday show ended up in the next
  week. The calendar still follows the phone
- **[Change]** **[📱 Mobile]** **A Relation's cell on a single line**: the
  linked rows that fit whole, then "**+N**" for the others ("Song A
  +17"). Before, the last one stayed cut in half, an arrow and two dots. A
  name that doesn't fit on its own is shortened with the three dots
- **[Change]** **[📱 Mobile]** In "Related to", "**Show on “…”**" and "Name
  there" are **right under the chosen database**: at the bottom of the
  list of all the databases they ended up off the screen
- **[Change]** **[📱 Mobile]** In a Rollup's window, the properties' icons
  are grey as elsewhere (a Relation's was blue)
- **[Project]** `RelatedRowsLine` measures the names with the text measurer
  and draws only those visible: measuring them by composing them left them
  in the accessibility tree even when hidden. Tests: `RelationCellTest`;
  `DateGroupingTest` with the phone in American English

### ✏️ v0.126.1 — 21/10/2026 · 09:17 — The board's bands: the first 12 and "Load more"
*(Claude Code on the PC; tested on the phone on 21/10/2026 on a board of the user: "Load more 1059" at the bottom of the bands; 760 tests passed)*

- **[Change]** **[📱 Mobile]** **The board's bands** (the subgroups) are
  shown **12 at a time**, and "**Load more**", at the bottom left, adds
  12 more, like each column's cards already (50 at a time). Every band
  has all the columns, and with weekly bands over many years there would
  have been a thousand bands to draw together: the board would have taken
  seconds to open. With "Newest first" the first 12 are the last twelve
  weeks, almost three months
- **[Project]** `RowLimits.take` and `limit` have `first` (how many right
  away), `BOARD_BAND_LIMIT` = 12. Test: `DatabaseRowLimitsTest`, a
  thousand weeks
- **[Project]** `ManySortRulesTest` sometimes failed in the full suite:
  after a save the ViewModel rereads the page, and with the PC busy a
  reread started earlier arrived later, putting back for a moment the
  previous rules right while the test added the next one. Now, after
  every step, it waits for the page to stay still

### 🆕 v0.126 — 21/10/2026 · 09:06 — "Date by": date groups become weeks, months or years, even from the most recent
*(Claude Code on the PC; tested on the phone on 21/10/2026 on a board of the user: columns per show, bands per week from the most recent; weeks started on Sunday, fixed in v0.126.2; 757 tests passed)*

The request was a board with a column per show and **bands per week**:
for the bands, grouping dates by week was needed, and this version does
it.

- **[New feature]** **[📱 Mobile]** **"Date by"**, as in Notion: grouping
  by a date — the table's "Group", the board's "Group by", the "Sub
  groups" of both — under the list of properties you choose whether the
  groups are **days** (as before), **weeks**, **months** or **years**. A
  week is named with its first and last day ("12/10/2026 – 18/10/2026")
  and starts where the calendar starts it (Monday, on a phone in Italian);
  a month with its name ("October 2026"); a year with its number. Every
  grouping of the view has its own
- **[New feature]** **[📱 Mobile]** **"Newest first"**, below: the groups
  from the most recent to the oldest, with the current one at the top.
  Rows without a date stay at the bottom
- **[New feature]** **[📱 Mobile]** A row created inside a week, a month
  or a year takes the group's first day, and stays there
- **[Project]** The choice is in `PageEntity.dateGroupingJson`, a JSON with
  the view's four groupings (null = everything by day, from the oldest):
  migration 59→60, test schema `v59.sql`. Files: `DateGrouping.kt`
  (`DateUnit`, `DateGroupBy`, `DateGroupings`, a group's start and end),
  `DateGroupingUi.kt`, `DateGroupStrings.kt` (eight languages);
  `buildRowGroups` receives `dateBy`. Tests: `DateGroupingTest` (rows of
  four days in three weeks, two months and two years) and `MigrationTest`
  from 59

### 🆕 v0.125 — 21/10/2026 · 08:53 — "Relation" and "Rollup": a database's rows linked to those of another, and calculations on the linked ones
*(Claude Code on the PC; tested on the phone on 21/10/2026 on two databases of the user, looking without changing anything: cells, Rollups, "Edit property" windows, choosing rows; a new property tested up to choosing the database and cancelled; touches in v0.126.2; 750 tests passed)*

- **[New feature]** **[📱 Mobile]** Among the property types there is
  "**Relation**" (↗ icon): it links this database's rows to those of
  another, or of the same one. Choosing it in the property window,
  "**Related to**" appears right below, the list of databases with the
  name of the page they are in; without having chosen one it isn't added.
  In the cell there are the names of the linked rows, underlined with the
  arrow as in "Page link": tapping one opens its page (in a simple
  database, where rows have no page, the picker opens). Tapping the rest
  of the cell you choose: at the top the rows already linked, below the
  database's others, with search; a tap links or unlinks, and the cell
  changes right away. A Relation already made keeps its database: its
  cells are rows of that one
- **[New feature]** **[📱 Mobile]** **"Show on “…”": the Relation both
  ways**, as in Notion. On, a twin Relation is born in the other database,
  with the name written below ("Name there"; by default this database's
  name), and the two stay aligned: linking the song "Song A" to "Band B",
  "Song A" appears in the row of "Band B", and vice versa. It is turned on
  and off later too, from "Edit property": on, the twin is born already
  with the existing links; off, it goes away (the cells here stay);
  changing the name, the twin is renamed. Deleting the Relation, the twin
  goes away too
- **[New feature]** **[📱 Mobile]** Among the property types there is
  "**Rollup**" (magnifier icon): it reads the rows linked by a Relation of
  this database, one of their properties (or the name) and makes a
  calculation on it. Choosing it, below you choose the Relation, the
  property and the calculation: **Show original**, **Show unique values**,
  the counts (Count all, values, unique values, empty, not empty) and the
  percentages; for numbers Sum, Average, Median, Min, Max and Range; for
  dates Earliest date, Latest date and the days between the first and the
  last; for checkboxes Checked, Unchecked and their percentages. The
  property read can itself be a Relation: from a row you read **the band
  of its song**, two databases away. It isn't written: it updates by itself
  as soon as something changes in the linked rows, even in the other
  database. Without a Relation in the database, the window says to add one
  first
- **[New feature]** **[📱 Mobile]** Where they show: in the table, in the
  cards of board, gallery and list, in the row's card and at the top of
  its page. **They are sorted** (the Relation by the linked names, the
  Rollup by its number or its date), **the magnifier searches them** (the
  names of the linked rows, the Rollup's text), "**Calculate**" at the
  bottom of the column counts every linked row as a value, "Fit to
  content" measures the names, and **a chart can be grouped by a
  Relation**: a bar for every linked row, with its name. Not yet in the
  filter and in the board's grouping (see "Known limits")
- **[New feature]** **[📱 Mobile]** **Copies.** Duplicating a row's page,
  the copy has the same links and, both ways, the rows over there have it
  among theirs too. Copying a database, the Relations with itself link the
  copy's rows to each other; those towards another database stay linked
  to that one, one way only (the twin over there belongs to the original).
  The copy's Rollups also read the copy's Relations
- **[Project]** No new column in the app's database (it stays at version
  59): a Relation's cell holds the ids of the linked rows, separated like
  tags, and the configuration of Relation and Rollup is in the column's
  options, like the tags'. Rollups are calculated in the
  `DatabaseViewModel`, before filtering and sorting, from the linked
  databases followed over time (`related`, at one and two steps). Files:
  `Relations.kt` (the calculations, the two halves, the copies),
  `DatabaseRepository` (`setRelation`, `addRelationColumn`,
  `updateRelationColumn`, `deleteColumnWithTwin`, in a transaction each),
  `RelationCell.kt`, `RelationSettings.kt`, `RelationStrings.kt` (eight
  languages). Tests: `RelationsTest` (the calculations, with bands, songs
  and rows) and `RelationTest` (with the database: the two halves aligned,
  the twin turned on, renamed and off, the two-step Rollup following the
  changes, sorting, search, duplicated row, copied database)

### ✏️ v0.124.1 — 21/10/2026 · 01:14 — The Changelog reread and renumbered: additions, changes and fixes, and every entry's platform
*(Claude Code on the PC; Changelog reread version by version; tested on the phone in Settings → Patch Notes: "v0.124.1" under the entry, the list with ✏️ v0.124.1 and 🆕 v0.124, the platform labels in the version's page; 729 tests passed)*

- **[Change]** **[📱 Mobile]** **All 246 versions renumbered**, rereading
  them one by one from the first: 🆕 **v0.N** for an addition (something
  the app didn't do before), ✏️ **v0.N.M** for a change to something that
  already existed (how it is made, how it behaves, where it is, removing
  it), 🔧 **v0.N.M.K** for a version of fixes only. A version with several
  things takes the number of the biggest. The result was **124 additions,
  77 changes and 45 fix-only versions**: the previous version,
  "Paragraph", which was v0.196, is now **v0.124**. The app has the new
  number too (Settings → Patch Notes and the version below)
- **[Change]** **[📱 Mobile]** **Every entry's platform**: **[📱 Mobile]**
  the phone app, **[💻 PC]**, **[🍎 Mac]**, **[🌐 Web App]**, more than one
  together or **[🌍 All]**. Put on all entries, the earlier ones too (731),
  which are all the phone app's; `[Project]` entries, about how it is made,
  don't have it
- **[Change]** **[📱 Mobile]** There are four labels: `[New feature]`,
  `[Change]` (before also `[Modified]` and `[Alteration]`), `[Removed]` and
  `[Bug fix]` (before also `[Bug]`). In the versions that are now changes,
  the entries that said "[New feature]" say "[Change]"
- **[Project]** How I decided: an addition is something that couldn't be
  done before (a view, a property type, an option, a menu, a widget);
  widening or moving something that existed (another entry where there
  already was one, colours, sizes, a different behaviour, removing it) is a
  change; a version of `[Bug fix]` only is a fix. The entries inside
  additions keep their label. References in the text ("see v0.N") have the
  new number, except inside the user's words and in the accounts of the
  numbering of the time (the old v0.131, v0.132 and v0.134). The old → new
  table, for earlier commits, is in "Old numbering", after the Changelog,
  outside the Patch Notes
- **[Project]** App: `PatchKind` has three kinds (🆕, ✏️, 🔧) and is read
  from the number (`PatchKind.of`); a version's title can have a
  four-part number, in the build and in the Patch Notes. `versionCode`
  stays the number of versions, so the phone accepts the update even with
  the lower number. Test: `PatchNotesTest`, with the three kinds, the
  numbers following the new rule, and every entry with its platform and a
  label suited to its version

### 🆕 v0.124 — 21/10/2026 · 00:59 — "Paragraph": the space between blocks and after numbers, bullets and checkboxes, as in Word
*(Claude Code on the PC; tested on the phone on a test page, with the database compared before and after; 728 tests passed)*

- **[New feature]** **[📱 Mobile]** **Settings → "Paragraph"**, under
  "Full line view", with the two sizes in points, chosen with "−" and "+":
  - **"Space between blocks"**: the extra space between one block and the
    next (from 0 to 32 pt; 0 is how pages always were), half above and
    half below every block, inside toggles too;
  - **"Space after number, bullet or checkbox"**: the space between a
    list's number, bullet or checkbox and the first character ("Default",
    as it always was, then from 0 to 32 pt). With **"Same for numbered,
    bulleted and to-do lists"** on it is a single one for all three lists;
    off, each has its own
- **[New feature]** **[📱 Mobile]** **"Paragraph" in every page's three
  dots**, under "Proofing": for each of the two sizes "As in Settings" or
  "Only this page", with the same choices as Settings
- **[New feature]** **[📱 Mobile]** **"Paragraph" for a group of lines**:
  from the new ¶ button of the bar above the keyboard (the lines touched by
  the selected text, or the caret's) and from the "Paragraph" entry of the
  block menu (six dots). "As in the page" or "Only these lines"; with
  several list types among the lines there is "same for all" there too. As
  in Word, the new lines made with Enter inside the group keep its sizes.
  Every choice shows right away in the page and is undone with Undo
- **[Note]** "Same for numbered, bulleted and to-do" I read as the space
  after the marker, the only one of the two sizes that concerns lists
  only. With a large space between blocks the caret becomes as tall as the
  line: it is the same limit as checkboxes (see `CHECKBOX_LINE_HEIGHT`)
- **[Project]** `data/entity/Paragraph.kt`: `ListGaps`, `PageParagraph`
  (column `pages.paragraphJson`, migration 58→59), `ParagraphLook` (the
  line wins, then the page, then Settings) and `spacingStartsOf`;
  `BlockAttrs.spacing` and `BlockAttrs.markerGap`, which `attrsForNewLine`
  passes to the new line; `AppSettings.blockSpacing` and `listGaps`. In the
  shared text field the space between blocks is **the line height of the
  whole field**, the road "Roads already tried" says is safe: a group with
  a space of its own becomes a field of its own, and `resolveRun` stops at
  the same boundaries as the screen. The space after the marker widens or
  narrows **only in width** the spaces already written in the marker
  (`MarkerGapScale`): no extra characters, the translation of positions
  stays the same. Windows in `ParagraphUi.kt`, labels in
  `ParagraphStrings`. In the debug build `ParagraphProbeActivity` makes the
  test page. Tests: `ParagraphTest`, `ParagraphLinesTest`,
  `ParagraphSettingsTest`, `MigrationTest` (with the `v58.sql` schema)

### 🔧 v0.123.0.1 — 21/10/2026 · 00:17 — "+ Row", "+ Column" and the trash can only while typing in the table
*(Claude Code on the PC; tested on the phone on the page clipped from a game guide; 719 tests passed)*

In a game guide page clipped with "Text and Images" there are six tables
("Previous / Next", the boss's weaknesses, the items of each floor), and
under each one the bar to edit it was always on, in the middle of the
article.

- **[Bug fix]** **[📱 Mobile]** **The bar under a table** ("+ Row", "+
  Column" and the trash can) **shows only while typing** in a cell of that
  table: you tap a cell and it appears, you close the keyboard and it goes
  away. It applies to all tables, not just the Web Clipper's
- **[Project]** `TableBlockContent` watches whether one of its cells has the
  caret (`onFocusChanged` and `focusGroup`). Test: `TableBarTest`

### 🆕 v0.123 — 20/10/2026 · 22:59 — The Web Clipper reads tables by rows, removes the advertising holes and has "Dark mode"
*(Claude Code on the PC; tested on the phone on a game guide page: "Text and Images", Full Page light and dark, Article dark; 718 tests passed)*

Seen on the phone (screenshots and structure of the two pages, asked by
the user): in the "Text and Images" clip the pieces were in the same order
as the site (the site's phone version also has the three maps in the
"Maps" section, after the boss), but **the tables were unrolled**: the
rows of the steps glued into a single block, a step's number at the end of
the previous sentence ("Exit the Station. 2"), and in card grids one
card's text above the next card's image ("Recommended level: 20" above
"Second Kalpa"). That is why everything looked scattered. On top of that
the four item tables were empty, and the Full Page photo (29222 pixels
tall) had a white hole in place of every advertising box.

- **[Bug fix]** **[📱 Mobile]** **"Text and Images" reads tables by
  rows**, like the page:
  - the **step tables** (number and what to do): one line per step, with
    the number in bold in front, "**1.** Exit the Station."; the notes
    under a step ("You can talk to the spirit…") stay under it; with an
    image at the top of the step, first the number ("**2.**"), then the
    image, then the text; "**Boss Fight**" and labels like "**1F**" in
    front of their content;
  - **card grids** (the chapter guides, the Kalpa areas): one card at a
    time, separated by a thin line, so every text stays with its image;
  - the header row ("Objectives") in bold;
  - horizontal lines inside a cell no longer become dividers
- **[Bug fix]** **[📱 Mobile]** **Text-only tables** (the items: "Obtained
  from", "Items", "Cache Cube"…) arrive with their text: cells are read
  from the written text, not from how they look, which on the phone
  sometimes came out empty
- **[Bug fix]** **[📱 Mobile]** The title of an index left out ("List of
  Contents") no longer stays on its own
- **[Bug fix]** **[📱 Mobile]** **The white holes of the Full Page photo**:
  notE removes the advertising boxes of the most common networks
  (AdThrive/Raptive, Google, Taboola, Outbrain, Ezoic…) and the advertising
  video player, and after scrolling closes every tall box left empty
  (neither text nor images, never inside a table). On that page: it goes
  from 17553 to 13625 pixels, the hole under the cover from 498 to 20, and
  all 41 images and 12 tables remain. "ad" classes are recognised as whole
  words ("head-container" isn't advertising) and the page itself is never
  hidden (AdThrive puts a class of its own there too)
- **[New feature]** **[📱 Mobile]** **"Dark mode"** in the Web Clipper
  panel, for Full Page, Region and Article: the site opens with the dark
  theme (those that have one use theirs) and Chrome darkens the others by
  itself; Article is laid out light on dark with the colours of Notion's
  dark theme. Before Android 13, where Chrome can't darken, colours are
  inverted (images aren't). The choice is remembered
- **[Project]** `web_clipper_reader.js`: `richTable`, `cell`, `TOC_TITLE`,
  `render(dark)`; `web_clipper_cleanup.js`: advertising (`adBox`, the
  `[data-note-hidden]` rule) and `__noteCollapseEmpty`; `WebClipEngine`
  with `dark` and `cleanUp(collapseEmpty)`; `WebClipRequest.dark`,
  `WebClipMode.isPicture`, `AppSettings.webClipDark`. Test:
  `WebClipperSheetTest`; the calculations of image resizing moved into
  `ImageFrameTest`

### 🆕 v0.122 — 20/10/2026 · 19:45 — Image handles only on request, and "Full resize" like a window
*(Claude Code on the PC; not yet tested on the phone; 717 tests passed)*

- **[New feature]** **[📱 Mobile]** **Image handles are no longer shown**
  (they had been there since 06/10/2026, always, on all of them).
  Long-pressing the image, at the top of the menu, above "View", there is
  **"Resize"**: the usual two handles appear on the sides, which widen and
  narrow the image keeping it centred and with its shape
- **[New feature]** **[📱 Mobile]** **"Full resize"**, right below: handles
  on **all four sides and on the four corners**, like a Windows window.
  Only the grabbed edge moves: the right one widens to the right, the left
  one moves the image's start, top and bottom change the height, the
  corners two edges together. The image fills the box (as in OneNote); it
  doesn't leave the page and doesn't become smaller than a finger
- **[New feature]** **[📱 Mobile]** While resizing, on the image there are
  **"Original size"** (the page's width, centred, with its shape) and
  **"Done"**. A tap on the image and the back gesture also finish. It is
  undone like every change; with the page locked the handles don't come
- **[Bug fix]** **[📱 Mobile]** **Large camera photos** (from 6 megapixels
  up) since v0.120 went to the long images' strips, which can't turn a
  photo taken upright. Now strips are only for truly long and narrow
  images (taller than 4096 pixels and two and a half times the width), like
  web page photos; the others are read as before. And an upright photo's
  shape is read already turned
- **[Project]** `ImageResize.kt` (`ImageFrame`, `frameOf`, `widened`,
  `dragged`, `withFrame`, `withOriginalSize`, `ResizeHandle`,
  `ResizeToolbar`), `BlockAttrs.left` and `aspect`,
  `MediaRequestKind.RESIZE` and `FULL_RESIZE`, `PageImageStore.imageSize`
  with the orientation, `StripImage` as tall as its box. Test:
  `ImageResizeTest`

### 🆕 v0.121 — 20/10/2026 · 19:24 — "Text and Images": a site's article as text and images, like OneNote
*(Claude Code on the PC; tested on the phone with the test activity, not yet by the user; 713 tests passed)*

- **[New feature]** **[📱 Mobile]** **"Text and Images"**, the Web
  Clipper's fifth choice: not a photo but **the article as notE lines**, in
  the page's order, in place of the first empty line (or at the bottom).
  At the top, like OneNote, the **large title** (24 points, bold: it is
  also an index entry), the note and "Clipped from" with the link; then the
  article:
  - **paragraphs** with their **bold**, italic, underline, strikethrough
    and tappable **links**;
  - **subheadings** in bold and larger (24 and 20 points: index entries
    too, v0.118);
  - **bulleted and numbered lists** with their indent, quotes, code,
    simple tables with the cells, dividers, and image captions in italic;
  - the **images**, downloaded into the app, as wide as in the page (those
    almost as wide as the column at full width)
- **[New feature]** **[📱 Mobile]** **What stays out**, as OneNote does:
  notE finds the article like browsers' "Reader" (the block with the most
  paragraphs of real text and few links, plus the siblings that have
  enough), so **cover, side columns, bottom bars, menus, comments and
  advertising** aren't there. More than OneNote it also leaves out the
  "Advertisement. Keep scrolling for more" boxes (OneNote had kept them)
  and **indexes made only of links** to the page's chapters (like
  OneNote). The article's title isn't repeated. On a game site's guide: 96
  blocks and 7 images out of 7, in 11 seconds, from the first paragraph to
  the last
- **[New feature]** **[📱 Mobile]** Images the page loads only when
  scrolling (an "image-loading" placeholder on some sites) arrive with
  their real address: notE scrolls the page and also reads the images'
  hidden addresses (`data-src`, the variants of the `<picture>` tag). They
  are downloaded as the browser downloads them, with the site's cookies: a
  site you are logged into gives its images too. While they download you
  see "Downloading the images (3 of 7)…"; stopped, those already
  downloaded don't stay
- **[Bug fix]** **[📱 Mobile]** Google's cookie banner ("Do not consent")
  is refused by itself like the others; before it was only hidden
- **[New feature]** **[📱 Mobile]** **"Article"** also uses the same way
  of finding the article: the cover is no longer there either
- **[Project]** `assets/web_clipper_reader.js` (`noteReader`: `render` for
  "Article", `serialize` for "Text and Images"; it replaces
  `web_clipper_article.js`), `WebClipEngine.readContent`,
  `WebClipContent.kt` (`parseClipContent`, `ClipPiece`, `toNewBlocks`,
  `headingSize`), `WebClipDownloads`, `NewBlock`,
  `PageRepository.insertAtFirstEmptyLine` (several blocks in a row, with
  the tables' cells; the photo goes through it too) and
  `PageEditorViewModel.insertWebClipBlocks`. Tests: `WebClipContentTest`,
  and in `WebClipPlacementTest` the article inserted with a list, an image
  and a table

### 🆕 v0.120 — 20/10/2026 · 18:59 — The Web Clipper, like OneNote: a photo of a site in the page
*(Claude Code on the PC; tested on the phone with the test activities, not yet by the user; 709 tests passed)*

- **[New feature]** **[📱 Mobile]** **"Web Clipper"** in every page's
  three-dots menu (not in databases; off with the page locked). The panel
  opens like OneNote Web Clipper's: the **link** (pasted with the button
  next to it or typed, even without "https://"), **"What do you want to
  capture?"** with **Full Page**, **Region**, **Bookmark** and **Article**,
  then **Title** (it fills itself with the site's title), **Note** and
  **Source**. **No "Location"**: removed, as asked. "Clip" turns on only
  with a real link
- **[New feature]** **[📱 Mobile]** **Full Page**: notE opens the site
  **as on the PC** (1440 pixels wide, like a laptop: the width at which the
  test site comes out as in OneNote's photo, left column included; light),
  scrolls it to the bottom so that the images that load only when
  scrolling arrive, and photographs it from top to bottom. While it does,
  you see the page scrolling, with the steps written and the bar
  advancing; "Cancel" stops everything. The test page: 2160 × 19023 pixels
  in 13 seconds, 4 MB
- **[New feature]** **[📱 Mobile]** **Pages come out whole** (question 3):
  the photo is written to the file **in strips** while the page scrolls,
  never being all in memory (whole, that page would be 160 MB), and is
  shown **in pieces**, reading from the file only those on the screen. Only
  a page taller than 65535 pixels (the JPEG format's limit, more than forty
  PC screens) is photographed a bit smaller, but always whole
- **[New feature]** **[📱 Mobile]** **Cookie banners** (question 2): before
  the shot notE taps **"Reject all"** / "Rifiuta" by itself (the choice
  that protects most), then hides the remaining banners (even those closed
  in a separate piece of page), the dark veil behind, the fixed bars at
  the bottom (advertising, chat) that would end up in the middle of the
  page in the photo, and unlocks scrolling. The site's bar at the top
  stays, at the top. Tested on the user's test page: banner and
  advertising bar gone
- **[New feature]** **[📱 Mobile]** **Region**: the site full screen as on
  the PC, to scroll and zoom as in a browser; "Select" stops the page and
  with the finger you draw the rectangle (outside it darkens), "Clip"
  photographs it as sharp as the whole page. "Scroll" goes back to
  scrolling
- **[New feature]** **[📱 Mobile]** **Article**: notE keeps only the
  article (title, text, images, tables), without menus, advertising,
  columns and comments, laid out cleanly like a book in a column to read,
  and photographs it whole
- **[New feature]** **[📱 Mobile]** **Bookmark**: not a photo but notE's
  "Web bookmark", with the site's title, description and image
- **[New feature]** **[📱 Mobile]** **Where it goes**: in place of the
  page's **first empty line, from the top** (even inside an open toggle);
  if there is none, at the bottom. Under the photo: the title in bold, the
  note (a line for each of its lines) and, small and grey, "Clipped from"
  with the tappable link. The page scrolls to the photo, which lights up
  for a moment. It is undone like every change
- **[New feature]** **[📱 Mobile]** **The photo in the page** is as wide as
  the page and as tall as it wants, sharp (in strips, see above); the
  handles on the sides narrow it like other images. **Tapping it** opens
  it full screen as wide as the screen, at the top: it scrolls with the
  finger **with momentum**, zooms with two fingers up to eight times, with
  two taps it zooms there or goes back; the X and the download are on a
  dark circle, which is visible over a white page. It applies to all very
  tall images
- **[New feature]** **[📱 Mobile]** **"Move to line…"** (question 1): in
  the menu of an image (and of bookmarks, files, dividers, subpages and
  databases), next to "Move up" and "Move down", the list of the page's
  lines indented as there, plus "Top of the page": tapping one the block
  goes right under it, even inside a toggle. Before there were only "Move
  up" and "Move down", one place at a time, and "Move to" towards another
  page
- **Paywalls** (question 4): bypassing them, no: it would mean getting
  around a site's subscription. If you have the subscription, you log in
  with your own account in "Region" (the site is used as in a browser) and
  notE remembers it: the later "Full Page" and "Article" photos also see
  the page as the user sees it
- **[Project]** `data/webclip`: `WebClipEngine` (the "PC" WebView, its
  sizes in real pixels, `prepare`, `captureFullPage` which scrolls one
  window at a time and draws with `WebView.draw`, `captureRegion`,
  `keepOnlyArticle`), `JpegStripWriter` (baseline 4:4:4 JPEG written in
  strips, with the standard tables), `WebClips.kt` (`webClipUrl`,
  `webClipCaptions`). The scripts in `assets/web_clipper_cleanup.js` and
  `web_clipper_article.js`. `PageRepository.insertWebClip` and
  `moveBlockAfter`, `PageLines.kt` (`inReadingOrder`, `firstEmptyLine`,
  `moveTargetsFor`), `PageImageStore.newFile`/`imageSize`, `TallImage.kt`
  (`StripImage`, `TallImageViewer` with `BitmapRegionDecoder`),
  `WebClipper.kt` (the panel, `WebClipCaptureDialog`,
  `WebClipRegionDialog`), `MoveToLineSheet`, `WebClipStrings` in the 8
  languages. Discovered on the phone:
  `WebView.enableSlowWholeDocumentDraw()` made photos come out white, and
  `WebView.getScale()` stayed stuck at 3.5: they aren't used. In the debug
  build `WebClipProbeActivity` and `WebClipUiProbeActivity` test everything
  from adb without touching the pages. Tests: `JpegStripWriterTest` (the
  JPEG read back by Java's decoder), `WebClipsTest`, `PageLinesTest`,
  `WebClipPlacementTest`, `WebClipperSheetTest`

### 🆕 v0.119 — 20/10/2026 · 17:55 — Favorite colours, under the last colours used
*(Claude Code on the PC; not yet tested on the phone; 693 tests passed)*

- **[New feature]** **[📱 Mobile]** **The star** in the colour picker, to
  the right of the preview ("Aa" with the colour's name and code): tapped,
  the chosen colour goes among the **favorites** and the star turns
  yellow; tapped again (on a favorite's colour) it removes it. The picker
  is the same across the app: text, cells, tags, pens, bars
- **[New feature]** **[📱 Mobile]** **"Favorite colors"**, the row of
  favorites **under "Recent colors"**: the circles are as big as the
  recent ones and line up with them, in the order they were added (the
  last at the end); beyond ten the row scrolls sideways. Tapping one the
  picker goes to that colour, and it is applied with "Apply" like the
  recent ones. They stay until removed (up to 40); without favorites the
  row says to tap the star
- **[Project]** `AppSettings.favoriteColors` and `toggleFavoriteColor`
  (saved in `favorite_colors`), `withFavoriteColorToggled`; in
  `ColorPickerSheet` the star, `FavoriteColorsRow` and the `ColorSwatch`
  circle shared with the recent ones; the texts in the 8 languages. Test:
  `FavoriteColorsTest`

### 🔧 v0.118.2.1 — 20/10/2026 · 15:17 — A column narrowed by hand stays narrow even with "Widen the column"
*(Claude Code on the PC; not yet tested on the phone; 691 tests passed)*

- **[Bug fix]** **[📱 Mobile]** **Narrowing a column with the handle**
  ("Resize column") in a table with "Long text" on **"Widen the column"**:
  while dragging the column narrowed, but on release it went back as wide
  as its content, because "Widen the column" widened every column up to
  there. Now it widens only the columns without a size chosen by hand: the
  one narrowed with the handle stays as it is, and the text that doesn't
  fit ends with the three dots. The other columns widen as before
- **[Bug fix]** **[📱 Mobile]** Still with "Widen the column", brought back
  to the usual width (where the handle snaps) the column stays there,
  instead of widening back to the content
- **[Project]** `tableColumnWidth` in `CellText.kt` decides which size wins
  (the finger, "Fit to content", the chosen size, the usual one, and when
  to widen); `DatabaseViewModel.setColumnWidth` with "Widen the column"
  also remembers the usual width. Test: `WrapCellsTest`

### ✏️ v0.118.2 — 20/10/2026 · 14:33 — "Select all" selects the whole page, as in Notion
*(Claude Code on the PC; not yet tested on the phone; 689 tests passed)*

- **[Change]** **[📱 Mobile]** **"Select all" veils the whole page in
  blue**: from the bar above the keyboard and from the text menu (the one
  that appears long-pressing). Before it reached only the edges of the
  50-line piece of text (since v0.113.1), and a text selection doesn't go
  over a database or an image anyway. The keyboard closes, and at the
  bottom there is a bar with **Copy**, **Cut** and **Delete** (in a locked
  page only Copy) and the ✕. A tap on the page, the ✕ or Back remove the
  selection; the page scrolls as always
- **[Change]** **[📱 Mobile]** **Copy** puts the whole page on the
  clipboard, in reading order: bold, italic, colours and links stay, and
  pasted into notE every line comes back with its type (text, bulleted or
  numbered list, checkbox ticked or not) and its indent. Callouts, quotes
  and code give one line for each of theirs; tables one line per row, with
  the cells separated by a tab (as a spreadsheet copies them); dividers
  "---"; images and files their address or name
- **[Change]** **[📱 Mobile]** **Cut** copies and then removes everything;
  **Delete** removes everything, after a question. **Subpages and
  databases stay** (and the toggles containing them): they live elsewhere
  too, and are removed one at a time from their menu. Both can be undone
- **[Change]** **[📱 Mobile]** In the page title and in the cells of the
  databases inside the page the text menu's "Select all" takes only that
  field, as before. Ctrl+A on a real keyboard selects the field's text
- Databases with thousands of rows (the question about the dictionary):
  **full page all the rows already scroll**, without "Load more", since
  v0.113.2; only the board has 50 cards per column there too. Inside a
  page the first 50 and "Load more" stay. Neither makes the app heavier
- **[Project]** `PageSelection.kt` (`WholePageTextToolbar`,
  `PlainTextToolbar`, `pageVeil`, `tapToEndPageSelection`,
  `copyPageToClipboard`, `PageSelectionBar`),
  `PageEditorViewModel.pageCopyLines` and `deletePageContent`,
  `ClipboardBarButtons(onSelectAll)`. Tests: `PageSelectAllTest`,
  `PageSelectionTest`

### ✏️ v0.118.1 — 20/10/2026 · 13:59 — "Keep Source Formatting" brings sizes, lists and tables from OneNote and the web
*(Claude Code on the PC; not yet tested on the phone; 685 tests passed)*

- **[Change]** **[📱 Mobile]** **Text size**, with "Keep Source
  Formatting": the size most used in the copied text becomes notE's normal
  one, and the rest in proportion. OneNote's normal text (11 points) stays
  normal; a heading at 20 points becomes 29, as large as it was there
  relative to the rest. From the web, text that doesn't state its size is
  worth browsers' usual one, and headings (H1–H6) arrive large and bold as
  on the page. A line that arrives entirely at 20 points or more acts as a
  landing point of the index (v0.118)
- **[Change]** **[📱 Mobile]** **Bulleted and numbered lists** arrive as
  notE lists, with their indent (a list inside another), without the
  bullets or numbers written as text
- **[Change]** **[📱 Mobile]** **Tables** arrive as notE simple tables,
  with the rows, the columns and the text split cell by cell as in the
  copied table (from OneNote or from a web page, headers included). A
  single-cell "table", which on sites only serves for layout, arrives as
  text. Cells are text only, as always in simple tables
- **[Change]** **[📱 Mobile]** Lines, deliberately empty paragraphs and
  links arrive as they were in the source. "Paste", "Match Destination
  Formatting" and "Keep Text Only" stay as before, and from notE to notE
  the line type arrives as before
- **[Project]** `ClipHtml.kt`: `parseClipHtml` (lines, lists, tables,
  styles and sizes from the clipboard's HTML; the font written on the
  `<body>` by OneNote applies to everything), `noteSizes`.
  `LinkClipboard.sourceFormatted` with the `pasted-table` label for the
  cells, `StyleRange.size`, `PastedLine.table`, and in
  `PageEditorViewModel.updateRun` the line becomes a table
  (`writePastedCells`). Tests: `ClipHtmlTest`, `PasteSourceStructureTest`

### 🔧 v0.118.0.2 — 20/10/2026 · 13:46 — The line at the end of the database goes back under the database
*(Claude Code on the PC; not yet tested on the phone; 678 tests passed)*

- **[Bug fix]** **[📱 Mobile]** **The thin line at the end of a database
  inside a page** (v0.112, 18/10/2026) was **at the top** of the database
  instead of below: in a row's page two lines showed at the top, the
  properties' one and this one. Now it is below again, separating the
  database from the text that follows. The fault came from v0.114 (19/10,
  "Full line view"): every block on its own was in a box that places its
  pieces one **on top of** the other, and the database and its line ended
  up in the same place. Now the pieces are one **under** the other, as
  before, for all blocks on their own
- **[Project]** In `PageEditorScreen` the islands' container is a `Column`,
  not a `Box`

### 🔧 v0.118.0.1 — 20/10/2026 · 13:39 — Numbers in text are sorted as numbers: 2 before 10
*(Claude Code on the PC; not yet tested on the phone; 678 tests passed)*

- **[Bug fix]** **[📱 Mobile]** **Sorting by a text** (the rows' name, a
  text property, the title of a linked page…) the numbers inside count for
  what they are: 1, 2, 3… 9, 10, 11, and no longer 1, 10, 11… 19, 2, 20.
  It also applies inside the text: "Day 9" before "Day 10", "v0.9" before
  "v0.10". Upper and lower case still don't count, and numbers come before
  letters as before
- **[Bug fix]** **[📱 Mobile]** The same order for the **groups** of a
  table or a board grouped by a text property
- **[Project]** `NaturalOrder` in `data/NaturalOrder.kt` (for ties, like
  "01" and "1", the previous comparison decides, so the order stays
  fixed), used by `DatabaseViewModel.compareSortValues` and by
  `buildRowGroups`. Test: `NaturalOrderTest`

### 🆕 v0.118 — 20/10/2026 · 13:01 — The navigable index: links to a line of the same page
*(Claude Code on the PC; not yet tested on the phone; 674 tests passed)*

- **[New feature]** **[📱 Mobile]** **The pin**: in the six-dots menu of a
  text line (also callout, quote, toggle, code), **"Pin as link target"**.
  The line has a small blue bookmark in the left margin; from the same
  menu, "Unpin link target" removes it. It can be undone
- **[New feature]** **[📱 Mobile]** **Large lines act as headings by
  themselves**: a line written entirely at 20 points or more (Aa bar) is
  already a landing point, without a pin. Headings H1, H2 and H3 have been
  gone since 24/09/2026, so the "heading" is large text
- **[New feature]** **[📱 Mobile]** **"In this page"** in the link window,
  next to "Web": the list of pinned lines and large ones, in the page's
  order and indented as there (bookmark for the pinned ones, "T" for the
  large ones). You select the index entry, tap the link icon, "In this
  page", and a tap on the line makes the link. Without lines to link to,
  the tab says how to make one
- **[New feature]** **[📱 Mobile]** **The jump**: tapping the linked entry
  the page goes **right away** to that line, at the top under the floating
  buttons, and the line lights up for a moment like a moved line. At the
  bottom **"Back"** appears (with the arrow towards where you return),
  which brings the page back where it was; it goes away by itself after
  ten seconds. A line inside a toggle leads to the toggle
- **[New feature]** **[📱 Mobile]** The link is **blue and underlined like
  the others, with a small arrow after the last letter**, so it can be told
  apart from one to a site. It follows the line and not the text: renamed
  or moved it is found again; deleted, tapping the entry a notice says it
  is gone
- **[New feature]** **[📱 Mobile]** Blocks on their own (callout, toggle,
  quote, code) now also light up when moved with "Move up" and "Move
  down", like text lines
- **[Project]** `BlockLinks.kt` (`note://block/<id>`, `blockLink`,
  `blockLinkTarget`, `LinkTarget`, `isLargeLine`, `linkTargetsOf` in
  reading order), `BlockAttrs.linkTarget`, `PageAnchors.kt`
  (`LocalBlockLinkJump`, `lineStartIn`/`lineTopIn`, `drawPinRibbon`,
  `drawBlockLinkArrows` with the `markBlockLinkEnd` space, `anchorDecor`,
  `LinkBackButton`), `VisibleRunLines.lineTops` (where a line is inside a
  group, for the jump),
  `PageEditorViewModel.setLinkTarget`/`linkTargets`/`highlightBlock`,
  `rememberLinkOpener` which jumps instead of opening the browser,
  `LinkDialog` with the tabs and `LinkTargetList`; the texts in the 8
  languages. Tests: `BlockLinksTest`, `PageLinkTargetTest`,
  `LinkDialogTargetsTest`

### 🆕 v0.117 — 20/10/2026 · 10:24 — The scroll bar on the right of pages
*(Claude Code on the PC; not yet tested on the phone; 664 tests passed)*

- **[New feature]** **[📱 Mobile]** **The scroll bar** on the right edge of
  pages: thin, light (dark with the light theme), with rounded ends, as in
  the phone's settings. It appears while the page scrolls and fades a bit
  less than a second after it stops. It is as tall as the visible part of
  the page (never less than a finger of page), at the top and bottom it
  touches the edge, and it doesn't take taps: below it you keep typing and
  tapping as before. It isn't there if the page fits entirely on the
  screen
- **[New feature]** **[📱 Mobile]** Pages are made of items of different
  heights (pieces of text, images, databases) that are measured only when
  they are visible: the bar remembers the height of those already seen and
  gives the average to the others, so it is exact where you have already
  been and settles while scrolling
- **[Project]** `PageScrollIndicator.kt`: `PageScrollIndicator` (above the
  page's list in `PageEditorScreen`, next to "Full line view"'s side bar),
  `ItemSizes`, `estimateExtent`, `scrollThumb`. Test:
  `PageScrollIndicatorTest` (the average for items never seen; height,
  minimum, top and bottom; it appears scrolling and fades after)

### 🔧 v0.116.0.2 — 20/10/2026 · 08:58 — "Today" moves to the next matchday at midnight
*(Claude Code on the PC; not yet tested on the phone; 661 tests passed; commit only local)*

- **[Bug fix]** **[📱 Mobile]** **"Today" no longer keeps the finished
  matchday the day after**: until 23:59 of the day of the last match it
  stays on that matchday, from midnight it goes to the next one. In the
  example: Monday 19/10 Serie A's seventh, from Tuesday 20 the eighth.
  Opening the widget, instead, on Tuesday you still see the results of the
  matchday finished on Monday, as before
- **[Project]** `todayRound` in `Sports.kt`, like `currentRound` but
  without the day after; "Today" in `CompetitionRounds` uses it. Test:
  `SportsTest` (the example's dates)

### 🔧 v0.116.0.1 — 20/10/2026 · 08:57 — The selection no longer stops between one piece and another of long pages
*(Claude Code on the PC; verified on the phone by the user on 20/10/2026; 661 tests passed; commit only local)*

The request, summarised (the conversation it was written in contained
private data and isn't reread): holding and dragging, the selection
always stopped at the same line of a long page.

- **[Bug fix]** **[📱 Mobile]** Since v0.113.1 long pages are split into
  pieces of 50 lines, and every piece is a text field of its own: a field
  can't select beyond its lines, so the selection stopped at the boundary
  between two pieces, about every 50 lines. Now, when the page is still
  and no finger touches it, the lines on the screen are in a single field,
  and the selection goes up and down the whole screen. The field being
  typed in isn't touched while typing: the lines join two seconds after
  the last letter. Once the field is left, the page goes back to being
  split into small pieces, so typing stays fast
- **[Bug fix]** **[📱 Mobile]** The first road was discarded, joining the
  pieces during the selection: dragging the blue handle, the handle
  stopped at the boundary and the magnifier stayed stuck on the screen
- **[Project]** `VisibleChunks.kt`: `VisibleRunLines`,
  `LocalVisibleRunLines`, `LocalPageFingerDown`, `watchPageFingers`,
  `KeepVisibleLinesTogether`. `PageEditorViewModel`: `joinChunkAfter`,
  `keepVisibleTogether` (the focused field stays and takes the lines above;
  `TYPING_QUIET_MS`, 2 seconds; `FOCUSED_CHUNK_CAP`, 200 lines), `rechunk`
  doesn't put boundaries between the lines on the screen, `lastTypedAt`.
  `PageEditorScreen`: lines that reach the bottom or the top join right
  away, `joinsAbove`/`joinsBelow`. Test: `ChunkSelectionTest`
- Tested by the user on the phone, with the v0.116 release: it works

### 🆕 v0.116 — 20/10/2026 · 08:29 — "Freeze Properties Row", "Today" in the sports widget and two-colour tags
*(Claude Code on the PC; verified on the phone by the user on 20/10/2026; 654 tests passed; commit only local)*

- **[New feature]** **[📱 Mobile]** "Freeze first row" is now called
  **"Freeze Properties Row"** ("Blocca la riga delle proprietà" in
  Italian, and likewise in the other languages): it is the row with the
  properties' names, not a row of the database. It does the same thing as
  before
- **[New feature]** **[📱 Mobile]** **"Today" in the sports widget**: a
  small calendar to the left of the refresh arrow. Tapping it, the
  competition shown goes back to today's matchday (or the next one, if
  there are no matches today), scrolling there; if you were looking at a
  past season, it also goes back to the current season. If the matches
  are old, they are downloaded again
- **[New feature]** **[📱 Mobile]** **Two-colour tags**: when editing a
  tag, under "Color", **"Add a second color"** opens the same spectrum as
  the first. The pill has the first colour in the left half and the second
  in the right half, wherever the tag shows: in cells, in the tags window,
  in filters, in the groups of the table and the board. Then, below:
  "Second color" to change it, **"Line between colors"** (off by default,
  the colours stay attached; on, a vertical line in the page's colour
  separates them) and "Remove second color". The label is white or black
  according to the average of the two colours. In the board with "Color
  columns" the column takes the first colour's veil
- **[Project]** `DbStrings.freezeFirstRow` only changes the text (the
  database's `freezeFirstRow` field stays). `SportsUpdates.goToToday` and
  `todayRequests` (the taps on "Today", per widget), `SportsTodayButton`,
  and in `CompetitionRounds` the scroll to `currentRound`'s matchday at
  every tap. `SelectOption.secondColor` and `colorLine` (tags saved before
  are read with a single colour), `DatabaseViewModel.setSelectOptionSecondColor`,
  `onSetOptionSecondColor` along the same road as `onMoveOptions`,
  `TagChip` with the two halves drawn behind the label,
  `RowGroup.secondColor`, `TagColorRow`, `darkTextOn(first, second)`;
  `DbStrings.addSecondColor`, `secondColor`, `lineBetweenColors`,
  `removeSecondColor`. Test: `TagSecondColorTest` (the second colour from
  the spectrum; the line and removing; the label on the average; the
  earlier tags)
- Tested by the user on the phone, with the v0.116 release: it works

### 🆕 v0.115 — 20/10/2026 · 08:17 — Tags are reordered with handles
*(Claude Code on the PC; verified on the phone by the user on 20/10/2026; 650 tests passed; commit only local)*

- **[New feature]** **[📱 Mobile]** **"Reorder"** ("Riordina" in Italian)
  at the top right of the tags window, next to the "Select" or
  "Multi-select" title, when there are at least two tags. Tapping it, the
  tags all appear in a row, each with the three-line handle on the right:
  you hold it and drag it, and the other tags make room as soon as the tag
  reaches the middle of one of them. The dragged tag lifts from the others
  with a shadow. "Done" goes back to the usual list
- **[New feature]** **[📱 Mobile]** The order is saved at every release:
  closing the window loses nothing. With many tags, bringing the dragged
  one near the window's top or bottom edge, the list scrolls by itself
- **[New feature]** **[📱 Mobile]** The order applies wherever tags are
  shown in a list: in the tags window, in filters, and in the groups of the
  table and the board grouped by that property, which follow the tags'
  order as in Notion. Cells don't change
- **[Project]** `TagReorderList.kt`: `TagReorderList` (the order on the
  screen counts more than the database's, which arrives a moment after
  release, so the tag doesn't go back for a moment where it was; the
  automatic scroll near the edges) and `moveWhileDragging` (past which
  neighbours the tag went). `SelectOption.reorderedAs` in
  `DatabaseColumnEntity.kt` (a tag missing from the order goes at the end,
  a deleted one isn't reborn), `DatabaseViewModel.moveSelectOptions`, and
  `onMoveOptions` going down to `TagPickerSheet` along the same road as
  `onDeleteOption`. `DbStrings.reorder` and `dragToReorder`. Test:
  `TagReorderTest` (the new order; past the neighbour's middle and not
  before; a short drag saves nothing; the second starts from the saved
  order; "Done" goes back to the list)
- Tested by the user on the phone, with the v0.116 release: it works

### 🆕 v0.114 — 19/10/2026 · 23:20 — Long lines no longer wrap
*(Claude Code on the PC; verified on the phone; 643 tests passed; commit only local)*

- **[New feature]** **[📱 Mobile]** **Settings → "Full line view"**
  ("Visualizzazione totale" in Italian), under "Title position", on by
  default: long lines of pages no longer wrap, and with the finger they
  scroll sideways to their end. The lines scroll **all together**, as on a
  wide sheet, and stay aligned with each other. The title, the properties,
  databases, images and the other blocks that aren't text stay still. It
  applies to the page's text (paragraphs, lists, checkboxes) and to that of
  toggles, callouts, quotes, notifications and code, which scrolls inside
  its box. Off, everything is as before
- **[New feature]** **[📱 Mobile]** **Made so as not to have OneNote's
  defects**:
  - the finger goes vertically **or** sideways, never diagonally: the first
    direction it moves in decides for the whole gesture;
  - sideways it goes only as far as the longest line among those on the
    screen ends, plus a bit of air, never beyond;
  - while typing, the caret always stays in view sideways too; going to a
    new line goes back to the start of the lines;
  - while scrolling sideways a blue bar appears at the bottom saying where
    you are, and fades shortly after;
  - a database table that has reached its edge doesn't drag the text
    around it along;
  - pen strokes on the text move with the text; while drawing the lines go
    back to the start
- **🔴 [To review]** **Scrolling down towards shorter lines, the page goes
  back sideways by itself**, just enough to see the end of the longest line
  on the screen, instead of leaving the screen empty. **The alternative**:
  staying where you are until you move it with the finger, like OneNote
  and code editors. Going back up you would find the same position, but
  you can end up on an empty screen
- **[Project]** `FullLineView.kt`: `SideScroll` (the common offset, in px,
  and `reach`, how far you can go, counted on the visible list items),
  `SideLane` (every text field is a lane: it is measured without a width
  limit and placed shifted; the offset is read only at the moment of
  placing, so scrolling sideways redoes neither the text nor its
  measurement), `SideResponder` (the caret in view sideways too; a
  rectangle wider than the space, that is the whole field, moves nothing),
  `KeepSidewaysInside` on the islands, `SideScrollEffects`,
  `SideScrollIndicator`. `AppSettings.fullLineView` (`full_line_view`),
  `Strings.fullLineView`/`fullLineViewNote`, `InkPageState.slidingItems` and
  `sideOffset`. Test: `FullLineViewTest` (lines don't wrap; sideways to the
  end of the line and not beyond, with the still box staying still; the
  first direction decides the gesture; gone down among short lines it goes
  back to the start; the caret stays in view and a new line goes back to
  the start; the setting)
- Tested on the phone. In a long page of the user, only looked at and
  scrolled: long lines no longer wrap, they scroll sideways to the end of
  the longest line on the screen, vertically the page doesn't move
  sideways, and going back up to the top it goes back sideways just enough.
  In a test row of a database: a long line written with the keyboard, with
  the caret always in view; a new line goes back to the start; dragged
  sideways with the keyboard open; a callout with a long text scrolls
  inside its box, aligned with the text above. The first bar, grey and just
  above the system bar, looked like a second system bar: now it is blue, on
  a track, higher up. On the release, ten sideways drags in the long page:
  295 frames, half under 6 ms, 95% under 9 ms, none slow because of the
  app. Test row deleted: database, settings and widgets identical to before

### ✏️ v0.113.3 — 19/10/2026 · 22:44 — Copied checkboxes are pasted as checkboxes
*(Claude Code on the PC; verified on the phone; 637 tests passed; commit only local)*

- **[Change]** **[📱 Mobile]** **Copying lines from notE, each brings along
  its type**: checkboxes (ticked or not), bulleted and numbered lists and
  the indent. Pasting them into notE they come back as they were: pasted
  lines that had been copied whole take back their type, and the indent is
  counted from that of the line pasted into. Pasting into an empty line it
  too becomes what it was; pasting after some text, the first copied line
  joins that text and stays as it is. A line copied only in part (the
  selection starting or ending halfway) is text as before. It applies with
  the copy of the bar above the keyboard, with the menu's "Copy" and with
  cut, and for paste in all modes except "Keep Text Only". Inside a toggle
  too
- **[Project]** The line type goes to the clipboard in an HTML comment
  (`<!--notE-lines:CHECKBOX,1,0;…-->`, `x` for a line taken in part),
  which other apps don't show: for them the copied text is the usual one.
  `PageEditorViewModel.selectedLines` (the type of the selection's lines),
  `CopiedLine` and `PastedLine` in `RichText.kt`, `PasteFormat.lines`;
  `LinkClipboard` writes and rereads it (`PASTED_LINES_TAG`).
  `writeRunChange` gives the type to new lines and to the one pasted into
  when it is exactly one whole copied line; `splitBlockAtMultiple` the
  same inside toggles (not to a toggle's title). Tests:
  `PasteLineTypesTest`, and in `ClipboardBarTest` the clipboard round trip
  and text-only lines, which are copied as before
- Tested on the phone in a test row of a database: three checkboxes milk
  (ticked), bread and eggs, copied and pasted into an empty line further
  down, came back as three checkboxes with milk ticked; in the database the
  new lines were CHECKBOX with the right tick. Test row deleted: database,
  settings and widgets identical to before

### 🔧 v0.113.2.1 — 19/10/2026 · 17:53 — The page follows the caret and no longer jumps
*(Claude Code on the PC; verified on the phone; 630 tests passed; commit only local)*

- **[Bug fix]** **[📱 Mobile]** **The page moves only if the keyboard
  would cover the caret, and only as much as needed.** Tapping a line in
  the middle of the page, the page went up until it brought the start of
  its piece of text to the top; joining with backspace a line to the one of
  the piece above, it went down until it put the end of that piece just
  above the keyboard. The fault of v0.113.1: long pages are split into
  pieces of 50 lines, and when a text field takes the caret (or the
  keyboard shortens the screen) Compose brings into view **the whole
  field**, that is the whole piece. Before, with a single field as tall as
  the page, it wasn't noticeable. Now whoever scrolls the page hears where
  the **caret** is, not the piece: if the caret is visible, the page stays
  still; if the keyboard would cover it, it goes up until the caret is just
  above the bar; going to a new line at the bottom it follows it
- **[Project]** `CaretInView.kt`: `followCaretOnly` on the field's box (a
  `BringIntoViewResponder` that answers anyone asking to bring something
  of the field into view with the caret's rectangle) and `CaretAnchor`, a
  placeholder as big as the caret which the page keeps in view when the
  keyboard rises, instead of the whole field. For the latter it uses
  `ModifierLocalFocusedBoundsObserver`, internal to Compose: a Compose
  update could break it, and the build would say so. The caret has 12 dp
  of space below (`CARET_KEYBOARD_MARGIN`) and above that of the floating
  buttons. Test: `CaretInViewTest` (without: the page jumped by more than
  250 dp; with: a tap in view moves nothing, a keyboard not covering the
  caret neither, one covering it makes it go up as much as needed)
- Tested on the phone in a test row of a database with 120 lines (three
  pieces): tapped line 63, mid screen, the page stays still (before it
  went up to line 51); backspace at the start of line 51 joins it to line
  50 and the page stays still; tapped line 84, which the keyboard would
  have covered, the page goes up and puts it just above the bar; three
  new lines while typing, the page follows the caret. Test row deleted:
  database, settings and widgets identical to before. In a long page of
  the user only two lines tapped, without typing

### ✏️ v0.113.2 — 19/10/2026 · 17:28 — Databases with ten thousand rows
*(Claude Code on the PC; verified on the phone with a test database of 10,000 rows; 627 tests passed; commit only local)*

After v0.113.1 the user had asked that the app bear "a lot of text and
databases", thinking of a word list that could reach ten thousand entries,
even in a simple database. A database drew all its rows at once: with ten
thousand that meant seconds to open it and redoing them all at every
change. Two roads were proposed, and the user chose the first.

- **🔴 [To review]** **Databases like Notion: inside a page the first 50
  rows and "Load more"**, which adds 50 more, with the number of those
  left out next to it. It applies to the table (and to every group and
  subgroup, each on its own), the list and the gallery. A row created with
  "New page" is always shown, even beyond the limit. Coming back to the
  page it starts again from the first 50. Calendar, timeline and chart
  stay as before. **Full page they all scroll**, without a limit: table,
  list and gallery are a list that draws only the rows on the screen, and
  "New page" is at the bottom of the list ("Freeze first row" keeps the
  header still, one per group). **The board** has the first 50 cards per
  column and "Load more" full page too, as in Notion: it isn't a list that
  draws only what is visible.
  **The alternative, if this isn't good**: all rows scrollable inside the
  page too, without "Load more", drawing only those on the screen as full
  page. From outside nothing would change, but it is much more work:
  inside a page the database is a piece of the page's list, and its rows
  would have to become pieces of that list, with groups, still headers,
  calculations and the columns' horizontal scrolling to redo
- **[Change]** **[📱 Mobile]** **Typing in a large database no longer
  stops the screen**: the filter, search and sorting of all the rows,
  which are redone at every letter typed in a cell, now run in the
  background and not on the thread that draws; and the tree of sub rows,
  when no row has any, is no longer built
- **[Change]** **[📱 Mobile]** **The red label "🔴 [To review]"** in the
  Changelog, asked by the user: it marks a choice made between two, to be
  looked at again later, with the other road written next to it. In the
  app's Patch Notes the label is red; on GitHub 🔴 gives the colour
- **[Project]** `RowLimits.kt` (`RowLimits`, `LoadMoreButton`, `LazyBody`,
  `LAZY_FULL_PAGE_LAYOUTS`), `DatabaseViewModel.createdHere`; in
  `DatabaseViewScreen.kt` the `lazy` branch of `TableLayout` (`LazyColumn`
  inside the horizontal scroll, `stickyHeader` for "Freeze first row"),
  `ListRow`, `GalleryRow`, `GroupNewRowButton`, and the limit in
  `BoardColumnCards`. The table's calculation with
  `flowOn(Dispatchers.Default)`; `rowTree` and `hasSubRows` return right
  away without sub rows. `REVIEW_TAG` in `PatchNotesScreen.kt`. Tests:
  `DatabaseRowLimitsTest` (1,200 rows in a real database: the first 50 and
  "Load more"; full page only those on the screen made, in the list too;
  created rows always visible), `PatchNotesTest.theReviewTagIsRed`. For
  the test on the phone, a command of the test version only filled a
  database called "Prova velocita" (no other) with rows: removed after the
  test
- Tested on the phone, in a simple database "Prova velocita" of 10,000
  rows inside a test row of a database: in the page the first 50 and "Load
  more 9950", which brings 51, 52…; full page scrolling fast only 0.8% of
  frames late (test version); typing and deleting 30 letters in a row's
  name (release), full page 5 ms median and 17–25 at most (before moving
  the calculation to the background 8 and 18), inside the page 7 and 18.
  Then the test row deleted (with the copy of the long page of v0.113.1
  and the database): database identical to how it was before the tests,
  apart from that row, its page and its lines; settings and widgets
  identical

### ✏️ v0.113.1 — 18/10/2026 · 18:45 — Fast long pages
*(Claude Code on the PC; installed on the phone and measured, partly verified: the phone disconnected halfway; 622 tests passed; commit only local)*

- **[Change]** **[📱 Mobile]** **Long pages are split into pieces**: one of
  the user's pages has 689 lines and 31,600 letters, and it was all a
  single text field, redone entirely at every key (and the Samsung keyboard
  had it sent back whole). Now every piece of 50 lines (or 3,500 letters)
  is a field of its own, and typing redoes only the one being typed in.
  From outside it can't be seen: the pieces touch with no space in between.
  Backspace at the start of a piece joins the line to the one above as
  before, Enter and new lines stay in the piece being typed in, and the
  pieces are redone only a moment after you stop typing, never under the
  fingers. **"Select all" (and the finger selection) reaches the edges of
  the piece**, not the whole page (told to the user before starting)
- **[Change]** **[📱 Mobile]** **At every key only what changes is
  redrawn**: before, every piece of the page already seen, the databases
  inside the page and the header (cover, icon, title, properties) were
  redone at every letter; now each receives only what it shows and is
  skipped if it didn't change. List numbers are counted once for the whole
  page (before line by line, rereading the page for every number), and the
  text of every line is read from the database only once
- **[Change]** **[📱 Mobile]** **The phone has the "release" version**, the
  one really used: the test version, which was there until now, is about
  three times slower because of how it is built (measured). It has the same
  signature, installs over it and the data stays
- **[Project]** The measurements, on the phone (Galaxy S25 Ultra, 120 Hz
  screen, 8 ms per frame), in a copy of the long page inside a database:
  **typing and deleting 30 letters**, before 150 ms per frame (99% of
  frames late), after 9 ms median and 19–23 at most; **scrolling with the
  keyboard open**, before 6.8% of frames late and peaks of 48 ms, after
  3.8% and 95% within 9 ms (as with the keyboard closed); the home already
  scrolled well (2.6%). Where the time went was told by a system trace
  (`atrace`, with the names of Compose's functions turned on only for the
  test): the normal profilers on the phone record nothing. Code:
  `PageEditorViewModel.chunkMembers`, `chunkStartsOf`, `rechunk`,
  `setEditing`, `noteNewChunkLines`, `numberedOrdinals`, the `spansOf`
  cache; `resolveRun` stops at the pieces' edges and
  `mergeIntoIslandAbove` also joins to the last line of the piece above.
  In the page: `groupIntoRenderItems(…, chunkStarts)`,
  `RunContent`/`IslandContent`, `PageItemEnv`, `RunItem`/`IslandItem`,
  `pageHeader`, `joinsAbove`/`joinsBelow`. Release signed with the test key
  in `build.gradle.kts`. Test: `PageChunksTest` (the pieces, typing in a
  piece, Enter and backspace at the edges, the pieces redone only when not
  typing, the numbers in one round)
- Tested on the phone in the copy of the long page: the pieces aren't
  visible, backspace at the start of the second piece joins "32." to the
  line "31." with the caret in the right place and the keyboard open,
  Enter splits it again and "xyz" ends up in front of "32.". Then the
  phone disconnected, and the test row "Untitled" (created at 14:28) was
  deleted the next day, with the tests of v0.113.2 (see there). Before
  starting the user's page was identical to how it was

### 🆕 v0.113 — 18/10/2026 · 14:00 — The volume of the Counter's clicks, backspace in lists as in Word
*(Claude Code on the PC; verified on the phone; 616 tests passed; commit only local)*

- **[New feature]** **[📱 Mobile]** **The volume bar in the Counter's
  sounds**, between the switch and the choice of sounds: from 0 to 100%,
  with the number next to it and the speaker icons at the ends. While
  dragging it the number changes; releasing it is saved and you hear the +
  sound at that volume. 100% is the phone's "System" volume (on the user's
  phone it is a volume of its own, in the sound settings), and the panel's
  note now says so. The bar follows the ear: halfway it sounds about half.
  With sounds off it is grey. Every counter has its own; the existing ones
  start at 100%
- **[Change]** **[📱 Mobile]** **Backspace at the start of a list item, as
  in Word**: the first removes the number or the bullet but **the text
  stays where it was**, aligned with the other items; the second brings it
  to the left (to where the number was, and with more backspaces one
  indent at a time is removed, then the line joins the one above, as
  before). The numbering after continues: with the number removed from the
  fifth, the following one stays 5, as in the photos. Going to a new line
  from a line without a number, the new one also stays aligned, as in
  Word. It also applies to lists inside a toggle. The checkbox stays as it
  was: the first backspace turns it into text
- **[Project]** Counter: `CounterWidget.volume`,
  `WidgetStore.setCounterVolume`, `CounterSounds.gain` (the bar's square)
  and `play(sound, volume)` with `AudioTrack.setVolume`; `CounterVolume`
  in the panel. Lists: `BlockAttrs.listIndent` (the list whose place the
  paragraph keeps, in the block's entries: no database change),
  `heldListType`, `attrsForNewLine`; `PageRepository.makeListText` and
  `releaseListIndent`, and `setBlockType` removes it when the block changes
  type. In the joined text the marker is written transparent
  (`LinePrefix.hidden`), so the text stays where it would be; inside a
  toggle it is a transparent `Text`. Tests: `ListIndentBackspaceTest` (the
  two steps, the bullets, the new line, the type change, the indent, the
  toggle, the checkbox), `CounterVolumeTest` (the bar in the panel),
  `CounterSoundsTest` (volume and gain)
- Before installing it the user tried it on the phone, which still had
  0.112 (the PC was waiting for the USB debugging permission): the text
  went straight to the left, as before ("It doesn't work"). With adb
  restarted the permission arrived, and with 0.113 installed: in a test
  row of a database, a list 1–6 of "A"; with the caret between "5." and
  the "A" the first backspace removes the number and the "A" stays aligned
  with the others, with the caret in front, and below there is 5; the
  second brings it to the left, below still 5 — as in Word's photos. The
  Counter's "Sounds" panel shows the bar at 100% with the note about the
  "System" volume (only looked at: the sounds chosen by the user stay).
  Test row deleted; database identical to before, in the widgets only the
  downloaded data and the Counter's new volume at 100

### 🆕 v0.112 — 18/10/2026 · 09:21 — Scorers and stadium, the Counter's sounds, free columns, the line after databases, copy and paste in the bar
*(Claude Code on the PC; verified on the phone; 607 tests passed; commit only local)*

- **[New feature]** **[📱 Mobile]** **In the match window, under the
  score, the scorers as in Sofascore**: the home ones on the left, the
  away ones on the right, with the ball in the middle; full name and
  minutes ("Antonio Raimondo 26', 30', 57'"), penalties and own goals
  marked (the own goal goes to the team that benefits). Below, **date and
  time, competition (with the flag) and stadium**, each with its icon; the
  broadcaster no. It applies to matches in progress, finished and to be
  played (which have only the second line). The line at the top with the
  competition is gone: it is here now
- **[New feature]** **[📱 Mobile]** **The Counter's sounds**: a click on +
  and a lighter one on −. The app draws them itself (no file to download)
  and plays them like key sounds: with the phone on silent they can't be
  heard. In the widget's three dots there is "Sounds": a switch to turn
  them off and, for + and for −, the choice among Click, Soft click, Tick,
  Pop, Drop, Wood, Beep and Coin (tapping one you hear it). Every counter
  has its own
- **[Change]** **[📱 Mobile]** **Database columns narrow as much as you
  want**, dragging the handle, until they disappear completely. Before
  they had a minimum (Name 120, those you type in 80, the others 48); it
  stays only for "Fit to content". The content of a narrow column no
  longer spills into the next one
- **[New feature]** **[📱 Mobile]** **A thin line at the end of every
  database inside a page** (simple or not, linked views too), like the
  divider: the text that follows no longer looks stuck to the table
- **[New feature]** **[📱 Mobile]** **Copy, paste, cut and select all in
  the bar above the keyboard**, between the six dots and the indent arrow.
  They do what Ctrl+C, Ctrl+V, Ctrl+X and Ctrl+A do on a real keyboard, on
  the text where the caret is. Copying formatted text, its formatting
  (bold, italic, underline, strikethrough, links) goes to the clipboard
  too, also with the "Copy" of the menu that opens long-pressing
- **[New feature]** **[📱 Mobile]** **Long-pressing "paste", Word's three
  modes**: "Keep Source Formatting" (the simple tap too) removes the
  formatting of the point pasted into and puts the copier's — bold,
  italic, underline, strikethrough and links, from notE or from another
  app (OneNote, Word, Google Docs, a browser); "Match Destination
  Formatting" pastes the text with the formatting of the point where it
  arrives; "Keep Text Only" only the text, without formatting. The "Paste"
  of the menu that opens long-pressing stays as it was (links yes)
- **[Bug fix]** **[📱 Mobile]** **"Select all" also took the hidden line
  break** every text line has at its head (it serves to hear backspace at
  the start of the line): copied and pasted at the end, the text ended up
  on a new line instead of attaching; cut, it was worth a backspace at the
  start of the line. Seen testing the bar on the phone; it also happened
  with the "Select all" of the menu that opens long-pressing. Now the
  selection stops at the real text
- **[Project]** Sports: `SportMatch.venue` from ESPN's scoreboard,
  `scorersOf` and `MatchEvent.playerFull` in `SportsMatch.kt`, `ScorersRow`
  and `MatchInfoRow` in `SportsMatchSheet.kt`. Counter: `CounterSounds.kt`
  (waves at 44.1 kHz in `AudioTrack`, "sonification" usage),
  `CounterWidget.soundsOn/plusSound/minusSound`, `CounterSoundsSheet`.
  Columns: `minColumnWidth` is 0, the previous minimums in
  `fitMinColumnWidth`; `clipToBounds` on the cells. The line after the
  database in `PageEditorScreen`. Bar: `ClipboardBarButtons`,
  `View.sendCtrlShortcut` (a key without a character, so the letter isn't
  typed), `PasteMode` and `LinkClipboard.nextPasteMode`, `PasteFormat`,
  `applyPaste` and `sliceSpans` in `RichText.kt`, `htmlStyleRuns` (the
  HTML's styles, also those in `span`s as OneNote, Word and Google Docs
  write them). `TextFieldValue.leadOutOfSelection` in the page's two text
  fields. Tests: `ClipboardBarTest` (the four buttons on a real field, the
  three modes, the style copied as HTML, the hidden line break out of the
  selection), `CounterSoundsTest`, `SportsMatchTest` (scorers, stadium),
  `WrapCellsTest` and `ColumnWidthTest` updated
- Tested on the phone: Bologna-Inter 1-3 shows Pobega 10' on the left and
  Thuram 44', Carlos Augusto 69', Bonny 83' on the right, then "17.10.2026
  • 18:00", the flag with Serie A and "Renato Dall'Ara". The Counter's
  "Sounds" panel opens with the switch on, Click for + and Soft click for −
  (only looked at: the counter stayed at 0). The line after the databases
  of the home page. In a test row of a database: select all, copy, paste,
  cut, and long-pressing paste the menu of the three modes (the keyboard
  stays open), with "Keep Text Only" attaching the text at the end of the
  line; the test row then deleted. Database identical to before the tests;
  in the widgets only downloaded weather and matches change and the
  Counter's three new fields with the starting values

### 🆕 v0.111 — 18/10/2026 · 08:51 — The name of the main property
*(Claude Code on the PC; installed together with v0.112 and verified on the phone; 596 tests passed; database at version 58; commit only local)*

- **[New feature]** **[📱 Mobile]** **The main property ("Aa Name") can be
  renamed**, database by database: a tap on the column header, or
  "Rename" at the top of the menu that opens long-pressing it. The small
  window shows the current name; leaving it empty goes back to "Name" (in
  the app's language). The new name appears everywhere: in the table
  header, in a simple database's row window, in "Sort" and its rules, in
  the calculations at the bottom of the column, in the chart ("What to
  show", "Sort by"); and "Fit to content" measures the column with the new
  name. With the database locked it isn't renamed
- **[Project]** `PageEntity.nameColumnTitle`, in the database and not in
  the view: linked views show (and change) their origin's. Database at
  version 58 (`MIGRATION_57_58`: for all it stays "Name").
  `DbStrings.nameColumn`, `LocalNameColumnTitle` and `nameColumnLabel()`
  in `DatabaseViewScreen.kt`; `DatabaseViewModel.renameNameColumn`. Tests:
  `MainPropertyNameTest` (the name in the right database, from a linked
  view too; empty goes back to "Name"; only the app's one translated) and
  `MigrationTest.from57TheMainPropertyIsStillName`, with the new
  `schema/v57.sql`
- Tested on the phone: first the copy of the database (version 57), then
  the install. The database goes to 58 with the same numbers, and for all
  it stays "Name". In a database of the user a tap on "Aa Name" opens
  "Rename the main property"; with another name the header of both groups
  changes, and with the field emptied it goes back to "Name" (put back as
  it was)

### ✏️ v0.110.2 — 18/10/2026 · 08:40 — Links open in Firefox
*(Claude Code on the PC; verified on the phone; 592 tests passed; commit only local)*

- **[Change]** **[📱 Mobile]** **Web pages open in the phone's default
  browser** (for the user Firefox), with its address bar and its tabs,
  and no longer in the browser box over the app (v0.105's "Custom Tab").
  It applies to links on text, bookmarks, videos and audio with an address
  and databases' URL cells. Emails and phone numbers open as before with
  mail and the phone, and an address that has its own app (a YouTube
  video) still opens in that
- **[Project]** `BlockFiles.openUrl`: no more Custom Tab request, only the
  address "to open in the browser" (`CATEGORY_BROWSABLE`). Test:
  `OpenUrlTest` updated (no Custom Tab)
- Verified on the phone, in a test row of a database then deleted: a
  Wikipedia bookmark opens in Firefox, and "back" from Firefox returns to
  notE's page (in v0.105, with the normal tab, it had shown another app:
  not today). At the end the database is the same as before the test

### ✏️ v0.110.1 — 17/10/2026 · 23:45 — "Turn into page" and you stay where you are
*(Claude Code on the PC; verified in the tests; 592 tests passed; commit only local)*

- **[Change]** **[📱 Mobile]** **"Turn into page"** on a database inside a
  page (from the database settings or from the block menu) turns it into a
  link to the database's page **and that's it**: you stay in the page you
  were in, with the link in place of the database. Before, the database
  also opened full screen. To open it just tap the link; to put it back
  inside the page there is always "Turn into database"
- **[Project]** `turnDatabaseIntoPage` no longer takes the action to do
  afterwards; the two entries (`PageEditorScreen`, `BlockActionsSheet`) no
  longer open the database. Test: `TurnDatabaseIntoPageTest` (the block
  becomes a link to the same database, the other lines and the database
  don't change)
- Not tested on the phone: in the pages where tests can be done there is
  no database inside the page, and creating a test one would have left it
  in the Trash. Installed the next morning, with the phone connected again:
  the app opens normally

### 🆕 v0.110 — 17/10/2026 · 19:49 — Sports: the arrow, the score in red, the seasons
*(Claude Code on the PC; verified on the phone; 591 tests passed; commit only local)*

- **[New feature]** **[📱 Mobile]** **The refresh arrow**, to the left of
  the sports widget's three dots: it downloads again right away the
  competition shown (in the chosen season) and the matchdays looked at by
  scrolling. It spins while downloading
- **[Change]** **[📱 Mobile]** **The score of a match in progress is red**,
  like the minute: in the widget, in the matchday window and in the
  match's one. Those of finished matches stay white
- **[New feature]** **[📱 Mobile]** **The season to the left of the name**
  ("2026-2027 Serie A"; for NFL, MLB and tennis a single year, "2026"). **A
  tap** on the season or the name opens the choice: the current one and
  the nine before. With another season chosen, the widget shows its
  matchdays from the last (scrolling, all the others), with the year in
  orange; the matchday window has the matches, that year's standings and,
  in football, the new **"Players"** tab: the top ten for goals, assists,
  yellow and red cards, with the team's crest (for the current season too).
  Every match opens as always, with the commentary and the statistics.
  Reopening the sidebar goes back to the current season
- **[Project]** The season is given by ESPN with the matches (`SeasonInfo`,
  `seasonLabel`); another season is asked for with a day of that season
  (`seasonProbeDay`), the standings with the year; the best players from
  ESPN's "core" API, with the names asked separately, all together
  (`SportsApi.leaders`). Other seasons stay in memory while the app is
  open: past ones don't change. Data saved before is downloaded again
  right away to have the season (`SPORT_REPORT_VERSION` 3). Tests:
  `SportsTest` (seasons of Serie A, NBA and NFL; the real best players of
  Serie A 2025-26) and `SportsWidgetTest` (the season, the arrow, the
  choice)
- Limit: ESPN doesn't give the matchday's number. In seasons already played
  there are the midweek rescheduled matches (in Serie A 2025-26 for
  example Wednesday 18 February), which make an extra group of dates:
  there "Matchday" isn't written and the dates stay; it can also happen to
  the current season when a rescheduled match comes
- Verified on the phone, on the Serie A widget: "2026-2027 Serie A" and the
  arrow next to the three dots (it spins when tapped); Bologna-Inter in
  progress with the 1-3 in red and the minute in red; with 2025-2026 chosen
  (in orange), the last matchday with the results, the final standings
  (Inter 87 points) and the players (Lautaro Martínez 17 goals, Dimarco 17
  assists, the yellows, the reds); then the current season again. Widgets,
  settings and database as before

### 🆕 v0.109 — 17/10/2026 · 19:33 — A match up close, like Sofascore
*(Claude Code on the PC; verified on the phone; 589 tests passed; commit only local)*

- **[New feature]** **[📱 Mobile]** **A tap on a match** (in the widget or
  in the matchday window) opens the match: at the top the two teams with
  the crest and the score (or the kick-off time), the minute in red if it
  is in progress. In football two tabs:
  - **"Events"**, the commentary like Sofascore, from the last event to the
    first, the home team on the left and the away team on the right: goals
    with the minute, the score, the scorer and the assist (own goals and
    penalties marked separately); missed penalties; substitutions, who
    comes in and who goes out; yellow cards, red cards and the second
    yellow, with the reason ("Foul", "Handball", "Violent conduct"…,
    translated); goals disallowed by VAR; the end of every half with the
    score ("HT 3 - 2", "FT 4 - 2") and the announced added time
    ("Additional time 4")
  - **"Statistics"**: ball possession, shots, shots on target, corners,
    offsides, fouls, cards, saves, passes and accurate passes (with the
    percentage), tackles, interceptions, with the bar of the two teams
- **[New feature]** **[📱 Mobile]** In the other sports: the points per
  quarter or inning (and the scoring plays in the NFL) and the statistics
  (NBA: shots, threes, free throws, rebounds, assists, steals, blocks,
  turnovers; NFL: first downs, yards, penalties, possession). In tennis the
  two players' sets. A match in progress updates every minute while it is
  open
- **[Change]** **[📱 Mobile]** The tap on the matchday title still opens
  the window with all the matches and the standings; long-pressing you
  choose the competitions, as before
- **[Project]** From ESPN's "summary": the key events and, for the
  announced added time and VAR, the commentary. `SportsMatch.kt`
  (`MatchDetail`, `parseMatchSummary`, `MatchDetails` which keeps them in
  memory), `SportsMatchSheet.kt` (the window). The players' short names
  ("D. Berardi") are made by the app: ESPN gives the full ones. Tests:
  `SportsMatchTest` (a real Frosinone-Sassuolo 4-2: goals, assists,
  scores, VAR, added times, substitutions, booking with the reason,
  statistics; NBA and NFL by period) and
  `SportsWidgetTest.aMatchLikeSofascore` (the match opened with the tap)
- Verified on the phone, on the Serie A widget (only looking):
  Bologna-Inter in progress at 86', 1-3, with the goals and the assists
  (Bonny assisted by Augusto, Augusto assisted by Zielinski), the
  substitutions, the yellows with the reason, and the statistics
  (possession 33%-67%, accurate passes 244 (80%) and 586 (92%)). Installed
  together with v0.110

### 🆕 v0.108 — 17/10/2026 · 19:19 — Pasting with links; Inter and Roma; faster crests
*(Claude Code on the PC; names and crests verified on the phone, pasting in the tests; 582 tests passed; commit only local)*

- **[New feature]** **[📱 Mobile]** **Text pasted from another app keeps
  its links**, on the same words: OneNote, a browser, any app that puts on
  the clipboard, next to the text, HTML or a text with links. It applies in
  paragraphs, lists, checkboxes, callouts and quotes, even when the paste
  makes several lines (the links of the new lines stay on their lines). The
  pasted text is the same as before: only the links change. It works with
  the "Paste" of the menu that opens long-pressing (or Ctrl+V); what the
  keyboard pastes from its clipboard panel arrives without links
- **[Change]** **[📱 Mobile]** In the sports widget **"Inter" and "Roma"**
  instead of "Inter Milan" and "AS Roma", in the matches (Champions League
  too) and in the standings
- **[Bug fix]** **[📱 Mobile]** **Crests arrive sooner.** They were slow
  because every image opened a new connection to ESPN's image server (with
  the address lookup, which with the phone's private DNS is sometimes
  slow) and closed it right away; and a crest that didn't arrive wasn't
  retried for ten minutes. Now connections are reused (it applies to all
  widgets: weather, sports, stocks), the crests of the whole competition
  are downloaded as soon as you look at it, and a missed one is retried
  after a minute, by itself. Once downloaded they stay on the phone and
  appear right away
- **[Project]** Pasting in `PasteLinks.kt`: `LinkClipboard` gives Compose's
  paste the text with the links marked on it; the editor's two fields
  collect them (`pastedLinks`) and put them in the rich text with
  `applyLinks` (`RichText.kt`), also in `updateRun`, `splitBlockAt` and
  `splitBlockAtMultiple`. The short names in `teamDisplayName`
  (`Sports.kt`). Tests: `PasteLinksTest` (clipboard with HTML like
  OneNote, text with links, plain text, repeated words, links on rich
  text) and `PasteLinksSaveTest` (the links saved in the shared text on two
  lines and in three checkboxes)
- Verified on the phone: on the Serie A widget "Inter" and "Roma", and the
  crests right away, already saved; with a test widget on the Bundesliga,
  never seen, almost all crests in 3 seconds and the last five in 8 (the
  same images from the PC answer right away: the delay is ESPN's image
  server). The test widget was removed: widgets, settings and database as
  before. Pasting with links wasn't tested on the phone: it would have
  meant overwriting the user's clipboard

### ✏️ v0.107.1 — 17/10/2026 · 19:00 — Sports by matchday, with crests and flags
*(Claude Code on the PC; verified on the phone; 574 tests passed; commit only local)*

- **[Change]** **[📱 Mobile]** **The crests** to the left of every team, in
  the matches and in the standings; in tennis the player's flag. They are
  ESPN's in the version for dark backgrounds (Juventus' J is white),
  downloaded small (64×64, about 4 KB) and kept in the app's cache: each is
  downloaded only once
- **[Change]** **[📱 Mobile]** **The widget shows a whole matchday** of a
  competition at a time, with the days separated ("Friday 16", "Today",
  "Tomorrow"), and **scrolling** you see the matchdays before and after,
  the whole season. It opens on the current matchday (the day after the
  last match you still see the results). The matchdays: in football the
  nearby match days, in the NFL and college the week ("Week 6"), in
  basketball, baseball and tennis the day (tennis split by tournament).
  Where the count adds up (Serie A, Serie B, Premier, Bundesliga, Ligue 1)
  "Matchday 7"; elsewhere (LaLiga with rescheduled and earlier matches, the
  cups) the dates, with the phase's name if ESPN gives it ("League Phase")
- **[Change]** **[📱 Mobile]** **The arrows** at the sides of the name
  change competition: the left one grey on the first, the right one on the
  last, both if there is only one. Opening the sidebar you see the first
- **[Change]** **[📱 Mobile]** **The flag** to the left of every
  competition's name (in the widget, in the window, in the list and in the
  catalogue): 🇮🇹 Serie A, Serie B, Coppa Italia, the St George's cross for
  the Premier, 🇪🇸 🇩🇪 🇫🇷, 🇺🇸 for the American leagues. For continental
  competitions the globe with the continent: 🌍 Europe and Africa (there is
  no emoji of Europe alone), 🌎 the Americas, 🌏 Asia; for the World Cup,
  ATP and WTA the whole globe 🌐
- **[Change]** **[📱 Mobile]** A tap on a matchday opens its window: the
  matches and the standings, now with the crests. Only the competition
  shown is downloaded: the matchdays twice a day, the current matchday
  every five minutes (every minute with a match in progress), the others
  when you reach them scrolling; "Refresh" downloads everything again
- **[Project]** ESPN doesn't give the matchday's number: matchdays are
  derived from its calendar (`buildRounds`, `soccerRounds`, `currentRound`
  in `Sports.kt`). Baseball lists days without matches; for the cups, which
  have only the phases, the days are looked for month by month. The crests
  in `TeamLogos.kt`. Data saved by the previous version is downloaded again
  right away (`SPORT_REPORT_VERSION`). Tests: `SportsTest` (Serie A with 38
  matchdays, LaLiga, NBA, NFL by weeks, baseball, Champions League from the
  months, tennis with the flags, the competitions' crests and flags) and
  `SportsWidgetTest` (the matchday drawn, the arrows), with new ESPN
  responses in `resources/sports`
- Verified on the phone, on the Sports widget (Serie A, only looking):
  Matchday 7 from Friday 16 to Monday 19 with all the matches, the
  results, half time in red and the crests; matchdays 6 and 8 scrolling;
  the standings with the crests; both arrows grey. With a test widget
  (Champions League, ATP, NFL): the League Phase of Tuesday 20 and
  Wednesday 21, today's ATP with the tournaments and the flags, the NFL's
  Week 6, the arrows grey at the first and the last. Then the test widget
  was removed: widgets, settings and database as before

### 🆕 v0.107 — 17/10/2026 · 14:50 — The sports and stocks widgets
*(Claude Code on the PC; tested on the phone with internet; 569 tests passed; commit only local)*

- **[New feature]** **[📱 Mobile]** **"Sports" among the sidebar
  widgets**. One page per competition, up to ten, swiped with the finger:
  football (Serie A, Serie B, Coppa Italia, Champions, Europa and
  Conference League, Premier League, LaLiga, Bundesliga, Ligue 1, Nations
  League, World Cup and UEFA qualifiers), tennis (ATP, WTA), basketball
  (NBA, WNBA, NCAA), baseball (MLB) and American football (NFL, college).
  On the page the last finished matches, those in progress with the minute
  in red and the next ones. **A tap** opens the window with two tabs:
  "Matches", all the matches divided by day, and "Standings" (in tennis
  "Ranking"), the standings. **Long-pressing** opens the list of
  competitions: "Add a competition" at the top, and for each, from the ⋮
  menu, Show first, Move up, Move down and Remove
- **[New feature]** **[📱 Mobile]** **"Stocks" among the widgets**: the
  list of securities as in the user's photo, with symbol, name, price and
  the day's change in green or red, up to thirty securities. They are
  searched by name or symbol (shares, indices, currencies, funds), with
  exchange and type in the app's language ("Milan · Equity"). **A tap** on
  a security opens the details: price and currency, previous close, the
  day's and the year's low and high, last trade (with date and time as in
  Settings). **Long-pressing** opens the list to add, move and remove
  securities
- **[New feature]** **[📱 Mobile]** Like the weather, sports and stocks are
  downloaded **only with the sidebar open**: sports every five minutes
  (every minute with a match in progress, the standings every half hour),
  stocks every five minutes, or right away with "Refresh" in the three
  dots. Without a network the last saved data stays, with "No connection"
  next to the time
- **[Change]** **[📱 Mobile]** **Cities are searched in the app's language
  and in the phone's**, as the user decided: with both in English you
  search "Milan"; with one of the two in Italian "Milano" also finds
  Milano, Lombardia. The cities found come **from the most populated to the
  least**, and the same city only once
- **[Bug fix]** **[📱 Mobile]** **Sports no longer make you wait
  minutes**. On the phone the private DNS (AdGuard) sometimes takes 16
  seconds to find a site, and the widget asked ESPN six questions one after
  the other: Serie A took more than two minutes. Now the questions all go
  out together, and the first time the matches appear as soon as the first
  answer arrives: the Premier League loaded in a few seconds. Searches and
  security prices also ask everything together
- **[Change]** **[📱 Mobile]** City and security searches show
  **"Loading…"** while waiting for the answer, instead of an empty window
- **[Change]** **[📱 Mobile]** In the matches window the day is only in
  the title above the matches, and the tab not chosen is grey. The "Remove
  this widget?" notice no longer lists clocks and counters, which have
  nothing to do with the new widgets
- **[Project]** Matches and standings from **ESPN**, prices and search
  from **Yahoo Finance**: free, without an account or key, but unofficial
  (they can change without notice), and prices can be up to fifteen
  minutes late. `SportsWidget` and the reading of responses in
  `Sports.kt`, the questions in `SportsApi.kt`, the widget in
  `SidebarSports.kt`; stocks in `Stocks.kt` and `SidebarStocks.kt`; the
  common request in `WidgetHttp.kt`. Tests: `SportsTest`, `StocksTest`,
  `SportsWidgetTest`, `StocksWidgetTest` and a new test in `WeatherTest`,
  with real responses saved in `resources/sports`, `resources/stocks` and
  `resources/weather`
- Tested on the phone, with internet: the weather of Milan (searching
  "Milan", Milano, Lombardia was the first) and the hourly weather; Serie A
  with matches and standings and the Premier League with a match in
  progress; a security search with its details, and a search in English
  with "Loading…". Then the three test widgets were removed, and widgets,
  settings and database went back identical to before

### 🆕 v0.106 — 17/10/2026 · 14:01 — The weather widget
*(Claude Code on the PC; tested on the phone without network, with the network from the PC and in the tests; 554 tests passed; commit only local)*

- **[New feature]** **[📱 Mobile]** **"Weather" among the sidebar
  widgets**. One page per city: the main one and up to nine more, swiped
  with the finger. Every page has temperature, sky, today's high and low,
  the next five days and the time of the last update. **A tap** opens the
  hourly weather: now (feels like, humidity, wind), then the next 48 hours
  divided by day, with the chance of rain, and the next seven days.
  **Long-pressing** opens the cities window: "Add a city" at the top, and
  for each, from the ⋮ menu, Change city, Make main city, Move up, Move
  down and Remove city. Cities are searched by name, with region and
  country to tell them apart. The same entries, "Cities" and "Refresh", are
  in the widget's three dots
- **[New feature]** **[📱 Mobile]** The weather is downloaded **only with
  the sidebar open**, and for each city at most every half hour (or right
  away, with "Refresh"). Without network the widget shows the last saved
  weather with "No connection" next to the time, and the search says "No
  connection"
- **[Project]** The data from **Open-Meteo**, free and without an account
  or key: forecasts and city search, with the names in the app's language.
  `WeatherWidget` and the reading of responses in `Weather.kt`, the
  questions in `WeatherApi.kt`, when to download again in
  `WeatherUpdates.kt`, the widget in `SidebarWeather.kt`. The cities and the
  last weather are with the widgets, in the phone's preferences. The sky
  comes from the WMO code, with an emoji for day and one for night and the
  translated name. Tests: `WeatherTest`, with two real Open-Meteo responses
  saved in `resources/weather`, and `WeatherWidgetTest` (cities, the limit
  of ten, and the drawing of Bergamo's page)
- Tested on the phone: the widget is added from "Add widget", and the
  three dots have "Cities" and "Refresh". The phone had no internet (Wi-Fi
  off, no active network): searching "Bergamo" the search said "No
  connection". Then the widget was removed, and widgets and database went
  back identical to before. With the network the weather was tested from
  the PC (Open-Meteo answers, and the responses are read right in the
  tests); on the phone it is to be tried again when there is internet

### ✏️ v0.105.2 — 17/10/2026 · 10:09 — As many sort rules as you want, in "Sort rules"
*(Claude Code on the PC; verified on the phone; 548 tests passed; database at version 57; commit only local)*

- **[Change]** **[📱 Mobile]** **"Sort rules" at the top of the Sort
  window**, with the number of rules. It opens a separate window with all
  the rules, from the first to the last, numbered in the order they count:
  each sorts only the rows equal for those above. "Add a sort rule" is
  always at the top, above the list, and proposes the properties not yet
  used; the new rule goes at the bottom, ascending, and the list scrolls
  down to it. A tap on a rule reverses its direction. The ⋮ menu has Move
  up, Move down and Remove sort. At the bottom there is "Remove all sorts".
  When all the properties are already in a rule, "Add a sort rule" turns
  off
- **[Change]** **[📱 Mobile]** Under "Sort rules", "Sort by" with the
  properties as before: tapping one it becomes the first rule, in place of
  the one there; tapping the one that is already first reverses its
  direction. Next to the properties already used, their place and
  direction ("1 ↓"). "Then sort by" and "Add a second sort" are gone: the
  second rule is a rule like the others
- **[Project]** All the rules in a single list, `pages.sortRulesJson`
  (`SortRuleSpec`, `SortRules.kt`). Migration 56→57 moves each page's first
  and second rule into the list, in the same order and direction, and
  empties the previous fields (`sortColumnId`, `thenSortColumnId`): they
  stay in the table, but nobody reads them anymore. No rule had to be
  deleted: the five that existed all moved over. In the ViewModel
  `addSortRule`, `toggleSortRule`, `moveSortRule`, `removeSortRule` and
  `clearSortRules`; `setSort` and `setThenSort` work on the list; rules on
  deleted properties fall at the first change. Tests: `SortRulesTest`,
  `ManySortRulesTest` (with four rules the fourth counts too),
  `MigrationTest.from56TheTwoSortRulesMoveIntoTheList` with
  `schema/v56.sql`, and `SecondSortTest` updated
- Verified on the phone, with a safety copy of the database made first.
  After the update the five sorts are in the list, with the same order and
  direction. In a database of the user "Sort rules" said "2 rules"; three
  test rules added (five rules, and "Add a sort rule" off), one reversed,
  one moved up: saved in that order. With the three test ones removed, the
  database went back as it was. The back key closes the whole window, like
  the other windows that rise from the bottom; to go back to the previous
  page there is the ← arrow at the top. At the end of the test the phone
  had disconnected: this version was installed when the user connected it
  again. Compared to the one tested only two removed lines change (a
  `BackHandler` that couldn't work in the window) and the version number

### ✏️ v0.105.1 — 17/10/2026 · 09:46 — Bookmarks, videos and URL cells also open over the app
*(Claude Code on the PC; verified on the phone; 539 tests passed; commit only local)*

- **[Change]** **[📱 Mobile]** **Web bookmarks, videos and audio with an
  address and databases' URL, email and phone cells open like links on
  text**: web pages in the browser box over the app, and "back" returns to
  notE. Before they opened as a normal browser tab, and "back" could show
  another app. An address that has its own app (a YouTube video with the
  YouTube app) still opens in that. Files (videos, PDFs…) opened "with
  another app" now also start from notE, so "back" from the player returns
  here
- **[Project]** A single way to open addresses: `BlockFiles.openUrl`,
  which opens as a Custom Tab (until v0.105 it was in `TextLinks.kt`).
  `rememberLinkOpener` of text links and `openLink` of cells use it.
  Without an activity, which doesn't happen in the app, it asks for a new
  activity as before. Test: `OpenUrlTest`
- Verified on the phone in a test row of a database, then deleted: an
  example.com bookmark opens in Firefox's box, and "back" returns to the
  page. At the end the phone's database is the same as before the test.
  Videos and URL cells go through the same function, tested by the tests;
  on the phone I didn't open them, because a YouTube video would have
  opened the YouTube app

### 🆕 v0.105 — 17/10/2026 · 09:39 — Links on selected text
*(Claude Code on the PC; verified on the phone; 536 tests passed; commit only local)*

- **[New feature]** **[📱 Mobile]** **The link icon in the bar above the
  keyboard**, next to the brush. It appears when there is selected text
  (long-pressing a word) or when the caret is inside a link, and opens the
  small "Link" window where to paste or type the address: "google.com" is
  enough (it becomes https://google.com), an email address becomes a link
  to write a mail. If the text already has a link, the window shows it and
  has "Remove link". With just the caret inside a link you change or remove
  the whole link
- **[New feature]** **[📱 Mobile]** **Text with a link is blue and
  underlined, and tapping it opens it**, without placing the caret or
  opening the keyboard. Long-pressing it, instead, selects it as always,
  to change or remove the link. Web pages open in the browser box over the
  app (Custom Tab), and "back" returns to notE's page. Typing right before
  or right after a link, the new text doesn't enter the link; typing
  inside it does. A link inside a covered spoiler doesn't open: the tap
  uncovers it, as always
- **[Project]** `RichTextSpan.link`: text saved before doesn't have the key
  and is null, no migration. In `RichText.kt` `setLinkInRange`,
  `linkInRange` and `linkRunAt`; the new `TextLinks.kt` has the colour,
  the mark on the shown text, `linkTaps` (it takes the tap before the
  field), `LinkDialog` and opening as a Custom Tab, without adding the
  androidx.browser library. In the ViewModel `selectionLink`,
  `capturedLink` and `applyLinkToCapturedSelection`, on the selection set
  aside as for the brush. It applies in the editor's two fields: text
  lines, lists and checkboxes, and blocks on their own like quotes,
  toggles and callouts. Tests: `RichTextLinkTest`, `TextLinkTest` and
  `TextLinkTapTest` (it fails if the tap on the link reaches the field)
- Verified on the phone in a test row of a database, then deleted.
  Long-pressing "Google" the icon appears; with "example.com" "Google"
  becomes blue and underlined and stays so after reopening the app.
  Tapping it opens the page, and "back" returns to notE without the
  keyboard; "Remove link" removes it; the same in a quote. The first try
  opened the link as a normal Firefox tab, and "back" showed the app used
  before instead of notE: hence the Custom Tab. At the end the phone's
  database is the same as before the test

### 🔧 v0.104.1.1 — 17/10/2026 · 09:14 — The high dot is really high: 2˙026
*(Claude Code on the PC; verified on the phone; 526 tests passed; commit only local)*

- **[Bug fix]** **[📱 Mobile]** **"Number format"'s fourth choice used
  "·", the dot at mid height**: now it is "˙", at the top of the digits.
  2˙026, 1˙000˙000 and with decimals 1˙234,56
- **[Project]** `NumberStyle.MIDDLE_DOT_GROUPS` becomes `HIGH_DOT_GROUPS`
  (U+02D9). Nobody had chosen it, so the new name changes nothing in the
  saved settings. `NumberStyleTest` updated
- Verified on the phone: in the menu and in a database's Year column the
  dot is at the height of the top of the digits. Then the user's choice put
  back, 2026 without a separator

### ✏️ v0.104.1 — 17/10/2026 · 09:05 — Numbers with more than three digits: 2 026, 2026, 2.026 or 2·026
*(Claude Code on the PC; verified on the phone; 526 tests passed; commit only local)*

- **[Change]** **[📱 Mobile]** **"Number format" in Settings has five
  choices instead of two**: 1 000 000, 1000000, 1.000.000, 1·000·000 and,
  as before, 1,000,000. Under each the same number with decimals: with the
  first four decimals go after the comma (1 234,56), with the last after
  the dot (1,234.56). It applies across the app: Number cells, calculations
  at the bottom of the columns, charts, filters, word count, widgets. The
  space doesn't wrap. The saved number doesn't change: only how it is read
  changes
- **[Project]** `NumberStyle` says the sign between the groups of three
  digits (`grouping`, null = none) and the decimal one;
  `Formats.number(raw, style)` also writes the menu's examples. Test:
  `NumberStyleTest`
- Verified on the phone: the menu shows the five spellings; in a database
  the Year column reads "2·026", "2 026" and "2026" depending on the
  choice. Then 1.000.000 put back, as it was

### 🆕 v0.104 — 17/10/2026 · 08:50 — In a row's page, the arrows for the previous and next page
*(Claude Code on the PC; verified on the phone; 523 tests passed; commit only local)*

- **[New feature]** **[📱 Mobile]** **˄ and ˅ at the top right of a row's
  page**, next to the three dots, like Notion's arrows: the previous and
  next page of the same database. The order is that of the view it was
  opened from: filter, sorting, search, the table's groups and subgroups
  (with the sub rows open under their row), the board's columns and bands,
  the dates of the calendar and timeline (and at the bottom the rows
  without a date). At the top ˄ is off, at the bottom ˅. The new page takes
  the place of the previous one: "back" returns to the database, not to
  all the pages browsed
- **[Project]** `RowNavigation.kt`: the order is photographed when the row
  is opened from the view (`displayedRowIds`, one rule per view, which uses
  `buildRowGroups` and `rowTree` like the views themselves) and stays that
  while browsing. Opening the page from elsewhere (search, links, after
  closing the app) the database's filter and sorting apply. A row that
  appears in several groups (multi-select) counts the first time. Test:
  `RowOrderTest` (simple table, with groups and subgroups, board with
  columns and bands, calendar)
- Verified on the phone on 16 and 17/10, in a database with three test
  rows "Prova 1–3" (then removed): with the search "Prova", from "Prova 2"
  ˅ → "Prova 3" with ˅ off; ˄ twice → "Prova 1" with ˄ off; "back" returns
  right away to the table. With sorting by name descending, from "Prova 2"
  ˅ → "Prova 1". Sorting and test rows removed; in the phone's database
  only the changes made by the user in the meantime remain

### ✏️ v0.103.3 — 16/10/2026 · 17:40 — "Time format" in Settings too, a single one for the whole app
*(Claude Code on the PC; verified on the phone the same day; 519 tests passed; commit only local)*

- **[Change]** **[📱 Mobile]** **"Time format" in Settings**, right under
  "Date format": **Hidden**, **12 hour**, **24 hour**, with the current
  time written that way. 12 hour applies across the app: database dates,
  the history, "Created" and "Last edited", favorites, trash, notebooks,
  "/notification" lines, events and the day's hours in the calendar ("3
  PM"), durations, the end of timers. **Hidden** removes the time from
  database dates; where the time is what you want to read (history,
  "Created", timers…) it stays at 24 hours. The "Set time" wheels stay at
  24 hours. By default 24 hour, as all the columns had
- **[Removed]** **[📱 Mobile]** **"Time format" from the "Edit property"
  of Date columns**: there now there is only the name
- **[Project]** `AppSettings.timeFormat` (`TimeFormatKind`);
  `Formats.timeFormatter`, `hourLabelFormatter` and
  `databaseDateTimeFormatter` instead of the formats written by hand in the
  various files. `DateSettings` and `DateSettingsSection` gone;
  `database_columns.timeFormat`, like `dateFormat`, stays in the database
  but is no longer read. Test: `GlobalTimeFormatTest` (24 hours, 12 hours
  and "3 PM", Hidden only in databases). The test changed the settings
  outside a Compose snapshot, and the graphical tests after it were left
  waiting: now it changes them inside, and the full round is back to 44
  seconds
- Verified on the phone: Settings → "Time format — 24 hour", the list
  Hidden ("No time in database dates"), 12 hour (5:37 PM), 24 hour
  (17:37); with 12 hour a calendar database → "2026年11月15日 3:00 PM",
  with Hidden "2026年11月15日" with the bell; the Date column's "Edit
  property" has only the name. 24 hour put back: database identical to
  before the tests

### ✏️ v0.103.2 — 16/10/2026 · 17:15 — "Date format" goes back to Settings, a single one for the whole app
*(Claude Code on the PC; verified on the phone the same day; 518 tests passed; commit only local)*

- **[Change]** **[📱 Mobile]** **"Date format" in Settings**, after
  "Number format", as before v0.64: the nine formats (Full date, Short
  date, Month.Day.Year, Month/Day/Year, Day.Month.Year, Day/Month/Year,
  Year.Month.Day, Year/Month/Day, Chinese date), each with today's date. It
  applies **everywhere**: cells, lists, calculations (Earliest, Latest…),
  the filter's summary, charts, history, favorites, trash, notebooks. The
  one chosen stays the previous one: on the phone "Chinese date"
- **[Removed]** **[📱 Mobile]** **"Date format" from the "Edit property" of
  Date columns**. "Time format" stays (Hidden, 12 hour, 24 hour), which
  applies to the column. The only column that had chosen its own ("Full
  date") now follows Settings'
- **[Project]** `DateSettings` has only the time;
  `database_columns.dateFormat` stays in the database but is no longer
  read, and saving the column empties it. Test: `GlobalDateFormatTest`
- Verified on the phone: Settings → "Date format — Chinese date
  2026年10月16日", the list of nine with the tick on Chinese date; with
  Day/Month/Year chosen for the test, a database's dates became "15/11/2026
  15:00"; in the Date column's "Edit property" there is only "Time format".
  Chinese date put back: the phone's database and settings are identical to
  before the tests

### ✏️ v0.103.1 — 16/10/2026 · 17:04 — The date's time stays the one chosen, and the notification no longer goes to 9:00
*(Claude Code on the PC; verified on the phone the same day; 516 tests passed; commit only local)*

- **[Bug fix]** **[📱 Mobile]** **"Set time" kept the previous time** if
  "Done" was pressed while the wheel was still stopping: after a quick
  flick on the minutes the starting ones stayed (9:00, or 15:00). Now
  "Done" takes the numbers visible in the middle. It applies to every "Set
  time" of the app: database dates, calendar events, durations,
  "/notification" lines
- **[Change]** **[📱 Mobile]** **No more 9:00 by itself**: turning on
  "Include notification" also turns on "Include time", and turning off the
  time turns off the notification, so the notification always rings at the
  chosen time. The proposed time, when there is none, is now and no longer
  9:00 (the end an hour later). Dates saved before with the notification
  and without a time stay as they were (at 9:00) until they are changed: in
  the phone's database there is only one
- **[Project]** `WheelPicker` has `onLiveValue`, the number in the middle
  at every moment (`snapshotFlow`), and `SamsungTimeDialog` uses it for
  hours and minutes. Test: `TimeDialogTest` (flick on the minutes, "Done"
  halfway: the minutes are no longer the starting ones), which fails
  without the fix
- Verified on the phone on a test row in a database (then removed): with
  the time off the notification turns off, with the notification on the
  time turns on; quick flick on the minutes and "Done" right away, with the
  wheel on 15:07 → "Start time 15:07", and in the database 20/10/2026 15:07
  with time and notification. Before the fix the same gesture had given
  09:05. The phone's database is identical to before the tests

### 🆕 v0.103 — 14/10/2026 · 11:35 — "---" makes a divider, and "Move up"/"Move down" for images, files and dividers
*(Claude Code on the PC; verified on the phone the same day; 515 tests passed; commit only local)*

- **[New feature]** **[📱 Mobile]** **"---" written alone in a text line
  turns it into a divider**, as in Notion: the dashes disappear and the
  caret goes to the new line below. It also works with the long dash in
  place of the first two dashes, and in lines inside a toggle. On lists and
  checkboxes, or in the middle of other text, the dashes stay written. Undo
  removes the divider and puts back "--"
- **[New feature]** **[📱 Mobile]** **"Move up" and "Move down" also for
  images, videos, audio, files, bookmarks, "Link to page" and dividers**,
  above "Add above", long-pressing them; greyed out at the top and bottom
- **[Bug fix]** **[📱 Mobile]** A divider kept in the database the text it
  had before (invisible, but counted in search): now, becoming a divider,
  the block always empties
- **[Project]** `dividerShortcutJustTyped` (like
  `numberedListPrefixJustTyped` for "5."),
  `PageEditorViewModel.dividerShortcutBlock`, then the same "Divider" entry
  of the "/" menu; `MoveGroup` in `ObjectBlockMenu` and
  `DividerBlockMenu`; `turnIntoDividerWithLineBelow` always empties the
  text. Test: `DividerShortcutTest`
- Verified on the phone in a test page in a database (then removed):
  "One", new line, three dashes → divider, and "Two" written below; with
  the divider long-pressed, "Move down" → under "Two"; a second "---" has
  empty text in the database; Undo → "--". The phone's database is
  identical to before the tests

### ✏️ v0.102.3 — 14/10/2026 · 11:22 — "Move up" and "Move down" for databases and pages too
*(Claude Code on the PC; verified on the phone the same day; 512 tests passed; commit only local)*

- **[Change]** **[📱 Mobile]** **"Move up" and "Move down"** at the top of
  the menu that opens from the six dots of a database inside a page and
  long-pressing the link to a page, above "Add above": the block swaps
  place with the one above or below, as the bar's arrows do for text
  lines. At the top "Move up" is grey, at the bottom "Move down". The menu
  closes, and the page brings the moved block into view
- **[Project]** `MoveGroup` in `BlockActionsSheet.kt`, in the menu of
  pages and databases (`PageBlockMenu`); it uses
  `PageEditorViewModel.moveBlockUp` and `moveBlockDown`, the same as the
  arrows. Test: in `MoveBlockTest` the database going up to the top and
  back down to the bottom
- Verified on the phone, in a test page in a database (then removed, with
  the pages it contained): "Above", a database, "Below". From the
  database's six dots "Move up" → the database above "Above", and
  reopening the menu "Move up" is grey; "Move down" twice → at the bottom,
  and "Move down" grey. A subpage long-pressed → "Move up" → it goes up
  above "Below". The phone's database is identical to before the tests

### ✏️ v0.102.2 — 14/10/2026 · 11:07 — The "+" changes all the selected lines, not just the one with the caret
*(Claude Code on the PC; verified on the phone the same day; 511 tests passed; commit only local)*

- **[Change]** **[📱 Mobile]** **With some text selected, the bar's "+"
  changes all the lines the selection touches**, in their order: three
  selected paragraphs and "Numbered list" make 1, 2, 3; "To-do list" makes
  three checkboxes. Whatever type they had (text, bulleted or numbered
  list, checkbox) and whatever font or size the text has, which stays as it
  was. The entries are Text, Bulleted list, Numbered list, To-do list,
  Toggle list, Callout, Quote and Code: every line becomes its own.
  Notification, Table and the others stay the caret line's, as before.
  Without a selection, or inside a single line, the caret's line changes.
  **Undo** puts them all back as they were in a single step
- **[Project]** Which lines the selection touches: those with at least one
  character selected, and the empty ones inside it; a selection that ends
  exactly at the start of a line doesn't take it (you selected up to the
  line break before). The "+" sets the selection aside when it opens, like
  the brush (`captureSelection`); `capturedSelectionBlocks` and
  `updateBlockTypes` in the ViewModel, `PageRepository.setBlockTypes`
  changes the lines in a single transaction (you see them all change
  together). Test: `TurnIntoManyTest` (which lines, numbered in order with
  the text intact and Undo in one step, four checkboxes)
- **[Project]** About the numbered list's menu: **it is still there**,
  long-pressing **on the number** ("Begin a new list here", "Renumber as
  continuation of previous list", "Remove number", "Change to bullet").
  Tested on the phone in a new page and in a long page of the user (on the
  "3.", on the indented "iv.", and with the keyboard open): it opens every
  time. Tried again by the user too: it works
- Verified on the phone in a test page in a database (then removed):
  "One, Two, Three, Four", selected from "One" to the start of "Three" and
  chosen Numbered list → "1. One, 2. Two", Three and Four stay text; then
  "Select all" and To-do list → four checkboxes, the two numbered ones too;
  Undo → again "1. One, 2. Two, Three, Four". The phone's database is
  identical to before the tests

### ✏️ v0.102.1 — 14/10/2026 · 10:51 — The cell being typed in rises above the keyboard, in a single movement
*(Claude Code on the PC; verified on the phone the same day; 508 tests passed; commit only local)*

- **[Change]** **[📱 Mobile]** **The cell being typed in always stays
  above the keyboard**, whole, with its border and a bit of space: while
  the keyboard rises, the table rises with it, in a single movement that
  accelerates and slows down and ends together with the keyboard. It
  applies to every field of a database: cells, row names, the title, in
  the full-page table, in the database inside a page and in the properties
  of a row opened as a page. Closing the keyboard everything goes back down
  in its place
- **[Bug fix]** **[📱 Mobile]** **Full page the keyboard went over the
  rows**: the scrolling part stayed as tall as the screen, and the cells at
  the bottom were typed blindly (the user's photos). Now it shortens
  together with the keyboard
- **[Bug fix]** **[📱 Mobile]** **Inside a page the cell stayed half
  covered**: the page rose, but only enough to show the text, and the
  cell's border stayed behind the keyboard
- **[Project]** `keepFocusedAboveKeyboard` (`KeepAboveKeyboard.kt`), put
  inside what scrolls. The scrolling box shortens with the keyboard
  (`imePadding` full page, as the page editor already did), and Compose
  chases the focused field by itself; in addition, at every frame of the
  rise, it asks to see the field with 24 dp above and below, and Compose
  merges the request into the animation already running. The keyboard is
  read outside drawing (`snapshotFlow`), so as not to redraw the whole
  database at every frame. The margin doesn't exceed the content below and
  above the field (asking to scroll past the end makes Compose interrupt
  the animation); fields taller than a third of the screen are left alone.
  The request also restarts when focus moves to another cell with the
  keyboard open (this not tested on the phone). A first version asked for
  the margin only with the keyboard still: two movements were visible, with
  two frames of pause in between
- Verified on the phone recording the screen at 120 Hz and measuring how
  much the table moves at every frame. **Full page**, in a shopping list,
  the last row's cell: the keyboard rises in 0.23 s and the table with it,
  4 → 12 → 16 → 24 → 28 px per frame, then down to 4 and 0, without jumps or
  pauses; at the end the row is entirely above the keyboard, with the
  border and the space. Before, the keyboard covered it completely. **In
  the page** (the home page, a cell of the same list): the same single
  movement, and the whole cell in view instead of cut in half. Closing the
  keyboard the table goes back down. No value written: the phone's database
  is identical to before the tests

### 🆕 v0.102 — 14/10/2026 · 10:28 — Subgroups in the Board too, and "Group by" for almost every property
*(Claude Code on the PC; verified on the phone the same day; 508 tests passed; database at version 56, copy of the phone's database made before installing; commit only local)*

- **[New feature]** **[📱 Mobile]** **"Sub groups" in the Board**, under
  "Group by", as in Notion: the board is divided into **horizontal bands**,
  one per value of the second property, and every band has all the board's
  columns with its cards. The column names are there only once, at the top;
  the band's name, with the number of its cards, stays on the left edge
  while the columns scroll, and tapping it the band closes and reopens.
  "New" inside a band creates a card already with the column's value **and**
  the band's. On the cards neither the column's value nor the band's is
  repeated
- **[New feature]** **[📱 Mobile]** **"Hide empty sub groups"** for the
  Board's bands, on by default, separate from the table's
- **[Change]** **[📱 Mobile]** **The Board's "Group by" offers the same
  properties as the table's grouping**: also checkbox, date, number, text,
  multi-select, URL, email, phone — no longer only single-select ones. A
  checkbox makes two columns, "Checked" and "Unchecked"; with multi-select
  a card is in the column of each of its tags. Colour, image, page link,
  duration, "Created time" and "Last edited time" aren't there, which don't
  give readable names or make one group per row. Without a choice the first
  single-select stays, as before
- **[Bug fix]** **[📱 Mobile]** In the Board a card with a tag later deleted
  from the property disappeared: now it has its column, after the tags'
  ones, as in the table
- **[Bug fix]** **[📱 Mobile]** In the table grouped by a date, "New page"
  inside a group created the row on **1 January 1970**, in a group all of
  its own: now it is born on the group's day. The same applies to the Board
- **[Project]** About the question: **it wasn't a bug but an intended
  limit**. The Board accepted on purpose only single-select properties, and
  in the user's database a property had become multi-select: with a number
  and a checkbox it was left out, and only one remained
- **[Project]** Database at **version 56**: `pages.boardSubGroupColumnId`
  and `boardHideEmptySubGroups` (`MIGRATION_55_56`), copied also when
  duplicating a page and removing the first view. `ColumnType.canGroup`
  says which properties can be grouped by, for table, subgroups and Board.
  The Board takes columns and bands from `buildRowGroups`, the same as the
  table's groups; the "Sub groups" settings page is a single one for table
  and Board. Tests: `BoardSubGroupTest` (ViewModel), `BoardLayoutTest` (the
  board drawn: cards in the right column and band, bands that close, "New"
  with the two values, columns for a checkbox), and the migration from 55
  in `MigrationTest` (`schema/v55.sql`)
- Verified on the phone, in the Board view of a database of the user:
  "Group by" offers all four properties; "Sub groups" the same except the
  first. With the bands by the multi-select property you see the bands,
  each with all the columns; scrolling right the band's name stays on the
  left; tapping it the band closes. "New" in a band under a column created
  a row with both values (then removed). Grouped by Checkbox: "Checked" and
  "Unchecked". The previous settings and the Table view put back: the
  phone's database is as it was (only the edit time of the Board view
  changes)

### ✏️ v0.101.3 — 09/10/2026 · 09:03 — The ticked row goes down after 0.2 seconds
*(Claude Code on the PC; verified on the phone the same day; 495 tests passed; commit only local)*

- **[Change]** **[📱 Mobile]** The row just ticked (or unticked) goes to
  its place **0.2 seconds** after its checkbox has filled, no longer one
  second after. Ticking several things in a row they stay still as long
  as you go on, and then go down together
- **[Project]** `CHECKBOX_SETTLE_MS` at 200. With such a short moment, a
  slow save would have made the row start before the checkbox filled, and
  v0.101.1's jump would have come back: that is why the 0.2 seconds are now
  counted **from when the new value arrives from the database**
  (`holdAfterCheckbox` waits to see it in the table, at most 2 seconds),
  not from the tap. `CheckboxHoldTest` uses the constant
- Verified on the phone recording the screen, on a row of a shopping list:
  the checkbox fills 0.15 s after the tap, and at 0.28 s the row has
  already gone down among the bought ones; in the checkbox column no other
  movement. Unticked, it goes back up. The phone's database went back as it
  was (only the row's edit time changes)

### ✏️ v0.101.2 — 09/10/2026 · 08:56 — The ticked row goes down to its place after a second
*(Claude Code on the PC; verified on the phone the same day; 495 tests passed; commit only local)*

v0.101.1 kept the rows still while you stayed on the page: no more
jumping checkboxes, but no bought item going down to the bottom either.

- **[Change]** **[📱 Mobile]** **A second after the last tick** the rows go
  to their place: the checkbox fills where it was tapped, and shortly after
  the row goes down among the bought ones (or goes back up, unticking).
  Ticking several things in a row, less than a second apart, they all stay
  still and then go down together: under the finger there is always the
  row you see. When the rows move each brings its checkbox along, so no
  checkbox is seen emptying and filling by itself. Leaving the page, or
  changing order and filter, they go to their place right away
- **[Project]** `DatabaseViewModel.holdAfterCheckbox` starts a countdown
  (`CHECKBOX_SETTLE_MS`, one second) that restarts at every tick and at the
  end calls `releaseCheckboxHold`. Test: one more case in
  `CheckboxHoldTest` (tick, 0.6 s still held, another tick, another 0.6 s
  still held, then down together)
- Verified on the phone recording the screen, on a row of a shopping
  list: tapped, the checkbox fills in 0.1 s where it is; 1.1 s after the
  tap the names change place and the row is among the bought ones; in the
  checkbox column no other movement. Unticked down there, it empties there
  and 1.1 s later goes back up. The phone's database went back as it was
  (only the row's edit time changes)

### ✏️ v0.101.1 — 09/10/2026 · 07:44 — Checkboxes no longer jump: the ticked row stays under the finger
*(Claude Code on the PC; tested on the phone on 08 and 09/10/2026, recording the screen; 494 tests passed; commit only local)*

- **[Bug fix]** **[📱 Mobile]** **A shopping list sorted by its
  checkbox** (first the things to buy, then the bought ones). Ticking one,
  the row jumped **right away** among the bought ones: the rows in between
  shifted by one place, and under the finger another row arrived, with its
  checkbox redrawing. You saw a checkbox never touched jump, above or below;
  and tapping the same spot again ticked **another row** (it happened
  during the tests too)
- **[Change]** **[📱 Mobile]** Now, with a checkbox ticked (or unticked)
  on which the view **sorts or filters**, the rows **stay where they are**
  while you stay on the page: you can tick several things in a row, as in a
  shopping list, and under the finger there is always the row you see.
  They go to their place **leaving the page** (even just opening another
  one and going back), or changing order and filter. Scrolling the page
  isn't enough: a table just off the screen is kept ready by the app as it
  is. With a filter on the checkbox ("only to buy") the row just bought
  stays visible until then, instead of disappearing
- **[Project]** `DatabaseViewModel.checkboxHold`: `setCellValue` on a
  checkbox that decides order or filter freezes the order (the same
  `_frozenOrder` as when typing a name) and the visible rows
  (`_heldVisible`); `releaseCheckboxHold` frees them, called by the table
  when it leaves composition (`DisposableEffect` in `DatabaseViewBody`) and
  by `setSort`, `setThenSort`, `setFilter`. Test: `CheckboxHoldTest` (4:
  the row stays and a second tap touches it, the filter, no freeze without
  order or filter on the checkbox, changing the order frees the rows)
- Tested on the phone recording the screen and measuring the checkboxes
  frame by frame. Before: ticking a row it went down and the checkbox of
  the place below started empty and filled; the second tap in the same spot
  ticked another row. After: the tapped checkbox fills and stays there, the
  second tap unticks it, the others don't move; six taps in a row, all on
  the same row. The delay between tap and filled checkbox is the previous
  one (between 17 and 156 ms, measured). On 09/10, with the phone connected
  again: with the row unticked, it stays among the bought ones; with the
  page brought to the top and back down it is still there (the table was
  just below the screen); opening another page and going back, it went back
  up among the things to buy. Then the two test rows removed: the phone's
  database differs only in what the user changed in the meantime

### 🆕 v0.101 — 08/10/2026 · 22:37 — The spell checker, like OneNote: a red line under misspelled words
*(Claude Code on the PC; verified on the phone the same day; 490 tests passed; database at version 55, copy of the phone's database made before installing; commit only local)*

- **[New feature]** **[📱 Mobile]** **The wavy red line** under the words
  the dictionary doesn't know, in the pages of the Workspace and of
  Notebooks: in text, lists, checkboxes, toggles, callouts, quotes,
  notifications and the title. The word being typed isn't marked until it
  is finished, and the others are checked again as soon as you stop typing
- **[New feature]** **[📱 Mobile]** **Tapping an underlined word** opens
  its menu: up to **five words in its place** (four with the keyboard
  open, so that the menu sits above or below the word without covering
  it), the best first ("perche" → "perché", "squola" → "scuola",
  "dellacasa" → "della casa"), **"Add to dictionary"** and **"Ignore"**.
  The keyboard stays open; "Back" or a tap outside close it
- **[New feature]** **[📱 Mobile]** **Settings → Proofing**, under
  Language: "Check spelling" for all pages, Workspace and Notebooks
  together; **the language**; **"My dictionary"**, the added words, which
  are removed from there. **Off by default**; the default language is the
  app's
- **[New feature]** **[📱 Mobile]** **"Proofing" in a page's three dots**:
  "Default" (as Settings says), "On" or "Off" only for it, and its own
  language — a page in German in an app in Italian. Below, the words
  ignored in that page, with "Check again" to put them back
- **[New feature]** **[📱 Mobile]** **The languages** are the app's:
  American and British English, Italian, German, French, Spanish. Chinese,
  Korean and Japanese are there but greyed out, "Not available yet". Words
  added to the dictionary apply in all of them
- **[New feature]** **[📱 Mobile]** **Not checked**, as in OneNote and
  Word: addresses, emails and file names ("notes.txt"), words with
  numbers, all-caps acronyms, one-letter words, those in another alphabet,
  text under a spoiler, code, database cells
- **[Project]** **The dictionaries are Hunspell's** — the spell checker of
  LibreOffice, Firefox and Chrome — inside the app (about 6 MB in all, 2
  compressed in the APK), with their licences; nothing to download
  separately, no internet. `tools/proofing/get_dictionaries.py` downloads
  them again at the tested versions
- **[Project]** **An engine written for the purpose in Kotlin**
  (`data/proofing/`), without native libraries: `SpellDictionary` reads
  Hunspell's files and checks a word as Hunspell does — prefixes and
  endings, even two endings one on the other ("dámelo"), elision
  ("dell'anno", "l'homme"), German compound words ("Haustür",
  "Donaudampfschifffahrt"), capitals and "ß"; `SpellSuggestions.kt` finds
  the words in its place (typical errors first — accents, doubles, swapped
  letters — and if that's not enough the similar words); `ProofingText`
  divides the text into words; `SpellChecker` adds the user's words and
  the ignored ones. In the page: `ProofingUi.kt` (the line and the menu,
  attached to the text fields without changing how they work) and
  `ProofingSettings.kt` (the choices)
- **[Project]** Database at **version 55**: `pages.proofingMode`,
  `proofingLanguage`, `proofingIgnored` (all pages "as Settings says") and
  the `personal_words` table. Tests: `SpellDictionaryTest` (real sentences
  in the six languages: right words pass, wrong ones don't, the first
  suggestion is the right one, and quickly), `ProofingTextTest`,
  `SpellCheckerTest`, `ProofingMarksTest`, `ProofingRenderTest` (the line
  drawn and the tap opening the menu), the migration 54→55 in
  `MigrationTest` and two checks in `PageRepositoryTest`
- Verified on the phone, after copying the database (the app opens with
  the database at 55, nothing lost). Settings → Proofing on, in English:
  the main page has the red line under the Italian words of the lists, and
  not under "lo", "WC" and "Shampoo". Then on a test row of a database,
  with the three dots → "Proofing" and Italian only for it, written
  "Domani vado a squola perche ce il compito di Mario. Parola zorblat e
  blablabla.": marked "squola", "perche", "zorblat", "blablabla" (and the
  title "Untitled"). Tapping "squola" the menu says scuola, suola, squalo,
  stuola, with the keyboard still open, and "scuola" takes its place;
  "perche" becomes "perché"; "zorblat" with "Add to dictionary" loses the
  line and appears in Settings → My dictionary, from which "Remove"
  removes it; "blablabla" with "Ignore" ends up among the page's ignored
  words, and "Check again" puts it back. The same in a quote. **The phone
  found three things**, fixed before the commit: the menu covered the word
  (with the keyboard open it fit neither above nor below), "Back" closed
  the keyboard but not the menu, and in Settings the switch when off was
  barely visible and the explanation below was cut. At the end test row
  removed, word removed from the dictionary and spell checker turned off
  again: the phone's database went back as it was

### 🆕 v0.100 — 08/10/2026 · 15:57 — "Move as inline": a moved database can arrive open in the page
*(Claude Code on the PC; verified on the phone the same day; 461 tests passed; commit only local)*

It arrived as a link to a page because of a choice of 24/09/2026 (asked
for by the user at the time): moving always remade a link.

- **[New feature]** **[📱 Mobile]** **"Move as inline"**, at the bottom of
  the "Move to" window when moving **a database** (from the block menu or
  from the three dots of the full-page database): on, the database arrives
  **open inside the chosen page**; off, as **a link to a page**, as
  before. For pages it isn't there
- **[New feature]** **[📱 Mobile]** **It starts as the database is now**:
  on if it shows open inside a page, off if it is a link. So, if you don't
  touch it, the database arrives as it was
- **[Change]** **[📱 Mobile]** The explanation at the top of the window
  says the two roads (before it referred to "Turn into database" to be done
  afterwards)
- **[Project]** `PageRepository.movePageTo(…, asInline)` creates the
  database block instead of the link (only for a database);
  `MoveExclusions.shownInline` says how it is now; `MovePagePicker` has the
  switch, in the bottom part that `DestinationPickerSheet` can now have
  (`footer`). Undo puts everything back as it was, as before. Test: one in
  `PageRepositoryTest`
- Verified on the phone, with two test rows of a database (then removed;
  the phone's database went back as it was): a database open in the first,
  "Move to" from its menu → at the bottom "Move as inline", already on;
  with the second row chosen ("Destination"), the database arrives there
  open. "Move to" again with the switch off → it arrives as a link to a
  page; and from a link, "Move to" shows the switch off. Before the tests
  the database comparison showed that the user had just moved a database
  to the bottom of the main page: it stayed where they had put it

### ✏️ v0.99.1 — 08/10/2026 · 15:39 — "Turn into database inline"
*(Claude Code on the PC; verified on the phone the same day; 460 tests passed; commit only local)*

- **[Change]** **[📱 Mobile]** The entry that puts a full-page database
  back inside the page that references it is now called **"Turn into
  database inline"**: both in the block menu (long-pressing the link, as in
  the photo) and in the three dots of the database opened full page, where
  it does the same thing. In the other languages like the "/" menu's
  "Database - Inline": "Trasforma in database in linea", "In
  Inline-Datenbank umwandeln", "Convertir en base de données intégrée",
  "Convertir en base de datos en línea", "转换为内嵌数据库", "인라인
  데이터베이스로 전환", "インラインデータベースに変換"
- **[Project]** `Strings.turnIntoDatabase`, the label of the two entries
- Installed on the phone at 15:37, still with the number 0.99 (the version
  is taken from the README, and this entry wasn't there yet): the new label
  is already there. The menu wasn't opened to see it: from the database
  comparison it turned out the user was using the app, and from then on
  Claude didn't touch the phone anymore. With the go-ahead from the user,
  0.99.1 installed: long-pressing a database's link in the main page the
  menu says "Turn into database inline". The menu was only opened and
  closed, and the database stayed as it was

### 🆕 v0.99 — 08/10/2026 · 15:32 — "Freeze first column" and "Freeze first row": first column and titles always in view
*(Claude Code on the PC; verified on the phone the same day; 460 tests passed; database at version 54, copy of the phone's database made before installing; commit only local)*

- **[New feature]** **[📱 Mobile]** **"Freeze first column"**, in the
  table's settings (under "Long text"): scrolling the table to the right
  **the first column stays at the left edge** — Name, or whichever is
  first if Name has been moved — and the others pass under it. You always
  know which row the checkbox being ticked belongs to
- **[New feature]** **[📱 Mobile]** **"Freeze first row"**: scrolling down
  **the row of the column titles stays at the top**, and the rows pass
  under it. The "first row" is the one with the properties' names: it says
  which property the cell belongs to. Inside a page it stops under the
  floating buttons (back, sidebar, three dots); full page under the view's
  bar. When the table ends it goes away with it. In a grouped table every
  group has its own, still while you are in the group
- **[New feature]** **[📱 Mobile]** They turn on **both together** or just
  one; together, the corner (the first column's title) also stays still.
  They are settings **of the view**, like "Long text": every view has its
  own, and with "Lock view" they stay greyed out
- **[Project]** Database at **version 54**: `pages.freezeFirstColumn` and
  `freezeFirstRow`, off. `FrozenTable.kt`: the first cell of every row is
  shifted by as much as the table has scrolled (`frozenFirstCell`, in
  `TableSlots`); the header comes down in the drawing
  (`PinnedHeaderSection`), so it stays still in the same frame as the
  scroll, without chasing it. The scrolling container tells the table
  (`StickyViewport`): the page (`forList`, with the buttons' margin) and
  the full-page database (`forScroll`). "Delete view" on the first view
  (v0.98.1) also brings these two along. `SettingsToggleRow` can be off.
  Tests: `FrozenTableTest` (5: the header at the top, under the margin,
  off, inside a page; the first cell on the left), the migration 53→54 in
  `MigrationTest`, and `DatabaseViewsTest`
- Verified on the phone, after copying the database (the app opens with
  the database at 54), on a test database with a text column, a Checkbox
  and about twenty rows, inside a test row of a database (then removed,
  and with it the database; the phone's database went back as it was): the
  two entries under "Long text", on; with the table scrolled to the right
  Name stays on the left and Text passes under it; scrolling the page the
  "Name | Checkbox" row stays under the buttons at the top; a checkbox
  ticked under there is ticked in the right row, and tapping the still
  "Checkbox" title opens "Edit property" (the tap reaches the title, not
  the row hidden below). Full page the titles row stays under the view's
  bar, and scrolling right Name and the corner stay still. **The first
  runs of the test found two delays**, before reaching the phone: full page
  the header was calculated with the scroll of the previous frame, and
  inside the page it stayed one step behind; now it puts itself back in
  place every time the table moves

### ✏️ v0.98.1 — 08/10/2026 · 09:57 — Views: "Edit view" from the three dots, and the first one can be removed too
*(Claude Code on the PC; verified on the phone the same day; 454 tests passed; commit only local)*

The Table couldn't be removed because it was **the starting view**: in the
app it is the database itself (the views added with "+" are separate),
and the database can't go away. In Notion instead all views are equal.

- **[Change]** **[📱 Mobile]** **"Edit view"** at the top of every view's
  three dots, in the list of views: it brings it to the front and opens its
  settings (Layout, Filter, Group, Long text, properties…), like the
  sliders icon
- **[Change]** **[📱 Mobile]** **"Delete view" is there for the starting
  view too**. The database takes the settings of the view that comes after
  — layout, name, filter, sorting, groups, hidden properties, column
  widths, calculations, chart — and that one goes away: for whoever looks,
  the first view disappeared and the others moved up, as in Notion. The
  pages and the properties all stay. **The last view left can't be
  removed**: "Delete view" disappears from its three dots
- **[Project]** `PageRepository.deleteStartingView` (a view's hidden
  columns are a list of its own, the database's are the columns
  themselves: they are poured over), `DatabaseViewModel.deleteStartingView`;
  in the three dots (`ViewsSheet`) "Edit view" and `canDelete`;
  `DatabaseViewBody` opens the settings as soon as the view shows
  (`startWithSettings`, like the charts window already). Tests: two in
  `DatabaseViewsTest`
- Verified on the phone on a test database, inside a test row of a
  database (then removed with the database; the user's databases weren't
  touched): a List view added next to the Table; the Table's three dots
  have "Edit view", "Rename" and "Delete view"; "Edit view" brings the
  Table to the front and opens its settings ("Layout: Table"); "Delete
  view" on the Table, confirmed, leaves only the List, in front; the List's
  three dots, now the only one, have "Edit view" and "Rename" but not
  "Delete view". At the end the phone's database is identical to before the
  test

### 🔧 v0.98.0.1 — 08/10/2026 · 09:48 — Deleting a row its page goes away too
*(Claude Code on the PC; verified on the phone the same day; 452 tests passed; copy of the phone's database made before; commit only local)*

- **[Bug fix]** **[📱 Mobile]** **Deleting a row** ("Delete this page? The
  row and its values will be deleted. This can't be undone.") **its page
  goes away too**, with what is inside it: the blocks, the history of
  changes, the files (icons, covers, images, videos, audio, files), and the
  subpages not referenced from anywhere else, as when a page is deleted
  forever from the trash. It is what the confirmation already said. Before,
  only the row went away
- **[Bug fix]** **[📱 Mobile]** **The pages left over from rows deleted
  earlier** aren't deleted: at start **they go to the trash**, where they
  can be looked at and restored, and from where they go away with the
  trash's rule (30 days). Restored, one of these goes back to being **a
  normal page, at the bottom of the main menu**: its row is no longer
  there, and leaving it a "row page" would have made it disappear again. If
  another page still referenced it, it stays there and becomes a normal
  page
- **[Project]** `DatabaseRepository.deleteRow` returns the row's page
  (read from the database, not from the row in the screen's hands);
  `PageRepository.deleteRowPage` deletes it with `deletePermanently`,
  unless another row still opens it; `PageRepository.tidyOrphanRowPages`
  (at start, after cleaning the trash) and `PageDao.orphanRowPages`;
  `restoreFromTrash` knows about pages without a row anymore.
  `ensureRowPage` creates the row's page and link together, so the
  cleanup never sees a half-made page. Test: `RowPageDeleteTest` (2)
- Verified on the phone, after copying the database: at start exactly the
  18 leftover pages went to the trash (created between 05/10 and today,
  almost all from Claude's tests: "Contenitore", "Sotto", "Prova",
  "Archivio", "Divisore", "ProvaDest", several "Untitled"), and nothing
  else in the database changed; the Trash shows them with "30 days left".
  A test row in a database (calendar view, 9 October), opened and with
  "Prova cancellazione" written inside, then deleted: in the database
  nothing remains, neither the row nor the page nor the text

### 🆕 v0.98 — 08/10/2026 · 09:39 — The "Icons" tab: Material icons, white or in the colour you choose
*(Claude Code on the PC; verified on the phone the same day; 450 tests passed; database at version 53, copy of the phone's database made before installing; commit only local)*

- **[New feature]** **[📱 Mobile]** **The "Icons" tab** of the icon picker,
  between "Emoji" and "Upload": the **2132 filled Material icons**, by
  default **white** (the text colour: dark on the light theme), in a grid
  like Notion's. "Filter…" searches them by the words of the English name
  ("home", "alar acc"), "Random" picks one at random
- **[New feature]** **[📱 Mobile]** **The circle next to "Random"** opens
  the app-wide colour spectrum ("Icon color"): the chosen colour colours
  all the icons of the grid and goes with the icon you choose; "No color"
  makes them white again. The colour stays for next time, as in Notion
- **[New feature]** **[📱 Mobile]** **The chosen icon applies
  everywhere** like emoji: at the top of the page, in the tree, in search,
  in favorites, in the trash, in links and references, in front of a
  database's name, next to the rows in all the database's views, in
  callouts
- **[Project]** Database at **version 53**: `pages.iconSymbol`, the icon's
  name and the colour in a string ("Home" or "Home|#040369",
  `IconSymbol`); in callouts the `symbol` entry of `BlockAttrs`. The
  order: image, then Material icon, then emoji. `MaterialIconSet.kt` is
  generated by `tools/icons/make_material_icons.py`, which reads the app's
  two Material libraries (the core one, with the most common icons like
  Home and Star, and the extended one) and takes the icons that mirror in
  right-to-left languages from their new version. The common drawing is
  `SymbolIcon`; the chosen colour is in `AppSettings.iconColor`. Tests:
  `MaterialIconsTest` (3: every name gives its icon, the words and the
  search, the colour), two in `PageIconTest`, one in `IconPickerTest`, and
  the migration 52→53 in `MigrationTest`
- Verified on the phone, in a test row of a database (then removed), after
  copying the phone's database: the app opens with the database at 53;
  "Add icon" → "Icons" shows the white grid; the circle opens the spectrum
  with "Icon color"; chosen the first of the recent colours (so as not to
  change their order) and "Apply", the whole grid turns blue; "home" → the
  house becomes the page's icon, blue, and appears blue next to the row's
  name in the calendar; "No color" makes the grid white again. **Before
  installing, the test checking every name found that the most common
  icons were missing** (Home, Star, Settings…): they are in Material's
  core library, and the generator read only the extended one

### 🆕 v0.97 — 08/10/2026 · 09:17 — Notion's emoji for icons: categories, search and skin tone
*(Claude Code on the PC; verified on the phone the same day; 443 tests passed; commit only local)*

- **[New feature]** **[📱 Mobile]** **The icon picker**, the same
  everywhere an icon is tapped to change it: a page's icon (also "Add
  icon" of row pages), "Icon" of a database's settings and of a row's
  actions, "Edit icon" of links, the callouts' icon. At the top two tabs,
  **"Emoji"** and **"Upload"** (the image from the phone, as before), and
  **"Remove"** when there is an icon
- **[New feature]** **[📱 Mobile]** **All the emoji, in Notion's
  categories**: Recent, People, Animals and nature, Food and drink,
  Activity, Travel and places, Objects, Symbols, Flags (1906 emoji of
  Unicode 16; those the phone can't draw yet stay out, instead of appearing
  as squares). At the bottom **"Custom"**: the images already uploaded as
  the icon of pages and callouts, only once even if copied several times;
  tapping one the page takes its own copy
- **[New feature]** **[📱 Mobile]** **The bottom bar of the photo**: clock,
  smiley, leaf, carrot, ball, plane, light bulb, tick, flag, squares and
  "+". Every entry leads to its category and stays lit while you look at
  it; "+" uploads a new image
- **[New feature]** **[📱 Mobile]** **"Filter…"**: you search by name **in
  all eight languages of the app together**, regardless of case and
  accents — "gatto", "cat", "Katze" and "猫" find the cat even with the app
  in English. Every word typed is the beginning of a word of the name
  ("pizz" finds the pizza, "cat" doesn't find "catena" before the cats). An
  emoji typed with the keyboard is used as it is ("Use this emoji")
- **[New feature]** **[📱 Mobile]** **"Random"** picks an emoji at random;
  **the little hand** next to it opens the six skin tones, which apply to
  all emoji of people and hands and stay chosen
- **[New feature]** **[📱 Mobile]** **Databases and row pages have the
  emoji too**: in front of the database's name, in the tree, in search, in
  links and next to the row's name in all views (table, board,
  calendar…). Before, for them only an image counted
- **[Change]** **[📱 Mobile]** **The callouts' picker** is this one, with
  v0.91's "Callout" family at the top; the "Other emoji" field is gone,
  search does the same
- **[Change]** **[📱 Mobile]** README: in the known limits, the sentence "a
  page's emoji is not chosen anywhere yet" is superseded
- **[Project]** `IconPicker.kt` (`IconPickerSheet`), `EmojiCatalog.kt`
  (`EmojiCatalog`, `withSkinTone`, `EmojiCatalogs`, `IconLibrary`),
  `PageIcons.kt` (`IconChoice`: an emoji, an image or nothing; the
  fallback 📄 in the `icon` field means "nothing chosen"), `EmojiStrings`.
  Recent emoji and the skin tone are in `AppSettings`. No database change:
  `PageDao.setIcon` writes emoji and image together, and the rows' icons
  (`DatabaseDao.observeRowIcons`) also bring the emoji. The assets are in
  `assets/emoji` (the emoji by category and the CLDR names in the eight
  languages) and are regenerated with `tools/emoji/make_emoji_assets.py`,
  which downloads Unicode's files by itself. Tests: `EmojiCatalogTest` (6),
  `PageIconTest` (2), `IconPickerTest` (4)
- Verified on the phone, in a test row of a database (then removed),
  created tapping 9 October in the calendar view: "Add icon" opens the
  picker with the tabs, search, the hand, People and the photo's bar; the
  carrot leads to "Food and drink", the flag to Flags, the squares to
  Custom (the pages' icons). **"gatto" gave "No results"**: the app is in
  English, and search knew only the app's language and English; now it
  knows all the languages and finds the cats. 🐈 becomes the page's icon
  and appears next to the row's name in the calendar. "Icon" of the
  database's settings: in Recent there is 🐈, chosen it appears in front of
  the database's name, "Remove" removes it (in the database it goes back as
  it was: 📄 and no image). In the callout: at the top the "Callout"
  family, the hand with the six tones, "pollice" gives the coloured hands,
  👍🏽 becomes the icon. **At the bottom of the grid the bar stayed on the
  flag** even tapping the squares: Custom don't reach the top; now the last
  visible section counts. The skin tone was put back to yellow. The phone's
  Recent have 🐈 and 👍🏽 from the tests

### 🆕 v0.96 — 07/10/2026 · 21:19 — "File" and "Web bookmark": the file to open or download and the link with the preview
*(Claude Code on the PC; verified on the phone the same day, with the network too; 431 tests passed; commit only local)*

With this one, **all the entries of that request are there**.

- **[New feature]** **[📱 Mobile]** **"File"** (from the "/" menu and the
  "+"): the "Upload or embed a file" box and v0.95's Upload/Link window:
  "Choose a file" opens the phone's files, **of any type**; "Link" ("Embed
  link") keeps the address of a file on the network
- **[New feature]** **[📱 Mobile]** The block is the row of the ninth
  photo: **the type's icon** (PDF, image, video, audio, document, archive),
  **the name and the size** ("215 KB", "1.4 MB"). **Tapping it opens it**
  with the app that can open it (Android asks which); **the arrow on the
  right downloads it where you choose** — the phone, the SD card, Drive —:
  it is the "download it wherever I am". Long-pressing it: "Open with…",
  "Download", "Replace", "Duplicate", "Delete". A file from a link opens by
  tapping it
- **[New feature]** **[📱 Mobile]** **"Web bookmark"**: the "Create
  bookmark" window, where you paste the link. The bookmark is the card of
  the tenth photo: **the site's title, description and its image**, and
  below the address. **Tapping it opens it in the default browser** (or in
  the site's app, if there is one). Long-pressing it: "Open in browser or
  app", "Replace", "Duplicate", "Delete"
- **[New feature]** **[📱 Mobile]** **The preview is read from the site
  only once**, as soon as the bookmark is born: it is the data the site
  itself gives for previews (the same Notion, WhatsApp and Telegram use).
  The image is saved in the app, so afterwards it shows even without a
  network. Without a network the bookmark shows the site's name and the
  address, and tries again at the next opening of the page
- **[Change]** **[📱 Mobile]** README: removed from the known limits the
  greyed-out entries of the "+" and "/" menus (now they are all there), and
  in the features the sentence "the app connects to nothing", true until
  v0.91
- **[Project]** `FileBookmarkBlocks.kt` (`FileBlockContent`,
  `BookmarkBlockContent`, `BookmarkSheet`, `formatFileSize`),
  `LinkPreviews.kt` (it reads at most 512 KB of the page, presenting itself
  as a browser, with 8 seconds of patience; the image at most 5 MB). The
  "Download" goes through Android's "Save to…", which asks for no
  permissions. Test: `LinkPreviewTest` (4: the "og:" tags in any order,
  the `<title>` when they are missing, a page with nothing, the files'
  size)
- Verified on the phone, in the same test row of a database (then
  removed), with a test audio file: the File block shows the audio icon,
  the name and "215 KB"; tapping it Android asks which app to open it
  with; the arrow saves it where you choose, and the saved file is
  identical to the original (same md5 fingerprint). The bookmark, with the
  network off, shows "wikipedia.org" and the address. **With the network on
  the preview never arrived**: reading the site restarted from scratch as
  soon as it began, and so it interrupted itself; now it starts once per
  link. Then: "Wikipedia, the free encyclopedia", the description and the
  globe; tapping it Firefox opens, the default browser, on wikipedia.org,
  and Back returns to notE

### 🆕 v0.95 — 07/10/2026 · 21:18 — "Video" and "Audio": watching and listening inside the page
*(Claude Code on the PC; verified on the phone the same day, with the network too; 431 tests passed together with v0.96; commit only local)*

- **[New feature]** **[📱 Mobile]** **"Video"** (from the "/" menu and the
  "+"): the line becomes the "Embed or upload a video" box and Notion's
  small window opens right away with two tabs: **"Upload"** ("Choose a
  video", the phone's gallery with videos only) and **"Link"** (you paste
  the address, "Embed video"). An address without "https://" is taken; a
  word that isn't a link isn't ("That doesn't look like a link")
- **[New feature]** **[📱 Mobile]** **An uploaded video, or the direct
  link to a video** (an .mp4…), is watched **in the app's player**: tapping
  the video it starts or stops, and below there are **play/pause, the time
  and the bar with the knob** — you drag it to go forward or back, and the
  video jumps there when you let go. Before starting it shows the first
  frame instead of black; once at the end, play starts again
- **[New feature]** **[📱 Mobile]** **YouTube and Vimeo** are watched **in
  their player, inside the page** (play, pause, the bar with the red
  knob): it is the only way those sites let their videos be seen outside
  themselves. The player's **full screen** button widens it to the whole
  phone, system bars included; Back brings it back into the page. Below,
  the site's name and the three dots of the block menu
- **[New feature]** **[📱 Mobile]** **The video's menu** (long-pressing
  it, or from the three dots): for a link **"Open in browser or app"** —
  Android asks whether to open it in the YouTube app or in the browser —;
  for a file **"Open with…"** (another player), **"Download"** (it is saved
  where you choose) and "Replace"; for all "Duplicate" and "Delete"
- **[New feature]** **[📱 Mobile]** **"Audio"**: the "Add an audio file"
  box, then the same small window ("Choose a file" opens the phone's files,
  audio only; "Link" for the address of an audio file). The block is the
  card of the seventh photo: **the name and, below, play/pause, the time
  and the bar with the knob**. Long-pressing it, the same menu as the video
- **[New feature]** **[📱 Mobile]** **Leaving the app or the page the
  player stops** (Home, another app, the screen off, another page): no
  sound from a page you can't see. Coming back it doesn't restart by
  itself, and play **resumes from where it had stopped**
- **[Project]** `MediaPlayers.kt`: `Playback` (Android's `MediaPlayer` on a
  `TextureView`), `PlaybackControls`, `InlineVideoPlayer`,
  `InlineAudioPlayer`, and `EmbeddedVideoPlayer` — a `WebView` with the
  iframe of `youtube-nocookie.com` or Vimeo, whose page has
  `https://com.gabriele.notionlocal` as its "home" address (YouTube wants
  to know who embeds it). `VideoAudioBlocks.kt`: the Upload/Link window and
  the blocks. Uploaded files live in the blocks' images folder and open to
  other apps from v0.94's `FileProvider`. Test: `MediaLinksTest` (4:
  YouTube in all the forms of its address, Vimeo, links without
  "https://", the time like a clock)
- Verified on the phone, in a test row of a database (then removed), with
  a test video and audio put on the phone on purpose (not the user's
  files): the uploaded video starts, stops, "Open with…" opens it in
  another player; the audio starts, the bar jumps forward and back, it
  resumes. **The YouTube video stayed black**: with the `WebView`'s
  inspector it turned out the iframe was zero tall — in Compose the
  `WebView` is born "as tall as its content", and so the page inside
  believes itself zero tall. Now it is as tall as the box: it shows,
  starts, stops, goes full screen (before that button did nothing) and
  comes back. **Leaving with Home the audio became "This audio can't be
  played here"**: for Android stopping a player that never started is an
  error; now only the one playing is stopped. The direct link to a test
  .mp4 loads, shows the first frame and starts (0:02 of 0:10). "Open in
  browser or app" on YouTube: Android would offer the YouTube app and
  Firefox (checked without opening it, so as not to touch the user's
  YouTube history)

### 🆕 v0.94 — 07/10/2026 · 20:39 — "Image": the image to resize, enlarge, download and move
*(Claude Code on the PC; verified on the phone on 06/10/2026; 423 tests passed; commit only local)*

- **[New feature]** **[📱 Mobile]** **"Image"** (from the "/" menu and the
  "+"): the line becomes the grey "Add an image" box and **the phone's
  gallery opens right away**, as in Notion. It is uploaded **only from the
  phone**, as asked (no links). Cancelling the gallery the box stays, and
  tapping it reopens it
- **[New feature]** **[📱 Mobile]** The image is shown **whole**, as wide as
  the page and centred. **Two handles on the sides** narrow it (down to a
  quarter of the page) or widen it by dragging them; the width stays
- **[New feature]** **[📱 Mobile]** **Tapping it opens it full screen**, on
  black: it zooms and moves with two fingers, two taps put it back as it
  was. There is also the button to **download it** there, to the phone's
  Gallery ("Pictures/notE")
- **[New feature]** **[📱 Mobile]** **Long-pressing it**: "View",
  "Download", "Replace" (another from the gallery), **"Move to"** — you
  choose the page, from the tree or searching it, and the image **goes to
  the bottom of that page** —, "Duplicate" (with its own copy of the file)
  and "Delete". "Move to" isn't undone with the back arrow: the notice says
  where it ended up
- **[Change]** **[📱 Mobile]** **Backspace at the start of the line under a
  callout or a quote** joins the line to their text, as already under a
  toggle or a checkbox. Under an image (or another block without text) it
  does nothing
- **[Project]** `MediaBlocks.kt` (`ImageBlockContent`, `MediaPlaceholder`),
  `BlockFiles` (the file's name and type, Gallery, opening with other
  apps), `PageImageStore.saveFile`, the `FileProvider`
  (`res/xml/shared_files.xml`), `PageRepository.moveBlockToPage`. The
  six-dots menu asks and the block does (`PageEditorViewModel.mediaRequest`):
  the menu closes before the gallery answers. The full-screen image is the
  one of databases' "Image" property, with "Download" in addition. Test:
  `ImageBlockTest` (4)
- Verified on the phone, in two test rows of a database (then removed),
  with a test image put in the Gallery on purpose (not the user's photos):
  "/image" opens the gallery; once chosen, the image appears at full width;
  the handles narrow it and then widen it, twice in a row; full screen
  "Download" saves it in Pictures/notE ("Saved in Pictures/notE");
  long-pressing it, the menu with the six entries; "Move to" → "ProvaDest"
  takes it to the bottom of that page ("Moved to "ProvaDest""), with its
  width. The database's rows are as before the test

### 🆕 v0.93 — 06/10/2026 · 10:15 — "Link to page" and "Database - Full page"
*(Claude Code on the PC; verified on the phone the same day; 419 tests passed; commit only local)*

- **[New feature]** **[📱 Mobile]** **"Link to page"** (from the "/" menu
  and the "+"): you choose a page from the Workspace tree, or searching it
  by name (the same as "Move to"; database row pages too), and the line
  becomes a **reference** to it: its icon with a **small arrow** and the
  underlined name, always the current ones. Tapping it opens the page (a
  database opens full page). **The page doesn't move**: it stays where it
  is in the tree, and this one doesn't become its parent
- **[New feature]** **[📱 Mobile]** Long-pressing the reference: **"Open"**,
  **"Change page"** (you choose another page), "Duplicate" and "Delete"
  (it removes the reference, not the page). If the page ends up in the
  trash the reference says so ("In the trash") and doesn't open it; if it
  is deleted forever it becomes "Deleted page"
- **[New feature]** **[📱 Mobile]** **"Database - Full page"**: a database
  is born that **opens full page right away**, and its link stays in the
  page (the table's icon and the name), like a database after "Turn into
  page". From "Turn into" the line's text becomes the database's name
- **[Change]** **[📱 Mobile]** "Link to page" isn't in "Turn into": the
  line's text would be lost
- **[Project]** `BlockType.PAGE_MENTION`, with the page in
  `BlockAttrs.pageId` and not in `linkedPageId` (which builds the tree);
  `PageMentionBlock.kt`; `PageRepository.makeFullPageDatabase`. The
  six-dots menu of blocks that aren't text is `ObjectBlockMenu`, which
  images, videos, audio and files will also use. Test: `LinkToPageTest` (4)
- Verified on the phone, in a test row of a database (then removed):
  "/link" → "Link to page" → a page of the user makes the reference with
  the small arrow; tapping it opens the page (only looked at, nothing
  touched), and long-pressing it there is its menu; "/full" → "Database -
  Full page" opens the new database full page, and going back the
  "Untitled" link with the table stays. The user's pages and the
  database's rows are as before the test

### 🆕 v0.92 — 06/10/2026 · 10:08 — "Code": code with its language and its colours
*(Claude Code on the PC; verified on the phone the same day; 415 tests passed; commit only local)*

- **[New feature]** **[📱 Mobile]** **"Code"** (from the "/" menu and the
  "+"): a **box** with a fixed-width font. At the top left the
  **language** (JavaScript by default, as in Notion): tapping it opens the
  list of the sixty most used, with search at the top and the mark next to
  the chosen one. At the top right the button that **copies all the code**
  ("Code copied")
- **[New feature]** **[📱 Mobile]** **The code is coloured** according to
  the language, with the colours of the "One" themes (dark and light):
  keywords purple, strings green, comments grey in italic, numbers orange,
  functions light blue, types yellow. "Plain Text" and "Diff" stay without
  colours
- **[New feature]** **[📱 Mobile]** In code **Enter always goes to a new
  line inside**, and the keyboard doesn't capitalise or correct words.
  While writing code the bar doesn't show "Aa" or the brush, and the six
  dots don't have "Color": the colours are the language's. They have
  "Delete" instead
- **[New feature]** **[📱 Mobile]** **Backspace in an empty code block**
  turns it back into a text line; **"Turn into"** towards text makes every
  line of code a text line
- **[Project]** `CodeBlock.kt` (the block and the choice of language),
  `CodeHighlight.kt` (`CODE_LANGUAGES` and `highlightCode`); the language
  is in `BlockAttrs.language`. Test: `CodeHighlightTest` (5)
- Verified on the phone, in a test row of a database (then removed):
  "/code" makes the "JavaScript" box; `const x = 1`, `// note`, `alert(x)`
  on three lines, coloured; "Python" from the search "pyth" changes the
  colours (there `const` and `//` mean nothing); the copy button ("Code
  copied"); "Turn into → Text" makes three text lines; backspace in an
  empty code block turns it back into text. The database's rows are as
  before the test

### 🆕 v0.91 — 06/10/2026 · 09:58 — "Callout" and "Quote": the box with the icon and the quote
*(Claude Code on the PC; verified on the phone the same day; 410 tests passed; commit only local)*

To Claude's question (since 25/09/2026 the app doesn't use the Internet:
for videos from a link and bookmark previews it is needed again) the
answer was **"Yes, put it back"**.

The entries are built in groups, each with its own version: this is the
first, **Callout** and **Quote**.

- **[New feature]** **[📱 Mobile]** **"Callout"** (from the "/" menu and
  the "+"): a **box** with a background grey lighter than the page, the
  icon on the left (by default the light bulb 💡, as in Notion) and the
  text next to it. **Enter goes to a new line inside the box**, which grows;
  an Enter on an empty line at the bottom exits: the empty line goes away
  and below a text line is born. The text is formatted like the other
  lines (bold, colours, size)
- **[New feature]** **[📱 Mobile]** **The box's icon**, tapping it (or from
  the six dots, "Edit icon"): the emoji of the "Callout" family of Notion's
  picker (💡 👉 ☝️ 👌 🔑 🚧 ⚠️ 🔥 📌 ✂️ ❓ 🚫 ⛔ ⏰ ☎️ 🚨 ♻️ ✅ 🔒 📎 📖 🗣️ ➡️ 📢
  🛠️ ⚙️), "Other emoji" to type any with the keyboard, **"Upload"** for an
  image from the phone as for the pages' icons, and "Remove" to leave it
  without an icon
- **[New feature]** **[📱 Mobile]** **The box's background**, from the six
  dots ("Background color"): the same colour picker as the whole app; "No
  color" puts back the grey
- **[New feature]** **[📱 Mobile]** **"Quote"**: the **thick vertical
  line** on the left, as tall as the text, and the grey hint "Empty quote"
  while it is empty. Here too Enter goes to a new line inside and the one on
  an empty line at the bottom exits
- **[New feature]** **[📱 Mobile]** **Backspace at the start of a callout
  or quote** turns them back into normal text, as in Notion, **with all
  their lines**: every line becomes a text line (the same with "Turn into"
  towards another type). Another backspace joins it to the line above
- **[New feature]** **[📱 Mobile]** In the **six dots** of callout and
  quote, besides the text lines' entries, **"Delete"**: they are boxes of
  their own
- **[Change]** **[📱 Mobile]** Tapping below a callout or a quote at the
  bottom of the page you write on a new line, instead of going back inside
  it
- **[Change]** **[📱 Mobile]** **The app can use the Internet again**
  (permission removed on 25/09/2026): it will serve the videos taken from a
  link and the bookmark previews of the next versions. The notes stay on
  the phone
- **[Project]** Database at **version 52**: `blocks.attrsJson`, the extra
  entries of the new blocks in JSON (`BlockAttrs`: file, address, name,
  width, language, icon, colour, page…). The new types are already all in
  the enum (`CALLOUT`, `QUOTE`, `CODE`, `IMAGE`, `VIDEO`, `AUDIO`, `FILE`,
  `BOOKMARK`, `PAGE_MENTION`). Block files are copied with the duplicated
  page or block and with templates, and go away with the page deleted
  forever. `PageRepository.convertMultiline` splits the lines;
  `CalloutParts.kt` has the icon and its window. Tests: `CalloutQuoteTest`
  (5) and the migration 51→52 in `MigrationTest`
- Verified on the phone, in a test row of a database (then removed):
  "/callout" makes the box with the light bulb; "First", Enter, "Second
  line" stay inside, on two lines; two Enters at the bottom exit; "/quote"
  makes the thick line with "Empty quote", then two lines; 🔥 from the icon
  window; the blue background from "Background color"; backspace at the
  start of the quote turns it back into two text lines. **At the first test
  Enter inside the box split it in two**: when the keyboard sends the same
  text again to move the caret, its line break was taken for an Enter.
  Fixed. The phone's database went to 52 without losing anything

### 🔧 v0.90.0.1 — 06/10/2026 · 09:26 — "/database" under a divider no longer closes the app
*(Claude Code on the PC; verified on the phone the same day; 404 tests passed; commit only local)*

- **[Bug fix]** **[📱 Mobile]** Yes, it was the crash of 03/10/2026 at
  19:31: a database of the user was born in that very second. A single line
  under a divider (or under a database) is a text group all of its own;
  choosing **"Database - Inline"** (or "Simple database", or one of the
  views) from the **"/"** menu or from the **"+"**, the line became a
  database and its writing field disappeared **with the caret inside**: the
  keyboard kept asking it where the caret was and the app closed
  (`LayoutCoordinate operations are only valid when isAttached is true`,
  the same error already solved for toggle and divider). Now the caret
  goes off first, and the keyboard closes: in the database there is
  nothing to write. The same for **"Table"**, which had the same defect.
  "Turn into" from the six dots didn't have it: there the caret already
  goes off before opening the menu
- **[Project]** `applyInsertAction`: `focusManager.clearFocus()` before
  `convertToDatabaseLink` and before turning the line into a table, as
  "Linked view of data source" already did. The crash depends on the real
  keyboard and doesn't reproduce in the tests on the PC
- Verified on the phone, in a test row of a database (then removed): with
  v0.90 the user's steps (database, "Add below", "/divider", "/database"
  under the divider, "Database - Inline") closed the app with the same
  error as on 03/10; with the fix, the same steps create the database
  without a crash. "/table" and "/page" were also tried on a single line
  between two islands: no crash. The database's rows are as before the
  test

### 🆕 v0.90 — 06/10/2026 · 09:13 — "Title position": the title under the icon, next to it or hidden
*(Claude Code on the PC; verified on the phone the same day; 404 tests passed; commit only local)*

- **[New feature]** **[📱 Mobile]** In **Settings**, above "Animations",
  the entry **"Title position"**, which applies to all pages together, the
  main menu's one too: **Below the icon** (as before, and it is the
  default choice), **Next to the icon** (the title to the right of the
  icon, on the same line: all the rest of the page moves up) and **Hidden**
  (the title isn't shown; the page is renamed with "Rename" from its block's
  menu in the parent page or, for a row, from the database cell).
  Notebook pages, which have no icon, keep the title where it is
- **[Change]** **[📱 Mobile]** **"Add cover"** is no longer next to the
  icon: it is the first entry of every page's **three dots**, always,
  whatever the title's position. It isn't there when the page already has a
  cover (it is changed by tapping it), in the trash and in notebook pages
- **[Project]** `AppSettings.titlePosition` (`TitlePosition`: `BELOW_ICON`,
  `BESIDE_ICON`, `HIDDEN`, saved in "title_position"); in the editor the
  title field is `TitleField`, put in the icon's row or below;
  `PageOptionsSheet(onAddCover)`. `TitlePositionTest`: the entry is above
  "Animations" and the choice is saved
- Verified on the phone: in Settings "Title position" above "Animations",
  with the three choices; with "Next to the icon" the root page's title
  next to its icon, and in a test row of a database "Untitled" next to "Add
  icon"; in that row's three dots "Add cover" at the top, which opens the
  cover choice; with "Hidden" the title disappears from the menu and from
  the row's page. Then "Below the icon" put back and the test row removed:
  the database's rows are as before the test

### ✏️ v0.89.1 — 05/10/2026 · 22:58 — The long-pressed divider: "Add above", "Add below" and "Delete"
*(Claude Code on the PC; verified on the phone the same day; 403 tests passed; commit only local)*

- **[Change]** **[📱 Mobile]** Long-pressing a **divider** opens the block
  menu (the six dots' one), with the title "Divider" and only three
  entries: **Add above** and **Add below** (an empty line above or below,
  with the caret inside) and **Delete** (it removes it right away; Undo
  puts it back). Before, the long press only asked "Delete this divider?".
  In a locked page the long press does nothing, as for the other blocks
- **[Removed]** **[📱 Mobile]** The question "Delete this divider?"
- **[Project]** `DividerBlockContent(onLongPress)` opens `BlockActionsHost`,
  which for a divider shows `DividerBlockMenu`. `AddLineTest`: one more
  test (a line above and one below the divider, and "Delete" removing only
  it)
- Verified on the phone, in a test row of a database (then removed) with
  "Above" and a divider: long-pressing it, the "Divider" menu with "Add
  above", "Add below" and "Delete"; "Add above" put a line between "Above"
  and the divider with the caret inside, "Add below" one right below,
  "Delete" removed the divider leaving the lines. At the first test the
  title said "Text": fixed. The database's rows are as before the test

### 🆕 v0.89 — 05/10/2026 · 22:48 — The last ten colours used, under the Hex field
*(Claude Code on the PC; verified on the phone the same day; 402 tests passed; commit only local)*

- **[New feature]** **[📱 Mobile]** In the colour picker, under the **Hex**
  field, there are the **"Recent colors"**: the last ten colours applied,
  the most recent first, in a row of circles. Tapping one the picker goes
  to that colour (it is applied with "Apply", like the others); the one
  equal to the chosen colour has the white ring
  - **everywhere**: the picker is the same for the colour of the text and
    the background, of colour cells, of tags, of the drawing pens, of the
    numbers' bars and rings, of the progress bars — it is the app's only
    Hex field
  - a colour is remembered when it is applied; one already in the row goes
    to the top without duplicates; they stay even closing the app. Until
    none has been applied, the row isn't there
- **[Project]** `AppSettings.recentColors` and `rememberColor` (key
  `recent_colors`), `withRecentColor` (at the top, without duplicates, at
  most `RECENT_COLORS_MAX` = 10); `RecentColorsRow` in `ColorPickerSheet`,
  which remembers the colour on "Apply". `RecentColorsTest`: 2 tests (the
  list of ten, without duplicates and only real colours; "Apply" remembers
  it and the circle puts it back)
- Verified on the phone, with the colour of a "Prova" tag on a property of
  a database added for the test and then removed: at first no row; applied
  a blue (#040369) and a brown (#562316), reopening there were "Recent
  colors" with the two circles, the brown first with the ring; tapping the
  blue the Hex field went to #040369. The two test colours stay among the
  recent ones. In the database only the test row's last edit time changed

### ✏️ v0.88.1 — 05/10/2026 · 22:27 — "Move to database": before moving, the name of the new page
*(Claude Code on the PC; verified on the phone the same day; 400 tests passed; commit only local)*

- **[Change]** **[📱 Mobile]** In "Move to database", once the database is
  chosen, **"Name of the new page"** appears: the new page's name, already
  written (the current one) and all selected, with what will happen above.
  "Move" moves, "Cancel" goes back to choosing the database
  - **a page** becomes the new page itself: the written name is its new
    name (Undo puts back the previous one)
  - **a database** ends up inside a new page with the written name, and
    keeps its own name
  - a name left empty keeps the current one; a database without a name
    leaves the field empty, with "Untitled" in grey
- **[Project]** `PageRepository.moveIntoDatabase(rowTitle)`;
  `PageChange.MovedToDatabase` remembers the chosen name (for Redo) and the
  previous one (for Undo). `MoveToDatabasePicker` asks for the name
  (`NewRowNameDialog`) before calling `onPick(destination, rowTitle)`.
  `MoveToDatabaseTest`: 3 more tests (page renamed with Undo and Redo, the
  name of a database's page, the empty name)
- Verified on the phone, in a database: a subpage "Draft" of a test row,
  "Move to database" → the database → the window with the name "Draft"
  selected → "Final notes" → "Move": it became the row "Final notes". Then
  a database without a name created in the test row, from the block menu →
  the database → "Archive": the row "Archive" with the database inside
- **A mistake of mine in the cleanup, remedied**: removing the test rows,
  the list had scrolled and I deleted by mistake an untitled row of the
  user (not mine). I put it back as it was — same row, same page, same
  place and same date — in the phone's database, with the app closed and
  after the backup, with the user's permission. Then the test rows
  removed: the database's rows are identical to before the test

### 🔧 v0.88.0.1 — 05/10/2026 · 22:15 — Backspace on the empty line under a divider, and "Search databases"
*(Claude Code on the PC; verified on the phone the same day; 397 tests passed; commit only local)*

(And first the things left from v0.88: testing on the phone the move of a
database, removing the test rows, the search label.)

- **[Bug fix]** **[📱 Mobile]** On an **empty line right under a
  divider**, without a text line below it (at the bottom of the page, or
  with a table, a database… below), Backspace did nothing. Now **the line
  goes away and the keyboard closes**: the divider can't be written in,
  and there is no other line to move the caret to. The divider stays. As
  before: with a text line below, the empty line goes away and the caret
  goes up onto that; on a line with text the first Backspace "takes" the
  divider and the second removes it
- **[Bug fix]** **[📱 Mobile]** In "Move to database" the search field
  said "Search pages", but it searches databases: now it says **"Search
  databases"**
- **[Project]** `BackspaceOutcome.REMOVED_CLOSE_KEYBOARD`: the field leaves
  focus and closes the keyboard before the line disappears (a field
  destroyed with focus makes the app close). `DividerBackspaceTest`: the
  test that said "the empty line at the bottom stays" became "it goes away
  and the keyboard closes", plus two new tests (with a database below; with
  text it stays). `SearchField(placeholder)`, `Strings.searchDatabases`
- Verified on the phone, in a test row of a database ("Contenitore"): a
  divider inserted, below the empty line with the caret, Backspace — the
  line disappeared, the divider stayed, the keyboard closed, no crash. Then
  the test missing from v0.88: a database created in there, from the block
  menu "Move to database" (with the "Search databases" field) → the
  database: a new row was born, and its page shows the database open
  inside; from "Contenitore" it disappeared. Then the three test rows
  removed ("Contenitore", "Sotto" and the database's one); two rows of the
  user, born in the meantime, aren't mine and stayed as they were

### 🆕 v0.88 — 05/10/2026 · 18:20 — "Move to database": a page or a database becomes a page of a database
*(Claude Code on the PC; verified on the phone the same day, for a page and — with v0.88.0.1 — for a database; 395 tests passed; commit only local)*

- **[New feature]** **[📱 Mobile]** **"Move to database"**, under "Move
  to": in the three dots of a page and of a database, and in the menu of a
  link to a page or to a database. The page tree opens: **tapping a
  database chooses it** (the arrow opens it), pages open like folders to
  reach the databases inside them, simple databases are grey (their rows
  aren't pages), and searching finds the databases
  - **a page** becomes a new page of the database, at the bottom, with its
    name and all its content; it leaves the place it was in
  - **a complex database** ends up in a new page of the chosen database,
    with its name and its icon, and **inside it there is the database
    itself, open** (not a copy): rows open as text pages, so a database
    can't itself be a row's page
  - a row's page leaves its database; a linked view counts as the database
    it shows; you can't move something into a database that is inside it
  - **Undo** puts everything back where it was (the page born for a
    database goes away); Redo does it again
- **[Project]** `PageRepository.moveIntoDatabase`,
  `PageChange.MovedToDatabase` (with Undo and Redo),
  `searchDatabaseDestinations`, `PageTreeNode.isSimpleDatabase`,
  `imageCopier` (the copy of the icon, from the `ViewModelFactory`).
  `DestinationPickerSheet(pickDatabases)`, `MoveToDatabasePicker`.
  `MoveToDatabaseTest`: 6 tests (page, database, a row's page, the noes,
  the linked view, search)
- Verified on the phone, in a database: a row "Contenitore" with a subpage
  "Sotto" inside; from the three dots of "Sotto", "Move to database" → the
  database: "Sotto" became the database's last row, and from "Contenitore"
  the link disappeared. Moving a database I tested with the phone later,
  with v0.88.0.1 (see there)

### 🔧 v0.87.0.1 — 05/10/2026 · 18:06 — Renaming a tag: the keyboard no longer closes at the first letter
*(Claude Code on the PC; verified on the phone the same day; 389 tests passed; commit only local)*

- **[Bug fix]** **[📱 Mobile]** When editing a tag (Select and
  Multi-select), typing in the name the keyboard closed at the first letter
  or digit. The reason: every letter renamed the tag right away, and the
  list of tags comes from the database a moment later; in that moment the
  editor looked for the tag with the new name, didn't find it, went back
  to the list for an instant and the text field disappeared. Creating a
  tag it didn't happen because there you type in another field, which
  renames nothing. Typing fast, moreover, a rename could start from the old
  name before the previous one was saved, and get lost
  - now **you type as much as you want**: the pill at the top shows the
    name while typing it, and the tag **is renamed only once, leaving the
    editor** — with "Done", going back or closing the window. The rows
    using it take the new name at that moment
  - a name left empty keeps the previous one; "Delete" deletes without
    renaming
- **[Project]** `TagOptionEditor`: the name stays in the field and is saved
  in `commitName` (from "Done" and on leaving, `DisposableEffect`).
  `TagRenameTest`: 3 tests (with the options not changing, as in that
  moment: typing letter by letter the editor stays open and the tag isn't
  renamed; "Done" renames once; closing, the written name counts)
- Verified on the phone, in a database with a Select property and a
  "Prova" tag added for the test and then removed: typed "1", "2", "3" and
  "X" one at a time, after each the keyboard was still open (and after two
  deletions too); "Done" renamed the tag to "Prova12", and typing "A" and
  closing with "back" it became "Prova12A". In the database only the test
  row's last edit time changed

### 🆕 v0.87 — 05/10/2026 · 17:57 — A second sort rule in databases
*(Claude Code on the PC; verified on the phone the same day, after backing up the database; 386 tests passed; commit only local)*

- **[New feature]** **[📱 Mobile]** In a database's **Sort** window, under
  the existing rule, there is now **"Then sort by"**: with "Add a second
  sort" you choose another property (or the name), and **it counts only
  among the rows equal for the first**. Example: by platform, and within
  each platform by rating
  - you choose among all the properties except the first rule's, and it
    starts ascending; tapping it changes direction, like the first, and it
    has its own "Remove sort"
  - **removing the first, the second takes its place**, with its direction;
    choosing for the first the second's property, the second goes away (it
    would sort twice by the same thing)
  - choosing the first rule's property the window now stays open, because
    the second is below
  - it applies to every view (Table, Board, List, Gallery…) and to linked
    views, each with its own rules; a duplicated database copies them
  - empty cells stay at the bottom in the second rule too
- **[Project]** Database at **version 51**: `pages.thenSortColumnId` and
  `thenSortDescending` (migration 50 → 51: no database has one, and they
  stay sorted as before). `DatabaseViewModel.sortRows` compares by the
  rules in a row (`sortRulesOf`, `compareForSort`); `setThenSort`;
  `setSort(null)` moves the second up. `SortSheet` with the choice of the
  second rule. New `schema/v50.sql` for `MigrationTest` (the test from 50:
  the previous sorting stays, no second rule). `SecondSortTest`: 4 tests
  (the second sorts the first's ties; the first comes first, even changing
  its direction; removing the first the second moves up; the second wants a
  first and another property)
- Verified on the phone (Galaxy S25 Ultra), after backing up the database:
  the migration to 51 lost nothing (the same pages before and after). In a
  database, with a Number property and three rows added for the test (Beta
  1, Alfa 1, Gamma 2) and then removed: by Number the rows were Beta, Alfa,
  Gamma and at the bottom the one without a number; with "Then sort by"
  Name they become Alfa, Beta, Gamma; with Name reversed Beta, Alfa; with
  the first removed, "Name · Descending" moved up to its place. With
  everything removed, the database is as before (only the view's last edit
  time changed)

### ✏️ v0.86.4 — 05/10/2026 · 17:40 — Tag colour from the spectrum, as in all the other places
*(Claude Code on the PC; verified on the phone the same day; 381 tests passed; commit only local)*

- **[Change]** **[📱 Mobile]** When editing a tag (Select and
  Multi-select, the three dots next to the tag) the list of Notion's ten
  colours became a **Color** row, with the colour's little square and its
  code: tapping it opens **the same picker** as the text, the colour cells,
  the pens and the bars — the spectrum with hue and brightness,
  Red/Green/Blue and Hex. "Default" goes back to the grey of Notion's tags.
  New tags still take, in turn, Notion's ten colours
- **[Change]** **[📱 Mobile]** A tag's label is **black on light colours**
  (a bright yellow, for example), white on the others: the same rule as
  the colour cells. Before it was always white, which was fine with
  Notion's ten dark colours
- **[Project]** `TagOptionEditor` opens `ColorPickerSheet`; `TagChip`
  chooses the label with `darkTextOn`. `TagCell` is `internal` for the
  tests; the "Colors" label, which nobody used anymore, removed.
  `TagColorTest`: 3 tests (the Color row with the colour's code, any colour
  written in Hex reaching the tag, "Default" going back to grey)
- Verified on the phone, in a database with a Select property and a
  "Prova" tag added for the test and then deleted: the row "Color
  #373735", the spectrum, a yellow (#FBF304) tapped in the spectrum and in
  the brightness strip, "Apply": the yellow tag with the black label, in
  the editor and in the table. In the database only the test row's last
  edit time changed

### ✏️ v0.86.3 — 05/10/2026 · 17:30 — All notifications in Settings, and among the sounds the ringtones too
*(Claude Code on the PC; verified on the phone the same day; 378 tests passed; commit only local)*

- **[Removed]** **[📱 Mobile]** The sidebar's **Notifications** entry, and
  its page (born with v0.85)
- **[Change]** **[📱 Mobile]** In **Settings** the section that was called
  "App sounds" (sound and notifications off) is now called
  **Notifications** and has everything that was in the page: the sound,
  **Vibration**, **Priority**, **App icon badge**, **Send a test
  notification**, "Turn off notifications" and "Turn off for a while", and
  at the top, if Android has turned them off for the app, the row to turn
  them back on. Below, the note on what they apply to (timers and pomodoro
  always ring)
- **[Change]** **[📱 Mobile]** In the choice of the notifications' sound,
  besides notification sounds (the entry is now called "Notification
  sounds (default and downloaded)", before "Phone sounds") there are the
  **ringtones**: **"Ringtones (default and downloaded)"** opens the list of
  the phone's ringtones, the stock ones and the added ones. A ringtone is
  made to repeat: like every sound of the app, it is played only once and
  for a few seconds
- **[Project]** `NotificationsScreen.kt` became `NotificationSettings.kt`:
  `NotificationSettingsSection`, which Settings puts under its header, and
  `NotificationSoundDialog`. Removed the route `Routes.NOTIFICATIONS`,
  `SidebarActions.onNotifications` and the "App sounds" label.
  `NotificationSettingsUiTest`: 3 tests (in Settings there are all the
  choices and no more "App sounds"; "Ringtones" opens the list of
  ringtones; the sidebar no longer has "Notifications")
- Verified on the phone: in the sidebar, after Templates, Backup and
  Trash, no "Notifications"; in Settings the Notifications section with
  all the entries; "Notification sound" shows "Notification sounds" and
  "Ringtones", and "Ringtones" (with SecSoundPicker chosen with "Just
  once") opens Samsung's "Ringtones" list, with the custom ringtones and
  the stock ones. Closed without choosing any: the sound stays the default
  one

### ✏️ v0.86.2 — 05/10/2026 · 17:20 — The List view without the lines between rows
*(Claude Code on the PC; verified on the phone the same day; 375 tests passed; commit only local)*

- **[Removed]** **[📱 Mobile]** In databases' **List** view there is no
  longer the line under every row: rows are separated by space, as in
  Notion. The rest of the row (icon, name, the first values on the right,
  tap and long press) is as it was
- Verified on the phone, in a database with a List view and a second row
  added for the test and then removed: the two rows one under the other,
  without lines. The database went back to the Table view, and it is
  identical to before the test

### ✏️ v0.86.1 — 05/10/2026 · 17:14 — URL cells: a tap opens the link, the long press edits, copies or pastes it
*(Claude Code on the PC; verified on the phone the same day; 375 tests passed; commit only local)*

- **[Change]** **[📱 Mobile]** A **URL** cell with an address inside is no
  longer written by tapping it: it is a link, underlined like those of
  Page link cells
  - **a tap opens it** with the phone's browser. Written without the
    beginning ("en.wikipedia.org"), it opens with "https://" in front; if no
    app can open it, it says so ("This link can't be opened")
  - **the long press** (with the vibration) gives a menu:
    - **Edit link**: you write in the cell itself, with the caret at the
      end, even when the link doesn't fit entirely. The page brings the
      cell above the keyboard
    - **Copy link**: it copies it. From Android 13 the phone says "copied";
      before, a notice from the app
    - **Paste link**: it puts the copied one in its place (on a single
      line, without spaces at the sides). It is there only if there is
      something to paste
  - it applies wherever the cell shows: in the table, in a row's card and
    in the properties at the top of a row's page. In a database with the
    view or the content locked only opening and "Copy link" remain
  - an empty URL cell is written by tapping it, as before
- **[Project]** `TextLikeCell` has `isLink` (`CellContent` passes it for
  URL columns); `linkUri`, `openLink`, `copyLink`, `hasClipText` and
  `pastedLink` in `CellText.kt`; the labels in `DbStrings`, in all the
  app's languages. `CellLinkTest`: 5 tests (the address with or without
  "https://", the tap that opens, the long press with Copy and Paste, Edit
  link making you write in the cell, the empty cell written as before)
- Verified on the phone, in a database with a URL property added for the
  test and then deleted: written "en.wikipedia.org", the tap opened the
  site in Firefox; the long press gave "Edit link" and "Copy link" ("Paste
  link" wasn't there: nothing to paste), and after "Copy link" also "Paste
  link"; "Edit link" added "/wiki/Example" and "Paste link" put back the
  copied link. At the first test "Edit link" opened the whole-text box,
  which at the bottom of the page stayed under the keyboard: now you write
  in the cell, which stays in view. In the database only the test row's
  last edit time changed

### 🆕 v0.86 — 30/09/2026 · 19:53 — The Timer in the notification shade: a fixed notification with the running time
*(Claude Code on the PC; verified on the phone the same day; 370 tests passed; commit only local)*

- **[New feature]** **[📱 Mobile]** Every timer started from the Timer
  widget has its **fixed notification** in the shade, like Samsung's
  timer:
  - **running**: the countdown next to the app's name, "Timer · 3 m" as the
    title and below "Ends at 19:37"; the **Pause** and **Cancel** buttons.
    Android runs the time, so it runs even with the app closed, without the
    app having to update the notification every second
  - **paused**: "Paused · 2:37", still; the **Resume** and **Cancel**
    buttons. "Cancel" brings the timer back to the wheels, like resetting
    the widget
  - **finished**: it goes away, and "Time's up" arrives as before
  - **it stays until removed**: a swipe removes it without stopping the
    timer (which then rings anyway), and it doesn't come back, not even
    reopening the app, until the timer changes: pause, resume or "Start"
    again
  - tapping it opens the app **with the sidebar open**, where the widget is
  - if the phone removes it (restarting, or updating the app) while the
    timer runs, it puts itself back
  - a channel of its own, **"Running timers"**, silent: no sound, no
    vibration, no badge on the icon. With notifications turned off by the
    app ("Turn off notifications") it doesn't appear
- **[New feature]** **[📱 Mobile]** On Android 16 the notification asks to
  be a **"live update"** (the `POST_PROMOTED_NOTIFICATIONS` permission,
  "Show live updates"), and on the phone Android accepts it: it has the
  `PROMOTED_ONGOING` mark. The pill with the time in the status bar and the
  Now Bar, however, Samsung (One UI 8.5) gives for now only to the apps on
  its list; for the others there is, apparently, an option among the
  developer ones. I didn't touch it: it is a setting of the phone
- **[Project]** `TimerNotifications` (it puts, changes and removes the
  notifications comparing the state before and after at every change of
  `WidgetStore`) and `TimerActionReceiver` (the buttons and the swipe
  away, even with the app closed: the timer changes with
  `TimerNotifications.handle`, then Android's alarm is set again at the new
  end); `CountdownTimer.notificationDismissed`;
  `WidgetStore.restoreTimerNotifications` at start and after a reboot;
  `NotificationTarget.openWidgets` for the sidebar.
  `TimerNotificationsTest`: 9 tests (which timers have the notification,
  the chronometer counting down to the end, the pause with the time still,
  the buttons, gone when it ends or is cancelled, the swipe away that
  doesn't come back, the "live update" request). The buttons' test calls
  `handle` and not the receiver: the alarm, set again in the background,
  opened the tests' database and made the following ones fail at random
- Verified on the phone (Galaxy S25 Ultra, Android 16), with a second
  3-minute timer added with "+" and then deleted: the notification appears
  at the start and runs; "Pause" and "Resume" from the shade stop and
  restart the widget; at the end it goes away and "Time's up" arrives;
  **with the app closed** (process killed) it stays and runs, and "Pause"
  works (the app starts only for that); removed with a swipe it doesn't
  come back reopening the app, and comes back resuming the timer; tapping
  it the sidebar opens on the widget; "Cancel" removes it and brings the
  timer back to the wheels. "Pause" and "Cancel" redone with the final
  APK. The widgets' state and the database the same as before the test

### ✏️ v0.85.1 — 30/09/2026 · 19:22 — The whole history, from v0.1 to today: one version for every request
*(Claude Code on the PC; 361 tests passed; verified on the phone the same day, with v0.86; commit only local)*

- **[Project]** README: the old 📦 v0.1, a single block with all the work
  that reached GitHub on 24/09, became **79 versions**, from **v0.1** to
  **v0.40**, one per request, in order of time; those that already existed
  come after, renumbered: from **v0.41** (it was v0.2, "Databases —
  Gallery view") to **v0.85** (it was v0.71), same order and same titles.
  In all **166 versions**. There is no longer a "before" and an "after"
  GitHub
  - **Where the order and the times come from**: Claude Code's
    conversations saved on the PC (from the evening of 18/09 to 23/09) have
    every request of the user with its time. Every request became a
    version with **the request word for word**, like the later ones; a
    report of a defect is a fix (🔧), or part of the version being worked
    on. The time is that of Claude's last answer before the next request
    (a pause of more than five hours, a night, closes the work). The
    entries' text is the previous one, moved: every entry of the old v0.1
    ended up in a single version, none lost and none doubled
  - **The first ones**, made in another chat («Home») from 8 to 18/09: of
    their requests there is no trace. Which they were is told by the README
    as it was on 18/09, read in one of those conversations: they are the
    versions from v0.1.1 to v0.4.0.2, with "between 8 and 18/09/2026 · time
    not recorded", in the order it listed them. Two moments are known from
    the dates of the files of the archive uploaded to GitHub: **v0.1 "notE
    is born"**, on 8/09 at 09:19 (the project's first files), and
    **v0.4.1**, the icon from the photo and the name "notE", on 17/09 at
    22:37
  - Written now, because before they had no entry of their own: the v0.1
    line (what notE is, from the first lines of the README) and the
    v0.6.0.1 one (backspace removing one indent at a time, already
    described in the backspace entry)
  - Updated the Changelog's introduction (how numbers and times are read),
    the "Agreed working rule" and the sentence on cloud sessions. Versions
    v0.84–v0.85 tell the numbering of the time and stayed as they were
- **[Project]** App: the Patch Notes also read "time not recorded"
  (`UNKNOWN_TIME`); no more 📦 (`PatchKind` has only new versions and
  fixes). `PatchNotesTest`: the first version is v0.1 of 8/09 at 09:19,
  those without a time are only the first ones, and where the time is there
  it never goes back
- Verified on the phone (at 19:42, v0.86 installed; before it was
  disconnected): Settings → Patch Notes, "Oldest first": at the top v0.1
  "notE is born" 08/09/2026 · 09:19, then the versions of the other chat
  with "between 8 and 18/09/2026 · time not recorded", v0.4.1 of 17/09 at
  22:37 and from v0.4.2 (18/09 at 21:19) those with the user's request
  inside, word for word; "Newest first": at the top the most recent

### 🆕 v0.85 — 30/09/2026 · 18:55 — The "Notifications" page: sound, vibration, priority and badge on the icon
*(Claude Code on the PC; verified on the phone the same day; 361 tests passed; commit only local)*

- **[New feature]** **[📱 Mobile]** The sidebar's **"Notifications"**
  entry, off until now, opens the notifications page:
  - **"Notification sound"**: "Phone sounds (default and downloaded)"
    opens the list of the phone's sounds — the stock ones and the
    downloaded ones, also from other apps: on the user's phone Android asks
    whether to choose them with SecSoundPicker or with Zedge —, "Choose
    from files" any audio file, "Use default sound" and "Play" as before.
    It is the same window as Settings → "Notification sound", which now
    also has the phone's sounds
  - **"Vibration"**, on or off
  - **"Priority"**: **Low** (silent, only in the shade), **Normal** (sound
    and vibration, in the shade) or **High** (also the box at the top of
    the screen). By default High, as it was before
  - **"App icon badge"**: the dot on notE's icon while there is a
    notification to read
  - **"Send a test notification"**: a notification right away, with the
    current choices, to hear and see how it is
  - "Turn off notifications" and "Turn off for a while", the same as in
    Settings; at the top, if Android has turned off notE's notifications, a
    row saying so and leading to the phone's settings
  - They apply to the reminders of pages and databases ("/notification"
    and "Include notification"). **Timers and pomodoro** are alarms: they
    always ring and appear, as they were (the note at the bottom of the
    page says so)
- **[New feature]** **[📱 Mobile]** **Tapping the notification of a row of
  a simple database** opens the database **with that row's card**; before
  only the database opened. The rest as before: the notification of a
  database's date opens the row's page, that of a "/notification" line its
  page
- **[Removed]** **[📱 Mobile]** The actions in the notifications' box
  (Complete, Snooze, Open note, Archive, Delete) aren't there: asked for and
  then removed by the user before being made
- **[Project]** Android doesn't let you change priority, vibration and
  badge of a notification channel that already exists: every combination
  has its channel ("reminders.high.vibrate.badge", …,
  `Notifier.remindersChannel`), and choosing another one the old one is
  removed — "reminders" too, the previous channel —, so in the phone's
  settings there is always just one. With Low the app doesn't ring. The
  choices are saved in `AppSettings` (`notificationVibrate`,
  `notificationPriority`, `notificationBadge`). New code:
  `ui/screen/NotificationsScreen.kt` (the page and the sound window,
  `NotificationSoundDialog`), `Routes.NOTIFICATIONS`,
  `NotificationTarget.rowToShow`; the texts in the eight languages. New
  test `NotificationSettingsTest` (5): the default values, saving, a
  different channel for each of the 12 combinations, the channel following
  the choices (importance, vibration, badge) with the old ones removed and
  the timers' channel intact, the test notification on the right channel
- Verified on the phone: the sidebar entry opens the page; "Send a test
  notification" with High appears at the top of the screen (importance 4),
  with Low only in the shade (importance 2); the new channel is there and
  "reminders" turns out removed; "Phone sounds" opens the choice between
  SecSoundPicker and Zedge and then the list with Samsung's sounds and one
  downloaded by the user, closed without choosing; the tap on the
  notification of a row of a simple database opens the database with its
  card. Then the priority put back on High; sound, vibration and badge
  never changed; the row doesn't turn out modified

### ✏️ v0.84.1 — 30/09/2026 · 18:33 — Patch Notes: the list of titles, and a page for every version
*(Claude Code on the PC; verified on the phone the same day; 356 tests passed; commit only local)*

- **[Change]** **[📱 Mobile]** Settings → Patch Notes is now **the list of
  titles**: for every version the emoji, the number, the day and time, the
  title and an arrow, one under the other; at the top the introduction
  explaining the numbers stays. **Tapping a title opens its page**: at the
  top the same title (emoji, number, day and time, title), below the text
  of that version — the request, the changes, the checks — as in the
  README. The page's name, at the top, is the version's number
- **[Change]** **[📱 Mobile]** Going back from a version the list **stays
  where it was**; it starts again from the top only changing the order
  ("Newest first" / "Oldest first", as before)
- **[Project]** `PatchNotesScreen` (the list, `VersionRow`) and
  `PatchNoteScreen` (a version's page), route `Routes.PATCH_NOTE`
  ("patchNote/{version}"). The Patch Notes are read only once
  (`PatchNotesAsset.loaded`): a version's page opens right away, without
  rereading the file's 400 KB. New test `PatchNotesPagesTest` (2): in the
  list the two most recent versions one under the other without the text
  of the first, and the tap opens the right one; a version's page shows its
  text and not the others'
- Verified on the phone: the list with the titles (🆕 v0.67 "Color
  columns" 30/09/2026 · 13:34, 🔧 v0.67.1…); tapped v0.65 "Long text", the
  page with the title and below the user's quote and the four entries; with
  the back gesture the list is where I had left it (v0.62 in the same spot
  after opening it); "Oldest first" starts from the top with 📦 v0.1,
  "Newest first" with v0.69

### 🆕 v0.84 — 30/09/2026 · 18:22 — The Patch Notes: every entry a version with day and time, and Settings → Patch Notes
*(Claude Code on the PC; verified on the phone the same day; 354 tests passed; commit only local)*

- **[Project]** README: the Changelog becomes the app's **patch notes**.
  Every entry has a new title, "### 🆕 v0.68 — 30/09/2026 · 15:09 — …":
  the version number, the day and the (Italian) time of the commit that
  closed it, taken from git's history. 🆕 is a new version (something new
  or removed), 🔧 a fix (only `[Bug fix]`, or a check), numbered on the
  previous version (v0.67.1). The 81 earlier entries became 85 versions,
  from v0.1 to v0.68, 17 of them fixes
  - The part written before the move to GitHub, divided by topic and
    without dates, is **📦 v0.1** ("until 24/09/2026 · 11:26", the time of
    the uploaded archive): of the requests of the time there is no trace
    one by one
  - The entry of 25/09 "the user's tests, and what came of them" gathered
    five requests with five commits: now they are five versions (v0.16 the
    tests of the evening before, 🔧 v0.16.1 the pomodoro starting late,
    v0.17 the time in the time zones, v0.18 stopwatch and timer, v0.19 the
    timer's ready-made times removed). The text is the same, moved; only
    the references change ("see v0.19")
  - Where an entry had several code commits (the text count, 12:28 and
    12:32) the time of the last applies; README-only commits (the checks
    written afterwards) don't move it. "Quick Enter" was dated 26/09 but
    its commit is of 28/09 at 19:33: the commit counts
  - Everything else is as it was: under every title the request word for
    word and the changes with their labels. Updated the "Agreed working
    rule" (every entry is a version), the "Technical note" and the
    "Project structure"
- **[New feature]** **[📱 Mobile]** **Settings → Patch Notes**, at the
  bottom of the settings, with the current version's number below. It
  opens the Changelog **identical** to the one of this README: the text
  with bold, italic, strikethrough and code, the user's requests as
  quotes, lists (indented too) and tables; every version has at the top the
  emoji, the number (light blue for new versions, grey for fixes), the day
  and time, and the title. By default the most recent is at the top;
  **"Newest first" / "Oldest first"** at the top right reverses the order
- **[Project]** The app **doesn't have a copy of its own** to keep up to
  date: at every build the `patchNotes` task (`app/build.gradle.kts`)
  copies the README's "Changelog" section among the assets
  (`patch_notes.md`), so every entry written here is in the app as soon as
  it is rebuilt. The **app's version** comes from here too: `versionName`
  is the number of the most recent entry, `versionCode` how many versions
  there are (before they were "0.1.0" and 1)
- **[Project]** New code: `data/patchnotes/PatchNotes.kt` (it divides the
  Markdown into versions and blocks) and `ui/screen/PatchNotesScreen.kt`
  (the screen, and bold, italic, strikethrough and code inside the lines);
  `Routes.PATCH_NOTES`; the texts in the eight languages. New test
  `PatchNotesTest` (7): the app has the Changelog equal to the README;
  every version has number, day and time; numbers follow each other
  (v0.N+1 after a version, v0.N.1, v0.N.2 after fixes); the time never goes
  back; the app's version is the last; reading the Markdown. **It is also
  the Changelog's check**: an entry written without a number or out of
  order makes the tests fail
- Verified on the phone: at the bottom of Settings "Patch Notes" with the
  version below; opened, the introduction and then 🆕 v0.68 "Time-blocking
  like Google Calendar…" 30/09/2026 · 15:09, with the quote, the italic and
  the code as here; "Oldest first" brings 📦 v0.1 to the top with its lists
  and the code names

### 🆕 v0.83 — 30/09/2026 · 15:09 — Time-blocking like Google Calendar: the calendar's Day mode
*(Claude Code on the PC; verified on the phone the same day; 347 tests passed; commit only local)*

To Claude's two questions the answer was: **in the Android app now** (the
Windows version and the web one don't exist yet, see "Windows app +
sync") and **the colour is chosen event by event**, as in Google
Calendar.

- **[New feature]** **[📱 Mobile]** The calendar has a fourth button,
  **Day**, after Year, Month and Week: a single day, with the twenty-four
  hours one under the other divided into **half-hour squares**. Every page
  with a time is a **block as tall as it lasts**, in its colour, with the
  name and the times ("Jog, 16:00 – 16:30" on one line if it is short);
  those without a time (or lasting a day or more) are in a **strip at the
  top**. Overlapping events sit **side by side**; those crossing midnight
  show on both days. Today has the **red line of now**; it opens an hour
  before now. Hours scroll up and down in a box reaching the bottom of the
  screen, days with the finger right and left (and with the arrows and
  Today, as always); the hours on the left stay still
- **[New feature]** **[📱 Mobile]** **Tapping an empty square** opens the
  quick window of the second photo: "Add title" with the keyboard already
  open, the day and the times (half an hour from the tapped square; you
  tap the day or a time to change them, with the Date property's Samsung
  pickers), "Does not repeat", the colour, and below **"More options"** and
  **"Save"**. While you write it the event already shows in its square,
  "(No title)" until it has a name. Saving, a page of the database is born
  with that name, that date and that colour (the date property is created
  on the fly if it isn't there, as when tapping a day)
- **[New feature]** **[📱 Mobile]** **"More options"** is the window of the
  third photo: the name; **"All day"**; start and end, day and time (moving
  the start the end follows it and the event lasts as before; an end before
  the start isn't accepted); the **repetition** with Google Calendar's
  choices — Daily, Weekly on…, Monthly on…, Annually on…, Every weekday
  (Monday to Friday), Custom… — the same as the Date property;
  "Include notification"; the **colour**. The colours are Google
  Calendar's eleven (Tomato, Flamingo, Tangerine, Banana, Sage, Basil,
  Peacock, Blueberry, Lavender, Grape, Graphite) plus "Default", the usual
  grey
- **[New feature]** **[📱 Mobile]** **"Edit event"**: long-pressing a page
  in the calendar, at the top of the actions, it opens "More options" on it
  (for a repeating one, on the whole series: it is a single page). Only
  what changes is written: saved as it was, the page doesn't turn out
  modified. Tapping a block opens its page, as in the other views
- **[New feature]** **[📱 Mobile]** **The month's and week's bars** also
  have their event's colour
- **[Bug fix]** **[📱 Mobile]** In the week (and the month) a page
  repeating several times in the same week — every day, for example —
  **appeared only once**, the last, and the others ended up in the "+N":
  the occurrences of the same page were in the same row and covered each
  other. Now they are all there (it was already there before: with "every
  week" it didn't show)
- **[Project]** Schema at **version 50**: `database_rows.eventColor`
  ("#RRGGBB", null for the usual colour), migration 49→50 which leaves it
  empty everywhere. Changing it updates `updatedAt`, as the rule for future
  sync asks. The `CalendarMode.DAY` mode is saved by name like the others,
  without migration. The event is saved in the date cell in the usual
  format (`encodeDateRange`), so filters, notifications and the other
  views already read it. New code: `data/entity/CalendarEvent.kt`
  (colours, the event being written, where every block goes in the day and
  how they sit side by side) and `ui/screen/CalendarDay.kt` (the grid, the
  quick window, "More options"); `DatabaseViewModel.saveEvent`. New tests:
  `CalendarEventTest` (16), `EventSaveTest` (4),
  `MigrationTest.from49NoRowHasAnEventColor`
- **[Bug fix]** **[📱 Mobile]** (found while testing, before the commit)
  Scrolling the days with the finger the day stopped about twenty pixels
  from the edge, with a sliver of the next day. The pager counted two
  hundred thousand pages: at a hundred and twenty million pixels its
  floating-point numbers go in steps of eight and the last stretch wasn't
  made. Now they are twenty thousand, like the week (twenty-seven years per
  side; beyond that the pager restarts by itself on the same day)
- Verified on the phone on a test view set to Calendar → Day: grid, red
  line, tap on the 16:00 square → quick window → "Jog test", Sage, Save →
  green block; a second square → "Study test" → More options → Daily,
  Peacock, start at 16:00 (the end moves by itself to 16:30) → Save → the
  two blocks side by side; Thursday and Friday only "Study test"; in the
  week "Study" from Wednesday to Saturday and "Jog" green; "Edit event" on
  "Study test" → Grape → purple; on "Jog test" → All day → in the strip at
  the top. In the database: the two rows with the colour, the right date
  and the filter's value. Then the two test rows deleted and the view put
  back to Table: database equal to the backup, except the test view's
  calendar mode (before empty, now "Month", which is the one that showed)
  and its modification date. The user's calendar in the same page wasn't
  touched

### 🔧 v0.82.1.1 — 30/09/2026 · 13:48 — "Wrap in the cell": the column titles wrap too
*(Claude Code on the PC; verified on the phone the same day; 326 tests passed; commit only local)*

- **[Bug fix]** **[📱 Mobile]** With "Wrap in the cell" the cells' text
  wrapped but not that of the **column titles**, which stayed on one line
  and got lost past the edge: now it wraps too, and the titles row grows as
  tall as the longest, with the lines between the columns all the way down
- Checked first on the phone that it worked in the cells: on the test
  view, with the mode on, the name "New example with a long title" goes on
  three lines inside the cell while you write it, and the whole row grows.
  In the user's database no view had the mode on; one has "Fit to content"
  on Number and Date (those columns widen as much as the text, so they
  don't wrap) and now shows its Board view: not touched
- Verified on the phone on the test view, with the Text column renamed
  for the test "Long test Text" and narrowed to 80: the title goes on four
  lines and the titles row grows; the date with the period goes on two
  lines and the row grows. Then name, width and mode put back: database
  equal, except "Last edited" of a row and the modification dates

### ✏️ v0.82.1 — 30/09/2026 · 13:34 — "Color columns": the coloured board as in Notion
*(Claude Code on the PC; verified on the phone the same day; 326 tests passed; commit only local)*

- **[Change]** **[📱 Mobile]** "Color cards" becomes "**Color columns**" and
  does as Notion: on, **the whole column** has a veil of its tag's colour;
  at the top there is the tag itself (the coloured pill) and the number of
  pages in its colour; the cards have a stronger veil and a thin border,
  with the usual labels; "New" is as wide as the column, with the border
  and the label in the colour. The column without a value is grey, like
  the "Default" colour. Off, the board is the usual one. Before, on, the
  cards were filled with the tag's colour
- **[Project]** No schema change (the same `pages.colorBoardCards`). The
  colours are mixed with the page's background (`lerp`): column 25%, cards
  45%, borders 65%; the number and "New" are the tag's colour lightened by
  35%. That way they read on the light theme too
- Verified on the phone on a test view set to Board, with the filter
  widened for the test to four tags: the columns blue, grey, red and green,
  with their tags at the top, the stronger cards with the thin border and
  the coloured "New". Then the filter back to one tag, "Color columns" off
  and the view put back to Table: database equal, except the view's
  modification date

### 🆕 v0.82 — 30/09/2026 · 13:23 — "Color cards": board cards in their tag's colour
*(Claude Code on the PC; verified on the phone the same day; 326 tests passed; commit only local)*

- **[New feature]** **[📱 Mobile]** In a board's settings, under "Group
  by", the "**Color cards**" switch (per view, off by default). On, every
  card takes the colour of its column's tag — the one chosen for the
  Select the board is grouped by — and the name and the other properties
  on the card become light as on tags, to read on the colour. The column of
  pages without a value keeps the usual colour
- **[Project]** **Database at version 48 → 49**: `pages.colorBoardCards`.
  `BoardCard` receives the group's colour and, if there is one, spreads it
  and lightens the labels (`MaterialTheme` with light `onBackground` and
  `onSurfaceVariant`). Tests in `WrapCellsTest` and `MigrationTest` 48→49,
  with the `v48.sql` file
- Verified on the phone (copy of the database first; migration to 49
  without errors) on a test view set to Board: the switch is there under
  "Group by"; on, the three cards of the first column turn blue like their
  tag (#3A6590) and are readable. With three more tags added for the test
  to the view's filter, every column has the cards of its colour: grey,
  red, green. Then (after the phone had disconnected and reconnected) the
  filter back to one tag, "Color cards" off and the view put back to Table:
  database equal to the copy, except the new column and the view's
  modification date

### ✏️ v0.81.1 — 30/09/2026 · 10:49 — "Long text": three dots, widened column or wrapped; the grey box centred
*(Claude Code on the PC; verified on the phone the same day; 324 tests passed; commit only local)*

- **[Change]** **[📱 Mobile]** The "Wrap cells" switch becomes the "**Long
  text**" entry with three choices, in the order asked: "**Show …**" (three
  dots, by default; tapping the cell the whole text appears), "**Widen the
  column**" (every column widens as much as needed for everything to be
  read, name and header included; narrower than that it doesn't go, wider
  it stays as it was made) and "**Wrap in the cell**" (the text wraps
  inside the cell, and **the whole row** grows: the other cells stay at
  the middle of the tallest row)
- **[Bug fix]** **[📱 Mobile]** The grey box with the whole text is
  **centred on the cell**, horizontally and vertically, right above it
  (before it started from its top left corner); for the name, centred on
  the whole cell, OPEN included
- **[Project]** `CellOverflow` (TRUNCATE, WIDEN, WRAP) in
  `PageEntity.cellOverflow`, which lives in the same `wrapCells` column of
  version 48 (0 and 1 were "no" and "yes", that is the three dots and the
  wrap): no migration, identical Room schema. `shownColumnWidth` makes the
  widths of "Widen the column"; tests in `WrapCellsTest`
- Verified on the phone on a test view, with a row's name lengthened for
  the test: with the three dots, tapping it the grey box appears centred on
  the cell; with "Widen the column" the Text column shows all the text and
  the date with the period reads whole; with "Wrap in the cell" the text
  goes on two lines inside the cell and the whole row grows. Then "Show …"
  again and the name put back from the box: database equal, except the
  row's "Last edited" and the view's modification date

### 🆕 v0.81 — 30/09/2026 · 10:35 — "Wrap cells", the three dots and a cell's whole text
*(Claude Code on the PC; verified on the phone the same day; 322 tests passed; commit only local)*

- **[New feature]** **[📱 Mobile]** In a table's settings, under "Group",
  the "**Wrap cells**" switch (per view, like the widths). On: the cells'
  text wraps and **the row grows** as tall as its tallest cell (name,
  text, number, link, email, phone, date, duration, "Page link", colour
  cell, created and edited date; tags wrap one under the other); the lines
  between the columns follow the whole row. Writing in a cell, the text
  wraps in there
- **[New feature]** **[📱 Mobile]** Off (by default): what doesn't fit ends
  with the **three dots**. Tapping a text, number, link, email or phone
  cell, or a row's name, with the text cut, **in the same spot a grey box
  with the whole text** appears, where you write as in the cell; it closes
  with the keyboard's "Done", tapping outside or with the back gesture. If
  the text fits, you write in the cell as before (with the caret at the
  end). Cut date, duration and "Page link" open with their usual tap, which
  shows them whole
- **[New feature]** **[📱 Mobile]** **Minimum width** of the columns you
  write in: 80 for text, number, link, email, phone and colour cell, 120
  for Name (there is OPEN). Neither dragging the handle nor with "Fit to
  content" you go below; the sizes saved before, narrower, widen up to
  there. The other columns as before (48; "Image" stays 44)
- **[Project]** **Database at version 48**: `pages.wrapCells`. New
  `CellText.kt`: `LocalWrapCells`, `TextLikeCell` (the cells you write in:
  plain text until tapped), `FullTextPopup` (the grey box). In "Wrap
  cells" the table's row is as tall as the tallest cell
  (`IntrinsicSize.Min`). `minColumnWidth` in `ColumnWidths.kt`. Tests in
  `WrapCellsTest`, `CellTextRenderTest` (really drawn: the dots, the box,
  writing, wrapping) and `MigrationTest` 47→48, with the `v47.sql` file
- Verified on the phone (copy of the database first; migration to 48
  without errors) on a test view: the date with the period ends with the
  dots; Text narrowed with the handle stops at 80; a name lengthened for
  the test becomes cut with "…", tapping it the grey box appears over it
  with the whole text and the keyboard; with the extra part deleted in the
  box, "Done" closes it. "Wrap cells" on: the date goes on two lines and
  the row grows, name, OPEN and tag in the middle. Then everything put back
  as it was: database equal, except the new column and a row's "Last
  edited"

### ✏️ v0.80.1 — 30/09/2026 · 10:18 — Tapping again a day with the dot, the dot goes away
*(Claude Code on the PC; verified on the phone the same day; 316 tests passed; commit only local)*

- **[Change]** **[📱 Mobile]** In the Date property's calendar, tapping
  again the chosen day (with the orange dot) the dot goes away. With "End
  date" on: tapping the end again only the start remains; tapping the start
  again only the end remains. With nothing chosen anymore, "Done" empties
  the cell, as before. The other taps are the usual ones
- **[Project]** The rule is in `pickCalendarDay` (`DayPick.kt`), used by
  `DateEditorSheet`; tests in `DayPickTest`
- Verified on the phone on the date of row "2" of the test database (the
  15/09): tapped the 15 again, the dot disappears; with "End date" and the
  period 15-18, tapping the 18 again the 15 remains, tapping the 15 again
  the 18 remains. Then "Cancel": database identical

### 🆕 v0.80 — 30/09/2026 · 10:11 — "Today" in the dates' calendar
*(Claude Code on the PC; verified on the phone the same day; 313 tests passed; commit only local)*

- **[New feature]** **[📱 Mobile]** At the top right of the calendar, next
  to the month's arrows, there is "**Today**", in orange like today's
  number: it brings the calendar back to today's month, even from the day,
  month and year wheels (which close). It doesn't choose the day: you tap
  it like the others, so it also serves to reach, for example, tomorrow. It
  applies in every calendar of the app (the Date property, the duration's
  and the repetition's "Set date"), because it is the same
- **[Project]** In `SamsungCalendar` (`SamsungPickers.kt`)
- Verified on the phone on the date of row "2" of the test database: from
  June 2026 "Today" brings back to September 2026 with the 30 marked; from
  the wheels brought to 2021, "Today" closes the wheels and goes back to
  September 2026. Then "Cancel": database identical

### ✏️ v0.79.1 — 30/09/2026 · 09:04 — In an empty page the caret goes to the first line
*(Claude Code on the PC; verified on the phone the same day; 313 tests passed; commit only local)*

- **[Change]** **[📱 Mobile]** The **empty lines at the bottom of the
  page** (the paragraphs without text after the last thing written; in an
  empty page, all of them) no longer take the caret where the finger
  falls: tapping one, or tapping below the last line, the caret goes to the
  **first** of them. In an empty or new page it is the very first line,
  and no emptiness remains above. An empty line in the middle of the text
  stays tappable as always (it is a deliberate space), and Enter keeps
  making space: it applies only to moving the caret, not to changing text.
  An empty bullet point isn't an empty line
- **[Project]** `PageEditorViewModel.trailingBlankLines` and
  `trailingBlankRedirect` (with the field's texts just written, which the
  database doesn't have yet); `focusLastBlock` goes to the first empty
  line at the bottom instead of the last; in the joined field moving only
  the caret onto one of those lines is brought to the first. Tests in
  `BlankTapTest`
- Verified on the phone on the page of row "2" of the test database (one
  empty line; My Space not touched). With 5 empty lines made with Enter:
  tapping below all of them, the typed "Z" ends up on the first line
  (before it went to the fifth); tapping the fourth line, the "W" ends up
  on the first; with the "W" written, tapping the fourth again the "V"
  goes on the second, right below. Then the page put back as it was, one
  empty line: database equal, except 3 "Updates" entries of that page

### 🔧 v0.79.0.1 — 30/09/2026 · 08:44 — A toggle title's size now changes
*(Claude Code on the PC; verified on the phone the same day; 308 tests passed; commit only local)*

- **[Bug fix]** **[📱 Mobile]** Reproduced on the test page: with a
  toggle's title selected and 30 chosen in "Font size" from the Aa bar, the
  bar said 30 and the database had 30, but the title stayed small; at the
  first letter typed the 30 disappeared from the database too. A toggle's
  title (like the checkbox and the notification) has a field of its own
  that keeps its own copy of the formatting, and it reread the saved one
  only without focus: the size window doesn't remove focus. Now font, size
  and colour set from the Aa bar and the brush reach that field right away
  too, and stay while typing
- **[Bug fix]** **[📱 Mobile]** With the large title, the toggle's arrow
  (and the checkbox, and the bell) comes down to the middle of the first
  line instead of staying at the top
- **[Project]** `PageEditorViewModel.externalSpans`: the block and its new
  spans, from `transformRun` (font and size) and `applyColorToRun`
  (colour); the field of blocks on their own takes them if the text is the
  same. `markerDrop` in the field: half of the line's lengthening. Tests in
  `ToggleTitleSizeTest`
- Verified on the phone on the test page (My Space not touched; a toggle
  there belongs to the user): before the fix the bug repeats the same.
  After: "Provax" at 30 grows right away, typing "y" it stays at 30
  (`[{"text":"Provaxy","size":30}]`), the arrow is halfway up the title.
  Then the toggle put back as it was, one empty line: database equal,
  except 3 "Updates" entries of the test page

### 🆕 v0.79 — 30/09/2026 · 08:33 — New "Duration" property: how much time passes between two dates
*(Claude Code on the PC; verified on the phone; 306 tests passed; commit only local)*

- **[New feature]** **[📱 Mobile]** New property type **Duration** (after
  Date in the list of types). Tapping a cell opens its window: at the top
  how long it lasts, below "Start date", "Start time", "End date", "End
  time", each with its picker (the calendar, with the day, month and year
  wheels tapping the month's name, and the clock). The time starts from
  00:00, the date from today. "Done" saves only with both dates and the end
  after the start (otherwise it warns "The end comes before the start");
  "Clear" empties the cell
- **[New feature]** **[📱 Mobile]** In "Edit property", "**Show as**" with
  the five choices asked for, each with the user's example below: "Years"
  (30 years), "Years and months", "Years, months and days", "Years, months,
  days and hours", "Years, months, days, hours and minutes" (default). The
  rest is left out, without decimals. Units at zero are skipped ("1 year
  and 17 days"); all at zero, the zero of the last ("0 years")
- **Note on the example**: from 12/02/1996 00:00 to 29/09/2026 21:22 the
  months are **7**, not 8: at 12/02/2026 it's 30 years, at 12/09/2026
  another 7 months, then 17 days to 29/09. With 8 months you would reach
  12/10/2026. The app therefore writes "30 years, 7 months, 17 days, 21
  hours and 22 minutes"
- **[New feature]** **[📱 Mobile]** The duration is written in the app's
  language ("30 anni, 7 mesi e 17 giorni" in Italian, with the singular for
  1; in Chinese and Japanese joined, "30年7か月17日"). The column is born
  320 wide so that the whole example fits; "Fit to content" measures the
  duration. Rows are sorted by how long they last (without the end at the
  bottom); you can't group or filter by duration (the value is two
  instants). In the cards and in the properties at the top of a row's page
  you read the duration
- **[Project]** **Database at version 47**:
  `database_columns.durationFormat` (null = down to the minutes). The
  cell's value is that of a date with the period and the time
  (`encodeDateRange`, "start|end|t"): a Date column with a period, changed
  to Duration, shows right away how long it lasts, and the other way
  round. The calculations in `Duration.kt` (`durationBetween`: the months
  counted all together from the start, on clock time, so the day of the
  daylight saving change stays a day), the words in `DurationWords.kt`, the
  cell and the window in `DurationCell.kt`, "Show as" in
  `NumberProperty.kt`. Tests in `DurationTest`, `DurationColumnTest` and
  `MigrationTest` 46→47, with the `v46.sql` file
- Verified on the phone (copy of the database first; migration to 47
  without errors) on a test linked view: with "+" the Duration type is
  there and the column is born; in row "1"'s cell chosen 12/02/1996 00:00
  and 29/09/2026 21:22 (year, month and day with the wheels); the window
  and then the cell write "30 years, 7 months, 17 days, 21 hours and 22
  minutes". The five choices of "Show as" change the cell into "30 years",
  "30 years and 7 months", "30 years, 7 months and 17 days", "30 years, 7
  months, 17 days and 21 hours" and complete again. After the test two
  details changed: the date not yet chosen now starts from today (before it
  started from the other date, that is from 1996 for the end) and the
  column is born 320 wide instead of 260 (at 260 the complete duration was
  cut). Then the column brought back to its width and deleted: the database
  is equal to the copy, except row "1"'s "Last edited", the view's
  modification date and the changes made in the meantime by the user

### ✏️ v0.78.1 — 29/09/2026 · 20:32 — The Name column moves wherever you want
*(Claude Code on the PC; verified on the phone the same day; 291 tests passed; commit only local)*

- **[Change]** **[📱 Mobile]** Long-pressing the **Name** header, at the
  top of the menu there are "**Move to left**" and "**Move to right**", as
  in the other columns. Name moves one place at a time, with its cells, the
  OPEN buttons, the sub rows and its calculation; the other columns pass
  next to it with their "Move to left/right". The entries are grey when
  Name is already first or last, and with "Lock view"
- **[Change]** **[📱 Mobile]** Name's place applies to **all the
  database's views**, like the columns' order: moved from a linked view,
  it moves in the database too. Hidden columns don't count (Name skips
  those not shown). A new property is always born at the far right, next
  to the "+", even with Name at the end. "Resize column" on Name puts the
  handle on its right edge, wherever it is. Copying the database, Name
  stays where it is
- **[Project]** **Database at version 46**: `pages.nameColumnOrder`, on the
  database that has the data: Name sits right before the first column with
  equal or higher `orderIndex`; -1 (all earlier databases) = first. That
  way moving Name the columns don't change number (`NameColumn.kt`:
  `namePosition`, `tableKeys`, `nameOrderAt`).
  `DatabaseTableState.orderAfterMove` replaces `neighbourForMove`;
  `moveColumn` accepts `NAME_COLUMN_KEY` and saves columns and Name
  together (`DatabaseRepository.reorderColumns`). In the table header,
  rows and calculations put Name in its place with `TableSlots`. Tests in
  `NameColumnMoveTest` and `MigrationTest` 45→46, with the `v45.sql` file
- Verified on the phone (copy of the database first; migration to 46
  without errors) on the test page's linked view: in Name's menu "Move to
  left" is grey; "Move to right" brings it after Select, then after Text
  (the "NOT EMPTY" calculation stays under Text), then at the end after
  Date, where "Move to right" turns grey. "Resize column" there puts the
  handle on Name's right edge; the back gesture closes it. Three "Move to
  left" bring it back first. The database is equal to the copy, except the
  new `nameColumnOrder` column (-1) on every page

### 🔧 v0.78.0.1 — 29/09/2026 · 20:00 — Dividers: deleting the empty lines below, the divider stays
*(Claude Code on the PC; verified on the phone the same day; 281 tests passed; commit only local)*

- **[Bug fix]** **[📱 Mobile]** Reproduced on the test page: divider, empty
  line, "Title"; with the caret on the empty line, backspace **removed the
  divider** and the empty line stayed (the rule set for "under a divider I
  can't delete anything", misread: the empty lines were meant to be
  deleted, not the divider). Now on the **empty line** right under a
  divider backspace **removes the line**: the divider stays, the line
  below moves up to its place with the caret at the start, the keyboard
  stays open
- **[Bug fix]** **[📱 Mobile]** On a **line with text** right under a
  divider, as in Notion: the first backspace "**takes**" the divider,
  which **lights up in light blue**, and only the second removes it. So
  pressing on the divider doesn't disappear without warning. After 3
  seconds without the second backspace it goes off by itself. An empty
  line that is the page's last under a divider stays where it is
- **[Project]** `PageEditorViewModel.backspaceUnderDivider`,
  `armedDivider` (lit for `ARMED_DIVIDER_MS`), `DividerBlockContent` draws
  it lit; tests in `DividerBackspaceTest`
- Verified on the phone, on the test page (My Space not touched): before
  the fix the bug repeats the same. After: the empty line goes away and the
  divider stays; a "K" typed ends up at the start of "Title"; backspace
  there lights up the divider without removing it; after 3 seconds it is
  normal again; two backspaces in a row remove it. Then the page put back
  as it was (database and 22 empty lines): equal, except an empty line
  remade and 2 "Updates" entries

### 🆕 v0.78 — 29/09/2026 · 19:47 — "Image" full screen, and the back gesture closes the columns' handle
*(Claude Code on the PC; verified on the phone the same day; 278 tests passed; commit only local)*

- **[New feature]** **[📱 Mobile]** Tapping an image of an "Image" cell you
  choose: "**View full screen**", "Upload" to change it, "Remove". Full
  screen the image shows whole on black; with two fingers you zoom and move
  it, with two taps it goes back as it was; it closes with the X or with
  the back gesture. An empty cell opens "Upload" directly
- **[New feature]** **[📱 Mobile]** With "Resize column"'s handle on, the
  **back gesture** closes it and leaves the table as it is, instead of
  leaving the page. Without the handle the gesture does the usual
- **[Project]** `PageImageSheet` has the optional entry `onView`;
  `FullScreenImage` in `ImageCell.kt`; `BackHandler` in `TableLayout` while
  the handle is there
- Verified on the phone on the test page (copy of the database first):
  with the handle on Select, the back gesture removes it and you stay on
  the page. With a test image in an "Image" column created on purpose: the
  tap opens "View full screen" / "Upload" / "Remove"; full screen it shows
  whole; it closes with the back gesture and with the X. Two-finger zoom
  can't be done from the PC: to be tried with the finger. Then image
  removed, property deleted, test image removed from the phone: database
  equal to before except row "1"'s "Last edited"

### 🆕 v0.77 — 29/09/2026 · 19:37 — New "Image" property: a square cell with an image
*(Claude Code on the PC; verified on the phone the same day; 278 tests passed; commit only local)*

- **[New feature]** **[📱 Mobile]** Among the property types there is
  "**Image**". The column is born **square**, as wide as a row is tall; the
  cell shows the image **whole**, small, without cropping it (a figure
  taller than wide shows entirely, with its background). Tapping the cell
  opens the same window as a page's icon: "**Upload**" from the gallery
  and, if there already is one, "**Remove**". The image is saved **as it
  is**, not shrunk
- The column widens with the handle like the others (and near the square
  it snaps back); "Fit to content" keeps it square. In the properties at
  the top of a row's page there is the same box, and in the cards of board,
  gallery and list a thumbnail. It doesn't group and doesn't go in the
  chart
- **[Project]** `ColumnType.IMAGE`, the value is the file's name in
  `page-images`; `ImageCell.kt`; `PageImage` now can also show a whole
  image (`ContentScale.Fit`). Every cell has its own file: copying a row or
  a database the file is copied, so removing the image from one row it
  doesn't disappear from the other; "Remove" deletes the file. Tests in
  `ImageColumnTest`
- Verified on the phone on the test page (copy of the database first),
  with a test image made on purpose (200×300, a ball on a blue background)
  put in a separate folder of the phone: the "Image" column is born square;
  "Upload", chosen from the test album, shows it whole in the cell; the
  saved file is identical to the original. "Remove" removes it and deletes
  the file (the images folder went back as it was). Then the property
  deleted and the test image removed from the phone: the database is equal
  to before, except row "1"'s "Last edited". (During the test my script
  created by mistake an empty Text property, deleted right away)

### ✏️ v0.76.1 — 29/09/2026 · 19:23 — Life progress: the bars fill the widget
*(Claude Code on the PC; verified on the phone the same day; 273 tests passed; commit only local)*

- **[Change]** **[📱 Mobile]** The label next to the bars no longer has a
  fixed width (112, more than needed, with an empty space on the right): it
  is **as wide as the widget's longest**, and all the rest goes to the
  bars, which now reach up to there. It is measured **at 100%** ("Month:
  100%"), the worst case, so it doesn't change while the percentages rise:
  the bars don't move and stay equally long
- **With the decimal** the measure is "100.0%": the bars shorten by that
  much, and the number always fits. A very long name of a date stops at 140
  and is shortened with the dots
- Verified on the phone: the bars go from about 540 to 690 pixels, right
  before "Year: 74%"; with the decimal 660, all equal, with "Month: 96.0%"
  whole. The decimal left off, as it was

### 🆕 v0.76 — 29/09/2026 · 19:19 — Life progress: the decimal on the percentages, to turn on and off
*(Claude Code on the PC; verified on the phone the same day; 273 tests passed; commit only local)*

- **[New feature]** **[📱 Mobile]** In the "⋯" menu of the sidebar's
  **Life progress** widget there is "**Show decimals**", with the switch:
  on, the percentages have a decimal ("Year: 74.3%" instead of "Year:
  74%"), with the separator chosen in the settings (74,3 or 74.3); off, they
  go back to whole numbers. It always rounds down, so 100% arrives only at
  the end. It applies to all the widget's bars, the dates added by hand
  too, and stays as it was reopening the app
- With the decimal the labels lengthen by ",0": the bars all shorten by
  that much, so they stay equally long
- **[Project]** `LifeProgressWidget.decimals` (a widget saved before
  starts off) and `progressPercentText`; tests in `WidgetsTest`
- Verified on the phone: on, "Year: 74.3%", "Month: 95.3%", "Week: 22.7%",
  "Day: 59.1%", bars all equal; still on after reopening the app; off,
  "Year: 74%" again. Left off, as it was

### 🆕 v0.75 — 29/09/2026 · 14:06 — New "Page link" property: the link to another page
*(Claude Code on the PC; verified on the phone the same day; 271 tests passed; commit only local)*

- **[New feature]** **[📱 Mobile]** Among the property types there is
  "**Page link**" (↗ icon). Tapping the cell opens "**Page link**": the
  **Workspace tree**, with pages opening as in the sidebar and databases
  showing their rows, and at the top the **search bar** to find the page by
  name (under every result, where it is). You choose any page, **also that
  of another row of the same database**; a row never opened can be chosen
  anyway, and its page is born then
- In the cell you see the linked page, with its icon and the **current
  title** (underlined): renamed elsewhere, here it updates by itself.
  **Tapping it opens it.** Tapping the rest of the cell you change page, or
  remove it with "**Remove link**" at the top. If the page no longer
  exists, "Page not found"
- It also applies in the **properties at the top of a row's page**, and in
  the cards of board, gallery and list ("↗ title"). It is sorted by title;
  "Fit to content" measures the title. It isn't filtered or grouped (groups
  and chart bars would be named with a code)
- **[Project]** `ColumnType.PAGE_LINK`, the value is the page's id;
  `PageLinkCell.kt` (`PageLinkHost` to open and choose, `LocalLinkedPages`
  for the titles, which the ViewModel follows with
  `PageDao.observeByIds`). The choice is the one of "Duplicate"/"Move to"
  (`DestinationPickerSheet`, now with an entry to remove). Copying a
  database, a link to the page of one of its rows passes to the copy of
  that page. No schema change. Tests in `PageLinkTest`
- Verified on the phone on the test page (copy of the database first): the
  "Page link" property added to the test view; the cell of "1" opens the
  tree, the view opened shows its rows, chosen "2": in the cell "2"
  underlined, and tapping it the page "2" opens. There, in the properties
  at the top, "Page link" searching "Esempio" finds "Esempio nuovo — In" the
  test view, and chooses it; tapping the cell outside the title "Remove
  link" removes it. Then the property deleted: the database is equal to
  before, except "Last edited" of rows "1" and "2", which were written
  during the test

### 🔧 v0.74.0.1 — 29/09/2026 · 13:53 — Sub rows: "Move up" and "Move down" only under their row (check)
*(Claude Code on the PC; verified on the phone the same day; no code change; commit only local)*

- It was already so from the previous entry: a sub row moves only among
  its siblings, under its main row; on the first "Move up" is grey, on the
  last "Move down"
- Verified on the phone on the test page, in a "Prova" view without
  sorting created on purpose: "Alfa" and "Beta" under "1"; on "Alfa" (the
  first) "Move up" grey; "Move down" brings it under "Beta", still under
  "1", and the main rows don't move; there "Move down" turns grey (it
  doesn't pass under "2"). Then put back up, the two sub rows deleted and
  the view removed: the database is identical to before

### 🆕 v0.74 — 29/09/2026 · 13:48 — Tables: sub rows ("Create sub row")
*(Claude Code on the PC; verified on the phone the same day; 266 tests passed; commit only local)*

- **[New feature]** **[📱 Mobile]** Long-pressing a table row's name, at
  the top of the actions, "**Create sub row**": a new row is born **under
  that one**, indented, and its name opens right away to write it. It can
  also be done on a sub row (one level further)
- Sub rows are **database rows like the others**: OPEN opens their page,
  properties are written in the cells, they also appear in the other
  views. They are born with the row's group values (to stay under it in a
  grouped table too) and with the filter's value, like every new row
- On the main row's name there is the **arrow** (▾ / ▸) that opens and
  closes its sub rows; it stays as it was even reopening the app. When the
  table has at least one sub row, every name leaves the arrow's place, so
  the names stay in a column
- **"Move up" / "Move down"** move a sub row among its siblings, a main row
  among the main ones (with its sub rows behind)
- Deleting a row **its sub rows stay**, one level up: the confirmation
  message says so
- **[New feature]** **[📱 Mobile]** In the **Calculate** window, under the
  calculations, the "**Include sub rows**" switch, **off by default**: the
  calculations count only the main rows; on, the sub rows too. It applies
  to that column's calculation
- **[Project]** **Database at version 45**: `database_rows.parentRowId` and
  `subRowsCollapsed`. The tree is made by `rowTree` (`SubRows.kt`): it
  follows the view's order (filter, search, sorting, groups); a sub row
  whose row isn't shown (filtered or in another group) sits at the top,
  instead of disappearing. Copying a database the sub rows stay under the
  copied rows. "Include sub rows" sits with the view's other calculations
  ("subrows:column"). Tests in `SubRowsTest` and `MigrationTest` 44→45,
  with the `v44.sql` file
- Verified on the phone (copy of the database first; migration to 45
  without errors) on the test page: "Create sub row" on "1" makes the row
  be born below, indented, with the caret and the keyboard in capitals;
  written "Below". The arrow on "1" closes and reopens it. At the bottom of
  Text, "NOT EMPTY" stays 100% (the sub row, with empty Text, doesn't
  count); with "Include sub rows" on it becomes 75%. The message to delete
  "1" warns about the sub rows (cancelled). Then the sub row deleted and
  the switch off: the database is equal to the copy, except the linked
  view's last modification date

### 🆕 v0.73 — 29/09/2026 · 13:35 — Tables: "Move up" and "Move down" long-pressing a row's name
*(Claude Code on the PC; verified on the phone the same day; 254 tests passed; commit only local)*

- **[New feature]** **[📱 Mobile]** In the actions that open long-pressing
  a table row's name, at the top, "**Move up**" and "**Move down**": the
  row passes above the one above it, or below the one below it, **as you
  see it** — with a filter or a search it skips the hidden rows, inside a
  group or a subgroup it stays among that group's rows. The window stays
  open to move it again; at the top "Move up" is grey, at the bottom "Move
  down". In the simple database too
- **With a sorting** the two entries are grey, and below it says why: the
  rows are where the sorting says, moving them by hand wouldn't show. Grey
  also with the database locked
- Moving a row doesn't change its "Last edited". The order is the
  database's, the same in all its views
- **[Project]** `DatabaseRepository.moveRowNextTo` (right before or right
  after the neighbour, then the rows are renumbered from 0 so there are no
  ties), `DatabaseDao.setRowOrderIndex`; the table says in which group the
  row was touched (`RowPlace`). Tests in `RowMoveTest`
- Verified on the phone on the test page (copy of the database first): in
  the test view, sorted by name, the two entries are grey with the
  explanation. In a "Prova" view created on purpose without sorting: "2"
  with "Move down" ends up under "6" (the window stays open, "Last edited"
  doesn't change) and with "Move up" goes back up; on the first row "Move
  up" is grey. Grouped by Select, "Esempio nuovo" in its group goes above
  "2" skipping the six rows of the other groups that in the database are in
  between. Then the order put back as it was with "Move down" (at the
  bottom the entry turns grey) and the view removed: the database is
  identical to the copy

### 🆕 v0.72 — 29/09/2026 · 13:26 — Tables: column width, with the handle or fitted to the content
*(Claude Code on the PC; verified on the phone the same day; 250 tests passed; commit only local)*

- **[New feature]** **[📱 Mobile]** Long-pressing a column's header (**Name**
  too) there is "**Resize column**": a blue **handle** appears on the edge
  between that column and the next, and the edge's line is coloured from
  the header to the last row. Dragged **to the right it widens** the
  column, **to the left it narrows** it, live; released, the size stays.
  The handle stays there for more touches and goes away tapping the table
  elsewhere. Near the starting width (150, Name 200) it snaps there: you go
  back exactly to how it was
- **[New feature]** **[📱 Mobile]** In the same menu "**Fit to content**",
  with the switch: on, the column is **as wide as needed for everything to
  be read**, on a single line: the header, every cell of the rows shown
  (text, tags, dates, numbers, colours…) and the calculation at the bottom.
  It stays on: typing, the column follows. Off, the previous width comes
  back; dragging the handle turns it off
- Widths are **per view**, as in Notion: a linked view or an extra view
  have their own. With "Lock view" the two entries are grey
- **[Project]** **Database at version 44**: `pages.columnWidths` (JSON, dp
  per column; Name is "__name__") and `pages.fitColumns`. See
  `ColumnWidths.kt`; in the table the widths are read by headers, cells and
  calculations from `LocalTableWidths`. Copying a database the widths pass
  to the new columns. Tests in `ColumnWidthTest` (the first run found a
  real error: saving a width would have closed the app) and
  `MigrationTest` 43→44, with the `v43.sql` file
- Verified on the phone (copy of the database first; migration to 44
  without errors) on the test page, linked test view: "Resize column" on
  Select makes the handle appear between Select and Text with the blue line
  on all the rows; dragged left the column narrows (saved at 78) and the
  handle follows it; tapping the Name header it goes away. "Fit to content"
  on Text fits it to "Esempio 1" and, after the fix, also to the
  calculation's "NOT EMPTY 100%", which was cut before; on Name it narrows
  it to "Esempio nuovo" + OPEN. Dragging Select right again it snapped at
  150. Then everything off: the database is equal to the copy, except the
  linked view's last modification date

### ✏️ v0.71.1 — 29/09/2026 · 13:02 — Automatic capital, wherever you write, only in an empty field
*(Claude Code on the PC; verified on the phone the same day; 240 tests passed; commit only local)*

- **[Change]** **[📱 Mobile]** **In every text field of the app** (38:
  "Create a tag", properties' names, cells, titles, names, searches, the
  pages' text…) **with an empty field the keyboard starts in capitals**; if
  there is already something, even just a space, no — not even after ". ",
  where the keyboard usually puts it back. In a page's text the line
  counts: going to a new line, the new line starts in capitals. Fields for
  numbers, passwords, emails, addresses and colour codes stay as they are,
  and the one asking for "DELETE" all in capitals
- **[Project]** `AutoCaps.kt`: the keyboard reads the capital request only
  when it starts, so it is changed while typing and restarted, **only at
  the points where it would put the capital** (empty line, after a space or
  a line break): there there is no half word to lose. Every field is inside
  an `AutoCaps { }`: windows, sheets and menus each have their own
  keyboard, and a single setting for the screen wouldn't reach them.
  `AutoCapsTest` checks the rule and that **every** field of the app is
  wrapped: a new one left out makes the tests fail
- Verified on the phone, reading from the system what the app asks the
  keyboard (capital yes/no) and looking at the Samsung keyboard: empty
  "Create a tag" → capitals; a space → lowercase; deleted → capitals again;
  "Ab. " → lowercase (then emptied and closed without creating any tag).
  The "Select" property name: full → lowercase, emptied → capitals, then
  "Cancel" (the name stayed "Select"). In the test page: empty line →
  capitals, "Ab. " → lowercase, new line → capitals. Then everything
  deleted: the database is equal to before, except 2 "Updates" entries of
  the test page and the internal order number of the 16 empty lines at the
  bottom, shifted by one (the visible order is the same). The letters were
  written via adb, which doesn't go through the keyboard's "composing"
  words: how it is when typing with the finger shows using it

### 🆕 v0.71 — 29/09/2026 · 12:48 — A database's views: the "+" next to "Table" and the list of views
*(Claude Code on the PC; verified on the phone the same day; 230 tests passed; commit only local)*

- **[New feature]** **[📱 Mobile]** **A database can have several
  views**, as in Notion: same pages, each with its own layout, filter,
  sorting, groups, hidden properties and chart. It applies to every
  database, inside a page and full screen, and to linked views
- **[New feature]** **[📱 Mobile]** **"+" right to the right of the view's
  name**: it opens "**New view**" — the name (empty = the layout's) and the
  seven layouts, the same boxes as "Layout" in the settings; "Done"
  creates the view and puts it in front. It starts from scratch, as in
  Notion: no filter or sorting, all properties shown. With "Chart" chosen,
  the chart choice opens right away
- **[New feature]** **[📱 Mobile]** **Tapping the view's name** no longer
  opens the layout choice but the **list of views** ("Views"): the tick on
  the one shown, a tap to switch to another, "Add view" at the bottom. From
  every view's three dots: "**Rename**" (the starting one too) and
  "**Delete view**" (only the added ones, with a confirmation: the pages
  stay in the database). The current view's layout is changed from
  Settings → Layout, as before. The chosen view stays chosen even closing
  the app
- With the database locked or with "Lock view" views can be looked at and
  chosen, but the "+", Rename and Delete view are off
- **[Project]** To make the "+" fit inside a page, the view bar's buttons
  went from 40 to 36 dp and the name's box is a bit narrower: before
  "Table" became "Tab…". A long name is shortened with the dots without
  pushing the right icons out
- **[Project]** **Database at version 43**: `pages.viewHostId` (the extra
  view is a page made like a linked view, tied to the database it was born
  on), `pages.viewName` and `pages.activeViewId`. Deleting a database
  forever its views go away too; copying it, the copy has only its own (the
  extra views aren't copied). `DatabaseContent` now chooses the view and
  draws `DatabaseViewBody`; title, icon, favorite and "Show title" stay the
  database's. Tests in `DatabaseViewsTest` and `MigrationTest` 42→43, with
  the `v42.sql` file
- Verified on the phone (copy of the database first; migration to 43
  without errors) on the test page, linked test view (Table, filtered): the
  "+" next to "Table"; "Prova vista" in Board, right in front with all the
  rows (also those filtered out); the list with the tick; going back to
  "Table" filter and sorting were intact; renamed to "Rinominata" and
  removed; a Chart view opens the chart choice, and full screen the same
  view shows. For the starting view there is only "Rename". At the end the
  database is identical to the copy from before

### 🔧 v0.70.0.2 — 29/09/2026 · 08:38 — Dividers: backspace below removes them, and the moved line lights up
*(Claude Code on the PC; verified on the phone the same day; 219 tests passed; commit only local)*

- **[Bug fix]** **[📱 Mobile]** Backspace at the start of the line right
  under a divider did nothing (reproduced on the phone: in the database
  nothing changed). Now it **removes the divider**, like a horizontal rule
  in a document: the line stays with its text and the caret at the start,
  and another backspace joins it to the one above. Under a database, a
  table or a link to a page it stays as it was: in there there is stuff not
  to throw away
- **[Bug fix]** **[📱 Mobile]** Moving with ↓ a line past a divider really
  moved it (checked in the database, also on My Space: no line gone), but
  with empty lines it didn't show: among five empty lines the first taps
  seemed to do nothing, and passing the divider it looked like the divider
  going up eating a line. Now **the moved line lights up** in light blue
  for a moment (0.7 s), empty or not: you see where it went
- **[Project]** `PageEditorViewModel.removeDividerAbove` (from
  `mergeIntoIslandAbove`), `movedBlock` and the veil drawn by the shared
  text field (`MOVE_FLASH_MS`); tests in `DividerBackspaceTest`
- Verified on the phone on the test page (copy of the database first):
  "Above", divider, "Below"; letters were deleted, at the start of the line
  backspace wasn't. After: the divider goes away, the caret stays on the
  line (a typed "K" ends up there) and the keyboard doesn't close. Screen
  recorded moving an empty line past the divider: from the frame after the
  tap the line is below, with the caret, in a light blue band that fades;
  the page doesn't jump. Then the page put back as it was and the database
  compared with the copy: equal, except 3 "Updates" entries of the test
  page

### 🔧 v0.70.0.1 — 29/09/2026 · 08:21 — With the back gesture the caret goes off right away
*(Claude Code on the PC; verified on the phone the same day; 216 tests passed; commit only local)*

- **[Bug fix]** **[📱 Mobile]** The back gesture with the keyboard open is
  taken by the keyboard, and the app turned off the caret when it noticed:
  with the keyboard **fully down** and then after another 450 ms (250 in
  the full-page database), the wait set for the Samsung keyboard that
  disappears and reappears when changing field. Measured on the phone with
  a timing log: the keyboard takes 290 ms to go down, and the caret went
  off **750 ms** after it had started. Now, if the caret has been still in
  the same field for more than 600 ms, the keyboard that **starts** going
  down (`imeAnimationTarget`) means the user closed it: the caret goes off
  right away, together with it. Right after a field change, or with a
  change asked by the app on its way, the previous wait stays
- **[Project]** `KeyboardClose.kt`: `ClearFocusWhenKeyboardCloses`, used by
  the editor and by the full-page database, and `KeyboardCloseWatch`, which
  knows when the caret changed field: the writing fields mark it
  (`markFocusChanges`: title, parking field, table cells, title, cells and
  search of databases) and the editor at every line change. Counting the
  taps on the screen instead didn't work: with gesture navigation the swipe
  from the edge arrives to the app as a tap
- Verified on the phone on the test page: after, the caret goes off even
  before the keyboard starts moving (before 750 ms after); the same from a
  database cell; title → cell "1" → cell "2" without the keyboard closing;
  "/divider" with the caret passing to the line below and the keyboard
  always open. Then the tests undone and the page the same as before

### 🆕 v0.70 — 29/09/2026 · 08:07 — Subgroups: "Hide empty sub groups", separate from the groups
*(Claude Code on the PC; verified on the phone the same day; 216 tests passed; commit only local)*

- **[New feature]** **[📱 Mobile]** Under "Hide empty groups" there is
  "**Hide empty sub groups**", only when there are subgroups: it hides (or
  shows) the empty subgroups, regardless of the groups. So under every
  genre you see all the platforms, even those without games (with 0 and
  their "New page"), without showing the empty genres for that
- **[Project]** **Database at version 42**: `pages.hideEmptySubGroups`.
  The migration gives it the value of "Hide empty groups", which until here
  applied to both: no table changes look by itself. Tests in `SubGroupTest`
  and `MigrationTest` 41→42, with the `v41.sql` file
- Verified on the phone (copy of the database first; migration to 42, all
  pages there, and in all of them the new switch equal to the groups' one)
  on the test view: Group: Text, Sub groups: Select, and the two switches
  one under the other; with only the subgroups' one off, under "Esempio 1"
  there are the first tag (1) and then three more with 0. Then the view put
  back as it was and the database compared with the copy: equal, except
  the view's last modification time

### 🆕 v0.69 — 29/09/2026 · 08:00 — Tables: subgroups, groups inside groups
*(Claude Code on the PC; verified on the phone the same day; 214 tests passed; commit only local)*

- **[New feature]** **[📱 Mobile]** In a table view's settings, as soon as
  "Group" has a property, a **fourth entry "Sub groups"** appears (after
  Layout, Filter and Group). You choose None or a property, the same ones
  usable for groups except the one already making the groups. Inside every
  group its rows are divided into the subgroups, a bit indented: each
  closes and reopens tapping its header, and has its own column headers,
  calculations and "New page". A page created inside a subgroup is born
  with the group's and the subgroup's values. "Hide empty groups" applies
  to both levels
- Removing the grouping (Group: None) the subgroups go away too; grouping
  by the subgroups' property, too
- Only in the table: the board has its own "Group by", which makes the
  columns, and there are no subgroups there
- **[Project]** **Database at version 41**: `pages.tableSubGroupColumnId`
  (copied also by "Duplicate"). `DatabaseViewModel.tableSubGroupColumn`,
  `setTableSubGroupColumn`, `addRowInSubGroup`; in `DatabaseViewScreen.kt`
  `GroupBody` makes the body of a group and of a subgroup, and
  `buildRowGroups` also divides just the rows of one group. Tests in
  `SubGroupTest` and `MigrationTest` 40→41, with the `v40.sql` file
- Verified on the phone (copy of the database first; migration to 41, all
  pages there) on the test page's linked view: without groups no "Sub
  groups"; Group: Select and the entry appears fourth; in its choice
  "Select" isn't there; Sub groups: Text, and inside the first group (3)
  the subgroups "Esempio 1", "Esempio 2" and a third one, each with its
  table; "Esempio 1" closes by itself. Then Group: None and the database
  compared with the copy: equal, except the view's last modification time.
  "New page" inside a subgroup wasn't tested on the phone, so as not to add
  rows to the database (there is the test)

### 🆕 v0.68 — 29/09/2026 · 07:48 — Six dots: "Add above" and "Add below"
*(Claude Code on the PC; verified on the phone the same day; 209 tests passed; commit only local)*

- **[New feature]** **[📱 Mobile]** In the six-dots menu, **at the top**,
  for every block (text line, page, database, linked view): "**Add
  above**" and "**Add below**". They add an empty line above or below the
  block, at the same level (inside a toggle it stays inside) and with its
  indent, and put the caret there with the keyboard open
- The place is read from the database in line after the previous writes,
  as for Enter; on the empty line of a new page that is saved first
- **[Project]** `PageRepository.insertLineNextTo`,
  `PageEditorViewModel.addLineNextTo`, `AddLineGroup` in
  `BlockActionsSheet.kt`; tests in `AddLineTest`
- Verified on the phone on the test page (copy of the database first):
  from the bar, on a line "Line", "Add above" and "Add below" (the letters
  typed right away end up in the new lines); from the test view's six dots,
  the two entries at the top and a new line above and below the database,
  with the caret inside. Then the page put back as it was and the database
  compared with the copy: equal, except 10 "Updates" entries of the test
  page with the tests' letters

### 🆕 v0.67 — 29/09/2026 · 00:56 — Checkbox columns: "Activate all" and "Deactivate all"
*(Claude Code on the PC; verified on the phone the same day; 204 tests passed; commit only local)*

- **[New feature]** **[📱 Mobile]** Long-pressing a **Checkbox** column's
  header, under "Change type" there are "**Activate all**" and
  "**Deactivate all**": they tick or untick all the column's checkboxes,
  on the rows the view shows (with the filter or the search, those
  remaining). Only for checkboxes: the other columns don't have them. With
  the database locked they stay grey, like the cells
- All in a single write, and checkboxes already that way aren't touched:
  their row doesn't turn out modified now ("Last edited time")
- **[Project]** `DatabaseRepository.setAllChecked`,
  `DatabaseViewModel.setAllChecked`; tests in `CheckAllTest`
- Verified on the phone on a test database created in the test page (a
  Checkbox column, three rows, the middle one ticked): in the menu the two
  entries under "Change type"; "Activate all" ticks all three, and the row
  already ticked keeps its time; "Deactivate all" unticks them all. Then
  the test database put in the trash and the page put back as it was
  (compared with the copy made before)

### 🔧 v0.66.0.1 — 29/09/2026 · 00:45 — The bar's arrows really move the line, and the caret follows it
*(Claude Code on the PC; verified on the phone the same day; 201 tests passed; commit only local)*

- **[Bug fix]** **[📱 Mobile]** The up and down arrows swapped two **whole**
  lines, taken from the screen's copy, with two separate saves: if the text
  just typed or the line just born with Enter weren't in the database yet,
  the swap put back the previous text and positions. And the text field, if
  it had a write of its own on its way, ignored the new order: it stayed
  with the old one, and the next letter ended up in the wrong line,
  deleting its text. Now the move waits in line for the previous writes
  and changes in the database **only the position**
  (`PageRepository.moveBlock`, `BlockDao.setOrderIndex`); the field always
  takes the new order, as for Undo
- **[Bug fix]** **[📱 Mobile]** **The caret follows the moved line**, at
  the same point. Before it stayed at the same height, that is on the
  swapped line: on empty lines nothing was seen moving, and above a
  database the line went up but the caret stayed below, so the next tap
  moved another line. If the line ends up at the top of the screen, the
  page leaves above the space of the floating buttons: before it ended up
  behind them
- **[Project]** Tests in `MoveBlockTest`; two of them (the caret above the
  database, two lines with the same position) fail with the previous code
- Verified on the phone (copy of the database first) on the test page,
  with the test database at the top: written "One", "Two", "Three"; ↑
  brings "Three" above "Two" with the caret; two more ↑ bring it above the
  database, and a letter typed right away ends up in it; ↓ brings it back
  below; a letter and right away ↑, an Enter and right away ↑, three quick
  ↓ in a row: no line lost, no double position, the text always in the
  right line; Undo puts the line back where it was. Then the page put back
  as it was (the database and 22 empty lines) and the database compared
  with the copy: equal, except 11 "Updates" entries of the test page with
  the tests' letters

### 🆕 v0.66 — 28/09/2026 · 20:48 — Databases: the Chart view, with bars, line and donut
*(Claude Code on the PC; verified on the phone the same day; 196 tests passed; commit only local)*

- **[New feature]** **[📱 Mobile]** **"Chart" among the databases'
  layouts**, after Gallery (and "Chart view" in the "/" menu). Chosen, the
  small "**Chart type**" window opens with the four charts: **Vertical**,
  **Horizontal**, **Line** and **Donut**; by default they are vertical
  bars, one per page, as tall as the count. In the settings, under
  "Layout: Chart", the "Chart type" entry reopens the same window
- **[New feature]** **[📱 Mobile]** **The entries of the photos**, under
  Layout:
  - **X axis**: *What to show* (Name, that is a bar per page, or a
    property: tags count once per tag, checkboxes Checked and Unchecked,
    dates one day per group in the column's format, empties all together in
    "Empty"); *Sort by* (Name A → Z, Z → A, and the value from the lowest
    or from the highest); *Omit zero values*
  - **Y axis**: *What to show* (Count, or a Number column with Values, Sum,
    Average, Min, Max, Median or Distinct values); *Decimal places*
    (Default, 1, 1.0, 1.00…); *Cumulative*; *Range* (minimum and maximum of
    the axis, empty = the chart chooses them); *Reference line* (dashed
    orange lines, with their name)
  - with **Horizontal** the axes swap; with **Donut** there is "**Data**"
    (What to show, Slice by, Sort by), as in the photo: in the middle the
    total ("186 Total Number"), next to the slices "81 (44%)", below the
    legend
  - **Style**: *Color* (Auto, that is a colour per group, or one of the
    ten); *More style options*: Height, Grid lines, Data labels, Smooth
    line, Show legend, Show total, each only where needed
- As in Notion, **every group keeps its colour** even changing the order
  or removing the zeros (in the photo the "3" stays green even first); the
  donut with a single colour uses its own shades, otherwise the slices
  couldn't be told apart. The axis leaves a bit of air above (400 reaches
  480, as in the photo). With many groups the bars scroll sideways
- **Not made**, of the entries in the photos: the fifth type "#" (the
  request was for four), the "Advanced" order, and *Source*, *Save chart
  as…*, *Copy link to view*, *Manage data sources*, *Learn about charts*,
  which in Notion serve the site and links; Filter and the view lock are
  already there, the same for all layouts
- **[Project]** **Database at version 40**: `pages.chartSettings`, the
  settings in JSON; all the defaults aren't saved (`null`). The
  calculations in `data/entity/Chart.kt` (`chartPoints`, `chartAxis`,
  `chartTicks`, `compactNumber`), the drawing in `DatabaseChart.kt`, the
  settings in `ChartSettingsUi.kt`, the labels in `ChartStrings`. Tests in
  `ChartTest` and `MigrationTest` 39→40, with the `v39.sql` file
- Verified on the phone (copy of the database first; migration to 40, all
  pages there) on a database of the user: Layout → Chart opens the window;
  bars of 1; with the Number 81, 25, 9, 71 on the 0–100 axis; Horizontal,
  Line with the area below and the "Number" legend, Donut; order from the
  highest; colour Green; without total and without labels, Large;
  Cumulative (81, 152, 177, 186, axis 0–240); two decimals ("0.00"); Range
  up to 200; line "Average" at 100; X axis by Date (three groups with
  "Empty"); Smooth line; also opened full page. Fixed during the test: the
  numbers cut by the grid and by the line (now they have around them a thin
  border in the background's colour), the colours following the position,
  the axis up to 300 for a maximum of 186, "Decimal places" saying "2"
  instead of "1.00". Then everything put back as it was from the app
  (Table, chart settings `null` again): the database is equal to the copy,
  except that page's last modification time. "1 line" in the singular
  wasn't tried again on the phone

### 🔧 v0.65.3.2 — 28/09/2026 · 20:05 — Scrolling no longer stops for an instant before starting
*(Claude Code on the PC; verified on the phone the same day; 185 tests passed; commit only local)*

- **[Bug fix]** **[📱 Mobile]** Going back up from the bottom of the page,
  the first frame of the scroll lasted 29 milliseconds instead of 8: the
  page's list redid from scratch the database above, which it had
  dismantled when it went off the screen. Shortly after there was another
  35-millisecond frame, for the text lines above. Now every item of the
  page, once made, stays ready even off the screen while you stay on the
  page (`KeepComposed`, with `LocalPinnableContainer`, the same way the
  list keeps a field with the caret alive); those never seen are made when
  they arrive, as before. A window to prepare the nearby items in advance
  would exist only in a newer version of Compose (here 1.6.8), to be
  updated separately
- Verified on the phone measuring the frames (`dumpsys gfxinfo`) with the
  same scroll from the bottom of the main page, started from a cell of a
  database. **Before**: the first frame 29 ms (27 of preparation), one of
  35 ms halfway. **After**: the first 14 ms (6 of preparation), none above
  16 ms out of 84. Typing works as before: tapped a line, caret at the end
  and keyboard open

### 🔧 v0.65.3.1 — 28/09/2026 · 19:57 — Going back up a page, databases no longer make everything jump
*(Claude Code on the PC; verified on the phone the same day; 185 tests passed; commit only local)*

- **[Bug fix]** **[📱 Mobile]** Going back up a page with databases inside,
  when the database above came back into the screen the page **jumped up**
  suddenly and skipped it entirely. It didn't depend on the cells: it also
  happened starting from the text below. Every database inside a page goes
  through the password gate (`LockGate`), which until it had asked the
  database whether the page is locked **drew nothing**; a database gone
  off the screen was dismantled, and on returning for a moment it was zero
  tall, then as tall as its table. The page keeps its top edge still, and
  the lengthening pushed everything else down. Now the gate remembers the
  last answer per page, until the protections change
  (`AppLock.protectionVersion`), and the database goes back right away as
  tall as it is; the question is asked again every time anyway
- Verified on the phone recording the screen on the main page, at the
  bottom, with a downward scroll of half a screen started from a Select
  cell of a database. **Before**: for four frames almost still, then a
  jump of about twice the finger's path, up to the database above and
  beyond; the same starting from the text under the table. **After**: the
  table goes down slowly with the finger, and from above the bottom of the
  other database appears a bit at a time

### ✏️ v0.65.3 — 28/09/2026 · 19:49 — Pomodoro: the study length from the big box, "Study sound" and "Break sound"
*(Claude Code on the PC; verified on the phone the same day; 185 tests passed; commit only local)*

- **[Removed]** **[📱 Mobile]** From the Pomodoro's gear the length of the
  study session. The break length and the sounds stay
- **[Change]** **[📱 Mobile]** **The time written in the big box during
  study is the session's length**, also for the following sessions and for
  "from the start", seconds included. Without it, with its entry removed
  from the gear, the following sessions would have gone back to the old
  length. During the break the written time applies only to that break
- **[Change]** **[📱 Mobile]** Two sounds instead of one: **"Study
  sound"**, when the study session starts again, and **"Break sound"**,
  when the break starts, each chosen among the phone's ringtones and each
  with its own "use the default sound". The previous single sound becomes
  the "Break sound" (it is still saved with its name of the time,
  `soundUri`)
- **[Project]** `PomodoroWidget`: `sessionMs`, `breakSoundUri` /
  `breakSoundName` (saved as `soundUri` / `soundName`), `studySoundUri` /
  `studySoundName`, `soundFor`, `withTimeWritten`.
  `WidgetStore.pomodoroSetBreakLength` and `pomodoroSetSounds`. Tests in
  `WidgetsTest`, also a pomodoro saved before that is read back with its
  sound as "Break sound"
- Verified on the phone: the gear has "Break length", "Study sound" and
  "Break sound" (both "Default"), no study length; "Study sound" opens the
  list of ringtones with its name at the top (Samsung's picker chosen with
  "Just once"), and going back nothing changed. Closed with Cancel: the
  pomodoro stayed as it was (1 minute of study, 5 of break, no sound
  chosen)

### ✏️ v0.65.2 — 28/09/2026 · 19:40 — In the calendars for choosing the date, only the month's days
*(Claude Code on the PC; verified on the phone the same day, just reconnected; 182 tests passed; commit only local)*

- **[Removed]** **[📱 Mobile]** In the calendar that opens to choose a date
  (database dates, with Repeat, and notifications: it is the same,
  `SamsungCalendar`) **the days of the previous and next month are no
  longer shown**: in their place the cells stay empty, without a number,
  without the repetition dot and without a tap. The weeks stay six, so the
  calendar doesn't change height moving from one month to another
- Verified on the phone: opened a date of a database of the user. December
  2025 starts on Monday 1 with the Sunday before empty, and after the 31st
  the cells are empty; with ">" January 2026 starts on Thursday 1, nothing
  before. Closed with Cancel: the dates in the database stayed the previous
  ones

### ✏️ v0.65.1 — 28/09/2026 · 19:38 — Notebooks at the top of the sidebar
*(Claude Code on the PC; verified on the phone the same day; 182 tests passed; commit only local)*

- **[Change]** **[📱 Mobile]** The **Notebooks** box with its notebooks is
  now **at the top of the sidebar**, above the widgets; under it the line
  asked for on 26/09 stays. Between the widgets and "Main menu" there is
  now the same line: before the notebooks separated them
- Verified on the phone: sidebar open, at the top Notebooks, the line, then
  "Widgets" with the time zones and the others; at the bottom of the
  widgets "Add widget", the line, "Main menu"

### 🔧 v0.65.0.2 — 28/09/2026 · 19:33 — Quick Enter: no extra lines, and fonts don't jump
*(Claude Code on the PC; verified on the phone the same day; 182 tests passed; commit only local on 28/09/2026)*

- **[Bug fix]** **[📱 Mobile]** **Enters pressed quickly created an extra
  line.** The second Enter arrived before the database had written the
  first: the field had one line more than the blocks, `updateRun` went back
  to comparing with the saved texts and from there it looked like two lines
  were born. Three Enters made four lines, and from that moment field and
  blocks no longer matched: a formatted test line stayed without its fonts
  and another without subscript and superscript, until the page was
  reopened. Now every change waits for the previous one and rereads the
  blocks from the database (`structuralEdits`), where the previous one
  already is; of the existing lines only the text is rewritten, so a type
  change that arrived an instant before (the "/" menu) isn't put back
- **[Bug fix]** **[📱 Mobile]** **The "jump"** even with a single Enter:
  for a frame, until the database wrote, every line took fonts, colours,
  subscripts and superscripts from the block of the next line, and the
  formatted lines were seen changing and coming back. Now lines and blocks
  are matched counting from the bottom and from the start
  (`blocksOfLines`), as list bullets already did — and now with backspace
  and with several lines of difference too; the line split in two or the
  two lines joined keep the expected formatting
- **[Project]** `updateRun` → `runDiff` and `writeRunChange`;
  `PageRepository.getBlocksOnce`, `applyBlockChanges` with `ensured` and
  `texts`. Tests in `FastEnterTest` (with the previous version the
  three-Enter test failed: four lines) and `LineBlocksTest`
- Verified on the phone recording the screen on the main page, caret on
  the first empty line and three Enters in a row with the keyboard's key.
  **Before**: in the frames after the third Enter the formatted test lines
  lost fonts, subscript and superscript until the end of the recording, and
  in the database four new lines instead of three. **After**: in all 23
  frames the two lines stay formatted, and in the database the new lines
  are exactly three. The ten empty lines added by the tests at the top of
  the main page I didn't manage to remove (the phone disconnected); on
  28/09/2026, reconnected, they were no longer there: at the top there are
  four empty lines, and the rest of the page, formatting included, is
  intact

### 🔧 v0.65.0.1 — 26/09/2026 · 18:31 — "To-do list" from the "+" on the first line of a new page
*(Claude Code on the PC; verified on the phone the same day; 174 tests passed; commit only local)*

- **[Bug fix]** **[📱 Mobile]** In a just created template — and in every
  new page, including database row pages — **choosing a type from the "+"
  on the first line, before writing in it, did nothing**: "To-do list"
  didn't put the checkbox, and what was written afterwards stayed normal
  text. The first line of a page without lines is shown but isn't in the
  database until you write in it (see `placeholder`), and the type change
  writes **a single column** of a row that must already be there: it
  didn't find it and got lost. With "/" it worked, because the typed slash
  saved the line first. Now single-column changes (type, text, indent,
  starting number, notification, table, linked view) go through
  `blocksDb`, which first saves that row **only if it is missing**: if in
  the meantime it has been written, it stays as it is
- **[Project]** `BlockDao.insertIfAbsent`, `PageRepository.saveBlockIfAbsent`.
  Tests in `EmptyPageTest`
- Verified on the phone: reproduced before the fix (new template, "+" →
  To-do list, "ciao": in the database a paragraph "ciao"). After: the
  checkbox appears right away, "ciao", Enter, "due" → two checkboxes in the
  database. The test templates are in the trash; the user's pages weren't
  touched

### 🆕 v0.65 — 26/09/2026 · 18:21 — Page templates, and the properties at the top of a row's page
*(Claude Code on the PC; verified on the phone the same day; 173 tests passed; commit only local)*

- **[New feature]** **[📱 Mobile]** **"Templates" in the sidebar**, under
  "Startup window": the list of templates, "New template" at the top to
  create one, and the three dots of each to send it to the trash (from
  which it goes back among the templates, not into the main menu). A
  template **is written like any page** — checkboxes, lists, headings,
  toggles, tables, subpages — with at the top, in grey, "You're editing a
  template"; without a name it is called "Untitled template". In a
  template's three dots there are no "Duplicate" and "Move to", which would
  turn it into a page of the main menu
- **[New feature]** **[📱 Mobile]** **The grey entries of a new database
  page**: opening the page of a still empty row, under the properties
  appear "Tap anywhere to continue with an empty page, or pick a template"
  and the two entries **"+ New template"** (it creates a template and opens
  it, to be written) and **"Add existing template"** (the list of
  templates: tapped one, everything written inside ends up in the page and
  the entries go away). They are grey like the rest of the app's hints.
  **A tap anywhere on the page** makes them disappear; reopening the still
  empty page they come back, as in Notion. The template takes the place of
  the page's empty lines (in a page already written it would go at the
  bottom); the page's title, properties, icon and cover don't change; Undo
  removes it. Templates are **the same for all databases**, both those
  created from the sidebar and those created from a page
- **[New feature]** **[📱 Mobile]** **The properties at the top of a row's
  page**, as in the third photo: between the title and the text, one per
  line, with the type's icon and the name, **each editable there** as in
  its cell (tags, dates with repetitions and notifications, numbers,
  checkboxes, colours, text…); below, "**Add a property**" to add one to
  the database, and a line separating them from the text. **Those hidden
  in the table are there too**, with the crossed-out eye next to them. All
  synced: changed here, the property changes in the table and in the other
  views, and the other way round. With the page or the database locked they
  are only read. Simple databases don't come into it: their rows have no
  pages
- **[Project]** **Database at version 39**: `pages.isTemplate`. Templates
  don't appear in the tree, in search, among the pages to start from and
  among the "Move to" destinations. `PageRepository`: `observeTemplates`,
  `createTemplate`, `applyTemplate` (it copies blocks, children, table
  cells and subpages, like "Duplicate"), `observeRowOfPage`.
  `RowPageProperties` (in `DatabaseViewScreen.kt`, with the table's cells)
  uses a `DatabaseViewModel` loaded `light`, without the gallery's previews
  that would change at every key. `TemplatesScreen.kt` has the list,
  `TemplatePicker` and `TemplateChooserSheet`; the labels are in
  `TemplateStrings`. Tests in `TemplateTest` and `MigrationTest` 38→39,
  with the `v38.sql` file
- Verified on the phone (copy of the database first; migration to 39, all
  pages there): Templates under Startup window, empty; "New template" → a
  test template with the checkboxes 1, 2, 3; in its three dots no Duplicate
  and Move to. In a test database, page "2": Select, Text, "Add a
  property", the line, and the grey entries; "Add existing template" → the
  test template → the three checkboxes in the page, the entries gone.
  Select changed from the page → changed in the table; put back from the
  table → back in the page. "Text" hidden in the table → in the page it is
  there, with the crossed-out eye. A tap in the empty space makes the
  entries disappear; "Add a property" opens "New property" (cancelled); "+
  New template" from page "4" opens a new template, and coming back the
  entries are still there with two templates in the list; page "1", which
  has text, doesn't show them. At the end everything put back as it was:
  Undo removed the checkboxes from page "2" (same block as before), "Text"
  visible again, the tag in its place, and the two test templates in the
  trash. The comparison with the previous database found **a mistake of
  mine**, fixed: a tap that landed on a cell of a database of the user and
  a scroll over the keyboard had added "⁵" to a row's name; removed, and at
  the second comparison the data was identical to before (apart from the
  two test templates)

### 🔧 v0.64.0.1 — 26/09/2026 · 17:49 — Enter at the start of the first line no longer closes the keyboard
*(Claude Code on the PC; verified on the phone the same day; 168 tests passed; commit only local)*

- **[Bug fix]** **[📱 Mobile]** **Enter with the caret at the start of the
  first line** of a group of text lines (at the top of the page, or right
  under a toggle, a divider, a link…): the keyboard disappeared. The field
  holding the lines together was recognised by its first block, and the
  new empty line is born right above it: with a different first block the
  field was remade from scratch and lost the caret. Only on the first line
  and only with some text because on an empty line, or on a line further
  down, the new line doesn't become the group's first. Now the field is
  recognised by what is above it (the island preceding it, or the start of
  the page), which doesn't change going to a new line
- **[Bug fix]** **[📱 Mobile]** In the same case, under an island **the
  empty line ended up at the top of the page**, above the island, instead
  of right above the line: it was given the page's place zero instead of
  the line's. It didn't show yet because the field closed first
- **[Project]** `groupIntoRenderItems` gives the key to the groups of
  lines (`RenderItem.Run.key`), and `MergedTextRunField` receives it
  instead of deriving it from the first block; in `updateRun` new lines
  without a block before go to the first line's place. Tests in
  `RunKeyTest` and `EnterAtFirstLineTest`
- Verified on the phone (Galaxy S25 Ultra, connected with wireless
  debugging), on the test page, which has only empty lines: written "prova"
  on the first line, caret at the start, Enter with the keyboard's key.
  **Before** the keyboard closed (reproduced), **after** it stays open,
  "prova" goes down a line and the letter typed right after goes in front
  of "prova". The same with a divider above: the empty line is born between
  the divider and "prova", the keyboard stays. (At the end of the line
  Enter already worked: tried before the fix, not again after.) At the end
  the test text and divider removed: the page has its 23 empty lines again

### 🆕 v0.64 — 26/09/2026 · 13:53 — "Date format" and "Time format" in the Date property, and out of Settings
*(Claude Code on the PC; verified on the phone the same day; 164 tests passed; commit only local)*

- **[New feature]** **[📱 Mobile]** **Date format** in a Date column's Edit
  property: the same nine formats that were in Settings (Full date, Short
  date, Month.Day.Year, Month/Day/Year, Day.Month.Year, Day/Month/Year,
  Year.Month.Day, Year/Month/Day, Chinese date), each with today's date
  written that way and the tick on the current one. "Relative" of Notion's
  photo isn't there: the request was Settings' list
- **[New feature]** **[📱 Mobile]** **Time format** next to it: **Hidden**,
  **12 hour**, **24 hour**, as in the second photo. 12 hour writes "7:43
  PM", 24 hour "19:43", Hidden removes the time from the cell (it stays
  saved, and so does the notification). It applies to cells, lists,
  calculations (Earliest, Latest…) and the filter's summary, in all views
  of the column; every Date column has its own
- **[Removed]** **[📱 Mobile]** **Date format from Settings**. Date columns
  that haven't chosen one yet (and the new ones) continue with the format
  last chosen there, and so do the history, favorites, the trash and the
  notebooks' date, which have no property to take it from
- **[Project]** **Database at version 38**: `database_columns.dateFormat`
  (the name of `DateStyle`; empty = the previous one) and
  `database_columns.timeFormat` (H12, HIDDEN; empty = 24 hours), read with
  `dateSettings()`. `Formats.dateTimeFormatter(style, time)` and
  `Formats.dateStyleName`. Tests in `MigrationTest` 37→38 (dates stay as
  before, now at 24 hours), with the `v37.sql` file
- Verified on the phone (copy of the database first; migration to 38, all
  pages there), on a database of the user: Date's Edit property → Date
  format "Chinese date", Time format "24 hour"; the list of the nine
  formats with today's examples and the tick on Chinese date; Time format
  with Hidden, 12 hour, 24 hour. Day/Month/Year and 12 hour → "25/09/2026
  7:43 PM", "08/12/2025", and the Earliest calculation "08/12/2025"; Hidden
  → "25/09/2026". In Settings, after Number format comes Language right
  away. At the end Chinese date and 24 hour put back: the cells went back
  to "2026年09月25日 19:43" and "2025年12月08日" as before

### 🆕 v0.63 — 26/09/2026 · 13:40 — Left, centre or right: cells and calculations
*(Claude Code on the PC; verified on the phone the same day; 163 tests passed; commit only local)*

- **[New feature]** **[📱 Mobile]** **The cells of every column on the
  left, centre or right**: long-pressing the header, three entries —
  **Align left**, **Center**, **Align right** — with the tick on the
  current one, for **every type** (before only checkboxes were centred).
  Text moves, numbers (bar and ring too: the ring in the centre or on the
  right follows the column, on the left its "Position" rules), tags,
  dates, the colour cells' text, checkboxes, the created and edited dates.
  **Headers always stay on the left.** It applies in all views showing the
  column; the name column doesn't change
- **[New feature]** **[📱 Mobile]** **The result of every calculation**
  under the table on the left, centre or right, **long-pressing it**
  (tapped, the calculation changes, as before). By default on the right, as
  in Notion. It applies to that view, like the calculations
- **[Project]** **Database at version 37**:
  `database_columns.contentAlignment` (LEFT, CENTER, RIGHT; empty = as
  before, in the centre only checkboxes centred with the old switch, see
  `cellAlignment`). The calculations' alignments are in the same string as
  the view's calculations, with the keys "align:column"
  (`parseCalculationAlignments`), without new columns. The cell's content
  knows where to sit from `LocalCellAlignment`; `setColumnCentered` is no
  longer needed. Tests in `NumberFormatTest` and `MigrationTest` 36→37 (a
  checkbox centred before stays in the centre), with the `v36.sql` file
- Verified on the phone (copy of the database first; migration to 37, all
  pages there), on a database of the user, where in the meantime the user
  had written some numbers and set "Sum": long-pressed the Number header →
  Align left ticked, Center, Align right → Align right → the numbers on the
  right; Date → Center → the dashes in the centre (the first row's date has
  the time and fills the cell, so it has no room to move); "SUM 186"
  long-pressed → Align left → on the left. At the end put back as it was:
  Number and Date on the left, "SUM 186" on the right, the user's
  calculation in its place (in the database the calculations string went
  back identical)

### 🆕 v0.62 — 26/09/2026 · 13:24 — The Number property: format, digits, bar and ring; "Calculate" under the table
*(Claude Code on the PC; verified on the phone the same day, except what is listed at the bottom; 161 tests passed; commit only local)*

- **[New feature]** **[📱 Mobile]** **"Change type"** in the menu that opens
  long-pressing a column's header, of **any** type: there is the list of
  types with search. **"Edit property"** (and the tap on the header) has
  the name and, for a Number column, the settings below; the type is no
  longer there
- **[New feature]** **[📱 Mobile]** **A Number column's Edit property**, as
  in the first photo: **Number format**, **Decimal places**, a line, "Show
  as" and the three boxes **Number** (42), **Bar** (half full) and **Ring**
  (40% full); the chosen one is bordered in blue. It is saved with "Save";
  it applies in all views showing the column
- **[New feature]** **[📱 Mobile]** **Number format**: Number and Percent, a
  line, then the twenty currencies **in the order asked**, each with its
  sign ($, €, £, CHF, ¥, ₩, A$, CA$, S$, ₽, ₹, HK$, NZ$, kr, MX$, NT$, kr.,
  zł), with a field to search them. The sign goes in front ("€25.00") or
  after for ₽, kr and zł ("100 kr"). "Pound" is **GBP**: the request had
  "GBR", which is the country's code. **Percent** adds the % sign to the
  written number: 45 reads 45% (it isn't multiplied by 100)
- **[New feature]** **[📱 Mobile]** **Decimal places**: Default (as the
  user wrote it) and from 0 to 5. Extra digits **are hidden without
  rounding** and without being deleted: 45.76 with one digit reads 45.7,
  with three 45.760; the saved value stays 45.76. Writing in the cell you
  see the number as it is, without format
- **[New feature]** **[📱 Mobile]** **Bar and Ring in the cells**: a
  horizontal bar or a ring filled for the number divided by 100 (72 →
  almost three quarters; "Divide by" isn't there, as asked), with the
  number next to it if **Show number** is on. **Color** opens the usual
  palette (spectrum, greys, RGB, Hex); by default it is green. For the
  ring, **Position**: on the left or in the centre of the cell. In the list
  and in the cards the number reads with its format
- **[New feature]** **[📱 Mobile]** **"Calculate"**, long-pressing the
  header of any column **and of the name**: None, **Count** (Count all,
  Count values, Count unique values, Count empty, Count not empty; for
  checkboxes Count all, Checked, Unchecked), **Percent** (Percent empty,
  Percent not empty), for dates **Date** (Earliest date, Latest date, Date
  range), for numbers **More options** (Sum, Average, Median, Min, Max,
  Range). Groups open tapping them. The calculation appears in a **row
  under the table**, under its column ("SUM €25.00", "COUNT 4"), on the
  rows shown; in a grouped table, under every group. Tapping it changes it;
  with None on all columns the row disappears. Calculations belong **to the
  view**, as in Notion: a linked view has its own
- **[Project]** **Database at version 36**: in `database_columns`
  `numberFormat`, `decimalPlaces`, `numberDisplay`, `numberColor`,
  `showNumber`, `ringCentered`; in `pages` `viewCalculations`
  ("column=CALCULATION;…"). Formats and calculations in `NumberFormat.kt`
  (read by name: an unknown one goes back to the usual value instead of
  making the app close), the graphical part in `NumberProperty.kt`. Tests
  `NumberFormatTest` (8, also with the photos' numbers: sum 309, average
  38.63, median 30.5) and `MigrationTest` 35→36 with the `v35.sql` file
- Verified on the phone (copy of the database first; migration to 36, all
  pages there), on the "Number" column of a database of the user (25 in a
  row): long-pressed → Move, Edit property, **Change type**, Hide,
  **Calculate**; Edit property as in the photo; Number format → the list
  with the signs, Number and Percent above the line → Euro; Decimal places
  → 2; Bar → Color and Show number → "Save" → **"€25.00" and the green bar
  a quarter full**; Ring + Center → "€25.00" and the ring in the middle of
  the cell; Calculate → More options → Sum → **"SUM €25.00"** under the
  column; on the name Count all → **"COUNT 4"**; tapping the cell you write
  "25", with "Done" it goes back to "€25.00"; Change type → only the types.
  At the end everything put back as it was (Number, Default, Number, on the
  left, no calculation: the calculations row disappears), checked in the
  database
- **Not tested on the phone**: digits hiding (45.76 → 45.7; the test
  proves it, on the phone it would have meant changing a number of the
  user), the bar's colour change, the calculations of dates and checkboxes
  and those under groups

### 🔧 v0.61.0.1 — 26/09/2026 · 12:51 — A row's name: the caret at the end, and the keyboard staying with sorting
*(Claude Code on the PC; verified on the phone the same day; 152 tests passed; commit only local)*

- **[Bug fix]** **[📱 Mobile]** **The caret at the end of the name.** The
  name field opened with the caret at the start (a text-only field starts
  there); now it starts at the end, ready to continue or delete
- **[Bug fix]** **[📱 Mobile]** **With a sorting, deleting the name the
  keyboard disappeared.** The table's rows didn't have their id: with the
  order by name, an empty name goes to the bottom, the row changed place
  and the field being written in ended up on another row, which closed it.
  Now every row has its id (`key(row.id)`), in groups too. In addition,
  **while the name is written the order stays still**
  (`DatabaseViewModel.startTitleEdit` / `endTitleEdit`): the row doesn't
  jump at every letter, and goes to its place when "Done" is pressed or the
  field is left
- **[Bug fix]** **[📱 Mobile]** (found while testing) **A row's name could
  be rewritten only once**: the second time the cell opened and closed in
  an instant, because it remembered having had focus the time before. Now
  it starts over at every tap
- Verified on the phone, on the user's database sorted by name (rows "1",
  "2", "4", "5" and one without a name), and on a row of the other: tapping
  it the caret is after the last letter; "2" → deleted → the keyboard
  stays, the row stays second (empty); "2" rewritten and "Done"; "2" again
  (the second time it opens) → "9" → it stays second while writing →
  "Done" → it goes after "5"; then "9" → "2" → it stays fourth while
  writing → "Done" → it goes back second. The names are again the previous
  ones
- Noted: in one try the coordinates were old (the page had moved closing
  the keyboard) and a tap ended up on a line inside a toggle; checked with
  the comparison against this morning's copy: My Space's 28 blocks are
  identical, nothing changed

### 🆕 v0.61 — 26/09/2026 · 12:39 — A notebook's page, and a divider under the notebooks
*(Claude Code on the PC; verified on the phone the same day; 152 tests passed; commit only local)*

- **[New feature]** **[📱 Mobile]** **A divider under the notebooks**, like
  the one under "Main menu": the last row of the notebooks is no longer
  confused with "Main menu"
- **[New feature]** **[📱 Mobile]** **In the sidebar, a notebook's name
  opens its page, the small arrow on the left opens and closes the tree**
  where it is. The arrow is now a button of its own, a bit wider, for the
  finger. Groups open and close as before
- **[New feature]** **[📱 Mobile]** **A notebook's page**: at the top the
  notebook (large icon and name), below **all** the sections and groups,
  indented as in the tree, every section with its tab, the padlock if it is
  protected and **how many pages it has**, lined up on the right edge.
  Tapping a section opens it (and "back" returns here); long-pressing it,
  its menu; a group's arrow closes it **only in this page** (the sidebar
  stays as it is), and remembers it even coming back from a section. At the
  bottom "+ New section" and "+ New section group"; the three dots open the
  notebook's menu, and deleting it you go back. In the sidebar, the open
  notebook is highlighted
- **[Project]** The route `notebook/{notebookId}` and `NotebookScreen`; the
  page count per section (`NotebookDao.observePageCounts`) is read only
  while the page is open, because the pages table changes at every key
- **[Bug fix]** **[📱 Mobile]** (found while testing) The pages' numbers
  moved right at every indent instead of staying in a column
- Verified on the phone, with the user's notebooks (one with two groups one
  inside the other, and a second one): the divider above "Main menu"; the
  first notebook's arrow closes and reopens it (left open as it was); the
  name opens the page with all five sections and the two groups, "0 pages"
  in a column; a group closed in the page and still open in the database; a
  section → the section, with the notebook › group path → back → the
  notebook's page again, with the group still closed. No user data touched

### 🆕 v0.60 — 26/09/2026 · 09:06 — Repetitions: without tapping the day, the orange dots, until 2100
*(Claude Code on the PC; verified on the phone the same day; 152 tests passed; commit only local)*

- **[Bug fix]** **[📱 Mobile]** **A repetition without a tapped day got
  lost.** Choosing only "Repeat" without tapping a day on the calendar,
  "Done" found the date empty and **emptied the cell**: the rule never
  reached the database, and that is why it showed neither on the calendar
  nor in the Calendar view (which can already draw repetitions). On the
  phone the user's row indeed had an empty date. Now, with a repetition,
  **the day doesn't have to be tapped**: it starts from today
- **[New feature]** **[📱 Mobile]** **The repetition's orange dots** on
  the date's calendar, the same as the chosen day: with "every Saturday",
  all Saturdays **from that day on** (not those before). The starting day,
  if it isn't one of them, has only the orange outline
- **[New feature]** **[📱 Mobile]** **From when**: under "Repeat", when
  there is a repetition, the **"Starts"** row ("Today" or the date).
  Tapping it: **"From today until 2100"** or **"From a day I choose until
  2100"** (with the date picker). Both make the repetition end on
  31/12/2100, which reads in the summary ("Weekly on Saturday, until
  2100年12月31日")
- **[New feature]** **[📱 Mobile]** **"The last Sunday of the month"
  without starting from a last Sunday.** In "Custom…" with "month": "On
  [day]" with the number to write, or "The first / second / third / fourth
  / last [day]" from a menu, with the weekday chosen from the dots below.
  Before you could only do "like the day tapped on the calendar"
- **[New feature]** **[📱 Mobile]** In the cell, next to the date, the
  **circled arrows** of a repeating date (like the notification's bell)
- **[Project]** `Recurrence` has three more fields, `monthDay`, `nthWeek`
  (1–4 or `LAST_WEEK`) and `nthWeekday`, saved in the rule (`MDAY`, `NTH`,
  `NWD`): null means "from the starting date", so rules written before are
  read as before. No migration: the rule is already in the cell's text. The
  quick mode "Monthly on the last Saturday" also writes week and day in the
  rule, so changing the starting day later it stays the last Saturday.
  "Every day" jumps straight to the shown period: from 2026 to 2100 there
  are twenty-seven thousand days, beyond the calculation's safety cap. Test
  `RecurrenceTest` (5)
- Verified on the phone, on a row of a database of the user (empty date):
  Repeat → "Weekly on Saturday" without tapping days → dots on 26
  September, 3 and 10 October, not on past Saturdays; "Starts Today".
  "Starts" → "From a day I choose until 2100" → 5 October → outline on the
  5th, dots from Saturday 10; the summary "…, until 2100年12月31日".
  "Custom…" → month → "The last Sunday" (menu and Sunday's dot) → dot on 25
  October. "Done" → saved `FREQ=MONTHLY;…;NTH=5;NWD=SUNDAY;UNTIL=…`, the
  cell with the date and the arrows. Calendar view (set for the test and
  then put back to Table): the row on 25 October. At the end the row's date
  was cleared, as it was before
- Good to know: the cell shows the day the repetition starts from, not the
  next time it happens

### 🆕 v0.59 — 26/09/2026 · 08:53 — Notebooks, as in OneNote
*(Claude Code on the PC; verified on the phone the same day, except what is listed at the bottom; 147 tests passed; commit only local)*

- **[New feature]** **[📱 Mobile]** **The sidebar's "Notebooks" entry is
  on**: it is the previous band (another colour, under the widgets), with
  the arrow to open and close it — the choice is remembered — and the
  **+** for a new notebook. Below, **OneNote's tree**: the notebooks with
  their drawn notebook and the arrow; inside, the **sections** with the
  coloured tab and the **section groups**, which open and close like
  toggles and can contain other sections and other groups (like
  "Languages" → "Deutsch" → "General" in the photo). At the bottom of every
  open notebook, "+ New section". The open section is highlighted
- **[New feature]** **[📱 Mobile]** **A new notebook**: name and colour
  (OneNote's twelve), and it is born with its first section "New Section
  1", as in OneNote. **The icon**: the notebook drawn in the chosen colour,
  or **an image of your choice from the gallery** — for example an icon
  downloaded from Flaticon — from "Notebook icon" in its menu. Downloading
  them from Flaticon I can't do: the app doesn't go online (and icons have
  their own licence); if the user sends them to me, I put them among the
  ready ones
- **[New feature]** **[📱 Mobile]** **Long-pressing an entry** opens its
  menu, the entries of OneNote's right click that make sense here:
  - notebook: Rename, Notebook color, Notebook icon, New section, New
    section group, Move up, Move down, Delete
  - section: Rename, Section color, **Password protect this section**
    (with the app's password or the fingerprint, like pages), New section,
    New section group (they are born next to it), Move up, Move down,
    Delete
  - group: Rename, New section and New section group (they are born
    inside), Move up, Move down, Delete
  The new name is proposed already written and selected ("New Section 2",
  "New Section Group 1"…). A new section takes the colour after the last
  one's, and opens right away
- **[New feature]** **[📱 Mobile]** **An open section**: at the top where
  it is ("Test notebook › Languages"), the name with the tab, **"Add
  page"** as in OneNote, and the pages in their order, with the day they
  were born. Tapping a page opens it; long-pressing it, Move up, Move down,
  Delete page. The three dots open the section's menu. On a phone
  OneNote's three columns don't fit: the sections column is the sidebar,
  the pages column is this screen
- **[New feature]** **[📱 Mobile]** **Notebook pages** use the same editor
  as the others, so **they scroll endlessly**: at first there is only what
  is visible, and they grow as you write. At the top, **as in OneNote**:
  the title ("Untitled page" in grey while empty), a thin line and below
  **the day, the date and the time** the page was born (in the date format
  chosen in the settings). No icon or cover. You draw, lock, protect with
  the password, look at the history as in the other pages
- **[New feature]** **[📱 Mobile]** **Two separate worlds**, as asked.
  Notebook pages never appear in the main menu: neither in the pages tree,
  nor in search, nor among the pages the app can start from, nor among the
  databases to show in a linked view. And from the notebook you don't reach
  the main menu: in the "/" and "+" menus of notebook pages there are no
  Page, Link to page and databases; in the three dots there are no
  favorites, search, Duplicate and Move to. **Deleting** (a page, a
  section, a group, a notebook) is **forever**, because the trash belongs
  to the main menu: first it asks, saying how many pages go away. A
  protected section, or a group or a notebook containing one, first asks
  for fingerprint or password
- **[Project]** **Database at version 35**: the `notebooks` and
  `notebook_sections` tables (sections and groups together, because they
  form a single tree with a single order), and `pages.notebookSectionId`.
  Pages are real pages, so they have the whole editor for free;
  `orderIndex` is their order in the section. `NotebookRepository`,
  `NotebooksViewModel`, `NotebookSectionViewModel`, `Notebooks.kt`
  (sidebar, menus, the section's screen), `NotebookStrings` in the eight
  languages. `lockingPageOf` now knows about protected sections; "Remove
  password" in the settings counts them too. Tests `NotebookRepositoryTest`
  (5) and `MigrationTest` 34→35, with the `v34.sql` file
- **Out, for now** (from OneNote's menu in the photo): Export, Move or Copy,
  Merge into Another Section, Copy Link to Section (and Copilot, which isn't
  of interest). Nor the sorting of pages, indented subpages and a notebooks
  trash. They are added if needed
- Verified on the phone (with a test notebook, then deleted): before
  installing, a copy of the phone's database; migration to 35 in place (the
  new column at the bottom of `pages`), the user's pages all there.
  "Notebooks" on in the sidebar, "+ New notebook" with selected name and
  colours → "Test notebook" with the blue notebook and "New Section 1";
  long-pressed → the notebook's menu → New section group "Languages" →
  inside, New section "Deutsch" (red) → **it opens by itself**, with "Test
  notebook › Languages"; "Add page" → the page with "Untitled page", the
  line and "Saturday, 2026年09月26日 08:47"; title "Example" and a line of
  text; in the "/" menu no Page, and "/data" → "Nothing with that name";
  the three dots with Draw, Delete page, Lock page, Lock with password,
  Updates, 2 words. Back → "Example" in the list. In the sidebar the tree
  as in the photo, "Deutsch" highlighted. "Password protect this section"
  → the section closes right away (padlock next to the name, "This section
  is locked", the fingerprint asked by itself; cancelled, I didn't open
  it). Delete of the notebook → "Everything inside goes with it, 1 page
  included" → deleted: in the database zero notebooks, zero sections, zero
  notebook pages, the user's pages intact
- **[Bug fix]** **[📱 Mobile]** (found while testing) The new notebook's
  blue wasn't one of the twelve colours, and in the choice none had the
  tick: now it is the first. Verified: the tick on the blue
- **Not tested on the phone**: the fingerprint asked before deleting a
  protected section (to finish the test the user's fingerprint would have
  been needed), the image as a notebook's icon, colours changed later,
  Rename and Move up/down from the menus (up and down are tested by the
  test)

### 🔧 v0.58.0.1 — 26/09/2026 · 08:27 — The toggle's arrow, a tiny bit higher
*(Claude Code on the PC; verified on the phone the same day; commit only local)*

- **[Bug fix]** **[📱 Mobile]** **The toggle's little triangle sat a bit
  low** compared to the title. The triangle is in the middle of its 24dp
  square and the title's line is in the middle of its 24, but letters don't
  sit in the middle of their line: they are higher, because below there is
  the room for the legs of g, p and q. Measured on the phone (in pixels, on
  the screenshot): the triangle had its centre 5 pixels below the centre of
  the lowercase letters and 9 below that of the capitals. Now it goes up by
  2dp (`TOGGLE_ARROW_LIFT`) and its centre is between the two, 2 pixels
  above the lowercase and 2 below the capitals. It applies to every toggle,
  closed and open
- Verified on the phone, on a toggle at the bottom of My Space: before
  2422–2455 (in pixels), after 2415–2448, with the title's "R" at
  2414–2445

### 🆕 v0.58 — 26/09/2026 · 08:20 — The padlock next to the three dots
*(Claude Code on the PC; verified on the phone the same day by the user; 141 tests passed; commit only local)*

- **[New feature]** **[📱 Mobile]** On a password-locked page, **to the
  left of the three dots** there is a second **identical** button — the
  same semi-transparent grey circle, the same size — with **a closed
  padlock**. Tapped, it says "Protected by a password" ("Protezione con
  password attiva"). On databases opened full screen too, next to their
  three dots (there without a circle, like the three dots). It appears only
  on the locked page itself, like the "Lock with password" switch in the
  menu: its subpages don't
- **Verified on the phone, from the previous entry**: the database the
  user locked **became a link** with the padlock next to the name — the
  conversion, so far tested only by the test, works on the phone too
- The padlock next to the three dots shows only with the page open, and
  locked pages open with the user's fingerprint or password: **the user
  looked at it**, opening the locked database
### 🔧 v0.57.0.1 — 26/09/2026 · 08:16 — The gallery card of a locked page
*(Claude Code on the PC; verified on the phone the same day; 141 tests passed; commit only local)*

- **[Bug fix]** **[📱 Mobile]** In the Gallery view **of any database**
  (inside a page too, a linked view too), the card of a password-locked
  page showed its cover. Now in place of the preview there is **a shaded
  dark grey background, always the same, with a white padlock** in a
  circle; the padlock grows with the card's size. It applies to "Page
  cover" and to "Page content"; with "None" the card has no preview and
  stays as it is. Long-pressing the card there is no longer "Reposition
  cover", which would have shown the cover again
- **[Bug fix]** **[📱 Mobile]** (found while working on it) The magnifier
  next to Sort also searched inside the text of locked pages, and finding
  it highlighted the row: now it skips them, like the general search
- **[Project]** The cover and the text of a locked page don't even leave
  the database (`observeRowCovers`, and the magnifier's three queries); the
  locked rows reach the gallery from `observeLockedRowIds`, followed only
  while the gallery has a preview. The background is `LockedCardPreview`,
  dark even with the light theme so that the padlock stays white. Tests in
  `PageLockTest`
- Verified on the phone, in My Space's gallery database: the card locked by
  the user, with a cover, shows the background with the padlock, the card
  next to it its cover; long-pressed, the locked one has Icon, Edit
  property and Delete, without "Reposition cover", which the other still
  has

### 🆕 v0.57 — 26/09/2026 · 08:04 — Locking pages and databases with the password or the fingerprint
*(Claude Code on the PC; verified on the phone the same day, except the database becoming a link, see at the bottom; 140 tests passed; commit only local)*

- **[New feature]** **[📱 Mobile]** **"Lock with password"** in the three
  dots of every page and every database, under "Lock page", with the
  fingerprint icon and the switch. It isn't in My Space: locking it would
  close the whole app. **The first time you choose the password** (at least
  4 characters, written twice) and whether to open with the fingerprint
  too; afterwards, locking is immediate. Locked, the page closes right
  away: in its place a padlock, and **the fingerprint request starts by
  itself**; "Use password" switches to the password. With the page
  **everything it contains** closes: subpages, databases, row pages. In the
  sidebar it no longer shows what it contains, search finds only its title
  (without the text), and its link has a small padlock next to it. Open, it
  stays open while you move inside it; going elsewhere, or leaving the app
  for more than 30 seconds, it closes again. Removing the lock (the switch
  off) asks again for fingerprint or password
- **[New feature]** **[📱 Mobile]** **A locked database, inside a page,
  becomes a link**, as with "Turn into page", and opening it asks for
  fingerprint or password. Its rows and its linked views close too: a
  linked view inside an open page shows, in place of the table, the
  request to unlock it
- **[New feature]** **[📱 Mobile]** In Settings, under "Time zone",
  **"Password for locked pages"** (Set / Not set): fingerprint yes or no,
  **"Change password"** (first you confirm it's you) and **"Remove
  password"**, which works only when no page is locked anymore — otherwise
  they would stay closed forever. From the password window, **"Forgot the
  password?"** asks for the phone's screen lock (PIN, pattern or
  fingerprint) and then a new password
- **[Project]** **Database at version 34**: `pages.isProtected`. The
  password isn't saved: only a PBKDF2 hash with its salt is kept, in the
  app's preferences (`AppLock`, `PasswordHash`). The `LockGate` gate is in
  front of every full-screen page and database (`AppNavHost`) and in front
  of the databases inside pages; `lockingPageOf` climbs links, rows and
  linked views to find who locks what. Search and the sidebar filter in
  `PageRepository`; the gallery cards' previews skip locked pages. The
  fingerprint is the system's `BiometricPrompt` (`USE_BIOMETRIC`
  permission). Tests `PageLockTest` and `MigrationTest` (33→34, with the
  `v33.sql` file)
- **[Bug fix]** **[📱 Mobile]** (found while testing) Going back from a
  locked page just opened, the fingerprint request restarted from the
  screen that was leaving: now it starts only when the screen is really in
  front
- Verified on the phone: the first time the window to choose the password
  → page locked, with the padlock; **the user tried the fingerprint**, and
  the page unlocked. Wrong password → "Wrong password"; the right one → the
  page opens; back → no request; reopened → the request comes back; the
  padlock next to the link in My Space; removing the lock from the menu
  asks to confirm. At the end Settings → "Remove password" → "Password
  removed", "Not set": **the test password is gone**, and the first time
  the user locks something they choose their own. **The database becoming a
  link wasn't tested on the phone**: the test proves it
- Noted: the drawings of the test page had disappeared already before the
  install; the user had deleted them, it isn't an error

### 🆕 v0.56 — 26/09/2026 · 00:23 — Clearing the cache, deleting all content
*(Claude Code on the PC; verified on the phone the same day, except the real deletion, see at the bottom; 133 tests passed; commit only local)*

- **[New feature]** **[📱 Mobile]** In Settings, **under "Time zone"** and
  separated by a line, two entries:
  - **"Clear cache"**, with how much it takes next to it: it removes only
    the temporary files, which the app remakes when needed. It asks for
    confirmation, then says "Cache cleared". Pages, databases, drawings and
    images aren't touched
  - **"Delete all content"** ("Pages, databases, drawings, images and
    trash"): it deletes every page, database, row, drawing, the history of
    changes, the trash and the pages' images. Settings and the sidebar
    widgets stay. **It can't be undone**, so the red "Delete everything"
    button turns on only after **typing the word** the window asks for
    ("DELETE" in English, "ELIMINA" in Italian). Done, My Space is reborn
    empty and **the app restarts from scratch**: the open screens still had
    the previous content in hand, and one of their Undo would have put it
    back
- **[Project]** `AppCleanup`: the cache is `cacheDir` (and the external
  one); the content is deleted with Room's `clearAllTables()`, then the
  main page is recreated right away (the rest of the app takes it for
  granted), the images folder is emptied (`PageImageStore.deleteAll`) and
  the notifications' alarm is set again. Test `AppCleanupTest`, with
  Robolectric, on a test database and files
- Verified on the phone: the two entries under "Time zone"; "Clear cache"
  (the cache was already empty, 0 B) → confirmation → "Cache cleared".
  "Delete all content" → the window with the warning and the word to type,
  "Delete everything" off until the word is there → cancelled: the user's
  pages are all there. **The real deletion wasn't done on the phone** (it
  would have deleted the user's notes): the test proves it
- Noted: in a first attempt to reach the settings the side menu hadn't
  opened, and a tap ended up on the settings icon of My Space's gallery
  database; no choice touched

### 🆕 v0.55 — 26/09/2026 · 00:14 — The "Color cell" property
*(Claude Code on the PC; verified on the phone the same day, except the board's and gallery's tags; 131 tests passed; commit only local)*

- **[New feature]** **[📱 Mobile]** **"Color cell"** among the property
  types (with the paint bucket), in normal and simple databases. In the
  table the colour **fills the whole cell**; the text, if there is any, sits
  on top, **black or white** depending on what reads better on that
  colour. Tapping the cell opens its window: how it looks, the **text
  (optional)** and the **colour**, chosen with the brush's picker
  (spectrum, greys, RGB, Hex; "No color" removes it). It is saved with
  "Done". In a row's card it is a coloured box; in the cards of the board
  and gallery and in the list, a tag in its colour. With the database
  locked you only look
- **[Project]** The value is in the cell like the others, in a string:
  `"#RRGGBB|text"`, only the colour or only the text (`ColorCellValue.kt`).
  No migration: a column's type is already saved as text. Sorting and
  search look at the text, not at the colour's code; the table doesn't
  group by a colour cell (the group would be called "#E03E3E"). Changing
  the column's type the values stay the same, as for the other types. Test
  `ColorCellValueTest`
- **[Bug fix]** **[📱 Mobile]** (found while testing) On the bright green
  chosen in the test (#12F20D) the text came out white and couldn't be
  read: the threshold between black and white went down to half
- Verified on the phone, in My Space's "Untitled" table: "Color cell" in the
  list of types → new column, cell "—"; tapped → the window; colour from
  the picker (#12F20D) → green preview; text "Done" → "Done" → the cell all
  green with "Done" (saved "#12F20D|Done"); the row's card → green box with
  "Done". Then the test property removed ("Delete property"): the table is
  as it was

### ✏️ v0.54.3 — 26/09/2026 · 00:03 — The trash can next to page links removed
*(Claude Code on the PC; verified on the phone the same day; 126 tests passed; commit only local)*

- **[Removed]** **[📱 Mobile]** The trash can to the right of every link to
  a page inside the text (and its "Remove link" label). To delete you
  long-press the link: in the block menu, at the bottom, there is "Move to
  trash". On a locked page the menu doesn't open, as before
- Verified on the phone, on the test page in My Space: the trash can is no
  longer there; long-pressing opens the menu with "Move to trash" (closed
  again without touching anything)

### ✏️ v0.54.2 — 25/09/2026 · 23:57 — One finger draws, two fingers scroll, the palm doesn't count
*(Claude Code on the PC; verified on the phone the same day, two fingers by the user on 26/09/2026, see at the bottom; 126 tests passed; commit only local)*

- **[Change]** **[📱 Mobile]** **While drawing, one finger draws and the
  page scrolls only with two fingers**, always. With the hand off one
  finger does nothing (it no longer scrolls the page)
- **[Change]** **[📱 Mobile]** **The hand sits next to ✓, Undo and Redo**,
  always in view, blue when the finger draws
- **[Removed]** **[📱 Mobile]** The automatic turning off of the finger at
  the first use of the S Pen, and its notice. The saved choice has a new
  name (`fingerDrawing`): the one turned off by itself on the user's phone
  was forgotten, and the finger went back to drawing
- **[Change]** **[📱 Mobile]** **The palm doesn't count**, instead of the
  turning off: while the S Pen is near the screen (even just hovering) or
  touches it, and for 0.6 seconds after, the hand's touches are ignored;
  and if the hand started a mark before the pen touched, that mark is
  thrown away and the pen draws
- Verified on the phone, on the test page (where the user's drawings are,
  left as they were): the hand next to Redo, blue; one finger → it draws;
  S Pen (simulated) and right after a finger → the finger's line isn't
  there; a finger a second later → it draws. The three test lines removed
  with Undo (the user's 7 strokes remain). **Two-finger scrolling was tried
  by the user on 26/09/2026: "it works"**. Still to try by hand: the
  hovering pen with the palm resting
- Noted: the S Pen simulated from adb had left Samsung's floating S Pen
  icon on, as if the pen were out of its slot. The user took it out and put
  it back, and the icon disappeared. From here on drawing tests are done
  with the finger, and what needs the real pen is tried by the user

### ✏️ v0.54.1 — 25/09/2026 · 23:49 — The pens' colour with the brush's picker
*(Claude Code on the PC; verified on the phone the same day; 126 tests passed; commit only local)*

- **[Change]** **[📱 Mobile]** In a pen's window, under "Colors", there is
  the colour in use (the circle and the code, "#2383E2"); tapping it opens
  **the brush's picker**, the same as the text bar's: the hue and
  saturation box, the strip of greys from white to black, Red/Green/Blue
  and Hex. It opens on the pen's colour. Instead of "No color" there is
  **"Text color"**, which puts back the text colour (the one following the
  theme); under the preview there is the pen's name instead of "Text
  color"
- **[Removed]** **[📱 Mobile]** The pen window's twenty fixed colour
  circles
- **[Project]** `ColorPickerSheet` takes two labels from outside
  (`previewLabel`, `noColorLabel`); the brush and the widgets' bars don't
  change. The pen's colour passes to the picker as "#RRGGBB" and comes back
  full (`inkHex`, `hexToInkColor`, test `InkColorTest`)
- Verified on the phone: blue pen → "Colors" → the brush's picker on
  #2383E2 (35, 131, 226), with "Text color" and "Apply"; "Apply" on the
  same colour → the pen stays #2383E2. The other pens are as the user had
  left them
- Noted: the user had already drawn on the test page with the real S Pen
  (white and pink scribbles, with the first pen made pink and thicker):
  **pressure works**, the stroke widens and narrows. Left as they were. In
  the test the blue pen stayed in hand

### 🆕 v0.54 — 25/09/2026 · 23:42 — Drawing by hand on pages, with the S Pen too
*(Claude Code on the PC; verified on the phone the same day, with the S Pen simulated from adb, see at the bottom; 123 tests passed; commit only local)*

- **[New feature]** **[📱 Mobile]** **"Draw"**, with the pencil, in the
  three dots of every text page (row pages too; not in full-screen
  databases). You draw **on the whole page from the end of the cover
  down**, cover excluded: over the text, the title, a database. At the
  bottom there is the drawing bar instead of the text's: **✓** to finish
  (the back gesture too), **Undo** and **Redo** (only of drawings, separate
  from the text's), the **eraser** and six pens as in the photo, and the
  **hand** to draw with the finger. While drawing the text can only be
  read; finished, the drawings stay and always show
- **[New feature]** **[📱 Mobile]** **The pens**: pen, pencil, marker and
  highlighter. By default: a pen in the text colour (white on the dark
  theme, black on the light), a blue pen, a red pen, a grey pencil, a green
  marker, a yellow highlighter. Tapping the tool already in hand (or
  long-pressing it) opens its window, as in the photo: **Thickness** (five
  thicknesses, and "−" "+" for the steps in between), **Types** (which of
  the four it is) and **Colors** (since 25/09/2026 with the brush's
  picker; before twenty fixed colours). The pen widens pressing harder with
  the S Pen; the pencil is a bit transparent; the highlighter is
  transparent, flat-tipped, and sits under the other strokes. The choices
  are remembered
- **[New feature]** **[📱 Mobile]** **The eraser**, with **Thickness** and
  **Types**: **Stroke Eraser** removes the whole touched stroke, **Point
  Eraser** only what passes under the eraser (the stroke breaks). While
  erasing you see the eraser's circle. One pass is a single Undo
- **[New feature]** **[📱 Mobile]** **The S Pen.** Yes, it works: the phone
  is a Galaxy S25 Ultra (SM-S938B) and the S Pen reaches the app as a real
  pen, with **pressure**. The first time it is used the finger stops
  drawing and **scrolls the page** (a notice says so): so the resting palm
  leaves no marks. The hand in the bar makes it draw again; with the finger
  drawing, **two fingers** scroll. With the **side button** pressed the S
  Pen **erases**, as in Samsung Notes (the eraser chosen in the bar). What
  isn't there: the remote gestures (Air actions), which the S25 Ultra's S
  Pen no longer has
- **[Project]** **Drawings follow what they were made on.** Every stroke is
  attached to the item of the page where it started — the group of lines
  between two dividers, a database, a toggle, or the header with the title
  — with coordinates counted from its corner (`InkStrokeEntity`,
  `InkGeometry.kt`). If lines or a divider are added above, the drawing
  goes down with its lines. With what it was made on deleted (the database
  that had been circled), the drawing doesn't show but stays: undoing the
  deletion it comes back. Duplicating the page the drawings come with the
  copy
- **[Project]** **Database at version 33**: the `ink_strokes` table, one
  stroke per row (the points in a compact string, "x,y,pressure" in dp),
  deleted with its page. Test `MigrationTest.from32…` on a file at 32
  (`schema/v32.sql`); 8 new tests of the calculations (`InkGeometryTest`:
  saving the points, the two erasers, the anchors, the thicknesses). The
  tools are in the preferences (`InkPrefs`), not in the database, like the
  settings
- Verified on the phone, on the test page, with the S Pen simulated from
  adb (`input stylus`): "Draw" in the three dots → bar; two strokes →
  saved, attached to the line under the title, and the "S Pen detected"
  notice; the pen's menu as in the photo; Stroke Eraser on a line → gone,
  Undo → back; Point Eraser in the middle of the other → broken in two;
  pencil, marker and highlighter on the title (attached to the header); the
  finger, off, doesn't draw; **divider put above the drawn lines → the
  strokes went down with the lines**, the highlighter stayed on the title;
  a stroke stayed after killing and reopening the app. Then everything put
  back as it was: text, drawings (zero) and starting tools. **To try with
  the real S Pen**: pressure (adb always gives it full), the side button,
  two fingers
- Noted: the test notification lines in the test page were no longer
  there. From the page's history the user emptied them at 23:16–23:17,
  before the update: it wasn't the migration

### 🆕 v0.53 — 25/09/2026 · 23:12 — Notifications: "/notification", database dates, timers and pomodoro
*(Claude Code on the PC; verified on the phone the same day, except the real reboot, see at the bottom; 114 tests passed; commit only local)*

- **[New feature]** **[📱 Mobile]** **The "/notification" line.** You write
  `/notification 22.10.2026 19:41 Take out the trash` and press Enter: the
  command disappears and a line remains with the **bell**, the text "Take
  out the trash" and at the end, in a blue box, **when it rings**; writing
  continues on the new line below. Tapping the box you choose the day and
  then the time, with Samsung's windows. The date can also be written
  `22/10/2026`, `22-10-26`, `2026-10-22` or without the year (`22.10`:
  this year, or next if it has already passed); the time also `19.41`.
  **Without a time, at 9:00**; with only the time, today (or tomorrow if it
  has already passed); with nothing, the next 9:00. Text in brackets loses
  the brackets. `/notifica` works too. Without text, the notification shows
  the **page's name** (the empty line suggests it in grey). There is also
  the **"Notification"** entry in the "/" menu and in "Turn into": it
  leaves the text as it is, sets the next 9:00 and opens the choice of the
  day right away. Backspace at the start of the line turns it back into
  text, like a checkbox. A notification already past turns grey
- **[New feature]** **[📱 Mobile]** **"Include notification"** in
  databases' date window, under "Include time" (and under its times): the
  date sends a notification with the **name of the row's page** and below
  the database's name. At the date's time, or **at 9:00** if there is no
  time; with an end date, **the first day and the last** (below: "Album ·
  Start", "Album · End"); a repeating date rings at every repetition. Under
  the switch a line says when it will ring. In the cell, next to the date,
  a little bell
- **[New feature]** **[📱 Mobile]** **The end of a timer and of every
  pomodoro phase** send a notification ("Time's up", below "Timer · 10 m";
  at the end of the session its name and "Time for a break · 5 min"; at the
  end of the break "Break over" and the session again), and now they ring
  **even with the app closed**
- **[New feature]** **[📱 Mobile]** Tapping a notification opens **its
  page**: that of the "/notification" line, or that of the database's row
  (if the row can't have a page, the database)
- **[New feature]** **[📱 Mobile]** The **notification permission**
  (Android 13 and later) is asked **when needed**, not at start: creating a
  notification, turning on "Include notification", starting a timer or a
  pomodoro. In the settings, instead of "Notifications aren't there yet", a
  line says where they come from. **"Turn off notifications" and "Turn off
  for a while" apply to all**; the sound is the one chosen in
  "Notification sound" (or the pomodoro's), only once
- **[Project]** How it works (`data/notify/`): **a single Android alarm**
  (`NoteAlarms`, exact `AlarmManager`, which rings even with the phone in
  standby) set at the first thing that must ring, among reminders and the
  end of timers and pomodoros. When it rings, `AlarmReceiver` — even with
  the app closed, Android starts the app only for this — sends what has
  arrived **from the last time to now** (`deliveredUpTo`), so nothing
  arrives twice, and a notification missed with the phone off arrives at
  power-on if no more than 12 hours have passed; then it sets the alarm to
  the next thing. After a reboot, an update or a time change
  `RescheduleReceiver` sets it again. An alarm set for something later
  deleted does no harm: when it rings the database is reread. The
  "Reminders" and "Timers and pomodoro" channels have no sound of their
  own: the app makes it (`WidgetStore.playOnce`), so the sound chosen in the
  settings applies right away. Calculating what rings and when
  (`Reminders.kt`) and the command (`NotificationCommand.kt`) are without
  Android, with 18 tests (`NotificationsTest`, on the request's examples)
- **[Project]** New permissions in the manifest: `POST_NOTIFICATIONS`,
  `USE_EXACT_ALARM` (Android 13 and later; `SCHEDULE_EXACT_ALARM` up to
  12), `RECEIVE_BOOT_COMPLETED`. Still no network permission
- **[Project]** **Database at version 32**: `blocks.notifyAt`, the time of
  "/notification" lines, added with `ALTER TABLE` (empty column for all
  earlier rows). Test `MigrationTest.from31…` on a file at 31
  (`schema/v31.sql`). The "Include notification" mark is instead in the
  date cell's value, at the end (`…|n`): values written before stay valid
- Verified on the phone: `/notification 19:36 Test notification` + Enter →
  line with bell and "25.09.2026 19:36", and at the first notification
  Android asked for the permission. **With the app closed** (process
  killed): `/notification 19:41 Closed app test` → at 19:41 Android
  restarted the app and the notification "Closed app test", with the test
  page below, arrived. The user tried "Include notification" on a test row
  at 19:35, with the app closed: "it works perfectly"
- **[Bug fix]** **[📱 Mobile]** It wasn't the closed app: in the phone's
  database and in Android's alarms you could see the row was for
  **tomorrow**. It had been written `/notification` without a time at
  19:32, so the next 9:00, that is tomorrow; then, tapping the box, only
  the time had been changed (19:34), and the day had stayed the 26th. The
  box said "26.09.2026 19:34" and it went unnoticed. Now **today and
  tomorrow are written by name** ("Today 19:41", "Tomorrow 09:00"), and if
  the chosen time has already passed a message says the notification won't
  ring. The closed-app test above was done precisely to rule out the other
  cause

(it was already 22:56:36: set for 22:58)

- Verified on the phone, with the app closed (process killed) in all
  cases: **simulated reboot** — app stopped completely, which removes its
  alarms like a reboot, then `RescheduleReceiver` called by hand: the 22:59
  alarm came back, and the notification arrived; **tap on the
  notification** "Tap to open" of 22:58 → the app opens directly on the
  test page (not on My Space, the start page); **pomodoro** of 1 minute →
  at 23:01 "Session · Time for a break · 5 min"; **timer** of 14 seconds
  added with "+" → "Time's up · Timer · 14 s". Then everything put back as
  it was: pomodoro stopped on 01:00, test timer deleted with the trash can,
  the first timer brought back to its wheels (00:21:02), the second never
  touched (00:14:00). The real reboot of the phone wasn't tried: it would
  turn off wireless debugging
- **[Bug fix]** **[📱 Mobile]** (found while testing) **The notification
  line stayed blue after ringing**, until the page redrew for something
  else: the time was looked at only when drawing. Now bell and box turn
  grey by themselves at the moment the notification rings (tried: "Turns
  grey" at 23:12, with the page open without touching anything)
- **[Bug fix]** **[📱 Mobile]** (found while testing, it was already there
  before) **The sidebar without pages.** If Android closed the app with the
  sidebar open, at reopening the sidebar came back open by itself, but
  without going through the button that reads the tree: under "Pages"
  there was nothing, until it was closed and reopened. Now the tree is read
  in that case too (tried: sidebar open, app killed, reopened → My Space,
  the test page and the two databases in their place)
- Noted: the tests left in the test page four notification lines already
  rung; left there, they are on the user's test page

### ✏️ v0.52.6 — 25/09/2026 · 18:46 — Date and time pickers like Samsung's
*(Claude Code on the PC; verified on the phone the same day; 95 tests passed; commit only local)*

- **[Change]** **[📱 Mobile]** **The month like Samsung's**
  (`SamsungCalendar` in `SamsungPickers.kt`), instead of Material's
  calendar: at the top "September 2026 ▾" with the ‹ › arrows at the
  sides, and the month also changes **dragging horizontally**. Six weeks in
  a grid, Sundays in red, the other months' days dimmed (but tappable),
  today in orange, the chosen day in an **orange circle**. Tapping the
  month you go to the **wheels** of month, day and year (▴), in the order
  the date is written where you are (in Italy day, month, year); the day
  adapts to the month (from the 31st to February you go to the 28th);
  tapping the month again you go back to the grid, on the chosen month.
  Years from 1900 to 2100
- **[Change]** **[📱 Mobile]** **"Set time" with wheels**
  (`SamsungTimeDialog`): hours and minutes, on twenty-four hours, **one
  minute at a time** — the request "intervals at every second" read as
  "every single value" (the photo went in fives, and the user asked for
  hours and minutes, not seconds). At the bottom **Cancel | Done**, as in
  the photo
- **[Change]** **[📱 Mobile]** **"End date"**: with the new calendar
  tapping another day **moves** the date (before, with Material's
  calendar, a second tap made a period). The period is made with the "End
  date" switch, as in Notion: on, the first tap is the start and the second
  the end, with a light orange band between the two
- **[Change]** **[📱 Mobile]** The same calendar, in a "Set date" window
  with Cancel | Done (`SamsungDateDialog`), also where a single day is
  chosen: the end of a repetition, the date filter, the Life progress
  widget's custom dates
- **[Project]** The wheels are **a single one** (`WheelPicker`), the same
  as the sidebar timer's (there peach-coloured, here white); they also do
  the wheels that stop at the ends (the years). Material's calendar and
  time (`DateRangePicker`, `DatePickerDialog`, `TimeInput`) and their UTC
  millisecond conversions removed, no longer needed
- **[Bug fix]** **[📱 Mobile]** (found while testing) The "Set time" window
  ended up under the navigation bar, with Cancel and Done cut: inside the
  window there was a full-screen box with the card at the bottom, and the
  window doesn't know where the bar is. Now the window itself sits at the
  bottom, as tall as the card
- Verified on the phone, on a test row's Date cell: the month of September
  as in the photo, tap on the 25th → orange circle, drag → October, tap on
  the month → wheels SEP · 25 · 2026, month wheel on OCT → back to the grid
  on October with the 25th chosen; Include time → Start time → "Set time"
  09 : 00, minutes turned by three → 09:03 in the row. Then Cancel: nothing
  was saved in the cell
- Noted: at the top of "My Space" there are six empty lines before the
  first test heading, born from the user's tests at 17:45; left as they
  were

### ✏️ v0.52.5 — 25/09/2026 · 18:18 — The trash can for added timers
*(Claude Code on the PC; verified on the phone the same day; 95 tests passed; commit only local)*

- **[Change]** **[📱 Mobile]** **The trash can of added timers.** In the
  Timer widget, from the second page on, at the top right there is a
  **trash can** that deletes that timer, in any state: on the wheels,
  running, paused, finished. On the first page it isn't there: **the first
  timer is never deleted**, and the rule is also in the saving
  (`TimerWidget.withoutTimer`), not only in the screen. If the deleted
  timer was running, its alarm goes away with it. New test
  `WidgetsTest.onlyTheTimersAddedAfterTheFirstCanBeDeleted`
- **[Removed]** **[📱 Mobile]** The **"Cancel"** on the wheels, which was
  there with more than one timer: it deleted the timer, but it wasn't
  clear — and it also applied to the first. Now on the wheels there is only
  Start, and to delete the trash can
- Verified on the phone, with the user's two timers (10:02 and 14:00, on
  the wheels): on the first page only Start, on the second the trash can;
  the second started, a third added with "+", started (9:58, with trash
  can, ↻ and +) and deleted with the trash can → two timers; the second
  brought back to the wheels with ↻ — the user's two timers are as they
  were

### ✏️ v0.52.4 — 25/09/2026 · 18:04 — Four bugs, the images' link removed, the empty page
*(Claude Code on the PC; all verified on the phone the same day; 94 tests passed; commit only local, **not** on GitHub until the user says so)*

- **[Removed]** **[📱 Mobile]** **The images' link** (icon and cover).
  Before there was a "Link" pill next to "Upload", off while the field was
  empty, which downloaded the image: it looked like a label. For a few
  hours it became a "Download" button next to the field, with the Enter key
  and the error's reasons explained (a site refusing, a link that isn't an
  image, no network); testing it on the phone the download turned out very
  slow (over 20 seconds even for a tiny page, 0.1 s from the PC: probably
  the phone's network), and the user asked to remove the link. Everything
  removed: the field, the button, the download
  (`PageImageStore.saveFromUrl`) and its labels. The small window has only
  **Upload** (and Reposition / Remove when there is an image)
- **[Removed]** **[📱 Mobile]** **The permission to go online**
  (`INTERNET` in the manifest): it served only the link. Chinese and
  Japanese fonts are downloaded by Google Play Services with its own
  permission. From now on the app connects to nothing
- **[Bug fix]** **[📱 Mobile]** Choosing from the gallery a file that isn't
  an image, the failed copy stayed in the app's folder: it deleted a file
  named after the error message (`PageImageStore.saveFromGallery`)
- **The PNG with the white background wasn't an app bug.** The chosen file
  was `Pictures/Telegram/IMG_20260925_143037_979.jpg`, a 512×512 **JPEG**
  of 10,716 bytes — exactly the one copied by the app: Telegram, sending it
  as a photo, had compressed it to JPEG, which has no transparency. Sent as
  a file it stayed transparent. See "Known limits"
- **[Bug fix]** **[📱 Mobile]** **Backspace among empty lines: the keyboard
  disappeared** (bug 2). With many empty lines, backspace at the start of
  the second removed the **first** line instead of the second: for the text
  it is the same, but the shared field is tied to the first block of its
  group (the key in the list), and with that removed it was reborn from
  scratch without a caret. The before/after comparison, with all-equal
  lines, attributed the disappeared line to the first; now, when lines
  decrease, the caret's line is always among those rewritten, and the next
  one is removed (`updateRun`). New tests in `EmptyLinesBackspaceTest`.
  Verified: four empty lines, backspace from the second → keyboard open,
  "x" written on the first
- **[Bug fix]** **[📱 Mobile]** **Copy / Paste / Select all in the small
  windows rising from the bottom** (bug 3). Material 1.2's
  `ModalBottomSheet` opens in a window added by hand, which can't show the
  phone's small bar: the word was selected and nothing appeared. Not only
  in the colour of the Life progress bar's bars: in **all** the small
  windows — the colour picker, the image's link, the time zones' search,
  the database's properties. Now they all go through `NoteBottomSheet`
  (`SheetTextToolbar.kt`), which inside uses a small bar drawn by the app
  (`PopupTextToolbar`) with Android's entries and labels. A trap was also
  found: the first version kept the "open" state observable, the text field
  read it while drawing the handles and closed it again — opened and closed
  four times in 30 ms, then nothing (seen in the log). Verified in the
  colour picker: Cut / Copy / Select all on "#FF0000", Copy → Paste
  appears, Select all selects everything
- **[Bug fix]** **[📱 Mobile]** **The crooked toggle** (bug 4). The lines
  inside a toggle each have their own field, and Compose removed the space
  above the first line: the text sat ~3dp higher than the arrow and the
  numbers, which are written separately (the numbers had 2dp of extra top
  margin). Now the line is made like the shared text's (24, text in the
  middle), without the 2dp, and the lines inside a toggle keep the pace of
  those outside (no 4dp above and below). The checkboxes' little square no
  longer needs to be raised by hand (`CHECKBOX_BASELINE_LIFT` gone).
  Verified: arrow centred on the title, "1." on the line of "A", same pace
  as the list outside
- **[Bug fix]** **[📱 Mobile]** **"1." inside a toggle didn't make the
  numbered list** (bug 4): the rule was only in the shared text. Now also in
  the lines inside a toggle (`startNumberedListInBlock`). Verified: Enter on
  the empty line, "1." → list from 1, "New" written inside; then undone
- **[Bug fix]** **[📱 Mobile]** **The empty page: the first Enter lost the
  caret** (found by Claude testing bug 2, fixed on request). A page without
  lines shows an empty one that isn't in the database; the first Enter
  saved only the new line, the empty one disappeared and with it the field:
  keyboard open, bar gone, and what was written went nowhere. To find it
  the database had to print every write of its own: at the tap on the empty
  page none started. Now the empty line has the same id while you stay on
  the page (before it changed at every database read) and is saved
  **together** with the first change, in the same transaction
  (`placeholder`, `unsavedPlaceholderIn`). New tests in `EmptyPageTest`.
  Verified: "Untitled" page emptied, tap under the title, Enter, "Line" →
  bar in its place, "Line" on the second line, in the database the two
  lines with the first saved; then undone
- Noted: a toggle in "My Space" was modified by the user at 17:45 (lines
  "A" redone, toggle closed); left as it is

### ✏️ v0.52.3 — 25/09/2026 · 13:31 — Divider under the pages, fonts menu no longer covered
*(Claude Code on the PC; verified on the phone the same day; 89 tests passed; commit only local)*

- **[Change]** **[📱 Mobile]** **A divider under the page tree** in the
  sidebar, the same as the one under "Main menu": the tree's last page and
  "Search" no longer look like the same list (`Sidebar.kt`). Verified on
  the phone: the divider is between the last "Untitled" and "Search"
- **[Bug fix]** **[📱 Mobile]** **The fonts menu was covered by the
  phone's Copy / Cut / Select all bar**, when some text was selected: it
  covered Garamond and Arial. The menu doesn't take focus on purpose (so
  the keyboard and the Aa bar stay), so the field stayed focused with the
  selection and the phone left the small bar over the menu. Now opening the
  menu the small bar closes (`LocalTextToolbar.hide()` in
  `FontPickerChip`); the selection stays, and the small bar comes back
  after the choice. Verified: "sotto" selected, menu opened — everything
  visible, Garamond included — Garamond chosen → only "sotto" in Garamond,
  still selected; then undone with ↶
- Noted during the test: in the "My Space" page a test word is in
  **Futura**. Claude didn't put it: at 13:21 it was in the normal font, at
  13:27, the app just reinstalled and before touching the page, it was
  already in Futura. **Confirmed by the user**, who had tried it. Left as
  it is

### ✏️ v0.52.2 — 25/09/2026 · 13:23 — Font and size on the selected text
*(Claude Code on the PC; verified on the phone the same day; schema at version **31**; 89 tests passed; commit only local, **not** on GitHub until the user says so)*

- **[Removed]** **[📱 Mobile]** **"Font" and "Font size" in the settings.**
  They were two greyed-out entries, put there for a future font for the
  whole Workspace: removed, with their labels (`Strings.font`,
  `Strings.fontSize`)
- **[Change]** **[📱 Mobile]** **Font and size apply only to the selected
  text.** Before, the Aa bar's two entries changed **the whole page**,
  whatever was selected (it wasn't a bug: it was made that way on 24/09,
  "per page"). Now they behave like the colour: you select a piece of text,
  choose the font or type the size, and **only that piece** changes; you
  select another piece and give it another font. The two entries show the
  font and size of the selected text (or of the text right before the
  caret). Tapping them with nothing selected shows **"Select some text
  first"**. Font and size are in the text itself, like bold and colour
  (`RichTextSpan.font`, `RichTextSpan.size`; `setFontInRange`,
  `setSizeInRange`, `spanAt`;
  `PageEditorViewModel.applyFontToCapturedSelection`,
  `applySizeToCapturedSelection`, `selectionTypography`; on screen
  `fontStyleOf`), and are undone with ↶ like every other change. The
  default 16 isn't written in the text. Text typed right after a piece in
  another font takes its font, like bold. There is nothing "per page"
  anymore: `PageTypography`, `LocalPageTypography`, `setPageFont`,
  `setPageFontSize` gone. Simple tables' cells and the page title, which
  took the page's font, go back to the system font (they are text without
  formatting)
- **[Change]** **[📱 Mobile]** **Lines grow for large text.** With a size
  larger than 16 in a group of lines, **all** the lines of that group get
  taller, as much as the largest piece needs; why not just its own, see
  "Known limits"
- **[Project]** Migration **30→31** (`MIGRATION_30_31`): pages that had a
  "page" font or size give them to every piece of text of their blocks that
  doesn't have one of its own already — so they show as before — and then
  `pageFont` and `pageFontSize` are emptied in all pages. The two columns
  stay (removing them would mean rebuilding the table) but nobody writes
  them anymore. `MigrationTest` also tests from 30 (schema in
  `test/resources/schema/v30.sql`, generated from Room's code at commit
  `a68a44c`, with a page in Garamond at 20 and its blocks); new tests in
  `RichTextFontTest`. Verified on the phone: database backup,
  `user_version` 31, no crash
- Verified on the phone, in the "My Space" page: selected "prova" →
  Consolas, **only that word**; selected "titolo" → the entries say
  "Default" and "16", chosen Georgia and then 28 → only "titolo", large,
  and the group's lines wider; in the database
  `{"text":"prova","font":"CONSOLAS"}` and
  `{"text":"titolo","font":"GEORGIA","size":28}`; ↶ brings "titolo" back
  to 16 and the lines to the previous pace; with just the caret "Select
  some text first"; the settings without Font and Font size. "prova" in
  Consolas and "titolo" in Georgia stayed in the page, to show them
- Noted during the test (it was already there): with a word selected, the
  fonts menu opened **under** the system Copy / Cut / Select all bar,
  which covered Garamond and Arial. **Fixed right after**, see the entry
  above

### ✏️ v0.52.1 — 25/09/2026 · 12:59 — Timer: the 10, 15 and 30 minute presets removed
*(Claude Code on the PC; verified on the phone the same day)*

- **[Removed]** **[📱 Mobile]** The presets 00:10:00, 00:15:00 and 00:30:00
  under the Timer's wheels, those of the second photo (v0.52): only
  **Start** remains

### 🆕 v0.52 — 25/09/2026 · 12:56 — Stopwatch and Timer among the widgets, like those of the Samsung clock
*(Claude Code on the PC; verified on the phone the same day; 80 tests passed)*

- **[New feature]** **[📱 Mobile]** **Stopwatch and Timer among the
  widgets**, like those of the Samsung clock.
  - **Stopwatch** (`StopwatchWidget`, `StopwatchView` in
    `SidebarTimers.kt`): the dial with sixty ticks with the hundredths in
    the centre in peach colour, the **blue** hand for the seconds of the
    total time and, after the first lap, the **white** one for the lap in
    progress (it is so in the photos: at 00:32.52 the white one marks the
    24.76 s of the fourth lap). Buttons as asked: **Lap** (off) and
    **Start**; running **Lap** and **Stop** (dark red); stopped **Reset**
    and **Resume**. Below, the **Lap / Lap times / Overall time** list,
    from the most recent lap, with the **fastest in orange and the slowest
    in red** as in the photo (from three laps up: with two both would be
    coloured — my choice, to be confirmed). It counts on the clock like the
    pomodoro: the time goes on even with the sidebar or the app closed
  - **Timer** (`TimerWidget`, `CountdownTimer`, `TimerView`): three
    **wheels** spinning endlessly as in Samsung's (hours 0-99, minutes and
    seconds 0-59), each stops on a whole number and the time is saved right
    away; **Start**. *(The presets 00:10:00, 00:15:00 and 00:30:00 of the
    second photo were there in this version and were removed right after,
    at the user's request: see v0.52.1)*
    Started: the dial with the **orange ticks for the time left**, inside
    the set time ("10 m"), the countdown ("9:55") and the **bell with the
    time it ends**; below **Pause** / **Resume**. At the top right **↻**
    (back to the wheels) and **+** (another timer, running together with
    the others): the new page opens by itself with **Cancel / Start**, like
    the "Add timer" of the third photo, and the timers are **swiped
    horizontally** with the dots below. Finished, it **rings** (the app's
    notification sound, only once, even with the sidebar closed) and shows
    **0:00 in red** with **Reset**
  - Verified on the phone: stopwatch Start → three Laps → Stop (00:10.28,
    laps 01 white, 02 red, 03 orange), Resume from 10.28 to 11.83, Reset;
    timer with the wheels brought to 00:07:02 and then to 00:00:05 by
    dragging, Start → 0:03 with the ticks and the end at 08:57, at zero the
    sound (08:57:05-08:57:08) and red 0:00; "+" → second timer with
    Cancel/Start and the dots, started, **the horizontal swipe browses the
    timers without closing the sidebar**, ↻ and Cancel remove it. New tests
    in `WidgetsTest` (stopwatch, laps and colours with the photos' times,
    timer states, wheels, texts): 80 tests passed

### ✏️ v0.51.1 — 25/09/2026 · 08:34 — Time zones: the time to the right of each zone in the picker
*(Claude Code on the PC; verified on the phone the same day)*

- **[Change]** **[📱 Mobile]** **The time in the zone picker of "Add to the
  list".** In the widget's list the time was already there, it was missing
  in the picker. Now there, to the right of every zone (under the name
  "Region · GMT±x" stays), there is **the time in that place now**, which
  advances while the window stays open. Now it is there in all three ways
  that open the window: "Add to the list", "Add clock" and a clock's
  "Change time zone" (`ZonePickerSheet`). Verified: Midway 19:33 → 19:34 at
  the minute's tick, Marquesas (GMT-9:30) 21:03 → 21:04; in "Add clock"
  19:36 at 08:36 Italian time

### 🔧 v0.51.0.1 — 25/09/2026 · 08:29 — Pomodoro: the digit changes every second from when ▶ is pressed
*(Claude Code on the PC; verified on the phone the same day)*

- **[Bug fix]** **[📱 Mobile]** **…and it started a second late.** Reported
  by the user after the extra second's fix (v0.51): the pomodoro started a
  second after the tap on play. The widget redrew at the tick of the
  **clock's** seconds, and the timer starts in the middle of any second:
  the first "00:59" arrived up to almost two seconds after ▶. Now the
  pomodoro has its own time, refreshed **when the timer's digit changes**
  (`rememberTimerNow`, `msUntilTimerTicks`); the clocks stay on the clock's
  seconds. New test `WidgetsTest.theTimerRedrawsExactlyWhenItsDigitChanges`.
  Verified with timed screenshots from the phone: "01:00" up to 0.76 s after
  the tap, "00:59" at 1.13 s, "00:58" at 2.16 s, "00:57" at 3.06 s

### 🆕 v0.51 — 25/09/2026 · 08:25 — The user's tests: "Reposition cover" in the cards, the pomodoro's sound, SimSun instead of MS YaHei
*(Claude Code on the PC; all verified on the phone the same day; schema at version **30**)*

To the three questions asked right after, the answer was: framing **only
of the card** (not the page's cover); sound **only of the pomodoro**; MS
YaHei to be removed, with SimSun in its place. And during the test, the
ringtone that rang forever.

- **[New feature]** **[📱 Mobile]** **"Reposition cover" in the gallery's
  cards.** Long-pressing a card showing the cover, in the actions there is
  "Reposition cover": the cover **inside the card** is dragged and pinched
  (up to 4×, as in the page), never beyond the edges; ✓ saves, ✕ leaves it
  as it was. While moving it the card has a border and doesn't open when
  tapped. The framing is **only of the card**: the cover at the top of the
  page stays as it is, and vice versa. Until one is chosen, the card uses
  the page's as before; a new cover resets both. The entry is there only in
  the gallery with "Page cover", a cover to move and the content not locked
  (`GalleryCard`, `CardCoverMoveControls`, `DatabaseViewModel.setRowCardCover`;
  columns `cardCoverScale`, `cardCoverOffsetX`, `cardCoverOffsetY` in
  `pages`). Verified: a test card's cover dragged, saved in the card only
  (`cardCoverOffsetY` 0.085, the page at 0)
- **[New feature]** **[📱 Mobile]** **The pomodoro's sound among the
  phone's ringtones.** In the gear there is "Sound": it opens the system
  list of sounds (with the preview), you choose, and with "Done" it applies
  **to that pomodoro**; "Use default sound" goes back to the app's
  notification sound, which stays the starting one
  (`PomodoroWidget.soundUri`, `soundName`). On the S25 Ultra the phone asks
  which app to choose it with (SecSoundPicker or Zedge, if installed).
  Verified with "Asteroid"
- **[Bug fix]** **[📱 Mobile]** **A sound that never ended.** The phone's
  ringtones (Samsung's "Galaxy" ones) are made to repeat until answered —
  the file itself asks for it, with the `ANDROID_LOOP` label — and played as
  an alert they went on forever (stopped by closing the app). Now every
  sound of the app (end of a pomodoro phase, and "Play" in the sound
  settings) plays **only once** (`isLooping = false`, from Android 9) and
  anyway **stops after 8 seconds** (`WidgetStore.playOnce`). Verified:
  started at 08:22:58, stopped and released at 08:23:06
- **[Bug fix]** **[📱 Mobile]** **The just started pomodoro showed an extra
  second** ("01:01" for a moment, with a minute). The time the sidebar
  passes to the widget refreshes at the tick of every second, and right
  after ▶ it was behind by almost a second compared to when the timer
  started. Now the time shown never exceeds the starting one
  (`PomodoroWidget.remainingAt`). New test
  `WidgetsTest.aJustStartedPomodoroNeverShowsMoreThanItStartedWith`.
  Verified: right after ▶ "01:00", and a minute later the break in green
- **[Removed]** **[📱 Mobile]** **MS YaHei.** It wasn't broken: its free
  look-alike, Noto Sans SC, is **the same design** Android already uses for
  Chinese, and on the phone it couldn't be told apart from the system font.
  Removed at the user's request; pages using it go back to the system font
  (migration 29→30)
- **[New feature]** **[📱 Mobile]** **SimSun** instead of MS YaHei, in the
  Chinese group: a thin-stroked Song, rendered with **Noto Serif SC in the
  Light weight** — the same font as Songti but lighter, which resembles
  SimSun and can be told apart from Songti. It is downloaded like the other
  Chinese ones. Verified on "我们都是意大利人": Default, Songti and SimSun are
  three different designs
- **[Project]** Migration **29→30** (`MIGRATION_29_30`): pages with
  `pageFont = 'MS_YAHEI'` go back to NULL — it must happen before any read,
  because the name no longer exists in the enum — and three empty columns
  for the cards' framing. `MigrationTest` now tests both from 28 and from 29
  (schema in `test/resources/schema/v29.sql`, generated from Room's code at
  commit `b53a7bf`, with a page in MS YaHei and one in Songti);
  `AppDatabase.resetForTests` so the two tests don't step on each other's
  toes. Verified on the phone: `user_version` 30, columns at the end of the
  `CREATE TABLE`, no crash
- **[Project]** Entry 14 confirmed by the user: small, medium and large
  cards as in Notion, **medium** by default — it is already so

### 🔧 v0.50.0.1 — 24/09/2026 · 23:17 — Resumed on the PC on 24/09/2026 — the cloud work tested on the phone
*(Claude Code on the PC, S25 Ultra with wireless debugging, build signed with the PC's debug key; outcome entry by entry in "Cloud sessions log" → "To be verified on the phone")*
- **[Project]** Cloned the `claude/funny-ramanujan-uuwahs` branch (pull
  request #1 wasn't merged yet) into `C:\Users\Utente\MyNotesApp`. The PC
  was missing Git: Git for Windows 2.55 installed with `winget`. The
  project has no `gradlew`: built with Gradle 8.7 already in cache
  (`%USERPROFILE%\.gradle\wrapper\dists\gradle-8.7-bin`) and JDK 21
  (`%USERPROFILE%\.jdks\jdk-21.0.12.1+1`), with a `local.properties`
  (ignored by Git) saying where the SDK is. `assembleDebug` succeeded,
  warnings only; `testDebugUnitTest` 70 out of 70. **Android Studio wasn't
  opened**: the sync there is still to be seen
- **[Project]** **Migrations 24→29 tested on a real phone.** notE wasn't
  installed (no database to save), so: build of `4a35c64` (schema 24)
  installed with the PC's key, test notes written by hand (H1, H2, H3,
  paragraph, list, toggle with a line inside, database with a row and some
  text in its page), database copied, then the new build installed **over
  it** (`adb install -r`, no uninstall asked). Outcome: `user_version` 29;
  the seven new columns at the end of the `pages` `CREATE TABLE`, without
  backticks (the table wasn't rebuilt); the three headings turned into
  `PARAGRAPH` with the text intact; everything else identical;
  `integrity_check` and `foreign_key_check` clean; no error in `logcat`
- **[Bug fix]** **[📱 Mobile]** **Aa bar: the size entry was empty** —
  only the little arrow, no "16" (cloud work). `BarValueChip` shortens the
  text with the dots with `weight(1f, fill = false)`, which wants a maximum
  width: the bar scrolls horizontally, and without a limit Compose gave the
  text zero pixels. The font entry had it (`FONT_CHIP_MAX_WIDTH`), the
  size one didn't: now it has `SIZE_CHIP_MAX_WIDTH` (64 dp). Verified:
  "16", and 32, 72, 5
- **[Bug fix]** **[📱 Mobile]** **Undo made the app close** in any page
  with a non-empty toggle that wasn't the first block
  (`SQLiteConstraintException: FOREIGN KEY constraint failed` in
  `replaceAllBlocks`). **It was already there before the cloud.** Undo's
  snapshot arrives sorted only by `orderIndex`, which for children restarts
  from 0: the child was reinserted before its toggle, and the foreign key
  to the parent refused it. Data wasn't lost (all in a transaction), but
  the app closed. Now it is reinserted with `inDocumentOrder`, parents
  before children. New test
  `PageRepositoryTest.undoSnapshotPutsBackAToggleWhoseChildComesFirstInOrderIndex`:
  without the fix it fails with the same error as the phone (787), with the
  fix it passes. Verified on the phone
- **[Bug fix]** **[📱 Mobile]** **After "Duplicate" from the block menu the
  text stayed behind, and the next letter deleted the copy.** The joined
  field showed the previous lines (a single copy) with the list marks
  shifted by a line; typing a letter, `updateRun` compared seven lines with
  eight blocks and **deleted the copy's block** (seen in the database).
  Cause: in `MergedTextRunField` the normal branch of `onValueChange` set
  `pendingText` even when only the caret moved or something was selected;
  without any save on its way `pendingText` was never cleared, and the
  field took every change arriving from outside for "database behind".
  Before the cloud the only ones were Undo and Redo, which clear it; the
  block menu is new. Now `pendingText` is set only if the text really
  changed. Verified: same sequence, two lines right away, marks right, the
  letter goes into the right block and the copy stays
- **[Bug fix]** **[📱 Mobile]** *(not fixed, it was already in `4a35c64`)*
  **The ghost line**: turning from the "/" menu the **last** line of a text
  group into an island (toggle, database, page, table), an extra line stays
  in the field with the text it had (empty if it was empty).
  `applySlashEntry` sets `pendingText` legitimately (it has just removed
  "/command"), then the line leaves the group; Room delivers only the final
  state, different from `pendingText`, and the field takes it for "database
  behind". Nothing is lost: writing in it a new block is born, and
  reopening the page it disappears. To be fixed calmly: it is the most
  delicate point of the app
- **[Bug fix]** **[📱 Mobile]** *(not fixed, it was already there)*
  Creating a database or a page from the "/" menu, the command's text
  ("/table", "/gallery") **stays saved** in the link's block:
  `convertToDatabaseLink(block)` saves the copy of the block taken before
  the command was removed. It doesn't show (the link shows the page's
  name)
- **[Bug fix]** **[📱 Mobile]** *(not fixed, cloud work, looks only)*
  Under the clock of the "Time zones" widget you read "Thu 24 Sep •
  GMT+02:00" instead of the "CEST" abbreviation the entry promised: on this
  phone the zone's short name, with the app's language in English, comes
  out as an offset
- **[Project]** How it was tested: `adb` and `uiautomator dump` to tap the
  elements by name, a screenshot between one step and the next, and the
  database reread from the PC after every operation that writes (`.db` and
  `-wal`, without `-shm`). Two traps seen: **wireless debugging turns off
  by itself** every 10-15 minutes (better the cable); and **a swipe made
  with the keyboard open starts on the Samsung keyboard** and types
  characters — a "⁵" ended up in a test paragraph, removed by hand. The
  Back key with the keyboard already closed leaves the app

### 🆕 v0.50 — 24/09/2026 · 15:52 — Subscript and superscript in the Aa bar
*(partly verified on the phone on 24/09/2026: entry 30)* *(cloud session of 24/09/2026, request 39: built, 70 tests passed — 7 new —, no migration, **not tested on the phone**)*
- **[New feature]** **[📱 Mobile]** In the **Aa** bar, right after S, two
  buttons: **x₂ subscript** (an x with the small 2 at the bottom right) and
  **x² superscript** (the 2 at the top right), as asked. Like B, I, U, S
  they act on the selected text, even across several lines, and a second
  tap removes it. **They exclude each other**: giving superscript to a
  subscript turns it, as in Word (`withSubscript`, `withSuperscript`)
- **[New feature]** **[📱 Mobile]** Subscript or superscript text is **at
  seven tenths of the page's size** (it grows with it) and sits below or
  above the line; the other formatting — bold, colour, spoiler — stays. It
  is only looks: the letters are where they were, so caret, selection,
  search and text count don't change (`scriptStyleOf`, used both by the
  joined text and by the islands)
- **[Project]** In the saved text there are two new keys, `subscript` and
  `superscript`, written only when they are on: the previous text reads as
  it was and, without subscripts, is saved identical. No migration (it is
  JSON inside a column that already existed)
- **[Bug fix]** **[📱 Mobile]** *(found with the test drawing, before the
  phone)* In the buttons, the superscript's 2 pushed down the x, which no
  longer stood at the height of B, U, S; then, with the 2 put in a corner,
  it stayed halfway in both buttons because it carried along the normal
  text's line height. And in the text the subscript, with the standard
  shift, went down below the legs of g and p: now it goes down less than
  the superscript goes up
- **[Project]** Tests: `RichTextScriptTest` (6: only on the selection, one
  replaces the other, a second tap removes, the other formatting stays,
  you keep writing in superscript, the text saved before reads and saves
  again the same) and `ScriptRenderTest` (the buttons and a text with H₂O
  and mc², in `app/build/widget-screenshots/scripts.png`). New texts in the
  eight languages (`EditorStrings.subscript`, `superscript`, which screen
  reader users hear on the two buttons)

### 🆕 v0.49 — 24/09/2026 · 15:21 — Widgets in the sidebar — time zones, progress, counter, pomodoro
*(partly verified on the phone on 24/09/2026: entry 29)* *(cloud session of 24/09/2026, request 37: built, 63 tests passed — 12 new —, no migration, **not tested on the phone**)*
- **[New feature]** **[📱 Mobile]** **The "Widgets" section at the top of
  the sidebar is no longer an empty space** (`WIDGETS_SPACE`, removed):
  widgets are added from **"+ Add widget"** at the bottom of the section,
  they arrive at the bottom, each in its rounded box with the name and the
  **three dots** (Move up, Move down, Remove with the confirmation; on the
  counter also Rename). Of each you can put as many as you want
  (`SidebarWidgets.kt`, `WidgetsSection`)
- **[New feature]** **[📱 Mobile]** **The switch**: a small arrow next to
  the "Widgets" title, up to close and down to reopen, **hides the whole
  section** and leaves only the title. The choice **stays** closing the
  sidebar and the app, as asked
- **[New feature]** **[📱 Mobile]** **Time zones**: **clocks** two per row,
  with the city's name above; the analog one as in the user's photo —
  ticks, hour, minute and second hands, and inside the written time
  ("16:05:09") with the day in the app's language and the zone's
  abbreviation ("Thu 24 Sep • CEST"); the digital one with the large time.
  **Long-pressing a clock**: Analog / Digital, Change time zone, Remove.
  The first clock is born on the app's zone (Settings)
- **[New feature]** **[📱 Mobile]** **The list of zones**, under the
  clocks: city, time, sun or moon (day from 6 to 18). **Always from west
  to east**, as asked: it is sorted every second on the distance from
  Greenwich **now**, because daylight saving time moves the zones (New
  York and London aren't always five hours apart); for ties, by name
  (`sortedWestToEast`). Long-pressing a row, you remove it
- **[New feature]** **[📱 Mobile]** **The zone picker**: all the zones with
  a city name (the old duplicates like "US/Pacific" or "Etc/GMT+5" out),
  also from west to east, with "Region · GMT+5:30" below, and search by
  city, region or "GMT+2" (`ZonePickerSheet`)
- **[New feature]** **[📱 Mobile]** **Progress** (the "Life Progress
  Bar"): **Year, Month, Week, Day** already inside, and **"Add a date"**
  for your own — name, From, To, with the databases' calendar; To before
  From isn't accepted. Bar with the frame, filled as much as has passed,
  "Year: 73%" next to it in the same colour; **long-pressing a bar** you
  change **the colour** (the text's palette, without the text/background
  switch, which means nothing here: `ColorPickerSheet` now has
  `showTargetSwitch`) and, for your own dates, Edit and Delete. Bars start
  **green** as in the photo. The week starts on Monday; a date is counted
  from the midnight of the first day to the midnight of the last, and from
  there it is at 100%; everything in the app's zone (`fractionAt`)
- **[New feature]** **[📱 Mobile]** **Counter**: square − and + at the
  sides of the number, below the turning arrow to reset, as in the photo.
  An optional name above the number (Rename, from the three dots)
- **[New feature]** **[📱 Mobile]** **Pomodoro**: the rounded box with the
  **large time** ("25:00"; tapping it you write it by hand, minutes and
  seconds), below **the session's name** (tapping it you change it; in the
  break it says "Break") and three round buttons: **▶** (which becomes ⏸),
  **from the start** (stopped, on the session, at full length), and **the
  gear** with **Session length** and **Break length**, − and + from 1 to
  180 minutes. The colour tells the phase: orange the session, green the
  break. When a phase ends it **rings** and the other starts by itself,
  like the "25 + 5" timers of the photo
- **[Project]** How the pomodoro is made: **it counts on the clock, not by
  ticks of a second**. When it runs it saves *when* the phase will end
  (`endsAt`), and the time left is derived from there; so it stays right
  with the sidebar closed, with the app in the background or closed and
  reopened. A wait in `WidgetStore`, which lives as long as the app and not
  as long as the sidebar, sleeps until the end of the phase and then
  advances and rings. Reopening the app after hours, the past phases are
  all recovered, counting from the exact moment each ended, and **without
  ringing** for the past (`advancedTo`). The sound is the one chosen for
  notifications in the settings (or the phone's), and it is silent if
  notifications are muted
- **[Project]** Where they live: **in the phone's preferences**, in a
  separate file (`widgets`), in JSON, and not in the notes' database: they
  are tools, not content — in the database they would end up inside every
  backup and every copy of a page. So **no migration**, and a broken widget
  can't touch the notes. `WidgetStore` is observable like `AppSettings`:
  the sidebar redraws by itself at every change. Clocks and bars update
  every second **only with the sidebar open** (closed, it stays drawn
  behind the page, and redrawing it for nothing would consume battery)
- **[Project]** **The widgets' drawing can be looked at in the cloud
  too**: `WidgetsRenderTest` draws the section (all four widgets, and the
  closed section) with Robolectric and saves it as an image in
  `app/build/widget-screenshots/`. Needed, only for the tests, Compose's
  `ui-test-junit4` library and `unitTests.isIncludeAndroidResources` in
  `app/build.gradle.kts`. Compose's `captureToImage` under Robolectric
  never finishes: the window is drawn by hand onto a bitmap
- **[Bug fix]** **[📱 Mobile]** *(found with that drawing, before reaching
  the phone)* **The content of every widget was all drawn in the same
  spot**, lines one over the other: it was in a `Box` instead of a
  `Column`. And **the progress bars were each as long as they liked**,
  because the label next to them ("Year", "Week") widened and narrowed;
  now it has a fixed width
- **[Project]** Tests: `WidgetsTest` (11: year, month, week from Monday,
  day, own dates, west-east even with daylight saving time, names and
  "GMT+5:30", the pomodoro advancing, recovering several phases, staying
  still in pause, and saving to JSON round trip) and `WidgetsRenderTest`
  (1). New texts in the eight languages (`WidgetStrings`).
  `DayPickerDialog` and its two helpers in `DatabaseViewScreen.kt` are now
  `internal`, to use them here too
- **[Project]** README: corrected two statements left behind in the Known
  limits — "search in databases doesn't exist" (it has been there since
  request 35) and "Linked view of data source" among the greyed-out entries
  of the "+" and "/" menus (built in request 36). Struck through, not
  deleted

### 🆕 v0.48 — 24/09/2026 · 14:56 — Linked views — the same database in another page
*(partly verified on the phone on 24/09/2026: entry 28)* *(cloud session of 24/09/2026, request 36: built, 51 tests passed — 9 new, including the 28→29 migration opened by Room —, **schema at version 29**, **not tested on the phone**)*
- **[New feature]** **[📱 Mobile]** **"Linked view of data source"** in the
  "/" and "+" menus (greyed out until now) and in "Turn into": you choose a
  database from a list with search at the top — all the app's databases,
  each with the page it lives in below, like Notion's "Existing data
  sources" (`LinkedSourcePickerSheet`) — and **the same database** appears
  in the page: same rows, properties, pages and what is written inside. It
  isn't a copy: writing on one side is writing on the other
- **[New feature]** **[📱 Mobile]** **The view is born identical** to the
  original — view, filter, sorting, grouping, gallery, calendar, hidden
  properties — and from there it has **its own settings**: in one page you
  filter Status = Done and in the main database nothing changes. **Hidden
  properties** belong to the view too (`PageEntity.viewHiddenColumnIds`)
- **[New feature]** **[📱 Mobile]** **The title is "↗ Database name"**, the
  origin's live one, and **tapping it opens the main database** full
  screen, as asked (`LinkedViewTitle`). Icon, name, content lock and
  "simple database" are the origin's
- **[New feature]** **[📱 Mobile]** **A new row is born with the filter's
  value**, as in Notion — in a filtered view, "New page" creates a row with
  that value in the main database. It applies to every filtered database,
  not only views: before a row created with an active filter disappeared
  as soon as it was born. For selects (the first chosen value) and for
  checkboxes filtered on the ticked ones (`fillFilterValue`)
- **[New feature]** **[📱 Mobile]** Block menu on a view: the database's
  entries (simple / complex, Lock database, Rename, Edit icon) act **on the
  origin**, the page's ones (Duplicate, Move to, Move to trash, favorites)
  **on the view**; no "Turn into page". **Removing the view** (Delete in
  its settings) says so with a window of its own: only the view goes away,
  the database stays
- **[New feature]** **[📱 Mobile]** If the origin database is deleted or in
  the trash, in place of the view a line says so, with the six dots to
  remove it (`LinkedSourceMissing`); restoring it, the view comes back
- **[New feature]** **[📱 Mobile]** Views **don't appear in the sidebar or
  in the general search**: they are another way of looking at a database
  that is already there, and they would have repeated it. A page with only
  views doesn't have the children's triangle (`BlockDao.countLinksInPage`)
- **[Project]** How it is made. A linked view is a database page with
  `sourceDatabaseId` (the origin) and **no data of its own**: the
  database's ViewModel keeps separate **the view's page** (`page`, the
  settings) and **the data's one** (`dataPage`, the origin), and reads
  rows, columns, cells, icons, covers and text for search from the second.
  For a real database they are the same page, and nothing changes.
  **Migration 28→29**: two `TEXT` columns in `pages`, empty for everything
  existing
- **[Project]** **A trap found while writing it**: in the view's state the
  columns have the view's "hidden"; every function saving a column starting
  from there — renaming it, changing its options, moving it, centring it —
  would have written that "hidden" **into the origin database**, where
  everyone would have seen it. Now it always starts from the real columns
  (`rawColumns`); counter-check done: with the protection removed, the test
  catches it
- **[Bug fix]** **[📱 Mobile]** *(found writing this)* **A new row in a
  filtered database could take the order number of a hidden row**: the
  number was calculated on the visible rows, already filtered (or, from
  today, searched). Two rows with the same number have no sure order
  between them. Now it counts on all of them (`lastRowOrderIndex`)
- **[Project]** Tests: `MigrationTest` (a file at version 28 with the
  schema of the time, opened by `AppDatabase.getInstance` as the app does:
  Room runs the migration and checks the schema; counter-check done —
  without the migration opening fails), `LinkedViewModelTest` (4: the
  origin's rows with the view's filter, property hidden only there, new row
  in the origin with the filter's value, origin in the trash), and 4 new
  checks in `PageRepositoryTest`. New texts in the eight languages
  (`DbStrings.linkedViewTitle`, `linkedViewHint`, `openSourceDatabase`,
  `linkedSourceMissing`, `noDatabasesToLink`, `removeLinkedViewTitle`,
  `removeLinkedViewText`)

### 🆕 v0.47 — 24/09/2026 · 14:35 — Search inside a database — the magnifier next to Sort
*(partly verified on the phone on 24/09/2026: entry 27)* *(cloud session of 24/09/2026, request 35: built, 42 tests passed — 10 new —, no migration, **not tested on the phone**)*
- **[New feature]** **[📱 Mobile]** In every database's tools row, **right
  after Sort**, the **magnifier**. Tapping it, where the row was **the
  search bar** appears, with the caret already inside: you type right away,
  as in Notion. The **X** closes it, clears the word and puts back the
  tools row (`DatabaseSearchBar`). You can search with "Lock view" too:
  searching doesn't change how the database is made
- **[New feature]** **[📱 Mobile]** **What is searched**, at every letter,
  without telling upper and lower case apart: the rows' **name**; the value
  of the **properties** that read as they are written — text, number,
  select, multi-select (value by value), URL, email, phone; and
  **everything inside the row's page** — the blocks' text (in closed
  toggles too), the tables' cells, the subpages' names. Dates, checkboxes
  and automatic dates no: their saved value ("2026-09-24", "true") isn't
  what you read, and "true" would find every ticked checkbox
- **[New feature]** **[📱 Mobile]** **What lights up**, as asked: if the
  word is **in the name**, **its letters** light up in yellow; if it is
  **inside** (a property, the page), **the whole row** lights up in a
  lighter yellow — or the card, or the bar. Yellow and not the app's light
  blue, which means "selected". In **all six views**: table, board, list,
  gallery, calendar, timeline (`highlightedTitle`, `Modifier.foundInside`,
  `LocalRowSearch`)
- **[New feature]** **[📱 Mobile]** **Rows without the word are hidden**
  while searching, as in Notion; with no row found, under the bar it says
  "No results". The search is an extra filter on top of the settings' one,
  and it is in the database's ViewModel (`searchRows`, inside the same
  calculation as filter and sorting): that is why it applies in every view
  without any of them having to know it, and opening a found page and going
  back the search is still there (without reopening the keyboard)
- **[New feature]** **[📱 Mobile]** **A row created while searching stays
  visible** until the search is closed: it is empty, it doesn't have the
  word, and by disappearing right away it would have made "New page" look
  broken
- **[Project]** **Fast even with a lot of text**: the text of the rows'
  pages is read from the database **once**, when the search opens and when
  coming back to the screen (three queries:
  `DatabaseDao.getRowPageBlockTexts`, `getRowPageTableTexts`,
  `getRowPageLinkTitles`; `DatabaseRepository.searchIndex`); every letter
  typed afterwards searches in memory. The name and the properties instead
  are the table's live ones
- **[Project]** Tests: `DatabaseSearchTest` (9 checks on `searchRows`:
  name, properties, multi-select, page, dates and checkboxes excluded,
  spaces, accents, new row) and one more check in `PageRepositoryTest` on
  the index, on a real database (text split into several pieces with bold
  and spoiler, table, subpage, row never opened). New texts in the eight
  languages (`DbStrings.searchInDatabase`, `closeSearch`)

### ✏️ v0.46.2 — 24/09/2026 · 14:24 — Undo and Redo for Move to, Duplicate and Move to trash too
*(partly verified on the phone on 24/09/2026: entries 25 and 26)* *(cloud session of 24/09/2026, request 34: built, 32 tests passed — 16 new, on the real database —, no migration, **not tested on the phone**)*
- **[Change]** **[📱 Mobile]** **Undo also undoes "Move to", "Duplicate"
  and "Move to trash", and Redo redoes them.** Before, Undo knew only the
  snapshots of the open page's blocks, and these three touch other pages:
  - **Move to** → the page goes back **exactly where it was**: same page,
    same point among the lines, inside the same toggle. A database goes
    back **open inside the page**, if it was. A row's page goes back into
    its row **with the properties' values**, which the move had removed
  - **Duplicate** → the copy goes **to the Trash**, as in Notion when a
    page is removed: if in the meantime something was written in it,
    nothing is lost. If the copy was a new row of a database, the row is
    removed. Redo takes it out of the Trash and puts it back where it was
  - **Move to trash** → the page leaves the Trash and goes back to its
    place (done too: same mechanism, and the entry was in the same list of
    Known limits)
- **[Change]** **[📱 Mobile]** **Where it is undone.** From the block menu,
  in the page itself. From the three dots the screen goes away — you go
  back, or enter the copy — and the Undo passes to the **next page that
  appears** (`PendingPageUndo`, taken in `adoptPendingPageChange` when the
  page opens or you come back to it): back after a Move to, there Undo puts
  the page back where it was; inside the copy after a Duplicate, Undo sends
  it to the Trash and **goes back by itself** (staying in front of a page
  in the Trash would make no sense)
- **[Project]** How it is made. The page's history has **two kinds of
  step**: the blocks' snapshots, as before (`BlocksStep`), and the changes
  to pages (`PageStep`), which carry what is needed to go back — the links
  as they were, the row and its cells (`PageRepository.PageChange`:
  `Moved`, `Duplicated`, `Trashed`; `undoPageChange`, `redoPageChange`).
  Links go back to their place shifting the following ones by one; if the
  toggle they were in is no longer there they go to the bottom of the
  page, and if the page couldn't be put back anywhere it goes to the bottom
  of the main menu instead of staying unreachable (`putLinksBack`). Undo
  and Redo now run **one at a time** (`historyLock`): a page step works on
  the database for a moment, and two quick taps mustn't overlap. If the
  page has in the meantime been deleted forever (Trash emptied), the step
  is skipped
- **[Project]** **Tests on a real database, without a phone.** Added
  Robolectric 4.13 and `androidx.test:core-ktx` **only for the tests** (the
  APK doesn't change) and `PageRepositoryTest`: 16 checks running on real
  Room and SQLite, in memory, with foreign keys on as in the app — moving
  (at the bottom; in the same page; a database arrives as a link; inside
  its own subpage refused; a row's page leaves and with Undo comes back
  with its properties; inside a row never opened), duplicating and
  trashing with Undo and Redo (also a row's page, and a page deleted
  forever in the meantime), Undo's snapshots (tables, link to a deleted
  database, to a moved page), "Duplicate" of a toggle with a subpage, the
  toggle letting its lines out, "Turn into simple database"
- **[Project]** **Counter-check on the two defects "found reading the
  code"**: with each fix removed for a moment, its test fails. Without
  `withoutStaleLinks` Undo after deleting a database ends in
  `SQLiteConstraintException: FOREIGN KEY constraint failed (code 787)` —
  it is the app closing; without putting the cells back, the table stays
  empty. They weren't hypotheses. The fixes are in place, and the tests
  pass

### ✏️ v0.46.1 — 24/09/2026 · 14:04 — Move to — the tree with search
*(partly verified on the phone on 24/09/2026: entry 24)* *(cloud session of 24/09/2026, request 33: built, tests passed, no migration, **not tested on the phone**)*
- **[Change]** **[📱 Mobile]** **"Move to" opens the sidebar's tree with
  search at the top** — the same as "Duplicate → Inside a page"
  (`DestinationPickerSheet`, now shared, and `MovePagePicker`) — from a
  page's three dots, from a full-screen database's three dots and from the
  block menu. Before it was a flat list, in alphabetical order, of text
  pages only
- **[Change]** **[📱 Mobile]** You can **also choose a database's page**:
  databases open like folders, and you choose one of their rows — even one
  never opened, whose page is born at that moment. Not the database itself:
  its content is rows, not blocks. Before row pages were excluded
- **[Change]** **[📱 Mobile]** **Always at the bottom** of the chosen page,
  as asked: also choosing the page it is already in (before nothing
  happened there). A notice says "Moved to …"; from the block menu you
  stay in the page, from the three dots you go back as before
- **[Change]** **[📱 Mobile]** **A moved database always arrives as a link
  to a page**, even if before it was open inside the page: to open it there
  again there is "Turn into database" (long-pressing the link, or from the
  database's three dots). Before it was always recreated open. The
  explanation line at the top of the tree says so
- **[Change]** **[📱 Mobile]** **"Move to" on the main page is there but
  greyed out** (before it wasn't there at all). The user left the choice
  between the greyed-out entry and a message "The main menu cannot be
  moved": the grey one was used, because it is already the way the app
  says "not here" (Import and Export in the same menu) and it doesn't cost
  a wasted tap. Changing it to the message is one line
- **[Bug fix]** **[📱 Mobile]** *(found writing this)* **A page could be
  moved inside one of its subpages**: the old list offered all pages except
  itself. The result was a closed loop — the page inside the subpage, the
  subpage inside the page — and neither could be reached from the main
  menu anymore. Now the page and everything it contains, all the way down,
  show greyed out and don't open, and searching they don't appear
  (`PageRepository.moveExclusions`); the repository refuses the move anyway
  if it comes another way
- **[Bug fix]** **[📱 Mobile]** *(found writing this)* **A moved row's page
  stayed in the database too**, that is in two places. Now it leaves the
  database, as in Notion: the row is removed with its properties, and the
  page becomes a page like the others (`isRowPage` off,
  `PageDao.setRowPage`). The explanation line says so before choosing
- **[Bug fix]** **[📱 Mobile]** *(found writing this; **confirmed by the
  tests** of request 34)* **Undo after deleting a database inside the page
  made the app close.** The deletion (`deleteDatabaseBlock`) takes a
  snapshot for Undo and then deletes the database; Undo put back the
  block, which pointed to a page no longer existing, and the database
  refused it (foreign key) — an error inside a coroutine with nobody to
  catch it. Now, at every Undo and Redo, the snapshot's links to pages
  **deleted, in the trash or moved to another page** don't come back
  (`PageRepository.withoutStaleLinks`, inside `replaceAllBlocks`). It
  applies to all roads — the subpage's three dots, the block menu, the
  trash — and replaces the previous entry's `forgetLinksTo`, which covered
  only the block menu
- **[Removed]** **[📱 Mobile]** The old "Move to" window (`MoveToSheet`),
  its destinations' query (`PageDao.getMoveDestinations`,
  `PageRepository.moveDestinations`), the functions loading it in the two
  ViewModels and the text "There's no other page to move it to"
  (`Strings.noOtherPage`): with the tree there is always at least the main
  menu
- **[Project]** `TreeRow` has the off state (`enabled`), grey and without
  the triangle. New texts in the eight languages (`Strings.moveHint`,
  `moveDatabaseHint`, `moveRowPageHint`, `movedInto`)

### 🆕 v0.46 — 24/09/2026 · 13:52 — The block menu — the six dots
*(partly verified on the phone on 24/09/2026: entries 22 and 23; Duplicate had a bug, fixed — see "Resumed on the PC")* *(cloud session of 24/09/2026, requests 31-32: built, tests passed, no migration — schema still at 28 —, **not tested on the phone**)*
- **[New feature]** **[📱 Mobile]** In the bar above the keyboard, **before
  the left indent**, an icon with the **six dots** (two columns of three,
  `Icons.Filled.DragIndicator`, the same as Notion's): it opens **the menu
  of the line where the caret is**. The caret goes off before opening it:
  the keyboard would disappear under the window anyway, and a line that
  "Turn into" makes disappear mustn't die with focus inside — the way the
  app used to close (see further down in the Changelog "The keyboard no
  longer disappears", and the root cause below it). The window
  (`BlockActionsHost`, new file `BlockActionsSheet.kt`) is at screen level,
  like the brush and the text size, because the bar disappears with the
  keyboard
- **[New feature]** **[📱 Mobile]** **For a text line** (paragraph, lists,
  checkbox, toggle, lines inside a toggle): **Turn into, Color,
  Duplicate**. Edit icon, Add to favorites, Move to and Move to trash are
  there only for pages, as asked (request 32). **Rename** too: a text line
  has no name to change, its name is the text itself — a choice to confirm
  with the user
  - **Turn into**: the **same entries as the "/" and "+" menus**, in the
    same order (`SLASH_ENTRIES`), the unbuilt ones greyed out, with the
    **tick on the current type**. Compared to "/" two differences, coming
    from where you start: the line's text **isn't lost** — it becomes the
    name of the page or the database, the table's first cell (without bold
    and colours: cells are plain text), the line under the divider — and
    **you stay in the page** instead of entering the new one, as in Notion.
    From "/" the line is empty and it wasn't needed: there nothing changes
    (`convertToPageLink`/`convertToDatabaseLink` with `title`,
    `convertToDividerAndFocusNext` with `keepText`,
    `convertToTableKeepingText`)
  - **Color**: the brush's window, but the colour — of the text or behind
    the text — goes on **the whole line**, without having to select it
    (`colorWholeBlock`). It starts from the colour the line already has
  - **Duplicate**: the copy is born **right below**, with type, text,
    colours, indent and tick. A toggle is copied **with its children**, a
    table **with its cells**, and subpages or databases inside a toggle are
    copied too, as a page's "Duplicate" does: two links to the same subpage
    wouldn't be a copy (`PageRepository.duplicateBlock`)
- **[New feature]** **[📱 Mobile]** **Long-pressing the link to a page** in
  the text (where the caret doesn't go) opens **the page's** menu: Turn
  into (grey: turning a page into something else isn't built yet),
  **Color** (the name's colour, only in that link, kept in an empty span of
  the block), **Edit icon** (the same Upload / Link / Remove window), **Add
  to favorites** / **Remove from favorites**, **Rename** (a small window
  with the name already all selected; it changes only the title,
  `PageDao.setTitle`, and the row's name if it is a row's page),
  **Duplicate** (the same choice of place as the three dots; "next to the
  original" means **under the tapped link**, even if the page is linked in
  two places), **Move to**, **Move to trash** (with the same confirmation
  as the three dots). You always stay in the page: a notice says where the
  copy ended up when it isn't here. The normal tap opens the page as before
  (`combinedClickable`)
- **[New feature]** **[📱 Mobile]** **For a database**, at the top: **Turn
  into page**, **Turn into simple database**, **Turn into complex
  database**, **Lock database** (switch), **Open as page**; below, the
  page's entries, with Turn into and Color greyed out. You get there from
  the **six dots** that now sit at the start of the tools row of a database
  inside the page — before the view's box, because the name can be hidden
  and the dots must always be there — or long-pressing a link to a
  database
  - **Turn into page** does what the database settings' entry does: the
    block becomes a link and the database opens full screen. If the
    database is **already** a link, in its place there is **Turn into
    database**, which puts **that** link back inside the page
    (`turnPageLinkIntoDatabase`; the three dots' entry puts them all back
    inside)
  - **Turn into simple database**: the row pages are deleted **forever**,
    with what they contain — not to the trash: in a simple database a row
    page would no longer have a row to go back to —; the rows' names and
    properties stay (`turnIntoSimpleDatabase`). The user said they'd be
    careful themselves; anyway, **if at least one row page has something
    inside** (text, a block that isn't an empty line, an icon, a cover) a
    confirmation says **how many there are**. If there is nothing to lose
    it is just done (`countRowPagesWithContent`)
  - **Turn into complex database**: rows become pages again. Each is born
    the first time it is opened, as in every database (`ensureRowPage`),
    and they appear right away in the sidebar
  - The two entries are always there: the one the database already is
    shows greyed out with the tick
- **[Bug fix]** **[📱 Mobile]** *(found reading the code for Duplicate;
  **confirmed by the tests** of request 34)* **Undo and Redo emptied all
  the page's tables.** They put the page back as it was rewriting all the
  blocks from scratch (`replaceAllBlocks`: delete and reinsert), and the
  tables' cells are tied to their block with `ON DELETE CASCADE`, with
  foreign keys on (`PRAGMA foreign_keys = ON` in the code generated by
  Room): deleting the table the database took its text away. It was enough
  to undo a letter typed three lines above. Now the cells are set aside and
  put back, all in a transaction (and `TableCellDao.getCellsForPageOnce`).
  The cells' text stays the current one: Undo doesn't follow it, as
  already written in the code
- **[Bug fix]** **[📱 Mobile]** *(found writing "Turn into")* **A toggle
  with lines inside, changed into another type from the "+"**, left its
  lines hanging on a block that doesn't show them: **they disappeared from
  view** (in the database they were still there). Now they come out right
  below, at the same level and in the same order, as when the toggle is
  removed with backspace — for every transformation: type, page, database,
  table, divider (`PageRepository.unnestChildren`, extracted from
  `unnestAndConvert`)
- **[New feature]** **[📱 Mobile]** The link to a page **follows the page
  over time** (`observePageInfo` instead of `getPageInfo`): before name and
  icon were read when the line appeared and stayed those; with Rename and
  Edit icon from the block menu they must change right away
- **[Project]** **Undo doesn't make the link to a page moved or thrown away
  from the block menu reappear**: Undo's snapshots are all the page's
  blocks, and those taken before still had the link. It is removed from all
  of them (`forgetLinksTo`). *Superseded* in the next entry ("Move to — the
  tree with search"): `forgetLinksTo` was removed, and instead a check made
  at every Undo applies to all roads, not only the block menu
  (`withoutStaleLinks`)
- **[Project]** `SLASH_ENTRIES`, `SlashEntry`, `SlashAction`,
  `SlashCategory` (in `PageEditorScreen.kt`) and `OptionsGroup`,
  `OptionRow` (in `PageOptionsSheet.kt`) from private to `internal`, to use
  them in the new file. `PageRepository.duplicatePage` has one more
  parameter, `besideLinkBlockId`, put **before** `copyImage` so that the
  calls passing the image copy as the last lambda keep working. New texts
  in the eight languages (`EditorStrings.blockOptions`, `turnInto`,
  `color`, `editIcon`, `addToFavorites`, `removeFromFavorites`, `rename`,
  `turnIntoSimpleDatabase`, `turnIntoComplexDatabase`, `lockDatabase`,
  `openAsPage`, `simpleDatabaseLossTitle`, `simpleDatabaseLossText`)

### 🆕 v0.45 — 24/09/2026 · 13:25 — Simple database — rows are only text
*(partly verified on the phone on 24/09/2026: entries 21 and 22)* *(cloud session of 24/09/2026, request 30: built, tests passed, migration 27→28 simulated, **not tested on the phone**)*
- **[New feature]** **[📱 Mobile]** **"Simple database"** in the "/" and
  "+" menus (greyed out until now) creates a **simple database**: a real
  database, with all the views and settings of the others — filters,
  sorting, grouping, properties, Lock view — but in which **rows are only
  text and never become pages**. The user had always wanted it, and no app
  has it. It is born as a table, like "Database - Inline"; the view is
  changed from the settings. Saved in `PageEntity.isSimpleDatabase`
  (**migration 27→28**, 0 for all existing databases: they stay normal)
- **[New feature]** **[📱 Mobile]** **Tapping a row opens its card**
  instead of a page, in all six views (table from the long press, board,
  list, gallery, calendar, timeline): at the top **the name to write**,
  right below **all the properties** editable in place, then Delete and
  the creation and edit dates (`RowActionsSheet` with `simple = true`,
  `SimpleRowNameField`). In the table the name is written by tapping it, as
  always
- **[New feature]** **[📱 Mobile]** Removed everything that in normal
  databases concerns row pages: **no OPEN** in the name cell, **no "Icon"
  entry** in the card, **no "Card preview"** in the gallery (cover and text
  belong to the row's page: the cards have name and properties), and the
  buttons say **"New row"** instead of "New page". At the bottom of the
  settings a line says the database is simple and what that means
- **[New feature]** **[📱 Mobile]** **No road leads to a row page**: the
  ban is in `PageRepository.ensureRowPage`, the only point a row page is
  born from, which refuses for a simple database. And in addition: in the
  **sidebar** the simple database has no children (`treeChildren`,
  `nodeFor`); in **search** a row found leads **to the database** and says
  which one it is in; **Duplicate** doesn't offer its rows as a place to
  copy into
- **[Bug fix]** **[📱 Mobile]** *(found writing this)* The search results
  list used the page's identifier as the key: a row result leading to the
  database, and two rows of the same database, would have had the same
  key, which a `LazyColumn` doesn't forgive — the app closes. Now the key
  is first the row, then the page (`SearchScreen`)
- **[Project]** `RowPropertiesList` was extracted from a row's actions
  window, to use it both in "Edit property" and in the simple row's card.
  New texts in the eight languages (`DbStrings.newRow`, `row`,
  `simpleDatabaseNote`)

### ✏️ v0.44.2 — 24/09/2026 · 13:12 — "+" menu — the same entries as the "/" menu, in the same order
*(partly verified on the phone on 24/09/2026: entry 20)* *(cloud session of 24/09/2026, request 29: built, tests passed, **not tested on the phone**)*
- **[Change]** **[📱 Mobile]** The bar's "+" menu **reads the same
  catalogue as the "/" menu** (`SLASH_ENTRIES`): same entries, same icons,
  **same order**, including the entries not built yet, which are there and
  greyed out. Before the "+" had a list of its own, stuck at the nine
  entries of the early days, while the "/" grew. Now an entry added to the
  catalogue appears in both
- **[Change]** **[📱 Mobile]** **Two columns filled row by row**, as asked:
  the first entry of "/" is the first on the left, the second the first on
  the right, and so on. Today (27 entries, the last row has one):

  | left | right |
  |---|---|
  | Text | Bulleted list |
  | Numbered list | To-do list |
  | Toggle list | Page |
  | *Callout* | *Quote* |
  | Table | Divider |
  | *Link to page* | *Image* |
  | *Video* | *Audio* |
  | *Code* | *File* |
  | *Web bookmark* | Table view |
  | Board view | Gallery view |
  | List view | Calendar view |
  | Timeline view | Database - Inline |
  | *Database - Full page* | Simple database |
  | *Linked view of data source* | |

  (in italic the greyed-out ones; "Simple database" was greyed out until
  request 30). Without the family titles the "/" has (Basic blocks, Media,
  Database): on two columns row by row they would break the order asked,
  leaving holes at the end of each family. The menu scrolls, because the
  rows are more than fit above the keyboard. The icons are the "/" ones
  (before the "+" had its own, with the letters "T", "H1"...)
- **[Project]** **A single function applies the chosen entry**, from both
  menus (`applyInsertAction` in `PageEditorScreen`): they were two almost
  equal copies and were already drifting apart — the "+" always created a
  database as a table, while from "/" "Calendar view" gave a calendar. Now
  from the "+" there are also all the database views (Table, Board,
  Gallery, List, Calendar, Timeline view), which before only "/" had. Every
  entry's behaviour is the one it had in "/"

### ✏️ v0.44.1 — 24/09/2026 · 13:06 — Pages — the size is typed by hand, and the headings removed
*(verified on the phone on 24/09/2026: entries 18 and 19, and the 26→27 migration on real headings)* *(cloud session of 24/09/2026, request 28: built, tests passed, migration 26→27 tried on a test database, **not tested on the phone**)*
- **[Change]** **[📱 Mobile]** **The text size is typed by hand**: tapping
  the number in the Aa bar opens a small window (`FontSizeDialog`) with a
  small box, the current number already selected (typing replaces it) and
  **the number keyboard**. It accepts only digits, two at most; "Done" —
  and the keyboard's Done key — works only between 5 and 72, and under the
  box "From 5 to 72" turns red if you go out. It replaces the first
  version's list of numbers, as asked by the user
- **The window and not a box inside the bar**, on purpose: while typing the
  number the keyboard passes to the box, no line of the page has the caret
  anymore, and the Aa bar — which exists only when a line has it — goes
  away. A box in the bar would disappear halfway through the number; for
  the same reason the window is switched on by a screen state
  (`showFontSizeDialog`), like the brush's, and isn't born inside the bar.
  With the window closed, **the caret doesn't go back by itself** into the
  page: you tap where you want to resume. Putting it back on command would
  mean moving focus while the keyboard switches from numbers to letters,
  the passage that gives the most trouble with the Samsung keyboard
- **[Removed]** **[📱 Mobile]** **Headings — Heading 1, 2 and 3 — from the
  whole app**, at the user's request: with the size chosen per page,
  fixed-size headings were no longer of any use (and the "Toggle Headings"
  they were about to ask for are no longer needed). Removed from the "+"
  menu and the "/" menu, from the block type (`BlockType`: the three values
  no longer exist), from the joined field (`FLOWING_TYPES`,
  `isFlowingTextType`), from its visual transformation, from the islands,
  from the Gallery's preview and from the translated texts. No writing
  shortcut created them
- **[Removed]** **[📱 Mobile]** **Those already written became normal text**
  (**migration 26→27**: `UPDATE blocks SET type = 'PARAGRAPH'`). Among the
  three possibilities proposed — bold text, normal text, leaving them as
  they are — the user chose normal text. The text isn't lost, and the
  formatting put by hand (bold, colours) stays. Tried on a test database:
  three headings with bold and colour become identical paragraphs, the
  other blocks aren't touched, and no type left in the database is unknown
  to the app — which is what matters, because a type name the enum no
  longer knows makes the app close on reading. The tables' schema doesn't
  change
- **"Default"** at the top of the fonts list stays, confirmed by the user

### 🆕 v0.44 — 24/09/2026 · 12:55 — Pages — font and text size (Aa bar)
*(verified on the phone on 24/09/2026: entries 17 and 18; the size entry was empty, fixed — see "Resumed on the PC")* *(cloud session of 24/09/2026, request 27: built, migration 25→26 simulated, **not tested on the phone**)*
- **[New feature]** **[📱 Mobile]** In the Aa bar, **before B**, two
  entries as in OneNote: **the font**, with the name of the one in use
  ("Calibri", or "Default" if the page hasn't chosen one), and **the
  size**, with the number in use ("16"). Tapping them opens the list to
  change them. They apply to **the whole page**, as asked ("automatically
  selecting everything"), and are saved per page (`PageEntity.pageFont` and
  `pageFontSize`, **migration 25→26**; null means "as before", and 16 is
  saved as null). With the page locked the two entries aren't there
- **[New feature]** **[📱 Mobile]** **The fonts, and who really draws
  them.** The ones asked for are commercial and can't be put in the app:
  in their place there are look-alikes with a free licence (SIL Open Font
  License), chosen and approved with the user. **In the bar you read the
  original name; in the list the original name with the look-alike in
  brackets** — the user's choice. The list, in the order asked, with a
  divider between the groups and "Default" at the top to go back to the
  system font:

  | Name | Look-alike | Where it comes from |
  |---|---|---|
  | Helvetica | Inter | inside the app |
  | Garamond | EB Garamond | inside the app |
  | Arial | Arimo (same metrics) | inside the app |
  | Verdana | Noto Sans (the closest) | inside the app |
  | Georgia | Gelasio (same metrics) | inside the app |
  | Calibri | Carlito (same metrics) | inside the app |
  | Futura | Jost | inside the app |
  | Times New Roman | Tinos (same metrics) | inside the app |
  | Cambria | Caladea (same metrics) | inside the app |
  | Consolas | Inconsolata (no real italic) | inside the app |
  | Songti | Noto Serif SC | downloaded |
  | Kaiti | Ma Shan Zheng (no real bold) | downloaded |
  | ~~MS YaHei~~ SimSun *(since 25/09/2026)* | ~~Noto Sans SC~~ Noto Serif SC in the Light weight | downloaded |
  | Mincho | Noto Serif JP | downloaded |
  | Gothic | Noto Sans JP | downloaded |
  | Kaisei | Kaisei Opti (it is the real one, already free) | downloaded |

  **Lishu removed**: a free Lishu doesn't exist, and the user chose to
  remove it instead of replacing it with another calligraphy
- **[New feature]** **[📱 Mobile]** **Where they come from**, the user's
  choice ("Mixed"): the ten Western ones are **inside the app** — 38 TTF
  files in `app/src/main/res/font` (regular, bold, italic, bold italic),
  about 12 MB, downloaded from Google Fonts on 24/09/2026 — and always
  work, even offline. The six Chinese and Japanese ones weigh 10-20 MB each
  and **are downloaded from Google Play Services** the first time a page
  uses them, then they stay on the phone. The debug APK went from 17.8 to
  23.9 MB
- **[New feature]** **[📱 Mobile]** **The size scales everything in
  proportion**, not just normal text (`PageTypography`,
  `ui/theme/PageFonts.kt`): headings H1-H3 (28/22/18 at size 16; removed
  shortly after, request 28), the line height of the joined field (24, or
  27 with checkboxes), the list marks. At 16 every measure stays exactly
  the previous one. The measures go through a `CompositionLocal`
  (`LocalPageTypography`) and reach the joined field (from its
  `textStyle`, the only place where the README says measures can be
  touched without risks), its visual transformation (only `fontSize` in
  the styles, no extra characters: the translation of positions doesn't
  change), the islands, toggles, table cells and links to pages. The page
  title takes **only the font**. The checkboxes' little squares and the
  toggles' arrows stay fixed, and so do the databases inside the page
- **[New feature]** **[📱 Mobile]** **The lists don't close the keyboard**
  (`PopupProperties(focusable = false)`): a menu that takes focus closes
  the keyboard, with it the bar goes away — it rests on it — and with the
  bar the entry the menu started from. So you choose and keep writing.
  ~~The size is chosen from a list with all the numbers from 5 to 72~~ —
  **superseded by request 28**: the size is typed by hand in a small
  window, see the entry above
- **[New feature]** **[📱 Mobile]** In the fonts list the Western names are
  written **each in its own font**, as in Word's menus; the Chinese and
  Japanese ones aren't, so as not to download them all just by opening the
  menu
- **[Project]** New dependency `androidx.compose.ui:ui-text-google-fonts`
  (version from Compose's BOM). The font provider's certificates are in
  `res/values/font_certs.xml`, copied from the official Android
  "DownloadableFonts" sample (Apache 2.0 licence). The copyright notices
  and the licence text of the ten included fonts are in
  `app/src/main/assets/font_licenses/OFL.txt`, as the licence asks. New
  texts in the eight languages (`EditorStrings.defaultFont`, `font`,
  `fontSize`). First use of `R` in the code (`R.font`, `R.array`)

### 🆕 v0.43 — 24/09/2026 · 12:32 — Pages — the text count ("X words")
*(verified on the phone on 24/09/2026: entry 16)* *(cloud session of 24/09/2026, request 25: built and with the automatic tests passed, **not tested on the phone**)*
- **[New feature]** **[📱 Mobile]** At the bottom of a page's three-dots
  menu, after "Updates", there is **"X words"** (with the number in the
  format chosen in the settings: 2.652 or 2,652). **Tapping it opens right
  below** the complete list, and tapping it again closes it. The user let
  me choose between a separate window and opening in place: the second was
  chosen, because a window over the menu, on a phone, covers exactly the
  menu you come from, and closing it needs one more tap. To be confirmed by
  the user
- **[New feature]** **[📱 Mobile]** The entries, in the order asked:
  **Words, Letters, Numbers, Letters and numbers, Characters (no spaces),
  Characters (with spaces), Lines, Japanese characters, Chinese
  characters, Korean characters** (in Italian: Parole, Lettere, Numeri,
  Lettere e numeri, Caratteri senza e con spazi, Righe, Caratteri
  giapponesi, Caratteri cinesi, Lettere coreane — the user's word). All ten
  always, even at zero, as in Word
- **[New feature]** **[📱 Mobile]** *(request 26)* **"Letters and
  numbers"** is an entry of its own, wanted by the user after seeing how
  characters are counted: the sum of letters and digits and **nothing
  else** (`TextStats.lettersAndDigits`), while **characters count
  everything** — letters, numbers and every other sign: brackets, dots,
  commas, asterisks... So "hin(...) = 3x!" makes 5 letters and numbers and
  12 characters without spaces. The two entries on characters (without and
  with spaces) stay as in the original request
- **[New feature]** **[📱 Mobile]** **The rules are those of Word's "Word
  Count" window**, which the user showed as the model (`data/TextStats.kt`,
  `textStatsOf`):
  - a **word** is what is between two spaces, even "=" or "[]"; in
    Chinese and Japanese **every character is a word**, because those
    languages don't separate words with spaces; Korean does, and is counted
    like Italian;
  - **letters** are those of the alphabets (Latin with accents, Greek,
    Cyrillic...), without Chinese, Japanese and Korean, which have their
    own entries; **numbers** are the digits, one by one ("2026" makes 4),
    because the user described them as a part of the characters;
  - **characters** are all the written signs, punctuation and symbols
    included, as in Word. The user had described them as "letters and
    numbers together", but that way they would have been just the sum of
    the two entries above: **a choice to be confirmed**. With spaces the
    spaces are counted too (including the "non-breaking" one and the wide
    one of Asian languages), not the line breaks;
  - **lines** = blocks with some text, as the user asked ("actually how
    many blocks"): empty blocks don't count, a table counts once;
  - **Japanese or Chinese**: the ideograms are the same, so the line is
    looked at — if it has at least one hiragana or katakana, its ideograms
    are Japanese, otherwise Chinese. Katakana's long dash (ー) counts as
    Japanese;
  - counting is by **code points** and not by `Char`, so an emoji or a rare
    ideogram is a single character and not two
- **[New feature]** **[📱 Mobile]** **What is counted**
  (`PageRepository.textStats`): the text of paragraphs, headings, lists,
  checkboxes and toggles, **including that inside closed toggles**, and the
  cells of simple tables (only the visible ones: with a table shrunk, the
  cells left out don't count). **Not counted**, as asked, databases and
  everything the user didn't write there: database rows and cells, the
  names of linked pages, and **the page title** — often set by the app
  ("Senza titolo", "Untitled"). To be confirmed by the user
- **[New feature]** **[📱 Mobile]** For a **database** opened full screen
  the entry isn't there: its content is rows, and rows aren't counted. The
  three-dots menu now **scrolls**, because with the count open the entries
  are more than fit on a short screen
- **[Project]** **The project's first automatic tests**:
  `app/src/test/java/com/gabriele/notionlocal/data/TextStatsTest.kt`,
  sixteen cases — Italian with accents, vocabulary lists with "[]" and "=",
  digits, letters and numbers separated by symbols, Japanese with and
  without kana, katakana with the long dash, Chinese, Korean, mixed
  languages, special spaces, emoji, line breaks, empty text. They run
  without a phone: `gradle testDebugUnitTest`

### 🆕 v0.42 — 24/09/2026 · 12:13 — Columns — moving them by long-pressing the header
*(partly verified on the phone on 24/09/2026: entry 15)* *(found already written in the code arrived from the PC on 24/09/2026, but missing in this README: documented in the cloud session of the same day. **To be verified on the phone**: there is no sign it was ever tested)*
- **[New feature]** **[📱 Mobile]** Long-pressing a table column's header,
  the menu has in order: **Move to left**, **Move to right**, **Edit
  property**, *Center / Align left* (only for checkbox columns) and
  **Hide**. It is the user's request, which in the cloud session they
  repeated word for word (Log, request 20) because they had just asked
  Code on the PC for it
- **[New feature]** **[📱 Mobile]** *(cloud session of 24/09/2026, request
  22)* **The two entries to move went to the top of the menu**, above Edit
  property. In the code arrived from the PC they were below, as the first
  request said; the user, never having seen them on the phone (the
  installed app was older than the code), asked to put them on top. Moving
  is the thing a header is long-pressed for — to edit it a tap is enough —
  and at the top it is found without reading the menu. Only the order of
  the entries in `PropertyHeaderCell` changed
- **[Removed]** **[📱 Mobile]** *(cloud session of 24/09/2026, request 23)*
  **"Move left" and "Move right" from the property window** (the Edit
  property one, `PropertySheet`): only "Delete property" stays there. They
  were a duplicate of the header menu, and moreover they appeared one at a
  time depending on the column — on the first after Name there was only
  "Move right", and that is the one the user asked to remove; both removed,
  because "Move left" is the same thing on the other columns. The
  `canMoveLeft`, `canMoveRight` and `onMove` parameters of `PropertySheet`
  removed too. **Consequence**: a hidden column can no longer be moved
  without showing it again (see Known limits). The branch of
  `DatabaseTableState.neighbourForMove` that moved hidden columns among all
  stays in the code but today nobody reaches it
- **[New feature]** **[📱 Mobile]** **"Move to left" is grey on the first
  column after Name**, whatever it is: Name is the pages' title, not a
  property, and always stays first. **"Move to right" is grey on the
  last**, and with **"Lock view"** both are. Grey and not hidden: an entry
  that disappears and reappears would make the others change place under
  the finger (`PropertyHeaderCell`, `canMoveLeft = !viewLocked && index >
  0`)
- **[New feature]** **[📱 Mobile]** It counts **among the visible
  columns** (`DatabaseTableState.neighbourForMove`): swapping with a hidden
  column in between wouldn't move anything visible. The move renumbers all
  columns in a single save (`DatabaseViewModel.moveColumn` →
  `DatabaseRepository.updateColumns`, one transaction), so the table
  redraws once. With the table grouped the header repeats above every
  group, with the same menu. The rules are the same as Move left / Move
  right in the property window

### 🆕 v0.41 — 24/09/2026 · 11:50 — Databases — Gallery view
*(partly verified on the phone on 24/09/2026: entries 3-14; covers, rotation, smoothness missing)* *(cloud session of 24/09/2026: **built** in the cloud — `assembleDebug` succeeded, queries checked by Room, migration simulated on SQLite, see Log — **but NOT tested on the phone**)*
- **[New feature]** **[📱 Mobile]** **Gallery**, the sixth view: the
  database's pages as **cards in a grid**. At the top the preview, below
  the name (with the page's icon, if there is one) and the properties that
  show and have a value, one per line as on the board's cards. A tap opens
  the page, the long press the row's actions, as in the other views. Filter
  and sorting apply here too. It is chosen from Settings → Layout (sixth
  box, grid icon) or created from the "/" menu with **"Gallery view"**,
  which until now was there but off
- **[New feature]** **[📱 Mobile]** **Card preview**, in the view's
  settings, with the Notion entries that can be done here: **None**,
  **Page cover**, **Page content**. Saved per database
  (`PageEntity.galleryCardPreview`, **migration 24→25**). Until one is
  chosen **Page cover** applies (`GALLERY_DEFAULT_PREVIEW`): the gallery
  was waiting for covers precisely because it is the view recognised by
  images. Notion's fourth entry, the preview from a "Files & media"
  property, is missing because that property type doesn't exist in the
  app
- **[New feature]** **[📱 Mobile]** **Card size**: **Small / Medium /
  Large**, saved per database (`PageEntity.galleryCardSize`, same
  migration), medium until one is chosen. Cards don't have a fixed width
  but **a minimum** (100, 150 and 280 points): in a row as many fit as can,
  and they widen together to fill it. On a phone held upright there are
  three, two or one per row; turning it more fit instead of becoming huge
  (`galleryColumnsFor`)
- **[New feature]** **[📱 Mobile]** With **Page cover** the card shows the
  page's cover **with the framing chosen in "Reposition"**. The card has a
  different shape from the page's strip, and the same framing could have
  brought the image past the edge and uncovered an empty corner:
  `CoverImage` (now `internal`, to use it from here) has a new parameter,
  `clampOffset`, which stops the move where the image stops covering. In
  the page it stays off and nothing changes there
- **[New feature]** **[📱 Mobile]** With **Page content** the card shows
  **the beginning of the page's text**, small (8, 10 or 12 points
  depending on the card's size), written as in the page: headings larger
  and bold, lists with **the same mark as the page** (`bulletMarkerFor` and
  `numberedMarkerFor`, now `internal`), numbered counting like
  `numberedListOrdinal` — a counter per level, `numberStartsAt` respected
  — checkboxes with ☐ and ☑ (the ticked ones grey and struck through),
  toggles with ▸. At most 12 top-level blocks per page are read
  (`GALLERY_PREVIEW_MAX_LINES`), skipping those without text (tables,
  dividers, links) and the empty lines at the head and tail; what doesn't
  fit is cut by the card's edge
- **[New feature]** **[📱 Mobile]** **Spoilers stay covered in the
  preview**: every covered letter becomes `▒`, spaces stay. Without it, a
  text hidden in the page would have been read in clear on the card —
  which is exactly the glance the spoiler protects from
- **[New feature]** **[📱 Mobile]** The cards of the same row are **equally
  tall** (`IntrinsicSize.Max`), and the last row, if not full, keeps the
  cards as wide as the others. The preview takes its place even when empty
  (page without cover or without text), so the cards stay aligned
- **[New feature]** **[📱 Mobile]** "Card preview" and "Card size" appear
  in the settings **only with the gallery**, right under Filter like the
  other views' entries, and **"Lock view" turns them off**. With an entry
  chosen the window stays open and the ✓ moves, as for "Date property" and
  "Group by": the gallery behind changes right away
- **[New feature]** **[📱 Mobile]** New texts in the app's eight languages
  (`DbStrings.layoutName`, `galleryPreviewName`, `gallerySizeName`,
  `cardPreview`, `cardSize`). In Italian the size is feminine because it
  refers to the *card* ("scheda")
- **[Project]** Covers and pages' text are read **only while the gallery
  shows them** (`DatabaseViewModel.load`, `flatMapLatest` on the page). The
  text above all: the query looks at the blocks table, which changes at
  every key pressed in **any** page, and a database's ViewModel stays alive
  even when it is left behind in the navigation stack. Following it always
  would have meant redoing the query at every letter written elsewhere. Two
  new queries in `DatabaseDao`: `observeRowCovers` and
  `observeRowPreviewBlocks`; cutting to the first blocks is done by
  `DatabaseRepository.observeRowContentPreviews`, because in SQL it would
  require functions that on Android arrive only from version 11
- **Choices to be confirmed by the user**: the cover as the default
  preview, the cards' minimum widths and the 1.6 : 1 ratio of the top part
  (`GALLERY_PREVIEW_RATIO`)

### 🆕 v0.40 — 23/09/2026 · 15:41 — Duplicate: you choose where the copy goes, and the copy is really a copy
*(Claude Code on the PC)*

- **[New feature]** **[📱 Mobile]** "Duplicate" in the three-dots menu (pages and
  full-screen databases) opens a window that asks **where to put the
  copy**, with three choices: **Next to the original** (right below the
  original; for a row's page it means **a new row of the same
  database**, right below, with all its properties), **End of the main
  menu** (at the bottom of the main page) and **Inside a page**
  (`DuplicateSheet.kt`)
- **Inside a page** opens the sidebar's tree: you tap the page to put it
  in and the copy ends up at the bottom. Databases open like folders and
  show their rows, which can all be chosen, **even those never opened**
  (their page is born at that moment, `DuplicateTarget.IntoRow`).
  Databases themselves can't be chosen: inside they have rows, not
  blocks. At the top there is a search by title, and under each result
  you read where the page is ("In A", "In Untitled"): after a Duplicate
  there are always two identical names. Pages that aren't anywhere don't
  appear, because a copy put inside them would vanish with them
- Once made, the copy **opens**, and if it isn't next to the original a
  notice says where it ended up ("Copy placed in “B”"). The title gets
  " (copy)" in the app's language; the pages inside it keep their name
- **[Bug fix]** **[📱 Mobile]** The copy **wasn't a copy**. It pointed to the same
  subpages and, for a database, to the same row pages: writing in a
  subpage of the copy changed the original too, renaming a row's page
  renamed the row in both databases, throwing one away removed it from
  both. Now **everything is copied, all the way down**, as on Notion
  (`PageRepository.copyPageDeep`); a page referenced twice is copied only
  once, and a backward link doesn't make the copy loop forever
- **[Bug fix]** **[📱 Mobile]** Duplicating **a row's page** created a page that
  wasn't anywhere: there was no link to put next to it, and the copy
  could only be found by searching for it. In the phone's database one
  was left like that, **"A (copia)"**, from an earlier test. Now next to
  the original it becomes a new row, and elsewhere a normal page
- **[Bug fix]** **[📱 Mobile]** The copy's icon and cover **shared the file** with
  the original: changing the image of one, or deleting it forever, the
  file vanished and the other was left without. Now each copy has its own
  file (`PageImageStore.copy`)
- **[Bug fix]** **[📱 Mobile]** The copy lost the text of **simple tables** (it
  lives in a separate table, per block) and, in a database, still pointed
  to the original's columns for grouping, calendar, sorting and filter,
  which therefore vanished. Toggles' children are saved after their
  toggle, as the constraint on the parent requires
- **[Bug fix]** **[📱 Mobile]** The text written in a **simple table vanished
  when reopening the page** (in the database it was still there): each
  box kept its text in a state born with the first value seen, and at the
  first drawing the cells hadn't arrived yet. Now the boxes are reborn
  once when they arrive (`TableBlockContent`)
- **Tried on the phone** with a test page (toggle with two children,
  2×2 table, subpage with cover, icon): copy of row A next to the
  original, copy inside row B never opened chosen from the search, copy
  of the whole database at the bottom of the main menu. In the database:
  no page referenced from two places, no shared image file, toggles'
  children in the right page, cells copied, `PRAGMA foreign_key_check`
  empty, no crash. The text in the table stays after leaving and coming
  back. Then the phone's database went back to how it was before the
  tests

### 🆕 v0.39 — 23/09/2026 · 15:06 — Icons for database pages
*(Claude Code on the PC)*

- **[New feature]** **[📱 Mobile]** The pages contained in a database (the rows)
  have their icon, and **it shows in the database next to the name**: in
  the table, in the list, in the board's cards, in the calendar's and
  timeline's bars, and among the rows without a date. A row without an
  icon starts from the margin, without a placeholder
- **It is set and removed in two ways**: long-pressing the row's name,
  from the **Icon** entry at the top of the actions (it says "Change" or
  "None"), or inside the page, tapping the icon. It is the same window as
  pages and databases: Upload, Link, Remove. If the row had never been
  opened as a page, the page is born at that moment: the icon lives in
  the page. With the database locked or in the trash the entry is off
- **A row's page is born without an icon**, as on Notion: in place of the
  fixed 📄 sheet there is **"Add icon"** next to "Add cover". Before,
  "Remove" removed the image but left the 📄, so a removed icon couldn't
  really be removed. The other pages keep their emoji
- The icons come from a query that follows rows and pages together
  (`DatabaseDao.observeRowIcons`): changed inside the page, the icon
  updates in the database without reopening it. The save writes only the
  icon's column (`PageDao.setIconImage`), and the previous image is
  removed from the folder
- **Tried on the phone** on row "A" of the table, both ways: set from
  the row's menu and removed inside the page, then set from "Add icon"
  and removed from the row's menu. Each time the icon appears and
  disappears both in the table and at the head of the page, the menu
  switches from "None" to "Change", the image file is born in
  `page-images` and is deleted on Remove, and in the database `iconImage`
  goes back to empty. No crash. The other views (board, list, calendar,
  timeline) use the same piece but I didn't open them

### 🆕 v0.38 — 23/09/2026 · 14:14 — The sidebar: favorites, tree, search, trash, startup and settings
*(Claude Code on the PC)*

- **[New feature]** **[📱 Mobile]** **Sidebar** that slides in from the left over the
  page, opened from the button with the three lines: same semi-transparent
  grey circle as the three dots, floating over the cover, and next to
  back in subpages (`FloatingPageButton`). In the full-screen database it
  sits in the top bar, next to back. It closes by tapping outside, with
  the back gesture or by dragging it; **it doesn't open by dragging from
  the edge** because the phone's back gesture starts from there. It is a
  touch darker than pages (`SidebarBackground`)
- **The order of the entries is the one asked for**: Widgets (title and
  space, to be filled — filled on 24/09/2026, see "Widgets in the
  sidebar"), Notebook greyed out in a band of another colour
  (`NotebookBand`), Main menu with the little house and a divider below,
  Favorite pages, the pages' tree, Search, Startup window, Backup and
  Notifications greyed out, Trash, Import/Export/Connections greyed out,
  Settings. All the sidebar's entries are translated (see below)
- **[New feature]** **[📱 Mobile]** **The pages' tree**, like Notion and File
  Explorer: it starts from the main menu (open), every page has its icon
  and the little triangle **only if there is something inside**. Tapping
  the triangle opens and closes it; tapping the name goes **straight to
  the page**, without passing through those that contain it, and below
  only the main menu stays, so "back" brings you there. The open page is
  highlighted
- The tree **doesn't live in a "parent" column**: it lives in the links
  inside pages, in the order of the text (inside toggles too), and for
  databases in the rows. Rows never opened as a page are there anyway:
  their page is born at the first tap, as with the OPEN button. One
  branch at a time is loaded, and it reloads at every opening of the
  sidebar. A backward link (a page that references one of its ancestors)
  is shown but doesn't reopen, otherwise the line would never end
- **[New feature]** **[📱 Mobile]** **Favorite pages**: the list, one below the
  other, with the date and time they were added. It is sorted by date
  added or by name, ascending or descending, and the choice stays saved.
  New column `favoritedAt` (**migration 23→24**); the favorites already
  there don't have a real date and say so ("Date added unknown") instead
  of inventing it. Pages in the trash don't appear among the favorites
- **[New feature]** **[📱 Mobile]** **Search** redone: it searches the titles and,
  with "Search in content" (on by default), the blocks' text, simple
  tables and the names of database rows. Each result shows **the piece of
  sentence with the word highlighted**. "Search in trash" includes thrown
  away pages, marked "In trash". The other eight options are there,
  greyed out; "Search in titles" is grey **but on**, because titles are
  always searched
- **[New feature]** **[📱 Mobile]** **Trash**: the thrown away pages, with date
  and days left. When opened, a page of the trash is **read-only** and
  has at the top a bar with **Restore** — it goes back **to the bottom of
  the main menu**, or to its row if it was a row's page — and **Delete
  permanently**, with confirmation. At the top right "Empty trash". Two
  rules: **keep pages 30 days**, or **empty automatically**, that is
  delete them at the next start of the app. The cleanup happens at
  startup
- Permanent deletion also takes away **the contained pages** (the links
  inside, the rows if it is a database, and so on down), but **only those
  referenced from there alone**: a subpage also linked from somewhere
  else stays. With the pages go the files of icons and covers
- The "Move to trash" confirmation now says the page goes to the trash
  and **when it will go away** according to the chosen rule (before it
  said there was no trash)
- **[New feature]** **[📱 Mobile]** **Startup window**: the app starts from the
  last page opened, from the main menu or from a page chosen from a list.
  If that page no longer exists or is in the trash it starts from the
  main menu. A rotation or a theme change don't redo the jump
- **[New feature]** **[📱 Mobile]** **Settings**, saved on the phone
  (`AppSettings`, `SharedPreferences`, not in the database: they are
  choices of the phone, not content):
  - **Font** and **Font size** greyed out
  - **Theme**: Dark, Light, System. The greys written by hand in about
    150 places now **choose by themselves the light or dark version**
    (`Color.kt`): the names stayed the same, and in light mode
    `NotionWhite` is the text colour. The icons of the phone's bar turn
    dark on the light theme
  - **App sounds**: notification sound chosen **with the phone's file
    explorer** (the permission to read it stays even after a restart),
    or the default one, with a button to listen to it; notifications off
    forever, or for 1 hour / 8 hours / 1 day / 1 week. **Notifications
    don't exist yet**: the choices are saved and will apply when they
    exist, and the page says so
  - **Animations**: On, Reduced, Off. They apply to the opening of the
    sidebar and to the fades between one screen and another (full 700 ms
    as before, reduced 120, none)
  - **Number format**: 1.000.000 or 1,000,000. Number cells show it when
    you aren't writing in them; **while writing the number stays raw**,
    otherwise the separators would move under the fingers. The saved
    value never changes
  - **Date format**: the nine formats asked for. "Full date" is written by
    each language its own way ("September 23, 2026", "23 settembre
    2026"). It applies to database cells, the history, favorites and
    trash
  - **Language**: English, Italiano, Deutsch, Français, Español, 中文,
    한국어, 日本語, each written in its own language. It translates **the
    app's entries**: sidebar, "/" and "+" menus, toolbar, ⋯ menu,
    databases (property types, views, filters, sorting, tags, repeating
    dates, settings), colour picker, history, confirmation windows. In
    the "/" menu you can search both with the translated name and with
    the English one
  - **Time zone**: Berlin by default, which follows daylight saving
    time; you search among all the "Region/City" zones, with the current
    offset from UTC
- **The translations** live in `ui/i18n/` (`Strings`, `EditorStrings`,
  `DbStrings`): **every entry with its eight translations on the same
  line**, so a new entry is written only once and a forgotten language
  doesn't compile. **Property names**: a property left without a name
  takes the English one of its type ("Number", "Date") and **is shown
  translated**; a name written by the user stays as it is. New properties
  without a name keep saving the English name, so they always follow the
  language
- **Declared limits**: the words of numbered lists "in words" ("One",
  "First") stay English, they are a numbering style; the errors of image
  downloads are now generic (before they were in Italian and detailed);
  **dates without time** are saved at midnight of the zone they were born
  in, so choosing a zone far west of Berlin the old ones could appear a
  day earlier; the animations inside pages (scrolling, windows) don't
  follow the setting yet; pages that **no link references** don't appear
  in the tree (on the phone there are two, an "Esempio" and an
  "Untitled": they can be found with the search)
- **Verified on the phone**: sidebar opened and closed; tree with the
  database's rows, tap on a row → page opened directly; favorite added →
  in the list with date and time; search → page found by content with the
  word highlighted, options panel; page thrown away → in the trash with
  "30 days left", opened with the bar, Restore; Italian → settings,
  sidebar, "/" menu and database headers translated; light theme on page,
  sidebar and settings; numbers with the thousands separator; "Last
  visited page" → app restarted, it starts again from the database opened
  last. No crash. Tests cleaned up and settings put back as the user had
  them

### ✏️ v0.37.1 — 23/09/2026 · 09:28 — The "/" menu really filters
*(Claude Code on the PC)*

- **[Bug fix]** **[📱 Mobile]** **Typing "/d" the menu vanished at the "d".** The
  "/" menu closed at every tap outside itself, and for Android **the
  keyboard is "outside"** too: it is another window. So the first letter
  typed after the slash closed it, and the filter was never seen working.
  In the tests it had slipped by because slash and letters came from
  `adb input text`, which doesn't touch the screen; typing the "d" on the
  real key the menu vanished right away. Removed the closing on outside
  tap (`dismissOnClickOutside = false`), as for the "+" menu: it closes
  anyway choosing an entry, removing the slash, bringing the caret before
  it or onto another line, with a space, or losing focus
- **[Change]** **[📱 Mobile]** **With a filter, the entries that look most alike
  first.** The filter kept every entry that *contained* the letters, in
  the catalogue's order: with "/d" there were sixteen, and "Divider" came
  seventh, below the edge of the box. Now whoever **starts** that way
  wins, then whoever has a **word** that starts that way ("/list" finds
  "Bulleted list", "/do" finds "To-do list"), and only after whoever
  contains them; when equal, the entries that work before those still
  off. With a filter the families (Basic blocks, Media, Database) are no
  longer separated, otherwise with "/data" the basic entries would come
  first anyway. At every letter the list starts again from the top
- **[Bug fix]** **[📱 Mobile]** **Choosing Divider the page slid down to the
  database and the keyboard hinted at closing.** Every group of text
  lines is a single box, recognised by its first line: if the first line
  becomes a divider, the box changes identity and is destroyed with the
  caret inside. For a moment the focus belonged to nobody — in the video:
  bar gone, keyboard going down and up again, page scrolling. Now, as
  already for the toggle, the focus first lands on the invisible field
  and from there passes to the new line below the divider. It applies
  from the "+" too
- **[Bug fix]** **[📱 Mobile]** **The bar stays while the caret changes line.** In
  the passages through the invisible field (divider, toggle, box going
  back to text) no line has focus for about a hundred milliseconds, and
  the bar vanished and came back. Now, focus lost with the keyboard open,
  it waits three tenths of a second before leaving
  (`BAR_HANDOFF_GRACE_MS`); with the keyboard closed it leaves right away
  as before. The price: passing to the page title the bar stays a moment
  longer
- **[Bug fix]** **[📱 Mobile]** **The divider carried the command along.** It was
  saved whole from the copy of the line the screen had in hand, which
  still had "/d" inside: invisible, but in the database. Same defect as
  the "/" in toggles, same remedy: only the type is written
  (`turnIntoDividerWithLineBelow`)
- **Verified on the phone** typing on the real keys: "/" then "d" → the
  menu stays open, **Divider at the top**; with the capital "D" the
  keyboard puts by itself at the start of a line, same. Tapped Divider on
  a line at the bottom of the screen, recording: bar and keyboard still
  in all 36 frames, caret on the new line, no keyboard closing in the
  log, divider **without text** in the database. After the tests the page
  was put back as it was from the copy made before
- **To be decided, not touched**: choosing Divider on a line that already
  has text, **the whole line** becomes a divider and the text stays
  hidden inside. On Notion the divider goes below instead and the text
  stays

### 🔧 v0.37.0.1 — 23/09/2026 · 09:12 — The jerks inside toggles
*(Claude Code on the PC)*

- **[Bug fix]** **[📱 Mobile]** **Pressing Enter inside a toggle the line just
  written vanished for a moment and the caret dropped suddenly.** The
  lines inside a toggle are separate fields, and Enter puts the new line
  **above** with the text before the caret (it is what keeps the keyboard
  still): the field with the caret switched right away to the text after
  the caret, but the line above arrived only when the database had
  written it and sent it back. Measured: 29 ms, and in the screen video
  there is a frame where the letter just typed **isn't anywhere
  anymore**, and in the next one it reappears one line higher
- Now the new line enters the list in memory **at the same instant** the
  field changes text (`showBeforeSaved`), and the database confirms it
  afterwards. Video redone: from "CB" to "C / B" in one frame, with no
  intermediate passages
- **Adding it once isn't enough**: a database read started a moment
  before Enter can arrive afterwards, without the line, and remove it
  again — and if that line already has the caret, removing its writing
  box makes the app close. The change therefore stays pending and **is
  reapplied at every read that doesn't contain the new line yet**; the
  first that contains it closes it. If the database gives up writing it
  (the block meanwhile is gone) the line is removed, and undo/redo throw
  away the pending ones
- Same treatment for Enter in the toggle's **title** and for the *Empty
  toggle* placeholder: the new line is seen right away. The caret instead
  gets there **as before**, after the database has written it: sending it
  there earlier would mean writing letters in a line that doesn't exist
  in the database yet, and losing them
- **[Bug fix]** **[📱 Mobile]** **Going up with backspace from an empty line the
  bar above the keyboard vanished and came back.** Focus passed through
  the invisible field, the line was deleted and 60 ms later the caret went
  to the one above: for all that time no line had focus, and the bar is
  seen only if there is one. In the video: **four frames, a hundred
  milliseconds, without bar**, with the keyboard always open
- Now the caret passes **directly** from this line to the one above (in
  the log: one loses focus and the other takes it in the same
  millisecond), and the line is deleted **afterwards**, when it no longer
  has the caret. The caret goes to the end of the line above: it is
  placed before taking focus, otherwise you would see it jump
- The Samsung keyboard sends the same backspace **two or three times**
  two milliseconds apart (with the real keyboard, not just with `adb`):
  the outgoing line is remembered, and the duplicates don't remove it a
  second time
- **[Bug fix] of mine, caught before delivering**: in the first round the
  empty line **was never deleted**. `withTimeoutOrNull` returns `null`
  both when the time runs out and when the block returns `null` — and
  that is exactly what the focus request just served becomes. Seen
  rereading the database after the test, not from the screen, where the
  caret went up correctly
- **Verified on the phone**, recording the screen and looking at the
  frames one by one: Enter in the middle of a line inside the toggle → no
  frame with the line missing; backspace on an empty line → bar present
  in all frames, line gone from the database, letter typed afterwards
  ended up at the end of the line above; Enter in the title → new line
  visible right away, backspace → line removed, caret at the end of the
  title. After the tests the page was put back as it was from the copy of
  the database made before

### 🆕 v0.37 — 23/09/2026 · 00:49 — Icons on databases too
*(Claude Code on the PC)*

- **[New feature]** **[📱 Mobile]** A database can have its icon, before the
  name: smaller when it sits inside a page, as large as the pages' one
  when it is opened full screen. To set it there is the **Icon** entry in
  the database's settings; when there is one, it can also be changed or
  removed by tapping it
- **Same window as pages** (Upload / Link / Remove), not a copy:
  `PageImageSheet` and `PageImage` were made shareable between the two
  screens. Removing or replacing the icon, the old file is deleted as for
  pages
- **"No icon" really means nothing next to the name.** Pages have a
  fallback emoji (📄) written in the `icon` field; for a database that
  one isn't used, otherwise every database would have had a sheet in
  front without anybody asking for it. Only `iconImage` counts, and
  without an icon the name starts from the margin as before
- The **link** to a database turned into a page shows its icon, if there
  is one; if there isn't, the table sign instead of the 📄
- With the page locked ("Lock page") the icon can't be changed: it is
  part of the content
- **Verified on the phone** with a copy of an image already there (so as
  not to touch the cover): icon visible next to the name, tapped → the
  window also offers Remove, Remove → icon gone and **the copy's file
  deleted**, cover intact

### ✏️ v0.36.1 — 23/09/2026 · 00:37 — The cover rises up to just below the phone's bar
*(Claude Code on the PC)*

- **[Change]** **[📱 Mobile]** Removed the top bar of pages: it took up an
  empty band between the clock and the cover just to host two icons. Now
  the cover starts **right below the last pixel of the phone's bar**, and
  back and three dots sit **floating above** in a small semi-transparent
  grey circle (`FloatingPageButton`, `FloatingButtonBackground`)
- **Semi-transparent and not solid**, on purpose: solid would be a patch
  on the cover, fully transparent it would vanish on light covers. This
  way it reads on any image and lets it show through
- **Without a cover** the buttons' band stays at the top
  (`FLOATING_BAR_CLEARANCE`), otherwise the back button would end up over
  the page's icon and over "Add cover"
- Each of the two buttons is **only as large as its circle**: around it
  the page responds to taps as before
- The database opened full screen still has the classic bar: there's no
  cover to raise there

### 🆕 v0.36 — 23/09/2026 · 00:29 — Turn into page / Turn into database
*(Claude Code on the PC)*

- **[New feature]** **[📱 Mobile]** In the settings of a database inside a
  page there is **Turn into page**: the database stops being shown there
  and becomes a link (📄 with its name) that opens it full screen. Once
  the entry is tapped, the database **opens right away** full screen,
  which is how it will be seen from then on
- **[New feature]** **[📱 Mobile]** In the three-dots menu of the full-screen
  database there is **Turn into database**: it goes back to being seen
  inside the page that references it, and you **go back** to that page,
  where you find it open as before
- **The database isn't touched**, in either direction: rows, columns,
  view, sorting and filter stay identical. Only **the block that shows
  it** in the page changes — `DATABASE_LINK` becomes `PAGE_LINK` and vice
  versa — written with a single query on the type column, without
  rewriting anything else
- **Turn into database appears only when needed**: if the database is
  referenced as a page somewhere. Opened full screen from the ↗ icon it
  is still inside its page, and there the entry isn't there. It is
  checked again at every opening of the menu, because meanwhile it may
  have been changed from the other screen
- **[Bug fix]** **[📱 Mobile]** A page link pointing to a database opens it **as
  a database**. Before only the block's type counted, and a page link
  always opened the text editor: on a database it would have shown an
  empty sheet instead of the rows
- **Verified on the phone** on the main page's database: Turn into page
  → opened full screen; back → in the page it is a link; tapped → it
  opens as a database; Turn into database → back in the main page, open,
  with the same rows; and opened from the ↗ icon the entry doesn't
  appear

### ✏️ v0.35.2 — 23/09/2026 · 00:18 — Toggles, redone
*(Claude Code on the PC)*

- **[Bug fix]** **[📱 Mobile]** **Opening or closing a toggle deleted everything
  it contained.** The blocks' save used `@Insert(onConflict = REPLACE)`,
  and in SQLite `REPLACE` doesn't update: **it deletes the row and
  inserts a new one**. The deletion triggers the cascade constraint
  towards the children (`parentBlockId`), so every save of a block with
  children destroyed them. Opening or closing a toggle saves it; so does
  moving it up or down and changing its type. Seen in the database: five
  rows inside a toggle, a tap on the arrow, zero rows. Now it is
  `@Upsert`, which inserts if the block isn't there and **updates in
  place** if it is: the row is never deleted and the children stay. The
  project's other `REPLACE`s were checked: they always create rows with
  new ids, so they never touch an existing row
- **[Bug fix]** **[📱 Mobile]** **The "/" that stayed inside every toggle created
  from the "/" menu.** You write `/toggle`, the field removes command and
  slash and saves; right after, the type change saved **the whole block**
  with the copy the UI had in hand before — and rewrote the old text,
  slash included. Now `updateBlockType` writes **only the type column**,
  as the other single-field changes already did
- **[Bug fix]** **[📱 Mobile]** **The arrow**: closed a small triangle pointing
  right, open pointing down, as on Notion. Before they were Material's
  chevron arrows, and open it pointed up
- **[Bug fix]** **[📱 Mobile]** **In a new toggle you couldn't write.** Open and
  empty it showed nothing, and the "+ Add inside" button appeared only if
  there was already at least one child. Now the grey line *Empty toggle.
  Tap to add a block inside.* appears; with children there is no extra
  button anymore, you add with Enter as on Notion
- **[Bug fix]** **[📱 Mobile]** **Enter in the title made the title leave the
  toggle.** It was treated as a paragraph: the new line was born above
  with the text before the caret — that is, with the title — and the
  toggle stayed empty below. Now (`enterInToggle`) with the toggle
  **open** the new line is the **first child**; with the toggle
  **closed** it is **another toggle** right below, because inside it
  wouldn't be seen
- **[Bug fix]** **[📱 Mobile]** **After an Enter inside the toggle the letters
  ended up in the wrong line**: "1", Enter, "2" gave "12" in the line
  above. The lines inside a toggle were in a column **without `key`**, so
  Compose recognised them by position: the new line is born above (it is
  what keeps the keyboard still, see `splitBlockAt`), the line with the
  caret moved down one place but its field stayed where it was and ended
  up showing the new line. Found with the `NOTE` log, which showed the
  letter arriving at a field that didn't have focus. The top-level text
  didn't have the problem because the list containing it already has the
  keys
- **[Change]** **[📱 Mobile]** **Backspace on an empty line inside a toggle
  removes it** and brings the caret to the one above, or to the title if
  it was the first (`removeEmptyNestedLine`). Before nothing happened and
  an empty line created by mistake stayed there
- **[Change]** **[📱 Mobile]** Once a toggle is created from the "/" menu or
  from the "+", **the caret goes to its title** and the keyboard stays
  up: before it closed and you had to tap the line again. Focus lands for
  an instant on the invisible field, because the shared field you were
  writing in — if that was its only line — dies with the caret inside
- **[Bug fix]** **[📱 Mobile]** The lines inside a toggle are **aligned under
  the title's text** (`NESTED_INDENT` = arrow + gap). Before the indent
  was a fixed 20, four points shorter, and they stood crooked to the left
- **[Change]** **[📱 Mobile]** **A toggle is removed with backspace**, as on
  Notion: at the start of the title it **goes back to normal text**, with
  its title, and the lines it contained **come out and stay right
  below**, at the same level (`unmakeToggle`, `unnestAndConvert`). A
  second backspace joins it to the line above. Before nothing happened,
  and a toggle with a written title couldn't be removed in any way: the
  bar's bin appears only on empty lines. The inner lines aren't deleted
  on purpose — a backspace that takes away half a page hidden in a closed
  toggle would be the worst way to lose text
- The caret stays **at the start of the line** after the transformation:
  the shared field, when it receives focus, now respects the requested
  position too (`pendingCaret`) instead of always going to the end. At the
  end, the next backspace would have eaten the title's last letter
  instead of joining the line to the one above
- **[Bug fix]** **[📱 Mobile]** **A new toggle is born open.** The open/closed
  state lives in the block and stayed there even when changing type: a
  line that had been a closed toggle, made a toggle again, was reborn
  closed, and Enter in the title created a second toggle below instead of
  going inside
- **[Bug fix]** **[📱 Mobile]** Opening/closing a toggle writes **only that
  column** (`setExpanded`) and starts from the most recent version of the
  block: before it saved the copy the arrow had in hand, which could have
  a title older than the one just written
- **[Bug fix]** **[📱 Mobile]** The bar's bin on empty lines now first lands
  focus on the invisible field: the line that vanishes had the caret
  inside, which is how the app closed
- **An open doubt, declared**: in the first attempt, turning an **empty**
  toggle into text, the empty line right below vanished too. Redone with
  the log on, it didn't happen; the log shows however that the backspace
  simulated with `adb` arrives at the field **twice**, which is the main
  suspect. If it happens with the real keyboard, it is to be resumed.
  *Update of 23/09/2026*: pressing the real Samsung keyboard's key the
  backspace arrives **two or three times** there too; for the empty line
  inside a toggle the duplicates are now discarded (see "The jerks inside
  toggles")
- **Declared limits**: backspace at the start of a line **with text**
  inside a toggle doesn't merge it with the one above (they are two
  separate fields, it is a separate job); the lines inside a toggle stay
  fields of their own, not the shared text, so two lines can't be
  selected together
- **Verified on the phone**: `/toggle` → toggle without "/", caret on the
  title; title, Enter → first line inside, aligned; `412`, Enter, `5`,
  Enter, `6` → three separate lines; backspace on an empty line →
  vanishes, caret at the end of the one above; two lines inside, closed
  and reopened → **both still in the database**

### ✏️ v0.35.1 — 22/09/2026 · 19:36 — How the "+" menu closes
*(Claude Code on the PC)*

- **[Change]** **[📱 Mobile]** Two ways, and they are the ones that come
  naturally: **tapping the "+" again**, which works as a switch, or
  **tapping an empty part of the page**. The keyboard stays where it is
  in both cases
- **[Bug fix]** **[📱 Mobile]** **Tapping the page no longer moves the caret.**
  The menu already closed by itself at the first tap outside, but that
  tap **also reached the page below**, which brought the caret elsewhere:
  you went back to writing and found yourself somewhere else
- To solve it, ignoring the tap "while the menu is open" wasn't enough:
  between the automatic closing and the tap on the page **it wasn't
  certain who arrived first**, so sometimes the menu was already closed
  and the caret moved anyway. The automatic closing was removed
  (`dismissOnClickOutside = false`) and the page does it: since we
  decide, the order is always the same
- **And intercepting the tap from the list wasn't enough either**: that
  handler only sees what the children haven't already taken, so tapping
  **a line of text** the writing field took the tap — caret moved, menu
  still open. It takes an **invisible veil** laid over the page while the
  menu is open, which intercepts before anyone: it closes the menu and
  lets nothing else through. It covers only the page and not the bar, so
  the "+" stays a real switch
- `focusable = false` on the menu **stays**: a menu that takes focus
  makes the keyboard close, and here the keyboard must stay where it is
- **Roads discarded, tried and removed on 22/09/2026**: (1) making the
  menu close with the **back gesture** — with the keyboard open that
  gesture is taken by the keyboard and doesn't reach the app at all, so
  two gestures were needed; (2) closing the keyboard when the menu opens
  to get around point (1) — it worked, but seeing the keyboard drop under
  your fingers every time a list opens is worse than the problem it
  solved; (3) closing menu and keyboard **together** with the back
  gesture — a single gesture, but it took the bar away too, which nobody
  had asked to close
- **Verified on the phone** with the test that leaves no doubt: wrote
  `Test` with the caret at the end, opened the "+", tapped the **line of
  text** (not the empty space), and then typed a letter. If the caret had
  moved it would have ended up at the start; it ended up at the end —
  `TestXY` — and the menu had closed

### 🆕 v0.35 — 22/09/2026 · 19:06 — Text colour and colour behind the text (the brush)
*(Claude Code on the PC)*

- **[New feature]** **[📱 Mobile]** A **brush in the bar, next to Aa**, opens a
  colour picker from which you tint the selected text or the band behind
  it. Two boxes at the top, *Text* and *Background*, to switch from one to
  the other without closing anything
- **Next to Aa and not inside**, on purpose: colour isn't a switch like
  bold and italic, it opens a window of its own, and putting it in line
  with those would have promised a behaviour it doesn't have
- **[New feature]** **[📱 Mobile]** The picker is Windows', which is the one
  asked for by looking: a large box with the spectrum (hue from left to
  right, saturation from top to bottom), the brightness strip on the
  right from white to black, the three Red/Green/Blue boxes and the
  **Hex** box, where the code is written or pasted by hand — it is the way
  colours are copied from outside
- **A single truth: hue, saturation, brightness.** The three ways of
  choosing always agree because they read and write the same triple.
  Keeping RGB as the truth instead would mean recomputing the dot's
  position at every turn, and **on greys the hue doesn't exist**: the dot
  would shoot to the left by itself every time you pass through a grey
- **While writing in the Hex box the colour moves only when what is
  written is a real colour**: halfway through `#FF00` there is nothing to
  show yet, and jumping to a random colour at every letter would have
  been worse than not moving
- **[Bug fix]** **[📱 Mobile]** **The selection is set aside before opening the
  window.** The panel rising from the bottom takes focus from the writing
  field, and a field losing focus **closes the selection** reducing it to
  a caret: when applying the colour there was nothing left to colour, and
  nothing happened. Seen on the phone at the first attempt. Now the
  fields report the selection as it goes (`reportSelection`) and the
  brush makes a copy of it the instant it is tapped. The Aa bar's
  commands never had this problem because they don't open anything
- **[Bug fix]** **[📱 Mobile]** Island blocks realigned with the database **only
  when the text changed**, not the formatting: the colour was saved but
  the line stayed as it was until the next keystroke
- **[Bug fix]** **[📱 Mobile]** **Switching to "Background" the dot no longer
  moved.** The colour state was also tied to the target
  (`remember(initialHex, background)`), so tapping the "Background" box
  `remember` built a **new** state — while the tap handlers, which a
  `pointerInput` keeps frozen from the first composition, kept writing to
  the old one, by then thrown away. From outside it looked like the
  colour box had stopped responding. Two fixes together: the state is
  created **only once** at opening, and the tap lambdas are kept fresh
  with `rememberUpdatedState`. Changing target the colour now **stays
  what it is**, which is also handier — usually you want the same colour,
  or a correction of it
- **[Bug fix]** **[📱 Mobile]** The dot stays **inside the edges** of the box:
  on full red it fell in the top left corner and a quarter of it was
  seen, that is it seemed not to be there
- **In the text the colour sits below the spoiler**: covered, the veil
  must win over everything, otherwise you would read the coloured text
  through it
- **A hex that can't be read is ignored** instead of breaking the
  drawing: those codes are written by hand by the user, and one letter
  too many mustn't make the text vanish
- **No migration**: `color` and `background` are two fields with a
  default value inside the text's JSON, like `spoiler`
- **Verified on the phone**: word selected, brush, colour taken from the
  spectrum, `Apply` — green text `#26D931` on screen and in the database;
  then the same on *Background*, and `No colour` which removes only that
  one leaving the text coloured

### 🆕 v0.34 — 22/09/2026 · 18:36 — A page's three-dots menu, and Updates
*(Claude Code on the PC)*

- **[New feature]** **[📱 Mobile]** The **search magnifier disappears** from the
  top bar, on every page and on the main one, and in its place — same
  position — there are the **three dots**. Inside: Favorite, Search,
  Duplicate, Move to, Move to trash, Lock view, Lock page, Import, Export,
  Updates. The things done to a whole page are in a single place instead
  of being scattered between the bar and the settings
- **[New feature]** **[📱 Mobile]** The same menu is also on the **database
  opened full screen**: a database there is a page like the others. It is
  a file of its own (`PageOptionsSheet.kt`) precisely because two
  different screens open it, and two copies of a command menu drift apart
  at the first change
- **Import and Export are off**, as asked: they are visible to say they
  will come. The entries that instead **make no sense** on a certain page
  aren't there at all — on the main one no Favorite, Duplicate, Move to
  or Move to trash, and "Lock view" only on databases (*since 24/09/2026
  Move to on the main one is there, greyed out*: request 33). They are
  two different things: off means "not yet", absent means "not here"
- **Duplicate** copies blocks and, for a database, columns rows and
  cells, and puts the copy **next to the original**. Block and column ids
  are translated while copying, otherwise the children of a toggle and the
  cells would stay hanging on the original. **Subpages aren't
  duplicated**: the copy points to the same ones, because duplicating
  half an archive in cascade is almost never what you want.
  *Superseded*: see "Duplicate: you choose where the copy goes" further up
  — pointing to the same subpages meant the copy wasn't a copy
- **Move to**: here the navigation tree isn't a field of the page but
  **the blocks that reference it**, so moving means removing that link and
  creating one at the bottom of the chosen page. Among the destinations
  databases don't appear (their content is rows, not blocks) nor pages in
  the trash.
  *Superseded* (24/09/2026, request 33): the choice is now the tree with
  the search, database pages included; see "Move to — the tree with the
  search" further up
- **Move to trash** doesn't delete anything: it marks `trashedAt` and
  removes the links that showed it. The content all stays. **There isn't
  a trash screen yet** — until there is, a thrown away page can only be
  found from the database, and the confirmation window says so instead of
  pretending nothing happened
- **Lock page** makes the content **read-only**, not off: `readOnly` and
  not `enabled = false`, so the text can still be selected and copied. It
  applies to title, body, rows' names and cells, and makes the bar above
  the keyboard and the "New page" button disappear — they are all things
  that modify
- **Lock view** is another thing and applies only to databases: rows can
  still be edited, **the layout can't** (view, sorting, filter, grouping,
  columns). The commands stay in their place **off** instead of
  disappearing: a button that vanishes makes you think of a fault, an off
  one says it was put to rest on purpose. Deleting instead stays possible
  — the lock protects how the database is put together, not its existence
- **[New feature]** **[📱 Mobile]** A screen that lists what was changed in
  the page, from the most recent, in the form asked for: `22.09.2026
  18:34` and below the previous text **struck through**, an arrow, the
  current text
- **One entry per writing session, not per key pressed.** It is the
  delicate point: recording every keystroke, "Exmaple → Example" would
  become seven unreadable lines. A session lasts as long as you write in
  the same block and closes when the caret goes elsewhere, when the page's
  structure changes or when you leave. It is **the same boundary Undo
  already uses**, reused instead of inventing a second one that sooner or
  later would have drifted out of line
- **[Bug fix]** **[📱 Mobile]** Just moving the caret, without writing, didn't
  close the session: the change stayed pending until the next one and the
  history was always one piece behind. Now the screen closes it when the
  focused line changes, and the title closes it when it loses focus
- **The "after" is read after a pause** (`EDIT_SETTLE_MS`, 400): what was
  just written travels to the database and back, and reading it right away
  the history would tell of a correction cut off at the last letter
- **And on a scope that doesn't die with the screen**
  (`PageRepository.recordEditAfterSettling`): the last session closes
  exactly while leaving the page, and the ViewModel's scope at that point
  is already cancelled — that line, which is exactly the one you want to
  find again, would never have been written
- **The title enters the history** with a reserved block id
  (`PageEditEntity.TITLE_BLOCK_ID`): it isn't a block but is edited like
  them, and giving it an id avoids treating a `null` as a special case
  everywhere
- **`blockId` has no constraint towards blocks**, on purpose: the history
  must survive the block it tells about, otherwise deleting a paragraph
  would also delete the proof it was there
- **Declared limit**: the blocks' text and the title are recorded.
  **Database cells aren't** — they live in separate tables and go through
  another road, which will have to be hooked separately
- **Verified on the phone**: migration to 23 with the pages intact and
  the new table created; wrote `Exmaple`, corrected to `Example`, moved
  the caret, and in "Updates" **a single line** appeared with the right
  date and the old word struck through

### 🆕 v0.33 — 22/09/2026 · 15:19 — Favorites, the base
*(Claude Code on the PC)*

- **[New feature]** **[📱 Mobile]** A page or a database can be put among the
  favorites. **For now there is nothing that shows them**: it is
  deliberate, it is the base the coming sidebar will rest on
- **A single column for pages and databases** (`pages.isFavorite`, with
  migration 21→22): here a database **is** a page with `isDatabase = 1`,
  so putting it among the favorites is exactly the same operation. Two
  separate lists would have been two places to keep in line and two ways
  of getting it wrong
- **The main page can't be put among the favorites**: it is the one you
  always start from, and a link to it wouldn't lead anywhere. The star
  doesn't appear there, and the ban is repeated in
  `PageRepository.setFavorite` — a rule that lives in only one place
  sooner or later gets bypassed by a new road
- **Two different places, and the reason**: for pages it is a **star in
  the top bar**, because up there there are no other settings; for
  databases it is a switch inside **Settings**, because that window is
  the only one reached both with the database inside a page and opened
  full screen, so it is also the only place where it doesn't have to be
  put twice
- **Row pages fit too**, unlike the Home list: there the exclusion serves
  not to fill the Home with pages nobody created on purpose, here instead
  only what was chosen one by one ends up in the list
- **Only that column** is written (`PageDao.setFavorite`), not the whole
  row: the same page can be open in two screens together, and saving it
  whole from here would put back a title changed in the other
- **Verified on the phone**: migration to 22 run with the five pages
  intact; star on and off on a page and the switch on and off on a
  database, checking the value in the database each time

### 🔧 v0.32.0.1 — 22/09/2026 · 14:52 — Formatting survives writing
*(Claude Code on the PC)*

- **[Bug fix]** **[📱 Mobile]** **The censored word no longer reappears at every
  key.** The saved spans are always one round behind the field — the
  field already has the letter just typed, the database will have it in
  a few tens of milliseconds — and in that frame the transformation gave
  up drawing every style. For bold and italic it wasn't noticeable; a
  spoiler that for an instant isn't covered instead **can be read**, and
  it was the flicker seen when writing a space or deleting. Now, instead
  of giving up, the same change is applied to the spans with
  `applyTextEdit`, which returns spans matching **exactly** the line
  shown: the positions can't drift
- **Declared limit**: when the field has a number of lines different from
  the blocks — that is, at the instant a line is born or vanishes —
  nothing keeps being drawn. There the line-block match isn't reliable,
  and a style taken from the wrong block would be worse than no style
- **[Bug fix]** **[📱 Mobile]** In the flowing text every line touched was
  rewritten as **a single piece without format**: adding one letter at
  the end of a paragraph was enough for the bold of a faraway word to
  vanish. It was an old defect, not noticed because formatting is almost
  always done at the end; with spoilers it would have been evident at the
  first attempt — you cover a word, keep writing and the veil falls by
  itself
- Now the changed zone is treated as **a single text**, line breaks
  included: the change is applied to the spans with `applyTextEdit`,
  which touches only the piece really changed, and then it is put back
  into lines (`splitLines`/`joinLines`). It applies across several lines
  too: breaking a line in the middle of a sentence, the half that goes
  down **keeps its format**
- **Adjacent spans with the same formatting are merged** (`merged`).
  Applying only the changed stretch, every key typed left its own little
  separate piece and a paragraph's JSON grew by one entry per letter
  written: really seen on the phone, a ten-character sentence was ten
  spans
- **Safety net**: if the result no longer matches the line it must
  describe, the line is written in plain text. Better to lose a bold than
  to write in the database a text different from the one the user sees —
  it already happened once, and that time it cost emptied blocks
- **[Bug fix]** **[📱 Mobile]** In island blocks (toggles) the **Aa** bar
  formatted **one letter before** the selected one: the selection's
  positions also count the hidden line break at the head, the spans don't

### 🆕 v0.32 — 22/09/2026 · 14:45 — Covered text (spoiler)
*(Claude Code on the PC)*

- **[New feature]** **[📱 Mobile]** As on Discord and Telegram: you write
  `||something||` and the four bars disappear — they are a command, not
  written stuff — leaving in their place a **grey band** that covers the
  letters. The same thing is done from the **Aa** bar, with the crossed
  eye, on any selected piece of text
- **[New feature]** **[📱 Mobile]** It is uncovered **by tapping it**: the
  covered piece becomes readable again when the caret, or the selection,
  is **inside** it, and is covered again as soon as the caret leaves or
  the field loses focus. Nothing else was needed: in an editor a tap is
  already a caret landing there
- **Inside, not on the edges**, and the reason is the moment the spoiler
  is born: once `||word||` is written the caret stays resting **right
  after the last letter**, so counting the edge too the spoiler came into
  the world already uncovered — you saw the word with its light band
  instead of the censorship. Counting only the inside it gets covered the
  instant the fourth bar is written, and that is what you expect. For
  reading it nothing changes: a finger falls inside the word, not on its
  edge
- **Choice**: when covered the letters are drawn **transparent**, not
  removed. They stay where they were, so the line is as wide, wraps where
  it did and the caret moves inside normally; the band passes over it.
  Really removing the text would have meant redoing the whole translation
  of positions, which is the most delicate part of this editor
- **Choice**: the closing binds to the **nearest opening**, so `||a|| and
  ||b||` makes two separate spoilers instead of covering the "and" in the
  middle too. Between the bars there can be neither another bar nor a
  line break
- **[New feature]** **[📱 Mobile]** A spoiler **doesn't grow by writing after
  it**. For everything else (bold, italic...) continuing to write
  continues the formatting, which is what all editors do; here not,
  because the caret stays right there attached and the next word would
  have become invisible without anybody asking for it. Writing **inside**,
  instead, it widens
- **No migration**: `spoiler` is a field with a default value inside the
  text's JSON, and the text already saved — which doesn't have that key —
  is read back as it is

### ✏️ v0.31.4 — 22/09/2026 · 14:27 — Numbered lists change way of counting at every indent
*(Claude Code on the PC)*

- **[Change]** **[📱 Mobile]** Same idea as bullets, eight ways in a row
  (`NUMBERED_STYLES`): `1.`, `i.`, `1)`, `i)`, `a.`, `One.`, `a)`,
  `First.` — and then it starts again from `1.`. Verified on the phone
  level by level, from zero to eight: at the ninth indent `1.` comes back
- **Declared assumption**: "letters with numbers" was understood as
  **letters with a dot** (`a.` `b.` `c.`), parallel to "letters with
  brackets" (`a)`). If something else was meant it is a single line to
  change
- **[Bug fix]** **[📱 Mobile]** **Every level has its own counter that restarts
  from one** (`numberedListOrdinal`). Before the number was composed with
  the whole branch — `1.1.1` — and at that point the writing styles of the
  innermost columns made no sense: `i.` can't say "one point one". Now the
  count is kept in a stack of counters, one per level, and going back it
  resumes from where that column had stopped
- **Choices inside the writing styles**: Roman numerals go back to being
  **digits beyond 3999** (`romanNumeral`), which is better than a line of
  `m`; letters continue **like a spreadsheet's columns** (`letterOrdinal`,
  `z` → `aa`) instead of stopping at zed; words (`One`, `First`) stop at
  **twenty** and beyond go back to digits, because "Thirtyseventh" would
  eat half a line
- **[Change]** **[📱 Mobile]** The numbers **inside a toggle** also go through
  the same function: at the same indent a number inside a toggle and one
  outside must be written the same
- **[Bug fix]** **[📱 Mobile]** The line just created with Enter takes the
  **expected** number (`carriedOrdinal + 1`) instead of no number: it is
  the same remedy as the bullets, and without it you saw the sign appear
  late

### ✏️ v0.31.3 — 22/09/2026 · 14:09 — Bulleted lists change sign at every indent
*(Claude Code on the PC)*

- **[Change]** **[📱 Mobile]** As on OneNote: `•` filled, `◦` empty, `▪`
  small filled square, `▫` small empty square, `◆` filled diamond, `◇`
  empty diamond, `▸` small filled triangle, `▹` small empty triangle — and
  then it starts again
- **[Change]** **[📱 Mobile]** The sign depends **only on the indent level**,
  not on what came before. It is what is needed to bring a line back: it
  finds by itself the sign of the column it arrives in, whatever road it
  took. With a "continue from where you were" counter, two lines at the
  same height could end up with different signs
- **[Change]** **[📱 Mobile]** Each has **its own size**
  (`BULLET_MARKER_SIZES`): these characters are drawn very differently
  from each other — a dot takes less than a third of its space, a diamond
  almost two thirds — and with a single size for all the dots came out
  tiny and the diamonds blotches. This way they look the same size
- **[Bug fix]** **[📱 Mobile]** **The flowing text's line height is always
  fixed** (`RUN_LINE_HEIGHT`, 24). Letting the font decide, every line
  became as tall as the largest sign it contained: the lines with dots
  (written at 20) were taller than those with diamonds (written at 13),
  and the list came out with an irregular step. The signs looked crooked,
  but it was the lines that were crooked. Measured on the same list of
  seventeen points: before the step between one line and the next went
  from 34 to 48 points, after it is 43-44 on all
- **[Change]** **[📱 Mobile]** The signs drop a hair below the baseline
  (`BULLET_MARKER_DROP`): resting exactly on the baseline they looked a
  bit too high compared to the text. It is a **fraction of the sign's
  size**, not a fixed measure, so the dots (written large) and the
  diamonds (written small) drop in proportion and stay aligned with each
  other. It isn't a computed centring: it is a touch-up decided by looking
  at the screen
- **Constraint**: no sign goes over **20**. The limit isn't the text's
  size (16) but **the line height**: the flowing text sits on 24-point
  lines and a 16 font takes less than 19 in height, so there is margin.
  Within those 24 the line doesn't widen; beyond, only the lines with the
  largest sign would widen and the list would come out in steps
- **[Bug fix]** **[📱 Mobile]** **The caret no longer snaps to the start of the
  line when pressing Enter.** Right after an Enter the field has one line
  more than the database, and from that point down lines and blocks didn't
  match: asking for "block number i" you got the one of the *next* line.
  If it was a paragraph — the normal case, a list almost always ends with
  an empty line below — the new line was drawn **without a bullet**, and
  you saw the caret snap left and then go back to its place as soon as
  the database arrived. Now the insertion point is found **from the end**
  (the lines at the bottom that still match their blocks say how many
  come after the new one) and from there down the match is shifted by
  one. Looking for it from the start doesn't work: pressing Enter the
  split line changes text too. Verified by capturing the **first frame**
  after Enter: the bullet is already there and the caret is in its place
- **[Change]** **[📱 Mobile]** The indent cap rises **from 5 to 10**: with
  five levels the round of eight could never have been seen either ending
  or starting again

### 🔧 v0.31.2.1 — 22/09/2026 · 13:54 — Cover and title scroll with the page
*(Claude Code on the PC)*

- **[Bug fix]** **[📱 Mobile]** They sat outside the scrolling list, so they
  stayed nailed at the top while the text scrolled below. With a tall
  cover they ate half a screen that couldn't be recovered. Now they are
  the **first item of the list** and go away when scrolling, as on Notion.
  It wasn't a recent regression: they were always out there, it shows
  since there is a cover
- **[Bug fix]** **[📱 Mobile]** They are **a single item** on purpose: the
  list's indices also serve to bring into view the line that takes the
  caret, and a fixed offset (`HEADER_ITEM_COUNT`) is much easier to keep
  right than a variable one
- **Verified**: the title can still be written, and **focused and then
  scrolled away six times in a row it doesn't close the app** — it was the
  real risk, because now that text field too can be destroyed by
  scrolling like the others inside the list

### ✏️ v0.31.2 — 22/09/2026 · 13:29 — The bar above the keyboard, redone
*(Claude Code on the PC)*

- **[Change]** **[📱 Mobile]** It has **a grey all of its own**
  (`DarkSurfaceVariant`), lighter than the page and different from the
  keyboard: before it was the same black as the page and you couldn't tell
  where one ended and the other began
- **[Change]** **[📱 Mobile]** It is **lower**: the buttons go from the
  standard 48 to 40 and the icons to 22 (`BAR_BUTTON_SIZE`,
  `BAR_ICON_SIZE`), and the margin above and below disappears — the height
  is given by the buttons, and every extra point there is an extra band
  covering the text. All the buttons go through `BarButton`, so they can't
  have different sizes from each other
- **[Bug fix]** **[📱 Mobile]** **It goes away together with the keyboard.**
  Its bottom edge rests on top of the keyboard, so while that goes down
  the bar follows it — but it stopped at the screen's edge and stayed
  there until the system declared the keyboard closed: from outside it
  looked like it vanished a few instants later. In the last points of the
  descent it is now pushed down by as much as it lacks to go out
  (`slideAway = barHeight - keyboardInset`), so it reaches the edge at the
  same moment; when the system then removes it it is already invisible and
  no jerk is seen. With the "+" menu open the sliding is off, because
  there the bar is seen even without keyboard
- **[Bug fix]** **[📱 Mobile]** It is **attached to the keyboard**: it stayed
  floating a few points up because the space for the navigation bar came
  from the `Scaffold` and the one for the keyboard from `imePadding`, and
  with the keyboard open **they added up**. Now the bottom takes the union
  of the two (`WindowInsets.ime.union(WindowInsets.navigationBars)`): the
  higher wins, which is always the right one — with the keyboard open the
  navigation bar sits behind it and takes nothing

### 🔧 v0.31.1.1 — 22/09/2026 · 11:31 — The formatting bar no longer stays in mid-air
*(Claude Code on the PC)*

- **[Bug fix]** **[📱 Mobile]** It is the defect that kept coming back for days,
  and the cause wasn't in the code but in the **manifest**: the activity
  didn't declare how to behave when the keyboard opens, and Android chose
  by itself to **scroll the whole window** to keep in view the point where
  you write — the page title went off the top of the screen. The trouble
  is that the code *already* adds the space for the keyboard with
  `imePadding`: the two effects added up and the bar ended up detached
  from the keyboard, by almost a keyboard's height
- **[Bug fix]** **[📱 Mobile]** With `android:windowSoftInputMode="adjustResize"`
  the window stays still and only `imePadding` takes care of making room,
  which knows where the keyboard really is. Verified on the same gesture
  that made it appear: the title now **stays in its place** and the bar
  is glued to the keyboard
- **[Bug fix]** **[📱 Mobile]** **The tap on empty space no longer sends the
  caret to the bottom of the page.** The "you tapped the empty space, keep
  writing" handler covered **the whole** list, so it also took the space
  next to the title and the gaps between one line and the next: tapping
  them, the caret jumped to the last block at the bottom and the keyboard
  opened. Now it fires only for taps **below the last block** — and only
  if the last block is really the last, not the last *visible*. Verified
  that the useful gesture keeps working

### ✏️ v0.31.1 — 22/09/2026 · 11:17 — Checkboxes enter the shared text field
*(Claude Code on the PC)*

- **[Change]** **[📱 Mobile]** It is the root cure for the whole family of
  checkbox defects. As long as each was a text field **of its own**,
  pressing Enter moved focus from one field to another and every change of
  kind destroyed one while it had the caret: hence the keyboard vanishing,
  the capital letter jerking, the letters in the previous line, the text
  deleted by Enter and the app closing. Inside the shared field **pressing
  Enter is just writing a line break**, as between two paragraphs: there is
  no focus to move and nothing to destroy
- **[Change]** **[📱 Mobile]** The little square can't sit *inside* a text
  field, so the text reserves its space with invisible characters
  (`CHECKBOX_MARKER`) and the real square is drawn **on top**, at the
  position the field itself declares for that character
  (`RunCheckboxOverlay`). This way it stays glued to its line even when the
  text wraps by itself, and has a real touch zone instead of being a
  character to centre with the finger
- **[Change]** **[📱 Mobile]** The overlay must be put **after** the field
  inside the `Box`: in Compose taps are received first by the last child,
  and put before it the square didn't respond — the tap went to the text
- **[Change]** **[📱 Mobile]** A ticked checkbox has its text **struck
  through and faded**, as on Notion. Enter creates another unticked one,
  and backspace at the start of the line removes the square leaving the
  text: they are the same two behaviours bulleted and numbered lists
  already had, not a separate branch
- **Trap, which cost half an hour**: the types living in the shared field
  are listed **twice**, in `FLOWING_TYPES` in the screen and in
  `isFlowingTextType` in the ViewModel. Updating only the first, the field
  showed the checkboxes but the ViewModel didn't recognise them as its
  own: on screen you wrote and **nothing reached the database**, without
  an error. If you touch one, the other must be touched
- **Verified on the phone**, measuring the keyboard's real events:
  ticking with a tap works and is saved; Enter creates another checkbox and
  the text stays (`AaaaaaaaaaaaaQQ` / `RR` / `SS` in the database, no
  duplicate index); backspace at the start of the line removes the square;
  creating a checkbox from the "/" menu leaves the keyboard open and you
  write right away. In all four tests `ImeTracker` **recorded neither
  `onHidden` nor `onShown`**, and there was no crash
- **[Change]** **[📱 Mobile]** Lines with a checkbox **are more spaced**
  (`CHECKBOX_LINE_HEIGHT`): the square is small and close together it is
  easy to tick the wrong one with the finger
- **[Bug fix]** **[📱 Mobile]** **The squares no longer jump at every key.**
  Writing or deleting, they vanished and reappeared shifted sideways for
  an instant. They were drawn at the positions recomputed from the field's
  current text, while the coordinates came from the previous frame's
  measure: two sources that for a moment don't match. Now the marker is
  looked for **inside the text the field really measured**
  (`layoutInput.text`), so drawing and coordinates can no longer drift
- **Declared compromise**: **the writing caret is as tall as the line**,
  and the text field draws it that way — it can't be separated. More space
  between checkboxes means a taller caret. At 34 it was noticeable; the
  value is 27, just above the natural one (24): a caret of normal size and
  a bit of air between one checkbox and the next
- **[Change]** **[📱 Mobile]** The text is **centred** in the tall line
  (`lineHeightStyle`). By itself Compose shares the extra space in
  proportion to how much the letters rise and descend, and the text
  doesn't end up in the middle: the squares, which instead sit in the
  middle, came out misaligned. Asking for the centre, text and square
  coincide **by construction**, whatever line height is chosen — better
  than an eyeballed correction to touch up every time
- **Stays as it was** the checkbox inside a *toggle*: there it is still a
  line of its own (`BlockRow`), because a toggle's children are drawn on
  their own

### 🔧 v0.31.0.1 — 22/09/2026 · 10:27 — Enter inside a checkbox no longer moves focus
*(Claude Code on the PC)*

- **[Bug fix]** **[📱 Mobile]** Three different checkbox defects — the capital
  letter jerking from lowercase to uppercase, the letters ending up in the
  previous line when pressing Enter quickly, and the app closing **without
  a message** — had **a single cause**, found with the log: every checkbox
  is a text field of its own, and pressing Enter the focus moved to the new
  field. In the log at every Enter you saw `onStartInputView
  restarting=true`, that is **the keyboard detaching and reattaching**. At
  that moment the automatic capital resets (hence the jerk), the keys
  already pressed arrive at the old field, and the field just left is
  destroyed while the keyboard is still asking it where the caret is —
  which is the `LayoutCoordinate ... isAttached` error with which the app
  vanished
- **[Bug fix]** **[📱 Mobile]** The cure is counterintuitive: **the new line is
  born above, not below.** It breaks the line putting the text *before* the
  caret in a new block inserted above, and leaving to the block you are
  writing in — which already has focus — the text *after* the caret. On
  screen it is identical, but **focus never moves**: no field change, no
  detached keyboard, nothing to destroy. Measured: in a burst of sixteen
  Enters, **zero** focus changes, and the letters all in the right place
- **[Bug fix]** **[📱 Mobile]** The tick and the "restart from N" stay with the
  line above, which is the continuation of the one that was there; the new
  line is born unticked, as on Notion
- **[Bug fix]** **[📱 Mobile]** The block's index is **reread from the database
  inside the transaction** (`PageRepository.splitBlockAbove`), and splits
  are queued with a `Mutex`. Before I used the index of the copy the screen
  had in hand: pressing Enter quickly it was one Enter old, and **two
  blocks were born with the same place** — really seen in the database, not
  hypothesised
- **[Bug fix]** **[📱 Mobile]** **The second Enter in a row was no longer
  done.** Side effect of the cure, found right after by reading the
  database: the recognition of the "keyboard echo" (see `SplitEcho`) is per
  field, and now that the field survives all the splits it stayed armed.
  Writing quickly two lines ending with the same text, the second Enter
  was mistaken for an echo and ignored. Now the echo's memory resets as
  soon as a real character is typed: an echo arrives right after the split
  and with nothing in between, so that memory is no longer needed.
  Verified: three Enters in a burst give three lines, `Z` `W` `Q`
- **[Bug fix]** **[📱 Mobile]** **A text save no longer moves the block.**
  `updateBlockSpans` saved again the **whole entity**, so it also sent back
  the `orderIndex` and the tick the screen had in hand: after an Enter that
  copy is from an instant before, and the next character sent the block
  back to the place the new line had meanwhile taken. It looked like "I
  write, press Enter, it deletes what I wrote". Now it writes **only the
  text column** (`BlockDao.setText`)
- **[Bug fix]** **[📱 Mobile]** **The keyboard no longer vanishes** deleting a
  checkbox or pressing Enter on an empty checkbox, and **the app no longer
  closes**. They were the two horns of the same problem: that field
  necessarily dies (the block goes from a line of its own to flowing text,
  which is another composable), and destroying it while it has focus makes
  the app close, while detaching the keyboard with `clearFocus()` makes it
  vanish and come back. Both measured: without `clearFocus()` twenty cycles
  out of twenty closed the app with `LayoutCoordinate ... isAttached`; with
  `clearFocus()` `ImeTracker` marked `onHidden` and then `onShown` 315 ms
  later
- **[Bug fix]** **[📱 Mobile]** The third road is **the keyboard's parking
  spot**: a text field one point tall and invisible, outside the scrolling
  list (inside it would vanish as soon as it leaves the screen), on which
  focus lands an instant before the real field is destroyed. The keyboard
  stays open because it always has a field below it, and starts again on
  the new block as soon as it exists. Verified: the same twenty cycles no
  longer close the app, and in a complete sequence — I write, Enter, I
  write, Enter, backspace, I write — `ImeTracker` **records neither
  `onHidden` nor `onShown`**, that is the keyboard never moves
- **[Bug fix]** **[📱 Mobile]** The parking spot was **also needed by the
  flowing text's field**, not only by islands: empty line below a checkbox
  + backspace merges the line into the checkbox, the line vanishes and
  with it its field. It was the easiest case to run into and it closed the
  app. `backspaceAtLineStartDetailed` now says *what* it did, so the field
  knows when it is about to die. Verified: twelve cycles, no crash
- **The underlying cause, declared**: every checkbox is a text field **of
  its own**, while paragraphs share a single one. A block passing from one
  kind to the other therefore changes field, and a field destroyed while it
  has focus makes the app close because of a Compose 1.6 defect. Every
  point where this can happen must be protected by hand, and as long as the
  architecture is this one a new one can always be discovered. The two real
  ways out are **updating Compose** (the defect is fixed from 1.7) or
  **putting the checkboxes inside the shared field too** — which would also
  solve the selection across checkboxes, already in "Known limits"
- **Note on measuring**: `dumpsys input_method | grep mInputShown` **isn't
  enough** to tell whether the keyboard vanishes: it stays `true` even while
  it is closing. It had made me declare solved a defect that was still
  there. The honest signal is the `ImeTracker: onHidden` / `onShown` events
  in `logcat`
- **Declared limit**: a block with children (a toggle) still uses the old
  road, new line below and focus moving. Moving the block's identity to the
  line below, its children would end up under the wrong line
- **Honest note**: the capital's jerk and the sudden closing **weren't
  reproduced on command** — the first because injecting text via adb the
  keyboard doesn't come into play and the automatic capital doesn't fire,
  the second because it didn't happen in any of the bursts tried. The
  common cause was removed, which instead was measured; the real
  confirmation comes from using the app by hand

### 🆕 v0.31 — 22/09/2026 · 09:16 — Filters on numbers and checkboxes, and the checkbox in the middle of the column
*(Claude Code on the PC)*

- **[New feature]** **[📱 Mobile]** You can filter on **numbers** (from x to y,
  ends included), **dates**, **Select/MultiSelect** and **checkboxes**.
  Text, address, mail and phone stay out: there the question would be
  "contains these letters", that is a search, which is another thing and
  isn't there yet
- **[New feature]** **[📱 Mobile]** On **numbers** one end is enough: only
  "from" means "from there up", only "to" means "up to there". Half of the
  real questions are open on one side ("above 100"), and forcing to write a
  second made-up number would make them wrong. An empty cell never passes:
  it isn't zero, it is "there's no number"
- **[New feature]** **[📱 Mobile]** On **checkboxes** you choose *Checked*,
  *Unchecked* or both. Cells never touched **don't exist in the
  database**: they count as "unticked", not "I don't know", otherwise they
  would vanish from both filters — the worst way to get it wrong. Verified
  on the phone: *Checked* gives A, B, E, G and *Unchecked* gives C, F,
  Example, which are exactly the complements read in SQLite
- **[Bug fix]** **[📱 Mobile]** **The tap on a cell's empty space no longer does
  anything.** Before it went through the table and reached the page behind
  it, which takes it as "write here": tapping next to a checkbox the caret
  ended up **below** the database and the keyboard opened with the
  formatting bar hanging at the top. Now the cell eats the taps that don't
  hit anything; the children are served first, so the checkbox keeps
  responding (verified: `mInputShown=false` before and after the empty
  tap, and the screen byte-for-byte identical)
- **[New feature]** **[📱 Mobile]** Long-pressing the header of a **checkbox**
  column **"Center"** appears, which puts the square in the middle of the
  column instead of resting on the left edge; the same entry then becomes
  "Align left". Being an alignment and not a measure, it stays in the
  middle **whatever width** the column has
- **[New feature]** **[📱 Mobile]** The option is there only for checkboxes: it
  is the only cell content that is a single sign, where alignment is
  noticed. A centred text or date would make the column unreadable,
  because the lines would no longer all start from the same point
- It lives in `centerContent` of `database_columns`, **schema at version
  21**

### 🆕 v0.30 — 22/09/2026 · 01:46 — Filters
*(Claude Code on the PC)*

- **[New feature]** **[📱 Mobile]** Rows can be **filtered**: the icon to the
  left of Sort in the view's bar, and the **"Filter"** entry in the
  settings. They aren't two twin screens but **the same window** opened
  from two places: two copies would have drifted apart at the first
  change, and it is the kind of difference you notice only by using them
- **[New feature]** **[📱 Mobile]** On **dates** a page passes if its days
  **touch** the chosen ones, not if they coincide: an appointment from the
  3rd to the 7th appears filtering the 5th, otherwise filtering a single
  day would almost never show anything. **Repeating dates** count all
  their occurrences, not just the first
- **[New feature]** **[📱 Mobile]** On **tags** whoever has **at least one**
  among the chosen ones passes, like Notion's "contains any of": asking
  for all of them would make choosing more than one useless on a
  single-select property, where no row could ever pass
- **[New feature]** **[📱 Mobile]** The icon is **lit blue** when there is a
  filter: without it, a table with half of the pages would look like a
  table that lost some pages. The **bin** at the top right of the window
  appears only when there really is a filter to throw away
- **[New feature]** **[📱 Mobile]** The filter applies to **all views**: the
  question "show me only these pages" doesn't change meaning switching to
  the board or the calendar, and a filter that switches off changing view
  would be a filter you can't trust. If the property being filtered on is
  deleted, the filter stops applying instead of hiding everything
- **[New feature]** **[📱 Mobile]** The filter is saved on the page
  (`filterColumnId` and `filterValue`, **schema at version 20**). The value
  uses the same format as the cells, so reading it is the same function
  that reads a date: two copies can't drift apart. For this reason
  `DateRange`/`parseDateRange` moved from `DatabaseViewScreen` to
  `data/entity/DateValue.kt`
- **Declared limits**: **one filter at a time** on a single property, like
  sorting; Notion chains more than one with "and"/"or"

### 🆕 v0.29 — 22/09/2026 · 01:39 — Columns that hide
*(Claude Code on the PC)*

- **[New feature]** **[📱 Mobile]** **Long-pressing the header** of a column a
  menu appears with *Edit property* and **Hide**: the column vanishes from
  the table, which shrinks accordingly
- **[New feature]** **[📱 Mobile]** In the view's settings there is **"Property
  visibility"**, with the number of hidden columns next to it. Inside, the
  two lists **"Shown in table"** and **"Hidden in table"** as on Notion,
  each with its *Hide all* / *Show all*; tapping a line moves it to the
  other list
- **[New feature]** **[📱 Mobile]** **Hidden doesn't mean deleted**: the values
  stay in the cells (verified by reading the database before and after),
  and the property keeps appearing in sorting, grouping and a row's
  actions. It is just one column less to scroll horizontally, which on a
  phone is why it is needed
- **[New feature]** **[📱 Mobile]** Hiding is **on the header** and showing
  again **in the settings**, not for lack of symmetry: you decide to hide
  looking at the table, while a hidden column no longer has a header to
  long-press
- **[New feature]** **[📱 Mobile]** "Name" appears in neither of the two lists:
  it is the page's title, not a property, and a row without a name could no
  longer be opened
- **[New feature]** **[📱 Mobile]** The state lives in the `hidden` column of
  `database_columns` (**schema at version 19**), so it applies to all views
  of the same database and survives a restart (verified: hidden, app closed
  and reopened, still hidden)

### 🆕 v0.28 — 22/09/2026 · 01:23 — Repeating dates
*(Claude Code on the PC)*

- **[New feature]** **[📱 Mobile]** A date can **repeat**: from the date's
  window, **Repeat** line, with Google Calendar's shortcuts built on the
  chosen day — *Daily*, *Weekly on Sunday*, *Monthly on the first Sunday*,
  *Annually on...*, *Every weekday* — and a **Custom** screen for
  everything else: every N days/weeks/months/years, on which days of the
  week, "on the 21st of the month" or "the third Monday", and when it ends
  (never / by a date / after N times)
- **[New feature]** **[📱 Mobile]** **A page that repeats stays a single
  page.** No copies are created: the rule is attached to the date, and the
  calendar turns it into as many appearances as needed for the period being
  looked at. Opening any of them opens that page, and editing it changes
  all the occurrences — it is the synchronisation you expect, and the only
  way to be able to correct "every Sunday" with a single edit. Really
  creating a row for each repetition would fill the database with copies to
  correct one by one
- **[New feature]** **[📱 Mobile]** Occurrences are computed **only for the
  period shown**: a rule without end would generate dates forever, and
  nobody does anything with those of 2043 until they get there
- **[New feature]** **[📱 Mobile]** The fixed row of each page in the calendar
  is computed on the **starting** dates, not on the appearances: those
  change from one month to another and would make the row dance depending
  on where you are
- **[New feature]** **[📱 Mobile]** "The third Monday" and **"the last
  Sunday"** are two different things: when the chosen day is the last of
  its kind in the month the rule becomes "the last", because whoever says
  the last Sunday always means the last, even in months that have five. In
  the same way "every month on the 31st" falls on the last day in short
  months
- **[New feature]** **[📱 Mobile]** The rule is saved **next to the date**, as
  the fourth field of the cell's value (`RecurrenceCodec`): no new columns,
  no migration, and the dates written before keep counting as "happens
  only once"
- **Declared limits**: repetitions are seen in the **calendar**, not yet
  in the timeline; and **notifications aren't there** — they are a piece
  of their own, with system permission, alarms and rescheduling at reboot,
  and it was decided to do them later

### 🆕 v0.27 — 22/09/2026 · 00:59 — The "/" menu
*(Claude Code on the PC)*

- **[New feature]** **[📱 Mobile]** Writing **"/"** inside the text opens the
  blocks menu, divided into **Basic blocks / Media / Database** as on
  Notion. What you write after the slash **filters the list** ("/tab"
  leaves Table and Table view), and the slash with the filter disappears
  from the text as soon as you choose: it is a command, not written stuff
- **[New feature]** **[📱 Mobile]** **The entries not built yet are there
  anyway, off**: Callout, Quote, Link to page, the whole Media group,
  Gallery view, Database full page, Simple database, Linked view of data
  source. They say what is there and what is missing without forcing you
  to look for it, and when one gets built it will be enough to change its
  action. **It is the only place in the app where an inert entry is better
  than an absent one**: here you are choosing from a catalogue, not
  looking for a command that should be there
- **[New feature]** **[📱 Mobile]** The database view entries (**Table, Board,
  List, Calendar, Timeline view**) create the database inside the page and
  give it that view right away: "Calendar view" gives a calendar, not a
  table to change by hand. Verified on the phone
- **[New feature]** **[📱 Mobile]** The menu **doesn't steal focus**
  (`focusable = false`): the keyboard stays open, because the normal way
  of using it is to keep writing to filter
- **Declared limit**: the "/" menu works in the flowing text (paragraphs,
  headings, lists), not inside a checkbox or a toggle, which have a text
  field all of their own

### ✏️ v0.26.1 — 22/09/2026 · 00:47 — The calendar even without a Date property
*(Claude Code on the PC)*

- **[Change]** **[📱 Mobile]** Choosing the Calendar view on a database just
  created, **the calendar shows anyway**: current month, empty, with its
  days. Before in its place there was a sign explaining that a date
  property was needed and offering to create it — correct but useless,
  because an empty calendar explains itself
- **[Change]** **[📱 Mobile]** **The first tap creates the date property.**
  Tapping a day the page is born there, and with it the "Date" property if
  the database didn't have one — which is also set as the calendar's one,
  so adding another tomorrow the view doesn't change column under your
  feet. Whoever taps a day has already said everything needed: they want a
  page, on that day. The scaffolding is our problem
- **[Change]** **[📱 Mobile]** "+ New page" at the bottom also works without a
  date property: it creates the page dated today, and the property with
  it. The timeline instead keeps asking for the property, because there a
  page without dates doesn't even have a bar to draw

### 🆕 v0.26 — 22/09/2026 · 00:39 — The number's menu, as on OneNote
*(Claude Code on the PC)*

- **[New feature]** **[📱 Mobile]** **Long-pressing the number** opens the
  list's menu, with the same four entries as OneNote: *Begin a new list
  here* (the count restarts from 1 from there), *Renumber as continuation
  of previous list* (removes that "restart" and puts the line back in
  line), *Remove number* (the line stays but as a paragraph) and *Change to
  bullet*. All four tried on the phone
- **[New feature]** **[📱 Mobile]** The *Renumber as continuation* entry is
  always there but is **off when not needed**, that is when that line
  doesn't restart anything: removing it altogether would make the menu
  dance between one opening and another, and a grey entry also says *why*
  it can't be done
- **[Change]** **[📱 Mobile]** **A simple tap on the number no longer
  restarts the count.** It was handy to discover and annoying to suffer:
  it renumbered the whole list for a finger resting badly, without asking
  anything. Now that is a menu entry, where you choose it on purpose

### 🆕 v0.25 — 22/09/2026 · 00:28 — Grouping the table
*(Claude Code on the PC)*

- **[New feature]** **[📱 Mobile]** The table can **group rows by a property**,
  as on Notion: every group has its header with the value and how many
  pages it contains, its column header row and its "+ New page". It is
  chosen from the settings, **Group** entry (`tableGroupColumnId`,
  migration 17→18)
- **[New feature]** **[📱 Mobile]** You can group by **almost anything**:
  single and multiple tags (the header is the coloured tag), checkboxes
  (Checked/Unchecked), dates (a group per day), text and numbers. "Created
  on" and "Edited on" stay out, which aren't cells but properties of the
  row, and at the exact instant would give a group per row
- **[New feature]** **[📱 Mobile]** With **multi-select** a row appears in
  **all** the groups of its tags: it is what you expect from a grouping,
  and it is also why the board doesn't support it — there a card would
  have to sit in two columns together
- **[New feature]** **[📱 Mobile]** **"+ New page" inside a group creates the
  page already with that value.** Verified on the phone: created inside the
  "PC" group, the new row is born with the PC tag and stays there, instead
  of ending up among those without value
- **[New feature]** **[📱 Mobile]** The **"no value" group is always at the
  bottom** and is called "No <property>": in front it would steal the first
  glance from the rows already sorted. The **"Hide empty groups"** switch
  hides groups without pages, and is on by default, as on Notion; it
  appears only when there is a grouping
- **[New feature]** **[📱 Mobile]** Groups **close and reopen** by tapping the
  header. Which one is closed isn't saved in the database: it is how you
  are looking at the table now, not a property of the database
  (*superseded on 11/11/2026, v0.153.58*: closed groups are now
  remembered)
- **[New feature]** **[📱 Mobile]** The table's grouping is a **different**
  setting from the board's "Group by", on purpose: a board without columns
  doesn't exist and so falls back on any property, the table instead works
  perfectly well flat and mustn't reshuffle itself. Changing view doesn't
  touch the other
- **[New feature]** **[📱 Mobile]** A tag left in the cells after the option
  was deleted **doesn't make its rows vanish**: it gets a group anyway, at
  the bottom of the declared ones

### 🔧 v0.24.2.2 — 22/09/2026 · 00:03 — Straight covers, and the keyboard that doesn't vanish
*(Claude Code on the PC)*

- **[Bug fix]** **[📱 Mobile]** **Covers taken from the gallery no longer come
  out lying down.** Photos taken with the phone rotated aren't saved
  rotated: the pixels stay as the sensor read them and inside the file
  there is a label (EXIF) saying how much to turn them. The gallery reads
  it, `BitmapFactory` doesn't. Now `PageImageStore` reads it and applies
  the rotation to the copy being looked at — the file isn't touched. Tried
  by building a JPEG with the label "rotate by 90" and looking at it inside
  the app
- **[Bug fix]** **[📱 Mobile]** **The keyboard no longer vanishes** creating,
  deleting or leaving a checkbox. They were two things together: the caret
  was requested on the new field only `if (!imeVisible)`, but right after a
  focus change the keyboard **still shows as open while it is vanishing**,
  so the request was skipped and the keyboard went away; and the effect
  that turns off the caret when the keyboard closes fired even while we
  were moving the caret. Now the keyboard is always requested (requesting
  it when open does nothing) and that effect stops as long as there is a
  focus request travelling. Verified with `dumpsys input_method`:
  `mInputShown=true` after Enter, after leaving the list and after the
  backspace that removes the checkbox

### 🔧 v0.24.2.1 — 21/09/2026 · 20:49 — Enter continues the list even starting from zero
*(Claude Code on the PC)*

- **[Bug fix]** **[📱 Mobile]** **Enter continues the list even when there are
  empty lines below.** Writing `0.` (or any number), then the text, then
  Enter, the new line came out as a paragraph and the list stopped. The
  cause wasn't speed: an empty line inserted among other empty lines is
  **indistinguishable** from its neighbours, and the comparison attributed
  the insertion to the last line of the group, making it inherit the type
  from the wrong block. Now the field also says where the caret is. Found
  with the log after a wrong diagnosis: the full entry, error included, is
  in "Roads already tried"

### ✏️ v0.24.2 — 21/09/2026 · 20:33 — Checkboxes: deleting them and leaving them
*(Claude Code on the PC)*

- **[Bug fix]** **[📱 Mobile]** **The app no longer closes pressing Enter on an
  empty checkbox.** That checkbox becomes a paragraph (right, like Notion),
  but by doing so it changes kind: from a line of its own to flowing text,
  which for the UI is a different place, and the previous field is
  destroyed. If it happens while the keyboard is still attached to that
  field, it keeps asking it where the caret is and Compose 1.6 crashes
  (`LayoutCoordinate operations are only valid when isAttached is true`).
  Now focus is removed **before** the line vanishes, and put back right
  after on the new field. Reproduced deterministically before the fix and
  tried again four times after
- **[Change]** **[📱 Mobile]** **Backspace at the start of a checkbox removes
  the checkbox** and leaves the text, which becomes a normal line — the
  same step the flowing text already does for lists. From there on the line
  merges with the one above like all the others: before checkboxes couldn't
  be deleted in any way from the keyboard
- **[Change]** **[📱 Mobile]** **Backspace at the start of the line below a
  checkbox merges it into the checkbox.** Before nothing happened there: it
  felt like being at the top of the page while above there was still stuff.
  The two things together make it possible to delete backwards across
  checkboxes, without finding walls
- **[Change]** **[📱 Mobile]** To notice the backspace at the start of the
  line, the checkboxes' field now also has **the hidden line break at the
  head** (`RUN_LEAD`), the same mechanism as the joined field: the keyboard
  doesn't send interceptable key events, but if backspace eats that line
  break the text changes, and a change can be seen
- **[Bug fix]** **[📱 Mobile]** **The page no longer jumps bringing the focused
  line to the top.** The automatic scrolling added for the block born
  behind the keyboard used `animateScrollToItem`, which glues the line to
  the top edge: since a flowing text line is as tall as the whole group,
  after an Enter or a merge the checkboxes above ended up off screen and
  seemed gone (they weren't: scrolling was enough, and in the database they
  were all there). Now it scrolls **only if the line is entirely out**, and
  only as much as needed
- **[Bug fix]** **[📱 Mobile]** The square is raised by 4.5dp instead of 3:
  measuring by zooming in on the screen, its centre stayed below the
  capitals' optical centre

### ✏️ v0.24.1 — 21/09/2026 · 20:09 — A numbered list can start from zero
*(Claude Code on the PC)*

- **[Change]** **[📱 Mobile]** **A list can start from zero.** By itself it
  starts from 1, but writing `0.` it starts from 0 and then continues
  normally: 0, 1, 2. It is needed when there is a step zero — a preamble
  before the first real point. A single check forbade it
  (`numberedListPrefixJustTyped` rejected numbers below one): the rest
  already held, because the starting number is a field saved on the block
  (`numberStartsAt`) and the count restarts from there. Verified on the
  phone and in the database: the block is saved with starting number 0,
  and the lines after are `1.` and `2.`

### 🔧 v0.24.0.1 — 21/09/2026 · 19:48 — Checkboxes: in line with the text, with the capital, and Enter doesn't get stuck
*(Claude Code on the PC)*

- **[Bug fix]** **[📱 Mobile]** The square is **smaller and in line with the
  text**. It was Material's `Checkbox`, which carries a 48dp touch zone and
  a 20 square that don't shrink: next to a 24-tall line it looked like a
  button placed there in the middle. Now it is drawn by hand
  (`CheckboxMark`), as tall as the letters, with the touch zone staying
  wide. It also needs to be **raised by 3dp**: centring it on the line
  isn't enough, because inside the line the text isn't in the middle —
  below the baseline there is the space for the tails of g and p, and the
  letters appear higher. Measured on the phone
- **[Bug fix]** **[📱 Mobile]** **The capital at the start of a sentence** now
  arrives in blocks too, checkboxes included: it must be asked of the
  keyboard (`KeyboardCapitalization.Sentences`) and had never been asked.
  Verified by looking at the keyboard: on an empty checkbox it shows
  capital letters and shift on
- **[Bug fix]** **[📱 Mobile]** **Enter + writing quickly no longer mashes
  everything into a single line.** It was the bug where, pressing Enter and
  writing without waiting, the letters all ended up stuck together in the
  previous line, the keyboard closed and the bar above stayed in a wrong
  state. Three different causes, found with the log and fixed one by one:
  the storm of redraws of the renumbering, the new line born behind the
  keyboard and so not existing for focus, and the keyboard's echo that
  split the same piece twice. The first two have their entry in "Roads
  already tried"
- **[Bug fix]** **[📱 Mobile]** **Enter on an empty checkbox** turns it into a
  paragraph — like Notion — but before it left the caret behind and the
  keyboard closed in your face: the block goes from "island" to flowing
  text, which for the UI is another place, and the previous field is
  destroyed. Now focus is called back

### 🆕 v0.24 — 20/09/2026 · 10:58 — The cover moves and zooms
*(Claude Code on the PC)*

- **[New feature]** **[📱 Mobile]** The cover **moves and zooms**: from the
  cover's window there is "Reposition", and from there the finger drags it
  in all directions and the pinch zooms it up to four times. At the top a
  line says how to do it — an image that stops responding to touch without
  explaining why looks broken — and at the bottom there are "Cancel" and
  "Save"
- **[New feature]** **[📱 Mobile]** **The file is never touched.** Three
  numbers end up in the database (`coverScale`, `coverOffsetX`,
  `coverOffsetY`, migration 16→17): the image stays whole at its
  resolution, and the strip acts as a window. The framing can be changed
  endlessly without consuming anything, and removing the zoom you go back
  exactly to the original
- **[New feature]** **[📱 Mobile]** The shift is saved in **fractions of the
  strip**, not in pixels: the same framing applies on any screen, even
  rotating the phone
- **[New feature]** **[📱 Mobile]** The trial framing lives in the screen until
  "Save" is tapped: writing in the database at every millimetre of finger
  would mean no longer being able to cancel, and making the disk write
  sixty times a second
- **[New feature]** **[📱 Mobile]** The image **can't be dragged out** of the
  strip: the limit is computed on how much it really sticks out
  (`coverPanLimit`), because letting it escape would mean being left
  looking at an empty rectangle
- **[New feature]** **[📱 Mobile]** Changing cover the framing **goes back to
  zero**: it had been chosen looking at another image, and on the new one
  it would give a crop decided at random
- **[New feature]** **[📱 Mobile]** When zoomed, the image is **read finer**
  (up to twice the screen's width), otherwise at zoom 2 it would look
  grainy. The cap is set by `PageImageStore`, which doesn't bring into
  memory more than six megapixels: a very large photo, at maximum zoom, may
  stay a bit soft. It is the price for not making the app close

### ✏️ v0.23.1 — 20/09/2026 · 10:48 — The view bar's icons smaller
*(Claude Code on the PC)*

- **[Change]** **[📱 Mobile]** The view bar's icons are **smaller** (20dp
  instead of 24, inside a 40 button instead of 48). The size lives in
  `TOOLBAR_ICON_SIZE` and is used through `ToolbarIconButton`: the icons up
  there must all stay equal to each other, including those that will be
  added, and rewriting the size by hand every time is the sure way for one
  to be different from the others sooner or later

### 🔧 v0.23.0.1 — 20/09/2026 · 10:44 — Renaming a full-screen database is no longer lost
*(Claude Code on the PC)*

- **[Bug fix]** **[📱 Mobile]** **Renaming a full-screen database is no longer
  lost going back.** The page was read only once at opening, and the same
  page is open in two screens together with two different ViewModels: the
  one left in the navigation stack had the old title in hand, and since
  every change saves the **whole** page, at the first thing touched in
  there — turning "Show title" back on was enough — the old title went back
  into the database and the rename vanished. Verified on the phone that
  the new name was really saved and then overwritten, not simply not
  saved. Now the page is **followed over time** (`PageDao.observeById`), in
  both ViewModels: the two screens always see the same thing and neither
  rewrites what it had in hand

### 🆕 v0.23 — 20/09/2026 · 10:30 — "Show title" for databases inside pages
*(Claude Code on the PC)*

- **[New feature]** **[📱 Mobile]** **"Show title"** in the same settings: a
  switch that hides the database's title **when it is inside a page**. It
  serves whoever already wrote the title above as a heading and ends up
  reading it twice. The choice is saved for each database
  (`showEmbeddedTitle`, migration 15→16)
- **[New feature]** **[📱 Mobile]** **Full screen the title is always there**,
  and the switch there doesn't even appear. It isn't an oversight: opened
  alone, the title is the only thing that says where you ended up, and
  being able to remove it would mean being able to reach a screen without
  a name. Verified on the phone: off inside the page, the title vanishes;
  opened the same database full screen, it is still there

### ✏️ v0.22.1 — 20/09/2026 · 10:21 — A single menu for the database: Delete in the settings, "Open as full page" next to Sort
*(Claude Code on the PC)*

- **[Change]** **[📱 Mobile]** Gone the three dots next to the database's
  title. **"Delete" moved into the settings** — the icon with the lines and
  dots — in a group of its own below the view's ones: two different menus
  for the things of the same database were one more place to look in, and
  the second contained only two entries
- **[Change]** **[📱 Mobile]** **"Open as full page" is instead an icon in
  the bar**, right to the right of Sort. The two things look similar but
  aren't: opening full screen is done often and in a hurry, because the
  table inside the page is cramped, while deleting is done once and mustn't
  happen by mistake. The first deserves one tap, the second two. The icon
  appears only where it makes sense: full screen the database is already
  open, and there it isn't there

### 🆕 v0.22 — 20/09/2026 · 10:17 — Sorting the rows (Sort)
*(Claude Code on the PC)*

- **[New feature]** **[📱 Mobile]** Rows can be **sorted by any property**,
  ascending or descending, from the **Sort** icon in the view's bar — the
  one with the three lines getting shorter, as on Notion. The choice is
  saved for each database (`sortColumnId` and `sortDescending`, migration
  14→15)
- **[New feature]** **[📱 Mobile]** The comparison depends on the type: numbers
  are compared **as numbers** (otherwise "10" would come before "9"), dates
  as instants, creation and edit dates from the row itself, and everything
  else alphabetically without distinguishing upper and lower case. Verified
  on the phone with 9, 10 and 100: ascending gives 9-10-100 and descending
  100-10-9, not the alphabetical order 10-100-9
- **[New feature]** **[📱 Mobile]** You can also sort by **the page's name**,
  which is a column of the table but not a property of the database: it has
  a reserved identifier (`SORT_BY_NAME`) that no real id can have
- **[New feature]** **[📱 Mobile]** **Empty cells stay at the bottom in both
  directions.** It isn't an oversight: reversing the order you expect to
  see the last value at the top, not a line of rows with nothing written
- **[New feature]** **[📱 Mobile]** The Sort icon is **lit blue** when there is
  a sorting: otherwise you wouldn't know the rows are no longer in the
  order they were created. The direction is changed by tapping the active
  sorting inside the window — they are two states of the same thing, not
  two different entries

### 🆕 v0.21 — 20/09/2026 · 09:38 — Page icons and covers
*(Claude Code on the PC)*

- **[New feature]** **[📱 Mobile]** Every page can have an **icon** and a
  **cover** chosen by the user, the root page included. You tap the icon
  (or the cover, or "Add cover" when there is none) and choose between
  **Upload** and **Link**. New fields `iconImage` and `coverImage`
  (migration 13→14): without an image you keep seeing the usual emoji
- **[New feature]** **[📱 Mobile]** Upload uses the **system photo picker**,
  which doesn't ask permission to read the whole gallery: the user picks a
  photo and the app sees only that one. On the phone you read "notE will
  only have access to the photos you select"
- **[New feature]** **[📱 Mobile]** **The link is downloaded only once and the
  file stays on the phone.** It is the important decision of this feature:
  keeping the address and downloading the image every time would have
  meant an app that without network shows holes, and that loses the images
  when the site removes them. This way instead it is a copy like
  everything else. The price, declared: if the image changes on the other
  side, ours doesn't
- **[New feature]** **[📱 Mobile]** That is why the **INTERNET** permission
  appeared in the manifest — the only thing the app goes online for, only
  when a link is pasted, never by itself
- **[New feature]** **[📱 Mobile]** The database saves the file's **name** and
  not the whole path: the app's folder changes place between one
  installation and another, or when restoring a backup, and a path written
  in the database would become a pointer to nothing
- **[New feature]** **[📱 Mobile]** Replacing or removing an image **deletes
  the old file**. Without it, the folder would fill with images nobody can
  reach anymore. Verified: after removing icon and cover the folder is
  empty
- **[New feature]** **[📱 Mobile]** Images are loaded already shrunk to what is
  really needed, with the size taken from the box containing them. A
  twelve-megapixel photo in memory is forty-eight megabytes: loading it
  whole to show it one centimetre wide would make the app close
- **[New feature]** **[📱 Mobile]** The "page link" block also shows the
  image, if the page has one: otherwise the same page would have two
  different faces depending on where you look at it

### ✏️ v0.20.1 — 19/09/2026 · 22:35 — Keyboard closed, the caret goes off
*(Claude Code on the PC)*

- **[Change]** **[📱 Mobile]** Once the keyboard is closed, **the caret goes
  off** — in the editor, in the title and in database cells, both embedded
  and full screen. Before the back gesture closed only the keyboard and
  left the caret blinking inside the field, as if you were still writing
  there. The signal isn't the gesture but the keyboard closing, however it
  is closed: with the back gesture the keyboard eats it and nothing reaches
  the app (see "Roads already tried"). There is a 250 ms wait before
  turning off the caret, because the Samsung keyboard vanishes and
  reappears for an instant during focus changes: if focus were removed in
  that instant, the Enter that passes from one block to the next would
  break. If the keyboard comes back, the wait is cancelled. Tried by
  writing three lines separated by Enter: none is lost

### 🆕 v0.20 — 19/09/2026 · 22:33 — Time in the Date property
*(Claude Code on the PC)*

- **[New feature]** **[📱 Mobile]** The date property can have **the time
  too**, as on Notion: an "Include time" switch in the date's window, off
  by default. On, "Start time" appears and — if there is an end date — "End
  time". Off, the date keeps counting as the whole day, which is what the
  vast majority of pages need
- **[New feature]** **[📱 Mobile]** The cell's value didn't change shape: it
  was already milliseconds, and milliseconds have the time inside. A third
  field was added, `t`, which says whether it **counts**. It isn't guessed
  from the milliseconds: guessing it would mean saying "if it doesn't fall
  at midnight then there is a time", and a page really set at midnight
  would lose its own. The values written before don't have the third field
  and count as "without time", which is what they were — no migration
  needed
- **[New feature]** **[📱 Mobile]** The date's window became a pop-up window
  like Notion's, with the buttons at the head. At the bottom, under the
  calendar and the switches, they ended up off screen: you chose a date
  without being able to confirm it. Same lesson as the properties' window.
  There is also **Clear**, which before was obtained only by confirming
  without having chosen anything
- **[Bug fix]** **[📱 Mobile]** Material's picker works in **UTC midnight**,
  not local, and the code took its milliseconds and saved them as they
  were. In Italy it wasn't noticed — UTC midnight is one or two in the
  morning of the same day — but west of Greenwich the date would have
  moved back by a day. Now the conversion always goes through the day, in
  both directions
- **[New feature]** **[📱 Mobile]** The timeline places the bars at the
  **instant** and not at the day: with the time on, at the "hours" zoom a
  page starting at 14:30 sits in mid-afternoon. The position is in days
  with the fraction, not in whole days. A page without time keeps taking
  the whole day, and one with time but without end is an instant — it
  shows anyway because the bars have a minimum width
- **[New feature]** **[📱 Mobile]** The order of rows in the calendar and in
  the timeline also looks at the instant: two pages of the same day at
  different times have a natural order, and that is it

### 🆕 v0.19 — 19/09/2026 · 22:15 — Seven Timeline zooms
*(Claude Code on the PC)*

- **[New feature]** **[📱 Mobile]** **Seven zooms** as on Notion — Hours, Day,
  Week, Bi-week, Month, Quarter, Year — from the same window ("Select a
  zoom level"), saved for each database (`PageEntity.timelineZoom`,
  migration 12→13). "5 years" is missing: at that scale a month is two
  millimetres wide and nothing can be told apart anymore. The model is a
  single one for all: **the width of a day**. Everything follows from
  there — where a bar starts, how long it is, where now falls — and the
  axis's boxes are as wide as the days they last, so a twenty-eight-day
  month is narrower than a thirty-one one and the bars stay glued to their
  dates instead of sliding against the labels
- **[Bug fix]** **[📱 Mobile]** At the wide zooms the days' numbers read wrong
  — "2" where "25" was written. A day's box is narrower than two digits
  and the text was cut in half. Now it is measured freely and spills over
  the neighbouring boxes, which are silent anyway
  (`wrapContentWidth(unbounded = true)`), and the number is written one box
  every so many instead of on all
- **[Bug fix]** **[📱 Mobile]** At the "Quarter" zoom the weeks' labels
  overlapped: a one-week box is 22 points wide and "13 Sep" wants twice as
  much. Now they carry only the day, every other week; the month is said
  by the header fixed above the axis
- **[Bug fix]** **[📱 Mobile]** At the "hours" zoom the timeline opened at
  midnight, that is half a day away from what is happening. The target of
  the opening (and of the Today button) is the **instant** of now, not
  today's midnight

### 🆕 v0.18 — 19/09/2026 · 21:55 — The Timeline view
*(Claude Code on the PC)*

- **[New feature]** **[📱 Mobile]** **Timeline**: an axis of days scrolling
  horizontally and a row per page, with the bar as long as the days it
  covers. It is the calendar unrolled — there time wraps every week and a
  long page breaks into several pieces, here time is a single line and the
  bar is whole, which is the whole point of a timeline: comparing
  durations and overlaps at a glance
- **[New feature]** **[📱 Mobile]** The rows follow **the same order as the
  calendar** — pages by date, the first at the top, with the same rules for
  ties — so switching from one view to the other you don't lose the
  thread. Here however every row necessarily has its page, so no empty
  rows are left
- **[New feature]** **[📱 Mobile]** The names sit in a column fixed on the
  left: scrolling by months the bars go away, and without the names next to
  them you would no longer know whose is the one you are looking at
- **[New feature]** **[📱 Mobile]** The month sits in a header fixed above the
  axis and follows the first day in view. Written on the axis it would sit
  at the start of its stretch and scrolling a few days it would vanish to
  the left, leaving a line of numbers without knowing which month they
  belong to. It reads the scroll position **inside itself**, so while the
  finger drags only that label is redrawn and not the whole timeline
- **[New feature]** **[📱 Mobile]** Weekends are recognised by the background
  instead of a letter, and the days carry only the number: the boxes are
  all composed together, so every text less is one text less multiplied by
  the length of the axis

### 🆕 v0.17 — 19/09/2026 · 20:14 — Today in the calendar, always in its place
*(Claude Code on the PC)*

- **[New feature]** **[📱 Mobile]** **Today** button to the right of the three
  mode buttons: it brings back to today from any distance, in all three.
  It is there **always**, even when you are already on today and it would
  do nothing: it had been made to appear only from far away, but a button
  that appears and disappears forces you to look for it every time and
  makes the row change look by itself while browsing. The user's choice,
  and it is the right one

### ✏️ v0.16.1 — 19/09/2026 · 20:07 — Every page has its row in the calendar, and the week no longer jerks
*(Claude Code on the PC)*

- **[Bug fix]** **[📱 Mobile]** Changing week with the finger the pages
  vanished and reappeared with a jerk. At the end of the scroll two things
  change — the week shown and the reset of the shift — and they were
  written one after the other: they could end up in two different frames,
  and in the one in between you saw the old week already in the centre, or
  the new one still at the side. Now the two writes are applied in one go
  (`Snapshot.withMutableSnapshot`), so no frame exists that sees one
  without the other. The shift became a normal state instead of an
  `Animatable` precisely to be able to do it: two values become true
  together only if they are both normal states
- **[Bug fix]** **[📱 Mobile]** The jerk however didn't end there, and the real
  cause was another: **the animation's spring bounced**. Launched by the
  finger's push it overshot the target and came back, and overshooting the
  target means uncovering the edge beyond the next week, where nothing is
  drawn. With `input swipe` it didn't show, because a synthetic drag has a
  much weaker push than a finger: that is why the first two times it
  seemed fine. Now the spring is `DampingRatioNoBouncy` — it never
  overshoots the target — and the drag is limited to one week per side, so
  not even the finger can uncover the void. Verified by recording the
  screen and reading the number of the first day **frame by frame**: the
  sequence rises monotonically up to the new week and stops there, without
  passing it and coming back.
  **Note for whoever rereads those recordings**: `screenrecord` closes the
  file by re-emitting an old frame, so the last frame often shows the
  starting week. It is an artefact of the recording, not of the app — it
  happens in all three recordings made, always at the end
- **[Change]** **[📱 Mobile]** The calendar's pages behave like **stacked
  blocks**: sorted by date from top to bottom, each with its row, and **the
  row never changes**. When a page's date ends, its row stays **empty** in
  the following weeks instead of being taken by the one below — so the bars
  don't reshuffle and following one with the eye is immediate.
  The row is decided **once over the whole database** and not week by
  week: that is the difference. Deciding it within the week would mean
  making it depend on who else is there, that is making it change as soon
  as a page enters or leaves — and it was exactly the defect, with the same
  page at the top in the week where it was alone and lower in the next.
  The order looks at the page's date, not at the day the bar enters the
  week: a page coming from before stays above one starting on Monday. With
  equal dates the order is still always the same, otherwise two pages of
  the same day would swap places at every redraw
- **[Change]** **[📱 Mobile]** Rows are counted over **the whole database**,
  not over the period being looked at: a page keeps its row in every week,
  every month and every year, forever. It is an explicit choice of the
  user, made knowing its price. Counting them over the period (as was done
  before) makes them recompact at every month change: no page steals
  another's row, but the same page finds itself at a different height
  depending on what is around it. Counting them over everything costs empty
  space — the tenth page in date order sits on the tenth row even in the
  weeks where it is the only one, with nine empty rows above — and every
  week is as tall as the whole grid, because the rows' grid is a single
  thing
- **[Change]** **[📱 Mobile]** The week's scrolling is now a
  `HorizontalPager`, the same component pages are flipped with everywhere
  on Android. Before it was written by hand: three weeks side by side, an
  offset followed with the finger and a spring to make it land. Redoing the
  physics of scrolling means also redoing the fling, the snapping to the
  page, the gesture grabbed mid-run and the release speed — each of those
  things is a way of getting it wrong, and the right one already exists.
  The weeks on the sides are prepared before the finger moves
  (`beyondBoundsPageCount = 1`), and the computation of the pieces and
  their rows is memoised instead of being redone at every frame
- **[Bug fix]** **[📱 Mobile]** In the table, tapping a date cell opened the
  calendar **only by hitting the dash**, a few points wide: from any other
  point of the cell the tap fell on the page under the database, which
  responded by starting to write at the bottom. The tappable zone was the
  text instead of the cell. Now it is the whole cell, with the side margin
  moved **inside** it so as not to leave out even the edges
  (`CellContent`, `cellPadding` parameter)

### 🆕 v0.16 — 19/09/2026 · 15:35 — The year view, and the week flipped with the finger
*(Claude Code on the PC)*

- **[New feature]** **[📱 Mobile]** **Year view**, to the left of Month and
  Week: twelve miniature months, three per row, the whole year in one
  screen. No bars — at one centimetre per month they couldn't be read — but
  the days with at least one page are circled, and that is enough to show
  where the work concentrates. Tapping a month opens it up close, which is
  the only sensible thing to do with a month one centimetre tall
- **[New feature]** **[📱 Mobile]** The three modes replaced the old yes/no
  `calendarWeekView` with `PageEntity.calendarMode` (migration 11→12),
  which carries over the previous value. The old column **stays in the
  database**: SQLite can drop a column only from 3.35, more recent than the
  minimum the app supports, and recreating the pages table to remove an
  unused field is a risk not worth the gain. Same choice already made for
  `numberResetHere`
- **[New feature]** **[📱 Mobile]** In the week you pass from one to the other
  **dragging with the finger**. Inside a window one week wide three sit side
  by side — the previous, the one shown, the next — and the drag moves the
  line, so under the finger you really see the next week arriving instead
  of seeing it appear suddenly on release. On release, past a quarter of
  the screen or with a decisive push, the line finishes scrolling and the
  week shown changes; the offset goes back to zero at the same instant, so
  the same content stays in the centre and no jump is seen. The three weeks
  are equally tall on purpose: if each took its own height, dragging you
  would see the row grow and shrink
- **[Bug fix]** **[📱 Mobile]** At the first drag the week appeared for an
  instant and then everything vanished. The three weeks were a `Row` three
  screens wide, and there **two width constraints added up**: inside a
  `Row` a child without weight is measured with *the space left over*, and
  `Modifier.width` lets itself be squeezed inside the constraint it
  receives — the first week took the whole width and the other two were
  left with zero. Switching to `requiredWidth` they became visible, but the
  second problem came out: **a line wider than the box containing it isn't
  placed where you expect**, it came out already shifted by a week on its
  own, and on top the finger's offset added up — so the header said one
  week and the grid showed another. Gone the `Row`: now the three weeks are
  overlapped in the same box, each exactly as wide as it, and each shifts
  itself by one width to the left, zero, one to the right. No measure to
  negotiate and no placement to guess. Same family as the old bug of the
  invisible table lines: **when a measure seems to vanish, it is almost
  always the container giving the child a different constraint from the one
  you imagine**

### 🆕 v0.15 — 19/09/2026 · 14:56 — Bars as long as the days, and the week view
*(Claude Code on the PC)*

- **[New feature]** **[📱 Mobile]** **Bars as long as the days covered**. Every
  week computes by itself which pages cross it and lays them on lanes, the
  longest at the top, so that two pages overlapping in time don't end up
  one on top of the other (`calendarLanes`). The widths are **weights, not
  measures**: a three-day bar takes three sevenths of the row on any
  screen. A page across two weeks produces two pieces, and the side where it
  continues is drawn square instead of rounded
- **[New feature]** **[📱 Mobile]** **Week view**, next to the month one
  (`PageEntity.calendarWeekView`, migration 10→11). In the month a week's
  height is fixed — six weeks must fit on the screen — and the bars beyond
  the third become a "+N"; in the week the height follows how many pages
  there really are, because the space is there and hiding them would be
  gratuitous

### 🆕 v0.14 — 19/09/2026 · 14:42 — Start and end dates
*(Claude Code on the PC)*

- **[New feature]** **[📱 Mobile]** **Date ranges**: a date cell can have a
  start and an end ("Date started"/"Date finished"), chosen with Material
  3's range picker. They are saved as two numbers separated by a dash in
  the same cell, so the column's type doesn't change and the single dates
  already written keep being read

### 🆕 v0.13 — 19/09/2026 · 14:38 — The Calendar view
*(Claude Code on the PC)*

- **[New feature]** **[📱 Mobile]** **Calendar**: pages placed on the day of
  their date property, month by month, with the days outside the month
  dimmed and today highlighted. The property is chosen from Settings → Date
  property (`PageEntity.calendarDateColumnId`, migration 9→10); if none is
  chosen the first date there is gets used
- **[Bug fix]** **[📱 Mobile]** In Calendar and Board `+ New page` seemed to
  create nothing: the page was born without date and without tag, that is
  **outside what was being looked at** — you found it only switching to
  Table. Now in the calendar it is born dated today and the calendar goes
  back to the current month to show it; in the board it is born already
  assigned to the column
- **[Bug fix]** **[📱 Mobile]** When the property the view rests on doesn't
  exist, `+ New page` vanished and only a message stayed: it created
  invisible pages. Now the message offers to create the missing property
  (`MissingPropertyNotice`), and the button comes back as soon as it exists
- **[New feature]** **[📱 Mobile]** Tapping a day creates there a page already
  dated. The days sit in a layer below and the bars in one above: the empty
  spaces between one bar and another let the tap through to the day, while
  tapping a bar opens its page

### ✏️ v0.12.1 — 19/09/2026 · 14:11 — The view button under the title changes the view
*(Claude Code on the PC)*

- **[Change]** **[📱 Mobile]** The view button under the title opens the
  layout picker (before it was an inert label)

### 🔧 v0.12.0.1 — 19/09/2026 · 14:06 — Tapping below the database writes below, and the text doesn't end up behind the keyboard
*(Claude Code on the PC)*

- **[Bug fix]** **[📱 Mobile]** Tapping below the last block brought you back to
  writing *above* it when it wasn't writable (database, divider, table,
  link): the last **writable** block was looked for instead of simply the
  last one. Now a new one is created after it
- **[Bug fix]** **[📱 Mobile]** Writing at the bottom of a long page the caret
  ended up behind the keyboard and the page stayed still. Cause: a text
  field that **grows in height** instead of scrolling inside itself asks
  to be made visible only when it takes focus, not at every caret move —
  and the joined field is as tall as the whole group it contains. Now the
  field computes where the caret is on the screen and asks the page to
  show **that point**.
  **Failed attempt before getting there**: scrolling the list to the item
  containing the block. It doesn't work because the whole flowing text is
  a single item: you end up at the top of that, that is far from the caret.
  Moreover that scrolling fought with the keyboard's animation and left a
  gap between bar and keyboard

### 🆕 v0.12 — 19/09/2026 · 11:29 — Databases inside pages, with "Open as full page" and Delete
*(Claude Code on the PC)*

- **[New feature]** **[📱 Mobile]** A database created from the "+" menu
  **appears and is edited inside the page**, as a block among the others,
  instead of opening as a screen of its own. The "..." menu next to its
  name opens it full screen or deletes it. The database screen's content
  was extracted into a single piece (`DatabaseContent`) used in both
  places: two copies would have meant fixing everything twice
- **[New feature]** **[📱 Mobile]** Deleting an embedded database also deletes
  the database page with columns and rows. Different from a page link,
  where the block points to something living on its own: here the block
  **is** that content, and leaving its data would mean leaving it where it
  can no longer be reached
- **[Bug fix]** **[📱 Mobile]** The embedded database doesn't scroll on its
  own: it takes the height it needs and lets the page scroll. Two areas
  scrolling one inside the other steal the gesture from each other

### 🆕 v0.11 — 19/09/2026 · 11:27 — Automatic capital in titles, and the database as wide as the page
*(Claude Code on the PC)*

- **[New feature]** **[📱 Mobile]** Automatic capital on titles (page,
  database, a row's name) and on text cells. Not on email, URL and phone,
  where it would have to be deleted every time
- **[Bug fix]** **[📱 Mobile]** The page's side margin was applied by the list
  to all blocks: now each block puts it on itself, so the database can use
  the whole width as in full screen (`PAGE_SIDE_PADDING`)

### ✏️ v0.10.1 — 19/09/2026 · 11:11 — Select properties become tags, as on Notion
*(Claude Code on the PC)*

- **[Change]** **[📱 Mobile]** Select properties became real tags: coloured
  pills, a dedicated window with "Create a tag" and "Select an existing
  option", colours assigned in rotation (`TAG_COLORS`). An empty cell stays
  empty, without dashes

### 🆕 v0.10 — 19/09/2026 · 10:57 — The Board view
*(Claude Code on the PC)*

- **[New feature]** **[📱 Mobile]** **Board**: a column for each value of a
  single-select property, rows as cards, the column of the unsorted ones at
  the far right, and `+ New` creating the row **already assigned** to that
  column. The property is chosen from Settings → Group by
  (`PageEntity.boardGroupColumnId`, migration 8→9); if none is chosen the
  first suitable one is used. Only single select: with multiple a row would
  sit in several columns together. **There is no dragging** of cards
  between columns yet
- **[Bug fix]** **[📱 Mobile]** A select column just created was a dead end:
  the cell showed only a dash and the options could be written only at
  creation, in a field that sat **below** the list of the eleven types and
  so off screen. Now they are created from the cell, and the field is above
  the list

### ✏️ v0.9.1 — 19/09/2026 · 10:31 — Notion's greys everywhere
*(Claude Code on the PC)*

- **[Change]** **[📱 Mobile]** Three greys sampled from Notion's screenshots:
  `#191919` for pages, `#202020` for the windows rising from the bottom,
  `#252525` for the boxes grouping the entries inside those windows
- **[Change]** **[📱 Mobile]** The app is always dark and **no longer uses
  dynamic colours** (Material You). They were a deliberate choice, but they
  tinted purple or green exactly the backgrounds that must stay those
  precise greys. Before the editor was fixed black and the rest followed the
  phone: half the app one way and half the other

### 🔧 v0.9.0.2 — 19/09/2026 · 10:22 — At startup the page right away, without the white flash
*(Claude Code on the PC)*

- **[Bug fix]** **[📱 Mobile]** White flash at startup: the window's theme was
  `Theme.Material.Light`, and its background is the one Android uses for
  the splash screen. **That screen can't be disabled since Android 12**, it
  can only be made to take the app's colours: background `#191919` and
  transparent icon (`values-v31/themes.xml`). The cold start measures 389
  ms, so there is no slowness to hide

### 🔧 v0.9.0.1 — 19/09/2026 · 10:16 — The table's grid, like a spreadsheet
*(Claude Code on the PC)*

- **[Bug fix]** **[📱 Mobile]** The table's horizontal lines were invisible:
  inside a container scrolling horizontally the available width is
  unlimited, and a line asking for "the whole width" gets zero. Now the
  table's width is computed

### 🆕 v0.9 — 19/09/2026 · 10:09 — Views: Table and List, from Settings → Layout
*(Claude Code on the PC)*

- **[New feature]** **[📱 Mobile]** The view is chosen from Settings → Layout,
  with the grid of boxes as on Notion, and is saved for each database
  (`PageEntity.databaseLayout`, migration 7→8). Besides **Table** there are
  **List** (the rows without grid; you tap and the page opens) and
  **Board**
- **[Bug fix]** **[📱 Mobile]** In the properties window the Cancel and Add
  buttons sat at the bottom, after the list of types: you chose a type and
  could no longer see how to confirm. Now they are in a fixed header. And
  with an empty name Add did nothing **silently**: now the property takes
  the type's name

### 🆕 v0.8 — 19/09/2026 · 09:58 — The database one point at a time: settings, a row's actions, Done in titles
*(Claude Code on the PC)*

- **[Bug fix]** **[📱 Mobile]** The long press on a row never opened the
  actions: the cell's text field took the gesture to select words, and the
  selection handles were left hanging over the windows. The name is plain
  text until it is tapped, so a single detector handles tap and long press
- **[Bug fix]** **[📱 Mobile]** The title of a row's page and the row's name
  were two disconnected values: renaming from inside, going back you found
  the old name. Now they are aligned in both directions

### 🆕 v0.7 — 18/09/2026 · 23:14 — Databases redone from scratch, like Notion's
*(Claude Code on the PC)*

- **[New feature]** **[📱 Mobile]** `DatabaseViewScreen` rewritten from scratch
  following Notion's mobile UI: large editable title inside the content (not
  in the bar), a line with the active view ("Table") and the button to add a
  page, headers with the type's icon, `+ Add property` after the columns,
  `OPEN` on every row to open it as a page, `+ New page` at the bottom.
  **The data store wasn't touched**: columns/rows/cells with the row linked
  to a page *already are* Notion's model, it was the UI that didn't look
  like it
- **[New feature]** **[📱 Mobile]** Properties window popping up from the
  bottom, as on Notion: name, list of types with search and icons, and —
  when editing an existing column — move left/right and delete. It opens by
  tapping a column's header or `+ Add property`. Before a created column
  was immutable: getting the name or type wrong forced you to redo it,
  losing the data
- **[New feature]** **[📱 Mobile]** Six new property types: Multi-select, URL,
  Email, Phone, Created time, Last edited time. Those shown by Notion and
  **not** doable here are listed in Known limits
- **[New feature]** **[📱 Mobile]** `updatedAt` on rows (migration 6→7) for
  "Last edited time": it is updated both changing the title and editing a
  cell. It allows null, because for the rows already existing the real date
  can't be rebuilt — in that case the creation date is shown
- **[Bug fix]** **[📱 Mobile]** A column added after deleting one in the
  middle got an `orderIndex` already taken (the number of columns was
  counted instead of taking the maximum plus one). Two columns with the same
  index can't be reordered: the swap changes nothing. The same defect is
  still present in `addRow`, where for now it isn't noticed because rows
  can't be reordered
- **[Bug fix]** **[📱 Mobile]** Changing a row's title rewrote the whole entity
  starting from the copy in the UI's hand: writing while the linked page was
  being created could reset `linkedPageId`, and the row lost its page. Now
  the save touches only the title (same lesson as the tap on the number in
  lists)

### 🔧 v0.6.0.1 — 18/09/2026 · 22:51 — Backspace on an indented line removes one indent at a time
*(Claude Code on the PC)*

- **[Bug fix]** **[📱 Mobile]** Backspace at the start of an indented line (a
  `3.3` under a `3.2`) now removes **one indent at a time** down to the
  margin, and only then merges the line with the one above: it is the "one
  step at a time" of v0.4.2

### 🆕 v0.6 — 18/09/2026 · 22:43 — Multi-level numbered lists: 1.1, 1.2
*(Claude Code on the PC)*

- **[New feature]** **[📱 Mobile]** Multi-level numbering driven by the indent:
  `1.` at the margin, `1.1` `1.2` moving right, `1.1.1` at two indents.
  Moving an item renumbers it, and with it all those that follow. The count
  keeps a counter per level: going back up, the deeper ones are forgotten,
  and that is why `2.1` restarts from 1
  (`PageEditorViewModel.numberedListLabel`)
- **[Bug fix]** **[📱 Mobile]** Huge vertical spaces above and below indented
  lines. The indent was done with a `ParagraphStyle`, but applying a
  paragraph style to part of an `AnnotatedString` breaks the text into
  separate paragraphs, and the line break ending at the boundary is rendered
  as one extra empty line. Now the indent is spaces inside the prefix
  (`INDENT_SPACES`) and the text stays a single paragraph
- **[Bug fix]** **[📱 Mobile]** The indent button (and move up/down, and
  delete) acted on the previous line if pressed right after an Enter: the
  new line exists in the field before existing in the blocks list, so it
  couldn't be matched and the "focused block" stayed one behind. Now the
  computation is redone also when the blocks list changes, not only when
  writing
- **[Bug fix]** **[📱 Mobile]** For an instant after Enter the caret blinked at
  the left margin of the new line, before jumping after the number. The line
  exists in the field before its block arrives from the database, so for a
  moment it was drawn without number. Now the prefix of lines not yet saved
  is predicted, inheriting it from the line above with the same rule as
  `updateRun`. It is a prediction only of what is drawn: it touches neither
  the data nor the editing, so in the worst case it gets a number wrong for
  one frame

### ✏️ v0.5.1 — 18/09/2026 · 22:24 — Tapping the number restarts the list, in the text too
*(Claude Code on the PC)*

- **[Change]** **[📱 Mobile]** Tapping a list's number works in the joined text
  too, no longer only inside toggles: the number is drawn by the visual
  transformation, so an invisible zone is put on top of it, positioned with
  the `TextLayoutResult`. The prefixes' computation was extracted into
  `runPrefixes` because whoever draws them and whoever makes them tappable
  must necessarily agree
- **[Bug fix]** **[📱 Mobile]** Tapping the number deleted that line's text.
  `pointerInput` doesn't regenerate when only the block's content changes
  (the key was the id), so the tap lambda kept a copy of the entity as it
  was at birth — empty — and saving it overwrote the text.
  `toggleNumberReset` now takes the id and updates only the numbering
  columns. **General rule: don't capture entities inside a lambda that
  doesn't recompose; pass the id and reread.**

### 🆕 v0.5 — 18/09/2026 · 22:14 — Writing "1." at the start of a line creates a numbered list
*(Claude Code on the PC)*

- **[New feature]** **[📱 Mobile]** Writing `1.` (or `5.`, or any number) and
  nothing else, at the start of a block, creates a numbered list starting
  from that number. It fires only if the number is the only thing written
  in the block: `abc 1.` doesn't convert anything, as asked
- **[New feature]** **[📱 Mobile]** Numbered lists starting from a chosen
  number: new field `numberStartsAt`, which replaces the old
  `numberResetHere` (it was the special case "restart from 1")
- **[New feature]** **[📱 Mobile]** First real schema migration (5→6) and
  removal of `fallbackToDestructiveMigration`. From here on the data on the
  phone survives schema changes

### 🔧 v0.4.2.1 — 18/09/2026 · 22:02 — Enter breaks the line and the list continues
*(Claude Code on the PC)*

- **[Bug fix]** **[📱 Mobile]** The first character written after an Enter
  ended up on the previous line. Cause: the "tap the empty space below the
  last block to write" area used `Modifier.clickable`, which in Compose
  fires **also with the Enter key** when an inner element has focus. Every
  line break therefore also started `focusLastBlock()`, which after 60 ms
  brought the caret back to the last block *known at the time of the call* —
  that is the old one, because the new block hadn't been saved yet. Now that
  area uses `detectTapGestures`, which responds only to real taps.
  Diagnosed by reading the log on the device: the Enter was correct (caret
  at 2), and 79 ms later a focus request arrived bringing it back to 1

### ✏️ v0.4.2 — 18/09/2026 · 21:19 — Backspace at the start of a line takes the line apart one step at a time
*(Claude Code on the PC: the first thing done on the project arrived from
the other chat, chosen by the user among the README's «Known limits»)*

- **[Change]** **[📱 Mobile]** Backspace at the start of a line "takes apart"
  the line one step at a time instead of merging it right away with the one
  above, as in Notion: first it removes the list marker, then it removes one
  indent at a time down to the margin, and only then merges. It applies
  **also to the first item** — the old limit at the top of "Known limits" is
  gone. Recognised without intercepting keys: on the lines after the first
  because the line break vanishes exactly where the caret is, on the first
  line because the `RUN_LEAD` vanishes and the caret ends up at zero, a
  position otherwise unreachable. The decision lives in the ViewModel
  (`backspaceAtLineStart`), not in the UI, because only there the group of
  blocks is the up-to-date one

### ✏️ v0.4.1 — 17/09/2026 · 22:37 — The icon from the user's photo and the name "notE"
*(in the other chat, «Home»: the date and time are those of the files of
the icon and of the app's name)*

- **[Change]** **[📱 Mobile]** Adaptive icon from a photo of the user; app
  name changed to "notE"

### 🔧 v0.4.0.2 — between 8 and 18/09/2026 · time not recorded — Black background and bar of the same colour
*(in the other chat, «Home», between 8 and 18/09/2026: of these changes
neither the date nor the time nor the order is left, and they are in the
order the README of 18/09 listed them)*

- **[Bug fix]** **[📱 Mobile]** Fixed black background with white text and caret
- **[Bug fix]** **[📱 Mobile]** Bar above the keyboard of the same colour as the
  background, instead of a grey that created a visible seam

### 🔧 v0.4.0.1 — between 8 and 18/09/2026 · time not recorded — Other bug fixes: divider, tables, "+" menu
*(in the other chat, «Home», between 8 and 18/09/2026: of these changes
neither the date nor the time nor the order is left, and they are in the
order the README of 18/09 listed them)*

- Crash when focus ended up on a divider block
- Resizing a table deleted the text already written
- Endless open/close loop of the "+" menu

### 🆕 v0.4 — between 8 and 18/09/2026 · time not recorded — Databases: the calendar for dates, rows as pages
*(in the other chat, «Home», between 8 and 18/09/2026: of these changes
neither the date nor the time nor the order is left, and they are in the
order the README of 18/09 listed them)*

- **[New feature]** **[📱 Mobile]** A real date picker (calendar)
- **[New feature]** **[📱 Mobile]** Opening a row as a page
- **[New feature]** **[📱 Mobile]** Deleting columns with a long press

### 🆕 v0.3 — between 8 and 18/09/2026 · time not recorded — Keyboard and interaction: Enter, backspace and the Samsung keyboard
*(in the other chat, «Home», between 8 and 18/09/2026: of these changes
neither the date nor the time nor the order is left, and they are in the
order the README of 18/09 listed them)*

- **[Bug fix]** **[📱 Mobile]** Changes silently discarded when the blocks list
  known to the UI was stale — the deep cause of Enter to be pressed twice,
  lists that didn't continue, type inherited at random
- **[Bug fix]** **[📱 Mobile]** Enter that didn't break the line at the first
  press (too aggressive resync with the database)
- **[Bug fix]** **[📱 Mobile]** Block type inherited from the wrong block
  pressing Enter; headings spreading to the next line
- **[Bug fix]** **[📱 Mobile]** Type change from the "+" menu applied to the
  wrong block while writing
- **[Bug fix]** **[📱 Mobile]** Backspace on an empty block: two failed attempts
  to intercept it as a key, then removed — the button in the bar stays
- **[New feature]** **[📱 Mobile]** Tap anywhere below the title to write
- **[Bug fix]** **[📱 Mobile]** Duplicate characters and keyboard closing by
  itself (mitigations for the Samsung keyboard)
- **[Bug fix]** **[📱 Mobile]** Smoother selection: no database save at every
  move of the selection

### 🆕 v0.2 — between 8 and 18/09/2026 · time not recorded — Aa, Undo/Redo, numbering and indents
*(in the other chat, «Home», between 8 and 18/09/2026: of these changes
neither the date nor the time nor the order is left, and they are in the
order the README of 18/09 listed them)*

- **[New feature]** **[📱 Mobile]** Inline formatting "Aa"
  (bold/italic/underline/strikethrough), then extended to the joined text
- **[New feature]** **[📱 Mobile]** Undo/Redo
- **[New feature]** **[📱 Mobile]** Numbering reset in lists
- **[New feature]** **[📱 Mobile]** Indent/outdent and move up/down

### ✏️ v0.1.2 — between 8 and 18/09/2026 · time not recorded — Structure and navigation: the root is a page, the "+" in the bar
*(in the other chat, «Home», between 8 and 18/09/2026: of these changes
neither the date nor the time nor the order is left, and they are in the
order the README of 18/09 listed them)*

- Removed the Home as a separate screen: the root is now a normal, editable
  page
- Removed the floating "+" button — replaced by the Page/Database blocks in
  the bar's "+" menu
- "+" menu redrawn as an icon grid on two columns

### ✏️ v0.1.1 — between 8 and 18/09/2026 · time not recorded — The text in a single field: the editing model rethought
*(in the other chat, «Home», between 8 and 18/09/2026: of these changes
neither the date nor the time nor the order is left, and they are in the
order the README of 18/09 listed them)*

**[New feature] The text editing model rethought** — the biggest change of
the development. See "Deliberate design decisions" above for the full
context.

### 🆕 v0.1 — 08/09/2026 · 09:19 — notE is born
*(in the other chat, «Home»: the date and time are those of the project's
first files, `build.gradle.kts` and `settings.gradle.kts`)*

- **[New feature]** **[📱 Mobile]** **notE**: a Notion-style notes and
  databases app for Android, with **all data saved on the phone** (SQLite
  via Room): no network, no account, no cloud. The project's internal name
  is NotionLocal

## Old numbering

**Until 21/10/2026 versions had another number**: 🆕 v0.N for a request
that brought something new or removed something, 🔧 v0.N.1 for one that
only fixed. That day, at the user's request, the Changelog was reread from
the start and renumbered as it is now (see the start of the Changelog). The
commits made before carry the old number in the message; here, for each,
the current one:

v0.1 → v0.1 · v0.2 → v0.1.1 · v0.3 → v0.1.2 · v0.4 → v0.2 · v0.5 → v0.3 · v0.6 → v0.4 · v0.6.1 → v0.4.0.1 · v0.6.2 → v0.4.0.2 · v0.7 → v0.4.1 · v0.8 → v0.4.2 · v0.8.1 → v0.4.2.1 · v0.9 → v0.5 · v0.10 → v0.5.1 · v0.11 → v0.6 · v0.11.1 → v0.6.0.1 · v0.12 → v0.7 · v0.13 → v0.8 · v0.14 → v0.9 · v0.14.1 → v0.9.0.1 · v0.14.2 → v0.9.0.2 · v0.15 → v0.9.1 · v0.16 → v0.10 · v0.17 → v0.10.1 · v0.18 → v0.11 · v0.19 → v0.12 · v0.19.1 → v0.12.0.1 · v0.20 → v0.12.1 · v0.21 → v0.13 · v0.22 → v0.14 · v0.23 → v0.15 · v0.24 → v0.16 · v0.25 → v0.16.1 · v0.26 → v0.17 · v0.27 → v0.18 · v0.28 → v0.19 · v0.29 → v0.20 · v0.30 → v0.20.1 · v0.31 → v0.21 · v0.32 → v0.22 · v0.33 → v0.22.1 · v0.34 → v0.23 · v0.34.1 → v0.23.0.1 · v0.35 → v0.23.1 · v0.36 → v0.24 · v0.36.1 → v0.24.0.1 · v0.37 → v0.24.1 · v0.38 → v0.24.2 · v0.38.1 → v0.24.2.1 · v0.38.2 → v0.24.2.2 · v0.39 → v0.25 · v0.40 → v0.26 · v0.41 → v0.26.1 · v0.42 → v0.27 · v0.43 → v0.28 · v0.44 → v0.29 · v0.45 → v0.30 · v0.46 → v0.31 · v0.46.1 → v0.31.0.1 · v0.47 → v0.31.1 · v0.47.1 → v0.31.1.1 · v0.48 → v0.31.2 · v0.48.1 → v0.31.2.1 · v0.49 → v0.31.3 · v0.50 → v0.31.4 · v0.51 → v0.32 · v0.51.1 → v0.32.0.1 · v0.52 → v0.33 · v0.53 → v0.34 · v0.54 → v0.35 · v0.55 → v0.35.1 · v0.56 → v0.35.2 · v0.57 → v0.36 · v0.58 → v0.36.1 · v0.59 → v0.37 · v0.59.1 → v0.37.0.1 · v0.60 → v0.37.1 · v0.61 → v0.38 · v0.62 → v0.39 · v0.63 → v0.40 · v0.64 → v0.41 · v0.65 → v0.42 · v0.66 → v0.43 · v0.67 → v0.44 · v0.68 → v0.44.1 · v0.69 → v0.44.2 · v0.70 → v0.45 · v0.71 → v0.46 · v0.72 → v0.46.1 · v0.73 → v0.46.2 · v0.74 → v0.47 · v0.75 → v0.48 · v0.76 → v0.49 · v0.77 → v0.50 · v0.77.1 → v0.50.0.1 · v0.78 → v0.51 · v0.78.1 → v0.51.0.1 · v0.79 → v0.51.1 · v0.80 → v0.52 · v0.81 → v0.52.1 · v0.82 → v0.52.2 · v0.83 → v0.52.3 · v0.84 → v0.52.4 · v0.85 → v0.52.5 · v0.86 → v0.52.6 · v0.87 → v0.53 · v0.88 → v0.54 · v0.89 → v0.54.1 · v0.90 → v0.54.2 · v0.91 → v0.54.3 · v0.92 → v0.55 · v0.93 → v0.56 · v0.94 → v0.57 · v0.94.1 → v0.57.0.1 · v0.95 → v0.58 · v0.95.1 → v0.58.0.1 · v0.96 → v0.59 · v0.97 → v0.60 · v0.98 → v0.61 · v0.98.1 → v0.61.0.1 · v0.99 → v0.62 · v0.100 → v0.63 · v0.101 → v0.64 · v0.101.1 → v0.64.0.1 · v0.102 → v0.65 · v0.102.1 → v0.65.0.1 · v0.102.2 → v0.65.0.2 · v0.103 → v0.65.1 · v0.104 → v0.65.2 · v0.105 → v0.65.3 · v0.105.1 → v0.65.3.1 · v0.105.2 → v0.65.3.2 · v0.106 → v0.66 · v0.106.1 → v0.66.0.1 · v0.107 → v0.67 · v0.108 → v0.68 · v0.109 → v0.69 · v0.110 → v0.70 · v0.110.1 → v0.70.0.1 · v0.110.2 → v0.70.0.2 · v0.111 → v0.71 · v0.112 → v0.71.1 · v0.113 → v0.72 · v0.114 → v0.73 · v0.115 → v0.74 · v0.115.1 → v0.74.0.1 · v0.116 → v0.75 · v0.117 → v0.76 · v0.118 → v0.76.1 · v0.119 → v0.77 · v0.120 → v0.78 · v0.120.1 → v0.78.0.1 · v0.121 → v0.78.1 · v0.122 → v0.79 · v0.122.1 → v0.79.0.1 · v0.123 → v0.79.1 · v0.124 → v0.80 · v0.125 → v0.80.1 · v0.126 → v0.81 · v0.127 → v0.81.1 · v0.128 → v0.82 · v0.129 → v0.82.1 · v0.129.1 → v0.82.1.1 · v0.130 → v0.83 · v0.131 → v0.84 · v0.132 → v0.84.1 · v0.133 → v0.85 · v0.134 → v0.85.1 · v0.135 → v0.86 · v0.136 → v0.86.1 · v0.137 → v0.86.2 · v0.138 → v0.86.3 · v0.139 → v0.86.4 · v0.140 → v0.87 · v0.140.1 → v0.87.0.1 · v0.141 → v0.88 · v0.141.1 → v0.88.0.1 · v0.142 → v0.88.1 · v0.143 → v0.89 · v0.144 → v0.89.1 · v0.145 → v0.90 · v0.145.1 → v0.90.0.1 · v0.146 → v0.91 · v0.147 → v0.92 · v0.148 → v0.93 · v0.149 → v0.94 · v0.150 → v0.95 · v0.151 → v0.96 · v0.152 → v0.97 · v0.153 → v0.98 · v0.153.1 → v0.98.0.1 · v0.154 → v0.98.1 · v0.155 → v0.99 · v0.156 → v0.99.1 · v0.157 → v0.100 · v0.158 → v0.101 · v0.158.1 → v0.101.1 · v0.158.2 → v0.101.2 · v0.158.3 → v0.101.3 · v0.159 → v0.102 · v0.160 → v0.102.1 · v0.161 → v0.102.2 · v0.162 → v0.102.3 · v0.163 → v0.103 · v0.163.1 → v0.103.1 · v0.164 → v0.103.2 · v0.165 → v0.103.3 · v0.166 → v0.104 · v0.167 → v0.104.1 · v0.167.1 → v0.104.1.1 · v0.168 → v0.105 · v0.168.1 → v0.105.1 · v0.169 → v0.105.2 · v0.170 → v0.106 · v0.171 → v0.107 · v0.172 → v0.107.1 · v0.173 → v0.108 · v0.174 → v0.109 · v0.175 → v0.110 · v0.176 → v0.110.1 · v0.177 → v0.110.2 · v0.178 → v0.111 · v0.179 → v0.112 · v0.180 → v0.113 · v0.181 → v0.113.1 · v0.182 → v0.113.2 · v0.182.1 → v0.113.2.1 · v0.183 → v0.113.3 · v0.184 → v0.114 · v0.185 → v0.115 · v0.186 → v0.116 · v0.186.1 → v0.116.0.1 · v0.186.2 → v0.116.0.2 · v0.187 → v0.117 · v0.188 → v0.118 · v0.188.1 → v0.118.0.1 · v0.188.2 → v0.118.0.2 · v0.189 → v0.118.1 · v0.190 → v0.118.2 · v0.190.1 → v0.118.2.1 · v0.191 → v0.119 · v0.192 → v0.120 · v0.193 → v0.121 · v0.194 → v0.122 · v0.195 → v0.123 · v0.195.1 → v0.123.0.1 · v0.196 → v0.124

## Project structure

**Since 04/11/2026 there are three modules** (v0.153): `shared` is the code
common to phone and PC, `app` the Android app, `desktop` the one for
Windows and Mac. The packages are the same in all three: a file moves from
`app` to `shared` without changing who uses it.

```
shared/src/commonMain/kotlin/com/gabriele/notionlocal/
                    # The common code: the database (entities, DAOs,
                    # AppDatabase), the repositories, the pages'
                    # images, the ViewModels, formulas, texts, theme,
                    # settings, Patch Notes (and growing)
shared/src/androidMain/
                    # Since 04/11/2026 (v0.153.6) the whole phone app
                    # (screens, notifications, backup…) and what changes
                    # between phone and PC (expect/actual)
shared/src/desktopMain/
                    # The PC side of expect/actual
app/src/main/java/com/gabriele/notionlocal/
                    # Only MainActivity, ShareActivity and the widget
desktop/src/main/kotlin/com/gabriele/notionlocal/desktop/Main.kt
                    # The PC window (gradle :desktop:run)
desktop/src/test/   # DesktopScreensTest: the PC screens as images;
                    # DesktopDatabaseTest: the database on the PC;
                    # DesktopRepositoryTest: the repositories on the PC;
                    # DesktopImageStoreTest: the images on the PC;
                    # DesktopViewModelTest: the ViewModels on the PC

app/src/main/java/com/gabriele/notionlocal/
├── data/           # Room entities, DAOs, Repositories, AppDatabase
├── ui/
│   ├── screen/     # PageEditorScreen, DatabaseViewScreen, SearchScreen
│   ├── theme/      # Colours, typography, Material 3 theme
│   └── navigation/ # Routes and NavHost
├── viewmodel/      # One ViewModel per screen + ViewModelFactory
└── MainActivity.kt

app/src/test/java/com/gabriele/notionlocal/
└── data/           # Automatic tests without a phone (since 24/09/2026:
    │               # TextStatsTest, the text count)
    └── repository/ # PageRepositoryTest: moving, duplicating, trashing,
                    # Undo and Redo, the databases' search index,
                    # on a real database (Robolectric + Room)
app/src/test/java/com/gabriele/notionlocal/viewmodel/
                    # DatabaseSearchTest: which rows the search inside a
                    # database keeps and which it highlights;
                    # LinkedViewModelTest: a real linked view
app/src/test/java/com/gabriele/notionlocal/data/entity/
                    # RichTextScriptTest: subscript and superscript in the saved text
app/src/test/java/com/gabriele/notionlocal/data/widgets/
                    # WidgetsTest: the progress bars, the west-east
                    # order, the pomodoro advancing, saving
app/src/test/java/com/gabriele/notionlocal/ui/
                    # WidgetsRenderTest: draws the widgets and saves
                    # the image in app/build/widget-screenshots/;
                    # ScriptRenderTest: the subscript and superscript buttons
app/src/test/java/com/gabriele/notionlocal/data/MigrationTest.kt
                    # migration 28→29 opened by Room as the app does
app/src/test/resources/schema/v28.sql
                    # the schema of version 28, for MigrationTest

app/src/main/res/font/            # The pages' 10 Western fonts (free look-alikes)
app/src/main/res/values/font_certs.xml   # Certificates to download fonts from Google
app/src/main/assets/font_licenses/OFL.txt # Licence and copyright of the included fonts
app/src/main/assets/proofing/     # The spell checker's dictionaries, with their licences
tools/proofing/get_dictionaries.py # Downloads them again, at the tested versions
```

`data/proofing/` is the spell checker: `SpellDictionary` reads a Hunspell
dictionary and checks a word, `SpellSuggestions.kt` finds the words to put
in its place, `ProofingText` splits the text into words to check,
`SpellChecker` adds the user's dictionary and the ignored words.
`ui/screen/ProofingUi.kt` is how it looks in the page (the wavy line, the
menu), `ProofingSettings.kt` the choices in Settings and in the three dots.

`ui/theme/PageFonts.kt` is the catalogue of the pages' fonts: names,
look-alikes, groups, where they come from, and `PageTypography`, which
brings font and size to all the text.

`data/widgets/` are the sidebar's widgets: `Widgets.kt` how each one is
made and the computations (bars, order of time zones, pomodoro), without
Android; `WidgetStore.kt` where they are saved and the wait that makes the
pomodoro ring. `ui/screen/SidebarWidgets.kt` is how they look.

`data/patchnotes/PatchNotes.kt` and `ui/screen/PatchNotesScreen.kt` (in
`shared`, since 04/11/2026) are **Settings → Patch Notes**: this README's
Changelog split into versions and drawn as on GitHub — the list of titles
(`PatchNotesScreen`) and the page of each version (`PatchNoteScreen`). The
text is copied by the `patchNotes` task of `app/build.gradle.kts` (see
"Technical note").

`ui/screen/BlockActionsSheet.kt` is a block's menu (the six dots): the
entries for a text line, for a page and for a database, and all the
windows it opens. Whoever uses it passes only the block's id.

**Key files to understand first**:
- `PageEditorViewModel.updateRun` — the heart of the joined text's editing
  logic: it compares lines before/after and decides whether it is an edit,
  a split or a merge
- `PageEditorScreen.MergedTextRunField` — the shared field and its sync with
  the database
- `MergedRunVisualTransformation` + `MergedRunOffsetMapping` — rendering of
  the prefixes (• and numbers) and mapping between real and shown
  positions. **This mapping is the most delicate point of the project**: a
  mistake here moves the caret or the selection in a way hard to diagnose.
  Careful: the real text starts at 1, not 0, because of the `RUN_LEAD` — it
  is the mapping's `leading` parameter
- `DatabaseContent` — the database, used both full screen and inside a
  page. The only differences are decided by `embedded`: the smaller title
  (and hideable, see `showEmbeddedTitle`), the "Delete" entry in the
  settings, and not scrolling on its own
- `backspacedLineStart` + `PageEditorViewModel.backspaceAtLineStart` —
  recognition of the backspace at the start of a line and refusal of the
  merge as long as the line still has a marker or an indent to lose

## Technical note

The schema is at **version 55** (55, of 08/10/2026, adds
`pages.proofingMode`, `proofingLanguage` and `proofingIgnored` and the
`personal_words` table, the spell checker — see `ProofingUi.kt`; 54, of the
same day, adds `pages.freezeFirstColumn` and `freezeFirstRow`, the frozen
first column and first row; 53, of the same day, adds `pages.iconSymbol`,
the pages' Material icons; 52, of 06/10/2026, adds `blocks.attrsJson`, the
extra entries of the new blocks of the "/" menu — see `BlockAttrs`; 51, of
05/10/2026, adds `pages.thenSortColumnId` and `thenSortDescending`, the
second sorting rule; 50, of 30/09/2026, adds `database_rows.eventColor`, the
colour of calendar events; 49, of the same day, adds
`pages.colorBoardCards`, the boards' "Color cards"; 48, of the same day,
adds `pages.wrapCells`, today the long text in table cells
(`PageEntity.cellOverflow`: 0 three dots, 1 wrap, 2 widened column); 47, of
29/09/2026, adds `database_columns.durationFormat`, the Duration property;
46, of the same day, adds `pages.nameColumnOrder`, the place of the Name
column; 45, of the same day, adds `database_rows.parentRowId` and
`subRowsCollapsed`, the sub-rows; 44, of the same day, adds
`pages.columnWidths` and `fitColumns`, the width of the table's columns; 43,
of the same day, adds `pages.viewHostId`, `viewName` and `activeViewId`, the
views of a database; 42, of the same day, adds `pages.hideEmptySubGroups`,
the switch of empty sub-groups; 41, of the same day, adds
`pages.tableSubGroupColumnId`, the table's sub-groups; 40, of 28/09/2026,
adds `pages.chartSettings`, the Chart view's settings; 39, of 26/09/2026,
adds page templates; 38, of the same day, adds the date and time format of
Date columns; 37, of the same day, adds cell alignment; 36, of the same
day, adds the settings of Number columns and the views' calculations; 35, of
the same day, adds notebooks: the `notebooks` and `notebook_sections` tables
and `pages.notebookSectionId`; 34, of the same day, adds
`pages.isProtected`, pages locked with a password; 33, of 25/09/2026, adds
the `ink_strokes` table of hand drawings; 32, of the same day, adds
`blocks.notifyAt`, the time of "/notification" lines; 31, of the same day,
moves font and size from the page to the text, without changing the shape
of the tables; 30, of the same day, removes MS YaHei and adds the framing of
gallery cards; 25 to 29 come from the cloud session of 24/09/2026, see Log:
migrations simulated on SQLite in the cloud and **tried on a real phone the
same evening**, from a database at version 24 with test notes, see
Changelog "Resumed on the PC on 24/09/2026"; 27 doesn't change the shape of
the tables, it turns headings into paragraphs; 28 adds the simple database;
29 linked views, `sourceDatabaseId` and `viewHiddenColumnIds` in `pages` —
this one is also **proven by an automatic test with Room**,
`MigrationTest`, which opens a real database at version 28 as the app does
and has Room check the resulting schema), and from here on **every schema
change wants a real migration** in `AppDatabase`. Up to 5 there was
`fallbackToDestructiveMigration()`, which at every change recreated the
database from scratch losing everything: acceptable as long as the phone
had only test data, no longer now that there are real notes. The shortcut
was removed together with migration 5→6, so a field added without its
migration makes the app refuse to start — a loud error, much better than
notes deleted silently.

**The app's Patch Notes come from this README** (since 30/09/2026). At every
build the `patchNotes` task (`PatchNotesTask` in `app/build.gradle.kts`)
copies the "Changelog" section — from its title up to the next `## ` — into
the app's assets as `patch_notes.md` (with `addGeneratedSourceDirectory`, so
Gradle knows it must be redone when the README changes). From the same
README come `versionName`, the number of the first heading "### … v0.N — "
(the most recent version; up to four parts, v0.N.M.K, since 21/10/2026),
and `versionCode`, how many version headings there are: that is why
renumbering the Changelog, as on 21/10/2026, doesn't make it go back and the
phone accepts the update. So: **a new entry in the README is a new version
of the app** at the next build. `PatchNotesTest` reads the asset as the app
does and compares it with the README.

Before installing a build that changes the schema it is wise to take a copy
of the database from the phone:

```
adb exec-out run-as com.gabriele.notionlocal cat databases/notion_local.db > backup.db
```

(on Windows it must go through `cmd /c`, because PowerShell's redirection
ruins binary files; `-wal` and `-shm` are needed too. The redirection **on
the phone**, inside `adb shell`, produces instead an empty file: the app's
process can't write to `/sdcard`).

After installing it is wise to verify that the migration really happened
and not secretly by recreating the database: in bytes 60-63 of the file
there is `user_version` (it must be the new number), and the added column
must appear **at the end** of the `CREATE TABLE` in `sqlite_master` — if it
were in the middle, or between backticks like the others, it would mean the
table was rebuilt.

### A backup is read without the `-shm` file

**Almost everything in the database is in the `-wal`, not in the main
file.** The app is closed by killing the process at every installation, so
SQLite almost never does the transfer (checkpoint): the `.db` may contain
only the root page of the first start and all the rest live in the journal.

The `-shm` is that journal's index. If a copy of the database is opened
**with the `-shm` that was copied along with it**, SQLite trusts that
index, declares that the journal has zero valid frames and **shows an
almost empty database** — without errors and without warnings. It looks
like the backup is to be thrown away, or worse that the data was lost.

So: copy all three files, but to read the backup keep only `.db` and
`-wal`, **deleting the `-shm`**. This way SQLite rebuilds the index from the
journal and the data all comes back.

A database that after this precaution still looks almost empty really is —
it happens normally, because test databases are deleted by the app as soon
as they've been tried.
