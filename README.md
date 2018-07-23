# Silo

Silo is log-based storage framework for building custom databases. Silo runs on
Java 8 and is built around entities. Most entities are key-value storages that
support querying via pluggable query engines.

## Log-based

Every instance of Silo runs on top of a log. In the most simple case the log
just applies every entry as they arrive, which is useful for a local, non-replicated
storage.
