package com.fitness.service;

import com.fitness.model.AssignedExercise;
import com.fitness.model.Exercise;
import com.fitness.model.GymCode;
import com.fitness.model.UserProfile;
import com.fitness.model.Workout;
import com.fitness.util.AppConfig;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserRecord;
import com.google.firebase.cloud.FirestoreClient;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * All Firebase access lives here (the data-access part of the Model layer).
 *  - Password verification uses the Firebase Auth REST API (the Admin SDK cannot check passwords).
 *  - Everything else uses the Admin SDK.
 *  - Data isolation between gyms: a Trainee stores the uid of the Trainer they picked at signup
 *    (trainerUid); a Trainer only ever queries trainees whose trainerUid equals their own uid.
 * Every method here blocks, so controllers call it through {@link com.fitness.util.Async}.
 */
public final class FirebaseService {
    private static Firestore db;
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // no 0/O/1/I

    private FirebaseService() { }

    public static void initialize() throws IOException {
        try (InputStream in = FirebaseService.class.getClassLoader().getResourceAsStream("serviceAccountKey.json")) {
            if (in == null) {
                throw new FileNotFoundException("serviceAccountKey.json not found in src/main/resources");
            }
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(in))
                    .build();
            if (FirebaseApp.getApps().isEmpty()) FirebaseApp.initializeApp(options);
            db = FirestoreClient.getFirestore();
        }
    }

    // ---------------------------------------------------------------- auth

    public static UserProfile login(String email, String password) throws Exception {
        String key = AppConfig.apiKey();
        if (key.isBlank() || key.startsWith("YOUR_")) {
            throw new IllegalStateException("Set firebase.apiKey in config.properties");
        }
        JsonObject body = new JsonObject();
        body.addProperty("email", email);
        body.addProperty("password", password);
        body.addProperty("returnSecureToken", true);

        HttpRequest req = HttpRequest.newBuilder(URI.create(
                        "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=" + key))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();
        HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        JsonObject json = JsonParser.parseString(res.body()).getAsJsonObject();

        if (res.statusCode() != 200) {
            String code = json.has("error") ? json.getAsJsonObject("error").get("message").getAsString() : "";
            throw new IOException(friendlyAuthError(code));
        }
        return getProfile(json.get("localId").getAsString());
    }

    public static void sendPasswordResetEmail(String email) throws Exception {
        String key = AppConfig.apiKey();
        if (key.isBlank() || key.startsWith("YOUR_")) {
            throw new IllegalStateException("Set firebase.apiKey in config.properties");
        }
        JsonObject body = new JsonObject();
        body.addProperty("requestType", "PASSWORD_RESET");
        body.addProperty("email", email);

        HttpRequest req = HttpRequest.newBuilder(URI.create(
                        "https://identitytoolkit.googleapis.com/v1/accounts:sendOobCode?key=" + key))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();
        HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() == 200) return;

        JsonObject json = JsonParser.parseString(res.body()).getAsJsonObject();
        String code = json.has("error") ? json.getAsJsonObject("error").get("message").getAsString() : "";
        if ("EMAIL_NOT_FOUND".equals(code)) return;
        if ("INVALID_EMAIL".equals(code)) throw new IllegalArgumentException("Enter a valid email address.");
        throw new IOException("Could not send a password reset email. " + code);
    }

    /**
     * @param role       "Trainee", "Trainer" or "Admin"
     * @param inviteCode for Trainer: a gym code created by an admin; for Admin: the bootstrap code
     *                   in config.properties; ignored for Trainee
     * @param trainerUid for Trainee: the uid of the trainer they picked; ignored otherwise
     */
    public static UserProfile register(String name, String email, String password, String role,
                                        String inviteCode, String trainerUid) throws Exception {
        if (password.length() < 6) throw new IllegalArgumentException("Password must be at least 6 characters.");

        String gymName = null;
        if ("Trainer".equals(role)) {
            gymName = lookupGymName(inviteCode == null ? "" : inviteCode.trim());
        } else if ("Admin".equals(role)) {
            String expected = AppConfig.adminCode();
            if (expected.isEmpty() || !expected.equals(inviteCode)) {
                throw new IllegalArgumentException("Invalid admin invite code.");
            }
        } else {
            role = "Trainee";
            if (trainerUid == null || trainerUid.isBlank()) {
                throw new IllegalArgumentException("Please select your trainer.");
            }
            DocumentSnapshot t = db.collection("users").document(trainerUid).get().get();
            if (!t.exists() || !"Trainer".equals(t.getString("role"))) {
                throw new IllegalArgumentException("Selected trainer could not be found. Please pick again.");
            }
        }

        UserRecord user = FirebaseAuth.getInstance().createUser(
                new UserRecord.CreateRequest().setEmail(email).setPassword(password).setDisplayName(name));

        Map<String, Object> profile = new HashMap<>();
        profile.put("email", email);
        profile.put("name", name);
        profile.put("role", role);
        profile.put("createdAt", System.currentTimeMillis());
        if ("Trainer".equals(role)) profile.put("gymName", gymName);
        if ("Trainee".equals(role)) profile.put("trainerUid", trainerUid);
        if ("Admin".equals(role)) {
            DocumentReference adminRef = db.collection("system").document("admin");
            DocumentReference profileRef = db.collection("users").document(user.getUid());
            try {
                db.runTransaction(transaction -> {
                    DocumentSnapshot currentAdmin = transaction.get(adminRef).get();
                    List<QueryDocumentSnapshot> admins = transaction.get(
                            db.collection("users").whereEqualTo("role", "Admin")).get().getDocuments();
                    if (currentAdmin.exists() || !admins.isEmpty()) {
                        throw new IllegalStateException("An admin account already exists.");
                    }
                    transaction.set(adminRef, Map.of("uid", user.getUid()));
                    transaction.set(profileRef, profile);
                    return null;
                }).get();
            } catch (Exception error) {
                try {
                    FirebaseAuth.getInstance().deleteUser(user.getUid());
                } catch (Exception cleanupError) {
                    error.addSuppressed(cleanupError);
                }
                throw error;
            }
        } else {
            db.collection("users").document(user.getUid()).set(profile).get();
        }
        return new UserProfile(user.getUid(), email, name, role, null,
                "Trainee".equals(role) ? trainerUid : null, gymName);
    }

    public static UserProfile getProfile(String uid) throws Exception {
        DocumentSnapshot d = db.collection("users").document(uid).get().get();
        if (!d.exists()) throw new IllegalStateException("No profile found for this account.");
        return toProfile(d);
    }

    private static UserProfile toProfile(DocumentSnapshot d) {
        String email = d.getString("email");
        String name = d.getString("name");
        Long goal = d.getLong("dailyGoal");
        return new UserProfile(d.getId(), email, name == null || name.isBlank() ? email : name,
                d.getString("role"), goal == null ? null : goal.intValue(),
                d.getString("trainerUid"), d.getString("gymName"));
    }

    private static String friendlyAuthError(String code) {
        if (code.startsWith("INVALID_LOGIN_CREDENTIALS") || code.startsWith("INVALID_PASSWORD")
                || code.startsWith("EMAIL_NOT_FOUND")) return "Incorrect email or password.";
        if (code.startsWith("USER_DISABLED")) return "This account has been disabled.";
        if (code.startsWith("TOO_MANY_ATTEMPTS")) return "Too many attempts. Try again later.";
        return code.isEmpty() ? "Login failed." : "Login failed: " + code;
    }

    // ---------------------------------------------------------------- gym / trainer codes (admin-managed)

    private static String lookupGymName(String code) throws Exception {
        if (code.isEmpty()) throw new IllegalArgumentException("Enter the gym code your admin gave you.");
        DocumentSnapshot d = db.collection("trainerCodes").document(code).get().get();
        if (!d.exists()) throw new IllegalArgumentException("That gym code was not recognized.");
        Boolean active = d.getBoolean("active");
        if (active != null && !active) throw new IllegalArgumentException("That gym code has been deactivated.");
        return d.getString("gymName");
    }

    /** Creates a gym code. If code is blank, generates a random 6-character one and returns it. */
    public static String createGymCode(String gymName, String code) throws Exception {
        String finalCode = (code == null || code.isBlank()) ? generateCode() : code.trim().toUpperCase();
        Map<String, Object> m = new HashMap<>();
        m.put("gymName", gymName);
        m.put("active", true);
        m.put("createdAt", System.currentTimeMillis());
        db.collection("trainerCodes").document(finalCode).set(m).get();
        return finalCode;
    }

    private static String generateCode() {
        SecureRandom r = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) sb.append(CODE_CHARS.charAt(r.nextInt(CODE_CHARS.length())));
        return sb.toString();
    }

    public static List<GymCode> getGymCodes() throws Exception {
        List<GymCode> out = new ArrayList<>();
        for (QueryDocumentSnapshot d : db.collection("trainerCodes").get().get().getDocuments()) {
            Boolean active = d.getBoolean("active");
            out.add(new GymCode(d.getId(), d.getString("gymName"), active == null || active));
        }
        out.sort(Comparator.comparing(GymCode::gymName, String.CASE_INSENSITIVE_ORDER));
        return out;
    }

    public static void setGymCodeActive(String code, boolean active) throws Exception {
        db.collection("trainerCodes").document(code).update("active", active).get();
    }

    public static void deleteGymCode(String code) throws Exception {
        db.collection("trainerCodes").document(code).delete().get();
    }

    // ---------------------------------------------------------------- workouts

    private static CollectionReference workouts(String uid) {
        return db.collection("users").document(uid).collection("workouts");
    }

    public static void addWorkout(String uid, Workout w) throws Exception {
        Map<String, Object> m = new HashMap<>();
        m.put("exercise", w.exercise());
        m.put("category", w.category());
        m.put("details", w.details());
        m.put("intensity", w.intensity());
        m.put("calories", w.calories());
        m.put("date", w.date());
        m.put("completed", w.completed());
        m.put("timestamp", System.currentTimeMillis());
        workouts(uid).add(m).get();
    }

    public static List<Workout> getWorkouts(String uid) throws Exception {
        List<Workout> out = new ArrayList<>();
        for (QueryDocumentSnapshot d : workouts(uid).orderBy("timestamp", Query.Direction.DESCENDING).get().get()
                .getDocuments()) {
            Long cal = d.getLong("calories");
            Boolean done = d.getBoolean("completed");
            out.add(new Workout(d.getId(), d.getString("exercise"), d.getString("category"),
                    d.getString("details"), d.getString("intensity"), cal == null ? 0 : cal.intValue(),
                    d.getString("date"), done == null || done));
        }
        return out;
    }

    public static void deleteWorkout(String uid, String workoutId) throws Exception {
        workouts(uid).document(workoutId).delete().get();
    }

    // ---------------------------------------------------------------- exercise library (trainer-authored)

    public static Exercise createExercise(Exercise e) throws Exception {
        Map<String, Object> m = new HashMap<>();
        m.put("name", e.name());
        m.put("category", e.category());
        m.put("sets", e.sets());
        m.put("reps", e.reps());
        m.put("notes", e.notes());
        m.put("timestamp", System.currentTimeMillis());
        var ref = db.collection("exercises").add(m).get();
        return new Exercise(ref.getId(), e.name(), e.category(), e.sets(), e.reps(), e.notes());
    }

    public static List<Exercise> getExerciseLibrary() throws Exception {
        List<Exercise> out = new ArrayList<>();
        for (QueryDocumentSnapshot d : db.collection("exercises")
                .orderBy("timestamp", Query.Direction.DESCENDING).get().get().getDocuments()) {
            Long sets = d.getLong("sets");
            Long reps = d.getLong("reps");
            out.add(new Exercise(d.getId(), d.getString("name"), d.getString("category"),
                    sets == null ? 0 : sets.intValue(), reps == null ? 0 : reps.intValue(), d.getString("notes")));
        }
        return out;
    }

    public static void deleteExercise(String exerciseId) throws Exception {
        db.collection("exercises").document(exerciseId).delete().get();
    }

    // ---------------------------------------------------------------- assigned exercises (per trainee)

    private static CollectionReference assigned(String traineeUid) {
        return db.collection("users").document(traineeUid).collection("assignedExercises");
    }

    public static void assignExercise(String traineeUid, AssignedExercise a) throws Exception {
        Map<String, Object> m = new HashMap<>();
        m.put("exerciseName", a.exerciseName());
        m.put("category", a.category());
        m.put("sets", a.sets());
        m.put("reps", a.reps());
        m.put("notes", a.notes());
        m.put("assignedBy", a.assignedBy());
        m.put("assignedDate", a.assignedDate());
        m.put("timestamp", System.currentTimeMillis());
        assigned(traineeUid).add(m).get();
    }

    public static List<AssignedExercise> getAssignedExercises(String traineeUid) throws Exception {
        List<AssignedExercise> out = new ArrayList<>();
        for (QueryDocumentSnapshot d : assigned(traineeUid)
                .orderBy("timestamp", Query.Direction.DESCENDING).get().get().getDocuments()) {
            Long sets = d.getLong("sets");
            Long reps = d.getLong("reps");
            out.add(new AssignedExercise(d.getId(), d.getString("exerciseName"), d.getString("category"),
                    sets == null ? 0 : sets.intValue(), reps == null ? 0 : reps.intValue(), d.getString("notes"),
                    d.getString("assignedBy"), d.getString("assignedDate")));
        }
        return out;
    }

    public static void removeAssignedExercise(String traineeUid, String assignedId) throws Exception {
        assigned(traineeUid).document(assignedId).delete().get();
    }

    // ---------------------------------------------------------------- goals

    public static void setDailyGoal(String traineeUid, int goal) throws Exception {
        db.collection("users").document(traineeUid).update("dailyGoal", goal).get();
    }

    // ---------------------------------------------------------------- user directories

    /** All trainers, for a trainee's "pick your trainer" dropdown at signup. */
    public static List<UserProfile> getTrainersForSelection() throws Exception {
        return getUsersByRole("Trainer");
    }

    /** Only the trainees that belong to this trainer - the data-isolation boundary. */
    public static List<UserProfile> getTraineesOf(String trainerUid) throws Exception {
        List<UserProfile> out = new ArrayList<>();
        for (QueryDocumentSnapshot d : db.collection("users")
                .whereEqualTo("role", "Trainee").whereEqualTo("trainerUid", trainerUid)
                .get().get().getDocuments()) {
            out.add(toProfile(d));
        }
        out.sort(Comparator.comparing(u -> u.name().toLowerCase()));
        return out;
    }

    public static List<UserProfile> getAllUsers() throws Exception {
        List<UserProfile> out = new ArrayList<>();
        for (QueryDocumentSnapshot d : db.collection("users").get().get().getDocuments()) {
            out.add(toProfile(d));
        }
        out.sort(Comparator.comparing((UserProfile u) -> u.role()).thenComparing(u -> u.name().toLowerCase()));
        return out;
    }

    private static List<UserProfile> getUsersByRole(String role) throws Exception {
        List<UserProfile> out = new ArrayList<>();
        for (QueryDocumentSnapshot d : db.collection("users").whereEqualTo("role", role).get().get().getDocuments()) {
            out.add(toProfile(d));
        }
        out.sort(Comparator.comparing(u -> u.name().toLowerCase()));
        return out;
    }

    // ---------------------------------------------------------------- admin user management

    public static void updateRole(String uid, String newRole, String assignmentId) throws Exception {
        if (!"Trainer".equals(newRole) && !"Trainee".equals(newRole)) {
            throw new IllegalArgumentException("Role can only be changed to Trainer or Trainee.");
        }
        DocumentReference profileRef = db.collection("users").document(uid);
        DocumentReference adminRef = db.collection("system").document("admin");
        db.runTransaction(transaction -> {
            DocumentSnapshot profile = transaction.get(profileRef).get();
            if (!profile.exists()) throw new IllegalArgumentException("Account could not be found.");

            Map<String, Object> updates = new HashMap<>();
            updates.put("role", newRole);
            if ("Trainer".equals(newRole)) {
                DocumentSnapshot gym = transaction.get(db.collection("trainerCodes").document(assignmentId)).get();
                if (!gym.exists()) throw new IllegalArgumentException("Selected gym could not be found.");
                updates.put("gymName", gym.getString("gymName"));
                updates.put("trainerUid", FieldValue.delete());
            } else {
                if (uid.equals(assignmentId)) {
                    throw new IllegalArgumentException("A trainer cannot be assigned to themselves.");
                }
                DocumentSnapshot trainer = transaction.get(profileRef.getParent().document(assignmentId)).get();
                if (!trainer.exists() || !"Trainer".equals(trainer.getString("role"))) {
                    throw new IllegalArgumentException("Selected trainer could not be found.");
                }
                updates.put("trainerUid", assignmentId);
                updates.put("gymName", FieldValue.delete());
            }

            DocumentSnapshot admin = null;
            if ("Admin".equals(profile.getString("role"))) admin = transaction.get(adminRef).get();
            transaction.update(profileRef, updates);
            if (admin != null && admin.exists() && uid.equals(admin.getString("uid"))) {
                transaction.delete(adminRef);
            }
            return null;
        }).get();
    }

    /** Deletes the auth account, the profile doc, and its workouts/assignedExercises subcollections. */
    public static void deleteUser(String uid) throws Exception {
        for (QueryDocumentSnapshot d : workouts(uid).get().get().getDocuments()) d.getReference().delete().get();
        for (QueryDocumentSnapshot d : assigned(uid).get().get().getDocuments()) d.getReference().delete().get();
        DocumentReference profileRef = db.collection("users").document(uid);
        DocumentReference adminRef = db.collection("system").document("admin");
        db.runTransaction(transaction -> {
            DocumentSnapshot profile = transaction.get(profileRef).get();
            DocumentSnapshot admin = transaction.get(adminRef).get();
            if (profile.exists()) transaction.delete(profileRef);
            if (profile.exists() && "Admin".equals(profile.getString("role"))
                    && admin.exists() && uid.equals(admin.getString("uid"))) {
                transaction.delete(adminRef);
            }
            return null;
        }).get();
        try {
            FirebaseAuth.getInstance().deleteUser(uid);
        } catch (Exception ignored) {
            // Auth user may already be gone; the Firestore data is removed either way.
        }
    }
}
