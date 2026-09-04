import '@testing-library/jest-dom';

// Mock matchMedia for Ant Design
Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: jest.fn().mockImplementation(query => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: jest.fn(), // Deprecated
    removeListener: jest.fn(), // Deprecated
    addEventListener: jest.fn(),
    removeEventListener: jest.fn(),
    dispatchEvent: jest.fn(),
  })),
});

// Polyfill MessageChannel for Ant Design Form in JSDOM
if (typeof MessageChannel === 'undefined') {
  const { MessageChannel } = require('worker_threads');
  (global as any).MessageChannel = MessageChannel;
}

// Polyfill ResizeObserver for Ant Design Table / Tabs in JSDOM
if (typeof ResizeObserver === 'undefined') {
  (global as any).ResizeObserver = class ResizeObserver {
    observe() {}
    unobserve() {}
    disconnect() {}
  };
}

// Polyfill getComputedStyle for Ant Design rc-util / pseudo elements in JSDOM
// Ngăn JSDOM ném "Not implemented: window.getComputedStyle(elt, pseudoElt)" khi rc-util đo scrollbar
if (typeof window !== 'undefined' && window.getComputedStyle) {
  const originalGetComputedStyle = window.getComputedStyle;
  window.getComputedStyle = function (elt: Element) {
    return originalGetComputedStyle.call(window, elt);
  };
}
