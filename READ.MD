# Getting Started

Распаковать архив, перейти в папку проекта.

Запустить docker-compose up -d (должны подняться PostgreSQL, Redis, Kafka).

Запустить приложение: ./mvnw spring-boot:run (или через IDE).

При старте должна быть ошибка – прочитать логи и исправить.

После успешного запуска проверить работу через curl:

Создание заказа:
curl -X POST http://localhost:8080/api/orders -H "Content-Type: application/json" -d '{"customerName":"John","status":"NEW","items":[{"productName":"Laptop","category":"electronics","price":1000,"quantity":1}]}'

Получение заказа по id: curl http://localhost:8080/api/orders/1

Получение заказов по статусу: curl http://localhost:8080/api/orders/by-status?status=NEW

Найти и исправить все 8 ошибок. После исправления каждой написать тест, который проверяет, что проблема устранена.

Все тесты должны проходить, а приложение работать стабильно.