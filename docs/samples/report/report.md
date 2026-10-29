# Proctor report

## Checked on

| Suite | Platform | Device | OS | Model version | Backend | Prompt | Availability | Run at |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| ticket-classifier | ios-simulator | iPhone19,2 | Version 27.0 (Build 24A434), simulator on host Darwin 27.0.0 | iOS 27.0 sim on Darwin 27.0.0 (os-derived) | foundation-models | v1 | available | 2026-10-26T00:20:35.110628Z |
| ticket-classifier | ios-simulator | iPhone19,2 | Version 27.0 (Build 24A434), simulator on host Darwin 27.0.0 | iOS 27.0 sim on Darwin 27.0.0 (os-derived) | foundation-models | v2 | available | 2026-10-26T00:21:07.182325Z |
| ticket-classifier | android | Google Pixel 11 Pro | Android 17 (API 37, build CD1A.261005.003) | nano-v4-fast | mlkit-genai-prompt preview/fast | v1 | available | 2026-10-29T14:01:12.964471Z |
| ticket-classifier | android | Google Pixel 11 Pro | Android 17 (API 37, build CD1A.261005.003) | nano-v4-fast | mlkit-genai-prompt preview/fast | v2 | available | 2026-10-29T14:01:54.380611Z |
| ticket-classifier | android | Google Pixel 11 Pro | Android 17 (API 37, build CD1A.261005.003) | nano-v4-full | mlkit-genai-prompt preview/full | v1 | available | 2026-10-29T14:09:34.419629Z |
| ticket-classifier | android | Google Pixel 11 Pro | Android 17 (API 37, build CD1A.261005.003) | nano-v4-full | mlkit-genai-prompt preview/full | v2 | available | 2026-10-29T14:10:21.130233Z |
| ticket-classifier | android | Google Pixel 11 Pro | Android 17 (API 37, build CD1A.261005.003) | nano-v4-full | mlkit-genai-prompt stable/full | v1 | available | 2026-10-29T14:30:23.099710Z |
| ticket-classifier | android | Google Pixel 11 Pro | Android 17 (API 37, build CD1A.261005.003) | nano-v4-full | mlkit-genai-prompt stable/full | v2 | available | 2026-10-29T14:31:04.289800Z |
| ticket-classifier | android | Google Pixel 11 Pro | Android 17 (API 37, build CD1A.261005.003) | nano-v4-fast | mlkit-genai-prompt stable/fast | v1 | available | 2026-10-29T14:33:38.391955Z |
| ticket-classifier | android | Google Pixel 11 Pro | Android 17 (API 37, build CD1A.261005.003) | nano-v4-fast | mlkit-genai-prompt stable/fast | v2 | available | 2026-10-29T14:34:19.541792Z |
| ticket-extraction | ios-simulator | iPhone19,2 | Version 27.0 (Build 24A434), simulator on host Darwin 27.0.0 | iOS 27.0 sim on Darwin 27.0.0 (os-derived) | foundation-models | v1 | available | 2026-10-26T00:21:37.913947Z |
| ticket-extraction | android | Google Pixel 11 Pro | Android 17 (API 37, build CD1A.261005.003) | nano-v4-fast | mlkit-genai-prompt preview/fast | v1 | available | 2026-10-29T14:02:35.633296Z |
| ticket-extraction | android | Google Pixel 11 Pro | Android 17 (API 37, build CD1A.261005.003) | nano-v4-full | mlkit-genai-prompt preview/full | v1 | available | 2026-10-29T14:11:06.107860Z |
| ticket-extraction | android | Google Pixel 11 Pro | Android 17 (API 37, build CD1A.261005.003) | nano-v4-full | mlkit-genai-prompt stable/full | v1 | available | 2026-10-29T14:31:45.662350Z |
| ticket-extraction | android | Google Pixel 11 Pro | Android 17 (API 37, build CD1A.261005.003) | nano-v4-fast | mlkit-genai-prompt stable/fast | v1 | available | 2026-10-29T14:35:00.740138Z |

## Results

