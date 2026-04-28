import React from 'react'
import { APP_NAME } from '../../../../base/constants'

/**
 * LOGO 可自定义
 */
const Logo: React.FC = () => {
  return (
    <div className="text-xl font-bold tracking-wide text-gray-800 dark:text-gray-200">
      {APP_NAME}
    </div>
  )
}

export default Logo
