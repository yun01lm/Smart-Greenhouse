# -*- coding: utf-8 -*-
"""语料上传接口测试（构造小 wav）"""
import io
import json
import time
import urllib.request
import uuid
import wave

def api(method, path, token=None, body=None):
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request('http://localhost:8080' + path, data=data, method=method)
    req.add_header('Content-Type', 'application/json')
    if token:
        req.add_header('Authorization', 'Bearer ' + token)
    try:
        with urllib.request.urlopen(req, timeout=30) as r:
            return json.loads(r.read().decode())
    except urllib.error.HTTPError as e:
        return {'code': e.code, 'msg': e.read().decode()[:300]}

# 构造 0.5 秒 8kHz 静音 wav
buf = io.BytesIO()
with wave.open(buf, 'wb') as w:
    w.setnchannels(1)
    w.setsampwidth(2)
    w.setframerate(8000)
    w.writeframes(b'\x00\x00' * 4000)
wav_bytes = buf.getvalue()

login = api('POST', '/api/v1/auth/login', body={'username': 'admin', 'password': '123456'})
token = login['data']['token']

# multipart 上传
boundary = '----testboundary' + uuid.uuid4().hex
parts = []
def field(name, value):
    parts.append(('--' + boundary + '\r\n').encode())
    parts.append(('Content-Disposition: form-data; name="' + name + '"\r\n\r\n').encode())
    parts.append((value + '\r\n').encode())

field('dialect', 'hebei')
field('annotationText', '测试标注文本')
field('source', 'MANUAL')
parts.append(('--' + boundary + '\r\n').encode())
parts.append(b'Content-Disposition: form-data; name="audio"; filename="test.wav"\r\nContent-Type: audio/wav\r\n\r\n')
parts.append(wav_bytes)
parts.append(('\r\n--' + boundary + '--\r\n').encode())
body = b''.join(parts)

req = urllib.request.Request('http://localhost:8080/api/v1/admin/corpus', data=body, method='POST')
req.add_header('Authorization', 'Bearer ' + token)
req.add_header('Content-Type', 'multipart/form-data; boundary=' + boundary)
t0 = time.time()
try:
    with urllib.request.urlopen(req, timeout=60) as r:
        resp = json.loads(r.read().decode())
        print('上传成功:', json.dumps(resp, ensure_ascii=False)[:200], f'耗时 {time.time()-t0:.1f}s')
except urllib.error.HTTPError as e:
    print('上传失败:', e.code, e.read().decode()[:300])

# 列表确认
r = api('GET', '/api/v1/admin/corpus?page=0&size=5', token)
print('列表:', json.dumps(r.get('data', {}).get('list', []), ensure_ascii=False)[:300])
