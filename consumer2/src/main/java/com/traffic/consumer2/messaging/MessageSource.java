package com.traffic.consumer2.messaging;

import com.traffic.consumer2.model.TrafficSignMessage;

/**
 * Abstração da fonte de mensagens.
 *
 * Implementações:
 *   - {@link MockMessageSource}     – gerador interno, não precisa de infra
 *   - {@link RabbitMQMessageSource} – consumidor real de RabbitMQ
 *
 * Para trocar o modo basta alterar a variável de ambiente USE_MOCK.
 * Nenhum outro código precisa mudar.
 */
public interface MessageSource extends AutoCloseable {

    /**
     * Retorna a próxima mensagem disponível.
     * Bloqueia até que uma mensagem esteja disponível ou o serviço seja encerrado.
     *
     * @return mensagem ou {@code null} se a fonte foi encerrada
     * @throws InterruptedException se a thread for interrompida durante a espera
     */
    TrafficSignMessage nextMessage() throws InterruptedException;

    /**
     * Confirma (ACK) o processamento bem-sucedido de uma mensagem.
     * Em modo mock isso é um no-op; no RabbitMQ faz o basicAck.
     */
    void acknowledge(TrafficSignMessage message);

    /**
     * Rejeita uma mensagem (NACK) sem requeue.
     * Em modo mock isso é um no-op.
     */
    void reject(TrafficSignMessage message);

    /** Retorna uma descrição legível da fonte (para logs). */
    String describe();
}
