package com.banny.lotsonote.service.impl;

import com.banny.lotsonote.mapper.StatisticMapper;
import com.banny.lotsonote.model.base.ApiResponse;
import com.banny.lotsonote.model.base.Pagination;
import com.banny.lotsonote.model.dto.statistic.StatisticQueryParam;
import com.banny.lotsonote.model.entity.Statistic;
import com.banny.lotsonote.service.StatisticService;
import com.banny.lotsonote.utils.ApiResponseUtil;
import com.banny.lotsonote.utils.PaginationUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StatisticServiceImpl implements StatisticService {
    @Autowired
    private StatisticMapper statisticMapper;

    @Override
    public ApiResponse<List<Statistic>> getStatistic(StatisticQueryParam queryParam) {

        Integer page = queryParam.getPage();
        Integer pageSize = queryParam.getPageSize();
        int offset = PaginationUtils.calculateOffset(page, pageSize);
        int total = statisticMapper.countStatistic();

        Pagination pagination = new Pagination(page, pageSize, total);

        try {
            List<Statistic> statistics = statisticMapper.findByPage(pageSize, offset);
            return ApiResponseUtil.success("获取统计列表成功", statistics, pagination);
        } catch (Exception e) {
            return ApiResponseUtil.error(e.getMessage());
        }
    }
}
