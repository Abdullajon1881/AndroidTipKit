/**
 * Minimal async key/value storage abstraction for persistence.
 *
 * This shape intentionally matches `@react-native-async-storage/async-storage`,
 * so an `AsyncStorage` instance can be passed directly to
 * {@link import('./manager').createPersistentTipManager} with no adapter and no
 * hard dependency. Expo Go works out of the box with {@link MemoryStorage};
 * AsyncStorage is opt-in and never required for {@link import('./manager').MemoryTipManager}.
 */
export interface TipStorage {
  getItem(key: string): Promise<string | null>;
  setItem(key: string, value: string): Promise<void>;
  removeItem(key: string): Promise<void>;
  clear(): Promise<void>;
}

/** In-memory {@link TipStorage} for tests, previews, and Expo Go. Nothing is persisted across process restarts. */
export class MemoryStorage implements TipStorage {
  private readonly map = new Map<string, string>();

  async getItem(key: string): Promise<string | null> {
    return this.map.has(key) ? (this.map.get(key) as string) : null;
  }
  async setItem(key: string, value: string): Promise<void> {
    this.map.set(key, value);
  }
  async removeItem(key: string): Promise<void> {
    this.map.delete(key);
  }
  async clear(): Promise<void> {
    this.map.clear();
  }
}
