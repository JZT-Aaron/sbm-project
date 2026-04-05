const mineflayer = require('mineflayer');
const { pathfinder, Movements, goals } = require('mineflayer-pathfinder');
const Vec3 = require('vec3');

const bot = mineflayer.createBot({
    host: process.env.MC_HOST,
    port: 25565,
    username: process.env.BOT_NAME,
    version: '1.21.4'
});

bot.loadPlugin(pathfinder);

// Runtime state flags
let side = null;
let joinedTeam = false;
let movingToField = false;

// Join retries and randomized field targets
const JOIN_RETRY_DELAY_MS = 1200;
const JOIN_RETRY_COUNT = 18;
const WALK_TIMEOUT_MS = 12000;
const WALK_RETRY_COUNT = 2;
const STUCK_CHECK_MS = 600;
const STUCK_MIN_MOVE = 0.2;
const STUCK_TICKS_BEFORE_NUDGE = 8;

const FIELD_TARGETS = {
    BLUE: [
        new Vec3(77, 0, 18),
        new Vec3(81, 0, 14),
        new Vec3(74, 0, 11),
        new Vec3(83, 0, 8)
    ],
    RED: [
        new Vec3(65, 0, -18),
        new Vec3(61, 0, -14),
        new Vec3(68, 0, -11),
        new Vec3(59, 0, -8)
    ]
};

function delay(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}

function nudgeForward() {
    bot.setControlState('jump', true);
    bot.setControlState('forward', true);
    setTimeout(() => bot.setControlState('jump', false), 280);
    setTimeout(() => bot.setControlState('forward', false), 520);
}

// Clear stale pathing/controls between rounds and phases
function resetMovementState() {
    bot.pathfinder.setGoal(null);
    bot.clearControlStates();
    movingToField = false;
}

// Pathfind to a block, recover from stalls, and fail fast on timeout
async function walkTo(x, y, z, timeoutMs = WALK_TIMEOUT_MS) {
    const goal = new goals.GoalBlock(x, y, z);
    bot.pathfinder.setGoal(goal);

    return new Promise((resolve) => {
        const target = new Vec3(x, y, z);
        const startedAt = Date.now();
        let lastPos = bot.entity.position.clone();
        let stuckTicks = 0;

        const checkArrival = setInterval(() => {
            if (bot.entity.position.distanceTo(target) < 1.2) {
                clearInterval(checkArrival);
                bot.pathfinder.setGoal(null);
                resolve(true);
                return;
            }

            if (Date.now() - startedAt > timeoutMs) {
                clearInterval(checkArrival);
                bot.pathfinder.setGoal(null);
                resolve(false);
                return;
            }

            const moved = bot.entity.position.distanceTo(lastPos);
            if (moved < STUCK_MIN_MOVE) {
                stuckTicks += 1;
            } else {
                stuckTicks = 0;
                lastPos = bot.entity.position.clone();
            }

            if (stuckTicks >= STUCK_TICKS_BEFORE_NUDGE) {
                stuckTicks = 0;
                nudgeForward();
            }
        }, 200);
    });
}

async function walkPath(points) {
    for (const point of points) {
        let reached = false;

        for (let attempt = 1; attempt <= WALK_RETRY_COUNT; attempt++) {
            reached = await walkTo(point.x, point.y, point.z);
            if (reached) break;

            console.warn(`[${bot.username}] Movement retry ${attempt}/${WALK_RETRY_COUNT} to ${point.x}, ${point.y}, ${point.z}`);
            resetMovementState();
            await delay(300);
        }

        if (!reached) {
            console.warn(`[${bot.username}] Failed path step at ${point.x}, ${point.y}, ${point.z}`);
            return false;
        }
    }

    return true;
}

function detectSide() {
    if (bot.entity.position.z > 0) {
        return 'BLUE';
    }

    return 'RED';
}

// Pick a random spread position so bots do not stack
function getFieldTarget() {
    const targets = FIELD_TARGETS[side || detectSide()];
    return targets[Math.floor(Math.random() * targets.length)];
}

// Repeatedly click the join item until the team message arrives
async function tryJoinTeam() {
    if (joinedTeam) return;

    for (let attempt = 0; attempt < JOIN_RETRY_COUNT && !joinedTeam; attempt++) {
        bot.setQuickBarSlot(0);
        bot.activateItem();
        bot.swingArm();
        await delay(JOIN_RETRY_DELAY_MS);
    }
}

