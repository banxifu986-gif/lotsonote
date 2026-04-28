package com.banny.lotsonote.service.impl;

import com.banny.lotsonote.mapper.MessageMapper;
import com.banny.lotsonote.model.base.ApiResponse;
import com.banny.lotsonote.model.base.EmptyVO;
import com.banny.lotsonote.model.base.Pagination;
import com.banny.lotsonote.model.dto.message.MessageDTO;
import com.banny.lotsonote.model.dto.message.MessageQueryParams;
import com.banny.lotsonote.model.entity.Message;
import com.banny.lotsonote.model.entity.User;
import com.banny.lotsonote.model.enums.message.MessageType;
import com.banny.lotsonote.model.vo.message.MessageVO;
import com.banny.lotsonote.scope.RequestScopeData;
import com.banny.lotsonote.service.MessageService;
import com.banny.lotsonote.service.UserService;
import com.banny.lotsonote.utils.ApiResponseUtil;
import com.banny.lotsonote.utils.PaginationUtils;
import com.banny.lotsonote.utils.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 消息服务实现类
 */
@Service
@Slf4j
public class MessageServiceImpl implements MessageService {

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private RequestScopeData requestScopeData;

    @Override
    public Integer createMessage(MessageDTO messageDTO) {
        Message message = new Message();
        BeanUtils.copyProperties(messageDTO, message);

        if (messageDTO.getContent() == null) {
            message.setContent("");
        }

        return messageMapper.insert(message);
    }

    @Override
    public ApiResponse<List<MessageVO>> getMessages(MessageQueryParams params) {
        Long currentUserId = requestScopeData.getUserId();
        int offset = PaginationUtils.calculateOffset(params.getPage(), params.getPageSize());

        List<Message> messages = messageMapper.selectByParams(currentUserId, params, params.getPageSize(), offset);
        int total = messageMapper.countByParams(currentUserId, params);

        List<Long> senderIds = messages.stream().map(Message::getSenderId).toList();
        Map<Long, User> userMap = userService.getUserMapByIds(senderIds);

        List<MessageVO> messageVOS = messages.stream().map(message -> {
            MessageVO messageVO = new MessageVO();
            BeanUtils.copyProperties(message, messageVO);

            MessageVO.Sender sender = new MessageVO.Sender();
            sender.setUserId(message.getSenderId());
            sender.setUsername(userMap.get(message.getSenderId()).getUsername());
            sender.setAvatarUrl(userMap.get(message.getSenderId()).getAvatarUrl());
            messageVO.setSender(sender);

            if (!Objects.equals(message.getType(), MessageType.SYSTEM)) {
                MessageVO.Target target = new MessageVO.Target();
                target.setTargetId(message.getTargetId());
                target.setTargetType(message.getTargetType());
                // TODO: 获取评论/点赞 对应的 note 的 question 信息
            }

            return messageVO;
        }).toList();

        Pagination pagination = new Pagination(params.getPage(), params.getPageSize(), total);
        return ApiResponseUtil.success("", messageVOS, pagination);
    }

    @Override
    public ApiResponse<EmptyVO> markAsRead(Integer messageId) {
        Long currentUserId = requestScopeData.getUserId();
        messageMapper.markAsRead(messageId, currentUserId);
        return ApiResponse.success();
    }

    @Override
    public ApiResponse<EmptyVO> markAsReadBatch(List<Integer> messageIds) {
        Long currentUserId = requestScopeData.getUserId();
        messageMapper.markAsReadBatch(messageIds, currentUserId);
        return ApiResponse.success();
    }

    @Override
    public ApiResponse<EmptyVO> markAllAsRead() {
        Long currentUserId = requestScopeData.getUserId();
        messageMapper.markAllAsRead(currentUserId);
        return ApiResponse.success();
    }

    @Override
    public ApiResponse<EmptyVO> deleteMessage(Integer messageId) {
        Long currentUserId = requestScopeData.getUserId();
        messageMapper.deleteMessage(messageId, currentUserId);
        return ApiResponse.success();
    }

    @Override
    public ApiResponse<Integer> getUnreadCount() {
        Long currentUserId = requestScopeData.getUserId();
        Integer count = messageMapper.countUnread(currentUserId);
        return ApiResponse.success(count);
    }
}
