import request from '@/utils/request'

/**
 * PaddleOCR 识别模块 API
 */

// 查询OCR任务列表
export function listOcrTask(query) {
  return request({
    url: '/lingdoc/ocr/list',
    method: 'get',
    params: query
  })
}

// 获取OCR任务详情
export function getOcrTask(taskId) {
  return request({
    url: '/lingdoc/ocr/' + taskId,
    method: 'get'
  })
}

// 上传文件并执行OCR识别
export function uploadAndOcr(data, taskName, async) {
  return request({
    url: '/lingdoc/ocr/upload',
    method: 'post',
    data: data,
    params: {
      taskName: taskName,
      async: async
    },
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 重新识别已有任务
export function reprocessOcr(taskId) {
  return request({
    url: '/lingdoc/ocr/reprocess/' + taskId,
    method: 'post'
  })
}

// 删除OCR任务
export function delOcrTask(taskId) {
  return request({
    url: '/lingdoc/ocr/' + taskId,
    method: 'delete'
  })
}
