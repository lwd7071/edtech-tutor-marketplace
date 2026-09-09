import '@testing-library/jest-dom';

// JSDOM throws when Ant Design measures a pseudo-element scrollbar.
// Browsers support this call, so tests can safely fall back to the element style.
const getComputedStyle = window.getComputedStyle.bind(window);
window.getComputedStyle = (element: Element) => getComputedStyle(element);

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

// Mock ResizeObserver
global.ResizeObserver = class ResizeObserver {
  observe() {}
  unobserve() {}
  disconnect() {}
};

Object.defineProperty(HTMLCanvasElement.prototype, 'getContext', {
  value: jest.fn(() => ({
    scale: jest.fn(),
    fill: jest.fn(),
    fillRect: jest.fn(),
    drawImage: jest.fn(),
  })),
});

// Mock MessageChannel
global.MessageChannel = class MessageChannel {
  port1 = {
    onmessage: null,
  } as any;
  port2 = {
    postMessage: jest.fn(),
  } as any;
} as any;

// Mock @ant-design/icons
jest.mock('@ant-design/icons', () => {
  return new Proxy({}, {
    get: function(target, prop) {
      if (prop === '__esModule') return true;
      return function() { return null; };
    }
  });
});
