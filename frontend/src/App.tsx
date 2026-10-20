import { useCallback, useEffect, useMemo, useState } from 'react'
import { ApiError, api } from './api/client'
import type {
  AcademicTitle,
  BlockDetails,
  BlockSummary,
  ChainStatus,
  ChainValidation,
  CreateTitleRequest,
  NodesStatus,
  NodeStatus,
  TitleDetails,
} from './types'

type Page = 'dashboard' | 'titles' | 'pending' | 'blocks' | 'register'
type IconName = 'grid' | 'cap' | 'clock' | 'blocks' | 'plus' | 'search' | 'refresh' | 'arrow' | 'check' | 'alert' | 'close' | 'copy' | 'shield'

const navigation: { id: Page; label: string; icon: IconName }[] = [
  { id: 'dashboard', label: 'Resumen', icon: 'grid' },
  { id: 'titles', label: 'Títulos confirmados', icon: 'cap' },
  { id: 'pending', label: 'Pendientes', icon: 'clock' },
  { id: 'blocks', label: 'Bloques', icon: 'blocks' },
  { id: 'register', label: 'Registrar título', icon: 'plus' },
]

function Icon({ name, className = 'h-5 w-5' }: { name: IconName; className?: string }) {
  const paths: Record<IconName, React.ReactNode> = {
    grid: <><rect x="3" y="3" width="7" height="7" rx="1.5" /><rect x="14" y="3" width="7" height="7" rx="1.5" /><rect x="3" y="14" width="7" height="7" rx="1.5" /><rect x="14" y="14" width="7" height="7" rx="1.5" /></>,
    cap: <><path d="m2 10 10-5 10 5-10 5-10-5Z" /><path d="M6 12v5c3.5 2.7 8.5 2.7 12 0v-5M22 10v6" /></>,
    clock: <><circle cx="12" cy="12" r="9" /><path d="M12 7v5l3 2" /></>,
    blocks: <><path d="m12 3 9 5-9 5-9-5 9-5Z" /><path d="m3 12 9 5 9-5M3 16l9 5 9-5" /></>,
    plus: <><path d="M12 5v14M5 12h14" /></>,
    search: <><circle cx="11" cy="11" r="7" /><path d="m20 20-4-4" /></>,
    refresh: <><path d="M20 7v5h-5M4 17v-5h5" /><path d="M5.6 9A7 7 0 0 1 18 6l2 6M4 12l2 6a7 7 0 0 0 12.4-3" /></>,
    arrow: <><path d="M5 12h14M13 6l6 6-6 6" /></>,
    check: <><path d="m5 12 4 4L19 6" /></>,
    alert: <><path d="M12 3 2.5 20h19L12 3Z" /><path d="M12 9v4M12 17h.01" /></>,
    close: <><path d="m18 6-12 12M6 6l12 12" /></>,
    copy: <><rect x="8" y="8" width="12" height="12" rx="2" /><path d="M16 8V5a2 2 0 0 0-2-2H5a2 2 0 0 0-2 2v9a2 2 0 0 0 2 2h3" /></>,
    shield: <><path d="M12 22s8-4 8-11V5l-8-3-8 3v6c0 7 8 11 8 11Z" /><path d="m9 12 2 2 4-4" /></>,
  }

  return (
    <svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      {paths[name]}
    </svg>
  )
}

function formatDate(value: string | number) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return 'Fecha no disponible'
  return new Intl.DateTimeFormat('es-SV', { day: 'numeric', month: 'short', year: 'numeric' }).format(date)
}

function shortHash(value: string) {
  if (value.length <= 22) return value
  return `${value.slice(0, 10)}…${value.slice(-8)}`
}

function formatApiError(error: unknown) {
  if (error instanceof ApiError) return error.message
  if (error instanceof Error) return error.message
  return 'Ocurrió un error inesperado al comunicarse con la API.'
}