| Run | Cases | Passed | Failed | Not graded | Mean score | Verdict |
| --- | --- | --- | --- | --- | --- | --- |
| ticket-classifier · ios-simulator iOS 27.0 sim on Darwin 27.0.0 (os-derived) · prompt v1 | 12 | 11 | 1 | 0 | 92% | pass |
| ticket-classifier · ios-simulator iOS 27.0 sim on Darwin 27.0.0 (os-derived) · prompt v2 | 12 | 11 | 1 | 0 | 97% | pass |
| ticket-classifier · android nano-v4-fast · prompt v1 | 12 | 11 | 1 | 0 | 97% | pass |
| ticket-classifier · android nano-v4-fast · prompt v2 | 12 | 12 | 0 | 0 | 100% | pass |
| ticket-classifier · android nano-v4-full · prompt v1 | 12 | 12 | 0 | 0 | 100% | pass |
| ticket-classifier · android nano-v4-full · prompt v2 | 12 | 12 | 0 | 0 | 100% | pass |
| ticket-classifier · android nano-v4-full · prompt v1 | 12 | 12 | 0 | 0 | 100% | pass |
| ticket-classifier · android nano-v4-full · prompt v2 | 12 | 12 | 0 | 0 | 100% | pass |
| ticket-classifier · android nano-v4-fast · prompt v1 | 12 | 12 | 0 | 0 | 100% | pass |
| ticket-classifier · android nano-v4-fast · prompt v2 | 12 | 12 | 0 | 0 | 100% | pass |
| ticket-extraction · ios-simulator iOS 27.0 sim on Darwin 27.0.0 (os-derived) · prompt v1 | 4 | 2 | 2 | 0 | 92% | pass |
| ticket-extraction · android nano-v4-fast · prompt v1 | 4 | 3 | 1 | 0 | 96% | pass |
| ticket-extraction · android nano-v4-full · prompt v1 | 4 | 3 | 1 | 0 | 99% | pass |
| ticket-extraction · android nano-v4-full · prompt v1 | 4 | 3 | 1 | 0 | 96% | pass |
| ticket-extraction · android nano-v4-fast · prompt v1 | 4 | 3 | 1 | 0 | 96% | pass |

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

## Compare: ticket-classifier

Base ticket-classifier · ios-simulator iOS 27.0 sim on Darwin 27.0.0 (os-derived) · prompt v1 against ticket-classifier · android nano-v4-fast · prompt v1.
Differs in platform: ios-simulator to android; device: iPhone19,2 to Google Pixel 11 Pro; OS version: Version 27.0 (Build 24A434), simulator on host Darwin 27.0.0 to Android 17 (API 37, build CD1A.261005.003); model version: iOS 27.0 sim on Darwin 27.0.0 (os-derived) to nano-v4-fast; backend: foundation-models to mlkit-genai-prompt preview/fast.

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

## Compare: ticket-classifier

Base ticket-classifier · ios-simulator iOS 27.0 sim on Darwin 27.0.0 (os-derived) · prompt v1 against ticket-classifier · android nano-v4-fast · prompt v2.
Differs in platform: ios-simulator to android; device: iPhone19,2 to Google Pixel 11 Pro; OS version: Version 27.0 (Build 24A434), simulator on host Darwin 27.0.0 to Android 17 (API 37, build CD1A.261005.003); model version: iOS 27.0 sim on Darwin 27.0.0 (os-derived) to nano-v4-fast; backend: foundation-models to mlkit-genai-prompt preview/fast; prompt version: v1 to v2.

Score 92% to 100% (+8 pts). 0 regressed, 1 improved.

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
| office | fail | pass | Improved |

## Compare: ticket-classifier

Base ticket-classifier · ios-simulator iOS 27.0 sim on Darwin 27.0.0 (os-derived) · prompt v1 against ticket-classifier · android nano-v4-full · prompt v1.
Differs in platform: ios-simulator to android; device: iPhone19,2 to Google Pixel 11 Pro; OS version: Version 27.0 (Build 24A434), simulator on host Darwin 27.0.0 to Android 17 (API 37, build CD1A.261005.003); model version: iOS 27.0 sim on Darwin 27.0.0 (os-derived) to nano-v4-full; backend: foundation-models to mlkit-genai-prompt preview/full.

Score 92% to 100% (+8 pts). 0 regressed, 1 improved.

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
| office | fail | pass | Improved |

## Compare: ticket-classifier

Base ticket-classifier · ios-simulator iOS 27.0 sim on Darwin 27.0.0 (os-derived) · prompt v1 against ticket-classifier · android nano-v4-full · prompt v2.
Differs in platform: ios-simulator to android; device: iPhone19,2 to Google Pixel 11 Pro; OS version: Version 27.0 (Build 24A434), simulator on host Darwin 27.0.0 to Android 17 (API 37, build CD1A.261005.003); model version: iOS 27.0 sim on Darwin 27.0.0 (os-derived) to nano-v4-full; backend: foundation-models to mlkit-genai-prompt preview/full; prompt version: v1 to v2.

Score 92% to 100% (+8 pts). 0 regressed, 1 improved.

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
| office | fail | pass | Improved |

