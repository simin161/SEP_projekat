package rs.sep.parking.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "webhook_inbox")
public class WebhookInbox {

    @Id
    private String eventId;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String pspPaymentId;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant receivedAt;

    private Instant processedAt;
}
