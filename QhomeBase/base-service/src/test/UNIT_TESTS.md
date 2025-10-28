# Controller Unit Tests Summary

## BuildingController (25 tests)

| #   | Test Name                                                        | Endpoint/Method                                       | Scenario          | Expected Result                   |
| --- | ---------------------------------------------------------------- | ----------------------------------------------------- | ----------------- | --------------------------------- |
| 1   | findAll_missingTenantId_returnsBadRequest                        | GET `/api/buildings?tenantId=`                        | Missing tenantId  | 400 Bad Request                   |
| 2   | findAll_ok_returnsList                                           | GET `/api/buildings?tenantId={tid}`                   | Valid tenantId    | 200 OK, list returned             |
| 3   | getBuildingById_ok                                               | GET `/api/buildings/{id}`                             | Existing building | 200 OK with `BuildingDto`         |
| 4   | getBuildingById_ok_returnsBody                                   | GET `/api/buildings/{id}`                             | Existing building | Body equals stubbed dto           |
| 5   | getBuildingById_notFound_returns404                              | GET `/api/buildings/{id}`                             | Not found         | 404 Not Found                     |
| 6   | createBuilding_ok                                                | POST `/api/buildings`                                 | Valid create      | 200 OK with `BuildingDto`         |
| 7   | createBuilding_serviceThrows_unhandled_propagates                | POST `/api/buildings`                                 | Service throws    | Exception propagated              |
| 8   | updateBuilding_ok                                                | PUT `/api/buildings/{id}`                             | Valid update      | 200 OK with `BuildingDto`         |
| 9   | updateBuilding_serviceCalledWithAuth                             | PUT `/api/buildings/{id}`                             | Verify args       | Service called with id, req, auth |
| 10  | updateBuilding_notFound_returns404                               | PUT `/api/buildings/{id}`                             | Not found         | 404 Not Found                     |
| 11  | createBuildingDeletionRequest_ok                                 | POST `/api/buildings/{id}/deletion-request`           | Valid             | 200 OK with request               |
| 12  | createBuildingDeletionRequest_illegalArgument_returnsBadRequest  | POST `/api/buildings/{id}/deletion-request`           | IllegalArgument   | 400 Bad Request                   |
| 13  | createBuildingDeletionRequest_illegalState_returnsBadRequest     | POST `/api/buildings/{id}/deletion-request`           | IllegalState      | 400 Bad Request                   |
| 14  | approveBuildingDeletionRequest_ok                                | POST `/api/buildings/deletion-requests/{rid}/approve` | Valid             | 200 OK                            |
| 15  | approveBuildingDeletionRequest_illegalArgument_returnsBadRequest | POST `/approve`                                       | IllegalArgument   | 400 Bad Request                   |
| 16  | approveBuildingDeletionRequest_illegalState_returnsBadRequest    | POST `/approve`                                       | IllegalState      | 400 Bad Request                   |
| 17  | rejectBuildingDeletionRequest_ok                                 | POST `/api/buildings/deletion-requests/{rid}/reject`  | Valid             | 200 OK                            |
| 18  | rejectBuildingDeletionRequest_illegalArgument_returnsBadRequest  | POST `/reject`                                        | IllegalArgument   | 400 Bad Request                   |
| 19  | rejectBuildingDeletionRequest_illegalState_returnsBadRequest     | POST `/reject`                                        | IllegalState      | 400 Bad Request                   |
| 20  | getPendingBuildingDeletionRequests_ok                            | GET `/api/buildings/deletion-requests/pending`        | Fetch pending     | 200 OK list                       |
| 21  | getBuildingDeletionRequest_ok                                    | GET `/api/buildings/deletion-requests/{rid}`          | Existing request  | 200 OK with dto                   |
| 22  | getBuildingDeletionRequest_notFound_returns404                   | GET `/deletion-requests/{rid}`                        | Not found         | 404 Not Found                     |
| 23  | doBuildingDeletion_ok                                            | POST `/api/buildings/{id}/do`                         | Perform deletion  | 200 OK with message               |
| 24  | getDeletingBuildings_accessDenied_throws                         | GET `/api/buildings/deleting`                         | Not authorized    | AccessDeniedException             |
| 25  | getDeletingBuildings_allowed_ok                                  | GET `/api/buildings/deleting`                         | Authorized        | Returns list                      |

