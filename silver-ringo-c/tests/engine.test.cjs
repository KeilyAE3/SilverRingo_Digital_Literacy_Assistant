const { test } = require("node:test"),
  assert = require("node:assert/strict");
const E = require("../web/engine.js");
function act(s, type, value, extra = {}) {
  const t = E.transition(s, { type, value, ...extra });
  assert.ok(t.accepted, `${s.screen}: ${type}`);
  return t.state;
}
function full(dining = "here", kind = "set", payment = "card") {
  let s = E.create(6);
  for (const [t, v] of [
    ["start"],
    ["dining", dining],
    ["product", "bulgogi"],
    ["kind", kind],
    ["add"],
    ["review"],
    ["checkout"],
    ["payment", payment],
    ["pay"],
  ])
    s = act(s, t, v);
  return s;
}
test("whole target order passes, with correct price and receipt", () => {
  const s = full();
  assert.equal(E.total(s), 7500);
  assert.equal(E.evaluate(s).passed, true);
  assert.equal(s.screen, "receipt");
});
test("single item bypasses set options and costs 5000", () => {
  const s = full("here", "single");
  assert.equal(s.cart[0].side, null);
  assert.equal(s.cart[0].drink, null);
  assert.equal(E.total(s), 5000);
  assert.equal(E.evaluate(s).passed, false);
});
test("side surcharge and quantity change exact total", () => {
  let s = E.create(3);
  s = act(s, "side", "cheese");
  s = act(s, "add");
  s = act(s, "quantity", null, { index: 0, delta: 1 });
  assert.equal(E.total(s), 16000);
  assert.equal(E.evaluate(s).passed, false);
});
test("empty cart cannot check out or pay", () => {
  let s = E.create(4);
  s = act(s, "remove", null, { index: 0 });
  assert.equal(E.total(s), 0);
  assert.equal(E.transition(s, { type: "review" }).accepted, false);
  assert.equal(E.transition(s, { type: "pay" }).accepted, false);
});
test("multiple items add, delete, and total independently", () => {
  let s = E.create(4);
  s = act(s, "more");
  s = act(s, "category", "side");
  s = act(s, "product", "fries");
  s = act(s, "add");
  assert.equal(s.cart.length, 2);
  assert.equal(E.total(s), 9700);
  s = act(s, "remove", null, { index: 0 });
  assert.equal(E.total(s), 2200);
});
test("wrong dining and payment do not pass final assessment", () => {
  const r = E.evaluate(full("takeout", "set", "mobile"));
  assert.equal(r.passed, false);
  assert.equal(r.failures.length, 2);
});
test("back from review preserves cart, can correct quantity", () => {
  let s = E.create(4);
  s = act(s, "quantity", null, { index: 0, delta: 1 });
  s = act(s, "review");
  s = act(s, "back");
  s = act(s, "quantity", null, { index: 0, delta: -1 });
  s = act(s, "review");
  s = act(s, "checkout");
  assert.ok(E.reached(s));
  assert.ok(E.evaluate(s).passed);
});
test("every short level has an achievable completion", () => {
  const paths = {
    1: [["start"], ["dining", "here"]],
    2: [["product", "bulgogi"]],
    3: [["kind", "set"], ["add"]],
    4: [["review"], ["checkout"]],
    5: [["payment", "card"], ["pay"]],
  };
  for (const id of [1, 2, 3, 4, 5]) {
    let s = E.create(id);
    for (const [t, v] of paths[id]) s = act(s, t, v);
    assert.ok(E.reached(s));
    assert.ok(E.evaluate(s).passed, `level ${id}`);
  }
});
test("quantity bound 1..9, invalid action and duplicate pay are rejected", () => {
  let s = E.create(4);
  for (let i = 0; i < 20; i++)
    s = act(s, "quantity", null, { index: 0, delta: 1 });
  assert.equal(s.cart[0].quantity, 9);
  assert.equal(
    E.transition(s, { type: "side", value: "nonsense" }).accepted,
    false,
  );
  s = full();
  assert.equal(E.transition(s, { type: "pay" }).accepted, false);
});
test("transition is pure and standalone sides cannot become sets", () => {
  const s = E.create(4),
    snapshot = JSON.stringify(s);
  E.transition(s, { type: "remove", index: 0 });
  assert.equal(JSON.stringify(s), snapshot);
  let t = E.create(2);
  t = act(t, "product", "fries");
  assert.equal(E.transition(t, { type: "kind", value: "set" }).accepted, false);
});
test("unlock needs a completed previous level, free mode never unlocks", () => {
  assert.ok(E.unlocked(1, []));
  assert.equal(E.unlocked(2, []), false);
  assert.equal(
    E.unlocked(2, [{ levelId: 1, passed: true, mode: "free" }]),
    false,
  );
  assert.ok(E.unlocked(2, [{ levelId: 1, passed: true, mode: "learn" }]));
});
