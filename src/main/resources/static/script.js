(() => {
  const promptEl = document.getElementById('prompt');
  const askBtn = document.getElementById('askBtn');
  const celebrateBtn = document.getElementById('celebrateBtn');
  const statusEl = document.getElementById('status');
  const answerArea = document.getElementById('answerArea');
  const historyList = document.getElementById('historyList');
  const emptyHint = document.getElementById('emptyHint');
  const canvas = document.getElementById('confettiCanvas');

  const STORAGE_KEY = 'simpsons_trivia_history';
  let history = [];
  let selectedId = null;

  function saveHistory(){
    localStorage.setItem(STORAGE_KEY, JSON.stringify(history));
  }

  function loadHistory(){
    try{
      history = JSON.parse(localStorage.getItem(STORAGE_KEY)) || [];
    }catch(e){ history = [] }
  }

  function renderHistory(){
    historyList.innerHTML = '';
    if(history.length===0){ emptyHint.style.display='block'; return }
    emptyHint.style.display='none';
    history.slice().reverse().forEach(item=>{
      const li = document.createElement('li');
      li.dataset.id = item.id;
      li.innerHTML = `<div class="q">${escapeHtml(item.question)}</div><div class="t">${new Date(item.ts).toLocaleString()}</div>`;
      li.addEventListener('click', ()=>{
        selectedId = item.id;
        showAnswer(item.answer);
        highlightSelected();
      });
      historyList.appendChild(li);
    });
    highlightSelected();
  }

  function highlightSelected(){
    Array.from(historyList.children).forEach(li=>{
      li.style.outline = (li.dataset.id==selectedId)?"3px solid rgba(43,47,144,0.12)":'none';
    })
  }

  function showAnswer(txt){
    answerArea.textContent = txt;
  }

  function escapeHtml(s){
    return s.replace(/[&<>"']/g, c => ({
      '&': '&amp;',
      '<': '&lt;',
      '>': '&gt;',
      '"': '&quot;',
      "'": '&#39;'
    }[c]));
  }

  async function ask(){
    const q = promptEl.value.trim();
    if(!q){statusEl.textContent='Please enter a question.';return}
    statusEl.textContent='Thinking...';
    askBtn.disabled = true;
    try{
      const res = await fetch('/trivia?prompt=' + encodeURIComponent(q));
      if(!res.ok){ throw new Error('Server error: '+res.status) }
      const text = (await res.text()) || 'No response.';
      showAnswer(text);
      const record = { id: Date.now().toString(), ts: Date.now(), question:q, answer:text };
      history.push(record);
      saveHistory();
      renderHistory();
      selectedId = record.id;
      highlightSelected();
      statusEl.textContent = 'Answered ✔';
      // little surprise
      fireConfetti();
    }catch(err){
      console.error(err);
      statusEl.textContent = 'Error: '+err.message;
    }finally{askBtn.disabled=false}
  }

  // Basic confetti/fireworks using canvas
  function fireConfetti(){
    if(!canvas) return;
    const ctx = canvas.getContext('2d');
    const w = canvas.width = window.innerWidth;
    const h = canvas.height = window.innerHeight;
    const particles = [];
    const colors = ['#ffd200','#2b2f90','#ff6f91','#7be495','#ffb86b'];
    for(let i=0;i<120;i++){
      particles.push({
        x: Math.random()*w,
        y: Math.random()*h*0.4,
        vx:(Math.random()-0.5)*6,
        vy:Math.random()*4+2,
        size:Math.random()*8+4,
        color:colors[Math.floor(Math.random()*colors.length)],
        life:Math.random()*60+60
      });
    }
    let t=0;
    const raf = setInterval(()=>{
      t++; ctx.clearRect(0,0,w,h);
      particles.forEach(p=>{
        p.x += p.vx; p.y += p.vy; p.vy += 0.12; p.life--;
        ctx.fillStyle = p.color; ctx.beginPath(); ctx.ellipse(p.x,p.y,p.size, p.size*0.7,0,0,Math.PI*2); ctx.fill();
      });
      if(t>160){ clearInterval(raf); ctx.clearRect(0,0,w,h) }
    },1000/60);
  }

  celebrateBtn.addEventListener('click', ()=>{ fireConfetti(); statusEl.textContent='Woo! Celebrate! 🎉' });
  askBtn.addEventListener('click', ask);

  // enter to ask
  promptEl.addEventListener('keydown', e=>{ if(e.key==='Enter' && (e.ctrlKey||e.metaKey)){ ask(); } });

  // load
  loadHistory(); renderHistory();
  // show last if any
  if(history.length){ const last = history[history.length-1]; selectedId = last.id; showAnswer(last.answer); }

  // small helper: escape bugfix (single quote mapping correct)
  function fixEscape(){
    // ensure escapeHtml works: replace mapping for single quote
  }
})();
