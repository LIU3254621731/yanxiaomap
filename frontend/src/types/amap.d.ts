declare namespace AMap {
  class Map {
    constructor(container: HTMLElement | string, opts?: MapOptions)
    destroy(): void
    addControl(control: any): void
    removeControl(control: any): void
    setZoom(zoom: number): void
    setCenter(center: LngLat | [number, number]): void
    getZoom(): number
    getCenter(): LngLat
    getBounds(): Bounds
    setFitView(overlays?: any[]): void
    add(overlay: any | any[]): void
    remove(overlay: any | any[]): void
    clearMap(): void
    on(event: string, handler: Function): void
    off(event: string, handler: Function): void
    plugin(plugins: string | string[], callback: Function): void
  }

  interface MapOptions {
    zoom?: number
    center?: LngLat | [number, number]
    layers?: any[]
    viewMode?: '2D' | '3D'
    pitch?: number
    rotation?: number
    mapStyle?: string
    features?: string[]
    resizeEnable?: boolean
    showIndoorMap?: boolean
    expandZoomRange?: boolean
    zooms?: [number, number]
    dragEnable?: boolean
    keyboardEnable?: boolean
    doubleClickZoom?: boolean
    scrollWheel?: boolean
    touchZoom?: boolean
    animateEnable?: boolean
  }

  class LngLat {
    constructor(lng: number, lat: number)
    lng: number
    lat: number
    offset(w: number, s: number): LngLat
    distance(lnglat: LngLat): number
  }

  class Pixel {
    constructor(x: number, y: number)
    x: number
    y: number
  }

  class Bounds {
    constructor(southWest: LngLat, northEast: LngLat)
    contains(lnglat: LngLat): boolean
    getCenter(): LngLat
    getSouthWest(): LngLat
    getNorthEast(): LngLat
  }

  class Marker {
    constructor(opts?: MarkerOptions)
    setPosition(position: LngLat | [number, number]): void
    getPosition(): LngLat
    setContent(content: string | HTMLElement): void
    getContent(): string | HTMLElement
    setOffset(offset: Pixel): void
    setLabel(label: any): void
    setIcon(icon: any): void
    setzIndex(index: number): void
    setAngle(angle: number): void
    hide(): void
    show(): void
    setMap(map: Map | null): void
    on(event: string, handler: Function): void
    off(event: string, handler: Function): void
  }

  interface MarkerOptions {
    position?: LngLat | [number, number]
    offset?: Pixel
    icon?: string | any
    content?: string | HTMLElement
    label?: any
    title?: string
    zIndex?: number
    angle?: number
    draggable?: boolean
    visible?: boolean
    cursor?: string
    bubble?: boolean
    extData?: any
  }

  class InfoWindow {
    constructor(opts?: InfoWindowOptions)
    open(map: Map, pos: LngLat | [number, number]): void
    close(): void
    setContent(content: string | HTMLElement): void
    getContent(): string | HTMLElement
    setPosition(position: LngLat | [number, number]): void
    getPosition(): LngLat
  }

  interface InfoWindowOptions {
    content?: string | HTMLElement
    size?: any
    offset?: Pixel
    position?: LngLat | [number, number]
    autoMove?: boolean
    closeWhenClickMap?: boolean
    isCustom?: boolean
    showShadow?: boolean
  }

  class ToolBar {
    constructor(opts?: ToolBarOptions)
  }

  interface ToolBarOptions {
    position?: string
    offset?: Pixel
    direction?: boolean
    locate?: boolean
    rvoType?: number
    noState?: boolean
  }

  class Scale {
    constructor(opts?: ScaleOptions)
  }

  interface ScaleOptions {
    position?: string
    offset?: Pixel
    visible?: boolean
  }

  class Geolocation {
    constructor(opts?: GeolocationOptions)
    getCurrentPosition(callback: (status: string, result: any) => void): void
  }

  interface GeolocationOptions {
    enableHighAccuracy?: boolean
    timeout?: number
    zoomToAccuracy?: boolean
    buttonPosition?: string
    showButton?: boolean
    showCircle?: boolean
    showMarker?: boolean
  }

  class Autocomplete {
    constructor(opts?: AutocompleteOptions)
    search(keyword: string, callback: (status: string, result: any) => void): void
    on(event: string, handler: Function): void
  }

  interface AutocompleteOptions {
    city?: string
    citylimit?: boolean
    input?: string
    type?: string
  }

  class PlaceSearch {
    constructor(opts?: PlaceSearchOptions)
    search(keyword: string, callback: (status: string, result: any) => void): void
    searchInBounds(keyword: string, bounds: Bounds, callback: (status: string, result: any) => void): void
  }

  interface PlaceSearchOptions {
    city?: string
    type?: string
    pageSize?: number
    pageIndex?: number
  }

  class Polyline {
    constructor(opts?: PolylineOptions)
    setPath(path: LngLat[]): void
    setMap(map: Map | null): void
  }

  interface PolylineOptions {
    path?: LngLat[]
    strokeColor?: string
    strokeWeight?: number
    strokeOpacity?: number
    strokeStyle?: string
    lineJoin?: string
    lineCap?: string
    zIndex?: number
    bubble?: boolean
  }

  class Circle {
    constructor(opts?: CircleOptions)
    setCenter(center: LngLat | [number, number]): void
    setRadius(radius: number): void
    setMap(map: Map | null): void
  }

  interface CircleOptions {
    center?: LngLat | [number, number]
    radius?: number
    strokeColor?: string
    strokeWeight?: number
    strokeOpacity?: number
    fillColor?: string
    fillOpacity?: number
    zIndex?: number
    bubble?: boolean
  }
}

interface Window {
  AMap: typeof AMap
  _AMapSecurityConfig: {
    securityJsCode: string
  }
}
