/**
 * Jarvis Core Reactor - Interactive 3D Glowing Orb
 * Communicates with Android Kotlin via window.setJarvisState(state)
 */
const canvas = document.getElementById('orbCanvas');
const ctx = canvas.getContext('2d');

let width, height, centerX, centerY;
let currentState = 'IDLE'; // IDLE, LISTENING, THINKING, SPEAKING
let rotation = 0;
let pulse = 0;
let particles = [];

function resize() {
    width = window.innerWidth;
    height = window.innerHeight;
    canvas.width = width * window.devicePixelRatio;
    canvas.height = height * window.devicePixelRatio;
    ctx.scale(window.devicePixelRatio, window.devicePixelRatio);
    centerX = width / 2;
    centerY = height / 2;
    initParticles();
}

window.addEventListener('resize', resize);

function initParticles() {
    particles = [];
    const count = 70;
    for (let i = 0; i < count; i++) {
        const angle = Math.random() * Math.PI * 2;
        const radius = 60 + Math.random() * 50;
        particles.push({
            angle: angle,
            radius: radius,
            speed: (Math.random() * 0.02 + 0.01) * (Math.random() > 0.5 ? 1 : -1),
            size: Math.random() * 2.5 + 1.2,
            alpha: Math.random() * 0.7 + 0.3
        });
    }
}

// Function called directly from Kotlin WebView
window.setJarvisState = function(state) {
    currentState = state;
};

function getThemeColors() {
    switch (currentState) {
        case 'LISTENING':
            return {
                core: 'rgba(0, 229, 255, ',
                ring: 'rgba(0, 255, 200, ',
                outer: 'rgba(0, 150, 255, '
            };
        case 'THINKING':
            return {
                core: 'rgba(180, 100, 255, ',
                ring: 'rgba(255, 120, 200, ',
                outer: 'rgba(120, 50, 255, '
            };
        case 'SPEAKING':
            return {
                core: 'rgba(0, 255, 180, ',
                ring: 'rgba(0, 200, 255, ',
                outer: 'rgba(0, 255, 120, '
            };
        case 'IDLE':
        default:
            return {
                core: 'rgba(0, 210, 255, ',
                ring: 'rgba(40, 120, 255, ',
                outer: 'rgba(0, 180, 255, '
            };
    }
}

function render() {
    ctx.clearRect(0, 0, width, height);

    const colors = getThemeColors();
    pulse += 0.04;
    const speedMultiplier = currentState === 'THINKING' ? 2.5 : (currentState === 'SPEAKING' ? 1.8 : 1.0);
    rotation += 0.015 * speedMultiplier;

    const baseRadius = Math.min(width, height) * 0.22;
    const currentRadius = baseRadius + Math.sin(pulse) * (currentState === 'SPEAKING' ? 12 : 5);

    // 1. Outer Diffuse Glow
    const glowGradient = ctx.createRadialGradient(centerX, centerY, baseRadius * 0.2, centerX, centerY, currentRadius * 1.8);
    glowGradient.addColorStop(0, colors.core + '0.45)');
    glowGradient.addColorStop(0.5, colors.outer + '0.15)');
    glowGradient.addColorStop(1, 'rgba(0, 0, 0, 0)');
    ctx.fillStyle = glowGradient;
    ctx.beginPath();
    ctx.arc(centerX, centerY, currentRadius * 1.8, 0, Math.PI * 2);
    ctx.fill();

    // 2. Gyroscopic Rotating Rings
    for (let r = 1; r <= 3; r++) {
        ctx.save();
        ctx.translate(centerX, centerY);
        const ringRot = rotation * (r % 2 === 0 ? 1 : -1) * (1 + r * 0.2);
        ctx.rotate(ringRot);

        ctx.beginPath();
        const rx = currentRadius * (0.8 + r * 0.18);
        const ry = rx * 0.38;
        ctx.ellipse(0, 0, rx, ry, 0, 0, Math.PI * 2);
        ctx.strokeStyle = colors.ring + (0.35 + Math.sin(pulse + r) * 0.2) + ')';
        ctx.lineWidth = 1.6;
        ctx.stroke();

        ctx.restore();
    }

    // 3. Floating Orbit Particles
    particles.forEach(p => {
        p.angle += p.speed * speedMultiplier;
        const px = centerX + Math.cos(p.angle) * p.radius;
        const py = centerY + Math.sin(p.angle) * p.radius * 0.85;

        ctx.beginPath();
        ctx.arc(px, py, p.size, 0, Math.PI * 2);
        ctx.fillStyle = colors.core + p.alpha + ')';
        ctx.shadowColor = colors.core + '0.8)';
        ctx.shadowBlur = 8;
        ctx.fill();
        ctx.shadowBlur = 0;
    });

    // 4. Dense Energy Core
    const coreGrad = ctx.createRadialGradient(centerX, centerY, 0, centerX, centerY, currentRadius * 0.7);
    coreGrad.addColorStop(0, '#FFFFFF');
    coreGrad.addColorStop(0.3, colors.core + '0.95)');
    coreGrad.addColorStop(0.7, colors.ring + '0.5)');
    coreGrad.addColorStop(1, 'rgba(0,0,0,0)');

    ctx.fillStyle = coreGrad;
    ctx.beginPath();
    ctx.arc(centerX, centerY, currentRadius * 0.7, 0, Math.PI * 2);
    ctx.fill();

    requestAnimationFrame(render);
}

resize();
render();