function App() {
  const [page, setPage] = useState<Page>('dashboard')
  const [status, setStatus] = useState<ChainStatus | null>(null)
  const [nodesStatus, setNodesStatus] = useState<NodesStatus | null>(null)
  const [selectedNodeId, setSelectedNodeId] = useState('node1')
  const [validation, setValidation] = useState<ChainValidation | null>(null)
  const [titles, setTitles] = useState<AcademicTitle[] | null>(null)
  const [pending, setPending] = useState<AcademicTitle[] | null>(null)
  const [blocks, setBlocks] = useState<BlockSummary[] | null>(null)
  const [refreshing, setRefreshing] = useState(false)
  const [errors, setErrors] = useState<string[]>([])
  const [notice, setNotice] = useState('')
  const [mining, setMining] = useState(false)
  const [detail, setDetail] = useState<BlockDetails | TitleDetails | null>(null)
  const [detailLoading, setDetailLoading] = useState(false)
  const [detailError, setDetailError] = useState('')
  const selectNode = useCallback((nodeId: string) => {
    setSelectedNodeId(nodeId)
    setStatus(null)
    setValidation(null)
    setTitles(null)
    setBlocks(null)
    setDetail(null)
    setDetailError('')
  }, [])

  const refreshData = useCallback(async () => {
    setRefreshing(true)
    const results = await Promise.allSettled([
      api.getStatus(selectedNodeId),
      api.getNodesStatus(),
      api.getValidation(selectedNodeId),
      api.getTitles(selectedNodeId),
      api.getPendingTitles(),
      api.getBlocks(selectedNodeId),
    ] as const)
    const nextErrors: string[] = []
    const [statusResult, nodesStatusResult, validationResult, titlesResult, pendingResult, blocksResult] = results

    if (statusResult.status === 'fulfilled') setStatus(statusResult.value)
    else {
      setStatus(null)
      nextErrors.push(`Estado del nodo seleccionado: ${formatApiError(statusResult.reason)}`)
    }
    if (nodesStatusResult.status === 'fulfilled') setNodesStatus(nodesStatusResult.value)
    else {
      setNodesStatus(null)
      nextErrors.push(`Estado de los peers: ${formatApiError(nodesStatusResult.reason)}`)
    }
    if (validationResult.status === 'fulfilled') setValidation(validationResult.value)
    else nextErrors.push(`Validación de cadena: ${formatApiError(validationResult.reason)}`)
    if (titlesResult.status === 'fulfilled') setTitles(titlesResult.value)
    else nextErrors.push(`Títulos confirmados: ${formatApiError(titlesResult.reason)}`)
    if (pendingResult.status === 'fulfilled') setPending(pendingResult.value)
    else nextErrors.push(`Títulos pendientes: ${formatApiError(pendingResult.reason)}`)
    if (blocksResult.status === 'fulfilled') setBlocks(blocksResult.value)
    else nextErrors.push(`Bloques: ${formatApiError(blocksResult.reason)}`)

    setErrors(nextErrors)
    setRefreshing(false)
  }, [selectedNodeId])

  useEffect(() => {
    void refreshData()
  }, [refreshData])

  const onMine = async () => {
    setMining(true)
    setNotice('')
    setErrors([])
    try {
      const result = await api.mine()
      const primaryName = nodesStatus?.nodes.find((node) => node.id === nodesStatus.miningNodeId)?.name ?? 'el nodo principal'
      setNotice(`${result.message} Se incluyeron ${result.titleCount} título${result.titleCount === 1 ? '' : 's'} en el bloque #${result.block.id} de ${primaryName}.`)
      await refreshData()
    } catch (error) {
      setErrors([formatApiError(error)])
    } finally {
      setMining(false)
    }
  }

  const openBlock = async (hash: string) => {
    setDetailLoading(true)
    setDetailError('')
    setDetail(null)
    try {
      setDetail(await api.getBlock(hash, selectedNodeId))
    } catch (error) {
      setDetailError(formatApiError(error))
    } finally {
      setDetailLoading(false)
    }
  }

  const openTitle = async (transactionId: string) => {
    setDetailLoading(true)
    setDetailError('')
    setDetail(null)
    try {
      setDetail(await api.getTitle(transactionId, selectedNodeId))
    } catch (error) {
      setDetailError(formatApiError(error))
    } finally {
      setDetailLoading(false)
    }
  }

  const dismissNotice = () => setNotice('')
  const counts = {
    confirmed: titles?.length,
    pending: pending?.length,
    blocks: status?.blockCount ?? blocks?.length,
  }
  const selectedNode = nodesStatus?.nodes.find((node) => node.id === selectedNodeId) ?? null
  const miningNodeName = nodesStatus?.nodes.find((node) => node.id === nodesStatus.miningNodeId)?.name ?? 'Nodo 1'
  const pageTitle = navigation.find((item) => item.id === page)?.label ?? 'Resumen'

  return (
    <div className="min-h-screen bg-canvas">
      <aside className="fixed inset-y-0 left-0 z-20 hidden w-[250px] flex-col border-r border-slate-200/80 bg-white lg:flex">
        <Brand />
        <div className="px-4 pt-8">
          <p className="px-3 text-[10px] font-bold uppercase tracking-[0.16em] text-slate-400">Espacio de trabajo</p>
          <nav className="mt-3 space-y-1" aria-label="Navegación principal">
            {navigation.map((item) => (
              <NavButton key={item.id} {...item} active={page === item.id} onClick={() => setPage(item.id)} />
            ))}
          </nav>
        </div>
        <div className="mt-auto p-4">
          <div className="rounded-2xl bg-slate-50 p-4">
            <div className="flex items-center gap-2 text-sm font-semibold text-slate-700">
              <Icon name="shield" className="h-4 w-4 text-brand" />
              Red académica
            </div>
            <p className="mt-2 text-xs leading-5 text-slate-500">Los títulos confirmados pueden verificarse en la cadena.</p>
          </div>
          <p className="mt-5 px-1 text-[11px] text-slate-400">CertiChain · Wallet académica</p>
        </div>
      </aside>

      <div className="lg:pl-[250px]">
        <header className="sticky top-0 z-10 border-b border-slate-200/80 bg-white/90 backdrop-blur">
          <div className="mx-auto flex h-[72px] max-w-[1500px] items-center justify-between gap-4 px-4 sm:px-7">
            <div className="flex min-w-0 items-center gap-3">
              <div className="lg:hidden"><Brand compact /></div>
              <div className="hidden h-8 w-px bg-slate-200 sm:block lg:hidden" />
              <div>
                <p className="text-xs text-slate-400">Wallet académica</p>
                <h1 className="truncate text-sm font-bold text-ink sm:text-base">{pageTitle}</h1>
              </div>
            </div>
            <div className="flex items-center gap-2 sm:gap-4">
              <NodeSelector
                nodes={nodesStatus?.nodes ?? []}
                selectedNodeId={selectedNodeId}
                onSelect={selectNode}
                loading={refreshing}
              />
              <ConnectionBadge
                status={status}
                nodeName={selectedNode?.name ?? 'Nodo'}
                synchronized={nodesStatus?.chainsSynchronized ?? null}
              />
              <button
                className="inline-flex h-10 w-10 items-center justify-center rounded-xl border border-slate-200 bg-white text-slate-500 transition hover:border-brand/30 hover:text-brand disabled:opacity-50 sm:w-auto sm:gap-2 sm:px-3"
                type="button"
                onClick={() => void refreshData()}
                disabled={refreshing}
                aria-label="Actualizar datos"
                title="Actualizar datos"
              >
                <Icon name="refresh" className={`h-[18px] w-[18px] ${refreshing ? 'animate-spin' : ''}`} />
                <span className="hidden text-sm font-semibold sm:inline">{refreshing ? 'Actualizando' : 'Actualizar'}</span>
              </button>
            </div>
          </div>
          <nav className="flex gap-1 overflow-x-auto border-t border-slate-100 px-3 py-2 lg:hidden" aria-label="Navegación principal">
            {navigation.map((item) => (
              <button
                key={item.id}
                className={`flex shrink-0 items-center gap-2 rounded-lg px-3 py-2 text-xs font-semibold ${page === item.id ? 'bg-brand/10 text-brand' : 'text-slate-500 hover:bg-slate-50'}`}
                type="button"
                onClick={() => setPage(item.id)}
              >
                <Icon name={item.icon} className="h-4 w-4" />{item.label}
              </button>
            ))}
          </nav>
        </header>

        <main className="mx-auto max-w-[1500px] px-4 py-6 sm:px-7 sm:py-8">
          {errors.length > 0 && <ErrorBanner errors={errors} onDismiss={() => setErrors([])} />}
          {notice && <SuccessBanner message={notice} onDismiss={dismissNotice} />}
          <NodeOverview
            nodesStatus={nodesStatus}
            selectedNodeId={selectedNodeId}
            refreshing={refreshing}
            onSelectNode={selectNode}
          />

          {page === 'dashboard' && (
            <Dashboard
              status={status}
              validation={validation}
              nodesStatus={nodesStatus}
              selectedNode={selectedNode}
              counts={counts}
              blocks={blocks}
              pending={pending}
              refreshing={refreshing}
              onNavigate={setPage}
              onMine={onMine}
              mining={mining}
              onOpenBlock={openBlock}
            />
          )}
          {page === 'titles' && <TitlesPage titles={titles} selectedNode={selectedNode} onOpenTitle={openTitle} />}
          {page === 'pending' && <PendingPage pending={pending} onMine={onMine} mining={mining} miningNodeName={miningNodeName} />}
          {page === 'blocks' && <BlocksPage blocks={blocks} selectedNode={selectedNode} onOpenBlock={openBlock} />}
          {page === 'register' && (
            <RegisterPage
              onCreated={(message) => {
                setNotice(message)
                setPage('pending')
                void refreshData()
              }}
              onError={(message) => setErrors([message])}
              miningNodeName={miningNodeName}
            />
          )}
        </main>
      </div>

      {(detailLoading || detail || detailError) && (
        <DetailModal
          loading={detailLoading}
          error={detailError}
          detail={detail}
          onClose={() => {
            setDetail(null)
            setDetailError('')
          }}
          onOpenTitle={openTitle}
          onOpenBlock={openBlock}
        />
      )}
    </div>
  )
}

