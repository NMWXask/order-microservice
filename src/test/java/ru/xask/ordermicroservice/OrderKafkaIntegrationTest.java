package ru.xask.ordermicroservice; // Объявление пакета

import org.apache.kafka.clients.consumer.Consumer; // Импорт интерфейса Kafka Consumer для чтения сообщений из топиков
import org.apache.kafka.clients.consumer.ConsumerConfig; // Импорт класса с константами конфигурации Kafka Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord; // Импорт класса для представления одного прочитанного сообщения Kafka
import org.apache.kafka.clients.consumer.ConsumerRecords; // Импорт класса для представления пакета прочитанных сообщений Kafka
import org.apache.kafka.common.TopicPartition; // Импорт класса для идентификации раздела (партиции) Kafka топика
import org.apache.kafka.common.serialization.StringDeserializer; // Импорт десериализатора строковых значений Kafka
import org.junit.jupiter.api.BeforeAll; // Импорт аннотации для метода инициализации перед всеми тестами класса
import org.junit.jupiter.api.Test; // Импорт аннотации для обозначения тестового метода
import org.junit.jupiter.api.TestInstance; // Импорт аннотации для настройки жизненного цикла тестового экземпляра
import org.springframework.beans.factory.annotation.Autowired; // Импорт аннотации для автоматического внедрения зависимостей Spring
import org.springframework.boot.test.context.SpringBootTest; // Импорт аннотации для запуска полного Spring Boot контекста в тестах
import org.springframework.kafka.core.DefaultKafkaConsumerFactory; // Импорт фабрики для создания Kafka Consumer по умолчанию
import org.springframework.kafka.test.EmbeddedKafkaBroker; // Импорт встроенного (embedded) Kafka брокера для тестирования
import org.springframework.kafka.test.context.EmbeddedKafka; // Импорт аннотации для автоматической настройки embedded Kafka
import org.springframework.kafka.test.utils.KafkaTestUtils; // Импорт утилитарного класса с вспомогательными методами для Kafka тестов
import org.springframework.test.annotation.DirtiesContext; // Импорт аннотации для сброса Spring контекста после тестов
import org.springframework.test.context.ActiveProfiles; // Импорт аннотации для активации профилей Spring
import org.springframework.transaction.annotation.Transactional; // Импорт аннотации для управления транзакциями в тестах
import ru.xask.ordermicroservice.entity.Order; // Импорт сущности Order — доменной модели заказа
import ru.xask.ordermicroservice.exception.IllegalOrderStatusTransitionException; // Импорт исключения при недопустимом переходе статуса
import ru.xask.ordermicroservice.exception.OrderNotFoundException; // Импорт исключения при отсутствии заказа в базе
import ru.xask.ordermicroservice.repository.OrderRepository; // Импорт репозитория для работы с заказами в БД
import ru.xask.ordermicroservice.service.OrderService; // Импорт сервисного слоя для бизнес-логики заказов

import java.time.Duration; // Импорт класса для представления временного интервала (таймауты)
import java.util.ArrayList; // Импорт динамического массива для хранения записей Kafka
import java.util.Collections; // Импорт утилитарного класса для работы с коллекциями (singletonList, emptySet)
import java.util.List; // Импорт интерфейса списка для типизации коллекций
import java.util.Map; // Импорт интерфейса карты для хранения конфигурационных свойств
import java.util.Set; // Импорт интерфейса множества для работы с партициями

import static org.assertj.core.api.Assertions.assertThat; // Статический импорт метода AssertJ для fluent-ассертов
import static org.assertj.core.api.Assertions.assertThatThrownBy; // Статический импорт метода AssertJ для проверки исключений

/**
 * Интеграционный тест для проверки взаимодействия сервиса заказов с Kafka.
 * Тестирует бизнес-логику переходов статусов заказа и публикацию событий в Kafka.
 */
