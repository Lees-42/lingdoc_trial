import request from '@/utils/request'

// 查询可用模型列表
export function listModel() {
  return request({
    url: '/lingdoc/ai/model/list',
    method: 'get'
  })
}

// 获取模型配置
export function getModelConfig(modelName) {
  return request({
    url: '/lingdoc/ai/model/config/' + modelName,
    method: 'get'
  })
}

// 模型连通性测试
export function testModelConnection(data) {
  return request({
    url: '/lingdoc/ai/model/test',
    method: 'post',
    data: data,
    timeout: 15000
  })
}
