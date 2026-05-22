export function createLocalId(prefix = "local") {
  const timestamp = Date.now();
  const random = Math.random().toString(36).slice(2, 10);
  return `${prefix}_${timestamp}_${random}`;
}
