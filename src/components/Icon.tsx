export function Icon({ name, className = '' }: { name: string; className?: string }) {
  const paths: Record<string, string> = {
    box: 'm12 3 9 5v8l-9 5-9-5V8l9-5Zm0 9 9-4M12 12 3 8m9 4v9M7 5.8l9 5v5',
    truck: 'M3 6h12v11H3V6Zm12 5h4l3 4v2h-7M5 17a2 2 0 1 0 4 0m8 0a2 2 0 1 0 4 0',
    grid: 'M3 3h7v7H3V3Zm11 0h7v7h-7V3ZM3 14h7v7H3v-7Zm11 0h7v7h-7v-7Z',
    cycle: 'M5 8a8 8 0 0 1 14-2l2 3m0-6v6h-6M19 16A8 8 0 0 1 5 18l-2-3m0 6v-6h6',
    route: 'M5 21V4m-3 3 3-3 3 3m11 14V4m-3 3 3-3 3 3M5 13c9 0 14-2 14-9',
    file: 'M6 3h8l4 4v14H6V3Zm8 0v5h4M9 12h6m-6 4h6',
    settings: 'M4 6h16M4 12h16M4 18h16M8 3v6m8 0v6m-6 0v6',
    light: 'M8 3h8v18H8V3Zm4 4h.01M12 12h.01M12 17h.01M4 6h4m8 0h4M4 12h4m8 0h4M4 18h4m8 0h4',
    warehouse: 'M3 21V7l9-4 9 4v14M7 21V11h10v10M10 21v-6h4v6',
    user: 'M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8Zm-7 9v-3c0-4 14-4 14 0v3H5Z',
    upload: 'M12 16V3m-5 5 5-5 5 5M4 15v6h16v-6',
    check: 'm5 12 4 4L19 6',
    arrow: 'M4 12h16m-6-6 6 6-6 6',
    play: 'm8 4 12 8-12 8V4Z',
    map: 'm3 5 6-2 6 2 6-2v16l-6 2-6-2-6 2V5Zm6-2v16m6-14v16',
    car: 'M4 17V9l3-5h10l3 5v8H4Zm0-7h16M6 17v3m12-3v3M7 13h1m8 0h1',
    bike: 'M8 16a4 4 0 1 0-8 0 4 4 0 0 0 8 0Zm16 0a4 4 0 1 0-8 0 4 4 0 0 0 8 0ZM4 16l5-9 5 9H4Zm16 0L15 3h4M7 7h5',
    moto: 'M8 17a4 4 0 1 0-8 0 4 4 0 0 0 8 0Zm16 0a4 4 0 1 0-8 0 4 4 0 0 0 8 0ZM4 17l5-8h6l-3 8H4Zm16 0L16 4h-4M5 8h5',
    clock: 'M12 22a10 10 0 1 0 0-20 10 10 0 0 0 0 20Zm0-16v6l4 3',
    search: 'M10 17a7 7 0 1 0 0-14 7 7 0 0 0 0 14Zm5-2 6 6',
    target: 'M12 19a7 7 0 1 0 0-14 7 7 0 0 0 0 14ZM12 2v5m0 10v5M2 12h5m10 0h5',
    close: 'm6 6 12 12M6 18 18 6',
  }
  return <svg className={`icon ${className}`} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><path d={paths[name] || paths.box} /></svg>
}
