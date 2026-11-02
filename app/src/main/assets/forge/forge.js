/* The Forge - LIA's 3D website-build scene (v2: "Forge Reactor").
 *
 * Driven entirely from Kotlin (see ForgeScene.kt) through the global `Forge` object:
 *   Forge.init({ theme, pixelRatioCap, lowRam, reducedMotion })
 *   Forge.setStage(name)            PARSING_PROMPT | SCAFFOLD | STYLING | IMAGERY | MOTION | FINALIZING | DONE
 *   Forge.setProgress(0..1)
 *   Forge.onChunk(len, totalChars, tokensPerSec)
 *   Forge.addSection(name)
 *   Forge.pushLog(text)
 *   Forge.complete()                reveal transition into the real website
 *   Forge.fail()                    failure / cancel look
 *   Forge.setReducedMotion(bool)
 *   Forge.dispose()
 * Nothing here is random decoration: every visual is a function of those real events.
 * Optional native bridge (`ForgeBridge`): onReady(), onRevealFinished(), onWebGLFailed().
 *
 * Scene: a reactor core with three gyroscopic rings charges as the stream flows; code glyphs
 * spiral in, data beams strike the core, ripples run across the floor grid, glass slabs (one per
 * page section) stack above it, and a ring of six stage nodes lights up. Bloom + vignette +
 * chromatic aberration sit on top. The camera flies in, kicks on every stage change and
 * punches through the stack on completion.
 */
import * as THREE from 'three';
import { EffectComposer } from './jsm/postprocessing/EffectComposer.js';
import { RenderPass } from './jsm/postprocessing/RenderPass.js';
import { UnrealBloomPass } from './jsm/postprocessing/UnrealBloomPass.js';
import { ShaderPass } from './jsm/postprocessing/ShaderPass.js';
import { OutputPass } from './jsm/postprocessing/OutputPass.js';

var STAGES = ['PARSING_PROMPT', 'SCAFFOLD', 'STYLING', 'IMAGERY', 'MOTION', 'FINALIZING'];
var STAGE_LABELS = ['Idea', 'Structure', 'Styling', 'Visuals', 'Motion', 'Finalize'];
var MAX_SLABS = 10;
var MAX_CARDS = 4;
var MAX_BEAMS = 22;
var RIPPLES = 6;
var GLYPHS = ['{', '}', '<', '>', '/', '=', ';', '(', ')', '0', '1', '#', '$', '*', '&', '%'];

var S = null; // live scene state, null until init / after dispose

function bridge(name) {
  try { if (window.ForgeBridge && window.ForgeBridge[name]) window.ForgeBridge[name](); } catch (e) { /* no bridge */ }
}
function clamp(v, a, b) { return Math.max(a, Math.min(b, v)); }
function lerp(a, b, t) { return a + (b - a) * t; }
function ease(t) { return t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2; }
function easeOut(t) { return 1 - Math.pow(1 - t, 3); }
function col(hex) { return new THREE.Color(hex || '#ffffff'); }

/* ---------- shaders ---------- */
var BACK_VERT = 'varying vec3 vDir; void main(){ vDir = normalize(position); gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0); }';
var BACK_FRAG = 'uniform vec3 uTop; uniform vec3 uBot; uniform vec3 uTint; uniform float uTime; uniform float uPower; varying vec3 vDir;' +
  'float h(vec3 p){ return fract(sin(dot(p, vec3(12.9898, 78.233, 37.719))) * 43758.5453); }' +
  'float n(vec3 p){ vec3 i = floor(p); vec3 f = fract(p); f = f*f*(3.0-2.0*f);' +
  ' return mix(mix(mix(h(i), h(i+vec3(1,0,0)), f.x), mix(h(i+vec3(0,1,0)), h(i+vec3(1,1,0)), f.x), f.y),' +
  ' mix(mix(h(i+vec3(0,0,1)), h(i+vec3(1,0,1)), f.x), mix(h(i+vec3(0,1,1)), h(i+vec3(1,1,1)), f.x), f.y), f.z); }' +
  'void main(){ float y = vDir.y * 0.5 + 0.5; vec3 c = mix(uBot, uTop * 1.6, smoothstep(0.30, 0.78, y));' +
  ' float neb = n(vDir * 3.0 + vec3(uTime * 0.02)) * 0.6 + n(vDir * 7.0 - vec3(uTime * 0.03)) * 0.4;' +
  ' vec3 tint = mix(uTint, uTop * 4.0, 0.55);' +
  ' c += tint * neb * neb * (0.07 + uPower * 0.05);' +
  ' vec3 sp = vDir * 230.0; vec3 cell = floor(sp); vec3 loc = fract(sp) - 0.5;' +
  ' float star = step(0.9975, h(cell)) * smoothstep(0.32, 0.0, length(loc)); c += star * 0.7;' +
  ' gl_FragColor = vec4(c, 1.0); }';

var CORE_VERT = 'varying vec3 vN; varying vec3 vV;' +
  'void main(){ vN = normalize(normalMatrix * normal); vec4 mv = modelViewMatrix * vec4(position, 1.0);' +
  ' vV = normalize(-mv.xyz); gl_Position = projectionMatrix * mv; }';
var CORE_FRAG = 'uniform vec3 uA; uniform vec3 uB; uniform float uTime; uniform float uPower; uniform float uDim;' +
  'varying vec3 vN; varying vec3 vV;' +
  'void main(){ float f = pow(1.0 - max(dot(normalize(vN), normalize(vV)), 0.0), 2.2);' +
  ' float n = sin(vN.x*6.0+uTime*1.6) * sin(vN.y*6.0-uTime*1.2) * sin(vN.z*6.0+uTime) * 0.5 + 0.5;' +
  ' vec3 c = mix(uA, uB, n*0.55 + f*0.45) * (0.62 + min(uPower, 1.2)*0.3);' +
  ' float a = (0.38 + f*0.7) * (0.6 + min(uPower, 1.2)*0.25) * (1.0 - uDim*0.85);' +
  ' gl_FragColor = vec4(c, a); }';

var SLAB_VERT = 'varying vec2 vUv; void main(){ vUv = uv; gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0); }';
var SLAB_FRAG = 'uniform vec3 uA; uniform vec3 uB; uniform vec2 uSize; uniform float uTime; uniform float uAppear;' +
  'uniform float uStyle; uniform float uShimmer; uniform float uSeed; uniform float uDim; uniform float uScan; uniform float uHero;' +
  'varying vec2 vUv;' +
  'float h(vec2 p){ return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453); }' +
  'void main(){' +
  ' vec2 p = (vUv - 0.5) * uSize; float r = 0.08; vec2 q = abs(p) - uSize*0.5 + r;' +
  ' float d = length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r; if (d > 0.0) discard;' +
  ' float edge = 1.0 - smoothstep(0.0, 0.055, -d);' +
  ' float rows = floor(uSize.y * 6.0) + 2.0; float ry = vUv.y * rows; float row = floor(ry);' +
  ' float inRow = step(0.28, fract(ry)) * (1.0 - step(0.62, fract(ry)));' +
  ' float len = 0.22 + h(vec2(row, uSeed)) * 0.62;' +
  ' float bar = inRow * step(0.07, vUv.x) * (1.0 - step(0.07 + len, vUv.x)) * uStyle;' +
  ' float sweep = fract(uTime*0.28 + uSeed*0.37);' +
  ' float sh = smoothstep(0.16, 0.0, abs(vUv.x - sweep)) * uShimmer;' +
  ' float scan = smoothstep(0.045, 0.0, abs(vUv.y - uScan)) * step(uScan, 0.999) * step(0.001, uScan);' +
  ' float chrome = 0.0; if (uHero > 0.5) { vec2 dp = vec2(vUv.x * uSize.x - 0.18, (1.0 - vUv.y) * uSize.y - 0.14);' +
  '   for (int i = 0; i < 3; i++) { chrome += smoothstep(0.045, 0.03, length(dp - vec2(float(i) * 0.13, 0.0))); } }' +
  ' float gloss = smoothstep(0.0, 1.0, vUv.y) * 0.08;' +
  ' vec3 base = mix(uA * 0.10, uB * 0.24, vUv.y) + gloss;' +
  ' vec3 c = base + bar * uB * 1.1 + sh * uA * 1.0 + edge * uA * (1.0 + uShimmer*0.5) + scan * vec3(1.0) * 0.9 + chrome * uB;' +
  ' float a = (0.18 + uStyle*0.16 + edge*0.7 + bar*0.6 + sh*0.4 + scan*0.8 + chrome*0.7) * uAppear * (1.0 - uDim*0.55);' +
  ' gl_FragColor = vec4(c * 0.72, a); }';

