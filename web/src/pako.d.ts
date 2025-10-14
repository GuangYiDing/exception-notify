declare module 'pako' {
  export function ungzip(data: Uint8Array, options?: { to?: 'string' | 'utf8' }): Uint8Array;
}
