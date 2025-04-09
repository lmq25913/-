package com.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import jakarta.activation.*;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.io.File;

@RestController
@RequestMapping("/push")
public class PushController {
    
    private static final Logger logger = Logger.getLogger(PushController.class.getName());
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    @RequestMapping(value = "/dataPush", produces = "text/plain;charset=UTF-8")
    public String dataPush(
            @RequestParam("email") String email,
            @RequestParam(value = "fileType", required = false) String[] fileTypes,
            @RequestParam(value = "dataType", required = false) String[] dataTypes,
            @RequestParam(value = "subject", required = false, defaultValue = "推送测试") String subject,
            @RequestParam(value = "content", required = false) String content,
            @RequestParam(value = "imagePath", required = false) String imagePath) {
        
        // 验证邮箱地址
        if (email == null || email.isEmpty()) {
            return "错误：邮箱地址不能为空";
        }
        
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return "错误：邮箱地址格式不正确\n" +
                   "请使用真实的邮箱地址，例如：\n" +
                   "- QQ邮箱：xxx@qq.com\n" +
                   "- 163邮箱：xxx@163.com\n" +
                   "- Gmail：xxx@gmail.com";
        }
        
        // 检查是否是示例邮箱
        if (email.toLowerCase().contains("example.com")) {
            return "错误：请使用真实的邮箱地址，不要使用示例邮箱（example.com）";
        }
        
        logger.info("开始发送邮件到: " + email);
        logger.info("文件类型: " + (fileTypes != null ? String.join(",", fileTypes) : "null"));
        logger.info("数据类型: " + (dataTypes != null ? String.join(",", dataTypes) : "null"));
        logger.info("邮件主题: " + subject);
        logger.info("邮件内容: " + content);
        logger.info("图片路径: " + imagePath);
        
        try {
            // 配置邮件服务器
            Properties props = new Properties();
            props.put("mail.smtp.host", "smtp.qq.com");
            props.put("mail.smtp.port", "465");
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.socketFactory.port", "465");
            props.put("mail.smtp.socketFactory.fallback", "false");
            props.put("mail.smtp.connectiontimeout", "30000");
            props.put("mail.smtp.timeout", "30000");
            props.put("mail.smtp.writetimeout", "30000");
            
            // 创建会话
            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication("2320686689@qq.com", "trkkzefldoqxdhge");
                }
            });
            
            // 设置调试模式
            session.setDebug(true);
            
            // 创建邮件
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress("2320686689@qq.com", "CYC"));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(email));
            message.setSubject(subject);
            
            // 创建多部分邮件
            Multipart multipart = new MimeMultipart();
            
            // 创建文本部分
            MimeBodyPart textPart = new MimeBodyPart();
            StringBuilder emailContent = new StringBuilder();
            
            // 如果有自定义内容，优先使用自定义内容
            if (content != null && !content.isEmpty()) {
                emailContent.append(content);
            } else {
                // 否则使用默认的内容生成逻辑
                if (fileTypes != null) {
                    for (String type : fileTypes) {
                        if ("csv".equals(type)) {
                            emailContent.append("推送csv文件;");
                        } else if ("NC".equals(type)) {
                            emailContent.append("推送NC文件;");
                        }
                    }
                }
                
                if (dataTypes != null) {
                    for (String type : dataTypes) {
                        if ("1".equals(type)) {
                            emailContent.append("短帧数据;");
                        } else if ("2".equals(type)) {
                            emailContent.append("长帧数据;");
                        } else if ("3".equals(type)) {
                            emailContent.append("任务帧数据;");
                        }
                    }
                }
            }
            
            textPart.setText(emailContent.toString());
            multipart.addBodyPart(textPart);
            
            // 如果有图片路径，添加图片附件
            if (imagePath != null && !imagePath.isEmpty()) {
                File imageFile = new File(imagePath);
                if (imageFile.exists()) {
                    MimeBodyPart imagePart = new MimeBodyPart();
                    DataSource source = new FileDataSource(imageFile);
                    imagePart.setDataHandler(new DataHandler(source));
                    imagePart.setFileName(imageFile.getName());
                    multipart.addBodyPart(imagePart);
                    logger.info("添加图片附件: " + imagePath);
                } else {
                    logger.warning("图片文件不存在: " + imagePath);
                }
            }
            
            // 设置邮件内容
            message.setContent(multipart);
            
            // 发送邮件
            Transport.send(message);
            logger.info("邮件发送成功");
            
            return "邮件发送成功";
        } catch (MessagingException e) {
            logger.severe("邮件发送失败: " + e.getMessage());
            e.printStackTrace();
            StringBuilder errorMsg = new StringBuilder();
            errorMsg.append("邮件发送失败: ").append(e.getMessage()).append("\n");
            errorMsg.append("可能的原因：\n");
            errorMsg.append("1. 收件人邮箱地址不存在或格式错误\n");
            errorMsg.append("2. QQ邮箱SMTP服务未开启：请登录QQ邮箱，在设置中开启SMTP服务\n");
            errorMsg.append("3. 授权码错误：请检查授权码是否正确\n");
            errorMsg.append("4. 网络连接问题：请检查是否可以访问 smtp.qq.com\n");
            errorMsg.append("5. 防火墙阻止：请检查防火墙设置\n");
            errorMsg.append("6. 网络代理问题：如果使用代理，请检查代理设置\n");
            errorMsg.append("7. 图片文件不存在或无法访问\n");
            return errorMsg.toString();
        } catch (Exception e) {
            logger.severe("未知错误: " + e.getMessage());
            e.printStackTrace();
            return "未知错误: " + e.getMessage() + "\n请查看服务器日志获取详细信息";
        }
    }
}
