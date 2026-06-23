package com.yanxiaomap.compliance;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 隐私政策控制器
 * 功能：
 * 1. 提供隐私政策API
 * 2. 提供隐私政策HTML页面
 * 3. 管理用户同意机制
 */
@Slf4j
@RestController
@RequestMapping("/api/compliance/privacy")
@Api(tags = "隐私政策接口")
public class PrivacyPolicyController {

    // 隐私政策版本
    @Value("${privacy-policy.version:1.0}")
    private String privacyVersion;

    // 生效日期
    @Value("${privacy-policy.effective-date:2026-04-18}")
    private String effectiveDate;

    // 最后更新日期
    @Value("${privacy-policy.last-updated:2026-04-18}")
    private String lastUpdated;

    // 公司名称
    @Value("${compliance.company-name:燕小图}")
    private String companyName;

    // 网站名称
    @Value("${compliance.site-name:燕小图-院校专业地图平台}")
    private String siteName;

    // 域名
    @Value("${compliance.domain:yanxiaomap.com}")
    private String domain;

    // 联系邮箱
    @Value("${compliance.contact-email:privacy@yanxiaomap.com}")
    private String contactEmail;

    /**
     * 获取隐私政策内容（JSON格式）
     */
    @GetMapping("/policy")
    @ApiOperation(value = "获取隐私政策", notes = "返回隐私政策的详细内容")
    public Map<String, Object> getPrivacyPolicy() {
        log.debug("获取隐私政策请求");
        
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> policy = new HashMap<>();
        
        policy.put("title", "隐私政策");
        policy.put("version", privacyVersion);
        policy.put("effectiveDate", effectiveDate);
        policy.put("lastUpdated", lastUpdated);
        policy.put("companyName", companyName);
        policy.put("siteName", siteName);
        policy.put("domain", domain);
        
        // 隐私政策正文
        Map<String, String> sections = new HashMap<>();
        
        sections.put("introduction", getIntroductionSection());
        sections.put("dataCollection", getDataCollectionSection());
        sections.put("dataUsage", getDataUsageSection());
        sections.put("dataSharing", getDataSharingSection());
        sections.put("dataSecurity", getDataSecuritySection());
        sections.put("userRights", getUserRightsSection());
        sections.put("cookies", getCookiesSection());
        sections.put("childrenPrivacy", getChildrenPrivacySection());
        sections.put("policyChanges", getPolicyChangesSection());
        sections.put("contact", getContactSection());
        
        policy.put("sections", sections);
        
        result.put("success", true);
        result.put("code", "SUCCESS");
        result.put("message", "隐私政策获取成功");
        result.put("data", policy);
        
        return result;
    }

    /**
     * 获取隐私政策HTML页面
     */
    @GetMapping(value = "/html", produces = MediaType.TEXT_HTML_VALUE)
    @ApiOperation(value = "获取隐私政策HTML页面", notes = "返回格式化的隐私政策HTML页面")
    public String getPrivacyPolicyHtml() {
        log.debug("获取隐私政策HTML页面请求");
        
        try {
            // 尝试从模板文件加载
            Resource resource = new ClassPathResource("templates/compliance/privacy-policy.html");
            if (resource.exists()) {
                String html = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
                return html;
            }
        } catch (IOException e) {
            log.debug("未找到隐私政策模板文件，使用默认生成: {}", e.getMessage());
        }
        
        // 如果模板文件不存在，动态生成HTML
        return generatePrivacyPolicyHtml();
    }

    /**
     * 获取隐私政策摘要（供用户同意时显示）
     */
    @GetMapping("/summary")
    @ApiOperation(value = "获取隐私政策摘要", notes = "返回隐私政策的简要说明，用于用户同意界面")
    public Map<String, Object> getPrivacySummary() {
        log.debug("获取隐私政策摘要请求");
        
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> summary = new HashMap<>();
        
        summary.put("title", "隐私政策摘要");
        summary.put("version", privacyVersion);
        summary.put("lastUpdated", lastUpdated);
        
        StringBuilder content = new StringBuilder();
        content.append("欢迎使用").append(siteName).append("。我们非常重视您的隐私保护。\n\n");
        content.append("本隐私政策说明我们如何收集、使用、存储和保护您的个人信息。\n\n");
        content.append("主要要点：\n");
        content.append("1. 我们收集的信息：账号信息、使用数据、设备信息等\n");
        content.append("2. 信息用途：提供服务、改进产品、安全保障\n");
        content.append("3. 信息共享：仅在必要时与合作伙伴共享\n");
        content.append("4. 您的权利：访问、更正、删除您的个人信息\n");
        content.append("5. 安全保障：采取技术和管理措施保护您的信息\n\n");
        content.append("继续使用我们的服务即表示您同意本隐私政策。\n");
        content.append("如需了解更多详情，请查看完整的隐私政策。");
        
        summary.put("content", content.toString());
        
        result.put("success", true);
        result.put("code", "SUCCESS");
        result.put("message", "隐私政策摘要获取成功");
        result.put("data", summary);
        
        return result;
    }

