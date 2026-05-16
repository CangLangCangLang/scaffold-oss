import request from '@/utils/request'
import type { ApiResult } from '@/types/api'

export interface ServerCpu {
  cpuNum?: number
  total?: number
  sys?: number
  used?: number
  wait?: number
  free?: number
}

export interface ServerMem {
  total?: number
  used?: number
  free?: number
  usage?: number
}

export interface ServerJvm {
  total?: number
  max?: number
  free?: number
  version?: string
  home?: string
  name?: string
  startTime?: string
  runTime?: string
  inputArgs?: string
  used?: number
  usage?: number
}

export interface ServerSysInfo {
  computerIp?: string
  computerName?: string
  osName?: string
  osArch?: string
  userDir?: string
}

export interface SysFileInfo {
  dirName?: string
  sysTypeName?: string
  typeName?: string
  total?: string
  free?: string
  used?: string
  usage?: number
}

export interface ServerInfo {
  cpu?: ServerCpu
  mem?: ServerMem
  jvm?: ServerJvm
  sys?: ServerSysInfo
  sysFiles?: SysFileInfo[]
}

export const getServer = () =>
  request.get<ApiResult<ServerInfo>, ApiResult<ServerInfo>>('/monitor/server')
