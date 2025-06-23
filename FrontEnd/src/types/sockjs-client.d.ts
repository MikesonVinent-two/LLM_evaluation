declare module 'sockjs-client' {
  class SockJS {
    constructor(url: string, _reserved?: any, options?: any);
    close(code?: number, reason?: string): void;
    send(data: string): void;
    onopen: (() => void) | null;
    onclose: ((e: { code: number; reason: string; wasClean: boolean }) => void) | null;
    onmessage: ((e: { data: string }) => void) | null;
    onerror: ((e: any) => void) | null;
  }
  export default SockJS;
}
