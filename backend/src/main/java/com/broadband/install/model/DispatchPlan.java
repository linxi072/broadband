package com.broadband.install.model;

import java.util.ArrayList;
import java.util.List;

/** 一次派单方案输出：成功结果 + 拦截异常 + 过程日志。 */
public class DispatchPlan {
    public List<DispatchResult> results = new ArrayList<>();
    public List<DispatchException> exceptions = new ArrayList<>();
    public List<String> logs = new ArrayList<>();
}
