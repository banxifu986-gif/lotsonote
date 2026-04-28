package com.banny.lotsonote.service.impl;

import com.banny.lotsonote.annotation.NeedLogin;
import com.banny.lotsonote.exception.BusinessException;
import com.banny.lotsonote.model.base.ApiResponse;
import com.banny.lotsonote.model.base.Pagination;
import com.banny.lotsonote.model.dto.user.LoginRequest;
import com.banny.lotsonote.model.dto.user.RegisterRequest;
import com.banny.lotsonote.model.dto.user.UpdateUserRequest;
import com.banny.lotsonote.model.dto.user.UserQueryParam;
import com.banny.lotsonote.model.entity.User;
import com.banny.lotsonote.model.enums.user.VerifyCodeType;
import com.banny.lotsonote.mapper.UserMapper;
import com.banny.lotsonote.model.vo.user.AdminUserVO;
import com.banny.lotsonote.model.vo.user.AvatarVO;
import com.banny.lotsonote.model.vo.user.RegisterVO;
import com.banny.lotsonote.model.vo.user.LoginUserVO;
import com.banny.lotsonote.model.vo.user.UserVO;
import com.banny.lotsonote.scope.RequestScopeData;
import com.banny.lotsonote.service.EmailService;
import com.banny.lotsonote.service.FileService;
import com.banny.lotsonote.service.UserService;
import com.banny.lotsonote.utils.ApiResponseUtil;
import com.banny.lotsonote.utils.JwtUtil;
import com.banny.lotsonote.utils.PaginationUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@Log4j2
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private FileService fileService;

    @Autowired
    private RequestScopeData requestScopeData;

    @Autowired
    private EmailService emailService;


    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<RegisterVO> register(RegisterRequest request, String ip) {
        // 检查账号是否已存在
        if (userMapper.findByAccount(request.getAccount()) != null) {
            throw new BusinessException("账号重复");
        }

        // 如果提供了邮箱，则进行邮箱相关验证
        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            if (userMapper.findByEmail(request.getEmail()) != null) {
                throw new BusinessException("邮箱已被使用");
            }
            if (request.getVerifyCode() == null || request.getVerifyCode().isEmpty()) {
                throw new BusinessException("请提供邮箱验证码");
            }
            if (!emailService.checkVerificationCode(request.getEmail(), request.getVerifyCode(), ip, VerifyCodeType.REGISTER)) {
                throw new BusinessException("验证码无效或已过期");
            }
        }

        // 创建新用户，加密密码后写入数据库
        User user = new User();
        BeanUtils.copyProperties(request, user);
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        userMapper.insert(user);
        userMapper.updateLastLoginAt(user.getUserId());

        String token = jwtUtil.generateToken(user.getUserId());
        RegisterVO registerVO = new RegisterVO();
        BeanUtils.copyProperties(user, registerVO);

        return ApiResponseUtil.success("注册成功", registerVO, token);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<LoginUserVO> login(LoginRequest request, String ip) {
        User user;

        if (request.getVerifyCode() != null && !request.getVerifyCode().isBlank()) {
            user = userMapper.findByEmail(request.getEmail());
        } else if (request.getAccount() != null && !request.getAccount().isEmpty()) {
            user = userMapper.findByAccount(request.getAccount());
        } else if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            user = userMapper.findByEmail(request.getEmail());
        } else {
            throw new BusinessException("请提供账号或邮箱");
        }

        if (request.getVerifyCode() != null && !request.getVerifyCode().isBlank()) {
            if (user == null || !emailService.checkVerificationCode(request.getEmail(), request.getVerifyCode(), ip, VerifyCodeType.LOGIN)) {
                throw new BusinessException("验证码无效或已过期");
            }
        } else {
            if (user == null) {
                throw new BusinessException("用户不存在");
            }
            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                throw new BusinessException("密码错误");
            }
        }

        String token = jwtUtil.generateToken(user.getUserId());
        LoginUserVO userVO = new LoginUserVO();
        BeanUtils.copyProperties(user, userVO);
        userMapper.updateLastLoginAt(user.getUserId());

        return ApiResponseUtil.success("登录成功", userVO, token);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userMapper.findByEmail(email) != null;
    }

    // 自动登录：前端每次刷新页面时调用，用已有 token 换取新 token + 最新用户信息
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<LoginUserVO> whoami() {
        Long userId = requestScopeData.getUserId();
        if (userId == null) {
            throw new BusinessException("用户 ID 异常");
        }

        User user = userMapper.findById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 每次调用都刷新 token，实现滑动过期
        String newToken = jwtUtil.generateToken(userId);
        LoginUserVO userVO = new LoginUserVO();
        BeanUtils.copyProperties(user, userVO);
        userMapper.updateLastLoginAt(userId);

        return ApiResponseUtil.success("自动登录成功", userVO, newToken);
    }

    @Override
    public ApiResponse<UserVO> getUserInfo(Long userId) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return ApiResponseUtil.success("获取用户信息成功", userVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @NeedLogin
    public ApiResponse<LoginUserVO> updateUserInfo(UpdateUserRequest request) {
        // 校验至少有一个字段需要更新
        if (request.getUsername() == null && request.getGender() == null
                && request.getBirthday() == null && request.getAvatarUrl() == null
                && request.getEmail() == null && request.getSchool() == null
                && request.getSignature() == null) {
            throw new BusinessException("请至少提供一个需要更新的字段");
        }

        Long userId = requestScopeData.getUserId();

        User user = new User();
        BeanUtils.copyProperties(request, user);
        user.setUserId(userId);

        int rows = userMapper.update(user);
        if (rows == 0) {
            throw new BusinessException("用户不存在");
        }

        // 返回最新用户信息
        User updated = userMapper.findById(userId);
        LoginUserVO userVO = new LoginUserVO();
        BeanUtils.copyProperties(updated, userVO);
        return ApiResponseUtil.success("更新成功", userVO);
    }

    @Override
    public Map<Long, User> getUserMapByIds(List<Long> authorIds) {

        // 处理空数组的情况
        if (authorIds.isEmpty()) return Collections.emptyMap();

        // 查询用户列表
        List<User> users = userMapper.findByIdBatch(authorIds);

        return users.stream()
                .collect(Collectors.toMap(User::getUserId, user -> user));
    }

    @Override
    public ApiResponse<List<AdminUserVO>> getUserList(UserQueryParam userQueryParam) {
        int total = userMapper.countByQueryParam(userQueryParam);
        int offset = PaginationUtils.calculateOffset(userQueryParam.getPage(), userQueryParam.getPageSize());
        Pagination pagination = new Pagination(userQueryParam.getPage(), userQueryParam.getPageSize(), total);
        List<User> users = userMapper.findByQueryParam(userQueryParam, userQueryParam.getPageSize(), offset);
        List<AdminUserVO> userVOList = users.stream().map(user -> {
            AdminUserVO userVO = new AdminUserVO();
            BeanUtils.copyProperties(user, userVO);
            return userVO;
        }).toList();
        return ApiResponseUtil.success("获取用户列表成功", userVOList, pagination);
    }

    @Override
    public ApiResponse<AvatarVO> uploadAvatar(MultipartFile file) {
        String url = fileService.uploadImage(file);
        AvatarVO avatarVO = new AvatarVO();
        avatarVO.setUrl(url);
        return ApiResponseUtil.success("上传成功", avatarVO);
    }
}
