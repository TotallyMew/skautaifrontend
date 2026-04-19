# CLAUDE.md
This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.
## Build & Development Commands
```bash
./gradlew assembleDebug          # Build debug APK
./gradlew assembleRelease        # Build release APK
./gradlew test                   # Run unit tests
./gradlew connectedAndroidTest   # Run instrumented tests (requires device/emulator)
./gradlew lint                   # Run lint checks
```
## Architecture
**Clean Architecture + MVVM + Repository pattern** for a scout inventory management app (Lithuanian: skautų inventoriaus valdymas).
**Layers:**
- `ui/` — Jetpack Compose screens + Hilt ViewModels per feature (inventory, members, requests, reservations, superadmin)
- `data/repository/` — Repository interfaces bridging ViewModels to remote data; use `Result<T>` for error propagation
- `data/remote/` — Retrofit API service interfaces + DTOs for each domain entity
- `di/` — Hilt modules: `NetworkModule` (Retrofit, OkHttp, all API services), `RepositoryModule` (repositories with token injection)
- `util/` — `TokenManager` (DataStore-backed JWT + user info persistence)
**Navigation:** Single `AppNavGraph.kt` composable with typed route objects in `NavRoutes`. Supports deep linking via `skautai://` URI scheme (used for super admin flow).
## Key Patterns
**ViewModel state:**
```kotlin
@HiltViewModel
class FooViewModel @Inject constructor(...) : ViewModel() {
    private val _uiState = MutableStateFlow(FooUiState())
    val uiState = _uiState.asStateFlow()
}
data class FooUiState(val isLoading: Boolean = false, ...)
```
**Screen consumption:**
```kotlin
val uiState by viewModel.uiState.collectAsStateWithLifecycle()
LaunchedEffect(Unit) { viewModel.loadData() }
```
**API calls in ViewModels** use `viewModelScope.launch` and `repository.doSomething()` returning `Result<T>` — handle `onSuccess`/`onFailure` to update state.
**Authentication:** JWT token stored via `TokenManager` (DataStore). `NetworkModule` injects the token into OkHttp interceptor for all authenticated requests.
## Tech Stack
- **Language/SDK:** Kotlin 2.1.0, compileSdk 36, minSdk 26, Java 11
- **UI:** Jetpack Compose (BOM 2026.03.01), Material 3, Navigation Compose
- **DI:** Hilt 2.56.1 with KSP
- **Networking:** Retrofit 3.0.0 + OkHttp 5.3.2 + Gson
- **Local storage:** DataStore Preferences (tokens/user info); Room is a dependency but not actively used
- **Dependencies managed via** `gradle/libs.versions.toml` version catalog
## Domain Features
- **Auth:** Login, registration via invitation link, tuntas (scout unit) selection/switching
- **Inventory:** CRUD for items with categories (Collective, Assigned, Individual), organisational unit scoping
- **Members:** Member lists, role management, invitation system
- **Reservations:** Equipment borrow/return tracking
- **Requests:** Equipment request workflow
- **Super admin:** Cross-unit dashboard, accessible via `skautai://superadmin` deep link

## Recent Major Refactor (April 2026) — Critical Context

### Item Ownership Model REPLACED
Old fields DELETED everywhere (backend + Android): `ownerType`, `ownerId`, `originalOwnerId`
New fields:
- `custodianId` (nullable) — unit currently holding the item. NULL = Tuntas storage
- `custodianName` (nullable) — resolved name, response only
- `origin` — `UNIT_ACQUIRED` or `TRANSFERRED_FROM_TUNTAS` only. No other values accepted.

### Organizational Unit Types REPLACED
Old types deleted: `DRAUGOVE`, `SKILTIS`, `GAUJA`, `BURELI`, `VYRESNIUJU_DRAUGOVE`
Valid types now: `VILKU_DRAUGOVE`, `SKAUTU_DRAUGOVE`, `PATYRUSIU_SKAUTU_DRAUGOVE`, `GILDIJA`, `VYR_SKAUTU_VIENETAS`, `VYR_SKAUCIU_VIENETAS`
New field: `subtype` — `DRAUGOVE` or `BURELIS` (only for VYR units)

### Unit Membership Table REPLACED
`user_draugove_memberships` → `unit_assignments`
`isLent: Boolean` → `assignmentType: String` (`MEMBER` or `VADOVO_PADEJEJAS`)
Response class: `DraugoveMembershipResponse` → `UnitMembershipResponse`
Request class: `AssignDraugoveMembershipRequest` → `AssignUnitMemberRequest`

### Renamed Fields (backend + Android DTOs)
- `draugoveId` → `requestingUnitId` in BendrasRequestDto and CreateBendrasRequestDto
- `draugoveName` → `requestingUnitName` in BendrasRequestDto
- `ownerType` query param → `custodianId` in GET /api/items

### New Fields Added
- `requestingUnitId` added to ReservationDto and CreateReservationRequestDto
- `subtype` added to OrganizationalUnitDto

### Permissions Renamed
- `OWN_DRAUGOVE` → `OWN_UNIT`
- `draugove.members.manage` → `unit.members.manage`
- `items.request.approve.draugove` → `items.request.approve.unit`

### Ranks Changed
Removed: `Suaugęs skautybėje`
Added: `Vilkas`, `Vyr. skautas kandidatas`, `Vyr. skautas`, `Vadovas`

### New Leadership Roles Added
For Gildija: `Gildijos pirmininkas`, `Gildijos pirmininko pavaduotojas`
For Vyr. vienetas: draugininkas/pirmininkas + pavaduotojas for all four combinations

### Database
Schema managed manually in pgAdmin. No SchemaUtils. Backend does NOT auto-create tables.
Source of truth: `schema.sql` in backend repo root.
Backend table `bendras_inventory_requests` column `draugove_id` → `requesting_unit_id`
Backend table `reservations` has new column `requesting_unit_id`
Backend table `item_transfers` redesigned: removed ownership columns, uses `from_custodian_id` and `to_custodian_id`

### Backend Stack
- Kotlin/Ktor 3.4.1, Exposed 0.54.0, PostgreSQL
- Located in `skautu-inventoriaus-valdymas-backend/`
- Roles seeded on tuntas registration via `PermissionSeeder.kt`
- Security resolved in `Security.kt` — `OWN_UNIT` scope checks against all unit IDs from both `UserLeadershipRoles` and `UnitAssignments`