function Brand({ compact = false }: { compact?: boolean }) {
  return (
    <div className={`flex items-center gap-3 ${compact ? '' : 'border-b border-slate-100 px-6 py-5'}`}>
      <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-navy text-white shadow-sm">
        <Icon name="cap" className="h-5 w-5" />
      </div>
      {!compact && <div><p className="text-[15px] font-extrabold tracking-tight text-ink">CertiChain</p><p className="text-[11px] text-slate-400">Wallet académica</p></div>}
    </div>
  )
}

function NavButton({ label, icon, active, onClick }: { label: string; icon: IconName; active: boolean; onClick: () => void }) {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-current={active ? 'page' : undefined}
      className={`flex w-full items-center gap-3 rounded-xl px-3 py-[11px] text-sm font-semibold transition ${active ? 'bg-brand/10 text-brand' : 'text-slate-500 hover:bg-slate-50 hover:text-slate-800'}`}
    >
      <Icon name={icon} className="h-[18px] w-[18px]" />
      <span className="flex-1 text-left">{label}</span>
    </button>
  )
}

function NodeSelector({
  nodes, selectedNodeId, onSelect, loading,
}: {
  nodes: NodeStatus[]
  selectedNodeId: string
  onSelect: (nodeId: string) => void
  loading: boolean
}) {
  return (
    <label className="flex h-10 items-center gap-2 rounded-xl border border-slate-200 bg-white px-2.5 text-xs font-semibold text-slate-600">
      <span className="hidden text-slate-400 sm:inline">Viendo</span>
      <select
        className="max-w-[105px] bg-transparent text-xs font-bold text-ink outline-none sm:max-w-[145px]"
        value={selectedNodeId}
        onChange={(event) => onSelect(event.target.value)}
        disabled={!nodes.length || loading}
        aria-label="Seleccionar nodo que se está consultando"
      >
        {nodes.length ? nodes.map((node) => <option key={node.id} value={node.id}>{node.name}</option>) : <option value={selectedNodeId}>{loading ? 'Consultando…' : 'Sin nodos'}</option>}
      </select>
    </label>
  )
}

function ConnectionBadge({ status, nodeName, synchronized }: { status: ChainStatus | null; nodeName: string; synchronized: boolean | null }) {
  const connected = status?.nodeConnected
  const classes = connected === true
    ? 'bg-emerald-50 text-emerald-700'
    : connected === false
      ? 'bg-rose-50 text-rose-700'
      : 'bg-slate-100 text-slate-500'
  const label = connected === true ? `${nodeName} conectado` : connected === false ? `${nodeName} desconectado` : 'Consultando nodo'
  const syncLabel = synchronized === true ? 'Sincronizados' : synchronized === false ? 'Desfase' : 'Verificando'
  return (
    <>
      <div className={`inline-flex items-center gap-2 rounded-full px-2.5 py-2 text-xs font-semibold sm:px-3 ${classes}`} title={status?.message ?? 'Consultando el nodo seleccionado'}>
        <span className={`h-2 w-2 rounded-full ${connected === true ? 'bg-emerald-500' : connected === false ? 'bg-rose-500' : 'bg-slate-400'}`} />
        <span className="hidden sm:inline">{label}</span>
        <span className="sm:hidden">{connected === true ? nodeName.replace('Nodo ', 'N') : connected === false ? 'Desconectado' : 'Consultando'}</span>
      </div>
      <div className={`hidden items-center gap-1.5 rounded-full px-2.5 py-2 text-xs font-semibold md:inline-flex ${synchronized === true ? 'bg-emerald-50 text-emerald-700' : synchronized === false ? 'bg-amber-50 text-amber-800' : 'bg-slate-100 text-slate-500'}`} title={synchronized === true ? 'Ambas cadenas válidas coinciden en altura y hash final' : synchronized === false ? 'Uno de los nodos está desconectado o las cadenas son distintas' : 'Consultando ambos nodos'}>
        <span className={`h-1.5 w-1.5 rounded-full ${synchronized === true ? 'bg-emerald-500' : synchronized === false ? 'bg-amber-500' : 'bg-slate-400'}`} />
        {syncLabel}
      </div>
    </>
  )
}

