# Changelog

## [0.0.9] - 2025-05-10 [UNRELEASED]

### Added

- [ :test_tube: ] Add `E2E` tests
- [ :package: ] Add `Selenium` 4.26.0
- [ :book: ] Add `UPDATING` documentation
- [ :package: ] Add `Webjars Locator Lite` 1.0.1

### Changed

- [ :whale2: ] Bump `Redis` from 7.4.0 to 7.4.2 ([`c71ccfde`](https://github.com/ivasibi/ascent/commit/c71ccfde))
- [ :whale2: ] Bump `MySQL` from 8.4.2 to 8.4.4 ([`4a0040d6`](https://github.com/ivasibi/ascent/commit/4a0040d6))
- [ :package: ] Bump `Font Awesome` from 6.5.2 to 6.7.2 ([`23568761`](https://github.com/ivasibi/ascent/commit/23568761))
- [ :package: ] Bump `Testcontainers` from 1.20.1 to 1.20.4 ([`3cc0ed21`](https://github.com/ivasibi/ascent/commit/3cc0ed21))
- [ :package: ] Bump `Spring Boot` from 3.3.3 to 3.4.2 ([`517fb1a7`](https://github.com/ivasibi/ascent/commit/517fb1a7))

### Removed

- [ :package: ] Remove `Webjars Locator` 0.52

## [0.0.8] - 2024-10-27 [UNRELEASED]

### Added

- [ :test_tube: ] Add `Logging` tests
- [ :rocket: ] Add `Logging` filter
- [ :rocket: ] Add `Logging` to `Rolling File`

### Changed

- [ :whale2: ] Bump `Redis` from 7.2.3 to 7.4.0 ([`f435dcb7`](https://github.com/ivasibi/ascent/commit/f435dcb7))
- [ :whale2: ] Bump `MySQL` from 8 to 8.4.2 ([`cf887f3c`](https://github.com/ivasibi/ascent/commit/cf887f3c))
- [ :package: ] Bump `HTMX` from 1.9.12 to 2.0.0 ([`50b0f7e2`](https://github.com/ivasibi/ascent/commit/50b0f7e2))
- [ :package: ] Bump `Spring Boot` from 3.3.2 to 3.3.3 ([`39b72d6b`](https://github.com/ivasibi/ascent/commit/39b72d6b))

## [0.0.7] - 2024-08-22 [UNRELEASED]

### Added

- [ :book: ] Add `INSTALLING` documentation
- [ :whale2: ] Add `compose-prod.yml` for production
- [ :whale2: ] Add `compose-stg.yml` for staging
- [ :gear: ] Add `.dockerignore` file
- [ :whale2: ] Add `Dockerfile` for building
- [ :package: ] Add `Maven Build Plugin` 3.3.2 (Spring Boot)
- [ :rocket: ] Add `stg` profile ([`ed81ba30`](https://github.com/ivasibi/ascent/commit/ed81ba30))

### Changed

- [ :package: ] Bump `Testcontainers` from 1.19.7 to 1.20.1 ([`ccb761aa`](https://github.com/ivasibi/ascent/commit/ccb761aa))
- [ :package: ] Bump `Spring Boot` from 3.2.5 to 3.3.2 ([`604735b6`](https://github.com/ivasibi/ascent/commit/604735b6))

## [0.0.6] - 2024-08-04 [UNRELEASED]

### Added

- [ :gear: ] Add `.wslconfig` file
- [ :gear: ] Add `.editorconfig` file
- [ :test_tube: ] Add `Session Timeout` tests ([`742790a0`](https://github.com/ivasibi/ascent/commit/742790a0))
- [ :rocket: ] Add `EDITOR` and `MODERATOR` roles
- [ :gear: ] Add `run` and `test` configurations
- [ :book: ] Add `TESTING` documentation
- [ :book: ] Add `DEVELOPING` documentation
- [ :whale2: ] Add `compose-dev.yml` for development ([`ff3a4da7`](https://github.com/ivasibi/ascent/commit/ff3a4da7))

### Changed

- [ :rocket: ] Set `Session Timeout` as `Role Based`
- [ :package: ] Bump `HTMX` from 1.9.11 to 1.9.12 ([`41176fa5`](https://github.com/ivasibi/ascent/commit/41176fa5))
- [ :package: ] Bump `Font Awesome` from 6.5.1 to 6.5.2 ([`ad04a42c`](https://github.com/ivasibi/ascent/commit/ad04a42c))
- [ :package: ] Bump `Spring Boot` from 3.2.4 to 3.2.5 ([`ff2a974f`](https://github.com/ivasibi/ascent/commit/ff2a974f))

## [0.0.5] - 2024-04-27 [UNRELEASED]

### Added

- [ :test_tube: ] Add `Cache` tests ([`0312ffe1`](https://github.com/ivasibi/ascent/commit/0312ffe1))
- [ :rocket: ] Add `Redis` as `Cache Store` ([`5a0650fc`](https://github.com/ivasibi/ascent/commit/5a0650fc))

### Changed

- [ :package: ] Bump `HTMX` from 1.9.10 to 1.9.11 ([`7bf4c683`](https://github.com/ivasibi/ascent/commit/7bf4c683))
- [ :package: ] Bump `Spring Boot` from 3.2.3 to 3.2.4 ([`5d99cd5c`](https://github.com/ivasibi/ascent/commit/5d99cd5c))

## [0.0.4] - 2024-03-30 [UNRELEASED]

### Added

- [ :test_tube: ] Add `Session Timeout` tests ([`2d484336`](https://github.com/ivasibi/ascent/commit/2d484336))
- [ :test_tube: ] Add `Server Protocol` and `Server IP` parameters ([`91f0ad4f`](https://github.com/ivasibi/ascent/commit/91f0ad4f))

### Changed

- [ :rocket: ] Set `Session Timeout` to `30m` ([`43481805`](https://github.com/ivasibi/ascent/commit/43481805))
- [ :test_tube: ] Set `Session Namespace` parameter ([`d32cde10`](https://github.com/ivasibi/ascent/commit/d32cde10))
- [ :rocket: ] Set `ascent:sessions` as `Session Namespace` ([`0723413d`](https://github.com/ivasibi/ascent/commit/0723413d))
- [ :test_tube: ] Set `Session Cookie` parameter ([`f772c41c`](https://github.com/ivasibi/ascent/commit/f772c41c))
- [ :rocket: ] Set `AC-SESSION` as `Session Cookie` ([`3ed7b7a5`](https://github.com/ivasibi/ascent/commit/3ed7b7a5))
- [ :package: ] Bump `HTMX` from 1.9.9 to 1.9.10 ([`4c47bfef`](https://github.com/ivasibi/ascent/commit/4c47bfef))
- [ :package: ] Bump `Bootstrap` from 5.3.2 to 5.3.3 ([`9598708b`](https://github.com/ivasibi/ascent/commit/9598708b))
- [ :package: ] Bump `Testcontainers` from 1.19.3 to 1.19.7 ([`4137ca5f`](https://github.com/ivasibi/ascent/commit/4137ca5f))
- [ :package: ] Bump `Spring Boot` from 3.2.1 to 3.2.3 ([`38c5a78f`](https://github.com/ivasibi/ascent/commit/38c5a78f))

## [0.0.3] - 2024-03-16 [UNRELEASED]

### Added

- [ :test_tube: ] Add `Unit`, `Integration` and `Functionality` tests
- [ :package: ] Add `Testcontainers` 1.19.3 ([`31e2efbd`](https://github.com/ivasibi/ascent/commit/31e2efbd))

### Changed

- [ :package: ] Bump `HTMX` from 1.9.8 to 1.9.9 ([`e343e125`](https://github.com/ivasibi/ascent/commit/e343e125))
- [ :package: ] Bump `Font Awesome` from 6.4.2 to 6.5.1 ([`30b0ac88`](https://github.com/ivasibi/ascent/commit/30b0ac88))
- [ :package: ] Bump `Spring Boot` from 3.1.5 to 3.2.1 ([`a4e692bb`](https://github.com/ivasibi/ascent/commit/a4e692bb))

### Removed

- [ :package: ] Remove `JUnit Params` 5.10.1 ([`bbb28909`](https://github.com/ivasibi/ascent/commit/bbb28909))

## [0.0.2] - 2023-12-26 [UNRELEASED]

### Added

- [ :rocket: ] Add `Authentication` logic ([`47365e93`](https://github.com/ivasibi/ascent/commit/47365e93))
- [ :rocket: ] Add `USER` and `ADMIN` roles
- [ :rocket: ] Add `dev` and `prod` profiles ([`67f6b601`](https://github.com/ivasibi/ascent/commit/67f6b601))
- [ :whale2: ] Add `Redis` 7.2.3
- [ :whale2: ] Add `MySQL` 8
- [ :package: ] Add `HTMX` 1.9.8
- [ :package: ] Add `Font Awesome` 6.4.2
- [ :package: ] Add `Bootstrap` 5.3.2
- [ :package: ] Add `Thymeleaf` 3.1.5 (Spring Boot) ([`3b8e3159`](https://github.com/ivasibi/ascent/commit/3b8e3159))
- [ :package: ] Add `JPA` 3.1.5 (Spring Boot)

### Changed

- [ :rocket: ] Set `Redis` as `Session Store` ([`3b31a591`](https://github.com/ivasibi/ascent/commit/3b31a591))

## [0.0.1] - 2023-10-22 [UNRELEASED]

### Added

- [ :package: ] Add `Spring Boot` 3.1.5
- [ :star: ] Start `Ascent` project ([`c30d11d5`](https://github.com/ivasibi/ascent/commit/c30d11d5))