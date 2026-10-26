# Proctor report

## Checked on

| Suite | Platform | Device | OS | Model version | Backend | Prompt | Availability | Run at |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| ticket-classifier | ios-simulator | iPhone19,2 | Version 27.0 (Build 24A434), simulator on host Darwin 27.0.0 | iOS 27.0 sim on Darwin 27.0.0 (os-derived) | foundation-models | v1 | available | 2026-10-26T00:20:35.110628Z |
| ticket-classifier | ios-simulator | iPhone19,2 | Version 27.0 (Build 24A434), simulator on host Darwin 27.0.0 | iOS 27.0 sim on Darwin 27.0.0 (os-derived) | foundation-models | v2 | available | 2026-10-26T00:21:07.182325Z |
| ticket-extraction | ios-simulator | iPhone19,2 | Version 27.0 (Build 24A434), simulator on host Darwin 27.0.0 | iOS 27.0 sim on Darwin 27.0.0 (os-derived) | foundation-models | v1 | available | 2026-10-26T00:21:37.913947Z |

## Results

| Run | Cases | Passed | Failed | Not graded | Mean score | Verdict |
| --- | --- | --- | --- | --- | --- | --- |
| ticket-classifier · ios-simulator iOS 27.0 sim on Darwin 27.0.0 (os-derived) · prompt v1 | 12 | 11 | 1 | 0 | 92% | pass |
| ticket-classifier · ios-simulator iOS 27.0 sim on Darwin 27.0.0 (os-derived) · prompt v2 | 12 | 11 | 1 | 0 | 97% | pass |
| ticket-extraction · ios-simulator iOS 27.0 sim on Darwin 27.0.0 (os-derived) · prompt v1 | 4 | 2 | 2 | 0 | 92% | pass |

## Compare: ticket-classifier

Base ticket-classifier · ios-simulator iOS 27.0 sim on Darwin 27.0.0 (os-derived) · prompt v1 against ticket-classifier · ios-simulator iOS 27.0 sim on Darwin 27.0.0 (os-derived) · prompt v2.
Differs in prompt version: v1 to v2.

Score 92% to 97% (+6 pts). 0 regressed, 0 improved.

| Case | Base | Other | Change |
| --- | --- | --- | --- |
| double-charge | pass | pass | Same |
| refund | pass | pass | Same |
| invoice-name | pass | pass | Same |
| crash-camera | pass | pass | Same |
| search-empty | pass | pass | Same |
| dark-mode-contrast | pass | pass | Same |
| reset-email | pass | pass | Same |
| change-email | pass | pass | Same |
| export-csv | pass | pass | Same |
| widget | pass | pass | Same |
| thanks | pass | pass | Same |
| office | fail | fail | Same |

## Cases: ticket-classifier · ios-simulator iOS 27.0 sim on Darwin 27.0.0 (os-derived) · prompt v1

| Case | Input | Expected | Output (first attempt) | Result | Score | Spread |
| --- | --- | --- | --- | --- | --- | --- |
| double-charge | My card was charged twice for the same order | Billing | Billing | pass | 100% | 0.0 |
| refund | I need a refund for the annual plan I never used | Billing | Billing | pass | 100% | 0.0 |
| invoice-name | The invoice shows a company name I did not enter | Billing | Billing | pass | 100% | 0.0 |
| crash-camera | The app crashes as soon as I open the camera screen | Bug | Bug | pass | 100% | 0.0 |
| search-empty | Search returns nothing even though the item exists | Bug | Bug | pass | 100% | 0.0 |
| dark-mode-contrast | Dark mode turns the text black on a black background | Bug | Bug | pass | 100% | 0.0 |
| reset-email | I forgot my password and the reset email never arrives | Account | Account | pass | 100% | 0.0 |
| change-email | Please change the email address on my profile | Account | Account | pass | 100% | 0.0 |
| export-csv | Can you add an option to export my data as CSV? | Feature | Feature | pass | 100% | 0.0 |
| widget | It would be great to have a widget for the home screen | Feature | Feature | pass | 100% | 0.0 |
| thanks | Thanks for the quick help last week, just saying hi | Other | Other | pass | 100% | 0.0 |
| office | Where is your office located? | Other | Account | fail | 0% | 0.0 |

## Cases: ticket-classifier · ios-simulator iOS 27.0 sim on Darwin 27.0.0 (os-derived) · prompt v2

| Case | Input | Expected | Output (first attempt) | Result | Score | Spread |
| --- | --- | --- | --- | --- | --- | --- |
| double-charge | My card was charged twice for the same order | Billing | Billing | pass | 100% | 0.0 |
| refund | I need a refund for the annual plan I never used | Billing | Billing | pass | 100% | 0.0 |
| invoice-name | The invoice shows a company name I did not enter | Billing | Billing | pass | 100% | 0.0 |
| crash-camera | The app crashes as soon as I open the camera screen | Bug | Bug | pass | 100% | 0.0 |
| search-empty | Search returns nothing even though the item exists | Bug | Bug | pass | 100% | 0.0 |
| dark-mode-contrast | Dark mode turns the text black on a black background | Bug | Bug | pass | 100% | 0.0 |
| reset-email | I forgot my password and the reset email never arrives | Account | Account | pass | 100% | 0.0 |
| change-email | Please change the email address on my profile | Account | Account | pass | 100% | 0.0 |
| export-csv | Can you add an option to export my data as CSV? | Feature | Feature | pass | 100% | 0.0 |
| widget | It would be great to have a widget for the home screen | Feature | Feature | pass | 100% | 0.0 |
| thanks | Thanks for the quick help last week, just saying hi | Other | Other | pass | 100% | 0.0 |
| office | Where is your office located? | Other | Account | fail | 67% | 0.47 |

## Cases: ticket-extraction · ios-simulator iOS 27.0 sim on Darwin 27.0.0 (os-derived) · prompt v1

| Case | Input | Expected | Output (first attempt) | Result | Score | Spread |
| --- | --- | --- | --- | --- | --- | --- |
| blender | Order 48213: the blender stopped working. This is urgent, I need it for an event tomorrow. | {"orderId":"48213","product":"blender","urgent":true} | {"orderId": "48213", "product": "blender", "urgent": true} | pass | 100% | 0.0 |
| headphones | My headphones arrived scratched. Order A-77. | {"orderId":"A-77","product":"headphones","urgent":false} | {"orderId": "A-77", "product": "headphones", "urgent": true} | fail | 83% | 0.0 |
| dashboard | I cannot log in to the web dashboard, blocking my whole team right now. | {"orderId":null,"product":"web dashboard","urgent":true} | {"orderId": null, "product": null, "urgent": true} | fail | 83% | 0.0 |
| shipping | Just wondering if you ship to Norway. | {"orderId":null,"product":null,"urgent":false} | {"orderId": null, "product": null, "urgent": false} | pass | 100% | 0.0 |