## Compare: ticket-classifier

Base ticket-classifier · ios-simulator iOS 27.0 sim on Darwin 27.0.0 (os-derived) · prompt v1 against ticket-classifier · android nano-v4-full · prompt v1.
Differs in platform: ios-simulator to android; device: iPhone19,2 to Google Pixel 11 Pro; OS version: Version 27.0 (Build 24A434), simulator on host Darwin 27.0.0 to Android 17 (API 37, build CD1A.261005.003); model version: iOS 27.0 sim on Darwin 27.0.0 (os-derived) to nano-v4-full; backend: foundation-models to mlkit-genai-prompt stable/full.

Score 92% to 100% (+8 pts). 0 regressed, 1 improved.

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
| office | fail | pass | Improved |

## Compare: ticket-classifier

Base ticket-classifier · ios-simulator iOS 27.0 sim on Darwin 27.0.0 (os-derived) · prompt v1 against ticket-classifier · android nano-v4-full · prompt v2.
Differs in platform: ios-simulator to android; device: iPhone19,2 to Google Pixel 11 Pro; OS version: Version 27.0 (Build 24A434), simulator on host Darwin 27.0.0 to Android 17 (API 37, build CD1A.261005.003); model version: iOS 27.0 sim on Darwin 27.0.0 (os-derived) to nano-v4-full; backend: foundation-models to mlkit-genai-prompt stable/full; prompt version: v1 to v2.

Score 92% to 100% (+8 pts). 0 regressed, 1 improved.

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
| office | fail | pass | Improved |

## Compare: ticket-classifier

Base ticket-classifier · ios-simulator iOS 27.0 sim on Darwin 27.0.0 (os-derived) · prompt v1 against ticket-classifier · android nano-v4-fast · prompt v1.
Differs in platform: ios-simulator to android; device: iPhone19,2 to Google Pixel 11 Pro; OS version: Version 27.0 (Build 24A434), simulator on host Darwin 27.0.0 to Android 17 (API 37, build CD1A.261005.003); model version: iOS 27.0 sim on Darwin 27.0.0 (os-derived) to nano-v4-fast; backend: foundation-models to mlkit-genai-prompt stable/fast.

Score 92% to 100% (+8 pts). 0 regressed, 1 improved.

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
| office | fail | pass | Improved |

## Compare: ticket-classifier

Base ticket-classifier · ios-simulator iOS 27.0 sim on Darwin 27.0.0 (os-derived) · prompt v1 against ticket-classifier · android nano-v4-fast · prompt v2.
Differs in platform: ios-simulator to android; device: iPhone19,2 to Google Pixel 11 Pro; OS version: Version 27.0 (Build 24A434), simulator on host Darwin 27.0.0 to Android 17 (API 37, build CD1A.261005.003); model version: iOS 27.0 sim on Darwin 27.0.0 (os-derived) to nano-v4-fast; backend: foundation-models to mlkit-genai-prompt stable/fast; prompt version: v1 to v2.

Score 92% to 100% (+8 pts). 0 regressed, 1 improved.

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
| office | fail | pass | Improved |

## Compare: ticket-extraction

Base ticket-extraction · ios-simulator iOS 27.0 sim on Darwin 27.0.0 (os-derived) · prompt v1 against ticket-extraction · android nano-v4-fast · prompt v1.
Differs in platform: ios-simulator to android; device: iPhone19,2 to Google Pixel 11 Pro; OS version: Version 27.0 (Build 24A434), simulator on host Darwin 27.0.0 to Android 17 (API 37, build CD1A.261005.003); model version: iOS 27.0 sim on Darwin 27.0.0 (os-derived) to nano-v4-fast; backend: foundation-models to mlkit-genai-prompt preview/fast.

Score 92% to 96% (+4 pts). 0 regressed, 1 improved.

| Case | Base | Other | Change |
| --- | --- | --- | --- |
| blender | pass | pass | Same |
| headphones | fail | pass | Improved |
| dashboard | fail | fail | Same |
| shipping | pass | pass | Same |

## Compare: ticket-extraction

Base ticket-extraction · ios-simulator iOS 27.0 sim on Darwin 27.0.0 (os-derived) · prompt v1 against ticket-extraction · android nano-v4-full · prompt v1.
Differs in platform: ios-simulator to android; device: iPhone19,2 to Google Pixel 11 Pro; OS version: Version 27.0 (Build 24A434), simulator on host Darwin 27.0.0 to Android 17 (API 37, build CD1A.261005.003); model version: iOS 27.0 sim on Darwin 27.0.0 (os-derived) to nano-v4-full; backend: foundation-models to mlkit-genai-prompt preview/full.

