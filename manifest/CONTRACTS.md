# Portable Architecture Contracts

The CLI/MCP/staging contract source introduced by PR #4 is reusable architecture verification, not a repository-specific runtime suite.

It owns checks for:

- public workspace/lease authority leakage;
- MCP projection outside the native bootstrap surface;
- MCP republication of CLI capability authority;
- execution-only sandbox boundaries;
- CLI command/help projection drift;
- active staging ancestry validity.

These contracts remain canonical source in this repository. Consumer repositories must execute applicable canonical architecture checks through the published architecture-test modules/Gradle gate rather than copying this source tree.

Repository-specific adapters may provide the real command/catalog/PR snapshots needed by a contract. Product runtime simulations, integration tests, and platform-specific behavior remain owned by the consumer repository.

The historical `repositories/` tree is provenance and migration evidence. It is not the long-term consumer execution mechanism.
