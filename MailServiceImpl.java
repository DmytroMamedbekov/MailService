import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

@Component
@Slf4j
public class MailServiceImpl implements MailService {

    private JavaMailSender mailSender;

    //In case you are implementing this module into real project, these parameters best be extracted into spring properties
    final String username;
    final String password;

    final String host;
    final int port;

    @PostConstruct
    private void initializeMailClient() {
        JavaMailSenderImpl mailSenderImpl = new JavaMailSenderImpl();
        mailSenderImpl.setHost(host);
        mailSenderImpl.setPort(port);

        mailSenderImpl.setUsername(username);
        mailSenderImpl.setPassword(password);

        Properties props = mailSenderImpl.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.debug", "true");

        mailSender = mailSenderImpl;
    }

    @Override
    public void sendEmail(String senderEmail, String subject, String content, String[] recipients) {
        try {
            MimeMessage message = prepareMessage(senderEmail, subject, content, recipients);
            MimeMessageHelper messageHelper = new MimeMessageHelper(message, "UTF-8");

            messageHelper.setFrom(senderEmail);

            log.info("Sending mail to: {}", String.join(", ", recipients));
            mailSender.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
            log.warn("Failed to send email.");
        }
    }

    @Override
    public void sendEmailWithAttachments(String senderEmail, String subject, String content, String[] recipients, Map<String, String> attachments) {
        try {
            MimeMessage message = prepareMessageWithoutContent(senderEmail, subject, recipients);

            MimeMultipart multipart = new MimeMultipart();

            MimeBodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setContent(content, "text/html");
            multipart.addBodyPart(messageBodyPart);

            //for each attachment we are creating a file with string, attaching it and then deleting it.
            for (Map.Entry<String, String> attachment : attachments.entrySet()
            ) {
                MimeBodyPart mimeAttachment = new MimeBodyPart();

                File attachmentFile = createFile(attachment.getKey());
                writeToFile(attachment.getKey(), attachment.getValue());
                mimeAttachment.attachFile(attachmentFile);

                multipart.addBodyPart(mimeAttachment);
            }

            message.setContent(multipart);

            log.info("Sending mail to: {}", String.join(", ", recipients));
            mailSender.send(message);

            for (Map.Entry<String, String> attachment : attachments.entrySet()
            ) {
                deleteFile(attachment.getKey());
            }

        } catch (MessagingException | IOException e) {
            e.printStackTrace();
            log.warn("Failed to send email.");
        }
    }

    private MimeMessage prepareMessage(String senderEmail, String subject, String content, String[] recipients) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message);
        helper.setFrom(new InternetAddress(senderEmail));
        helper.setSubject(subject);
        helper.setText(content);

        for (String developerEmail : recipients
        ) {
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(developerEmail));
        }

        return message;
    }

    private MimeMessage prepareMessageWithoutContent(String senderEmail, String subject, String[] recipients) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message);
        helper.setFrom(new InternetAddress(senderEmail));
        helper.setSubject(subject);

        for (String developerEmail : recipients
        ) {
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(developerEmail));
        }

        return message;
    }

    private File createFile(String fileName) {
        File newFile = null;
        try {
            newFile = new File(fileName);
            if (newFile.createNewFile()) {
                log.info("File created: " + newFile.getName());
            } else {
                log.error("File "+newFile.getName()+" already exists.");
            }
        } catch (
                IOException e) {
            e.printStackTrace();
        }
        return newFile;
    }

    private void writeToFile(String fileName, String text) {
        try {
            FileWriter fileWriter = new FileWriter(fileName);
            fileWriter.write(text);
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void deleteFile(String fileName) {
        log.info("Deleting file {}", fileName);
        try {
            File f = new File(fileName);
            if (!f.delete()) {
                log.error("File {} wasn't deleted!", fileName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }