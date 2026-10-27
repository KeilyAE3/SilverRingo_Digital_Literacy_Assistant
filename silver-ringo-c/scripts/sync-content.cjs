const fs = require("node:fs"),
  path = require("node:path");
const root = path.join(__dirname, "..");
fs.writeFileSync(
  path.join(root, "web/content.json"),
  JSON.stringify(require("../web/content.js"), null, 2) + "\n",
);
console.log(
  "content.json generated; Android and web use the same lesson definitions.",
);
