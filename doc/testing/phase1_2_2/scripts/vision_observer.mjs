#!/usr/bin/env node
/**
 * Phase 1.2.2 Vision Observer.
 *
 * Uses an available lightweight vision model as "eyes" for validation
 * screenshots. Model is CONFIGURABLE via VISION_MODEL env, never hardcoded
 * as the sole truth. The observer only OBSERVES; final PASS decisions are
 * never made by vision alone.
 *
 * Supported backends:
 *   VISION_MODEL=auto       (default) kimi first, auto-fallback to codex-luna
 *   VISION_MODEL=kimi      -> TokenRouter moonshotai/kimi-k3-free (direct API)
 *   VISION_MODEL=codex-luna-> Codex CLI + gpt-5.6-luna (OpenAI ChatGPT login)
 *   VISION_MODEL=<name>    -> any TokenRouter model id (direct API)
 *
 * Credentials:
 *   kimi:   OPENCODE_AUTH=~/.local/share/opencode/auth.json (tokenrouter key)
 *   codex-luna: codex CLI logged in (ChatGPT). codex bin from env CODEX_BIN.
 *
 * Usage:
 *   node vision_observer.mjs <image.png>
 * Output (stdout): single JSON object (machine-readable).
 */
import { execFileSync } from 'node:child_process';
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { homedir } from 'node:os';

const MODEL = process.env.VISION_MODEL || 'auto';
const AUTH = process.env.OPENCODE_AUTH || resolve(homedir(), '.local/share/opencode/auth.json');
const TOKENROUTER_URL = 'https://api.tokenrouter.com/v1/chat/completions';

const PROMPT_TEXT =
  'You are a screenshot classifier. Classify this screenshot. ' +
  'Output ONLY a compact JSON object with EXACTLY these keys and no other keys, no markdown, no code fences: ' +
  '{"status":"PASS|SUSPECT|FAIL|HUMAN_REQUIRED","screen_state":"MAIN_MENU|IN_WORLD|LOADING|WARNING|CRASH|PAUSE|OTHER","rail_visible":true,"left_rail_visible":true,"right_rail_visible":true,"sleepers_visible":true,"obvious_gap":false,"obvious_break":false,"camera_valid":true,"notes":"one short sentence","confidence":0.9}. ' +
  'status is PASS if the screenshot clearly shows the expected state; SUSPECT if uncertain; FAIL if clearly wrong; HUMAN_REQUIRED if a human must look. ' +
  'screen_state: WARNING for content/username dialogs, MAIN_MENU for the title menu, LOADING for loading/downloading screens, IN_WORLD for gameplay, PAUSE for the pause menu, CRASH for error/crash screens, OTHER otherwise.';

function loadTokenrouterKey() {
  const d = JSON.parse(readFileSync(AUTH, 'utf8'));
  return d?.tokenrouter?.key;
}

async function callTokenRouter(imgB64) {
  const key = loadTokenrouterKey();
  if (!key) throw new Error('tokenrouter key not found in ' + AUTH);
  const modelId = MODEL === 'kimi' || MODEL === 'auto' ? 'moonshotai/kimi-k3-free' : MODEL;
  const body = {
    model: modelId,
    messages: [
      {
        role: 'user',
        content: [
          { type: 'text', text: PROMPT_TEXT },
          { type: 'image_url', image_url: { url: 'data:image/png;base64,' + imgB64 } },
        ],
      },
    ],
    max_tokens: 600,
    temperature: 0.0,
  };
  const res = await fetch(TOKENROUTER_URL, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Authorization: 'Bearer ' + key },
    body: JSON.stringify(body),
  });
  if (!res.ok) {
    const t = await res.text();
    throw new Error('tokenrouter HTTP ' + res.status + ' ' + t.slice(0, 300));
  }
  const j = await res.json();
  return j.choices?.[0]?.message?.content || '';
}

function callCodexLuna(imgPath) {
  const codex = process.env.CODEX_BIN || 'codex';
  const prompt =
    '画像ファイル ' + imgPath + ' を読み込んで、' + PROMPT_TEXT +
    ' 結果はJSONのみ出力してください。';
  const out = execFileSync(codex, ['exec', '--skip-git-repo-check', '-c', 'model="gpt-5.6-luna"', '--sandbox', 'read-only', '-'], {
    input: prompt,
    encoding: 'utf8',
    timeout: 180000,
    env: process.env,
  });
  return out;
}

function extractJson(text) {
  const m = text.match(/\{[\s\S]*\}/);
  if (!m) return null;
  try {
    return JSON.parse(m[0]);
  } catch (_) {
    return null;
  }
}

async function main() {
  const imgPath = process.argv[2];
  if (!imgPath) {
    console.error('usage: node vision_observer.mjs <image.png>');
    process.exit(2);
  }
  const imgB64 = readFileSync(imgPath).toString('base64');
  let raw;
  let primaryErr = null;
  const model = MODEL === 'auto' ? 'kimi' : MODEL;
  if (model === 'codex-luna') {
    raw = callCodexLuna(imgPath);
  } else {
    try {
      raw = await callTokenRouter(imgB64);
    } catch (e) {
      primaryErr = e;
    }
  }
  let obj = extractJson(raw);
  if (!obj && MODEL === 'auto') {
    // Primary model unavailable or unparsable; fall back to Codex GPT-5.6 Luna.
    console.error('kimi failed (' + (primaryErr ? primaryErr.message : 'parse') + '), falling back to codex-luna');
    try {
      raw = callCodexLuna(imgPath);
      obj = extractJson(raw);
    } catch (e) {
      console.error('codex-luna fallback error: ' + e.message);
    }
  }
  if (!obj) {
    // Machine-readable fallback even on parse failure.
    console.log(JSON.stringify({ status: 'SUSPECT', screen_state: 'OTHER', rail_visible: false, camera_valid: false, notes: 'vision parse failed: ' + raw.slice(0, 120).replace(/\n/g, ' '), confidence: 0.0 }));
    process.exit(0);
  }
  console.log(JSON.stringify(obj));
  process.exit(0);
}

main().catch((e) => {
  console.log(JSON.stringify({ status: 'HUMAN_REQUIRED', screen_state: 'OTHER', rail_visible: false, camera_valid: false, notes: 'vision error: ' + e.message, confidence: 0.0 }));
  process.exit(0);
});
