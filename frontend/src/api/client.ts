import type {
  AcademicTitle,
  BlockDetails,
  BlockSummary,
  ChainStatus,
  ChainValidation,
  CreateTitleRequest,
  MinedBlockResponse,
  NodesStatus,
  PendingTitleResponse,
  TitleDetails,
} from '../types'

export class ApiError extends Error {
  constructor(
    message: string,
    readonly status?: number,
  ) {
    super(message)
    this.name = 'ApiError'
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  let response: Response

  try {
    response = await fetch(`/api${path}`, {
      ...init,
      headers: {
        ...(init?.body ? { 'Content-Type': 'application/json' } : {}),
        ...init?.headers,
      },
    })
  } catch (error) {
    if (error instanceof TypeError) {
      throw new ApiError(
        'No se pudo contactar la API. Revisa que Quarkus esté ejecutándose en http://localhost:8080.',
      )
    }
    throw error
  }

  const text = await response.text()
  let body: unknown
  if (text) {
    try {
      body = JSON.parse(text)
    } catch {
      body = text
    }
  }

  if (!response.ok) {
    if (response.status === 500 && !text.trim()) {
      throw new ApiError(
        'No se pudo contactar la API. Revisa que Quarkus esté ejecutándose en http://localhost:8080.',
        response.status,
      )
    }
    const detail =
      typeof body === 'object' && body !== null && 'message' in body
        ? String(body.message)
        : typeof body === 'string'
          ? body
          : ''
    const messageByStatus: Record<number, string> = {
      404: 'El recurso solicitado no existe.',
      409: 'La solicitud entra en conflicto con el estado actual.',
      503: 'El nodo blockchain no está disponible. Verifica su conexión e inténtalo de nuevo.',
    }
    const fallback =
      messageByStatus[response.status] ??
      `La API respondió con un error (HTTP ${response.status}).`
    throw new ApiError(
      detail ? `${fallback} ${detail}` : fallback,
      response.status,
    )
  }

  return body as T
}

export const api = {
  getStatus: (nodeId: string) => request<ChainStatus>(`/chain/status?node=${encodeURIComponent(nodeId)}`),
  getNodesStatus: () => request<NodesStatus>('/chain/nodes'),
  getValidation: (nodeId: string) => request<ChainValidation>(`/chain/validation?node=${encodeURIComponent(nodeId)}`),
  getTitles: (nodeId: string) => request<AcademicTitle[]>(`/titles?node=${encodeURIComponent(nodeId)}`),
  getPendingTitles: () => request<AcademicTitle[]>('/titles/pending'),
  getBlocks: (nodeId: string) => request<BlockSummary[]>(`/blocks?node=${encodeURIComponent(nodeId)}`),
  getBlock: (hash: string, nodeId: string) =>
    request<BlockDetails>(`/blocks/${encodeURIComponent(hash)}?node=${encodeURIComponent(nodeId)}`),
  getTitle: (transactionId: string, nodeId: string) =>
    request<TitleDetails>(`/titles/${encodeURIComponent(transactionId)}?node=${encodeURIComponent(nodeId)}`),
  createTitle: (title: CreateTitleRequest) =>
    request<PendingTitleResponse>('/titles', {
      method: 'POST',
      body: JSON.stringify(title),
    }),
  mine: () =>
    request<MinedBlockResponse>('/chain/mine', {
      method: 'POST',
    }),
}
