export interface AcademicTitle {
  transactionId: string
  academicProgram: string
  degree: string
  institutionName: string
  graduationDate: string
  emissionDate: string
  issuerSignature: string
}

export interface BlockSummary {
  id: number
  timestamp: number
  previousHash: string
  hash: string
  transactionCount: number
}

export interface BlockDetails extends BlockSummary {
  nonce: number
  transactions: AcademicTitle[]
}

export interface TitleDetails {
  title: AcademicTitle
  block: BlockSummary
}

export interface ChainStatus {
  nodeConnected: boolean
  blockCount: number
  message: string
}

export interface NodeStatus {
  id: string
  name: string
  host: string
  port: number
  connected: boolean
  blockCount: number
  lastBlockHash: string | null
  chainValid: boolean
  message: string
}

export interface NodesStatus {
  miningNodeId: string
  chainsSynchronized: boolean
  message: string
  nodes: NodeStatus[]
}

export interface ChainValidation {
  valid: boolean
  blockCount: number
  message: string
}

export interface PendingTitleResponse {
  title: AcademicTitle
  pendingCount: number
  message: string
}

export interface MinedBlockResponse {
  block: BlockSummary
  titleCount: number
  message: string
}

export interface CreateTitleRequest {
  transactionId: string
  fullName: string
  dui: string
  academicProgram: string
  degree: string
  institutionName: string
  graduationDate: string
  emissionDate: string
  issuerSignature: string
}
