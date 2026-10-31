const { chromium } = require('playwright');
const assert = require('node:assert/strict');
const path = require('node:path'), fs = require('node:fs');
(async()=>{
 const browser=await chromium.launch({headless:true,executablePath:process.env.SR_BROWSER_PATH || undefined,args:['--no-sandbox']});
 const out=path.join(__dirname,'artifacts');fs.mkdirSync(out,{recursive:true});
 const flow=[['dining','here'],['category','burger'],['product','bulgogi'],['bread','none'],['kind','set'],['side','fries'],['drink','cola'],['add'],['review'],['checkout'],['pay'],['points','skip'],['print']];
 for(const width of [420,1440]){
  const context=await browser.newContext({viewport:{width,height:1000}});const p=await context.newPage();const errors=[];p.on('pageerror',e=>errors.push(e.message));
  await p.addInitScript(()=>{window.nativeEvents=[];window.SilverRingoNative={postMessage:s=>window.nativeEvents.push(JSON.parse(s))};});
  const click=async(a,v)=>{const sel=`button[data-action="${a}"]${v===undefined?'':`[data-value="${v}"]`}:not(.option-backdrop *)`;await p.locator(sel).first().click();};
  for(let level=1;level<=5;level++){
   await p.goto('file://'+path.resolve(__dirname,'../web/index.html')+`?embed=1&level=${level}`);await click('launch');
   if(level===5)assert.equal(await p.locator('.highlight').count(),0);
   for(const [a,v]of flow.slice([0,1,3,8,0][level-1])){
    await click(a,v);
    if(level===5&&a==='kind')await p.screenshot({path:path.join(out,`options-${width}.png`),fullPage:true});
    if(await p.locator('.result').count())break;
   }
   if(!(await p.locator('.result').count()))await click('finish');
   assert.match(await p.locator('.result').innerText(),/잘 해냈어요/);
   assert.ok(await p.evaluate(()=>window.nativeEvents.some(e=>e.type==='result'&&e.passed)));
  }
  await p.goto('file://'+path.resolve(__dirname,'../web/index.html')+'?embed=1&level=5');await click('launch');await click('dining','here');await click('category','burger');
  await p.locator('.product img').first().waitFor();await p.waitForFunction(()=>Array.from(document.images).every(i=>i.complete&&i.naturalWidth>0));
  assert.equal(await p.evaluate(()=>document.documentElement.scrollWidth>window.innerWidth),false);
  await p.screenshot({path:path.join(out,`menu-${width}.png`),fullPage:true});
  assert.deepEqual(errors,[]);console.log(`PASS ${width}px: five level flows, final no highlights, native results, images, no horizontal overflow`);await context.close();
 }
 await browser.close();
})().catch(e=>{console.error(e);process.exit(1)});
