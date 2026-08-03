import request from '@/utils/request'

/** 获取当前天气（按大棚，后端自动取其登记城市） */
export function getCurrentWeather(params) {
  return request.get('/weather/current', { params })
}

/** 按地区名获取当前天气（管理员按地区查看） */
export function getWeatherByLocation(location) {
  return request.get('/weather/current', { params: { location } })
}