## UnitController (25 tests)

| #   | Test Name                                        | Endpoint/Method                                      | Scenario        | Expected Result          |
| --- | ------------------------------------------------ | ---------------------------------------------------- | --------------- | ------------------------ |
| 1   | createUnit_ok                                    | POST `/api/units`                                    | Valid create    | 200 OK with `UnitDto`    |
| 2   | createUnit_illegalArgument_returnsBadRequest     | POST `/api/units`                                    | IllegalArgument | 400 Bad Request          |
| 3   | createUnit_illegalState_returnsBadRequest        | POST `/api/units`                                    | IllegalState    | 400 Bad Request          |
| 4   | createUnit_unhandledException_propagates         | POST `/api/units`                                    | Service throws  | Exception propagated     |
| 5   | updateUnit_ok                                    | PUT `/api/units/{id}`                                | Valid update    | 200 OK with `UnitDto`    |
| 6   | updateUnit_illegalArgument_returnsNotFound       | PUT `/api/units/{id}`                                | Not found       | 404 Not Found            |
| 7   | updateUnit_unhandledException_propagates         | PUT `/api/units/{id}`                                | Service throws  | Exception propagated     |
| 8   | deleteUnit_ok                                    | DELETE `/api/units/{id}`                             | Valid id        | 200 OK                   |
| 9   | deleteUnit_illegalArgument_returnsNotFound       | DELETE `/api/units/{id}`                             | Not found       | 404 Not Found            |
| 10  | deleteUnit_unhandledException_propagates         | DELETE `/api/units/{id}`                             | Service throws  | Exception propagated     |
| 11  | getUnitById_ok                                   | GET `/api/units/{id}`                                | Existing unit   | 200 OK with dto          |
| 12  | getUnitById_illegalArgument_returnsNotFound      | GET `/api/units/{id}`                                | Not found       | 404 Not Found            |
| 13  | getUnitById_unhandledException_propagates        | GET `/api/units/{id}`                                | Service throws  | Exception propagated     |
| 14  | getUnitsByBuildingId_ok                          | GET `/api/units/building/{buildingId}`               | Valid building  | 200 OK list              |
| 15  | getUnitsByBuildingId_returnsListSize             | GET `/building/{buildingId}`                         | Multi items     | List size matches        |
| 16  | getUnitsByTenantId_ok                            | GET `/api/units/tenant/{tenantId}`                   | Valid tenant    | 200 OK list              |
| 17  | getUnitsByTenantId_returnsListSize               | GET `/tenant/{tenantId}`                             | Multi items     | List size matches        |
| 18  | getUnitsByFloor_ok                               | GET `/api/units/building/{buildingId}/floor/{floor}` | Valid floor     | 200 OK list              |
| 19  | getUnitsByFloor_callsServiceWithParams           | GET `/building/{buildingId}/floor/{floor}`           | Verify args     | Called with id+floor     |
| 20  | changeUnitStatus_ok                              | PATCH `/api/units/{id}/status`                       | Valid status    | 200 OK                   |
| 21  | changeUnitStatus_illegalArgument_returnsNotFound | PATCH `/api/units/{id}/status`                       | Not found       | 404 Not Found            |
| 22  | changeUnitStatus_unhandledException_propagates   | PATCH `/api/units/{id}/status`                       | Service throws  | Exception propagated     |
| 23  | createUnit_passesDtoToService                    | POST `/api/units`                                    | Verify args     | Passed dto to service    |
| 24  | updateUnit_passesDtoAndId                        | PUT `/api/units/{id}`                                | Verify args     | Passed dto+id to service |
| 25  | changeUnitStatus_passesStatusToService           | PATCH `/api/units/{id}/status`                       | Verify args     | Passed id+status         |
