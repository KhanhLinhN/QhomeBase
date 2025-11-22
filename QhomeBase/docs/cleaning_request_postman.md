# Testing Cleaning Request Workflow via Postman

This document describes the APIs involved in the resend/timeout workflow and how to trigger them manually in Postman.

## 1. Create the cleaning request

- **Endpoint:** `POST /api/cleaning-requests`
- **Description:** Creates a request. Admin must approve; otherwise the resend logic kicks in once the reminder threshold passes.
- **Sample JSON body**
  ```json
  {
    "unitId": "11111111-2222-3333-4444-555555555555",
    "cleaningType": "Dọn dẹp tổng thể",
    "cleaningDate": "2025-12-01",
    "startTime": "08:00:00",
    "durationHours": 2.0,
    "location": "Căn hộ 2205",
    "note": "Dọn sạch phòng khách",
    "contactPhone": "0900000000",
    "extraServices": ["Giặt rèm"],
    "paymentMethod": "PAY_LATER"
  }
  ```
- **Headers:** Authorization (Bearer token for a resident).

After submitting, this request is tracked by `createdAt` and `status = PENDING`.

## 2. Check existing requests (optional)

- **Endpoint:** `GET /api/cleaning-requests/my`
- **Description:** Returns all requests for the authenticated resident. Use this to fetch the `requestId` you need for resend.

## 3. Resend the request

- **Endpoint:** `POST /api/cleaning-requests/{requestId}/resend`
- **Description:** Only works when the request is still `PENDING` and not approved. It resets the `lastResentAt` timestamp and clears the alert flag.
- **Example URL:** `POST /api/cleaning-requests/aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee/resend`
- **Response:** Updated `CleaningRequestDto` with `lastResentAt` now set and `resendAlertSent = false`.

## 4. Cancel a request

- **Cleaning cancel endpoint:** `PATCH /api/cleaning-requests/{requestId}/cancel`
- **Maintenance cancel endpoint:** `PATCH /api/maintenance-requests/{requestId}/cancel`
- **Description:** Allows the resident to cancel a request while it is `PENDING` or `IN_PROGRESS`. The response returns the updated DTO with `status = CANCELLED`.

```bash
curl -X PATCH http://localhost:8081/api/cleaning-requests/aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee/cancel \
  -H "Authorization: Bearer <resident-token>"
```

Canceled requests are treated as final by the scheduler, so the resident can create a new request immediately after cancellation.

## 5. Testing the scheduled workflow

The reminder/auto-cancel thresholds can be shortened during tests by setting the environment variables before starting the service:

| Property | Env var | Meaning | Test-friendly default |
| --- | --- | --- | --- |
| `cleaning.request.reminder.threshold` | `CLEANING_REQUEST_REMINDER_THRESHOLD` | How long to wait before issuing the resend reminder | `PT5M` (5 minutes) |
| `cleaning.request.cancel.threshold` | `CLEANING_REQUEST_CANCEL_THRESHOLD` | How long to wait before auto-canceling after the most recent send | `PT24H` (default) |

You can also adjust `cleaning.request.monitor.delay` to control how often the scheduler runs (default `900000` ms). To test immediately, set reminder threshold to `PT5M` (or shorter) and restart the service.

## 6. Notifications

When the scheduler detects a request past the reminder threshold, it marks `resendAlertSent = true` and sends a notification via the Notification service. After 24 hours with no approval, the request is cancelled and the resident receives another notification. You can monitor `resendAlertSent` and `status` in the response to verify behavior.


