import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Plus } from 'lucide-react'
import { Alert, Button, Pagination, CTA_BUTTON_STYLE } from '@/shared/ui'
import { enrollmentTokensApi } from '../api/enrollmentTokensApi'
import type { IEnrollmentTokensApi, EnrollmentToken } from '../model/IEnrollmentTokensApi'
import { AdminTokensApiProvider } from '../model/enrollmentTokensApiContext'
import { useEnrollmentTokens } from '../model/useEnrollmentTokens'
import { useCreateToken, useRevokeToken } from '../model/useTokenMutations'
import { TokensTable } from './TokensTable'
import { TokensCardList } from './TokensCardList'
import { EnrollmentProcedureCard } from './EnrollmentProcedureCard'
import { GenerateTokenModal } from './GenerateTokenModal'
import { RevokeTokenModal } from './RevokeTokenModal'

interface AdminTokensPageProps {
  /** Composition seam: defaults to the real implementation; tests inject a fake. */
  api?: IEnrollmentTokensApi
}

/** Slice composition root: provisions the enrollment-tokens API at its own boundary. */
export function AdminTokensPage({ api = enrollmentTokensApi }: AdminTokensPageProps = {}) {
  return (
    <AdminTokensApiProvider api={api}>
      <AdminTokensPageContent />
    </AdminTokensApiProvider>
  )
}

function AdminTokensPageContent() {
  const { t } = useTranslation('adminTokens')

  const [page, setPage] = useState(0)
  const { data, isPending, isError: loadError, isPlaceholderData } = useEnrollmentTokens(page)

  const [createOpen, setCreateOpen] = useState(false)
  const [revokeTarget, setRevokeTarget] = useState<EnrollmentToken | null>(null)

  const createToken = useCreateToken()
  const revokeToken = useRevokeToken()

  const tokens = data?.content ?? []
  const totalElements = data?.totalElements ?? 0
  const pageSize = data?.size ?? 20
  const totalPages = totalElements > 0 ? Math.ceil(totalElements / pageSize) : 1
  const showPagination = !isPending && totalPages > 1
  const pendingRevokeId = revokeToken.isPending ? revokeToken.variables : null

  function handleRevokeSuccess() {
    // Step back a page when the revoked row was the last on a page beyond the first.
    if (!isPlaceholderData && data && data.content.length === 1 && page > 0) {
      setPage(p => p - 1)
    }
    setRevokeTarget(null)
  }

  return (
    <div className="py-5 px-6 mx-auto">
      <div className="flex flex-col gap-3 mb-4 sm:flex-row sm:items-start sm:justify-between sm:gap-4">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight text-fg-0">{t('title')}</h1>
          <p className="text-sm text-fg-2 mt-1">{t('subtitle')}</p>
        </div>
        <Button
          onClick={() => setCreateOpen(true)}
          className="self-start shrink-0 border-transparent font-semibold"
          style={CTA_BUTTON_STYLE}
        >
          <Plus className="size-4" />
          {t('action.create')}
        </Button>
      </div>

      {loadError && <Alert variant="error" className="mb-4">{t('load_error')}</Alert>}

      <EnrollmentProcedureCard />

      <div className="hidden md:block">
        <TokensTable
          tokens={tokens}
          isLoading={isPending}
          pendingRevokeId={pendingRevokeId}
          onRevoke={setRevokeTarget}
        />
      </div>
      <div className="md:hidden">
        <TokensCardList
          tokens={tokens}
          isLoading={isPending}
          pendingRevokeId={pendingRevokeId}
          onRevoke={setRevokeTarget}
        />
      </div>

      {showPagination && (
        <div className="mt-3">
          <Pagination
            page={page}
            totalPages={totalPages}
            onPageChange={setPage}
            pageLabel={t('pagination.page', { current: page + 1, total: totalPages })}
            prevLabel={t('pagination.prev')}
            nextLabel={t('pagination.next')}
            summary={t('pagination.total', { count: totalElements })}
            isTransitioning={isPlaceholderData}
          />
        </div>
      )}

      {createOpen && (
        <GenerateTokenModal
          onClose={() => setCreateOpen(false)}
          onCreate={(serverName, ttlMinutes) => createToken.mutateAsync({ serverName, ttlMinutes })}
          onSuccess={() => setPage(0)}
        />
      )}

      {revokeTarget && (
        <RevokeTokenModal
          token={revokeTarget}
          onClose={() => setRevokeTarget(null)}
          onRevoke={revokeToken.mutateAsync}
          onSuccess={handleRevokeSuccess}
        />
      )}
    </div>
  )
}
