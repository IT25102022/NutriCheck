/**
 * NutriCheck - Modernized Cinematic Scrollytelling Visual Engine
 * Uses requestAnimationFrame with lerp interpolation for 60fps smooth scrolling.
 */

(function () {
  'use strict';

  // Check for reduced motion preference
  const prefersReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  if (prefersReducedMotion) {
    return;
  }

  // DOM Elements
  const track = document.querySelector('.scrolly-track');
  const progressBar = document.querySelector('.scrolly-progress-bar');
  const morphConsole = document.querySelector('.morph-console');
  const glow1 = document.querySelector('.ambient-glow-1');
  const glow2 = document.querySelector('.ambient-glow-2');

  // Narrative Chapters
  const narrativeChapters = Array.from(document.querySelectorAll('.narrative-chapter'));
  // Visual Layers
  const layerUnderstand = document.querySelector('.layer-understand');
  const layerTrack = document.querySelector('.layer-track');
  const layerNourish = document.querySelector('.layer-nourish');
  const layerConnect = document.querySelector('.layer-connect');
  const layerInform = document.querySelector('.layer-inform');
  const layerStart = document.querySelector('.layer-start');

  // Scrubber Dots
  const scrubberDots = Array.from(document.querySelectorAll('.scrubber-dot'));

  if (!track || !morphConsole) return;

  // State
  let targetProgress = 0;
  let currentProgress = 0;
  let rafId = null;

  function clamp(val, min, max) {
    return Math.max(min, Math.min(max, val));
  }

  function lerp(start, end, factor) {
    return start + (end - start) * factor;
  }

  function mapRange(val, inMin, inMax, outMin, outMax) {
    if (inMax === inMin) return outMin;
    const normalized = clamp((val - inMin) / (inMax - inMin), 0, 1);
    return outMin + normalized * (outMax - outMin);
  }

  function calculateProgress() {
    const trackRect = track.getBoundingClientRect();
    const trackTop = trackRect.top + window.scrollY;
    const trackHeight = track.offsetHeight;
    const viewportHeight = window.innerHeight;

    const totalScrollable = trackHeight - viewportHeight;
    if (totalScrollable <= 0) return 0;

    const relativeScroll = window.scrollY - trackTop;
    return clamp(relativeScroll / totalScrollable, 0, 1);
  }

  function renderLayers(p) {
    // 1. Subtle console 3D tilt
    const tiltX = mapRange(p, 0, 1, 3, -3);
    const tiltY = Math.sin(p * Math.PI * 2) * 2;
    morphConsole.style.transform = `perspective(1200px) rotateX(${tiltX.toFixed(2)}deg) rotateY(${tiltY.toFixed(2)}deg)`;

    // Ambient glow motion
    if (glow1) {
      glow1.style.transform = `translate3d(${Math.cos(p * Math.PI * 2) * 40}px, ${Math.sin(p * Math.PI * 2) * 30}px, 0)`;
    }
    if (glow2) {
      glow2.style.transform = `translate3d(${Math.sin(p * Math.PI * 2) * -40}px, ${Math.cos(p * Math.PI * 2) * -30}px, 0)`;
    }

    // Top progress bar
    if (progressBar) {
      progressBar.style.width = `${(p * 100).toFixed(1)}%`;
    }

    // ------------------------------------------------------------------------
    // Chapter Thresholds
    // 0: [0.00 -> 0.18] UNDERSTAND (Health Vitals Profile)
    // 1: [0.18 -> 0.40] TRACK (BMI & Category Evaluation)
    // 2: [0.40 -> 0.62] NOURISH (Rule-Based Diet Plans)
    // 3: [0.62 -> 0.80] CONNECT (Nutritionist Appointments)
    // 4: [0.80 -> 0.92] STAY INFORMED (Timely Alerts & Updates)
    // 5: [0.92 -> 1.00] START (Call to Action)
    // ------------------------------------------------------------------------

    // Layer 1: UNDERSTAND [0.00 -> 0.20]
    if (layerUnderstand) {
      if (p <= 0.22) {
        const opacity = mapRange(p, 0.14, 0.22, 1, 0);
        const transY = mapRange(p, 0.0, 0.22, 0, -20);
        const scale = mapRange(p, 0.0, 0.22, 1, 1.04);
        layerUnderstand.style.opacity = opacity.toFixed(3);
        layerUnderstand.style.transform = `translate3d(0, ${transY.toFixed(1)}px, 0) scale(${scale.toFixed(3)})`;
        layerUnderstand.style.visibility = opacity > 0.01 ? 'visible' : 'hidden';
      } else {
        layerUnderstand.style.opacity = '0';
        layerUnderstand.style.visibility = 'hidden';
      }
    }

    // Layer 2: TRACK [0.18 -> 0.42]
    if (layerTrack) {
      if (p >= 0.16 && p <= 0.44) {
        let opacity = 0;
        if (p < 0.24) opacity = mapRange(p, 0.16, 0.24, 0, 1);
        else if (p > 0.36) opacity = mapRange(p, 0.36, 0.44, 1, 0);
        else opacity = 1;

        const transY = mapRange(p, 0.16, 0.26, 25, 0) + mapRange(p, 0.36, 0.44, 0, -20);
        const scale = mapRange(p, 0.16, 0.26, 0.95, 1);
        layerTrack.style.opacity = opacity.toFixed(3);
        layerTrack.style.transform = `translate3d(0, ${transY.toFixed(1)}px, 0) scale(${scale.toFixed(3)})`;
        layerTrack.style.visibility = opacity > 0.01 ? 'visible' : 'hidden';
      } else {
        layerTrack.style.opacity = '0';
        layerTrack.style.visibility = 'hidden';
      }
    }

    // Layer 3: NOURISH [0.40 -> 0.64]
    if (layerNourish) {
      if (p >= 0.38 && p <= 0.65) {
        let opacity = 0;
        if (p < 0.46) opacity = mapRange(p, 0.38, 0.46, 0, 1);
        else if (p > 0.58) opacity = mapRange(p, 0.58, 0.65, 1, 0);
        else opacity = 1;

        const transY = mapRange(p, 0.38, 0.48, 25, 0) + mapRange(p, 0.58, 0.65, 0, -20);
        const scale = mapRange(p, 0.38, 0.48, 0.95, 1);
        layerNourish.style.opacity = opacity.toFixed(3);
        layerNourish.style.transform = `translate3d(0, ${transY.toFixed(1)}px, 0) scale(${scale.toFixed(3)})`;
        layerNourish.style.visibility = opacity > 0.01 ? 'visible' : 'hidden';
      } else {
        layerNourish.style.opacity = '0';
        layerNourish.style.visibility = 'hidden';
      }
    }

    // Layer 4: CONNECT [0.60 -> 0.82]
    if (layerConnect) {
      if (p >= 0.60 && p <= 0.83) {
        let opacity = 0;
        if (p < 0.68) opacity = mapRange(p, 0.60, 0.68, 0, 1);
        else if (p > 0.76) opacity = mapRange(p, 0.76, 0.83, 1, 0);
        else opacity = 1;

        const transY = mapRange(p, 0.60, 0.70, 25, 0) + mapRange(p, 0.76, 0.83, 0, -20);
        const scale = mapRange(p, 0.60, 0.70, 0.95, 1);
        layerConnect.style.opacity = opacity.toFixed(3);
        layerConnect.style.transform = `translate3d(0, ${transY.toFixed(1)}px, 0) scale(${scale.toFixed(3)})`;
        layerConnect.style.visibility = opacity > 0.01 ? 'visible' : 'hidden';
      } else {
        layerConnect.style.opacity = '0';
        layerConnect.style.visibility = 'hidden';
      }
    }

    // Layer 5: STAY INFORMED [0.78 -> 0.94]
    if (layerInform) {
      if (p >= 0.78 && p <= 0.94) {
        let opacity = 0;
        if (p < 0.84) opacity = mapRange(p, 0.78, 0.84, 0, 1);
        else if (p > 0.90) opacity = mapRange(p, 0.90, 0.94, 1, 0);
        else opacity = 1;

        const transY = mapRange(p, 0.78, 0.86, 25, 0) + mapRange(p, 0.90, 0.94, 0, -15);
        const scale = mapRange(p, 0.78, 0.86, 0.95, 1);
        layerInform.style.opacity = opacity.toFixed(3);
        layerInform.style.transform = `translate3d(0, ${transY.toFixed(1)}px, 0) scale(${scale.toFixed(3)})`;
        layerInform.style.visibility = opacity > 0.01 ? 'visible' : 'hidden';
      } else {
        layerInform.style.opacity = '0';
        layerInform.style.visibility = 'hidden';
      }
    }

    // Layer 6: START [0.90 -> 1.00]
    if (layerStart) {
      if (p >= 0.88) {
        const opacity = mapRange(p, 0.88, 0.96, 0, 1);
        const transY = mapRange(p, 0.88, 0.96, 25, 0);
        const scale = mapRange(p, 0.88, 0.96, 0.95, 1);
        layerStart.style.opacity = opacity.toFixed(3);
        layerStart.style.transform = `translate3d(0, ${transY.toFixed(1)}px, 0) scale(${scale.toFixed(3)})`;
        layerStart.style.visibility = opacity > 0.01 ? 'visible' : 'hidden';
      } else {
        layerStart.style.opacity = '0';
        layerStart.style.visibility = 'hidden';
      }
    }

    // Narrative chapter active states
    let activeChapter = 0;
    if (p < 0.18) activeChapter = 0;
    else if (p < 0.40) activeChapter = 1;
    else if (p < 0.62) activeChapter = 2;
    else if (p < 0.80) activeChapter = 3;
    else if (p < 0.92) activeChapter = 4;
    else activeChapter = 5;

    narrativeChapters.forEach((ch, idx) => {
      if (idx === activeChapter) ch.classList.add('active');
      else ch.classList.remove('active');
    });

    scrubberDots.forEach((dot, idx) => {
      if (idx === activeChapter) dot.classList.add('active');
      else dot.classList.remove('active');
    });
  }

  // Animation Loop with Lerp
  function tick() {
    targetProgress = calculateProgress();
    currentProgress = lerp(currentProgress, targetProgress, 0.15);

    if (Math.abs(targetProgress - currentProgress) > 0.0001) {
      renderLayers(currentProgress);
    } else {
      renderLayers(targetProgress);
    }

    rafId = requestAnimationFrame(tick);
  }

  window.addEventListener('scroll', () => {
    if (!rafId) rafId = requestAnimationFrame(tick);
  }, { passive: true });

  window.addEventListener('resize', () => {
    targetProgress = calculateProgress();
    renderLayers(targetProgress);
  }, { passive: true });

  // Scrubber clicks
  const chapterTargets = [0.04, 0.28, 0.50, 0.70, 0.86, 0.96];
  scrubberDots.forEach((dot, idx) => {
    dot.addEventListener('click', () => {
      const targetP = chapterTargets[idx] || 0;
      const trackRect = track.getBoundingClientRect();
      const trackTop = trackRect.top + window.scrollY;
      const trackHeight = track.offsetHeight;
      const viewportHeight = window.innerHeight;
      const scrollPos = trackTop + targetP * (trackHeight - viewportHeight);

      window.scrollTo({
        top: scrollPos,
        behavior: 'smooth'
      });
    });
  });

  // Start engine
  currentProgress = calculateProgress();
  renderLayers(currentProgress);
  rafId = requestAnimationFrame(tick);
})();
