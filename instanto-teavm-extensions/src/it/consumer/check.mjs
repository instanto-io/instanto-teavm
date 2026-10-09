import assert from 'node:assert/strict';
import { pathToFileURL } from 'node:url';

const app = await import(pathToFileURL(process.argv[2]));
const expected = Number(process.argv[3]);
let completions = 0;
app.main([], error => {
  assert.equal(error, undefined);
  assert.equal(app.completed(), true);
  assert.equal(app.callback(), expected, 'native completion callback');
  ++completions;
});
assert.equal(completions, 1);
assert.equal(app.callback(), expected, 'after normal main return');
app.main(['fail'], error => {
  assert.ok(error, 'the deliberate main failure must be delivered');
  assert.equal(app.callback(), expected, 'after exceptional main return');
  ++completions;
});
assert.equal(completions, 2);
console.log(`synchronous main and native callbacks passed (rejected access mask: ${expected})`);
