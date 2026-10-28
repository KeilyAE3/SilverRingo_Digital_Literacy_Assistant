// 분리 소스가 원본. 전달용 단일 HTML에 CSS/JS/SVG를 인라인한다.
const fs = require("node:fs"),
  path = require("node:path"),
  root = path.join(__dirname, ".."),
  web = path.join(root, "web");
let html = fs.readFileSync(path.join(web, "index.html"), "utf8");
html = html.replace(
  /<link\s+rel="stylesheet"\s+href="styles\.css"\s*\/?>/,
  () =>
    "<style>" +
    fs.readFileSync(path.join(web, "styles.css"), "utf8") +
    "</style>",
);
for (const f of ["content.js", "engine.js", "app.js"]) {
  let js = fs.readFileSync(path.join(web, f), "utf8");
  if (f === "app.js") {
    const images = Object.fromEntries(
      fs
        .readdirSync(path.join(web, "assets"))
        .map((name) => [
          name,
          "data:image/svg+xml;base64," +
            fs.readFileSync(path.join(web, "assets", name)).toString("base64"),
        ]),
    );
    js = "const SRImages=" + JSON.stringify(images) + ";\n" + js;
    js = js
      .replace('src="assets/burger.svg"', "src=\"${SRImages['burger.svg']}\"")
      .replaceAll('src="assets/${p.image}"', 'src="${SRImages[p.image]}"');
  }
  html = html.replace(
    `<script src="${f}"></script>`,
    () => "<script>" + js + "</script>",
  );
}
if (/<script\s+src=|<link[^>]+href="styles\.css"/.test(html)) {
  throw new Error("Demo still contains external source references");
}
fs.writeFileSync(path.join(root, "silver-ringo-demo.html"), html);
console.log("Standalone demo built.");