var PART_VERT = 'attribute float aA; attribute float aS; attribute float aG; attribute vec3 aC; varying float vA; varying vec3 vC; varying float vG;' +
  'uniform float uScale;' +
  'void main(){ vA = aA; vC = aC; vG = aG; vec4 mv = modelViewMatrix * vec4(position, 1.0);' +
  ' gl_PointSize = aS * uScale * (1.0 / -mv.z); gl_Position = projectionMatrix * mv; }';
var PART_FRAG = 'uniform sampler2D uAtlas; varying float vA; varying vec3 vC; varying float vG;' +
  'void main(){ vec2 pc = gl_PointCoord; float d = length(pc - 0.5);' +
  ' float glow = smoothstep(0.5, 0.0, d) * 0.45; float g = 0.0;' +
  ' if (vG >= 0.0) { float col = mod(vG, 4.0); float row = floor(vG / 4.0);' +
  '   vec2 uv = vec2((col + pc.x) / 4.0, 1.0 - (row + pc.y) / 4.0); g = texture2D(uAtlas, uv).a; }' +
  ' float a = (g * 1.1 + glow) * vA; if (a < 0.01) discard; gl_FragColor = vec4(vC * (0.7 + g * 0.5), a * 0.8); }';

var RING_VERT = 'varying vec2 vP; void main(){ vP = position.xy; gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0); }';
var RING_FRAG = 'uniform vec3 uA; uniform float uProg; uniform float uDim; varying vec2 vP;' +
  'void main(){ float ang = atan(vP.x, vP.y); if (ang < 0.0) ang += 6.2831853; float f = ang / 6.2831853;' +
  ' float on = step(f, uProg); float head = smoothstep(0.03, 0.0, abs(f - uProg)) * step(0.001, uProg);' +
  ' vec3 c = mix(vec3(0.45), uA, on) + head * vec3(1.0); float a = (mix(0.18, 0.95, on) + head) * (1.0 - uDim*0.6);' +
  ' gl_FragColor = vec4(c * 0.72, a); }';

var FLOOR_VERT = 'varying vec2 vXZ; void main(){ vec4 w = modelMatrix * vec4(position, 1.0); vXZ = w.xz; gl_Position = projectionMatrix * viewMatrix * w; }';
var FLOOR_FRAG = 'uniform vec3 uA; uniform float uTime; uniform float uPower; uniform vec2 uRip[' + RIPPLES + ']; varying vec2 vXZ;' +
  'float grid(vec2 p, float s){ vec2 g = abs(fract(p/s - 0.5) - 0.5) / fwidth(p/s); return 1.0 - min(min(g.x, g.y), 1.0); }' +
  'void main(){ float d = length(vXZ); float fade = smoothstep(10.5, 1.2, d);' +
  ' float g = grid(vXZ, 0.7) * 0.5 + grid(vXZ, 3.5) * 0.7;' +
  ' float rip = 0.0; for (int i = 0; i < ' + RIPPLES + '; i++) { float t = uTime - uRip[i].x;' +
  '   if (t > 0.0 && t < 3.2) { float r = t * 3.6; rip += smoothstep(0.42, 0.0, abs(d - r)) * (1.0 - t/3.2) * uRip[i].y; } }' +
  ' float pulse = 0.10 + 0.10 * sin(uTime * 1.4 - d * 0.9) + uPower * 0.06;' +
  ' vec3 c = uA * (g * (pulse + rip * 2.4) + rip * 0.8);' +
  ' gl_FragColor = vec4(c, (g * (0.32 + rip) + rip * 0.45) * fade); }';

var POST_FRAG = 'uniform sampler2D tDiffuse; uniform float uAber; uniform float uVig; varying vec2 vUv;' +
  'void main(){ vec2 c = vUv - 0.5; float r2 = dot(c, c); vec2 off = c * uAber * (0.4 + r2 * 2.0);' +
  ' vec3 col = vec3(texture2D(tDiffuse, vUv - off).r, texture2D(tDiffuse, vUv).g, texture2D(tDiffuse, vUv + off).b);' +
  ' col *= 1.0 - smoothstep(0.18, 0.62, r2) * uVig; gl_FragColor = vec4(col, 1.0); }';

/* ---------- canvas textures ---------- */
function roundRect(ctx, x, y, w, h, r) {
  ctx.beginPath(); ctx.moveTo(x + r, y); ctx.arcTo(x + w, y, x + w, y + h, r); ctx.arcTo(x + w, y + h, x, y + h, r);
  ctx.arcTo(x, y + h, x, y, r); ctx.arcTo(x, y, x + w, y, r); ctx.closePath();
}
function makeCardTexture(text, theme, dotColor) {
  var c = document.createElement('canvas'); c.width = 512; c.height = 96;
  var g = c.getContext('2d');
  roundRect(g, 4, 4, 504, 88, 30);
  var grad = g.createLinearGradient(0, 0, 512, 0);
  grad.addColorStop(0, 'rgba(255,255,255,0.20)'); grad.addColorStop(1, 'rgba(255,255,255,0.07)');
  g.fillStyle = grad; g.fill();
  g.lineWidth = 2; g.strokeStyle = theme.accent; g.globalAlpha = 0.65; g.stroke(); g.globalAlpha = 1;
  g.fillStyle = dotColor; g.beginPath(); g.arc(44, 48, 9, 0, Math.PI * 2); g.fill();
  g.fillStyle = theme.text; g.font = '600 31px system-ui, -apple-system, Roboto, sans-serif'; g.textBaseline = 'middle';
  var t = text; while (g.measureText(t).width > 400 && t.length > 4) t = t.slice(0, -2);
  if (t !== text) t += '...';
  g.fillText(t, 74, 50);
  var tex = new THREE.CanvasTexture(c); tex.colorSpace = THREE.SRGBColorSpace; tex.minFilter = THREE.LinearFilter; tex.generateMipmaps = false;
  return tex;
}
function makeLabelTexture(text, theme) {
  var c = document.createElement('canvas'); c.width = 256; c.height = 64;
  var g = c.getContext('2d');
  g.fillStyle = theme.textSecondary; g.font = '600 28px system-ui, -apple-system, Roboto, sans-serif';
  g.textAlign = 'center'; g.textBaseline = 'middle'; g.fillText(text, 128, 34);
  var tex = new THREE.CanvasTexture(c); tex.colorSpace = THREE.SRGBColorSpace; tex.minFilter = THREE.LinearFilter; tex.generateMipmaps = false;
  return tex;
}
function makeGlyphAtlas() {
  var c = document.createElement('canvas'); c.width = c.height = 256;
  var g = c.getContext('2d'); g.fillStyle = '#fff'; g.font = '700 46px ui-monospace, Menlo, Consolas, monospace';
  g.textAlign = 'center'; g.textBaseline = 'middle';
  for (var i = 0; i < 16; i++) g.fillText(GLYPHS[i], (i % 4) * 64 + 32, Math.floor(i / 4) * 64 + 34);
  var tex = new THREE.CanvasTexture(c); tex.minFilter = THREE.LinearFilter; tex.generateMipmaps = false;
  return tex;
}
function makeRingTexture() {
  var c = document.createElement('canvas'); c.width = c.height = 256;
  var g = c.getContext('2d'); var grd = g.createRadialGradient(128, 128, 90, 128, 128, 126);
  grd.addColorStop(0, 'rgba(255,255,255,0)'); grd.addColorStop(0.75, 'rgba(255,255,255,0.9)'); grd.addColorStop(1, 'rgba(255,255,255,0)');
  g.fillStyle = grd; g.fillRect(0, 0, 256, 256);
  return new THREE.CanvasTexture(c);
}
function makeHaloTexture() {
  var c = document.createElement('canvas'); c.width = c.height = 128;
  var g = c.getContext('2d'); var grd = g.createRadialGradient(64, 64, 0, 64, 64, 64);
  grd.addColorStop(0, 'rgba(255,255,255,0.9)'); grd.addColorStop(0.35, 'rgba(255,255,255,0.25)'); grd.addColorStop(1, 'rgba(255,255,255,0)');
  g.fillStyle = grd; g.fillRect(0, 0, 128, 128);
  return new THREE.CanvasTexture(c);
}

