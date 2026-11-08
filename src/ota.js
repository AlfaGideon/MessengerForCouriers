const OWNER = 'AlfaGideon';
const REPO = 'MessengerForCouriers';
const BRANCH = 'arena/52ee8420-messengerforcouriers';

export async function fetchOtaManifest() {
  const ref = encodeURIComponent(BRANCH);
  const url = `https://api.github.com/repos/${OWNER}/${REPO}/contents/release/ota.json?ref=${ref}&t=${Date.now()}`;
  const response = await fetch(url, {
    cache: 'no-store',
    headers: { Accept: 'application/vnd.github+json' },
  });
  if (!response.ok) throw new Error(`GitHub ответил ${response.status}`);
  const file = await response.json();
  if (!file.content) throw new Error('Манифест обновления недоступен.');
  const binary = atob(file.content.replace(/\s/g, ''));
  const bytes = Uint8Array.from(binary, character => character.charCodeAt(0));
  const manifest = JSON.parse(new TextDecoder().decode(bytes));
  const expectedUrl = `https://raw.githubusercontent.com/${OWNER}/${REPO}/${BRANCH}/release/web-update.zip`;
  if (!Number.isSafeInteger(manifest.webVersion) || manifest.webVersion < 1 ||
      manifest.webUpdateUrl !== expectedUrl || !/^[a-f0-9]{64}$/i.test(manifest.webUpdateSha256 || '')) {
    throw new Error('Неверный формат манифеста обновления.');
  }
  return manifest;
}
