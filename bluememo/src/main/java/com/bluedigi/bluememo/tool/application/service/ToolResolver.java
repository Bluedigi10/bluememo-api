package com.bluedigi.bluememo.tool.application.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.bluedigi.bluememo.common.domain.ToolType;
import com.bluedigi.bluememo.tool.application.port.out.Tool;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ToolResolver {
    private final Map<ToolType, Tool<?, ?>> tools;

    public ToolResolver(List<Tool<?, ?>> tools) {
        this.tools = tools.stream().collect(
            Collectors.toMap(Tool::getToolType, Function.identity())
        );
    }

    @SuppressWarnings("java:S1452")
    public Tool<?, ?> resolve(ToolType type){
        return tools.get(type);
    }


}
