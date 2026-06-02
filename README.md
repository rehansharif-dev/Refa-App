# Refa — Your Ride, Your Way 🚗

> A real-time ride-hailing Android application for Pakistan — connecting passengers with nearby drivers instantly, with built-in wallet payments, live GPS tracking, and dual passenger/driver mode in a single app.

\---

## App Details

|Field|Info|
|-|-|
|**App Name**|Refa|
|**Category**|Travel \& Transport|
|**Version Name**|1.0|
|**Version Code**|1|
|**Min Android Version**|Android 8.0 Oreo (API 26)|
|**Target SDK**|Android 15 (API 35)|
|**Package Name**|com.example.transportapp|
|**Language**|Kotlin|

\---

## About the App

**Refa** is a full-featured ride-hailing application built for the Pakistani market. Passengers can book rides across four vehicle types (Bike, Rickshaw, Car, Premium), track their driver live on Google Maps, and pay seamlessly through a built-in digital wallet. The same app also supports a **Driver Mode** — any registered user can switch to become a driver, receive ride requests filtered to their vehicle type, and earn money directly into their Refa wallet.

The app solves a real problem: affordable, trackable, cashless transportation in Pakistan with no need for two separate apps for passengers and drivers. Fare estimates are calculated in real-time using the Google Directions API based on actual route distance. All data syncs in real time through Firebase Firestore, meaning both passenger and driver see live status updates with near-zero latency.

**Who can use it:** Anyone in Pakistan who needs a reliable ride or wants to earn by driving.

\---

## App Screenshots

|Splash Screen|Login|Sign Up|
|:-:|:-:|:-:|
|!\[Splash](screenshots/splash\_screen.png)|!\[Login](screenshots/login\_screen.png)|!\[Sign Up](screenshots/signup\_screen.png)|

|Home Dashboard|Ride Booking Map|Driver Dashboard|Profile \& Wallet|
|:-:|:-:|:-:|:-:|
|!\[Dashboard](screenshots/dashboard.png)|!\[Map](screenshots/ride\_booking.png)|!\[Driver](screenshots/driver\_dashboard.png)|!\[Profile](screenshots/profile\_screen.png)|

|Ride History|Register Vehicle|
|:-:|:-:|
|!\[Ride History](screenshots/ride\_history.png)|!\[Register Vehicle](screenshots/register\_vehicle.png)|

\---

## Features

### Passenger Features

* **Smart Ride Booking** — Select destination on Google Maps, get instant fare estimates for all vehicle types
* **4 Vehicle Types** — Bike, Rickshaw, Car, and Premium — each with dynamic distance-based pricing
* **Live GPS Tracking** — Track your driver on an interactive map in real time during the ride
* **Refa Wallet** — Built-in digital wallet (starting Rs. 400), top-up anytime, automatic fare deduction
* **Ride History** — Full log of all past rides with dates, fares, and destinations
* **Cancel Anytime** — Optimistic UI cancellation — screen clears instantly without waiting for network

### Driver Features

* **Driver Mode** — Toggle between passenger and driver from your profile
* **Smart Ride Feed** — Receive only rides matching your vehicle type (Firestore-level filtering)
* **Atomic Ride Acceptance** — Firestore transaction prevents two drivers from claiming the same ride
* **Live Earnings Tracker** — Total earnings, completed trips, and recent trip history in real time
* **Automatic Fare Transfer** — Fare is atomically transferred from passenger wallet to driver wallet on trip completion
* **Foreground Location Service** — Background GPS tracking keeps your position updated even when app is minimized

### App-Wide Features

* **Firebase Authentication** — Secure email/password login and registration
* **15-Day Driver Lock** — Firestore-persisted lock prevents abuse of the driver/passenger toggle
* **Push Notifications (FCM)** — Real-time ride request alerts and status change notifications
* **Responsive Design** — Adaptive layout for both phones and tablets
* **Dark Navy Theme** — Professional dark UI optimized for outdoor/nighttime use
* **WhatsApp Support** — One-tap in-app contact to support team

\---

## Technologies Used

|Category|Technology|
|-|-|
|Language|Kotlin|
|UI Framework|Jetpack Compose (Material Design 3)|
|IDE|Android Studio|
|Architecture|MVVM + Repository Pattern|
|Dependency Injection|Hilt (Dagger)|
|Async|Kotlin Coroutines + Flow|
|Authentication|Firebase Authentication|
|Database|Firebase Firestore (real-time)|
|Realtime Presence|Firebase Realtime Database|
|File Storage|Firebase Storage|
|Push Notifications|Firebase Cloud Messaging (FCM)|
|Maps|Google Maps Compose SDK|
|Routing \& Fares|Google Directions API|
|Image Loading|Coil|
|Build System|Gradle (Kotlin DSL)|
|Min SDK|Android 8.0 (API 26)|

\---

## App Permissions

The following permissions are declared in `AndroidManifest.xml` and are required for the app to function correctly:

|Permission|Purpose|
|-|-|
|`INTERNET`|Required for all network requests — Firebase, Google Maps, Directions API|
|`ACCESS\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\_FINE\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\_LOCATION`|Precise GPS location for ride pickup and driver tracking|
|`ACCESS\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\_COARSE\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\_LOCATION`|Fallback network-based location when GPS is unavailable|
|`ACCESS\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\_BACKGROUND\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\_LOCATION`|Keeps driver location updated while app is in the background|
|`FOREGROUND\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\_SERVICE`|Required to run the GPS location service in the foreground|
|`FOREGROUND\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\_SERVICE\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\_LOCATION`|Declares that the foreground service uses location (Android 14+)|
|`VIBRATE`|Vibrates the phone when a new ride request arrives (driver mode)|
|`READ\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\_MEDIA\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\_IMAGES`|Allows selecting a profile picture from the gallery (Android 13+)|
|`READ\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\_EXTERNAL\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\_STORAGE`|Allows selecting a profile picture on Android 12 and below|
|`POST\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\_NOTIFICATIONS`|Required to show push notification banners on Android 13+|

> No user data is collected beyond what is necessary for ride-booking functionality. See \\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\[Privacy Policy](docs/Privacy%20Policy.pdf) for full details.

\---

## APK Download

> Download and install the Refa app directly on your Android phone.

📥 [**Download Refa.apk**](apk/Refa.apk)

\---

## How to Install the APK

1. **Download** the `Refa.apk` file from the `apk/` folder above
2. **Transfer** the file to your Android phone (via USB, WhatsApp, Google Drive, etc.)
3. **Open** the APK file on your Android phone
4. If prompted, tap **"Allow from this source"** or go to:
`Settings → Security → Install Unknown Apps → Enable`
5. Tap **Install** and wait a few seconds
6. Open **Refa** from your app drawer and register your account

> \\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\*\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\*Minimum Android version:\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\*\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\* Android 8.0 (Oreo) or higher

\---

## How to Run the Project (Developers)

```bash
# 1. Clone this repository
git clone https://github.com/rehansharif-dev/Refa-App.git

# 2. Open the project in Android Studio
#    File → Open → Select the app/ folder inside Refa-App/

# 3. Sync Gradle files
#    Android Studio will prompt you automatically

# 4. Set up Firebase
#    - Create a project at https://console.firebase.google.com
#    - Add an Android app with package name: com.example.transportapp
#    - Download google-services.json and place it in app/app/ folder
#    - Enable: Authentication (Email/Password), Firestore, Realtime Database,
#      Storage, and Cloud Messaging

# 5. Set up Google Maps
#    - Get an API key from https://console.cloud.google.com
#    - Enable: Maps SDK for Android, Directions API
#    - Add your key to app/app/src/main/AndroidManifest.xml

# 6. Run the app
#    Connect an Android device or start an emulator, then click Run ▶
```

\---



## Firebase Firestore Structure

```
firestore/
├── users/
│   └── {userId}/
│       ├── name, email, phoneNumber, address
│       ├── walletBalance (Double)
│       ├── isDriverMode (Boolean)
│       ├── profilePicture (Base64 string)
│       ├── vehicle { model, plateNumber, type }
│       ├── rating (Double)
│       ├── driverLockUntil (Long, timestamp ms)
│       └── fcmToken (String)
│
└── rides/
    └── {rideId}/
        ├── userId, userName, userPhone
        ├── driverId, driverName
        ├── pickupLocation { latitude, longitude, bearing }
        ├── pickupAddress, dropAddress
        ├── dropLocation { latitude, longitude, bearing }
        ├── vehicleType (BIKE/CAR/RICKSHAW/PREMIUM)
        ├── fare (Double, PKR)
        ├── status (SEARCHING/CONFIRMED/ARRIVING/ONGOING/COMPLETED/CANCELLED)
        ├── timestamp (Long)
        └── passengerLocation { latitude, longitude, bearing }
```

\---

## Privacy Policy

This app collects location data, email address, and phone number for ride-booking functionality.
All data is stored securely on Firebase and is never sold to third parties.

📄 [**View Full Privacy Policy**](docs/Privacy%20Policy.pdf)

\---

## Project Structure

```
Refa-App/
├── app/                          ← Complete Android source code
│   └── src/main/java/
│       └── com/example/transportapp/
│           ├── data/             ← Repositories, Firebase services
│           ├── domain/           ← Models, Repository interfaces
│           ├── presentation/     ← Screens \\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\& ViewModels (Compose)
│           ├── di/               ← Hilt dependency injection modules
│           └── ui/               ← Theme, Colors, Typography
├── screenshots/                  ← App screenshots (9 screens)
├── apk/                          ← Installable APK file
├── docs/                         ← Privacy Policy \\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\& User Manual
├── README.md                     ← This file
├── LICENSE                       ← MIT License
└── .gitignore                    ← Android gitignore
```

\---

## Future Enhancements

* \[ ] In-app chat between passenger and driver
* \[ ] Promo codes and discount system
* \[ ] Driver rating and review system after each ride
* \[ ] Admin web panel for ride monitoring and analytics
* \[ ] Multiple payment methods (EasyPaisa, JazzCash integration)
* \[ ] SOS / emergency button for passenger safety
* \[ ] Scheduled rides (book in advance)
* \[ ] Multi-language support (Urdu + English)
* \[ ] Driver document verification system

\---

## Developed By

**M. Rehan Sharif \& Faisal Tehseen Mehdi**

BS Computer Science — Semester 6th

Department of Computer Science

University of Layyah

* 🔗 GitHub: [github.com/rehansharif-dev](https://github.com/rehansharif-dev)
* 📧 Email: rehansharif000@gmail.com

\---

## License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.

\---

<p align="center">Made with ❤️ in Pakistan 🇵🇰</p>