// Move from gate area into the active field when round starts
async function spreadOutOnField() {
    if (movingToField || !joinedTeam) return;
    movingToField = true;
    bot.pathfinder.setGoal(null);

    const currentY = Math.floor(bot.entity.position.y);
    const target = getFieldTarget();
    let success = false;

    try {
        if (side === 'BLUE') {
            bot.setControlState('forward', true);
            await delay(1400);
            bot.setControlState('forward', false);
            success = await walkPath([
                new Vec3(76, currentY, 20),
                new Vec3(82, currentY, 12),
                new Vec3(83, currentY, 9),
                new Vec3(target.x, currentY, target.z)
            ]);
        } else {
            bot.setControlState('forward', true);
            await delay(1400);
            bot.setControlState('forward', false);
            success = await walkPath([
                new Vec3(66, currentY, -19),
                new Vec3(60, currentY, -12),
                new Vec3(59, currentY, -9),
                new Vec3(target.x, currentY, target.z)
            ]);
        }

        if (!success) return false;
        bot.lookAt(new Vec3(71.5, currentY + 1, 0.5));
        return true;
    } finally {
        movingToField = false;
    }
}

// Stage bot at the correct gate after team assignment
async function moveToGate() {
    const currentY = Math.floor(bot.entity.position.y);
    bot.pathfinder.setGoal(null);

    if (side === 'BLUE') {
        const success = await walkPath([
            new Vec3(84, currentY, 43),
            new Vec3(84, currentY, 38),
            new Vec3(73, currentY, 31)
        ]);
        if (!success) return false;
        bot.lookAt(new Vec3(74, 1, 25));
    } else {
        const success = await walkPath([
            new Vec3(58, currentY, -42),
            new Vec3(58, currentY, -38),
            new Vec3(69, currentY, -30)
        ]);
        if (!success) return false;
        bot.lookAt(new Vec3(68, 1, -25));
    }

    return true;
}

async function runFieldMoveWithRetries() {
    for (let attempt = 1; attempt <= 3; attempt++) {
        const success = await spreadOutOnField();
        if (success) return true;

        console.warn(`[${bot.username}] Field move retry ${attempt}/3`);
        resetMovementState();
        await delay(1200);
    }

    return false;
}

// Initial login flow: join game server, then team
bot.once('spawn', async () => {
    console.log(`[${bot.username}] Logged in. Preparing interaction...`);
    await delay(700);
    await bot.look(bot.entity.yaw, 0.4);
    await tryTeleport(bot.entity.position.clone());
    await delay(500);
    await tryJoinTeam();
});

// Uses slot-0 item to trigger lobby/game-server teleport
async function tryTeleport(position) {
    bot.setQuickBarSlot(0);
    for (let i = 0; i < 20; i++) { 
        bot.activateItem();
        bot.swingArm();
        await delay(1000);
        if (bot.entity.position.distanceTo(position) > 5) return;
    }
    console.log(`[${bot.username}] Failed to teleport.`);
}

// Chat-driven game flow handling
bot.on('messagestr', async (message) => {
    if (message.includes("You are now in Team") || message.includes("Du bist jetzt in ")) {
        joinedTeam = true;
        side = detectSide();

        const mcData = require('minecraft-data')(bot.version);
        const movements = new Movements(bot, mcData);
        movements.canDig = false;
        movements.allowDiagonal = false;
        bot.pathfinder.setMovements(movements);

        console.log(`[${bot.username}] ${side} side detected.`);
        resetMovementState();
        const atGate = await moveToGate();
        if (atGate) {
            bot.chat("I'm waiting at the gate.");
        } else {
            bot.chat("I got stuck while moving to gate, retrying soon.");
        }
    }

    if (message.includes("The gates are open!") || message.includes("Go!")) {
        console.log(`[${bot.username}] Gates are open. Starting final movement.`);

        if (!side) side = detectSide();

        resetMovementState();
        const moved = await runFieldMoveWithRetries();
        if (moved) {
            bot.chat("I am on the field now.");
        } else {
            bot.chat("I am still blocked, please reset my position.");
        }
    }
});

// Connection diagnostics
bot.on('error', (err) => console.error(`[${bot.username}] Error: ${err.message}`));
bot.on('kicked', (reason) => console.warn(`[${bot.username}] Kicked: ${reason}`));