/* ---------- scene build ---------- */
function init(cfg) {
  if (S) dispose();
  cfg = cfg || {};
  var theme = cfg.theme || {};
  theme.bg = theme.bg || '#06050e'; theme.bgTop = theme.bgTop || '#15113a';
  theme.accent = theme.accent || '#ffb547'; theme.accent2 = theme.accent2 || '#ff8a3d';
  theme.glow = theme.glow || '#ffc978'; theme.lagoon = theme.lagoon || '#3fe0d0';
  theme.lotus = theme.lotus || '#ff6fae'; theme.text = theme.text || '#eee9ff';
  theme.textSecondary = theme.textSecondary || '#a7a0cc'; theme.success = theme.success || '#3fe0d0';
  theme.error = theme.error || '#ff7a85';
  var root = document.documentElement;
  root.style.setProperty('--bg', theme.bg); root.style.setProperty('--top', theme.bgTop);
  root.classList.remove('failed');

  var canvas = document.getElementById('c');
  var renderer;
  try {
    renderer = new THREE.WebGLRenderer({ canvas: canvas, antialias: false, alpha: false, powerPreference: 'high-performance' });
  } catch (e) { bridge('onWebGLFailed'); return; }
  if (!renderer || !renderer.getContext()) { bridge('onWebGLFailed'); return; }

  var cap = cfg.pixelRatioCap || (cfg.lowRam ? 1.5 : 2);
  var pr = Math.min(window.devicePixelRatio || 1, cap);
  renderer.setPixelRatio(pr);
  renderer.setClearColor(col(theme.bg), 1);

  var scene = new THREE.Scene();
  var camera = new THREE.PerspectiveCamera(48, 1, 0.1, 80);

  var stageColors = {
    PARSING_PROMPT: col(theme.lagoon), SCAFFOLD: col(theme.lagoon), STYLING: col(theme.lotus),
    IMAGERY: col(theme.accent2), MOTION: col(theme.accent), FINALIZING: col(theme.glow), DONE: col(theme.success)
  };
  var curColor = stageColors.PARSING_PROMPT.clone();
  var curColor2 = col(theme.lotus);

  var disposables = [];
  function track(o) { disposables.push(o); return o; }
  var T0 = { value: 0 };

  /* backdrop: gradient + nebula + stars on an inverted sphere */
  var backMat = track(new THREE.ShaderMaterial({
    uniforms: { uTop: { value: col(theme.bgTop) }, uBot: { value: col(theme.bg) }, uTint: { value: curColor.clone() }, uTime: T0, uPower: { value: 0.2 } },
    vertexShader: BACK_VERT, fragmentShader: BACK_FRAG, side: THREE.BackSide, depthWrite: false
  }));
  var back = new THREE.Mesh(track(new THREE.SphereGeometry(40, 32, 20)), backMat); back.renderOrder = -10; back.frustumCulled = false;
  scene.add(back);

  /* floor grid with ripples */
  var rip = []; for (var ri = 0; ri < RIPPLES; ri++) rip.push(new THREE.Vector2(-99, 0));
  var floorMat = track(new THREE.ShaderMaterial({
    uniforms: { uA: { value: curColor.clone() }, uTime: T0, uPower: { value: 0.2 }, uRip: { value: rip } },
    vertexShader: FLOOR_VERT, fragmentShader: FLOOR_FRAG, transparent: true, depthWrite: false, blending: THREE.AdditiveBlending
  }));
  var floor = new THREE.Mesh(track(new THREE.PlaneGeometry(26, 26)), floorMat);
  floor.rotation.x = -Math.PI / 2; floor.position.y = -2.3; scene.add(floor);

  /* reactor core: fresnel orb + wire shell + three gyroscopic rings */
  var coreMat = track(new THREE.ShaderMaterial({
    uniforms: { uA: { value: curColor.clone() }, uB: { value: curColor2.clone() }, uTime: T0, uPower: { value: 0.2 }, uDim: { value: 0 } },
    vertexShader: CORE_VERT, fragmentShader: CORE_FRAG, transparent: true, depthWrite: false, blending: THREE.AdditiveBlending
  }));
  var coreGroup = new THREE.Group(); scene.add(coreGroup);
  coreGroup.add(new THREE.Mesh(track(new THREE.SphereGeometry(0.55, cfg.lowRam ? 28 : 44, cfg.lowRam ? 20 : 32)), coreMat));
  var shellMat = track(new THREE.MeshBasicMaterial({ color: curColor.clone(), wireframe: true, transparent: true, opacity: 0.35, blending: THREE.AdditiveBlending, depthWrite: false }));
  var shell = new THREE.Mesh(track(new THREE.IcosahedronGeometry(0.78, 1)), shellMat); coreGroup.add(shell);
  var gyros = [];
  var gyroColors = [col(theme.accent), col(theme.lotus), col(theme.lagoon)];
  for (var gi = 0; gi < 3; gi++) {
    var gm = track(new THREE.MeshBasicMaterial({ color: gyroColors[gi].clone(), transparent: true, opacity: 0.8, blending: THREE.AdditiveBlending, depthWrite: false }));
    var gring = new THREE.Mesh(track(new THREE.TorusGeometry(0.98 + gi * 0.2, 0.014, 8, 128)), gm);
    gring.rotation.set(gi * 1.05, gi * 0.6, 0); coreGroup.add(gring);
    gyros.push({ mesh: gring, mat: gm, speed: 0.6 + gi * 0.35, axis: gi });
  }
  var haloTex = track(makeHaloTexture());
  var haloMat = track(new THREE.SpriteMaterial({ map: haloTex, color: curColor.clone(), transparent: true, depthWrite: false, blending: THREE.AdditiveBlending, opacity: 0.5 }));
  var halo = new THREE.Sprite(haloMat); halo.scale.set(4.2, 4.2, 1); coreGroup.add(halo);

  /* glyph particle stream */
  var N = cfg.lowRam ? 700 : 1800;
  var pPos = new Float32Array(N * 3), pA = new Float32Array(N), pS = new Float32Array(N), pC = new Float32Array(N * 3), pG = new Float32Array(N);
  var pt = new Float32Array(N).fill(1), pSpd = new Float32Array(N), pAng = new Float32Array(N), pR0 = new Float32Array(N), pY0 = new Float32Array(N), pSpin = new Float32Array(N);
  var pGeo = track(new THREE.BufferGeometry());
  pGeo.setAttribute('position', new THREE.BufferAttribute(pPos, 3));
  pGeo.setAttribute('aA', new THREE.BufferAttribute(pA, 1));
  pGeo.setAttribute('aS', new THREE.BufferAttribute(pS, 1));
  pGeo.setAttribute('aG', new THREE.BufferAttribute(pG, 1));
  pGeo.setAttribute('aC', new THREE.BufferAttribute(pC, 3));
  var atlas = track(makeGlyphAtlas());
  var pMat = track(new THREE.ShaderMaterial({
    uniforms: { uScale: { value: 64 * pr }, uAtlas: { value: atlas } }, vertexShader: PART_VERT, fragmentShader: PART_FRAG,
    transparent: true, depthWrite: false, blending: THREE.AdditiveBlending
  }));
  var points = new THREE.Points(pGeo, pMat); points.frustumCulled = false; scene.add(points);

  /* drifting dust (no glyph: aG = -1) */
  var DUST = cfg.lowRam ? 90 : 260;
  var dPos = new Float32Array(DUST * 3), dA = new Float32Array(DUST).fill(0.22), dS = new Float32Array(DUST), dC = new Float32Array(DUST * 3), dG = new Float32Array(DUST).fill(-1);
  for (var i = 0; i < DUST; i++) {
    dPos[i * 3] = (Math.random() - 0.5) * 16; dPos[i * 3 + 1] = (Math.random() - 0.5) * 12; dPos[i * 3 + 2] = (Math.random() - 0.5) * 10 - 2;
    dS[i] = 1.2 + Math.random() * 2.4; var sc = col(theme.textSecondary); dC[i * 3] = sc.r; dC[i * 3 + 1] = sc.g; dC[i * 3 + 2] = sc.b;
  }
  var dGeo = track(new THREE.BufferGeometry());
  dGeo.setAttribute('position', new THREE.BufferAttribute(dPos, 3));
  dGeo.setAttribute('aA', new THREE.BufferAttribute(dA, 1));
  dGeo.setAttribute('aS', new THREE.BufferAttribute(dS, 1));
  dGeo.setAttribute('aG', new THREE.BufferAttribute(dG, 1));
  dGeo.setAttribute('aC', new THREE.BufferAttribute(dC, 3));
  var dMat = track(new THREE.ShaderMaterial({
    uniforms: { uScale: { value: 14 * pr }, uAtlas: { value: atlas } }, vertexShader: PART_VERT, fragmentShader: PART_FRAG,
    transparent: true, depthWrite: false, blending: THREE.AdditiveBlending
  }));
  var dust = new THREE.Points(dGeo, dMat); dust.frustumCulled = false; scene.add(dust);

  /* data beams striking the core on every chunk */
  var bPos = new Float32Array(MAX_BEAMS * 6), bCol = new Float32Array(MAX_BEAMS * 6);
  var bGeo = track(new THREE.BufferGeometry());
  bGeo.setAttribute('position', new THREE.BufferAttribute(bPos, 3));
  bGeo.setAttribute('color', new THREE.BufferAttribute(bCol, 3));
  var bMat = track(new THREE.LineBasicMaterial({ vertexColors: true, transparent: true, blending: THREE.AdditiveBlending, depthWrite: false }));
  var beamLines = new THREE.LineSegments(bGeo, bMat); beamLines.frustumCulled = false; scene.add(beamLines);
  var beams = []; for (var bi = 0; bi < MAX_BEAMS; bi++) beams.push({ life: 0, max: 1, sx: 0, sy: 0, sz: 0 });

  /* shockwave rings (sprites) */
  var ringTex = track(makeRingTexture());
  var shocks = [];
  for (var si = 0; si < 4; si++) {
    var sm = track(new THREE.SpriteMaterial({ map: ringTex, color: curColor.clone(), transparent: true, depthWrite: false, blending: THREE.AdditiveBlending, opacity: 0 }));
    var ss = new THREE.Sprite(sm); ss.visible = false; coreGroup.add(ss); shocks.push({ spr: ss, mat: sm, t: 9, life: 1, size: 7 });
  }

  /* slab stack: one glass slab per detected page section */
  var slabGroup = new THREE.Group(); scene.add(slabGroup);
  var slabGeo = track(new THREE.PlaneGeometry(1, 1));
  var slabs = [];
  function addSlab(name) {
    if (slabs.length >= MAX_SLABS) return;
    var idx = slabs.length;
    var tall = idx === 0 ? 0.95 : 0.52;
    var width = 3.0;
    var mat = track(new THREE.ShaderMaterial({
      uniforms: {
        uA: { value: curColor.clone() }, uB: { value: curColor2.clone() }, uSize: { value: new THREE.Vector2(width, tall) },
        uTime: T0, uAppear: { value: 0 }, uStyle: { value: 0 }, uShimmer: { value: 0 }, uSeed: { value: idx * 1.7 + 0.3 }, uDim: { value: 0 },
        uScan: { value: 0 }, uHero: { value: idx === 0 ? 1 : 0 }
      },
      vertexShader: SLAB_VERT, fragmentShader: SLAB_FRAG, transparent: true, depthWrite: false, side: THREE.DoubleSide
    }));
    var mesh = new THREE.Mesh(slabGeo, mat); mesh.scale.set(width, tall, 1);
    slabGroup.add(mesh);
    slabs.push({ mesh: mesh, mat: mat, name: name, born: S.time, h: tall, w: width, drift: new THREE.Vector3((Math.random() - 0.5) * 2, -Math.random() * 2, (Math.random() - 0.5) * 2) });
    ripple(0.9); shock(S.curColor, 5.5, 0.9);
    S.dirty = true;
  }

  /* browser panel everything fuses into on completion */
  var panelMat = track(new THREE.ShaderMaterial({
    uniforms: {
      uA: { value: col(theme.success) }, uB: { value: col(theme.accent) }, uSize: { value: new THREE.Vector2(3.0, 1.9) },
      uTime: T0, uAppear: { value: 0 }, uStyle: { value: 1 }, uShimmer: { value: 0.7 }, uSeed: { value: 4.2 }, uDim: { value: 0 },
      uScan: { value: 0 }, uHero: { value: 1 }
    },
    vertexShader: SLAB_VERT, fragmentShader: SLAB_FRAG, transparent: true, depthWrite: false, side: THREE.DoubleSide
  }));
  var panel = new THREE.Mesh(slabGeo, panelMat); panel.scale.set(3.0, 1.9, 1); panel.visible = false; scene.add(panel);

  /* stage ring + nodes */
  var ringGroup = new THREE.Group(); ringGroup.rotation.x = -1.05; scene.add(ringGroup);
  var RR = 3.1;
  var ringMat = track(new THREE.ShaderMaterial({
    uniforms: { uA: { value: curColor.clone() }, uProg: { value: 0 }, uDim: { value: 0 } },
    vertexShader: RING_VERT, fragmentShader: RING_FRAG, transparent: true, depthWrite: false, side: THREE.DoubleSide
  }));
  ringGroup.add(new THREE.Mesh(track(new THREE.RingGeometry(RR - 0.014, RR + 0.014, 192)), ringMat));
  var nodes = [];
  var nodeGeo = track(new THREE.IcosahedronGeometry(0.17, 0));
  for (var k = 0; k < STAGES.length; k++) {
    var a = (k / STAGES.length) * Math.PI * 2;
    var nm = track(new THREE.MeshBasicMaterial({ color: col(theme.textSecondary), transparent: true, opacity: 0.45 }));
    var node = new THREE.Mesh(nodeGeo, nm);
    node.position.set(Math.sin(a) * RR, Math.cos(a) * RR, 0); ringGroup.add(node);
    var ng = track(new THREE.SpriteMaterial({ map: haloTex, color: col(theme.textSecondary), transparent: true, depthWrite: false, blending: THREE.AdditiveBlending, opacity: 0 }));
    var nglow = new THREE.Sprite(ng); nglow.scale.set(0.9, 0.9, 1); node.add(nglow);
    var lt = track(makeLabelTexture(STAGE_LABELS[k], theme));
    var lm = track(new THREE.SpriteMaterial({ map: lt, transparent: true, depthWrite: false, opacity: 0.9 }));
    var label = new THREE.Sprite(lm); label.scale.set(1.0, 0.25, 1);
    label.position.set(Math.sin(a) * (RR + 0.58), Math.cos(a) * (RR + 0.58), 0.1); ringGroup.add(label);
    nodes.push({ mesh: node, mat: nm, glow: ng, pulse: 0 });
  }

  /* floating glass cards for the live feed */
  var cardGroup = new THREE.Group(); scene.add(cardGroup);
  var cards = [];

  /* post: bloom -> vignette + chromatic aberration -> output */
  var composer = null, bloom = null, postPass = null;
  if (!cfg.lowRam) {
    try {
      var rt = new THREE.WebGLRenderTarget(4, 4, { type: THREE.HalfFloatType, samples: 0 });
      composer = new EffectComposer(renderer, rt);
      composer.addPass(new RenderPass(scene, camera));
      bloom = new UnrealBloomPass(new THREE.Vector2(256, 256), 0.4, 0.42, 0.55);
      composer.addPass(bloom);
      postPass = new ShaderPass({
        uniforms: { tDiffuse: { value: null }, uAber: { value: 0.0 }, uVig: { value: 0.55 } },
        vertexShader: 'varying vec2 vUv; void main(){ vUv = uv; gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0); }',
        fragmentShader: POST_FRAG
      });
      composer.addPass(postPass);
      composer.addPass(new OutputPass());
    } catch (e) { composer = null; bloom = null; postPass = null; }
  }

  S = {
    cfg: cfg, theme: theme, renderer: renderer, scene: scene, camera: camera, canvas: canvas, pr: pr,
    composer: composer, bloom: bloom, postPass: postPass,
    stageColors: stageColors, curColor: curColor, curColor2: curColor2, targetColor: curColor.clone(),
    backMat: backMat, floorMat: floorMat, rip: rip, ripCursor: 0, coreMat: coreMat, coreGroup: coreGroup, shell: shell, shellMat: shellMat, gyros: gyros,
    halo: halo, haloMat: haloMat,
    points: points, pGeo: pGeo, pPos: pPos, pA: pA, pS: pS, pC: pC, pG: pG, pt: pt, pSpd: pSpd, pAng: pAng, pR0: pR0, pY0: pY0, pSpin: pSpin, N: N, pCursor: 0, pMat: pMat, dMat: dMat, activeCap: N,
    dust: dust, beams: beams, beamCursor: 0, bPos: bPos, bCol: bCol, bGeo: bGeo, shocks: shocks,
    slabGroup: slabGroup, slabs: slabs, addSlab: addSlab, panel: panel, panelMat: panelMat,
    ringGroup: ringGroup, ringMat: ringMat, nodes: nodes, cardGroup: cardGroup, cards: cards,
    disposables: disposables, track: track, T0: T0,
    time: 0, last: performance.now(), raf: 0, stageIndex: 0, stage: 'PARSING_PROMPT', progress: 0, shownProgress: 0,
    power: 0.2, tps: 0, flow: 0, phase: 'building', phaseT: 0, reduced: !!cfg.reducedMotion, dirty: true,
    dragYaw: 0, dragPitch: 0, tiltYaw: 0, tiltPitch: 0, stackScale: 1, kick: 0, flash: 0, intro: 0,
    frames: 0, acc: 0, degraded: 0, paused: false, finished: false, baseZ: 8, logged: false, w: 360, h: 640
  };
  for (var q = 0; q < N; q++) { pA[q] = 0; }
  S.intro = S.reduced ? 1 : 0;

  resize();
  window.addEventListener('resize', resize);
  document.addEventListener('visibilitychange', onVisibility);
  canvas.addEventListener('touchstart', onTouchStart, { passive: true });
  canvas.addEventListener('touchmove', onTouchMove, { passive: true });
  canvas.addEventListener('touchend', onTouchEnd, { passive: true });
  window.addEventListener('deviceorientation', onTilt);
  S.raf = requestAnimationFrame(loop);
  console.log('forge: init ok ' + window.innerWidth + 'x' + window.innerHeight + ' pr=' + pr + ' bloom=' + !!composer);
  bridge('onReady');
}

