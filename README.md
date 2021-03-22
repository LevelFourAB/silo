# Silo

Silo is a key-value storage framework with a reactive API, transactions, 
extendable indexing and backup support. Silo runs on Java 9+ and is intended to
be used as an embedded part of larger solutions.

```java
// Definition of a an entity named `books`
EntityDefinition booksDef = EntityDefinition.create("books", Book.class)
  .withCodec(...)
  .withId(Book::getId)
  .build();

// Start a Silo instance with one entity
LocalSilo silo = LocalSilo.open("path/to/data/directory")
  .addEntity(booksDef)
  .start()
  .block();

// Get `Entity` object to perform operations
Entity<Long, Book> books = silo.entity("books", Long.class, Books.class);

// Store an object
books.store(new Book(1l, "The Tourist's Guide through North Wales"))
  .block();

// Get an object
Book stored = books.get(1l)
  .block();

// Delete an object
books.delete(1l)
  .block();
```

## Features

* Key-value storage based on [MVStore](https://www.h2database.com/html/mvstore.html)
* Transactions with read committed isolation
* Extensible indexing support
* Basic index, simple matching against fields
* Search index, complex matching including full text search
* Live backup support

## Status

`master` currently contains a major rewrite that is not yet ready for release.

## License

This project is licensed under the [MIT license](https://opensource.org/licenses/MIT),
see the file `LICENSE.md` for details.
