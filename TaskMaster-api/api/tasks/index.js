const admin = require("firebase-admin");
const cors = require("cors")({ origin: true });

// --- INITIALIZE FIREBASE ADMIN (once per serverless instance) ---
// This checks if the app is already initialized to avoid errors.
if (!admin.apps.length) {
  // Decode the Base64 environment variable to get the JSON string
  const serviceAccountJson = Buffer.from(
    process.env.FIREBASE_SERVICE_ACCOUNT_BASE64,
    "base64"
  ).toString("utf-8");
  
  const serviceAccount = JSON.parse(serviceAccountJson);

  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount),
  });
}

const db = admin.firestore();
const auth = admin.auth();
const tasksCollection = db.collection("tasks");

// This is our main handler function
module.exports = async (req, res) => {
  // Handle CORS preflight requests
  cors(req, res, async () => {
    // --- Authentication Middleware ---
    const token = req.headers.authorization?.split("Bearer ")[1];
    if (!token) {
      return res.status(401).send({ error: "Unauthorized: No token." });
    }

    let decodedToken;
    try {
      decodedToken = await auth.verifyIdToken(token);
    } catch (error) {
      return res.status(401).send({ error: "Unauthorized: Invalid token." });
    }
    const userId = decodedToken.uid;

    // --- ROUTER LOGIC (based on HTTP method) ---
    switch (req.method) {
      case "POST":
        try {
          const { title, description, dueDate } = req.body;
          if (!title) {
            return res.status(400).send({ error: "Title is required." });
          }
          const newTask = {
            userId,
            title,
            description: description || "",
            isDone: false,
            dueDate: dueDate || null,
            createdAt: new Date().toISOString(),
          };
          const docRef = await tasksCollection.add(newTask);
          res.status(201).send({ id: docRef.id, ...newTask });
        } catch (error) {
          res.status(500).send({ error: "Failed to create task." });
        }
        break;

      // NOTE: We don't need a GET route here because the app reads live from Firestore.
      // If you needed it, it would go here.

      default:
        res.setHeader("Allow", ["POST"]);
        res.status(405).end(`Method ${req.method} Not Allowed`);
    }
  });
};