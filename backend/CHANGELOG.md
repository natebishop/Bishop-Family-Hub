# Changelog

## [1.9.0](https://github.com/joe-bor/family-hub-api/compare/v1.8.0...v1.9.0) (2026-07-05)


### Features

* **lists:** add named MAX_BULK_ITEMS constant and service-level batch guard ([99b69bd](https://github.com/joe-bor/family-hub-api/commit/99b69bd6f61445cfce0a70f53e538145c6f467d2))
* **lists:** add transactional bulk item append service ([4746074](https://github.com/joe-bor/family-hub-api/commit/47460740730608c1d9ab963c1bfb741067d284e5))
* **lists:** expose bulk item append endpoint ([4fb14a8](https://github.com/joe-bor/family-hub-api/commit/4fb14a883c07463112458742e17f2543bde81b07))


### Bug Fixes

* **lists:** make list-item read order deterministic with id tiebreaker ([e47b6a1](https://github.com/joe-bor/family-hub-api/commit/e47b6a1ff4b6de991a896a9b171df26ea2bd924c))

## [1.8.0](https://github.com/joe-bor/family-hub-api/compare/v1.7.0...v1.8.0) (2026-07-01)


### Features

* **meals:** add atomic meal plan save ([f8f35e3](https://github.com/joe-bor/family-hub-api/commit/f8f35e311af4c3102bb7cc5b3e6361517f4e2a6e))
* **meals:** expose meal plan batch save ([e57e203](https://github.com/joe-bor/family-hub-api/commit/e57e203728c28bf029451b2558a3316364163d80))


### Bug Fixes

* **meals:** validate meal plan save slots ([bd01f51](https://github.com/joe-bor/family-hub-api/commit/bd01f514ef0c0750c38a0ee9a5f6fa5f69a9f067))


### Tests

* **family:** keep integration usernames valid ([51d9e45](https://github.com/joe-bor/family-hub-api/commit/51d9e45d8abdc69acff7d55ecd6527497f0e54da))
* **meals:** prove batch save family isolation ([1086d70](https://github.com/joe-bor/family-hub-api/commit/1086d70fb66e1898c737666395088ef5db7d5428))

## [1.7.0](https://github.com/joe-bor/family-hub-api/compare/v1.6.0...v1.7.0) (2026-06-27)


### Features

* **lists:** add category catalog scope migration ([8743eb4](https://github.com/joe-bor/family-hub-api/commit/8743eb4b4d7e857bc57443ca378273822e6aa507))
* **lists:** define managed category contracts ([0e56567](https://github.com/joe-bor/family-hub-api/commit/0e5656770ebcbbc76170cdbc2d9b79ae84b3fb40))
* **lists:** enable family categories across list kinds ([21c5618](https://github.com/joe-bor/family-hub-api/commit/21c561824de854ecff426859e2e9160300686991))
* **lists:** expose family category catalog API ([6a0e8cc](https://github.com/joe-bor/family-hub-api/commit/6a0e8cca6aa5eda87c658915efe913db7d38961d))
* **lists:** manage serialized category catalogs ([5a07d83](https://github.com/joe-bor/family-hub-api/commit/5a07d8367099ac0839a6cc2a4a9faca6ae9ef473))


### Bug Fixes

* **lists:** make category bulk updates transactional ([e30717c](https://github.com/joe-bor/family-hub-api/commit/e30717c9aaf6cb60adf3515013f8595ab897f5dc))
* **lists:** validate item category before mutation and trim-aware name length ([0a5118c](https://github.com/joe-bor/family-hub-api/commit/0a5118c861d2123dcd691fa3d1a53d25dbde9b82))
* **release:** fail closed when backend release is unavailable ([87e6ff0](https://github.com/joe-bor/family-hub-api/commit/87e6ff0819396dc20ea25c416de1c535e48ecc71))


### Code Refactoring

* **lists:** targeted item count on rename and descriptive catalog message ([b295744](https://github.com/joe-bor/family-hub-api/commit/b295744d0de43b9cec5edda49d2514dea1ba1795))


### Documentation

* add project README ([4891579](https://github.com/joe-bor/family-hub-api/commit/48915792867e4d0ee1732f57ddaf93994050b9f6))
* **release:** clarify resolver intent and extend parser test coverage ([cede2e2](https://github.com/joe-bor/family-hub-api/commit/cede2e2593cc54d754d247462513f6a8eb3a2322))


### Tests

* **chore:** clear leftover list items before family wipe ([33c5a64](https://github.com/joe-bor/family-hub-api/commit/33c5a64745463986421c8627e1fb814b938dfbcb))
* **lists:** cover V17 normalized-duplicate migration guard ([151995f](https://github.com/joe-bor/family-hub-api/commit/151995f32eb1d6f6336f7caa54a161aad44e5f6e))
* **lists:** guard list-mode stability and refactor item assignment ([e7037b5](https://github.com/joe-bor/family-hub-api/commit/e7037b56ccfe135d69d64801fa56cde6deec836c))
* **lists:** malformed body kind, HTTP dup race, and lock-serialized contention ([54ad72f](https://github.com/joe-bor/family-hub-api/commit/54ad72fbb17871a169262e39eaaf26af09ad079d))
* **lists:** prove category scope lock contention ([0eac34c](https://github.com/joe-bor/family-hub-api/commit/0eac34c8df2b5323ddcafc042c2d7a678ded21d3))
* **lists:** strengthen category service reorder and delete ordering coverage ([6e1d3b1](https://github.com/joe-bor/family-hub-api/commit/6e1d3b15f0c7f20d2257165804ad7eb2bf99a8c0))
* **lists:** tighten category API isolation and rollback assertions ([0d77f63](https://github.com/joe-bor/family-hub-api/commit/0d77f63c4d30ccf6e4589794dc11fbfc94e6e700))

## [1.6.0](https://github.com/joe-bor/family-hub-api/compare/v1.5.0...v1.6.0) (2026-06-12)


### Features

* **family:** expose and allow updating family timezone ([c4469a9](https://github.com/joe-bor/family-hub-api/commit/c4469a9fc8eb9557426ed356f037f0a056d94758))


### Tests

* **family:** cover invalid timezone combined with other fields ([68adfce](https://github.com/joe-bor/family-hub-api/commit/68adfce88436a67950e4e133c561979ca6a99cd1))

## [1.5.0](https://github.com/joe-bor/family-hub-api/compare/v1.4.1...v1.5.0) (2026-06-04)


### Features

* **meals:** add meal board foundation ([1dc95e1](https://github.com/joe-bor/family-hub-api/commit/1dc95e101934ce36e34659a8b23046dd83bb1883))
* **meals:** add meal slot write flows ([6d232dd](https://github.com/joe-bor/family-hub-api/commit/6d232dddb73afb7d427d2e81cd5176f09766536a))
* **meals:** add optimistic locking and conflict handling ([c0b8e29](https://github.com/joe-bor/family-hub-api/commit/c0b8e29c61a8760ad8fc754efb7b8089f6e55d31))
* **meals:** add remove meal slot endpoint ([328b40b](https://github.com/joe-bor/family-hub-api/commit/328b40b7fc51145b6823bfb4f91d4344af985e9c))


### Bug Fixes

* **meals:** constrain meal enum columns in schema ([1cf0fc3](https://github.com/joe-bor/family-hub-api/commit/1cf0fc381d30046139b9ec707d1918d901eeb204))
* **meals:** delete source slot row on move ([cb93f9e](https://github.com/joe-bor/family-hub-api/commit/cb93f9ef993d99fed4df46bcc93f7c7b89e831d1))
* **meals:** enforce lowercase enum wire values ([30dc513](https://github.com/joe-bor/family-hub-api/commit/30dc513866807a909a0737c87832e9da5ab66fb9))
* **meals:** guard duplicate onto the same slot ([ebc23f8](https://github.com/joe-bor/family-hub-api/commit/ebc23f844c3b139570e9227231cf4c9334d53c2a))
* **meals:** harden board aggregation against duplicate slot rows ([f1ca2c0](https://github.com/joe-bor/family-hub-api/commit/f1ca2c06b32d15b24bed99fe5ced39975264a741))
* **meals:** ignore null meal extra entries ([22891bc](https://github.com/joe-bor/family-hub-api/commit/22891bcc76640edfbd79fd17039579fe554c1f92))
* **meals:** require dayIndex in slot requests ([b7f8e70](https://github.com/joe-bor/family-hub-api/commit/b7f8e70ef0287e4bbc66678573cc59d05095330e))
* **meals:** validate source slot on same-slot move ([feb2fb5](https://github.com/joe-bor/family-hub-api/commit/feb2fb5847f81e4e2251926782d7e5ed0a832609))
* **recipes:** address import safety review ([d84c1a9](https://github.com/joe-bor/family-hub-api/commit/d84c1a98fb046c6cdaa306968f2cf4659ce76bcd))
* **recipes:** harden import url handling and add read-time cap ([79cbb8e](https://github.com/joe-bor/family-hub-api/commit/79cbb8edf755eca967ea216275d3f8c02ea915da))


### Performance Improvements

* **meals:** fetch meal entry recipe lazily ([3efb096](https://github.com/joe-bor/family-hub-api/commit/3efb0966dd5b238c4c2754f8bba06043269617cd))


### Code Refactoring

* **meals:** drop dead role/sortOrder assignment in snapshot ([0a38398](https://github.com/joe-bor/family-hub-api/commit/0a38398a31a1d620250b6dd62e286c713d348559))


### Tests

* **meals:** cover validation and cross-week edge cases ([ab8def8](https://github.com/joe-bor/family-hub-api/commit/ab8def80bbf2753d7779333ca96ce7c15568c8ab))

## [1.4.1](https://github.com/joe-bor/family-hub-api/compare/v1.4.0...v1.4.1) (2026-06-01)


### Bug Fixes

* **datasource:** drain idle Hikari pool so Neon can autosuspend ([d4c56ac](https://github.com/joe-bor/family-hub-api/commit/d4c56ac8d55f6be8d2eee566ca152b314c2d56b8))

## [1.4.0](https://github.com/joe-bor/family-hub-api/compare/v1.3.0...v1.4.0) (2026-05-20)


### Features

* **chores:** add family timezone groundwork ([2afb705](https://github.com/joe-bor/family-hub-api/commit/2afb705b449462c9136b13ac9ad9815e66c976c4))
* **chores:** ship recurring chores board contract ([401bdfb](https://github.com/joe-bor/family-hub-api/commit/401bdfbb27db003f33f243077a9ca56d033a82c9))


### Bug Fixes

* harden chore completion idempotency ([fa5028e](https://github.com/joe-bor/family-hub-api/commit/fa5028e101eb78d1c64c0b235fe41410eec784b4))

## [1.3.0](https://github.com/joe-bor/family-hub-api/compare/v1.2.0...v1.3.0) (2026-05-07)


### Features

* **lists:** add backend list endpoints ([6730377](https://github.com/joe-bor/family-hub-api/commit/673037725fabb50ba6eba9f41faa041505515c5c))
* **lists:** seed backend list defaults ([03654d3](https://github.com/joe-bor/family-hub-api/commit/03654d39be468365ad8ba3ff3fdac15f15471ae4))


### Code Refactoring

* **lists:** drop createItem refetch and document PATCH semantics ([27649b5](https://github.com/joe-bor/family-hub-api/commit/27649b5bf2a0515ad5f354d684e894f204b99397))


### Tests

* **lists:** add backend list integration coverage ([f8295cd](https://github.com/joe-bor/family-hub-api/commit/f8295cdc4e5eee71d57d071237ea6e3e09950bd3))
* **lists:** cover showCompletedOverride null round-trip ([1460f49](https://github.com/joe-bor/family-hub-api/commit/1460f49090ad7c1ee9242e409e2db006c9650a1c))

## [1.2.0](https://github.com/joe-bor/family-hub-api/compare/v1.1.0...v1.2.0) (2026-05-05)


### Features

* **chores:** add chore integration coverage ([9cb2d91](https://github.com/joe-bor/family-hub-api/commit/9cb2d911095e38fd4cda00ed64e6df33a421d3a2))
* **chores:** implement family chore endpoints ([a88da81](https://github.com/joe-bor/family-hub-api/commit/a88da8147457852f7a7c51c05f6627f4f10a62a7))


### Bug Fixes

* **chores:** enforce assignee family invariant ([056ac9a](https://github.com/joe-bor/family-hub-api/commit/056ac9a42df8260ea7ac290c35d1b35554b47c93))

## [1.1.0](https://github.com/joe-bor/family-hub-api/compare/v1.0.1...v1.1.0) (2026-04-24)


### Features

* **logging:** add business event logging to service layer ([82f5217](https://github.com/joe-bor/family-hub-api/commit/82f521713e4ea0d4d3e26edebc60312ec7bfbe8b))
* **logging:** add request logging filter with correlation ID via MDC ([e69ad65](https://github.com/joe-bor/family-hub-api/commit/e69ad651d162cba311f01cba343b7f4e5056cbea))
* **logging:** add WARN/ERROR logging to GlobalExceptionHandler ([915864d](https://github.com/joe-bor/family-hub-api/commit/915864deb0fd1d2cfdc62c7ffe0b165717687bd2))


### Bug Fixes

* **ci:** pin actions/add-to-project to v1.0.2 (no v1 tag exists) ([#38](https://github.com/joe-bor/family-hub-api/issues/38)) ([dc53545](https://github.com/joe-bor/family-hub-api/commit/dc5354570b3d4f4646486c03477d54af1a22b676))
* **google-auth:** replace Map.of with typed GoogleAuthUrlResponse DTO ([314924a](https://github.com/joe-bor/family-hub-api/commit/314924a3a3962386fdd73b22a3716015078b751a))
* **logging:** address PR review feedback ([b942919](https://github.com/joe-bor/family-hub-api/commit/b9429191094d371739519c4e675a1b3cf3074628))
* **logging:** remove trailing space in request log format string ([517b4c8](https://github.com/joe-bor/family-hub-api/commit/517b4c8928a8e5d6598d495673327477fb59e308))
* **logging:** use MDC.remove() instead of MDC.clear() to avoid wiping other MDC keys ([b8b6d3d](https://github.com/joe-bor/family-hub-api/commit/b8b6d3da60d6ac6be0c3cfd1b955e03e40d5c1b4))


### Documentation

* add mentor-first CLAUDE.md with cross-repo product pointer ([#39](https://github.com/joe-bor/family-hub-api/issues/39)) ([a2f879a](https://github.com/joe-bor/family-hub-api/commit/a2f879a808ab80f32e68a4533452b08ba55409fb))


### Tests

* **google-auth:** update integration test for renamed url field ([c93bff7](https://github.com/joe-bor/family-hub-api/commit/c93bff7be25358d33e20d0a55a92e4564f4480b8))

## [1.0.1](https://github.com/joe-bor/family-hub-api/compare/v1.0.0...v1.0.1) (2026-03-20)


### Bug Fixes

* **ci:** fold retag job into release workflow ([dfd9e51](https://github.com/joe-bor/family-hub-api/commit/dfd9e514e79033045ab8c97f1ee7d55ef7f705ef))

## 1.0.0 (2026-03-20)


### Features

* `FamilyMember` model/entity that represents a household unit that belongs to a `Family` ([28be1ed](https://github.com/joe-bor/family-hub-api/commit/28be1edcf297e7d0fa816644b949a2631dad333a))
* add `Dockerfile` ([94b2d02](https://github.com/joe-bor/family-hub-api/commit/94b2d027f6fd8c84210218b3ddfa749d494256b3))
* add `ON DELETE CASCADE` so events get deleted when the family member it is assigned to is deleted ([eac4fe2](https://github.com/joe-bor/family-hub-api/commit/eac4fe2e73fd5864a0bb37aec96746e7f552324e))
* add 1:N relationship on existing Family model. ([00db8cd](https://github.com/joe-bor/family-hub-api/commit/00db8cd10bf19bd5c749aee178e044ad12d3154b))
* add endpoint consumed by FE during account creation, to check for username availability ([21b225b](https://github.com/joe-bor/family-hub-api/commit/21b225b6956bcd0414bf3838e8dd36ee88ee922a))
* add exception handling in our filterchain ([fd2d533](https://github.com/joe-bor/family-hub-api/commit/fd2d5337230822190f62ce27d8a9800c4c6d283a))
* add guard preventing start time after end time ([a413b33](https://github.com/joe-bor/family-hub-api/commit/a413b339ac22cd41753781b494722c3278c850b9))
* add method level security, preventing users to alter accounts that don't belong to them ([a243c83](https://github.com/joe-bor/family-hub-api/commit/a243c8343ce92b73169b9c45ade91c31c93bf345))
* add new exception for register requests containing username that is already in use ([0e8ec4d](https://github.com/joe-bor/family-hub-api/commit/0e8ec4d33bb0bda82f6aa4dd985e6f474363a891))
* add new exception handler for `InvalidCredentialException` ([a1fc7cd](https://github.com/joe-bor/family-hub-api/commit/a1fc7cd260ecc7cdcf1b83553b5a22e2fcc22f38))
* add optional filters for "get all events" functionality ([561a81d](https://github.com/joe-bor/family-hub-api/commit/561a81d8010e6666a224ceca4695c3824682163a))
* add regex for time format validation ([d289465](https://github.com/joe-bor/family-hub-api/commit/d289465ced302e9a7f462d056d47d91a6efbe3e5))
* add service responsible for generating and validating JWTs ([bccc124](https://github.com/joe-bor/family-hub-api/commit/bccc1248d48f89d5d528952d8fc2aa72165e1662))
* add update and delete functionality for calendar events on all layers ([e099572](https://github.com/joe-bor/family-hub-api/commit/e099572fecc0ad0d0d9f2ce740bbfbfc3ab4d341))
* added get all events, get events by id, and create event in controller, service, and repository layers ([1d13cff](https://github.com/joe-bor/family-hub-api/commit/1d13cffe259fd27bcb976c7a0e2931dfaf7ed747))
* **auth:** accept family name and members during registration ([4317059](https://github.com/joe-bor/family-hub-api/commit/4317059de5563154fbb1f167c779a92fdbedcc9b))
* **calendar:** add description to request and source/description to response DTOs ([f3a7cc4](https://github.com/joe-bor/family-hub-api/commit/f3a7cc4a1f870d0bed6c0262d4c86ee078e7dbf9))
* **calendar:** add endDate to model, DTOs, mapper, and migration ([4dcfde5](https://github.com/joe-bor/family-hub-api/commit/4dcfde58f8387388d412c0ee3c3d049138678427))
* **calendar:** add endDate validation rules ([07ea73a](https://github.com/joe-bor/family-hub-api/commit/07ea73a2afc532b2ae92afc680911be425144323))
* **calendar:** add EventSource enum (NATIVE, GOOGLE) ([0054e7b](https://github.com/joe-bor/family-hub-api/commit/0054e7b841c26521627d9a4ef8a5a1cf26acac93))
* **calendar:** add instance edit/delete endpoints for recurring events ([e3b1026](https://github.com/joe-bor/family-hub-api/commit/e3b1026fb8b9e99204e35dff034bf479c789fe17))
* **calendar:** add recurrence support — model, DTOs, validator, migration ([3193c70](https://github.com/joe-bor/family-hub-api/commit/3193c707ee666fc09f712d3d4d8c1266715fd00c))
* **calendar:** add source and description fields to CalendarEvent entity ([19858f9](https://github.com/joe-bor/family-hub-api/commit/19858f9a593ce7d9695c084965d60ac94b12919a))
* **calendar:** add V4 migration for source and description columns ([69eb6e7](https://github.com/joe-bor/family-hub-api/commit/69eb6e75a05a615492711c79da07f761162c2ec2))
* **calendar:** expand recurring events in GET endpoint ([fb5cd64](https://github.com/joe-bor/family-hub-api/commit/fb5cd64ec9731b239fe73b06a62de26f79babd7f))
* **calendar:** map description field on update and edit-instance ([ddb4078](https://github.com/joe-bor/family-hub-api/commit/ddb4078c7a8b64f9416a8cf8dd98a86d0c0b4dc9))
* **calendar:** map source and description in CalendarEventMapper ([5b1d19c](https://github.com/joe-bor/family-hub-api/commit/5b1d19c47bd61de0d0549a3c27fdb43b2e31724a))
* **calendar:** protect Google-sourced events from modification ([3913e4f](https://github.com/joe-bor/family-hub-api/commit/3913e4f82ac9bedd54460549994379b9c89b0419))
* **calendar:** update date range filtering for multi-day overlap ([5fd7bee](https://github.com/joe-bor/family-hub-api/commit/5fd7bee3d71dc0d20518aa8df044f17c06a7aa2b))
* **controller:** add calendar listing and selection endpoints ([c2782d3](https://github.com/joe-bor/family-hub-api/commit/c2782d358535220707ab15c7355986869ff4e145))
* **controller:** add POST /api/google/sync/{memberId} endpoint ([9dcf769](https://github.com/joe-bor/family-hub-api/commit/9dcf769828337175a8b51924ef633e4182df393e))
* create an enum for possible colors ([78a09d5](https://github.com/joe-bor/family-hub-api/commit/78a09d5dfb6fee8a20f04a0edd9fa79a3761d516))
* create baseline query (family, family_member, calendar_event tables and constraints) for flyway migration ([2fd4f0f](https://github.com/joe-bor/family-hub-api/commit/2fd4f0fb4ca5cc381798c6dd60da06786d2c3590))
* create CalendarEvent entity to represent the calendar_event table in our DB ([3fd877a](https://github.com/joe-bor/family-hub-api/commit/3fd877aa6b0eb3e05adf7815730b29e7053a9f99))
* create dto to represent family w/o exposing sensitive information ([425351e](https://github.com/joe-bor/family-hub-api/commit/425351e8def8660d9f4e103f842e07fe37aa7ac3))
* create DTOs with validations for auth-related request/response ([cc0c18a](https://github.com/joe-bor/family-hub-api/commit/cc0c18a02146882228282e4985c98fa0c283d319))
* create filter responsible for setting auth from JWT ([ac02a02](https://github.com/joe-bor/family-hub-api/commit/ac02a02e2aa212950a8340e202a416d07a633c8f))
* create generic DTO that wraps data, to be consumed directly by FrontEnd ([8cf9f4a](https://github.com/joe-bor/family-hub-api/commit/8cf9f4aae1a1813a82c113c60f1cc7d71691708d))
* create request-response DTO pair for CalendarEvent ([46f6f53](https://github.com/joe-bor/family-hub-api/commit/46f6f53d57e12d8a3284b720930d843b4c843528))
* create utility class that maps to and from calendar event DTOs and entity ([a1cd425](https://github.com/joe-bor/family-hub-api/commit/a1cd4259d72bf72cc9378ed024e9e3dccde35d93))
* CRUD endpoints for family members and corresponding service methods ([9a7d6ab](https://github.com/joe-bor/family-hub-api/commit/9a7d6ab6098e9ed7eda3261a8884c834014946b8))
* custom exception and its handler for "family member not found" ([638da99](https://github.com/joe-bor/family-hub-api/commit/638da99cd20012bca13be00bd015e87a4ed45da8))
* custom exception and its handler for bad requests (eg. an event containing startTime after endTime) ([3d76eef](https://github.com/joe-bor/family-hub-api/commit/3d76eefcddbcb18f5082890cc259008bf82faed4))
* **dto:** add htmlLink field to CalendarEventResponse ([c546eda](https://github.com/joe-bor/family-hub-api/commit/c546edaf6df975112a616d91ee2ef11f54cbfa71))
* enforced regex for usename coming from client ([d5aae34](https://github.com/joe-bor/family-hub-api/commit/d5aae3425410911ecf8d820bf5577daef2ccd6a5))
* **exception:** create a global exception handler ([083d088](https://github.com/joe-bor/family-hub-api/commit/083d088e829614ab01b6094aec693b6f0c0959a8))
* expose registration endpoint at `/api/auth/register` ([89a80a8](https://github.com/joe-bor/family-hub-api/commit/89a80a8148ac5d118a55f68386acd1fac0232b3b))
* externalize cors allowed origins to env variables read at deploy time ([00415b0](https://github.com/joe-bor/family-hub-api/commit/00415b0023c1f705515c4e91b18d0c56cbf7ca78))
* Family Vertical Slice ([b938401](https://github.com/joe-bor/family-hub-api/commit/b938401c780a77c9e16197a17b113caf92304917))
* **family:** created family entity with username and pw hash for future jwt ([42f8190](https://github.com/joe-bor/family-hub-api/commit/42f8190cb6cbe82e1cd6e369abbf1e280ee0ea64))
* **family:** created repository for family entity ([8a2ca52](https://github.com/joe-bor/family-hub-api/commit/8a2ca52c85e2fd2d77ea3fcb2a830e1bebeee3d3))
* **family:** implement basic controllers for family. ([772478e](https://github.com/joe-bor/family-hub-api/commit/772478ec9473bdf2e82c709f892af8d1986433b5))
* **family:** implement CRUD service for family. ([acf9d02](https://github.com/joe-bor/family-hub-api/commit/acf9d029e7e841022c875bebda9181e4be1e0f5d))
* implement login service ([0c1749d](https://github.com/joe-bor/family-hub-api/commit/0c1749d2d8ceadc8d4a61d8e7a9fcec704aa0931))
* implement service that handles registration ([a100e2c](https://github.com/joe-bor/family-hub-api/commit/a100e2cfbc8cd8e49102c792a267ba41ff151998))
* implement unique username validation ([5a838fd](https://github.com/joe-bor/family-hub-api/commit/5a838fd0191391946d4f7a37d1caf61ffbcbbd82))
* made repository layer return optional; to be handled by service layer ([198e6fa](https://github.com/joe-bor/family-hub-api/commit/198e6fafb1d9aa74dc152c4fad92a5ee2f96d5d0))
* make Family implement UserDetails, now representing an account for the entire family ([a9882d3](https://github.com/joe-bor/family-hub-api/commit/a9882d31ca2d5d322ee286b4d8537d79c0d4107a))
* **mapper:** add GoogleEventMapper with TDD tests ([7594104](https://github.com/joe-bor/family-hub-api/commit/75941049ede18aa507af8effda25bcf4e960daa3))
* **migration:** add google event columns to calendar_event table ([feee1ba](https://github.com/joe-bor/family-hub-api/commit/feee1bab1f90594d9a32c5dd1413fc175358f92a))
* **migration:** add google_synced_calendar table (V7) ([7a690ac](https://github.com/joe-bor/family-hub-api/commit/7a690aca63b0f63f731cb501ed69dcd6d27de0d7))
* **model:** add GoogleSyncedCalendar entity and repository ([5ac338b](https://github.com/joe-bor/family-hub-api/commit/5ac338b78e48916610fb73188620c64f83d5ab0a))
* **model:** add syncedCalendar and exdates fields to CalendarEvent ([f2559c1](https://github.com/joe-bor/family-hub-api/commit/f2559c1a606b4745ab9c452170e7a7eeacacfe09))
* **oauth:** add GoogleOAuthConfig and config properties ([b2e8683](https://github.com/joe-bor/family-hub-api/commit/b2e868381464d4a8e343fdb3fbefd6f979815c74))
* **oauth:** add GoogleOAuthController with auth, callback, status, disconnect ([f589659](https://github.com/joe-bor/family-hub-api/commit/f5896594f84295f8c81bb737885ef00276f324e5))
* **oauth:** add GoogleOAuthService with auth URL, token exchange, disconnect ([96bca13](https://github.com/joe-bor/family-hub-api/commit/96bca1395029965b714c22bb6fe087a322638d36))
* **oauth:** add GoogleOAuthToken entity ([f29534d](https://github.com/joe-bor/family-hub-api/commit/f29534d2146e0c75b7a03fce4df2742f8acbb34a))
* **oauth:** add GoogleOAuthTokenRepository ([b35ca6e](https://github.com/joe-bor/family-hub-api/commit/b35ca6e28adb8459a6b23990adb0e2322b87c988))
* **oauth:** add TokenEncryptionService with AES-256-GCM encryption ([0e0db38](https://github.com/joe-bor/family-hub-api/commit/0e0db386e9612749f587eea021b741190b4a5911))
* **oauth:** add V5 migration for google_oauth_token table ([d29f616](https://github.com/joe-bor/family-hub-api/commit/d29f616fcadccc8e827a3b683b2f9887e532ec76))
* **oauth:** cleanup synced calendars and Google events on disconnect ([aac68ac](https://github.com/joe-bor/family-hub-api/commit/aac68acd3f94c5fbc4d93a9ca5e49926c8d96428))
* **oauth:** whitelist Google callback endpoint in security config ([dd24d46](https://github.com/joe-bor/family-hub-api/commit/dd24d468886815a091b2d85381f2d7d4d17a185a))
* **repository:** add findByGoogleEventId and deleteByGoogleEventId ([3a0dfec](https://github.com/joe-bor/family-hub-api/commit/3a0dfec2b382e33aebd15fef1343d9f6dc220670))
* **scheduler:** add GoogleCalendarSyncScheduler with conditional loading ([ab02db0](https://github.com/joe-bor/family-hub-api/commit/ab02db0b2070e8383ca147d06cbf86b2652a2e06))
* **selection:** trigger full sync after calendar selection update ([0610318](https://github.com/joe-bor/family-hub-api/commit/0610318e13e2d9eb26d8c0a010fa9e6e7d53cdc3))
* **service:** add GoogleCalendarListService for calendar discovery ([29e5f75](https://github.com/joe-bor/family-hub-api/commit/29e5f7546b6d5c73b672ae76a542bce3d817e2b0))
* **service:** add GoogleCalendarSyncService with full sync logic ([4b1938e](https://github.com/joe-bor/family-hub-api/commit/4b1938e503bf91d24b5b265d27384fd795c3f69f))
* **service:** add GoogleCredentialService for token management ([563fda3](https://github.com/joe-bor/family-hub-api/commit/563fda30715a07ebc1f5813f8b47fbcfd3367b8e))
* **status:** include synced calendars in connection status response ([3a0f204](https://github.com/joe-bor/family-hub-api/commit/3a0f2043a685d4bd7070c1fc57fc48b453f6438f))
* **sync:** add fetchIncrementalEvents using Google sync token ([7400916](https://github.com/joe-bor/family-hub-api/commit/74009164232d0693940c1438a9b5aab8cd3a657e))
* **sync:** add persistIncrementalChanges with upsert logic ([ff93c95](https://github.com/joe-bor/family-hub-api/commit/ff93c95f654093ce250fa11d88b8bfbead1ff35e))
* **sync:** add updateExistingEvent for incremental upserts ([a26a67f](https://github.com/joe-bor/family-hub-api/commit/a26a67fb1d1d1efec70fb7def246217091a56a77))
* **sync:** make syncMember async, use event listener for post-selection sync ([98ae71b](https://github.com/joe-bor/family-hub-api/commit/98ae71b56ef00a0830e40657d74ba4da04ab2f53))


### Bug Fixes

* add generic catch-all to handle "deleted user after generating a token" ([c85131b](https://github.com/joe-bor/family-hub-api/commit/c85131bd8715ab9ba8456caae7672666f4dce284))
* **async:** add AsyncUncaughtExceptionHandler, remove dead try/catch ([24721d8](https://github.com/joe-bor/family-hub-api/commit/24721d8fec0c5532e517af155dc81b8b2b7051c9))
* **auth:** use saveAndFlush in register to populate createdAt ([1c1ff73](https://github.com/joe-bor/family-hub-api/commit/1c1ff738a2cad11bfbba3c4f4e8c3fa26ce132d5))
* **calendar:** account for endDate in single-param date filters ([03c63c4](https://github.com/joe-bor/family-hub-api/commit/03c63c4c7d5361e102563f56b2d213687f1aa15e))
* **calendar:** add @OnDelete CASCADE to recurringEvent FK ([54a5baa](https://github.com/joe-bor/family-hub-api/commit/54a5baa9d76aace335e528444aba81c1f2cfda46))
* **calendar:** add date filter to recurring parents query and fix JPQL spacing ([09be89b](https://github.com/joe-bor/family-hub-api/commit/09be89b00b121fa413f2d6c2b4489dd224b930b9))
* **calendar:** add unique constraint on (recurring_event_id, original_date) ([318f3c8](https://github.com/joe-bor/family-hub-api/commit/318f3c8b25002662a4f95ec37accc916f9b7eadc))
* **calendar:** add validation to editRecurringInstance and extract validateEvent ([be7d0fc](https://github.com/joe-bor/family-hub-api/commit/be7d0fc950147434fbfc0ac042e0aceff849009f))
* **calendar:** address PR review — sort, validation, cleanup ([526dd5f](https://github.com/joe-bor/family-hub-api/commit/526dd5fa584393e721eead4718d7f71f75d701af))
* **calendar:** copy recurrenceRule onto expanded virtual instances ([51a04c5](https://github.com/joe-bor/family-hub-api/commit/51a04c54f95bbf7940b55919d532b52b5ef1f7b1))
* **calendar:** remove recurrenceRule from virtual instance responses ([019b845](https://github.com/joe-bor/family-hub-api/commit/019b845eacb8e6c7fcfb32dd6b79444e43014ae0))
* **calendar:** update controller test to pass required date params ([a0d228c](https://github.com/joe-bor/family-hub-api/commit/a0d228caca6ccd5cbf8b078f8e1a7f21173ec4bc))
* **calendar:** virtual instances use id=null and recurringEventId=parentId ([874d340](https://github.com/joe-bor/family-hub-api/commit/874d340d537a610ba89ba15d70eecc7ba275673c))
* **ci:** add setup-buildx-action for GHA cache support ([b02b37e](https://github.com/joe-bor/family-hub-api/commit/b02b37e86d4e201114520bed5d9deb060e175acb))
* **ci:** add TOKEN_ENCRYPTION_KEY env var to build-and-test job ([a8178d2](https://github.com/joe-bor/family-hub-api/commit/a8178d2d461d3ace9884c3aac3f28c4c09bdd2eb))
* consolidate dev and prod filter chains & add stateless session management ([5b507fd](https://github.com/joe-bor/family-hub-api/commit/5b507fd16b81749146d3364f040a454960b9e27d))
* **dto:** add @NotNull validation to CalendarSelectionRequest.calendarIds ([95adfea](https://github.com/joe-bor/family-hub-api/commit/95adfead06974e6725e906fba828dd04811e0ef8))
* **family:** switched password hash field to camel case ([a19ea9e](https://github.com/joe-bor/family-hub-api/commit/a19ea9e60c2da6a267b5fac7a7dd63991c36b9e4))
* logging in with an invalid username and invalid credentials now return the same error, preventing attackers to determine which part was wrong ([a633982](https://github.com/joe-bor/family-hub-api/commit/a633982d9e2be8cdcff792d71d234b902ecb0033))
* made name and username field from Family required ([34c5f01](https://github.com/joe-bor/family-hub-api/commit/34c5f01779b5d79b215166624aa6697cb7771a01))
* **migration:** add unique constraint on (member_id, google_calendar_id) ([48112d3](https://github.com/joe-bor/family-hub-api/commit/48112d3b045e96938abc78dece96d2bdc473491b))
* **migration:** standardize family table timestamps to TIMESTAMPTZ ([cd23773](https://github.com/joe-bor/family-hub-api/commit/cd23773a297032a544fb11ccdfb444394d474402))
* **migration:** use TIMESTAMPTZ for google_updated_at column ([7663e7b](https://github.com/joe-bor/family-hub-api/commit/7663e7ba670d44e932f368bc3a453fe5186c6c45))
* **migration:** use TIMESTAMPTZ for Instant-mapped columns ([577e42e](https://github.com/joe-bor/family-hub-api/commit/577e42e095730b2989963ccf61fe93132b593cfd))
* missing @Component annotation ([077fb7c](https://github.com/joe-bor/family-hub-api/commit/077fb7ca0d5314ae9f2227edc3d5d797bbc71c7b))
* missing `/` for location ([f732091](https://github.com/joe-bor/family-hub-api/commit/f7320910e755d4b0a575d465816782b4cb4c4797))
* **oauth:** catch token exchange failures and redirect with error ([07e7e76](https://github.com/joe-bor/family-hub-api/commit/07e7e7630dd070cdd34d3e3c58421dd9821fd04c))
* **oauth:** handle consent denial gracefully in callback ([e32be29](https://github.com/joe-bor/family-hub-api/commit/e32be2906a61f5570808f59f8b8c31743b400e3e))
* **oauth:** reorder token validation before deleting existing token ([b76e63f](https://github.com/joe-bor/family-hub-api/commit/b76e63f1f6fc83995694c94dceeb541f7e7bff5b))
* prevent hash leaking into response ([6352e3c](https://github.com/joe-bor/family-hub-api/commit/6352e3c47b1917ea913acc023f8ae275351224aa))
* reduce avatarUrl bound to 255 to match DB default ([50f5014](https://github.com/joe-bor/family-hub-api/commit/50f5014b125305d6bfe6e37bfc55503c72f89f72))
* remove @ForeignKey annotation, this is moved to SQL ([b01d190](https://github.com/joe-bor/family-hub-api/commit/b01d19032743474c38f27ff59e557f3ae671ae55))
* **scheduler:** use @ConditionalOnExpression to guard against empty client-id ([264874b](https://github.com/joe-bor/family-hub-api/commit/264874bb82810e08c5c24387ee011ae8ae148678))
* **security:** add CSRF state protection to OAuth callback ([df3fb22](https://github.com/joe-bor/family-hub-api/commit/df3fb2276dad64181469749acdf2be9d93ff79b1))
* **security:** harden token encryption key validation ([c3fa3c9](https://github.com/joe-bor/family-hub-api/commit/c3fa3c9aeaf1391e1a2fd0e8c112f3af932ffbd5))
* **security:** revoke refresh token on disconnect ([3e3a22b](https://github.com/joe-bor/family-hub-api/commit/3e3a22b43bba52b4b37f947d6ba5336ca19898cd))
* **service:** catch RRULE expansion errors per parent to prevent crash ([1caed53](https://github.com/joe-bor/family-hub-api/commit/1caed53af84bffeed3a640df6d52950ac399511e))
* **service:** handle null calendar list items from Google API ([4262307](https://github.com/joe-bor/family-hub-api/commit/4262307cf1e834343a070a221c6f3c3b24207849))
* set response content type ([4ae0c91](https://github.com/joe-bor/family-hub-api/commit/4ae0c91438ea18b01b29ef425cd200ab7a4c6c35))
* switched from `familyResponseDto` to `data` more intuitive ([020f38a](https://github.com/joe-bor/family-hub-api/commit/020f38a1f65f5a5db1299f63b1326f10a0752d88))
* switched from `String` to `LocalDate` in event request/response DTOs to better handle edge-cases such as February 29 ([04730e1](https://github.com/joe-bor/family-hub-api/commit/04730e1ace3bda888d7a65d2b711ea0314a9dcdb))
* **sync:** delete once for multi-calendar sync, isolate fetch errors ([b66a8f4](https://github.com/joe-bor/family-hub-api/commit/b66a8f420c42bdfd84b678d33e06d901ed5fdb20))
* **sync:** extract client builder, guard against data loss when all fetches fail ([2d14c8e](https://github.com/joe-bor/family-hub-api/commit/2d14c8ec9126b49478f2a76796b31e39e935fc01))
* **sync:** preserve EXDATE entries from Google recurrence rules ([0bf5a8e](https://github.com/joe-bor/family-hub-api/commit/0bf5a8eff2b52b97998efc6f5f829087943f4737))
* **sync:** strengthen updateExistingEvent test assertions and fix stale javadoc ([3c0addd](https://github.com/joe-bor/family-hub-api/commit/3c0addd4f53861426872ea5254558ce50eae4687))
* **sync:** track calendar association per event, abort on partial fetch failure ([369b7ab](https://github.com/joe-bor/family-hub-api/commit/369b7ab4a899973d4560cacd9487295d31c87c6e))
* user UUID instead of username to look up users in jwt validation flow; this way updating username (or other fields) does not invalidate their existing jwt ([2687872](https://github.com/joe-bor/family-hub-api/commit/26878723940807c209df19f7bdfbc3d232640698))
* wrapped primitive, so it can be nullable (not part of request) making it optional ([de7b1dd](https://github.com/joe-bor/family-hub-api/commit/de7b1dd520f475d29af6dfd6289d6ef8217ccf42))


### Code Refactoring

* **api:** align controllers with FE contract and fix lazy loading ([5fd2b9b](https://github.com/joe-bor/family-hub-api/commit/5fd2b9b9ea744026127d0e08aa9c8b40858fe56d))
* **calendar:** require startDate and endDate query params ([aed0179](https://github.com/joe-bor/family-hub-api/commit/aed0179e753855faa0d08a3189949538b2aa8d13))
* consolidated per-resource exception into one generic resource not found exception that accepts resource name ([e5a3e15](https://github.com/joe-bor/family-hub-api/commit/e5a3e15281da5eda9c79e14fb83b8319f96855a2))
* **controller:** extract calendar selection logic to service ([edfa3d9](https://github.com/joe-bor/family-hub-api/commit/edfa3d9d09164d3a5370f3aee21f61805f854e62))
* **dto:** extract mappers and rename FamilyResponseDto to FamilyResponse ([9e4b9b9](https://github.com/joe-bor/family-hub-api/commit/9e4b9b9fcd1b5fecc54095db5ef94794880de002))
* **google-oauth:** replace raw Map with GoogleTokenResponse DTO ([85d407f](https://github.com/joe-bor/family-hub-api/commit/85d407ff18516804b934567d2153ecc5ad34299a))
* **google-oauth:** use service layer for member validation ([e521398](https://github.com/joe-bor/family-hub-api/commit/e5213985fb30733f296ac1384599ab5be3f598cd))
* **oauth:** squash V5+V6 migrations into single TIMESTAMPTZ migration ([1b3295c](https://github.com/joe-bor/family-hub-api/commit/1b3295c7609865bb2e75f3a1d79333c4d1abbeba))
* return DTOs from FamilyMemberService ([78cc902](https://github.com/joe-bor/family-hub-api/commit/78cc902d60d47e4708081b673cdb19297755be19))
* **service:** move connection status logic from controller to service ([cdcf83e](https://github.com/joe-bor/family-hub-api/commit/cdcf83ea318b472569848558aa61e7956be63151))
* **sync:** extract DB operations to separate @Transactional method ([785106d](https://github.com/joe-bor/family-hub-api/commit/785106d1d6788b245e68de65b9fb5350e2b0a107))
* **sync:** extract network I/O out of fullSync's @Transactional ([9a7afa7](https://github.com/joe-bor/family-hub-api/commit/9a7afa73f5aaf8fd5eba4f22d7b547ee4fcdca14))
* **sync:** extract shared saveGoogleEvents method from syncMember and fullSync ([c9f78c4](https://github.com/joe-bor/family-hub-api/commit/c9f78c4c4dfbeac9afbb90fb567192e8c5e04056))
* **sync:** rewrite syncMember for per-calendar isolation with incremental sync ([bb4569a](https://github.com/joe-bor/family-hub-api/commit/bb4569ae644f5d64bf0cdb9c664091be6b30bf93))
* **test:** use TestDataFactory constants in controller tests ([85c7cff](https://github.com/joe-bor/family-hub-api/commit/85c7cffe64ccd322439cdba051f8ae14907d303b))


### Documentation

* **mapper:** document timezone assumption on toZonedDateTime ([469cfe5](https://github.com/joe-bor/family-hub-api/commit/469cfe52028de609898bdfd157b2da9621700e2b))


### Tests

* add calendar selection unit and integration tests ([79c66a5](https://github.com/joe-bor/family-hub-api/commit/79c66a5364b1a1d7e59fb3c1892b6984de33fde3))
* **auth:** update mock to use saveAndFlush in register test ([f9cf4ef](https://github.com/joe-bor/family-hub-api/commit/f9cf4eff64a46c40aaa0844880da5f5c0aa003d7))
* **calendar:** add integration tests for source, description, and Google protection ([ccdda78](https://github.com/joe-bor/family-hub-api/commit/ccdda78b7ccf14f9de9fd28ad99d8df1a0f66b41))
* **calendar:** add multi-day event tests ([fb948ed](https://github.com/joe-bor/family-hub-api/commit/fb948edd05d434376e63ffa2e472b4c1c1e228bc))
* **google-oauth:** add exchangeCodeForTokens unit tests ([e271634](https://github.com/joe-bor/family-hub-api/commit/e271634930a3a91faf9542d42555a3d021cecd6d))
* **google-oauth:** validate encryption round-trip in integration test ([837386c](https://github.com/joe-bor/family-hub-api/commit/837386c1fc1ed21d35c385753e666aed3516ffa7))
* **infra:** add test configuration and shared utilities ([dbc6f60](https://github.com/joe-bor/family-hub-api/commit/dbc6f6018953e2ccf7bba91b6f26e21eb4be4120))
* **integration:** add full-stack integration tests ([e1bc959](https://github.com/joe-bor/family-hub-api/commit/e1bc9592477368e0a2fbe0ccbe9870b60d8b9e47))
* **integration:** add Google Calendar sync end-to-end tests ([4015978](https://github.com/joe-bor/family-hub-api/commit/4015978642ee40f968eeba45e3ee45a6e840d7fc))
* **integration:** assert native events survive Google disconnect ([f337cc6](https://github.com/joe-bor/family-hub-api/commit/f337cc6922730d5bdd27ba276336d1f21d5ba3ec))
* **oauth:** add GoogleOAuthController unit tests ([1333a5f](https://github.com/joe-bor/family-hub-api/commit/1333a5fb453709ba54eb5bdd2b4ecf360875eb4c))
* **oauth:** add integration tests for Google OAuth flow ([8d07e91](https://github.com/joe-bor/family-hub-api/commit/8d07e91b0fddb33869d9735b3cd46396033cbe31))
* **scheduler:** add integration tests for conditional bean loading ([488ddc7](https://github.com/joe-bor/family-hub-api/commit/488ddc7fd7439da0c3c0760d693bfae5f7af0ebe))
* **slice:** add controller tests with @WebMvcTest ([310620f](https://github.com/joe-bor/family-hub-api/commit/310620f0ed2dc70081e90dd421084f7e79678d4e))
* **slice:** add repository tests with @DataJpaTest ([e1b759a](https://github.com/joe-bor/family-hub-api/commit/e1b759a839b63ac992f57e8f0574b1c80e7c0878))
* **sync:** add cascade delete and upsert integration tests ([3dafe33](https://github.com/joe-bor/family-hub-api/commit/3dafe33c45f1027cb6d344ab7746627585999d08))
* **sync:** add disconnect cleanup integration test ([ae52a45](https://github.com/joe-bor/family-hub-api/commit/ae52a4534d6551ae26e06af8a6c64f8cff7c9eda))
* **sync:** add unit tests for incremental persistence edge cases ([171c99b](https://github.com/joe-bor/family-hub-api/commit/171c99b21a488fffc31645511c166e85e5710751))
* **unit:** add FamilyService.findFamilyResponse tests ([05e7dd2](https://github.com/joe-bor/family-hub-api/commit/05e7dd239cf94660c8c152be12642f73d5b9be14))
* **unit:** add mapper unit tests ([274be0a](https://github.com/joe-bor/family-hub-api/commit/274be0a69319946758724452150b9fa6b2ef38bf))
* **unit:** add service layer unit tests ([dcb616a](https://github.com/joe-bor/family-hub-api/commit/dcb616a89472aebec3d820fea30d5ffaf0f279f5))