    /**
     * 检查隐私政策版本（供前端检查更新）
     */
    @GetMapping("/check-version")
    @ApiOperation(value = "检查隐私政策版本", notes = "检查隐私政策是否有更新")
    public Map<String, Object> checkPrivacyVersion(
            @io.swagger.annotations.ApiParam(value = "客户端当前版本", defaultValue = "1.0") 
            String clientVersion) {
        
        log.debug("检查隐私政策版本请求: clientVersion={}", clientVersion);
        
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> versionInfo = new HashMap<>();
        
        versionInfo.put("currentVersion", privacyVersion);
        versionInfo.put("lastUpdated", lastUpdated);
        versionInfo.put("clientVersion", clientVersion);
        
        boolean isUpdated = !privacyVersion.equals(clientVersion);
        versionInfo.put("isUpdated", isUpdated);
        
        if (isUpdated) {
            versionInfo.put("updateMessage", "隐私政策有更新，请查看最新版本");
            
            // 简单比较版本号（这里使用简单的字符串比较，实际应该使用版本号解析）
            try {
                double current = Double.parseDouble(privacyVersion);
                double client = Double.parseDouble(clientVersion);
                versionInfo.put("updateType", current > client ? "MAJOR" : "MINOR");
            } catch (NumberFormatException e) {
                versionInfo.put("updateType", "UNKNOWN");
            }
        } else {
            versionInfo.put("updateMessage", "隐私政策已是最新版本");
        }
        
        result.put("success", true);
        result.put("code", "SUCCESS");
        result.put("message", isUpdated ? "隐私政策有更新" : "隐私政策已是最新");
        result.put("data", versionInfo);
        
        return result;
    }

    /**
     * 记录用户同意（模拟接口）
     */
    @GetMapping("/consent")
    @ApiOperation(value = "记录用户同意", notes = "记录用户对隐私政策的同意")
    public Map<String, Object> recordConsent(
            @io.swagger.annotations.ApiParam(value = "用户ID", required = false) 
            String userId,
            @io.swagger.annotations.ApiParam(value = "同意版本", defaultValue = "1.0") 
            String consentVersion) {
        
        log.info("记录用户隐私政策同意: userId={}, version={}", userId, consentVersion);
        
        Map<String, Object> result = new HashMap<>();
        
        // 这里应该实际记录到数据库
        // 简化实现，只返回成功
        
        Map<String, Object> consentRecord = new HashMap<>();
        consentRecord.put("userId", userId);
        consentRecord.put("consentVersion", consentVersion);
        consentRecord.put("consentTime", LocalDate.now().format(DateTimeFormatter.ISO_DATE));
        consentRecord.put("ipAddress", "记录中..."); // 实际应从请求中获取
        
        result.put("success", true);
        result.put("code", "SUCCESS");
        result.put("message", "隐私政策同意已记录");
        result.put("data", consentRecord);
        
        return result;
    }

