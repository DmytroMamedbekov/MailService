import java.util.Map;

public interface MailService {
    void sendEmail(String senderEmail, String subject, String content, String[] recepients);

    void sendEmailWithAttachments(String senderEmail, String subject, String content, String[] recepients, Map<String, String> attachments);
}