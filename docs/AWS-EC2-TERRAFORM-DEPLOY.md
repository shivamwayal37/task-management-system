# AWS EC2 Deployment with Terraform

This guide provisions infrastructure with Terraform, then deploys the Spring Boot JAR to the created EC2 instance.

## 1. Prerequisites

- AWS account with programmatic credentials configured (`aws configure`)
- Terraform >= `1.6`
- Existing EC2 key pair in your target region
- Java/Maven locally to build the JAR

## 2. Provision Infrastructure

1. Move into Terraform folder:

```bash
cd infrastructure/terraform
```

2. Create `terraform.tfvars` from the example:

```bash
cp terraform.tfvars.example terraform.tfvars
```

3. Update required values in `terraform.tfvars`:
- `key_pair_name` must match an existing EC2 key pair.
- `ssh_cidr_blocks` should be narrowed to your public IP in production.
- `app_cidr_blocks` can be restricted if needed.

4. Run Terraform:

```bash
terraform init
terraform plan
terraform apply
```

5. Get instance details:

```bash
terraform output
```

Capture `public_ip`.

## 3. Build Spring Boot Application

From project root:

```bash
./mvnw clean package -DskipTests
```

## 4. Deploy JAR to EC2

Use the helper script from project root:

```bash
bash scripts/deploy-ec2.sh <ec2-public-ip> <path-to-pem> <ec2-user>
```

Example:

```bash
bash scripts/deploy-ec2.sh 54.210.10.20 ~/.ssh/task-management.pem ec2-user
```

The script:
- copies `target/*.jar` to EC2 as `/home/<user>/app.jar`
- installs Java 25 Corretto (if missing)
- stops previous Java process
- starts app with `nohup`
- tails recent logs

## 5. Validate

From your machine:

```bash
curl http://<ec2-public-ip>:8080/actuator/health
```

Swagger:

```text
http://<ec2-public-ip>:8080/swagger-ui/index.html
```

## 6. Destroy Infrastructure

When done:

```bash
cd infrastructure/terraform
terraform destroy
```

## Notes

- This setup is best for demo/dev workloads.
- For production, move DB/Redis/Kafka out of single-node EC2 to managed services (RDS, ElastiCache, MSK) and front the app with HTTPS.
