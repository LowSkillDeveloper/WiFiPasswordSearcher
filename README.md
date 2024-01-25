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
- Dark theme and option to switch to it. [Enabled in settings, disabled by default]
- Added double scanning feature to get more networks (It does two scans in a row with an interval of 4 seconds and then merges it into one list.) [Enabled in settings, disabled by default]
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
    
## Updated Features:
- Offline mode button.
- In network security definition, WPA3 definition has been added
- Feature for checking application updates.
- Latest wpspin.html from 3WiFi.
- Local pin.db updated to the 2024 version from "Wps Wpa Tester" (includes 2000 new pins).
- Update some libraries.
- Added more buttons for logging out of your account
- After checking via 3wifi, there is a check using the local database. [can be disabled in settings, enabled by default]
  - Added a switch to disable searching in local database after 3wifi

## Fixed Issues:
- Getting online vendor information from wpsfinder.
- Remade some hardcoded lines
- Visibility of some objects
- Fixed a crash when switching passwords (The problem exists in all versions from drygdryg, which migrated from java to kotlin, but I no longer have it)


# TODO:
- Add GPS sniffer
- Integrate RouterKeygen algorithms
- Add manual language change
- Fix known minor bugs
- Fix the message that the application is intended for older versions of Android (Too much to redo)



# Screenshots
<div>
  <table>
    <tr>
      <td><img src="https://github.com/LowSkillDeveloper/3WiFiLocator-Unofficial/assets/25121341/2e666106-beee-4fdc-ad72-507dcb3a9276" alt="Скриншот 1" width="216" height="480"></td>
      <td><img src="https://github.com/LowSkillDeveloper/3WiFiLocator-Unofficial/assets/25121341/d5d0e44f-3326-4dae-afca-4558fc3b2522" alt="Скриншот 2" width="216" height="480"></td>
    </tr>
    <tr>
      <td><img src="https://github.com/LowSkillDeveloper/3WiFiLocator-Unofficial/assets/25121341/8f329acb-ce78-49d5-9267-59ee57c49420" alt="Скриншот 3" width="216" height="480"></td>
      <td><img src="https://github.com/LowSkillDeveloper/3WiFiLocator-Unofficial/assets/25121341/cd9eaeed-888e-4061-a801-1fa2861640fd" alt="Скриншот 4" width="216" height="480"></td>
    </tr>
  </table>
</div>
------------------


drygdryg version of Locator (abandoned?): https://github.com/drygdryg/WiFiPasswordSearcher

Original locator source (abandoned): https://github.com/FusixGit/WiFiPasswordSearcher

3WIFI source: https://github.com/binarymaster/3WiFi

ROOT version of Locator (abandoned): https://github.com/LowSkillDeveloper/Root-3WiFiLocator-Unofficial

