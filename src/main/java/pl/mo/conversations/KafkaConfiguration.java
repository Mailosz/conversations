package pl.mo.conversations;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.common.serialization.UUIDDeserializer;
import org.apache.kafka.common.serialization.UUIDSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.messaging.Message;
import org.springframework.data.mongodb.core.messaging.MessageListener;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.GenericMessageListener;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;

import jakarta.annotation.PostConstruct;
import pl.mo.conversations.services.SseService;


@Configuration
@EnableKafka
public class KafkaConfiguration {

    @Bean
    KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<UUID, String>>kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<UUID, String> factory =
        new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(3);
        factory.getContainerProperties().setPollTimeout(3000);
        return factory;
    }

    @Bean
    public ConsumerFactory<UUID, String> consumerFactory() {
        return new DefaultKafkaConsumerFactory<>(consumerConfigs());
    }
    
    @Bean
    public Map<String, Object> consumerConfigs() { 
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, UUIDDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        // See https://kafka.apache.org/documentation/#producerconfigs for more properties
        return props;
    }
    
    @Bean
    public ProducerFactory<UUID, String> producerFactory() {
        return new DefaultKafkaProducerFactory<>(producerConfigs());
    }
    
    @Bean
    public Map<String, Object> producerConfigs() { 
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, UUIDSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        // See https://kafka.apache.org/documentation/#producerconfigs for more properties
        return props;
    }

    @Bean
    public KafkaTemplate<UUID, String> kafkaTemplate() {
        var template = new KafkaTemplate<UUID, String>(producerFactory());
        template.setConsumerFactory(consumerFactory());
        return template;
    }

    
    class Listener implements MessageListener<UUID, String>, GenericMessageListener<String> {

        @Override
        public void onMessage(Message<UUID, String> message) {
            System.out.println("!!!!!!!!!!!!1");
            System.out.println(message.getBody());
        }

        @Override
        public void onMessage(String message) {
            System.out.println("!!!!!!!!!!!!1");
            System.out.println(message);
        }

    }

    @Autowired
    SseService sseService;

    @KafkaListener(id = "conversationsListener", topicPattern = ".*", clientIdPrefix = "conversationClient", properties = "metadata.max.age.ms:60000", autoStartup = "true")
    public void listen(ConsumerRecord<UUID,String> record) {
        sseService.sendEvents(record.topic(), record.key(), record.value()); 
    }
    
    // @Bean
    // public KafkaMessageListenerContainer<UUID, String> listenerContainer() {

    //     var containerProperties = new ContainerProperties("conversation1");
    //     containerProperties.setClientId("clientEin");
    //     containerProperties.setGroupId("groupEin");

    //     containerProperties.setMessageListener(new MessageListener<UUID, String>() {
    //         @Override
    //         public void onMessage(Message<UUID, String> message) {
    //             System.out.println("!!!!!!!!!!!!1");
    //             System.out.println(message.getBody());
    //         }
    //     });

    //     var listenerContainer = new KafkaMessageListenerContainer<UUID, String>(consumerFactory(), containerProperties);
    //     return listenerContainer;


    // }
}
