# JSql

![Verify status](https://github.com/lucar94/jsql/actions/workflows/maven.yml/badge.svg)

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

## 🏷 Column names

Each JSON key becomes a column whose name is:

- **flattened**: nested objects and arrays use `.` and `[n]` (`address.city`, `phones[0]`), which become `_`, so `ADDRESS_CITY`, `PHONES_0`;
- **sanitized**: any character other than letters, digits and `_` becomes `_`, and trailing `_` are removed;
- **upper-cased**: `isActive` becomes `ISACTIVE`.

Columns are always created as quoted identifiers, so keys that are SQL reserved words (`order`, `value`, `user`, …) or start with a digit work too. To reference them in a query use a quoted name:

```java
select().from(jSql.getTable()).where(field(name("ORDER")).eq("2"));
```

If two different keys map to the same column (e.g. `a.b` and `a_b`, or `name` and `Name`), `build()` throws an `IllegalArgumentException` instead of silently dropping one of the values.

---

## 📊 Benchmarks

JSON arrays are read in streaming: rows are parsed one at a time and inserted in batches, so the whole document is never held in memory. The memory needed is about **1.5× the size of the JSON file**, almost all of it taken by the in-memory H2 table that holds the data.

| JSON file | Rows | `build()` time | Minimum heap (`-Xmx`) | Heap used after `build()` |
|---|---|---|---|---|
| 29 MB | 100,000 | 2.1 s | 64 MB | 48 MB |
| 145 MB | 500,000 | 7.0 s | 224 MB | 208 MB |
| 582 MB | 2,000,000 | 25 s | 896 MB | 806 MB |

As a rule of thumb, give the JVM a heap of at least **1.5× the JSON size plus some headroom** for your own application.

<sub>Measured on an Apple M1 Pro (16 GB RAM), OpenJDK 25, with JSql 1.x. Each record is an object of about 300 bytes, with nested objects and arrays, flattened into 17 columns. "Minimum heap" is the smallest tested `-Xmx` that completes without `OutOfMemoryError`: tested values were 48/64 MB, 192/224 MB and 768/896 MB. Results depend on the shape of the data: many short values take more space per MB than few long ones.</sub>

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