@DirtiesContext // Аннотация: сбрасывает Spring контекст после выполнения класса тестов, изолируя от других тестов
@TestInstance(TestInstance.Lifecycle.PER_CLASS) // Аннотация: создаёт один экземпляр тестового класса на весь класс (не на метод)
@ActiveProfiles("test") // Аннотация: активирует Spring профиль "test" для загрузки тестовых конфигураций
@EmbeddedKafka( // Аннотация: автоматически запускает встроенный Kafka брокер для изолированного тестирования
        partitions = 1, // Параметр: количество партиций в топике (1 — для упрощения тестов)
        count = 1, // Параметр: количество брокеров Kafka (1 — минимальная конфигурация)
        controlledShutdown = true, // Параметр: корректное завершение работы брокера после тестов
        topics = {"order_events"} // Параметр: список топиков, которые нужно создать перед тестами
)
@SpringBootTest(properties = { // Аннотация: запускает полный Spring Boot контекст с дополнительными свойствами
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}", // Свойство: адрес embedded Kafka брокера
        "spring.kafka.consumer.group-id=order-service-test", // Свойство: идентификатор группы потребителя Kafka
        "spring.kafka.consumer.auto-offset-reset=earliest", // Свойство: чтение сообщений с самого начала (earliest offset)
        "spring.kafka.listener.auto-startup=false" // Свойство: отключение автоматического запуска Kafka listener
})
class OrderKafkaIntegrationTest { // Объявление класса интеграционных тестов заказов с Kafka

    @Autowired // Аннотация: автоматическое внедрение бина OrderService из Spring контекста
    private OrderService orderService; // Поле: сервис для выполнения операций с заказами

    @Autowired // Аннотация: автоматическое внедрение бина OrderRepository из Spring контекста
    private OrderRepository orderRepository; // Поле: репозиторий для CRUD операций с заказами в БД

    @Autowired // Аннотация: автоматическое внедрение бина EmbeddedKafkaBroker из Spring контекста
    private EmbeddedKafkaBroker embeddedKafkaBroker; // Поле: встроенный Kafka брокер для тестирования

    private Consumer<String, String> kafkaConsumer; // Поле: Kafka consumer для чтения строковых сообщений (ключ-значение)
    private static final String TOPIC_NAME = "order_events"; // Константа: имя топика Kafka для событий заказов

