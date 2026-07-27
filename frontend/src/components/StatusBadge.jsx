const STATUS_CLASS = {
  PENDING: 'badge--pending',
  APPROVED: 'badge--approved',
  REJECTED: 'badge--rejected',
  CANCELLED: 'badge--cancelled',
  COMPLETED: 'badge--completed',
};

export default function StatusBadge({ status }) {
  return <span className={`badge ${STATUS_CLASS[status] || ''}`}>{status}</span>;
}
