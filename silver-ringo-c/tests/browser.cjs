const { chromium } = require("playwright"),
  assert = require("node:assert/strict"),
  path = require("node:path"),
  fs = require("node:fs");
(async () => {
  const out = path.join(__dirname, "artifacts");
  fs.mkdirSync(out, { recursive: true });
  const browser = await chromium.launch({
    headless: true,
    executablePath: process.env.SR_BROWSER_PATH || undefined,
    args: ["--no-sandbox"],
  });
  const context = await browser.newContext({
    viewport: { width: 1440, height: 1100 },
  });
  const p = await context.newPage(),
    errors = [];
  p.on("pageerror", (e) => errors.push(e.message));
  const url = "file://" + path.resolve(__dirname, "../web/index.html");
  const click = (a, v) =>
    p
      .locator(
        `[data-action="${a}"]${v !== undefined ? `[data-value="${v}"]` : ""}`,
      )
      .click();
  await p.goto(url);
  await p.screenshot({ path: out + "/01-learning-map.png", fullPage: true });
  assert.ok(
    await p.locator('[data-action="intro"][data-value="2"]').isDisabled(),
  );
  // v2 runtime reproduction, in its own page.
  const old = await context.newPage();
  const source = path.resolve(
    __dirname,
    "../../original/silver-ringo-v2/index.html",
  );
  if (fs.existsSync(source)) {
    await old.goto("file://" + source);
    await old.evaluate(() => {
      next();
      next();
      next();
      chooseKind("단품");
      chooseSide("감자튀김");
      chooseDrink("콜라");
      next();
    });
    assert.match(await old.locator("#screen").innerText(), /7,500/);
    await old.close();
    console.log("PASS v2 single-item pricing defect reproduced");
  }
  for (let id = 1; id <= 6; id++) {
    await click("intro", id);
    await click("launch");
    if (id === 1 || id === 6) {
      await click("start");
      await click("dining", "here");
    }
    if (id === 2 || id === 6) {
      await click("product", "bulgogi");
    }
    if (id === 3 || id === 6) {
      await click("kind", "set");
      await click("add");
    }
    if (id === 4 || id === 6) {
      await click("review");
      await click("checkout");
    }
    if (id === 5 || id === 6) {
      await click("payment", "card");
      await click("pay");
      await click("finish");
    }
    await p.getByRole("heading", { name: "차근차근 잘 해냈어요!" }).waitFor();
    await click("home");
    console.log("PASS browser level " + id);
  }
  await p.reload();
  assert.equal(await p.locator(".circle").filter({ hasText: "✓" }).count(), 6);
  console.log("PASS persisted level unlock after reload");
  await click("free");
  await click("start");
  await click("dining", "here");
  assert.equal(await p.locator(".highlight").count(), 0);
  await p.screenshot({ path: out + "/02-kiosk-menu.png", fullPage: true });
  await click("product", "bulgogi");
  await click("kind", "set");
  await click("side", "cheese");
  await p.screenshot({ path: out + "/03-options.png", fullPage: true });
  await click("add");
  await p.locator('[data-action="quantity"][data-delta="1"]').click();
  assert.match(await p.locator(".amount").innerText(), /16,000/);
  await click("more");
  await click("category", "side");
  await click("product", "fries");
  await click("add");
  assert.match(await p.locator(".amount").innerText(), /18,200/);
  await click("remove", 1);
  await click("review");
  await click("back");
  await p.locator('[data-action="quantity"][data-delta="-1"]').click();
  assert.match(await p.locator(".amount").innerText(), /8,000/);
  await p.reload();
  await click("resume");
  assert.match(await p.locator(".amount").innerText(), /8,000/);
  await click("review");
  await click("checkout");
  await click("payment", "mobile");
  await click("pay");
  await click("finish");
  await p.getByRole("heading", { name: "한 번 더 연습해 볼까요?" }).waitFor();
  console.log(
    "PASS multiple items, surcharge, quantity, back, resume, and wrong-order feedback",
  );
  await click("restart");
  await click("start");
  await click("dining", "takeout");
  await click("product", "bulgogi");
  assert.equal(await p.locator('[data-action="side"]').count(), 0);
  assert.match(await p.locator(".amount").innerText(), /5,000/);
  await click("add");
  await click("remove", 0);
  assert.equal(await p.locator('[data-action="review"]').count(), 0);
  console.log("PASS single branch and empty cart");
  await p.setViewportSize({ width: 390, height: 844 });
  await click("more");
  await p.screenshot({ path: out + "/04-mobile.png", fullPage: true });
  assert.equal(
    await p.evaluate(() => document.documentElement.scrollWidth <= innerWidth),
    true,
  );
  await p.setViewportSize({ width: 320, height: 700 });
  assert.equal(
    await p.evaluate(() => document.documentElement.scrollWidth <= innerWidth),
    true,
  );
  await p.evaluate(() => (document.documentElement.style.fontSize = "30px"));
  assert.equal(
    await p.evaluate(() => document.documentElement.scrollWidth <= innerWidth),
    true,
  );
  console.log("PASS 390px, 320px, enlarged text no horizontal overflow");
  // Native entry and bridge shape, without claiming an Android device run.
  const embed = await context.newPage();
  const messages = [];
  await embed.addInitScript(() => {
    window.SilverRingoNative = {
      postMessage: (m) => {
        (window.bridgeLog ??= []).push(JSON.parse(m));
      },
    };
  });
  await embed.goto(url + "?embed=1&level=1");
  await embed.locator('[data-action="launch"]').click();
  await embed.locator('[data-action="start"]').click();
  await embed.locator('[data-action="dining"][data-value="here"]').click();
  const posted = await embed.evaluate(() => window.bridgeLog);
  assert.ok(
    posted.some((x) => x.type === "result" && x.levelId === 1 && x.passed),
  );
  assert.equal(await embed.locator('[data-action="next-level"]').count(), 0);
  await embed.locator('[data-action="home"]').click();
  assert.ok(
    (await embed.evaluate(() => window.bridgeLog)).some(
      (x) => x.type === "exit",
    ),
  );
  console.log(
    "PASS embedded entry, completion and exit bridge messages (browser mock)",
  );
  assert.deepEqual(errors, []);
  console.log("PASS no browser JS errors");
  const demo = await context.newPage();
  await demo.goto(
    "file://" + path.resolve(__dirname, "../silver-ringo-demo.html"),
  );
  await demo.locator('[data-action="free"]').click();
  await demo.locator('[data-action="start"]').click();
  await demo.locator('[data-action="dining"][data-value="here"]').click();
  assert.ok(
    await demo
      .locator("img")
      .first()
      .evaluate((img) => img.complete && img.naturalWidth > 0),
  );
  await demo.locator('[data-action="product"][data-value="bulgogi"]').click();
  await demo.locator('[data-action="kind"][data-value="set"]').click();
  await demo.locator('[data-action="add"]').click();
  for (const a of ["review", "checkout"])
    await demo.locator('[data-action="' + a + '"]').click();
  await demo.locator('[data-action="payment"][data-value="card"]').click();
  await demo.locator('[data-action="pay"]').click();
  await demo.locator('[data-action="finish"]').click();
  await demo.getByRole("heading", { name: "차근차근 잘 해냈어요!" }).waitFor();
  console.log("PASS standalone single-HTML target order and embedded SVG");
  const blocked = await context.newPage();
  await blocked.addInitScript(() =>
    Object.defineProperty(window, "localStorage", {
      get() {
        throw Error("blocked");
      },
    }),
  );
  await blocked.goto(url);
  await blocked.locator('[data-action="free"]').click();
  assert.match(await blocked.locator("#notice").innerText(), /저장/);
  await blocked.locator('[data-action="start"]').click();
  console.log("PASS storage-disabled fallback continues practice");
  await browser.close();
})().catch((e) => {
  console.error(e);
  process.exitCode = 1;
});
