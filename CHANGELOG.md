# Changelog

## [v1.1.1] - 2026-01-24

### Added
- Added `TURNSTILE_SECRET_KEY` environment variable support in `docker-compose.yml`.

### Changed
- Refactored service dependencies to improve maintainability.
- Enhanced user audit logic for better workflow reliability.

### Fixed
- Fixed a typo in the project package name (renamed `ShigureCafeBackened` to `ShigureCafeBackend`).

## [v1.1.0] - 2026-01-24

### Added
- Integrated Cloudflare Turnstile for CAPTCHA verification.
- Added database migration support with Flyway and initial migration script.

### Changed
- Updated `spring-boot-starter-aop` to 3.5.10.
- Configured JPA `ddl-auto` to `validate`.
- Updated dependencies and formatted `pom.xml`.

### Fixed
- Updated environment variables in `.env.example` and `README.md`.

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
