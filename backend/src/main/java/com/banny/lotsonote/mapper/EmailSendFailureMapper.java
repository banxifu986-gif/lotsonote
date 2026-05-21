package com.banny.lotsonote.mapper;

import com.banny.lotsonote.model.entity.EmailSendFailure;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EmailSendFailureMapper {
    int insert(EmailSendFailure emailSendFailure);
}
