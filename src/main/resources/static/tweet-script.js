(() => {
  const inEl = document.getElementById('tweetIn');
  const rewriteBtn = document.getElementById('rewriteBtn');
  const celebrateBtn = document.getElementById('celebrateBtn');
  const spinner = document.getElementById('spinner');
  const variantsArea = document.getElementById('variantsArea');
  const historyList = document.getElementById('historyList');
  const emptyHint = document.getElementById('emptyHint');
  const topicHint = document.getElementById('topicHint');
  const emojiLevel = document.getElementById('emojiLevel');
  const modernLevel = document.getElementById('modernLevel');
  const confettiCanvas = document.getElementById('confetti');

  const STORAGE_KEY = 'tweet_rewriter_history_v1';
  let history = [];

  function save(){ localStorage.setItem(STORAGE_KEY, JSON.stringify(history)); }
  function load(){ try{ history = JSON.parse(localStorage.getItem(STORAGE_KEY))||[] }catch(e){ history=[] } }

  function renderHistory(){
    historyList.innerHTML = '';
    if(history.length===0){ emptyHint.style.display='block'; return }
    emptyHint.style.display='none';
    history.slice().reverse().forEach(entry=>{
      const li = document.createElement('li');
      li.innerHTML = `<strong>${escapeHtml(entry.prompt)}</strong><div class="t">${new Date(entry.ts).toLocaleString()}</div>`;
      li.addEventListener('click', ()=>{ showEntry(entry); });
      historyList.appendChild(li);
    })
  }

  function showEntry(entry){
    variantsArea.innerHTML = '';
    const meta = document.createElement('div');
    meta.className = 'meta';
    meta.innerHTML = `<em>Emoji level: ${entry.emojiLevel} • Modernization: ${entry.modernLevel} • Topic: ${escapeHtml(entry.topic)}</em><hr>`;
    variantsArea.appendChild(meta);
    entry.variants.forEach(v => {
      const div = document.createElement('div'); div.className='variant';
      const h = document.createElement('h4'); h.textContent = v.voice || 'VOICE';
      const p = document.createElement('p'); p.textContent = v.tweet || '';
      const btn = document.createElement('button'); btn.textContent='Copy'; btn.className='btn'; btn.style.marginLeft='8px';
      btn.addEventListener('click', ()=>{ navigator.clipboard.writeText(v.tweet); btn.textContent='Copied!'; setTimeout(()=>btn.textContent='Copy',1200)});
      div.appendChild(h); div.appendChild(p); div.appendChild(btn);
      variantsArea.appendChild(div);
    })
  }

  function escapeHtml(s){ return (s||'').replace(/[&<>"']/g, c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c])); }

  async function rewrite(){
    const prompt = inEl.value.trim();
    if(!prompt){ alert('Please enter a tweet to rewrite'); return }
    // map fields to controller params: aI=prompt, springAI=topic, priority=emojiLevel, level=modernLevel
    const params = new URLSearchParams({ aI: prompt, springAI: topicHint.value||'Spring AI', priority: emojiLevel.value, level: modernLevel.value });
    spinner.classList.remove('hidden');
    rewriteBtn.disabled = true;
    variantsArea.innerHTML = '<em>Querying the LLM — please wait...</em>';
    try{
      const res = await fetch('/tweets?'+params.toString());
      if(!res.ok) throw new Error('Server error '+res.status);
      const data = await res.json();
      // expected shape: { tweets: [ { tweet: '...', voice: 'TECH_BRO' }, ... ] }
      const entry = { id: Date.now().toString(), ts: Date.now(), prompt, topic: topicHint.value, emojiLevel: emojiLevel.value, modernLevel: modernLevel.value, variants: data.tweets || [] };
      history.push(entry); save(); renderHistory(); showEntry(entry);
      // celebration
      fireConfetti();
    }catch(err){
      console.error(err); variantsArea.innerHTML = `<div class="error">Error: ${err.message}</div>`;
    }finally{ spinner.classList.add('hidden'); rewriteBtn.disabled=false }
  }

  // confetti simple
  function fireConfetti(){
    if(!confettiCanvas) return; const ctx = confettiCanvas.getContext('2d'); const w=confettiCanvas.width=window.innerWidth; const h=confettiCanvas.height=window.innerHeight;
    const parts=[]; const colors=['#ff6fa3','#ffd200','#7b61ff','#7be495','#ffb86b'];
    for(let i=0;i<140;i++) parts.push({x:Math.random()*w,y:Math.random()*h*0.4,vx:(Math.random()-0.5)*6,vy:Math.random()*4+2,size:Math.random()*8+4,color:colors[Math.floor(Math.random()*colors.length)],life:Math.random()*80+40});
    let t=0; const id=setInterval(()=>{ t++; ctx.clearRect(0,0,w,h); parts.forEach(p=>{p.x+=p.vx;p.y+=p.vy;p.vy+=0.12;p.life--; ctx.fillStyle=p.color; ctx.beginPath(); ctx.ellipse(p.x,p.y,p.size,p.size*0.7,0,0,Math.PI*2); ctx.fill();}); if(t>180){clearInterval(id);ctx.clearRect(0,0,w,h)} },1000/60);
  }

  celebrateBtn.addEventListener('click', ()=>{ fireConfetti(); });
  rewriteBtn.addEventListener('click', rewrite);

  // load history
  load(); renderHistory(); if(history.length){ showEntry(history[history.length-1]) }

})();
