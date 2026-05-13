package com.tencent.supersonic.headless.server.rest;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.headless.api.pojo.request.MetaBatchReq;
import com.tencent.supersonic.headless.api.pojo.request.TermReq;
import com.tencent.supersonic.headless.api.pojo.response.TermResp;
import com.tencent.supersonic.headless.server.service.TermService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/semantic/term")
public class TermController {

    @Autowired
    private TermService termService;

    @PostMapping("/saveOrUpdate")
    public boolean saveOrUpdate(@RequestBody TermReq termReq, HttpServletRequest request,
            HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        termService.saveOrUpdate(termReq, user);
        return true;
    }

    @PostMapping("/saveBatch")
    public boolean saveBatch(@RequestBody List<TermReq> termReqs, HttpServletRequest request,
            HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        termService.saveBatch(termReqs, user);
        return true;
    }

    @PostMapping("/upload")
    public List<TermUploadResp> upload(@RequestParam("file") MultipartFile file,
            @RequestParam("domainId") Long domainId,
            HttpServletRequest request, HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        List<TermUploadResp> result = new ArrayList<>();
        try {
            InputStream inputStream = file.getInputStream();
            
            List<TermExcelData> dataList = new ArrayList<>();
            EasyExcel.read(inputStream, TermExcelData.class, new AnalysisEventListener<TermExcelData>() {
                @Override
                public void invoke(TermExcelData data, AnalysisContext context) {
                    dataList.add(data);
                }
                @Override
                public void doAfterAllAnalysed(AnalysisContext context) {
                }
            }).sheet().doRead();

            int successCount = 0;
            int failCount = 0;
            
            for (int i = 0; i < dataList.size(); i++) {
                TermExcelData data = dataList.get(i);
                TermUploadResp resp = new TermUploadResp();
                resp.setRowNum(i + 2);
                resp.setName(data.getName());
                resp.setAlias(data.getAlias());
                resp.setDescription(data.getDescription());
                
                if (StringUtils.isBlank(data.getName())) {
                    resp.setSuccess(false);
                    resp.setMessage("名称不能为空");
                    failCount++;
                } else if (StringUtils.isBlank(data.getDescription())) {
                    resp.setSuccess(false);
                    resp.setMessage("描述不能为空");
                    failCount++;
                } else {
                    try {
                        TermReq termReq = new TermReq();
                        termReq.setDomainId(domainId);
                        termReq.setName(data.getName().trim());
                        termReq.setDescription(data.getDescription().trim());
                        if (StringUtils.isNotBlank(data.getAlias())) {
                            termReq.setAlias(Arrays.asList(data.getAlias().trim().split("[,，]")));
                        }
                        termService.saveOrUpdate(termReq, user);
                        resp.setSuccess(true);
                        resp.setMessage("导入成功");
                        successCount++;
                    } catch (Exception e) {
                        resp.setSuccess(false);
                        resp.setMessage("导入失败：" + e.getMessage());
                        failCount++;
                    }
                }
                result.add(resp);
            }
            log.info("术语批量导入完成，成功：{}条，失败：{}条", successCount, failCount);
        } catch (Exception e) {
            log.error("术语批量导入失败", e);
            TermUploadResp resp = new TermUploadResp();
            resp.setSuccess(false);
            resp.setMessage("文件解析失败：" + e.getMessage());
            result.add(resp);
        }
        return result;
    }

    @GetMapping("/downloadTemplate")
    public void downloadTemplate(HttpServletResponse response) {
        try {
            String fileName = "术语导入模板.xlsx";
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            response.setHeader("Content-disposition", "attachment;filename*=utf-8''" 
                    + URLEncoder.encode(fileName, StandardCharsets.UTF_8));
            
            List<TermExcelData> dataList = new ArrayList<>();
            TermExcelData example1 = new TermExcelData();
            example1.setName("日活跃用户");
            example1.setAlias("DAU,活跃用户");
            example1.setDescription("每日访问系统的独立用户数量");
            dataList.add(example1);
            
            TermExcelData example2 = new TermExcelData();
            example2.setName("月活跃用户");
            example2.setAlias("MAU");
            example2.setDescription("每月访问系统的独立用户数量");
            dataList.add(example2);
            
            EasyExcel.write(response.getOutputStream(), TermExcelData.class)
                    .sheet("模板")
                    .doWrite(dataList);
        } catch (Exception e) {
            log.error("下载模板失败", e);
        }
    }

    @GetMapping
    public List<TermResp> getTerms(@RequestParam("domainId") Long domainId,
            @RequestParam(name = "queryKey", required = false) String queryKey) {
        return termService.getTerms(domainId, queryKey);
    }

    @Deprecated
    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable("id") Long id) {
        termService.delete(id);
        return true;
    }

    @PostMapping("/deleteBatch")
    public boolean deleteBatch(@RequestBody MetaBatchReq metaBatchReq) {
        termService.deleteBatch(metaBatchReq);
        return true;
    }
}
