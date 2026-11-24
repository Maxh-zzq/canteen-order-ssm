export type Role = 'STUDENT' | 'STAFF' | 'ADMIN'

export interface ApiResult<T> {
  code: number
  message: string
  data: T
  traceId?: string
  timestamp?: number
}

export interface PageResult<T> {
  total: number
  pages: number
  pageNum: number
  pageSize: number
  list: T[]
}

export interface UserVO {
  id: string
  username: string
  realName: string
  phone: string
  roleCode: Role
  roleName: string
  balance: number
  status: number
}

export interface LoginPayload {
  username: string
  password: string
  captchaKey?: string
  captchaCode?: string
}

export interface LoginResult {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  user: UserVO
}

export interface CategoryVO {
  id: number
  name: string
  sort: number
  status: number
}

export interface StallVO {
  id: number
  name: string
  floor: string
  windowNo: string
  openTime: string
  closeTime: string
  status: number
}

export interface DishSpecVO {
  id: number
  name: string
  priceDiff: number
}

export interface DishVO {
  id: number
  stallId: number
  stallName: string
  categoryId: number
  categoryName: string
  name: string
  coverUrl: string | null
  price: number
  description: string
  dailyStock: number
  sales: number
  avgScore: number
  status: number
  specs: DishSpecVO[]
}

export interface MenuVO {
  menuId: number
  dishId: number
  dishName: string
  coverUrl: string | null
  price: number
  categoryId: number
  categoryName: string
  stallId: number
  stallName: string
  stockTotal: number
  stockLeft: number
  sales: number
  serveDate: string
  status: number
}

export interface CartItemVO {
  dishId: number
  specId: number | null
  specName: string | null
  dishName: string
  coverUrl: string | null
  price: number
  quantity: number
  amount: number
  stockLeft: number | null
}

export interface CartVO {
  items: CartItemVO[]
  totalQuantity: number
  totalAmount: number
}

export interface OrderItemVO {
  dishId: number
  dishName: string
  specName: string | null
  price: number
  quantity: number
  amount: number
}

export interface OrderVO {
  orderNo: string
  status: number
  statusText: string
  totalAmount: number
  payAmount: number
  pickupCode: string
  expectTime: string | null
  expireTime: string | null
  payTime: string | null
  finishTime: string | null
  createTime: string | null
  stallId: number | null
  stallName: string | null
  remark: string | null
  cancelReason: string | null
  canCancel: boolean
  items: OrderItemVO[]
}

export interface ReviewVO {
  id: number
  username: string
  score: number
  content: string
  images: string[] | null
  createTime: string
}

export interface StatsOverviewVO {
  todayOrderCount: number
  todayRevenue: number
  totalUsers: number
  avgScore: number
  pendingOrders: number
}

export interface SalesTrendVO {
  statDate: string
  orderCount: number
  revenue: number
}

export interface TopDishVO {
  dishName: string
  quantity: number
  amount: number
}

export interface OperLogVO {
  id: number
  traceId: string
  username: string
  module: string
  method: string
  params: string
  ip: string
  costMs: number
  success: number
  errorMsg: string | null
  createTime: string
}

export interface ConfigVO {
  id: number
  configKey: string
  configValue: string
  remark: string
}

export interface CaptchaVO {
  key: string
  image: string
}

/* ---------- 请求体 ---------- */

export interface UpdateProfilePayload {
  realName?: string
  phone?: string
}

export interface RechargePayload {
  amount: number
}

export interface AddCartPayload {
  dishId: number
  specId?: number | null
  quantity: number
}

export interface UpdateCartPayload {
  dishId: number
  specId?: number | null
  quantity: number
}

export interface PlaceOrderPayload {
  requestId: string
  expectTime?: string | null
  remark?: string
}

export interface PayCallbackPayload {
  orderNo: string
  tradeNo: string
  amount: number
  sign: string
}

export interface CreateReviewPayload {
  orderNo: string
  score: number
  content?: string
  images?: string
}

export interface CategoryPayload {
  name: string
  sort?: number
  status?: number
}

export interface DishSpecPayload {
  name: string
  priceDiff: number
}

export interface DishPayload {
  stallId: number
  categoryId: number
  name: string
  coverUrl?: string | null
  price: number
  description?: string
  dailyStock?: number
  status?: number
  specs?: DishSpecPayload[]
}

export interface StallPayload {
  name: string
  floor?: string
  windowNo?: string
  managerId?: number | null
  openTime?: string
  closeTime?: string
  status?: number
}

export interface MenuPublishPayload {
  serveDate?: string | null
  dishIds?: number[]
}

export interface UserStatusPayload {
  status: number
}

export interface ConfigPayload {
  configKey: string
  configValue: string
  remark?: string
}
