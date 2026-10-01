# Gym Fitness Tracker (Java Swing + Firebase, MVC, multi-gym)

Three portals from one login screen (switch with the gear icon, top-right of the login card - the
screen defaults to the **Trainee** portal):

- **Trainee** - registers by picking their trainer from a dropdown (no code needed). Logs workouts,
  sees exercises that trainer assigned, a daily-goal ring, 7-day and by-category charts, and history.
- **Trainer** - registers with a **gym code** an admin gave them. Sees only the trainees who picked
  *them* as their trainer (never another trainer's or gym's trainees), builds a reusable exercise
  library, assigns exercises to their own trainees, and sets each trainee's daily calorie goal.
- **Admin** - registers with the bootstrap code in `config.properties`. Sees every account, can
  change anyone's role or delete an account, and creates/manages a **gym code per gym** from the
  "Gym Codes" tab.

## How gyms and data isolation work
1. An admin creates a gym code (Admin Dashboard > Gym Codes > enter a gym name, optionally a custom
   code, or leave it blank to auto-generate one).
2. A trainer registers through the Trainer portal using that code; their account is tagged with that
   gym's name.
3. A trainee registers through the Trainee portal and picks a specific trainer from a dropdown; their
   account stores that trainer's uid.
4. Every query a trainer's dashboard makes (`FirebaseService.getTraineesOf(trainerUid)`) is filtered
   to trainees whose stored trainer uid matches - so a trainer at Gym A never sees Gym B's people,
   even though they share the same Firestore database. `firestore.rules` encodes the same boundary
   for any future client that talks to Firestore directly instead of through this desktop app.

## Architecture (MVC)
```
com.fitness
├── model/      Workout, Exercise, AssignedExercise, UserProfile, GymCode, Stats
├── service/    FirebaseService                         (all Firestore/Auth calls - the only place
│                                                          gym/trainer filtering happens)
├── controller/ AuthController, TraineeController, TrainerController, AdminController
├── view/       LoginPanel, TraineeDashboard, TrainerDashboard, AdminDashboard, MainFrame,
│               Theme, chart panels
├── util/       AppConfig, Async
└── Main.java
```
Views call controllers, controllers call the service, the service calls Firebase. Nothing in `view/`
talks to Firebase directly.

## 1. Install
- JDK 17+  (`java -version`)
- Apache Maven  (`mvn -version`)
- VS Code + "Extension Pack for Java" (Microsoft)

## 2. Firebase setup (console.firebase.google.com)
1. Create a project.
2. **Build > Authentication > Get started > Email/Password > Enable.**
3. **Build > Firestore Database > Create database.**
4. **Project Settings > Service accounts > Generate new private key** -> save as
   `src/main/resources/serviceAccountKey.json`
5. **Project Settings > General > Web API Key** -> copy it (see note below if you don't see one).

## 3. App config
Copy `src/main/resources/config.properties.example` to `src/main/resources/config.properties` and set:
- `firebase.apiKey`     - the Web API key (used to verify passwords)
- `admin.invite.code`   - code for registering the first Admin account (keep this one closely held)
- `daily.calorie.goal`  - fallback goal, used until a trainer sets one for a trainee

Trainer gym codes are **not** set here - create them from the app once it's running (step 4 below).

## 4. Run
Open the folder in VS Code, then either press Run above `main` in `Main.java`, or:

    mvn clean compile exec:java

## 5. Build a shareable app
    mvn clean package
    java -jar target/GymFitnessApp-1.0.0-all.jar

## First use
1. Click the gear, choose **Admin login**, then "New here? Create an account" and register the
   admin (needs `admin.invite.code`).
2. As the admin, open **Gym Codes**, create a code for your first gym (e.g. "Downtown Fitness").
3. Click the gear, choose **Trainer login**, register a trainer using that gym code.
4. Click the gear, choose **Trainee login** (the default), register a trainee, and pick the trainer
   from the dropdown.
5. Log in as the trainer: build an exercise or two in the library, assign one to the trainee, and
   set that trainee's daily goal.
6. Log in as the trainee: the assigned exercise appears at the top; select it and click
   "Log selected" to prefill the log form, then fill in calories and add it.
7. Create a second gym code and a second trainer to confirm that trainer only ever sees their own
   trainees on the Trainer Progress tab.

## If you don't see a Web API Key in Firebase
Go to **Project settings > General > Your apps**, click the **`</>`** icon to register a web app
(you don't need to use the generated code), then the key appears in the `firebaseConfig` snippet
and afterwards under Project settings > General.

## Security notes
- Only Firebase Auth checks the password; the app never stores it.
- `serviceAccountKey.json` and `config.properties` are gitignored - never commit them or ship them
  publicly, since the service account key gives full access to your Firebase project.
- The Admin SDK (used by this desktop app) bypasses `firestore.rules`, so the gym isolation is
  actually enforced in `FirebaseService` (every trainer-facing query is filtered by trainerUid).
  The included rules encode the same boundary in case you later add a client that talks to
  Firestore directly with user tokens.