    /**
     * Метод инициализации, выполняется один раз перед всеми тестами класса.
     * Настраивает и создаёт Kafka consumer для чтения сообщений из embedded брокера.
     */
    @BeforeAll // Аннотация: метод выполняется один раз перед всеми тестами (требует PER_CLASS lifecycle)
    void setUp() { // Метод: конфигурация и создание Kafka consumer
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps( // Создание карты свойств consumer через утилиту Spring Kafka
                "order-service-test-group", // Аргумент: идентификатор группы consumer для тестов
                "true", // Аргумент: включение авто-коммита offset (строка "true")
                embeddedKafkaBroker // Аргумент: ссылка на embedded брокер для получения адреса
        );
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class); // Добавление: класс десериализатора ключа (String)
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class); // Добавление: класс десериализатора значения (String)
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"); // Добавление: стратегия сброса offset — читать с начала
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-consumer-group"); // Добавление: идентификатор группы consumer
        consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true"); // Добавление: автоматический коммит offset
        consumerProps.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "100"); // Добавление: интервал авто-коммита — 100 мс
        consumerProps.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "100"); // Добавление: максимальное количество записей за poll — 100

        DefaultKafkaConsumerFactory<String, String> consumerFactory = // Объявление фабрики consumer с типизацией <String, String>
                new DefaultKafkaConsumerFactory<>(consumerProps); // Создание фабрики с переданными свойствами

        kafkaConsumer = consumerFactory.createConsumer(); // Создание экземпляра Kafka consumer через фабрику
        kafkaConsumer.subscribe(Collections.singletonList(TOPIC_NAME)); // Подписка consumer на топик order_events (singletonList — список из 1 элемента)

        // Ждем assignment с таймаутом 10 секунд
        Set<TopicPartition> assignment = Collections.emptySet(); // Инициализация: пустое множество назначенных партиций
        int retries = 0; // Счётчик попыток ожидания назначения партиций
        while (assignment.isEmpty() && retries < 100) { // Цикл: пока нет назначенных партиций и не превышен лимит попыток
            assignment = kafkaConsumer.assignment(); // Получение текущего множества назначенных партиций consumer
            retries++; // Инкремент счётчика попыток
        }

        // Сбрасываем offset на начало
        kafkaConsumer.seekToBeginning(assignment); // Установка offset на начало для всех назначенных партиций
    }

    /**
     * Вспомогательный метод для вычитывания всех сообщений из Kafka топика.
     *
     * @param expectedCount ожидаемое количество сообщений для чтения
     * @param timeoutMs максимальное время ожидания в миллисекундах
     * @return список прочитанных записей Kafka
     */
    private List<ConsumerRecord<String, String>> drainTopic(int expectedCount, long timeoutMs) { // Метод: чтение сообщений из топика
        List<ConsumerRecord<String, String>> records = new ArrayList<>(); // Создание списка для накопления прочитанных записей
        long start = System.currentTimeMillis(); // Фиксация времени начала ожидания

        while (records.size() < expectedCount && (System.currentTimeMillis() - start) < timeoutMs) { // Цикл: пока не набрано нужное количество и не истёк таймаут
            ConsumerRecords<String, String> polled = kafkaConsumer.poll(Duration.ofMillis(100)); // Опрос Kafka с таймаутом 100 мс
            polled.forEach(records::add); // Добавление всех полученных записей в накопительный список
        }
        return records; // Возврат накопленного списка записей
    }

    /**
     * Тест: проверка успешного перехода статуса NEW → PROCESSING
     * и отправки соответствующего события в Kafka.
     */
    @Test // Аннотация: обозначает тестовый метод
    @Transactional // Аннотация: оборачивает тест в транзакцию (откат после теста)
    void shouldSuccessfullyTransitionNewToProcessingAndSendKafkaEvent() { // Метод: тест перехода NEW → PROCESSING
        Order order = new Order(); // Создание нового экземпляра заказа
        order.setStatus("NEW"); // Установка начального статуса заказа — NEW
        order = orderRepository.save(order); // Сохранение заказа в БД и получение persisted экземпляра
        Long orderId = order.getId(); // Получение сгенерированного идентификатора заказа

        orderService.updateOrderStatus(orderId, "PROCESSING"); // Вызов сервиса для обновления статуса на PROCESSING

        Order updatedOrder = orderRepository.findById(orderId).orElseThrow(); // Повторное чтение заказа из БД по ID
        assertThat(updatedOrder.getStatus()).isEqualTo("PROCESSING"); // AssertJ: проверка, что статус в БД изменился на PROCESSING

        List<ConsumerRecord<String, String>> records = drainTopic(1, 5000); // Чтение сообщений из Kafka (ожидаем 1, таймаут 5 сек)
        assertThat(records).hasSize(1); // AssertJ: проверка, что получено ровно 1 сообщение
        assertThat(records.get(0).topic()).isEqualTo(TOPIC_NAME); // AssertJ: проверка, что сообщение из правильного топика
        assertThat(records.get(0).value()).contains("OrderStatusChanged"); // AssertJ: проверка содержимого — тип события
        assertThat(records.get(0).value()).contains(orderId.toString()); // AssertJ: проверка содержимого — ID заказа
    }

    /**
     * Тест: проверка успешного перехода статуса PROCESSING → SHIPPED
     * и отправки события в Kafka.
     */
    @Test // Аннотация: обозначает тестовый метод
    @Transactional // Аннотация: оборачивает тест в транзакцию
    void shouldSuccessfullyTransitionProcessingToShippedAndSendKafkaEvent() { // Метод: тест перехода PROCESSING → SHIPPED
        Order order = new Order(); // Создание нового экземпляра заказа
        order.setStatus("PROCESSING"); // Установка начального статуса — PROCESSING
        order = orderRepository.save(order); // Сохранение заказа в БД
        Long orderId = order.getId(); // Получение ID сохранённого заказа

        orderService.updateOrderStatus(orderId, "SHIPPED"); // Вызов сервиса для обновления статуса на SHIPPED

        Order updatedOrder = orderRepository.findById(orderId).orElseThrow(); // Чтение актуального состояния заказа из БД
        assertThat(updatedOrder.getStatus()).isEqualTo("SHIPPED"); // AssertJ: проверка статуса SHIPPED в БД

        List<ConsumerRecord<String, String>> records = drainTopic(1, 5000); // Чтение сообщения из Kafka (1 сообщение, 5 сек)
        assertThat(records).hasSize(1); // AssertJ: проверка наличия 1 сообщения
        assertThat(records.get(0).value()).contains("OrderStatusChanged"); // AssertJ: проверка типа события
        assertThat(records.get(0).value()).contains(orderId.toString()); // AssertJ: проверка ID заказа в сообщении
    }

    /**
     * Тест: проверка успешного перехода статуса SHIPPED → DELIVERED
     * и отправки события в Kafka.
     */
    @Test // Аннотация: обозначает тестовый метод
    @Transactional // Аннотация: оборачивает тест в транзакцию
    void shouldSuccessfullyTransitionShippedToDeliveredAndSendKafkaEvent() { // Метод: тест перехода SHIPPED → DELIVERED
        Order order = new Order(); // Создание нового заказа
        order.setStatus("SHIPPED"); // Установка начального статуса — SHIPPED
        order = orderRepository.save(order); // Сохранение в БД
        Long orderId = order.getId(); // Получение ID заказа

        orderService.updateOrderStatus(orderId, "DELIVERED"); // Обновление статуса на DELIVERED

        Order updatedOrder = orderRepository.findById(orderId).orElseThrow(); // Чтение из БД
        assertThat(updatedOrder.getStatus()).isEqualTo("DELIVERED"); // AssertJ: проверка статуса DELIVERED

        List<ConsumerRecord<String, String>> records = drainTopic(1, 5000); // Чтение из Kafka
        assertThat(records).hasSize(1); // AssertJ: проверка наличия 1 сообщения
    }

    /**
     * Тест: проверка успешной отмены заказа со статуса NEW.
     * Отмена с NEW — допустимая операция.
     */
    @Test // Аннотация: обозначает тестовый метод
    @Transactional // Аннотация: оборачивает тест в транзакцию
    void shouldSuccessfullyCancelFromNewStatus() { // Метод: тест отмены заказа со статуса NEW
        Order order = new Order(); // Создание заказа
        order.setStatus("NEW"); // Установка статуса NEW
        order = orderRepository.save(order); // Сохранение в БД
        Long orderId = order.getId(); // Получение ID

        orderService.updateOrderStatus(orderId, "CANCELLED"); // Вызов отмены заказа

        Order updatedOrder = orderRepository.findById(orderId).orElseThrow(); // Чтение из БД
        assertThat(updatedOrder.getStatus()).isEqualTo("CANCELLED"); // AssertJ: проверка статуса CANCELLED

        List<ConsumerRecord<String, String>> records = drainTopic(1, 5000); // Чтение события из Kafka
        assertThat(records).hasSize(1); // AssertJ: проверка отправки события об отмене
    }

    /**
     * Тест: проверка успешной отмены заказа со статуса PROCESSING.
     * Отмена на этапе обработки — допустимая операция.
     */
    @Test // Аннотация: обозначает тестовый метод
    @Transactional // Аннотация: оборачивает тест в транзакцию
    void shouldSuccessfullyCancelFromProcessingStatus() { // Метод: тест отмены со статуса PROCESSING
        Order order = new Order(); // Создание заказа
        order.setStatus("PROCESSING"); // Установка статуса PROCESSING
        order = orderRepository.save(order); // Сохранение в БД
        Long orderId = order.getId(); // Получение ID

        orderService.updateOrderStatus(orderId, "CANCELLED"); // Вызов отмены

        Order updatedOrder = orderRepository.findById(orderId).orElseThrow(); // Чтение из БД
        assertThat(updatedOrder.getStatus()).isEqualTo("CANCELLED"); // AssertJ: проверка статуса CANCELLED

        List<ConsumerRecord<String, String>> records = drainTopic(1, 5000); // Чтение события из Kafka
        assertThat(records).hasSize(1); // AssertJ: проверка отправки события
    }

    /**
     * Тест: проверка запрета отмены заказа со статуса SHIPPED.
     * Отправленный заказ нельзя отменить — ожидается исключение.
     * Событие в Kafka при ошибке отправляться не должно.
     */
    @Test // Аннотация: обозначает тестовый метод
    @Transactional // Аннотация: оборачивает тест в транзакцию
    void shouldThrowExceptionWhenCancelFromShipped() { // Метод: тест недопустимой отмены со статуса SHIPPED
        Order order = new Order(); // Создание заказа
        order.setStatus("SHIPPED"); // Установка статуса SHIPPED
        order = orderRepository.save(order); // Сохранение в БД
        Long orderId = order.getId(); // Получение ID

        assertThatThrownBy(() -> orderService.updateOrderStatus(orderId, "CANCELLED")) // AssertJ: проверка выброса исключения
                .isInstanceOf(IllegalOrderStatusTransitionException.class) // Проверка типа исключения
                .hasMessageContaining("SHIPPED") // Проверка: сообщение содержит текущий статус
                .hasMessageContaining("CANCELLED"); // Проверка: сообщение содержит целевой статус

        List<ConsumerRecord<String, String>> records = drainTopic(0, 2000); // Чтение из Kafka (ожидаем 0 сообщений, таймаут 2 сек)
        assertThat(records).isEmpty(); // AssertJ: проверка отсутствия событий в Kafka при ошибке
    }

    /**
     * Тест: проверка запрета пропуска промежуточных статусов.
     * Переход NEW → DELIVERED напрямую — недопустим.
     */
    @Test // Аннотация: обозначает тестовый метод
    @Transactional // Аннотация: оборачивает тест в транзакцию
    void shouldThrowExceptionWhenInvalidTransitionNewToDelivered() { // Метод: тест недопустимого перехода NEW → DELIVERED
        Order order = new Order(); // Создание заказа
        order.setStatus("NEW"); // Установка статуса NEW
        order = orderRepository.save(order); // Сохранение в БД
        Long orderId = order.getId(); // Получение ID

        assertThatThrownBy(() -> orderService.updateOrderStatus(orderId, "DELIVERED")) // AssertJ: проверка исключения
                .isInstanceOf(IllegalOrderStatusTransitionException.class) // Проверка типа исключения
                .hasMessageContaining("NEW") // Проверка: сообщение содержит исходный статус
                .hasMessageContaining("DELIVERED"); // Проверка: сообщение содержит целевой статус
    }

    /**
     * Тест: проверка запрета обратного перехода статуса.
     * DELIVERED → PROCESSING — недопустимый переход.
     */
    @Test // Аннотация: обозначает тестовый метод
    @Transactional // Аннотация: оборачивает тест в транзакцию
    void shouldThrowExceptionWhenInvalidTransitionDeliveredToProcessing() { // Метод: тест недопустимого перехода DELIVERED → PROCESSING
        Order order = new Order(); // Создание заказа
        order.setStatus("DELIVERED"); // Установка конечного статуса DELIVERED
        order = orderRepository.save(order); // Сохранение в БД
        Long orderId = order.getId(); // Получение ID

        assertThatThrownBy(() -> orderService.updateOrderStatus(orderId, "PROCESSING")) // AssertJ: проверка исключения
                .isInstanceOf(IllegalOrderStatusTransitionException.class) // Проверка типа исключения
                .hasMessageContaining("DELIVERED") // Проверка: сообщение содержит текущий статус
                .hasMessageContaining("PROCESSING"); // Проверка: сообщение содержит целевой статус
    }

    /**
     * Тест: проверка обработки несуществующего заказа.
     * При обращении по несуществующему ID должно выбрасываться исключение OrderNotFoundException.
     */
    @Test // Аннотация: обозначает тестовый метод (без @Transactional — нет изменений в БД)
    void shouldThrowExceptionWhenOrderNotFound() { // Метод: тест обработки несуществующего заказа
        Long nonExistentOrderId = 99999L; // Объявление заведомо несуществующего ID заказа

        assertThatThrownBy(() -> orderService.updateOrderStatus(nonExistentOrderId, "PROCESSING")) // AssertJ: проверка исключения
                .isInstanceOf(OrderNotFoundException.class) // Проверка типа исключения
                .hasMessageContaining("Заказ не найден") // Проверка: сообщение содержит текст на русском
                .hasMessageContaining(nonExistentOrderId.toString()); // Проверка: сообщение содержит ID заказа
    }

    /**
     * Тест: проверка полного жизненного цикла заказа.
     * Последовательный переход через все допустимые статусы: NEW → PROCESSING → SHIPPED → DELIVERED.
     * Проверяет отправку 3 событий в Kafka (по одному на каждый переход).
     */
    @Test // Аннотация: обозначает тестовый метод
    @Transactional // Аннотация: оборачивает тест в транзакцию
    void shouldSuccessfullyTransitionFullLifecycle() { // Метод: тест полного жизненного цикла заказа
        Order order = new Order(); // Создание заказа
        order.setStatus("NEW"); // Установка начального статуса NEW
        order = orderRepository.save(order); // Сохранение в БД
        Long orderId = order.getId(); // Получение ID

        orderService.updateOrderStatus(orderId, "PROCESSING"); // Переход 1: NEW → PROCESSING
        orderService.updateOrderStatus(orderId, "SHIPPED"); // Переход 2: PROCESSING → SHIPPED
        orderService.updateOrderStatus(orderId, "DELIVERED"); // Переход 3: SHIPPED → DELIVERED

        Order finalOrder = orderRepository.findById(orderId).orElseThrow(); // Чтение финального состояния из БД
        assertThat(finalOrder.getStatus()).isEqualTo("DELIVERED"); // AssertJ: проверка конечного статуса DELIVERED

        List<ConsumerRecord<String, String>> records = drainTopic(3, 5000); // Чтение всех событий из Kafka (ожидаем 3)
        assertThat(records).hasSize(3); // AssertJ: проверка наличия ровно 3 событий (по одному на переход)
    }
}