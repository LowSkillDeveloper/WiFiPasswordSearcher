# Unofficial 3WiFi Locator v2 for Android 

This is a new version of 3wifi locator, which is based on the version from drygdryg.


> [!NOTE]
> Делать новую версию локатора оказалось интереснее чем я думал, поэтому я запилил огромное обновление, по моему мнению конечно, ведь локатор давно был заброшен.
Поэтому надеюсь проект 3wifi не умрёт окончательно, и получит вторую жизнь, как и локатор.

# Changelog
The changelog contains only information about the changes that I made myself. Changes made by drygdryg to the original version can be found in his repository, link at the end of the description.

Descriptions and instructions for my added new functions can be found on the Wiki Github page: https://github.com/LowSkillDeveloper/3WiFiLocator-Unofficial/wiki

## Added Features:
- Added the ability to connect to the 3wifi server directly by IP address (No longer need a proxy server for redirection)
- Server list URLs in the start menu.
  - Functionality to load server list from online sources (working mirrors or proxy servers).
- Display of 3WiFi API keys with the ability to copy them.
- Dark theme and option to switch to it.
- Added double scanning feature to get more networks (It does two scans in a row with an interval of 4 seconds and then merges it into one list.)
- Offline vendor database from "Wps Wpa Tester" app.
- Added history of data founded in 3wifi for networks you scanned. (Local database in the application, where the networks saves when you found data in 3wifi)
  - Added import and export of local database
  - Manually adding to the database
  - Interaction with networks in the database (wps generation, wps connection, copying)
  - Added columns for login and password to the router admin panel. (only manual addition or import from router scan txt)
  - Import txt file from RouterScan to local DB (or myuploads.txt from 3wifi)
    - Now the application can import a large myuploads.txt file that containing more than 100K lines
  - Added a switch that disables automatic adding of received data from 3wifi to the local database
- Added links to the start menu
- Added a local database search button next to the search button in the online 3wifi database
  - Added a primary button switch, now the local DB search button can be made primary.
- Added the ability to download wpspin.html to the phone
    
## Updated Features:
- Offline mode button.
- Upgraded the SDK version
- In network security definition, WPA3 definition has been added
- Feature for checking application updates.
- Latest wpspin.html from 3WiFi.
- Local pin.db updated to the 2024 version from "Wps Wpa Tester" (includes 2000 new pins).
- Update some libraries.
- Added more buttons for logging out of your account
- After checking via 3wifi, there is a check using the local database.
  - Added a switch to disable searching in local database after 3wifi
- Updated the way to grant permissions due to the requirements of the new SDK

## Fixed Issues:
- Getting online vendor information from wpsfinder.
- Remade some hardcoded lines
- Visibility of some objects
- Fixed a crash when switching passwords (The problem exists in all versions from drygdryg, which migrated from java to kotlin, but I no longer have it)
- Fixed the message "this app was built for an older version of android" (If Android is 13 or higher, the message still appears, it will not appear until version 13 of Android)
- Fixed black bars at the top and bottom of the application


# TODO:
- Add GPS sniffer
- Integrate RouterKeygen algorithms
- Add manual language change
- Fix known minor bugs


# Screenshots
<div>
  <table>
    <tr>
      <td><img src="https://github.com/LowSkillDeveloper/3WiFiLocator-Unofficial/assets/25121341/e264f3a6-7b54-4074-8390-2cae6521e778" alt="Скриншот 1" width="216" height="480"></td>
      <td><img src="https://github.com/LowSkillDeveloper/3WiFiLocator-Unofficial/assets/25121341/0f9ad4bc-e9b6-4b3e-a236-698f00719e2f" alt="Скриншот 2" width="216" height="480"></td>
      <td><img src="https://github.com/LowSkillDeveloper/3WiFiLocator-Unofficial/assets/25121341/fa29982c-5d15-4cc5-82df-a86cb1db84d2" alt="Скриншот 4" width="216" height="480"></td>
    </tr>
    <tr>
      <td><img src="https://github.com/LowSkillDeveloper/3WiFiLocator-Unofficial/assets/25121341/53e450dc-6802-4451-bf1b-86105f5187d8" alt="Скриншот 5" width="216" height="480"></td>
      <td><img src="https://github.com/LowSkillDeveloper/3WiFiLocator-Unofficial/assets/25121341/aac7d763-48d5-4b61-b772-b21b073367cb" alt="Скриншот 6" width="216" height="480"></td>
      <td><img src="https://github.com/LowSkillDeveloper/3WiFiLocator-Unofficial/assets/25121341/8e518654-3c24-4516-878a-6edb421076d1" alt="Скриншот 7" width="216" height="480"></td>
    </tr>
    <tr>
      <td><img src="https://github.com/LowSkillDeveloper/3WiFiLocator-Unofficial/assets/25121341/7dabe98f-d94d-4568-ba5a-4f3630572e4d" alt="Скриншот 8" width="216" height="480"></td>
      <td><img src="https://github.com/LowSkillDeveloper/3WiFiLocator-Unofficial/assets/25121341/56aa1231-4855-4002-ab80-4a56bb7f7e63" alt="Скриншот 9" width="216" height="480"></td>
      <td><img src="https://github.com/LowSkillDeveloper/3WiFiLocator-Unofficial/assets/25121341/25769ac7-8fcd-41ec-9db6-ae0b464f895d" alt="Скриншот 10" width="216" height="480"></td>
    </tr>
  </table>
</div>
------------------


drygdryg version of Locator (abandoned?): https://github.com/drygdryg/WiFiPasswordSearcher

Original locator source (abandoned): https://github.com/FusixGit/WiFiPasswordSearcher

3WIFI source: https://github.com/binarymaster/3WiFi

ROOT version of Locator (abandoned): https://github.com/LowSkillDeveloper/Root-3WiFiLocator-Unofficial

