package com.banny.lotsonote.service;

import com.banny.lotsonote.model.base.ApiResponse;
import com.banny.lotsonote.model.dto.statistic.StatisticQueryParam;
import com.banny.lotsonote.model.entity.Statistic;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional
public interface StatisticService {
    /**
     * 获取统计信息
     * @param queryParam 查询参数，用于指定统计条件
     * @return 返回一个ApiResponse对象，其中包含符合查询条件的统计信息列表
     */
    ApiResponse<List<Statistic>> getStatistic(StatisticQueryParam queryParam);
}
