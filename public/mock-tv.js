'use strict';
/* TV وهمي للتجربة في متصفح عادي على الكمبيوتر — للتطوير بس، مش بيتحط في الـ APK.
   للتجربة: حمّل الملف ده قبل app.js. بيعرّف window.TV بس لو مش موجود أصلاً. */
/* -------- لو الصفحة مفتوحة في متصفح عادي (للتجربة) بنعمل TV وهمي -------- */
if (!window.TV) {
  const fire = (o, ms = 300) => setTimeout(() => window.onTv(o), ms);
  let devices = [];
  let tabs = [];
  let activeHost = '';
  let paired = false;
  let lgOk = false;
  const guess = (h) => h.endsWith('.31') ? 'lg' : h.endsWith('.50') ? 'rcv' : 'atv';
  const LG_APPS = [
    { id: 'youtube.leanback.v4', title: 'YouTube', icon: '' }, { id: 'netflix', title: 'Netflix', icon: '' },
    { id: 'shahid', title: 'Shahid', icon: '' }, { id: 'com.webos.app.livetv', title: 'Live TV', icon: '' },
    { id: 'com.webos.app.hdmi1', title: 'HDMI 1', icon: '' }, { id: 'amazon', title: 'Prime Video', icon: '' }];
  const LG_INPUTS = [
    { id: 'HDMI_1', label: 'HDMI 1', appId: 'com.webos.app.hdmi1', connected: true },
    { id: 'HDMI_2', label: 'PlayStation', appId: 'com.webos.app.hdmi2', connected: true },
    { id: 'HDMI_3', label: 'HDMI 3', appId: 'com.webos.app.hdmi3', connected: false }];
  const emitTabs = () => fire({ type: 'tabs', list: tabs.map(t => Object.assign({}, t, { active: t.host === activeHost })) }, 0);
  const ensure = (host, name, kind) => {
    let t = tabs.find(x => x.host === host);
    if (t) return t;
    if (tabs.length >= 4) { fire({ type: 'error', msg: T('التابات مليانة (4 أجهزة)') }, 0); return null; }
    t = { host, name, kind: kind || (devices.find(d => d.host === host) || {}).kind || guess(host), state: 'connecting', volume: -1 };
    tabs.push(t);
    devices = [{ host, name, kind: t.kind }].concat(devices.filter(d => d.host !== host));
    return t;
  };
  const replay = (t) => {
    const h = t.host;
    fire({ type: 'kind', kind: t.kind, host: h }, 0);
    fire({ type: 'status', state: t.state, host: h, name: t.name, vendor: t.vendor || '', model: t.model || '', kind: t.kind }, 0);
    if (t.volume >= 0) fire({ type: 'volume', level: String(t.volume), max: '100', muted: '', host: h }, 0);
    fire({ type: 'app', pkg: t.app || '', host: h }, 0);
    fire({ type: 'apps', list: t.kind === 'lg' && t.state === 'connected' ? LG_APPS : [], host: h }, 0);
    fire({ type: 'inputs', list: t.kind === 'lg' && t.state === 'connected' ? LG_INPUTS : [], host: h }, 0);
    if (t.state === 'pair') fire({ type: 'needPair', host: h, name: t.name }, 0);
    if (t.kind === 'lg' && t.state === 'connected') fire({ type: 'channel', num: '124', name: 'MBC 1', host: h }, 0);
  };
  const run = (t) => {
    if (t.state === 'connected') return;
    t.state = 'connecting'; emitTabs();
    let delay = 600;
    if (t.kind === 'lg' && !lgOk) { lgOk = true; delay = 2600; fire({ type: 'lgPrompt', host: t.host, name: t.name }, 500); fire({ type: 'paired', host: t.host }, 2500); }
    setTimeout(() => {
      if (t.kind === 'atv' && !paired) { t.state = 'pair'; emitTabs(); if (t.host === activeHost) fire({ type: 'needPair', host: t.host, name: t.name }, 0); return; }
      t.state = 'connected';
      t.vendor = t.kind === 'lg' ? 'LG' : t.kind === 'rcv' ? 'Receiver' : 'Xiaomi';
      t.model = t.kind === 'lg' ? 'OLED55C1' : t.kind === 'rcv' ? t.name : 'MIBOX4';
      t.volume = t.kind === 'rcv' ? -1 : t.kind === 'lg' ? 12 : 9;
      t.app = t.kind === 'lg' ? 'com.webos.app.hdmi2' : t.kind === 'atv' ? 'com.google.android.youtube.tv' : '';
      emitTabs();
      if (t.host === activeHost) replay(t);
    }, delay);
  };
  window.TV = {
    saved: () => JSON.stringify(devices),
    lastHost: () => '',
    tabs: () => JSON.stringify(tabs.map(t => ({ host: t.host, name: t.name, kind: t.kind }))),
    isReady: () => true,
    discover() {
      fire({ type: 'found', host: '192.168.1.23', name: 'Mi Box S' }, 700);
      fire({ type: 'found', host: '192.168.1.31', name: '[LG] webOS TV OLED55C1', kind: 'lg' }, 1100);
      fire({ type: 'found', host: '192.168.1.50', name: 'TIGER T8 HIGH CLASS V2', kind: 'rcv' }, 1300);
      fire({ type: 'found', host: '192.168.1.40', name: '' }, 1500);
      fire({ type: 'scanDone' }, 2500);
    },
    connect(host, name, kind) {
      const t = ensure(host, name, kind); if (!t) return;
      activeHost = host; emitTabs(); replay(t); run(t);
    },
    addTab(host, name, kind) { const t = ensure(host, name, kind); if (!t) return; emitTabs(); run(t); },
    activate(host) { const t = tabs.find(x => x.host === host); if (!t) return; activeHost = host; emitTabs(); replay(t); },
    removeDevice(host) {
      tabs = tabs.filter(x => x.host !== host);
      if (activeHost === host) { activeHost = tabs.length ? tabs[0].host : ''; if (activeHost) replay(tabs[0]); }
      emitTabs();
    },
    disconnect() { if (activeHost) this.removeDevice(activeHost); }, forget() {},
    pairStart(h, n) { fire({ type: 'pairCode' }, 500); },
    pairFinish(code) {
      if (code !== 'A1B2C3') return fire({ type: 'pairError', msg: T('الكود غلط، اتأكد منه'), retry: '1' });
      paired = true; fire({ type: 'paired' }); const t = tabs.find(x => x.host === activeHost); if (t) { t.state = 'offline'; run(t); }
    },
    pairCancel() {},
    key(c) { console.log('key', c); }, keyDir(c, d) { console.log('key', c, d); },
    text(t) { console.log('text', t); }, launch(l) { console.log('launch', l); },
    getMac: () => '', setMac: (m) => /^([0-9a-f]{2}[:-]){5}[0-9a-f]{2}$/i.test(m.trim()),
    diagnostics: () => 'app 1.0.x • lg • 192.168.1.31\n16:20:01  main: wss:3001 OK\n16:20:01  buttons socket OK', share() {},
    switchInput(id) { console.log('input', id); },
    takeShare() { if (location.hash !== '#share' || this._shared) return ''; this._shared = true; return JSON.stringify({ url: 'https://example.com', title: 'Mohamed Salah — Best Goals 2026' }); },
    castTo(h, url) { const t = tabs.find(x => x.host === h); if (!t || t.state !== 'connected' || t.kind === 'rcv') return false; console.log('cast', h, url); fire({ type: 'casted', name: t.name }, 200); return true; },
    openMirror() { console.log('mirror'); },
    setVolume(v) { console.log('setVolume', v); fire({ type: 'volume', level: String(v), max: '100', muted: '', host: activeHost }, 100); },
    voice() { fire({ type: 'voice', text: 'محمد صلاح' }, 300); },
    inputsOf(h) { const t = tabs.find(x => x.host === h); return JSON.stringify(t && t.kind === 'lg' && t.state === 'connected' ? LG_INPUTS : []); },
    switchInputOn(h, id) { const t = tabs.find(x => x.host === h); if (!t || t.state !== 'connected') return false; console.log('switchOn', h, id); return true; }, rawKey(k) { console.log('raw', k); },
    pointer(dx, dy) { console.log('pointer', dx, dy); }, click() { console.log('click'); }, scroll() {}, kind: () => state.kind,
    getNotif: () => true, setNotif() {},
    vibrate() {}, setVibrate() {}, getVibrate: () => true, resetIdentity() {},
    setBars() {}, appVersion: () => '1.0.1',
    checkUpdate(manual) { fire(manual || location.hash === '#update' ? { type: 'update', current: '1.0.1', latest: '1.0.2' } : { type: 'noUpdate', manual: '' }, 900); },
    startUpdate() { let p = 0; const t = setInterval(() => { p += 20; window.onTv({ type: 'updateProgress', pct: String(p) }); if (p >= 100) clearInterval(t); }, 300); },
  };
}
