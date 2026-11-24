# Arpontols Pay Gateway (Android Auto-SMS APK)

Automatically reads incoming **bKash, Nagad, Rocket, and Upay** payment SMS on your Android phone and forwards verified TrxID + BDT amount directly to your `clainds.py` Telegram Bot (`/api/v1/sms-webhook`).

## How to Build the `.apk` Free on GitHub (2 Minutes, No PC Needed!)
1. Create a free repository on GitHub (e.g. `arpontols-pay-apk`).
2. Extract `ArpontolsPayAPK.zip` and upload all files (including the `.github/workflows/build-apk.yml` folder) to the repository.
3. Open the **Actions** tab on GitHub -> click **Build ArpontolsPay APK** -> click **Run workflow**.
4. Wait ~2 minutes for the green checkmark -> scroll down to **Artifacts** -> download **ArpontolsPay-Gateway-APK** (contains `app-debug.apk`).
5. Install `app-debug.apk` on your Android phone, tap **Generate Key**, enter your bot's Webhook URL (`https://your-server/api/v1/sms-webhook`), allow SMS permissions, and tap **Start Auto-Gateway**!
