(() => {
  "use strict";
  const C = SRContent,
    E = SREngine,
    $ = (id) => document.getElementById(id),
    app = $("app");
  const key = "silver-ringo-c-v3",
    params = new URLSearchParams(location.search),
    embedded = params.get("embed") === "1";
  let store = { version: 1, results: [], completed: [], session: null },
    state = null,
    view = "map",
    level = 1,
    result = null,
    voice = false,
    storageOK = true;
  try {
    const saved = JSON.parse(localStorage.getItem(key));
    if (saved?.version === 1 && Array.isArray(saved.results)) store = saved;
  } catch {
    storageOK = false;
  }
  store.completed = Array.from(
    new Set([
      ...(Array.isArray(store.completed) ? store.completed : []),
      ...store.results
        .filter((r) => r.passed && r.mode !== "free")
        .map((r) => r.levelId),
    ]),
  );
  const progressResults = () =>
    store.completed.map((levelId) => ({
      levelId,
      passed: true,
      mode: "learn",
    }));
  if (embedded) document.body.classList.add("embed");
  function save() {
    store.session = state;
    try {
      localStorage.setItem(key, JSON.stringify(store));
    } catch {
      storageOK = false;
      notice(
        "이 환경에서는 진도가 저장되지 않아요. 현재 연습은 계속할 수 있습니다.",
      );
    }
  }
  function notice(t = "") {
    $("notice").textContent = t;
  }
  function native(type, payload = {}) {
    if (window.SilverRingoNative?.postMessage)
      window.SilverRingoNative.postMessage(
        JSON.stringify({ type, ...payload }),
      );
  }
  function speak(text) {
    if (window.SilverRingoNative?.postMessage) {
      native("speak", { text });
      return;
    }
    if (!window.speechSynthesis) {
      notice("음성을 지원하지 않는 환경이에요. 화면 안내를 읽어 주세요.");
      return;
    }
    speechSynthesis.cancel();
    const u = new SpeechSynthesisUtterance(text);
    u.lang = "ko-KR";
    u.rate = 0.85;
    speechSynthesis.speak(u);
  }
  const money = (n) => n.toLocaleString("ko-KR") + "원",
    product = (id) => C.products.find((x) => x.id === id),
    name = (list, id) => list.find((x) => x.id === id)?.name || "";
  const B = (label, action, value = "", cls = "", extra = "") =>
    `<button class="${cls}" data-action="${action}" data-value="${value}" ${extra}>${label}</button>`;
  function hinted(action, value) {
    if (!state || state.mode !== "learn") return false;
    return (
      {
        start: ["start", ""],
        dining: ["dining", "here"],
        menu:
          state.category === "burger"
            ? ["product", "bulgogi"]
            : ["category", "burger"],
        options: state.draft?.kind === "set" ? ["add", ""] : ["kind", "set"],
        cart: ["review", ""],
        review: ["checkout", ""],
        payment: ["payment", "card"],
        card: ["pay", ""],
      }[state.screen]?.join(":") === [action, value].join(":")
    );
  }
  function choice(label, action, value = "", cls = "", extra = "") {
    const h = hinted(action, value);
    return B(
      label + (h ? '<span class="target-label">여기를 눌러 보세요</span>' : ""),
      action,
      value,
      cls + (h ? " highlight" : ""),
      extra,
    );
  }
  function heading(text) {
    return `<h2 tabindex="-1" id="screen-title">${text}</h2>`;
  }
  function map() {
    return `<section class="hero"><div class="eyebrow">IT 기능 배우기 / 키오스크</div><h1>한 걸음씩 배우는<br><em>나의 첫 키오스크 주문</em></h1><p>짧게 배우고, 직접 눌러 보고, 익숙해질 때까지 연습해요.</p><span class="pill">${store.completed.length} / 6 단계 완료</span></section><div class="map-layout"><section class="path" aria-label="학습 단계">${C.levels
      .map((l) => {
        const done = store.completed.includes(l.id),
          open = E.unlocked(l.id, progressResults());
        return B(
          `<span class="circle">${done ? "✓" : open ? l.id : "🔒"}</span><span>${l.title}<small>${open ? l.description : "앞 단계를 완료하면 열려요"}</small></span>`,
          "intro",
          l.id,
          "node",
          open ? "" : "disabled",
        );
      })
      .join(
        "",
      )}</section><aside class="aside"><span class="eyebrow">차근차근, 부담 없이</span><h2>주문 연습 안내</h2><p>1~5단계는 한 부분씩 연습합니다. 마지막 단계에서 전체 주문을 해봐요.</p><p class="quiet">시간제한은 없어요. 틀려도 다시 할 수 있습니다.</p>${B("자유 실습 시작", "free", "", "primary wide")}${store.session ? B("이어 하던 연습", "resume", "", "wide") : ""}<p class="quiet" style="margin-top:16px">자유 실습은 단계 잠금에 영향을 주지 않아요.</p></aside></div>`;
  }
  function intro() {
    const l = C.levels[level - 1];
    return `<section class="intro"><div class="eyebrow">키오스크 배우기 · ${level}단계</div><h1>${l.title}</h1><span class="pill">${level === 6 ? "실전 모드 · 강조 없이 도전" : "학습 모드 · 안내와 함께"}</span><p>${l.concept}</p><div class="card secondary"><strong>이번 연습 목표</strong><p>${l.task}</p></div>${level > 1 && level < 6 ? '<p class="quiet">앞부분의 주문은 미리 준비되어 있어요. 이번 단계만 연습합니다.</p>' : ""}${B("직접 연습하기", "launch", "", "primary wide")}${B("학습 목록으로", "home", "", "wide")}</section>`;
  }
  function detail(i) {
    return i.kind === "set"
      ? `세트 · ${name(C.sides, i.side)} · ${name(C.drinks, i.drink)}`
      : "단품";
  }
  function orderLines(edit = false) {
    return state.cart
      .map(
        (i, index) =>
          `<div class="line"><strong>${product(i.productId).name} ${i.kind === "set" ? "세트" : ""}</strong><p>${detail(i)}</p><strong>${money(E.unit(i) * i.quantity)}</strong>${edit ? `<div class="quantity">${B("−", "quantity", index, "", `aria-label="${product(i.productId).name} 수량 줄이기" data-delta="-1" ${i.quantity === 1 ? "disabled" : ""}`)}<span>${i.quantity}개</span>${B("+", "quantity", index, "", `aria-label="${product(i.productId).name} 수량 늘리기" data-delta="1" ${i.quantity === 9 ? "disabled" : ""}`)}${B("삭제", "remove", index, "quiet", `aria-label="${product(i.productId).name} 삭제"`)}</div>` : `<p>수량 ${i.quantity}개</p>`}</div>`,
      )
      .join("");
  }
  function screen() {
    const s = state;
    switch (s.screen) {
      case "start":
        return `<div class="splash"><span class="pill">직접 눌러 보는 가상 매장</span><img src="assets/burger.svg" alt="햄버거 그림"><strong>링고 버거</strong><p>맛있는 한 끼,<br>천천히 골라 보세요.</p></div>${heading("화면을 눌러 주문을 시작하세요")}${choice("주문 시작하기", "start", "", "primary wide")}`;
      case "dining":
        return (
          heading("어디에서 드시나요?") +
          `<p>식사하실 장소를 선택해 주세요.</p><div class="choices">${choice('<span aria-hidden="true">🍽️</span>매장에서 먹기', "dining", "here", "choice")}${choice('<span aria-hidden="true">🛍️</span>포장하기', "dining", "takeout", "choice")}</div>`
        );
      case "menu":
        return (
          heading("메뉴를 골라 주세요") +
          `<nav class="tabs" aria-label="메뉴 카테고리">${C.categories.map(([id, n]) => choice(n, "category", id, s.category === id ? "selected" : "", `aria-pressed="${s.category === id}"`)).join("")}</nav><div class="products">${C.products
            .filter((p) => p.category === s.category)
            .map((p) =>
              choice(
                `<img src="assets/${p.image}" alt=""><strong>${p.name}</strong><span class="price">${money(p.price)}부터</span>`,
                "product",
                p.id,
                "product",
              ),
            )
            .join(
              "",
            )}</div><div class="cart-bar row between"><span>장바구니 ${s.cart.reduce((a, i) => a + i.quantity, 0)}개</span><strong>${money(E.total(s))}</strong>${B("장바구니 보기", "open-cart", "", "wide", s.cart.length ? "" : "disabled")}</div>`
        );
      case "options": {
        const d = s.draft,
          p = product(d.productId),
          burger = p.category === "burger";
        return (
          heading("선택한 메뉴를 확인하세요") +
          `<section class="dialog" role="region" aria-label="메뉴 옵션 선택"><div class="row"><img width="110" height="90" src="assets/${p.image}" alt=""><strong>${p.name}</strong></div>${burger ? `<h3>1. 구성을 선택하세요</h3><div class="options">${["single", "set"].map((k) => choice(k === "set" ? "세트 +2,500원" : "단품", "kind", k, d.kind === k ? "selected" : "", `aria-pressed="${d.kind === k}"`)).join("")}</div>` : ""}${d.kind === "set" ? `<h3>2. 사이드 선택</h3><div class="options">${C.sides.map((o) => choice(o.name + (o.extra ? ` +${o.extra}원` : ""), "side", o.id, d.side === o.id ? "selected" : "", `aria-pressed="${d.side === o.id}"`)).join("")}</div><h3>3. 음료 선택</h3><div class="options">${C.drinks.map((o) => choice(o.name, "drink", o.id, d.drink === o.id ? "selected" : "", `aria-pressed="${d.drink === o.id}"`)).join("")}</div>` : "<p>이 메뉴는 단품으로 담습니다.</p>"}<p class="amount">${money(E.unit(d))}</p>${choice("장바구니 담기", "add", "", "primary wide")}${B("선택 취소", "cancel", "", "wide")}</section>`
        );
      }
      case "cart":
        return (
          heading("장바구니") +
          (s.cart.length
            ? orderLines(true) +
              `<p class="amount">합계 ${money(E.total(s))}</p>${choice("주문 확인", "review", "", "primary wide")}`
            : "<p>장바구니가 비어 있어요.<br>메뉴를 먼저 담아 주세요.</p>") +
          B("메뉴 더 담기", "more", "", "wide")
        );
      case "review":
        return (
          heading("주문 내역이 맞나요?") +
          `<p class="pill">${s.dining === "here" ? "매장에서 먹기" : "포장하기"}</p>${orderLines()}<p class="amount">결제 금액 ${money(E.total(s))}</p>${choice("결제 수단 선택", "checkout", "", "primary wide")}`
        );
      case "payment":
        return (
          heading("어떻게 결제하시겠어요?") +
          `<p class="amount">${money(E.total(s))}</p><div class="choices">${choice('<span aria-hidden="true">💳</span>카드 결제', "payment", "card", "choice")}${choice('<span aria-hidden="true">📱</span>간편결제', "payment", "mobile", "choice")}</div>`
        );
      case "card":
        return (
          heading(s.payment === "card" ? "카드 결제 연습" : "간편결제 연습") +
          `<div class="splash"><span style="font-size:76px" aria-hidden="true">${s.payment === "card" ? "💳" : "📱"}</span><p>화면 아래의 모의 결제 버튼을 눌러 주세요.</p><strong>${money(E.total(s))}</strong></div><p>실제 카드·휴대폰을 대거나 결제 정보를 입력할 필요가 없습니다.</p>${choice(s.payment === "card" ? "모의 카드로 결제하기" : "모의 간편결제 하기", "pay", "", "primary wide")}`
        );
      case "receipt":
        return (
          heading("모의 주문이 완료됐어요") +
          `<div class="receipt"><p>주문번호</p><div class="number">${s.orderNo}</div><p>${s.dining === "here" ? "매장에서 먹기" : "포장하기"} · ${money(E.total(s))}</p></div>${B("연습 결과 확인", "finish", "", "primary wide")}`
        );
    }
  }
  function practice() {
    const l = C.levels[state.level - 1],
      learn = state.mode === "learn";
    return `<div class="practice-layout"><aside class="coach"><section class="card"><div class="eyebrow">${state.mode === "free" ? "자유 실습" : learn ? "안내와 함께 연습" : "혼자서 실전 연습"}</div><h1>${l.title}</h1><p class="task">${l.task}</p>${learn ? `<p id="guide" aria-live="polite">${C.guides[state.screen]}</p>` : '<p class="quiet">주문 목표를 보고 자유롭게 조작해 보세요. 버튼 강조는 없습니다.</p>'}<details class="practice-tools" ${matchMedia("(max-width:850px)").matches ? "" : "open"}><summary>도움말 · 음성 · 다시 시작</summary>${learn ? `${B("🔊 안내 다시 듣기", "speak", "", "wide")}${B(voice ? "자동 음성 끄기" : "자동 음성 켜기", "voice", "", "wide", `aria-pressed="${voice}"`)}` : ""}${B("힌트 보기", "hint", "", "wide")}${B("이번 단계 다시 시작", "restart", "", "wide")}${B("학습 목록으로", "home", "", "wide")}</details></section><p class="quiet">서두르지 않아도 괜찮아요.<br>어떤 화면에서도 다시 연습할 수 있어요.</p></aside><section class="kiosk" aria-label="키오스크 시뮬레이터"><div class="store-head"><span>RINGO BURGER</span><small>연습용</small></div><div class="store-info">${state.dining ? (state.dining === "here" ? "매장에서 먹는 주문" : "포장 주문") : "가상 매장 · 실제 결제 없음"}</div><div class="screen">${screen()}</div><div class="bottom-nav">${B("← 이전 화면", "back", "", "", state.screen === l.start || state.screen === "receipt" ? "disabled" : "")}</div></section></div>`;
  }
  function resultHTML() {
    const r = result,
      l = C.levels[state.level - 1];
    return `<section class="card result"><div class="result-icon" aria-hidden="true">${r.passed ? "✓" : "↻"}</div><div class="eyebrow">${l.title} · 연습 결과</div><h1>${r.passed ? "차근차근 잘 해냈어요!" : "한 번 더 연습해 볼까요?"}</h1>${r.passed ? `<p>${state.level === 6 ? "처음부터 주문을 완성했어요." : l.description + " 이번 단계를 완료했어요."}</p>` : `<p>주문 목표와 다른 부분이 있어요.</p><ul>${r.failures.map((t) => `<li>${t}</li>`).join("")}</ul>`}${state.orderNo ? `<p>모의 주문번호 <strong>${state.orderNo}</strong> · ${money(E.total(state))}</p>` : ""}<div class="metrics"><div>직접 조작<strong>${r.actions}회</strong></div><div>요청한 힌트<strong>${r.hints}회</strong></div></div><p class="quiet">점수나 시간제한 없이, 익숙해질 때까지 반복해요.</p>${!embedded && r.passed && state.level < 6 && state.mode !== "free" ? B("다음 단계로", "next-level", "", "primary wide") : ""}${B("같은 단계 다시 연습", "restart", "", "wide")}${B("학습 목록으로", "home", "", "wide")}</section>`;
  }
  function render(focus = true) {
    app.innerHTML =
      view === "map"
        ? map()
        : view === "intro"
          ? intro()
          : view === "result"
            ? resultHTML()
            : practice();
    if (focus) {
      (
        app.querySelector("#screen-title") || app.querySelector("h1")
      )?.setAttribute("tabindex", "-1");
      (app.querySelector("#screen-title") || app.querySelector("h1"))?.focus({
        preventScroll: true,
      });
    }
    if (!storageOK) notice("진도 저장을 사용할 수 없는 환경입니다.");
  }
  function launch(id, mode = "learn") {
    level = id;
    state = E.create(id, mode);
    view = "practice";
    result = null;
    save();
    render();
    window.scrollTo(0, 0);
  }
  function finish() {
    if (view === "result") return;
    result = E.evaluate(state);
    view = "result";
    store.results.push(result);
    if (
      result.passed &&
      result.mode !== "free" &&
      !store.completed.includes(result.levelId)
    )
      store.completed.push(result.levelId);
    store.results = store.results.slice(-120);
    store.session = null;
    try {
      localStorage.setItem(key, JSON.stringify(store));
    } catch {
      storageOK = false;
    }
    native("result", result);
    render();
    window.scrollTo(0, 0);
  }
  function home() {
    if (embedded) {
      native("exit");
      return;
    }
    state = null;
    view = "map";
    notice();
    render();
    window.scrollTo(0, 0);
  }
  function action(a) {
    notice();
    const old = state.screen;
    if (a.type === "open-cart") {
      state.screen = "cart";
      save();
      render();
      return;
    }
    if (a.type === "cancel") {
      state.draft = null;
      state.screen = "menu";
      save();
      render();
      return;
    }
    const t = E.transition(state, a);
    state = t.state;
    if (!t.accepted) {
      notice("먼저 메뉴와 수량을 확인해 주세요.");
      return;
    }
    if (state.mode === "learn") {
      const unexpected =
        (a.type === "dining" && a.value !== "here") ||
        (a.type === "product" && a.value !== "bulgogi") ||
        (a.type === "kind" && a.value !== "set") ||
        (a.type === "side" && a.value !== "fries") ||
        (a.type === "drink" && a.value !== "cola") ||
        (a.type === "payment" && a.value !== "card");
      if (unexpected) {
        state.hints++;
        notice(
          "선택할 수 있는 항목이지만 이번 목표와는 달라요. 목표를 다시 읽고 이전 화면에서 바꿀 수 있어요.",
        );
      }
    }
    save();
    if (E.reached(state) && state.screen !== "receipt") {
      finish();
      return;
    }
    render(old !== state.screen);
    if (voice && state.mode === "learn" && old !== state.screen)
      speak(C.guides[state.screen]);
  }
  app.addEventListener("click", (e) => {
    const b = e.target.closest("button[data-action]");
    if (!b || b.disabled) return;
    const a = b.dataset.action,
      v = b.dataset.value;
    if (a === "intro") {
      level = Number(v);
      if (E.unlocked(level, progressResults())) {
        view = "intro";
        render();
        window.scrollTo(0, 0);
      }
      return;
    }
    if (a === "launch") {
      launch(level);
      return;
    }
    if (a === "free") {
      launch(6, "free");
      return;
    }
    if (a === "home") {
      home();
      return;
    }
    if (a === "resume") {
      try {
        const s = store.session;
        if (
          s?.version !== 1 ||
          !C.levels.some((l) => l.id === s.level) ||
          !C.guides[s.screen] ||
          !Array.isArray(s.cart) ||
          !Array.isArray(s.events)
        )
          throw Error();
        state = s;
        level = s.level;
        view = "practice";
        render();
      } catch {
        store.session = null;
        notice("이전 연습을 불러올 수 없어요. 새 연습을 시작해 주세요.");
      }
      return;
    }
    if (a === "next-level") {
      level = state.level + 1;
      view = "intro";
      state = null;
      render();
      return;
    }
    if (a === "restart") {
      launch(state.level, state.mode);
      return;
    }
    if (a === "finish") {
      finish();
      return;
    }
    if (a === "speak") {
      speak(C.guides[state.screen]);
      return;
    }
    if (a === "voice") {
      voice = !voice;
      if (voice) speak(C.guides[state.screen]);
      else {
        window.speechSynthesis?.cancel();
        native("stopSpeech");
      }
      render(false);
      return;
    }
    if (a === "hint") {
      state.hints++;
      save();
      notice(C.guides[state.screen]);
      speak(C.guides[state.screen]);
      return;
    }
    action(
      a === "quantity"
        ? { type: a, index: Number(v), delta: Number(b.dataset.delta) }
        : a === "remove"
          ? { type: a, index: Number(v) }
          : { type: a, value: v },
    );
  });
  window.addEventListener("beforeunload", () =>
    window.speechSynthesis?.cancel(),
  );
  if (embedded) {
    const n = Number(params.get("level"));
    level = C.levels.some((l) => l.id === n) ? n : 1;
    view = "intro";
  }
  render(false);
})();
