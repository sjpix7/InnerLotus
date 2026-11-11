// Renders docs/store/icon-512.png and feature-graphic-1024x500.png with headless Chromium.
// Usage: npm i playwright && node tools/store/render_store_assets.js  (run from anywhere inside the repo)
const { chromium } = require('playwright');
const path = require('path');
const root = path.resolve(__dirname, '../..');
const fs=require('fs'); const font = (f) => 'data:font/ttf;base64,' + fs.readFileSync(path.join(root, 'app/src/main/res/font', f)).toString('base64');
const lotus = `
  <path fill="#F4A3BC" d="M54,73 C42,73 31,66 26,54 C36,54 47,60 54,73 Z"/>
  <path fill="#F4A3BC" d="M54,73 C66,73 77,66 82,54 C72,54 61,60 54,73 Z"/>
  <path fill="#C77DD0" d="M54,72 C44,66 37,56 37,43 C46,47 52,58 54,72 Z"/>
  <path fill="#C77DD0" d="M54,72 C64,66 71,56 71,43 C62,47 56,58 54,72 Z"/>
  <path fill="#8A5CD6" d="M54,31 C63,42 64,57 54,72 C44,57 45,42 54,31 Z"/>
  <path fill="#8FC9D9" d="M33,77 C44,80.5 64,80.5 75,77 C64,79 44,79 33,77 Z"/>
  <circle cx="54" cy="21.5" r="3" fill="#F2C14E"/>`;
const grad = `<defs><linearGradient id="g" x1="0" y1="0" x2="1" y2="1">
  <stop offset="0" stop-color="#FFF1F5"/><stop offset="0.5" stop-color="#F3E8FB"/><stop offset="1" stop-color="#DDEBFA"/></linearGradient>
  <radialGradient id="glow" cx="0.5" cy="0.5" r="0.5"><stop offset="0" stop-color="#ffffff" stop-opacity="0.9"/><stop offset="1" stop-color="#ffffff" stop-opacity="0"/></radialGradient></defs>`;
const icon = `<!doctype html><html><body style="margin:0">
<svg width="512" height="512" viewBox="16 14 76 76" xmlns="http://www.w3.org/2000/svg">${grad}
<rect x="0" y="0" width="108" height="108" fill="url(#g)"/>
<circle cx="54" cy="54" r="30" fill="url(#glow)"/>${lotus}</svg></body></html>`;
const feature = `<!doctype html><html><head><style>
@font-face{font-family:Corm;src:url('${font('cormorant_semibold.ttf')}')}
@font-face{font-family:CormI;src:url('${font('cormorant_italic.ttf')}')}
body{margin:0;width:1024px;height:500px;overflow:hidden;background:linear-gradient(135deg,#FFF1F5 0%,#F3E8FB 50%,#DDEBFA 100%);font-family:Corm,serif;position:relative}
.glow{position:absolute;left:70px;top:40px;width:420px;height:420px;border-radius:50%;background:radial-gradient(circle,rgba(255,255,255,.95),rgba(255,255,255,0) 70%)}
svg{position:absolute;left:90px;top:40px}
.t{position:absolute;left:500px;top:125px;color:#4B2E63}
h1{font-size:92px;margin:0;line-height:1;letter-spacing:1px}
p{font-family:CormI,serif;font-size:34px;margin:18px 0 0;color:#7A4E8C}
.dots{margin-top:28px;font-size:24px;color:#6B5A78;letter-spacing:.5px}
</style></head><body><div class="glow"></div>
<svg width="380" height="420" viewBox="20 12 68 74" xmlns="http://www.w3.org/2000/svg">${lotus}</svg>
<div class="t"><h1>Inner Lotus</h1><p>Heal, breathe and bloom</p>
<div class="dots">Affirmations · EFT · Sound healing · Journal</div></div></body></html>`;
(async () => {
  const b = await chromium.launch();
  const p = await b.newPage({ viewport: { width: 512, height: 512 } });
  await p.setContent(icon); await p.screenshot({ path: path.join(root, 'docs/store/icon-512.png') });
  await p.setViewportSize({ width: 1024, height: 500 });
  await p.setContent(feature); await p.waitForTimeout(400);
  await p.screenshot({ path: path.join(root, 'docs/store/feature-graphic-1024x500.png') });
  await b.close();
})();
