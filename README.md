# Khoji - Location tracking and visualization using Augmented Reality

An android app, that tracks the location of a user and updates it in the Firebase Database. As soon as the location of the user gets updated, this update is reflected down to all the contacts of that user so they are able to see where that person is at the moment. 

We use Augmented Reality, using ARCore, to visualize the location markers of the users so it is easier for them to locate their contacts.

We have also included Real-time chat, as well as integrated a chat-bot into the application.

![Khoji](./screenshots/Khoji_screen.png)

---

To better understand the project and to see all the features, you can take a look the project report. You can find the complete report here: [Khoji Report/Khoji Report.pdf](./Khoji%20Report/Khoji%20Report.pdf)
There is also a presentation that will highlight the main features of the app and provide some demos as well. You can find it here: [presentation/FYP.pptx](./presentation/FYP.pptx)

## Installation Instructions
You have to add the following files before using this application:
- khoji_app/app/keystore.jks - Keystore to build the app. 
- khoji_app/app/google-services.json - For using Firebase services, can be downloaded from your Firebase project.
- khoji_app/app/src/main/res/values/secrets.xml - Facebook and Twitter keys to use them as Authentication providers.
- khoji_app/gradle.properties - 

What to include in **secrets.xml**:
```
<resources>
    <string name="facebook_application_id" translatable="false">your_id</string>
    <!-- Facebook Application ID, prefixed by 'fb'.  Enables Chrome Custom tabs. -->
    <string name="facebook_login_protocol_scheme" translatable="false">your_id</string>
    <string name="twitter_consumer_key" translatable="false">your_id</string>
    <string name="twitter_consumer_secret" translatable="false">your_key</string>
</resources>
```

What to include in **gradle.properties**:
```
GOOGLE_MAPS_API_KEY=YOUR_API_KEY
KEYSTORE_PASSWORD=your_psswd
KEY_PASSWORD=your_psswd
```

## Libraries used
- Firebase Authentication
- Firebase Database
- ARCore
- ARCore Location

background-image: linear-gradient(to top, #a18cd1 0%, #fbc2eb 100%);

