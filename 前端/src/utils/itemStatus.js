// 商品状态与风险等级的展示映射
// 后端传的是英文码，界面上要中文 + 对应颜色，集中在这里，别在页面里各写一份

/** 商品状态：code → 中文名 / el-tag 类型 */
export const ITEM_STATUS = {
  DRAFT: { label: '草稿', type: 'info' },
  PENDING_AUDIT: { label: '待审核', type: 'warning' },
  ON_SALE: { label: '在售', type: 'success' },
  OFF_SALE: { label: '已下架', type: 'info' },
  SOLD: { label: '已售出', type: 'primary' },
  REJECTED: { label: '已驳回', type: 'danger' }
}

/** 风险等级：LOW / MEDIUM / HIGH */
export const RISK_LEVEL = {
  LOW: { label: '低风险', type: 'success' },
  MEDIUM: { label: '中风险', type: 'warning' },
  HIGH: { label: '高风险', type: 'danger' }
}

export function statusLabel(code) {
  return ITEM_STATUS[code] ? ITEM_STATUS[code].label : (code || '—')
}

export function statusType(code) {
  return ITEM_STATUS[code] && ITEM_STATUS[code].type ? ITEM_STATUS[code].type : 'info'
}

export function riskLabel(code) {
  return RISK_LEVEL[code] ? RISK_LEVEL[code].label : '未评级'
}

export function riskType(code) {
  return RISK_LEVEL[code] && RISK_LEVEL[code].type ? RISK_LEVEL[code].type : 'info'
}

/** 我发布的页面上的筛选项，顺序按「最常看的排前面」 */
export const MY_FILTERS = [
  { value: '', label: '全部' },
  { value: 'ON_SALE', label: '在售' },
  { value: 'PENDING_AUDIT', label: '待审核' },
  { value: 'OFF_SALE', label: '已下架' },
  { value: 'REJECTED', label: '已驳回' },
  { value: 'SOLD', label: '已售出' }
]