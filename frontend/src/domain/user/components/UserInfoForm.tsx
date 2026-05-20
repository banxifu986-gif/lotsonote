import React, { useEffect, useState } from 'react'
import dayjs from 'dayjs'
import ImgCrop from 'antd-img-crop'
import { Upload } from 'antd'
import {
  Avatar,
  Button,
  DatePicker,
  Form,
  GetProp,
  Input,
  message,
  Select,
  UploadFile,
  UploadProps,
} from 'antd'
import { RcFile, UploadChangeParam } from 'antd/es/upload/interface'
import type { UploadRequestOption as RcCustomRequestOptions } from 'rc-upload/lib/interface'
import { userService } from '../service/userService.ts'
import { useUser } from '../hooks/useUser.ts'
import { useUserForm } from '../hooks/useUserForm.ts'
import { UserState } from '../types/types.ts'
import { resolveAvatarUrl } from '../utils/avatar.ts'

type UploadAvatarResponse = {
  code: number
  message: string
  data: {
    url: string
  }
}

const UserInfoForm: React.FC = () => {
  const user = useUser() as UserState

  const [form] = Form.useForm()
  const [editing, setEditing] = useState(false)

  const { updateUserInfo, updateUserAvatar } = useUserForm()

  const handleEditToggle = () => {
    setEditing(!editing)
  }

  useEffect(() => {
    form.setFieldsValue({
      username: user.username,
      gender: user.gender,
      birthday: dayjs(user.birthday),
      email: user.email,
      school: user.school,
      signature: user.signature,
    })
  })

  const handleSave = async (values: UserState) => {
    const oldValues = user
    const diff: Partial<typeof oldValues> = {}

    Object.entries(values).forEach(([key, newValue]) => {
      // @ts-expect-error existing shape is dynamic here
      const oldValue = oldValues[key]
      if (key === 'birthday') {
        const newBirthday = newValue
          ? dayjs(newValue as string | Date).format('YYYY-MM-DD')
          : null
        const oldBirthday =
          dayjs(oldValues[key] as string | Date).format('YYYY-MM-DD') ?? null
        if (newBirthday !== oldBirthday) {
          // @ts-expect-error existing shape is dynamic here
          diff[key] = newBirthday
        }
      } else if (newValue !== oldValue) {
        // @ts-expect-error existing shape is dynamic here
        diff[key] = newValue
      }
    })

    if (Object.keys(diff).length === 0) {
      message.info('未更新任何字段')
      return
    }

    try {
      await updateUserInfo(diff)
      message.success('更新成功')
    } catch (e: any) {
      message.error(e.message)
    } finally {
      setEditing(false)
    }
  }

  type FileType = Parameters<GetProp<UploadProps, 'beforeUpload'>>[0]

  const onPreview = async (file: UploadFile) => {
    let src = file.url as string
    if (!src) {
      src = await new Promise((resolve) => {
        const reader = new FileReader()
        reader.readAsDataURL(file.originFileObj as FileType)
        reader.onload = () => resolve(reader.result as string)
      })
    }
    const image = new Image()
    image.src = src
    const imgWindow = window.open(src)
    imgWindow?.document.write(image.outerHTML)
  }

  const uploadAvatarHandle = async (
    info: UploadChangeParam<UploadFile<UploadAvatarResponse>>,
  ) => {
    if (info.file.response?.code === 200) {
      await updateUserAvatar(info.file.response.data.url)
      message.success('头像上传成功')
      return
    }

    if (info.file.status === 'error') {
      const errorMessage =
        info.file.response?.message ||
        (info.file.error instanceof Error
          ? info.file.error.message
          : '头像上传失败')
      message.error(errorMessage)
    }
  }

  const uploadAvatarRequest = async (options: RcCustomRequestOptions) => {
    const { file, onError, onSuccess } = options

    try {
      const formData = new FormData()
      formData.append('file', file as RcFile)
      const response = await userService.uploadImageService(formData)
      onSuccess?.(response, file)
    } catch (e) {
      onError?.(e as Error)
    }
  }

  return (
    <div className="rounded-lg bg-white p-6">
      <div className="mb-6 flex items-center space-x-4">
        <ImgCrop rotationSlider>
          <Upload
            customRequest={uploadAvatarRequest}
            onChange={uploadAvatarHandle}
            onPreview={onPreview}
            accept="image/*"
            showUploadList={false}
          >
            <Avatar
              src={resolveAvatarUrl(user.avatarUrl || '')}
              size={64}
              className="cursor-pointer"
            />
          </Upload>
        </ImgCrop>
        <div>
          <h2 className="text-2xl font-semibold">{user.username}</h2>
          <p className="text-gray-600">{user.email}</p>
        </div>
        <Button onClick={handleEditToggle} className="ml-auto">
          {editing ? '取消编辑' : '编辑'}
        </Button>
      </div>
      <Form
        form={form}
        layout="vertical"
        onFinish={handleSave}
        className={editing ? '' : 'pointer-events-none'}
      >
        <Form.Item
          label="用户名"
          name="username"
          rules={[{ required: true, message: '请输入用户名' }]}
        >
          <Input disabled={!editing} />
        </Form.Item>
        <Form.Item label="性别" name="gender">
          <Select disabled={!editing}>
            <Select.Option value={1}>男</Select.Option>
            <Select.Option value={2}>女</Select.Option>
            <Select.Option value={3}>保密</Select.Option>
          </Select>
        </Form.Item>
        <Form.Item label="生日" name="birthday">
          <DatePicker disabled={!editing} style={{ width: '100%' }} />
        </Form.Item>
        <Form.Item
          label="邮箱"
          name="email"
          rules={[{ type: 'email', message: '请输入有效的邮箱地址' }]}
        >
          <Input disabled={!editing} />
        </Form.Item>
        <Form.Item label="学校" name="school">
          <Input disabled={!editing} />
        </Form.Item>
        <Form.Item label="个性签名" name="signature">
          <Input.TextArea rows={3} disabled={!editing} />
        </Form.Item>
        {editing && (
          <Form.Item>
            <Button type="primary" htmlType="submit" className="mr-2">
              保存
            </Button>
            <Button onClick={handleEditToggle}>取消</Button>
          </Form.Item>
        )}
      </Form>
    </div>
  )
}

export default UserInfoForm