function NodeOverview({
  nodesStatus, selectedNodeId, refreshing, onSelectNode,
}: {
  nodesStatus: NodesStatus | null
  selectedNodeId: string
  refreshing: boolean
  onSelectNode: (nodeId: string) => void
}) {
  const synchronized = nodesStatus?.chainsSynchronized
  const summary = synchronized === true
    ? 'Ambos nodos están conectados y comparten una cadena válida con el mismo hash final.'
    : nodesStatus
      ? nodesStatus.message
      : refreshing
        ? 'Consultando ambos nodos…'
        : 'No se pudo consultar el estado de los nodos.'

  return (
    <section className="mb-6 overflow-hidden rounded-2xl border border-slate-200/80 bg-white shadow-card" aria-label="Estado de sincronización de nodos">
      <div className={`flex flex-wrap items-center justify-between gap-3 border-b px-5 py-3 ${synchronized === true ? 'border-emerald-100 bg-emerald-50/70' : synchronized === false ? 'border-amber-100 bg-amber-50/70' : 'border-slate-100 bg-slate-50'}`}>
        <div className="flex items-center gap-2.5">
          <span className={`flex h-8 w-8 items-center justify-center rounded-lg ${synchronized === true ? 'bg-emerald-100 text-emerald-700' : synchronized === false ? 'bg-amber-100 text-amber-700' : 'bg-slate-200 text-slate-500'}`}><Icon name={synchronized === true ? 'check' : synchronized === false ? 'alert' : 'refresh'} className={`h-4 w-4 ${refreshing ? 'animate-spin' : ''}`} /></span>
          <div><p className={`text-sm font-bold ${synchronized === true ? 'text-emerald-800' : synchronized === false ? 'text-amber-900' : 'text-slate-700'}`}>{synchronized === true ? 'Nodos sincronizados' : synchronized === false ? 'Nodos aún no sincronizados' : 'Verificando sincronización'}</p><p className="mt-0.5 text-xs text-slate-500">{summary}</p></div>
        </div>
        {nodesStatus && <p className="text-[11px] font-medium text-slate-500">La confirmación compara altura, hash final y validez de ambas cadenas.</p>}
      </div>
      <div className="grid gap-px bg-slate-100 sm:grid-cols-2">
        {nodesStatus?.nodes.map((node) => {
          const selected = node.id === selectedNodeId
          return (
            <button
              key={node.id}
              type="button"
              onClick={() => onSelectNode(node.id)}
              disabled={refreshing}
              className={`flex min-w-0 items-start gap-3 bg-white px-5 py-4 text-left transition hover:bg-blue-50/40 ${selected ? 'ring-2 ring-inset ring-brand/40' : ''}`}
              aria-pressed={selected}
            >
              <span className={`mt-0.5 h-2.5 w-2.5 shrink-0 rounded-full ${node.connected ? node.chainValid ? 'bg-emerald-500' : 'bg-amber-500' : 'bg-rose-500'}`} />
              <span className="min-w-0 flex-1">
                <span className="flex flex-wrap items-center gap-2"><span className="text-sm font-bold text-ink">{node.name}</span><span className={`rounded-full px-2 py-0.5 text-[10px] font-bold ${node.connected ? node.chainValid ? 'bg-emerald-50 text-emerald-700' : 'bg-amber-50 text-amber-700' : 'bg-rose-50 text-rose-700'}`}>{node.connected ? node.chainValid ? 'Conectado · válido' : 'Conectado · inválido' : 'Desconectado'}</span>{selected && <span className="rounded-full bg-brand/10 px-2 py-0.5 text-[10px] font-bold text-brand">Consultando</span>}</span>
                <span className="mt-1 block truncate text-xs text-slate-500">{node.connected ? `${node.blockCount} bloques · ${node.lastBlockHash ? `hash ${shortHash(node.lastBlockHash)}` : 'sin hash de bloque'}` : node.message}</span>
                <span className="mt-1 block text-[10px] text-slate-400">{node.host}:{node.port}</span>
              </span>
            </button>
          )
        })}
        {!nodesStatus && <div className="col-span-full px-5 py-4 text-xs text-slate-400">{refreshing ? 'Consultando nodo 1 y nodo 2…' : 'No hay resultados de consulta. Actualiza para reintentar.'}</div>}
      </div>
    </section>
  )
}

function ErrorBanner({ errors, onDismiss }: { errors: string[]; onDismiss: () => void }) {
  return (
    <div role="alert" className="mb-6 flex items-start justify-between gap-4 rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-800">
      <div className="flex gap-3"><Icon name="alert" className="mt-0.5 h-5 w-5 shrink-0" /><div className="space-y-1">{errors.map((error, index) => <p key={`${index}-${error}`}>{error}</p>)}</div></div>
      <button type="button" className="rounded-md p-1 text-rose-500 hover:bg-rose-100" onClick={onDismiss} aria-label="Cerrar mensaje"><Icon name="close" className="h-4 w-4" /></button>
    </div>
  )
}

function SuccessBanner({ message, onDismiss }: { message: string; onDismiss: () => void }) {
  return (
    <div role="status" className="mb-6 flex items-start justify-between gap-4 rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-800">
      <div className="flex items-center gap-3"><Icon name="check" className="h-5 w-5 shrink-0" /><p>{message}</p></div>
      <button type="button" className="rounded-md p-1 text-emerald-600 hover:bg-emerald-100" onClick={onDismiss} aria-label="Cerrar confirmación"><Icon name="close" className="h-4 w-4" /></button>
    </div>
  )
}

function PageHeading({ eyebrow, title, description, action }: { eyebrow: string; title: string; description: string; action?: React.ReactNode }) {
  return (
    <div className="mb-7 flex flex-wrap items-end justify-between gap-4">
      <div><p className="mb-1 text-xs font-bold uppercase tracking-[0.14em] text-brand">{eyebrow}</p><h2 className="text-2xl font-extrabold tracking-tight text-ink sm:text-[28px]">{title}</h2><p className="mt-2 max-w-2xl text-sm leading-6 text-slate-500">{description}</p></div>
      {action}
    </div>
  )
}