/* ---------- input ---------- */
var touch = null;
function onTouchStart(e) { if (e.touches.length) touch = { x: e.touches[0].clientX, y: e.touches[0].clientY }; }
function onTouchMove(e) {
  if (!S || !touch || !e.touches.length || S.reduced) return;
  var t = e.touches[0];
  S.dragYaw = clamp(S.dragYaw + (t.clientX - touch.x) * 0.004, -0.9, 0.9);
  S.dragPitch = clamp(S.dragPitch + (t.clientY - touch.y) * 0.003, -0.4, 0.4);
  touch = { x: t.clientX, y: t.clientY };
}
function onTouchEnd() { touch = null; }
function onTilt(e) {
  if (!S || S.reduced || e.gamma == null) return;
  S.tiltYaw = clamp(e.gamma / 90, -1, 1) * 0.35;
  S.tiltPitch = clamp((e.beta - 50) / 90, -1, 1) * 0.2;
}
function onVisibility() { if (S) { S.last = performance.now(); S.paused = document.hidden; if (!document.hidden) S.dirty = true; } }

function resize() {
  if (!S) return;
  var w = window.innerWidth || 360, h = window.innerHeight || 640;
  S.w = w; S.h = h;
  S.renderer.setSize(w, h, false);
  if (S.composer) {
    S.composer.setPixelRatio(S.pr);
    S.composer.setSize(w, h);
    if (S.bloom) S.bloom.resolution.set(Math.floor(w * 0.5), Math.floor(h * 0.5));
  }
  S.camera.aspect = w / h;
  var halfFov = Math.tan(THREE.MathUtils.degToRad(48 / 2));
  S.baseZ = Math.max(8, 4.6 / (halfFov * Math.min(S.camera.aspect, 1.6)));
  S.camera.updateProjectionMatrix();
  S.dirty = true;
}

