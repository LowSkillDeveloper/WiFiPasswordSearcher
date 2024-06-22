# Unofficial 3WiFi Locator v2 for Android 

This is a new version of 3wifi locator, which is based on the version from drygdryg.


> [!NOTE]
> The changelog contains only information about the changes that I made myself in version of locator by drygdryg. The original drygdryg version repository has been removed.

# Changelog

Descriptions and instructions for my added new functions can be found on the Wiki Github page: https://github.com/LowSkillDeveloper/3WiFiLocator-Unofficial/wiki

## Added Features:
- Added ability to view saved passwords on your phone (root only)
- Added Connection via WPS button
- Added the ability to connect to the 3wifi server directly by IP address (No longer need a proxy server for redirection)
- Server list URLs in the start menu.
  - Functionality to load server list from online sources (working mirrors or proxy servers).
- Display of 3WiFi API keys with the ability to copy them.
- Dark theme and option to switch to it.
- Added double scanning feature to get more networks (It does two scans in a row with an interval of 4 seconds and then merges it into one list.)
- Offline vendor database from "Wps Wpa Tester" app.
- Added history of data founded in 3wifi for networks you scanned. (Local database in the application, where the networks saves when you found data in 3wifi)
  - Added import and export of local database in .json file
  - Added import and export in .csv file
  - Manually adding wifi to the database
  - Import txt file from RouterScan to local DB (or myuploads.txt from 3wifi)
  - Added ability to backup SQLite file and restore database from it.
- Added links to the start menu
- Added a local database search button next to the search button in the online 3wifi database
- Added the ability to download wpspin.html to the phone
- Added caching of wps pin code data from the 3wifi server (reduces the load on the server and makes offline re-viewing available)
- Added the ability to login using only the API key
- Ability to open 3wifi website in WebView
- Added the ability to manually search by BSSID in the 3WiFi database
- Added the ability to manually search by ESSID in the 3WiFi database
- Added WPS connection via Root
- Added button to display Wi-Fi point location on Google Map if coordinates are returned by server.
    
## Updated Features:
- Offline mode button.
- Upgraded the SDK version
- In network security definition, WPA3 definition has been added
- Feature for checking application updates.
- Latest wpspin.html from 3WiFi.
- Local pin.db updated to the 2024 version from "Wps Wpa Tester" (includes 2000 new pins).
- Update some libraries.
- Added more buttons for logging out of your account
- Updated the way to grant permissions due to the requirements of the new SDK
- Optimization of wps pin generation initialization (Now PIN code generation loads faster.)
- Minor updates to the section with detailed information about the network

## Fixed Issues:
- Getting online vendor information from wpsfinder.
- Remade some hardcoded lines
- Visibility of some objects
- Fixed a crash when switching passwords (The problem exists in all versions from drygdryg, which migrated from java to kotlin, but I no longer have it)
- Fixed the message "this app was built for an older version of android" (If Android is 13 or higher, the message still appears, it will not appear until version 13 of Android)
- Fixed black bars at the top and bottom of the application
- Fixed offline wps generation from wpspin.html file (The problem exists in all versions from drygdryg, which migrated from java to kotlin, but I no longer have it)
- Fixed Error when trying to save an empty network profile after checking in the database
- Added multidex to the application because the application hit the limit of methods.
- Fixed WPS connection without root permissions.
- Fixed overlap of the last item in Wi-Fi networks list.
- Fixed crash when clicking on a list item after searching in local database.
- Fixed adding server URL with '/' at the end.


# Screenshots

<div>
  <table>
    <tr>
      <td><img src="https://github.com/LowSkillDeveloper/3WiFiLocator-Unofficial/assets/25121341/49600f7a-971b-482b-ae75-8f96f9e2d1f8" alt="Скриншот 1" width="216" height="480"></td>
      <td><img src="https://github.com/LowSkillDeveloper/3WiFiLocator-Unofficial/assets/25121341/40cf05ed-fa75-4b96-995e-e927b689441e" alt="Скриншот 2" width="216" height="480"></td>
      <td><img src="https://github.com/LowSkillDeveloper/3WiFiLocator-Unofficial/assets/25121341/5a268bcc-a5ed-486a-aafa-0aa81439e52a" alt="Скриншот 3" width="216" height="480"></td>
    </tr>
    <tr>
      <td><img src="https://github.com/LowSkillDeveloper/3WiFiLocator-Unofficial/assets/25121341/c3772448-8e41-479f-ad63-99fb7f5cf226" alt="Скриншот 4" width="216" height="480"></td>
      <td><img src="https://github.com/LowSkillDeveloper/3WiFiLocator-Unofficial/assets/25121341/d618c1b4-e868-492f-b6f0-ce1e409ca3e9" alt="Скриншот 6" width="216" height="480"></td>
      <td><img src="https://github.com/LowSkillDeveloper/3WiFiLocator-Unofficial/assets/25121341/e28e9344-87f4-4b75-8dd1-bae01c59e7a8" alt="Скриншот 7" width="216" height="480"></td>
    </tr>
    <tr>
      <td><img src="https://github.com/LowSkillDeveloper/3WiFiLocator-Unofficial/assets/25121341/0e30c1e2-6c60-4831-a082-a972c693b593" alt="Скриншот 8" width="216" height="480"></td>
      <td><img src="https://github.com/LowSkillDeveloper/3WiFiLocator-Unofficial/assets/25121341/266d45c7-56ea-4031-b76e-ea2ba8d01ff1" alt="Скриншот 9" width="216" height="480"></td>
      <td><img src="https://github.com/LowSkillDeveloper/3WiFiLocator-Unofficial/assets/25121341/34600782-8e4a-4c2f-9b7f-e8cfc06a3ab1" alt="Скриншот 10" width="216" height="480"></td>
    </tr>
    <tr>
      <td><img src="https://github.com/LowSkillDeveloper/3WiFiLocator-Unofficial/assets/25121341/b2ece9a2-2e24-4541-8186-c2b97faaadd4" alt="Скриншот 11" width="216" height="480"></td>
      <td><img src="https://github.com/LowSkillDeveloper/3WiFiLocator-Unofficial/assets/25121341/b1a12727-7666-4403-9aa0-de5db84f4fd0" alt="Скриншот 12" width="216" height="480"></td>
      <td><img src="https://github.com/LowSkillDeveloper/3WiFiLocator-Unofficial/assets/25121341/20d5bff3-4a71-4604-8624-9f670daec963" alt="Скриншот 13" width="216" height="480"></td>
    </tr>
<tr>
      <td><img src="https://github.com/LowSkillDeveloper/3WiFiLocator-Unofficial/assets/25121341/fa29982c-5d15-4cc5-82df-a86cb1db84d2" alt="Скриншот 4" width="216" height="480"></td>
</tr>
  </table>
</div>
------------------


drygdryg version of Locator (abandoned and deleted): https://github.com/drygdryg/WiFiPasswordSearcher

Original locator source (abandoned): https://github.com/FusixGit/WiFiPasswordSearcher

3WIFI source: https://github.com/binarymaster/3WiFi

ROOT version of Locator (abandoned): https://github.com/LowSkillDeveloper/Root-3WiFiLocator-Unofficial

