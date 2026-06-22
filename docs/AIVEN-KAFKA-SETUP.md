# Aiven Kafka Setup

This project already uses `spring-kafka`, so you do not need to add raw `kafka-clients` manually. Spring Boot dependency management brings the compatible Kafka client version.

Use this guide when replacing local Docker Kafka with Aiven for Apache Kafka.

## 1. Download Aiven Certificates

From Aiven Console:

1. Open your Kafka service.
2. Go to `Overview`.
3. Download:
   - `service.key`
   - `service.cert`
   - `ca.pem`

Keep these files out of Git.

## 2. Create Keystore and Truststore

Create a local folder for secrets, for example:

```bash
mkdir -p secrets/aiven-kafka
```

Move the downloaded files there:

```bash
mv service.key service.cert ca.pem secrets/aiven-kafka/
```

Create the client keystore:

```bash
openssl pkcs12 -export \
  -inkey secrets/aiven-kafka/service.key \
  -in secrets/aiven-kafka/service.cert \
  -out secrets/aiven-kafka/client.keystore.p12 \
  -name service_key
```

Create the truststore:

```bash
keytool -import \
  -file secrets/aiven-kafka/ca.pem \
  -alias CA \
  -keystore secrets/aiven-kafka/client.truststore.jks
```

Use strong passwords and store them as environment variables.

## 3. mTLS Configuration

Use this when Aiven Kafka is configured with SSL certificate authentication.

```bash
export KAFKA_BOOTSTRAP_SERVERS="your-aiven-host:your-ssl-port"
export KAFKA_SECURITY_PROTOCOL="SSL"

export KAFKA_SSL_KEYSTORE_TYPE="PKCS12"
export KAFKA_SSL_KEYSTORE_LOCATION="file:/absolute/path/to/secrets/aiven-kafka/client.keystore.p12"
export KAFKA_SSL_KEYSTORE_PASSWORD="your-keystore-password"
export KAFKA_SSL_KEY_PASSWORD="your-key-password"

export KAFKA_SSL_TRUSTSTORE_TYPE="JKS"
export KAFKA_SSL_TRUSTSTORE_LOCATION="file:/absolute/path/to/secrets/aiven-kafka/client.truststore.jks"
export KAFKA_SSL_TRUSTSTORE_PASSWORD="your-truststore-password"
```

Start with prod profile:

```bash
SPRING_PROFILES_ACTIVE=prod ./mvnw spring-boot:run
```

## 4. SASL/SCRAM Configuration

Use this when Aiven Kafka is configured with username/password authentication.

```bash
export KAFKA_BOOTSTRAP_SERVERS="your-aiven-host:your-sasl-port"
export KAFKA_SECURITY_PROTOCOL="SASL_SSL"
export KAFKA_SASL_MECHANISM="SCRAM-SHA-256"
export KAFKA_SASL_JAAS_CONFIG='org.apache.kafka.common.security.scram.ScramLoginModule required username="your-user" password="your-password";'

export KAFKA_SSL_TRUSTSTORE_TYPE="JKS"
export KAFKA_SSL_TRUSTSTORE_LOCATION="file:/absolute/path/to/secrets/aiven-kafka/client.truststore.jks"
export KAFKA_SSL_TRUSTSTORE_PASSWORD="your-truststore-password"
```

Start with prod profile:

```bash
SPRING_PROFILES_ACTIVE=prod ./mvnw spring-boot:run
```

## 5. Docker Profile

For Docker, mount the keystore/truststore files into the app container and use container paths:

```yaml
volumes:
  - ./secrets/aiven-kafka:/app/secrets/aiven-kafka:ro
```

Then set:

```bash
KAFKA_SSL_KEYSTORE_LOCATION=file:/app/secrets/aiven-kafka/client.keystore.p12
KAFKA_SSL_TRUSTSTORE_LOCATION=file:/app/secrets/aiven-kafka/client.truststore.jks
```

## 6. Required Project Variables

The application reads these Kafka variables:

```text
KAFKA_BOOTSTRAP_SERVERS
KAFKA_SECURITY_PROTOCOL
KAFKA_SSL_KEYSTORE_TYPE
KAFKA_SSL_KEYSTORE_LOCATION
KAFKA_SSL_KEYSTORE_PASSWORD
KAFKA_SSL_KEY_PASSWORD
KAFKA_SSL_TRUSTSTORE_TYPE
KAFKA_SSL_TRUSTSTORE_LOCATION
KAFKA_SSL_TRUSTSTORE_PASSWORD
KAFKA_SASL_MECHANISM
KAFKA_SASL_JAAS_CONFIG
KAFKA_CONSUMER_GROUP
TASK_EVENTS_TOPIC
```

## 7. Verification

When the app starts, look for Kafka consumer startup logs such as:

```text
KafkaMessageListenerContainer
Subscribed to topic(s): task-events
```

Then trigger a task update:

```bash
curl -X PATCH http://localhost:8080/api/tasks/1/status \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status":"IN_PROGRESS"}'
```

Expected flow:

```text
TaskService -> TaskEventPublisher -> Aiven Kafka -> NotificationConsumer -> pending_notifications
```