    /**
     * 生成隐私政策HTML
     */
    private String generatePrivacyPolicyHtml() {
        StringBuilder html = new StringBuilder();
        
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"zh-CN\">\n");
        html.append("<head>\n");
        html.append("    <meta charset=\"UTF-8\">\n");
        html.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("    <title>隐私政策 - ").append(siteName).append("</title>\n");
        html.append("    <style>\n");
        html.append("        body { font-family: Arial, sans-serif; line-height: 1.6; max-width: 800px; margin: 0 auto; padding: 20px; }\n");
        html.append("        h1 { color: #333; border-bottom: 2px solid #4CAF50; padding-bottom: 10px; }\n");
        html.append("        h2 { color: #555; margin-top: 30px; }\n");
        html.append("        h3 { color: #666; }\n");
        html.append("        .version { color: #777; font-size: 14px; margin-bottom: 20px; }\n");
        html.append("        .section { margin-bottom: 20px; }\n");
        html.append("        .highlight { background-color: #f9f9f9; padding: 10px; border-left: 3px solid #4CAF50; margin: 10px 0; }\n");
        html.append("        .contact-info { background-color: #e8f5e9; padding: 15px; border-radius: 5px; margin: 20px 0; }\n");
        html.append("        .footer { margin-top: 40px; padding-top: 20px; border-top: 1px solid #ddd; color: #777; font-size: 12px; }\n");
        html.append("    </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        
        html.append("    <h1>隐私政策</h1>\n");
        html.append("    <div class=\"version\">\n");
        html.append("        版本: ").append(privacyVersion).append(" | \n");
        html.append("        生效日期: ").append(effectiveDate).append(" | \n");
        html.append("        最后更新: ").append(lastUpdated).append("\n");
        html.append("    </div>\n");
        
        html.append("    <div class=\"highlight\">\n");
        html.append("        <p><strong>重要提示：</strong>请仔细阅读本隐私政策。使用我们的服务即表示您同意本隐私政策的条款。</p>\n");
        html.append("    </div>\n");
        
        html.append("    <div class=\"section\">\n");
        html.append("        <h2>1. 引言</h2>\n");
        html.append("        <p>").append(getIntroductionSection()).append("</p>\n");
        html.append("    </div>\n");
        
        html.append("    <div class=\"section\">\n");
        html.append("        <h2>2. 信息收集</h2>\n");
        html.append("        <p>").append(getDataCollectionSection()).append("</p>\n");
        html.append("    </div>\n");
        
        html.append("    <div class=\"section\">\n");
        html.append("        <h2>3. 信息使用</h2>\n");
        html.append("        <p>").append(getDataUsageSection()).append("</p>\n");
        html.append("    </div>\n");
        
        html.append("    <div class=\"section\">\n");
        html.append("        <h2>4. 信息共享</h2>\n");
        html.append("        <p>").append(getDataSharingSection()).append("</p>\n");
        html.append("    </div>\n");
        
        html.append("    <div class=\"section\">\n");
        html.append("        <h2>5. 信息安全</h2>\n");
        html.append("        <p>").append(getDataSecuritySection()).append("</p>\n");
        html.append("    </div>\n");
        
        html.append("    <div class=\"section\">\n");
        html.append("        <h2>6. 您的权利</h2>\n");
        html.append("        <p>").append(getUserRightsSection()).append("</p>\n");
        html.append("    </div>\n");
        
        html.append("    <div class=\"section\">\n");
        html.append("        <h2>7. Cookie政策</h2>\n");
        html.append("        <p>").append(getCookiesSection()).append("</p>\n");
        html.append("    </div>\n");
        
        html.append("    <div class=\"section\">\n");
        html.append("        <h2>8. 儿童隐私</h2>\n");
        html.append("        <p>").append(getChildrenPrivacySection()).append("</p>\n");
        html.append("    </div>\n");
        
        html.append("    <div class=\"section\">\n");
        html.append("        <h2>9. 政策变更</h2>\n");
        html.append("        <p>").append(getPolicyChangesSection()).append("</p>\n");
        html.append("    </div>\n");
        
        html.append("    <div class=\"contact-info\">\n");
        html.append("        <h2>10. 联系我们</h2>\n");
        html.append("        <p>").append(getContactSection()).append("</p>\n");
        html.append("    </div>\n");
        
        html.append("    <div class=\"footer\">\n");
        html.append("        <p>").append(companyName).append(" 版权所有 © 2026</p>\n");
        html.append("        <p>本隐私政策最后更新于：").append(lastUpdated).append("</p>\n");
        html.append("    </div>\n");
        
        html.append("</body>\n");
        html.append("</html>\n");
        
        return html.toString();
    }

    /**
     * 引言部分
     */
    private String getIntroductionSection() {
        return "欢迎使用" + siteName + "。我们深知个人信息对您的重要性，并会尽全力保护您的个人信息安全可靠。本隐私政策旨在说明我们如何收集、使用、存储和保护您的个人信息，以及您对个人信息享有的权利。请在使用我们的服务前，仔细阅读并了解本隐私政策。";
    }

