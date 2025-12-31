import type { PaginatedResponse } from "../shared/types";

const createPaginationConfig = <T>(
  response: PaginatedResponse<T> | null,
  onPageChange: (page: number, pageSize: number) => void,
  itemName: string = "items"
) => ({
  current: (response?.page?.number || 0) + 1,
  pageSize: response?.page?.size || 10,
  total: response?.page?.totalElements || 0,
  style: { marginRight: 16 },
  showTotal: (total: number, range: [number, number]) => `Showing ${range[0]}-${range[1]} of ${total} ${itemName}`,
  onChange: (page: number, pageSize: number) => {
    onPageChange(page - 1, pageSize);
  },
  onShowSizeChange: (current: number, size: number) => {
    onPageChange(0, size);
  },
});

export default createPaginationConfig;
