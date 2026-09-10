type Props = {
  value: string;
  status: string;
  date: string;
  onValueChange: (value: string) => void;
  onStatusChange: (value: string) => void;
  onDateChange: (value: string) => void;
  onClear: () => void;
  onSearch: () => void;
};

export default function SearchFilterBar(props: Props) {
  return (
    <div className="filter-bar">
      <div className="search-field"><span aria-hidden="true">⌕</span><input value={props.value} onChange={(event) => props.onValueChange(event.target.value)} placeholder="Search by shipment ID or tracking number" />{props.value && <button type="button" className="clear-button" onClick={props.onClear}>×</button>}</div>
      <select value={props.status} onChange={(event) => props.onStatusChange(event.target.value)} aria-label="Filter by status"><option value="">All statuses</option><option value="IN_TRANSIT">In transit</option><option value="DELIVERED">Delivered</option><option value="DELAYED">Delayed</option><option value="OUT_FOR_DELIVERY">Out for delivery</option></select>
      <input type="date" value={props.date} onChange={(event) => props.onDateChange(event.target.value)} aria-label="Filter by date" />
      <button type="button" onClick={props.onSearch}>Search shipment</button>
    </div>
  );
}