    /**
     * 数据收集部分
     */
    private String getDataCollectionSection() {
        return "我们可能收集以下类型的个人信息：\n" +
               "1. 账号信息：当您注册账号时，我们收集您的用户名、邮箱地址、密码等信息。\n" +
               "2. 使用数据：我们记录您使用服务时的行为数据，如搜索记录、浏览记录、点击记录等。\n" +
               "3. 设备信息：我们收集您访问服务时使用的设备信息，如设备型号、操作系统、浏览器类型、IP地址等。\n" +
               "4. 位置信息：在您授权的情况下，我们可能收集您的地理位置信息，以提供基于位置的服务。\n" +
               "5. 日志信息：我们自动收集服务器日志，包括访问时间、访问页面、请求来源等。";
    }

    /**
     * 数据使用部分
     */
    private String getDataUsageSection() {
        return "我们可能将收集的信息用于以下目的：\n" +
               "1. 提供服务：提供、维护和改进我们的服务。\n" +
               "2. 个性化体验：根据您的偏好和兴趣，提供个性化的内容推荐。\n" +
               "3. 安全保障：检测、防范和应对欺诈、滥用、安全风险和技术问题。\n" +
               "4. 沟通联系：向您发送服务通知、更新、安全警报和支持消息。\n" +
               "5. 合规要求：遵守法律法规和监管要求。";
    }

    /**
     * 数据共享部分
     */
    private String getDataSharingSection() {
        return "我们不会出售您的个人信息。在以下情况下，我们可能与第三方共享您的信息：\n" +
               "1. 服务提供商：与为我们提供服务的合作伙伴共享必要信息，如云服务提供商、分析服务商等。\n" +
               "2. 法律要求：为遵守法律法规、法院命令或其他法律程序的要求。\n" +
               "3. 保护权利：为保护我们的权利、财产或安全，或保护其他用户或公众的权利、财产或安全。\n" +
               "4. 业务转让：在公司合并、收购或资产转让的情况下，个人信息可能作为转让资产的一部分。";
    }

    /**
     * 数据安全部分
     */
    private String getDataSecuritySection() {
        return "我们采取合理的技术和组织措施保护您的个人信息安全，包括：\n" +
               "1. 加密技术：对敏感信息进行加密存储和传输。\n" +
               "2. 访问控制：严格限制对个人信息的访问权限。\n" +
               "3. 安全审计：定期进行安全审计和漏洞扫描。\n" +
               "4. 员工培训：对处理个人信息的员工进行安全培训。\n" +
               "5. 应急响应：建立安全事件应急响应机制。";
    }

    /**
     * 用户权利部分
     */
    private String getUserRightsSection() {
        return "您对个人信息享有以下权利：\n" +
               "1. 访问权：有权访问我们持有的关于您的个人信息。\n" +
               "2. 更正权：有权更正不准确或不完整的个人信息。\n" +
               "3. 删除权：在特定情况下，有权要求删除您的个人信息。\n" +
               "4. 限制处理权：在特定情况下，有权限制我们对您个人信息的处理。\n" +
               "5. 数据可携权：有权以结构化、常用且机器可读的格式获取您的个人信息。\n" +
               "6. 反对权：有权反对基于特定目的处理您的个人信息。";
    }

    /**
     * Cookie政策部分
     */
    private String getCookiesSection() {
        return "我们使用Cookie和类似技术来改善您的用户体验。Cookie是存储在您设备上的小文本文件，用于记录偏好设置和登录状态等。您可以通过浏览器设置管理或禁用Cookie，但这可能会影响部分功能的使用。";
    }

    /**
     * 儿童隐私部分
     */
    private String getChildrenPrivacySection() {
        return "我们的服务不面向儿童。如果我们发现无意中收集了儿童的个人信息，我们将尽快删除这些信息。如果您是儿童的监护人并认为我们可能持有儿童的信息，请联系我们。";
    }

    /**
     * 政策变更部分
     */
    private String getPolicyChangesSection() {
        return "我们可能会不时更新本隐私政策。更新后的隐私政策将在网站上公布，并更新生效日期。对于重大变更，我们将通过显著方式通知您。建议您定期查看本隐私政策以了解最新信息。";
    }

    /**
     * 联系部分
     */
    private String getContactSection() {
        return "如果您对本隐私政策有任何疑问、意见或建议，或者需要行使您的个人信息权利，请通过以下方式联系我们：\n" +
               "电子邮箱：" + contactEmail + "\n" +
               "我们将尽快回复您的请求。";
    }
}