/* ---------- effects helpers ---------- */
function ripple(strength) {
  if (!S) return;
  S.rip[S.ripCursor].set(S.time, strength || 0.8);
  S.ripCursor = (S.ripCursor + 1) % RIPPLES;
}
function shock(color, size, life) {
  if (!S || S.reduced) return;
  for (var i = 0; i < S.shocks.length; i++) {
    var s = S.shocks[i];
    if (s.t >= s.life) {
      s.t = 0; s.life = life || 1; s.size = size || 6; s.mat.color.copy(color || S.curColor); s.spr.visible = true;
      return;
    }
  }
}
function burst(n, color) {
  if (!S || S.reduced) return;
  var c = color || S.curColor;
  for (var i = 0; i < n; i++) {
    var idx = S.pCursor; S.pCursor = (S.pCursor + 1) % S.activeCap;
    var ang = Math.random() * Math.PI * 2;
    S.pt[idx] = 0; S.pSpd[idx] = 0.26 + Math.random() * 0.34; S.pAng[idx] = ang;
    S.pR0[idx] = 4.2 + Math.random() * 2.6; S.pY0[idx] = (Math.random() - 0.5) * 5.5; S.pSpin[idx] = 1.1 + Math.random() * 2.2;
    S.pS[idx] = 1.1 + Math.random() * 1.5; S.pG[idx] = Math.floor(Math.random() * 16);
    S.pC[idx * 3] = c.r; S.pC[idx * 3 + 1] = c.g; S.pC[idx * 3 + 2] = c.b;
  }
}
function fireBeams(n) {
  if (!S || S.reduced) return;
  for (var i = 0; i < n; i++) {
    var b = S.beams[S.beamCursor]; S.beamCursor = (S.beamCursor + 1) % S.beams.length;
    var th = Math.random() * Math.PI * 2, ph = (Math.random() - 0.3) * 1.6, r = 5.5 + Math.random() * 2.5;
    b.sx = Math.cos(th) * Math.cos(ph) * r; b.sy = Math.sin(ph) * r * 0.7; b.sz = Math.sin(th) * Math.cos(ph) * r;
    b.life = 0.45 + Math.random() * 0.25; b.max = b.life;
  }
}

