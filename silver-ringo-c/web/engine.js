(function (root) {
  const C =
    typeof module !== "undefined" ? require("./content.js") : root.SRContent;
  const clone = (x) => JSON.parse(JSON.stringify(x));
  function item(
    productId = "bulgogi",
    kind = "set",
    side = "fries",
    drink = "cola",
  ) {
    return {
      productId,
      bread: "none",
      kind,
      side: kind === "set" ? side : null,
      drink: kind === "set" ? drink : null,
      quantity: 1,
    };
  }
  function create(level = 5, mode = "learn") {
    const l = C.levels.find((x) => x.id === level) || C.levels[4];
    const s = {
      version: 1,
      level: l.id,
      mode: l.id === 5 && mode !== "free" ? "test" : mode,
      screen: l.start,
      dining: null,
      category: "recommend",
      draft: null,
      cart: [],
      payment: null,
      events: [],
      hints: 0,
      startedAt: Date.now(),
      orderNo: null,
    };
    if (l.id >= 2 && l.id <= 4) s.dining = "here";
    if (l.id === 3) s.draft = {...item(), bread:null, kind:null, side:null, drink:null};
    if (l.id === 4) s.cart = [item()];
    return s;
  }
  function unit(i) {
    const p = C.products.find((x) => x.id === i.productId);
    return (
      p.price + (C.breads.find(b=>b.id===i.bread)?.extra||0) +
      (i.kind === "set"
        ? 2400 + (C.sides.find((x) => x.id === i.side)?.extra || 0)
        : 0)
    );
  }
  function total(s) {
    return s.cart.reduce((sum, i) => sum + unit(i) * i.quantity, 0);
  }
  function transition(state, a) {
    const s = clone(state),
      at = s.screen;
    let accepted = true;
    switch (a.type) {
      case "start":
        if (at === "start") s.screen = "dining";
        else accepted = false;
        break;
      case "dining":
        if (at === "dining" && ["here", "takeout"].includes(a.value)) {
          s.dining = a.value;
          s.screen = "menu";
        } else accepted = false;
        break;
      case "category":
        if (at === "menu" && C.categories.some((x) => x[0] === a.value))
          s.category = a.value;
        else accepted = false;
        break;
      case "product": {
        const p = C.products.find((x) => x.id === a.value);
        if (at !== "menu" || !p || p.category !== s.category) {
          accepted = false;
          break;
        }
        s.draft = item(p.id, "single");
        if (p.category === "burger") s.draft = {...s.draft, bread:null, kind:null, side:null, drink:null};
        s.screen = "options";
        break;
      }
      case "bread":
        if(at==="options" && C.breads.some(b=>b.id===a.value)) s.draft.bread=a.value; else accepted=false; break;
      case "kind":
        if (
          at === "options" && s.draft.bread &&
          C.products.find((x) => x.id === s.draft.productId).category ===
            "burger" &&
          ["single", "set"].includes(a.value)
        )
          s.draft = {...s.draft,kind:a.value,side:null,drink:null};
        else accepted = false;
        break;
      case "side":
      case "drink": {
        const list = a.type === "side" ? C.sides : C.drinks;
        if (
          at === "options" &&
          s.draft.kind === "set" &&
          list.some((x) => x.id === a.value)
        )
          s.draft[a.type] = a.value;
        else accepted = false;
        break;
      }
      case "add":
        if (at === "options" && s.draft?.bread && s.draft.kind && (s.draft.kind!=="set" || (s.draft.side && s.draft.drink))) {
          s.cart.push(clone(s.draft));
          s.draft = null;
          s.screen = "cart";
        } else accepted = false;
        break;
      case "quantity":
        if (at === "cart" && s.cart[a.index] && [-1, 1].includes(a.delta))
          s.cart[a.index].quantity = Math.max(
            1,
            Math.min(9, s.cart[a.index].quantity + a.delta),
          );
        else accepted = false;
        break;
      case "remove":
        if (at === "cart" && s.cart[a.index]) s.cart.splice(a.index, 1);
        else accepted = false;
        break;
      case "more":
        if (at === "cart") s.screen = "menu";
        else accepted = false;
        break;
      case "review":
        if (at === "cart" && s.cart.length) s.screen = "review";
        else accepted = false;
        break;
      case "checkout":
        if (at === "review" && s.cart.length) s.payment="card", s.screen = "card";
        else accepted = false;
        break;
      case "payment":
        if (at === "payment" && ["card", "mobile"].includes(a.value)) {
          s.payment = a.value;
          s.screen = "card";
        } else accepted = false;
        break;
      case "pay":
        if (at === "card" && s.cart.length) {
          s.screen = "points";
          s.orderNo = String(100 + Math.floor(Math.random() * 900));
        } else accepted = false;
        break;
      case "points":
        if(at==="points" && ["skip","earn"].includes(a.value)){s.points=a.value;s.screen="printing";}else accepted=false;break;
      case "print":
        if(at==="printing"){s.receiptPrinted=true;s.screen="receipt";}else accepted=false;break;
      case "back": {
        const prev = {

          menu: "dining",
          options: "menu",
          cart: "menu",
          review: "cart",
          payment: "review",
          card: "review",
        };
        if (prev[at]) s.screen = prev[at];
        else accepted = false;
        break;
      }
      default:
        accepted = false;
    }
    if (accepted)
      s.events.push({
        type: a.type,
        value: a.value ?? null,
        from: at,
        to: s.screen,
      });
    return { state: s, accepted };
  }
  function evaluate(s) {
    const l = C.levels.find((x) => x.id === s.level),
      g = C.goal;
    const target = s.cart[0] || s.draft;
    const set =
      !!target &&
      target.productId === g.productId &&
      target.bread === "none" && target.kind === g.kind &&
      target.side === g.side &&
      target.drink === g.drink;
    const checks = {
      dining: [s.dining === g.dining, "매장에서 먹기를 선택했나요?"],
      product: [
        s.draft?.productId === g.productId,
        "불고기 버거를 선택했나요?",
      ],
      set: [set, "불고기 버거 세트 · 감자튀김 · 콜라를 확인해 주세요."],
      cart: [
        s.cart.length === 1 && set && target.quantity === g.quantity,
        "장바구니에는 지정한 세트 1개만 담아 주세요.",
      ],
      payment: [
        s.payment === g.payment && s.receiptPrinted && s.screen === "receipt",
        "카드 결제를 선택하고 모의 결제를 끝내 주세요.",
      ],
    };
    const failures = l.checks
      .filter((k) => !checks[k][0])
      .map((k) => checks[k][1]);
    return {
      schemaVersion: 1,
      courseVersion: C.courseVersion,
      topicId: C.topicId,
      levelId: s.level,
      mode: s.mode,
      passed: failures.length === 0,
      failures,
      hints: s.hints,
      actions: s.events.length,
      elapsedSeconds: Math.round((Date.now() - s.startedAt) / 1000),
      completedAt: new Date().toISOString(),
    };
  }
  function reached(s) {
    return s.screen === C.levels.find((x) => x.id === s.level).end;
  }
  function unlocked(level, results) {
    return (
      level === 1 ||
      results.some(
        (r) => r.levelId === level - 1 && r.passed && r.mode !== "free",
      )
    );
  }
  const api = {
    create,
    transition,
    total,
    unit,
    evaluate,
    reached,
    unlocked,
    item,
  };
  root.SREngine = api;
  if (typeof module !== "undefined") module.exports = api;
})(typeof globalThis !== "undefined" ? globalThis : this);
