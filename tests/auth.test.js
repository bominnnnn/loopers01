const request = require('supertest')
const app     = require('../src/app')

// 환경변수 설정 (테스트용)
process.env.JWT_SECRET      = 'test_secret'
process.env.JWT_EXPIRES_IN  = '1h'

describe('Auth API', () => {

  const testUser = {
    loginId:  'testuser',
    name:     '테스트 유저',
    email:    'test@test.com',
    password: 'Test1234!',
  }
  let token = ''

  // ── 회원가입 ─────────────────────────────────────
  describe('POST /api/auth/register', () => {
    it('정상 회원가입 → 201', async () => {
      const res = await request(app).post('/api/auth/register').send(testUser)
      expect(res.status).toBe(201)
      expect(res.body.token).toBeDefined()
      expect(res.body.user.loginId).toBe(testUser.loginId)
    })

    it('중복 아이디 → 409', async () => {
      const res = await request(app).post('/api/auth/register').send(testUser)
      expect(res.status).toBe(409)
    })

    it('필수 항목 누락 → 400', async () => {
      const res = await request(app).post('/api/auth/register').send({ loginId: 'only' })
      expect(res.status).toBe(400)
    })
  })

  // ── 로그인 ───────────────────────────────────────
  describe('POST /api/auth/login', () => {
    it('정상 로그인 → 200 + token', async () => {
      const res = await request(app).post('/api/auth/login').send({
        loginId:  testUser.loginId,
        password: testUser.password,
      })
      expect(res.status).toBe(200)
      expect(res.body.token).toBeDefined()
      token = res.body.token
    })

    it('잘못된 비밀번호 → 401', async () => {
      const res = await request(app).post('/api/auth/login').send({
        loginId: testUser.loginId, password: 'wrongpassword',
      })
      expect(res.status).toBe(401)
    })

    it('존재하지 않는 아이디 → 404', async () => {
      const res = await request(app).post('/api/auth/login').send({
        loginId: 'nobody', password: '1234',
      })
      expect(res.status).toBe(404)
    })
  })

  // ── 내 정보 조회 ─────────────────────────────────
  describe('GET /api/auth/me', () => {
    it('토큰 없이 요청 → 401', async () => {
      const res = await request(app).get('/api/auth/me')
      expect(res.status).toBe(401)
    })

    it('정상 조회 → 200', async () => {
      const res = await request(app).get('/api/auth/me')
        .set('Authorization', `Bearer ${token}`)
      expect(res.status).toBe(200)
      expect(res.body.loginId).toBe(testUser.loginId)
    })
  })

  // ── 비밀번호 유효성 ──────────────────────────────
  describe('POST /api/auth/validate-password', () => {
    it('유효한 비밀번호 → 200 valid:true', async () => {
      const res = await request(app).post('/api/auth/validate-password')
        .send({ password: 'Secure1!' })
      expect(res.status).toBe(200)
      expect(res.body.valid).toBe(true)
    })

    it('짧은 비밀번호 → 422 valid:false', async () => {
      const res = await request(app).post('/api/auth/validate-password')
        .send({ password: 'short' })
      expect(res.status).toBe(422)
      expect(res.body.valid).toBe(false)
    })
  })

})