/* ---------- public events ---------- */
function setStage(name) {
  if (!S) return;
  var idx = Math.max(0, STAGES.indexOf(name));
  if (name === 'DONE') idx = STAGES.length;
  if (idx < S.stageIndex) return;
  if (idx > S.stageIndex) {
    for (var k = S.stageIndex; k < Math.min(idx, S.nodes.length); k++) S.nodes[k].pulse = 1;
    burst(110, S.stageColors[name] || S.curColor);
    S.power = Math.min(2, S.power + 0.6);
    S.kick = 1; ripple(1.3); shock(S.stageColors[name] || S.curColor, 8, 1.3);
  }
  S.stageIndex = idx; S.stage = name;
  if (S.stageColors[name]) S.targetColor.copy(S.stageColors[name]);
  S.dirty = true;
}
function setProgress(p) { if (S) { S.progress = clamp(p, 0, 1); S.dirty = true; } }
function onChunk(len, total, tps) {
  if (!S || S.phase !== 'building') return;
  S.tps = tps || 0;
  S.flow = Math.min(1, S.flow + 0.5);
  burst(clamp(Math.round((len || 0) / 14), 3, 46), S.curColor);
  fireBeams(clamp(Math.round((len || 0) / 120) + 1, 1, 4));
  if (Math.random() < 0.12) ripple(0.45);
}
function addSection(name) { if (S && S.phase === 'building') S.addSlab(name); }
function pushLog(text) {
  if (!S || !text) return;
  if (S.cards.length >= MAX_CARDS) { S.cards[0].fadeFast = true; }
  var hex = '#' + S.curColor.getHexString();
  var tex = S.track(makeCardTexture(String(text), S.theme, hex));
  var mat = S.track(new THREE.SpriteMaterial({ map: tex, transparent: true, depthWrite: false, opacity: 0 }));
  var spr = new THREE.Sprite(mat); spr.scale.set(2.3, 0.43, 1);
  var side = S.cards.length % 2 === 0 ? -1 : 1;
  spr.position.set(side * (0.9 + Math.random() * 0.5), -2.0, 1.6); S.cardGroup.add(spr);
  S.cards.push({ spr: spr, mat: mat, tex: tex, age: 0, fadeFast: false, side: side, sway: Math.random() * 6 });
  S.dirty = true;
}
function complete() {
  if (!S || S.phase === 'reveal') return;
  S.phase = 'reveal'; S.phaseT = 0; S.power = 2.6; S.targetColor.copy(S.stageColors.DONE);
  S.panel.visible = true; S.flash = 1; S.kick = 1.6;
  ripple(1.6); shock(S.stageColors.DONE, 10, 1.4); burst(160, S.stageColors.DONE); fireBeams(10);
  S.dirty = true;
}
function fail() {
  if (!S) return;
  S.phase = 'failed'; S.phaseT = 0; document.documentElement.classList.add('failed'); S.dirty = true;
}
function setReducedMotion(v) { if (S) { S.reduced = !!v; if (S.reduced) S.intro = 1; S.dirty = true; } }

/* ---------- frame loop ---------- */
function updateParticles(dt, speedMul) {
  var pos = S.pPos, a = S.pA, t = S.pt;
  for (var i = 0; i < S.N; i++) {
    if (t[i] >= 1) { a[i] = 0; continue; }
    t[i] += dt * S.pSpd[i] * speedMul;
    var e = ease(Math.min(t[i], 1));
    S.pAng[i] += dt * S.pSpin[i] * (1.0 + e * 3.0);
    var r = lerp(S.pR0[i], 0.55, e), y = lerp(S.pY0[i], 0, e);
    pos[i * 3] = Math.cos(S.pAng[i]) * r; pos[i * 3 + 1] = y; pos[i * 3 + 2] = Math.sin(S.pAng[i]) * r;
    a[i] = Math.pow(Math.sin(Math.PI * Math.min(t[i], 1)), 0.7);
    if (t[i] >= 1) { a[i] = 0; S.power = Math.min(2.4, S.power + 0.011); }
  }
  S.pGeo.attributes.position.needsUpdate = true; S.pGeo.attributes.aA.needsUpdate = true;
  S.pGeo.attributes.aS.needsUpdate = true; S.pGeo.attributes.aC.needsUpdate = true; S.pGeo.attributes.aG.needsUpdate = true;
}

function updateBeams(dt) {
  var p = S.bPos, c = S.bCol, cc = S.curColor;
  for (var i = 0; i < S.beams.length; i++) {
    var b = S.beams[i], o = i * 6;
    if (b.life <= 0) { p[o] = p[o + 1] = p[o + 2] = p[o + 3] = p[o + 4] = p[o + 5] = 0; c[o] = c[o + 1] = c[o + 2] = c[o + 3] = c[o + 4] = c[o + 5] = 0; continue; }
    b.life -= dt;
    var u = clamp(1 - b.life / b.max, 0, 1);              // 0 = just fired, 1 = absorbed
    var head = easeOut(u), tail = clamp(u * 1.6 - 0.45, 0, 1);
    p[o] = lerp(b.sx, 0, tail); p[o + 1] = lerp(b.sy, 0, tail); p[o + 2] = lerp(b.sz, 0, tail);
    p[o + 3] = lerp(b.sx, 0, head); p[o + 4] = lerp(b.sy, 0, head); p[o + 5] = lerp(b.sz, 0, head);
    var br = Math.sin(Math.PI * u) * 1.4;
    c[o] = cc.r * 0.2 * br; c[o + 1] = cc.g * 0.2 * br; c[o + 2] = cc.b * 0.2 * br;
    c[o + 3] = cc.r * br; c[o + 4] = cc.g * br; c[o + 5] = cc.b * br;
  }
  S.bGeo.attributes.position.needsUpdate = true; S.bGeo.attributes.color.needsUpdate = true;
}

