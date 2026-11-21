/* A sitter link is a read-only snapshot. Never render shared text as HTML. */
(async function () {
  const token = location.pathname.match(/^\/s\/([A-Za-z0-9_-]{43})\/?$/)?.[1];
  const key = window.PURRFOLIO_FIREBASE_API_KEY;
  const loading = document.getElementById('loading');
  const unavailable = document.getElementById('unavailable');
  const care = document.getElementById('care');
  const showUnavailable = () => { loading.hidden = true; unavailable.hidden = false; };
  if (!token || !key) { showUnavailable(); return; }
  try {
    const endpoint = 'https://firestore.googleapis.com/v1/projects/purrfolio-b1e6a/databases/(default)/documents/sitterLinks/' + encodeURIComponent(token) + '?key=' + encodeURIComponent(key);
    const response = await fetch(endpoint, { cache: 'no-store', referrerPolicy: 'no-referrer' });
    if (!response.ok) { showUnavailable(); return; }
    const data = (await response.json()).fields || {};
    const value = (field) => field?.stringValue || '';
    const expiry = Number(data.expiresAt?.integerValue || 0);
    if (data.active?.booleanValue !== true || expiry <= Date.now()) { showUnavailable(); return; }
    const pet = data.pet?.mapValue?.fields || {};
    const set = (id, text) => { document.getElementById(id).textContent = text || 'Not provided'; };
    set('pet-name', value(pet.name));
    set('pet-kind', [value(pet.species), value(pet.breed)].filter(Boolean).join(' · '));
    set('notes', value(pet.notes));
    set('medical', value(pet.medicalInfo));
    set('contact', value(pet.emergencyContactName));
    set('contact-phone', value(pet.emergencyContactPhone));
    set('vet', value(pet.vetClinic));
    set('vet-phone', value(pet.vetPhone));
    set('expiry', 'Link expires ' + new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(expiry)));
    loading.hidden = true;
    care.hidden = false;
  } catch (_) { showUnavailable(); }
})();
