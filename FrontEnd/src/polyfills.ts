// 为 sockjs-client 提供 global 对象的 polyfill
if (typeof window !== 'undefined' && !window.global) {
  (window as any).global = window;
}
