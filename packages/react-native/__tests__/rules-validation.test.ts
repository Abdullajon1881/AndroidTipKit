import { Rules } from '../src/rules';

// Mirrors the `require(...)` validation in Kotlin NudgeKit's rule constructors.
describe('Rules builder validation', () => {
  test('maxDisplayCount must be positive', () => {
    expect(() => Rules.maxDisplayCount(0)).toThrow();
  });
  test('afterEvent count must be positive', () => {
    expect(() => Rules.afterEvent('e', 0)).toThrow();
  });
  test('afterEvent eventName must not be blank', () => {
    expect(() => Rules.afterEvent('   ', 1)).toThrow();
  });
  test('afterScreenVisits screenName must not be blank', () => {
    expect(() => Rules.afterScreenVisits('', 1)).toThrow();
  });
  test('minIntervalHours must be positive', () => {
    expect(() => Rules.minIntervalHours(0)).toThrow();
  });
  test('expiresAfter duration must be positive', () => {
    expect(() => Rules.expiresAfter(0)).toThrow();
  });
  test('anyOf must not be empty', () => {
    expect(() => Rules.anyOf([])).toThrow();
  });
  test('allOf must not be empty', () => {
    expect(() => Rules.allOf([])).toThrow();
  });
  test('valid rules build the expected shape', () => {
    expect(Rules.once()).toEqual({ type: 'once' });
    expect(Rules.maxDisplayCount(3)).toEqual({ type: 'maxDisplayCount', count: 3 });
    expect(Rules.anyOf([Rules.once(), Rules.notDismissed()])).toEqual({
      type: 'anyOf',
      rules: [{ type: 'once' }, { type: 'notDismissed' }],
    });
  });
});
