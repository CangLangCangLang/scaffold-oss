/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_APP_TITLE: string
  readonly VITE_APP_BASE_API: string
  readonly VITE_APP_PUBLIC_PATH: string
  readonly VITE_BACKEND_ORIGIN: string
  readonly VITE_DEV_SERVER_PORT: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}

// bpmn-js 没有官方的 TS 类型声明，仅做最小 shim 以便 import 不报错
declare module 'bpmn-js/lib/Modeler' {
  const Modeler: any
  export default Modeler
}
declare module 'bpmn-js/lib/Viewer' {
  const Viewer: any
  export default Viewer
}
declare module 'bpmn-js/lib/NavigatedViewer' {
  const NavigatedViewer: any
  export default NavigatedViewer
}

// form-create 没有官方 TS 类型；先 shim 成 any，使用面足够。
declare module '@form-create/element-ui' {
  const formCreate: any
  export default formCreate
}
declare module '@form-create/designer' {
  const formCreateDesigner: any
  export default formCreateDesigner
}