Score 92% to 99% (+7 pts). 0 regressed, 1 improved.

| Case | Base | Other | Change |
| --- | --- | --- | --- |
| blender | pass | pass | Same |
| headphones | fail | pass | Improved |
| dashboard | fail | fail | Same |
| shipping | pass | pass | Same |

## Compare: ticket-extraction

Base ticket-extraction · ios-simulator iOS 27.0 sim on Darwin 27.0.0 (os-derived) · prompt v1 against ticket-extraction · android nano-v4-full · prompt v1.
Differs in platform: ios-simulator to android; device: iPhone19,2 to Google Pixel 11 Pro; OS version: Version 27.0 (Build 24A434), simulator on host Darwin 27.0.0 to Android 17 (API 37, build CD1A.261005.003); model version: iOS 27.0 sim on Darwin 27.0.0 (os-derived) to nano-v4-full; backend: foundation-models to mlkit-genai-prompt stable/full.

Score 92% to 96% (+4 pts). 0 regressed, 1 improved.

| Case | Base | Other | Change |
| --- | --- | --- | --- |
| blender | pass | pass | Same |
| headphones | fail | pass | Improved |
| dashboard | fail | fail | Same |
| shipping | pass | pass | Same |

## Compare: ticket-extraction

Base ticket-extraction · ios-simulator iOS 27.0 sim on Darwin 27.0.0 (os-derived) · prompt v1 against ticket-extraction · android nano-v4-fast · prompt v1.
Differs in platform: ios-simulator to android; device: iPhone19,2 to Google Pixel 11 Pro; OS version: Version 27.0 (Build 24A434), simulator on host Darwin 27.0.0 to Android 17 (API 37, build CD1A.261005.003); model version: iOS 27.0 sim on Darwin 27.0.0 (os-derived) to nano-v4-fast; backend: foundation-models to mlkit-genai-prompt stable/fast.

Score 92% to 96% (+4 pts). 0 regressed, 1 improved.

| Case | Base | Other | Change |
| --- | --- | --- | --- |
| blender | pass | pass | Same |
| headphones | fail | pass | Improved |
| dashboard | fail | fail | Same |
| shipping | pass | pass | Same |

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

## Cases: ticket-classifier · android nano-v4-fast · prompt v1

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

## Cases: ticket-classifier · android nano-v4-fast · prompt v2

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
| office | Where is your office located? | Other | Other | pass | 100% | 0.0 |

## Cases: ticket-classifier · android nano-v4-full · prompt v1

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
| office | Where is your office located? | Other | Other | pass | 100% | 0.0 |

## Cases: ticket-classifier · android nano-v4-full · prompt v2

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
| office | Where is your office located? | Other | Other | pass | 100% | 0.0 |

## Cases: ticket-classifier · android nano-v4-full · prompt v1

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
| office | Where is your office located? | Other | Other | pass | 100% | 0.0 |

## Cases: ticket-classifier · android nano-v4-full · prompt v2

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
| office | Where is your office located? | Other | Other | pass | 100% | 0.0 |

## Cases: ticket-classifier · android nano-v4-fast · prompt v1

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
| office | Where is your office located? | Other | Other | pass | 100% | 0.0 |

## Cases: ticket-classifier · android nano-v4-fast · prompt v2

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
| office | Where is your office located? | Other | Other | pass | 100% | 0.0 |

## Cases: ticket-extraction · ios-simulator iOS 27.0 sim on Darwin 27.0.0 (os-derived) · prompt v1

| Case | Input | Expected | Output (first attempt) | Result | Score | Spread |
| --- | --- | --- | --- | --- | --- | --- |
| blender | Order 48213: the blender stopped working. This is urgent, I need it for an event tomorrow. | {"orderId":"48213","product":"blender","urgent":true} | {"orderId": "48213", "product": "blender", "urgent": true} | pass | 100% | 0.0 |
| headphones | My headphones arrived scratched. Order A-77. | {"orderId":"A-77","product":"headphones","urgent":false} | {"orderId": "A-77", "product": "headphones", "urgent": true} | fail | 83% | 0.0 |
| dashboard | I cannot log in to the web dashboard, blocking my whole team right now. | {"orderId":null,"product":"web dashboard","urgent":true} | {"orderId": null, "product": null, "urgent": true} | fail | 83% | 0.0 |
| shipping | Just wondering if you ship to Norway. | {"orderId":null,"product":null,"urgent":false} | {"orderId": null, "product": null, "urgent": false} | pass | 100% | 0.0 |

