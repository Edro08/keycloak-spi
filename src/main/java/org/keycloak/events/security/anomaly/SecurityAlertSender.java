package org.keycloak.events.security.anomaly;

/**
 * Capa de salida desacoplada para el despacho de alertas de incidentes de seguridad.
 * <p>
 * Diseñada de forma análoga a la capa de envío de correos (SendEmail): por defecto
 * emite un banner ASCII en consola (stdout) para su visualización inmediata en laboratorios
 * o monitores de logs, pero está concebida para que cada quien la adapte a lo que requiera
 * (Webhook HTTP, Slack, Discord, PagerDuty, colas RabbitMQ/Kafka, AWS SNS, etc.).
 */
public class SecurityAlertSender {

    public static void send(SecurityAlertPayload payload) {
        String alertType = payload.getEventType() != null ? payload.getEventType() : "SECURITY_INCIDENT";
        String ip = payload.getIpAddress() != null ? payload.getIpAddress() : "UNKNOWN";
        String realm = payload.getRealm() != null ? payload.getRealm() : "master";
        String clientId = payload.getClientId() != null ? payload.getClientId() : "N/A";
        String user = payload.getUser() != null ? payload.getUser() : "N/A";
        String detailsMsg = payload.getDetails() != null ? payload.getDetails().get("details") : "";

        System.out.println("=========================================================================================");
        System.out.println(">>> [KEYCLOAK SPI - SECURITY ANOMALY ALERT]");
        System.out.println(">>> Alert Type : " + alertType);
        System.out.println(">>> Realm      : " + realm);
        System.out.println(">>> Client ID  : " + clientId);
        System.out.println(">>> Source IP  : " + ip);
        System.out.println(">>> Last User  : " + user);
        if (detailsMsg != null && !detailsMsg.isEmpty()) {
            System.out.println(">>> Message    : " + detailsMsg);
        }
        System.out.println("=========================================================================================");
    }
}
