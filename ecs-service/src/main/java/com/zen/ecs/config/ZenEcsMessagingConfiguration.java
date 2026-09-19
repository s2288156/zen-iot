package com.zen.ecs.config;

import com.zen.ecs.dto.RcsCommandMessage;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RCS → ECS 指令通道的拓扑声明：这三个 Bean 由 {@code RabbitAdmin} 在连接建立时自动声明，不需要运维手工建队列。
 *
 * <p>名字全部来自 {@link ZenEcsProperties.Messaging}，换环境只改配置不改代码。
 */
@Configuration(proxyBeanMethods = false)
public class ZenEcsMessagingConfiguration {

    @Bean
    TopicExchange rcsCommandExchange(ZenEcsProperties properties) {
        return new TopicExchange(properties.getMessaging().getExchange(), true, false);
    }

    @Bean
    Queue rcsCommandQueue(ZenEcsProperties properties) {
        return QueueBuilder.durable(properties.getMessaging().getQueue()).build();
    }

    @Bean
    Binding rcsCommandBinding(TopicExchange rcsCommandExchange, Queue rcsCommandQueue, ZenEcsProperties properties) {
        return BindingBuilder.bind(rcsCommandQueue)
                .to(rcsCommandExchange)
                .with(properties.getMessaging().getRoutingKey());
    }

    /**
     * 消息体是 JSON，因此 {@code __TypeId__} 头会被写进消息；反序列化时能据它实例化类，所以必须限定可信包，
     * 否则一条外部消息就能让服务端构造任意类的对象（反序列化 gadget）。这里只放行本服务收的这一种消息。
     *
     * <p>注册成 Bean 即可：Boot 的 Rabbit 自动配置会把它同时装到 {@code RabbitTemplate} 和监听容器工厂上，
     * 因此不需要自己建工厂。
     */
    @Bean
    MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter(RcsCommandMessage.class.getPackageName());
    }
}
