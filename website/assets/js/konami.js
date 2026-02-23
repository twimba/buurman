(function () {
    'use strict';

    // ↑ ↑ ↓ ↓ ← → ← → B A
    var KONAMI = [38, 38, 40, 40, 37, 39, 37, 39, 66, 65];
    var seq    = [];

    document.addEventListener('keydown', function (e) {
        seq.push(e.keyCode);
        if (seq.length > KONAMI.length) seq.shift();
        if (seq.length === KONAMI.length && seq.every(function (k, i) { return k === KONAMI[i]; })) {
            activateDisco();
        }
    });

    // ── Party cast ────────────────────────────────────────────────────────────
    // type: 'dancer' | 'bbq' | 'drink'
    var CAST = [
        { type: 'dancer', emoji: '\uD83D\uDC69\u200D\uD83E\uDDB0', label: 'Apt 1A' }, // 👩‍🦰
        { type: 'bbq',    fire:  '\uD83D\uDD25',                                       // 🔥
                          meat:  '\uD83C\uDF56',                    label: 'BBQ'    }, // 🍖
        { type: 'dancer', emoji: '\uD83D\uDC83',                    label: 'Apt 2B' }, // 💃
        { type: 'dancer', emoji: '\uD83D\uDC68\u200D\uD83E\uDDB1',  label: 'Apt 3C' }, // 👨‍🦱
        { type: 'dancer', emoji: '\uD83D\uDD7A',                    label: 'Apt 4D' }, // 🕺
        { type: 'drink',  emoji: '\uD83C\uDF7A',                    label: 'Bar'    }, // 🍺
        { type: 'dancer', emoji: '\uD83D\uDC75',                    label: 'Apt 5E' }, // 👵
        { type: 'bbq',    fire:  '\uD83D\uDD25',                                       // 🔥
                          meat:  '\uD83E\uDD69',                    label: 'Grill'  }, // 🥩
        { type: 'dancer', emoji: '\uD83D\uDC74',                    label: 'Apt 6F' }, // 👴
        { type: 'dancer', emoji: '\uD83E\uDDD1\u200D\uD83E\uDDB2',  label: 'Apt 7G' }, // 🧑‍🦲
        { type: 'dancer', emoji: '\uD83D\uDC68\u200D\uD83E\uDDB3',  label: 'Apt PH' }, // 👨‍🦳
    ];

    // ── CSS ───────────────────────────────────────────────────────────────────
    var CSS = [
        // Overlay — warm dark outdoor-night
        '#buurman-disco{position:fixed;inset:0;z-index:99999;display:flex;flex-direction:column;',
        'align-items:center;justify-content:space-between;',
        'background:linear-gradient(185deg,rgba(6,3,1,.97) 0%,rgba(22,8,2,.97) 100%);',
        'cursor:pointer;overflow:hidden;user-select:none;animation:discoIn .5s ease-out forwards;}',
        '@keyframes discoIn{0%{opacity:0}40%{opacity:1}55%{opacity:.6}72%{opacity:1}100%{opacity:1}}',

        // Fire-toned spotlights
        '.disco-spot{position:absolute;border-radius:50%;filter:blur(90px);pointer-events:none;}',
        '.disco-s1{width:460px;height:460px;background:#ff4500;opacity:.34;animation:ds1 3.2s ease-in-out infinite}',
        '.disco-s2{width:400px;height:400px;background:#74c0fc;opacity:.30;animation:ds2 4.6s ease-in-out infinite}',
        '.disco-s3{width:380px;height:380px;background:#ffb300;opacity:.28;animation:ds3 3.9s ease-in-out infinite}',
        '.disco-s4{width:420px;height:420px;background:#cc5de8;opacity:.26;animation:ds4 3.0s ease-in-out infinite}',
        '.disco-s5{width:350px;height:350px;background:#ff7b00;opacity:.30;animation:ds5 2.8s ease-in-out infinite}',
        '@keyframes ds1{0%{transform:translate(-35vw,-25vh)}33%{transform:translate(25vw,35vh)}66%{transform:translate(-15vw,-42vh)}100%{transform:translate(-35vw,-25vh)}}',
        '@keyframes ds2{0%{transform:translate(42vw,-32vh)}33%{transform:translate(-22vw,22vh)}66%{transform:translate(32vw,42vh)}100%{transform:translate(42vw,-32vh)}}',
        '@keyframes ds3{0%{transform:translate(5vw,40vh)}50%{transform:translate(38vw,-35vh)}100%{transform:translate(5vw,40vh)}}',
        '@keyframes ds4{0%{transform:translate(-42vw,32vh)}50%{transform:translate(15vw,-22vh)}100%{transform:translate(-42vw,32vh)}}',
        '@keyframes ds5{0%{transform:translate(12vw,10vh)}25%{transform:translate(-32vw,40vh)}75%{transform:translate(44vw,-12vh)}100%{transform:translate(12vw,10vh)}}',

        // Stars (tiny static dots for outdoor night feel)
        '.disco-stars{position:absolute;inset:0;pointer-events:none;',
        'background-image:radial-gradient(1px 1px at 15% 12%,rgba(255,255,255,.55) 0%,transparent 100%),',
        'radial-gradient(1px 1px at 72% 8%,rgba(255,255,255,.45) 0%,transparent 100%),',
        'radial-gradient(1.5px 1.5px at 40% 18%,rgba(255,255,255,.60) 0%,transparent 100%),',
        'radial-gradient(1px 1px at 85% 22%,rgba(255,255,255,.40) 0%,transparent 100%),',
        'radial-gradient(1px 1px at 28% 30%,rgba(255,255,255,.35) 0%,transparent 100%),',
        'radial-gradient(1.5px 1.5px at 60% 5%,rgba(255,255,255,.55) 0%,transparent 100%),',
        'radial-gradient(1px 1px at 92% 35%,rgba(255,255,255,.45) 0%,transparent 100%),',
        'radial-gradient(1px 1px at 8% 40%,rgba(255,255,255,.40) 0%,transparent 100%);}',

        // Moon
        '.disco-moon{position:absolute;top:2.5rem;right:4rem;font-size:2.2rem;',
        'opacity:.7;animation:moonGlow 4s ease-in-out infinite alternate;pointer-events:none;}',
        '@keyframes moonGlow{from{filter:drop-shadow(0 0 8px rgba(255,230,150,.4))}',
        'to{filter:drop-shadow(0 0 20px rgba(255,230,150,.8))}}',

        // Top section
        '.disco-top{display:flex;flex-direction:column;align-items:center;',
        'padding-top:clamp(.6rem,3.5vh,2rem);position:relative;z-index:1;gap:.45rem;}',

        // Disco ball — warm amber glow
        '.disco-ball{font-size:clamp(4.5rem,12vw,8rem);line-height:1;animation:ballPulse 1.3s ease-in-out infinite;}',
        '@keyframes ballPulse{',
        '0%  {transform:scale(1) rotate(-4deg);filter:drop-shadow(0 0 18px rgba(255,180,50,.8))}',
        '25% {transform:scale(1.07) rotate(4deg);filter:drop-shadow(0 0 55px rgba(255,100,0,.95)) drop-shadow(0 0 80px rgba(255,60,0,.35))}',
        '50% {transform:scale(1) rotate(-4deg);filter:drop-shadow(0 0 18px rgba(255,200,100,.8))}',
        '75% {transform:scale(1.07) rotate(4deg);filter:drop-shadow(0 0 55px rgba(255,220,80,.95)) drop-shadow(0 0 80px rgba(255,200,0,.35))}',
        '100%{transform:scale(1) rotate(-4deg);filter:drop-shadow(0 0 18px rgba(255,180,50,.8))}}',

        // Title
        '.disco-title{font-family:"Inter",-apple-system,sans-serif;font-size:clamp(1.6rem,5.5vw,3.75rem);',
        'font-weight:900;letter-spacing:-.02em;color:#fff;text-align:center;padding:0 1rem;',
        'animation:titleGlow .65s ease-in-out infinite alternate;}',
        '@keyframes titleGlow{',
        'from{text-shadow:0 0 18px rgba(255,160,40,.9),0 0 38px rgba(255,80,0,.5);transform:scale(1)}',
        'to{text-shadow:0 0 28px #fff,0 0 65px rgba(255,140,0,.9),0 0 100px rgba(255,50,0,.5);transform:scale(1.03)}}',


        // Neighborhood silhouette strip
        '.disco-hood{width:100%;text-align:center;font-size:clamp(1.4rem,3.5vw,2.2rem);',
        'line-height:1;opacity:.18;position:relative;z-index:1;letter-spacing:.2rem;',
        'margin-bottom:-0.5rem;pointer-events:none;}',

        // Dance floor
        '.disco-floor{width:100%;position:relative;z-index:1;',
        'background:linear-gradient(to top,rgba(28,9,2,.98) 0%,rgba(18,5,1,.75) 55%,transparent 100%);',
        'padding:clamp(.6rem,2vh,1.2rem) 0 clamp(.4rem,1.2vh,.8rem);}',
        '.disco-floor-row{display:flex;justify-content:center;align-items:flex-end;',
        'gap:clamp(.3rem,2vw,1.4rem);padding:0 .75rem;flex-wrap:wrap;}',

        // Dancer
        '.disco-dancer{display:flex;flex-direction:column;align-items:center;gap:.22rem;flex-shrink:0;}',
        '.disco-dancer-emoji{font-size:clamp(2.6rem,6vw,4.2rem);line-height:1;display:block;}',
        '.disco-cast-label{font-family:"Inter",-apple-system,sans-serif;font-size:.56rem;',
        'letter-spacing:.1em;text-transform:uppercase;color:rgba(255,200,130,.45);font-weight:600;}',

        // BBQ station
        '.disco-bbq{display:flex;flex-direction:column;align-items:center;gap:.08rem;flex-shrink:0;}',
        '.disco-bbq-fire{font-size:clamp(1.1rem,2.7vw,1.9rem);line-height:1;',
        'animation:bbqFlicker .35s ease-in-out infinite alternate;}',
        '.disco-bbq-meat{font-size:clamp(2.1rem,4.8vw,3.4rem);line-height:1;',
        'animation:bbqSizzle 1.9s ease-in-out infinite;}',
        '@keyframes bbqFlicker{from{transform:scale(1) rotate(-6deg);filter:drop-shadow(0 0 4px #ff6b00)}',
        'to{transform:scale(1.25) rotate(6deg);filter:drop-shadow(0 0 10px #ffaa00)}}',
        '@keyframes bbqSizzle{0%,100%{transform:scale(1) rotate(-2deg)}50%{transform:scale(1.06) rotate(2deg) translateY(-3px)}}',

        // Drinks table
        '.disco-drink{display:flex;flex-direction:column;align-items:center;gap:.22rem;flex-shrink:0;}',
        '.disco-drink-emoji{font-size:clamp(2.4rem,5.2vw,3.7rem);line-height:1;',
        'animation:drinkWobble 1.3s ease-in-out infinite;}',
        '@keyframes drinkWobble{0%,100%{transform:rotate(-9deg)}50%{transform:rotate(9deg) translateY(-6px)}}',

        // Dance moves (4 variants)
        '@keyframes danceA{0%,100%{transform:translateY(0) rotate(-5deg)}',
        '25%{transform:translateY(-22px) rotate(5deg) scale(1.12)}',
        '50%{transform:translateY(-6px) rotate(-2deg)}',
        '75%{transform:translateY(-18px) rotate(7deg) scale(1.08)}}',
        '@keyframes danceB{0%,100%{transform:translateY(0) rotate(6deg)}',
        '20%{transform:translateY(-26px) rotate(-10deg) scale(1.14)}',
        '40%{transform:translateY(-8px) rotate(4deg) scale(.94)}',
        '60%{transform:translateY(-22px) rotate(-8deg) scale(1.1)}',
        '80%{transform:translateY(-4px) rotate(5deg) scale(.97)}}',
        '@keyframes danceC{0%,100%{transform:translateY(0) scale(1) rotate(3deg)}',
        '35%{transform:translateY(-32px) scale(1.18) rotate(-8deg)}',
        '65%{transform:translateY(-12px) scale(.92) rotate(6deg)}}',
        '@keyframes danceD{0%,100%{transform:translateX(0) translateY(0) rotate(0)}',
        '20%{transform:translateX(-9px) translateY(-14px) rotate(-8deg)}',
        '40%{transform:translateX(9px) translateY(-22px) rotate(8deg)}',
        '60%{transform:translateX(-7px) translateY(-16px) rotate(-6deg)}',
        '80%{transform:translateX(7px) translateY(-8px) rotate(5deg)}}',

        // Floating items (music notes + food + drinks + smoke)
        '.disco-float{position:absolute;bottom:-3rem;font-size:clamp(.9rem,2.2vw,1.6rem);',
        'pointer-events:none;opacity:0;animation:floatUp linear infinite;}',
        '@keyframes floatUp{0%{transform:translateY(0) rotate(-8deg);opacity:0}',
        '8%{opacity:.82}92%{opacity:.82}100%{transform:translateY(-115vh) rotate(8deg);opacity:0}}',
        '.disco-smoke{animation:smokeDrift linear infinite;}',
        '@keyframes smokeDrift{0%{transform:translateY(0) translateX(0) scale(1);opacity:0}',
        '10%{opacity:.45}90%{opacity:.2}100%{transform:translateY(-80vh) translateX(35px) scale(1.8);opacity:0}}',

        // Hint
        '.disco-hint{font-family:"Inter",-apple-system,sans-serif;font-size:.66rem;',
        'letter-spacing:.14em;text-transform:uppercase;color:rgba(255,220,160,.2);',
        'padding-bottom:clamp(.4rem,1.2vh,.8rem);text-align:center;position:relative;z-index:1;}',
    ].join('');

    // ── Audio: "Disco Inferno" — The Trammps (1976) ────────────────────────────
    // Synthesised at 127 BPM · key: Bb major
    // Bb2=116.5  Eb2=77.8  F2=87.3  G2=98.0  C3=130.8
    // Burn-baby-burn motif: Bb4=466 · F4=349 · Eb4=311 · Db4=277 · Bb3=233
    function startAudio() {
        try {
            var AC = window.AudioContext || window.webkitAudioContext;
            if (!AC) return null;
            var ctx    = new AC();
            var master = ctx.createGain(); master.gain.value = 0.60; master.connect(ctx.destination);

            var BPM  = 127;
            var BEAT = 60 / BPM;   // ≈ 0.4724 s
            var HALF = BEAT / 2;   // 8th note
            var BAR  = BEAT * 4;

            var BASS_FREQS = [116.5, 116.5, 77.8, 87.3, 116.5, 98.0, 87.3, 116.5]; // Bb groove
            var HORN_FREQS = [466.16, 466.16, 466.16, 0, 349.23, 311.13, 277.18, 0]; // "burn-ba-by-burn"
            var barN = 0;

            function noiseBuf(secs) {
                var len = Math.ceil(ctx.sampleRate * secs), b = ctx.createBuffer(1, len, ctx.sampleRate), d = b.getChannelData(0);
                for (var i = 0; i < len; i++) d[i] = Math.random() * 2 - 1;
                return b;
            }
            function kick(t) {
                var o = ctx.createOscillator(), g = ctx.createGain();
                o.frequency.setValueAtTime(175, t); o.frequency.exponentialRampToValueAtTime(0.01, t + 0.42);
                g.gain.setValueAtTime(1.3, t); g.gain.exponentialRampToValueAtTime(0.001, t + 0.42);
                o.connect(g); g.connect(master); o.start(t); o.stop(t + 0.45);
            }
            function snare(t) {
                var s = ctx.createBufferSource(); s.buffer = noiseBuf(0.14);
                var f = ctx.createBiquadFilter(); f.type = 'highpass'; f.frequency.value = 1800;
                var g = ctx.createGain(); g.gain.setValueAtTime(0.52, t); g.gain.exponentialRampToValueAtTime(0.001, t + 0.14);
                s.connect(f); f.connect(g); g.connect(master); s.start(t); s.stop(t + 0.18);
            }
            function hihat(t, open) {
                var dur = open ? 0.18 : 0.04;
                var s = ctx.createBufferSource(); s.buffer = noiseBuf(dur);
                var f = ctx.createBiquadFilter(); f.type = 'bandpass'; f.frequency.value = 10000; f.Q.value = 0.4;
                var g = ctx.createGain(); g.gain.setValueAtTime(open ? 0.19 : 0.08, t); g.gain.exponentialRampToValueAtTime(0.001, t + dur);
                s.connect(f); f.connect(g); g.connect(master); s.start(t); s.stop(t + dur + 0.02);
            }
            function bass(t, freq) {
                var o = ctx.createOscillator(); o.type = 'sawtooth'; o.frequency.value = freq;
                var f = ctx.createBiquadFilter(); f.type = 'lowpass'; f.frequency.value = 360; f.Q.value = 2.8;
                var g = ctx.createGain(); g.gain.setValueAtTime(0.58, t); g.gain.exponentialRampToValueAtTime(0.001, t + 0.24);
                o.connect(f); f.connect(g); g.connect(master); o.start(t); o.stop(t + 0.27);
            }
            function horn(t, freq) {
                var o = ctx.createOscillator(); o.type = 'sawtooth'; o.frequency.value = freq;
                var f = ctx.createBiquadFilter(); f.type = 'bandpass'; f.frequency.value = 960; f.Q.value = 3.5;
                var g = ctx.createGain();
                g.gain.setValueAtTime(0, t); g.gain.linearRampToValueAtTime(0.40, t + 0.018);
                g.gain.setValueAtTime(0.40, t + 0.09); g.gain.exponentialRampToValueAtTime(0.001, t + 0.22);
                o.connect(f); f.connect(g); g.connect(master); o.start(t); o.stop(t + 0.25);
            }
            // Bb major string stab on beats 1 & 3
            function strings(t) {
                [233.08, 293.66, 349.23].forEach(function (freq) {
                    var o = ctx.createOscillator(); o.type = 'triangle'; o.frequency.value = freq;
                    var g = ctx.createGain();
                    g.gain.setValueAtTime(0, t); g.gain.linearRampToValueAtTime(0.11, t + 0.03); g.gain.exponentialRampToValueAtTime(0.001, t + 0.34);
                    o.connect(g); g.connect(master); o.start(t); o.stop(t + 0.37);
                });
            }
            function scheduleBar(t) {
                var doHorn = (barN % 2 === 0); barN++;
                for (var i = 0; i < 8; i++) {
                    var ti = t + i * HALF;
                    if (i % 2 === 0) kick(ti);
                    if (i === 2 || i === 6) snare(ti);
                    hihat(ti, i % 2 !== 0);
                    bass(ti, BASS_FREQS[i]);
                    if (i === 0 || i === 4) strings(ti);
                    if (doHorn && HORN_FREQS[i] > 0) horn(ti, HORN_FREQS[i]);
                }
            }
            var nextBar = ctx.currentTime + 0.08, alive = true;
            function tick() {
                if (!alive) return;
                while (nextBar < ctx.currentTime + BAR * 2) { scheduleBar(nextBar); nextBar += BAR; }
                setTimeout(tick, 120);
            }
            tick();
            return function () { alive = false; try { ctx.close(); } catch (e) {} };
        } catch (e) { return null; }
    }

    // ── Activate ──────────────────────────────────────────────────────────────
    function activateDisco() {
        if (document.getElementById('buurman-disco')) return;

        var style = document.createElement('style');
        style.id = 'buurman-disco-style'; style.textContent = CSS;
        document.head.appendChild(style);

        var DANCES  = ['danceA', 'danceB', 'danceC', 'danceD'];
        var BEAT    = 60 / 127;
        var di      = 0;

        var castHtml = CAST.map(function (c) {
            if (c.type === 'bbq') {
                return '<div class="disco-bbq">' +
                    '<span class="disco-bbq-fire">' + c.fire + '</span>' +
                    '<span class="disco-bbq-meat">' + c.meat + '</span>' +
                    '<span class="disco-cast-label">' + c.label + '</span>' +
                    '</div>';
            }
            if (c.type === 'drink') {
                return '<div class="disco-drink">' +
                    '<span class="disco-drink-emoji">' + c.emoji + '</span>' +
                    '<span class="disco-cast-label">' + c.label + '</span>' +
                    '</div>';
            }
            var anim  = DANCES[di % 4];
            var dur   = (BEAT * (1 + (di % 3) * 0.22)).toFixed(3);
            var delay = (di * BEAT / 8).toFixed(3);
            di++;
            return '<div class="disco-dancer">' +
                '<span class="disco-dancer-emoji" style="animation:' + anim + ' ' + dur + 's ease-in-out ' + delay + 's infinite">' + c.emoji + '</span>' +
                '<span class="disco-cast-label">' + c.label + '</span>' +
                '</div>';
        }).join('');

        var overlay = document.createElement('div');
        overlay.id  = 'buurman-disco';
        overlay.innerHTML =
            '<div class="disco-stars"></div>' +
            '<div class="disco-moon">\uD83C\uDF15</div>' +  // 🌕
            '<div class="disco-spot disco-s1"></div><div class="disco-spot disco-s2"></div>' +
            '<div class="disco-spot disco-s3"></div><div class="disco-spot disco-s4"></div>' +
            '<div class="disco-spot disco-s5"></div>' +
            '<div class="disco-top">' +
                '<div class="disco-ball">\uD83E\uDEA9</div>' +
                '<div class="disco-title">\uD83D\uDD25 Block Party! \uD83D\uDD25</div>' +
            '</div>' +
            '<div class="disco-hood">\uD83C\uDFE0\uD83C\uDFE1\uD83C\uDFE0\uD83C\uDFE0\uD83C\uDFE1\uD83C\uDFE0\uD83C\uDFE1</div>' +  // 🏠🏡🏠🏠🏡🏠🏡
            '<div class="disco-floor"><div class="disco-floor-row">' + castHtml + '</div></div>' +
            '<div class="disco-hint">click anywhere \u00B7 esc to exit</div>';

        document.body.appendChild(overlay);
        document.body.style.overflow = 'hidden';

        var stopAudio = startAudio();

        // Floating mix: music notes, food, drinks, BBQ smoke
        var FLOAT_GROUPS = [
            { pool: ['\u266A','\u266B','\u266C','\uD83C\uDFB5','\uD83C\uDFB6'], smoke: false },
            { pool: ['\uD83C\uDF7A','\uD83C\uDF79','\uD83E\uDD42','\uD83C\uDF77'], smoke: false },
            { pool: ['\uD83C\uDF2D','\uD83C\uDF56','\uD83E\uDD69','\uD83C\uDF57'], smoke: false },
            { pool: ['\uD83D\uDCA8','\uD83D\uDCA8','\uD83D\uDCA8'],               smoke: true  },
        ];
        var noteTimer = setInterval(function () {
            var g = FLOAT_GROUPS[Math.floor(Math.random() * FLOAT_GROUPS.length)];
            var n = document.createElement('span');
            n.className = g.smoke ? 'disco-float disco-smoke' : 'disco-float';
            n.textContent = g.pool[Math.floor(Math.random() * g.pool.length)];
            n.style.left = (Math.random() * 88 + 4) + 'vw';
            var dur = 4.5 + Math.random() * 4;
            n.style.animationDuration = dur + 's';
            n.style.animationDelay    = (Math.random() * 1.2) + 's';
            overlay.appendChild(n);
            setTimeout(function () { if (n.parentNode) n.parentNode.removeChild(n); }, (dur + 2) * 1000);
        }, 460);

        function exit() {
            clearInterval(noteTimer);
            if (stopAudio) stopAudio();
            var el = document.getElementById('buurman-disco');
            var st = document.getElementById('buurman-disco-style');
            if (el) el.parentNode.removeChild(el);
            if (st) st.parentNode.removeChild(st);
            document.body.style.overflow = '';
            document.removeEventListener('keydown', onKey);
            seq = [];
        }
        function onKey(e) { if (e.key === 'Escape') exit(); }
        overlay.addEventListener('click', exit);
        document.addEventListener('keydown', onKey);
    }
}());
