/**
 * NutriCheck - 3D Bio-Scene Canvas Engine
 * --------------------------------------------------------------------------
 * Renders a layered, scroll-driven background scene on a single <canvas>:
 *   1. Bokeh layer      - large soft light orbs drifting upward (far depth)
 *   2. Molecular network - depth-sorted nodes + proximity links with parallax
 *   3. Distant helix    - small, faint secondary DNA helix (mid depth)
 *   4. Hero DNA helix   - 3D particle double helix with perspective projection,
 *                         depth shading, glowing front nodes and a travelling
 *                         "data pulse" (near depth)
 *
 * Scroll progress through the scrollytelling track drives helix rotation,
 * drift and network parallax. Mouse movement adds a subtle 3D tilt.
 * Fully light/dark theme aware and respects prefers-reduced-motion.
 */
(function () {
  'use strict';

  const canvas = document.getElementById('bioCanvas');
  const track = document.querySelector('.scrolly-track');
  if (!canvas || !track || !canvas.getContext) return;

  const ctx = canvas.getContext('2d');
  const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  const TAU = Math.PI * 2;

  // --------------------------------------------------------------------------
  // Theme palettes (RGB tuples + per-layer intensity)
  // --------------------------------------------------------------------------
  const PALETTES = {
    light: {
      primary: [5, 150, 105],
      accent: [13, 148, 136],
      highlight: [16, 185, 129],
      helixFront: 0.55,
      helixBack: 0.14,
      rung: 0.22,
      backbone: 0.28,
      glow: 0.28,
      network: 0.20,
      bokeh: 0.10,
      dust: 0.38
    },
    dark: {
      primary: [52, 211, 153],
      accent: [45, 212, 191],
      highlight: [110, 231, 183],
      helixFront: 0.70,
      helixBack: 0.10,
      rung: 0.18,
      backbone: 0.24,
      glow: 0.45,
      network: 0.20,
      bokeh: 0.08,
      dust: 0.5
    }
  };

  const isDark = () => document.documentElement.getAttribute('data-theme') === 'dark';
  let pal = PALETTES[isDark() ? 'dark' : 'light'];
  new MutationObserver(() => {
    pal = PALETTES[isDark() ? 'dark' : 'light'];
    if (reduceMotion) draw(0);
  }).observe(document.documentElement, { attributes: true, attributeFilter: ['data-theme'] });

  const rgba = (c, a) => `rgba(${c[0]},${c[1]},${c[2]},${a < 0 ? 0 : a > 1 ? 1 : a})`;
  const clamp = (v, a, b) => Math.max(a, Math.min(b, v));
  const lerp = (a, b, t) => a + (b - a) * t;

  // --------------------------------------------------------------------------
  // Sizing
  // --------------------------------------------------------------------------
  let W = 0, H = 0, DPR = 1, isCompact = false;
  let netNodes = [];
  let bokeh = [];
  let dust = [];

  function resize() {
    DPR = Math.min(window.devicePixelRatio || 1, 2);
    const rect = canvas.getBoundingClientRect();
    W = rect.width;
    H = rect.height;
    isCompact = W < 900;
    canvas.width = Math.round(W * DPR);
    canvas.height = Math.round(H * DPR);
    ctx.setTransform(DPR, 0, 0, DPR, 0, 0);
    buildNetwork();
    buildBokeh();
    buildDust();
  }

  // Deterministic PRNG so the layout is stable across resizes
  function mulberry32(seed) {
    return function () {
      seed |= 0; seed = (seed + 0x6D2B79F5) | 0;
      let t = Math.imul(seed ^ (seed >>> 15), 1 | seed);
      t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
      return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
    };
  }

  function buildNetwork() {
    const rand = mulberry32(42);
    const count = Math.round(clamp((W * H) / 16000, 35, isCompact ? 45 : 95));
    netNodes = [];
    for (let i = 0; i < count; i++) {
      const depth = 0.35 + rand() * 0.65;
      netNodes.push({
        x: rand() * W,
        y: rand() * H,
        depth,
        vx: (rand() - 0.5) * 0.12,
        vy: (rand() - 0.5) * 0.12,
        hub: rand() < 0.1,
        phase: rand() * TAU,
        sx: 0,
        sy: 0
      });
    }
  }

  function buildDust() {
    const rand = mulberry32(1337);
    const count = Math.round(clamp((W * H) / 5500, 70, isCompact ? 110 : 260));
    dust = [];
    for (let i = 0; i < count; i++) {
      dust.push({
        x: rand() * W,
        y: rand() * H,
        depth: 0.2 + rand() * 0.8,
        size: 0.4 + rand() * 1.1,
        speed: 0.5 + rand() * 1.0,
        phase: rand() * TAU,
        tint: rand() < 0.65 ? 'primary' : (rand() < 0.5 ? 'accent' : 'highlight')
      });
    }
  }

  function buildBokeh() {
    const rand = mulberry32(7);
    const count = isCompact ? 7 : 14;
    bokeh = [];
    for (let i = 0; i < count; i++) {
      bokeh.push({
        x: rand() * W,
        y: rand() * H,
        r: 30 + rand() * 90,
        depth: 0.3 + rand() * 0.7,
        speed: 0.004 + rand() * 0.01,
        tint: rand() < 0.5 ? 'primary' : 'accent',
        phase: rand() * TAU
      });
    }
  }

  // --------------------------------------------------------------------------
  // Inputs: scroll progress & mouse
  // --------------------------------------------------------------------------
  function scrollProgress() {
    const rect = track.getBoundingClientRect();
    const total = track.offsetHeight - window.innerHeight;
    if (total <= 0) return 0;
    return clamp(-rect.top / total, 0, 1);
  }

  function trackVisible() {
    const rect = track.getBoundingClientRect();
    return rect.bottom > 0 && rect.top < window.innerHeight;
  }

  const mouse = { tx: 0, ty: 0, x: 0, y: 0 };
  window.addEventListener('mousemove', (e) => {
    mouse.tx = (e.clientX / window.innerWidth) * 2 - 1;
    mouse.ty = (e.clientY / window.innerHeight) * 2 - 1;
  }, { passive: true });

  // --------------------------------------------------------------------------
  // Layer 1: Bokeh
  // --------------------------------------------------------------------------
  function drawBokeh(time, p) {
    for (let i = 0; i < bokeh.length; i++) {
      const b = bokeh[i];
      const drift = (time * b.speed + p * H * 0.9 * b.depth);
      let y = (b.y - drift) % (H + b.r * 2);
      if (y < -b.r) y += H + b.r * 2;
      const x = b.x + Math.sin(time * 0.0003 + b.phase) * 24 * b.depth + mouse.x * 20 * b.depth;
      const twinkle = 0.75 + 0.25 * Math.sin(time * 0.0009 + b.phase);
      const a = pal.bokeh * b.depth * twinkle;
      const g = ctx.createRadialGradient(x, y, 0, x, y, b.r);
      const c = pal[b.tint];
      g.addColorStop(0, rgba(c, a));
      g.addColorStop(0.6, rgba(c, a * 0.4));
      g.addColorStop(1, rgba(c, 0));
      ctx.fillStyle = g;
      ctx.beginPath();
      ctx.arc(x, y, b.r, 0, TAU);
      ctx.fill();
    }
  }

  // --------------------------------------------------------------------------
  // Layer 2: Molecular network
  // --------------------------------------------------------------------------
  const LINK_DIST = 150;

  function drawNetwork(time, p, dt) {
    const span = H + 80;
    for (let i = 0; i < netNodes.length; i++) {
      const n = netNodes[i];
      if (!reduceMotion) {
        n.x += n.vx * dt * 0.06;
        n.y += n.vy * dt * 0.06;
        if (n.x < -40) n.x += W + 80;
        if (n.x > W + 40) n.x -= W + 80;
      }
      let sy = (n.y - p * H * 1.4 * n.depth) % span;
      if (sy < -40) sy += span;
      n.sx = n.x + mouse.x * 26 * n.depth;
      n.sy = sy - 40 + mouse.y * 14 * n.depth;
    }

    ctx.lineWidth = 1;
    const maxD2 = LINK_DIST * LINK_DIST;
    for (let i = 0; i < netNodes.length; i++) {
      const a = netNodes[i];
      for (let j = i + 1; j < netNodes.length; j++) {
        const b = netNodes[j];
        const dx = a.sx - b.sx, dy = a.sy - b.sy;
        const d2 = dx * dx + dy * dy;
        if (d2 < maxD2) {
          const alpha = pal.network * (1 - Math.sqrt(d2) / LINK_DIST) * Math.min(a.depth, b.depth);
          ctx.strokeStyle = rgba(pal.primary, alpha);
          ctx.beginPath();
          ctx.moveTo(a.sx, a.sy);
          ctx.lineTo(b.sx, b.sy);
          ctx.stroke();
        }
      }
    }

    for (let i = 0; i < netNodes.length; i++) {
      const n = netNodes[i];
      const pulse = 0.7 + 0.3 * Math.sin(time * 0.002 + n.phase);
      const r = (n.hub ? 2.6 : 1.2) + n.depth * 1.4;
      ctx.fillStyle = rgba(pal.primary, pal.network * 2.2 * n.depth * pulse);
      ctx.beginPath();
      ctx.arc(n.sx, n.sy, r, 0, TAU);
      ctx.fill();
      if (n.hub) {
        ctx.strokeStyle = rgba(pal.accent, pal.network * 1.4 * n.depth * pulse);
        ctx.beginPath();
        ctx.arc(n.sx, n.sy, r + 4 + pulse * 2, 0, TAU);
        ctx.stroke();
      }
    }
  }

  // --------------------------------------------------------------------------
  // Layer 2b: Free-flowing micro particles (smooth pseudo flow field)
  // near=false draws the far particles (behind helix) and advances motion;
  // near=true draws the closest particles in front of the helix for depth.
  // --------------------------------------------------------------------------
  const NEAR_DEPTH = 0.78;

  function drawDust(time, p, dt, near) {
    const spanY = H + 20;
    const spanX = W + 20;
    for (let i = 0; i < dust.length; i++) {
      const d = dust[i];
      const isNear = d.depth >= NEAR_DEPTH;

      if (!near && !reduceMotion) {
        // Flow-field direction varies smoothly with position and time
        const ang = (Math.sin(d.x * 0.0021 + time * 0.00011 + d.phase * 0.4) +
                     Math.cos(d.y * 0.0017 - time * 0.00009)) * Math.PI;
        const v = d.speed * dt * 0.018 * (0.4 + d.depth);
        d.x += Math.cos(ang) * v;
        d.y += Math.sin(ang) * v - v * 0.35; // gentle upward bias
        if (d.x < -10) d.x += spanX;
        else if (d.x > W + 10) d.x -= spanX;
        if (d.y < -10) d.y += spanY;
        else if (d.y > H + 10) d.y -= spanY;
      }

      if (isNear !== near) continue;

      let sy = (d.y - p * H * 1.8 * d.depth) % spanY;
      if (sy < -10) sy += spanY;
      const sx = d.x + mouse.x * 34 * d.depth;
      sy += mouse.y * 18 * d.depth;

      const twinkle = 0.55 + 0.45 * Math.sin(time * 0.0025 * d.speed + d.phase);
      const alpha = pal.dust * (0.35 + 0.65 * d.depth) * twinkle;
      const r = d.size * (0.6 + d.depth * 0.9);

      ctx.fillStyle = rgba(pal[d.tint], alpha);
      ctx.beginPath();
      ctx.arc(sx, sy, r, 0, TAU);
      ctx.fill();

      // A faint halo on the closest, brightest specks
      if (isNear && twinkle > 0.85) {
        ctx.fillStyle = rgba(pal.highlight, alpha * 0.25);
        ctx.beginPath();
        ctx.arc(sx, sy, r * 3.2, 0, TAU);
        ctx.fill();
      }
    }
  }

  // --------------------------------------------------------------------------
  // Layers 3 & 4: 3D particle DNA helix
  // --------------------------------------------------------------------------
  const FOCAL = 900;
  const pointsBuf = [];

  function drawHelix(opts, time) {
    const { cx, cy, length, radius, tilt, rotation, pairs, twist, alphaMul, sizeMul, pulseU } = opts;
    const cosT = Math.cos(tilt), sinT = Math.sin(tilt);
    pointsBuf.length = 0;

    // Build projected points for both strands
    for (let i = 0; i < pairs; i++) {
      const u = i / (pairs - 1);
      const along = (u - 0.5) * length;
      const ang = i * twist + rotation;
      const fade = Math.pow(Math.sin(u * Math.PI), 0.8);
      const pulse = pulseU == null ? 0 : Math.exp(-Math.pow((u - pulseU) * 9, 2));

      for (let s = 0; s < 2; s++) {
        const a = ang + s * Math.PI;
        const x = radius * Math.cos(a);
        const z = radius * Math.sin(a);
        const X = x * cosT - along * sinT;
        const Y = x * sinT + along * cosT;
        const sc = FOCAL / (FOCAL + z);
        pointsBuf.push({
          x: cx + X * sc,
          y: cy + Y * sc,
          z,
          sc,
          depth: (radius - z) / (2 * radius), // 1 = front, 0 = back
          fade,
          pulse,
          strand: s,
          i
        });
      }
    }

    // Rungs (base pairs) between the two strands
    for (let i = 0; i < pairs; i++) {
      const A = pointsBuf[i * 2], B = pointsBuf[i * 2 + 1];
      const d = (A.depth + B.depth) * 0.5;
      const alpha = pal.rung * A.fade * (0.45 + 0.55 * d) * alphaMul * (1 + A.pulse * 1.5);
      const g = ctx.createLinearGradient(A.x, A.y, B.x, B.y);
      g.addColorStop(0, rgba(pal.primary, alpha));
      g.addColorStop(0.5, rgba(pal.accent, alpha * 0.55));
      g.addColorStop(1, rgba(pal.primary, alpha));
      ctx.strokeStyle = g;
      ctx.lineWidth = 1.4 * sizeMul;
      ctx.beginPath();
      ctx.moveTo(A.x, A.y);
      ctx.lineTo(B.x, B.y);
      ctx.stroke();
    }

    // Backbones as smooth curves per strand
    for (let s = 0; s < 2; s++) {
      ctx.lineWidth = 2 * sizeMul;
      for (let i = 0; i < pairs - 1; i++) {
        const P = pointsBuf[i * 2 + s], Q = pointsBuf[(i + 1) * 2 + s];
        const d = (P.depth + Q.depth) * 0.5;
        ctx.strokeStyle = rgba(pal.primary, pal.backbone * P.fade * (0.3 + 0.7 * d) * alphaMul);
        ctx.beginPath();
        ctx.moveTo(P.x, P.y);
        ctx.lineTo(Q.x, Q.y);
        ctx.stroke();
      }
    }

    // Nucleotide nodes, painter-sorted back-to-front
    pointsBuf.sort((a, b) => b.z - a.z);
    for (let k = 0; k < pointsBuf.length; k++) {
      const pt = pointsBuf[k];
      const r = (1.8 + 3.4 * pt.depth) * pt.sc * sizeMul * (1 + pt.pulse * 0.6);
      const alpha = lerp(pal.helixBack, pal.helixFront, pt.depth) * pt.fade * alphaMul;
      const col = pt.strand === 0 ? pal.primary : pal.accent;

      // Soft glow for front-facing / pulsing nodes
      if (pt.depth > 0.6 || pt.pulse > 0.2) {
        const glowA = pal.glow * alpha * (pt.depth + pt.pulse * 1.5);
        const gr = r * 4.5;
        const g = ctx.createRadialGradient(pt.x, pt.y, 0, pt.x, pt.y, gr);
        g.addColorStop(0, rgba(pt.pulse > 0.2 ? pal.highlight : col, glowA));
        g.addColorStop(1, rgba(col, 0));
        ctx.fillStyle = g;
        ctx.beginPath();
        ctx.arc(pt.x, pt.y, gr, 0, TAU);
        ctx.fill();
      }

      // Sphere body with a subtle specular offset for a 3D feel
      const body = ctx.createRadialGradient(pt.x - r * 0.35, pt.y - r * 0.35, r * 0.1, pt.x, pt.y, r);
      body.addColorStop(0, rgba(pal.highlight, Math.min(1, alpha * 1.25)));
      body.addColorStop(1, rgba(col, alpha));
      ctx.fillStyle = body;
      ctx.beginPath();
      ctx.arc(pt.x, pt.y, r, 0, TAU);
      ctx.fill();
    }
  }

  // --------------------------------------------------------------------------
  // Frame composition
  // --------------------------------------------------------------------------
  let progress = 0;
  let lastTime = 0;

  function draw(time, dt = 16) {
    ctx.clearRect(0, 0, W, H);
    const p = progress;
    const t = reduceMotion ? 0 : time;

    drawBokeh(t, p);
    drawNetwork(t, p, dt);
    drawDust(t, p, dt, false);

    const minSide = Math.min(W, H);
    const diag = Math.hypot(W, H);

    // Distant secondary helix (right edge, faint, counter-rotating)
    if (!isCompact) {
      drawHelix({
        cx: W * 0.94 + mouse.x * 8,
        cy: H * 0.5 + (0.5 - p) * H * 0.35,
        length: H * 1.3,
        radius: clamp(minSide * 0.05, 22, 46),
        tilt: -0.18 + mouse.x * 0.02,
        rotation: -t * 0.0004 - p * Math.PI * 4,
        pairs: 30,
        twist: 0.42,
        alphaMul: 0.42,
        sizeMul: 0.7,
        pulseU: null
      }, t);
    }

    // Hero helix: diagonal sweep across the stage
    drawHelix({
      cx: W * (isCompact ? 0.5 : 0.52) + (p - 0.5) * W * 0.08 + mouse.x * 18,
      cy: H * 0.5 + mouse.y * 12,
      length: diag * 1.0,
      radius: clamp(minSide * 0.12, 48, 125),
      tilt: 0.55 + (p - 0.5) * 0.18 + mouse.x * 0.04,
      rotation: t * 0.00028 + p * Math.PI * 6,
      pairs: isCompact ? 34 : 46,
      twist: 0.36,
      alphaMul: isCompact ? 0.6 : 1,
      sizeMul: 1,
      pulseU: reduceMotion ? null : ((t * 0.00006 + p * 1.5) % 1.3) - 0.15
    }, t);

    // Nearest micro particles float in front of the helix
    drawDust(t, p, dt, true);
  }

  function frame(time) {
    const dt = Math.min(64, time - (lastTime || time));
    lastTime = time;

    progress = lerp(progress, scrollProgress(), 0.1);
    mouse.x = lerp(mouse.x, mouse.tx, 0.05);
    mouse.y = lerp(mouse.y, mouse.ty, 0.05);

    if (trackVisible()) draw(time, dt);
    requestAnimationFrame(frame);
  }

  // --------------------------------------------------------------------------
  // Boot
  // --------------------------------------------------------------------------
  let resizeTimer = null;
  window.addEventListener('resize', () => {
    clearTimeout(resizeTimer);
    resizeTimer = setTimeout(() => { resize(); if (reduceMotion) draw(0); }, 120);
  }, { passive: true });

  resize();
  progress = scrollProgress();

  if (reduceMotion) {
    draw(0);
    window.addEventListener('scroll', () => {
      progress = scrollProgress();
      draw(0);
    }, { passive: true });
  } else {
    requestAnimationFrame(frame);
  }

  requestAnimationFrame(() => canvas.classList.add('is-ready'));
})();
