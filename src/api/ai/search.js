import request from '@/utils/request'

// 向量检索
export function vectorSearch(data) {
  return request({
    url: '/lingdoc/ai/search/vector',
    method: 'post',
    data: data,
    timeout: 30000
  })
}

// 全文检索
export function fullTextSearch(data) {
  return request({
    url: '/lingdoc/ai/search/fulltext',
    method: 'post',
    data: data,
    timeout: 30000
  })
}

// 混合检索（向量 + 全文）
export function hybridSearch(data) {
  return request({
    url: '/lingdoc/ai/search/hybrid',
    method: 'post',
    data: data,
    timeout: 30000
  })
}
