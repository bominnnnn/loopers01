require('dotenv').config()
const app = require('./src/app')

const PORT = process.env.PORT || 3000

app.listen(PORT, () => {
  console.log(`🛡️  LOOPERS 서버 실행 중 → http://localhost:${PORT}`)
  console.log(`📋 환경: ${process.env.NODE_ENV || 'development'}`)
})