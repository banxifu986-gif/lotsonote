package com.banny.lotsonote.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.banny.lotsonote.model.base.ApiResponse;
import com.banny.lotsonote.model.dto.statistic.StatisticQueryParam;
import com.banny.lotsonote.model.entity.Statistic;
import com.banny.lotsonote.service.StatisticService;

@RestController
@RequestMapping("/api")
public class StatisticController {

    @Autowired
    StatisticService statisticService;

    @GetMapping("/statistic")
    public ApiResponse<List<Statistic>> getStatistic(@Valid StatisticQueryParam queryParam) {
        return statisticService.getStatistic(queryParam);
    }
}
