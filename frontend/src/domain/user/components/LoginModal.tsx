import React, { useEffect, useState } from 'react'
import {
  Avatar,
  Button,
  Col,
  Form,
  Input,
  message,
  Modal,
  Row,
  Segmented,
} from 'antd'
import {
  ALPHANUMERIC_UNDERSCORE,
  ALPHANUMERIC_UNDERSCORE_CHINESE,
  PASSWORD_ALLOWABLE_CHARACTERS,
  EMAIL_PATTERN,
} from '../../../base/regex'
import { useLogin } from '../hooks/useLogin.ts'
import { useRegister } from '../hooks/useRegister.ts'
import { userService } from '../service/userService.ts'
import { useForm } from 'antd/es/form/Form'
import CountDownButton from '@/domain/user/components/CountDownButton.tsx'
import { useDispatch } from 'react-redux'
import { setLoginModalOpen } from '../../../store/appSlice.ts'

interface LoginModalProps {
  forceOpen?: boolean
}

type LoginMode = 'login' | 'register'
type LoginType = 'password' | 'verifyCode'

const LoginModal: React.FC<LoginModalProps> = ({ forceOpen = false }) => {
  const [open, setOpen] = useState(forceOpen)
  const [mode, setMode] = useState<LoginMode>('login')
  const [loginType, setLoginType] = useState<LoginType>('password')
  const [loading, setLoading] = useState(false)

  const dispatch = useDispatch()
  const { loginHandle } = useLogin()
  const { registerHandle } = useRegister()
  const [form] = useForm()

  useEffect(() => {
    setOpen(forceOpen)
    if (forceOpen) {
      setMode('login')
      setLoginType('password')
    }
  }, [forceOpen])

  useEffect(() => {
    form.resetFields()
    if (mode !== 'login') {
      setLoginType('password')
    }
  }, [form, mode])

  const handleOpenChange = (nextOpen: boolean) => {
    setOpen(nextOpen)
    dispatch(setLoginModalOpen(nextOpen && forceOpen))
  }

  const handleLoginAccountChange = (inputValue: string) => {
    if (inputValue.includes('@')) {
      form.setFieldsValue({ email: inputValue, account: undefined })
      return
    }
    form.setFieldsValue({ account: inputValue, email: undefined })
  }

  const handleSendVerifyCode = async () => {
    try {
      await form.validateFields(['email'])
      const email = form.getFieldValue('email')
      if (!email) {
        message.error('请输入邮箱')
        return false
      }
      setLoading(true)
      await userService.sendVerifyCode({
        email,
        type: mode === 'register' ? 'REGISTER' : 'LOGIN',
      })
      message.success('验证码已发送')
      return true
    } catch (e: any) {
      message.error(e.message || '发送失败')
      return false
    } finally {
      setLoading(false)
    }
  }

  async function onFinish(values: any) {
    try {
      setLoading(true)
      if (mode === 'login') {
        const loginPayload =
          loginType === 'verifyCode'
            ? {
                email: values.email,
                verifyCode: values.verifyCode,
              }
            : values.email
              ? {
                  email: values.email,
                  password: values.password,
                }
              : {
                  account: values.account,
                  password: values.password,
                }
        await loginHandle(loginPayload)
        message.success('登录成功')
      } else {
        await registerHandle(values)
        message.success('注册成功')
      }
      handleOpenChange(false)
    } catch (e: any) {
      message.error(e.message)
    } finally {
      setLoading(false)
    }
  }

  const renderPasswordLogin = () => (
    <>
      <Form.Item
        label="账号或邮箱"
        name="loginAccount"
        rules={[
          { required: true, message: '请输入账号或邮箱' },
          {
            validator: async (_, fieldValue: string) => {
              if (!fieldValue) {
                return
              }
              const isEmail = fieldValue.includes('@')
              const matched = isEmail
                ? EMAIL_PATTERN.test(fieldValue)
                : ALPHANUMERIC_UNDERSCORE.test(fieldValue)
              if (!matched) {
                throw new Error(
                  isEmail ? '邮箱格式不正确' : '账号只能包含字母、数字和下划线',
                )
              }
            },
          },
        ]}
      >
        <Input
          autoComplete="off"
          onChange={(e) => handleLoginAccountChange(e.target.value)}
        />
      </Form.Item>
      <Form.Item name="account" hidden>
        <Input />
      </Form.Item>
      <Form.Item name="email" hidden>
        <Input />
      </Form.Item>
    </>
  )

  const renderVerifyCodeLogin = () => (
    <>
      <Form.Item
        label="邮箱"
        name="email"
        rules={[
          { required: true, message: '请输入邮箱' },
          { type: 'email', message: '邮箱格式不正确' },
        ]}
      >
        <Input autoComplete="off" />
      </Form.Item>
      <Row gutter={8} align="middle">
        <Col flex="auto">
          <Form.Item
            label="验证码"
            name="verifyCode"
            rules={[
              { required: true, message: '请输入验证码' },
              { len: 6, message: '验证码长度必须为 6 位' },
            ]}
          >
            <Input autoComplete="off" />
          </Form.Item>
        </Col>
        <Col>
          <CountDownButton handleSendVerifyCode={handleSendVerifyCode} />
        </Col>
      </Row>
    </>
  )

  const renderRegister = () => (
    <>
      <Form.Item
        label="账号"
        name="account"
        rules={[
          { required: true, message: '请输入账号' },
          {
            pattern: ALPHANUMERIC_UNDERSCORE,
            message: '账号只能包含字母、数字和下划线',
          },
          {
            min: 6,
            max: 16,
            message: '账号长度在 6 - 16 个字符',
          },
        ]}
      >
        <Input autoComplete="off" />
      </Form.Item>
      <Form.Item
        label="昵称"
        name="username"
        rules={[
          { required: true, message: '请输入用户名' },
          {
            pattern: ALPHANUMERIC_UNDERSCORE_CHINESE,
            message: '昵称只能包含中文、字母、数字和下划线',
          },
          {
            min: 1,
            max: 16,
            message: '昵称长度在 1 - 16 个字符之间',
          },
        ]}
      >
        <Input autoComplete="off" />
      </Form.Item>
      <Form.Item
        label="邮箱"
        name="email"
        rules={[
          { required: true, message: '请输入邮箱' },
          { type: 'email', message: '邮箱格式不正确' },
        ]}
      >
        <Input autoComplete="off" />
      </Form.Item>
      <Row gutter={8} align="middle">
        <Col flex="auto">
          <Form.Item
            label="验证码"
            name="verifyCode"
            rules={[
              { required: true, message: '请输入验证码' },
              { len: 6, message: '验证码长度必须为 6 位' },
            ]}
          >
            <Input autoComplete="off" />
          </Form.Item>
        </Col>
        <Col>
          <CountDownButton handleSendVerifyCode={handleSendVerifyCode} />
        </Col>
      </Row>
    </>
  )

  const renderPassword = () => (
    <Form.Item
      label="密码"
      name="password"
      rules={[
        { required: true, message: '请输入密码' },
        {
          pattern: PASSWORD_ALLOWABLE_CHARACTERS,
          message: '密码中包含不允许的字符',
        },
        {
          min: 8,
          max: 16,
          message: '密码长度在 8 - 16 个字符之间',
        },
      ]}
    >
      <Input.Password autoComplete="new-password" />
    </Form.Item>
  )

  return (
    <div className="cursor-pointer">
      {!forceOpen ? (
        <Avatar size={36} onClick={() => handleOpenChange(true)}>
          <span className="flex items-center text-xs">登录</span>
        </Avatar>
      ) : null}
      <Modal
        title="注册登录"
        open={open}
        onCancel={() => {
          if (!forceOpen) {
            handleOpenChange(false)
          }
        }}
        footer={null}
        closable={!forceOpen}
        maskClosable={!forceOpen}
        keyboard={!forceOpen}
      >
        <div className="mt-4">
          <Segmented
            block
            options={[
              {
                label: '登录',
                value: 'login',
              },
              {
                label: '注册',
                value: 'register',
              },
            ]}
            value={mode}
            onChange={(nextValue) => setMode(nextValue as LoginMode)}
          />
        </div>
        <div className="mt-4 flex justify-center pb-4">
          <Form
            name="loginForm"
            labelCol={{ span: 24 }}
            wrapperCol={{ span: 24 }}
            onFinish={onFinish}
            style={{ minWidth: '100%' }}
            autoComplete="off"
            layout="vertical"
            form={form}
          >
            {mode === 'login' && (
              <>
                <Segmented
                  block
                  options={[
                    {
                      label: '密码登录',
                      value: 'password',
                    },
                    {
                      label: '邮箱验证码登录',
                      value: 'verifyCode',
                    },
                  ]}
                  value={loginType}
                  onChange={(nextValue) => setLoginType(nextValue as LoginType)}
                />
                <div className="mt-4">
                  {loginType === 'password'
                    ? renderPasswordLogin()
                    : renderVerifyCodeLogin()}
                </div>
              </>
            )}

            {mode === 'register' && renderRegister()}

            {(mode === 'register' || loginType === 'password') &&
              renderPassword()}

            <Button type="primary" htmlType="submit" block loading={loading}>
              {mode === 'register' ? '注册' : '登录'}
            </Button>
          </Form>
        </div>
      </Modal>
    </div>
  )
}

export default LoginModal