function loop(now) {
  if (!S) return;
  S.raf = requestAnimationFrame(loop);
  if (S.paused) return;
  var dt = Math.min((now - S.last) / 1000, 0.05); S.last = now;

  // Adaptive quality: sustained frames over 20 ms -> fewer particles, then no post, lower resolution.
  S.acc += dt; S.frames++;
  if (S.acc >= 1) {
    var avg = S.acc / S.frames;
    if (avg > 0.021 && S.degraded < 2) {
      S.degraded++; S.activeCap = Math.max(300, Math.floor(S.activeCap * 0.55));
      if (S.degraded === 1 && S.bloom) { S.bloom.strength *= 0.8; }
      if (S.degraded === 2) {
        S.pr = 1; S.renderer.setPixelRatio(1); S.pMat.uniforms.uScale.value = 64; S.dMat.uniforms.uScale.value = 14;
        if (S.composer) { S.composer.dispose(); S.composer = null; }
        resize();
      }
    }
    S.acc = 0; S.frames = 0;
  }

  var animating = !S.reduced;
  if (!animating && !S.dirty && S.phase === 'building') return; // reduced motion: draw only on change
  if (animating) S.time += dt;
  S.T0.value = S.time;
  S.phaseT += dt;

  // intro fly-in
  if (S.intro < 1) S.intro = Math.min(1, S.intro + dt / 2.0);
  var intro = easeOut(S.intro);

  // colour follow
  S.curColor.lerp(S.targetColor, Math.min(1, dt * 3));

  // power / flow / kick / flash decay
  S.flow = Math.max(0, S.flow - dt * 1.4);
  S.kick = Math.max(0, S.kick - dt * 1.7);
  S.flash = Math.max(0, S.flash - dt * 1.5);
  var targetPower = 0.25 + clamp(S.tps / 70, 0, 1) * 0.95 + S.flow * 0.5;
  if (S.phase === 'building') S.power = lerp(S.power, targetPower, Math.min(1, dt * 2.2));
  else if (S.phase === 'failed') S.power = lerp(S.power, 0.05, Math.min(1, dt * 2));
  S.shownProgress = lerp(S.shownProgress, S.progress, Math.min(1, dt * 4));

  var T = S.time;
  var speedMul = S.phase === 'reveal' ? 3.2 : 1;
  if (animating) { updateParticles(dt, speedMul); updateBeams(dt); }

  // backdrop + floor
  S.backMat.uniforms.uTint.value.copy(S.curColor); S.backMat.uniforms.uPower.value = S.power;
  S.floorMat.uniforms.uA.value.copy(S.curColor); S.floorMat.uniforms.uPower.value = S.power;

  // reactor core
  S.coreMat.uniforms.uPower.value = S.power;
  S.coreMat.uniforms.uA.value.copy(S.curColor); S.coreMat.uniforms.uB.value.copy(S.curColor2);
  S.coreMat.uniforms.uDim.value = S.phase === 'failed' ? lerp(S.coreMat.uniforms.uDim.value, 1, Math.min(1, dt * 2)) : 0;
  var breathe = animating ? 1 + Math.sin(T * 2.2) * 0.04 * (0.5 + S.power) : 1;
  S.coreGroup.scale.setScalar(breathe * (1 + Math.max(0, S.power - 1) * 0.1) * (0.4 + 0.6 * intro));
  S.coreGroup.position.y = -0.1;
  S.shellMat.color.copy(S.curColor); S.shellMat.opacity = S.phase === 'failed' ? 0.1 : 0.28 + S.power * 0.1;
  if (animating) { S.shell.rotation.y += dt * (0.3 + S.power * 0.5); S.shell.rotation.x += dt * 0.2; }
  for (var g = 0; g < S.gyros.length; g++) {
    var gy = S.gyros[g];
    if (animating) {
      var spd = gy.speed * (0.5 + S.power * 1.1) * dt;
      gy.mesh.rotation.x += spd * (gy.axis === 0 ? 1 : 0.4); gy.mesh.rotation.y += spd * (gy.axis === 1 ? 1.2 : 0.5); gy.mesh.rotation.z += spd * (gy.axis === 2 ? 1.3 : 0.3);
    }
    gy.mat.opacity = (S.phase === 'failed' ? 0.15 : 0.55 + Math.min(S.power, 1.4) * 0.25);
  }
  S.haloMat.color.copy(S.curColor); S.haloMat.opacity = clamp(0.10 + Math.min(S.power, 1.2) * 0.09, 0, 0.26) * (S.phase === 'failed' ? 0.3 : 1);

  // shockwaves
  for (var s = 0; s < S.shocks.length; s++) {
    var sh = S.shocks[s];
    if (sh.t < sh.life) {
      sh.t += dt; var su = clamp(sh.t / sh.life, 0, 1);
      sh.spr.scale.setScalar(0.8 + easeOut(su) * sh.size); sh.mat.opacity = (1 - su) * 0.85;
      if (su >= 1) sh.spr.visible = false;
    }
  }

  // stack layout
  var n = S.slabs.length, spacing = 0.58, baseY = 1.25;
  var totalH = baseY + n * spacing + 0.3;
  S.stackScale = lerp(S.stackScale, Math.min(1, 4.2 / totalH), Math.min(1, dt * 3));
  S.slabGroup.scale.setScalar(S.stackScale);
  S.slabGroup.rotation.y = animating ? Math.sin(T * 0.35) * 0.38 : 0.12;
  var styled = S.stageIndex >= 2, motion = S.stageIndex >= 4;
  var reveal = S.phase === 'reveal', rt = reveal ? S.phaseT : 0;
  for (var i = 0; i < n; i++) {
    var sl = S.slabs[i], u = sl.mat.uniforms;
    var born = clamp((S.time - sl.born) / 0.8, 0, 1); if (S.reduced) born = 1;
    var targetY = baseY + i * spacing;
    var y = targetY, x = 0, z = 0, appear = ease(born);
    if (reveal) {
      var f = ease(clamp((rt - 0.25) / 0.6, 0, 1));
      y = lerp(targetY, 1.0, f); appear *= 1 - clamp((rt - 0.55) / 0.35, 0, 1) * 0.95;
    } else if (S.phase === 'failed') {
      var d = clamp(S.phaseT / 1.4, 0, 1);
      x = sl.drift.x * d; y = targetY + sl.drift.y * d; z = sl.drift.z * d; appear *= 1 - d * 0.5;
      sl.mesh.rotation.z = (sl.drift.x * 0.4) * d;
    }
    var hover = animating && !reveal && S.phase !== 'failed' ? Math.sin(T * 1.1 + i * 0.8) * 0.03 : 0;
    sl.mesh.position.set(x, y - (1 - born) * 0.7 + hover, z);
    sl.mesh.scale.set(sl.w * (0.9 + 0.1 * ease(born)), sl.h * (0.2 + 0.8 * ease(born)), 1);
    u.uAppear.value = appear; u.uScan.value = born < 1 ? born : 0;
    u.uStyle.value = lerp(u.uStyle.value, styled ? 1 : 0, Math.min(1, dt * 2.5)); if (S.reduced) u.uStyle.value = styled ? 1 : 0;
    u.uShimmer.value = lerp(u.uShimmer.value, motion ? 0.65 : 0, Math.min(1, dt * 2));
    u.uA.value.copy(S.curColor); u.uB.value.copy(S.curColor2);
    u.uDim.value = S.phase === 'failed' ? 1 : 0;
  }

  // reveal: slabs fuse into one browser panel that flies at the camera
  var fovPunch = 0, aber = 0;
  if (reveal) {
    var pa = clamp((rt - 0.55) / 0.35, 0, 1);
    S.panelMat.uniforms.uAppear.value = pa; S.panelMat.uniforms.uScan.value = rt > 0.5 && rt < 1.0 ? (rt - 0.5) * 2 : 0;
    var fly = ease(clamp((rt - 0.85) / 0.55, 0, 1));
    S.panel.position.set(0, lerp(1.0, 0, fly), lerp(0, S.baseZ - 3.2, fly));
    var sc = lerp(1, 4.6, fly); S.panel.scale.set(3.0 * sc, 1.9 * sc, 1);
    S.panel.rotation.y = S.slabGroup.rotation.y * (1 - fly);
    fovPunch = Math.sin(clamp((rt - 0.7) / 0.8, 0, 1) * Math.PI) * 14; aber = Math.sin(clamp((rt - 0.7) / 0.8, 0, 1) * Math.PI) * 0.012;
    if (rt > 1.45 && !S.finished) { S.finished = true; bridge('onRevealFinished'); }
  }

  // stage ring + nodes
  S.ringMat.uniforms.uA.value.copy(S.curColor);
  S.ringMat.uniforms.uProg.value = S.phase === 'reveal' ? 1 : S.shownProgress;
  S.ringMat.uniforms.uDim.value = S.phase === 'failed' ? 1 : 0;
  S.ringGroup.rotation.z = animating ? T * 0.05 : 0;
  for (var k = 0; k < S.nodes.length; k++) {
    var nd = S.nodes[k], done = k < S.stageIndex, active = k === S.stageIndex;
    nd.pulse = Math.max(0, nd.pulse - dt * 1.6);
    var tc = done ? S.stageColors[STAGES[k]] : (active ? S.curColor : null);
    if (tc) { nd.mat.color.lerp(tc, Math.min(1, dt * 6)); nd.glow.color.lerp(tc, Math.min(1, dt * 6)); } else { nd.mat.color.set(S.theme.textSecondary); nd.glow.color.set(S.theme.textSecondary); }
    nd.mat.opacity = done ? 0.95 : (active ? 0.75 + (animating ? Math.sin(T * 6) * 0.25 : 0) : 0.4);
    nd.glow.opacity = done ? 0.55 : (active ? 0.45 + (animating ? Math.sin(T * 6) * 0.2 : 0) : 0);
    var s2 = 1 + nd.pulse * 1.3 + (active ? 0.3 : 0);
    nd.mesh.scale.setScalar(s2);
    nd.mesh.rotation.y = animating ? (active ? T * 3 : T * 0.4) : 0; nd.mesh.rotation.x = animating ? T * 0.5 : 0.5;
  }

  // glass cards rise on a gentle S-curve and fade
  for (var c = S.cards.length - 1; c >= 0; c--) {
    var cd = S.cards[c]; cd.age += dt * (cd.fadeFast ? 3 : 1);
    var life = 5.2, p = cd.age / life;
    cd.spr.position.y = -2.0 + p * 5.0;
    cd.spr.position.x = cd.side * (1.0 + Math.sin(cd.sway + cd.age * 0.9) * 0.18);
    var fadeIn = clamp(cd.age / 0.5, 0, 1), fadeOut = clamp((1 - p) / 0.3, 0, 1);
    cd.mat.opacity = fadeIn * fadeOut * 0.95;
    cd.spr.scale.set(2.3 * (0.85 + 0.15 * easeOut(fadeIn)), 0.43 * (0.85 + 0.15 * easeOut(fadeIn)), 1);
    if (p >= 1) { S.cardGroup.remove(cd.spr); cd.mat.dispose(); cd.tex.dispose(); S.cards.splice(c, 1); }
  }

  // dust drift
  if (animating) {
    var dp = S.dust.geometry.attributes.position.array;
    for (var d2 = 0; d2 < dp.length; d2 += 3) { dp[d2 + 1] += dt * 0.12; if (dp[d2 + 1] > 6) dp[d2 + 1] = -6; }
    S.dust.geometry.attributes.position.needsUpdate = true;
  }

  // camera: fly-in, orbit, tilt parallax, stage kick, throughput breathing
  S.dragYaw *= 0.995; S.dragPitch *= 0.995;
  var kickE = easeOut(S.kick > 1 ? 1 : S.kick);
  var yaw = (animating ? Math.sin(T * 0.22) * 0.24 : 0) + S.dragYaw + S.tiltYaw + (1 - intro) * 1.3;
  var pitch = 0.26 + (animating ? Math.sin(T * 0.17) * 0.05 : 0) + S.dragPitch + S.tiltPitch + (1 - intro) * 0.25;
  var dist = S.baseZ * (1 + (1 - intro) * 0.9) - kickE * 0.55 + (animating ? Math.sin(T * 0.6) * 0.12 * S.power : 0);
  S.camera.position.set(Math.sin(yaw) * dist, Math.sin(pitch) * dist * 0.5 + 0.55, Math.cos(yaw) * dist);
  S.camera.lookAt(0, 0.35, 0);
  S.camera.rotation.z += animating ? Math.sin(T * 40) * 0.0035 * kickE : 0;
  var wantFov = 48 + (1 - intro) * 14 + kickE * 5 + fovPunch;
  if (Math.abs(S.camera.fov - wantFov) > 0.05) { S.camera.fov = wantFov; S.camera.updateProjectionMatrix(); }

  // post
  if (S.bloom) {
    S.bloom.strength = (S.phase === 'failed' ? 0.2 : 0.34 + Math.min(S.power, 1.4) * 0.14) + S.flash * 0.9 + kickE * 0.18;
    S.bloom.threshold = 0.55; S.bloom.radius = 0.42;
  }
  if (S.postPass) { S.postPass.uniforms.uAber.value = aber + kickE * 0.004; S.postPass.uniforms.uVig.value = S.phase === 'failed' ? 0.85 : 0.55; }

  if (S.composer) S.composer.render(); else S.renderer.render(S.scene, S.camera);
  S.dirty = false;
  if (!S.logged && S.time > 2) { S.logged = true; console.log('forge: rendering t=' + S.time.toFixed(1) + ' slabs=' + S.slabs.length + ' stage=' + S.stage + ' bloom=' + !!S.composer); }
}

/* ---------- cleanup ---------- */
function dispose() {
  if (!S) return;
  cancelAnimationFrame(S.raf);
  window.removeEventListener('resize', resize);
  document.removeEventListener('visibilitychange', onVisibility);
  window.removeEventListener('deviceorientation', onTilt);
  S.canvas.removeEventListener('touchstart', onTouchStart);
  S.canvas.removeEventListener('touchmove', onTouchMove);
  S.canvas.removeEventListener('touchend', onTouchEnd);
  S.cards.forEach(function (c) { c.mat.dispose(); c.tex.dispose(); });
  S.disposables.forEach(function (d) { if (d && d.dispose) d.dispose(); });
  if (S.composer) S.composer.dispose();
  S.renderer.dispose();
  try { S.renderer.forceContextLoss(); } catch (e) { /* already lost */ }
  S = null;
}

window.Forge = {
  init: init, setStage: setStage, setProgress: setProgress, onChunk: onChunk, addSection: addSection,
  pushLog: pushLog, complete: complete, fail: fail, setReducedMotion: setReducedMotion, dispose: dispose
};
