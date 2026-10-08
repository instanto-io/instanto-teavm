# Historical consumer patches

These patches were captured before this repository was available and its shared
artifacts could be published. The migration is now prepared as changes in the
consumer repositories; see [the migration guide](../docs/consumer-migration.md)
and the associated pull requests for current status.

Do not apply these patches blindly: they refer to earlier revisions and omit
later dependency and documentation changes. They are retained as reference.
`mockatcha-java-reformat.patch` is unrelated formatting and is not part of the
migration. Domain-specific native callback tests belong in Sarto's invocation
tests, not the shared TeaVM runtime.