## Cases: ticket-extraction · android nano-v4-fast · prompt v1

| Case | Input | Expected | Output (first attempt) | Result | Score | Spread |
| --- | --- | --- | --- | --- | --- | --- |
| blender | Order 48213: the blender stopped working. This is urgent, I need it for an event tomorrow. | {"orderId":"48213","product":"blender","urgent":true} | ```json { "orderId": "48213", "product": "blender", "urgent": true } ``` | pass | 100% | 0.0 |
| headphones | My headphones arrived scratched. Order A-77. | {"orderId":"A-77","product":"headphones","urgent":false} | ```json { "orderId": "A-77", "product": "headphones", "urgent": false } ``` | pass | 100% | 0.0 |
| dashboard | I cannot log in to the web dashboard, blocking my whole team right now. | {"orderId":null,"product":"web dashboard","urgent":true} | ```json { "orderId": null, "product": null, "urgent": true } ``` | fail | 83% | 0.0 |
| shipping | Just wondering if you ship to Norway. | {"orderId":null,"product":null,"urgent":false} | ```json { "orderId": null, "product": null, "urgent": false } ``` | pass | 100% | 0.0 |

## Cases: ticket-extraction · android nano-v4-full · prompt v1

| Case | Input | Expected | Output (first attempt) | Result | Score | Spread |
| --- | --- | --- | --- | --- | --- | --- |
| blender | Order 48213: the blender stopped working. This is urgent, I need it for an event tomorrow. | {"orderId":"48213","product":"blender","urgent":true} | { "orderId": "48213", "product": "blender", "urgent": true } | pass | 100% | 0.0 |
| headphones | My headphones arrived scratched. Order A-77. | {"orderId":"A-77","product":"headphones","urgent":false} | {"orderId": "A-77", "product": "headphones", "urgent": false} | pass | 100% | 0.0 |
| dashboard | I cannot log in to the web dashboard, blocking my whole team right now. | {"orderId":null,"product":"web dashboard","urgent":true} | ```json { "orderId": null, "product": null, "urgent": true } ``` | fail | 94% | 0.08 |
| shipping | Just wondering if you ship to Norway. | {"orderId":null,"product":null,"urgent":false} | {"orderId": null, "product": null, "urgent": false} | pass | 100% | 0.0 |

## Cases: ticket-extraction · android nano-v4-full · prompt v1

| Case | Input | Expected | Output (first attempt) | Result | Score | Spread |
| --- | --- | --- | --- | --- | --- | --- |
| blender | Order 48213: the blender stopped working. This is urgent, I need it for an event tomorrow. | {"orderId":"48213","product":"blender","urgent":true} | ```json { "orderId": "48213", "product": "blender", "urgent": true } ``` | pass | 100% | 0.0 |
| headphones | My headphones arrived scratched. Order A-77. | {"orderId":"A-77","product":"headphones","urgent":false} | ```json { "orderId": "A-77", "product": "headphones", "urgent": false } ``` | pass | 100% | 0.0 |
| dashboard | I cannot log in to the web dashboard, blocking my whole team right now. | {"orderId":null,"product":"web dashboard","urgent":true} | ```json { "orderId": null, "product": null, "urgent": true } ``` | fail | 83% | 0.0 |
| shipping | Just wondering if you ship to Norway. | {"orderId":null,"product":null,"urgent":false} | ```json { "orderId": null, "product": null, "urgent": false } ``` | pass | 100% | 0.0 |

## Cases: ticket-extraction · android nano-v4-fast · prompt v1

| Case | Input | Expected | Output (first attempt) | Result | Score | Spread |
| --- | --- | --- | --- | --- | --- | --- |
| blender | Order 48213: the blender stopped working. This is urgent, I need it for an event tomorrow. | {"orderId":"48213","product":"blender","urgent":true} | ```json { "orderId": "48213", "product": "blender", "urgent": true } ``` | pass | 100% | 0.0 |
| headphones | My headphones arrived scratched. Order A-77. | {"orderId":"A-77","product":"headphones","urgent":false} | ```json { "orderId": "A-77", "product": "headphones", "urgent": false } ``` | pass | 100% | 0.0 |
| dashboard | I cannot log in to the web dashboard, blocking my whole team right now. | {"orderId":null,"product":"web dashboard","urgent":true} | ```json { "orderId": null, "product": null, "urgent": true } ``` | fail | 83% | 0.0 |
| shipping | Just wondering if you ship to Norway. | {"orderId":null,"product":null,"urgent":false} | ```json { "orderId": null, "product": null, "urgent": false } ``` | pass | 100% | 0.0 |