function Dashboard({
  status, validation, nodesStatus, selectedNode, counts, blocks, pending, refreshing, onNavigate, onMine, mining, onOpenBlock,
}: {
  status: ChainStatus | null
  validation: ChainValidation | null
  nodesStatus: NodesStatus | null
  selectedNode: NodeStatus | null
  counts: { confirmed: number | undefined; pending: number | undefined; blocks: number | undefined }
  blocks: BlockSummary[] | null
  pending: AcademicTitle[] | null
  refreshing: boolean
  onNavigate: (page: Page) => void
  onMine: () => void
  mining: boolean
  onOpenBlock: (hash: string) => void
}) {
  return (
    <>
      <section className="relative mb-7 overflow-hidden rounded-2xl bg-navy px-6 py-7 text-white shadow-card sm:px-9 sm:py-9">
        <div className="absolute -right-12 -top-24 h-72 w-72 rounded-full border border-white/10" />
        <div className="absolute -right-1 top-8 h-44 w-44 rounded-full border border-white/10" />
        <div className="relative max-w-2xl">
          <div className="mb-3 inline-flex items-center gap-2 rounded-full bg-white/10 px-3 py-1.5 text-xs font-semibold text-blue-100"><Icon name="shield" className="h-4 w-4" /> Credenciales académicas verificables</div>
          <h2 className="text-2xl font-extrabold tracking-tight sm:text-3xl">Tus títulos, respaldados por la cadena.</h2>
          <p className="mt-3 max-w-xl text-sm leading-6 text-blue-100/80">Consulta credenciales públicas, registra nuevos títulos académicos y revisa su estado de confirmación.</p>
          <button type="button" onClick={() => onNavigate('register')} className="mt-6 inline-flex items-center gap-2 rounded-lg bg-white px-4 py-2.5 text-sm font-bold text-navy transition hover:bg-blue-50">Registrar un título <Icon name="arrow" className="h-4 w-4" /></button>
        </div>
      </section>

      <div className="mb-7 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard icon="blocks" label="Bloques en la cadena" value={counts.blocks} tone="blue" detail="Estado reportado por el nodo" />
        <StatCard icon="cap" label="Títulos confirmados" value={counts.confirmed} tone="green" detail="Registrados en la blockchain" />
        <StatCard icon="clock" label="Títulos pendientes" value={counts.pending} tone="amber" detail="En espera de ser minados" />
        <div className="rounded-2xl border border-slate-200/80 bg-white p-5 shadow-card">
          <div className="flex items-start justify-between"><div><p className="text-sm font-semibold text-slate-500">Integridad · {selectedNode?.name ?? 'nodo seleccionado'}</p><p className={`mt-3 text-lg font-extrabold ${validation?.valid ? 'text-emerald-700' : validation?.valid === false ? 'text-rose-700' : 'text-slate-400'}`}>{validation ? validation.valid ? 'Cadena válida' : 'Requiere revisión' : 'Sin datos'}</p></div><span className="flex h-10 w-10 items-center justify-center rounded-xl bg-violet-50 text-violet-600"><Icon name="shield" /></span></div>
          <p className="mt-2 truncate text-xs text-slate-400" title={validation?.message}>{validation?.message ?? 'Consulta la API para validar la cadena'}</p>
        </div>
      </div>

      <div className="grid gap-5 xl:grid-cols-[1.4fr_1fr]">
        <section className="rounded-2xl border border-slate-200/80 bg-white shadow-card">
          <div className="flex items-center justify-between border-b border-slate-100 px-5 py-4 sm:px-6"><div><h3 className="font-bold text-ink">Bloques recientes · {selectedNode?.name ?? 'nodo seleccionado'}</h3><p className="mt-1 text-xs text-slate-400">Datos consultados directamente de este nodo</p></div><button onClick={() => onNavigate('blocks')} className="text-xs font-bold text-brand hover:text-blue-700" type="button">Ver todos</button></div>
          {refreshing && !blocks ? <LoadingState /> : blocks?.length ? <div className="divide-y divide-slate-100">{blocks.slice(-4).reverse().map((block) => <BlockRow key={block.hash} block={block} onOpen={onOpenBlock} />)}</div> : <EmptyState title="Aún no hay bloques disponibles" detail="Cuando la API devuelva bloques, aparecerán aquí." />}
        </section>
        <section className="rounded-2xl border border-slate-200/80 bg-white shadow-card">
          <div className="flex items-center justify-between border-b border-slate-100 px-5 py-4 sm:px-6"><div><h3 className="font-bold text-ink">Cola de pendientes</h3><p className="mt-1 text-xs text-slate-400">Títulos en esta instancia de la API</p></div><span className="rounded-lg bg-amber-50 px-2.5 py-1 text-xs font-bold text-amber-700">{pending?.length ?? '—'}</span></div>
          {refreshing && !pending ? <LoadingState /> : pending?.length ? <div className="p-5"><p className="text-sm leading-6 text-slate-500">Hay {pending.length} título{pending.length === 1 ? '' : 's'} esperando minado. Se publicarán juntos en un único bloque.</p><p className="mt-1 text-xs text-slate-400">El minado se envía a {nodesStatus?.nodes.find((node) => node.id === nodesStatus.miningNodeId)?.name ?? 'Nodo 1'}; los peers sincronizan después.</p><button type="button" disabled={mining} onClick={onMine} className="mt-4 inline-flex items-center justify-center gap-2 rounded-lg bg-brand px-4 py-2.5 text-sm font-bold text-white hover:bg-blue-700 disabled:cursor-wait disabled:opacity-60">{mining ? 'Minando bloque…' : 'Minar títulos pendientes'} <Icon name="arrow" className="h-4 w-4" /></button><button type="button" onClick={() => onNavigate('pending')} className="ml-3 text-sm font-semibold text-slate-500 hover:text-ink">Ver cola</button></div> : <EmptyState title="No hay títulos pendientes" detail="Los nuevos registros aparecerán en esta cola." />}
        </section>
      </div>
      {status && !status.nodeConnected && <div className="mt-5 rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800"><strong>El nodo está desconectado.</strong> {status.message}</div>}
    </>
  )
}

function StatCard({ icon, label, value, tone, detail }: { icon: IconName; label: string; value?: number; tone: 'blue' | 'green' | 'amber'; detail: string }) {
  const tones = {
    blue: 'bg-blue-50 text-blue-600',
    green: 'bg-emerald-50 text-emerald-600',
    amber: 'bg-amber-50 text-amber-600',
  }
  return (
    <div className="rounded-2xl border border-slate-200/80 bg-white p-5 shadow-card">
      <div className="flex items-start justify-between"><p className="text-sm font-semibold text-slate-500">{label}</p><span className={`flex h-10 w-10 items-center justify-center rounded-xl ${tones[tone]}`}><Icon name={icon} /></span></div>
      <p className="mt-3 text-3xl font-extrabold tracking-tight text-ink">{value ?? '—'}</p><p className="mt-1 text-xs text-slate-400">{detail}</p>
    </div>
  )
}

function TitlesPage({ titles, selectedNode, onOpenTitle }: { titles: AcademicTitle[] | null; selectedNode: NodeStatus | null; onOpenTitle: (id: string) => void }) {
  const [search, setSearch] = useState('')
  const filtered = useMemo(() => {
    const query = search.trim().toLocaleLowerCase()
    return (titles ?? []).filter((title) =>
      [title.transactionId, title.institutionName, title.academicProgram].some((value) => value.toLocaleLowerCase().includes(query)),
    )
  }, [search, titles])

  return (
    <>
      <PageHeading eyebrow={`Registro público · ${selectedNode?.name ?? 'nodo seleccionado'}`} title="Títulos confirmados" description="Consulta los títulos que ya forman parte de la cadena de este nodo. Los datos personales no se muestran en esta vista." />
      <div className="mb-5 flex flex-col gap-3 rounded-2xl border border-slate-200/80 bg-white p-4 shadow-card sm:flex-row sm:items-center sm:justify-between">
        <div><p className="text-sm font-bold text-ink">{titles ? `${titles.length} títulos` : 'Cargando títulos…'}</p><p className="mt-1 text-xs text-slate-400">Busca por identificador, institución o programa</p></div>
        <label className="relative w-full sm:max-w-sm"><span className="sr-only">Buscar títulos</span><Icon name="search" className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" /><input value={search} onChange={(event) => setSearch(event.target.value)} className="w-full rounded-lg border border-slate-200 py-2.5 pl-9 pr-3 text-sm outline-none transition placeholder:text-slate-400 focus:border-brand" placeholder="Buscar títulos…" /></label>
      </div>
      <div className="overflow-hidden rounded-2xl border border-slate-200/80 bg-white shadow-card">
        {!titles ? <LoadingState /> : filtered.length ? (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[760px] text-left">
              <thead className="bg-slate-50 text-[11px] font-bold uppercase tracking-wider text-slate-400"><tr><th className="px-5 py-3">Identificador</th><th className="px-5 py-3">Programa / grado</th><th className="px-5 py-3">Institución</th><th className="px-5 py-3">Graduación</th><th className="px-5 py-3">Emisión</th><th className="px-5 py-3"></th></tr></thead>
              <tbody className="divide-y divide-slate-100">{filtered.map((title) => <tr key={title.transactionId} className="transition hover:bg-slate-50/70"><td className="px-5 py-4"><button type="button" onClick={() => onOpenTitle(title.transactionId)} className="font-mono text-xs font-bold text-brand hover:underline">{title.transactionId}</button></td><td className="px-5 py-4"><p className="text-sm font-semibold text-ink">{title.academicProgram}</p><p className="mt-1 text-xs text-slate-400">{title.degree}</p></td><td className="px-5 py-4 text-sm text-slate-600">{title.institutionName}</td><td className="px-5 py-4 text-sm text-slate-600">{formatDate(title.graduationDate)}</td><td className="px-5 py-4 text-sm text-slate-600">{formatDate(title.emissionDate)}</td><td className="px-5 py-4"><button type="button" onClick={() => onOpenTitle(title.transactionId)} className="rounded-lg p-2 text-slate-400 hover:bg-blue-50 hover:text-brand" aria-label={`Ver título ${title.transactionId}`}><Icon name="arrow" className="h-4 w-4" /></button></td></tr>)}</tbody>
            </table>
          </div>
        ) : <EmptyState title={search ? 'No hay resultados para esa búsqueda' : 'No hay títulos confirmados'} detail={search ? 'Prueba con otro identificador, institución o programa.' : 'Los títulos aparecerán aquí después de minar un bloque.'} />}
      </div>
    </>
  )
}

