# allobank-be

A Spring Boot app for bill splitting with settlement calculation and service charges.

## How to Run the Project

1. Ensure you have Java Development Kit (JDK) version 17 or higher installed on your system.
2. Ensure you have Spring Boot version 4.1.0 or higher.
3. Use MySQL database to store application data.
4. Use Maven to manage dependencies and build the project.
5. Run the following commands to build and run the project:

```bash
mvn clean install
mvn spring-boot:run
```

## Curl Examples for Endpoints

### 1. Create a Group

```bash
curl -X POST http://localhost:8080/groups \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Trip Bali",
    "participants": [
      { "name": "Alice" },
      { "name": "Bob" },
      { "name": "Jhon" }
    ]
  }'
```

### 2. Add Expenses

```bash
curl -X POST http://localhost:8080/groups/1/expenses \
  -H "Content-Type: application/json" \
  -d '{
    "payerId": 1,
    "amount": 150.00,
    "beneficiaryIds": [1, 2, 3],
    "category": "Food"
  }'
```

```bash
curl -X POST http://localhost:8080/groups/1/expenses \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 300.00,
    "payerId": 1,
    "beneficiaryIds": [2, 3],
    "category": "Accommodation",
    "splitStrategy": "EXACT",
    "exactShares": {
      "2": 200.00,
      "3": 100.00
    }
  }'
```

```bash
curl -X POST http://localhost:8080/groups/1/expenses \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 300.00,
    "payerId": 1,
    "beneficiaryIds": [2, 3],
    "category": "Transport",
    "splitStrategy": "PERCENTAGE",
    "percentageShares": {
      "2": 60,
      "3": 40
    }
  }'
```

### 3. Calculate Settlement

```bash
curl -X GET "http://localhost:8080/groups/1/settlement?githubUsername=Alice"
```

### 4. Make a Payment

```bash
curl -X POST http://localhost:8080/groups/1/payments \
  -H "Content-Type: application/json" \
  -d '{
    "fromId": 2,
    "toId": 1,
    "amount": 50.00,
    "githubUsername": "Jhon"
  }'
```

## GitHub

- **Username:** nugraharmd
- **Service Charge:** 8%  

---  

## What Was the Hardest Design Decision You Made While Building This, and What Trade-off Did You Accept?

The hardest design decision I made was how to model the expense splitting strategy within the system. Initially, I could have chosen to support only equal splits, but I decided to add percentage-based and exact amount options for greater flexibility. The trade-off I accepted was increased complexity in entity design and settlement calculation logic, requiring more code and testing. I accepted this compromise because flexibility better reflects real user needs, even though it made the initial implementation more challenging.
