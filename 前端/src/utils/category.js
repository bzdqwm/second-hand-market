// 类目树工具
//
// 后端 /api/category/list 返回的是【扁平列表】：
//   [{ id: 1, parentId: 0, name: '数码' },
//    { id: 2, parentId: 1, name: '手机' }, ...]
//
// Element Plus 的 el-cascader 要的是【嵌套树】：
//   [{ id: 1, name: '数码', children: [{ id: 2, name: '手机' }] }]
//
// 这个函数负责把前者变成后者。

/**
 * 扁平类目列表 → 级联选择器用的树
 * @param {Array<{id:number,parentId:number,name:string,sort?:number}>} list
 * @returns {Array<{id:number,name:string,children?:Array}>}
 */
export function toCascaderOptions(list) {
  if (!Array.isArray(list)) return []

  // ① 按 parentId 分组，key 是父 id
  const childrenMap = new Map()
  list.forEach((item) => {
    const pid = item.parentId ?? 0
    if (!childrenMap.has(pid)) childrenMap.set(pid, [])
    childrenMap.get(pid).push(item)
  })

  // ② 从一级（parentId = 0）开始递归搭树
  const build = (parentId) => {
    const group = childrenMap.get(parentId) || []
    return group.map((node) => {
      const kids = build(node.id)
      const option = { id: node.id, name: node.name }
      // 没有子类目就不要加 children 字段，否则级联器会显示一个空的展开箭头
      if (kids.length > 0) option.children = kids
      return option
    })
  }

  return build(0)
}