function PendingPage({ pending, onMine, mining, miningNodeName }: { pending: AcademicTitle[] | null; onMine: () => void; mining: boolean; miningNodeName: string }) {
  return (
    <>
      <PageHeading
        eyebrow="Cola de esta API"
        title="Títulos pendientes"
        description={`Estos títulos están en memoria en la instancia de API y se minarán en ${miningNodeName}. Si reinicias la API, la cola se borra.`}
        action={pending?.length ? <button type="button" disabled={mining} onClick={onMine} className="inline-flex items-center gap-2 rounded-lg bg-brand px-4 py-2.5 text-sm font-bold text-white hover:bg-blue-700 disabled:cursor-wait disabled:opacity-60">{mining ? 'Minando bloque…' : 'Minar todos'} <Icon name="arrow" className="h-4 w-4" /></button> : undefined}
      />
      {pending?.length ? <div className="mb-5 rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm leading-6 text-amber-900"><strong>Un solo bloque por solicitud.</strong> Al minar, todos los títulos pendientes de esta instancia se incluyen y publican juntos en un único bloque.</div> : null}
      <div className="overflow-hidden rounded-2xl border border-slate-200/80 bg-white shadow-card">
        {!pending ? <LoadingState /> : pending.length ? <div className="divide-y divide-slate-100">{pending.map((title) => <TitleCard key={title.transactionId} title={title} />)}</div> : <EmptyState title="No hay títulos pendientes" detail="Registra un nuevo título para agregarlo a la cola de minado." />}
      </div>
    </>
  )
}

function TitleCard({ title }: { title: AcademicTitle }) {
  return (
    <article className="grid gap-4 px-5 py-5 sm:grid-cols-[1.4fr_1fr_1fr] sm:items-center">
      <div><p className="font-mono text-xs font-bold text-brand">{title.transactionId}</p><h3 className="mt-1 font-bold text-ink">{title.academicProgram}</h3><p className="mt-1 text-sm text-slate-500">{title.degree}</p></div>
      <div><p className="text-xs font-semibold uppercase tracking-wide text-slate-400">Institución</p><p className="mt-1 text-sm text-slate-700">{title.institutionName}</p></div>
      <div className="flex gap-7"><div><p className="text-xs font-semibold uppercase tracking-wide text-slate-400">Graduación</p><p className="mt-1 text-sm text-slate-700">{formatDate(title.graduationDate)}</p></div><div><p className="text-xs font-semibold uppercase tracking-wide text-slate-400">Emisión</p><p className="mt-1 text-sm text-slate-700">{formatDate(title.emissionDate)}</p></div></div>
    </article>
  )
}

