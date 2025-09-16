// api/tasks/[id].js

const admin = require("firebase-admin");
const cors = require("cors")({ origin: true });

// Initialize app if not already done
if (!admin.apps.length) {
  const serviceAccountJson = Buffer.from(
    process.env.FIREBASE_SERVICE_ACCOUNT_BASE64, "base64"
  ).toString("utf-8");
  const serviceAccount = JSON.parse(serviceAccountJson);
  admin.initializeApp({ credential: admin.credential.cert(serviceAccount) });
}

const db = admin.firestore();
const auth = admin.auth();
const tasksCollection = db.collection("tasks");

module.exports = async (req, res) => {
  cors(req, res, async () => {
    // --- Authentication ---
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

    // --- Get Task ID from URL ---
    // The file name [id].js makes the 'id' available in req.query
    const { id } = req.query;
    const taskRef = tasksCollection.doc(id);

    // --- ROUTER LOGIC ---
    switch (req.method) {
      case "PUT":
        console.log(`==> EXECUTING PUT /api/tasks/${id}`);
        try {
          // Get all possible fields from the request body
          const { title, description, dueDate, isDone } = req.body;
          console.log(`   - Received body:`, req.body);

          const taskDoc = await taskRef.get();
          console.log("   - Fetched document from Firestore.");

          if (!taskDoc.exists) {
            console.log("   - ERROR: Document not found.");
            return res.status(404).send({ error: "Task not found." });
          }
          if (taskDoc.data().userId !== userId) {
            console.log("   - ERROR: User ID mismatch. Forbidden.");
            return res.status(403).send({ error: "Forbidden." });
          }

          // Build an object with only the fields that were provided in the request
          const updatedFields = {};
          if (title !== undefined) updatedFields.title = title;
          if (description !== undefined) updatedFields.description = description;
          if (dueDate !== undefined) updatedFields.dueDate = dueDate;
          if (isDone !== undefined) updatedFields.isDone = isDone;

          console.log("   - Preparing to update with fields:", updatedFields);

          // Perform the update with the new fields
          await taskRef.update(updatedFields);
          console.log("   - SUCCESS: Firestore document updated.");
          res.status(200).send({ message: "Task updated successfully." });

        } catch (error) {
          // --- THIS IS THE MOST IMPORTANT PART ---
          console.error("!!! CRITICAL ERROR in PUT handler:", error);
          res.status(500).send({ 
              error: "Failed to update task.", 
              details: error.message // Send back the error details for easier debugging
          });
        }
        break;

      case "DELETE":
        try {
          const taskDoc = await taskRef.get();
          if (!taskDoc.exists) {
            return res.status(404).send({ error: "Task not found." });
          }
          if (taskDoc.data().userId !== userId) {
            return res.status(403).send({ error: "Forbidden." });
          }
          await taskRef.delete();
          res.status(204).send(); // No content
        } catch (error) {
          res.status(500).send({ error: "Failed to delete task." });
        }
        break;

      default:
        res.setHeader("Allow", ["PUT", "DELETE"]);
        res.status(405).end(`Method ${req.method} Not Allowed`);
    }
  });
};