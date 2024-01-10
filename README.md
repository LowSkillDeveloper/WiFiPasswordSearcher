# Unofficial 3WiFi Locator v2 for Android 

This is a new version of 3wifi locator, which is based on the version from drygdryg.

> [!NOTE]
> Делать новую версию локатора оказалось интереснее чем я думал, поэтому я запилил огромное обновление, по моему мнению конечно, ведь локатор давно был заброшен.
Поэтому надеюсь проект 3wifi не умрёт окончательно, и получит вторую жизнь, как и локатор.

# Changelog
The changelog contains only information about the changes that I made myself. Changes made by drygdryg to the original version can be found in his repository, link at the end of the description.

## Added Features:
- Server list URLs in the start menu.
  - Functionality to load server list from online sources (working mirrors or proxy servers).
- Display of 3WiFi API keys with the ability to copy them.
- Dark theme and option to switch to it.
- Offline vendor database from "Wps Wpa Tester" app.
- Added history of data founded in 3wifi for networks you scanned. (Local database in the application, where the networks saves when you found data in 3wifi)
  - Added import and export of local database
  - Manually adding to the database
  - Interaction with networks in the database (wps generation, wps connection, copying)
  - Added columns for login and password to the router admin panel. (manual addition or import from router scan)
  - Import txt file from RouterScan to local DB
  
## Updated Features:
- Offline mode button.
- In network security definition, WPA3 definition has been added
- Feature for checking application updates.
- Latest wpspin.html from 3WiFi.
- Local pin.db updated to the 2024 version from "Wps Wpa Tester" (includes 2000 new pins).
- Update some libraries.

## Fixed Issues:
- Getting online vendor information from wpsfinder.
- Remade some hardcoded lines
- Visibility of some objects


# TODO:
- Add GPS sniffer
- Add manual language change
- Add a 3wifi sniffer for surrounding networks
- Import data from a txt file from Router Sacan to local DB


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

