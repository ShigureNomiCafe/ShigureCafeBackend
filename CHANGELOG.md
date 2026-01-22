# Changelog

## [v1.0.1] - 2026-01-22

### Fixed
- Allow deleting users who have associated audit information and notices.

### Changed
- Update Docker service restart policy to `unless-stopped`.
- Refactor port exposure configuration in `docker-compose.yml`.

## [v1.0.0] - 2026-01-19
- Initial release with Docker support.
- Minecraft chat synchronization and whitelist management.
- S3 storage integration.
- AOP-based rate limiting.