function BlocksPage({ blocks, selectedNode, onOpenBlock }: { blocks: BlockSummary[] | null; selectedNode: NodeStatus | null; onOpenBlock: (hash: string) => void }) {
  return (
    <>
      <PageHeading eyebrow={`Blockchain · ${selectedNode?.name ?? 'nodo seleccionado'}`} title="Bloques" description="Resúmenes consultados directamente de este nodo. Selecciona un bloque para revisar el detalle de su cadena." />
      <div className="overflow-hidden rounded-2xl border border-slate-200/80 bg-white shadow-card">
        {!blocks ? <LoadingState /> : blocks.length ? <div className="overflow-x-auto"><table className="w-full min-w-[820px] text-left"><thead className="bg-slate-50 text-[11px] font-bold uppercase tracking-wider text-slate-400"><tr><th className="px-5 py-3">Bloque</th><th className="px-5 py-3">Hash</th><th className="px-5 py-3">Bloque anterior</th><th className="px-5 py-3">Fecha</th><th className="px-5 py-3">Transacciones</th><th className="px-5 py-3"></th></tr></thead><tbody className="divide-y divide-slate-100">{[...blocks].reverse().map((block) => <tr key={block.hash} className="hover:bg-slate-50/70"><td className="px-5 py-4 font-bold text-ink">#{block.id}</td><td className="px-5 py-4"><HashValue value={block.hash} /></td><td className="px-5 py-4"><HashValue value={block.previousHash} /></td><td className="px-5 py-4 text-sm text-slate-600">{formatDate(block.timestamp)}</td><td className="px-5 py-4 text-sm text-slate-600">{block.transactionCount}</td><td className="px-5 py-4"><button type="button" onClick={() => onOpenBlock(block.hash)} className="inline-flex items-center gap-1 rounded-lg px-2 py-1.5 text-xs font-bold text-brand hover:bg-blue-50">Detalles <Icon name="arrow" className="h-3.5 w-3.5" /></button></td></tr>)}</tbody></table></div> : <EmptyState title="No hay bloques disponibles" detail="No se recibieron bloques desde la API." />}
      </div>
    </>
  )
}

function HashValue({ value }: { value: string }) {
  return <span className="font-mono text-xs text-slate-600" title={value}>{shortHash(value)}</span>
}

function BlockRow({ block, onOpen }: { block: BlockSummary; onOpen: (hash: string) => void }) {
  return (
    <button type="button" onClick={() => onOpen(block.hash)} className="flex w-full items-center gap-4 px-5 py-4 text-left transition hover:bg-slate-50 sm:px-6">
      <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-blue-50 text-brand"><Icon name="blocks" /></span>
      <span className="min-w-0 flex-1"><span className="flex items-center gap-2"><span className="text-sm font-bold text-ink">Bloque #{block.id}</span><span className="rounded-md bg-slate-100 px-1.5 py-0.5 text-[10px] font-semibold text-slate-500">{block.transactionCount} tx</span></span><span className="mt-1 block truncate font-mono text-[11px] text-slate-400">{shortHash(block.hash)}</span></span>
      <span className="hidden text-xs text-slate-400 sm:block">{formatDate(block.timestamp)}</span><Icon name="arrow" className="h-4 w-4 shrink-0 text-slate-300" />
    </button>
  )
}

function LoadingState() {
  return <div className="flex items-center justify-center gap-3 px-6 py-12 text-sm text-slate-400"><span className="h-4 w-4 animate-spin rounded-full border-2 border-slate-200 border-t-brand" />Cargando información desde la API…</div>
}

function EmptyState({ title, detail }: { title: string; detail: string }) {
  return <div className="px-6 py-12 text-center"><span className="mx-auto flex h-11 w-11 items-center justify-center rounded-xl bg-slate-50 text-slate-400"><Icon name="blocks" /></span><h3 className="mt-3 text-sm font-bold text-slate-700">{title}</h3><p className="mx-auto mt-1 max-w-sm text-xs leading-5 text-slate-400">{detail}</p></div>
}

const localToday = () => {
  const date = new Date()
  const offset = date.getTimezoneOffset()
  return new Date(date.getTime() - offset * 60_000).toISOString().slice(0, 10)
}

function newTransactionId() {
  const suffix = window.crypto.randomUUID().slice(0, 8)
  return `tx-${localToday()}-${suffix}`
}

function isValidDate(value: string) {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) return false
  const date = new Date(`${value}T00:00:00Z`)
  return !Number.isNaN(date.getTime()) && date.toISOString().slice(0, 10) === value
}

function RegisterPage({ onCreated, onError, miningNodeName }: { onCreated: (message: string) => void; onError: (message: string) => void; miningNodeName: string }) {
  const [form, setForm] = useState<CreateTitleRequest>(() => ({
    transactionId: newTransactionId(),
    fullName: '',
    dui: '',
    academicProgram: '',
    degree: '',
    institutionName: '',
    graduationDate: '',
    emissionDate: localToday(),
    issuerSignature: '',
  }))
  const [submitting, setSubmitting] = useState(false)
  const [validationError, setValidationError] = useState('')

  const update = (field: keyof CreateTitleRequest, value: string) => {
    setForm((current) => ({ ...current, [field]: value }))
    setValidationError('')
  }

  const submit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setValidationError('')
    const requiredFields: (keyof CreateTitleRequest)[] = [
      'transactionId', 'fullName', 'dui', 'academicProgram', 'degree',
      'institutionName', 'graduationDate', 'emissionDate', 'issuerSignature',
    ]
    if (requiredFields.some((field) => !form[field].trim())) {
      setValidationError('Completa todos los campos obligatorios.')
      return
    }
    if (!isValidDate(form.graduationDate) || !isValidDate(form.emissionDate)) {
      setValidationError('Introduce fechas válidas en el formato solicitado.')
      return
    }
    if (form.graduationDate > form.emissionDate) {
      setValidationError('La fecha de graduación no puede ser posterior a la fecha de emisión.')
      return
    }

    setSubmitting(true)
    try {
      const result = await api.createTitle({
        ...form,
        transactionId: form.transactionId.trim(),
        fullName: form.fullName.trim(),
        dui: form.dui.trim(),
        academicProgram: form.academicProgram.trim(),
        degree: form.degree.trim(),
        institutionName: form.institutionName.trim(),
        issuerSignature: form.issuerSignature.trim(),
      })
      onCreated(`${result.message} El título quedó pendiente de minado.`)
      setForm((current) => ({
        ...current,
        transactionId: newTransactionId(),
        fullName: '',
        dui: '',
        academicProgram: '',
        degree: '',
        institutionName: '',
        graduationDate: '',
        issuerSignature: '',
      }))
    } catch (error) {
      onError(formatApiError(error))
    } finally {
      setSubmitting(false)
    }
  }

  const inputClass = 'mt-1.5 w-full rounded-lg border border-slate-200 bg-white px-3 py-2.5 text-sm text-ink outline-none transition placeholder:text-slate-400 focus:border-brand focus:ring-2 focus:ring-brand/10'
  return (
    <>
      <PageHeading eyebrow="Nuevo registro" title="Registrar título académico" description={`Registra una credencial para añadirla a la cola de pendientes. La API la enviará a ${miningNodeName} al minar. El nombre y DUI se envían a la API para su cifrado y no aparecerán en las vistas públicas.`} />
      <form onSubmit={(event) => void submit(event)} className="mx-auto max-w-4xl rounded-2xl border border-slate-200/80 bg-white p-5 shadow-card sm:p-8">
        <div className="mb-6 flex items-start gap-3 rounded-xl bg-blue-50 px-4 py-3 text-sm leading-6 text-blue-900"><Icon name="shield" className="mt-1 h-5 w-5 shrink-0 text-brand" /><p>Los datos personales se incluyen solo en la solicitud de registro. La API cifra esos campos y no los devuelve en las respuestas públicas.</p></div>
        <div className="grid gap-x-5 gap-y-4 sm:grid-cols-2">
          <Field label="Identificador de transacción" required><div className="flex gap-2"><input className={inputClass} value={form.transactionId} onChange={(event) => update('transactionId', event.target.value)} required /><button type="button" onClick={() => update('transactionId', newTransactionId())} className="mt-1.5 shrink-0 rounded-lg border border-slate-200 px-3 text-xs font-semibold text-slate-600 hover:bg-slate-50">Generar</button></div></Field>
          <Field label="Nombre completo" required><input autoComplete="off" className={inputClass} value={form.fullName} onChange={(event) => update('fullName', event.target.value)} required /></Field>
          <Field label="DUI" required><input autoComplete="off" className={inputClass} value={form.dui} onChange={(event) => update('dui', event.target.value)} required /></Field>
          <Field label="Programa académico" required><input className={inputClass} value={form.academicProgram} onChange={(event) => update('academicProgram', event.target.value)} required /></Field>
          <Field label="Grado académico" required><input className={inputClass} value={form.degree} onChange={(event) => update('degree', event.target.value)} placeholder="Ej. Licenciatura" required /></Field>
          <Field label="Institución" required><input className={inputClass} value={form.institutionName} onChange={(event) => update('institutionName', event.target.value)} required /></Field>
          <Field label="Fecha de graduación" required><input type="date" className={inputClass} value={form.graduationDate} onChange={(event) => update('graduationDate', event.target.value)} required /></Field>
          <Field label="Fecha de emisión" required><input type="date" className={inputClass} value={form.emissionDate} onChange={(event) => update('emissionDate', event.target.value)} required /></Field>
          <Field label="Firma / identificador del emisor" required><input className={inputClass} value={form.issuerSignature} onChange={(event) => update('issuerSignature', event.target.value)} required /></Field>
        </div>
        {validationError && <p role="alert" className="mt-5 rounded-lg bg-rose-50 px-3 py-2.5 text-sm text-rose-700">{validationError}</p>}
        <div className="mt-7 flex flex-col-reverse justify-end gap-3 border-t border-slate-100 pt-5 sm:flex-row">
          <button type="button" onClick={() => setForm({ transactionId: newTransactionId(), fullName: '', dui: '', academicProgram: '', degree: '', institutionName: '', graduationDate: '', emissionDate: localToday(), issuerSignature: '' })} className="rounded-lg border border-slate-200 px-4 py-2.5 text-sm font-semibold text-slate-600 hover:bg-slate-50">Limpiar formulario</button>
          <button type="submit" disabled={submitting} className="inline-flex items-center justify-center gap-2 rounded-lg bg-brand px-5 py-2.5 text-sm font-bold text-white hover:bg-blue-700 disabled:cursor-wait disabled:opacity-60">{submitting ? 'Registrando…' : 'Registrar título'} <Icon name="arrow" className="h-4 w-4" /></button>
        </div>
      </form>
    </>
  )
}

function Field({ label, required, children }: { label: string; required: boolean; children: React.ReactNode }) {
  return <label className="block"><span className="text-xs font-bold text-slate-600">{label}{required && <span className="ml-1 text-rose-500" aria-hidden="true">*</span>}</span>{children}</label>
}

function DetailModal({
  loading, error, detail, onClose, onOpenTitle, onOpenBlock,
}: {
  loading: boolean
  error: string
  detail: BlockDetails | TitleDetails | null
  onClose: () => void
  onOpenTitle: (id: string) => void
  onOpenBlock: (hash: string) => void
}) {
  const isBlock = detail !== null && 'transactions' in detail
  const titleDetails = detail !== null && 'title' in detail && !('transactions' in detail) ? detail : null
  const title = titleDetails?.title
  const [copied, setCopied] = useState('')

  const copyValue = async (value: string) => {
    try {
      await navigator.clipboard.writeText(value)
      setCopied(value)
      window.setTimeout(() => setCopied(''), 1500)
    } catch {
      setCopied('')
    }
  }

  return (
    <div className="fixed inset-0 z-30 flex items-center justify-center bg-slate-950/40 p-3 backdrop-blur-sm sm:p-6" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose() }}>
      <section role="dialog" aria-modal="true" aria-labelledby="detail-title" className="max-h-[90vh] w-full max-w-2xl overflow-y-auto rounded-2xl bg-white shadow-2xl">
        <div className="flex items-center justify-between border-b border-slate-100 px-5 py-4 sm:px-6"><div><p className="text-[10px] font-bold uppercase tracking-[0.14em] text-brand">{isBlock ? 'Blockchain' : 'Registro público'}</p><h2 id="detail-title" className="mt-1 font-bold text-ink">{isBlock ? `Bloque #${detail.id}` : title ? 'Detalle del título' : 'Detalle'}</h2></div><button type="button" onClick={onClose} aria-label="Cerrar detalle" className="rounded-lg p-2 text-slate-400 hover:bg-slate-100 hover:text-slate-700"><Icon name="close" /></button></div>
        {loading ? <LoadingState /> : error ? <div role="alert" className="p-6 text-sm text-rose-700">{error}</div> : detail && (
          <div className="space-y-5 p-5 sm:p-6">
            {isBlock && <><DetailValue label="Hash del bloque" value={detail.hash} copied={copied} onCopy={copyValue} /><DetailValue label="Bloque anterior" value={detail.previousHash} copied={copied} onCopy={copyValue} /><div className="grid grid-cols-2 gap-4"><DetailValue label="Fecha" value={formatDate(detail.timestamp)} /><DetailValue label="Nonce" value={String(detail.nonce)} /></div><div><p className="mb-2 text-xs font-bold uppercase tracking-wide text-slate-400">Transacciones ({detail.transactions.length})</p>{detail.transactions.length ? <div className="divide-y divide-slate-100 rounded-xl border border-slate-100">{detail.transactions.map((item) => <button key={item.transactionId} type="button" onClick={() => onOpenTitle(item.transactionId)} className="flex w-full items-center justify-between gap-3 px-3 py-3 text-left hover:bg-slate-50"><span><span className="block font-mono text-xs font-bold text-brand">{item.transactionId}</span><span className="mt-1 block text-sm text-slate-600">{item.academicProgram} · {item.degree}</span></span><Icon name="arrow" className="h-4 w-4 shrink-0 text-slate-400" /></button>)}</div> : <p className="rounded-lg bg-slate-50 p-3 text-sm text-slate-500">Este bloque no contiene transacciones.</p>}</div></>}
            {title && <><div className="rounded-xl bg-blue-50 p-4"><p className="font-mono text-xs font-bold text-brand">{title.transactionId}</p><h3 className="mt-1 text-lg font-bold text-ink">{title.academicProgram}</h3><p className="text-sm text-slate-500">{title.degree}</p></div><div className="grid gap-4 sm:grid-cols-2"><DetailValue label="Institución" value={title.institutionName} /><DetailValue label="Firma del emisor" value={title.issuerSignature} /><DetailValue label="Fecha de graduación" value={formatDate(title.graduationDate)} /><DetailValue label="Fecha de emisión" value={formatDate(title.emissionDate)} /></div><button type="button" onClick={() => onOpenBlock(titleDetails.block.hash)} className="flex w-full items-center justify-between rounded-xl border border-slate-200 p-4 text-left hover:bg-slate-50"><span><span className="block text-xs font-bold uppercase tracking-wide text-slate-400">Bloque de confirmación</span><span className="mt-1 block font-mono text-xs text-slate-700">#{titleDetails.block.id} · {shortHash(titleDetails.block.hash)}</span></span><Icon name="arrow" className="h-4 w-4 text-brand" /></button></>}
          </div>
        )}
      </section>
    </div>
  )
}

function DetailValue({ label, value, copied, onCopy }: { label: string; value: string; copied?: string; onCopy?: (value: string) => void }) {
  return <div><p className="mb-1 text-[10px] font-bold uppercase tracking-[0.12em] text-slate-400">{label}</p><div className="flex items-start justify-between gap-2 rounded-lg bg-slate-50 px-3 py-2.5"><span className="break-all font-mono text-xs text-slate-700">{value}</span>{onCopy && <button type="button" onClick={() => void onCopy(value)} className="shrink-0 text-slate-400 hover:text-brand" aria-label={`Copiar ${label}`} title={copied === value ? 'Copiado' : 'Copiar'}>{copied === value ? <Icon name="check" className="h-4 w-4 text-emerald-600" /> : <Icon name="copy" className="h-4 w-4" />}</button>}</div></div>
}

export default App
