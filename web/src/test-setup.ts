// jsdom 未实现 matchMedia，而 antd 响应式组件（Grid 断点等）依赖它；按 AntD 测试文档提供最小 polyfill。
// jsdom does not implement matchMedia, but antd's responsive components (Grid breakpoints, etc.) depend on it.
Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: (query: string): MediaQueryList => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: () => {},
    removeListener: () => {},
    addEventListener: () => {},
    removeEventListener: () => {},
    dispatchEvent: () => false,
  }),
})

// jsdom 未实现 ResizeObserver，而 rc-trigger（Dropdown / Select 等弹层）靠它测量内容；缺失会直接抛 ReferenceError。
// jsdom does not implement ResizeObserver, which rc-trigger (Dropdown / Select, etc.) uses to measure popups.
class ResizeObserverStub {
  observe(): void {}
  unobserve(): void {}
  disconnect(): void {}
}

Object.defineProperty(window, 'ResizeObserver', {
  writable: true,
  value: ResizeObserverStub,
})
