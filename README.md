# JSql

**JSql** is a lightweight Java library that lets you query JSON files using standard SQL syntax through jOOQ's DSL. It transforms a JSON array (from a `String` or file) into an in-memory SQL table you can query with familiar jOOQ constructs.

---

## 🔍 What it does

- Converts a JSON array into a virtual SQL table
- Allows SQL-style querying via jOOQ DSL
- Supports input from a JSON `String` or a file
- Fully in-memory, no external dependencies beyond jOOQ
- Open-source and free to use under a permissive license

---

## 🚀 Quick Example

```java
// Create a JSql instance with your JSON array (String or File)
JSql jSql = JSql.create()
                .withJson(myJsonString) // or .withJson(myJsonFile)
                .build();

// Build a SQL query using jOOQ DSL
Select<Record> select = select()
                .from(jSql.getTable())
                .where(field("id").eq("2"));

// Execute the query
Result<Record> results = jSql.execute(select);

// Read results in different ways
String id = record.get(DSL.field(DSL.name("ID")), String.class);
String name = record.get("NAME", String.class);
```

---

## 📦 Requirements
- Java 17 or higher

---

## 💡 Use Cases
- Query static or dynamic JSON data without a database
- Validate SQL logic on mock data
- Build tools or services that rely on JSON-based data transformation

---

## 🛠 Installation

### Coming soon: Maven/Gradle coordinates

For now, clone the repository and build locally:

```
git clone https://github.com/lucar94/jsql.git
cd jsql
mvn clean install
```

---

## 📖 License

This library is open-source and distributed under a permissive free software license (e.g., MIT or Apache 2.0). You are free to use, modify, and distribute it.

---

## 🤝 Contributing

Contributions are welcome! Feel free to open issues, suggest features, or submit pull requests.
