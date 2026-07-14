import request from '@/utils/request'

/** 获取当前天气 */
export function getCurrentWeather(greenhouseId) {
  return request.get('/weather/current', { params: { greenhouseId } })
}
