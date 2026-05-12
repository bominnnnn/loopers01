const express = require('express')
const cors    = require('cors')

const authRoutes  = require('./routes/auth')
const postRoutes  = require('./routes/posts')

const app = express()

// ── 미들웨어 ─────────────────────────────────────
app.use(cors({
  origin: process.env.CORS_ORIGIN || 'http://localhost:5173',
  credentials: true,
}))
app.use(express.json())

// ── 헬스체크 ────────────────────────────────────
app.get('/health', (req, res) => {
  res.json({ status: 'ok', timestamp: new Date().toISOString() })
})

// ── 라우터 ──────────────────────────────────────
app.use('/api/auth',  authRoutes)
app.use('/api/posts', postRoutes)

// ── 전역 에러 핸들러 ─────────────────────────────
app.use((err, req, res, next) => {
  console.error(err.stack)
  res.status(err.status || 500).json({
    message: err.message || '서버 오류가 발생했습니다.',
  })
})

// ── 404 ─────────────────────────────────────────
app.use((req, res) => {
  res.status(404).json({ message: '존재하지 않는 API입니다.' })
})

module.exports = app