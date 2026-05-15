package com.tencent.supersonic.headless.server.rest;

import com.alibaba.excel.annotation.ExcelProperty;

public class TermExcelData {
    @ExcelProperty("名称")
    private String name;

    @ExcelProperty("近义词")
    private String alias;

    @ExcelProperty("描述")
    private String description;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
