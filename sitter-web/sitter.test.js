const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');

const source = fs.readFileSync(__dirname + '/sitter.js', 'utf8');
const token = 'A'.repeat(43);

async function render(response) {
  const elements = new Map();
  for (const id of ['loading', 'unavailable', 'care', 'pet-name', 'pet-kind', 'notes',
    'medical', 'contact', 'contact-phone', 'vet', 'vet-phone', 'expiry'])
    elements.set(id, { hidden: id === 'care' || id === 'unavailable', textContent: '' });
  const context = {
    location: { pathname: '/s/' + token },
    window: { PURRFOLIO_FIREBASE_API_KEY: 'test-key' },
    document: { getElementById: id => elements.get(id) },
    fetch: async () => response,
    Date, Intl, Number, encodeURIComponent
  };
  await vm.runInNewContext(source, context);
  return elements;
}

(async () => {
  const active = await render({ ok: true, json: async () => ({ fields: {
    active: { booleanValue: true }, expiresAt: { integerValue: String(Date.now() + 60000) },
    pet: { mapValue: { fields: { name: { stringValue: '<script>unsafe</script>' },
      notes: { stringValue: 'Feed twice daily' } } } }
  } }) });
  assert.equal(active.get('care').hidden, false);
  assert.equal(active.get('pet-name').textContent, '<script>unsafe</script>');
  assert.equal(active.get('notes').textContent, 'Feed twice daily');

  const denied = await render({ ok: false });
  assert.equal(denied.get('unavailable').hidden, false);
  assert.equal(denied.get('care').hidden, true);
  const expired = await render({ ok: true, json: async () => ({ fields: {
    active: { booleanValue: true }, expiresAt: { integerValue: String(Date.now() - 1000) }
  } }) });
  assert.equal(expired.get('unavailable').hidden, false);
  const revoked = await render({ ok: true, json: async () => ({ fields: {
    active: { booleanValue: false }, expiresAt: { integerValue: String(Date.now() + 60000) }
  } }) });
  assert.equal(revoked.get('unavailable').hidden, false);
  console.log('Sitter page active and unavailable states pass');
})().catch(error => { console.error(error); process.exitCode = 1; });
