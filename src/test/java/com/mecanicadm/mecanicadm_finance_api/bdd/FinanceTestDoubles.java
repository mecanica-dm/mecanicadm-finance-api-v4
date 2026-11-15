package com.mecanicadm.mecanicadm_finance_api.bdd;

import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.CardData;
import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.MercadoPagoOrder;
import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.MercadoPagoOrderData;
import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.port.MercadoPagoCardTokenGateway;
import com.mecanicadm.mecanicadm_finance_api.core.payment.domain.port.MercadoPagoOrderGateway;
import com.mecanicadm.mecanicadm_finance_api.core.shared.domain.port.EmailAttachment;
import com.mecanicadm.mecanicadm_finance_api.core.shared.domain.port.EmailService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@TestConfiguration
public class FinanceTestDoubles {

    @Bean
    @Primary
    public FakeMercadoPago fakeMercadoPago() {
        return new FakeMercadoPago();
    }

    @Bean
    @Primary
    public MercadoPagoCardTokenGateway fakeCardTokenGateway() {
        return (CardData card, String publicKey) -> "card-token-fake";
    }

    @Bean
    @Primary
    public FakeEmailService fakeEmailService() {
        return new FakeEmailService();
    }

    public static class FakeMercadoPago implements MercadoPagoOrderGateway {

        private final AtomicInteger sequence = new AtomicInteger();
        private final Map<String, MercadoPagoOrder> orders = new ConcurrentHashMap<>();
        private volatile String statusOnCreate = "processed";
        private volatile boolean failOnCreate;

        public void reset() {
            orders.clear();
            statusOnCreate = "processed";
            failOnCreate = false;
        }

        public void createOrdersAs(String status) {
            this.statusOnCreate = status;
        }

        public void failOnCreate() {
            this.failOnCreate = true;
        }

        public void changeStatus(String orderId, String status) {
            orders.put(orderId, order(orderId, orders.get(orderId).getExternalReference(), status));
        }

        public List<String> orderIds() {
            return new ArrayList<>(orders.keySet());
        }

        @Override
        public MercadoPagoOrder createOrder(MercadoPagoOrderData data, String accessToken, String idempotencyKey) {
            if (failOnCreate) {
                throw new IllegalStateException("payment_method_not_allowed_for_collector");
            }
            String id = "ORD-" + sequence.incrementAndGet();
            MercadoPagoOrder order = order(id, data.externalReference(), statusOnCreate);
            orders.put(id, order);
            return order;
        }

        @Override
        public MercadoPagoOrder getOrder(String orderId, String accessToken) {
            return orders.get(orderId);
        }

        @Override
        public MercadoPagoOrder cancelOrder(String orderId, String accessToken, String idempotencyKey) {
            MercadoPagoOrder order = order(orderId, orders.get(orderId).getExternalReference(), "canceled");
            orders.put(orderId, order);
            return order;
        }

        @Override
        public MercadoPagoOrder refundOrder(String orderId, String accessToken, String idempotencyKey) {
            MercadoPagoOrder order = order(orderId, orders.get(orderId).getExternalReference(), "refunded");
            orders.put(orderId, order);
            return order;
        }

        private static MercadoPagoOrder order(String id, String externalReference, String status) {
            MercadoPagoOrder.PaymentMethod method = new MercadoPagoOrder.PaymentMethod();
            method.setId("visa");
            method.setTicketUrl("https://pagamento.test/" + id);
            MercadoPagoOrder.Payment payment = new MercadoPagoOrder.Payment();
            payment.setId("PAY-" + id);
            payment.setStatus(status);
            payment.setPaymentMethod(method);
            MercadoPagoOrder.Transactions transactions = new MercadoPagoOrder.Transactions();
            transactions.setPayments(List.of(payment));
            MercadoPagoOrder order = new MercadoPagoOrder();
            order.setId(id);
            order.setExternalReference(externalReference);
            order.setStatus(status);
            order.setTransactions(transactions);
            return order;
        }
    }

    public static class FakeEmailService implements EmailService {

        private final List<SentEmail> sent = new ArrayList<>();

        public void reset() {
            sent.clear();
        }

        public List<SentEmail> sent() {
            return sent;
        }

        @Override
        public void send(String to, String subject, String htmlBody, EmailAttachment attachment) {
            sent.add(new SentEmail(to, subject, htmlBody));
        }

        public record SentEmail(String to, String subject, String htmlBody) {
        }
    }
}
